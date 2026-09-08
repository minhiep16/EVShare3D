package com.example.evshare.service;

import com.example.evshare.dto.request.CheckInRequest;
import com.example.evshare.dto.request.CheckOutRequest;
import com.example.evshare.dto.response.UsageSessionResponse;
import com.example.evshare.dto.response.VehicleInspectionResponse;
import com.example.evshare.entity.*;
import com.example.evshare.entity.enums.*;
import com.example.evshare.exception.BusinessException;
import com.example.evshare.exception.HistoricalUsageImmutableException;
import com.example.evshare.exception.InvalidStateTransitionException;
import com.example.evshare.exception.InvalidUsageSessionException;
import com.example.evshare.exception.ResourceNotFoundException;
import com.example.evshare.repository.*;
import com.example.evshare.security.OwnershipSecurity;
import com.example.evshare.service.impl.UsageSessionServiceImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UsageSessionServiceTest {

    @Mock private UsageSessionRepository usageSessionRepository;
    @Mock private BookingRepository bookingRepository;
    @Mock private VehicleRepository vehicleRepository;
    @Mock private VehicleInspectionRepository vehicleInspectionRepository;
    @Mock private AuditLogRepository auditLogRepository;
    @Mock private UserRepository userRepository;
    @Mock private OwnershipShareRepository ownershipShareRepository;
    @Mock private CoOwnershipContractRepository coOwnershipContractRepository;
    @Mock private OwnershipGroupRepository ownershipGroupRepository;

    private OwnershipSecurity ownershipSecurity;
    private BookingStateMachine bookingStateMachine;
    private VehicleStateMachine vehicleStateMachine;
    private ObjectMapper objectMapper;
    private UsageSessionServiceImpl usageSessionService;

    private User testUser;
    private Vehicle testVehicle;
    private Booking testBooking;
    private UsageSession testSession;

    @BeforeEach
    void setUp() {
        bookingStateMachine = new BookingStateMachine();
        vehicleStateMachine = new VehicleStateMachine();
        objectMapper = new ObjectMapper();
        ownershipSecurity = new OwnershipSecurity(ownershipShareRepository, coOwnershipContractRepository, ownershipGroupRepository);

        usageSessionService = new UsageSessionServiceImpl(
                usageSessionRepository,
                bookingRepository,
                vehicleRepository,
                vehicleInspectionRepository,
                auditLogRepository,
                userRepository,
                bookingStateMachine,
                vehicleStateMachine,
                ownershipSecurity,
                objectMapper
        );

        testUser = new User();
        testUser.setId(10L);
        testUser.setFullName("Nguyen Van A");
        testUser.setEmail("nguyen.vana@evshare.vn");
        testUser.setRoles(new HashSet<>());

        testVehicle = new Vehicle();
        testVehicle.setId(100L);
        testVehicle.setModelName("VinFast VF8 Plus");
        testVehicle.setLicensePlate("30A-999.88");
        testVehicle.setStatus(VehicleStatus.AVAILABLE);
        testVehicle.setOdometerKm(new BigDecimal("12000.00"));
        testVehicle.setBatteryLevel(90);

        testBooking = new Booking();
        testBooking.setId(500L);
        testBooking.setUser(testUser);
        testBooking.setVehicle(testVehicle);
        testBooking.setStartTime(Instant.now().minus(10, ChronoUnit.MINUTES));
        testBooking.setEndTime(Instant.now().plus(3, ChronoUnit.HOURS));
        testBooking.setStatus(BookingStatus.CONFIRMED);
        testBooking.setEstimatedCost(new BigDecimal("350000.00"));

        testSession = new UsageSession();
        testSession.setId(1000L);
        testSession.setBooking(testBooking);
        testSession.setStartOdometer(new BigDecimal("12000.00"));
        testSession.setStartBattery(90);
        testSession.setCheckInTime(Instant.now().minus(2, ChronoUnit.HOURS));
        testSession.setStatus(UsageSessionStatus.ACTIVE);
    }

    // =========================================================================
    // 1. CHECK-IN TESTS (05-J CHECK-IN VALIDATION)
    // =========================================================================

    @Test
    @DisplayName("Check-In: Successful check-in transitions booking and vehicle to IN_USE")
    void testCheckIn_Success_Basic() {
        CheckInRequest request = new CheckInRequest(500L, new BigDecimal("12000.00"), 90);

        when(bookingRepository.findById(500L)).thenReturn(Optional.of(testBooking));
        when(usageSessionRepository.findByBookingId(500L)).thenReturn(Optional.empty());
        when(usageSessionRepository.findByBookingVehicleIdAndStatus(100L, UsageSessionStatus.ACTIVE)).thenReturn(Optional.empty());
        when(usageSessionRepository.save(any(UsageSession.class))).thenAnswer(invocation -> {
            UsageSession s = invocation.getArgument(0);
            s.setId(1000L);
            return s;
        });

        UsageSessionResponse response = usageSessionService.checkIn(request, 10L);

        assertNotNull(response);
        assertEquals(1000L, response.getId());
        assertEquals(500L, response.getBookingId());
        assertEquals(10L, response.getUserId());
        assertEquals(100L, response.getVehicleId());
        assertEquals(new BigDecimal("12000.00"), response.getStartOdometer());
        assertEquals(90, response.getStartBattery());
        assertEquals(UsageSessionStatus.ACTIVE, response.getStatus());

        // Verifications
        assertEquals(BookingStatus.IN_USE, testBooking.getStatus());
        assertEquals(VehicleStatus.IN_USE, testVehicle.getStatus());
        assertEquals(new BigDecimal("12000.00"), testVehicle.getOdometerKm());
        assertEquals(90, testVehicle.getBatteryLevel());

        verify(bookingRepository).save(testBooking);
        verify(vehicleRepository).save(testVehicle);
        verify(usageSessionRepository).save(any(UsageSession.class));
        verify(auditLogRepository).save(any(AuditLog.class));
    }

    @Test
    @DisplayName("Check-In: Successful check-in with matching vehicleId")
    void testCheckIn_Success_WithMatchingVehicleId() {
        CheckInRequest request = new CheckInRequest(500L, 100L, new BigDecimal("12000.00"), 90);

        when(bookingRepository.findById(500L)).thenReturn(Optional.of(testBooking));
        when(usageSessionRepository.findByBookingId(500L)).thenReturn(Optional.empty());
        when(usageSessionRepository.findByBookingVehicleIdAndStatus(100L, UsageSessionStatus.ACTIVE)).thenReturn(Optional.empty());
        when(usageSessionRepository.save(any(UsageSession.class))).thenAnswer(inv -> {
            UsageSession s = inv.getArgument(0);
            s.setId(1000L);
            return s;
        });

        UsageSessionResponse response = usageSessionService.checkIn(request, 10L);

        assertNotNull(response);
        assertEquals(100L, response.getVehicleId());
    }

    @Test
    @DisplayName("Check-In: Captures 3D defect flags, inspection report, and photo evidence")
    void testCheckIn_Success_WithInspectionAndEvidence() {
        List<String> photos = List.of("https://s3.evshare.vn/photos/checkin-front.jpg", "https://s3.evshare.vn/photos/checkin-scratch.jpg");
        CheckInRequest request = new CheckInRequest(
                500L,
                100L,
                new BigDecimal("12000.00"),
                90,
                "{\"defects\": [\"BUMPER_FRONT_RIGHT\"]}",
                "Minor scratch on right bumper noted",
                photos
        );

        when(bookingRepository.findById(500L)).thenReturn(Optional.of(testBooking));
        when(usageSessionRepository.findByBookingId(500L)).thenReturn(Optional.empty());
        when(usageSessionRepository.findByBookingVehicleIdAndStatus(100L, UsageSessionStatus.ACTIVE)).thenReturn(Optional.empty());
        when(userRepository.findById(10L)).thenReturn(Optional.of(testUser));
        when(usageSessionRepository.save(any(UsageSession.class))).thenAnswer(inv -> {
            UsageSession s = inv.getArgument(0);
            s.setId(1000L);
            return s;
        });
        when(vehicleInspectionRepository.save(any(VehicleInspection.class))).thenAnswer(inv -> {
            VehicleInspection insp = inv.getArgument(0);
            insp.setId(77L);
            return insp;
        });

        UsageSessionResponse response = usageSessionService.checkIn(request, 10L);

        assertNotNull(response);
        assertEquals(1, response.getInspections().size());
        VehicleInspectionResponse inspResp = response.getInspections().get(0);
        assertEquals(InspectionType.CHECK_IN, inspResp.getInspectionType());
        assertEquals("{\"defects\": [\"BUMPER_FRONT_RIGHT\"]}", inspResp.getConditionMeshFlags());
        assertTrue(inspResp.getNotes().contains("Minor scratch"));
        assertTrue(inspResp.getNotes().contains("Photographic Evidence [2 item(s)]"));

        verify(vehicleInspectionRepository).save(any(VehicleInspection.class));
    }

    @Test
    @DisplayName("Check-In: Rejects unauthenticated check-in attempt")
    void testCheckIn_Rejects_UnauthenticatedUser() {
        CheckInRequest request = new CheckInRequest(500L, new BigDecimal("12000.00"), 90);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> usageSessionService.checkIn(request, null));
        assertTrue(ex.getMessage().contains("Authentication required"));
    }

    @Test
    @DisplayName("Check-In: Rejects check-in if target vehicleId does not match booking reservation")
    void testCheckIn_Rejects_VehicleMismatch() {
        CheckInRequest request = new CheckInRequest(500L, 999L, new BigDecimal("12000.00"), 90); // 999L != 100L

        when(bookingRepository.findById(500L)).thenReturn(Optional.of(testBooking));

        InvalidUsageSessionException ex = assertThrows(InvalidUsageSessionException.class,
                () -> usageSessionService.checkIn(request, 10L));
        assertTrue(ex.getMessage().contains("Vehicle mismatch"));
    }

    @Test
    @DisplayName("Check-In: Rejects check-in when current time is too early (>15 min before start)")
    void testCheckIn_Rejects_TimeWindowTooEarly() {
        testBooking.setStartTime(Instant.now().plus(45, ChronoUnit.MINUTES)); // 45m in future (> 15m)
        CheckInRequest request = new CheckInRequest(500L, new BigDecimal("12000.00"), 90);

        when(bookingRepository.findById(500L)).thenReturn(Optional.of(testBooking));

        InvalidUsageSessionException ex = assertThrows(InvalidUsageSessionException.class,
                () -> usageSessionService.checkIn(request, 10L));
        assertTrue(ex.getMessage().contains("Check-in is not yet open"));
    }

    @Test
    @DisplayName("Check-In: Rejects check-in when window expired (>30 min past start) and marks booking as NO_SHOW")
    void testCheckIn_Rejects_TimeWindowExpired_MarksNoShow() {
        testBooking.setStartTime(Instant.now().minus(45, ChronoUnit.MINUTES)); // 45m in past (> 30m)
        CheckInRequest request = new CheckInRequest(500L, new BigDecimal("12000.00"), 90);

        when(bookingRepository.findById(500L)).thenReturn(Optional.of(testBooking));

        InvalidUsageSessionException ex = assertThrows(InvalidUsageSessionException.class,
                () -> usageSessionService.checkIn(request, 10L));
        assertTrue(ex.getMessage().contains("Check-in window has expired"));
        assertTrue(ex.getMessage().contains("marked as NO_SHOW"));
        assertEquals(BookingStatus.NO_SHOW, testBooking.getStatus());
        verify(bookingRepository).save(testBooking);
    }

    @Test
    @DisplayName("Check-In: Rejects check-in when vehicle is already IN_USE")
    void testCheckIn_Rejects_VehicleAlreadyInUse() {
        testVehicle.setStatus(VehicleStatus.IN_USE);
        CheckInRequest request = new CheckInRequest(500L, new BigDecimal("12000.00"), 90);

        when(bookingRepository.findById(500L)).thenReturn(Optional.of(testBooking));

        InvalidUsageSessionException ex = assertThrows(InvalidUsageSessionException.class,
                () -> usageSessionService.checkIn(request, 10L));
        assertTrue(ex.getMessage().contains("currently in use"));
    }

    @Test
    @DisplayName("Check-In: Rejects check-in when vehicle is in MAINTENANCE or DAMAGED state")
    void testCheckIn_Rejects_VehicleInMaintenance() {
        testVehicle.setStatus(VehicleStatus.MAINTENANCE);
        CheckInRequest request = new CheckInRequest(500L, new BigDecimal("12000.00"), 90);

        when(bookingRepository.findById(500L)).thenReturn(Optional.of(testBooking));

        InvalidUsageSessionException ex = assertThrows(InvalidUsageSessionException.class,
                () -> usageSessionService.checkIn(request, 10L));
        assertTrue(ex.getMessage().contains("not ready for check-in"));
    }

    @Test
    @DisplayName("Check-In: Rejects check-in if vehicle already has another active session in progress")
    void testCheckIn_Rejects_DuplicateActiveSessionOnVehicle() {
        UsageSession otherActiveSession = new UsageSession();
        otherActiveSession.setId(888L);
        otherActiveSession.setStatus(UsageSessionStatus.ACTIVE);

        CheckInRequest request = new CheckInRequest(500L, new BigDecimal("12000.00"), 90);

        when(bookingRepository.findById(500L)).thenReturn(Optional.of(testBooking));
        when(usageSessionRepository.findByBookingId(500L)).thenReturn(Optional.empty());
        when(usageSessionRepository.findByBookingVehicleIdAndStatus(100L, UsageSessionStatus.ACTIVE))
                .thenReturn(Optional.of(otherActiveSession));

        InvalidUsageSessionException ex = assertThrows(InvalidUsageSessionException.class,
                () -> usageSessionService.checkIn(request, 10L));
        assertTrue(ex.getMessage().contains("already has an active usage session"));
    }

    @Test
    @DisplayName("Check-In: Rejects check-in if session already exists and is active for booking")
    void testCheckIn_Rejects_DuplicateSession() {
        CheckInRequest request = new CheckInRequest(500L, new BigDecimal("12000.00"), 90);

        UsageSession activeSession = new UsageSession();
        activeSession.setId(999L);
        activeSession.setStatus(UsageSessionStatus.ACTIVE);

        when(bookingRepository.findById(500L)).thenReturn(Optional.of(testBooking));
        when(usageSessionRepository.findByBookingId(500L)).thenReturn(Optional.of(activeSession));

        InvalidUsageSessionException ex = assertThrows(InvalidUsageSessionException.class,
                () -> usageSessionService.checkIn(request, 10L));
        assertTrue(ex.getMessage().contains("already active"));
    }

    @Test
    @DisplayName("Check-In: Rejects check-in if booking is not in CONFIRMED or APPROVED status")
    void testCheckIn_Rejects_UnconfirmedBooking() {
        testBooking.setStatus(BookingStatus.PENDING);
        CheckInRequest request = new CheckInRequest(500L, new BigDecimal("12000.00"), 90);

        when(bookingRepository.findById(500L)).thenReturn(Optional.of(testBooking));

        InvalidUsageSessionException ex = assertThrows(InvalidUsageSessionException.class,
                () -> usageSessionService.checkIn(request, 10L));
        assertTrue(ex.getMessage().contains("Only CONFIRMED bookings can be checked in"));
    }

    @Test
    @DisplayName("Check-In: Rejects check-in with negative start odometer")
    void testCheckIn_Rejects_NegativeOdometer() {
        CheckInRequest request = new CheckInRequest(500L, new BigDecimal("-10.00"), 90);

        when(bookingRepository.findById(500L)).thenReturn(Optional.of(testBooking));

        InvalidUsageSessionException ex = assertThrows(InvalidUsageSessionException.class,
                () -> usageSessionService.checkIn(request, 10L));
        assertTrue(ex.getMessage().contains("odometer reading cannot be negative"));
    }

    @Test
    @DisplayName("Check-In: Rejects check-in with invalid battery SoC (> 100 or < 0)")
    void testCheckIn_Rejects_InvalidBatterySoC() {
        CheckInRequest request = new CheckInRequest(500L, new BigDecimal("12000.00"), 105);

        when(bookingRepository.findById(500L)).thenReturn(Optional.of(testBooking));

        InvalidUsageSessionException ex = assertThrows(InvalidUsageSessionException.class,
                () -> usageSessionService.checkIn(request, 10L));
        assertTrue(ex.getMessage().contains("battery SoC must be between 0% and 100%"));
    }

    @Test
    @DisplayName("Check-In: Rejects check-in by unauthorized third party")
    void testCheckIn_Rejects_UnauthorizedUser() {
        CheckInRequest request = new CheckInRequest(500L, new BigDecimal("12000.00"), 90);

        when(bookingRepository.findById(500L)).thenReturn(Optional.of(testBooking));
        when(ownershipGroupRepository.findByVehicleId(100L)).thenReturn(Optional.empty());
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        BusinessException ex = assertThrows(BusinessException.class,
                () -> usageSessionService.checkIn(request, 999L));
        assertTrue(ex.getMessage().contains("Access denied"));
    }

    // =========================================================================
    // 2. CHECK-OUT TESTS (BALANCED USAGE & SURCHARGES)
    // =========================================================================

    @Test
    @DisplayName("Check-Out: Balanced usage (>20% SoC) completes session with zero surcharge")
    void testCheckOut_Success_BalancedUsage_NoSurcharge() {
        testBooking.setStatus(BookingStatus.IN_USE);
        testVehicle.setStatus(VehicleStatus.IN_USE);

        CheckOutRequest request = new CheckOutRequest(
                new BigDecimal("12085.50"), // 85.5 km traveled
                65,                         // 25% battery consumed (90% -> 65%)
                false,
                false,
                null,
                "Vehicle returned clean, standard usage",
                null,
                null
        );

        when(usageSessionRepository.findById(1000L)).thenReturn(Optional.of(testSession));
        when(userRepository.findById(10L)).thenReturn(Optional.of(testUser));
        when(usageSessionRepository.save(any(UsageSession.class))).thenAnswer(inv -> inv.getArgument(0));
        when(vehicleInspectionRepository.save(any(VehicleInspection.class))).thenAnswer(inv -> inv.getArgument(0));
        when(vehicleInspectionRepository.findByUsageSessionIdOrderByCreatedAtAsc(1000L)).thenReturn(Collections.emptyList());

        UsageSessionResponse response = usageSessionService.checkOut(1000L, request, 10L);

        assertNotNull(response);
        assertEquals(UsageSessionStatus.COMPLETED, response.getStatus());
        assertEquals(new BigDecimal("12085.50"), response.getEndOdometer());
        assertEquals(new BigDecimal("85.50"), response.getMileage());
        assertEquals(65, response.getEndBattery());
        assertEquals(25, response.getBatteryDelta());
        assertEquals(new BigDecimal("0.00"), response.getAdditionalCost());
        assertTrue(response.getCostBreakdown().isEmpty());

        // State Machine Verifications
        assertEquals(BookingStatus.COMPLETED, testBooking.getStatus());
        assertEquals(VehicleStatus.AVAILABLE, testVehicle.getStatus());
        assertEquals(new BigDecimal("12085.50"), testVehicle.getOdometerKm());
        assertEquals(65, testVehicle.getBatteryLevel());

        verify(bookingRepository).save(testBooking);
        verify(vehicleRepository).save(testVehicle);
        verify(usageSessionRepository).save(testSession);
        verify(auditLogRepository).save(any(AuditLog.class));
    }

    @Test
    @DisplayName("Check-Out: BR-OPS-02 Low battery (<20%) without plugging in assesses 150,000 VND surcharge")
    void testCheckOut_BR_OPS_02_LowBatteryWithoutPlugIn_Assesses150kSurcharge() {
        testBooking.setStatus(BookingStatus.IN_USE);
        testVehicle.setStatus(VehicleStatus.IN_USE);

        CheckOutRequest request = new CheckOutRequest(
                new BigDecimal("12150.00"),
                14,   // < 20% SoC
                false, // NOT plugged into charger
                false,
                null,
                "Battery depleted to 14%",
                null,
                null
        );

        when(usageSessionRepository.findById(1000L)).thenReturn(Optional.of(testSession));
        when(userRepository.findById(10L)).thenReturn(Optional.of(testUser));
        when(usageSessionRepository.save(any(UsageSession.class))).thenAnswer(inv -> inv.getArgument(0));
        when(vehicleInspectionRepository.save(any(VehicleInspection.class))).thenAnswer(inv -> inv.getArgument(0));
        when(vehicleInspectionRepository.findByUsageSessionIdOrderByCreatedAtAsc(1000L)).thenReturn(Collections.emptyList());

        UsageSessionResponse response = usageSessionService.checkOut(1000L, request, 10L);

        assertNotNull(response);
        assertEquals(UsageSessionStatus.COMPLETED, response.getStatus());
        assertEquals(14, response.getEndBattery());
        assertEquals(76, response.getBatteryDelta()); // 90 - 14

        // Surcharge verification
        assertEquals(new BigDecimal("150000.00"), response.getAdditionalCost());
        assertTrue(response.getCostBreakdown().containsKey("LOW_BATTERY_SURCHARGE"));
        assertEquals(new BigDecimal("150000.00"), response.getCostBreakdown().get("LOW_BATTERY_SURCHARGE"));

        // Vehicle returned to AVAILABLE
        assertEquals(VehicleStatus.AVAILABLE, testVehicle.getStatus());
    }

    @Test
    @DisplayName("Check-Out: BR-OPS-02 Low battery (<20%) WITH plugging in avoids surcharge and transitions vehicle to CHARGING")
    void testCheckOut_BR_OPS_02_LowBatteryWithPlugIn_NoSurcharge_VehicleCharging() {
        testBooking.setStatus(BookingStatus.IN_USE);
        testVehicle.setStatus(VehicleStatus.IN_USE);

        CheckOutRequest request = new CheckOutRequest(
                new BigDecimal("12150.00"),
                15,  // < 20% SoC
                true, // PLUGGED IN to stall charger
                false,
                null,
                "Vehicle plugged into station bay #3",
                null,
                null
        );

        when(usageSessionRepository.findById(1000L)).thenReturn(Optional.of(testSession));
        when(userRepository.findById(10L)).thenReturn(Optional.of(testUser));
        when(usageSessionRepository.save(any(UsageSession.class))).thenAnswer(inv -> inv.getArgument(0));
        when(vehicleInspectionRepository.save(any(VehicleInspection.class))).thenAnswer(inv -> inv.getArgument(0));
        when(vehicleInspectionRepository.findByUsageSessionIdOrderByCreatedAtAsc(1000L)).thenReturn(Collections.emptyList());

        UsageSessionResponse response = usageSessionService.checkOut(1000L, request, 10L);

        assertNotNull(response);
        assertEquals(UsageSessionStatus.COMPLETED, response.getStatus());
        assertEquals(new BigDecimal("0.00"), response.getAdditionalCost());
        assertFalse(response.getCostBreakdown().containsKey("LOW_BATTERY_SURCHARGE"));

        // Vehicle must transition to CHARGING
        assertEquals(VehicleStatus.CHARGING, testVehicle.getStatus());
    }

    @Test
    @DisplayName("Check-Out: Flagging vehicle damage transitions vehicle to DAMAGED status")
    void testCheckOut_WithDamage_TransitionsToDamaged() {
        testBooking.setStatus(BookingStatus.IN_USE);
        testVehicle.setStatus(VehicleStatus.IN_USE);

        CheckOutRequest request = new CheckOutRequest(
                new BigDecimal("12050.00"),
                75,
                false,
                true, // HAS DAMAGE
                "{\"defects\": [\"WINDSHIELD_CRACK\"]}",
                "Pebble cracked front windshield on highway",
                List.of("https://s3.evshare.vn/photos/crack.jpg"),
                new BigDecimal("200000.00") // cleaning/damage fee
        );

        when(usageSessionRepository.findById(1000L)).thenReturn(Optional.of(testSession));
        when(userRepository.findById(10L)).thenReturn(Optional.of(testUser));
        when(usageSessionRepository.save(any(UsageSession.class))).thenAnswer(inv -> inv.getArgument(0));
        when(vehicleInspectionRepository.save(any(VehicleInspection.class))).thenAnswer(inv -> inv.getArgument(0));
        when(vehicleInspectionRepository.findByUsageSessionIdOrderByCreatedAtAsc(1000L)).thenReturn(Collections.emptyList());

        UsageSessionResponse response = usageSessionService.checkOut(1000L, request, 10L);

        assertNotNull(response);
        assertEquals(VehicleStatus.DAMAGED, testVehicle.getStatus());
        assertEquals(new BigDecimal("200000.00"), response.getAdditionalCost());
        assertTrue(response.getCostBreakdown().containsKey("OTHER_ADDITIONAL_COST"));
    }

    @Test
    @DisplayName("Check-Out: Rejects check-out if ending odometer is less than starting odometer")
    void testCheckOut_Rejects_EndOdometerLessThanStart() {
        testBooking.setStatus(BookingStatus.IN_USE);
        testVehicle.setStatus(VehicleStatus.IN_USE);
        CheckOutRequest request = new CheckOutRequest(new BigDecimal("11950.00"), 80);

        when(usageSessionRepository.findById(1000L)).thenReturn(Optional.of(testSession));

        InvalidUsageSessionException ex = assertThrows(InvalidUsageSessionException.class,
                () -> usageSessionService.checkOut(1000L, request, 10L));
        assertTrue(ex.getMessage().contains("cannot be less than start odometer"));
    }

    @Test
    @DisplayName("Check-Out: Rejects check-out with negative or out-of-range battery SoC")
    void testCheckOut_Rejects_InvalidBatterySoC() {
        testBooking.setStatus(BookingStatus.IN_USE);
        testVehicle.setStatus(VehicleStatus.IN_USE);
        CheckOutRequest request = new CheckOutRequest(new BigDecimal("12100.00"), -5);

        when(usageSessionRepository.findById(1000L)).thenReturn(Optional.of(testSession));

        InvalidUsageSessionException ex = assertThrows(InvalidUsageSessionException.class,
                () -> usageSessionService.checkOut(1000L, request, 10L));
        assertTrue(ex.getMessage().contains("battery SoC must be between 0% and 100%"));
    }

    @Test
    @DisplayName("Check-Out: Successful check-out with matching vehicleId and userId")
    void testCheckOut_Success_WithMatchingVehicleIdAndUserId() {
        testBooking.setStatus(BookingStatus.IN_USE);
        testVehicle.setStatus(VehicleStatus.IN_USE);

        CheckOutRequest request = new CheckOutRequest(
                new BigDecimal("12090.00"),
                75,
                100L,
                10L
        );

        when(usageSessionRepository.findById(1000L)).thenReturn(Optional.of(testSession));
        when(userRepository.findById(10L)).thenReturn(Optional.of(testUser));
        when(usageSessionRepository.save(any(UsageSession.class))).thenAnswer(inv -> inv.getArgument(0));
        when(vehicleInspectionRepository.save(any(VehicleInspection.class))).thenAnswer(inv -> inv.getArgument(0));
        when(vehicleInspectionRepository.findByUsageSessionIdOrderByCreatedAtAsc(1000L)).thenReturn(Collections.emptyList());

        UsageSessionResponse response = usageSessionService.checkOut(1000L, request, 10L);

        assertNotNull(response);
        assertEquals(UsageSessionStatus.COMPLETED, response.getStatus());
        assertEquals(100L, response.getVehicleId());
        assertEquals(10L, response.getUserId());
    }

    @Test
    @DisplayName("Check-Out: Rejects check-out when caller is unauthenticated")
    void testCheckOut_Rejects_UnauthenticatedUser() {
        testBooking.setStatus(BookingStatus.IN_USE);
        testVehicle.setStatus(VehicleStatus.IN_USE);
        CheckOutRequest request = new CheckOutRequest(new BigDecimal("12050.00"), 80);

        when(usageSessionRepository.findById(1000L)).thenReturn(Optional.of(testSession));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> usageSessionService.checkOut(1000L, request, null));
        assertTrue(ex.getMessage().contains("Authentication required"));
    }

    @Test
    @DisplayName("Check-Out: Rejects check-out when target vehicleId does not match session vehicle")
    void testCheckOut_Rejects_VehicleMismatch() {
        testBooking.setStatus(BookingStatus.IN_USE);
        testVehicle.setStatus(VehicleStatus.IN_USE);
        CheckOutRequest request = new CheckOutRequest(new BigDecimal("12050.00"), 80, 999L, 10L);

        when(usageSessionRepository.findById(1000L)).thenReturn(Optional.of(testSession));

        InvalidUsageSessionException ex = assertThrows(InvalidUsageSessionException.class,
                () -> usageSessionService.checkOut(1000L, request, 10L));
        assertTrue(ex.getMessage().contains("Vehicle mismatch"));
    }

    @Test
    @DisplayName("Check-Out: Rejects check-out when target userId does not match booking reservation")
    void testCheckOut_Rejects_UserMismatch() {
        testBooking.setStatus(BookingStatus.IN_USE);
        testVehicle.setStatus(VehicleStatus.IN_USE);
        CheckOutRequest request = new CheckOutRequest(new BigDecimal("12050.00"), 80, 100L, 999L);

        when(usageSessionRepository.findById(1000L)).thenReturn(Optional.of(testSession));

        InvalidUsageSessionException ex = assertThrows(InvalidUsageSessionException.class,
                () -> usageSessionService.checkOut(1000L, request, 10L));
        assertTrue(ex.getMessage().contains("User mismatch"));
    }

    @Test
    @DisplayName("Check-Out: Rejects check-out when associated booking is not in IN_USE status")
    void testCheckOut_Rejects_BookingNotInUse() {
        testBooking.setStatus(BookingStatus.CONFIRMED); // Not IN_USE
        testVehicle.setStatus(VehicleStatus.IN_USE);
        CheckOutRequest request = new CheckOutRequest(new BigDecimal("12050.00"), 80);

        when(usageSessionRepository.findById(1000L)).thenReturn(Optional.of(testSession));

        InvalidUsageSessionException ex = assertThrows(InvalidUsageSessionException.class,
                () -> usageSessionService.checkOut(1000L, request, 10L));
        assertTrue(ex.getMessage().contains("expected 'IN_USE'"));
    }

    @Test
    @DisplayName("Check-Out: Rejects check-out when vehicle is not in IN_USE status")
    void testCheckOut_Rejects_VehicleNotInUse() {
        testBooking.setStatus(BookingStatus.IN_USE);
        testVehicle.setStatus(VehicleStatus.MAINTENANCE); // Not IN_USE
        CheckOutRequest request = new CheckOutRequest(new BigDecimal("12050.00"), 80);

        when(usageSessionRepository.findById(1000L)).thenReturn(Optional.of(testSession));

        InvalidUsageSessionException ex = assertThrows(InvalidUsageSessionException.class,
                () -> usageSessionService.checkOut(1000L, request, 10L));
        assertTrue(ex.getMessage().contains("expected 'IN_USE'"));
    }

    @Test
    @DisplayName("Check-Out: Rejects check-out when session is not in ACTIVE status")
    void testCheckOut_Rejects_SessionNotActive() {
        testSession.setStatus(UsageSessionStatus.DISPUTED);
        CheckOutRequest request = new CheckOutRequest(new BigDecimal("12050.00"), 80);

        when(usageSessionRepository.findById(1000L)).thenReturn(Optional.of(testSession));

        InvalidUsageSessionException ex = assertThrows(InvalidUsageSessionException.class,
                () -> usageSessionService.checkOut(1000L, request, 10L));
        assertTrue(ex.getMessage().contains("is in status 'DISPUTED'"));
    }

    @Test
    @DisplayName("Check-Out: Rejects check-out with negative end odometer")
    void testCheckOut_Rejects_NegativeEndOdometer() {
        testBooking.setStatus(BookingStatus.IN_USE);
        testVehicle.setStatus(VehicleStatus.IN_USE);
        CheckOutRequest request = new CheckOutRequest(new BigDecimal("-50.00"), 80);

        when(usageSessionRepository.findById(1000L)).thenReturn(Optional.of(testSession));

        InvalidUsageSessionException ex = assertThrows(InvalidUsageSessionException.class,
                () -> usageSessionService.checkOut(1000L, request, 10L));
        assertTrue(ex.getMessage().contains("End odometer reading cannot be negative"));
    }

    @Test
    @DisplayName("Check-Out: Rejects check-out by unauthorized user")
    void testCheckOut_Rejects_UnauthorizedUser() {
        testBooking.setStatus(BookingStatus.IN_USE);
        testVehicle.setStatus(VehicleStatus.IN_USE);
        CheckOutRequest request = new CheckOutRequest(new BigDecimal("12050.00"), 80);

        when(usageSessionRepository.findById(1000L)).thenReturn(Optional.of(testSession));
        when(ownershipGroupRepository.findByVehicleId(100L)).thenReturn(Optional.empty());
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        BusinessException ex = assertThrows(BusinessException.class,
                () -> usageSessionService.checkOut(1000L, request, 999L));
        assertTrue(ex.getMessage().contains("Access denied"));
    }

    // =========================================================================
    // 3. HISTORICAL USAGE IMMUTABILITY TESTS
    // =========================================================================

    @Test
    @DisplayName("Historical Immutability: Re-checking out an already COMPLETED session throws HistoricalUsageImmutableException")
    void testHistoricalUsage_CannotBeRewritten_RejectsReCheckout() {
        testSession.setStatus(UsageSessionStatus.COMPLETED);
        testSession.setEndOdometer(new BigDecimal("12090.00"));
        testSession.setEndBattery(70);
        testSession.setCheckOutTime(Instant.now().minus(1, ChronoUnit.HOURS));

        CheckOutRequest request = new CheckOutRequest(new BigDecimal("12150.00"), 60);

        when(usageSessionRepository.findById(1000L)).thenReturn(Optional.of(testSession));

        HistoricalUsageImmutableException ex = assertThrows(HistoricalUsageImmutableException.class,
                () -> usageSessionService.checkOut(1000L, request, 10L));

        assertEquals(1000L, ex.getSessionId());
        assertTrue(ex.getMessage().contains("immutable"));
        assertTrue(ex.getMessage().contains("Historical usage telemetry must not be rewritten"));

        // Verify no repository save or state changes were triggered
        verify(usageSessionRepository, never()).save(any(UsageSession.class));
        verify(bookingRepository, never()).save(any(Booking.class));
        verify(vehicleRepository, never()).save(any(Vehicle.class));
    }

    @Test
    @DisplayName("Historical Immutability: Original telemetry values remain strictly intact")
    void testHistoricalUsage_OriginalTelemetryPreserved() {
        testSession.setStatus(UsageSessionStatus.COMPLETED);
        BigDecimal originalStartOdometer = new BigDecimal("12000.00");
        BigDecimal originalEndOdometer = new BigDecimal("12090.00");
        Integer originalStartBattery = 90;
        Integer originalEndBattery = 70;

        testSession.setStartOdometer(originalStartOdometer);
        testSession.setEndOdometer(originalEndOdometer);
        testSession.setStartBattery(originalStartBattery);
        testSession.setEndBattery(originalEndBattery);

        when(usageSessionRepository.findById(1000L)).thenReturn(Optional.of(testSession));

        CheckOutRequest tamperRequest = new CheckOutRequest(new BigDecimal("12500.00"), 50);

        assertThrows(HistoricalUsageImmutableException.class,
                () -> usageSessionService.checkOut(1000L, tamperRequest, 10L));

        assertEquals(originalStartOdometer, testSession.getStartOdometer());
        assertEquals(originalEndOdometer, testSession.getEndOdometer());
        assertEquals(originalStartBattery, testSession.getStartBattery());
        assertEquals(originalEndBattery, testSession.getEndBattery());
    }

    // =========================================================================
    // 4. QUERY & READ OPERATIONS
    // =========================================================================

    @Test
    @DisplayName("Query: getSessionById returns session with mileage and battery delta")
    void testGetSessionById_Success() {
        testSession.setStatus(UsageSessionStatus.COMPLETED);
        testSession.setEndOdometer(new BigDecimal("12100.00"));
        testSession.setEndBattery(60);
        testSession.setCheckOutTime(Instant.now());

        when(usageSessionRepository.findById(1000L)).thenReturn(Optional.of(testSession));
        when(vehicleInspectionRepository.findByUsageSessionIdOrderByCreatedAtAsc(1000L)).thenReturn(Collections.emptyList());

        UsageSessionResponse response = usageSessionService.getSessionById(1000L, 10L);

        assertNotNull(response);
        assertEquals(1000L, response.getId());
        assertEquals(new BigDecimal("100.00"), response.getMileage());
        assertEquals(30, response.getBatteryDelta());
    }

    @Test
    @DisplayName("Query: getSessionByBookingId returns corresponding session")
    void testGetSessionByBookingId_Success() {
        when(bookingRepository.findById(500L)).thenReturn(Optional.of(testBooking));
        when(usageSessionRepository.findByBookingId(500L)).thenReturn(Optional.of(testSession));
        when(vehicleInspectionRepository.findByUsageSessionIdOrderByCreatedAtAsc(1000L)).thenReturn(Collections.emptyList());

        UsageSessionResponse response = usageSessionService.getSessionByBookingId(500L, 10L);

        assertNotNull(response);
        assertEquals(1000L, response.getId());
    }

    @Test
    @DisplayName("Query: getMySessions returns list of user sessions")
    void testGetMySessions_Success() {
        when(usageSessionRepository.findByBookingUserIdOrderByCheckInTimeDesc(10L))
                .thenReturn(List.of(testSession));
        when(vehicleInspectionRepository.findByUsageSessionIdOrderByCreatedAtAsc(1000L)).thenReturn(Collections.emptyList());

        List<UsageSessionResponse> sessions = usageSessionService.getMySessions(10L);

        assertNotNull(sessions);
        assertEquals(1, sessions.size());
        assertEquals(1000L, sessions.get(0).getId());
    }

    @Test
    @DisplayName("Query: getInspections returns inspection list for session")
    void testGetInspections_Success() {
        VehicleInspection insp = new VehicleInspection();
        insp.setId(1L);
        insp.setUsageSession(testSession);
        insp.setInspectorUser(testUser);
        insp.setInspectionType(InspectionType.CHECK_IN);
        insp.setNotes("All good");

        when(usageSessionRepository.findById(1000L)).thenReturn(Optional.of(testSession));
        when(vehicleInspectionRepository.findByUsageSessionIdOrderByCreatedAtAsc(1000L))
                .thenReturn(List.of(insp));

        List<VehicleInspectionResponse> inspections = usageSessionService.getInspections(1000L, 10L);

        assertNotNull(inspections);
        assertEquals(1, inspections.size());
        assertEquals("All good", inspections.get(0).getNotes());
    }

    @Test
    @DisplayName("Check-In: Rejects check-in on vehicle in DAMAGED status (InvalidUsageSessionException)")
    void testCheckIn_Rejects_VehicleInDamaged() {
        testVehicle.setStatus(VehicleStatus.DAMAGED);
        CheckInRequest request = new CheckInRequest(500L, new BigDecimal("12000.00"), 90);

        when(bookingRepository.findById(500L)).thenReturn(Optional.of(testBooking));

        assertThrows(InvalidUsageSessionException.class,
                () -> usageSessionService.checkIn(request, 10L));
    }
}
