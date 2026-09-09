package com.example.evshare.service.allocation;

import com.example.evshare.entity.enums.AllocationStrategy;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Encapsulates the complete result of an expense allocation computation across syndicate members.
 * Guarantees mathematical reconciliation: sum of allocated amounts equals the original expense total.
 */
public class AllocationResult {

    private BigDecimal totalAmount;
    private String currency;
    private AllocationStrategy strategy;
    private List<AllocatedMemberShare> shares = new ArrayList<>();
    private boolean reconciled;
    private BigDecimal totalAllocated;
    private BigDecimal roundingAdjustment;
    private String roundingStrategyDescription;
    private String summary;

    public AllocationResult() {
    }

    public AllocationResult(BigDecimal totalAmount, String currency, AllocationStrategy strategy,
                            List<AllocatedMemberShare> shares, boolean reconciled,
                            BigDecimal totalAllocated, BigDecimal roundingAdjustment,
                            String roundingStrategyDescription, String summary) {
        this.totalAmount = totalAmount;
        this.currency = currency;
        this.strategy = strategy;
        this.shares = shares != null ? shares : new ArrayList<>();
        this.reconciled = reconciled;
        this.totalAllocated = totalAllocated;
        this.roundingAdjustment = roundingAdjustment != null ? roundingAdjustment : BigDecimal.ZERO;
        this.roundingStrategyDescription = roundingStrategyDescription;
        this.summary = summary;
    }

    public Optional<AllocatedMemberShare> getShareForUser(Long userId) {
        if (userId == null || shares == null) {
            return Optional.empty();
        }
        return shares.stream()
                .filter(s -> userId.equals(s.getUserId()))
                .findFirst();
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public AllocationStrategy getStrategy() {
        return strategy;
    }

    public void setStrategy(AllocationStrategy strategy) {
        this.strategy = strategy;
    }

    public List<AllocatedMemberShare> getShares() {
        return shares;
    }

    public void setShares(List<AllocatedMemberShare> shares) {
        this.shares = shares;
    }

    public boolean isReconciled() {
        return reconciled;
    }

    public void setReconciled(boolean reconciled) {
        this.reconciled = reconciled;
    }

    public BigDecimal getTotalAllocated() {
        return totalAllocated;
    }

    public void setTotalAllocated(BigDecimal totalAllocated) {
        this.totalAllocated = totalAllocated;
    }

    public BigDecimal getRoundingAdjustment() {
        return roundingAdjustment;
    }

    public void setRoundingAdjustment(BigDecimal roundingAdjustment) {
        this.roundingAdjustment = roundingAdjustment;
    }

    public String getRoundingStrategyDescription() {
        return roundingStrategyDescription;
    }

    public void setRoundingStrategyDescription(String roundingStrategyDescription) {
        this.roundingStrategyDescription = roundingStrategyDescription;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    @Override
    public String toString() {
        return "AllocationResult{" +
                "totalAmount=" + totalAmount +
                ", currency='" + currency + '\'' +
                ", strategy=" + strategy +
                ", reconciled=" + reconciled +
                ", totalAllocated=" + totalAllocated +
                ", roundingAdjustment=" + roundingAdjustment +
                ", sharesCount=" + (shares != null ? shares.size() : 0) +
                '}';
    }
}
