package com.example.evshare.service.payment.model;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.Map;

public class PaymentRefundCommand {

    private String transactionReference;
    private String externalTransactionId;
    private BigDecimal amount;
    private String reason;
    private Map<String, Object> metadata;

    public PaymentRefundCommand() {
        this.metadata = Collections.emptyMap();
    }

    public PaymentRefundCommand(String transactionReference, String externalTransactionId,
                                BigDecimal amount, String reason, Map<String, Object> metadata) {
        this.transactionReference = transactionReference;
        this.externalTransactionId = externalTransactionId;
        this.amount = amount;
        this.reason = reason;
        this.metadata = metadata != null ? metadata : Collections.emptyMap();
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String transactionReference;
        private String externalTransactionId;
        private BigDecimal amount;
        private String reason;
        private Map<String, Object> metadata = Collections.emptyMap();

        public Builder transactionReference(String transactionReference) {
            this.transactionReference = transactionReference;
            return this;
        }

        public Builder externalTransactionId(String externalTransactionId) {
            this.externalTransactionId = externalTransactionId;
            return this;
        }

        public Builder amount(BigDecimal amount) {
            this.amount = amount;
            return this;
        }

        public Builder reason(String reason) {
            this.reason = reason;
            return this;
        }

        public Builder metadata(Map<String, Object> metadata) {
            this.metadata = metadata;
            return this;
        }

        public PaymentRefundCommand build() {
            return new PaymentRefundCommand(transactionReference, externalTransactionId, amount, reason, metadata);
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

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }

    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata;
    }
}
