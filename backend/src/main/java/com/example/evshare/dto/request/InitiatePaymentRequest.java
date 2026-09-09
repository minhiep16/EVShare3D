package com.example.evshare.dto.request;

import com.example.evshare.entity.enums.PaymentMethod;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.Map;

@Schema(description = "Request to initiate an idempotent payment operation")
public class InitiatePaymentRequest {

    @Schema(description = "Payer user ID", example = "10")
    @NotNull(message = "userId is required")
    private Long userId;

    @Schema(description = "Target shared fund vault ID", example = "5")
    @NotNull(message = "fundId is required")
    private Long fundId;

    @Schema(description = "Optional expense allocation ID if settling an expense share", example = "42")
    private Long expenseAllocationId;

    @Schema(description = "Payment amount in VND", example = "2500000.00")
    @NotNull(message = "amount is required")
    @DecimalMin(value = "1000.00", message = "Minimum payment amount is 1,000.00 VND")
    private BigDecimal amount;

    @Schema(description = "Payment method channel", example = "BANK_TRANSFER")
    @NotNull(message = "paymentMethod is required")
    private PaymentMethod paymentMethod;

    @Schema(description = "Transaction narrative or description", example = "Syndicate Monthly Capital Contribution")
    private String description;

    @Schema(description = "Unique client-generated idempotency key (e.g. UUIDv4)", example = "9b1deb4d-3b7d-4bad-9bdd-2b0d7b3dcb6d")
    private String idempotencyKey;

    @Schema(description = "Channel-specific metadata or simulation options")
    private Map<String, Object> metadata;

    public InitiatePaymentRequest() {
    }

    public InitiatePaymentRequest(Long userId, Long fundId, Long expenseAllocationId, BigDecimal amount,
                                  PaymentMethod paymentMethod, String description, String idempotencyKey,
                                  Map<String, Object> metadata) {
        this.userId = userId;
        this.fundId = fundId;
        this.expenseAllocationId = expenseAllocationId;
        this.amount = amount;
        this.paymentMethod = paymentMethod;
        this.description = description;
        this.idempotencyKey = idempotencyKey;
        this.metadata = metadata;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Long getFundId() {
        return fundId;
    }

    public void setFundId(Long fundId) {
        this.fundId = fundId;
    }

    public Long getExpenseAllocationId() {
        return expenseAllocationId;
    }

    public void setExpenseAllocationId(Long expenseAllocationId) {
        this.expenseAllocationId = expenseAllocationId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public PaymentMethod getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(PaymentMethod paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public void setIdempotencyKey(String idempotencyKey) {
        this.idempotencyKey = idempotencyKey;
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }

    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata;
    }
}
