package com.example.evshare.dto.response;

import com.example.evshare.entity.AuditLog;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

@Schema(description = "Audit trail entry for an expense mutation or creation event")
public class ExpenseAuditLogResponse {

    @Schema(description = "Audit log ID", example = "101")
    private Long id;

    @Schema(description = "Target expense ID", example = "42")
    private Long expenseId;

    @Schema(description = "Audit action name", example = "EXPENSE_CREATED")
    private String action;

    @Schema(description = "Actor user ID who executed the action", example = "3")
    private Long actorId;

    @Schema(description = "Actor full name", example = "Field Tech Tran")
    private String actorName;

    @Schema(description = "JSON snapshot of state changes", example = "{\"amount\": 250000.00, \"category\": \"CHARGING\"}")
    private String detailsJson;

    @Schema(description = "Client IP address", example = "192.168.1.100")
    private String ipAddress;

    @Schema(description = "Timestamp when the audit log was recorded", example = "2026-09-08T10:15:30Z")
    private Instant createdAt;

    public ExpenseAuditLogResponse() {
    }

    public ExpenseAuditLogResponse(Long id, Long expenseId, String action, Long actorId, String actorName,
                                   String detailsJson, String ipAddress, Instant createdAt) {
        this.id = id;
        this.expenseId = expenseId;
        this.action = action;
        this.actorId = actorId;
        this.actorName = actorName;
        this.detailsJson = detailsJson;
        this.ipAddress = ipAddress;
        this.createdAt = createdAt;
    }

    public static ExpenseAuditLogResponse fromEntity(AuditLog log) {
        if (log == null) {
            return null;
        }
        return new ExpenseAuditLogResponse(
                log.getId(),
                log.getEntityId(),
                log.getAction(),
                log.getUser() != null ? log.getUser().getId() : null,
                log.getUser() != null ? log.getUser().getFullName() : null,
                log.getNewStateJson() != null ? log.getNewStateJson() : log.getOldStateJson(),
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

    public Long getExpenseId() {
        return expenseId;
    }

    public void setExpenseId(Long expenseId) {
        this.expenseId = expenseId;
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

    public String getDetailsJson() {
        return detailsJson;
    }

    public void setDetailsJson(String detailsJson) {
        this.detailsJson = detailsJson;
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
