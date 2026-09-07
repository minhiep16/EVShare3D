package com.example.evshare.dto.request;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public class IssueOwnershipShareRequest {

    @NotNull(message = "User ID is required")
    private Long userId;

    @NotNull(message = "Percentage is required")
    @DecimalMin(value = "0.01", message = "Percentage must be strictly greater than 0.00%")
    @DecimalMax(value = "100.00", message = "Percentage cannot exceed 100.00%")
    @Digits(integer = 3, fraction = 2, message = "Percentage can have at most 2 decimal places")
    private BigDecimal percentage;

    public IssueOwnershipShareRequest() {
    }

    public IssueOwnershipShareRequest(Long userId, BigDecimal percentage) {
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
