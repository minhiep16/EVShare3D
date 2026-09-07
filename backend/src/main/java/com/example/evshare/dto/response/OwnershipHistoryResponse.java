package com.example.evshare.dto.response;

import java.math.BigDecimal;
import java.time.Instant;

public class OwnershipHistoryResponse {

    private Long id;
    private Long shareId;
    private String action;
    private Long actingUserId;
    private BigDecimal previousPercentage;
    private BigDecimal newPercentage;
    private String previousCertificateNumber;
    private String newCertificateNumber;
    private Boolean previousIsActive;
    private Boolean newIsActive;
    private Instant effectiveDate;
    private String oldStateJson;
    private String newStateJson;

    public OwnershipHistoryResponse() {
    }

    public OwnershipHistoryResponse(Long id, Long shareId, String action, Long actingUserId,
                                    BigDecimal previousPercentage, BigDecimal newPercentage,
                                    String previousCertificateNumber, String newCertificateNumber,
                                    Boolean previousIsActive, Boolean newIsActive,
                                    Instant effectiveDate, String oldStateJson, String newStateJson) {
        this.id = id;
        this.shareId = shareId;
        this.action = action;
        this.actingUserId = actingUserId;
        this.previousPercentage = previousPercentage;
        this.newPercentage = newPercentage;
        this.previousCertificateNumber = previousCertificateNumber;
        this.newCertificateNumber = newCertificateNumber;
        this.previousIsActive = previousIsActive;
        this.newIsActive = newIsActive;
        this.effectiveDate = effectiveDate;
        this.oldStateJson = oldStateJson;
        this.newStateJson = newStateJson;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getShareId() {
        return shareId;
    }

    public void setShareId(Long shareId) {
        this.shareId = shareId;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public Long getActingUserId() {
        return actingUserId;
    }

    public void setActingUserId(Long actingUserId) {
        this.actingUserId = actingUserId;
    }

    public BigDecimal getPreviousPercentage() {
        return previousPercentage;
    }

    public void setPreviousPercentage(BigDecimal previousPercentage) {
        this.previousPercentage = previousPercentage;
    }

    public BigDecimal getNewPercentage() {
        return newPercentage;
    }

    public void setNewPercentage(BigDecimal newPercentage) {
        this.newPercentage = newPercentage;
    }

    public String getPreviousCertificateNumber() {
        return previousCertificateNumber;
    }

    public void setPreviousCertificateNumber(String previousCertificateNumber) {
        this.previousCertificateNumber = previousCertificateNumber;
    }

    public String getNewCertificateNumber() {
        return newCertificateNumber;
    }

    public void setNewCertificateNumber(String newCertificateNumber) {
        this.newCertificateNumber = newCertificateNumber;
    }

    public Boolean getPreviousIsActive() {
        return previousIsActive;
    }

    public void setPreviousIsActive(Boolean previousIsActive) {
        this.previousIsActive = previousIsActive;
    }

    public Boolean getNewIsActive() {
        return newIsActive;
    }

    public void setNewIsActive(Boolean newIsActive) {
        this.newIsActive = newIsActive;
    }

    public Instant getEffectiveDate() {
        return effectiveDate;
    }

    public void setEffectiveDate(Instant effectiveDate) {
        this.effectiveDate = effectiveDate;
    }

    public String getOldStateJson() {
        return oldStateJson;
    }

    public void setOldStateJson(String oldStateJson) {
        this.oldStateJson = oldStateJson;
    }

    public String getNewStateJson() {
        return newStateJson;
    }

    public void setNewStateJson(String newStateJson) {
        this.newStateJson = newStateJson;
    }
}
