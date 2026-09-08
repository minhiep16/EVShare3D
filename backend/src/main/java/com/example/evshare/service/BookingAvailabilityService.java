package com.example.evshare.service;

import com.example.evshare.dto.response.BookingTimelineSlotResponse;
import com.example.evshare.dto.response.VehicleAvailabilityResponse;

import java.time.Instant;
import java.util.List;

public interface BookingAvailabilityService {

    /**
     * Checks if the target electric vehicle is available for reservation across the requested interval.
     * Incorporates operational vehicle status (rejects MAINTENANCE, DAMAGED, UNAVAILABLE),
     * booking constraints (min 30m, max 72h, max 30 days advance, no past dates),
     * and mandatory 30-minute turnaround buffer between consecutive bookings.
     *
     * @param vehicleId target vehicle ID
     * @param startTime proposed booking commencement timestamp (UTC)
     * @param endTime proposed booking conclusion timestamp (UTC)
     * @param currentUserId authenticated user ID (can be null for public/general checks)
     * @return VehicleAvailabilityResponse with availability flag, reason, and any conflicting intervals
     */
    VehicleAvailabilityResponse checkAvailability(Long vehicleId, Instant startTime, Instant endTime, Long currentUserId);

    /**
     * Retrieves all active/scheduled booking intervals for a vehicle within a date range [from, to]
     * for rendering on the 3D Calendar Timeline.
     * Automatically filters out CANCELLED and REJECTED bookings.
     *
     * @param vehicleId target vehicle ID
     * @param from start of timeline inspection window (UTC)
     * @param to end of timeline inspection window (UTC)
     * @param currentUserId authenticated user ID to mark isMyBooking flags
     * @return List of chronological timeline occupancy slots with turnaround buffers
     */
    List<BookingTimelineSlotResponse> getTimeline(Long vehicleId, Instant from, Instant to, Long currentUserId);
}
