package com.example.evshare.service.payment.model;

import com.example.evshare.entity.enums.PaymentProviderType;
import com.example.evshare.entity.enums.PaymentStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collections;
import java.util.Map;

public class PaymentRefundResult {

    private String transactionReference;
    private String refundReference;
    private String externalRefundId;
    private PaymentProviderType providerType;
    private PaymentStatus status;
    private BigDecimal amountRefunded;
    private boolean successful;
    private String providerMessage;
    private Instant refundedAt;
    private Map<String, Object> rawResponse;

    public PaymentRefundResult() {
        this.status = PaymentStatus.REFUNDED;
        this.rawResponse = Collections.emptyMap();
    }

    public PaymentRefundResult(String transactionReference, String refundReference,
                               String externalRefundId, PaymentProviderType providerType,
                               PaymentStatus status, BigDecimal amountRefunded,
                               boolean successful, String providerMessage,
                               Instant refundedAt, Map<String, Object> rawResponse) {
        this.transactionReference = transactionReference;
        this.refundReference = refundReference;
        this.externalRefundId = externalRefundId;
        this.providerType = providerType;
        this.status = status != null ? status : PaymentStatus.REFUNDED;
        this.amountRefunded = amountRefunded;
        this.successful = successful;
        this.providerMessage = providerMessage;
        this.refundedAt = refundedAt != null ? refundedAt : Instant.now();
        this.rawResponse = rawResponse != null ? rawResponse : Collections.emptyMap();
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String transactionReference;
        private String refundReference;
        private String externalRefundId;
        private PaymentProviderType providerType;
        private PaymentStatus status = PaymentStatus.REFUNDED;
        private BigDecimal amountRefunded;
        private boolean successful;
        private String providerMessage;
        private Instant refundedAt = Instant.now();
        private Map<String, Object> rawResponse = Collections.emptyMap();

        public Builder transactionReference(String transactionReference) {
            this.transactionReference = transactionReference;
            return this;
        }

        public Builder refundReference(String refundReference) {
            this.refundReference = refundReference;
            return this;
        }

        public Builder externalRefundId(String externalRefundId) {
            this.externalRefundId = externalRefundId;
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

        public Builder amountRefunded(BigDecimal amountRefunded) {
            this.amountRefunded = amountRefunded;
            return this;
        }

        public Builder successful(boolean successful) {
            this.successful = successful;
            return this;
        }

        public Builder providerMessage(String providerMessage) {
            this.providerMessage = providerMessage;
            return this;
        }

        public Builder refundedAt(Instant refundedAt) {
            this.refundedAt = refundedAt;
            return this;
        }

        public Builder rawResponse(Map<String, Object> rawResponse) {
            this.rawResponse = rawResponse;
            return this;
        }

        public PaymentRefundResult build() {
            return new PaymentRefundResult(transactionReference, refundReference, externalRefundId,
                    providerType, status, amountRefunded, successful, providerMessage, refundedAt, rawResponse);
        }
    }

    public String getTransactionReference() {
        return transactionReference;
    }

    public void setTransactionReference(String transactionReference) {
        this.transactionReference = transactionReference;
    }

    public String getRefundReference() {
        return refundReference;
    }

    public void setRefundReference(String refundReference) {
        this.refundReference = refundReference;
    }

    public String getExternalRefundId() {
        return externalRefundId;
    }

    public void setExternalRefundId(String externalRefundId) {
        this.externalRefundId = externalRefundId;
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

    public BigDecimal getAmountRefunded() {
        return amountRefunded;
    }

    public void setAmountRefunded(BigDecimal amountRefunded) {
        this.amountRefunded = amountRefunded;
    }

    public boolean isSuccessful() {
        return successful;
    }

    public void setSuccessful(boolean successful) {
        this.successful = successful;
    }

    public String getProviderMessage() {
        return providerMessage;
    }

    public void setProviderMessage(String providerMessage) {
        this.providerMessage = providerMessage;
    }

    public Instant getRefundedAt() {
        return refundedAt;
    }

    public void setRefundedAt(Instant refundedAt) {
        this.refundedAt = refundedAt;
    }

    public Map<String, Object> getRawResponse() {
        return rawResponse;
    }

    public void setRawResponse(Map<String, Object> rawResponse) {
        this.rawResponse = rawResponse;
    }
}
