package com.example.evshare.service;

import com.example.evshare.dto.request.QrValidationRequest;
import com.example.evshare.dto.response.QrCodeResponse;
import com.example.evshare.dto.response.QrValidationResponse;
import com.example.evshare.entity.Booking;
import com.example.evshare.entity.Role;
import com.example.evshare.entity.User;
import com.example.evshare.entity.Vehicle;
import com.example.evshare.entity.enums.BookingStatus;
import com.example.evshare.entity.enums.RoleName;
import com.example.evshare.entity.enums.VehicleStatus;
import com.example.evshare.exception.BusinessException;
import com.example.evshare.exception.InvalidQrException;
import com.example.evshare.exception.ResourceNotFoundException;
import com.example.evshare.repository.*;
import com.example.evshare.security.OwnershipSecurity;
import com.example.evshare.security.TokenService;
import com.example.evshare.service.impl.QrValidationServiceImpl;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Checkpoint 05-L — QR Validation Service Unit Tests")
class QrValidationServiceTest {

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private VehicleRepository vehicleRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private TokenService tokenService;

    @Mock
    private OwnershipShareRepository ownershipShareRepository;

    @Mock
    private CoOwnershipContractRepository coOwnershipContractRepository;

    @Mock
    private OwnershipGroupRepository ownershipGroupRepository;

    private OwnershipSecurity ownershipSecurity;
    private BookingStateMachine bookingStateMachine;
    private QrValidationServiceImpl qrValidationService;

    private User bookingUser;
    private User otherUser;
    private User staffUser;
    private Vehicle vehicle;
    private Booking booking;

    private static final Long USER_ID = 1L;
    private static final Long OTHER_USER_ID = 2L;
    private static final Long STAFF_USER_ID = 3L;
    private static final Long VEHICLE_ID = 100L;
    private static final Long BOOKING_ID = 500L;
    private static final String VALID_TOKEN = "valid.jwt.qr.token";

    @BeforeEach
    void setUp() {
        ownershipSecurity = new OwnershipSecurity(ownershipShareRepository, coOwnershipContractRepository, ownershipGroupRepository);
        bookingStateMachine = new BookingStateMachine();
        qrValidationService = new QrValidationServiceImpl(
                bookingRepository,
                vehicleRepository,
                userRepository,
                tokenService,
                ownershipSecurity,
                bookingStateMachine
        );

        bookingUser = new User();
        bookingUser.setId(USER_ID);
        bookingUser.setEmail("user@example.com");
        bookingUser.setFullName("Nguyen Van User");

        otherUser = new User();
        otherUser.setId(OTHER_USER_ID);
        otherUser.setEmail("other@example.com");
        otherUser.setFullName("Tran Van Other");

        staffUser = new User();
        staffUser.setId(STAFF_USER_ID);
        staffUser.setEmail("staff@example.com");
        staffUser.setFullName("Staff Operator");
        Role staffRole = new Role(RoleName.ROLE_STAFF);
        staffUser.setRoles(Set.of(staffRole));

        vehicle = new Vehicle();
        vehicle.setId(VEHICLE_ID);
        vehicle.setLicensePlate("51H-999.88");
        vehicle.setModelName("VinFast VF8");
        vehicle.setStatus(VehicleStatus.AVAILABLE);

        Instant now = Instant.now();
        booking = new Booking();
        booking.setId(BOOKING_ID);
        booking.setUser(bookingUser);
        booking.setVehicle(vehicle);
        booking.setStatus(BookingStatus.CONFIRMED);
        booking.setStartTime(now);
        booking.setEndTime(now.plus(2, ChronoUnit.HOURS));
    }

    // =========================================================================
    // 1. GENERATE QR TESTS
    // =========================================================================
    @Nested
    @DisplayName("Generate QR Code Tests")
    class GenerateQrTests {

