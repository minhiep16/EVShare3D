package com.example.evshare.dto.response;

import com.example.evshare.entity.Vehicle;
import com.example.evshare.entity.enums.VehicleStatus;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.math.BigDecimal;
import java.time.Instant;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class VehicleResponse {

    private Long id;
    private String vin;
    private String licensePlate;
    private String modelName;
    private String manufacturer;
    private String model3dAssetPath;
    private VehicleStatus status;
    private Integer batteryLevel;
    private BigDecimal odometerKm;
    private String stallLocationCode;
    private Instant createdAt;
    private Instant updatedAt;

    public VehicleResponse() {
    }

    public VehicleResponse(Long id, String vin, String licensePlate, String modelName, String manufacturer,
                           String model3dAssetPath, VehicleStatus status, Integer batteryLevel,
                           BigDecimal odometerKm, String stallLocationCode, Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.vin = vin;
        this.licensePlate = licensePlate;
        this.modelName = modelName;
        this.manufacturer = manufacturer;
        this.model3dAssetPath = model3dAssetPath;
        this.status = status;
        this.batteryLevel = batteryLevel;
        this.odometerKm = odometerKm;
        this.stallLocationCode = stallLocationCode;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static VehicleResponse fromEntity(Vehicle vehicle) {
        if (vehicle == null) {
            return null;
        }
        return VehicleResponse.builder()
                .id(vehicle.getId())
                .vin(vehicle.getVin())
                .licensePlate(vehicle.getLicensePlate())
                .modelName(vehicle.getModelName())
                .manufacturer(vehicle.getManufacturer())
                .model3dAssetPath(vehicle.getModel3dAssetPath())
                .status(vehicle.getStatus())
                .batteryLevel(vehicle.getBatteryLevel())
                .odometerKm(vehicle.getOdometerKm())
                .stallLocationCode(vehicle.getStallLocationCode())
                .createdAt(vehicle.getCreatedAt())
                .updatedAt(vehicle.getUpdatedAt())
                .build();
    }

    public static VehicleResponseBuilder builder() {
        return new VehicleResponseBuilder();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public String getManufacturer() {
        return manufacturer;
    }

    public void setManufacturer(String manufacturer) {
        this.manufacturer = manufacturer;
    }

    public String getModel3dAssetPath() {
        return model3dAssetPath;
    }

    public void setModel3dAssetPath(String model3dAssetPath) {
        this.model3dAssetPath = model3dAssetPath;
    }

    public VehicleStatus getStatus() {
        return status;
    }

    public void setStatus(VehicleStatus status) {
        this.status = status;
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

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    public static class VehicleResponseBuilder {
        private Long id;
        private String vin;
        private String licensePlate;
        private String modelName;
        private String manufacturer;
        private String model3dAssetPath;
        private VehicleStatus status;
        private Integer batteryLevel;
        private BigDecimal odometerKm;
        private String stallLocationCode;
        private Instant createdAt;
        private Instant updatedAt;

        VehicleResponseBuilder() {
        }

        public VehicleResponseBuilder id(Long id) {
            this.id = id;
            return this;
        }

        public VehicleResponseBuilder vin(String vin) {
            this.vin = vin;
            return this;
        }

        public VehicleResponseBuilder licensePlate(String licensePlate) {
            this.licensePlate = licensePlate;
            return this;
        }

        public VehicleResponseBuilder modelName(String modelName) {
            this.modelName = modelName;
            return this;
        }

        public VehicleResponseBuilder manufacturer(String manufacturer) {
            this.manufacturer = manufacturer;
            return this;
        }

        public VehicleResponseBuilder model3dAssetPath(String model3dAssetPath) {
            this.model3dAssetPath = model3dAssetPath;
            return this;
        }

        public VehicleResponseBuilder status(VehicleStatus status) {
            this.status = status;
            return this;
        }

        public VehicleResponseBuilder batteryLevel(Integer batteryLevel) {
            this.batteryLevel = batteryLevel;
            return this;
        }

        public VehicleResponseBuilder odometerKm(BigDecimal odometerKm) {
            this.odometerKm = odometerKm;
            return this;
        }

        public VehicleResponseBuilder stallLocationCode(String stallLocationCode) {
            this.stallLocationCode = stallLocationCode;
            return this;
        }

        public VehicleResponseBuilder createdAt(Instant createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public VehicleResponseBuilder updatedAt(Instant updatedAt) {
            this.updatedAt = updatedAt;
            return this;
        }

        public VehicleResponse build() {
            return new VehicleResponse(id, vin, licensePlate, modelName, manufacturer,
                    model3dAssetPath, status, batteryLevel, odometerKm, stallLocationCode, createdAt, updatedAt);
        }
    }
}
