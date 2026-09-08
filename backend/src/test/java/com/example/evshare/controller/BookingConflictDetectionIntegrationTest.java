package com.example.evshare.controller;

import com.example.evshare.dto.request.CreateBookingRequest;
import com.example.evshare.entity.*;
import com.example.evshare.entity.enums.*;
import com.example.evshare.exception.BookingConflictException;
import com.example.evshare.repository.*;
import com.example.evshare.security.UserPrincipal;
import com.example.evshare.service.BookingService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("Checkpoint 05-F — Robust Overlapping Booking Prevention & Concurrency Tests")
class BookingConflictDetectionIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private BookingService bookingService;

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private VehicleRepository vehicleRepository;

    @Autowired
    private OwnershipGroupRepository ownershipGroupRepository;

    @Autowired
    private OwnershipShareRepository ownershipShareRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private AuditLogRepository auditLogRepository;

    private User createTestUser(String email, String fullName, Role role) {
        User u = new User();
        u.setEmail(email);
        u.setPasswordHash("$2a$12$dummyHashForConflictTestingPurposes12345678");
        u.setFullName(fullName);
        u.setIsActive(true);
        u.getRoles().add(role);
        return userRepository.saveAndFlush(u);
    }

    private Vehicle createTestVehicle(String model, String manufacturer, String bay) {
        Vehicle v = new Vehicle();
        v.setVin("VIN" + UUID.randomUUID().toString().replace("-", "").substring(0, 14).toUpperCase());
        v.setLicensePlate("51M-" + (int) (10000 + Math.random() * 89999));
        v.setModelName(model);
        v.setManufacturer(manufacturer);
        v.setModel3dAssetPath("models/vehicles/" + manufacturer.toLowerCase() + ".glb");
        v.setStatus(VehicleStatus.AVAILABLE);
        v.setBatteryLevel(90);
        v.setOdometerKm(new BigDecimal("15000.00"));
        v.setStallLocationCode(bay);
        return vehicleRepository.saveAndFlush(v);
    }

    private OwnershipGroup createTestGroup(String name, Vehicle vehicle) {
        OwnershipGroup g = new OwnershipGroup();
        g.setGroupName(name);
        g.setVehicle(vehicle);
        g.setFormationDate(LocalDate.now());
        g.setIsActive(true);
        return ownershipGroupRepository.saveAndFlush(g);
    }

    private OwnershipShare createShare(OwnershipGroup group, User user, BigDecimal pct) {
        OwnershipShare s = new OwnershipShare();
        s.setGroup(group);
        s.setUser(user);
        s.setPercentage(pct);
        s.setShareCertificateNumber("CERT-" + UUID.randomUUID().toString().substring(0, 8));
        s.setIsActive(true);
        return ownershipShareRepository.saveAndFlush(s);
    }

    private Booking createExistingBooking(Vehicle vehicle, User user, Instant start, Instant end, BookingStatus status) {
        Booking b = Booking.builder()
                .vehicle(vehicle)
                .user(user)
                .startTime(start)
                .endTime(end)
                .status(status)
                .estimatedCost(new BigDecimal("150000.00"))
                .createdAt(Instant.now())
                .build();
        return bookingRepository.saveAndFlush(b);
    }

    private Role getOrCreateCoOwnerRole() {
        return roleRepository.findByName(RoleName.ROLE_CO_OWNER)
                .orElseGet(() -> roleRepository.save(new Role(null, RoleName.ROLE_CO_OWNER)));
    }

    private UserPrincipal toPrincipal(User u) {
        return UserPrincipal.create(u);
    }

    // =========================================================================
    // 1. EXACT SAME TIME CONFLICT
    // =========================================================================

    @Test
    @Transactional
    @DisplayName("1. Exact Same Time: Collides identically with existing reservation (409 Conflict)")
    void testExactSameTimeConflict() throws Exception {
        Role role = getOrCreateCoOwnerRole();
        User userA = createTestUser("conf.a1." + UUID.randomUUID() + "@evshare.io", "User A1", role);
        User userB = createTestUser("conf.b1." + UUID.randomUUID() + "@evshare.io", "User B1", role);
        Vehicle vehicle = createTestVehicle("Lucid Air Dream", "Lucid", "BAY-L1");
        OwnershipGroup group = createTestGroup("Lucid Group 1", vehicle);
        createShare(group, userA, new BigDecimal("50.00"));
        createShare(group, userB, new BigDecimal("50.00"));

        Instant baseStart = Instant.now().plus(2, ChronoUnit.DAYS).truncatedTo(ChronoUnit.HOURS);
        Instant baseEnd = baseStart.plus(3, ChronoUnit.HOURS);
        createExistingBooking(vehicle, userA, baseStart, baseEnd, BookingStatus.CONFIRMED);

        CreateBookingRequest req = new CreateBookingRequest(vehicle.getId(), baseStart, baseEnd);

        mockMvc.perform(post("/api/v1/bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req))
                        .with(user(toPrincipal(userB))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message", containsString("30-minute turnaround buffer")));
    }

    // =========================================================================
    // 2. PARTIAL OVERLAPS
    // =========================================================================

    @Test
    @Transactional
    @DisplayName("2. Partial Overlap (Start): Candidate starts before and ends inside existing booking (409 Conflict)")
    void testPartialOverlap_StartOverlap() throws Exception {
        Role role = getOrCreateCoOwnerRole();
        User userA = createTestUser("conf.a2." + UUID.randomUUID() + "@evshare.io", "User A2", role);
        User userB = createTestUser("conf.b2." + UUID.randomUUID() + "@evshare.io", "User B2", role);
        Vehicle vehicle = createTestVehicle("Lucid Air", "Lucid", "BAY-L2");
        OwnershipGroup group = createTestGroup("Lucid Group 2", vehicle);
        createShare(group, userA, new BigDecimal("50.00"));
        createShare(group, userB, new BigDecimal("50.00"));

        Instant baseStart = Instant.now().plus(3, ChronoUnit.DAYS).truncatedTo(ChronoUnit.HOURS);
        Instant baseEnd = baseStart.plus(3, ChronoUnit.HOURS);
        createExistingBooking(vehicle, userA, baseStart, baseEnd, BookingStatus.CONFIRMED);

        // Candidate: [baseStart - 1h, baseStart + 1h]
        CreateBookingRequest req = new CreateBookingRequest(vehicle.getId(),
                baseStart.minus(1, ChronoUnit.HOURS),
                baseStart.plus(1, ChronoUnit.HOURS));

        mockMvc.perform(post("/api/v1/bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req))
                        .with(user(toPrincipal(userB))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message", containsString("30-minute turnaround buffer")));
    }

    @Test
    @Transactional
    @DisplayName("3. Partial Overlap (End): Candidate starts inside and ends after existing booking (409 Conflict)")
    void testPartialOverlap_EndOverlap() throws Exception {
        Role role = getOrCreateCoOwnerRole();
        User userA = createTestUser("conf.a3." + UUID.randomUUID() + "@evshare.io", "User A3", role);
        User userB = createTestUser("conf.b3." + UUID.randomUUID() + "@evshare.io", "User B3", role);
        Vehicle vehicle = createTestVehicle("Lucid Air", "Lucid", "BAY-L3");
        OwnershipGroup group = createTestGroup("Lucid Group 3", vehicle);
        createShare(group, userA, new BigDecimal("50.00"));
        createShare(group, userB, new BigDecimal("50.00"));

        Instant baseStart = Instant.now().plus(4, ChronoUnit.DAYS).truncatedTo(ChronoUnit.HOURS);
        Instant baseEnd = baseStart.plus(3, ChronoUnit.HOURS);
        createExistingBooking(vehicle, userA, baseStart, baseEnd, BookingStatus.CONFIRMED);

        // Candidate: [baseEnd - 1h, baseEnd + 2h]
        CreateBookingRequest req = new CreateBookingRequest(vehicle.getId(),
                baseEnd.minus(1, ChronoUnit.HOURS),
                baseEnd.plus(2, ChronoUnit.HOURS));

        mockMvc.perform(post("/api/v1/bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req))
                        .with(user(toPrincipal(userB))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message", containsString("30-minute turnaround buffer")));
    }

    // =========================================================================
    // 3. CONTAINED BOOKING
    // =========================================================================

    @Test
    @Transactional
    @DisplayName("4. Contained Booking (Enclosing): Candidate completely wraps around existing booking (409 Conflict)")
    void testContainedBooking_Enclosing() throws Exception {
        Role role = getOrCreateCoOwnerRole();
        User userA = createTestUser("conf.a4." + UUID.randomUUID() + "@evshare.io", "User A4", role);
        User userB = createTestUser("conf.b4." + UUID.randomUUID() + "@evshare.io", "User B4", role);
        Vehicle vehicle = createTestVehicle("Lucid Air", "Lucid", "BAY-L4");
        OwnershipGroup group = createTestGroup("Lucid Group 4", vehicle);
        createShare(group, userA, new BigDecimal("50.00"));
        createShare(group, userB, new BigDecimal("50.00"));

        Instant baseStart = Instant.now().plus(5, ChronoUnit.DAYS).truncatedTo(ChronoUnit.HOURS);
        Instant baseEnd = baseStart.plus(2, ChronoUnit.HOURS);
        createExistingBooking(vehicle, userA, baseStart, baseEnd, BookingStatus.CONFIRMED);

        // Candidate starts 1h before baseStart and ends 1h after baseEnd
        CreateBookingRequest req = new CreateBookingRequest(vehicle.getId(),
                baseStart.minus(1, ChronoUnit.HOURS),
                baseEnd.plus(1, ChronoUnit.HOURS));

        mockMvc.perform(post("/api/v1/bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req))
                        .with(user(toPrincipal(userB))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message", containsString("30-minute turnaround buffer")));
    }

    @Test
    @Transactional
    @DisplayName("5. Contained Booking (Enclosed): Candidate completely fits inside existing booking (409 Conflict)")
    void testContainedBooking_Enclosed() throws Exception {
        Role role = getOrCreateCoOwnerRole();
        User userA = createTestUser("conf.a5." + UUID.randomUUID() + "@evshare.io", "User A5", role);
        User userB = createTestUser("conf.b5." + UUID.randomUUID() + "@evshare.io", "User B5", role);
        Vehicle vehicle = createTestVehicle("Lucid Air", "Lucid", "BAY-L5");
        OwnershipGroup group = createTestGroup("Lucid Group 5", vehicle);
        createShare(group, userA, new BigDecimal("50.00"));
        createShare(group, userB, new BigDecimal("50.00"));

        Instant baseStart = Instant.now().plus(6, ChronoUnit.DAYS).truncatedTo(ChronoUnit.HOURS);
        Instant baseEnd = baseStart.plus(4, ChronoUnit.HOURS);
        createExistingBooking(vehicle, userA, baseStart, baseEnd, BookingStatus.CONFIRMED);

        // Candidate fits entirely in the middle: [baseStart + 1h, baseStart + 2h]
        CreateBookingRequest req = new CreateBookingRequest(vehicle.getId(),
                baseStart.plus(1, ChronoUnit.HOURS),
                baseStart.plus(2, ChronoUnit.HOURS));

        mockMvc.perform(post("/api/v1/bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req))
                        .with(user(toPrincipal(userB))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message", containsString("30-minute turnaround buffer")));
    }

    // =========================================================================
    // 4. ADJACENT BOOKING & TURNAROUND BUFFER
    // =========================================================================

    @Test
    @Transactional
    @DisplayName("6. Adjacent Booking (After Buffer): Starts exactly at end + 30m succeeds (201 Created)")
    void testAdjacentBooking_ValidAfterBuffer() throws Exception {
        Role role = getOrCreateCoOwnerRole();
        User userA = createTestUser("conf.a6." + UUID.randomUUID() + "@evshare.io", "User A6", role);
        User userB = createTestUser("conf.b6." + UUID.randomUUID() + "@evshare.io", "User B6", role);
        Vehicle vehicle = createTestVehicle("Lucid Air", "Lucid", "BAY-L6");
        OwnershipGroup group = createTestGroup("Lucid Group 6", vehicle);
        createShare(group, userA, new BigDecimal("50.00"));
        createShare(group, userB, new BigDecimal("50.00"));

        Instant baseStart = Instant.now().plus(7, ChronoUnit.DAYS).truncatedTo(ChronoUnit.HOURS);
        Instant baseEnd = baseStart.plus(2, ChronoUnit.HOURS);
        createExistingBooking(vehicle, userA, baseStart, baseEnd, BookingStatus.CONFIRMED);

        // Candidate starts exactly at baseEnd + 30m
        Instant reqStart = baseEnd.plus(30, ChronoUnit.MINUTES);
        Instant reqEnd = reqStart.plus(2, ChronoUnit.HOURS);

        CreateBookingRequest req = new CreateBookingRequest(vehicle.getId(), reqStart, reqEnd);

        mockMvc.perform(post("/api/v1/bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req))
                        .with(user(toPrincipal(userB))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.status", is("CONFIRMED")));
    }

    @Test
    @Transactional
    @DisplayName("7. Adjacent Booking (Inside Buffer After): Starts at end + 15m rejected (409 Conflict)")
    void testAdjacentBooking_InsideBufferAfter() throws Exception {
        Role role = getOrCreateCoOwnerRole();
        User userA = createTestUser("conf.a7." + UUID.randomUUID() + "@evshare.io", "User A7", role);
        User userB = createTestUser("conf.b7." + UUID.randomUUID() + "@evshare.io", "User B7", role);
        Vehicle vehicle = createTestVehicle("Lucid Air", "Lucid", "BAY-L7");
        OwnershipGroup group = createTestGroup("Lucid Group 7", vehicle);
        createShare(group, userA, new BigDecimal("50.00"));
        createShare(group, userB, new BigDecimal("50.00"));

        Instant baseStart = Instant.now().plus(8, ChronoUnit.DAYS).truncatedTo(ChronoUnit.HOURS);
        Instant baseEnd = baseStart.plus(2, ChronoUnit.HOURS);
        createExistingBooking(vehicle, userA, baseStart, baseEnd, BookingStatus.CONFIRMED);

        // Candidate starts at baseEnd + 15m (inside 30m turnaround buffer!)
        Instant reqStart = baseEnd.plus(15, ChronoUnit.MINUTES);
        Instant reqEnd = reqStart.plus(2, ChronoUnit.HOURS);

        CreateBookingRequest req = new CreateBookingRequest(vehicle.getId(), reqStart, reqEnd);

        mockMvc.perform(post("/api/v1/bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req))
                        .with(user(toPrincipal(userB))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message", containsString("30-minute turnaround buffer")));
    }

    @Test
    @Transactional
    @DisplayName("8. Adjacent Booking (Before Buffer): Ends exactly at start - 30m succeeds (201 Created)")
    void testAdjacentBooking_ValidBeforeBuffer() throws Exception {
        Role role = getOrCreateCoOwnerRole();
        User userA = createTestUser("conf.a8." + UUID.randomUUID() + "@evshare.io", "User A8", role);
        User userB = createTestUser("conf.b8." + UUID.randomUUID() + "@evshare.io", "User B8", role);
        Vehicle vehicle = createTestVehicle("Lucid Air", "Lucid", "BAY-L8");
        OwnershipGroup group = createTestGroup("Lucid Group 8", vehicle);
        createShare(group, userA, new BigDecimal("50.00"));
        createShare(group, userB, new BigDecimal("50.00"));

        Instant baseStart = Instant.now().plus(9, ChronoUnit.DAYS).truncatedTo(ChronoUnit.HOURS);
        Instant baseEnd = baseStart.plus(2, ChronoUnit.HOURS);
        createExistingBooking(vehicle, userA, baseStart, baseEnd, BookingStatus.CONFIRMED);

        // Candidate ends exactly at baseStart - 30m
        Instant reqEnd = baseStart.minus(30, ChronoUnit.MINUTES);
        Instant reqStart = reqEnd.minus(2, ChronoUnit.HOURS);

        CreateBookingRequest req = new CreateBookingRequest(vehicle.getId(), reqStart, reqEnd);

        mockMvc.perform(post("/api/v1/bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req))
                        .with(user(toPrincipal(userB))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success", is(true)));
    }

    @Test
    @Transactional
    @DisplayName("9. Adjacent Booking (Inside Buffer Before): Ends at start - 15m rejected (409 Conflict)")
    void testAdjacentBooking_InsideBufferBefore() throws Exception {
        Role role = getOrCreateCoOwnerRole();
        User userA = createTestUser("conf.a9." + UUID.randomUUID() + "@evshare.io", "User A9", role);
        User userB = createTestUser("conf.b9." + UUID.randomUUID() + "@evshare.io", "User B9", role);
        Vehicle vehicle = createTestVehicle("Lucid Air", "Lucid", "BAY-L9");
        OwnershipGroup group = createTestGroup("Lucid Group 9", vehicle);
        createShare(group, userA, new BigDecimal("50.00"));
        createShare(group, userB, new BigDecimal("50.00"));

        Instant baseStart = Instant.now().plus(10, ChronoUnit.DAYS).truncatedTo(ChronoUnit.HOURS);
        Instant baseEnd = baseStart.plus(2, ChronoUnit.HOURS);
        createExistingBooking(vehicle, userA, baseStart, baseEnd, BookingStatus.CONFIRMED);

        // Candidate ends at baseStart - 15m (inside buffer before!)
        Instant reqEnd = baseStart.minus(15, ChronoUnit.MINUTES);
        Instant reqStart = reqEnd.minus(2, ChronoUnit.HOURS);

        CreateBookingRequest req = new CreateBookingRequest(vehicle.getId(), reqStart, reqEnd);

        mockMvc.perform(post("/api/v1/bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req))
                        .with(user(toPrincipal(userB))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message", containsString("30-minute turnaround buffer")));
    }

    // =========================================================================
    // 5. DIFFERENT VEHICLES
    // =========================================================================

    @Test
    @Transactional
    @DisplayName("10. Different Vehicles: Identical time range on Vehicle B does not conflict with Vehicle A (201 Created)")
    void testDifferentVehicles_NoConflict() throws Exception {
        Role role = getOrCreateCoOwnerRole();
        User userA = createTestUser("conf.a10." + UUID.randomUUID() + "@evshare.io", "User A10", role);
        User userB = createTestUser("conf.b10." + UUID.randomUUID() + "@evshare.io", "User B10", role);
        Vehicle vehicle1 = createTestVehicle("Lucid Air", "Lucid", "BAY-L10");
        Vehicle vehicle2 = createTestVehicle("Mercedes EQS", "Mercedes", "BAY-M10");

        OwnershipGroup group1 = createTestGroup("Lucid Group 10", vehicle1);
        createShare(group1, userA, new BigDecimal("100.00"));

        OwnershipGroup group2 = createTestGroup("EQS Group 10", vehicle2);
        createShare(group2, userB, new BigDecimal("100.00"));

        Instant baseStart = Instant.now().plus(11, ChronoUnit.DAYS).truncatedTo(ChronoUnit.HOURS);
        Instant baseEnd = baseStart.plus(3, ChronoUnit.HOURS);

        // Vehicle 1 has active booking
        createExistingBooking(vehicle1, userA, baseStart, baseEnd, BookingStatus.CONFIRMED);

        // Vehicle 2 requested for the EXACT same interval by userB
        CreateBookingRequest req = new CreateBookingRequest(vehicle2.getId(), baseStart, baseEnd);

        mockMvc.perform(post("/api/v1/bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req))
                        .with(user(toPrincipal(userB))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.vehicleId", is(vehicle2.getId().intValue())));
    }

    // =========================================================================
    // 6. CANCELLED & REJECTED EXCLUSION
    // =========================================================================

    @Test
    @Transactional
    @DisplayName("11. Cancelled Booking: Prior cancelled reservation does not block slot (201 Created)")
    void testCancelledBooking_DoesNotBlock() throws Exception {
        Role role = getOrCreateCoOwnerRole();
        User userA = createTestUser("conf.a11." + UUID.randomUUID() + "@evshare.io", "User A11", role);
        User userB = createTestUser("conf.b11." + UUID.randomUUID() + "@evshare.io", "User B11", role);
        Vehicle vehicle = createTestVehicle("Lucid Air", "Lucid", "BAY-L11");
        OwnershipGroup group = createTestGroup("Lucid Group 11", vehicle);
        createShare(group, userA, new BigDecimal("50.00"));
        createShare(group, userB, new BigDecimal("50.00"));

        Instant baseStart = Instant.now().plus(12, ChronoUnit.DAYS).truncatedTo(ChronoUnit.HOURS);
        Instant baseEnd = baseStart.plus(3, ChronoUnit.HOURS);

        // Vehicle had a CANCELLED reservation
        createExistingBooking(vehicle, userA, baseStart, baseEnd, BookingStatus.CANCELLED);

        // New request for the exact same slot
        CreateBookingRequest req = new CreateBookingRequest(vehicle.getId(), baseStart, baseEnd);

        mockMvc.perform(post("/api/v1/bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req))
                        .with(user(toPrincipal(userB))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.status", is("CONFIRMED")));
    }

    @Test
    @Transactional
    @DisplayName("12. Rejected Booking: Prior rejected reservation does not block slot (201 Created)")
    void testRejectedBooking_DoesNotBlock() throws Exception {
        Role role = getOrCreateCoOwnerRole();
        User userA = createTestUser("conf.a12." + UUID.randomUUID() + "@evshare.io", "User A12", role);
        User userB = createTestUser("conf.b12." + UUID.randomUUID() + "@evshare.io", "User B12", role);
        Vehicle vehicle = createTestVehicle("Lucid Air", "Lucid", "BAY-L12");
        OwnershipGroup group = createTestGroup("Lucid Group 12", vehicle);
        createShare(group, userA, new BigDecimal("50.00"));
        createShare(group, userB, new BigDecimal("50.00"));

        Instant baseStart = Instant.now().plus(13, ChronoUnit.DAYS).truncatedTo(ChronoUnit.HOURS);
        Instant baseEnd = baseStart.plus(3, ChronoUnit.HOURS);

        // Vehicle had a REJECTED reservation
        createExistingBooking(vehicle, userA, baseStart, baseEnd, BookingStatus.REJECTED);

        // New request for the exact same slot
        CreateBookingRequest req = new CreateBookingRequest(vehicle.getId(), baseStart, baseEnd);

        mockMvc.perform(post("/api/v1/bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req))
                        .with(user(toPrincipal(userB))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.status", is("CONFIRMED")));
    }

    // =========================================================================
    // 7. CONCURRENT BOOKING ATTEMPTS (PESSIMISTIC ROW LOCKING)
    // =========================================================================

    @Test
    @DisplayName("13. Concurrency: Two simultaneous booking attempts for same slot serialize with exactly 1 winner and 1 409 Conflict")
    void testConcurrentBookingAttempt_PessimisticLocking() throws Exception {
        Role role = getOrCreateCoOwnerRole();
        User userA = createTestUser("conf.ca." + UUID.randomUUID() + "@evshare.io", "Concurrent User A", role);
        User userB = createTestUser("conf.cb." + UUID.randomUUID() + "@evshare.io", "Concurrent User B", role);
        Vehicle vehicle = createTestVehicle("Lucid Air Conc", "Lucid", "BAY-CONC");
        OwnershipGroup group = createTestGroup("Lucid Group Conc", vehicle);
        createShare(group, userA, new BigDecimal("50.00"));
        createShare(group, userB, new BigDecimal("50.00"));

        Long vehicleId = vehicle.getId();
        Long userAId = userA.getId();
        Long userBId = userB.getId();

        Instant baseStart = Instant.now().plus(14, ChronoUnit.DAYS).truncatedTo(ChronoUnit.HOURS);
        Instant baseEnd = baseStart.plus(2, ChronoUnit.HOURS);

        int numThreads = 2;
        ExecutorService executor = Executors.newFixedThreadPool(numThreads);
        CountDownLatch readyLatch = new CountDownLatch(numThreads);
        CountDownLatch startLatch = new CountDownLatch(1);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger conflictCount = new AtomicInteger(0);

        Callable<Void> taskA = () -> {
            SecurityContextHolder.getContext().setAuthentication(
                    new UsernamePasswordAuthenticationToken("userA", null,
                            Collections.singletonList(new SimpleGrantedAuthority("ROLE_CO_OWNER")))
            );
            readyLatch.countDown();
            startLatch.await();
            try {
                CreateBookingRequest req = new CreateBookingRequest(vehicleId, baseStart, baseEnd);
                bookingService.createBooking(req, userAId, false);
                successCount.incrementAndGet();
            } catch (BookingConflictException ex) {
                conflictCount.incrementAndGet();
            }
            return null;
        };

        Callable<Void> taskB = () -> {
            SecurityContextHolder.getContext().setAuthentication(
                    new UsernamePasswordAuthenticationToken("userB", null,
                            Collections.singletonList(new SimpleGrantedAuthority("ROLE_CO_OWNER")))
            );
            readyLatch.countDown();
            startLatch.await();
            try {
                CreateBookingRequest req = new CreateBookingRequest(vehicleId, baseStart, baseEnd);
                bookingService.createBooking(req, userBId, false);
                successCount.incrementAndGet();
            } catch (BookingConflictException ex) {
                conflictCount.incrementAndGet();
            }
            return null;
        };

        Future<Void> f1 = executor.submit(taskA);
        Future<Void> f2 = executor.submit(taskB);

        // Wait until both threads are ready
        readyLatch.await(5, TimeUnit.SECONDS);

        // Unleash both threads simultaneously
        startLatch.countDown();

        f1.get(10, TimeUnit.SECONDS);
        f2.get(10, TimeUnit.SECONDS);

        executor.shutdown();
        executor.awaitTermination(5, TimeUnit.SECONDS);

        try {
            // Assert exactly one transaction succeeded and the other caught a BookingConflictException
            assertEquals(1, successCount.get(), "Exactly one concurrent booking attempt must succeed");
            assertEquals(1, conflictCount.get(), "Exactly one concurrent booking attempt must encounter a Conflict");

            // Assert database state: exactly 1 booking exists in the database for vehicle
            List<Booking> activeBookings = bookingRepository.findByVehicleIdAndStatus(vehicleId, BookingStatus.CONFIRMED);
            assertEquals(1, activeBookings.size(), "Only 1 booking must be saved in database, completely avoiding race condition");
        } finally {
            // Clean up entities created for this non-transactional test
            bookingRepository.deleteAll(bookingRepository.findByVehicleId(vehicleId));
            ownershipShareRepository.deleteAll(ownershipShareRepository.findByGroupId(group.getId()));
            ownershipGroupRepository.deleteById(group.getId());
            vehicleRepository.deleteById(vehicleId);
            auditLogRepository.deleteAll(auditLogRepository.findByUserIdOrderByCreatedAtDesc(userAId));
            auditLogRepository.deleteAll(auditLogRepository.findByUserIdOrderByCreatedAtDesc(userBId));
            userRepository.deleteById(userAId);
            userRepository.deleteById(userBId);
        }
    }
}