        @Test
        @DisplayName("Success: Co-owner generates valid 5-minute QR check-in token")
        void generateQr_Success() {
            when(bookingRepository.findById(BOOKING_ID)).thenReturn(Optional.of(booking));
            when(tokenService.generateQrToken(eq(BOOKING_ID), eq(VEHICLE_ID), eq(USER_ID), any(Duration.class)))
                    .thenReturn(VALID_TOKEN);

            QrCodeResponse response = qrValidationService.generateCheckInQr(BOOKING_ID, USER_ID);

            assertThat(response).isNotNull();
            assertThat(response.getQrToken()).isEqualTo(VALID_TOKEN);
            assertThat(response.getBookingId()).isEqualTo(BOOKING_ID);
            assertThat(response.getVehicleId()).isEqualTo(VEHICLE_ID);
            assertThat(response.getVehicleLicensePlate()).isEqualTo("51H-999.88");
            assertThat(response.getVehicleModel()).isEqualTo("VinFast VF8");
            assertThat(response.getExpiresInSeconds()).isEqualTo(300L);
            assertThat(response.getExpiresAt()).isAfter(response.getIssuedAt());

            verify(tokenService).generateQrToken(BOOKING_ID, VEHICLE_ID, USER_ID, Duration.ofMinutes(5));
        }

        @Test
        @DisplayName("Failure: Unauthenticated user cannot generate QR")
        void generateQr_UnauthenticatedUser_ThrowsUnauthorized() {
            assertThatThrownBy(() -> qrValidationService.generateCheckInQr(BOOKING_ID, null))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex -> assertThat(((BusinessException) ex).getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED));
        }

        @Test
        @DisplayName("Failure: Booking not found throws ResourceNotFoundException")
        void generateQr_BookingNotFound_ThrowsException() {
            when(bookingRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> qrValidationService.generateCheckInQr(999L, USER_ID))
                    .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        @DisplayName("Failure: Booking not in CONFIRMED or APPROVED status throws InvalidQrException")
        void generateQr_InvalidBookingStatus_ThrowsException() {
            booking.setStatus(BookingStatus.PENDING);
            when(bookingRepository.findById(BOOKING_ID)).thenReturn(Optional.of(booking));

            assertThatThrownBy(() -> qrValidationService.generateCheckInQr(BOOKING_ID, USER_ID))
                    .isInstanceOf(InvalidQrException.class)
                    .hasMessageContaining("QR check-in code can only be generated for CONFIRMED bookings");
        }

        @Test
        @DisplayName("Failure: Unauthorized user cannot generate QR for another user's booking")
        void generateQr_UnauthorizedUser_ThrowsForbidden() {
            when(bookingRepository.findById(BOOKING_ID)).thenReturn(Optional.of(booking));
            when(ownershipGroupRepository.findByVehicleId(VEHICLE_ID)).thenReturn(Optional.empty());
            when(userRepository.findById(OTHER_USER_ID)).thenReturn(Optional.of(otherUser));

            assertThatThrownBy(() -> qrValidationService.generateCheckInQr(BOOKING_ID, OTHER_USER_ID))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex -> assertThat(((BusinessException) ex).getStatus()).isEqualTo(HttpStatus.FORBIDDEN));
        }

        @Test
        @DisplayName("Failure: Attempting to generate QR too early (>15 min before start) throws InvalidQrException")
        void generateQr_TooEarly_ThrowsInvalidQrException() {
            booking.setStartTime(Instant.now().plus(2, ChronoUnit.HOURS));
            when(bookingRepository.findById(BOOKING_ID)).thenReturn(Optional.of(booking));

            assertThatThrownBy(() -> qrValidationService.generateCheckInQr(BOOKING_ID, USER_ID))
                    .isInstanceOf(InvalidQrException.class)
                    .hasMessageContaining("cannot be generated yet");
        }

