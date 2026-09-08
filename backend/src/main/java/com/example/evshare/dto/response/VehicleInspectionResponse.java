package com.example.evshare.dto.response;

import com.example.evshare.entity.VehicleInspection;
import com.example.evshare.entity.enums.InspectionType;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class VehicleInspectionResponse {

    private Long id;
    private Long usageSessionId;
    private Long inspectorUserId;
    private String inspectorName;
    private InspectionType inspectionType;
    private String conditionMeshFlags;
    private String notes;
    private Instant createdAt;

    public VehicleInspectionResponse() {
    }

    public VehicleInspectionResponse(Long id, Long usageSessionId, Long inspectorUserId, String inspectorName, InspectionType inspectionType, String conditionMeshFlags, String notes, Instant createdAt) {
        this.id = id;
        this.usageSessionId = usageSessionId;
        this.inspectorUserId = inspectorUserId;
        this.inspectorName = inspectorName;
        this.inspectionType = inspectionType;
        this.conditionMeshFlags = conditionMeshFlags;
        this.notes = notes;
        this.createdAt = createdAt;
    }

    public static VehicleInspectionResponse fromEntity(VehicleInspection inspection) {
        if (inspection == null) {
            return null;
        }

        return new VehicleInspectionResponse(
                inspection.getId(),
                inspection.getUsageSession() != null ? inspection.getUsageSession().getId() : null,
                inspection.getInspectorUser() != null ? inspection.getInspectorUser().getId() : null,
                inspection.getInspectorUser() != null ? inspection.getInspectorUser().getFullName() : null,
                inspection.getInspectionType(),
                inspection.getConditionMeshFlags(),
                inspection.getNotes(),
                inspection.getCreatedAt()
        );
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getUsageSessionId() {
        return usageSessionId;
    }

    public void setUsageSessionId(Long usageSessionId) {
        this.usageSessionId = usageSessionId;
    }

    public Long getInspectorUserId() {
        return inspectorUserId;
    }

    public void setInspectorUserId(Long inspectorUserId) {
        this.inspectorUserId = inspectorUserId;
    }

    public String getInspectorName() {
        return inspectorName;
    }

    public void setInspectorName(String inspectorName) {
        this.inspectorName = inspectorName;
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
