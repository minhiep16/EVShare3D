package com.example.evshare.service.impl;

import com.example.evshare.dto.request.CheckInRequest;
import com.example.evshare.dto.request.CheckOutRequest;
import com.example.evshare.dto.response.UsageSessionResponse;
import com.example.evshare.dto.response.VehicleInspectionResponse;
import com.example.evshare.entity.*;
import com.example.evshare.entity.enums.BookingStatus;
import com.example.evshare.entity.enums.InspectionType;
import com.example.evshare.entity.enums.RoleName;
import com.example.evshare.entity.enums.UsageSessionStatus;
import com.example.evshare.entity.enums.VehicleStatus;
import com.example.evshare.exception.BusinessException;
import com.example.evshare.exception.HistoricalUsageImmutableException;
import com.example.evshare.exception.InvalidUsageSessionException;
import com.example.evshare.exception.ResourceNotFoundException;
import com.example.evshare.repository.*;
import com.example.evshare.security.OwnershipSecurity;
import com.example.evshare.service.BookingStateMachine;
import com.example.evshare.service.UsageSessionService;
import com.example.evshare.service.VehicleStateMachine;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
public class UsageSessionServiceImpl implements UsageSessionService {

    private static final Logger log = LoggerFactory.getLogger(UsageSessionServiceImpl.class);

    private static final BigDecimal LOW_BATTERY_SURCHARGE_AMOUNT = new BigDecimal("150000.00");
    private static final BigDecimal LATE_RETURN_FEE_RATE = new BigDecimal("50000.00"); // per 30 mins
    private static final int MINIMUM_BATTERY_THRESHOLD = 20;

    private final UsageSessionRepository usageSessionRepository;
    private final BookingRepository bookingRepository;
    private final VehicleRepository vehicleRepository;
    private final VehicleInspectionRepository vehicleInspectionRepository;
    private final AuditLogRepository auditLogRepository;
    private final UserRepository userRepository;
    private final BookingStateMachine bookingStateMachine;
    private final VehicleStateMachine vehicleStateMachine;
    private final OwnershipSecurity ownershipSecurity;
    private final ObjectMapper objectMapper;

    public UsageSessionServiceImpl(UsageSessionRepository usageSessionRepository,
                                  BookingRepository bookingRepository,
                                  VehicleRepository vehicleRepository,
                                  VehicleInspectionRepository vehicleInspectionRepository,
                                  AuditLogRepository auditLogRepository,
                                  UserRepository userRepository,
                                  BookingStateMachine bookingStateMachine,
                                  VehicleStateMachine vehicleStateMachine,
                                  OwnershipSecurity ownershipSecurity,
                                  ObjectMapper objectMapper) {
        this.usageSessionRepository = usageSessionRepository;
        this.bookingRepository = bookingRepository;
        this.vehicleRepository = vehicleRepository;
        this.vehicleInspectionRepository = vehicleInspectionRepository;
        this.auditLogRepository = auditLogRepository;
        this.userRepository = userRepository;
        this.bookingStateMachine = bookingStateMachine;
        this.vehicleStateMachine = vehicleStateMachine;
        this.ownershipSecurity = ownershipSecurity;
        this.objectMapper = objectMapper;
    }

