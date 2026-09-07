package com.example.evshare.dto.request;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public class TransferShareRequest {

    @NotNull(message = "Seller user ID (fromUserId) is required")
    private Long fromUserId;

    @NotNull(message = "Buyer user ID (toUserId) is required")
    private Long toUserId;

    @NotNull(message = "Transfer percentage is required")
    @DecimalMin(value = "0.01", message = "Transfer percentage must be strictly greater than 0.00%")
    @DecimalMax(value = "100.00", message = "Transfer percentage cannot exceed 100.00%")
    @Digits(integer = 3, fraction = 2, message = "Transfer percentage can have at most 2 decimal places")
    private BigDecimal percentage;

    public TransferShareRequest() {
    }

    public TransferShareRequest(Long fromUserId, Long toUserId, BigDecimal percentage) {
        this.fromUserId = fromUserId;
        this.toUserId = toUserId;
        this.percentage = percentage;
    }

    public Long getFromUserId() {
        return fromUserId;
    }

    public void setFromUserId(Long fromUserId) {
        this.fromUserId = fromUserId;
    }

    public Long getToUserId() {
        return toUserId;
    }

    public void setToUserId(Long toUserId) {
        this.toUserId = toUserId;
    }

    public BigDecimal getPercentage() {
        return percentage;
    }

    public void setPercentage(BigDecimal percentage) {
        this.percentage = percentage;
    }
}
