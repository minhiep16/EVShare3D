package com.example.evshare.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public class FundContributionRequest {

    @NotNull(message = "Contribution amount is required")
    @DecimalMin(value = "1.00", message = "Contribution amount must be at least 1.00")
    private BigDecimal amount;

    @Size(max = 255, message = "Description cannot exceed 255 characters")
    private String description;

    @Size(max = 64, message = "Transaction reference cannot exceed 64 characters")
    private String transactionReference;

    private com.example.evshare.entity.enums.FundTransactionSource source;

    public FundContributionRequest() {
    }

    public FundContributionRequest(BigDecimal amount, String description) {
        this.amount = amount;
        this.description = description;
    }

    public FundContributionRequest(BigDecimal amount, String description, String transactionReference,
                                   com.example.evshare.entity.enums.FundTransactionSource source) {
        this.amount = amount;
        this.description = description;
        this.transactionReference = transactionReference;
        this.source = source;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getTransactionReference() {
        return transactionReference;
    }

    public void setTransactionReference(String transactionReference) {
        this.transactionReference = transactionReference;
    }

    public com.example.evshare.entity.enums.FundTransactionSource getSource() {
        return source;
    }

    public void setSource(com.example.evshare.entity.enums.FundTransactionSource source) {
        this.source = source;
    }
}
