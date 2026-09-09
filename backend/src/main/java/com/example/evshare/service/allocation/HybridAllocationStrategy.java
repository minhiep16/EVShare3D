package com.example.evshare.service.allocation;

import com.example.evshare.entity.Expense;
import com.example.evshare.entity.OwnershipGroup;
import com.example.evshare.entity.OwnershipShare;
import com.example.evshare.entity.UsageSession;
import com.example.evshare.entity.enums.AllocationStrategy;
import com.example.evshare.exception.BusinessException;
import com.example.evshare.repository.OwnershipShareRepository;
import com.example.evshare.repository.UsageSessionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

/**
 * Strategy implementing the dual-factor HYBRID cost allocation model specified in BUSINESS_RULES.md (BR-FIN-02):
 *
 * 1. WEIGHTING SPECIFICATION (BR-FIN-02):
 *    - Ownership Component (Fixed base): Exactly 30.00% (weight: 0.30).
 *      Reflects syndicate capital holding, vehicle depreciation, regulatory road tax, and baseline insurance.
 *    - Usage Component (Variable remainder): Exactly 70.00% (weight: 0.70).
 *      Reflects consumable wear-and-tear, tire wear, brake degradation, and charging energy bills.
 *    - No invented percentages: Ratios 30/70 are hard-coded directly from specification.
 *
 * 2. OWNERSHIP COMPONENT (30%):
 *    - Formula: FixedPart = round(TotalExpense * 0.30, 2, HALF_EVEN)
 *    - Allocated strictly pro-rata to active ownership equity percentages:
 *      FixedMemberShare_i = round(FixedPart * (percentage_i / 100.00), 2, HALF_EVEN)
 *    - Enforces 100.00% syndicate active equity invariant.
 *
 * 3. USAGE COMPONENT (70%):
 *    - Formula: VariablePart = TotalExpense - FixedPart (guarantees FixedPart + VariablePart == TotalExpense)
 *    - Allocated pro-rata according to odometer distance logged (or hours used) from completed UsageSession records:
 *      VariableMemberShare_i = round(VariablePart * (DistanceLogged_i / sum(DistanceLogged)), 2, HALF_EVEN)
 *    - Zero-Usage Fallback: If zero utilization (0.0 km and 0.0 hrs) is recorded during the billing window,
 *      the variable component safely degrades to active ownership equity percentages.
 *
 * 4. ROUNDING STRATEGY:
 *    - Documented Banker's Rounding (RoundingMode.HALF_EVEN, scale 2) applied at all component and member levels.
 *
 * 5. RECONCILIATION INVARIANT:
 *    - CombinedMemberShare_i = FixedMemberShare_i + VariableMemberShare_i
 *    - Residual Difference: Delta = TotalExpense - sum(CombinedMemberShare_i)
 *    - Deterministic Residual Penny Absorption: Delta is absorbed by the co-owner with the highest total combined share
 *      (tie-breaker: lowest userId). No pseudo-random or arbitrary selection.
 *    - Strict Invariant: sum(AllocatedAmount_i) == TotalExpense (exact down to 0.01 currency unit).
 */
@Component
public class HybridAllocationStrategy implements CostAllocationStrategy {

    private static final Logger log = LoggerFactory.getLogger(HybridAllocationStrategy.class);
    public static final BigDecimal FIXED_WEIGHT = new BigDecimal("0.30");
    public static final BigDecimal VARIABLE_WEIGHT = new BigDecimal("0.70");
    private static final int CURRENCY_SCALE = 2;

    public static final String ROUNDING_STRATEGY_DOC =
            "Hybrid model: exactly 30% fixed ownership + 70% variable usage. Each component rounded using Banker's " +
            "Rounding (HALF_EVEN) to 2 decimals. Any combined residual penny (|delta| > 0) is deterministically " +
            "absorbed by the member with the largest total combined liability (tie-breaker: lowest userId).";

    private final OwnershipBasedAllocationStrategy ownershipBasedAllocationStrategy;
    private final UsageBasedAllocationStrategy usageBasedAllocationStrategy;
    private final OwnershipShareRepository ownershipShareRepository;
    private final UsageSessionRepository usageSessionRepository;

    @org.springframework.beans.factory.annotation.Autowired
    public HybridAllocationStrategy(OwnershipBasedAllocationStrategy ownershipBasedAllocationStrategy,
                                    UsageBasedAllocationStrategy usageBasedAllocationStrategy,
                                    OwnershipShareRepository ownershipShareRepository,
                                    UsageSessionRepository usageSessionRepository) {
        this.ownershipBasedAllocationStrategy = ownershipBasedAllocationStrategy;
        this.usageBasedAllocationStrategy = usageBasedAllocationStrategy;
        this.ownershipShareRepository = ownershipShareRepository;
        this.usageSessionRepository = usageSessionRepository;
    }

