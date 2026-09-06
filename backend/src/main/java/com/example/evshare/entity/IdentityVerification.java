package com.example.evshare.entity;

import com.example.evshare.entity.enums.VerificationStatus;
import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "identity_verifications")
public class IdentityVerification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "id_card_number", nullable = false, unique = true, length = 50)
    private String idCardNumber;

    @Enumerated(EnumType.STRING)
    @org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.VARCHAR)
    @Column(name = "verification_status", nullable = false, length = 30)
    private VerificationStatus verificationStatus = VerificationStatus.PENDING;

    @Column(name = "document_front_url", nullable = false, length = 255)
    private String documentFrontUrl;

    @Column(name = "document_back_url", nullable = false, length = 255)
    private String documentBackUrl;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "verified_by_user_id")
    private User verifiedByUser;

    @Column(name = "verified_at")
    private Instant verifiedAt;

    public IdentityVerification() {
    }

    public IdentityVerification(Long id, User user, String idCardNumber, VerificationStatus verificationStatus, String documentFrontUrl, String documentBackUrl, User verifiedByUser, Instant verifiedAt) {
        this.id = id;
        this.user = user;
        this.idCardNumber = idCardNumber;
        this.verificationStatus = verificationStatus != null ? verificationStatus : VerificationStatus.PENDING;
        this.documentFrontUrl = documentFrontUrl;
        this.documentBackUrl = documentBackUrl;
        this.verifiedByUser = verifiedByUser;
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

    public String getIdCardNumber() {
        return idCardNumber;
    }

    public void setIdCardNumber(String idCardNumber) {
        this.idCardNumber = idCardNumber;
    }

    public VerificationStatus getVerificationStatus() {
        return verificationStatus;
    }

    public void setVerificationStatus(VerificationStatus verificationStatus) {
        this.verificationStatus = verificationStatus;
    }

    public String getDocumentFrontUrl() {
        return documentFrontUrl;
    }

    public void setDocumentFrontUrl(String documentFrontUrl) {
        this.documentFrontUrl = documentFrontUrl;
    }

    public String getDocumentBackUrl() {
        return documentBackUrl;
    }

    public void setDocumentBackUrl(String documentBackUrl) {
        this.documentBackUrl = documentBackUrl;
    }

    public User getVerifiedByUser() {
        return verifiedByUser;
    }

    public void setVerifiedByUser(User verifiedByUser) {
        this.verifiedByUser = verifiedByUser;
    }

    public Instant getVerifiedAt() {
        return verifiedAt;
    }

    public void setVerifiedAt(Instant verifiedAt) {
        this.verifiedAt = verifiedAt;
    }
}
