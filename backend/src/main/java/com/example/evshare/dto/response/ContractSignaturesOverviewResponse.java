package com.example.evshare.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Schema(description = "Overview of contract signatures and pending signers for a syndicate agreement")
public class ContractSignaturesOverviewResponse {

    @Schema(description = "Contract ID", example = "10")
    private Long contractId;

    @Schema(description = "Contract version number", example = "1")
    private Integer contractVersion;

    @Schema(description = "Current lifecycle status of the contract", example = "PENDING_SIGNATURE")
    private String contractStatus;

    @Schema(description = "Total number of active co-owners required to sign", example = "3")
    private int totalRequiredSignatures;

    @Schema(description = "Total number of valid signatures submitted", example = "2")
    private int totalSubmittedSignatures;

    @Schema(description = "True if all active co-owners have signed", example = "false")
    private boolean allSigned;

    @Schema(description = "List of submitted signature records")
    private List<ContractSignatureResponse> signatures = new ArrayList<>();

    @Schema(description = "List of co-owners who have not yet signed")
    private List<PendingSignerDto> pendingSigners = new ArrayList<>();

    public ContractSignaturesOverviewResponse() {
    }

    public ContractSignaturesOverviewResponse(Long contractId, Integer contractVersion, String contractStatus,
                                            int totalRequiredSignatures, int totalSubmittedSignatures,
                                            boolean allSigned, List<ContractSignatureResponse> signatures,
                                            List<PendingSignerDto> pendingSigners) {
        this.contractId = contractId;
        this.contractVersion = contractVersion;
        this.contractStatus = contractStatus;
        this.totalRequiredSignatures = totalRequiredSignatures;
        this.totalSubmittedSignatures = totalSubmittedSignatures;
        this.allSigned = allSigned;
        this.signatures = signatures != null ? signatures : new ArrayList<>();
        this.pendingSigners = pendingSigners != null ? pendingSigners : new ArrayList<>();
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

    public String getContractStatus() {
        return contractStatus;
    }

    public void setContractStatus(String contractStatus) {
        this.contractStatus = contractStatus;
    }

    public int getTotalRequiredSignatures() {
        return totalRequiredSignatures;
    }

    public void setTotalRequiredSignatures(int totalRequiredSignatures) {
        this.totalRequiredSignatures = totalRequiredSignatures;
    }

    public int getTotalSubmittedSignatures() {
        return totalSubmittedSignatures;
    }

    public void setTotalSubmittedSignatures(int totalSubmittedSignatures) {
        this.totalSubmittedSignatures = totalSubmittedSignatures;
    }

    public boolean isAllSigned() {
        return allSigned;
    }

    public void setAllSigned(boolean allSigned) {
        this.allSigned = allSigned;
    }

    public List<ContractSignatureResponse> getSignatures() {
        return signatures;
    }

    public void setSignatures(List<ContractSignatureResponse> signatures) {
        this.signatures = signatures;
    }

    public List<PendingSignerDto> getPendingSigners() {
        return pendingSigners;
    }

    public void setPendingSigners(List<PendingSignerDto> pendingSigners) {
        this.pendingSigners = pendingSigners;
    }

    @Schema(description = "Details of an active co-owner whose signature is still pending")
    public static class PendingSignerDto {

        @Schema(description = "User ID", example = "42")
        private Long userId;

        @Schema(description = "User full name", example = "Tran Thi B")
        private String fullName;

        @Schema(description = "User email address", example = "owner2@evshare.vn")
        private String email;

        @Schema(description = "Active ownership equity percentage", example = "35.00")
        private BigDecimal percentage;

        @Schema(description = "Assigned share certificate number", example = "CERT-GRP1-002")
        private String certificateNumber;

        public PendingSignerDto() {
        }

        public PendingSignerDto(Long userId, String fullName, String email, BigDecimal percentage, String certificateNumber) {
            this.userId = userId;
            this.fullName = fullName;
            this.email = email;
            this.percentage = percentage;
            this.certificateNumber = certificateNumber;
        }

        public Long getUserId() {
            return userId;
        }

        public void setUserId(Long userId) {
            this.userId = userId;
        }

        public String getFullName() {
            return fullName;
        }

        public void setFullName(String fullName) {
            this.fullName = fullName;
        }

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }

        public BigDecimal getPercentage() {
            return percentage;
        }

        public void setPercentage(BigDecimal percentage) {
            this.percentage = percentage;
        }

        public String getCertificateNumber() {
            return certificateNumber;
        }

        public void setCertificateNumber(String certificateNumber) {
            this.certificateNumber = certificateNumber;
        }
    }
}
