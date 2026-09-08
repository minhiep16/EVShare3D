package com.example.evshare.service.impl;

import com.example.evshare.dto.request.FairUsageInput;
import com.example.evshare.dto.response.FairUsageMetricsResponse;
import com.example.evshare.dto.response.GroupFairUsageResponse;
import com.example.evshare.entity.Booking;
import com.example.evshare.entity.OwnershipGroup;
import com.example.evshare.entity.OwnershipShare;
import com.example.evshare.entity.UsageSession;
import com.example.evshare.entity.enums.BookingStatus;
import com.example.evshare.entity.enums.ImbalanceLevel;
import com.example.evshare.exception.BusinessException;
import com.example.evshare.exception.ResourceNotFoundException;
import com.example.evshare.repository.BookingRepository;
import com.example.evshare.repository.OwnershipGroupRepository;
import com.example.evshare.repository.OwnershipShareRepository;
import com.example.evshare.repository.UsageSessionRepository;
import com.example.evshare.security.OwnershipSecurity;
import com.example.evshare.service.FairUsageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.*;

/**
 * Implementation of FairUsageService providing deterministic equity quota,
 * weighted usage units, fairness ratios, and transparent recommendations per BR-FAIR-01..03.
 */
@Service
public class FairUsageServiceImpl implements FairUsageService {

    private static final Logger log = LoggerFactory.getLogger(FairUsageServiceImpl.class);

    // Multipliers per BR-FAIR-01
    public static final BigDecimal PEAK_MULTIPLIER = BigDecimal.valueOf(1.5);
    public static final BigDecimal STANDARD_MULTIPLIER = BigDecimal.valueOf(1.0);
    public static final BigDecimal OFF_PEAK_MULTIPLIER = BigDecimal.valueOf(0.7);

    // Classification boundaries per BR-FAIR-02
    public static final BigDecimal FAIR_LOWER = new BigDecimal("0.90");
    public static final BigDecimal FAIR_UPPER = new BigDecimal("1.10");
    public static final BigDecimal SLIGHT_LOWER = new BigDecimal("0.75");
    public static final BigDecimal SLIGHT_UPPER = new BigDecimal("1.25");
    public static final BigDecimal IMBALANCED_LOWER = new BigDecimal("0.50");
    public static final BigDecimal IMBALANCED_UPPER = new BigDecimal("1.50");

    // Dynamic scheduling priority thresholds per BR-FAIR-03
    public static final BigDecimal PRIORITY_THRESHOLD = new BigDecimal("1.00"); // FR < 1.0 gains priority
    public static final BigDecimal PEAK_RESTRICTION_THRESHOLD = new BigDecimal("1.30"); // FR > 1.30 restricted

    public static final int DEFAULT_WINDOW_DAYS = 30;
    public static final BigDecimal DEFAULT_WINDOW_HOURS = BigDecimal.valueOf(720); // 30 days * 24h

    private final OwnershipGroupRepository ownershipGroupRepository;
    private final OwnershipShareRepository ownershipShareRepository;
    private final BookingRepository bookingRepository;
    private final UsageSessionRepository usageSessionRepository;
    private final OwnershipSecurity ownershipSecurity;

    public FairUsageServiceImpl(OwnershipGroupRepository ownershipGroupRepository,
                                OwnershipShareRepository ownershipShareRepository,
                                BookingRepository bookingRepository,
                                UsageSessionRepository usageSessionRepository,
                                OwnershipSecurity ownershipSecurity) {
        this.ownershipGroupRepository = ownershipGroupRepository;
        this.ownershipShareRepository = ownershipShareRepository;
        this.bookingRepository = bookingRepository;
        this.usageSessionRepository = usageSessionRepository;
        this.ownershipSecurity = ownershipSecurity;
    }

    // =========================================================================
    // PURE MATHEMATICAL FUNCTIONS (100% Deterministic & Unit Testable)
    // =========================================================================

