package com.example.evshare.dto.request;

import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public class AddGroupMemberRequest {

    @NotNull(message = "User ID is required")
    private Long userId;

    private BigDecimal percentage;

    public AddGroupMemberRequest() {
    }

    public AddGroupMemberRequest(Long userId) {
        this.userId = userId;
    }

    public AddGroupMemberRequest(Long userId, BigDecimal percentage) {
        this.userId = userId;
        this.percentage = percentage;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public BigDecimal getPercentage() {
        return percentage;
    }

    public void setPercentage(BigDecimal percentage) {
        this.percentage = percentage;
    }
}
