package com.example.evshare.service;

import com.example.evshare.dto.request.CheckInRequest;
import com.example.evshare.dto.request.CheckOutRequest;
import com.example.evshare.dto.response.UsageSessionResponse;
import com.example.evshare.dto.response.VehicleInspectionResponse;

import java.util.List;

/**
 * Service interface governing EV usage trip sessions, vehicle check-in/out protocols,
 * odometer/battery telemetry validation, inspection reporting, and historical immutability.
 */
public interface UsageSessionService {

    /**
     * Commences a usage trip session for a confirmed booking.
     * Transitions booking to IN_USE and vehicle to IN_USE.
     * Captures starting odometer, starting battery SoC, and optional check-in inspection.
     *
     * @param request the check-in parameters
     * @param currentUserId the ID of the authenticated user performing check-in
     * @return the created UsageSessionResponse
     */
    UsageSessionResponse checkIn(CheckInRequest request, Long currentUserId);

    /**
     * Concludes an active usage trip session.
     * Validates odometer continuity and battery SoC.
     * Assesses additional costs (e.g. 150,000 VND low battery surcharge per BR-OPS-02).
     * Enforces strict immutability: historical usage cannot be rewritten once completed.
     * Transitions booking to COMPLETED and vehicle to AVAILABLE / CHARGING / MAINTENANCE.
     *
     * @param sessionId the usage session ID
     * @param request the check-out parameters
     * @param currentUserId the ID of the authenticated user performing check-out
     * @return the finalized UsageSessionResponse with cost and telemetry breakdown
     */
    UsageSessionResponse checkOut(Long sessionId, CheckOutRequest request, Long currentUserId);

    /**
     * Retrieves usage session by ID.
     */
    UsageSessionResponse getSessionById(Long sessionId, Long currentUserId);

    /**
     * Retrieves usage session associated with a booking ID.
     */
    UsageSessionResponse getSessionByBookingId(Long bookingId, Long currentUserId);

    /**
     * Retrieves usage session history for a specific vehicle.
     */
    List<UsageSessionResponse> getSessionsByVehicle(Long vehicleId, Long currentUserId);

    /**
     * Retrieves usage sessions for the currently authenticated user.
     */
    List<UsageSessionResponse> getMySessions(Long currentUserId);

    /**
     * Retrieves physical inspection reports linked to a usage session.
     */
    List<VehicleInspectionResponse> getInspections(Long sessionId, Long currentUserId);
}
