package com.example.evshare.dto.response;

import com.example.evshare.entity.AuditLog;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

@Schema(description = "Audit trail entry representing an immutable payment lifecycle state transition event")
public class PaymentAuditLogResponse {

    @Schema(description = "Audit log entry ID", example = "301")
    private Long id;

    @Schema(description = "Payment entity ID", example = "15")
    private Long paymentId;

    @Schema(description = "Audit action name", example = "PAYMENT_STATE_TRANSITION")
    private String action;

    @Schema(description = "Actor user ID who initiated or approved the transition", example = "4")
    private Long actorId;

    @Schema(description = "Actor full name", example = "Nguyen Van A")
    private String actorName;

    @Schema(description = "JSON snapshot of state prior to transition")
    private String oldStateJson;

    @Schema(description = "JSON snapshot of state after transition")
    private String newStateJson;

    @Schema(description = "Client or gateway IP address", example = "127.0.0.1")
    private String ipAddress;

    @Schema(description = "Timestamp when the transition was executed and audited", example = "2026-09-09T10:00:00Z")
    private Instant createdAt;

    public PaymentAuditLogResponse() {
    }

    public PaymentAuditLogResponse(Long id, Long paymentId, String action, Long actorId, String actorName,
                                   String oldStateJson, String newStateJson, String ipAddress, Instant createdAt) {
        this.id = id;
        this.paymentId = paymentId;
        this.action = action;
        this.actorId = actorId;
        this.actorName = actorName;
        this.oldStateJson = oldStateJson;
        this.newStateJson = newStateJson;
        this.ipAddress = ipAddress;
        this.createdAt = createdAt;
    }

    public static PaymentAuditLogResponse fromEntity(AuditLog log) {
        if (log == null) {
            return null;
        }
        return new PaymentAuditLogResponse(
                log.getId(),
                log.getEntityId(),
                log.getAction(),
                log.getUser() != null ? log.getUser().getId() : null,
                log.getUser() != null ? log.getUser().getFullName() : "SYSTEM",
                log.getOldStateJson(),
                log.getNewStateJson(),
                log.getIpAddress(),
                log.getCreatedAt()
        );
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getPaymentId() {
        return paymentId;
    }

    public void setPaymentId(Long paymentId) {
        this.paymentId = paymentId;
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

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
