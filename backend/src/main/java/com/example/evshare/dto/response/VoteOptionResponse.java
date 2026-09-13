package com.example.evshare.dto.response;

import com.example.evshare.entity.VoteOption;
import com.example.evshare.entity.enums.VoteOptionKey;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Available voting option on a proposal")
public class VoteOptionResponse {

    @Schema(description = "Vote option ID", example = "1")
    private Long id;

    @Schema(description = "Associated proposal ID", example = "10")
    private Long proposalId;

    @Schema(description = "Canonical vote option key (APPROVE, REJECT, ABSTAIN)", example = "APPROVE")
    private VoteOptionKey optionKey;

    @Schema(description = "Display label for the option", example = "Approve Proposal")
    private String label;

    public VoteOptionResponse() {
    }

    public VoteOptionResponse(Long id, Long proposalId, VoteOptionKey optionKey, String label) {
        this.id = id;
        this.proposalId = proposalId;
        this.optionKey = optionKey;
        this.label = label;
    }

    public static VoteOptionResponse fromEntity(VoteOption option) {
        if (option == null) {
            return null;
        }
        return new VoteOptionResponse(
                option.getId(),
                option.getProposal() != null ? option.getProposal().getId() : null,
                option.getOptionKey(),
                option.getLabel()
        );
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getProposalId() {
        return proposalId;
    }

    public void setProposalId(Long proposalId) {
        this.proposalId = proposalId;
    }

    public VoteOptionKey getOptionKey() {
        return optionKey;
    }

    public void setOptionKey(VoteOptionKey optionKey) {
        this.optionKey = optionKey;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }
}
