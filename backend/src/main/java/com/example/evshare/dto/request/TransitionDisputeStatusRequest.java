package com.example.evshare.dto.request;

import com.example.evshare.entity.enums.DisputeStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Schema(description = "Dispute lifecycle status transition payload")
public class TransitionDisputeStatusRequest {

    @NotNull(message = "Target dispute status is required")
    @Schema(description = "Target lifecycle status (UNDER_REVIEW, RESOLVED, ESCALATED)", example = "UNDER_REVIEW")
    private DisputeStatus targetStatus;

    @Size(max = 1000, message = "Transition reason cannot exceed 1000 characters")
    @Schema(description = "Explanation or context for the state transition", example = "Staff mediation initiated between co-owners")
    private String reason;

    @Size(max = 5000, message = "Resolution summary cannot exceed 5000 characters")
    @Schema(description = "Summary of terms, penalties, or compensation when resolving dispute", example = "Respondent agreed to pay 500,000 VND from shared fund deposit.")
    private String resolutionSummary;

    public TransitionDisputeStatusRequest() {
    }

    public TransitionDisputeStatusRequest(DisputeStatus targetStatus, String reason, String resolutionSummary) {
        this.targetStatus = targetStatus;
        this.reason = reason;
        this.resolutionSummary = resolutionSummary;
    }

    public DisputeStatus getTargetStatus() {
        return targetStatus;
    }

    public void setTargetStatus(DisputeStatus targetStatus) {
        this.targetStatus = targetStatus;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public String getResolutionSummary() {
        return resolutionSummary;
    }

    public void setResolutionSummary(String resolutionSummary) {
        this.resolutionSummary = resolutionSummary;
    }
}
