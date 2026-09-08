package com.example.evshare.service.impl;

import com.example.evshare.dto.request.QrValidationRequest;
import com.example.evshare.dto.response.QrCodeResponse;
import com.example.evshare.dto.response.QrValidationResponse;
import com.example.evshare.entity.Booking;
import com.example.evshare.entity.User;
import com.example.evshare.entity.Vehicle;
import com.example.evshare.entity.enums.BookingStatus;
import com.example.evshare.entity.enums.RoleName;
import com.example.evshare.entity.enums.VehicleStatus;
import com.example.evshare.exception.BusinessException;
import com.example.evshare.exception.InvalidQrException;
import com.example.evshare.exception.ResourceNotFoundException;
import com.example.evshare.repository.BookingRepository;
import com.example.evshare.repository.UserRepository;
import com.example.evshare.repository.VehicleRepository;
import com.example.evshare.security.OwnershipSecurity;
import com.example.evshare.security.TokenService;
import com.example.evshare.service.BookingStateMachine;
import com.example.evshare.service.QrValidationService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Service
@Transactional
public class QrValidationServiceImpl implements QrValidationService {

    private static final Logger log = LoggerFactory.getLogger(QrValidationServiceImpl.class);
    private static final Duration QR_TOKEN_TTL = Duration.ofMinutes(5);

    private final BookingRepository bookingRepository;
    private final VehicleRepository vehicleRepository;
    private final UserRepository userRepository;
    private final TokenService tokenService;
    private final OwnershipSecurity ownershipSecurity;
    private final BookingStateMachine bookingStateMachine;

    public QrValidationServiceImpl(BookingRepository bookingRepository,
                                  VehicleRepository vehicleRepository,
                                  UserRepository userRepository,
                                  TokenService tokenService,
                                  OwnershipSecurity ownershipSecurity,
                                  BookingStateMachine bookingStateMachine) {
        this.bookingRepository = bookingRepository;
        this.vehicleRepository = vehicleRepository;
        this.userRepository = userRepository;
        this.tokenService = tokenService;
        this.ownershipSecurity = ownershipSecurity;
        this.bookingStateMachine = bookingStateMachine;
    }

    @Override
    public QrCodeResponse generateCheckInQr(Long bookingId, Long currentUserId) {
        log.info("Generating QR check-in token for bookingId={} by userId={}", bookingId, currentUserId);

        // 1. Authenticated User Guard
        if (currentUserId == null) {
            throw new BusinessException("Authentication required to generate QR code.", HttpStatus.UNAUTHORIZED);
        }

        // 2. Authoritative Booking Lookup
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking", "id", bookingId));

        // 3. Valid Booking Status
        if (booking.getStatus() != BookingStatus.CONFIRMED && booking.getStatus() != BookingStatus.APPROVED) {
            throw new InvalidQrException(String.format(
                    "QR check-in code can only be generated for CONFIRMED bookings. Current status: '%s'.",
                    booking.getStatus()));
        }

        // 4. Authorization Guard
        validateUserAccess(booking, currentUserId, "generate QR code for");

        // 5. Vehicle Lookup
        Vehicle vehicle = booking.getVehicle();
        if (vehicle == null) {
            throw new InvalidQrException("Booking #" + bookingId + " is not associated with any vehicle.");
        }

        // 6. Time Window Guard (BR-OPS-01)
        Instant now = Instant.now();
        if (booking.getStartTime() != null) {
            Instant qrAvailableStart = booking.getStartTime().minus(15, ChronoUnit.MINUTES);
            Instant checkInWindowEnd = booking.getStartTime().plus(30, ChronoUnit.MINUTES);

            if (now.isBefore(qrAvailableStart)) {
                throw new InvalidQrException(String.format(
                        "QR check-in token cannot be generated yet. Tokens become available 15 minutes prior to scheduled start time (%s).",
                        booking.getStartTime()));
            }

            if (now.isAfter(checkInWindowEnd)) {
                throw new InvalidQrException(String.format(
                        "Check-in window has expired (>30 minutes past scheduled start time %s).",
                        booking.getStartTime()));
            }
        }

        // 7. Cryptographic Token Generation (Zero sensitive data inside QR)
        Long authorizedUserId = booking.getUser() != null ? booking.getUser().getId() : currentUserId;
        String qrToken = tokenService.generateQrToken(booking.getId(), vehicle.getId(), authorizedUserId, QR_TOKEN_TTL);
        Instant expiresAt = now.plus(QR_TOKEN_TTL);

        log.info("QR check-in token generated successfully for bookingId={}, vehicleId={}, expiresAt={}",
                booking.getId(), vehicle.getId(), expiresAt);

        return new QrCodeResponse(
                qrToken,
                booking.getId(),
                vehicle.getId(),
                vehicle.getLicensePlate(),
                vehicle.getModelName(),
                now,
                expiresAt,
                QR_TOKEN_TTL.toSeconds()
        );
    }

