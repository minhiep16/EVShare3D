package com.example.evshare.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public class CreateContractRequest {

    @NotNull(message = "Ownership group ID cannot be null")
    private Long groupId;

    @NotBlank(message = "Contract title cannot be blank")
    @Size(max = 150, message = "Contract title cannot exceed 150 characters")
    private String contractTitle;

    @NotBlank(message = "Contract terms text cannot be blank")
    private String contractTermsText;

    private LocalDate effectiveDate;
    private LocalDate expiryDate;

    public CreateContractRequest() {
    }

    public CreateContractRequest(Long groupId, String contractTitle, String contractTermsText, LocalDate effectiveDate, LocalDate expiryDate) {
        this.groupId = groupId;
        this.contractTitle = contractTitle;
        this.contractTermsText = contractTermsText;
        this.effectiveDate = effectiveDate;
        this.expiryDate = expiryDate;
    }

    public Long getGroupId() {
        return groupId;
    }

    public void setGroupId(Long groupId) {
        this.groupId = groupId;
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
}
