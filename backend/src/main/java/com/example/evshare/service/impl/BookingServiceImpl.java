package com.example.evshare.service.impl;

import com.example.evshare.dto.request.CancelBookingRequest;
import com.example.evshare.dto.request.CreateBookingRequest;
import com.example.evshare.dto.request.UpdateBookingRequest;
import com.example.evshare.dto.request.UpdateBookingStatusRequest;
import com.example.evshare.dto.response.BookingCancellationResponse;
import com.example.evshare.dto.response.BookingHistoryResponse;
import com.example.evshare.dto.response.BookingResponse;
import com.example.evshare.dto.response.PagedData;
import com.example.evshare.entity.AuditLog;
import com.example.evshare.entity.Booking;
import com.example.evshare.entity.User;
import com.example.evshare.entity.Vehicle;
import com.example.evshare.entity.enums.BookingStatus;
import com.example.evshare.entity.enums.VehicleStatus;
import com.example.evshare.exception.BookingConflictException;
import com.example.evshare.exception.BusinessException;
import com.example.evshare.exception.ResourceNotFoundException;
import com.example.evshare.repository.AuditLogRepository;
import com.example.evshare.repository.BookingRepository;
import com.example.evshare.repository.UserRepository;
import com.example.evshare.repository.VehicleRepository;
import com.example.evshare.security.OwnershipSecurity;
import com.example.evshare.service.BookingService;
import com.example.evshare.service.BookingStateMachine;
import com.example.evshare.service.VehicleStateMachine;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class BookingServiceImpl implements BookingService {

    private static final Logger log = LoggerFactory.getLogger(BookingServiceImpl.class);

    private static final int BUFFER_MINUTES = 30;
    private static final int MIN_BOOKING_MINUTES = 30;
    private static final int MAX_BOOKING_HOURS = 72;
    private static final int MAX_ADVANCE_DAYS = 30;
    private static final BigDecimal DEFAULT_HOURLY_RATE_VND = BigDecimal.valueOf(50_000);
    private static final BigDecimal LATE_CANCELLATION_PENALTY_RATE = BigDecimal.valueOf(0.20); // 20% per BR-BKG-03
    private static final long FREE_CANCELLATION_HOURS_THRESHOLD = 12; // 12h per BR-BKG-03

    private final BookingRepository bookingRepository;
    private final VehicleRepository vehicleRepository;
    private final UserRepository userRepository;
    private final OwnershipSecurity ownershipSecurity;
    private final AuditLogRepository auditLogRepository;
    private final ObjectMapper objectMapper;
    private final BookingStateMachine bookingStateMachine;
    private final VehicleStateMachine vehicleStateMachine;

    public BookingServiceImpl(BookingRepository bookingRepository,
                              VehicleRepository vehicleRepository,
                              UserRepository userRepository,
                              OwnershipSecurity ownershipSecurity,
                              AuditLogRepository auditLogRepository,
                              ObjectMapper objectMapper,
                              BookingStateMachine bookingStateMachine,
                              VehicleStateMachine vehicleStateMachine) {
        this.bookingRepository = bookingRepository;
        this.vehicleRepository = vehicleRepository;
        this.userRepository = userRepository;
        this.ownershipSecurity = ownershipSecurity;
        this.auditLogRepository = auditLogRepository;
        this.objectMapper = objectMapper;
        this.bookingStateMachine = bookingStateMachine;
        this.vehicleStateMachine = vehicleStateMachine;
    }

    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public BookingResponse createBooking(CreateBookingRequest request, Long currentUserId, boolean isAdmin) {
        if (request == null) {
            throw new BusinessException("Request body must not be null", HttpStatus.BAD_REQUEST);
        }
        if (request.getVehicleId() == null) {
            throw new BusinessException("Vehicle ID is required", HttpStatus.BAD_REQUEST);
        }
        if (request.getStartTime() == null || request.getEndTime() == null) {
            throw new BusinessException("Start time and end time are required", HttpStatus.BAD_REQUEST);
        }

        Instant startTime = request.getStartTime();
        Instant endTime = request.getEndTime();

        // 1. Authenticated user validation & spoofing prevention
        if (currentUserId == null) {
            throw new AccessDeniedException("User must be authenticated to create a booking");
        }

        Long effectiveUserId = (isAdmin && request.getUserId() != null) ? request.getUserId() : currentUserId;

        User user = userRepository.findById(effectiveUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + effectiveUserId));

        if (!Boolean.TRUE.equals(user.getIsActive())) {
            throw new BusinessException("User account is deactivated or inactive", HttpStatus.FORBIDDEN);
        }

        // 2. Timing validation per BR-BKG-01
        validateTimingConstraints(startTime, endTime);

        // 3. Concurrency serialization: acquire pessimistic row lock on vehicle
        Vehicle vehicle = vehicleRepository.findByIdForUpdate(request.getVehicleId())
                .orElseThrow(() -> new ResourceNotFoundException("Vehicle not found with id: " + request.getVehicleId()));

        // 4. Group equity membership validation (cannot be bypassed by frontend)
        if (!isAdmin) {
            boolean isMember = ownershipSecurity.isVehicleGroupMember(vehicle.getId(), user.getId());
            if (!isMember) {
                log.warn("Booking creation denied: User [{}] does not belong to vehicle [{}] syndicate", user.getId(), vehicle.getId());
                throw new AccessDeniedException("User is not an authorized equity holder in this vehicle's ownership group");
            }
        }

        // 5. Vehicle operational status validation
        validateVehicleOperational(vehicle);

        // 6. Conflict query incorporating mandatory 30-minute turnaround buffer (BR-BKG-02)
        Instant bufferedStart = startTime.minus(BUFFER_MINUTES, ChronoUnit.MINUTES);
        Instant bufferedEnd = endTime.plus(BUFFER_MINUTES, ChronoUnit.MINUTES);

        List<Booking> conflicts = bookingRepository.findOverlappingBookingsWithBuffer(
                vehicle.getId(), bufferedStart, bufferedEnd
        );

        if (!conflicts.isEmpty()) {
            log.warn("Booking collision detected for vehicle [{}]: {} overlapping booking(s) for interval [{} to {}] (with buffer)",
                    vehicle.getId(), conflicts.size(), startTime, endTime);
            throw new BookingConflictException(
                    "Requested time slot conflicts with an existing reservation (including mandatory 30-minute turnaround buffer)"
            );
        }

        // 7. Calculate estimated cost
        long durationMinutes = ChronoUnit.MINUTES.between(startTime, endTime);
        BigDecimal hours = BigDecimal.valueOf(durationMinutes)
                .divide(BigDecimal.valueOf(60), 2, RoundingMode.HALF_UP);
        BigDecimal estimatedCost = hours.multiply(DEFAULT_HOURLY_RATE_VND).setScale(2, RoundingMode.HALF_UP);

        // 8. Persist Booking entity
        Booking booking = Booking.builder()
                .vehicle(vehicle)
                .user(user)
                .startTime(startTime)
                .endTime(endTime)
                .status(BookingStatus.CONFIRMED)
                .estimatedCost(estimatedCost)
                .createdAt(Instant.now())
                .build();

        Booking savedBooking = bookingRepository.save(booking);
        log.info("Booking [{}] successfully created for user [{}] and vehicle [{}] from [{}] to [{}]",
                savedBooking.getId(), user.getId(), vehicle.getId(), startTime, endTime);

        // Controlled Vehicle State Transition:
        // If booking starts immediately or within check-in prep window (now >= startTime - 15m),
        // and vehicle is currently AVAILABLE, transition AVAILABLE -> BOOKED through state machine
        Instant now = Instant.now();
        Instant checkInPrepStart = startTime.minus(15, ChronoUnit.MINUTES);
        if (!now.isBefore(checkInPrepStart) && vehicle.getStatus() == VehicleStatus.AVAILABLE) {
            transitionVehicleStatus(vehicle, VehicleStatus.BOOKED, "Immediate/imminent reservation confirmed (bookingId=" + savedBooking.getId() + ")", currentUserId);
        }

        recordAudit("CREATE_BOOKING", savedBooking.getId(), null, serializeBookingSnapshot(savedBooking), currentUserId);

        return BookingResponse.fromEntity(savedBooking);
    }

    @Override
    @Transactional(readOnly = true)
    public BookingResponse getBookingById(Long id, Long currentUserId, boolean isAdmin) {
        if (id == null) {
            throw new BusinessException("Booking ID is required", HttpStatus.BAD_REQUEST);
        }

        Booking booking = bookingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found with id: " + id));

        validateBookingReadAccess(booking, currentUserId, isAdmin);

        return BookingResponse.fromEntity(booking);
    }

    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public BookingResponse updateBooking(Long id, UpdateBookingRequest request, Long currentUserId, boolean isAdmin) {
        if (id == null) {
            throw new BusinessException("Booking ID is required", HttpStatus.BAD_REQUEST);
        }
        if (request == null) {
            throw new BusinessException("Request payload is required", HttpStatus.BAD_REQUEST);
        }
        if (request.getStartTime() == null || request.getEndTime() == null) {
            throw new BusinessException("Start time and end time are required", HttpStatus.BAD_REQUEST);
        }

        Instant newStartTime = request.getStartTime();
        Instant newEndTime = request.getEndTime();

        // 1. Fetch booking with pessimistic row lock
        Booking booking = bookingRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found with id: " + id));

        // 2. Authorization check: only booking owner or admin can update
        validateBookingModificationAccess(booking, currentUserId, isAdmin);

        // 3. Lifecycle state check: historical completed / cancelled / in-use bookings cannot be modified
        BookingStatus status = booking.getStatus();
        if (status == BookingStatus.COMPLETED) {
            throw new BusinessException("Cannot modify completed historical booking", HttpStatus.BAD_REQUEST);
        }
        if (status == BookingStatus.IN_USE) {
            throw new BusinessException("Cannot modify booking currently in progress", HttpStatus.BAD_REQUEST);
        }
        if (status == BookingStatus.CANCELLED) {
            throw new BusinessException("Cannot modify cancelled booking", HttpStatus.BAD_REQUEST);
        }
        if (status == BookingStatus.REJECTED) {
            throw new BusinessException("Cannot modify rejected booking", HttpStatus.BAD_REQUEST);
        }
        if (status == BookingStatus.NO_SHOW) {
            throw new BusinessException("Cannot modify booking marked as no-show", HttpStatus.BAD_REQUEST);
        }

        // 4. Validate timing constraints per BR-BKG-01
        validateTimingConstraints(newStartTime, newEndTime);

        // 5. Concurrency serialization: lock vehicle and check operational status
        Vehicle vehicle = vehicleRepository.findByIdForUpdate(booking.getVehicle().getId())
                .orElseThrow(() -> new ResourceNotFoundException("Vehicle not found with id: " + booking.getVehicle().getId()));

        validateVehicleOperational(vehicle);

        // 6. Overlap & 30-minute buffer check excluding the current booking itself
        Instant bufferedStart = newStartTime.minus(BUFFER_MINUTES, ChronoUnit.MINUTES);
        Instant bufferedEnd = newEndTime.plus(BUFFER_MINUTES, ChronoUnit.MINUTES);

        List<Booking> conflicts = bookingRepository.findOverlappingBookingsWithBufferExcludingId(
                vehicle.getId(), booking.getId(), bufferedStart, bufferedEnd
        );

        if (!conflicts.isEmpty()) {
            log.warn("Reschedule conflict detected for vehicle [{}]: collision with {} booking(s) for interval [{} to {}]",
                    vehicle.getId(), conflicts.size(), newStartTime, newEndTime);
            throw new BookingConflictException(
                    "Requested rescheduled time slot conflicts with an existing reservation (including mandatory 30-minute turnaround buffer)"
            );
        }

        String oldStateJson = serializeBookingSnapshot(booking);

        // 7. Update timing and recalculate estimated cost
        booking.setStartTime(newStartTime);
        booking.setEndTime(newEndTime);
        long durationMinutes = ChronoUnit.MINUTES.between(newStartTime, newEndTime);
        BigDecimal hours = BigDecimal.valueOf(durationMinutes)
                .divide(BigDecimal.valueOf(60), 2, RoundingMode.HALF_UP);
        booking.setEstimatedCost(hours.multiply(DEFAULT_HOURLY_RATE_VND).setScale(2, RoundingMode.HALF_UP));

        Booking updated = bookingRepository.save(booking);
        String newStateJson = serializeBookingSnapshot(updated);

        recordAudit("UPDATE_BOOKING", updated.getId(), oldStateJson, newStateJson, currentUserId);
        log.info("Booking [{}] successfully updated to interval [{} to {}]", updated.getId(), newStartTime, newEndTime);

        return BookingResponse.fromEntity(updated);
    }

    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public BookingCancellationResponse cancelBooking(Long id, CancelBookingRequest request, Long currentUserId, boolean isAdmin) {
        if (id == null) {
            throw new BusinessException("Booking ID is required", HttpStatus.BAD_REQUEST);
        }

        // 1. Fetch booking with row lock
        Booking booking = bookingRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found with id: " + id));

        // 2. Authorization check: only booking owner or admin can cancel
        validateBookingModificationAccess(booking, currentUserId, isAdmin);

        // 3. Lifecycle state validation
        BookingStatus status = booking.getStatus();
        if (status == BookingStatus.CANCELLED) {
            throw new BusinessException("Booking is already cancelled", HttpStatus.BAD_REQUEST);
        }
        if (status == BookingStatus.COMPLETED) {
            throw new BusinessException("Cannot cancel a completed booking", HttpStatus.BAD_REQUEST);
        }
        if (status == BookingStatus.IN_USE) {
            throw new BusinessException("Cannot cancel an active reservation in progress", HttpStatus.BAD_REQUEST);
        }
        if (status == BookingStatus.NO_SHOW) {
            throw new BusinessException("Cannot cancel a booking marked as no-show", HttpStatus.BAD_REQUEST);
        }

        // Validate via BookingStateMachine
        bookingStateMachine.validateTransition(status, BookingStatus.CANCELLED);

        // 4. Cancellation rules evaluation per BR-BKG-03
        Instant now = Instant.now();
        Duration durationUntilStart = Duration.between(now, booking.getStartTime());
        boolean isFreeCancellation = !durationUntilStart.isNegative()
                && durationUntilStart.toMinutes() >= (FREE_CANCELLATION_HOURS_THRESHOLD * 60);

        BigDecimal cancellationFee;
        boolean penaltyApplied;
        String message;

        if (isFreeCancellation) {
            cancellationFee = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
            penaltyApplied = false;
            message = String.format("Reservation cancelled successfully with zero penalty (free cancellation window >= %d hours)",
                    FREE_CANCELLATION_HOURS_THRESHOLD);
        } else {
            // Late cancellation within 12 hours assesses 20% reservation fee and logs penalty per BR-BKG-03
            cancellationFee = booking.getEstimatedCost()
                    .multiply(LATE_CANCELLATION_PENALTY_RATE)
                    .setScale(2, RoundingMode.HALF_UP);
            penaltyApplied = true;
            message = String.format("Late cancellation executed within %d hours of start time: 20%% reservation fee assessed (%s VND) and penalty logged against fair usage profile (BR-BKG-03)",
                    FREE_CANCELLATION_HOURS_THRESHOLD, cancellationFee.toPlainString());
        }

        String oldStateJson = serializeBookingSnapshot(booking);

        // 5. Update status to CANCELLED
        booking.setStatus(BookingStatus.CANCELLED);
        Booking saved = bookingRepository.save(booking);

        // Controlled Vehicle State Release:
        // If vehicle is currently in BOOKED status, return it to AVAILABLE via state machine
        Vehicle vehicle = booking.getVehicle();
        if (vehicle != null && vehicle.getStatus() == VehicleStatus.BOOKED) {
            transitionVehicleStatus(vehicle, VehicleStatus.AVAILABLE, "Reservation cancelled (bookingId=" + saved.getId() + ")", currentUserId);
        }

        // 6. Record audit trail
        String cancellationReason = request != null ? request.getReason() : null;
        Map<String, Object> auditDetails = new LinkedHashMap<>();
        auditDetails.put("status", saved.getStatus().name());
        auditDetails.put("cancellationFee", cancellationFee);
        auditDetails.put("penaltyApplied", penaltyApplied);
        auditDetails.put("reason", cancellationReason);
        auditDetails.put("cancelledAt", now.toString());

        String newStateJson;
        try {
            newStateJson = objectMapper.writeValueAsString(auditDetails);
        } catch (Exception e) {
            newStateJson = serializeBookingSnapshot(saved);
        }

        recordAudit("CANCEL_BOOKING", saved.getId(), oldStateJson, newStateJson, currentUserId);
        log.info("Booking [{}] cancelled by user [{}]. Late={}, Fee={}, Penalty={}",
                saved.getId(), currentUserId, !isFreeCancellation, cancellationFee, penaltyApplied);

        return new BookingCancellationResponse(
                saved.getId(),
                saved.getStatus(),
                cancellationFee,
                !isFreeCancellation,
                penaltyApplied,
                cancellationReason,
                now,
                message
        );
    }

    @Override
    @Transactional(readOnly = true)
    public List<BookingHistoryResponse> getBookingHistory(Long id, Long currentUserId, boolean isAdmin) {
        if (id == null) {
            throw new BusinessException("Booking ID is required", HttpStatus.BAD_REQUEST);
        }

        Booking booking = bookingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found with id: " + id));

        validateBookingReadAccess(booking, currentUserId, isAdmin);

        List<AuditLog> logs = auditLogRepository.findByEntityNameAndEntityIdOrderByIdDesc("Booking", id);
        return logs.stream()
                .map(BookingHistoryResponse::fromAuditLog)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public PagedData<BookingResponse> getMyBookings(Long userId, BookingStatus status, Pageable pageable) {
        if (userId == null) {
            throw new AccessDeniedException("User must be authenticated to view reservations");
        }

        Page<Booking> page = status != null
                ? bookingRepository.findByUserIdAndStatus(userId, status, pageable)
                : bookingRepository.findByUserId(userId, pageable);

        List<BookingResponse> items = page.getContent().stream()
                .map(BookingResponse::fromEntity)
                .collect(Collectors.toList());

        return PagedData.of(items, page.getNumber(), page.getSize(), page.getTotalElements());
    }

    @Override
    @Transactional(readOnly = true)
    public PagedData<BookingResponse> getVehicleBookings(Long vehicleId, BookingStatus status, Long currentUserId, boolean isAdmin, Pageable pageable) {
        if (vehicleId == null) {
            throw new BusinessException("Vehicle ID is required", HttpStatus.BAD_REQUEST);
        }

        if (!isAdmin) {
            boolean isMember = ownershipSecurity.isVehicleGroupMember(vehicleId, currentUserId);
            if (!isMember) {
                throw new AccessDeniedException("User is not an authorized member of this vehicle's syndicate");
            }
        }

        Page<Booking> page = status != null
                ? bookingRepository.findByVehicleIdAndStatus(vehicleId, status, pageable)
                : bookingRepository.findByVehicleId(vehicleId, pageable);

        List<BookingResponse> items = page.getContent().stream()
                .map(BookingResponse::fromEntity)
                .collect(Collectors.toList());

        return PagedData.of(items, page.getNumber(), page.getSize(), page.getTotalElements());
    }

    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public BookingResponse transitionBookingStatus(Long id, UpdateBookingStatusRequest request, Long currentUserId, boolean isAdmin) {
        if (id == null) {
            throw new BusinessException("Booking ID is required", HttpStatus.BAD_REQUEST);
        }
        if (request == null || request.getStatus() == null) {
            throw new BusinessException("Target booking status is required", HttpStatus.BAD_REQUEST);
        }

        // 1. Fetch booking with row lock
        Booking booking = bookingRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found with id: " + id));

        // 2. Authorization check: booking owner, staff, or admin
        validateBookingModificationAccess(booking, currentUserId, isAdmin);

        BookingStatus currentStatus = booking.getStatus();
        BookingStatus targetStatus = request.getStatus();

        // 3. State machine validation
        bookingStateMachine.validateTransition(currentStatus, targetStatus);

        // 4. Special handling for cancellation: enforce full cancellation penalty logic per BR-BKG-03
        if (targetStatus == BookingStatus.CANCELLED) {
            cancelBooking(id, new CancelBookingRequest(request.getReason()), currentUserId, isAdmin);
            Booking reloaded = bookingRepository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException("Booking not found with id: " + id));
            return BookingResponse.fromEntity(reloaded);
        }

        // 5. Special handling for CONFIRMED: verify vehicle operational status and buffer overlap
        if (targetStatus == BookingStatus.CONFIRMED) {
            Vehicle vehicle = vehicleRepository.findByIdForUpdate(booking.getVehicle().getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Vehicle not found with id: " + booking.getVehicle().getId()));
            validateVehicleOperational(vehicle);

            Instant bufferedStart = booking.getStartTime().minus(BUFFER_MINUTES, ChronoUnit.MINUTES);
            Instant bufferedEnd = booking.getEndTime().plus(BUFFER_MINUTES, ChronoUnit.MINUTES);
            List<Booking> conflicts = bookingRepository.findOverlappingBookingsWithBufferExcludingId(
                    vehicle.getId(), booking.getId(), bufferedStart, bufferedEnd
            );
            if (!conflicts.isEmpty()) {
                throw new BookingConflictException("Cannot confirm booking: slot conflicts with another reservation (including 30-minute turnaround buffer)");
            }
        }

        String oldStateJson = serializeBookingSnapshot(booking);
        booking.setStatus(targetStatus);
        Booking updated = bookingRepository.save(booking);
        String newStateJson = serializeBookingSnapshot(updated);

        String action = "TRANSITION_BOOKING_STATUS";
        recordAudit(action, updated.getId(), oldStateJson, newStateJson, currentUserId);
        log.info("Booking [{}] transitioned from [{}] to [{}] by user [{}] (reason: '{}')",
                id, currentStatus, targetStatus, currentUserId, request.getReason());

        return BookingResponse.fromEntity(updated);
    }

    // --- Helper validation & audit methods ---

    private void validateTimingConstraints(Instant startTime, Instant endTime) {
        if (!endTime.isAfter(startTime)) {
            throw new BusinessException("End time must be strictly after start time", HttpStatus.BAD_REQUEST);
        }

        Instant now = Instant.now();
        if (startTime.isBefore(now)) {
            throw new BusinessException("Cannot schedule a reservation in the past", HttpStatus.BAD_REQUEST);
        }

        long durationMinutes = ChronoUnit.MINUTES.between(startTime, endTime);
        if (durationMinutes < MIN_BOOKING_MINUTES) {
            throw new BusinessException(
                    String.format("Minimum booking duration is %d minutes (BR-BKG-01)", MIN_BOOKING_MINUTES),
                    HttpStatus.BAD_REQUEST
            );
        }

        if (durationMinutes > (long) MAX_BOOKING_HOURS * 60) {
            throw new BusinessException(
                    String.format("Maximum continuous booking duration is %d hours (BR-BKG-01)", MAX_BOOKING_HOURS),
                    HttpStatus.BAD_REQUEST
            );
        }

        Instant maxAdvanceDate = now.plus(MAX_ADVANCE_DAYS, ChronoUnit.DAYS);
        if (startTime.isAfter(maxAdvanceDate)) {
            throw new BusinessException(
                    String.format("Bookings can only be scheduled up to %d days in advance (BR-BKG-01)", MAX_ADVANCE_DAYS),
                    HttpStatus.BAD_REQUEST
            );
        }
    }

    private void validateVehicleOperational(Vehicle vehicle) {
        if (vehicle.getStatus() == VehicleStatus.MAINTENANCE) {
            throw new BookingConflictException("Vehicle is currently under maintenance and cannot be booked");
        }
        if (vehicle.getStatus() == VehicleStatus.DAMAGED) {
            throw new BookingConflictException("Vehicle is flagged as damaged and cannot be operated");
        }
        if (vehicle.getStatus() == VehicleStatus.UNAVAILABLE) {
            throw new BookingConflictException("Vehicle is currently unavailable / decommissioned");
        }
    }

    private void validateBookingModificationAccess(Booking booking, Long currentUserId, boolean isAdmin) {
        if (isAdmin) {
            return;
        }
        boolean isOwner = booking.getUser() != null && currentUserId != null
                && currentUserId.equals(booking.getUser().getId());
        if (!isOwner) {
            log.warn("Unauthorized booking modification attempt: User [{}] is not owner of booking [{}]", currentUserId, booking.getId());
            throw new AccessDeniedException("You do not have permission to modify this reservation");
        }
    }

    private void validateBookingReadAccess(Booking booking, Long currentUserId, boolean isAdmin) {
        if (isAdmin || currentUserId == null) {
            return;
        }
        boolean isOwner = booking.getUser() != null && currentUserId.equals(booking.getUser().getId());
        boolean isSyndicateMember = ownershipSecurity.isVehicleGroupMember(booking.getVehicle().getId(), currentUserId);
        if (!isOwner && !isSyndicateMember) {
            log.warn("Access denied to booking [{}]: User [{}] is neither the owner nor a syndicate member", booking.getId(), currentUserId);
            throw new AccessDeniedException("You do not have permission to view this reservation");
        }
    }

    private String serializeBookingSnapshot(Booking booking) {
        if (booking == null) {
            return "{}";
        }
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", booking.getId());
        map.put("vehicleId", booking.getVehicle() != null ? booking.getVehicle().getId() : null);
        map.put("userId", booking.getUser() != null ? booking.getUser().getId() : null);
        map.put("startTime", booking.getStartTime() != null ? booking.getStartTime().toString() : null);
        map.put("endTime", booking.getEndTime() != null ? booking.getEndTime().toString() : null);
        map.put("status", booking.getStatus() != null ? booking.getStatus().name() : null);
        map.put("estimatedCost", booking.getEstimatedCost());
        try {
            return objectMapper.writeValueAsString(map);
        } catch (Exception e) {
            log.error("Failed to serialize booking snapshot for ID={}", booking.getId(), e);
            return "{}";
        }
    }

    private void recordAudit(String action, Long bookingId, String oldStateJson, String newStateJson, Long userId) {
        User user = userId != null ? userRepository.findById(userId).orElse(null) : null;
        AuditLog auditLog = new AuditLog();
        auditLog.setUser(user);
        auditLog.setAction(action);
        auditLog.setEntityName("Booking");
        auditLog.setEntityId(bookingId);
        auditLog.setOldStateJson(oldStateJson);
        auditLog.setNewStateJson(newStateJson);
        auditLog.setCreatedAt(Instant.now());
        auditLogRepository.save(auditLog);
    }

    /**
     * Executes a controlled vehicle state transition validated by the VehicleStateMachine.
     * Direct arbitrary mutation of vehicle state without state machine validation is strictly disallowed.
     */
    private void transitionVehicleStatus(Vehicle vehicle, VehicleStatus targetStatus, String reason, Long userId) {
        if (vehicle == null || targetStatus == null) {
            return;
        }
        VehicleStatus currentStatus = vehicle.getStatus();
        if (currentStatus == targetStatus) {
            return;
        }

        log.info("BookingService transitioning vehicleId={} from {} to {} (reason: '{}')",
                vehicle.getId(), currentStatus, targetStatus, reason);

        // Authoritative state machine transition guard
        vehicleStateMachine.validateTransition(currentStatus, targetStatus);

        vehicle.setStatus(targetStatus);
        Vehicle savedVehicle = vehicleRepository.save(vehicle);

        // Record immutable audit trail
        Map<String, Object> details = new LinkedHashMap<>();
        details.put("vehicleId", savedVehicle.getId());
        details.put("fromStatus", currentStatus != null ? currentStatus.name() : null);
        details.put("toStatus", targetStatus.name());
        details.put("reason", reason);
        String detailsJson = "{}";
        try {
            detailsJson = objectMapper.writeValueAsString(details);
        } catch (Exception ignored) {}

        recordAudit("TRANSITION_VEHICLE_STATE", savedVehicle.getId(), null, detailsJson, userId);
    }
}
