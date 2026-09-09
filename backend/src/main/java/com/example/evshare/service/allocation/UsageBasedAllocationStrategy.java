package com.example.evshare.service.allocation;

import com.example.evshare.entity.*;
import com.example.evshare.entity.enums.AllocationStrategy;
import com.example.evshare.entity.enums.UsageSessionStatus;
import com.example.evshare.exception.BusinessException;
import com.example.evshare.repository.OwnershipShareRepository;
import com.example.evshare.repository.UsageSessionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Strategy allocating syndicate expenses based on vehicle utilization according to BUSINESS_RULES.md (BR-FIN-02):
 * - Distance Logged: Owed = TotalExpense * (DistanceLogged_i / sum(DistanceLogged))
 * - Hours Used / Duration: Owed = TotalExpense * (HoursUsed_i / sum(HoursUsed))
 *
 * If zero utilization has been recorded for the vehicle, gracefully falls back to active ownership equity percentages.
 * Adheres to Banker's Rounding (HALF_EVEN, scale 2) and deterministic residual penny absorption.
 */
@Component
public class UsageBasedAllocationStrategy implements CostAllocationStrategy {

    private static final Logger log = LoggerFactory.getLogger(UsageBasedAllocationStrategy.class);
    private static final int COMPUTATION_SCALE = 6;
    private static final int CURRENCY_SCALE = 2;

    public static final String ROUNDING_STRATEGY_DOC =
            "Banker's Rounding (HALF_EVEN) to 2 decimal places. Residual pennies (|delta| > 0) are " +
            "deterministically absorbed by the active co-owner with the highest recorded utilization (tie-breaker: lowest userId).";

    private final UsageSessionRepository usageSessionRepository;
    private final OwnershipShareRepository ownershipShareRepository;
    private final OwnershipBasedAllocationStrategy ownershipBasedAllocationStrategy;

    public UsageBasedAllocationStrategy(UsageSessionRepository usageSessionRepository,
                                        OwnershipShareRepository ownershipShareRepository,
                                        OwnershipBasedAllocationStrategy ownershipBasedAllocationStrategy) {
        this.usageSessionRepository = usageSessionRepository;
        this.ownershipShareRepository = ownershipShareRepository;
        this.ownershipBasedAllocationStrategy = ownershipBasedAllocationStrategy;
    }

    @Override
    public AllocationStrategy getStrategyMode() {
        return AllocationStrategy.USAGE_BASED;
    }

    @Override
    public AllocationResult allocate(Expense expense) {
        return allocate(expense, UsageMetric.DISTANCE);
    }

    /**
     * Allocates expense using a designated usage metric (DISTANCE or DURATION) per BR-FIN-02.
     */
    public AllocationResult allocate(Expense expense, UsageMetric preferredMetric) {
        if (expense == null) {
            throw new IllegalArgumentException("Expense cannot be null for usage-based allocation");
        }
        OwnershipGroup group = expense.getGroup();
        if (group == null || group.getId() == null) {
            throw new BusinessException("Expense must be associated with a valid ownership group", HttpStatus.BAD_REQUEST);
        }

        BigDecimal totalAmount = expense.getTotalAmount();
        if (totalAmount == null || totalAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("Expense total amount must be positive for allocation", HttpStatus.BAD_REQUEST);
        }

        // Active members in group
        List<OwnershipShare> activeShares = ownershipShareRepository.findByGroupIdAndIsActiveTrue(group.getId());
        if (activeShares.isEmpty()) {
            throw new BusinessException("No active ownership shares found for group", HttpStatus.BAD_REQUEST);
        }

        Vehicle vehicle = expense.getVehicle() != null ? expense.getVehicle() : group.getVehicle();
        if (vehicle == null) {
            log.warn("Expense {} has no assigned vehicle; falling back to ownership-based allocation", expense.getId());
            return ownershipBasedAllocationStrategy.allocateForShares(expense, activeShares);
        }

        // Fetch completed usage sessions for this vehicle
        List<UsageSession> sessions = usageSessionRepository.findByBookingVehicleIdOrderByCheckInTimeDesc(vehicle.getId());
        return allocateForSessionsAndShares(expense, sessions, activeShares, preferredMetric);
    }

    /**
     * Backward-compatible overload defaulting to DISTANCE metric.
     */
    public AllocationResult allocateForSessionsAndShares(Expense expense, List<UsageSession> sessions, List<OwnershipShare> activeShares) {
        return allocateForSessionsAndShares(expense, sessions, activeShares, UsageMetric.DISTANCE);
    }

