package com.example.evshare.dto.request;

import com.example.evshare.entity.enums.VehicleStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class UpdateVehicleStatusRequest {

    @NotNull(message = "Vehicle status is required")
    private VehicleStatus status;

    @Size(max = 255, message = "Reason cannot exceed 255 characters")
    private String reason;

    public UpdateVehicleStatusRequest() {
    }

    public UpdateVehicleStatusRequest(VehicleStatus status) {
        this.status = status;
    }

    public UpdateVehicleStatusRequest(VehicleStatus status, String reason) {
        this.status = status;
        this.reason = reason;
    }

    public VehicleStatus getStatus() {
        return status;
    }

    public void setStatus(VehicleStatus status) {
        this.status = status;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}
