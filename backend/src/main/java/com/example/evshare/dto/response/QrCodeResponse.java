package com.example.evshare.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Cryptographic QR Check-In Token response payload")
public class QrCodeResponse {

    @Schema(description = "Signed compact JWT QR token string", example = "eyJhbGciOiJIUzI1NiIsIn...")
    private String qrToken;

    @Schema(description = "Associated booking ID", example = "500")
    private Long bookingId;

    @Schema(description = "Assigned vehicle ID", example = "100")
    private Long vehicleId;

    @Schema(description = "Assigned vehicle license plate", example = "51H-999.88")
    private String vehicleLicensePlate;

    @Schema(description = "Assigned vehicle model title", example = "VinFast VF8 Plus")
    private String vehicleModel;

    @Schema(description = "Timestamp when token was issued")
    private Instant issuedAt;

    @Schema(description = "Timestamp when token expires (5 minutes TTL)")
    private Instant expiresAt;

    @Schema(description = "Seconds remaining until expiration", example = "300")
    private Long expiresInSeconds;

    public QrCodeResponse() {
    }

    public QrCodeResponse(String qrToken, Long bookingId, Long vehicleId, String vehicleLicensePlate, String vehicleModel, Instant issuedAt, Instant expiresAt, Long expiresInSeconds) {
        this.qrToken = qrToken;
        this.bookingId = bookingId;
        this.vehicleId = vehicleId;
        this.vehicleLicensePlate = vehicleLicensePlate;
        this.vehicleModel = vehicleModel;
        this.issuedAt = issuedAt;
        this.expiresAt = expiresAt;
        this.expiresInSeconds = expiresInSeconds;
    }

    public String getQrToken() {
        return qrToken;
    }

    public void setQrToken(String qrToken) {
        this.qrToken = qrToken;
    }

    public Long getBookingId() {
        return bookingId;
    }

    public void setBookingId(Long bookingId) {
        this.bookingId = bookingId;
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

    public Instant getIssuedAt() {
        return issuedAt;
    }

    public void setIssuedAt(Instant issuedAt) {
        this.issuedAt = issuedAt;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(Instant expiresAt) {
        this.expiresAt = expiresAt;
    }

    public Long getExpiresInSeconds() {
        return expiresInSeconds;
    }

    public void setExpiresInSeconds(Long expiresInSeconds) {
        this.expiresInSeconds = expiresInSeconds;
    }
}
