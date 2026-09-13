package com.example.evshare.dto.response;

import com.example.evshare.entity.AuditLog;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

@Schema(description = "Audit trail entry representing an immutable proposal lifecycle state transition event")
public class ProposalAuditLogResponse {

    @Schema(description = "Audit log entry ID", example = "501")
    private Long id;

    @Schema(description = "Proposal entity ID", example = "10")
    private Long proposalId;

    @Schema(description = "Audit action name", example = "PROPOSAL_STATE_TRANSITION")
    private String action;

    @Schema(description = "Actor user ID who initiated or approved the transition", example = "5")
    private Long actorId;

    @Schema(description = "Actor full name", example = "Nguyen Van A")
    private String actorName;

    @Schema(description = "JSON snapshot of state prior to transition")
    private String oldStateJson;

    @Schema(description = "JSON snapshot of state after transition")
    private String newStateJson;

    @Schema(description = "Timestamp when the transition was executed and audited", example = "2026-09-09T18:00:00Z")
    private Instant createdAt;

    public ProposalAuditLogResponse() {
    }

    public ProposalAuditLogResponse(Long id, Long proposalId, String action, Long actorId, String actorName,
                                   String oldStateJson, String newStateJson, Instant createdAt) {
        this.id = id;
        this.proposalId = proposalId;
        this.action = action;
        this.actorId = actorId;
        this.actorName = actorName;
        this.oldStateJson = oldStateJson;
        this.newStateJson = newStateJson;
        this.createdAt = createdAt;
    }

    public static ProposalAuditLogResponse fromEntity(AuditLog auditLog) {
        if (auditLog == null) {
            return null;
        }
        return new ProposalAuditLogResponse(
                auditLog.getId(),
                auditLog.getEntityId(),
                auditLog.getAction(),
                auditLog.getUser() != null ? auditLog.getUser().getId() : null,
                auditLog.getUser() != null ? auditLog.getUser().getFullName() : "SYSTEM",
                auditLog.getOldStateJson(),
                auditLog.getNewStateJson(),
                auditLog.getCreatedAt()
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

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public Long getActorId() {
        return actorId;
    }

    public void setActorId(Long actorId) {
        this.actorId = actorId;
    }

    public String getActorName() {
        return actorName;
    }

    public void setActorName(String actorName) {
        this.actorName = actorName;
    }

    public String getOldStateJson() {
        return oldStateJson;
    }

    public void setOldStateJson(String oldStateJson) {
        this.oldStateJson = oldStateJson;
    }

    public String getNewStateJson() {
        return newStateJson;
    }

    public void setNewStateJson(String newStateJson) {
        this.newStateJson = newStateJson;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
