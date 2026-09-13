package com.example.evshare.dto.response;

import com.example.evshare.entity.enums.ProposalStatus;
import com.example.evshare.entity.enums.ProposalType;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Schema(description = "Deterministic equity-weighted vote aggregation, quorum analysis, and ballot history")
public class ProposalTallyResponse {

    @Schema(description = "Proposal ID", example = "10")
    private Long proposalId;

    @Schema(description = "Proposal title", example = "Tire Replacement")
    private String proposalTitle;

    @Schema(description = "Proposal category", example = "ROUTINE_EXPENSE")
    private ProposalType proposalType;

    @Schema(description = "Proposal status", example = "ACTIVE")
    private ProposalStatus status;

    @Schema(description = "Total active equity in syndicate group", example = "100.00")
    private BigDecimal totalGroupActiveEquity;

    @Schema(description = "Total participating equity weight from cast ballots", example = "70.00")
    private BigDecimal totalParticipatingEquity;

    @Schema(description = "Participation rate percentage relative to total active group equity", example = "70.00")
    private BigDecimal participationRatePercentage;

    @Schema(description = "Total count of co-owners who cast ballots", example = "2")
    private int totalVotersCount;

    @Schema(description = "Mandatory quorum threshold percentage", example = "60.00")
    private BigDecimal quorumPercentage;

    @Schema(description = "Whether the 60.00% quorum threshold has been satisfied", example = "true")
    private boolean quorumReached;

    @Schema(description = "Total equity weight voting APPROVE", example = "40.00")
    private BigDecimal approveEquity;

    @Schema(description = "Total equity weight voting REJECT", example = "30.00")
    private BigDecimal rejectEquity;

    @Schema(description = "Total equity weight voting ABSTAIN", example = "0.00")
    private BigDecimal abstainEquity;

    @Schema(description = "APPROVE percentage of participating equity", example = "57.14")
    private BigDecimal approvePercentageOfParticipating;

    @Schema(description = "REJECT percentage of participating equity", example = "42.86")
    private BigDecimal rejectPercentageOfParticipating;

    @Schema(description = "ABSTAIN percentage of participating equity", example = "0.00")
    private BigDecimal abstainPercentageOfParticipating;

    @Schema(description = "APPROVE percentage of total group active equity", example = "40.00")
    private BigDecimal approvePercentageOfTotal;

    @Schema(description = "Required approval threshold percentage (>50.00% for ROUTINE, >=75.00% for MAJOR)", example = "50.00")
    private BigDecimal requiredThresholdPercentage;

    @Schema(description = "Threshold evaluation rule (RELATIVE_TO_PARTICIPATING or RELATIVE_TO_TOTAL)", example = "RELATIVE_TO_PARTICIPATING")
    private String thresholdType;

    @Schema(description = "Whether the proposal currently satisfies all conditions to pass", example = "true")
    private boolean passed;

    @Schema(description = "Human-readable deterministic outcome summary", example = "Approved with 57.14% of participating equity (>50.00%)")
    private String outcomeReason;

    @Schema(description = "Granular list of cast ballots preserving complete audit trail")
    private List<VoteResponse> ballots = new ArrayList<>();

    public ProposalTallyResponse() {}

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

    public BigDecimal getTotalGroupActiveEquity() {
        return totalGroupActiveEquity;
    }

    public void setTotalGroupActiveEquity(BigDecimal totalGroupActiveEquity) {
        this.totalGroupActiveEquity = totalGroupActiveEquity;
    }

    public BigDecimal getTotalParticipatingEquity() {
        return totalParticipatingEquity;
    }

    public void setTotalParticipatingEquity(BigDecimal totalParticipatingEquity) {
        this.totalParticipatingEquity = totalParticipatingEquity;
    }

    public BigDecimal getParticipationRatePercentage() {
        return participationRatePercentage;
    }

    public void setParticipationRatePercentage(BigDecimal participationRatePercentage) {
        this.participationRatePercentage = participationRatePercentage;
    }

    public int getTotalVotersCount() {
        return totalVotersCount;
    }

    public void setTotalVotersCount(int totalVotersCount) {
        this.totalVotersCount = totalVotersCount;
    }

    public BigDecimal getQuorumPercentage() {
        return quorumPercentage;
    }

    public void setQuorumPercentage(BigDecimal quorumPercentage) {
        this.quorumPercentage = quorumPercentage;
    }

    public boolean isQuorumReached() {
        return quorumReached;
    }

    public void setQuorumReached(boolean quorumReached) {
        this.quorumReached = quorumReached;
    }

    public BigDecimal getApproveEquity() {
        return approveEquity;
    }

    public void setApproveEquity(BigDecimal approveEquity) {
        this.approveEquity = approveEquity;
    }

    public BigDecimal getRejectEquity() {
        return rejectEquity;
    }

    public void setRejectEquity(BigDecimal rejectEquity) {
        this.rejectEquity = rejectEquity;
    }

    public BigDecimal getAbstainEquity() {
        return abstainEquity;
    }

    public void setAbstainEquity(BigDecimal abstainEquity) {
        this.abstainEquity = abstainEquity;
    }

    public BigDecimal getApprovePercentageOfParticipating() {
        return approvePercentageOfParticipating;
    }

    public void setApprovePercentageOfParticipating(BigDecimal approvePercentageOfParticipating) {
        this.approvePercentageOfParticipating = approvePercentageOfParticipating;
    }

    public BigDecimal getRejectPercentageOfParticipating() {
        return rejectPercentageOfParticipating;
    }

    public void setRejectPercentageOfParticipating(BigDecimal rejectPercentageOfParticipating) {
        this.rejectPercentageOfParticipating = rejectPercentageOfParticipating;
    }

    public BigDecimal getAbstainPercentageOfParticipating() {
        return abstainPercentageOfParticipating;
    }

    public void setAbstainPercentageOfParticipating(BigDecimal abstainPercentageOfParticipating) {
        this.abstainPercentageOfParticipating = abstainPercentageOfParticipating;
    }

    public BigDecimal getApprovePercentageOfTotal() {
        return approvePercentageOfTotal;
    }

    public void setApprovePercentageOfTotal(BigDecimal approvePercentageOfTotal) {
        this.approvePercentageOfTotal = approvePercentageOfTotal;
    }

    public BigDecimal getRequiredThresholdPercentage() {
        return requiredThresholdPercentage;
    }

    public void setRequiredThresholdPercentage(BigDecimal requiredThresholdPercentage) {
        this.requiredThresholdPercentage = requiredThresholdPercentage;
    }

    public String getThresholdType() {
        return thresholdType;
    }

    public void setThresholdType(String thresholdType) {
        this.thresholdType = thresholdType;
    }

    public boolean isPassed() {
        return passed;
    }

    public void setPassed(boolean passed) {
        this.passed = passed;
    }

    public String getOutcomeReason() {
        return outcomeReason;
    }

    public void setOutcomeReason(String outcomeReason) {
        this.outcomeReason = outcomeReason;
    }

    public List<VoteResponse> getBallots() {
        return ballots;
    }

    public void setBallots(List<VoteResponse> ballots) {
        this.ballots = ballots;
    }
}
