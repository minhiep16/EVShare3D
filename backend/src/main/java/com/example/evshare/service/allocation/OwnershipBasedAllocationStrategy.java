package com.example.evshare.service.allocation;

import com.example.evshare.entity.Expense;
import com.example.evshare.entity.OwnershipGroup;
import com.example.evshare.entity.OwnershipShare;
import com.example.evshare.entity.User;
import com.example.evshare.entity.enums.AllocationStrategy;
import com.example.evshare.exception.BusinessException;
import com.example.evshare.exception.InvalidOwnershipDistributionException;
import com.example.evshare.repository.OwnershipShareRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/**
 * Strategy allocating syndicate expenses strictly based on active ownership percentage.
 *
 * Requirements satisfied:
 * - Active ownership only: Inactive shares (isActive == false) are excluded.
 * - Ownership total must equal 100.00%: Validated prior to allocation; throws InvalidOwnershipDistributionException otherwise.
 * - Deterministic rounding: Banker's Rounding (RoundingMode.HALF_EVEN) to 2 decimal places.
 * - Exact mathematical reconciliation: Residual difference (total - sum(rounded)) is deterministically
 *   absorbed by the co-owner with the highest equity percentage (tie-broken by lowest userId).
 * - Explainable & unit-testable: Produces itemized explanations with no hidden randomness.
 */
@Component
public class OwnershipBasedAllocationStrategy implements CostAllocationStrategy {

    private static final Logger log = LoggerFactory.getLogger(OwnershipBasedAllocationStrategy.class);
    private static final BigDecimal HUNDRED = new BigDecimal("100.00");
    private static final int COMPUTATION_SCALE = 6;
    private static final int CURRENCY_SCALE = 2;

    public static final String ROUNDING_STRATEGY_DOC =
            "Banker's Rounding (HALF_EVEN) to 2 decimal places. Residual pennies (|delta| > 0) are " +
            "deterministically absorbed by the active co-owner with the highest equity share (tie-breaker: lowest userId).";

    private final OwnershipShareRepository ownershipShareRepository;

    public OwnershipBasedAllocationStrategy(OwnershipShareRepository ownershipShareRepository) {
        this.ownershipShareRepository = ownershipShareRepository;
    }

    @Override
    public AllocationStrategy getStrategyMode() {
        return AllocationStrategy.OWNERSHIP_BASED;
    }

    @Override
    public AllocationResult allocate(Expense expense) {
        if (expense == null) {
            throw new IllegalArgumentException("Expense cannot be null for allocation calculation");
        }
        OwnershipGroup group = expense.getGroup();
        if (group == null || group.getId() == null) {
            throw new BusinessException("Expense must be associated with a valid ownership group", HttpStatus.BAD_REQUEST);
        }

        BigDecimal totalAmount = expense.getTotalAmount();
        if (totalAmount == null || totalAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("Expense total amount must be positive for allocation", HttpStatus.BAD_REQUEST);
        }

        // 1. Fetch active ownership shares only
        List<OwnershipShare> activeShares = ownershipShareRepository.findByGroupIdAndIsActiveTrue(group.getId());
        return allocateForShares(expense, activeShares);
    }

