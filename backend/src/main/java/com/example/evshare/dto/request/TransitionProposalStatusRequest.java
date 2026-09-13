package com.example.evshare.dto.request;

import com.example.evshare.entity.enums.ProposalStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Schema(description = "Request payload to transition a proposal lifecycle status")
public class TransitionProposalStatusRequest {

    @NotNull(message = "Target status is mandatory")
    @Schema(description = "Target proposal lifecycle status (PASSED, REJECTED, EXPIRED)", example = "PASSED")
    private ProposalStatus targetStatus;

    @Size(max = 255, message = "Reason cannot exceed 255 characters")
    @Schema(description = "Formal justification or context for the state transition", example = "Voting concluded; quorum and passing threshold satisfied")
    private String reason;

    public TransitionProposalStatusRequest() {
    }

    public TransitionProposalStatusRequest(ProposalStatus targetStatus, String reason) {
        this.targetStatus = targetStatus;
        this.reason = reason;
    }

    public ProposalStatus getTargetStatus() {
        return targetStatus;
    }

    public void setTargetStatus(ProposalStatus targetStatus) {
        this.targetStatus = targetStatus;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}
