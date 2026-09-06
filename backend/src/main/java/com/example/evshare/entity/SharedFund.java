package com.example.evshare.entity;

import jakarta.persistence.*;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "shared_funds")
@EntityListeners(AuditingEntityListener.class)
public class SharedFund {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_id", nullable = false, unique = true)
    private OwnershipGroup group;

    @Column(name = "current_balance", nullable = false, precision = 15, scale = 2)
    private BigDecimal currentBalance = BigDecimal.ZERO;

    @Column(name = "minimum_reserve_threshold", nullable = false, precision = 15, scale = 2)
    private BigDecimal minimumReserveThreshold = new BigDecimal("10000000.00");

    @Column(name = "currency", nullable = false, length = 10)
    private String currency = "VND";

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    public SharedFund() {
    }

    public SharedFund(Long id, OwnershipGroup group, BigDecimal currentBalance, BigDecimal minimumReserveThreshold, String currency, Instant updatedAt) {
        this.id = id;
        this.group = group;
        this.currentBalance = currentBalance != null ? currentBalance : BigDecimal.ZERO;
        this.minimumReserveThreshold = minimumReserveThreshold != null ? minimumReserveThreshold : new BigDecimal("10000000.00");
        this.currency = currency != null ? currency : "VND";
        this.updatedAt = updatedAt != null ? updatedAt : Instant.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public OwnershipGroup getGroup() {
        return group;
    }

    public void setGroup(OwnershipGroup group) {
        this.group = group;
    }

    public BigDecimal getCurrentBalance() {
        return currentBalance;
    }

    public void setCurrentBalance(BigDecimal currentBalance) {
        this.currentBalance = currentBalance;
    }

    public BigDecimal getMinimumReserveThreshold() {
        return minimumReserveThreshold;
    }

    public void setMinimumReserveThreshold(BigDecimal minimumReserveThreshold) {
        this.minimumReserveThreshold = minimumReserveThreshold;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
