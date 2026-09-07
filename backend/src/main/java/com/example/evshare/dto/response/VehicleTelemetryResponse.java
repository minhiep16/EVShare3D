package com.example.evshare.dto.response;

import com.example.evshare.entity.Vehicle;
import com.example.evshare.entity.enums.VehicleStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.Instant;

@Schema(description = "Real-time telemetry and state projection for a Digital Twin EV")
public class VehicleTelemetryResponse {

    @Schema(description = "Unique Vehicle ID", example = "1")
    private Long vehicleId;

    @Schema(description = "17-character ISO standard VIN", example = "VF8US123456789012")
    private String vin;

    @Schema(description = "Vehicle license plate", example = "30A-999.88")
    private String licensePlate;

    @Schema(description = "Vehicle model name", example = "VinFast VF 8 Plus")
    private String modelName;

    @Schema(description = "Current battery state of charge (SoC) percentage (0-100%)", example = "85")
    private Integer batteryLevel;

    @Schema(description = "Total distance logged in kilometers", example = "14520.50")
    private BigDecimal odometerKm;

    @Schema(description = "Current physical stall or charging bay identifier", example = "BAY-04")
    private String stallLocationCode;

    @Schema(description = "Current operational status of the vehicle", example = "AVAILABLE")
    private VehicleStatus status;

    @Schema(description = "Timestamp of the last telemetry update in UTC", example = "2026-09-07T14:30:00Z")
    private Instant lastUpdated;

    public VehicleTelemetryResponse() {
    }

    public VehicleTelemetryResponse(Long vehicleId, String vin, String licensePlate, String modelName,
                                  Integer batteryLevel, BigDecimal odometerKm, String stallLocationCode,
                                  VehicleStatus status, Instant lastUpdated) {
        this.vehicleId = vehicleId;
        this.vin = vin;
        this.licensePlate = licensePlate;
        this.modelName = modelName;
        this.batteryLevel = batteryLevel;
        this.odometerKm = odometerKm;
        this.stallLocationCode = stallLocationCode;
        this.status = status;
        this.lastUpdated = lastUpdated;
    }

    public static VehicleTelemetryResponse fromEntity(Vehicle vehicle) {
        if (vehicle == null) {
            return null;
        }
        return new VehicleTelemetryResponse(
                vehicle.getId(),
                vehicle.getVin(),
                vehicle.getLicensePlate(),
                vehicle.getModelName(),
                vehicle.getBatteryLevel(),
                vehicle.getOdometerKm(),
                vehicle.getStallLocationCode(),
                vehicle.getStatus(),
                vehicle.getUpdatedAt() != null ? vehicle.getUpdatedAt() : vehicle.getCreatedAt()
        );
    }

    public Long getVehicleId() {
        return vehicleId;
    }

    public void setVehicleId(Long vehicleId) {
        this.vehicleId = vehicleId;
    }

    public String getVin() {
        return vin;
    }

    public void setVin(String vin) {
        this.vin = vin;
    }

    public String getLicensePlate() {
        return licensePlate;
    }

    public void setLicensePlate(String licensePlate) {
        this.licensePlate = licensePlate;
    }

    public String getModelName() {
        return modelName;
    }

    public void setModelName(String modelName) {
        this.modelName = modelName;
    }

    public Integer getBatteryLevel() {
        return batteryLevel;
    }

    public void setBatteryLevel(Integer batteryLevel) {
        this.batteryLevel = batteryLevel;
    }

    public BigDecimal getOdometerKm() {
        return odometerKm;
    }

    public void setOdometerKm(BigDecimal odometerKm) {
        this.odometerKm = odometerKm;
    }

    public String getStallLocationCode() {
        return stallLocationCode;
    }

    public void setStallLocationCode(String stallLocationCode) {
        this.stallLocationCode = stallLocationCode;
    }

    public VehicleStatus getStatus() {
        return status;
    }

    public void setStatus(VehicleStatus status) {
        this.status = status;
    }

    public Instant getLastUpdated() {
        return lastUpdated;
    }

    public void setLastUpdated(Instant lastUpdated) {
        this.lastUpdated = lastUpdated;
    }
}
