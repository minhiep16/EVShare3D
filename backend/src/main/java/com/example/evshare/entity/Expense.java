package com.example.evshare.entity;

import com.example.evshare.entity.enums.AllocationStrategy;
import com.example.evshare.entity.enums.ExpenseCategory;
import jakarta.persistence.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "expenses")
@EntityListeners(AuditingEntityListener.class)
public class Expense {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_id", nullable = false)
    private OwnershipGroup group;

    @Column(name = "title", nullable = false, length = 150)
    private String title;

    @Enumerated(EnumType.STRING)
    @org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.VARCHAR)
    @Column(name = "category", nullable = false, length = 40)
    private ExpenseCategory category;

    @Column(name = "total_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal totalAmount;

    @Enumerated(EnumType.STRING)
    @org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.VARCHAR)
    @Column(name = "allocation_strategy", nullable = false, length = 30)
    private AllocationStrategy allocationStrategy = AllocationStrategy.OWNERSHIP_BASED;

    @Column(name = "invoice_reference", length = 100)
    private String invoiceReference;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "logged_by_user_id", nullable = false)
    private User loggedByUser;

    @Column(name = "incurred_date", nullable = false)
    private LocalDate incurredDate;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    public Expense() {
    }

    public Expense(Long id, OwnershipGroup group, String title, ExpenseCategory category, BigDecimal totalAmount, AllocationStrategy allocationStrategy, String invoiceReference, User loggedByUser, LocalDate incurredDate, Instant createdAt) {
        this.id = id;
        this.group = group;
        this.title = title;
        this.category = category;
        this.totalAmount = totalAmount;
        this.allocationStrategy = allocationStrategy != null ? allocationStrategy : AllocationStrategy.OWNERSHIP_BASED;
        this.invoiceReference = invoiceReference;
        this.loggedByUser = loggedByUser;
        this.incurredDate = incurredDate;
        this.createdAt = createdAt != null ? createdAt : Instant.now();
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

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public ExpenseCategory getCategory() {
        return category;
    }

    public void setCategory(ExpenseCategory category) {
        this.category = category;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }

    public AllocationStrategy getAllocationStrategy() {
        return allocationStrategy;
    }

    public void setAllocationStrategy(AllocationStrategy allocationStrategy) {
        this.allocationStrategy = allocationStrategy;
    }

    public String getInvoiceReference() {
        return invoiceReference;
    }

    public void setInvoiceReference(String invoiceReference) {
        this.invoiceReference = invoiceReference;
    }

    public User getLoggedByUser() {
        return loggedByUser;
    }

    public void setLoggedByUser(User loggedByUser) {
        this.loggedByUser = loggedByUser;
    }

    public LocalDate getIncurredDate() {
        return incurredDate;
    }

    public void setIncurredDate(LocalDate incurredDate) {
        this.incurredDate = incurredDate;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
