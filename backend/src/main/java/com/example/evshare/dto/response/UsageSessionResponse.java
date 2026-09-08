package com.example.evshare.dto.response;

import com.example.evshare.entity.Booking;
import com.example.evshare.entity.UsageSession;
import com.example.evshare.entity.Vehicle;
import com.example.evshare.entity.enums.UsageSessionStatus;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class UsageSessionResponse {

    private Long id;
    private Long bookingId;

    private Long userId;
    private String userName;
    private String userEmail;

    private Long vehicleId;
    private String vehicleLicensePlate;
    private String vehicleModel;

    private BigDecimal startOdometer;
    private BigDecimal endOdometer;
    private BigDecimal mileage;

    private Integer startBattery;
    private Integer endBattery;
    private Integer batteryDelta;

    private Instant checkInTime;
    private Instant checkOutTime;
    private Long durationMinutes;

    private UsageSessionStatus status;

    private BigDecimal additionalCost = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
    private Map<String, BigDecimal> costBreakdown = new HashMap<>();
    private List<VehicleInspectionResponse> inspections = new ArrayList<>();

    public UsageSessionResponse() {
    }

    public UsageSessionResponse(Long id, Long bookingId, Long userId, String userName, String userEmail,
                                Long vehicleId, String vehicleLicensePlate, String vehicleModel,
                                BigDecimal startOdometer, BigDecimal endOdometer, BigDecimal mileage,
                                Integer startBattery, Integer endBattery, Integer batteryDelta,
                                Instant checkInTime, Instant checkOutTime, Long durationMinutes,
                                UsageSessionStatus status, BigDecimal additionalCost,
                                Map<String, BigDecimal> costBreakdown,
                                List<VehicleInspectionResponse> inspections) {
        this.id = id;
        this.bookingId = bookingId;
        this.userId = userId;
        this.userName = userName;
        this.userEmail = userEmail;
        this.vehicleId = vehicleId;
        this.vehicleLicensePlate = vehicleLicensePlate;
        this.vehicleModel = vehicleModel;
        this.startOdometer = startOdometer;
        this.endOdometer = endOdometer;
        this.mileage = mileage;
        this.startBattery = startBattery;
        this.endBattery = endBattery;
        this.batteryDelta = batteryDelta;
        this.checkInTime = checkInTime;
        this.checkOutTime = checkOutTime;
        this.durationMinutes = durationMinutes;
        this.status = status;
        this.additionalCost = additionalCost != null ? additionalCost : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        this.costBreakdown = costBreakdown != null ? costBreakdown : new HashMap<>();
        this.inspections = inspections != null ? inspections : new ArrayList<>();
    }

    public static UsageSessionResponse fromEntity(UsageSession session) {
        return fromEntity(session, BigDecimal.ZERO, new HashMap<>(), new ArrayList<>());
    }

    public static UsageSessionResponse fromEntity(UsageSession session,
                                                  BigDecimal additionalCost,
                                                  Map<String, BigDecimal> costBreakdown,
                                                  List<VehicleInspectionResponse> inspections) {
        if (session == null) {
            return null;
        }

        Booking booking = session.getBooking();
        Vehicle vehicle = booking != null ? booking.getVehicle() : null;
        var user = booking != null ? booking.getUser() : null;

        BigDecimal mileage = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        if (session.getStartOdometer() != null && session.getEndOdometer() != null) {
            mileage = session.getEndOdometer().subtract(session.getStartOdometer()).setScale(2, RoundingMode.HALF_UP);
        }

        Integer batteryDelta = 0;
        if (session.getStartBattery() != null && session.getEndBattery() != null) {
            batteryDelta = session.getStartBattery() - session.getEndBattery();
        }

        Long durationMinutes = null;
        if (session.getCheckInTime() != null && session.getCheckOutTime() != null) {
            durationMinutes = Duration.between(session.getCheckInTime(), session.getCheckOutTime()).toMinutes();
        }

        BigDecimal finalCost = additionalCost != null ? additionalCost.setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

        return new UsageSessionResponse(
                session.getId(),
                booking != null ? booking.getId() : null,
                user != null ? user.getId() : null,
                user != null ? user.getFullName() : null,
                user != null ? user.getEmail() : null,
                vehicle != null ? vehicle.getId() : null,
                vehicle != null ? vehicle.getLicensePlate() : null,
                vehicle != null ? vehicle.getModelName() : null,
                session.getStartOdometer(),
                session.getEndOdometer(),
                mileage,
                session.getStartBattery(),
                session.getEndBattery(),
                batteryDelta,
                session.getCheckInTime(),
                session.getCheckOutTime(),
                durationMinutes,
                session.getStatus(),
                finalCost,
                costBreakdown != null ? costBreakdown : new HashMap<>(),
                inspections != null ? inspections : new ArrayList<>()
        );
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getBookingId() {
        return bookingId;
    }

    public void setBookingId(Long bookingId) {
        this.bookingId = bookingId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getUserEmail() {
        return userEmail;
    }

    public void setUserEmail(String userEmail) {
        this.userEmail = userEmail;
    }

    public Long getVehicleId() {
        return vehicleId;
    }

    public void setVehicleId(Long vehicleId) {
        this.vehicleId = vehicleId;
    }

    public String getVehicleLicensePlate() {
        return vehicleLicensePlate;
    }

    public void setVehicleLicensePlate(String vehicleLicensePlate) {
        this.vehicleLicensePlate = vehicleLicensePlate;
    }

    public String getVehicleModel() {
        return vehicleModel;
    }

    public void setVehicleModel(String vehicleModel) {
        this.vehicleModel = vehicleModel;
    }

    public BigDecimal getStartOdometer() {
        return startOdometer;
    }

    public void setStartOdometer(BigDecimal startOdometer) {
        this.startOdometer = startOdometer;
    }

    public BigDecimal getEndOdometer() {
        return endOdometer;
    }

    public void setEndOdometer(BigDecimal endOdometer) {
        this.endOdometer = endOdometer;
    }

    public BigDecimal getMileage() {
        return mileage;
    }

    public void setMileage(BigDecimal mileage) {
        this.mileage = mileage;
    }

    public Integer getStartBattery() {
        return startBattery;
    }

    public void setStartBattery(Integer startBattery) {
        this.startBattery = startBattery;
    }

    public Integer getEndBattery() {
        return endBattery;
    }

    public void setEndBattery(Integer endBattery) {
        this.endBattery = endBattery;
    }

    public Integer getBatteryDelta() {
        return batteryDelta;
    }

    public void setBatteryDelta(Integer batteryDelta) {
        this.batteryDelta = batteryDelta;
    }

    public Instant getCheckInTime() {
        return checkInTime;
    }

    public void setCheckInTime(Instant checkInTime) {
        this.checkInTime = checkInTime;
    }

    public Instant getCheckOutTime() {
        return checkOutTime;
    }

    public void setCheckOutTime(Instant checkOutTime) {
        this.checkOutTime = checkOutTime;
    }

    public Long getDurationMinutes() {
        return durationMinutes;
    }

    public void setDurationMinutes(Long durationMinutes) {
        this.durationMinutes = durationMinutes;
    }

    public UsageSessionStatus getStatus() {
        return status;
    }

    public void setStatus(UsageSessionStatus status) {
        this.status = status;
    }

    public BigDecimal getAdditionalCost() {
        return additionalCost;
    }

    public void setAdditionalCost(BigDecimal additionalCost) {
        this.additionalCost = additionalCost;
    }

    public Map<String, BigDecimal> getCostBreakdown() {
        return costBreakdown;
    }

    public void setCostBreakdown(Map<String, BigDecimal> costBreakdown) {
        this.costBreakdown = costBreakdown;
    }

    public List<VehicleInspectionResponse> getInspections() {
        return inspections;
    }

    public void setInspections(List<VehicleInspectionResponse> inspections) {
        this.inspections = inspections;
    }
}
