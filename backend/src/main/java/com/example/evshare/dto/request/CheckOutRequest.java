package com.example.evshare.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Schema(description = "Request payload to conclude a vehicle trip session (Check-Out)")
public class CheckOutRequest {

    @NotNull(message = "End odometer reading is required")
    @DecimalMin(value = "0.00", message = "End odometer reading cannot be negative")
    @Schema(description = "Final odometer reading upon return (km)", example = "15280.75", requiredMode = Schema.RequiredMode.REQUIRED)
    private BigDecimal endOdometer;

    @NotNull(message = "End battery State of Charge (SoC) is required")
    @Min(value = 0, message = "End battery SoC must be at least 0%")
    @Max(value = 100, message = "End battery SoC cannot exceed 100%")
    @Schema(description = "Final battery State of Charge (0-100%)", example = "42", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer endBattery;

    @Schema(description = "Whether the vehicle was plugged into an active charger at the garage stall", example = "false")
    private Boolean isPluggedIn = false;

    @Schema(description = "Whether new damage or technical defects were detected upon return", example = "false")
    private Boolean hasDamage = false;

    @Schema(description = "3D vehicle avatar defect vertex / zone flags in JSON", example = "{\"defects\": [\"DOOR_REAR_LEFT_DENT\"]}")
    private String conditionMeshFlags;

    @Schema(description = "Textual physical return inspection report notes", example = "Vehicle returned clean, slight tire wear noted")
    private String inspectionNotes;

    @Schema(description = "Photographic evidence URLs taken during check-out")
    private List<String> evidencePhotoUrls = new ArrayList<>();

    @DecimalMin(value = "0.00", message = "Additional cost cannot be negative")
    @Schema(description = "Explicit additional fees (cleaning, tolls, damages) in VND", example = "0.00")
    private BigDecimal otherAdditionalCost;

    @Schema(description = "Target vehicle ID (optional verification)", example = "100")
    private Long vehicleId;

    @Schema(description = "Target user ID executing checkout (optional verification)", example = "10")
    private Long userId;

    public CheckOutRequest() {
    }

    public CheckOutRequest(BigDecimal endOdometer, Integer endBattery) {
        this.endOdometer = endOdometer;
        this.endBattery = endBattery;
    }

    public CheckOutRequest(BigDecimal endOdometer, Integer endBattery, Long vehicleId, Long userId) {
        this.endOdometer = endOdometer;
        this.endBattery = endBattery;
        this.vehicleId = vehicleId;
        this.userId = userId;
    }

    public CheckOutRequest(BigDecimal endOdometer, Integer endBattery, Boolean isPluggedIn, Boolean hasDamage, String conditionMeshFlags, String inspectionNotes, List<String> evidencePhotoUrls, BigDecimal otherAdditionalCost) {
        this(endOdometer, endBattery, isPluggedIn, hasDamage, conditionMeshFlags, inspectionNotes, evidencePhotoUrls, otherAdditionalCost, null, null);
    }

    public CheckOutRequest(BigDecimal endOdometer, Integer endBattery, Boolean isPluggedIn, Boolean hasDamage, String conditionMeshFlags, String inspectionNotes, List<String> evidencePhotoUrls, BigDecimal otherAdditionalCost, Long vehicleId, Long userId) {
        this.endOdometer = endOdometer;
        this.endBattery = endBattery;
        this.isPluggedIn = isPluggedIn != null ? isPluggedIn : false;
        this.hasDamage = hasDamage != null ? hasDamage : false;
        this.conditionMeshFlags = conditionMeshFlags;
        this.inspectionNotes = inspectionNotes;
        this.evidencePhotoUrls = evidencePhotoUrls != null ? evidencePhotoUrls : new ArrayList<>();
        this.otherAdditionalCost = otherAdditionalCost;
        this.vehicleId = vehicleId;
        this.userId = userId;
    }

    public BigDecimal getEndOdometer() {
        return endOdometer;
    }

    public void setEndOdometer(BigDecimal endOdometer) {
        this.endOdometer = endOdometer;
    }

    public Integer getEndBattery() {
        return endBattery;
    }

    public void setEndBattery(Integer endBattery) {
        this.endBattery = endBattery;
    }

    public Boolean getIsPluggedIn() {
        return isPluggedIn;
    }

    public void setIsPluggedIn(Boolean pluggedIn) {
        isPluggedIn = pluggedIn;
    }

    public Boolean getHasDamage() {
        return hasDamage;
    }

    public void setHasDamage(Boolean hasDamage) {
        this.hasDamage = hasDamage;
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

    public BigDecimal getOtherAdditionalCost() {
        return otherAdditionalCost;
    }

    public void setOtherAdditionalCost(BigDecimal otherAdditionalCost) {
        this.otherAdditionalCost = otherAdditionalCost;
    }

    public Long getVehicleId() {
        return vehicleId;
    }

    public void setVehicleId(Long vehicleId) {
        this.vehicleId = vehicleId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }
}
