package com.example.evshare.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Dispute evidence attachment payload (photos, inspection records, or 3D coordinate annotations)")
public class CreateDisputeEvidenceRequest {

    @NotBlank(message = "Evidence file URL is required")
    @Size(max = 255, message = "File URL cannot exceed 255 characters")
    @Schema(description = "URL or object storage URI of the evidence media file", example = "https://storage.evshare.io/evidences/defect_scratch_01.jpg")
    private String fileUrl;

    @Schema(description = "JSON coordinate string pinpointing damage on the 3D vehicle mesh model", example = "{\"x\": 1.25, \"y\": 0.85, \"z\": -0.42, \"part\": \"front_bumper\"}")
    private String mesh3dDefectCoordinates;

    @Size(max = 255, message = "Evidence description cannot exceed 255 characters")
    @Schema(description = "Detailed annotation or context for this piece of evidence", example = "Deep scratch on front bumper post check-out")
    private String description;

    public CreateDisputeEvidenceRequest() {
    }

    public CreateDisputeEvidenceRequest(String fileUrl, String mesh3dDefectCoordinates, String description) {
        this.fileUrl = fileUrl;
        this.mesh3dDefectCoordinates = mesh3dDefectCoordinates;
        this.description = description;
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
}