    @Override
    public BigDecimal calculateWeightedUsage(BigDecimal peakHours, BigDecimal standardHours, BigDecimal offPeakHours) {
        BigDecimal peak = peakHours != null ? peakHours : BigDecimal.ZERO;
        BigDecimal standard = standardHours != null ? standardHours : BigDecimal.ZERO;
        BigDecimal offPeak = offPeakHours != null ? offPeakHours : BigDecimal.ZERO;

        BigDecimal weighted = peak.multiply(PEAK_MULTIPLIER)
                .add(standard.multiply(STANDARD_MULTIPLIER))
                .add(offPeak.multiply(OFF_PEAK_MULTIPLIER));

        return weighted.setScale(2, RoundingMode.HALF_UP);
    }

    @Override
    public BigDecimal calculateEquityQuota(BigDecimal ownershipPercentage, BigDecimal totalAvailableHours) {
        if (ownershipPercentage == null || totalAvailableHours == null) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        return ownershipPercentage
                .divide(BigDecimal.valueOf(100), 6, RoundingMode.HALF_UP)
                .multiply(totalAvailableHours)
                .setScale(2, RoundingMode.HALF_UP);
    }

    @Override
    public BigDecimal calculateFairnessRatio(BigDecimal userWeightedUsage, BigDecimal totalGroupWeightedUsage, BigDecimal ownershipPercentage) {
        if (ownershipPercentage == null || ownershipPercentage.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }

        // Base case: Zero group usage -> all members are in perfect equilibrium (1.00)
        if (totalGroupWeightedUsage == null || totalGroupWeightedUsage.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ONE.setScale(2, RoundingMode.HALF_UP);
        }

        BigDecimal userWeighted = userWeightedUsage != null ? userWeightedUsage : BigDecimal.ZERO;
        BigDecimal usageShare = userWeighted.divide(totalGroupWeightedUsage, 6, RoundingMode.HALF_UP);
        BigDecimal equityShare = ownershipPercentage.divide(BigDecimal.valueOf(100), 6, RoundingMode.HALF_UP);

        return usageShare.divide(equityShare, 2, RoundingMode.HALF_UP);
    }

    @Override
    public BigDecimal calculateFairnessScore(BigDecimal fairnessRatio) {
        if (fairnessRatio == null) {
            return BigDecimal.valueOf(100).setScale(2, RoundingMode.HALF_UP);
        }

        // Deterministic linear score: Score = max(0.0, 100.0 - 100.0 * |FR - 1.0|)
        BigDecimal diff = fairnessRatio.subtract(BigDecimal.ONE).abs();
        BigDecimal penalty = diff.multiply(BigDecimal.valueOf(100));
        BigDecimal score = BigDecimal.valueOf(100).subtract(penalty);

        if (score.compareTo(BigDecimal.ZERO) < 0) {
            score = BigDecimal.ZERO;
        }
        return score.setScale(2, RoundingMode.HALF_UP);
    }

    @Override
    public ImbalanceLevel determineImbalanceLevel(BigDecimal fairnessRatio) {
        if (fairnessRatio == null) {
            return ImbalanceLevel.FAIR;
        }

        if (fairnessRatio.compareTo(FAIR_LOWER) >= 0 && fairnessRatio.compareTo(FAIR_UPPER) <= 0) {
            return ImbalanceLevel.FAIR;
        }
        if ((fairnessRatio.compareTo(SLIGHT_LOWER) >= 0 && fairnessRatio.compareTo(FAIR_LOWER) < 0) ||
                (fairnessRatio.compareTo(FAIR_UPPER) > 0 && fairnessRatio.compareTo(SLIGHT_UPPER) <= 0)) {
            return ImbalanceLevel.SLIGHTLY_IMBALANCED;
        }
        if ((fairnessRatio.compareTo(IMBALANCED_LOWER) >= 0 && fairnessRatio.compareTo(SLIGHT_LOWER) < 0) ||
                (fairnessRatio.compareTo(SLIGHT_UPPER) > 0 && fairnessRatio.compareTo(IMBALANCED_UPPER) <= 0)) {
            return ImbalanceLevel.IMBALANCED;
        }
        return ImbalanceLevel.SEVERELY_IMBALANCED;
    }

