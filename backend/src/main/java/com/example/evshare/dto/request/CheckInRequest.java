package com.example.evshare.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Schema(description = "Request payload to commence a vehicle trip session (Check-In)")
public class CheckInRequest {

    @NotNull(message = "Booking ID is required")
    @Schema(description = "ID of the confirmed booking reservation", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long bookingId;

    @Schema(description = "Target vehicle ID to verify physical station matching", example = "1")
    private Long vehicleId;

    @NotNull(message = "Start odometer reading is required")
    @DecimalMin(value = "0.00", message = "Start odometer reading cannot be negative")
    @Schema(description = "Current vehicle odometer reading at check-in (km)", example = "15200.50", requiredMode = Schema.RequiredMode.REQUIRED)
    private BigDecimal startOdometer;

    @NotNull(message = "Start battery State of Charge (SoC) is required")
    @Min(value = 0, message = "Start battery SoC must be at least 0%")
    @Max(value = 100, message = "Start battery SoC cannot exceed 100%")
    @Schema(description = "Current battery State of Charge (0-100%)", example = "85", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer startBattery;

    @Schema(description = "3D vehicle avatar defect vertex / zone flags in JSON", example = "{\"defects\": [\"BUMPER_FRONT_SCRATCH\"]}")
    private String conditionMeshFlags;

    @Schema(description = "Textual physical condition inspection notes", example = "Pre-existing scratch on front right bumper noted")
    private String inspectionNotes;

    @Schema(description = "Photographic evidence URLs taken during check-in")
    private List<String> evidencePhotoUrls = new ArrayList<>();

    public CheckInRequest() {
    }

    public CheckInRequest(Long bookingId, BigDecimal startOdometer, Integer startBattery) {
        this.bookingId = bookingId;
        this.startOdometer = startOdometer;
        this.startBattery = startBattery;
    }

    public CheckInRequest(Long bookingId, Long vehicleId, BigDecimal startOdometer, Integer startBattery) {
        this.bookingId = bookingId;
        this.vehicleId = vehicleId;
        this.startOdometer = startOdometer;
        this.startBattery = startBattery;
    }

    public CheckInRequest(Long bookingId, BigDecimal startOdometer, Integer startBattery, String conditionMeshFlags, String inspectionNotes, List<String> evidencePhotoUrls) {
        this.bookingId = bookingId;
        this.startOdometer = startOdometer;
        this.startBattery = startBattery;
        this.conditionMeshFlags = conditionMeshFlags;
        this.inspectionNotes = inspectionNotes;
        this.evidencePhotoUrls = evidencePhotoUrls != null ? evidencePhotoUrls : new ArrayList<>();
    }

    public CheckInRequest(Long bookingId, Long vehicleId, BigDecimal startOdometer, Integer startBattery, String conditionMeshFlags, String inspectionNotes, List<String> evidencePhotoUrls) {
        this.bookingId = bookingId;
        this.vehicleId = vehicleId;
        this.startOdometer = startOdometer;
        this.startBattery = startBattery;
        this.conditionMeshFlags = conditionMeshFlags;
        this.inspectionNotes = inspectionNotes;
        this.evidencePhotoUrls = evidencePhotoUrls != null ? evidencePhotoUrls : new ArrayList<>();
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

    public BigDecimal getStartOdometer() {
        return startOdometer;
    }

    public void setStartOdometer(BigDecimal startOdometer) {
        this.startOdometer = startOdometer;
    }

    public Integer getStartBattery() {
        return startBattery;
    }

    public void setStartBattery(Integer startBattery) {
        this.startBattery = startBattery;
    }

    public String getConditionMeshFlags() {
        return conditionMeshFlags;
    }

    public void setConditionMeshFlags(String conditionMeshFlags) {
        this.conditionMeshFlags = conditionMeshFlags;
    }

    public String getInspectionNotes() {
        return inspectionNotes;
    }

    public void setInspectionNotes(String inspectionNotes) {
        this.inspectionNotes = inspectionNotes;
    }

    public List<String> getEvidencePhotoUrls() {
        return evidencePhotoUrls;
    }

    public void setEvidencePhotoUrls(List<String> evidencePhotoUrls) {
        this.evidencePhotoUrls = evidencePhotoUrls;
    }
}
