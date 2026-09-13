package com.example.evshare.dto.response;

import com.example.evshare.entity.Dispute;
import com.example.evshare.entity.DisputeEvidence;
import com.example.evshare.entity.enums.DisputeStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Schema(description = "Dispute grievance and resolution details")
public class DisputeResponse {

    @Schema(description = "Dispute ID", example = "10")
    private Long id;

    @Schema(description = "Syndicate ownership group ID", example = "1")
    private Long groupId;

    @Schema(description = "Syndicate ownership group name", example = "Tesla Model 3 Co-Owners Group")
    private String groupName;

    @Schema(description = "Associated usage session ID if applicable", example = "105")
    private Long usageSessionId;

    @Schema(description = "ID of the complainant co-owner who initiated the dispute", example = "5")
    private Long complainantUserId;

    @Schema(description = "Full name of the complainant", example = "Alice Owner")
    private String complainantUserName;

    @Schema(description = "ID of the respondent co-owner or driver if applicable", example = "3")
    private Long respondentUserId;

    @Schema(description = "Full name of the respondent", example = "Bob Driver")
    private String respondentUserName;

    @Schema(description = "Dispute title", example = "Unreported front bumper damage")
    private String title;

    @Schema(description = "Dispute grievance description/reason", example = "Vehicle returned with bumper scratch and depleted battery.")
    private String description;

    @Schema(description = "Current lifecycle status (OPEN, UNDER_REVIEW, RESOLVED, ESCALATED)", example = "OPEN")
    private DisputeStatus status;

    @Schema(description = "Resolution summary upon settlement or administrative arbitration", example = "Settled: 500,000 VND reimbursed to shared maintenance vault.")
    private String resolutionSummary;

    @Schema(description = "Staff recorded mediation review notes and observations", example = "Review conducted with parties. Complainant confirmed no prior damage.")
    private String mediationNotes;

    @Schema(description = "Staff formulated proposed resolution recommendation", example = "Propose respondent pays 350,000 VND deductible.")
    private String proposedResolution;

    @Schema(description = "User ID of the staff mediator handling the dispute", example = "30")
    private Long mediatorUserId;

    @Schema(description = "Full name of the staff mediator", example = "Staff Operator")
    private String mediatorUserName;

    @Schema(description = "User ID of the administrator who executed final binding arbitration", example = "1")
    private Long arbitratorUserId;

    @Schema(description = "Full name of the administrator arbitrator", example = "System Admin")
    private String arbitratorUserName;

    @Schema(description = "Timestamp when dispute was arbitrated and resolved")
    private Instant resolvedAt;

    @Schema(description = "Financial adjustment amount to/from SharedFund upon resolution", example = "500000.00")
    private BigDecimal fundAdjustmentAmount;

    @Schema(description = "Linked SharedFund transaction ID", example = "201")
    private Long fundTransactionId;

    @Schema(description = "Unique SharedFund transaction reference string", example = "DISP-10-A1B2C3D4")
    private String fundTransactionReference;

    @Schema(description = "List of associated evidence records")
    private List<DisputeEvidenceResponse> evidences = new ArrayList<>();

    @Schema(description = "Timestamp when dispute was filed")
    private Instant createdAt;

    public DisputeResponse() {
    }

    public static DisputeResponse fromEntity(Dispute dispute) {
        return fromEntity(dispute, Collections.emptyList());
    }

