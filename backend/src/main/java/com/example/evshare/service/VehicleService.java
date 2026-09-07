package com.example.evshare.service;

import com.example.evshare.dto.request.CreateVehicleRequest;
import com.example.evshare.dto.request.UpdateVehicleStatusRequest;
import com.example.evshare.dto.response.PagedData;
import com.example.evshare.dto.response.VehicleResponse;
import com.example.evshare.dto.response.VehicleTelemetryResponse;
import com.example.evshare.entity.enums.VehicleStatus;
import org.springframework.data.domain.Pageable;

public interface VehicleService {

    /**
     * Registers a new Digital Twin EV.
     * Restricted to ADMIN.
     *
     * @param request Vehicle registration payload
     * @return Created vehicle response
     */
    VehicleResponse createVehicle(CreateVehicleRequest request);

    /**
     * Retrieves a paginated list of vehicles with optional filtering by status and manufacturer.
     * Accessible by authenticated users.
     *
     * @param status       Optional status filter
     * @param manufacturer Optional manufacturer filter
     * @param pageable     Pagination parameters
     * @return Paginated list of vehicle responses
     */
    PagedData<VehicleResponse> getVehicles(VehicleStatus status, String manufacturer, Pageable pageable);

    /**
     * Retrieves real-time telemetry (battery SoC, odometer, bay location) for a vehicle.
     * Accessible by authenticated users.
     *
     * @param vehicleId Vehicle ID
     * @return Real-time telemetry response
     */
    VehicleTelemetryResponse getVehicleTelemetry(Long vehicleId);

    /**
     * Updates the status of a vehicle according to state machine rules.
     * Enforces transactional validation and pessimistic concurrency locking.
     *
     * @param vehicleId Vehicle ID
     * @param request   Update status request payload
     * @return Updated vehicle response
     */
    VehicleResponse updateVehicleStatus(Long vehicleId, UpdateVehicleStatusRequest request);

    /**
     * Retrieves vehicle details by ID.
     *
     * @param id Vehicle ID
     * @return Vehicle response
     */
    VehicleResponse getVehicleById(Long id);
}
