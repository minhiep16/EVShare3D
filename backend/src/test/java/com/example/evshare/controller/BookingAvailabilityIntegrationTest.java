package com.example.evshare.controller;

import com.example.evshare.entity.*;
import com.example.evshare.entity.enums.*;
import com.example.evshare.repository.*;
import com.example.evshare.security.UserPrincipal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@DisplayName("Checkpoint 05-C — Booking Availability & Timeline Integration Tests")
class BookingAvailabilityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

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

    private User memberCoOwner;
    private User nonMemberCoOwner;
    private User staffUser;
    private User adminUser;
    private Vehicle testVehicle;
    private OwnershipGroup testGroup;

    @BeforeEach
    void setUp() {
        Role roleCoOwner = roleRepository.findByName(RoleName.ROLE_CO_OWNER)
                .orElseGet(() -> roleRepository.save(new Role(null, RoleName.ROLE_CO_OWNER)));
        Role roleStaff = roleRepository.findByName(RoleName.ROLE_STAFF)
                .orElseGet(() -> roleRepository.save(new Role(null, RoleName.ROLE_STAFF)));
        Role roleAdmin = roleRepository.findByName(RoleName.ROLE_ADMIN)
                .orElseGet(() -> roleRepository.save(new Role(null, RoleName.ROLE_ADMIN)));

        memberCoOwner = createTestUser("member." + UUID.randomUUID() + "@evshare.io", "Member Co-Owner", roleCoOwner);
        nonMemberCoOwner = createTestUser("nonmember." + UUID.randomUUID() + "@evshare.io", "Non-Member Co-Owner", roleCoOwner);
        staffUser = createTestUser("staff." + UUID.randomUUID() + "@evshare.io", "Staff Member", roleStaff);
        adminUser = createTestUser("admin." + UUID.randomUUID() + "@evshare.io", "Admin User", roleAdmin);

        testVehicle = new Vehicle();
        testVehicle.setVin("VIN" + UUID.randomUUID().toString().replace("-", "").substring(0, 14).toUpperCase());
        testVehicle.setLicensePlate("51H-" + (int) (10000 + Math.random() * 89999));
        testVehicle.setModelName("Audi e-tron GT");
        testVehicle.setManufacturer("Audi");
        testVehicle.setModel3dAssetPath("models/vehicles/etron.glb");
        testVehicle.setStatus(VehicleStatus.AVAILABLE);
        testVehicle.setBatteryLevel(90);
        testVehicle.setOdometerKm(new BigDecimal("5000.00"));
        testVehicle.setStallLocationCode("BAY-03");
        testVehicle = vehicleRepository.saveAndFlush(testVehicle);

        testGroup = new OwnershipGroup();
        testGroup.setGroupName("Syndicate " + UUID.randomUUID().toString().substring(0, 8));
        testGroup.setVehicle(testVehicle);
        testGroup.setFormationDate(LocalDate.now());
        testGroup.setIsActive(true);
        testGroup = ownershipGroupRepository.saveAndFlush(testGroup);

        OwnershipShare memberShare = new OwnershipShare();
        memberShare.setGroup(testGroup);
        memberShare.setUser(memberCoOwner);
        memberShare.setPercentage(new BigDecimal("100.00"));
        memberShare.setShareCertificateNumber("CERT-" + UUID.randomUUID().toString().substring(0, 8));
        memberShare.setIsActive(true);
        ownershipShareRepository.saveAndFlush(memberShare);
    }

    private User createTestUser(String email, String fullName, Role role) {
        User u = new User();
        u.setEmail(email);
        u.setPasswordHash("$2a$12$dummyHashForAvailabilityIntegrationTestingPurpose12345");
        u.setFullName(fullName);
        u.setIsActive(true);
        u.getRoles().add(role);
        return userRepository.saveAndFlush(u);
    }

    private UserPrincipal toPrincipal(User u) {
        return UserPrincipal.create(u);
    }

    @Test
    @DisplayName("1. Free Vehicle: Returns isAvailable=true for future slot with no bookings")
    void testFreeVehicleAvailable() throws Exception {
        Instant start = Instant.now().plus(1, ChronoUnit.DAYS).truncatedTo(ChronoUnit.HOURS);
        Instant end = start.plus(2, ChronoUnit.HOURS);

        mockMvc.perform(get("/api/v1/bookings/availability")
                        .param("vehicleId", testVehicle.getId().toString())
                        .param("startTime", start.toString())
                        .param("endTime", end.toString())
                        .with(user(toPrincipal(memberCoOwner))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.vehicleId", is(testVehicle.getId().intValue())))
                .andExpect(jsonPath("$.data.vehicleStatus", is("AVAILABLE")))
                .andExpect(jsonPath("$.data.isAvailable", is(true)))
                .andExpect(jsonPath("$.data.conflictingBookings", hasSize(0)));
    }

    @Test
    @DisplayName("2. Existing Booking Conflict: Returns isAvailable=false when overlapping an existing confirmed booking")
    void testExistingBookingConflict() throws Exception {
        Instant bookedStart = Instant.now().plus(2, ChronoUnit.DAYS).truncatedTo(ChronoUnit.HOURS);
        Instant bookedEnd = bookedStart.plus(3, ChronoUnit.HOURS);

        bookingRepository.saveAndFlush(
                Booking.builder()
                        .vehicle(testVehicle)
                        .user(memberCoOwner)
                        .startTime(bookedStart)
                        .endTime(bookedEnd)
                        .status(BookingStatus.CONFIRMED)
                        .estimatedCost(new BigDecimal("300000.00"))
                        .build()
        );

        // Requested interval overlaps [bookedStart + 1h, bookedEnd + 1h]
        Instant reqStart = bookedStart.plus(1, ChronoUnit.HOURS);
        Instant reqEnd = bookedEnd.plus(1, ChronoUnit.HOURS);

        mockMvc.perform(get("/api/v1/bookings/availability")
                        .param("vehicleId", testVehicle.getId().toString())
                        .param("startTime", reqStart.toString())
                        .param("endTime", reqEnd.toString())
                        .with(user(toPrincipal(memberCoOwner))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.isAvailable", is(false)))
                .andExpect(jsonPath("$.data.reason", containsString("conflicts with an existing reservation")))
                .andExpect(jsonPath("$.data.conflictingBookings", hasSize(1)))
                .andExpect(jsonPath("$.data.conflictingBookings[0].status", is("CONFIRMED")));
    }

    @Test
    @DisplayName("3. 30-Minute Turnaround Buffer: Rejects booking starting 15 minutes after existing booking ends")
    void testTurnaroundBufferAfterExistingBooking() throws Exception {
        Instant bookedStart = Instant.now().plus(3, ChronoUnit.DAYS).truncatedTo(ChronoUnit.HOURS);
        Instant bookedEnd = bookedStart.plus(2, ChronoUnit.HOURS);

        bookingRepository.saveAndFlush(
                Booking.builder()
                        .vehicle(testVehicle)
                        .user(memberCoOwner)
                        .startTime(bookedStart)
                        .endTime(bookedEnd)
                        .status(BookingStatus.CONFIRMED)
                        .build()
        );

        // Request starts 15 minutes after bookedEnd (within 30-min turnaround buffer)
        Instant reqStart = bookedEnd.plus(15, ChronoUnit.MINUTES);
        Instant reqEnd = reqStart.plus(2, ChronoUnit.HOURS);

        mockMvc.perform(get("/api/v1/bookings/availability")
                        .param("vehicleId", testVehicle.getId().toString())
                        .param("startTime", reqStart.toString())
                        .param("endTime", reqEnd.toString())
                        .with(user(toPrincipal(memberCoOwner))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.isAvailable", is(false)))
                .andExpect(jsonPath("$.data.conflictingBookings", hasSize(1)));
    }

    @Test
    @DisplayName("4. Turnaround Buffer Boundary: Permits booking starting exactly 30 minutes after existing booking ends")
    void testTurnaroundBufferClearAfter30Minutes() throws Exception {
        Instant bookedStart = Instant.now().plus(4, ChronoUnit.DAYS).truncatedTo(ChronoUnit.HOURS);
        Instant bookedEnd = bookedStart.plus(2, ChronoUnit.HOURS);

        bookingRepository.saveAndFlush(
                Booking.builder()
                        .vehicle(testVehicle)
                        .user(memberCoOwner)
                        .startTime(bookedStart)
                        .endTime(bookedEnd)
                        .status(BookingStatus.CONFIRMED)
                        .build()
        );

        // Request starts exactly 30 minutes after bookedEnd
        Instant reqStart = bookedEnd.plus(30, ChronoUnit.MINUTES);
        Instant reqEnd = reqStart.plus(2, ChronoUnit.HOURS);

        mockMvc.perform(get("/api/v1/bookings/availability")
                        .param("vehicleId", testVehicle.getId().toString())
                        .param("startTime", reqStart.toString())
                        .param("endTime", reqEnd.toString())
                        .with(user(toPrincipal(memberCoOwner))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.isAvailable", is(true)))
                .andExpect(jsonPath("$.data.conflictingBookings", hasSize(0)));
    }

    @Test
    @DisplayName("5. Cancelled & Rejected Bookings: Do not cause availability conflicts")
    void testCancelledAndRejectedBookingsDoNotConflict() throws Exception {
        Instant bookedStart1 = Instant.now().plus(5, ChronoUnit.DAYS).truncatedTo(ChronoUnit.HOURS);
        Instant bookedEnd1 = bookedStart1.plus(3, ChronoUnit.HOURS);

        Instant bookedStart2 = bookedEnd1.plus(1, ChronoUnit.HOURS);
        Instant bookedEnd2 = bookedStart2.plus(2, ChronoUnit.HOURS);

        // Cancelled booking
        bookingRepository.saveAndFlush(
                Booking.builder()
                        .vehicle(testVehicle)
                        .user(memberCoOwner)
                        .startTime(bookedStart1)
                        .endTime(bookedEnd1)
                        .status(BookingStatus.CANCELLED)
                        .build()
        );

        // Rejected booking
        bookingRepository.saveAndFlush(
                Booking.builder()
                        .vehicle(testVehicle)
                        .user(memberCoOwner)
                        .startTime(bookedStart2)
                        .endTime(bookedEnd2)
                        .status(BookingStatus.REJECTED)
                        .build()
        );

        // Request exact same time as cancelled booking
        mockMvc.perform(get("/api/v1/bookings/availability")
                        .param("vehicleId", testVehicle.getId().toString())
                        .param("startTime", bookedStart1.toString())
                        .param("endTime", bookedEnd1.toString())
                        .with(user(toPrincipal(memberCoOwner))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.isAvailable", is(true)))
                .andExpect(jsonPath("$.data.conflictingBookings", hasSize(0)));

        // Request exact same time as rejected booking
        mockMvc.perform(get("/api/v1/bookings/availability")
                        .param("vehicleId", testVehicle.getId().toString())
                        .param("startTime", bookedStart2.toString())
                        .param("endTime", bookedEnd2.toString())
                        .with(user(toPrincipal(memberCoOwner))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.isAvailable", is(true)))
                .andExpect(jsonPath("$.data.conflictingBookings", hasSize(0)));
    }

    @Test
    @DisplayName("6. Vehicle Status Awareness: Rejects booking if vehicle is MAINTENANCE, DAMAGED, or UNAVAILABLE")
    void testVehicleStatusAwareness() throws Exception {
        Instant start = Instant.now().plus(6, ChronoUnit.DAYS).truncatedTo(ChronoUnit.HOURS);
        Instant end = start.plus(2, ChronoUnit.HOURS);

        // Test MAINTENANCE
        testVehicle.setStatus(VehicleStatus.MAINTENANCE);
        vehicleRepository.saveAndFlush(testVehicle);

        mockMvc.perform(get("/api/v1/bookings/availability")
                        .param("vehicleId", testVehicle.getId().toString())
                        .param("startTime", start.toString())
                        .param("endTime", end.toString())
                        .with(user(toPrincipal(memberCoOwner))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.isAvailable", is(false)))
                .andExpect(jsonPath("$.data.reason", containsString("maintenance")));

        // Test DAMAGED
        testVehicle.setStatus(VehicleStatus.DAMAGED);
        vehicleRepository.saveAndFlush(testVehicle);

        mockMvc.perform(get("/api/v1/bookings/availability")
                        .param("vehicleId", testVehicle.getId().toString())
                        .param("startTime", start.toString())
                        .param("endTime", end.toString())
                        .with(user(toPrincipal(memberCoOwner))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.isAvailable", is(false)))
                .andExpect(jsonPath("$.data.reason", containsString("damaged")));

        // Test UNAVAILABLE
        testVehicle.setStatus(VehicleStatus.UNAVAILABLE);
        vehicleRepository.saveAndFlush(testVehicle);

        mockMvc.perform(get("/api/v1/bookings/availability")
                        .param("vehicleId", testVehicle.getId().toString())
                        .param("startTime", start.toString())
                        .param("endTime", end.toString())
                        .with(user(toPrincipal(memberCoOwner))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.isAvailable", is(false)))
                .andExpect(jsonPath("$.data.reason", containsString("unavailable")));
    }

    @Test
    @DisplayName("7. Business Rule Constraints: Rejects past dates, <30m duration, >72h duration, >30d advance")
    void testBusinessRuleConstraints() throws Exception {
        Instant now = Instant.now().truncatedTo(ChronoUnit.HOURS);

        // In past
        mockMvc.perform(get("/api/v1/bookings/availability")
                        .param("vehicleId", testVehicle.getId().toString())
                        .param("startTime", now.minus(2, ChronoUnit.HOURS).toString())
                        .param("endTime", now.plus(1, ChronoUnit.HOURS).toString())
                        .with(user(toPrincipal(memberCoOwner))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.isAvailable", is(false)))
                .andExpect(jsonPath("$.data.reason", containsString("past")));

        // < 30 minutes duration
        Instant future1 = now.plus(1, ChronoUnit.DAYS);
        mockMvc.perform(get("/api/v1/bookings/availability")
                        .param("vehicleId", testVehicle.getId().toString())
                        .param("startTime", future1.toString())
                        .param("endTime", future1.plus(20, ChronoUnit.MINUTES).toString())
                        .with(user(toPrincipal(memberCoOwner))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.isAvailable", is(false)))
                .andExpect(jsonPath("$.data.reason", containsString("Minimum booking duration is 30 minutes")));

        // > 72 hours duration
        mockMvc.perform(get("/api/v1/bookings/availability")
                        .param("vehicleId", testVehicle.getId().toString())
                        .param("startTime", future1.toString())
                        .param("endTime", future1.plus(73, ChronoUnit.HOURS).toString())
                        .with(user(toPrincipal(memberCoOwner))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.isAvailable", is(false)))
                .andExpect(jsonPath("$.data.reason", containsString("Maximum continuous booking duration is 72 hours")));

        // > 30 days in advance
        Instant future35 = now.plus(35, ChronoUnit.DAYS);
        mockMvc.perform(get("/api/v1/bookings/availability")
                        .param("vehicleId", testVehicle.getId().toString())
                        .param("startTime", future35.toString())
                        .param("endTime", future35.plus(2, ChronoUnit.HOURS).toString())
                        .with(user(toPrincipal(memberCoOwner))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.isAvailable", is(false)))
                .andExpect(jsonPath("$.data.reason", containsString("30 days in advance")));
    }

    @Test
    @DisplayName("8. 3D Timeline Query: Returns chronological active booking slots excluding cancelled")
    void testGetTimeline() throws Exception {
        Instant base = Instant.now().plus(7, ChronoUnit.DAYS).truncatedTo(ChronoUnit.HOURS);
        Instant b1Start = base.plus(1, ChronoUnit.HOURS);
        Instant b1End = b1Start.plus(2, ChronoUnit.HOURS);

        Instant b2Start = b1End.plus(1, ChronoUnit.HOURS);
        Instant b2End = b2Start.plus(2, ChronoUnit.HOURS);

        Instant b3Start = b2End.plus(1, ChronoUnit.HOURS);
        Instant b3End = b3Start.plus(2, ChronoUnit.HOURS);

        // Booking 1: Confirmed by member
        bookingRepository.saveAndFlush(
                Booking.builder().vehicle(testVehicle).user(memberCoOwner).startTime(b1Start).endTime(b1End).status(BookingStatus.CONFIRMED).build()
        );
        // Booking 2: Cancelled (should be excluded)
        bookingRepository.saveAndFlush(
                Booking.builder().vehicle(testVehicle).user(memberCoOwner).startTime(b2Start).endTime(b2End).status(BookingStatus.CANCELLED).build()
        );
        // Booking 3: Confirmed by staff/other
        bookingRepository.saveAndFlush(
                Booking.builder().vehicle(testVehicle).user(staffUser).startTime(b3Start).endTime(b3End).status(BookingStatus.CONFIRMED).build()
        );

        Instant queryFrom = base;
        Instant queryTo = base.plus(24, ChronoUnit.HOURS);

        mockMvc.perform(get("/api/v1/bookings/timeline")
                        .param("vehicleId", testVehicle.getId().toString())
                        .param("from", queryFrom.toString())
                        .param("to", queryTo.toString())
                        .with(user(toPrincipal(memberCoOwner))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data", hasSize(2))) // Only b1 and b3
                .andExpect(jsonPath("$.data[0].isMyBooking", is(true)))
                .andExpect(jsonPath("$.data[1].isMyBooking", is(false)));
    }

    @Test
    @DisplayName("9. Authorization: Non-member Co-Owner receives 403 Forbidden; Staff and Admin receive 200 OK")
    void testAuthorization() throws Exception {
        Instant start = Instant.now().plus(8, ChronoUnit.DAYS).truncatedTo(ChronoUnit.HOURS);
        Instant end = start.plus(2, ChronoUnit.HOURS);

        // Non-member co-owner -> 403 Forbidden
        mockMvc.perform(get("/api/v1/bookings/availability")
                        .param("vehicleId", testVehicle.getId().toString())
                        .param("startTime", start.toString())
                        .param("endTime", end.toString())
                        .with(user(toPrincipal(nonMemberCoOwner))))
                .andExpect(status().isForbidden());

        // Member co-owner -> 200 OK
        mockMvc.perform(get("/api/v1/bookings/availability")
                        .param("vehicleId", testVehicle.getId().toString())
                        .param("startTime", start.toString())
                        .param("endTime", end.toString())
                        .with(user(toPrincipal(memberCoOwner))))
                .andExpect(status().isOk());

        // Staff -> 200 OK
        mockMvc.perform(get("/api/v1/bookings/availability")
                        .param("vehicleId", testVehicle.getId().toString())
                        .param("startTime", start.toString())
                        .param("endTime", end.toString())
                        .with(user(toPrincipal(staffUser))))
                .andExpect(status().isOk());

        // Admin -> 200 OK
        mockMvc.perform(get("/api/v1/bookings/availability")
                        .param("vehicleId", testVehicle.getId().toString())
                        .param("startTime", start.toString())
                        .param("endTime", end.toString())
                        .with(user(toPrincipal(adminUser))))
                .andExpect(status().isOk());

        // Unauthenticated -> 401 Unauthorized
        mockMvc.perform(get("/api/v1/bookings/availability")
                        .param("vehicleId", testVehicle.getId().toString())
                        .param("startTime", start.toString())
                        .param("endTime", end.toString()))
                .andExpect(status().isUnauthorized());
    }
}