    @Override
    public QrValidationResponse validateQr(QrValidationRequest request, Long currentUserId) {
        log.info("Executing QR validation by userId={}, targetVehicleId={}, targetBookingId={}",
                currentUserId,
                request != null ? request.getVehicleId() : null,
                request != null ? request.getBookingId() : null);

        // 1. Authenticated User Guard
        if (currentUserId == null) {
            throw new BusinessException("Authentication required to validate QR token.", HttpStatus.UNAUTHORIZED);
        }

        // 2. Token / Code Presence
        if (request == null || request.getQrToken() == null || request.getQrToken().trim().isEmpty()) {
            throw new InvalidQrException("QR token is required and cannot be empty.");
        }

        String qrToken = request.getQrToken().trim();

        // 3. Cryptographic Signature & Expiration Verification
        Claims claims;
        try {
            claims = tokenService.extractAllClaims(qrToken);
        } catch (ExpiredJwtException e) {
            log.warn("QR token validation failed: Token has expired at {}", e.getClaims() != null ? e.getClaims().getExpiration() : "unknown");
            throw new InvalidQrException("QR token has expired. Please refresh your QR code.");
        } catch (JwtException | IllegalArgumentException e) {
            log.warn("QR token validation failed: Invalid cryptographic signature or malformed payload ({})", e.getMessage());
            throw new InvalidQrException("Invalid QR token: Cryptographic signature verification failed or token is malformed.");
        }

        // Verify tokenType claim
        String tokenType = claims.get("tokenType", String.class);
        if (!"QR_CHECK_IN".equalsIgnoreCase(tokenType)) {
            throw new InvalidQrException(String.format(
                    "Invalid token type in QR code: Expected 'QR_CHECK_IN', got '%s'.", tokenType));
        }

        // 4. Extract Claims (Never trust QR data alone)
        Long tokenBookingId;
        Long tokenVehicleId;
        try {
            Object bId = claims.get("bookingId");
            tokenBookingId = bId instanceof Number ? ((Number) bId).longValue() : null;

            Object vId = claims.get("vehicleId");
            tokenVehicleId = vId instanceof Number ? ((Number) vId).longValue() : null;
        } catch (Exception e) {
            throw new InvalidQrException("Malformed QR token: Failed to extract reservation claims.");
        }

        if (tokenBookingId == null) {
            throw new InvalidQrException("Malformed QR token: Missing booking reservation reference.");
        }

        // 5. Cross-Check Target Booking against QR Token
        if (request.getBookingId() != null && !request.getBookingId().equals(tokenBookingId)) {
            throw new InvalidQrException(String.format(
                    "Booking mismatch: Request specifies booking #%d, but QR token is for booking #%d.",
                    request.getBookingId(), tokenBookingId));
        }

        // 6. Authoritative Database Lookup: Booking & Status Guard
        Booking booking = bookingRepository.findById(tokenBookingId)
                .orElseThrow(() -> new InvalidQrException(String.format(
                        "Booking #%d referenced in QR token does not exist.", tokenBookingId)));

        if (booking.getStatus() != BookingStatus.CONFIRMED && booking.getStatus() != BookingStatus.APPROVED) {
            throw new InvalidQrException(String.format(
                    "Booking #%d is in status '%s'. Only CONFIRMED bookings can be validated for check-in.",
                    booking.getId(), booking.getStatus()));
        }

        // 7. Authoritative Database Lookup: Vehicle & Cross-Match Guard
        Vehicle vehicle = booking.getVehicle();
        if (vehicle == null) {
            throw new InvalidQrException(String.format(
                    "Booking #%d is not associated with any vehicle.", booking.getId()));
        }

        if (tokenVehicleId != null && !vehicle.getId().equals(tokenVehicleId)) {
            throw new InvalidQrException(String.format(
                    "Vehicle mismatch: QR token specifies vehicle #%d, but booking #%d is reserved for vehicle #%d (%s).",
                    tokenVehicleId, booking.getId(), vehicle.getId(), vehicle.getModelName()));
        }

        if (request.getVehicleId() != null && !vehicle.getId().equals(request.getVehicleId())) {
            throw new InvalidQrException(String.format(
                    "Vehicle mismatch: Scanned vehicle #%d does not match booking reservation vehicle #%d (%s).",
                    request.getVehicleId(), vehicle.getId(), vehicle.getModelName()));
        }

        // Vehicle State Guard
        if (vehicle.getStatus() != VehicleStatus.AVAILABLE && vehicle.getStatus() != VehicleStatus.BOOKED) {
            throw new InvalidQrException(String.format(
                    "Vehicle #%d (%s) is not ready for check-in: currently in status '%s'.",
                    vehicle.getId(), vehicle.getModelName(), vehicle.getStatus()));
        }

        // 8. Time Window Verification (BR-OPS-01)
        Instant now = Instant.now();
        Instant checkInWindowStart = null;
        Instant checkInWindowEnd = null;

        if (booking.getStartTime() != null) {
            checkInWindowStart = booking.getStartTime().minus(15, ChronoUnit.MINUTES);
            checkInWindowEnd = booking.getStartTime().plus(30, ChronoUnit.MINUTES);

            if (now.isBefore(checkInWindowStart)) {
                throw new InvalidQrException(String.format(
                        "Check-in window is not yet open for booking #%d. Check-in opens 15 minutes prior to scheduled start time (%s).",
                        booking.getId(), booking.getStartTime()));
            }

            if (now.isAfter(checkInWindowEnd)) {
                // Automatically transition booking to NO_SHOW per BR-OPS-01
                bookingStateMachine.validateTransition(booking.getStatus(), BookingStatus.NO_SHOW);
                booking.setStatus(BookingStatus.NO_SHOW);
                bookingRepository.save(booking);
                throw new InvalidQrException(String.format(
                        "Check-in window has expired (>30 minutes past scheduled start time %s). Booking #%d has been marked as NO_SHOW.",
                        booking.getStartTime(), booking.getId()));
            }
        }

        // 9. Authorization Guard
        validateUserAccess(booking, currentUserId, "validate QR access for");

        log.info("QR validation succeeded: bookingId={}, vehicleId={}, userId={}",
                booking.getId(), vehicle.getId(), currentUserId);

        return QrValidationResponse.valid(
                booking.getId(),
                vehicle.getId(),
                vehicle.getLicensePlate(),
                vehicle.getModelName(),
                booking.getUser() != null ? booking.getUser().getId() : null,
                booking.getUser() != null ? booking.getUser().getFullName() : null,
                checkInWindowStart,
                checkInWindowEnd,
                "QR validation successful. Vehicle access granted."
        );
    }

    private void validateUserAccess(Booking booking, Long currentUserId, String action) {
        if (currentUserId == null) {
            throw new BusinessException(String.format("Authentication required to %s this booking.", action),
                    HttpStatus.UNAUTHORIZED);
        }

        boolean isBookingOwner = booking.getUser() != null && booking.getUser().getId().equals(currentUserId);
        boolean isCoOwner = booking.getVehicle() != null &&
                ownershipSecurity.isCoOwnerOfVehicle(currentUserId, booking.getVehicle().getId());
        boolean isAdminOrStaff = userRepository.findById(currentUserId)
                .map(u -> u.getRoles().stream().anyMatch(r ->
                        r.getName() == RoleName.ROLE_ADMIN || r.getName() == RoleName.ROLE_STAFF))
                .orElse(false);

        if (!isBookingOwner && !isCoOwner && !isAdminOrStaff) {
            throw new BusinessException(String.format(
                    "Access denied: You are not authorized to %s this booking or vehicle.", action),
                    HttpStatus.FORBIDDEN);
        }
    }
}
