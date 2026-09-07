package com.example.evshare.dto.response;

import com.example.evshare.entity.OwnershipGroup;
import com.example.evshare.entity.OwnershipShare;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class OwnershipGroupResponse {

    private Long id;
    private String groupName;
    private Long vehicleId;
    private String vehicleModelName;
    private String vehicleLicensePlate;
    private String vehicleManufacturer;
    private LocalDate formationDate;
    private Boolean isActive;
    private int memberCount;
    private List<OwnershipShareResponse> memberShares;

    public OwnershipGroupResponse() {
    }

    public OwnershipGroupResponse(Long id, String groupName, Long vehicleId, String vehicleModelName,
                                  String vehicleLicensePlate, String vehicleManufacturer,
                                  LocalDate formationDate, Boolean isActive, int memberCount,
                                  List<OwnershipShareResponse> memberShares) {
        this.id = id;
        this.groupName = groupName;
        this.vehicleId = vehicleId;
        this.vehicleModelName = vehicleModelName;
        this.vehicleLicensePlate = vehicleLicensePlate;
        this.vehicleManufacturer = vehicleManufacturer;
        this.formationDate = formationDate;
        this.isActive = isActive;
        this.memberCount = memberCount;
        this.memberShares = memberShares != null ? memberShares : Collections.emptyList();
    }

    public static OwnershipGroupResponse fromEntity(OwnershipGroup group) {
        return fromEntity(group, Collections.emptyList());
    }

    public static OwnershipGroupResponse fromEntity(OwnershipGroup group, List<OwnershipShare> shares) {
        if (group == null) {
            return null;
        }
        List<OwnershipShareResponse> shareResponses = shares != null
                ? shares.stream().map(OwnershipShareResponse::fromEntity).collect(Collectors.toList())
                : Collections.emptyList();

        return new OwnershipGroupResponse(
                group.getId(),
                group.getGroupName(),
                group.getVehicle() != null ? group.getVehicle().getId() : null,
                group.getVehicle() != null ? group.getVehicle().getModelName() : null,
                group.getVehicle() != null ? group.getVehicle().getLicensePlate() : null,
                group.getVehicle() != null ? group.getVehicle().getManufacturer() : null,
                group.getFormationDate(),
                group.getIsActive(),
                shareResponses.size(),
                shareResponses
        );
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public String getVehicleModelName() {
        return vehicleModelName;
    }

    public void setVehicleModelName(String vehicleModelName) {
        this.vehicleModelName = vehicleModelName;
    }

    public String getVehicleLicensePlate() {
        return vehicleLicensePlate;
    }

    public void setVehicleLicensePlate(String vehicleLicensePlate) {
        this.vehicleLicensePlate = vehicleLicensePlate;
    }

    public String getVehicleManufacturer() {
        return vehicleManufacturer;
    }

    public void setVehicleManufacturer(String vehicleManufacturer) {
        this.vehicleManufacturer = vehicleManufacturer;
    }

    public LocalDate getFormationDate() {
        return formationDate;
    }

    public void setFormationDate(LocalDate formationDate) {
        this.formationDate = formationDate;
    }

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }

    public int getMemberCount() {
        return memberCount;
    }

    public void setMemberCount(int memberCount) {
        this.memberCount = memberCount;
    }

    public List<OwnershipShareResponse> getMemberShares() {
        return memberShares;
    }

    public void setMemberShares(List<OwnershipShareResponse> memberShares) {
        this.memberShares = memberShares;
    }
}
