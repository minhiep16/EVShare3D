package com.example.evshare.dto.request;

import com.example.evshare.entity.enums.ProposalType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;

@Schema(description = "Request payload to create a new governance decision proposal")
public class CreateProposalRequest {

    @NotNull(message = "Ownership group ID is mandatory")
    @Schema(description = "Target syndicate ownership group ID", example = "1")
    private Long groupId;

    @NotBlank(message = "Proposal title cannot be blank")
    @Size(max = 150, message = "Proposal title cannot exceed 150 characters")
    @Schema(description = "Concise, descriptive title for the proposal", example = "VinFast VF8 Battery Preventive Diagnostics & Service")
    private String title;

    @NotBlank(message = "Proposal description cannot be blank")
    @Schema(description = "Detailed justification, scope, and technical rationale for the proposal",
            example = "Scheduled 40,000km traction battery diagnostic, thermal coolant flush, and software calibration at authorized service workshop.")
    private String description;

    @NotNull(message = "Proposal category is mandatory")
    @Schema(description = "Category of the proposal (ROUTINE_EXPENSE, MAJOR_EXPENSE, OPERATIONAL_RULE_CHANGE, OWNER_ADMISSION_OR_EXIT)",
            example = "ROUTINE_EXPENSE")
    private ProposalType proposalType;

    @Future(message = "Voting deadline must be in the future")
    @Schema(description = "Optional deadline for casting ballots (defaults to 72 hours from creation if omitted)", example = "2026-09-12T17:00:00Z")
    private Instant votingDeadline;

    public CreateProposalRequest() {
    }

    public CreateProposalRequest(Long groupId, String title, String description, ProposalType proposalType, Instant votingDeadline) {
        this.groupId = groupId;
        this.title = title;
        this.description = description;
        this.proposalType = proposalType;
        this.votingDeadline = votingDeadline;
    }

    public Long getGroupId() {
        return groupId;
    }

    public void setGroupId(Long groupId) {
        this.groupId = groupId;
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

    public Instant getVotingDeadline() {
        return votingDeadline;
    }

    public void setVotingDeadline(Instant votingDeadline) {
        this.votingDeadline = votingDeadline;
    }
}