    @Override
    public String generateRecommendation(BigDecimal fairnessRatio, ImbalanceLevel level, boolean isOverUser) {
        if (level == null) {
            return "Usage data insufficient for recommendation.";
        }

        switch (level) {
            case FAIR:
                return "Usage is in equilibrium with your equity stake. Standard booking access maintained.";

            case SLIGHTLY_IMBALANCED:
                if (isOverUser) {
                    return "Usage slightly exceeds proportional equity. Consider shifting non-urgent trips to off-peak hours (22:00–07:00).";
                } else {
                    return "Usage is slightly below your equity entitlement. You have priority booking for upcoming peak slots.";
                }

            case IMBALANCED:
                if (isOverUser) {
                    return "Usage significantly exceeds your quota (FR > 1.25). Recommend utilizing off-peak slots with 0.7x demand weighting to rebalance.";
                } else {
                    return "Usage is significantly below your quota. Early booking window advantage granted up to 48 hours in advance.";
                }

            case SEVERELY_IMBALANCED:
                if (isOverUser) {
                    return "Severe over-utilization (FR > 1.50). Restricted from booking peak slots unless other co-owners forfeit reservations (per BR-FAIR-03).";
                } else {
                    return "Severe under-utilization detected. Immediate priority scheduling unlocked across all operating windows.";
                }

            default:
                return "Maintain equitable driving schedule.";
        }
    }

    @Override
    public FairUsageMetricsResponse computeMetrics(FairUsageInput input, BigDecimal totalGroupWeightedUnits, BigDecimal totalAvailableHours) {
        if (input == null) {
            throw new IllegalArgumentException("FairUsageInput cannot be null");
        }

        BigDecimal availableHours = totalAvailableHours != null ? totalAvailableHours : DEFAULT_WINDOW_HOURS;
        BigDecimal quota = calculateEquityQuota(input.getOwnershipPercentage(), availableHours);
        BigDecimal weightedUsage = calculateWeightedUsage(input.getPeakHours(), input.getStandardHours(), input.getOffPeakHours());
        BigDecimal ratio = calculateFairnessRatio(weightedUsage, totalGroupWeightedUnits, input.getOwnershipPercentage());
        BigDecimal score = calculateFairnessScore(ratio);
        ImbalanceLevel level = determineImbalanceLevel(ratio);

        boolean isOverUser = ratio.compareTo(BigDecimal.ONE) > 0;
        boolean isPriorityEligible = ratio.compareTo(PRIORITY_THRESHOLD) < 0;
        boolean isPeakRestricted = ratio.compareTo(PEAK_RESTRICTION_THRESHOLD) > 0;
        String recommendation = generateRecommendation(ratio, level, isOverUser);

        return FairUsageMetricsResponse.builder()
                .userId(input.getUserId())
                .ownershipPercentage(input.getOwnershipPercentage().setScale(2, RoundingMode.HALF_UP))
                .equityQuotaHours(quota)
                .bookingFrequency(input.getBookingFrequency())
                .totalDurationHours(input.getBookingDurationHours().setScale(2, RoundingMode.HALF_UP))
                .totalDistanceKm(input.getDistanceKm().setScale(2, RoundingMode.HALF_UP))
                .cancellationCount(input.getCancellations())
                .lateCancellationCount(input.getLateCancellations())
                .peakHours(input.getPeakHours().setScale(2, RoundingMode.HALF_UP))
                .standardHours(input.getStandardHours().setScale(2, RoundingMode.HALF_UP))
                .offPeakHours(input.getOffPeakHours().setScale(2, RoundingMode.HALF_UP))
                .weightedUsageUnits(weightedUsage)
                .fairnessRatio(ratio)
                .fairnessScore(score)
                .imbalanceLevel(level)
                .isPriorityBookingEligible(isPriorityEligible)
                .isPeakRestricted(isPeakRestricted)
                .recommendation(recommendation)
                .build();
    }