    @Override
    public UsageSessionResponse checkIn(CheckInRequest request, Long currentUserId) {
        log.info("Executing check-in for bookingId={} by userId={}", request.getBookingId(), currentUserId);

        // 1. Authenticated User Guard
        if (currentUserId == null) {
            throw new BusinessException("Authentication required: You must be authenticated to perform check-in.", HttpStatus.UNAUTHORIZED);
        }

        // 2. Valid Booking Guard
        Booking booking = bookingRepository.findById(request.getBookingId())
                .orElseThrow(() -> new ResourceNotFoundException("Booking", "id", request.getBookingId()));

        if (booking.getStatus() != BookingStatus.CONFIRMED && booking.getStatus() != BookingStatus.APPROVED) {
            throw new InvalidUsageSessionException(String.format(
                    "Cannot check in: Booking #%d is in status '%s'. Only CONFIRMED bookings can be checked in.",
                    booking.getId(), booking.getStatus()));
        }

        // 3. Correct Vehicle Guard
        Vehicle vehicle = booking.getVehicle();
        if (vehicle == null) {
            throw new InvalidUsageSessionException("Booking #" + booking.getId() + " is not associated with any vehicle.");
        }
        if (request.getVehicleId() != null && !vehicle.getId().equals(request.getVehicleId())) {
            throw new InvalidUsageSessionException(String.format(
                    "Vehicle mismatch: Request specifies vehicle #%d, but booking #%d is reserved for vehicle #%d (%s).",
                    request.getVehicleId(), booking.getId(), vehicle.getId(), vehicle.getModelName()));
        }

        // 4. Current Time Window Guard (BR-OPS-01)
        Instant now = Instant.now();
        if (booking.getStartTime() != null) {
            Instant checkInWindowStart = booking.getStartTime().minus(15, ChronoUnit.MINUTES);
            Instant checkInWindowEnd = booking.getStartTime().plus(30, ChronoUnit.MINUTES);

            if (now.isBefore(checkInWindowStart)) {
                throw new InvalidUsageSessionException(String.format(
                        "Check-in is not yet open for booking #%d. Check-in opens 15 minutes prior to scheduled start time (%s).",
                        booking.getId(), booking.getStartTime()));
            }

            if (now.isAfter(checkInWindowEnd)) {
                // Window expired: automatically transition booking to NO_SHOW per BR-OPS-01
                bookingStateMachine.validateTransition(booking.getStatus(), BookingStatus.NO_SHOW);
                booking.setStatus(BookingStatus.NO_SHOW);
                bookingRepository.save(booking);
                throw new InvalidUsageSessionException(String.format(
                        "Check-in window has expired (>30 minutes past scheduled start time %s). Booking #%d has been marked as NO_SHOW.",
                        booking.getStartTime(), booking.getId()));
            }
        }

        // 5. Booking Permission Guard
        validateUserAccess(booking, currentUserId, "check in for");

        // 6. Vehicle State Guard
        if (vehicle.getStatus() == VehicleStatus.IN_USE) {
            throw new InvalidUsageSessionException(String.format(
                    "Vehicle '%s' (%s) is currently in use by another session.",
                    vehicle.getModelName(), vehicle.getLicensePlate()));
        }
        if (vehicle.getStatus() != VehicleStatus.AVAILABLE && vehicle.getStatus() != VehicleStatus.BOOKED) {
            throw new InvalidUsageSessionException(String.format(
                    "Vehicle '%s' (%s) is not ready for check-in because it is in status '%s'.",
                    vehicle.getModelName(), vehicle.getLicensePlate(), vehicle.getStatus()));
        }

        // 7. No Duplicate Active Session Guard
        Optional<UsageSession> existingBookingSessionOpt = usageSessionRepository.findByBookingId(booking.getId());
        if (existingBookingSessionOpt.isPresent()) {
            UsageSession existing = existingBookingSessionOpt.get();
            if (existing.getStatus() == UsageSessionStatus.ACTIVE) {
                throw new InvalidUsageSessionException("Usage session [id=" + existing.getId() + "] is already active for booking #" + booking.getId());
            } else if (existing.getStatus() == UsageSessionStatus.COMPLETED) {
                throw new InvalidUsageSessionException("Usage session [id=" + existing.getId() + "] for booking #" + booking.getId() + " is already completed.");
            }
        }

        Optional<UsageSession> activeVehicleSession = usageSessionRepository.findByBookingVehicleIdAndStatus(vehicle.getId(), UsageSessionStatus.ACTIVE);
        if (activeVehicleSession.isPresent()) {
            throw new InvalidUsageSessionException(String.format(
                    "Vehicle '%s' (ID #%d) already has an active usage session (#%d) in progress.",
                    vehicle.getModelName(), vehicle.getId(), activeVehicleSession.get().getId()));
        }

        // Validate start odometer
        if (request.getStartOdometer() == null || request.getStartOdometer().compareTo(BigDecimal.ZERO) < 0) {
            throw new InvalidUsageSessionException("Start odometer reading cannot be negative.");
        }

        if (vehicle.getOdometerKm() != null && request.getStartOdometer().compareTo(vehicle.getOdometerKm()) < 0) {
            log.warn("Start odometer ({}) is less than vehicle's registered odometer ({}) for vehicleId={}",
                    request.getStartOdometer(), vehicle.getOdometerKm(), vehicle.getId());
        }

        // Validate start battery
        if (request.getStartBattery() == null || request.getStartBattery() < 0 || request.getStartBattery() > 100) {
            throw new InvalidUsageSessionException("Start battery SoC must be between 0% and 100%.");
        }

        // Transition Booking: CONFIRMED -> IN_USE
        bookingStateMachine.validateTransition(booking.getStatus(), BookingStatus.IN_USE);
        booking.setStatus(BookingStatus.IN_USE);
        bookingRepository.save(booking);

        // Transition Vehicle: AVAILABLE/BOOKED -> IN_USE
        if (vehicle.getStatus() == VehicleStatus.AVAILABLE) {
            vehicleStateMachine.validateTransition(vehicle.getStatus(), VehicleStatus.BOOKED);
            vehicle.setStatus(VehicleStatus.BOOKED);
        }
        vehicleStateMachine.validateTransition(vehicle.getStatus(), VehicleStatus.IN_USE);
        vehicle.setStatus(VehicleStatus.IN_USE);
        vehicle.setOdometerKm(request.getStartOdometer());
        vehicle.setBatteryLevel(request.getStartBattery());
        vehicleRepository.save(vehicle);

        // Create or update UsageSession
        UsageSession session = existingBookingSessionOpt.orElseGet(UsageSession::new);
        session.setBooking(booking);
        session.setStartOdometer(request.getStartOdometer());
        session.setStartBattery(request.getStartBattery());
        session.setCheckInTime(now);
        session.setStatus(UsageSessionStatus.ACTIVE);
        session = usageSessionRepository.save(session);

        // Create Initial Physical Inspection (CHECK_IN) if details provided
        List<VehicleInspectionResponse> inspectionResponses = new ArrayList<>();
        if (request.getInspectionNotes() != null || request.getConditionMeshFlags() != null ||
                (request.getEvidencePhotoUrls() != null && !request.getEvidencePhotoUrls().isEmpty())) {
            User inspector = userRepository.findById(currentUserId).orElse(booking.getUser());
            VehicleInspection inspection = new VehicleInspection();
            inspection.setUsageSession(session);
            inspection.setInspectorUser(inspector);
            inspection.setInspectionType(InspectionType.CHECK_IN);
            inspection.setConditionMeshFlags(request.getConditionMeshFlags());
            inspection.setNotes(formatInspectionNotesWithPhotos(request.getInspectionNotes(), request.getEvidencePhotoUrls()));
            inspection.setCreatedAt(now);
            inspection = vehicleInspectionRepository.save(inspection);
            session.addInspection(inspection);
            inspectionResponses.add(VehicleInspectionResponse.fromEntity(inspection));
        }

        // Record immutable AuditLog
        recordAuditLog(currentUserId, "USAGE_SESSION_CHECK_IN", session.getId(), null, session);

        log.info("Check-in successful: sessionId={}, vehicleId={}, bookingId={}",
                session.getId(), vehicle.getId(), booking.getId());

        return UsageSessionResponse.fromEntity(session, BigDecimal.ZERO, new HashMap<>(), inspectionResponses);
    }

