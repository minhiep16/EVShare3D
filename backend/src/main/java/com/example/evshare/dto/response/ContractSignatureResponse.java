package com.example.evshare.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

@Schema(description = "Details of a digital contract signature record")
public class ContractSignatureResponse {

    @Schema(description = "Signature record ID", example = "1")
    private Long id;

    @Schema(description = "Bound contract ID", example = "10")
    private Long contractId;

    @Schema(description = "Contract version number", example = "1")
    private Integer contractVersion;

    @Schema(description = "Signatory user ID", example = "42")
    private Long userId;

    @Schema(description = "Signatory full name", example = "Nguyen Van A")
    private String userFullName;

    @Schema(description = "Signatory email address", example = "user@evshare.vn")
    private String userEmail;

    @Schema(description = "Cryptographic SHA-256 digest binding terms, version, signer, timestamp, and IP",
            example = "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855")
    private String signatureHash;

    @Schema(description = "Timestamp when signature was submitted in UTC", example = "2026-09-07T14:30:00Z")
    private Instant signedAt;

    @Schema(description = "Client IP address from which the signature was submitted", example = "192.168.1.100")
    private String ipAddress;

    public ContractSignatureResponse() {
    }

    public ContractSignatureResponse(Long id, Long contractId, Integer contractVersion, Long userId,
                                   String userFullName, String userEmail, String signatureHash,
                                   Instant signedAt, String ipAddress) {
        this.id = id;
        this.contractId = contractId;
        this.contractVersion = contractVersion;
        this.userId = userId;
        this.userFullName = userFullName;
        this.userEmail = userEmail;
        this.signatureHash = signatureHash;
        this.signedAt = signedAt;
        this.ipAddress = ipAddress;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getContractId() {
        return contractId;
    }

    public void setContractId(Long contractId) {
        this.contractId = contractId;
    }

    public Integer getContractVersion() {
        return contractVersion;
    }

    public void setContractVersion(Integer contractVersion) {
        this.contractVersion = contractVersion;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getUserFullName() {
        return userFullName;
    }

    public void setUserFullName(String userFullName) {
        this.userFullName = userFullName;
    }

    public String getUserEmail() {
        return userEmail;
    }

    public void setUserEmail(String userEmail) {
        this.userEmail = userEmail;
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
