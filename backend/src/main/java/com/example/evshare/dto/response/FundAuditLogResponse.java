package com.example.evshare.dto.response;

import com.example.evshare.entity.AuditLog;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

@Schema(description = "Audit trail entry for a SharedFund balance-changing or configuration event")
public class FundAuditLogResponse {

    @Schema(description = "Audit log ID", example = "201")
    private Long id;

    @Schema(description = "Target shared fund ID", example = "5")
    private Long fundId;

    @Schema(description = "Audit action name", example = "SHARED_FUND_CONTRIBUTION")
    private String action;

    @Schema(description = "Actor user ID who performed the action", example = "10")
    private Long actorId;

    @Schema(description = "Actor full name", example = "Nguyen Van A")
    private String actorName;

    @Schema(description = "JSON snapshot of state changes", example = "{\"oldBalance\": 10000000.00, \"newBalance\": 12000000.00}")
    private String detailsJson;

    @Schema(description = "Client IP address", example = "127.0.0.1")
    private String ipAddress;

    @Schema(description = "Timestamp when the audit event occurred", example = "2026-09-08T15:30:00Z")
    private Instant createdAt;

    public FundAuditLogResponse() {
    }

    public FundAuditLogResponse(Long id, Long fundId, String action, Long actorId, String actorName,
                                String detailsJson, String ipAddress, Instant createdAt) {
        this.id = id;
        this.fundId = fundId;
        this.action = action;
        this.actorId = actorId;
        this.actorName = actorName;
        this.detailsJson = detailsJson;
        this.ipAddress = ipAddress;
        this.createdAt = createdAt;
    }

    public static FundAuditLogResponse fromEntity(AuditLog log) {
        if (log == null) {
            return null;
        }
        return new FundAuditLogResponse(
                log.getId(),
                log.getEntityId(),
                log.getAction(),
                log.getUser() != null ? log.getUser().getId() : null,
                log.getUser() != null ? log.getUser().getFullName() : "SYSTEM",
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

    public Long getFundId() {
        return fundId;
    }

    public void setFundId(Long fundId) {
        this.fundId = fundId;
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
