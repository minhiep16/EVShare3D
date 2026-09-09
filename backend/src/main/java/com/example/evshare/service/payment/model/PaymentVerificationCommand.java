package com.example.evshare.service.payment.model;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.Map;

public class PaymentVerificationCommand {

    private String transactionReference;
    private String externalTransactionId;
    private BigDecimal expectedAmount;
    private Map<String, Object> payload;

    public PaymentVerificationCommand() {
        this.payload = Collections.emptyMap();
    }

    public PaymentVerificationCommand(String transactionReference, String externalTransactionId,
                                      BigDecimal expectedAmount, Map<String, Object> payload) {
        this.transactionReference = transactionReference;
        this.externalTransactionId = externalTransactionId;
        this.expectedAmount = expectedAmount;
        this.payload = payload != null ? payload : Collections.emptyMap();
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String transactionReference;
        private String externalTransactionId;
        private BigDecimal expectedAmount;
        private Map<String, Object> payload = Collections.emptyMap();

        public Builder transactionReference(String transactionReference) {
            this.transactionReference = transactionReference;
            return this;
        }

        public Builder externalTransactionId(String externalTransactionId) {
            this.externalTransactionId = externalTransactionId;
            return this;
        }

        public Builder expectedAmount(BigDecimal expectedAmount) {
            this.expectedAmount = expectedAmount;
            return this;
        }

        public Builder payload(Map<String, Object> payload) {
            this.payload = payload;
            return this;
        }

        public PaymentVerificationCommand build() {
            return new PaymentVerificationCommand(transactionReference, externalTransactionId, expectedAmount, payload);
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

    public BigDecimal getExpectedAmount() {
        return expectedAmount;
    }

    public void setExpectedAmount(BigDecimal expectedAmount) {
        this.expectedAmount = expectedAmount;
    }

    public Map<String, Object> getPayload() {
        return payload;
    }

    public void setPayload(Map<String, Object> payload) {
        this.payload = payload;
    }
}
