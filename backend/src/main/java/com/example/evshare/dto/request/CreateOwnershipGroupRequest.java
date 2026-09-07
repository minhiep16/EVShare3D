package com.example.evshare.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.List;

public class CreateOwnershipGroupRequest {

    @NotBlank(message = "Group name is required")
    @Size(min = 2, max = 100, message = "Group name must be between 2 and 100 characters")
    private String groupName;

    @NotNull(message = "Vehicle ID is required")
    private Long vehicleId;

    private LocalDate formationDate;

    private List<Long> memberUserIds;

    public CreateOwnershipGroupRequest() {
    }

    public CreateOwnershipGroupRequest(String groupName, Long vehicleId) {
        this.groupName = groupName;
        this.vehicleId = vehicleId;
    }

    public CreateOwnershipGroupRequest(String groupName, Long vehicleId, LocalDate formationDate, List<Long> memberUserIds) {
        this.groupName = groupName;
        this.vehicleId = vehicleId;
        this.formationDate = formationDate;
        this.memberUserIds = memberUserIds;
    }

    public String getGroupName() {
        return groupName;
    }

    public void setGroupName(String groupName) {
        this.groupName = groupName;
    }

    public Long getVehicleId() {
        return vehicleId;
    }

    public void setVehicleId(Long vehicleId) {
        this.vehicleId = vehicleId;
    }

    public LocalDate getFormationDate() {
        return formationDate;
    }

    public void setFormationDate(LocalDate formationDate) {
        this.formationDate = formationDate;
    }

    public List<Long> getMemberUserIds() {
        return memberUserIds;
    }

    public void setMemberUserIds(List<Long> memberUserIds) {
        this.memberUserIds = memberUserIds;
    }
}
