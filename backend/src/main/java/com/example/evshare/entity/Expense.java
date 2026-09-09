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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vehicle_id")
    private Vehicle vehicle;

    @Column(name = "title", nullable = false, length = 150)
    private String title;

    @Enumerated(EnumType.STRING)
    @org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.VARCHAR)
    @Column(name = "category", nullable = false, length = 40)
    private ExpenseCategory category;

    @Column(name = "total_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal totalAmount;

    @Column(name = "currency", nullable = false, length = 10)
    private String currency = "VND";

    @Enumerated(EnumType.STRING)
    @org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.VARCHAR)
    @Column(name = "allocation_strategy", nullable = false, length = 30)
    private AllocationStrategy allocationStrategy = AllocationStrategy.OWNERSHIP_BASED;

    @Column(name = "invoice_reference", length = 100)
    private String invoiceReference;

    @Column(name = "evidence_url", length = 255)
    private String evidenceUrl;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "logged_by_user_id", nullable = false)
    private User loggedByUser;

    @Column(name = "incurred_date", nullable = false)
    private LocalDate incurredDate;

    @OneToMany(mappedBy = "expense", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private java.util.List<ExpenseAllocation> allocations = new java.util.ArrayList<>();

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    public Expense() {
    }

    public Expense(Long id, OwnershipGroup group, Vehicle vehicle, String title, ExpenseCategory category,
                   BigDecimal totalAmount, String currency, AllocationStrategy allocationStrategy,
                   String invoiceReference, String evidenceUrl, User loggedByUser, LocalDate incurredDate, Instant createdAt) {
        this.id = id;
        this.group = group;
        this.vehicle = vehicle;
        this.title = title;
        this.category = category;
        this.totalAmount = totalAmount;
        this.currency = currency != null ? currency : "VND";
        this.allocationStrategy = allocationStrategy != null ? allocationStrategy : AllocationStrategy.OWNERSHIP_BASED;
        this.invoiceReference = invoiceReference;
        this.evidenceUrl = evidenceUrl;
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

    public Vehicle getVehicle() {
        return vehicle;
    }

    public void setVehicle(Vehicle vehicle) {
        this.vehicle = vehicle;
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

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
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

    public String getEvidenceUrl() {
        return evidenceUrl;
    }

    public void setEvidenceUrl(String evidenceUrl) {
        this.evidenceUrl = evidenceUrl;
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

    public java.util.List<ExpenseAllocation> getAllocations() {
        return allocations;
    }

    public void setAllocations(java.util.List<ExpenseAllocation> allocations) {
        this.allocations = allocations;
    }

    public void addAllocation(ExpenseAllocation allocation) {
        if (this.allocations == null) {
            this.allocations = new java.util.ArrayList<>();
        }
        this.allocations.add(allocation);
        allocation.setExpense(this);
    }

    public void clearAllocations() {
        if (this.allocations != null) {
            this.allocations.clear();
        }
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public boolean isEvidenceProvided() {
        return (evidenceUrl != null && !evidenceUrl.trim().isEmpty())
                || (invoiceReference != null && !invoiceReference.trim().isEmpty());
    }
}