    /**
     * Package-private testable method for isolated unit testing with arbitrary sessions, shares, and metrics.
     */
    public AllocationResult allocateForSessionsAndShares(Expense expense, List<UsageSession> sessions,
                                                        List<OwnershipShare> activeShares, UsageMetric metric) {
        BigDecimal totalAmount = expense.getTotalAmount().setScale(CURRENCY_SCALE, RoundingMode.HALF_EVEN);
        String currency = expense.getCurrency() != null ? expense.getCurrency() : "VND";

        Set<Long> activeUserIds = activeShares.stream()
                .filter(s -> Boolean.TRUE.equals(s.getIsActive()))
                .map(s -> s.getUser().getId())
                .collect(Collectors.toSet());

        // Map userId -> User
        Map<Long, User> userMap = activeShares.stream()
                .filter(s -> Boolean.TRUE.equals(s.getIsActive()))
                .collect(Collectors.toMap(s -> s.getUser().getId(), OwnershipShare::getUser, (u1, u2) -> u1));

        // Aggregate utilization per user from completed sessions
        Map<Long, BigDecimal> userDistanceMap = new HashMap<>();
        Map<Long, BigDecimal> userDurationMap = new HashMap<>();
        for (Long userId : activeUserIds) {
            userDistanceMap.put(userId, BigDecimal.ZERO);
            userDurationMap.put(userId, BigDecimal.ZERO);
        }

        if (sessions != null) {
            for (UsageSession session : sessions) {
                if (session.getStatus() == UsageSessionStatus.COMPLETED
                        && session.getBooking() != null
                        && session.getBooking().getUser() != null) {
                    Long userId = session.getBooking().getUser().getId();
                    if (activeUserIds.contains(userId)) {
                        // Distance aggregation
                        BigDecimal startOdo = session.getStartOdometer() != null ? session.getStartOdometer() : BigDecimal.ZERO;
                        BigDecimal endOdo = session.getEndOdometer() != null ? session.getEndOdometer() : BigDecimal.ZERO;
                        BigDecimal distance = endOdo.subtract(startOdo);
                        if (distance.compareTo(BigDecimal.ZERO) > 0) {
                            userDistanceMap.merge(userId, distance, BigDecimal::add);
                        }

                        // Duration aggregation (hours)
                        if (session.getCheckInTime() != null && session.getCheckOutTime() != null) {
                            long seconds = Duration.between(session.getCheckInTime(), session.getCheckOutTime()).toSeconds();
                            if (seconds > 0) {
                                BigDecimal hours = BigDecimal.valueOf(seconds)
                                        .divide(BigDecimal.valueOf(3600), COMPUTATION_SCALE, RoundingMode.HALF_EVEN);
                                userDurationMap.merge(userId, hours, BigDecimal::add);
                            }
                        }
                    }
                }
            }
        }

        BigDecimal totalDistance = userDistanceMap.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalDuration = userDurationMap.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);

        // Determine effective metric:
        // If DISTANCE requested: use distance if > 0; if distance == 0 and duration > 0, fallback to duration.
        // If DURATION requested: use duration if > 0; if duration == 0 and distance > 0, fallback to distance.
        UsageMetric effectiveMetric = metric != null ? metric : UsageMetric.DISTANCE;
        Map<Long, BigDecimal> activeMetricMap;
        BigDecimal totalMetricValue;
        String metricUnit;

        if (effectiveMetric == UsageMetric.DURATION) {
            if (totalDuration.compareTo(BigDecimal.ZERO) > 0) {
                activeMetricMap = userDurationMap;
                totalMetricValue = totalDuration;
                metricUnit = "hrs";
            } else if (totalDistance.compareTo(BigDecimal.ZERO) > 0) {
                log.info("0.0 hrs duration recorded; degrading to distance-based metric for expense {}", expense.getId());
                activeMetricMap = userDistanceMap;
                totalMetricValue = totalDistance;
                metricUnit = "km";
                effectiveMetric = UsageMetric.DISTANCE;
            } else {
                return fallbackToOwnership(expense, activeShares, totalAmount, currency, "0.0 hrs and 0.0 km recorded usage");
            }
        } else {
            if (totalDistance.compareTo(BigDecimal.ZERO) > 0) {
                activeMetricMap = userDistanceMap;
                totalMetricValue = totalDistance;
                metricUnit = "km";
            } else if (totalDuration.compareTo(BigDecimal.ZERO) > 0) {
                log.info("0.0 km distance recorded; degrading to duration-based metric for expense {}", expense.getId());
                activeMetricMap = userDurationMap;
                totalMetricValue = totalDuration;
                metricUnit = "hrs";
                effectiveMetric = UsageMetric.DURATION;
            } else {
                return fallbackToOwnership(expense, activeShares, totalAmount, currency, "0.0 km recorded usage");
            }
        }

        // Sort users deterministically: primary metric value DESC, secondary userId ASC
        List<Map.Entry<Long, BigDecimal>> sortedEntries = activeMetricMap.entrySet().stream()
                .sorted(Comparator
                        .<Map.Entry<Long, BigDecimal>, BigDecimal>comparing(Map.Entry::getValue, Comparator.reverseOrder())
                        .thenComparing(Map.Entry::getKey)
                )
                .toList();

