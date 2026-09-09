package com.example.evshare.entity;

import com.example.evshare.entity.enums.TransactionType;
import jakarta.persistence.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "fund_transactions")
@EntityListeners(AuditingEntityListener.class)
public class FundTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fund_id", nullable = false, updatable = false)
    private SharedFund fund;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", updatable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.VARCHAR)
    @Column(name = "transaction_type", nullable = false, length = 30, updatable = false)
    private TransactionType transactionType;

    @Enumerated(EnumType.STRING)
    @org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.VARCHAR)
    @Column(name = "entry_type", nullable = false, length = 10, updatable = false)
    private com.example.evshare.entity.enums.TransactionEntryType entryType = com.example.evshare.entity.enums.TransactionEntryType.CREDIT;

    @Column(name = "amount", nullable = false, precision = 15, scale = 2, updatable = false)
    private BigDecimal amount;

    @Column(name = "balance_after", nullable = false, precision = 15, scale = 2, updatable = false)
    private BigDecimal balanceAfter;

    @Column(name = "transaction_reference", nullable = false, length = 64, unique = true, updatable = false)
    private String transactionReference;

    @Column(name = "description", nullable = false, length = 255, updatable = false)
    private String description;

    @Enumerated(EnumType.STRING)
    @org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.VARCHAR)
    @Column(name = "source", nullable = false, length = 50, updatable = false)
    private com.example.evshare.entity.enums.FundTransactionSource source = com.example.evshare.entity.enums.FundTransactionSource.MEMBER_CONTRIBUTION;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    public FundTransaction() {
    }

    public FundTransaction(Long id, SharedFund fund, User user, TransactionType transactionType, BigDecimal amount, BigDecimal balanceAfter, String description, Instant createdAt) {
        this.id = id;
        this.fund = fund;
        this.user = user;
        this.transactionType = transactionType;
        this.amount = amount;
        this.balanceAfter = balanceAfter;
        this.description = description;
        this.createdAt = createdAt != null ? createdAt : Instant.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public SharedFund getFund() {
        return fund;
    }

    public void setFund(SharedFund fund) {
        this.fund = fund;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public TransactionType getTransactionType() {
        return transactionType;
    }

    public void setTransactionType(TransactionType transactionType) {
        this.transactionType = transactionType;
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

    public FundTransaction(Long id, SharedFund fund, User user, TransactionType transactionType,
                           com.example.evshare.entity.enums.TransactionEntryType entryType, BigDecimal amount,
                           BigDecimal balanceAfter, String transactionReference, String description,
                           com.example.evshare.entity.enums.FundTransactionSource source, Instant createdAt) {
        this.id = id;
        this.fund = fund;
        this.user = user;
        this.transactionType = transactionType;
        this.entryType = entryType != null ? entryType : (transactionType == TransactionType.WITHDRAWAL || transactionType == TransactionType.EXPENSE_PAYOUT ? com.example.evshare.entity.enums.TransactionEntryType.DEBIT : com.example.evshare.entity.enums.TransactionEntryType.CREDIT);
        this.amount = amount;
        this.balanceAfter = balanceAfter;
        this.transactionReference = transactionReference;
        this.description = description;
        this.source = source != null ? source : com.example.evshare.entity.enums.FundTransactionSource.MEMBER_CONTRIBUTION;
        this.createdAt = createdAt != null ? createdAt : Instant.now();
    }

    public com.example.evshare.entity.enums.TransactionEntryType getEntryType() {
        return entryType;
    }

    public void setEntryType(com.example.evshare.entity.enums.TransactionEntryType entryType) {
        this.entryType = entryType;
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

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public boolean isCredit() {
        return com.example.evshare.entity.enums.TransactionEntryType.CREDIT.equals(this.entryType);
    }

    public boolean isDebit() {
        return com.example.evshare.entity.enums.TransactionEntryType.DEBIT.equals(this.entryType);
    }

    @PreUpdate
    public void onPreUpdate() {
        throw new IllegalStateException("FundTransaction records are strictly immutable and cannot be updated");
    }

    @PreRemove
    public void onPreRemove() {
        throw new IllegalStateException("FundTransaction records are strictly immutable and cannot be deleted");
    }
}
