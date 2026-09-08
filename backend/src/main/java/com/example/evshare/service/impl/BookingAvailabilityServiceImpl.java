package com.example.evshare.service.impl;

import com.example.evshare.dto.response.BookingTimelineSlotResponse;
import com.example.evshare.dto.response.VehicleAvailabilityResponse;
import com.example.evshare.entity.Booking;
import com.example.evshare.entity.Vehicle;
import com.example.evshare.entity.enums.VehicleStatus;
import com.example.evshare.exception.BusinessException;
import com.example.evshare.exception.ResourceNotFoundException;
import com.example.evshare.repository.BookingRepository;
import com.example.evshare.repository.VehicleRepository;
import com.example.evshare.service.BookingAvailabilityService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class BookingAvailabilityServiceImpl implements BookingAvailabilityService {

    private static final Logger log = LoggerFactory.getLogger(BookingAvailabilityServiceImpl.class);

    private static final int BUFFER_MINUTES = 30;
    private static final int MIN_BOOKING_MINUTES = 30;
    private static final int MAX_BOOKING_HOURS = 72;
    private static final int MAX_ADVANCE_DAYS = 30;

    private final BookingRepository bookingRepository;
    private final VehicleRepository vehicleRepository;

    public BookingAvailabilityServiceImpl(BookingRepository bookingRepository, VehicleRepository vehicleRepository) {
        this.bookingRepository = bookingRepository;
        this.vehicleRepository = vehicleRepository;
    }

    @Override
    public VehicleAvailabilityResponse checkAvailability(Long vehicleId, Instant startTime, Instant endTime, Long currentUserId) {
        if (vehicleId == null) {
            throw new BusinessException("Vehicle ID must not be null", HttpStatus.BAD_REQUEST);
        }
        if (startTime == null || endTime == null) {
            throw new BusinessException("Start time and end time must not be null", HttpStatus.BAD_REQUEST);
        }

        Vehicle vehicle = vehicleRepository.findById(vehicleId)
                .orElseThrow(() -> new ResourceNotFoundException("Vehicle not found with id: " + vehicleId));

        // 1. Vehicle operational status check
        if (vehicle.getStatus() == VehicleStatus.MAINTENANCE) {
            return VehicleAvailabilityResponse.unavailable(
                    vehicleId, vehicle.getStatus(), startTime, endTime,
                    "Vehicle is currently under maintenance and cannot be booked",
                    Collections.emptyList()
            );
        }
        if (vehicle.getStatus() == VehicleStatus.DAMAGED) {
            return VehicleAvailabilityResponse.unavailable(
                    vehicleId, vehicle.getStatus(), startTime, endTime,
                    "Vehicle is currently flagged as damaged and cannot be operated",
                    Collections.emptyList()
            );
        }
        if (vehicle.getStatus() == VehicleStatus.UNAVAILABLE) {
            return VehicleAvailabilityResponse.unavailable(
                    vehicleId, vehicle.getStatus(), startTime, endTime,
                    "Vehicle is currently unavailable / administratively decommissioned",
                    Collections.emptyList()
            );
        }

        // 2. Timing constraint validations per BR-BKG-01
        if (!endTime.isAfter(startTime)) {
            return VehicleAvailabilityResponse.unavailable(
                    vehicleId, vehicle.getStatus(), startTime, endTime,
                    "End time must be strictly after start time",
                    Collections.emptyList()
            );
        }

        Instant now = Instant.now();
        if (startTime.isBefore(now)) {
            return VehicleAvailabilityResponse.unavailable(
                    vehicleId, vehicle.getStatus(), startTime, endTime,
                    "Cannot schedule a reservation in the past",
                    Collections.emptyList()
            );
        }

        long durationMinutes = ChronoUnit.MINUTES.between(startTime, endTime);
        if (durationMinutes < MIN_BOOKING_MINUTES) {
            return VehicleAvailabilityResponse.unavailable(
                    vehicleId, vehicle.getStatus(), startTime, endTime,
                    String.format("Minimum booking duration is %d minutes (BR-BKG-01)", MIN_BOOKING_MINUTES),
                    Collections.emptyList()
            );
        }

        if (durationMinutes > (long) MAX_BOOKING_HOURS * 60) {
            return VehicleAvailabilityResponse.unavailable(
                    vehicleId, vehicle.getStatus(), startTime, endTime,
                    String.format("Maximum continuous booking duration is %d hours (BR-BKG-01)", MAX_BOOKING_HOURS),
                    Collections.emptyList()
            );
        }

        Instant maxAdvanceDate = now.plus(MAX_ADVANCE_DAYS, ChronoUnit.DAYS);
        if (startTime.isAfter(maxAdvanceDate)) {
            return VehicleAvailabilityResponse.unavailable(
                    vehicleId, vehicle.getStatus(), startTime, endTime,
                    String.format("Bookings can only be scheduled up to %d days in advance (BR-BKG-01)", MAX_ADVANCE_DAYS),
                    Collections.emptyList()
            );
        }

        // 3. Conflict query incorporating the mandatory 30-minute turnaround buffer (BR-BKG-02)
        Instant bufferedStart = startTime.minus(BUFFER_MINUTES, ChronoUnit.MINUTES);
        Instant bufferedEnd = endTime.plus(BUFFER_MINUTES, ChronoUnit.MINUTES);

        List<Booking> overlappingBookings = bookingRepository.findOverlappingBookingsWithBuffer(
                vehicleId, bufferedStart, bufferedEnd
        );

        if (!overlappingBookings.isEmpty()) {
            List<BookingTimelineSlotResponse> conflictResponses = overlappingBookings.stream()
                    .map(b -> BookingTimelineSlotResponse.fromEntity(b, currentUserId))
                    .collect(Collectors.toList());

            log.info("Availability conflict detected for vehicle [{}]: {} overlapping booking(s) within requested [{} to {}] with buffer",
                    vehicleId, overlappingBookings.size(), startTime, endTime);

            return VehicleAvailabilityResponse.unavailable(
                    vehicleId, vehicle.getStatus(), startTime, endTime,
                    "Requested time slot conflicts with an existing reservation (including mandatory 30-minute turnaround buffer)",
                    conflictResponses
            );
        }

        log.debug("Vehicle [{}] is available for requested interval [{} to {}]", vehicleId, startTime, endTime);
        return VehicleAvailabilityResponse.available(vehicleId, vehicle.getStatus(), startTime, endTime);
    }

    @Override
    public List<BookingTimelineSlotResponse> getTimeline(Long vehicleId, Instant from, Instant to, Long currentUserId) {
        if (vehicleId == null) {
            throw new BusinessException("Vehicle ID must not be null", HttpStatus.BAD_REQUEST);
        }
        if (from == null || to == null) {
            throw new BusinessException("From and To parameters must not be null", HttpStatus.BAD_REQUEST);
        }
        if (from.isAfter(to)) {
            throw new BusinessException("Parameter 'from' must be before or equal to 'to'", HttpStatus.BAD_REQUEST);
        }

        if (!vehicleRepository.existsById(vehicleId)) {
            throw new ResourceNotFoundException("Vehicle not found with id: " + vehicleId);
        }

        List<Booking> bookings = bookingRepository.findTimelineBookings(vehicleId, from, to);

        return bookings.stream()
                .map(b -> BookingTimelineSlotResponse.fromEntity(b, currentUserId))
                .collect(Collectors.toList());
    }
}
