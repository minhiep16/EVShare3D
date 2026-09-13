package com.example.evshare.dto.response;

import com.example.evshare.entity.Proposal;
import com.example.evshare.entity.VoteOption;
import com.example.evshare.entity.enums.ProposalStatus;
import com.example.evshare.entity.enums.ProposalType;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Schema(description = "Detailed representation of a governance decision chamber proposal")
public class ProposalResponse {

    @Schema(description = "Proposal ID", example = "10")
    private Long id;

    @Schema(description = "Target syndicate ownership group ID", example = "1")
    private Long groupId;

    @Schema(description = "Target ownership group name", example = "VinFast VF8 Syndicate Alpha")
    private String groupName;

    @Schema(description = "Proposer user ID", example = "5")
    private Long proposerUserId;

    @Schema(description = "Proposer full name", example = "Nguyen Van A")
    private String proposerFullName;

    @Schema(description = "Proposer email address", example = "nguyen.a@evshare.io")
    private String proposerEmail;

    @Schema(description = "Proposal title", example = "VinFast VF8 Battery Preventive Diagnostics & Service")
    private String title;

    @Schema(description = "Proposal description and technical rationale",
            example = "Scheduled 40,000km traction battery diagnostic, thermal coolant flush, and software calibration at authorized service workshop.")
    private String description;

    @Schema(description = "Proposal category", example = "ROUTINE_EXPENSE")
    private ProposalType proposalType;

    @Schema(description = "Current lifecycle status (ACTIVE, PASSED, REJECTED, EXPIRED)", example = "ACTIVE")
    private ProposalStatus status;

    @Schema(description = "Deadline timestamp for casting ballots", example = "2026-09-12T17:00:00Z")
    private Instant votingDeadline;

    @Schema(description = "Creation timestamp", example = "2026-09-09T17:00:00Z")
    private Instant createdAt;

    @Schema(description = "Available ballot options")
    private List<VoteOptionResponse> options = new ArrayList<>();

    public ProposalResponse() {
    }

    public static ProposalResponse fromEntity(Proposal proposal) {
        return fromEntity(proposal, null);
    }

    public static ProposalResponse fromEntity(Proposal proposal, List<VoteOption> voteOptions) {
        if (proposal == null) {
            return null;
        }
        ProposalResponse response = new ProposalResponse();
        response.setId(proposal.getId());
        if (proposal.getGroup() != null) {
            response.setGroupId(proposal.getGroup().getId());
            response.setGroupName(proposal.getGroup().getGroupName());
        }
        if (proposal.getProposerUser() != null) {
            response.setProposerUserId(proposal.getProposerUser().getId());
            response.setProposerFullName(proposal.getProposerUser().getFullName());
            response.setProposerEmail(proposal.getProposerUser().getEmail());
        }
        response.setTitle(proposal.getTitle());
        response.setDescription(proposal.getDescription());
        response.setProposalType(proposal.getProposalType());
        response.setStatus(proposal.getStatus());
        response.setVotingDeadline(proposal.getVotingDeadline());
        response.setCreatedAt(proposal.getCreatedAt());

        if (voteOptions != null) {
            response.setOptions(voteOptions.stream()
                    .map(VoteOptionResponse::fromEntity)
                    .collect(Collectors.toList()));
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

    public Long getProposerUserId() {
        return proposerUserId;
    }

    public void setProposerUserId(Long proposerUserId) {
        this.proposerUserId = proposerUserId;
    }

    public String getProposerFullName() {
        return proposerFullName;
    }

    public void setProposerFullName(String proposerFullName) {
        this.proposerFullName = proposerFullName;
    }

    public String getProposerEmail() {
        return proposerEmail;
    }

    public void setProposerEmail(String proposerEmail) {
        this.proposerEmail = proposerEmail;
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

    public Instant getVotingDeadline() {
        return votingDeadline;
    }

    public void setVotingDeadline(Instant votingDeadline) {
        this.votingDeadline = votingDeadline;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public List<VoteOptionResponse> getOptions() {
        return options;
    }

    public void setOptions(List<VoteOptionResponse> options) {
        this.options = options != null ? options : new ArrayList<>();
    }
}
