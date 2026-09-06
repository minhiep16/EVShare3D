package com.example.evshare.entity;

import com.example.evshare.entity.enums.VehicleStatus;
import jakarta.persistence.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "vehicles")
@EntityListeners(AuditingEntityListener.class)
public class Vehicle {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "vin", nullable = false, unique = true, length = 50)
    private String vin;

    @Column(name = "license_plate", nullable = false, unique = true, length = 20)
    private String licensePlate;

    @Column(name = "model_name", nullable = false, length = 100)
    private String modelName;

    @Column(name = "manufacturer", nullable = false, length = 50)
    private String manufacturer;

    @Column(name = "model_3d_asset_path", nullable = false, length = 255)
    private String model3dAssetPath;

    @Enumerated(EnumType.STRING)
    @org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.VARCHAR)
    @Column(name = "status", nullable = false, length = 30)
    private VehicleStatus status = VehicleStatus.AVAILABLE;

    @Column(name = "battery_level", nullable = false)
    private Integer batteryLevel = 100;

    @Column(name = "odometer_km", nullable = false, precision = 10, scale = 2)
    private BigDecimal odometerKm = BigDecimal.ZERO;

    @Column(name = "stall_location_code", nullable = false, length = 30)
    private String stallLocationCode = "BAY-01";

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    public Vehicle() {
    }

    public Vehicle(Long id, String vin, String licensePlate, String modelName, String manufacturer, String model3dAssetPath, VehicleStatus status, Integer batteryLevel, BigDecimal odometerKm, String stallLocationCode, Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.vin = vin;
        this.licensePlate = licensePlate;
        this.modelName = modelName;
        this.manufacturer = manufacturer;
        this.model3dAssetPath = model3dAssetPath;
        this.status = status != null ? status : VehicleStatus.AVAILABLE;
        this.batteryLevel = batteryLevel != null ? batteryLevel : 100;
        this.odometerKm = odometerKm != null ? odometerKm : BigDecimal.ZERO;
        this.stallLocationCode = stallLocationCode != null ? stallLocationCode : "BAY-01";
        this.createdAt = createdAt != null ? createdAt : Instant.now();
        this.updatedAt = updatedAt != null ? updatedAt : Instant.now();
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
}
