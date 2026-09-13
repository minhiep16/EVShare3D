package com.example.evshare.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Payload for formulating a staff proposed resolution recommendation")
public class ProposeResolutionRequest {

    @NotBlank(message = "Proposed resolution cannot be blank")
    @Size(max = 5000, message = "Proposed resolution cannot exceed 5000 characters")
    @Schema(description = "Staff formulated proposed resolution terms", example = "Propose respondent reimburses 350,000 VND repair deductible, remainder covered by shared reserve.")
    private String proposedResolution;

    public ProposeResolutionRequest() {
    }

    public ProposeResolutionRequest(String proposedResolution) {
        this.proposedResolution = proposedResolution;
    }

    public String getProposedResolution() {
        return proposedResolution;
    }

    public void setProposedResolution(String proposedResolution) {
        this.proposedResolution = proposedResolution;
    }
}
