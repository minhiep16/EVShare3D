package com.example.evshare.dto.response;

import com.example.evshare.entity.enums.VehicleStatus;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class VehicleAvailabilityResponse {

    private Long vehicleId;
    private VehicleStatus vehicleStatus;
    private Boolean isAvailable;
    private String reason;
    private Instant requestedStartTime;
    private Instant requestedEndTime;
    private Integer bufferMinutes = 30;
    private List<BookingTimelineSlotResponse> conflictingBookings = new ArrayList<>();

    public VehicleAvailabilityResponse() {
    }

    public VehicleAvailabilityResponse(Long vehicleId, VehicleStatus vehicleStatus, Boolean isAvailable, String reason,
                                       Instant requestedStartTime, Instant requestedEndTime, Integer bufferMinutes,
                                       List<BookingTimelineSlotResponse> conflictingBookings) {
        this.vehicleId = vehicleId;
        this.vehicleStatus = vehicleStatus;
        this.isAvailable = isAvailable;
        this.reason = reason;
        this.requestedStartTime = requestedStartTime;
        this.requestedEndTime = requestedEndTime;
        this.bufferMinutes = bufferMinutes != null ? bufferMinutes : 30;
        this.conflictingBookings = conflictingBookings != null ? conflictingBookings : new ArrayList<>();
    }

    public static VehicleAvailabilityResponse available(Long vehicleId, VehicleStatus vehicleStatus,
                                                        Instant startTime, Instant endTime) {
        return new VehicleAvailabilityResponse(
                vehicleId,
                vehicleStatus,
                true,
                "Vehicle is available for the requested time slot",
                startTime,
                endTime,
                30,
                new ArrayList<>()
        );
    }

    public static VehicleAvailabilityResponse unavailable(Long vehicleId, VehicleStatus vehicleStatus,
                                                          Instant startTime, Instant endTime,
                                                          String reason,
                                                          List<BookingTimelineSlotResponse> conflictingBookings) {
        return new VehicleAvailabilityResponse(
                vehicleId,
                vehicleStatus,
                false,
                reason,
                startTime,
                endTime,
                30,
                conflictingBookings != null ? conflictingBookings : new ArrayList<>()
        );
    }

    public Long getVehicleId() {
        return vehicleId;
    }

    public void setVehicleId(Long vehicleId) {
        this.vehicleId = vehicleId;
    }

    public VehicleStatus getVehicleStatus() {
        return vehicleStatus;
    }

    public void setVehicleStatus(VehicleStatus vehicleStatus) {
        this.vehicleStatus = vehicleStatus;
    }

    public Boolean getIsAvailable() {
        return isAvailable;
    }

    public void setIsAvailable(Boolean available) {
        isAvailable = available;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public Instant getRequestedStartTime() {
        return requestedStartTime;
    }

    public void setRequestedStartTime(Instant requestedStartTime) {
        this.requestedStartTime = requestedStartTime;
    }

    public Instant getRequestedEndTime() {
        return requestedEndTime;
    }

    public void setRequestedEndTime(Instant requestedEndTime) {
        this.requestedEndTime = requestedEndTime;
    }

    public Integer getBufferMinutes() {
        return bufferMinutes;
    }

    public void setBufferMinutes(Integer bufferMinutes) {
        this.bufferMinutes = bufferMinutes;
    }

    public List<BookingTimelineSlotResponse> getConflictingBookings() {
        return conflictingBookings;
    }

    public void setConflictingBookings(List<BookingTimelineSlotResponse> conflictingBookings) {
        this.conflictingBookings = conflictingBookings;
    }
}