    public HybridAllocationStrategy(OwnershipBasedAllocationStrategy ownershipBasedAllocationStrategy,
                                    UsageBasedAllocationStrategy usageBasedAllocationStrategy) {
        this(ownershipBasedAllocationStrategy, usageBasedAllocationStrategy, null, null);
    }

    @Override
    public AllocationStrategy getStrategyMode() {
        return AllocationStrategy.HYBRID;
    }

    @Override
    public AllocationResult allocate(Expense expense) {
        validateExpenseInputs(expense);

        OwnershipGroup group = expense.getGroup();
        List<OwnershipShare> activeShares = ownershipShareRepository.findByGroupIdAndIsActiveTrue(group.getId());
        if (activeShares.isEmpty()) {
            throw new BusinessException("No active ownership shares found for syndicate group", HttpStatus.BAD_REQUEST);
        }

        List<UsageSession> sessions = Collections.emptyList();
        if (expense.getVehicle() != null && expense.getVehicle().getId() != null) {
            sessions = usageSessionRepository.findByBookingVehicleIdOrderByCheckInTimeDesc(expense.getVehicle().getId());
        } else if (group.getVehicle() != null && group.getVehicle().getId() != null) {
            sessions = usageSessionRepository.findByBookingVehicleIdOrderByCheckInTimeDesc(group.getVehicle().getId());
        }

        return allocateForSharesAndSessions(expense, activeShares, sessions, UsageMetric.DISTANCE);
    }

