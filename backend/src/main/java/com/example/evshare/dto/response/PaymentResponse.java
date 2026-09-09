package com.example.evshare.dto.response;

import com.example.evshare.entity.Payment;
import com.example.evshare.entity.enums.PaymentMethod;
import com.example.evshare.entity.enums.PaymentStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;

@Schema(description = "Payment details and provider instructions")
public class PaymentResponse {

    @Schema(description = "Payment entity ID", example = "101")
    private Long id;

    @Schema(description = "Transaction reference", example = "TX-PAY-20260909-ABCD1234")
    private String transactionReference;

    @Schema(description = "Target shared fund ID", example = "5")
    private Long fundId;

    @Schema(description = "Payer user ID", example = "10")
    private Long userId;

    @Schema(description = "Payment amount in VND", example = "2500000.00")
    private BigDecimal amount;

    @Schema(description = "Payment method", example = "BANK_TRANSFER")
    private PaymentMethod paymentMethod;

    @Schema(description = "Current lifecycle status", example = "PENDING")
    private PaymentStatus status;

    @Schema(description = "Checkout or redirect URL", example = "https://sandbox.evshare.io/mock-pay?ref=...")
    private String checkoutUrl;

    @Schema(description = "QR payload string", example = "vietqr://pay?acc=...")
    private String qrPayload;

    @Schema(description = "Payment instruction text for payer")
    private String instructions;

    @Schema(description = "Provider metadata or simulation flags")
    private Map<String, Object> metadata;

    @Schema(description = "Payment creation timestamp", example = "2026-09-09T10:00:00Z")
    private Instant createdAt;

    public PaymentResponse() {
    }

    public PaymentResponse(Long id, String transactionReference, Long fundId, Long userId, BigDecimal amount,
                           PaymentMethod paymentMethod, PaymentStatus status, String checkoutUrl, String qrPayload,
                           String instructions, Map<String, Object> metadata, Instant createdAt) {
        this.id = id;
        this.transactionReference = transactionReference;
        this.fundId = fundId;
        this.userId = userId;
        this.amount = amount;
        this.paymentMethod = paymentMethod;
        this.status = status;
        this.checkoutUrl = checkoutUrl;
        this.qrPayload = qrPayload;
        this.instructions = instructions;
        this.metadata = metadata;
        this.createdAt = createdAt;
    }

    public static PaymentResponse fromEntity(Payment payment) {
        if (payment == null) {
            return null;
        }
        return new PaymentResponse(
                payment.getId(),
                payment.getTransactionReference(),
                payment.getFund() != null ? payment.getFund().getId() : null,
                payment.getUser() != null ? payment.getUser().getId() : null,
                payment.getAmount(),
                payment.getPaymentMethod(),
                payment.getStatus(),
                null,
                null,
                null,
                null,
                payment.getCreatedAt()
        );
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTransactionReference() {
        return transactionReference;
    }

    public void setTransactionReference(String transactionReference) {
        this.transactionReference = transactionReference;
    }

    public Long getFundId() {
        return fundId;
    }

    public void setFundId(Long fundId) {
        this.fundId = fundId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
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

    public PaymentStatus getStatus() {
        return status;
    }

    public void setStatus(PaymentStatus status) {
        this.status = status;
    }

    public String getCheckoutUrl() {
        return checkoutUrl;
    }

    public void setCheckoutUrl(String checkoutUrl) {
        this.checkoutUrl = checkoutUrl;
    }

    public String getQrPayload() {
        return qrPayload;
    }

    public void setQrPayload(String qrPayload) {
        this.qrPayload = qrPayload;
    }

    public String getInstructions() {
        return instructions;
    }

    public void setInstructions(String instructions) {
        this.instructions = instructions;
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }

    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
