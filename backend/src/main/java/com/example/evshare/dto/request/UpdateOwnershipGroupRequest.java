package com.example.evshare.dto.request;

import jakarta.validation.constraints.Size;

public class UpdateOwnershipGroupRequest {

    @Size(min = 2, max = 100, message = "Group name must be between 2 and 100 characters")
    private String groupName;

    private Boolean isActive;

    public UpdateOwnershipGroupRequest() {
    }

    public UpdateOwnershipGroupRequest(String groupName, Boolean isActive) {
        this.groupName = groupName;
        this.isActive = isActive;
    }

    public String getGroupName() {
        return groupName;
    }

    public void setGroupName(String groupName) {
        this.groupName = groupName;
    }

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }
}