    @Override
    public UsageSessionResponse checkOut(Long sessionId, CheckOutRequest request, Long currentUserId) {
        log.info("Executing check-out for sessionId={} by userId={}", sessionId, currentUserId);

        UsageSession session = usageSessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("UsageSession", "id", sessionId));

        // 1. Active Usage Session Guard
        if (session.getStatus() == UsageSessionStatus.COMPLETED) {
            log.warn("Immutability violation attempt: Session [id={}] is already COMPLETED and cannot be rewritten.", sessionId);
            throw new HistoricalUsageImmutableException(sessionId);
        }

        if (session.getStatus() != UsageSessionStatus.ACTIVE) {
            throw new InvalidUsageSessionException(String.format(
                    "Usage session #%d cannot be checked out because it is in status '%s'.",
                    sessionId, session.getStatus()));
        }

        Booking booking = session.getBooking();
        if (booking == null) {
            throw new InvalidUsageSessionException("Usage session #" + sessionId + " is not associated with any booking reservation.");
        }
        if (booking.getStatus() != BookingStatus.IN_USE) {
            throw new InvalidUsageSessionException(String.format(
                    "Cannot check out session #%d: Associated booking #%d is in status '%s', expected 'IN_USE'.",
                    sessionId, booking.getId(), booking.getStatus()));
        }

