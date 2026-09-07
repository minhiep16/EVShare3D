package com.example.evshare.dto.response;

import com.example.evshare.entity.CoOwnershipContract;
import com.example.evshare.entity.enums.ContractStatus;

import java.time.Instant;
import java.time.LocalDate;

public class ContractResponse {

    private Long id;
    private Long groupId;
    private String groupName;
    private String contractTitle;
    private String contractTermsText;
    private Integer version;
    private ContractStatus status;
    private LocalDate effectiveDate;
    private LocalDate expiryDate;
    private Instant createdAt;

    public ContractResponse() {
    }

    public ContractResponse(Long id, Long groupId, String groupName, String contractTitle,
                            String contractTermsText, Integer version, ContractStatus status,
                            LocalDate effectiveDate, LocalDate expiryDate, Instant createdAt) {
        this.id = id;
        this.groupId = groupId;
        this.groupName = groupName;
        this.contractTitle = contractTitle;
        this.contractTermsText = contractTermsText;
        this.version = version;
        this.status = status;
        this.effectiveDate = effectiveDate;
        this.expiryDate = expiryDate;
        this.createdAt = createdAt;
    }

    public static ContractResponse fromEntity(CoOwnershipContract contract) {
        if (contract == null) {
            return null;
        }
        return new ContractResponse(
                contract.getId(),
                contract.getGroup() != null ? contract.getGroup().getId() : null,
                contract.getGroup() != null ? contract.getGroup().getGroupName() : null,
                contract.getContractTitle(),
                contract.getContractTermsText(),
                contract.getVersion(),
                contract.getStatus(),
                contract.getEffectiveDate(),
                contract.getExpiryDate(),
                contract.getCreatedAt()
        );
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getGroupId() {
        return groupId;
    }

    public void setGroupId(Long groupId) {
        this.groupId = groupId;
    }

    public String getGroupName() {
        return groupName;
    }

    public void setGroupName(String groupName) {
        this.groupName = groupName;
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
