package com.example.evshare.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Request payload to generate a signed 5-minute QR check-in token")
public class GenerateQrRequest {

    @NotNull(message = "Booking ID is required")
    @Schema(description = "ID of the confirmed booking reservation", example = "500", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long bookingId;

    public GenerateQrRequest() {
    }

    public GenerateQrRequest(Long bookingId) {
        this.bookingId = bookingId;
    }

    public Long getBookingId() {
        return bookingId;
    }

    public void setBookingId(Long bookingId) {
        this.bookingId = bookingId;
    }
}
