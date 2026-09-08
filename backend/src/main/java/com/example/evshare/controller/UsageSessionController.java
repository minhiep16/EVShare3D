package com.example.evshare.controller;

import com.example.evshare.dto.request.CheckInRequest;
import com.example.evshare.dto.request.CheckOutRequest;
import com.example.evshare.dto.request.GenerateQrRequest;
import com.example.evshare.dto.request.QrValidationRequest;
import com.example.evshare.dto.response.ApiResponse;
import com.example.evshare.dto.response.QrCodeResponse;
import com.example.evshare.dto.response.QrValidationResponse;
import com.example.evshare.dto.response.UsageSessionResponse;
import com.example.evshare.dto.response.VehicleInspectionResponse;
import com.example.evshare.security.UserPrincipal;
import com.example.evshare.service.QrValidationService;
import com.example.evshare.service.UsageSessionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/usage-sessions")
@Tag(name = "Usage Sessions & Vehicle Telemetry", description = "Endpoints for vehicle check-in/out, odometer/battery telemetry, inspection reporting, and session history")
@SecurityRequirement(name = "bearerAuth")
public class UsageSessionController {

    private final UsageSessionService usageSessionService;
    private final QrValidationService qrValidationService;

    public UsageSessionController(UsageSessionService usageSessionService, QrValidationService qrValidationService) {
        this.usageSessionService = usageSessionService;
        this.qrValidationService = qrValidationService;
    }

    @PostMapping("/generate-qr")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Generate 5-minute QR check-in token",
            description = "Issues a signed, time-bounded cryptographic token containing only non-sensitive reservation identifiers")
    public ResponseEntity<ApiResponse<QrCodeResponse>> generateQr(
            @Valid @RequestBody GenerateQrRequest request,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        Long currentUserId = principal != null ? principal.getId() : null;
        QrCodeResponse response = qrValidationService.generateCheckInQr(request.getBookingId(), currentUserId);
        return ResponseEntity.ok(ApiResponse.ok("QR check-in token generated successfully", response));
    }

    @PostMapping("/validate-qr")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Validate QR check-in token",
            description = "Performs cryptographic verification, expiration check, and authoritative DB cross-validation of vehicle, booking, time window, and authorization")
    public ResponseEntity<ApiResponse<QrValidationResponse>> validateQr(
            @Valid @RequestBody QrValidationRequest request,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        Long currentUserId = principal != null ? principal.getId() : null;
        QrValidationResponse response = qrValidationService.validateQr(request, currentUserId);
        return ResponseEntity.ok(ApiResponse.ok("QR check-in token validated successfully", response));
    }

    @PostMapping("/check-in")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Check-in to commence vehicle trip",
            description = "Transitions booking to IN_USE, vehicle to IN_USE, captures initial odometer, battery SoC, and physical condition")
    public ResponseEntity<ApiResponse<UsageSessionResponse>> checkIn(
            @Valid @RequestBody CheckInRequest request,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        Long currentUserId = principal != null ? principal.getId() : null;
        UsageSessionResponse response = usageSessionService.checkIn(request, currentUserId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created("Trip commenced successfully: Vehicle checked in", response));
    }

    @PostMapping("/{id}/check-out")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Check-out to conclude vehicle trip",
            description = "Validates final odometer, calculates battery delta, evaluates BR-OPS-02 minimum battery surcharge, and seals historical session")
    public ResponseEntity<ApiResponse<UsageSessionResponse>> checkOut(
            @PathVariable Long id,
            @Valid @RequestBody CheckOutRequest request,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        Long currentUserId = principal != null ? principal.getId() : null;
        UsageSessionResponse response = usageSessionService.checkOut(id, request, currentUserId);
        return ResponseEntity.ok(ApiResponse.ok("Trip concluded successfully: Vehicle checked out", response));
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get usage session details by ID",
            description = "Retrieves session telemetry, inspection reports, and itemized surcharges")
    public ResponseEntity<ApiResponse<UsageSessionResponse>> getSessionById(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        Long currentUserId = principal != null ? principal.getId() : null;
        UsageSessionResponse response = usageSessionService.getSessionById(id, currentUserId);
        return ResponseEntity.ok(ApiResponse.ok("Usage session retrieved successfully", response));
    }

    @GetMapping("/booking/{bookingId}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get usage session by booking ID",
            description = "Retrieves the active or completed usage session associated with a booking reservation")
    public ResponseEntity<ApiResponse<UsageSessionResponse>> getSessionByBookingId(
            @PathVariable Long bookingId,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        Long currentUserId = principal != null ? principal.getId() : null;
        UsageSessionResponse response = usageSessionService.getSessionByBookingId(bookingId, currentUserId);
        return ResponseEntity.ok(ApiResponse.ok("Usage session retrieved successfully", response));
    }

    @GetMapping("/vehicle/{vehicleId}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get vehicle usage session history",
            description = "Retrieves all historical trip sessions logged for a co-owned vehicle")
    public ResponseEntity<ApiResponse<List<UsageSessionResponse>>> getSessionsByVehicle(
            @PathVariable Long vehicleId,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        Long currentUserId = principal != null ? principal.getId() : null;
        List<UsageSessionResponse> response = usageSessionService.getSessionsByVehicle(vehicleId, currentUserId);
        return ResponseEntity.ok(ApiResponse.ok("Vehicle usage sessions retrieved successfully", response));
    }

    @GetMapping("/my-sessions")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get current user usage sessions",
            description = "Retrieves all historical and active trip sessions for the authenticated user")
    public ResponseEntity<ApiResponse<List<UsageSessionResponse>>> getMySessions(
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        Long currentUserId = principal != null ? principal.getId() : null;
        List<UsageSessionResponse> response = usageSessionService.getMySessions(currentUserId);
        return ResponseEntity.ok(ApiResponse.ok("User usage sessions retrieved successfully", response));
    }

    @GetMapping("/{id}/inspections")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get session vehicle inspection reports",
            description = "Retrieves check-in and check-out physical condition inspections, 3D mesh flags, and photo evidence")
    public ResponseEntity<ApiResponse<List<VehicleInspectionResponse>>> getInspections(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        Long currentUserId = principal != null ? principal.getId() : null;
        List<VehicleInspectionResponse> response = usageSessionService.getInspections(id, currentUserId);
        return ResponseEntity.ok(ApiResponse.ok("Vehicle inspection reports retrieved successfully", response));
    }
}