        List<AllocatedMemberShare> memberShares = new ArrayList<>();
        BigDecimal sumRounded = BigDecimal.ZERO;

        for (Map.Entry<Long, BigDecimal> entry : sortedEntries) {
            Long userId = entry.getKey();
            BigDecimal userMetricValue = entry.getValue();
            User user = userMap.get(userId);

            BigDecimal usageRatio = userMetricValue.divide(totalMetricValue, COMPUTATION_SCALE, RoundingMode.HALF_EVEN);
            BigDecimal effectivePercentage = usageRatio.multiply(new BigDecimal("100.00")).setScale(CURRENCY_SCALE, RoundingMode.HALF_EVEN);
            BigDecimal rawAmount = totalAmount.multiply(usageRatio).setScale(COMPUTATION_SCALE, RoundingMode.HALF_EVEN);
            BigDecimal roundedAmount = rawAmount.setScale(CURRENCY_SCALE, RoundingMode.HALF_EVEN);

            sumRounded = sumRounded.add(roundedAmount);

            String explanation = String.format(
                    "Usage-based allocation (%s): %s %s / %s %s (%s%%) of %s %s = %s %s",
                    effectiveMetric.name().toLowerCase(),
                    userMetricValue.setScale(2, RoundingMode.HALF_EVEN).toPlainString(),
                    metricUnit,
                    totalMetricValue.setScale(2, RoundingMode.HALF_EVEN).toPlainString(),
                    metricUnit,
                    effectivePercentage.toPlainString(),
                    totalAmount.toPlainString(),
                    currency,
                    roundedAmount.toPlainString(),
                    currency
            );

            memberShares.add(new AllocatedMemberShare(
                    userId,
                    user,
                    roundedAmount,
                    effectivePercentage,
                    rawAmount,
                    BigDecimal.ZERO.setScale(CURRENCY_SCALE, RoundingMode.HALF_EVEN),
                    explanation
            ));
        }

        // Residual penny absorption
        BigDecimal residual = totalAmount.subtract(sumRounded).setScale(CURRENCY_SCALE, RoundingMode.HALF_EVEN);
        if (residual.compareTo(BigDecimal.ZERO) != 0 && !memberShares.isEmpty()) {
            AllocatedMemberShare primary = memberShares.get(0);
            primary.setAllocatedAmount(primary.getAllocatedAmount().add(residual));
            primary.setRoundingDelta(residual);
            String sign = residual.compareTo(BigDecimal.ZERO) > 0 ? "+" : "";
            primary.setExplanation(primary.getExplanation() + String.format(
                    " (adjusted by %s%s %s to absorb rounding residual for exact reconciliation)",
                    sign, residual.toPlainString(), currency
            ));
        }

        BigDecimal finalSum = memberShares.stream()
                .map(AllocatedMemberShare::getAllocatedAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(CURRENCY_SCALE, RoundingMode.HALF_EVEN);

        if (finalSum.compareTo(totalAmount) != 0) {
            throw new IllegalStateException(String.format(
                    "Mathematical reconciliation failure: total %s %s != allocated sum %s %s",
                    totalAmount, currency, finalSum, currency
            ));
        }

        String summary = String.format(
                "Usage-based allocation (%s) of %s %s across %d active co-owners based on %s total %s (reconciled: 100.00%%)",
                effectiveMetric.name().toLowerCase(),
                totalAmount.toPlainString(), currency, memberShares.size(),
                totalMetricValue.setScale(2, RoundingMode.HALF_EVEN).toPlainString(), metricUnit
        );

        return new AllocationResult(
                totalAmount,
                currency,
                AllocationStrategy.USAGE_BASED,
                memberShares,
                true,
                finalSum,
                residual,
                ROUNDING_STRATEGY_DOC,
                summary
        );
    }

    private AllocationResult fallbackToOwnership(Expense expense, List<OwnershipShare> activeShares,
                                                 BigDecimal totalAmount, String currency, String reason) {
        log.info("Zero utilization recorded for vehicle; falling back to ownership equity allocation for expense {}: {}",
                expense.getId(), reason);
        AllocationResult ownershipResult = ownershipBasedAllocationStrategy.allocateForShares(expense, activeShares);
        ownershipResult.setStrategy(AllocationStrategy.USAGE_BASED);
        ownershipResult.setSummary(String.format(
                "Usage-based allocation for %s %s (fallback to ownership equity due to %s)",
                totalAmount.toPlainString(), currency, reason
        ));
        for (AllocatedMemberShare share : ownershipResult.getShares()) {
            share.setExplanation(share.getExplanation() + String.format(" [%s; defaulted to ownership ratio]", reason));
        }
        return ownershipResult;
    }
}
