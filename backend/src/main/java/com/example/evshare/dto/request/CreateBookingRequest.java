package com.example.evshare.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;

@Schema(description = "Request payload to create a new vehicle reservation")
public class CreateBookingRequest {

    @NotNull(message = "Vehicle ID is required")
    @Schema(description = "ID of the vehicle to book", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long vehicleId;

    @NotNull(message = "Start time is required")
    @Schema(description = "Booking reservation start time in UTC ISO-8601", example = "2026-09-10T08:00:00Z", requiredMode = Schema.RequiredMode.REQUIRED)
    private Instant startTime;

    @NotNull(message = "End time is required")
    @Schema(description = "Booking reservation end time in UTC ISO-8601", example = "2026-09-10T12:00:00Z", requiredMode = Schema.RequiredMode.REQUIRED)
    private Instant endTime;

    @Schema(description = "Target user ID (Admin override only; ignored for co-owners to prevent spoofing)", example = "2")
    private Long userId;

    public CreateBookingRequest() {
    }

    public CreateBookingRequest(Long vehicleId, Instant startTime, Instant endTime) {
        this.vehicleId = vehicleId;
        this.startTime = startTime;
        this.endTime = endTime;
    }

    public CreateBookingRequest(Long vehicleId, Instant startTime, Instant endTime, Long userId) {
        this.vehicleId = vehicleId;
        this.startTime = startTime;
        this.endTime = endTime;
        this.userId = userId;
    }

    public Long getVehicleId() {
        return vehicleId;
    }

    public void setVehicleId(Long vehicleId) {
        this.vehicleId = vehicleId;
    }

    public Instant getStartTime() {
        return startTime;
    }

    public void setStartTime(Instant startTime) {
        this.startTime = startTime;
    }

    public Instant getEndTime() {
        return endTime;
    }

    public void setEndTime(Instant endTime) {
        this.endTime = endTime;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }
}
