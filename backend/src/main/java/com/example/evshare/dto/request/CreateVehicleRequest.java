package com.example.evshare.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;

@Schema(description = "Request payload to register a new Digital Twin EV")
public class CreateVehicleRequest {

    @Schema(description = "17-character ISO standard Vehicle Identification Number", example = "VF8US123456789012", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "VIN cannot be blank")
    @Size(min = 17, max = 17, message = "VIN must be exactly 17 characters")
    @Pattern(regexp = "^[A-HJ-NPR-Z0-9]{17}$", message = "VIN must be a valid 17-character alphanumeric string excluding I, O, Q")
    private String vin;

    @Schema(description = "Official registered license plate code", example = "30A-999.88", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "License plate cannot be blank")
    @Size(max = 20, message = "License plate must not exceed 20 characters")
    private String licensePlate;

    @Schema(description = "Commercial model name", example = "VinFast VF 8 Plus", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "Model name cannot be blank")
    @Size(max = 50, message = "Model name must not exceed 50 characters")
    private String modelName;

    @Schema(description = "Automotive manufacturer brand", example = "VinFast", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "Manufacturer cannot be blank")
    @Size(max = 50, message = "Manufacturer must not exceed 50 characters")
    private String manufacturer;

    @Schema(description = "Relative filesystem or storage URI to 3D GLB/glTF model asset",
            example = "models/vehicles/vinfast_vf8.glb", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "3D asset path cannot be blank")
    @Size(max = 255, message = "3D asset path must not exceed 255 characters")
    private String model3dAssetPath;

    @Schema(description = "Initial parking stall / charging bay code", example = "BAY-01")
    @Size(max = 30, message = "Stall location code must not exceed 30 characters")
    private String stallLocationCode = "BAY-01";

    @Schema(description = "Initial battery level percentage (0 to 100)", example = "100")
    @Min(value = 0, message = "Battery level cannot be less than 0%")
    @Max(value = 100, message = "Battery level cannot exceed 100%")
    private Integer batteryLevel = 100;

    @Schema(description = "Initial odometer reading in kilometers", example = "0.00")
    @DecimalMin(value = "0.00", message = "Odometer reading cannot be negative")
    private BigDecimal odometerKm = BigDecimal.ZERO;

    public CreateVehicleRequest() {
    }

    public CreateVehicleRequest(String vin, String licensePlate, String modelName, String manufacturer,
                                String model3dAssetPath, String stallLocationCode, Integer batteryLevel,
                                BigDecimal odometerKm) {
        this.vin = vin;
        this.licensePlate = licensePlate;
        this.modelName = modelName;
        this.manufacturer = manufacturer;
        this.model3dAssetPath = model3dAssetPath;
        if (stallLocationCode != null && !stallLocationCode.isBlank()) {
            this.stallLocationCode = stallLocationCode;
        }
        if (batteryLevel != null) {
            this.batteryLevel = batteryLevel;
        }
        if (odometerKm != null) {
            this.odometerKm = odometerKm;
        }
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

    public String getStallLocationCode() {
        return stallLocationCode;
    }

    public void setStallLocationCode(String stallLocationCode) {
        this.stallLocationCode = stallLocationCode;
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
}
