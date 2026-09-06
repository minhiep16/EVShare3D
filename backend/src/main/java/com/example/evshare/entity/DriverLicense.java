package com.example.evshare.entity;

import com.example.evshare.entity.enums.DriverLicenseClass;
import jakarta.persistence.*;

import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "driver_licenses")
public class DriverLicense {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(name = "license_number", nullable = false, unique = true, length = 50)
    private String licenseNumber;

    @Enumerated(EnumType.STRING)
    @org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.VARCHAR)
    @Column(name = "license_class", nullable = false, length = 20)
    private DriverLicenseClass licenseClass = DriverLicenseClass.B2;

    @Column(name = "issue_date", nullable = false)
    private LocalDate issueDate;

    @Column(name = "expiry_date", nullable = false)
    private LocalDate expiryDate;

    @Column(name = "is_verified", nullable = false)
    private Boolean isVerified = false;

    @Column(name = "verified_at")
    private Instant verifiedAt;

    public DriverLicense() {
    }

    public DriverLicense(Long id, User user, String licenseNumber, DriverLicenseClass licenseClass, LocalDate issueDate, LocalDate expiryDate, Boolean isVerified, Instant verifiedAt) {
        this.id = id;
        this.user = user;
        this.licenseNumber = licenseNumber;
        this.licenseClass = licenseClass != null ? licenseClass : DriverLicenseClass.B2;
        this.issueDate = issueDate;
        this.expiryDate = expiryDate;
        this.isVerified = isVerified != null ? isVerified : false;
        this.verifiedAt = verifiedAt;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public String getLicenseNumber() {
        return licenseNumber;
    }

    public void setLicenseNumber(String licenseNumber) {
        this.licenseNumber = licenseNumber;
    }

    public DriverLicenseClass getLicenseClass() {
        return licenseClass;
    }

    public void setLicenseClass(DriverLicenseClass licenseClass) {
        this.licenseClass = licenseClass;
    }

    public LocalDate getIssueDate() {
        return issueDate;
    }

    public void setIssueDate(LocalDate issueDate) {
        this.issueDate = issueDate;
    }

    public LocalDate getExpiryDate() {
        return expiryDate;
    }

    public void setExpiryDate(LocalDate expiryDate) {
        this.expiryDate = expiryDate;
    }

    public Boolean getIsVerified() {
        return isVerified;
    }

    public void setIsVerified(Boolean isVerified) {
        this.isVerified = isVerified;
    }

    public Instant getVerifiedAt() {
        return verifiedAt;
    }

    public void setVerifiedAt(Instant verifiedAt) {
        this.verifiedAt = verifiedAt;
    }
}
