package com.example.evshare.entity;

import com.example.evshare.entity.enums.ServiceStatus;
import com.example.evshare.entity.enums.ServiceType;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "vehicle_services")
public class VehicleService {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vehicle_id", nullable = false)
    private Vehicle vehicle;

    @Enumerated(EnumType.STRING)
    @org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.VARCHAR)
    @Column(name = "service_type", nullable = false, length = 40)
    private ServiceType serviceType;

    @Column(name = "description", nullable = false, columnDefinition = "TEXT")
    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "technician_user_id", nullable = false)
    private User technicianUser;

    @Column(name = "cost_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal costAmount = BigDecimal.ZERO;

    @Column(name = "odometer_at_service", nullable = false, precision = 10, scale = 2)
    private BigDecimal odometerAtService;

    @Enumerated(EnumType.STRING)
    @org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.VARCHAR)
    @Column(name = "service_status", nullable = false, length = 30)
    private ServiceStatus serviceStatus = ServiceStatus.PENDING;

    @Column(name = "started_at")
    private Instant startedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    public VehicleService() {
    }

    public VehicleService(Long id, Vehicle vehicle, ServiceType serviceType, String description, User technicianUser, BigDecimal costAmount, BigDecimal odometerAtService, ServiceStatus serviceStatus, Instant startedAt, Instant completedAt) {
        this.id = id;
        this.vehicle = vehicle;
        this.serviceType = serviceType;
        this.description = description;
        this.technicianUser = technicianUser;
        this.costAmount = costAmount != null ? costAmount : BigDecimal.ZERO;
        this.odometerAtService = odometerAtService;
        this.serviceStatus = serviceStatus != null ? serviceStatus : ServiceStatus.PENDING;
        this.startedAt = startedAt;
        this.completedAt = completedAt;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Vehicle getVehicle() {
        return vehicle;
    }

    public void setVehicle(Vehicle vehicle) {
        this.vehicle = vehicle;
    }

    public ServiceType getServiceType() {
        return serviceType;
    }

    public void setServiceType(ServiceType serviceType) {
        this.serviceType = serviceType;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public User getTechnicianUser() {
        return technicianUser;
    }

    public void setTechnicianUser(User technicianUser) {
        this.technicianUser = technicianUser;
    }

    public BigDecimal getCostAmount() {
        return costAmount;
    }

    public void setCostAmount(BigDecimal costAmount) {
        this.costAmount = costAmount;
    }

    public BigDecimal getOdometerAtService() {
        return odometerAtService;
    }

    public void setOdometerAtService(BigDecimal odometerAtService) {
        this.odometerAtService = odometerAtService;
    }

    public ServiceStatus getServiceStatus() {
        return serviceStatus;
    }

    public void setServiceStatus(ServiceStatus serviceStatus) {
        this.serviceStatus = serviceStatus;
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(Instant startedAt) {
        this.startedAt = startedAt;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(Instant completedAt) {
        this.completedAt = completedAt;
    }
}
