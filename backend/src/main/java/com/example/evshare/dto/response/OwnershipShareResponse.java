package com.example.evshare.dto.response;

import com.example.evshare.entity.OwnershipShare;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.math.BigDecimal;
import java.time.Instant;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class OwnershipShareResponse {

    private Long id;
    private Long userId;
    private String userFullName;
    private String userEmail;
    private BigDecimal percentage;
    private String shareCertificateNumber;
    private Instant acquiredAt;
    private Boolean isActive;

    public OwnershipShareResponse() {
    }

    public OwnershipShareResponse(Long id, Long userId, String userFullName, String userEmail,
                                  BigDecimal percentage, String shareCertificateNumber,
                                  Instant acquiredAt, Boolean isActive) {
        this.id = id;
        this.userId = userId;
        this.userFullName = userFullName;
        this.userEmail = userEmail;
        this.percentage = percentage;
        this.shareCertificateNumber = shareCertificateNumber;
        this.acquiredAt = acquiredAt;
        this.isActive = isActive;
    }

    public static OwnershipShareResponse fromEntity(OwnershipShare share) {
        if (share == null) {
            return null;
        }
        return new OwnershipShareResponse(
                share.getId(),
                share.getUser() != null ? share.getUser().getId() : null,
                share.getUser() != null ? share.getUser().getFullName() : null,
                share.getUser() != null ? share.getUser().getEmail() : null,
                share.getPercentage(),
                share.getShareCertificateNumber(),
                share.getAcquiredAt(),
                share.getIsActive()
        );
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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
