package com.example.evshare.dto.response;

import com.example.evshare.entity.AuditLog;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

@Schema(description = "Audit trail entry representing an immutable dispute lifecycle event or evidence attachment")
public class DisputeAuditLogResponse {

    @Schema(description = "Audit log entry ID", example = "601")
    private Long id;

    @Schema(description = "Entity ID (Dispute ID or DisputeEvidence ID)", example = "10")
    private Long entityId;

    @Schema(description = "Target entity type (Dispute or DisputeEvidence)", example = "DisputeEvidence")
    private String entityName;

    @Schema(description = "Audit action (DISPUTE_CREATED, DISPUTE_STATUS_TRANSITION, DISPUTE_EVIDENCE_ATTACHED)", example = "DISPUTE_EVIDENCE_ATTACHED")
    private String action;

    @Schema(description = "Actor user ID who executed the audited action", example = "5")
    private Long actorId;

    @Schema(description = "Actor full name", example = "Alice Owner")
    private String actorName;

    @Schema(description = "JSON snapshot of state prior to the action")
    private String oldStateJson;

    @Schema(description = "JSON snapshot of state after the action")
    private String newStateJson;

    @Schema(description = "Timestamp when the event occurred and was audited", example = "2026-09-10T02:00:00Z")
    private Instant createdAt;

    public DisputeAuditLogResponse() {
    }

    public DisputeAuditLogResponse(Long id, Long entityId, String entityName, String action, Long actorId, String actorName, String oldStateJson, String newStateJson, Instant createdAt) {
        this.id = id;
        this.entityId = entityId;
        this.entityName = entityName;
        this.action = action;
        this.actorId = actorId;
        this.actorName = actorName;
        this.oldStateJson = oldStateJson;
        this.newStateJson = newStateJson;
        this.createdAt = createdAt;
    }

    public static DisputeAuditLogResponse fromEntity(AuditLog auditLog) {
        if (auditLog == null) {
            return null;
        }
        return new DisputeAuditLogResponse(
                auditLog.getId(),
                auditLog.getEntityId(),
                auditLog.getEntityName(),
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

    public Long getEntityId() {
        return entityId;
    }

    public void setEntityId(Long entityId) {
        this.entityId = entityId;
    }

    public String getEntityName() {
        return entityName;
    }

    public void setEntityName(String entityName) {
        this.entityName = entityName;
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
