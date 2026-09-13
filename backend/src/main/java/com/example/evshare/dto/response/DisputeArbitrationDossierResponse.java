package com.example.evshare.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "Comprehensive dispute arbitration dossier for administrator evidence and mediation review")
public class DisputeArbitrationDossierResponse {

    @Schema(description = "Core dispute grievance details and current status")
    private DisputeResponse dispute;

    @Schema(description = "All attached immutable evidence items including 3D defect coordinate annotations")
    private List<DisputeEvidenceResponse> evidences;

    @Schema(description = "Chronological audit trail of all lifecycle transitions, evidence uploads, and mediation events")
    private List<DisputeAuditLogResponse> history;

    @Schema(description = "Total count of attached evidence records")
    private int totalEvidences;

    @Schema(description = "Indicates whether staff mediation review notes have been recorded")
    private boolean hasMediationNotes;

    @Schema(description = "Indicates whether a staff proposed resolution has been recorded")
    private boolean hasProposedResolution;

    public DisputeArbitrationDossierResponse() {
    }

    public DisputeArbitrationDossierResponse(DisputeResponse dispute, List<DisputeEvidenceResponse> evidences, List<DisputeAuditLogResponse> history) {
        this.dispute = dispute;
        this.evidences = evidences;
        this.history = history;
        this.totalEvidences = evidences != null ? evidences.size() : 0;
        this.hasMediationNotes = dispute != null && dispute.getMediationNotes() != null && !dispute.getMediationNotes().isBlank();
        this.hasProposedResolution = dispute != null && dispute.getProposedResolution() != null && !dispute.getProposedResolution().isBlank();
    }

    public DisputeResponse getDispute() {
        return dispute;
    }

    public void setDispute(DisputeResponse dispute) {
        this.dispute = dispute;
    }

    public List<DisputeEvidenceResponse> getEvidences() {
        return evidences;
    }

    public void setEvidences(List<DisputeEvidenceResponse> evidences) {
        this.evidences = evidences;
        this.totalEvidences = evidences != null ? evidences.size() : 0;
    }

    public List<DisputeAuditLogResponse> getHistory() {
        return history;
    }

    public void setHistory(List<DisputeAuditLogResponse> history) {
        this.history = history;
    }

    public int getTotalEvidences() {
        return totalEvidences;
    }

    public void setTotalEvidences(int totalEvidences) {
        this.totalEvidences = totalEvidences;
    }

    public boolean isHasMediationNotes() {
        return hasMediationNotes;
    }

    public void setHasMediationNotes(boolean hasMediationNotes) {
        this.hasMediationNotes = hasMediationNotes;
    }

    public boolean isHasProposedResolution() {
        return hasProposedResolution;
    }

    public void setHasProposedResolution(boolean hasProposedResolution) {
        this.hasProposedResolution = hasProposedResolution;
    }
}
