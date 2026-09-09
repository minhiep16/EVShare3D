package com.example.evshare.service.payment.model;

import com.example.evshare.entity.enums.PaymentMethod;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.Map;

public class PaymentInitiationCommand {

    private String transactionReference;
    private BigDecimal amount;
    private String currency;
    private PaymentMethod paymentMethod;
    private String description;
    private Long payerId;
    private String payerName;
    private String payerEmail;
    private String payerPhone;
    private String returnUrl;
    private String cancelUrl;
    private Map<String, Object> metadata;

    public PaymentInitiationCommand() {
        this.currency = "VND";
        this.metadata = Collections.emptyMap();
    }

    public PaymentInitiationCommand(String transactionReference, BigDecimal amount, String currency,
                                    PaymentMethod paymentMethod, String description, Long payerId,
                                    String payerName, String payerEmail, String payerPhone,
                                    String returnUrl, String cancelUrl, Map<String, Object> metadata) {
        this.transactionReference = transactionReference;
        this.amount = amount;
        this.currency = currency != null ? currency : "VND";
        this.paymentMethod = paymentMethod;
        this.description = description;
        this.payerId = payerId;
        this.payerName = payerName;
        this.payerEmail = payerEmail;
        this.payerPhone = payerPhone;
        this.returnUrl = returnUrl;
        this.cancelUrl = cancelUrl;
        this.metadata = metadata != null ? metadata : Collections.emptyMap();
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String transactionReference;
        private BigDecimal amount;
        private String currency = "VND";
        private PaymentMethod paymentMethod;
        private String description;
        private Long payerId;
        private String payerName;
        private String payerEmail;
        private String payerPhone;
        private String returnUrl;
        private String cancelUrl;
        private Map<String, Object> metadata = Collections.emptyMap();

        public Builder transactionReference(String transactionReference) {
            this.transactionReference = transactionReference;
            return this;
        }

        public Builder amount(BigDecimal amount) {
            this.amount = amount;
            return this;
        }

        public Builder currency(String currency) {
            this.currency = currency;
            return this;
        }

        public Builder paymentMethod(PaymentMethod paymentMethod) {
            this.paymentMethod = paymentMethod;
            return this;
        }

        public Builder description(String description) {
            this.description = description;
            return this;
        }

        public Builder payerId(Long payerId) {
            this.payerId = payerId;
            return this;
        }

        public Builder payerName(String payerName) {
            this.payerName = payerName;
            return this;
        }

        public Builder payerEmail(String payerEmail) {
            this.payerEmail = payerEmail;
            return this;
        }

        public Builder payerPhone(String payerPhone) {
            this.payerPhone = payerPhone;
            return this;
        }

        public Builder returnUrl(String returnUrl) {
            this.returnUrl = returnUrl;
            return this;
        }

        public Builder cancelUrl(String cancelUrl) {
            this.cancelUrl = cancelUrl;
            return this;
        }

        public Builder metadata(Map<String, Object> metadata) {
            this.metadata = metadata;
            return this;
        }

        public PaymentInitiationCommand build() {
            return new PaymentInitiationCommand(transactionReference, amount, currency, paymentMethod,
                    description, payerId, payerName, payerEmail, payerPhone, returnUrl, cancelUrl, metadata);
        }
    }

    public String getTransactionReference() {
        return transactionReference;
    }

    public void setTransactionReference(String transactionReference) {
        this.transactionReference = transactionReference;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
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

    public Long getPayerId() {
        return payerId;
    }

    public void setPayerId(Long payerId) {
        this.payerId = payerId;
    }

    public String getPayerName() {
        return payerName;
    }

    public void setPayerName(String payerName) {
        this.payerName = payerName;
    }

    public String getPayerEmail() {
        return payerEmail;
    }

    public void setPayerEmail(String payerEmail) {
        this.payerEmail = payerEmail;
    }

    public String getPayerPhone() {
        return payerPhone;
    }

    public void setPayerPhone(String payerPhone) {
        this.payerPhone = payerPhone;
    }

    public String getReturnUrl() {
        return returnUrl;
    }

    public void setReturnUrl(String returnUrl) {
        this.returnUrl = returnUrl;
    }

    public String getCancelUrl() {
        return cancelUrl;
    }

    public void setCancelUrl(String cancelUrl) {
        this.cancelUrl = cancelUrl;
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }

    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata;
    }
}
