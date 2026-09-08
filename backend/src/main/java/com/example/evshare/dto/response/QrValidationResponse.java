package com.example.evshare.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Result payload of backend QR code cryptographic and state validation")
public class QrValidationResponse {

    @Schema(description = "Whether the QR code and all domain cross-validations succeeded", example = "true")
    private boolean valid;

    @Schema(description = "Validated booking reservation ID", example = "500")
    private Long bookingId;

    @Schema(description = "Validated vehicle ID", example = "100")
    private Long vehicleId;

    @Schema(description = "Validated vehicle license plate", example = "51H-999.88")
    private String vehicleLicensePlate;

    @Schema(description = "Validated vehicle model title", example = "VinFast VF8 Plus")
    private String vehicleModel;

    @Schema(description = "Authorized user ID", example = "10")
    private Long userId;

    @Schema(description = "Authorized user full legal name", example = "Nguyen Van A")
    private String userName;

    @Schema(description = "Check-in window commencement timestamp")
    private Instant checkInWindowStart;

    @Schema(description = "Check-in window expiration timestamp")
    private Instant checkInWindowEnd;

    @Schema(description = "Informative human-readable status message", example = "QR validation successful. Vehicle ready for check-in.")
    private String message;

    public QrValidationResponse() {
    }

    public QrValidationResponse(boolean valid, Long bookingId, Long vehicleId, String vehicleLicensePlate, String vehicleModel, Long userId, String userName, Instant checkInWindowStart, Instant checkInWindowEnd, String message) {
        this.valid = valid;
        this.bookingId = bookingId;
        this.vehicleId = vehicleId;
        this.vehicleLicensePlate = vehicleLicensePlate;
        this.vehicleModel = vehicleModel;
        this.userId = userId;
        this.userName = userName;
        this.checkInWindowStart = checkInWindowStart;
        this.checkInWindowEnd = checkInWindowEnd;
        this.message = message;
    }

    public static QrValidationResponse valid(Long bookingId, Long vehicleId, String vehicleLicensePlate, String vehicleModel, Long userId, String userName, Instant checkInWindowStart, Instant checkInWindowEnd, String message) {
        return new QrValidationResponse(true, bookingId, vehicleId, vehicleLicensePlate, vehicleModel, userId, userName, checkInWindowStart, checkInWindowEnd, message);
    }

    public static QrValidationResponse invalid(String message) {
        QrValidationResponse resp = new QrValidationResponse();
        resp.setValid(false);
        resp.setMessage(message);
        return resp;
    }

    public boolean isValid() {
        return valid;
    }

    public void setValid(boolean valid) {
        this.valid = valid;
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

    public Instant getCheckInWindowStart() {
        return checkInWindowStart;
    }

    public void setCheckInWindowStart(Instant checkInWindowStart) {
        this.checkInWindowStart = checkInWindowStart;
    }

    public Instant getCheckInWindowEnd() {
        return checkInWindowEnd;
    }

    public void setCheckInWindowEnd(Instant checkInWindowEnd) {
        this.checkInWindowEnd = checkInWindowEnd;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
