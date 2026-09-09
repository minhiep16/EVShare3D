package com.example.evshare.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.math.BigDecimal;
import java.time.Instant;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class FundReconciliationResponse {

    private Long fundId;
    private Long groupId;
    private BigDecimal currentBalance;
    private BigDecimal calculatedLedgerBalance;
    private BigDecimal totalCredits;
    private BigDecimal totalDebits;
    private int creditCount;
    private int debitCount;
    private int transactionCount;
    private Boolean isReconciled;
    private BigDecimal reconciliationDelta;
    private Instant reconciledAt;
    private String summary;

    public FundReconciliationResponse() {
    }

    public FundReconciliationResponse(Long fundId, Long groupId, BigDecimal currentBalance,
                                      BigDecimal calculatedLedgerBalance, BigDecimal totalCredits,
                                      BigDecimal totalDebits, int creditCount, int debitCount,
                                      int transactionCount, Boolean isReconciled,
                                      BigDecimal reconciliationDelta, Instant reconciledAt,
                                      String summary) {
        this.fundId = fundId;
        this.groupId = groupId;
        this.currentBalance = currentBalance;
        this.calculatedLedgerBalance = calculatedLedgerBalance;
        this.totalCredits = totalCredits;
        this.totalDebits = totalDebits;
        this.creditCount = creditCount;
        this.debitCount = debitCount;
        this.transactionCount = transactionCount;
        this.isReconciled = isReconciled;
        this.reconciliationDelta = reconciliationDelta;
        this.reconciledAt = reconciledAt;
        this.summary = summary;
    }

    public Long getFundId() {
        return fundId;
    }

    public void setFundId(Long fundId) {
        this.fundId = fundId;
    }

    public Long getGroupId() {
        return groupId;
    }

    public void setGroupId(Long groupId) {
        this.groupId = groupId;
    }

    public BigDecimal getCurrentBalance() {
        return currentBalance;
    }

    public void setCurrentBalance(BigDecimal currentBalance) {
        this.currentBalance = currentBalance;
    }

    public BigDecimal getCalculatedLedgerBalance() {
        return calculatedLedgerBalance;
    }

    public void setCalculatedLedgerBalance(BigDecimal calculatedLedgerBalance) {
        this.calculatedLedgerBalance = calculatedLedgerBalance;
    }

    public BigDecimal getTotalCredits() {
        return totalCredits;
    }

    public void setTotalCredits(BigDecimal totalCredits) {
        this.totalCredits = totalCredits;
    }

    public BigDecimal getTotalDebits() {
        return totalDebits;
    }

    public void setTotalDebits(BigDecimal totalDebits) {
        this.totalDebits = totalDebits;
    }

    public int getCreditCount() {
        return creditCount;
    }

    public void setCreditCount(int creditCount) {
        this.creditCount = creditCount;
    }

    public int getDebitCount() {
        return debitCount;
    }

    public void setDebitCount(int debitCount) {
        this.debitCount = debitCount;
    }

    public int getTransactionCount() {
        return transactionCount;
    }

    public void setTransactionCount(int transactionCount) {
        this.transactionCount = transactionCount;
    }

    public Boolean getIsReconciled() {
        return isReconciled;
    }

    public void setIsReconciled(Boolean isReconciled) {
        this.isReconciled = isReconciled;
    }

    public BigDecimal getReconciliationDelta() {
        return reconciliationDelta;
    }

    public void setReconciliationDelta(BigDecimal reconciliationDelta) {
        this.reconciliationDelta = reconciliationDelta;
    }

    public Instant getReconciledAt() {
        return reconciledAt;
    }

    public void setReconciledAt(Instant reconciledAt) {
        this.reconciledAt = reconciledAt;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }
}
