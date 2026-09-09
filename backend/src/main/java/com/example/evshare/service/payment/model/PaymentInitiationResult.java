package com.example.evshare.service.payment.model;

import com.example.evshare.entity.enums.PaymentProviderType;
import com.example.evshare.entity.enums.PaymentStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collections;
import java.util.Map;

public class PaymentInitiationResult {

    private String transactionReference;
    private String externalTransactionId;
    private PaymentProviderType providerType;
    private PaymentStatus status;
    private BigDecimal amount;
    private String paymentUrl;
    private String qrCodeData;
    private String instructions;
    private Instant expiresAt;
    private Map<String, Object> providerData;

    public PaymentInitiationResult() {
        this.status = PaymentStatus.PENDING;
        this.providerData = Collections.emptyMap();
    }

    public PaymentInitiationResult(String transactionReference, String externalTransactionId,
                                   PaymentProviderType providerType, PaymentStatus status,
                                   BigDecimal amount, String paymentUrl, String qrCodeData,
                                   String instructions, Instant expiresAt,
                                   Map<String, Object> providerData) {
        this.transactionReference = transactionReference;
        this.externalTransactionId = externalTransactionId;
        this.providerType = providerType;
        this.status = status != null ? status : PaymentStatus.PENDING;
        this.amount = amount;
        this.paymentUrl = paymentUrl;
        this.qrCodeData = qrCodeData;
        this.instructions = instructions;
        this.expiresAt = expiresAt;
        this.providerData = providerData != null ? providerData : Collections.emptyMap();
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String transactionReference;
        private String externalTransactionId;
        private PaymentProviderType providerType;
        private PaymentStatus status = PaymentStatus.PENDING;
        private BigDecimal amount;
        private String paymentUrl;
        private String qrCodeData;
        private String instructions;
        private Instant expiresAt;
        private Map<String, Object> providerData = Collections.emptyMap();

        public Builder transactionReference(String transactionReference) {
            this.transactionReference = transactionReference;
            return this;
        }

        public Builder externalTransactionId(String externalTransactionId) {
            this.externalTransactionId = externalTransactionId;
            return this;
        }

        public Builder providerType(PaymentProviderType providerType) {
            this.providerType = providerType;
            return this;
        }

        public Builder status(PaymentStatus status) {
            this.status = status;
            return this;
        }

        public Builder amount(BigDecimal amount) {
            this.amount = amount;
            return this;
        }

        public Builder paymentUrl(String paymentUrl) {
            this.paymentUrl = paymentUrl;
            return this;
        }

        public Builder qrCodeData(String qrCodeData) {
            this.qrCodeData = qrCodeData;
            return this;
        }

        public Builder instructions(String instructions) {
            this.instructions = instructions;
            return this;
        }

        public Builder expiresAt(Instant expiresAt) {
            this.expiresAt = expiresAt;
            return this;
        }

        public Builder providerData(Map<String, Object> providerData) {
            this.providerData = providerData;
            return this;
        }

        public PaymentInitiationResult build() {
            return new PaymentInitiationResult(transactionReference, externalTransactionId, providerType,
                    status, amount, paymentUrl, qrCodeData, instructions, expiresAt, providerData);
        }
    }

    public String getTransactionReference() {
        return transactionReference;
    }

    public void setTransactionReference(String transactionReference) {
        this.transactionReference = transactionReference;
    }

    public String getExternalTransactionId() {
        return externalTransactionId;
    }

    public void setExternalTransactionId(String externalTransactionId) {
        this.externalTransactionId = externalTransactionId;
    }

    public PaymentProviderType getProviderType() {
        return providerType;
    }

    public void setProviderType(PaymentProviderType providerType) {
        this.providerType = providerType;
    }

    public PaymentStatus getStatus() {
        return status;
    }

    public void setStatus(PaymentStatus status) {
        this.status = status;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getPaymentUrl() {
        return paymentUrl;
    }

    public void setPaymentUrl(String paymentUrl) {
        this.paymentUrl = paymentUrl;
    }

    public String getQrCodeData() {
        return qrCodeData;
    }

    public void setQrCodeData(String qrCodeData) {
        this.qrCodeData = qrCodeData;
    }

    public String getInstructions() {
        return instructions;
    }

    public void setInstructions(String instructions) {
        this.instructions = instructions;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(Instant expiresAt) {
        this.expiresAt = expiresAt;
    }

    public Map<String, Object> getProviderData() {
        return providerData;
    }

    public void setProviderData(Map<String, Object> providerData) {
        this.providerData = providerData;
    }
}
