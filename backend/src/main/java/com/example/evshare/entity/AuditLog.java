package com.example.evshare.entity;

import jakarta.persistence.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;

@Entity
@Table(
        name = "audit_logs",
        indexes = {
                @Index(name = "idx_audit_entity", columnList = "entity_name, entity_id"),
                @Index(name = "idx_audit_created", columnList = "created_at")
        }
)
@EntityListeners(AuditingEntityListener.class)
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(name = "action", nullable = false, length = 100)
    private String action;

    @Column(name = "entity_name", nullable = false, length = 50)
    private String entityName;

    @Column(name = "entity_id", nullable = false)
    private Long entityId;

    @Column(name = "old_state_json", columnDefinition = "json")
    private String oldStateJson;

    @Column(name = "new_state_json", columnDefinition = "json")
    private String newStateJson;

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    public AuditLog() {
    }

    public AuditLog(Long id, User user, String action, String entityName, Long entityId, String oldStateJson, String newStateJson, String ipAddress, Instant createdAt) {
        this.id = id;
        this.user = user;
        this.action = action;
        this.entityName = entityName;
        this.entityId = entityId;
        this.oldStateJson = oldStateJson;
        this.newStateJson = newStateJson;
        this.ipAddress = ipAddress;
        this.createdAt = createdAt != null ? createdAt : Instant.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getEntityName() {
        return entityName;
    }

    public void setEntityName(String entityName) {
        this.entityName = entityName;
    }

    public Long getEntityId() {
        return entityId;
    }

    public void setEntityId(Long entityId) {
        this.entityId = entityId;
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
