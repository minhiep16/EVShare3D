package com.example.evshare.controller;

import com.example.evshare.dto.request.CreateVehicleRequest;
import com.example.evshare.dto.request.UpdateVehicleStatusRequest;
import com.example.evshare.dto.response.ApiResponse;
import com.example.evshare.dto.response.PagedData;
import com.example.evshare.dto.response.VehicleResponse;
import com.example.evshare.dto.response.VehicleTelemetryResponse;
import com.example.evshare.entity.enums.VehicleStatus;
import com.example.evshare.security.SecurityRoles;
import com.example.evshare.service.VehicleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/vehicles")
@Tag(name = "Vehicles", description = "Digital Twin Vehicle management and telemetry endpoints")
public class VehicleController {

    private final VehicleService vehicleService;

    public VehicleController(VehicleService vehicleService) {
        this.vehicleService = vehicleService;
    }

    @PostMapping
    @PreAuthorize(SecurityRoles.HAS_ROLE_ADMIN)
    @Operation(summary = "Register new digital twin vehicle",
            description = "Registers a new EV with 3D asset model and telemetry defaults. Restricted to ADMIN.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Vehicle registered successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid request payload or VIN format"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Authentication required"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden: Insufficient role permissions"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Vehicle with VIN or license plate already exists")
    })
    public ResponseEntity<ApiResponse<VehicleResponse>> createVehicle(
            @Valid @RequestBody CreateVehicleRequest request
    ) {
        VehicleResponse response = vehicleService.createVehicle(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Vehicle registered successfully", response));
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "List accessible vehicles",
            description = "Retrieves a paginated list of vehicles with optional filtering by status and manufacturer. Accessible by authenticated users.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Vehicles retrieved successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Authentication required")
    })
    public ResponseEntity<ApiResponse<PagedData<VehicleResponse>>> getVehicles(
            @Parameter(description = "Filter by vehicle lifecycle status")
            @RequestParam(required = false) VehicleStatus status,
            @Parameter(description = "Filter by vehicle manufacturer (case-insensitive)")
            @RequestParam(required = false) String manufacturer,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        PagedData<VehicleResponse> response = vehicleService.getVehicles(status, manufacturer, pageable);
        return ResponseEntity.ok(ApiResponse.ok("Vehicles retrieved successfully", response));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize(SecurityRoles.HAS_STAFF_OR_ADMIN)
    @Operation(summary = "Transition vehicle status",
            description = "Transitions vehicle lifecycle status via controlled state machine. Restricted to STAFF and ADMIN roles.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Vehicle status updated successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid request payload"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Authentication required"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden: Insufficient role permissions"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Vehicle not found"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Invalid state transition or conflict")
    })
    public ResponseEntity<ApiResponse<VehicleResponse>> updateVehicleStatus(
            @PathVariable Long id,
            @Valid @RequestBody UpdateVehicleStatusRequest request
    ) {
        VehicleResponse response = vehicleService.updateVehicleStatus(id, request);
        return ResponseEntity.ok(ApiResponse.ok("Vehicle status transitioned successfully", response));
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Retrieve vehicle details by ID",
            description = "Fetches full digital twin vehicle attributes and state. Accessible by authenticated users.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Vehicle details retrieved successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Authentication required"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Vehicle not found")
    })
    public ResponseEntity<ApiResponse<VehicleResponse>> getVehicleById(@PathVariable Long id) {
        VehicleResponse response = vehicleService.getVehicleById(id);
        return ResponseEntity.ok(ApiResponse.ok("Vehicle details retrieved successfully", response));
    }

    @GetMapping("/{id}/telemetry")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Retrieve real-time vehicle telemetry",
            description = "Fetches real-time battery SoC, odometer reading, bay stall location, and operational status. Accessible by authenticated users.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Vehicle telemetry retrieved successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Authentication required"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Vehicle not found")
    })
    public ResponseEntity<ApiResponse<VehicleTelemetryResponse>> getVehicleTelemetry(@PathVariable Long id) {
        VehicleTelemetryResponse response = vehicleService.getVehicleTelemetry(id);
        return ResponseEntity.ok(ApiResponse.ok("Vehicle telemetry retrieved successfully", response));
    }
}
