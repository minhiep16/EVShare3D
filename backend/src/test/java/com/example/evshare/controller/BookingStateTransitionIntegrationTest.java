package com.example.evshare.controller;

import com.example.evshare.dto.request.UpdateBookingStatusRequest;
import com.example.evshare.entity.*;
import com.example.evshare.entity.enums.BookingStatus;
import com.example.evshare.entity.enums.RoleName;
import com.example.evshare.entity.enums.VehicleStatus;
import com.example.evshare.repository.*;
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
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@DisplayName("Checkpoint 05-G — Booking State Machine Integration Tests")
class BookingStateTransitionIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

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

    private User memberCoOwner;
    private User otherMemberCoOwner;
    private User adminUser;
    private Vehicle testVehicle;
    private OwnershipGroup testGroup;

    @BeforeEach
    void setUp() {
        Role roleCoOwner = roleRepository.findByName(RoleName.ROLE_CO_OWNER)
                .orElseGet(() -> roleRepository.save(new Role(null, RoleName.ROLE_CO_OWNER)));
        Role roleAdmin = roleRepository.findByName(RoleName.ROLE_ADMIN)
                .orElseGet(() -> roleRepository.save(new Role(null, RoleName.ROLE_ADMIN)));

        memberCoOwner = createTestUser("fsm.member1." + UUID.randomUUID() + "@evshare.io", "Member One", roleCoOwner);
        otherMemberCoOwner = createTestUser("fsm.member2." + UUID.randomUUID() + "@evshare.io", "Member Two", roleCoOwner);
        adminUser = createTestUser("fsm.admin." + UUID.randomUUID() + "@evshare.io", "Platform Administrator", roleAdmin);

        testVehicle = new Vehicle();
        testVehicle.setVin("VIN" + UUID.randomUUID().toString().replace("-", "").substring(0, 14).toUpperCase());
        testVehicle.setLicensePlate("51L-" + (int) (10000 + Math.random() * 89999));
        testVehicle.setModelName("Porsche Taycan Cross Turismo");
        testVehicle.setManufacturer("Porsche");
        testVehicle.setModel3dAssetPath("models/vehicles/models.glb");
        testVehicle.setStatus(VehicleStatus.AVAILABLE);
        testVehicle.setBatteryLevel(95);
        testVehicle.setOdometerKm(new BigDecimal("6200.00"));
        testVehicle.setStallLocationCode("BAY-PORSCHE-01");
        testVehicle = vehicleRepository.saveAndFlush(testVehicle);

        testGroup = new OwnershipGroup();
        testGroup.setGroupName("Taycan Syndicate " + UUID.randomUUID().toString().substring(0, 8));
        testGroup.setVehicle(testVehicle);
        testGroup.setFormationDate(LocalDate.now());
        testGroup.setIsActive(true);
        testGroup = ownershipGroupRepository.saveAndFlush(testGroup);

        OwnershipShare share1 = new OwnershipShare();
        share1.setGroup(testGroup);
        share1.setUser(memberCoOwner);
        share1.setPercentage(new BigDecimal("70.00"));
        share1.setShareCertificateNumber("CERT-" + UUID.randomUUID().toString().substring(0, 8));
        share1.setIsActive(true);
        ownershipShareRepository.saveAndFlush(share1);

        OwnershipShare share2 = new OwnershipShare();
        share2.setGroup(testGroup);
        share2.setUser(otherMemberCoOwner);
        share2.setPercentage(new BigDecimal("30.00"));
        share2.setShareCertificateNumber("CERT-" + UUID.randomUUID().toString().substring(0, 8));
        share2.setIsActive(true);
        ownershipShareRepository.saveAndFlush(share2);
    }

    private User createTestUser(String email, String fullName, Role role) {
        User u = new User();
        u.setEmail(email);
        u.setPasswordHash("$2a$12$dummyHashForBookingFsmTesting1234567890");
        u.setFullName(fullName);
        u.setIsActive(true);
        u.getRoles().add(role);
        return userRepository.saveAndFlush(u);
    }

    private UserPrincipal toPrincipal(User u) {
        return UserPrincipal.create(u);
    }

    private Booking createTestBooking(User user, Instant start, Instant end, BookingStatus status) {
        Booking b = Booking.builder()
                .vehicle(testVehicle)
                .user(user)
                .startTime(start)
                .endTime(end)
                .status(status)
                .estimatedCost(new BigDecimal("120000.00"))
                .createdAt(Instant.now())
                .build();
        return bookingRepository.saveAndFlush(b);
    }

    // =========================================================================
    // VALID TRANSITIONS
    // =========================================================================

    @Test
    @DisplayName("1. Valid Transition: PENDING -> APPROVED (200 OK)")
    void testTransition_PendingToApproved() throws Exception {
        Instant start = Instant.now().plus(24, ChronoUnit.HOURS);
        Booking booking = createTestBooking(memberCoOwner, start, start.plus(2, ChronoUnit.HOURS), BookingStatus.PENDING);

        UpdateBookingStatusRequest req = new UpdateBookingStatusRequest(BookingStatus.APPROVED, "Syndicate consensus reached");

        mockMvc.perform(patch("/api/v1/bookings/" + booking.getId() + "/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req))
                        .with(user(toPrincipal(adminUser))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.status", is("APPROVED")));
    }

    @Test
    @DisplayName("2. Valid Transition: APPROVED -> CONFIRMED (200 OK)")
    void testTransition_ApprovedToConfirmed() throws Exception {
        Instant start = Instant.now().plus(24, ChronoUnit.HOURS);
        Booking booking = createTestBooking(memberCoOwner, start, start.plus(2, ChronoUnit.HOURS), BookingStatus.APPROVED);

        UpdateBookingStatusRequest req = new UpdateBookingStatusRequest(BookingStatus.CONFIRMED, "Deposit locked, calendar scheduled");

        mockMvc.perform(patch("/api/v1/bookings/" + booking.getId() + "/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req))
                        .with(user(toPrincipal(adminUser))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.status", is("CONFIRMED")));
    }

    @Test
    @DisplayName("3. Valid Transition: CONFIRMED -> IN_USE on check-in (200 OK)")
    void testTransition_ConfirmedToInUse() throws Exception {
        Instant start = Instant.now().plus(1, ChronoUnit.HOURS);
        Booking booking = createTestBooking(memberCoOwner, start, start.plus(2, ChronoUnit.HOURS), BookingStatus.CONFIRMED);

        UpdateBookingStatusRequest req = new UpdateBookingStatusRequest(BookingStatus.IN_USE, "QR check-in authenticated, trip commenced");

        mockMvc.perform(patch("/api/v1/bookings/" + booking.getId() + "/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req))
                        .with(user(toPrincipal(memberCoOwner))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.status", is("IN_USE")));
    }

    @Test
    @DisplayName("4. Valid Transition: IN_USE -> COMPLETED on check-out (200 OK)")
    void testTransition_InUseToCompleted() throws Exception {
        Instant start = Instant.now().minus(2, ChronoUnit.HOURS);
        Booking booking = createTestBooking(memberCoOwner, start, start.plus(3, ChronoUnit.HOURS), BookingStatus.IN_USE);

        UpdateBookingStatusRequest req = new UpdateBookingStatusRequest(BookingStatus.COMPLETED, "Check-out completed, vehicle returned");

        mockMvc.perform(patch("/api/v1/bookings/" + booking.getId() + "/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req))
                        .with(user(toPrincipal(memberCoOwner))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.status", is("COMPLETED")));
    }

    @Test
    @DisplayName("5. Valid Transition: CONFIRMED -> NO_SHOW (200 OK)")
    void testTransition_ConfirmedToNoShow() throws Exception {
        Instant start = Instant.now().minus(45, ChronoUnit.MINUTES);
        Booking booking = createTestBooking(memberCoOwner, start, start.plus(2, ChronoUnit.HOURS), BookingStatus.CONFIRMED);

        UpdateBookingStatusRequest req = new UpdateBookingStatusRequest(BookingStatus.NO_SHOW, "30-min check-in window elapsed without arrival");

        mockMvc.perform(patch("/api/v1/bookings/" + booking.getId() + "/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req))
                        .with(user(toPrincipal(adminUser))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.status", is("NO_SHOW")));
    }

    @Test
    @DisplayName("6. Valid Transition: CONFIRMED -> CANCELLED (200 OK)")
    void testTransition_ConfirmedToCancelled() throws Exception {
        Instant start = Instant.now().plus(20, ChronoUnit.HOURS);
        Booking booking = createTestBooking(memberCoOwner, start, start.plus(2, ChronoUnit.HOURS), BookingStatus.CONFIRMED);

        UpdateBookingStatusRequest req = new UpdateBookingStatusRequest(BookingStatus.CANCELLED, "Co-owner cancelled reservation");

        mockMvc.perform(patch("/api/v1/bookings/" + booking.getId() + "/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req))
                        .with(user(toPrincipal(memberCoOwner))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.status", is("CANCELLED")));
    }

    @Test
    @DisplayName("7. Valid Transition: PENDING -> REJECTED (200 OK)")
    void testTransition_PendingToRejected() throws Exception {
        Instant start = Instant.now().plus(24, ChronoUnit.HOURS);
        Booking booking = createTestBooking(memberCoOwner, start, start.plus(2, ChronoUnit.HOURS), BookingStatus.PENDING);

        UpdateBookingStatusRequest req = new UpdateBookingStatusRequest(BookingStatus.REJECTED, "Booking quota limit exceeded");

        mockMvc.perform(patch("/api/v1/bookings/" + booking.getId() + "/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req))
                        .with(user(toPrincipal(adminUser))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.status", is("REJECTED")));
    }

    @Test
    @DisplayName("8. Valid Transition: PENDING -> CONFIRMED (direct instant reservation) (200 OK)")
    void testTransition_PendingToConfirmed() throws Exception {
        Instant start = Instant.now().plus(24, ChronoUnit.HOURS);
        Booking booking = createTestBooking(memberCoOwner, start, start.plus(2, ChronoUnit.HOURS), BookingStatus.PENDING);

        UpdateBookingStatusRequest req = new UpdateBookingStatusRequest(BookingStatus.CONFIRMED, "Instant confirmation pass");

        mockMvc.perform(patch("/api/v1/bookings/" + booking.getId() + "/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req))
                        .with(user(toPrincipal(adminUser))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.status", is("CONFIRMED")));
    }

    // =========================================================================
    // INVALID TRANSITIONS
    // =========================================================================

    @Test
    @DisplayName("9. Invalid Transition: COMPLETED -> IN_USE is rejected (409 Conflict)")
    void testTransition_CompletedToInUse_Conflict() throws Exception {
        Instant start = Instant.now().minus(3, ChronoUnit.DAYS);
        Booking booking = createTestBooking(memberCoOwner, start, start.plus(2, ChronoUnit.HOURS), BookingStatus.COMPLETED);

        UpdateBookingStatusRequest req = new UpdateBookingStatusRequest(BookingStatus.IN_USE, "Attempting to restart completed trip");

        mockMvc.perform(patch("/api/v1/bookings/" + booking.getId() + "/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req))
                        .with(user(toPrincipal(adminUser))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message", containsString("Cannot transition booking from status 'COMPLETED' to 'IN_USE'")));
    }

    @Test
    @DisplayName("10. Invalid Transition: CANCELLED -> CONFIRMED is rejected (409 Conflict)")
    void testTransition_CancelledToConfirmed_Conflict() throws Exception {
        Instant start = Instant.now().plus(24, ChronoUnit.HOURS);
        Booking booking = createTestBooking(memberCoOwner, start, start.plus(2, ChronoUnit.HOURS), BookingStatus.CANCELLED);

        UpdateBookingStatusRequest req = new UpdateBookingStatusRequest(BookingStatus.CONFIRMED, "Attempting to reactivate cancelled reservation");

        mockMvc.perform(patch("/api/v1/bookings/" + booking.getId() + "/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req))
                        .with(user(toPrincipal(adminUser))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message", containsString("Cannot transition booking from status 'CANCELLED' to 'CONFIRMED'")));
    }

    @Test
    @DisplayName("11. Invalid Transition: IN_USE -> CANCELLED is rejected (409 Conflict)")
    void testTransition_InUseToCancelled_Conflict() throws Exception {
        Instant start = Instant.now().minus(1, ChronoUnit.HOURS);
        Booking booking = createTestBooking(memberCoOwner, start, start.plus(2, ChronoUnit.HOURS), BookingStatus.IN_USE);

        UpdateBookingStatusRequest req = new UpdateBookingStatusRequest(BookingStatus.CANCELLED, "Cannot cancel ongoing drive");

        mockMvc.perform(patch("/api/v1/bookings/" + booking.getId() + "/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req))
                        .with(user(toPrincipal(memberCoOwner))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message", containsString("Cannot transition booking from status 'IN_USE' to 'CANCELLED'")));
    }

    @Test
    @DisplayName("12. Invalid Transition: PENDING -> IN_USE skipping CONFIRMED is rejected (409 Conflict)")
    void testTransition_PendingToInUse_Conflict() throws Exception {
        Instant start = Instant.now().plus(1, ChronoUnit.HOURS);
        Booking booking = createTestBooking(memberCoOwner, start, start.plus(2, ChronoUnit.HOURS), BookingStatus.PENDING);

        UpdateBookingStatusRequest req = new UpdateBookingStatusRequest(BookingStatus.IN_USE, "Cannot check-in unconfirmed booking");

        mockMvc.perform(patch("/api/v1/bookings/" + booking.getId() + "/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req))
                        .with(user(toPrincipal(adminUser))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message", containsString("Cannot transition booking from status 'PENDING' to 'IN_USE'")));
    }

    @Test
    @DisplayName("13. Invalid Transition: CONFIRMED -> COMPLETED skipping IN_USE is rejected (409 Conflict)")
    void testTransition_ConfirmedToCompleted_Conflict() throws Exception {
        Instant start = Instant.now().plus(1, ChronoUnit.HOURS);
        Booking booking = createTestBooking(memberCoOwner, start, start.plus(2, ChronoUnit.HOURS), BookingStatus.CONFIRMED);

        UpdateBookingStatusRequest req = new UpdateBookingStatusRequest(BookingStatus.COMPLETED, "Cannot checkout without check-in");

        mockMvc.perform(patch("/api/v1/bookings/" + booking.getId() + "/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req))
                        .with(user(toPrincipal(adminUser))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message", containsString("Cannot transition booking from status 'CONFIRMED' to 'COMPLETED'")));
    }

    @Test
    @DisplayName("14. Redundant Self-Transition: CONFIRMED -> CONFIRMED is rejected (409 Conflict)")
    void testTransition_ConfirmedToConfirmed_Conflict() throws Exception {
        Instant start = Instant.now().plus(10, ChronoUnit.HOURS);
        Booking booking = createTestBooking(memberCoOwner, start, start.plus(2, ChronoUnit.HOURS), BookingStatus.CONFIRMED);

        UpdateBookingStatusRequest req = new UpdateBookingStatusRequest(BookingStatus.CONFIRMED, "Redundant transition");

        mockMvc.perform(patch("/api/v1/bookings/" + booking.getId() + "/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req))
                        .with(user(toPrincipal(adminUser))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message", containsString("already in status 'CONFIRMED'")));
    }

    @Test
    @DisplayName("15. Invalid Transition: REJECTED -> APPROVED is rejected (409 Conflict)")
    void testTransition_RejectedToApproved_Conflict() throws Exception {
        Instant start = Instant.now().plus(24, ChronoUnit.HOURS);
        Booking booking = createTestBooking(memberCoOwner, start, start.plus(2, ChronoUnit.HOURS), BookingStatus.REJECTED);

        UpdateBookingStatusRequest req = new UpdateBookingStatusRequest(BookingStatus.APPROVED, "Cannot revive rejected booking");

        mockMvc.perform(patch("/api/v1/bookings/" + booking.getId() + "/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req))
                        .with(user(toPrincipal(adminUser))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message", containsString("Cannot transition booking from status 'REJECTED' to 'APPROVED'")));
    }

    @Test
    @DisplayName("16. Invalid Transition: NO_SHOW -> IN_USE is rejected (409 Conflict)")
    void testTransition_NoShowToInUse_Conflict() throws Exception {
        Instant start = Instant.now().minus(1, ChronoUnit.HOURS);
        Booking booking = createTestBooking(memberCoOwner, start, start.plus(2, ChronoUnit.HOURS), BookingStatus.NO_SHOW);

        UpdateBookingStatusRequest req = new UpdateBookingStatusRequest(BookingStatus.IN_USE, "Cannot check-in after no-show marked");

        mockMvc.perform(patch("/api/v1/bookings/" + booking.getId() + "/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req))
                        .with(user(toPrincipal(adminUser))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message", containsString("Cannot transition booking from status 'NO_SHOW' to 'IN_USE'")));
    }

    // =========================================================================
    // AUTHORIZATION & AUDIT TRAIL
    // =========================================================================

    @Test
    @DisplayName("17. Authorization: Non-owner Co-owner receives 403 Forbidden")
    void testTransition_NonOwnerForbidden() throws Exception {
        Instant start = Instant.now().plus(24, ChronoUnit.HOURS);
        Booking booking = createTestBooking(memberCoOwner, start, start.plus(2, ChronoUnit.HOURS), BookingStatus.PENDING);

        UpdateBookingStatusRequest req = new UpdateBookingStatusRequest(BookingStatus.CONFIRMED, "Unauthorized attempt");

        mockMvc.perform(patch("/api/v1/bookings/" + booking.getId() + "/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req))
                        .with(user(toPrincipal(otherMemberCoOwner))))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("18. Audit Trail: TRANSITION_BOOKING_STATUS is recorded in audit_logs")
    void testTransition_AuditLogRecorded() throws Exception {
        Instant start = Instant.now().plus(24, ChronoUnit.HOURS);
        Booking booking = createTestBooking(memberCoOwner, start, start.plus(2, ChronoUnit.HOURS), BookingStatus.PENDING);

        UpdateBookingStatusRequest req = new UpdateBookingStatusRequest(BookingStatus.APPROVED, "Consensus reached by syndicate");

        mockMvc.perform(patch("/api/v1/bookings/" + booking.getId() + "/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req))
                        .with(user(toPrincipal(adminUser))))
                .andExpect(status().isOk());

        List<AuditLog> logs = auditLogRepository.findByEntityNameAndEntityIdOrderByIdDesc("Booking", booking.getId());
        assertFalse(logs.isEmpty(), "Audit log should be recorded for state transition");
        AuditLog latest = logs.get(0);
        assertEquals("TRANSITION_BOOKING_STATUS", latest.getAction());
        assertEquals("Booking", latest.getEntityName());
        assertEquals(booking.getId(), latest.getEntityId());
        assertEquals(adminUser.getId(), latest.getUser().getId());
        org.junit.jupiter.api.Assertions.assertTrue(latest.getOldStateJson().contains("\"status\":\"PENDING\""));
        org.junit.jupiter.api.Assertions.assertTrue(latest.getNewStateJson().contains("\"status\":\"APPROVED\""));
    }
}
