package com.example.evshare.dto.response;

import com.example.evshare.entity.FundTransaction;
import com.example.evshare.entity.enums.FundTransactionSource;
import com.example.evshare.entity.enums.TransactionEntryType;
import com.example.evshare.entity.enums.TransactionType;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.math.BigDecimal;
import java.time.Instant;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class FundTransactionResponse {

    private Long id;
    private Long fundId;
    private Long userId;
    private String userName;
    private TransactionType transactionType;
    private TransactionEntryType entryType;
    private BigDecimal amount;
    private BigDecimal balanceAfter;
    private String transactionReference;
    private String description;
    private FundTransactionSource source;
    private Instant createdAt;

    public FundTransactionResponse() {
    }

    public FundTransactionResponse(Long id, Long fundId, Long userId, String userName,
                                   TransactionType transactionType, TransactionEntryType entryType,
                                   BigDecimal amount, BigDecimal balanceAfter,
                                   String transactionReference, String description,
                                   FundTransactionSource source, Instant createdAt) {
        this.id = id;
        this.fundId = fundId;
        this.userId = userId;
        this.userName = userName;
        this.transactionType = transactionType;
        this.entryType = entryType;
        this.amount = amount;
        this.balanceAfter = balanceAfter;
        this.transactionReference = transactionReference;
        this.description = description;
        this.source = source;
        this.createdAt = createdAt;
    }

    public static FundTransactionResponse fromEntity(FundTransaction tx) {
        if (tx == null) {
            return null;
        }
        return new FundTransactionResponse(
                tx.getId(),
                tx.getFund() != null ? tx.getFund().getId() : null,
                tx.getUser() != null ? tx.getUser().getId() : null,
                tx.getUser() != null ? tx.getUser().getFullName() : "SYSTEM",
                tx.getTransactionType(),
                tx.getEntryType(),
                tx.getAmount(),
                tx.getBalanceAfter(),
                tx.getTransactionReference(),
                tx.getDescription(),
                tx.getSource(),
                tx.getCreatedAt()
        );
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public TransactionType getTransactionType() {
        return transactionType;
    }

    public void setTransactionType(TransactionType transactionType) {
        this.transactionType = transactionType;
    }

    public TransactionEntryType getEntryType() {
        return entryType;
    }

    public void setEntryType(TransactionEntryType entryType) {
        this.entryType = entryType;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public BigDecimal getBalanceAfter() {
        return balanceAfter;
    }

    public void setBalanceAfter(BigDecimal balanceAfter) {
        this.balanceAfter = balanceAfter;
    }

    public String getTransactionReference() {
        return transactionReference;
    }

    public void setTransactionReference(String transactionReference) {
        this.transactionReference = transactionReference;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public FundTransactionSource getSource() {
        return source;
    }

    public void setSource(FundTransactionSource source) {
        this.source = source;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
