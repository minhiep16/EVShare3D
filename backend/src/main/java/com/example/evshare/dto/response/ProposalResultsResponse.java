package com.example.evshare.dto.response;

import com.example.evshare.entity.enums.ProposalStatus;
import com.example.evshare.entity.enums.ProposalType;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

@Schema(description = "Official proposal voting results and final governance decision")
public class ProposalResultsResponse {

    @Schema(description = "Proposal ID", example = "10")
    private Long proposalId;

    @Schema(description = "Proposal title", example = "Tire Replacement")
    private String proposalTitle;

    @Schema(description = "Proposal category", example = "ROUTINE_EXPENSE")
    private ProposalType proposalType;

    @Schema(description = "Proposal lifecycle status", example = "ACTIVE")
    private ProposalStatus status;

    @Schema(description = "Total eligible active equity in syndicate group", example = "100.00")
    private BigDecimal totalEligibleEquity;

    @Schema(description = "Total participating equity weight from cast ballots", example = "70.00")
    private BigDecimal participatingEquity;

    @Schema(description = "Total equity weight voting APPROVE", example = "40.00")
    private BigDecimal approveWeight;

    @Schema(description = "Total equity weight voting REJECT", example = "30.00")
    private BigDecimal rejectWeight;

    @Schema(description = "Total equity weight voting ABSTAIN", example = "0.00")
    private BigDecimal abstainWeight;

    @Schema(description = "Quorum status (REACHED or NOT_REACHED)", example = "REACHED")
    private String quorumStatus;

    @Schema(description = "Whether the 60.00% quorum requirement has been satisfied", example = "true")
    private boolean quorumReached;

    @Schema(description = "Mandatory quorum threshold percentage", example = "60.00")
    private BigDecimal quorumPercentage;

    @Schema(description = "Participation rate percentage relative to eligible equity", example = "70.00")
    private BigDecimal participationRatePercentage;

    @Schema(description = "Required decision threshold percentage", example = "50.00")
    private BigDecimal threshold;

    @Schema(description = "Threshold evaluation rule (RELATIVE_TO_PARTICIPATING or RELATIVE_TO_TOTAL)", example = "RELATIVE_TO_PARTICIPATING")
    private String thresholdType;

    @Schema(description = "Human-readable threshold description", example = "> 50.00% of participating equity")
    private String thresholdDescription;

    @Schema(description = "Final governance decision (PASSED, REJECTED, EXPIRED, QUORUM_NOT_MET)", example = "PASSED")
    private String finalDecision;

    @Schema(description = "Whether the proposal satisfies all conditions to pass", example = "true")
    private boolean passed;

    @Schema(description = "Outcome reason and mathematical decision justification", example = "Approved with 57.14% of participating equity (>50.00%)")
    private String decisionReason;

    public ProposalResultsResponse() {}

    public Long getProposalId() {
        return proposalId;
    }

    public void setProposalId(Long proposalId) {
        this.proposalId = proposalId;
    }

    public String getProposalTitle() {
        return proposalTitle;
    }

    public void setProposalTitle(String proposalTitle) {
        this.proposalTitle = proposalTitle;
    }

    public ProposalType getProposalType() {
        return proposalType;
    }

    public void setProposalType(ProposalType proposalType) {
        this.proposalType = proposalType;
    }

    public ProposalStatus getStatus() {
        return status;
    }

    public void setStatus(ProposalStatus status) {
        this.status = status;
    }

    public BigDecimal getTotalEligibleEquity() {
        return totalEligibleEquity;
    }

    public void setTotalEligibleEquity(BigDecimal totalEligibleEquity) {
        this.totalEligibleEquity = totalEligibleEquity;
    }

    public BigDecimal getParticipatingEquity() {
        return participatingEquity;
    }

    public void setParticipatingEquity(BigDecimal participatingEquity) {
        this.participatingEquity = participatingEquity;
    }

    public BigDecimal getApproveWeight() {
        return approveWeight;
    }

    public void setApproveWeight(BigDecimal approveWeight) {
        this.approveWeight = approveWeight;
    }

    public BigDecimal getRejectWeight() {
        return rejectWeight;
    }

    public void setRejectWeight(BigDecimal rejectWeight) {
        this.rejectWeight = rejectWeight;
    }

    public BigDecimal getAbstainWeight() {
        return abstainWeight;
    }

    public void setAbstainWeight(BigDecimal abstainWeight) {
        this.abstainWeight = abstainWeight;
    }

    public String getQuorumStatus() {
        return quorumStatus;
    }

    public void setQuorumStatus(String quorumStatus) {
        this.quorumStatus = quorumStatus;
    }

    public boolean isQuorumReached() {
        return quorumReached;
    }

    public void setQuorumReached(boolean quorumReached) {
        this.quorumReached = quorumReached;
    }

    public BigDecimal getQuorumPercentage() {
        return quorumPercentage;
    }

    public void setQuorumPercentage(BigDecimal quorumPercentage) {
        this.quorumPercentage = quorumPercentage;
    }

    public BigDecimal getParticipationRatePercentage() {
        return participationRatePercentage;
    }

    public void setParticipationRatePercentage(BigDecimal participationRatePercentage) {
        this.participationRatePercentage = participationRatePercentage;
    }

    public BigDecimal getThreshold() {
        return threshold;
    }

    public void setThreshold(BigDecimal threshold) {
        this.threshold = threshold;
    }

    public String getThresholdType() {
        return thresholdType;
    }

    public void setThresholdType(String thresholdType) {
        this.thresholdType = thresholdType;
    }

    public String getThresholdDescription() {
        return thresholdDescription;
    }

    public void setThresholdDescription(String thresholdDescription) {
        this.thresholdDescription = thresholdDescription;
    }

    public String getFinalDecision() {
        return finalDecision;
    }

    public void setFinalDecision(String finalDecision) {
        this.finalDecision = finalDecision;
    }

    public boolean isPassed() {
        return passed;
    }

    public void setPassed(boolean passed) {
        this.passed = passed;
    }

    public String getDecisionReason() {
        return decisionReason;
    }

    public void setDecisionReason(String decisionReason) {
        this.decisionReason = decisionReason;
    }
}
