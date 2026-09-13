package com.example.evshare.dto.response;

import com.example.evshare.entity.DisputeEvidence;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

@Schema(description = "Dispute evidence attachment details")
public class DisputeEvidenceResponse {

    @Schema(description = "Evidence ID", example = "1")
    private Long id;

    @Schema(description = "Associated dispute ID", example = "10")
    private Long disputeId;

    @Schema(description = "ID of the user who uploaded the evidence", example = "5")
    private Long uploadedByUserId;

    @Schema(description = "Full name of the uploader", example = "Alice Owner")
    private String uploadedByUserName;

    @Schema(description = "File URL or asset storage path", example = "https://storage.evshare.io/evidences/defect_01.jpg")
    private String fileUrl;

    @Schema(description = "3D vehicle model mesh coordinates for defect visualization", example = "{\"x\": 1.25, \"y\": 0.85, \"z\": -0.42}")
    private String mesh3dDefectCoordinates;

    @Schema(description = "Annotation or description of the evidence item", example = "Front bumper damage post check-out")
    private String description;

    @Schema(description = "Timestamp when evidence was uploaded")
    private Instant createdAt;

    public DisputeEvidenceResponse() {
    }

    public DisputeEvidenceResponse(Long id, Long disputeId, Long uploadedByUserId, String uploadedByUserName, String fileUrl, String mesh3dDefectCoordinates, String description, Instant createdAt) {
        this.id = id;
        this.disputeId = disputeId;
        this.uploadedByUserId = uploadedByUserId;
        this.uploadedByUserName = uploadedByUserName;
        this.fileUrl = fileUrl;
        this.mesh3dDefectCoordinates = mesh3dDefectCoordinates;
        this.description = description;
        this.createdAt = createdAt;
    }

    public static DisputeEvidenceResponse fromEntity(DisputeEvidence evidence) {
        if (evidence == null) {
            return null;
        }
        return new DisputeEvidenceResponse(
                evidence.getId(),
                evidence.getDispute() != null ? evidence.getDispute().getId() : null,
                evidence.getUploadedByUser() != null ? evidence.getUploadedByUser().getId() : null,
                evidence.getUploadedByUser() != null ? evidence.getUploadedByUser().getFullName() : null,
                evidence.getFileUrl(),
                evidence.getMesh3dDefectCoordinates(),
                evidence.getDescription(),
                evidence.getCreatedAt()
        );
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getDisputeId() {
        return disputeId;
    }

    public void setDisputeId(Long disputeId) {
        this.disputeId = disputeId;
    }

    public Long getUploadedByUserId() {
        return uploadedByUserId;
    }

    public void setUploadedByUserId(Long uploadedByUserId) {
        this.uploadedByUserId = uploadedByUserId;
    }

    public String getUploadedByUserName() {
        return uploadedByUserName;
    }

    public void setUploadedByUserName(String uploadedByUserName) {
        this.uploadedByUserName = uploadedByUserName;
    }

    public String getFileUrl() {
        return fileUrl;
    }

    public void setFileUrl(String fileUrl) {
        this.fileUrl = fileUrl;
    }

    public String getMesh3dDefectCoordinates() {
        return mesh3dDefectCoordinates;
    }

    public void setMesh3dDefectCoordinates(String mesh3dDefectCoordinates) {
        this.mesh3dDefectCoordinates = mesh3dDefectCoordinates;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
