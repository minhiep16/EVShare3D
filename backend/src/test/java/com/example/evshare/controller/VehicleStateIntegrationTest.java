package com.example.evshare.controller;

import com.example.evshare.dto.request.*;
import com.example.evshare.entity.*;
import com.example.evshare.entity.enums.BookingStatus;
import com.example.evshare.entity.enums.RoleName;
import com.example.evshare.entity.enums.VehicleStatus;
import com.example.evshare.repository.*;
import com.example.evshare.security.TokenService;
import com.example.evshare.security.UserPrincipal;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@DisplayName("Checkpoint 05-M — Vehicle State Integration Lifecycle Tests")
class VehicleStateIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @Autowired private UserRepository userRepository;
    @Autowired private RoleRepository roleRepository;
    @Autowired private VehicleRepository vehicleRepository;
    @Autowired private OwnershipGroupRepository ownershipGroupRepository;
    @Autowired private OwnershipShareRepository ownershipShareRepository;
    @Autowired private BookingRepository bookingRepository;
    @Autowired private UsageSessionRepository usageSessionRepository;
    @Autowired private TokenService tokenService;

    private static final java.util.concurrent.atomic.AtomicLong PHONE_SEQ =
            new java.util.concurrent.atomic.AtomicLong(System.currentTimeMillis() % 80000000L + 10000000L);

    private User coOwner;
    private User staffUser;
    private Vehicle testVehicle;
    private OwnershipGroup testGroup;
    private String staffToken;

    @BeforeEach
    void setUp() {
        Role roleCoOwner = roleRepository.findByName(RoleName.ROLE_CO_OWNER)
                .orElseGet(() -> roleRepository.save(new Role(null, RoleName.ROLE_CO_OWNER)));
        Role roleStaff = roleRepository.findByName(RoleName.ROLE_STAFF)
                .orElseGet(() -> roleRepository.save(new Role(null, RoleName.ROLE_STAFF)));

        coOwner = createTestUser("vsi.owner." + UUID.randomUUID() + "@evshare.io", "Tran CoOwner", roleCoOwner);
        staffUser = createTestUser("vsi.staff." + UUID.randomUUID() + "@evshare.io", "Staff Operator", roleStaff);
        staffToken = tokenService.generateAccessToken(staffUser.getId(), staffUser.getEmail(), Collections.singletonList("ROLE_STAFF"));

        testVehicle = new Vehicle();
        testVehicle.setVin("VIN" + UUID.randomUUID().toString().replace("-", "").substring(0, 14).toUpperCase());
        testVehicle.setLicensePlate("51H-" + UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase());
        testVehicle.setModelName("VinFast VF8 Plus");
        testVehicle.setManufacturer("VinFast");
        testVehicle.setModel3dAssetPath("models/vehicles/vf8.glb");
        testVehicle.setStatus(VehicleStatus.AVAILABLE);
        testVehicle.setBatteryLevel(92);
        testVehicle.setOdometerKm(new BigDecimal("10000.00"));
        testVehicle.setStallLocationCode("BAY-VF8-01");
        testVehicle = vehicleRepository.saveAndFlush(testVehicle);

        testGroup = new OwnershipGroup();
        testGroup.setGroupName("VF8 Syndicate " + UUID.randomUUID().toString().substring(0, 8));
        testGroup.setVehicle(testVehicle);
        testGroup.setFormationDate(LocalDate.now());
        testGroup.setIsActive(true);
        testGroup = ownershipGroupRepository.saveAndFlush(testGroup);

        OwnershipShare share = new OwnershipShare();
        share.setGroup(testGroup);
        share.setUser(coOwner);
        share.setPercentage(new BigDecimal("50.00"));
        share.setShareCertificateNumber("CERT-" + UUID.randomUUID().toString().substring(0, 8));
        share.setIsActive(true);
        ownershipShareRepository.saveAndFlush(share);
    }

    private User createTestUser(String email, String fullName, Role role) {
        User u = new User();
        u.setEmail(email);
        u.setPasswordHash("$2a$12$dummyPasswordHashForTestingLifecycle12345");
        u.setFullName(fullName);
        u.setPhoneNumber(String.format("09%08d", PHONE_SEQ.incrementAndGet() % 100000000L));
        u.setIsActive(true);
        u.getRoles().add(role);
        return userRepository.saveAndFlush(u);
    }

    // =========================================================================
    // 1. COMPLETE OPERATIONAL LIFECYCLE: AVAILABLE -> BOOKED -> IN_USE -> AVAILABLE
    // =========================================================================
    @Test
    @DisplayName("1. Complete Operational Lifecycle: AVAILABLE -> BOOKED -> IN_USE -> AVAILABLE")
    void testCompleteOperationalLifecycle() throws Exception {
        // Step 1: Vehicle is AVAILABLE
        Vehicle initialVehicle = vehicleRepository.findById(testVehicle.getId()).orElseThrow();
        assertThat(initialVehicle.getStatus()).isEqualTo(VehicleStatus.AVAILABLE);

        // Step 2: Immediate Reservation Created -> Controlled Transition: AVAILABLE -> BOOKED
        Instant now = Instant.now().plus(2, ChronoUnit.MINUTES);
        Instant end = now.plus(2, ChronoUnit.HOURS);
        CreateBookingRequest bookingRequest = new CreateBookingRequest(testVehicle.getId(), now, end);

        MvcResult bookingResult = mockMvc.perform(post("/api/v1/bookings")
                        .with(user(UserPrincipal.create(coOwner)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bookingRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success", is(true)))
                .andReturn();

        Long bookingId = objectMapper.readTree(bookingResult.getResponse().getContentAsString())
                .path("data").path("id").asLong();

        // Verify vehicle is now BOOKED
        Vehicle bookedVehicle = vehicleRepository.findById(testVehicle.getId()).orElseThrow();
        assertThat(bookedVehicle.getStatus()).isEqualTo(VehicleStatus.BOOKED);

        // Step 3: Check-In -> Controlled Transition: BOOKED -> IN_USE
        CheckInRequest checkInRequest = new CheckInRequest(
                bookingId,
                new BigDecimal("10000.00"),
                92,
                null,
                "Clean start",
                List.of()
        );

        MvcResult checkInResult = mockMvc.perform(post("/api/v1/usage-sessions/check-in")
                        .with(user(UserPrincipal.create(coOwner)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(checkInRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success", is(true)))
                .andReturn();

        Long sessionId = objectMapper.readTree(checkInResult.getResponse().getContentAsString())
                .path("data").path("id").asLong();

        // Verify vehicle is now IN_USE
        Vehicle inUseVehicle = vehicleRepository.findById(testVehicle.getId()).orElseThrow();
        assertThat(inUseVehicle.getStatus()).isEqualTo(VehicleStatus.IN_USE);

        // Step 4: Check-Out -> Controlled Transition: IN_USE -> AVAILABLE
        CheckOutRequest checkOutRequest = new CheckOutRequest(
                new BigDecimal("10080.00"),
                75,
                false, // not plugged in
                false, // no damage
                null,
                "Trip concluded cleanly",
                List.of(),
                BigDecimal.ZERO
        );

        mockMvc.perform(post("/api/v1/usage-sessions/{id}/check-out", sessionId)
                        .with(user(UserPrincipal.create(coOwner)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(checkOutRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)));

        // Verify vehicle returned to AVAILABLE
        Vehicle availableVehicle = vehicleRepository.findById(testVehicle.getId()).orElseThrow();
        assertThat(availableVehicle.getStatus()).isEqualTo(VehicleStatus.AVAILABLE);
        assertThat(availableVehicle.getOdometerKm()).isEqualByComparingTo("10080.00");
        assertThat(availableVehicle.getBatteryLevel()).isEqualTo(75);
    }

    // =========================================================================
    // 2. CANCELLATION LIFECYCLE: AVAILABLE -> BOOKED -> (CANCELLED) -> AVAILABLE
    // =========================================================================
    @Test
    @DisplayName("2. Cancellation Lifecycle: BOOKED vehicle releases to AVAILABLE when booking cancelled")
    void testCancellationLifecycle() throws Exception {
        // Create imminent booking -> transitions vehicle to BOOKED
        Instant now = Instant.now().plus(5, ChronoUnit.MINUTES);
        Instant end = now.plus(2, ChronoUnit.HOURS);
        CreateBookingRequest bookingRequest = new CreateBookingRequest(testVehicle.getId(), now, end);

        MvcResult bookingResult = mockMvc.perform(post("/api/v1/bookings")
                        .with(user(UserPrincipal.create(coOwner)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bookingRequest)))
                .andExpect(status().isCreated())
                .andReturn();

        Long bookingId = objectMapper.readTree(bookingResult.getResponse().getContentAsString())
                .path("data").path("id").asLong();

        Vehicle bookedVehicle = vehicleRepository.findById(testVehicle.getId()).orElseThrow();
        assertThat(bookedVehicle.getStatus()).isEqualTo(VehicleStatus.BOOKED);

        // Cancel the booking -> Controlled release: BOOKED -> AVAILABLE
        CancelBookingRequest cancelReq = new CancelBookingRequest("Change of plans");
        mockMvc.perform(post("/api/v1/bookings/{id}/cancel", bookingId)
                        .with(user(UserPrincipal.create(coOwner)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(cancelReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.status", is("CANCELLED")));

        // Verify vehicle is released back to AVAILABLE
        Vehicle releasedVehicle = vehicleRepository.findById(testVehicle.getId()).orElseThrow();
        assertThat(releasedVehicle.getStatus()).isEqualTo(VehicleStatus.AVAILABLE);
    }

    // =========================================================================
    // 3. DAMAGE LIFECYCLE: IN_USE -> DAMAGED -> MAINTENANCE -> AVAILABLE
    // =========================================================================
    @Test
    @DisplayName("3. Damage Lifecycle: Check-out with damage flags DAMAGED, requires MAINTENANCE before AVAILABLE")
    void testDamageAndRepairLifecycle() throws Exception {
        // Set vehicle to IN_USE for active trip
        testVehicle.setStatus(VehicleStatus.IN_USE);
        vehicleRepository.saveAndFlush(testVehicle);

        Booking booking = new Booking();
        booking.setUser(coOwner);
        booking.setVehicle(testVehicle);
        booking.setStartTime(Instant.now().minus(1, ChronoUnit.HOURS));
        booking.setEndTime(Instant.now().plus(1, ChronoUnit.HOURS));
        booking.setStatus(BookingStatus.IN_USE);
        booking.setEstimatedCost(new BigDecimal("150000.00"));
        booking = bookingRepository.saveAndFlush(booking);

        UsageSession session = new UsageSession();
        session.setBooking(booking);
        session.setStartOdometer(new BigDecimal("10000.00"));
        session.setStartBattery(90);
        session.setCheckInTime(Instant.now().minus(1, ChronoUnit.HOURS));
        session.setStatus(com.example.evshare.entity.enums.UsageSessionStatus.ACTIVE);
        session = usageSessionRepository.saveAndFlush(session);

        // Check-out with damage detected: hasDamage = true
        CheckOutRequest checkOutRequest = new CheckOutRequest(
                new BigDecimal("10050.00"),
                70,
                false,
                true, // hasDamage = true
                "{\"defects\": [\"FRONT_BUMPER_DENT\"]}",
                "Vehicle collided with post during parking",
                List.of("https://s3.evshare.vn/damage-1.jpg"),
                new BigDecimal("500000.00") // damage surcharge
        );

        mockMvc.perform(post("/api/v1/usage-sessions/{id}/check-out", session.getId())
                        .with(user(UserPrincipal.create(coOwner)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(checkOutRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)));

        // 1. Verify vehicle transitioned to DAMAGED
        Vehicle damagedVehicle = vehicleRepository.findById(testVehicle.getId()).orElseThrow();
        assertThat(damagedVehicle.getStatus()).isEqualTo(VehicleStatus.DAMAGED);

        // 2. Invariant: DAMAGED vehicle CANNOT be booked
        Instant futureStart = Instant.now().plus(1, ChronoUnit.DAYS).truncatedTo(ChronoUnit.HOURS);
        Instant futureEnd = futureStart.plus(2, ChronoUnit.HOURS);
        CreateBookingRequest failBookingReq = new CreateBookingRequest(testVehicle.getId(), futureStart, futureEnd);

        mockMvc.perform(post("/api/v1/bookings")
                        .with(user(UserPrincipal.create(coOwner)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(failBookingReq)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message", containsString("damaged")));

        // 3. Move vehicle from DAMAGED -> MAINTENANCE (Work order at repair garage)
        UpdateVehicleStatusRequest maintReq = new UpdateVehicleStatusRequest(VehicleStatus.MAINTENANCE, "Sent to body repair shop");
        mockMvc.perform(patch("/api/v1/vehicles/{id}/status", testVehicle.getId())
                        .header("Authorization", "Bearer " + staffToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(maintReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status", is("MAINTENANCE")));

        // 4. Complete repair: MAINTENANCE -> AVAILABLE
        UpdateVehicleStatusRequest availReq = new UpdateVehicleStatusRequest(VehicleStatus.AVAILABLE, "Body repair completed and certified");
        mockMvc.perform(patch("/api/v1/vehicles/{id}/status", testVehicle.getId())
                        .header("Authorization", "Bearer " + staffToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(availReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status", is("AVAILABLE")));

        Vehicle repairedVehicle = vehicleRepository.findById(testVehicle.getId()).orElseThrow();
        assertThat(repairedVehicle.getStatus()).isEqualTo(VehicleStatus.AVAILABLE);
    }

    // =========================================================================
    // 4. MAINTENANCE LIFECYCLE: AVAILABLE/BOOKED -> MAINTENANCE -> AVAILABLE
    // =========================================================================
    @Test
    @DisplayName("4. Maintenance Lifecycle: Rejects bookings while in MAINTENANCE, returns to AVAILABLE")
    void testMaintenanceLifecycle() throws Exception {
        // AVAILABLE -> MAINTENANCE (Scheduled technical inspection)
        UpdateVehicleStatusRequest maintReq = new UpdateVehicleStatusRequest(VehicleStatus.MAINTENANCE, "Routine battery calibration");
        mockMvc.perform(patch("/api/v1/vehicles/{id}/status", testVehicle.getId())
                        .header("Authorization", "Bearer " + staffToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(maintReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status", is("MAINTENANCE")));

        // Invariant: Booking creation rejected while vehicle is in MAINTENANCE
        Instant futureStart = Instant.now().plus(1, ChronoUnit.DAYS).truncatedTo(ChronoUnit.HOURS);
        Instant futureEnd = futureStart.plus(2, ChronoUnit.HOURS);
        CreateBookingRequest bkgReq = new CreateBookingRequest(testVehicle.getId(), futureStart, futureEnd);

        mockMvc.perform(post("/api/v1/bookings")
                        .with(user(UserPrincipal.create(coOwner)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bkgReq)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message", containsString("maintenance")));

        // MAINTENANCE -> AVAILABLE
        UpdateVehicleStatusRequest availReq = new UpdateVehicleStatusRequest(VehicleStatus.AVAILABLE, "Calibration complete");
        mockMvc.perform(patch("/api/v1/vehicles/{id}/status", testVehicle.getId())
                        .header("Authorization", "Bearer " + staffToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(availReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status", is("AVAILABLE")));
    }

    // =========================================================================
    // 5. INVALID TRANSITION GUARDS: Disallowed Transitions Intercepted
    // =========================================================================
    @Test
    @DisplayName("5. Invalid Transition Guards: Rejects illegal jumps (DAMAGED -> AVAILABLE, MAINTENANCE -> IN_USE)")
    void testInvalidTransitions_RejectedByStateMachine() throws Exception {
        // Set vehicle to DAMAGED
        testVehicle.setStatus(VehicleStatus.DAMAGED);
        vehicleRepository.saveAndFlush(testVehicle);

        // Attempt illegal direct transition DAMAGED -> AVAILABLE (must go through MAINTENANCE)
        UpdateVehicleStatusRequest illegalReq1 = new UpdateVehicleStatusRequest(VehicleStatus.AVAILABLE, "Attempting illegal bypass");
        mockMvc.perform(patch("/api/v1/vehicles/{id}/status", testVehicle.getId())
                        .header("Authorization", "Bearer " + staffToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(illegalReq1)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message", containsString("Invalid state transition")));

        // Attempt illegal direct transition DAMAGED -> IN_USE
        UpdateVehicleStatusRequest illegalReq2 = new UpdateVehicleStatusRequest(VehicleStatus.IN_USE, "Attempting illegal check-in");
        mockMvc.perform(patch("/api/v1/vehicles/{id}/status", testVehicle.getId())
                        .header("Authorization", "Bearer " + staffToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(illegalReq2)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message", containsString("Invalid state transition")));

        // Set vehicle to MAINTENANCE
        testVehicle.setStatus(VehicleStatus.MAINTENANCE);
        vehicleRepository.saveAndFlush(testVehicle);

        // Attempt illegal direct transition MAINTENANCE -> IN_USE
        UpdateVehicleStatusRequest illegalReq3 = new UpdateVehicleStatusRequest(VehicleStatus.IN_USE, "Attempting illegal trip start");
        mockMvc.perform(patch("/api/v1/vehicles/{id}/status", testVehicle.getId())
                        .header("Authorization", "Bearer " + staffToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(illegalReq3)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message", containsString("Invalid state transition")));

        // Attempt redundant transition MAINTENANCE -> MAINTENANCE
        UpdateVehicleStatusRequest redundantReq = new UpdateVehicleStatusRequest(VehicleStatus.MAINTENANCE, "Redundant state");
        mockMvc.perform(patch("/api/v1/vehicles/{id}/status", testVehicle.getId())
                        .header("Authorization", "Bearer " + staffToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(redundantReq)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message", containsString("Redundant state transition")));
    }
}
