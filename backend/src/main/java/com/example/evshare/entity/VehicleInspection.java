package com.example.evshare.entity;

import com.example.evshare.entity.enums.InspectionType;
import jakarta.persistence.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;

@Entity
@Table(name = "vehicle_inspections")
@EntityListeners(AuditingEntityListener.class)
public class VehicleInspection {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usage_session_id", nullable = false)
    private UsageSession usageSession;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "inspector_user_id", nullable = false)
    private User inspectorUser;

    @Enumerated(EnumType.STRING)
    @org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.VARCHAR)
    @Column(name = "inspection_type", nullable = false, length = 20)
    private InspectionType inspectionType;

    @Column(name = "condition_mesh_flags", columnDefinition = "json")
    private String conditionMeshFlags;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    public VehicleInspection() {
    }

    public VehicleInspection(Long id, UsageSession usageSession, User inspectorUser, InspectionType inspectionType, String conditionMeshFlags, String notes, Instant createdAt) {
        this.id = id;
        this.usageSession = usageSession;
        this.inspectorUser = inspectorUser;
        this.inspectionType = inspectionType;
        this.conditionMeshFlags = conditionMeshFlags;
        this.notes = notes;
        this.createdAt = createdAt != null ? createdAt : Instant.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public UsageSession getUsageSession() {
        return usageSession;
    }

    public void setUsageSession(UsageSession usageSession) {
        this.usageSession = usageSession;
    }

    public User getInspectorUser() {
        return inspectorUser;
    }

    public void setInspectorUser(User inspectorUser) {
        this.inspectorUser = inspectorUser;
    }

    public InspectionType getInspectionType() {
        return inspectionType;
    }

    public void setInspectionType(InspectionType inspectionType) {
        this.inspectionType = inspectionType;
    }

    public String getConditionMeshFlags() {
        return conditionMeshFlags;
    }

    public void setConditionMeshFlags(String conditionMeshFlags) {
        this.conditionMeshFlags = conditionMeshFlags;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
