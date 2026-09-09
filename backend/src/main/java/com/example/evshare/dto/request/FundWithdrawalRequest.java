package com.example.evshare.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public class FundWithdrawalRequest {

    @NotNull(message = "Withdrawal amount is required")
    @DecimalMin(value = "1.00", message = "Withdrawal amount must be at least 1.00")
    private BigDecimal amount;

    @NotBlank(message = "Withdrawal description/purpose is required")
    @Size(max = 255, message = "Description cannot exceed 255 characters")
    private String description;

    private Boolean allowOverdraft = false;

    @Size(max = 64, message = "Transaction reference cannot exceed 64 characters")
    private String transactionReference;

    private com.example.evshare.entity.enums.FundTransactionSource source;

    public FundWithdrawalRequest() {
    }

    public FundWithdrawalRequest(BigDecimal amount, String description, Boolean allowOverdraft) {
        this.amount = amount;
        this.description = description;
        this.allowOverdraft = allowOverdraft != null ? allowOverdraft : false;
    }

    public FundWithdrawalRequest(BigDecimal amount, String description, Boolean allowOverdraft,
                                 String transactionReference, com.example.evshare.entity.enums.FundTransactionSource source) {
        this.amount = amount;
        this.description = description;
        this.allowOverdraft = allowOverdraft != null ? allowOverdraft : false;
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

    public Boolean getAllowOverdraft() {
        return allowOverdraft;
    }

    public void setAllowOverdraft(Boolean allowOverdraft) {
        this.allowOverdraft = allowOverdraft;
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
