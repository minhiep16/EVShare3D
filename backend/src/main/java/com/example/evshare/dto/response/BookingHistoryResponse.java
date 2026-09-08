package com.example.evshare.dto.response;

import com.example.evshare.entity.AuditLog;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class BookingHistoryResponse {

    private Long id;
    private Long bookingId;
    private String action;
    private Long actingUserId;
    private String actingUserName;
    private String oldStateJson;
    private String newStateJson;
    private Instant createdAt;

    public BookingHistoryResponse() {
    }

    public BookingHistoryResponse(Long id, Long bookingId, String action, Long actingUserId,
                                  String actingUserName, String oldStateJson, String newStateJson,
                                  Instant createdAt) {
        this.id = id;
        this.bookingId = bookingId;
        this.action = action;
        this.actingUserId = actingUserId;
        this.actingUserName = actingUserName;
        this.oldStateJson = oldStateJson;
        this.newStateJson = newStateJson;
        this.createdAt = createdAt;
    }

    public static BookingHistoryResponse fromAuditLog(AuditLog auditLog) {
        if (auditLog == null) {
            return null;
        }
        return new BookingHistoryResponse(
                auditLog.getId(),
                auditLog.getEntityId(),
                auditLog.getAction(),
                auditLog.getUser() != null ? auditLog.getUser().getId() : null,
                auditLog.getUser() != null ? auditLog.getUser().getFullName() : null,
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

    public Long getBookingId() {
        return bookingId;
    }

    public void setBookingId(Long bookingId) {
        this.bookingId = bookingId;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public Long getActingUserId() {
        return actingUserId;
    }

    public void setActingUserId(Long actingUserId) {
        this.actingUserId = actingUserId;
    }

    public String getActingUserName() {
        return actingUserName;
    }

    public void setActingUserName(String actingUserName) {
        this.actingUserName = actingUserName;
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