        // 2. Correct User Guard
        if (currentUserId == null) {
            throw new BusinessException("Authentication required to check out.", HttpStatus.UNAUTHORIZED);
        }
        if (request.getUserId() != null && !currentUserId.equals(request.getUserId())
                && (booking.getUser() == null || !booking.getUser().getId().equals(request.getUserId()))) {
            throw new InvalidUsageSessionException(String.format(
                    "User mismatch: Request specifies user #%d, but session #%d is reserved for user #%d.",
                    request.getUserId(), sessionId, booking.getUser() != null ? booking.getUser().getId() : null));
        }
        validateUserAccess(booking, currentUserId, "check out");

        // 3. Correct Vehicle Guard
        Vehicle vehicle = booking.getVehicle();
        if (vehicle == null) {
            throw new InvalidUsageSessionException("Booking #" + booking.getId() + " is not associated with any vehicle.");
        }
        if (request.getVehicleId() != null && !vehicle.getId().equals(request.getVehicleId())) {
            throw new InvalidUsageSessionException(String.format(
                    "Vehicle mismatch: Request specifies vehicle #%d, but session #%d is for vehicle #%d (%s).",
                    request.getVehicleId(), sessionId, vehicle.getId(), vehicle.getModelName()));
        }
        if (vehicle.getStatus() != VehicleStatus.IN_USE) {
            throw new InvalidUsageSessionException(String.format(
                    "Cannot check out session #%d: Vehicle #%d is currently in status '%s', expected 'IN_USE'.",
                    sessionId, vehicle.getId(), vehicle.getStatus()));
        }

        // 4. Telemetry Validation
        if (request.getEndOdometer() == null || request.getEndOdometer().compareTo(BigDecimal.ZERO) < 0) {
            throw new InvalidUsageSessionException("End odometer reading cannot be negative.");
        }
        if (request.getEndOdometer().compareTo(session.getStartOdometer()) < 0) {
            throw new InvalidUsageSessionException(String.format(
                    "End odometer (%s km) cannot be less than start odometer (%s km).",
                    request.getEndOdometer(), session.getStartOdometer()));
        }

        if (request.getEndBattery() == null || request.getEndBattery() < 0 || request.getEndBattery() > 100) {
            throw new InvalidUsageSessionException("End battery SoC must be between 0% and 100%.");
        }

        Instant checkOutTime = Instant.now();

        // Calculate Additional Costs / Surcharges deterministically
        Map<String, BigDecimal> costBreakdown = new LinkedHashMap<>();
        BigDecimal totalAdditionalCost = BigDecimal.ZERO;

        // BR-OPS-02: Minimum Battery Rule (150,000 VND surcharge if < 20% and not plugged in)
        boolean isLowBattery = request.getEndBattery() < MINIMUM_BATTERY_THRESHOLD;
        boolean isPluggedIn = Boolean.TRUE.equals(request.getIsPluggedIn());
        if (isLowBattery && !isPluggedIn) {
            costBreakdown.put("LOW_BATTERY_SURCHARGE", LOW_BATTERY_SURCHARGE_AMOUNT);
            totalAdditionalCost = totalAdditionalCost.add(LOW_BATTERY_SURCHARGE_AMOUNT);
            log.info("Assessed low-battery surcharge (150,000 VND) for sessionId={}: SoC returned at {}% without plug-in",
                    sessionId, request.getEndBattery());
        }

