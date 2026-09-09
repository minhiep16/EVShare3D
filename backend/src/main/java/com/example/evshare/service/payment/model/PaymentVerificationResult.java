package com.example.evshare.service.payment.model;

import com.example.evshare.entity.enums.PaymentProviderType;
import com.example.evshare.entity.enums.PaymentStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collections;
import java.util.Map;

public class PaymentVerificationResult {

    private String transactionReference;
    private String externalTransactionId;
    private PaymentProviderType providerType;
    private PaymentStatus status;
    private BigDecimal amount;
    private boolean verified;
    private String providerMessage;
    private Instant completedAt;
    private Map<String, Object> rawResponse;

    public PaymentVerificationResult() {
        this.rawResponse = Collections.emptyMap();
    }

    public PaymentVerificationResult(String transactionReference, String externalTransactionId,
                                     PaymentProviderType providerType, PaymentStatus status,
                                     BigDecimal amount, boolean verified, String providerMessage,
                                     Instant completedAt, Map<String, Object> rawResponse) {
        this.transactionReference = transactionReference;
        this.externalTransactionId = externalTransactionId;
        this.providerType = providerType;
        this.status = status;
        this.amount = amount;
        this.verified = verified;
        this.providerMessage = providerMessage;
        this.completedAt = completedAt;
        this.rawResponse = rawResponse != null ? rawResponse : Collections.emptyMap();
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String transactionReference;
        private String externalTransactionId;
        private PaymentProviderType providerType;
        private PaymentStatus status;
        private BigDecimal amount;
        private boolean verified;
        private String providerMessage;
        private Instant completedAt;
        private Map<String, Object> rawResponse = Collections.emptyMap();

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

        public Builder verified(boolean verified) {
            this.verified = verified;
            return this;
        }

        public Builder providerMessage(String providerMessage) {
            this.providerMessage = providerMessage;
            return this;
        }

        public Builder completedAt(Instant completedAt) {
            this.completedAt = completedAt;
            return this;
        }

        public Builder rawResponse(Map<String, Object> rawResponse) {
            this.rawResponse = rawResponse;
            return this;
        }

        public PaymentVerificationResult build() {
            return new PaymentVerificationResult(transactionReference, externalTransactionId, providerType,
                    status, amount, verified, providerMessage, completedAt, rawResponse);
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

    public boolean isVerified() {
        return verified;
    }

    public void setVerified(boolean verified) {
        this.verified = verified;
    }

    public String getProviderMessage() {
        return providerMessage;
    }

    public void setProviderMessage(String providerMessage) {
        this.providerMessage = providerMessage;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(Instant completedAt) {
        this.completedAt = completedAt;
    }

    public Map<String, Object> getRawResponse() {
        return rawResponse;
    }

    public void setRawResponse(Map<String, Object> rawResponse) {
        this.rawResponse = rawResponse;
    }
}