        @Test
        @DisplayName("Failure: Attempting to generate QR when window expired (>30 min past start) throws InvalidQrException")
        void generateQr_ExpiredWindow_ThrowsInvalidQrException() {
            booking.setStartTime(Instant.now().minus(45, ChronoUnit.MINUTES));
            when(bookingRepository.findById(BOOKING_ID)).thenReturn(Optional.of(booking));

            assertThatThrownBy(() -> qrValidationService.generateCheckInQr(BOOKING_ID, USER_ID))
                    .isInstanceOf(InvalidQrException.class)
                    .hasMessageContaining("Check-in window has expired");
        }
    }

    // =========================================================================
    // 2. VALIDATE QR TESTS (The 6 Mandatory Requirements + Edge Cases)
    // =========================================================================
    @Nested
    @DisplayName("Validate QR Code Tests")
    class ValidateQrTests {

        private Claims validClaims;

        @BeforeEach
        void setUpClaims() {
            validClaims = io.jsonwebtoken.Jwts.claims()
                    .add("tokenType", "QR_CHECK_IN")
                    .add("bookingId", BOOKING_ID)
                    .add("vehicleId", VEHICLE_ID)
                    .add("userId", USER_ID)
                    .build();
        }

        @Test
        @DisplayName("1. Valid QR: All 7 dimensions match -> Returns valid response")
        void validateQr_ValidQr_Success() {
            when(tokenService.extractAllClaims(VALID_TOKEN)).thenReturn(validClaims);
            when(bookingRepository.findById(BOOKING_ID)).thenReturn(Optional.of(booking));

            QrValidationRequest request = new QrValidationRequest(VALID_TOKEN, VEHICLE_ID, BOOKING_ID);
            QrValidationResponse response = qrValidationService.validateQr(request, USER_ID);

            assertThat(response).isNotNull();
            assertThat(response.isValid()).isTrue();
            assertThat(response.getBookingId()).isEqualTo(BOOKING_ID);
            assertThat(response.getVehicleId()).isEqualTo(VEHICLE_ID);
            assertThat(response.getVehicleLicensePlate()).isEqualTo("51H-999.88");
            assertThat(response.getVehicleModel()).isEqualTo("VinFast VF8");
            assertThat(response.getUserId()).isEqualTo(USER_ID);
            assertThat(response.getUserName()).isEqualTo("Nguyen Van User");
            assertThat(response.getMessage()).contains("QR validation successful");
        }

        @Test
        @DisplayName("2. Invalid QR: Malformed token or invalid signature throws InvalidQrException")
        void validateQr_InvalidQr_CryptographicFailure_ThrowsInvalidQrException() {
            when(tokenService.extractAllClaims("malformed.tampered.token"))
                    .thenThrow(new JwtException("Invalid HMAC signature"));

            QrValidationRequest request = new QrValidationRequest("malformed.tampered.token", VEHICLE_ID, BOOKING_ID);

            assertThatThrownBy(() -> qrValidationService.validateQr(request, USER_ID))
                    .isInstanceOf(InvalidQrException.class)
                    .hasMessageContaining("Invalid QR token: Cryptographic signature verification failed or token is malformed.");
        }

        @Test
        @DisplayName("2b. Invalid QR: Blank or null token string throws InvalidQrException")
        void validateQr_InvalidQr_BlankToken_ThrowsInvalidQrException() {
            QrValidationRequest request = new QrValidationRequest("   ");

            assertThatThrownBy(() -> qrValidationService.validateQr(request, USER_ID))
                    .isInstanceOf(InvalidQrException.class)
                    .hasMessageContaining("QR token is required and cannot be empty.");
        }

        @Test
        @DisplayName("2c. Invalid QR: Wrong token type (e.g. REFRESH) throws InvalidQrException")
        void validateQr_InvalidQr_WrongTokenType_ThrowsInvalidQrException() {
            Claims refreshClaims = io.jsonwebtoken.Jwts.claims()
                    .add("tokenType", "REFRESH")
                    .add("bookingId", BOOKING_ID)
                    .add("vehicleId", VEHICLE_ID)
                    .add("userId", USER_ID)
                    .build();
            when(tokenService.extractAllClaims(VALID_TOKEN)).thenReturn(refreshClaims);

            QrValidationRequest request = new QrValidationRequest(VALID_TOKEN);

            assertThatThrownBy(() -> qrValidationService.validateQr(request, USER_ID))
                    .isInstanceOf(InvalidQrException.class)
                    .hasMessageContaining("Invalid token type in QR code: Expected 'QR_CHECK_IN', got 'REFRESH'.");
        }

        @Test
        @DisplayName("3. Expired QR: Token past 5-minute TTL throws InvalidQrException")
        void validateQr_ExpiredQr_ThrowsInvalidQrException() {
            when(tokenService.extractAllClaims("expired.qr.token"))
                    .thenThrow(new ExpiredJwtException(null, validClaims, "JWT expired"));

            QrValidationRequest request = new QrValidationRequest("expired.qr.token", VEHICLE_ID, BOOKING_ID);

            assertThatThrownBy(() -> qrValidationService.validateQr(request, USER_ID))
                    .isInstanceOf(InvalidQrException.class)
                    .hasMessageContaining("QR token has expired. Please refresh your QR code.");
        }

        @Test
        @DisplayName("4. Wrong Vehicle: Scanned physical vehicle does not match QR or booking vehicle")
        void validateQr_WrongVehicle_ThrowsInvalidQrException() {
            when(tokenService.extractAllClaims(VALID_TOKEN)).thenReturn(validClaims);
            when(bookingRepository.findById(BOOKING_ID)).thenReturn(Optional.of(booking));

            // Scanned physical station is vehicle #200, but booking is for vehicle #100
            QrValidationRequest request = new QrValidationRequest(VALID_TOKEN, 200L, BOOKING_ID);

            assertThatThrownBy(() -> qrValidationService.validateQr(request, USER_ID))
                    .isInstanceOf(InvalidQrException.class)
                    .hasMessageContaining("Vehicle mismatch: Scanned vehicle #200 does not match booking reservation vehicle #100");
        }

        @Test
        @DisplayName("4b. Wrong Vehicle: QR token claim vehicleId does not match booking vehicle in DB")
        void validateQr_WrongVehicleInToken_ThrowsInvalidQrException() {
            Claims wrongVehicleClaims = io.jsonwebtoken.Jwts.claims()
                    .add("tokenType", "QR_CHECK_IN")
                    .add("bookingId", BOOKING_ID)
                    .add("vehicleId", 999L)
                    .add("userId", USER_ID)
                    .build();
            when(tokenService.extractAllClaims(VALID_TOKEN)).thenReturn(wrongVehicleClaims);
            when(bookingRepository.findById(BOOKING_ID)).thenReturn(Optional.of(booking));

            QrValidationRequest request = new QrValidationRequest(VALID_TOKEN);

            assertThatThrownBy(() -> qrValidationService.validateQr(request, USER_ID))
                    .isInstanceOf(InvalidQrException.class)
                    .hasMessageContaining("Vehicle mismatch: QR token specifies vehicle #999, but booking #500 is reserved for vehicle #100");
        }

        @Test
        @DisplayName("5. Wrong Booking: Target booking does not match QR token reservation claim")
        void validateQr_WrongBooking_ThrowsInvalidQrException() {
            when(tokenService.extractAllClaims(VALID_TOKEN)).thenReturn(validClaims);

            // Request targets booking 999, but QR token is for booking 500
            QrValidationRequest request = new QrValidationRequest(VALID_TOKEN, VEHICLE_ID, 999L);

            assertThatThrownBy(() -> qrValidationService.validateQr(request, USER_ID))
                    .isInstanceOf(InvalidQrException.class)
                    .hasMessageContaining("Booking mismatch: Request specifies booking #999, but QR token is for booking #500.");
        }

        @Test
        @DisplayName("5b. Wrong Booking: Booking referenced in QR token does not exist in authoritative DB")
        void validateQr_BookingNotFoundInDb_ThrowsInvalidQrException() {
            when(tokenService.extractAllClaims(VALID_TOKEN)).thenReturn(validClaims);
            when(bookingRepository.findById(BOOKING_ID)).thenReturn(Optional.empty());

            QrValidationRequest request = new QrValidationRequest(VALID_TOKEN);

            assertThatThrownBy(() -> qrValidationService.validateQr(request, USER_ID))
                    .isInstanceOf(InvalidQrException.class)
                    .hasMessageContaining("Booking #500 referenced in QR token does not exist.");
        }

        @Test
        @DisplayName("6. Unauthorized User: Third party not owning booking, syndicate share, or staff/admin role")
        void validateQr_UnauthorizedUser_ThrowsForbidden() {
            when(tokenService.extractAllClaims(VALID_TOKEN)).thenReturn(validClaims);
            when(bookingRepository.findById(BOOKING_ID)).thenReturn(Optional.of(booking));
            when(ownershipGroupRepository.findByVehicleId(VEHICLE_ID)).thenReturn(Optional.empty());
            when(userRepository.findById(OTHER_USER_ID)).thenReturn(Optional.of(otherUser));

            QrValidationRequest request = new QrValidationRequest(VALID_TOKEN, VEHICLE_ID, BOOKING_ID);

            assertThatThrownBy(() -> qrValidationService.validateQr(request, OTHER_USER_ID))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex -> assertThat(((BusinessException) ex).getStatus()).isEqualTo(HttpStatus.FORBIDDEN))
                    .hasMessageContaining("Access denied: You are not authorized to validate QR access for this booking or vehicle.");
        }

        @Test
        @DisplayName("7. Unauthenticated User: Missing caller principal throws 401 Unauthorized")
        void validateQr_UnauthenticatedUser_ThrowsUnauthorized() {
            QrValidationRequest request = new QrValidationRequest(VALID_TOKEN);

            assertThatThrownBy(() -> qrValidationService.validateQr(request, null))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex -> assertThat(((BusinessException) ex).getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED));
        }

        @Test
        @DisplayName("8. Time Window: Scanned too early (<15 min before start) throws InvalidQrException")
        void validateQr_TimeWindowTooEarly_ThrowsInvalidQrException() {
            booking.setStartTime(Instant.now().plus(45, ChronoUnit.MINUTES));
            when(tokenService.extractAllClaims(VALID_TOKEN)).thenReturn(validClaims);
            when(bookingRepository.findById(BOOKING_ID)).thenReturn(Optional.of(booking));

            QrValidationRequest request = new QrValidationRequest(VALID_TOKEN, VEHICLE_ID, BOOKING_ID);

            assertThatThrownBy(() -> qrValidationService.validateQr(request, USER_ID))
                    .isInstanceOf(InvalidQrException.class)
                    .hasMessageContaining("Check-in window is not yet open for booking #500");
        }

        @Test
        @DisplayName("9. Time Window: Scanned >30 min late transitions booking to NO_SHOW per BR-OPS-01")
        void validateQr_TimeWindowExpired_MarksNoShow_ThrowsInvalidQrException() {
            booking.setStartTime(Instant.now().minus(40, ChronoUnit.MINUTES));
            when(tokenService.extractAllClaims(VALID_TOKEN)).thenReturn(validClaims);
            when(bookingRepository.findById(BOOKING_ID)).thenReturn(Optional.of(booking));

            QrValidationRequest request = new QrValidationRequest(VALID_TOKEN, VEHICLE_ID, BOOKING_ID);

            assertThatThrownBy(() -> qrValidationService.validateQr(request, USER_ID))
                    .isInstanceOf(InvalidQrException.class)
                    .hasMessageContaining("Check-in window has expired (>30 minutes past scheduled start time")
                    .hasMessageContaining("Booking #500 has been marked as NO_SHOW.");

            assertThat(booking.getStatus()).isEqualTo(BookingStatus.NO_SHOW);
            verify(bookingRepository).save(booking);
        }

        @Test
        @DisplayName("10. Staff Authorization: Staff operator can validate QR check-in on behalf of co-owner")
        void validateQr_StaffAuthorization_Success() {
            when(tokenService.extractAllClaims(VALID_TOKEN)).thenReturn(validClaims);
            when(bookingRepository.findById(BOOKING_ID)).thenReturn(Optional.of(booking));
            when(userRepository.findById(STAFF_USER_ID)).thenReturn(Optional.of(staffUser));

            QrValidationRequest request = new QrValidationRequest(VALID_TOKEN, VEHICLE_ID, BOOKING_ID);
            QrValidationResponse response = qrValidationService.validateQr(request, STAFF_USER_ID);

            assertThat(response).isNotNull();
            assertThat(response.isValid()).isTrue();
            assertThat(response.getBookingId()).isEqualTo(BOOKING_ID);
            assertThat(response.getMessage()).contains("QR validation successful");
        }

        @Test
        @DisplayName("11. Vehicle Not Ready: Vehicle in MAINTENANCE status throws InvalidQrException")
        void validateQr_VehicleInMaintenance_ThrowsInvalidQrException() {
            vehicle.setStatus(VehicleStatus.MAINTENANCE);
            when(tokenService.extractAllClaims(VALID_TOKEN)).thenReturn(validClaims);
            when(bookingRepository.findById(BOOKING_ID)).thenReturn(Optional.of(booking));

            QrValidationRequest request = new QrValidationRequest(VALID_TOKEN, VEHICLE_ID, BOOKING_ID);

            assertThatThrownBy(() -> qrValidationService.validateQr(request, USER_ID))
                    .isInstanceOf(InvalidQrException.class)
                    .hasMessageContaining("Vehicle #100 (VinFast VF8) is not ready for check-in: currently in status 'MAINTENANCE'.");
        }
    }
}