        // Late return fee evaluation (>15 min grace period)
        if (booking.getEndTime() != null && checkOutTime.isAfter(booking.getEndTime())) {
            long overdueMinutes = Duration.between(booking.getEndTime(), checkOutTime).toMinutes();
            if (overdueMinutes > 15) {
                long unitsOf30Min = (overdueMinutes + 29) / 30;
                BigDecimal lateFee = BigDecimal.valueOf(unitsOf30Min).multiply(LATE_RETURN_FEE_RATE).setScale(2, RoundingMode.HALF_UP);
                costBreakdown.put("LATE_RETURN_FEE", lateFee);
                totalAdditionalCost = totalAdditionalCost.add(lateFee);
                log.info("Assessed late return fee ({}) for sessionId={}: overdue by {} minutes", lateFee, sessionId, overdueMinutes);
            }
        }

        // Other explicit surcharges (e.g. cleaning, tolls, damages)
        if (request.getOtherAdditionalCost() != null && request.getOtherAdditionalCost().compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal otherCost = request.getOtherAdditionalCost().setScale(2, RoundingMode.HALF_UP);
            costBreakdown.put("OTHER_ADDITIONAL_COST", otherCost);
            totalAdditionalCost = totalAdditionalCost.add(otherCost);
        }

        // Snapshot old state for AuditLog before mutations
        UsageSession oldStateSnapshot = copySessionState(session);

        // Update UsageSession final telemetry
        session.setEndOdometer(request.getEndOdometer());
        session.setEndBattery(request.getEndBattery());
        session.setCheckOutTime(checkOutTime);
        session.setStatus(UsageSessionStatus.COMPLETED);
        session = usageSessionRepository.save(session);

        // Record check-out inspection
        List<VehicleInspectionResponse> inspectionResponses = new ArrayList<>();
        User inspector = userRepository.findById(currentUserId).orElse(booking.getUser());
        VehicleInspection checkOutInspection = new VehicleInspection();
        checkOutInspection.setUsageSession(session);
        checkOutInspection.setInspectorUser(inspector);
        checkOutInspection.setInspectionType(InspectionType.CHECK_OUT);
        checkOutInspection.setConditionMeshFlags(request.getConditionMeshFlags());
        checkOutInspection.setNotes(formatInspectionNotesWithPhotos(request.getInspectionNotes(), request.getEvidencePhotoUrls()));
        checkOutInspection.setCreatedAt(checkOutTime);
        checkOutInspection = vehicleInspectionRepository.save(checkOutInspection);
        session.addInspection(checkOutInspection);

        // Retrieve all inspections for the session
        List<VehicleInspection> allInspections = vehicleInspectionRepository.findByUsageSessionIdOrderByCreatedAtAsc(sessionId);
        for (VehicleInspection insp : allInspections) {
            inspectionResponses.add(VehicleInspectionResponse.fromEntity(insp));
        }

        // Synchronize Vehicle State
        vehicle.setOdometerKm(request.getEndOdometer());
        vehicle.setBatteryLevel(request.getEndBattery());
        VehicleStatus targetVehicleStatus;
        if (Boolean.TRUE.equals(request.getHasDamage())) {
            targetVehicleStatus = VehicleStatus.DAMAGED;
        } else if (isPluggedIn) {
            targetVehicleStatus = VehicleStatus.CHARGING;
        } else {
            targetVehicleStatus = VehicleStatus.AVAILABLE;
        }
        vehicleStateMachine.validateTransition(vehicle.getStatus(), targetVehicleStatus);
        vehicle.setStatus(targetVehicleStatus);
        vehicleRepository.save(vehicle);

        // Synchronize Booking State: IN_USE -> COMPLETED
        bookingStateMachine.validateTransition(booking.getStatus(), BookingStatus.COMPLETED);
        booking.setStatus(BookingStatus.COMPLETED);
        bookingRepository.save(booking);

