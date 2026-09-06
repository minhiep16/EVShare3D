package com.example.evshare.entity;

import jakarta.persistence.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;

@Entity
@Table(
        name = "contract_signatures",
        uniqueConstraints = @UniqueConstraint(name = "uk_contract_user_signature", columnNames = {"contract_id", "user_id"})
)
@EntityListeners(AuditingEntityListener.class)
public class ContractSignature {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "contract_id", nullable = false)
    private CoOwnershipContract contract;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "signature_hash", nullable = false, length = 255)
    private String signatureHash;

    @CreatedDate
    @Column(name = "signed_at", nullable = false, updatable = false)
    private Instant signedAt = Instant.now();

    @Column(name = "ip_address", nullable = false, length = 45)
    private String ipAddress;

    public ContractSignature() {
    }

    public ContractSignature(Long id, CoOwnershipContract contract, User user, String signatureHash, Instant signedAt, String ipAddress) {
        this.id = id;
        this.contract = contract;
        this.user = user;
        this.signatureHash = signatureHash;
        this.signedAt = signedAt != null ? signedAt : Instant.now();
        this.ipAddress = ipAddress;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public CoOwnershipContract getContract() {
        return contract;
    }

    public void setContract(CoOwnershipContract contract) {
        this.contract = contract;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public String getSignatureHash() {
        return signatureHash;
    }

    public void setSignatureHash(String signatureHash) {
        this.signatureHash = signatureHash;
    }

    public Instant getSignedAt() {
        return signedAt;
    }

    public void setSignedAt(Instant signedAt) {
        this.signedAt = signedAt;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }
}