    /**
     * Isolated, unit-testable execution method allowing arbitrary active shares, usage sessions, and metrics.
     */
    public AllocationResult allocateForSharesAndSessions(Expense expense, List<OwnershipShare> activeShares,
                                                        List<UsageSession> sessions, UsageMetric metric) {
        validateExpenseInputs(expense);

        if (activeShares == null || activeShares.isEmpty()) {
            throw new BusinessException("Active ownership shares list cannot be empty for hybrid allocation", HttpStatus.BAD_REQUEST);
        }

        BigDecimal total = expense.getTotalAmount().setScale(CURRENCY_SCALE, RoundingMode.HALF_EVEN);
        String currency = expense.getCurrency() != null ? expense.getCurrency() : "VND";

        // 1. Split total into 30% fixed ownership and 70% variable usage components
        BigDecimal fixedAmount = total.multiply(FIXED_WEIGHT).setScale(CURRENCY_SCALE, RoundingMode.HALF_EVEN);
        BigDecimal variableAmount = total.subtract(fixedAmount).setScale(CURRENCY_SCALE, RoundingMode.HALF_EVEN);

        // 2. Delegate Fixed 30% allocation to OwnershipBasedAllocationStrategy
        Expense fixedExpense = createSubExpense(expense, fixedAmount, AllocationStrategy.OWNERSHIP_BASED);
        AllocationResult fixedResult = ownershipBasedAllocationStrategy.allocateForShares(fixedExpense, activeShares);

        // 3. Delegate Variable 70% allocation to UsageBasedAllocationStrategy (with zero-usage fallback)
        Expense variableExpense = createSubExpense(expense, variableAmount, AllocationStrategy.USAGE_BASED);
        AllocationResult variableResult = usageBasedAllocationStrategy.allocateForSessionsAndShares(
                variableExpense, sessions, activeShares, metric != null ? metric : UsageMetric.DISTANCE);

        // 4. Merge component liabilities per syndicate member
        Map<Long, AllocatedMemberShare> memberMap = new LinkedHashMap<>();
        Map<Long, BigDecimal> fixedAmounts = new HashMap<>();
        Map<Long, BigDecimal> variableAmounts = new HashMap<>();

        for (AllocatedMemberShare share : fixedResult.getShares()) {
            fixedAmounts.put(share.getUserId(), share.getAllocatedAmount());
            memberMap.put(share.getUserId(), share);
        }

        for (AllocatedMemberShare share : variableResult.getShares()) {
            variableAmounts.put(share.getUserId(), share.getAllocatedAmount());
            memberMap.putIfAbsent(share.getUserId(), share);
        }

        List<AllocatedMemberShare> combinedShares = new ArrayList<>();
        BigDecimal sumAllocated = BigDecimal.ZERO;

        for (Map.Entry<Long, AllocatedMemberShare> entry : memberMap.entrySet()) {
            Long userId = entry.getKey();
            AllocatedMemberShare templateShare = entry.getValue();

            BigDecimal userFixed = fixedAmounts.getOrDefault(userId, BigDecimal.ZERO);
            BigDecimal userVariable = variableAmounts.getOrDefault(userId, BigDecimal.ZERO);
            BigDecimal combinedAmount = userFixed.add(userVariable).setScale(CURRENCY_SCALE, RoundingMode.HALF_EVEN);
            sumAllocated = sumAllocated.add(combinedAmount);

            BigDecimal effectivePct = combinedAmount.divide(total, 6, RoundingMode.HALF_EVEN)
                    .multiply(new BigDecimal("100.00")).setScale(CURRENCY_SCALE, RoundingMode.HALF_EVEN);

            String explanation = String.format(
                    "Hybrid allocation: Fixed (30%% equity: %s %s) + Variable (70%% usage: %s %s) = %s %s (%s%%)",
                    userFixed.toPlainString(), currency,
                    userVariable.toPlainString(), currency,
                    combinedAmount.toPlainString(), currency,
                    effectivePct.toPlainString()
            );

            AllocatedMemberShare combinedShare = new AllocatedMemberShare(
                    userId,
                    templateShare.getUser(),
                    combinedAmount,
                    effectivePct,
                    combinedAmount,
                    BigDecimal.ZERO.setScale(CURRENCY_SCALE, RoundingMode.HALF_EVEN),
                    explanation
            );
            combinedShares.add(combinedShare);
        }

        // 5. Sort deterministically: highest combined amount DESC, userId ASC
        combinedShares.sort(Comparator
                .comparing(AllocatedMemberShare::getAllocatedAmount, Comparator.reverseOrder())
                .thenComparing(s -> s.getUserId() != null ? s.getUserId() : 0L)
        );

        // 6. Deterministic residual penny absorption
        BigDecimal residual = total.subtract(sumAllocated).setScale(CURRENCY_SCALE, RoundingMode.HALF_EVEN);
        if (residual.compareTo(BigDecimal.ZERO) != 0 && !combinedShares.isEmpty()) {
            AllocatedMemberShare primary = combinedShares.get(0);
            primary.setAllocatedAmount(primary.getAllocatedAmount().add(residual));
            primary.setRoundingDelta(residual);
            String sign = residual.compareTo(BigDecimal.ZERO) > 0 ? "+" : "";
            primary.setExplanation(primary.getExplanation() + String.format(
                    " (adjusted by %s%s %s to absorb rounding residual for exact reconciliation)",
                    sign, residual.toPlainString(), currency
            ));
        }

        // 7. Verify strict mathematical equality invariant
        BigDecimal finalSum = combinedShares.stream()
                .map(AllocatedMemberShare::getAllocatedAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(CURRENCY_SCALE, RoundingMode.HALF_EVEN);

        if (finalSum.compareTo(total) != 0) {
            throw new IllegalStateException(String.format(
                    "Hybrid mathematical reconciliation failure: total %s %s != allocated sum %s %s",
                    total, currency, finalSum, currency
            ));
        }

        String summary = String.format(
                "Hybrid allocation (30%% fixed: %s %s, 70%% variable: %s %s) across %d active co-owners (reconciled: 100.00%%)",
                fixedAmount.toPlainString(), currency, variableAmount.toPlainString(), currency, combinedShares.size()
        );

        return new AllocationResult(
                total,
                currency,
                AllocationStrategy.HYBRID,
                combinedShares,
                true,
                finalSum,
                residual,
                ROUNDING_STRATEGY_DOC,
                summary
        );
    }

    private void validateExpenseInputs(Expense expense) {
        if (expense == null) {
            throw new IllegalArgumentException("Expense cannot be null for hybrid allocation");
        }
        if (expense.getGroup() == null || expense.getGroup().getId() == null) {
            throw new BusinessException("Expense must be associated with a valid syndicate ownership group", HttpStatus.BAD_REQUEST);
        }
        BigDecimal totalAmount = expense.getTotalAmount();
        if (totalAmount == null || totalAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("Expense total amount must be positive for hybrid allocation", HttpStatus.BAD_REQUEST);
        }
    }

    private Expense createSubExpense(Expense parent, BigDecimal amount, AllocationStrategy strategy) {
        Expense sub = new Expense();
        sub.setId(parent.getId());
        sub.setGroup(parent.getGroup());
        sub.setVehicle(parent.getVehicle());
        sub.setTitle(parent.getTitle());
        sub.setCategory(parent.getCategory());
        sub.setTotalAmount(amount);
        sub.setCurrency(parent.getCurrency());
        sub.setAllocationStrategy(strategy);
        sub.setLoggedByUser(parent.getLoggedByUser());
        sub.setIncurredDate(parent.getIncurredDate());
        return sub;
    }
}