        // Record immutable AuditLog
        recordAuditLog(currentUserId, "USAGE_SESSION_CHECK_OUT", session.getId(), oldStateSnapshot, session);

        log.info("Check-out completed: sessionId={}, vehicleId={}, mileage={} km, batteryDelta={}%",
                session.getId(), vehicle.getId(),
                session.getEndOdometer().subtract(session.getStartOdometer()),
                session.getStartBattery() - session.getEndBattery());

        return UsageSessionResponse.fromEntity(session, totalAdditionalCost, costBreakdown, inspectionResponses);
    }

    @Override
    @Transactional(readOnly = true)
    public UsageSessionResponse getSessionById(Long sessionId, Long currentUserId) {
        UsageSession session = usageSessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("UsageSession", "id", sessionId));

        validateUserAccess(session.getBooking(), currentUserId, "view");

        return mapToResponseWithBreakdown(session);
    }

    @Override
    @Transactional(readOnly = true)
    public UsageSessionResponse getSessionByBookingId(Long bookingId, Long currentUserId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking", "id", bookingId));

        validateUserAccess(booking, currentUserId, "view");

        UsageSession session = usageSessionRepository.findByBookingId(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("UsageSession", "bookingId", bookingId));

        return mapToResponseWithBreakdown(session);
    }

    @Override
    @Transactional(readOnly = true)
    public List<UsageSessionResponse> getSessionsByVehicle(Long vehicleId, Long currentUserId) {
        vehicleRepository.findById(vehicleId)
                .orElseThrow(() -> new ResourceNotFoundException("Vehicle", "id", vehicleId));

        if (currentUserId != null && !ownershipSecurity.isVehicleGroupMember(vehicleId, currentUserId)) {
            // Check if admin, otherwise restrict
            User user = userRepository.findById(currentUserId).orElse(null);
            boolean isAdmin = user != null && user.getRoles().stream()
                    .anyMatch(r -> r.getName() == RoleName.ROLE_ADMIN);
            if (!isAdmin) {
                throw new BusinessException("Access denied: You are not a co-owner of this vehicle.", HttpStatus.FORBIDDEN);
            }
        }

        List<UsageSession> sessions = usageSessionRepository.findByBookingVehicleIdOrderByCheckInTimeDesc(vehicleId);
        return sessions.stream().map(this::mapToResponseWithBreakdown).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<UsageSessionResponse> getMySessions(Long currentUserId) {
        if (currentUserId == null) {
            return Collections.emptyList();
        }

        List<UsageSession> sessions = usageSessionRepository.findByBookingUserIdOrderByCheckInTimeDesc(currentUserId);
        return sessions.stream().map(this::mapToResponseWithBreakdown).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<VehicleInspectionResponse> getInspections(Long sessionId, Long currentUserId) {
        UsageSession session = usageSessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("UsageSession", "id", sessionId));

        validateUserAccess(session.getBooking(), currentUserId, "view inspections for");

        return vehicleInspectionRepository.findByUsageSessionIdOrderByCreatedAtAsc(sessionId)
                .stream()
                .map(VehicleInspectionResponse::fromEntity)
                .collect(Collectors.toList());
    }

    private UsageSessionResponse mapToResponseWithBreakdown(UsageSession session) {
        Map<String, BigDecimal> breakdown = new HashMap<>();
        BigDecimal additionalCost = BigDecimal.ZERO;

        if (session.getStatus() == UsageSessionStatus.COMPLETED && session.getEndBattery() != null) {
            if (session.getEndBattery() < MINIMUM_BATTERY_THRESHOLD) {
                breakdown.put("LOW_BATTERY_SURCHARGE", LOW_BATTERY_SURCHARGE_AMOUNT);
                additionalCost = additionalCost.add(LOW_BATTERY_SURCHARGE_AMOUNT);
            }
        }

        List<VehicleInspectionResponse> inspections = vehicleInspectionRepository
                .findByUsageSessionIdOrderByCreatedAtAsc(session.getId())
                .stream()
                .map(VehicleInspectionResponse::fromEntity)
                .collect(Collectors.toList());

        return UsageSessionResponse.fromEntity(session, additionalCost, breakdown, inspections);
    }

    private void validateUserAccess(Booking booking, Long currentUserId, String action) {
        if (currentUserId == null || booking == null) {
            return;
        }

        boolean isBookingOwner = booking.getUser() != null && booking.getUser().getId().equals(currentUserId);
        boolean isCoOwner = booking.getVehicle() != null &&
                ownershipSecurity.isVehicleGroupMember(booking.getVehicle().getId(), currentUserId);

        User currentUser = userRepository.findById(currentUserId).orElse(null);
        boolean isAdmin = currentUser != null && currentUser.getRoles().stream()
                .anyMatch(r -> r.getName() == RoleName.ROLE_ADMIN);

        if (!isBookingOwner && !isCoOwner && !isAdmin) {
            throw new BusinessException(String.format("Access denied: You are not authorized to %s this session.", action),
                    HttpStatus.FORBIDDEN);
        }
    }

    private String formatInspectionNotesWithPhotos(String notes, List<String> photoUrls) {
        if (photoUrls == null || photoUrls.isEmpty()) {
            return notes;
        }

        StringBuilder sb = new StringBuilder();
        if (notes != null && !notes.isBlank()) {
            sb.append(notes.trim()).append("\n\n");
        }
        sb.append("Photographic Evidence [").append(photoUrls.size()).append(" item(s)]:\n");
        for (String url : photoUrls) {
            sb.append("- ").append(url).append("\n");
        }
        return sb.toString().trim();
    }

    private UsageSession copySessionState(UsageSession source) {
        UsageSession copy = new UsageSession();
        copy.setId(source.getId());
        copy.setBooking(source.getBooking());
        copy.setStartOdometer(source.getStartOdometer());
        copy.setEndOdometer(source.getEndOdometer());
        copy.setStartBattery(source.getStartBattery());
        copy.setEndBattery(source.getEndBattery());
        copy.setCheckInTime(source.getCheckInTime());
        copy.setCheckOutTime(source.getCheckOutTime());
        copy.setStatus(source.getStatus());
        return copy;
    }

    private String serializeSessionState(UsageSession session) {
        if (session == null) {
            return null;
        }
        try {
            Map<String, Object> stateMap = new LinkedHashMap<>();
            stateMap.put("id", session.getId());
            stateMap.put("bookingId", session.getBooking() != null ? session.getBooking().getId() : null);
            stateMap.put("startOdometer", session.getStartOdometer());
            stateMap.put("endOdometer", session.getEndOdometer());
            stateMap.put("startBattery", session.getStartBattery());
            stateMap.put("endBattery", session.getEndBattery());
            stateMap.put("checkInTime", session.getCheckInTime() != null ? session.getCheckInTime().toString() : null);
            stateMap.put("checkOutTime", session.getCheckOutTime() != null ? session.getCheckOutTime().toString() : null);
            stateMap.put("status", session.getStatus() != null ? session.getStatus().name() : null);
            return objectMapper.writeValueAsString(stateMap);
        } catch (Exception e) {
            log.warn("Failed to serialize session state: {}", e.getMessage());
            return null;
        }
    }

    private void recordAuditLog(Long actorUserId, String action, Long sessionId, UsageSession oldState, UsageSession newState) {
        try {
            User actor = actorUserId != null ? userRepository.findById(actorUserId).orElse(null) : null;
            String oldStateJson = serializeSessionState(oldState);
            String newStateJson = serializeSessionState(newState);

            AuditLog logEntry = new AuditLog();
            logEntry.setUser(actor);
            logEntry.setAction(action);
            logEntry.setEntityName("UsageSession");
            logEntry.setEntityId(sessionId);
            logEntry.setOldStateJson(oldStateJson);
            logEntry.setNewStateJson(newStateJson);
            logEntry.setCreatedAt(Instant.now());
            auditLogRepository.save(logEntry);
        } catch (Exception e) {
            log.warn("Failed to save audit log for UsageSession [id={}]: {}", sessionId, e.getMessage());
        }
    }
}