    // =========================================================================
    // DATABASE AGGREGATION METHODS
    // =========================================================================

    @Override
    @Transactional(readOnly = true)
    public GroupFairUsageResponse getGroupFairUsage(Long groupId, int windowDays, Long currentUserId, boolean isAdmin) {
        if (groupId == null) {
            throw new BusinessException("Group ID is required", HttpStatus.BAD_REQUEST);
        }

        OwnershipGroup group = ownershipGroupRepository.findById(groupId)
                .orElseThrow(() -> new ResourceNotFoundException("Ownership group not found with id: " + groupId));

        // Scoped authorization
        if (!isAdmin && currentUserId != null) {
            boolean isMember = ownershipSecurity.isGroupMember(groupId, currentUserId);
            if (!isMember) {
                throw new AccessDeniedException("You do not have permission to view this group's fair usage analytics");
            }
        }

        int days = windowDays > 0 ? windowDays : DEFAULT_WINDOW_DAYS;
        BigDecimal totalAvailableHours = BigDecimal.valueOf(days * 24L);

        List<OwnershipShare> activeShares = ownershipShareRepository.findByGroupIdAndIsActiveTrue(groupId);

        // Fetch bookings for vehicle within trailing window
        Instant from = Instant.now().minus(days, ChronoUnit.DAYS);
        Instant to = Instant.now().plus(7, ChronoUnit.DAYS); // include imminent bookings

        List<Booking> bookings = bookingRepository.findTimelineBookings(group.getVehicle().getId(), from, to);

        // Map data per user
        Map<Long, UserAggregatedData> userAggregates = new HashMap<>();
        for (OwnershipShare share : activeShares) {
            Long uid = share.getUser().getId();
            userAggregates.put(uid, new UserAggregatedData(uid, share.getUser().getFullName(), share.getPercentage()));
        }

        BigDecimal totalGroupUsageHours = BigDecimal.ZERO;

        for (Booking b : bookings) {
            if (b.getStatus() == BookingStatus.CANCELLED || b.getStatus() == BookingStatus.REJECTED) {
                continue;
            }
            Long uid = b.getUser().getId();
            UserAggregatedData agg = userAggregates.get(uid);
            if (agg == null) {
                continue;
            }

            agg.bookingFrequency++;
            long minutes = Math.max(0, ChronoUnit.MINUTES.between(b.getStartTime(), b.getEndTime()));
            BigDecimal hours = BigDecimal.valueOf(minutes).divide(BigDecimal.valueOf(60), 4, RoundingMode.HALF_UP);
            agg.totalDurationHours = agg.totalDurationHours.add(hours);
            totalGroupUsageHours = totalGroupUsageHours.add(hours);

            // Break down hours into demand tiers
            DemandBreakdown tierHours = partitionInterval(b.getStartTime(), b.getEndTime());
            agg.peakHours = agg.peakHours.add(tierHours.peakHours);
            agg.standardHours = agg.standardHours.add(tierHours.standardHours);
            agg.offPeakHours = agg.offPeakHours.add(tierHours.offPeakHours);
        }

        // Add mileage from usage sessions
        List<UsageSession> sessions = usageSessionRepository.findByVehicleId(group.getVehicle().getId());
        for (UsageSession s : sessions) {
            if (s.getBooking() != null && s.getEndOdometer() != null && s.getStartOdometer() != null) {
                Long uid = s.getBooking().getUser().getId();
                UserAggregatedData agg = userAggregates.get(uid);
                if (agg != null) {
                    BigDecimal dist = s.getEndOdometer().subtract(s.getStartOdometer()).max(BigDecimal.ZERO);
                    agg.distanceKm = agg.distanceKm.add(dist);
                }
            }
        }

        // Step 1: Compute weighted usage for each member and sum group total
        BigDecimal totalGroupWeightedUnits = BigDecimal.ZERO;
        for (UserAggregatedData agg : userAggregates.values()) {
            BigDecimal userWeighted = calculateWeightedUsage(agg.peakHours, agg.standardHours, agg.offPeakHours);
            agg.weightedUnits = userWeighted;
            totalGroupWeightedUnits = totalGroupWeightedUnits.add(userWeighted);
        }

        // Step 2: Compute deterministic metrics for each member
        List<FairUsageMetricsResponse> memberMetrics = new ArrayList<>();
        for (UserAggregatedData agg : userAggregates.values()) {
            FairUsageInput input = FairUsageInput.builder()
                    .userId(agg.userId)
                    .ownershipPercentage(agg.ownershipPercentage)
                    .bookingFrequency(agg.bookingFrequency)
                    .bookingDurationHours(agg.totalDurationHours)
                    .distanceKm(agg.distanceKm)
                    .cancellations(agg.cancellations)
                    .lateCancellations(agg.lateCancellations)
                    .peakHours(agg.peakHours)
                    .standardHours(agg.standardHours)
                    .offPeakHours(agg.offPeakHours)
                    .build();

            FairUsageMetricsResponse metrics = computeMetrics(input, totalGroupWeightedUnits, totalAvailableHours);
            // Set full name from user record
            FairUsageMetricsResponse enriched = FairUsageMetricsResponse.builder()
                    .userId(metrics.getUserId())
                    .fullName(agg.fullName)
                    .ownershipPercentage(metrics.getOwnershipPercentage())
                    .equityQuotaHours(metrics.getEquityQuotaHours())
                    .bookingFrequency(metrics.getBookingFrequency())
                    .totalDurationHours(metrics.getTotalDurationHours())
                    .totalDistanceKm(metrics.getTotalDistanceKm())
                    .cancellationCount(metrics.getCancellationCount())
                    .lateCancellationCount(metrics.getLateCancellationCount())
                    .peakHours(metrics.getPeakHours())
                    .standardHours(metrics.getStandardHours())
                    .offPeakHours(metrics.getOffPeakHours())
                    .weightedUsageUnits(metrics.getWeightedUsageUnits())
                    .fairnessRatio(metrics.getFairnessRatio())
                    .fairnessScore(metrics.getFairnessScore())
                    .imbalanceLevel(metrics.getImbalanceLevel())
                    .isPriorityBookingEligible(metrics.isPriorityBookingEligible())
                    .isPeakRestricted(metrics.isPeakRestricted())
                    .recommendation(metrics.getRecommendation())
                    .build();

            memberMetrics.add(enriched);
        }

        // Sort members by ownership percentage descending
        memberMetrics.sort(Comparator.comparing(FairUsageMetricsResponse::getOwnershipPercentage).reversed());

        return GroupFairUsageResponse.builder()
                .groupId(group.getId())
                .groupName(group.getGroupName())
                .vehicleId(group.getVehicle().getId())
                .vehicleModel(group.getVehicle().getManufacturer() + " " + group.getVehicle().getModelName())
                .evaluationWindowDays(days)
                .totalAvailableHours(totalAvailableHours.setScale(2, RoundingMode.HALF_UP))
                .totalGroupUsageHours(totalGroupUsageHours.setScale(2, RoundingMode.HALF_UP))
                .totalGroupWeightedUnits(totalGroupWeightedUnits.setScale(2, RoundingMode.HALF_UP))
                .memberMetrics(memberMetrics)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public FairUsageMetricsResponse getUserFairUsage(Long groupId, Long userId, int windowDays, Long currentUserId, boolean isAdmin) {
        GroupFairUsageResponse groupReport = getGroupFairUsage(groupId, windowDays, currentUserId, isAdmin);
        return groupReport.getMemberMetrics().stream()
                .filter(m -> m.getUserId().equals(userId))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException(
                        String.format("User [%d] is not an active co-owner in group [%d]", userId, groupId)));
    }

