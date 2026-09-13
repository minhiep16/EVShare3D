package com.example.evshare.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.ArrayList;
import java.util.List;

@Schema(description = "Dispute initiation payload with required evidence and grievance details per BR-DIS-01")
public class CreateDisputeRequest {

    @NotNull(message = "Ownership group ID is required")
    @Schema(description = "ID of the syndicate ownership group", example = "1")
    private Long groupId;

    @Schema(description = "Optional ID of the related usage session where issue occurred", example = "105")
    private Long usageSessionId;

    @Schema(description = "Optional ID of the respondent co-owner or driver involved in the dispute", example = "3")
    private Long respondentUserId;

    @NotBlank(message = "Dispute title is required")
    @Size(max = 150, message = "Dispute title cannot exceed 150 characters")
    @Schema(description = "Concise summary of the grievance", example = "Unreported front bumper dent and interior mess")
    private String title;

    @NotBlank(message = "Dispute reason and description is required")
    @Size(max = 5000, message = "Dispute description cannot exceed 5000 characters")
    @Schema(description = "Comprehensive explanation of the incident, damage, or rule violation", example = "Vehicle returned with bumper scratch and 15% battery instead of agreed 80% minimum.")
    private String description;

    @NotEmpty(message = "At least one evidence item is required to file a dispute")
    @Valid
    @Schema(description = "List of supporting evidence items (photos, documents, 3D coordinate annotations)")
    private List<CreateDisputeEvidenceRequest> evidences = new ArrayList<>();

    public CreateDisputeRequest() {
    }

    public CreateDisputeRequest(Long groupId, Long usageSessionId, Long respondentUserId, String title, String description, List<CreateDisputeEvidenceRequest> evidences) {
        this.groupId = groupId;
        this.usageSessionId = usageSessionId;
        this.respondentUserId = respondentUserId;
        this.title = title;
        this.description = description;
        this.evidences = evidences != null ? evidences : new ArrayList<>();
    }

    public Long getGroupId() {
        return groupId;
    }

    public void setGroupId(Long groupId) {
        this.groupId = groupId;
    }

    public Long getUsageSessionId() {
        return usageSessionId;
    }

    public void setUsageSessionId(Long usageSessionId) {
        this.usageSessionId = usageSessionId;
    }

    public Long getRespondentUserId() {
        return respondentUserId;
    }

    public void setRespondentUserId(Long respondentUserId) {
        this.respondentUserId = respondentUserId;
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

    public List<CreateDisputeEvidenceRequest> getEvidences() {
        return evidences;
    }

    public void setEvidences(List<CreateDisputeEvidenceRequest> evidences) {
        this.evidences = evidences;
    }
}
