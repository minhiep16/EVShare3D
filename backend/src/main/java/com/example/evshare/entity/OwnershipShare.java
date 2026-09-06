package com.example.evshare.entity;

import jakarta.persistence.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(
        name = "ownership_shares",
        uniqueConstraints = @UniqueConstraint(name = "uk_share_group_user", columnNames = {"group_id", "user_id"})
)
@EntityListeners(AuditingEntityListener.class)
public class OwnershipShare {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_id", nullable = false)
    private OwnershipGroup group;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "percentage", nullable = false, precision = 5, scale = 2)
    private BigDecimal percentage;

    @Column(name = "share_certificate_number", nullable = false, unique = true, length = 100)
    private String shareCertificateNumber;

    @CreatedDate
    @Column(name = "acquired_at", nullable = false, updatable = false)
    private Instant acquiredAt = Instant.now();

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    public OwnershipShare() {
    }

    public OwnershipShare(Long id, OwnershipGroup group, User user, BigDecimal percentage, String shareCertificateNumber, Instant acquiredAt, Boolean isActive) {
        this.id = id;
        this.group = group;
        this.user = user;
        this.percentage = percentage;
        this.shareCertificateNumber = shareCertificateNumber;
        this.acquiredAt = acquiredAt != null ? acquiredAt : Instant.now();
        this.isActive = isActive != null ? isActive : true;
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

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public BigDecimal getPercentage() {
        return percentage;
    }

    public void setPercentage(BigDecimal percentage) {
        this.percentage = percentage;
    }

    public String getShareCertificateNumber() {
        return shareCertificateNumber;
    }

    public void setShareCertificateNumber(String shareCertificateNumber) {
        this.shareCertificateNumber = shareCertificateNumber;
    }

    public Instant getAcquiredAt() {
        return acquiredAt;
    }

    public void setAcquiredAt(Instant acquiredAt) {
        this.acquiredAt = acquiredAt;
    }

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }
}