    // =========================================================================
    // HELPER CLASSES & TIME-OF-DAY DEMAND PARTITIONING
    // =========================================================================

    private static class UserAggregatedData {
        Long userId;
        String fullName;
        BigDecimal ownershipPercentage;
        int bookingFrequency = 0;
        BigDecimal totalDurationHours = BigDecimal.ZERO;
        BigDecimal distanceKm = BigDecimal.ZERO;
        int cancellations = 0;
        int lateCancellations = 0;
        BigDecimal peakHours = BigDecimal.ZERO;
        BigDecimal standardHours = BigDecimal.ZERO;
        BigDecimal offPeakHours = BigDecimal.ZERO;
        BigDecimal weightedUnits = BigDecimal.ZERO;

        UserAggregatedData(Long userId, String fullName, BigDecimal ownershipPercentage) {
            this.userId = userId;
            this.fullName = fullName;
            this.ownershipPercentage = ownershipPercentage;
        }
    }

    public static class DemandBreakdown {
        public final BigDecimal peakHours;
        public final BigDecimal standardHours;
        public final BigDecimal offPeakHours;

        public DemandBreakdown(BigDecimal peakHours, BigDecimal standardHours, BigDecimal offPeakHours) {
            this.peakHours = peakHours;
            this.standardHours = standardHours;
            this.offPeakHours = offPeakHours;
        }
    }

