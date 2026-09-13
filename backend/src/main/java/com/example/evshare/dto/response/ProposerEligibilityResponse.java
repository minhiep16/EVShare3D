package com.example.evshare.dto.response;

import java.math.BigDecimal;

public class ProposerEligibilityResponse {

    private Long groupId;
    private Long userId;
    private boolean isGroupMember;
    private boolean isActiveMember;
    private BigDecimal equityPercentage;
    private BigDecimal requiredPercentage;
    private boolean eligible;
    private String reason;

    public ProposerEligibilityResponse() {}

    public ProposerEligibilityResponse(Long groupId, Long userId, boolean isGroupMember, boolean isActiveMember,
                                       BigDecimal equityPercentage, BigDecimal requiredPercentage,
                                       boolean eligible, String reason) {
        this.groupId = groupId;
        this.userId = userId;
        this.isGroupMember = isGroupMember;
        this.isActiveMember = isActiveMember;
        this.equityPercentage = equityPercentage;
        this.requiredPercentage = requiredPercentage;
        this.eligible = eligible;
        this.reason = reason;
    }

    public Long getGroupId() {
        return groupId;
    }

    public void setGroupId(Long groupId) {
        this.groupId = groupId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public boolean isGroupMember() {
        return isGroupMember;
    }

    public void setGroupMember(boolean groupMember) {
        isGroupMember = groupMember;
    }

    public boolean isActiveMember() {
        return isActiveMember;
    }

    public void setActiveMember(boolean activeMember) {
        isActiveMember = activeMember;
    }

    public BigDecimal getEquityPercentage() {
        return equityPercentage;
    }

    public void setEquityPercentage(BigDecimal equityPercentage) {
        this.equityPercentage = equityPercentage;
    }

    public BigDecimal getRequiredPercentage() {
        return requiredPercentage;
    }

    public void setRequiredPercentage(BigDecimal requiredPercentage) {
        this.requiredPercentage = requiredPercentage;
    }

    public boolean isEligible() {
        return eligible;
    }

    public void setEligible(boolean eligible) {
        this.eligible = eligible;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}
