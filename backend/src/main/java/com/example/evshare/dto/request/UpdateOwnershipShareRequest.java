package com.example.evshare.dto.request;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;

import java.math.BigDecimal;

public class UpdateOwnershipShareRequest {

    @DecimalMin(value = "0.01", message = "Percentage must be strictly greater than 0.00%")
    @DecimalMax(value = "100.00", message = "Percentage cannot exceed 100.00%")
    @Digits(integer = 3, fraction = 2, message = "Percentage can have at most 2 decimal places")
    private BigDecimal percentage;

    private Boolean isActive;

    public UpdateOwnershipShareRequest() {
    }

    public UpdateOwnershipShareRequest(BigDecimal percentage, Boolean isActive) {
        this.percentage = percentage;
        this.isActive = isActive;
    }

    public BigDecimal getPercentage() {
        return percentage;
    }

    public void setPercentage(BigDecimal percentage) {
        this.percentage = percentage;
    }

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }
}