    /**
     * Package-private / public testable method to allocate cost given an expense and pre-loaded active shares list.
     * Facilitates unit testing with diverse ownership distributions without mocking repository internals.
     */
    public AllocationResult allocateForShares(Expense expense, List<OwnershipShare> activeShares) {
        if (activeShares == null || activeShares.isEmpty()) {
            throw new BusinessException("No active ownership shares found for group", HttpStatus.BAD_REQUEST);
        }

        OwnershipGroup group = expense.getGroup();
        Long groupId = group != null ? group.getId() : null;

        // 2. Validate that active ownership shares sum to exactly 100.00%
        BigDecimal totalPercentage = activeShares.stream()
                .filter(s -> Boolean.TRUE.equals(s.getIsActive()))
                .map(OwnershipShare::getPercentage)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(CURRENCY_SCALE, RoundingMode.HALF_EVEN);

        if (totalPercentage.compareTo(HUNDRED) != 0) {
            log.error("Ownership syndicate {} equity invariant violated: sum of active shares is {}% (must be 100.00%)",
                    groupId, totalPercentage);
            throw new InvalidOwnershipDistributionException(groupId, totalPercentage);
        }

        // 3. Filter active shares only and sort deterministically:
        //    Primary: percentage DESC (largest equity holder absorbs residual)
        //    Secondary: userId ASC (deterministic tie-breaker)
        List<OwnershipShare> sortedShares = activeShares.stream()
                .filter(s -> Boolean.TRUE.equals(s.getIsActive()))
                .sorted(Comparator
                        .comparing(OwnershipShare::getPercentage, Comparator.reverseOrder())
                        .thenComparing(s -> s.getUser() != null && s.getUser().getId() != null ? s.getUser().getId() : 0L)
                )
                .toList();

        BigDecimal totalAmount = expense.getTotalAmount().setScale(CURRENCY_SCALE, RoundingMode.HALF_EVEN);
        String currency = expense.getCurrency() != null ? expense.getCurrency() : "VND";

        List<AllocatedMemberShare> memberShares = new ArrayList<>();
        BigDecimal sumRounded = BigDecimal.ZERO;

        // 4. Compute pro-rata share with Banker's Rounding (HALF_EVEN)
        for (OwnershipShare share : sortedShares) {
            User user = share.getUser();
            Long userId = user != null ? user.getId() : null;
            BigDecimal percentage = share.getPercentage();

            // rawAmount = totalAmount * percentage / 100.00
            BigDecimal rawAmount = totalAmount
                    .multiply(percentage)
                    .divide(HUNDRED, COMPUTATION_SCALE, RoundingMode.HALF_EVEN);

            // roundedAmount = round(rawAmount, 2, HALF_EVEN)
            BigDecimal roundedAmount = rawAmount.setScale(CURRENCY_SCALE, RoundingMode.HALF_EVEN);
            sumRounded = sumRounded.add(roundedAmount);

            String explanation = String.format(
                    "Ownership-based allocation: %s%% equity of %s %s = %s %s",
                    percentage.toPlainString(),
                    totalAmount.toPlainString(),
                    currency,
                    roundedAmount.toPlainString(),
                    currency
            );

            AllocatedMemberShare memberShare = new AllocatedMemberShare(
                    userId,
                    user,
                    roundedAmount,
                    percentage,
                    rawAmount,
                    BigDecimal.ZERO.setScale(CURRENCY_SCALE, RoundingMode.HALF_EVEN),
                    explanation
            );
            memberShares.add(memberShare);
        }

        // 5. Reconcile residual penny (totalAmount - sum(rounded))
        BigDecimal residual = totalAmount.subtract(sumRounded).setScale(CURRENCY_SCALE, RoundingMode.HALF_EVEN);

        if (residual.compareTo(BigDecimal.ZERO) != 0 && !memberShares.isEmpty()) {
            // Deterministically absorb into the first member (highest equity, tie-break lowest userId)
            AllocatedMemberShare primaryHolder = memberShares.get(0);
            BigDecimal adjustedAmount = primaryHolder.getAllocatedAmount().add(residual);
            primaryHolder.setAllocatedAmount(adjustedAmount);
            primaryHolder.setRoundingDelta(residual);

            String adjustmentSign = residual.compareTo(BigDecimal.ZERO) > 0 ? "+" : "";
            String updatedExplanation = primaryHolder.getExplanation() + String.format(
                    " (adjusted by %s%s %s to absorb rounding residual for exact reconciliation)",
                    adjustmentSign,
                    residual.toPlainString(),
                    currency
            );
            primaryHolder.setExplanation(updatedExplanation);

            log.debug("Residual penny {} {} absorbed by primary equity holder user {}",
                    residual, currency, primaryHolder.getUserId());
        }

        // 6. Verify exact mathematical reconciliation
        BigDecimal finalAllocatedSum = memberShares.stream()
                .map(AllocatedMemberShare::getAllocatedAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(CURRENCY_SCALE, RoundingMode.HALF_EVEN);

        if (finalAllocatedSum.compareTo(totalAmount) != 0) {
            throw new IllegalStateException(String.format(
                    "Mathematical reconciliation assertion failure: Total expense is %s %s, but allocated sum is %s %s",
                    totalAmount, currency, finalAllocatedSum, currency
            ));
        }

        String summary = String.format(
                "Ownership-based allocation of %s %s across %d active co-owners (reconciled: 100.00%%)",
                totalAmount.toPlainString(), currency, memberShares.size()
        );

        return new AllocationResult(
                totalAmount,
                currency,
                AllocationStrategy.OWNERSHIP_BASED,
                memberShares,
                true,
                finalAllocatedSum,
                residual,
                ROUNDING_STRATEGY_DOC,
                summary
        );
    }
}
