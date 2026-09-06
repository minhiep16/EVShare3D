package com.example.evshare.entity;

import com.example.evshare.entity.enums.ContractStatus;
import jakarta.persistence.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "co_ownership_contracts")
@EntityListeners(AuditingEntityListener.class)
public class CoOwnershipContract {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_id", nullable = false)
    private OwnershipGroup group;

    @Column(name = "contract_title", nullable = false, length = 150)
    private String contractTitle;

    @Column(name = "contract_terms_text", nullable = false, columnDefinition = "LONGTEXT")
    private String contractTermsText;

    @Column(name = "version", nullable = false)
    private Integer version = 1;

    @Enumerated(EnumType.STRING)
    @org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.VARCHAR)
    @Column(name = "status", nullable = false, length = 30)
    private ContractStatus status = ContractStatus.DRAFT;

    @Column(name = "effective_date")
    private LocalDate effectiveDate;

    @Column(name = "expiry_date")
    private LocalDate expiryDate;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    public CoOwnershipContract() {
    }

    public CoOwnershipContract(Long id, OwnershipGroup group, String contractTitle, String contractTermsText, Integer version, ContractStatus status, LocalDate effectiveDate, LocalDate expiryDate, Instant createdAt) {
        this.id = id;
        this.group = group;
        this.contractTitle = contractTitle;
        this.contractTermsText = contractTermsText;
        this.version = version != null ? version : 1;
        this.status = status != null ? status : ContractStatus.DRAFT;
        this.effectiveDate = effectiveDate;
        this.expiryDate = expiryDate;
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

    public String getContractTitle() {
        return contractTitle;
    }

    public void setContractTitle(String contractTitle) {
        this.contractTitle = contractTitle;
    }

    public String getContractTermsText() {
        return contractTermsText;
    }

    public void setContractTermsText(String contractTermsText) {
        this.contractTermsText = contractTermsText;
    }

    public Integer getVersion() {
        return version;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }

    public ContractStatus getStatus() {
        return status;
    }

    public void setStatus(ContractStatus status) {
        this.status = status;
    }

    public LocalDate getEffectiveDate() {
        return effectiveDate;
    }

    public void setEffectiveDate(LocalDate effectiveDate) {
        this.effectiveDate = effectiveDate;
    }

    public LocalDate getExpiryDate() {
        return expiryDate;
    }

    public void setExpiryDate(LocalDate expiryDate) {
        this.expiryDate = expiryDate;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
