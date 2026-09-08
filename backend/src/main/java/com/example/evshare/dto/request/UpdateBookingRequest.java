package com.example.evshare.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;

@Schema(description = "Request payload to reschedule an existing booking")
public class UpdateBookingRequest {

    @NotNull(message = "Start time is required")
    @Schema(description = "New booking start time in UTC ISO-8601", example = "2026-09-11T09:00:00Z", requiredMode = Schema.RequiredMode.REQUIRED)
    private Instant startTime;

    @NotNull(message = "End time is required")
    @Schema(description = "New booking end time in UTC ISO-8601", example = "2026-09-11T13:00:00Z", requiredMode = Schema.RequiredMode.REQUIRED)
    private Instant endTime;

    public UpdateBookingRequest() {
    }

    public UpdateBookingRequest(Instant startTime, Instant endTime) {
        this.startTime = startTime;
        this.endTime = endTime;
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
}