    public static DisputeResponse fromEntity(Dispute dispute, List<DisputeEvidence> evidences) {
        if (dispute == null) {
            return null;
        }
        DisputeResponse response = new DisputeResponse();
        response.setId(dispute.getId());
        response.setGroupId(dispute.getGroup() != null ? dispute.getGroup().getId() : null);
        response.setGroupName(dispute.getGroup() != null ? dispute.getGroup().getGroupName() : null);
        response.setUsageSessionId(dispute.getUsageSession() != null ? dispute.getUsageSession().getId() : null);
        response.setComplainantUserId(dispute.getComplainantUser() != null ? dispute.getComplainantUser().getId() : null);
        response.setComplainantUserName(dispute.getComplainantUser() != null ? dispute.getComplainantUser().getFullName() : null);
        response.setRespondentUserId(dispute.getRespondentUser() != null ? dispute.getRespondentUser().getId() : null);
        response.setRespondentUserName(dispute.getRespondentUser() != null ? dispute.getRespondentUser().getFullName() : null);
        response.setTitle(dispute.getTitle());
        response.setDescription(dispute.getDescription());
        response.setStatus(dispute.getStatus());
        response.setResolutionSummary(dispute.getResolutionSummary());
        response.setMediationNotes(dispute.getMediationNotes());
        response.setProposedResolution(dispute.getProposedResolution());
        response.setMediatorUserId(dispute.getMediatorUser() != null ? dispute.getMediatorUser().getId() : null);
        response.setMediatorUserName(dispute.getMediatorUser() != null ? dispute.getMediatorUser().getFullName() : null);
        response.setArbitratorUserId(dispute.getArbitratorUser() != null ? dispute.getArbitratorUser().getId() : null);
        response.setArbitratorUserName(dispute.getArbitratorUser() != null ? dispute.getArbitratorUser().getFullName() : null);
        response.setResolvedAt(dispute.getResolvedAt());
        response.setFundAdjustmentAmount(dispute.getFundAdjustmentAmount());
        if (dispute.getFundTransaction() != null) {
            response.setFundTransactionId(dispute.getFundTransaction().getId());
            response.setFundTransactionReference(dispute.getFundTransaction().getTransactionReference());
        }
        response.setCreatedAt(dispute.getCreatedAt());

        if (evidences != null && !evidences.isEmpty()) {
            response.setEvidences(evidences.stream()
                    .map(DisputeEvidenceResponse::fromEntity)
                    .collect(Collectors.toList()));
        } else {
            response.setEvidences(new ArrayList<>());
        }

        return response;
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

    public Long getUsageSessionId() {
        return usageSessionId;
    }

    public void setUsageSessionId(Long usageSessionId) {
        this.usageSessionId = usageSessionId;
    }

    public Long getComplainantUserId() {
        return complainantUserId;
    }

    public void setComplainantUserId(Long complainantUserId) {
        this.complainantUserId = complainantUserId;
    }

    public String getComplainantUserName() {
        return complainantUserName;
    }

    public void setComplainantUserName(String complainantUserName) {
        this.complainantUserName = complainantUserName;
    }

    public Long getRespondentUserId() {
        return respondentUserId;
    }

    public void setRespondentUserId(Long respondentUserId) {
        this.respondentUserId = respondentUserId;
    }

    public String getRespondentUserName() {
        return respondentUserName;
    }

    public void setRespondentUserName(String respondentUserName) {
        this.respondentUserName = respondentUserName;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public DisputeStatus getStatus() {
        return status;
    }

    public void setStatus(DisputeStatus status) {
        this.status = status;
    }

    public String getResolutionSummary() {
        return resolutionSummary;
    }

    public void setResolutionSummary(String resolutionSummary) {
        this.resolutionSummary = resolutionSummary;
    }

    public String getMediationNotes() {
        return mediationNotes;
    }

    public void setMediationNotes(String mediationNotes) {
        this.mediationNotes = mediationNotes;
    }

    public String getProposedResolution() {
        return proposedResolution;
    }

    public void setProposedResolution(String proposedResolution) {
        this.proposedResolution = proposedResolution;
    }

    public Long getMediatorUserId() {
        return mediatorUserId;
    }

    public void setMediatorUserId(Long mediatorUserId) {
        this.mediatorUserId = mediatorUserId;
    }

    public String getMediatorUserName() {
        return mediatorUserName;
    }

    public void setMediatorUserName(String mediatorUserName) {
        this.mediatorUserName = mediatorUserName;
    }

    public Long getArbitratorUserId() {
        return arbitratorUserId;
    }

    public void setArbitratorUserId(Long arbitratorUserId) {
        this.arbitratorUserId = arbitratorUserId;
    }

    public String getArbitratorUserName() {
        return arbitratorUserName;
    }

    public void setArbitratorUserName(String arbitratorUserName) {
        this.arbitratorUserName = arbitratorUserName;
    }

    public Instant getResolvedAt() {
        return resolvedAt;
    }

    public void setResolvedAt(Instant resolvedAt) {
        this.resolvedAt = resolvedAt;
    }

    public BigDecimal getFundAdjustmentAmount() {
        return fundAdjustmentAmount;
    }

    public void setFundAdjustmentAmount(BigDecimal fundAdjustmentAmount) {
        this.fundAdjustmentAmount = fundAdjustmentAmount;
    }

    public Long getFundTransactionId() {
        return fundTransactionId;
    }

    public void setFundTransactionId(Long fundTransactionId) {
        this.fundTransactionId = fundTransactionId;
    }

    public String getFundTransactionReference() {
        return fundTransactionReference;
    }

    public void setFundTransactionReference(String fundTransactionReference) {
        this.fundTransactionReference = fundTransactionReference;
    }

    public List<DisputeEvidenceResponse> getEvidences() {
        return evidences;
    }

    public void setEvidences(List<DisputeEvidenceResponse> evidences) {
        this.evidences = evidences;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
