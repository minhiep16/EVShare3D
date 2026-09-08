package com.example.evshare.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Request payload to cancel an existing reservation")
public class CancelBookingRequest {

    @Schema(description = "Optional reason for reservation cancellation", example = "Change of travel plans")
    private String reason;

    public CancelBookingRequest() {
    }

    public CancelBookingRequest(String reason) {
        this.reason = reason;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}
