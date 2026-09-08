package com.example.evshare.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Request payload to validate a physical or digital QR check-in token")
public class QrValidationRequest {

    @NotBlank(message = "QR token is required")
    @Schema(description = "Signed compact JWT QR token string", example = "eyJhbGciOiJIUzI1NiIsIn...", requiredMode = Schema.RequiredMode.REQUIRED)
    private String qrToken;

    @Schema(description = "ID of vehicle at the physical scanner station (optional cross-match)", example = "100")
    private Long vehicleId;

    @Schema(description = "Target booking ID (optional cross-match)", example = "500")
    private Long bookingId;

    public QrValidationRequest() {
    }

    public QrValidationRequest(String qrToken) {
        this.qrToken = qrToken;
    }

    public QrValidationRequest(String qrToken, Long vehicleId) {
        this.qrToken = qrToken;
        this.vehicleId = vehicleId;
    }

    public QrValidationRequest(String qrToken, Long vehicleId, Long bookingId) {
        this.qrToken = qrToken;
        this.vehicleId = vehicleId;
        this.bookingId = bookingId;
    }

    public String getQrToken() {
        return qrToken;
    }

    public void setQrToken(String qrToken) {
        this.qrToken = qrToken;
    }

    public Long getVehicleId() {
        return vehicleId;
    }

    public void setVehicleId(Long vehicleId) {
        this.vehicleId = vehicleId;
    }

    public Long getBookingId() {
        return bookingId;
    }

    public void setBookingId(Long bookingId) {
        this.bookingId = bookingId;
    }
}
