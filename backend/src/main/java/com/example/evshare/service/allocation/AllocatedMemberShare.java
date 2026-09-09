package com.example.evshare.service.allocation;

import com.example.evshare.entity.User;

import java.math.BigDecimal;

/**
 * Encapsulates the calculated cost liability for an individual syndicate co-owner,
 * including raw pro-rata amounts, rounding adjustments, and audit explanations.
 */
public class AllocatedMemberShare {

    private Long userId;
    private User user;
    private BigDecimal allocatedAmount;
    private BigDecimal effectivePercentage;
    private BigDecimal rawAmount;
    private BigDecimal roundingDelta;
    private String explanation;

    public AllocatedMemberShare() {
    }

    public AllocatedMemberShare(Long userId, User user, BigDecimal allocatedAmount,
                                BigDecimal effectivePercentage, BigDecimal rawAmount,
                                BigDecimal roundingDelta, String explanation) {
        this.userId = userId;
        this.user = user;
        this.allocatedAmount = allocatedAmount;
        this.effectivePercentage = effectivePercentage;
        this.rawAmount = rawAmount;
        this.roundingDelta = roundingDelta != null ? roundingDelta : BigDecimal.ZERO;
        this.explanation = explanation;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public BigDecimal getAllocatedAmount() {
        return allocatedAmount;
    }

    public void setAllocatedAmount(BigDecimal allocatedAmount) {
        this.allocatedAmount = allocatedAmount;
    }

    public BigDecimal getEffectivePercentage() {
        return effectivePercentage;
    }

    public void setEffectivePercentage(BigDecimal effectivePercentage) {
        this.effectivePercentage = effectivePercentage;
    }

    public BigDecimal getRawAmount() {
        return rawAmount;
    }

    public void setRawAmount(BigDecimal rawAmount) {
        this.rawAmount = rawAmount;
    }

    public BigDecimal getRoundingDelta() {
        return roundingDelta;
    }

    public void setRoundingDelta(BigDecimal roundingDelta) {
        this.roundingDelta = roundingDelta;
    }

    public String getExplanation() {
        return explanation;
    }

    public void setExplanation(String explanation) {
        this.explanation = explanation;
    }

    @Override
    public String toString() {
        return "AllocatedMemberShare{" +
                "userId=" + userId +
                ", allocatedAmount=" + allocatedAmount +
                ", effectivePercentage=" + effectivePercentage +
                ", roundingDelta=" + roundingDelta +
                ", explanation='" + explanation + '\'' +
                '}';
    }
}