    /**
     * Partitions a time interval into Peak, Standard, and Off-Peak hours per BR-FAIR-01:
     * - Peak: Friday 16:00 to Sunday 22:00
     * - Off-Peak: Daily 22:00 to 07:00
     * - Standard: All remaining hours (Monday–Friday 07:00 to 16:00, Mon-Thu 16:00-22:00)
     */
    public DemandBreakdown partitionInterval(Instant start, Instant end) {
        if (start == null || end == null || !end.isAfter(start)) {
            return new DemandBreakdown(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO);
        }

        long peakMinutes = 0;
        long standardMinutes = 0;
        long offPeakMinutes = 0;

        ZonedDateTime current = start.atZone(ZoneId.of("UTC"));
        ZonedDateTime stop = end.atZone(ZoneId.of("UTC"));

        while (current.isBefore(stop)) {
            ZonedDateTime nextHour = current.truncatedTo(ChronoUnit.HOURS).plusHours(1);
            ZonedDateTime stepEnd = nextHour.isBefore(stop) ? nextHour : stop;
            long minutesInStep = Duration.between(current, stepEnd).toMinutes();

            DayOfWeek dow = current.getDayOfWeek();
            int hour = current.getHour();

            // 1. Peak: Friday 16:00 to Sunday 22:00
            boolean isPeak = false;
            if (dow == DayOfWeek.FRIDAY && hour >= 16) {
                isPeak = true;
            } else if (dow == DayOfWeek.SATURDAY) {
                isPeak = true;
            } else if (dow == DayOfWeek.SUNDAY && hour < 22) {
                isPeak = true;
            }

            if (isPeak) {
                peakMinutes += minutesInStep;
            } else if (hour >= 22 || hour < 7) {
                // 2. Off-Peak: Daily 22:00 to 07:00
                offPeakMinutes += minutesInStep;
            } else {
                // 3. Standard: Remainder (Mon-Thu 07:00-22:00, Fri 07:00-16:00)
                standardMinutes += minutesInStep;
            }

            current = stepEnd;
        }

        BigDecimal peak = BigDecimal.valueOf(peakMinutes).divide(BigDecimal.valueOf(60), 4, RoundingMode.HALF_UP);
        BigDecimal standard = BigDecimal.valueOf(standardMinutes).divide(BigDecimal.valueOf(60), 4, RoundingMode.HALF_UP);
        BigDecimal offPeak = BigDecimal.valueOf(offPeakMinutes).divide(BigDecimal.valueOf(60), 4, RoundingMode.HALF_UP);

        return new DemandBreakdown(peak, standard, offPeak);
    }
}
