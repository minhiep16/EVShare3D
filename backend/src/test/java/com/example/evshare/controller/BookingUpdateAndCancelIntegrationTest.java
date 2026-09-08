package com.example.evshare.controller;

import com.example.evshare.dto.request.CancelBookingRequest;
import com.example.evshare.dto.request.UpdateBookingRequest;
import com.example.evshare.entity.*;
import com.example.evshare.entity.enums.*;
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
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@DisplayName("Checkpoint 05-E — Update, Cancel & Booking History Integration Tests")
class BookingUpdateAndCancelIntegrationTest {

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
    private User nonMemberCoOwner;
    private User adminUser;
    private Vehicle testVehicle;
    private OwnershipGroup testGroup;

    @BeforeEach
    void setUp() {
        Role roleCoOwner = roleRepository.findByName(RoleName.ROLE_CO_OWNER)
                .orElseGet(() -> roleRepository.save(new Role(null, RoleName.ROLE_CO_OWNER)));
        Role roleAdmin = roleRepository.findByName(RoleName.ROLE_ADMIN)
                .orElseGet(() -> roleRepository.save(new Role(null, RoleName.ROLE_ADMIN)));

        memberCoOwner = createTestUser("update.member1." + UUID.randomUUID() + "@evshare.io", "Member One", roleCoOwner);
        otherMemberCoOwner = createTestUser("update.member2." + UUID.randomUUID() + "@evshare.io", "Member Two", roleCoOwner);
        nonMemberCoOwner = createTestUser("update.nonmember." + UUID.randomUUID() + "@evshare.io", "Non-Member Co-Owner", roleCoOwner);
        adminUser = createTestUser("update.admin." + UUID.randomUUID() + "@evshare.io", "Platform Administrator", roleAdmin);

        testVehicle = new Vehicle();
        testVehicle.setVin("VIN" + UUID.randomUUID().toString().replace("-", "").substring(0, 14).toUpperCase());
        testVehicle.setLicensePlate("51L-" + (int) (10000 + Math.random() * 89999));
        testVehicle.setModelName("Tesla Model S Plaid");
        testVehicle.setManufacturer("Tesla");
        testVehicle.setModel3dAssetPath("models/vehicles/models.glb");
        testVehicle.setStatus(VehicleStatus.AVAILABLE);
        testVehicle.setBatteryLevel(92);
        testVehicle.setOdometerKm(new BigDecimal("8500.00"));
        testVehicle.setStallLocationCode("BAY-TESLA-01");
        testVehicle = vehicleRepository.saveAndFlush(testVehicle);

        testGroup = new OwnershipGroup();
        testGroup.setGroupName("Model S Syndicate " + UUID.randomUUID().toString().substring(0, 8));
        testGroup.setVehicle(testVehicle);
        testGroup.setFormationDate(LocalDate.now());
        testGroup.setIsActive(true);
        testGroup = ownershipGroupRepository.saveAndFlush(testGroup);

        OwnershipShare share1 = new OwnershipShare();
        share1.setGroup(testGroup);
        share1.setUser(memberCoOwner);
        share1.setPercentage(new BigDecimal("60.00"));
        share1.setShareCertificateNumber("CERT-" + UUID.randomUUID().toString().substring(0, 8));
        share1.setIsActive(true);
        ownershipShareRepository.saveAndFlush(share1);

        OwnershipShare share2 = new OwnershipShare();
        share2.setGroup(testGroup);
        share2.setUser(otherMemberCoOwner);
        share2.setPercentage(new BigDecimal("40.00"));
        share2.setShareCertificateNumber("CERT-" + UUID.randomUUID().toString().substring(0, 8));
        share2.setIsActive(true);
        ownershipShareRepository.saveAndFlush(share2);
    }

    private User createTestUser(String email, String fullName, Role role) {
        User u = new User();
        u.setEmail(email);
        u.setPasswordHash("$2a$12$dummyHashForBookingUpdateCancelTesting12345");
        u.setFullName(fullName);
        u.setIsActive(true);
        u.getRoles().add(role);
        return userRepository.saveAndFlush(u);
    }

    private UserPrincipal toPrincipal(User u) {
        return UserPrincipal.create(u);
    }

    private Booking createTestBooking(User user, Instant start, Instant end, BookingStatus status, BigDecimal cost) {
        Booking b = Booking.builder()
                .vehicle(testVehicle)
                .user(user)
                .startTime(start)
                .endTime(end)
                .status(status)
                .estimatedCost(cost)
                .createdAt(Instant.now())
                .build();
        return bookingRepository.saveAndFlush(b);
    }

    @Test
    @DisplayName("1. Update Booking: Owner successfully reschedules future slot (200 OK)")
    void testUpdateBookingSuccess() throws Exception {
        Instant origStart = Instant.now().plus(3, ChronoUnit.DAYS).truncatedTo(ChronoUnit.HOURS);
        Instant origEnd = origStart.plus(2, ChronoUnit.HOURS);
        Booking booking = createTestBooking(memberCoOwner, origStart, origEnd, BookingStatus.CONFIRMED, new BigDecimal("100000.00"));

        Instant newStart = origStart.plus(5, ChronoUnit.HOURS);
        Instant newEnd = newStart.plus(3, ChronoUnit.HOURS);

        UpdateBookingRequest req = new UpdateBookingRequest(newStart, newEnd);

        mockMvc.perform(put("/api/v1/bookings/" + booking.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req))
                        .with(user(toPrincipal(memberCoOwner))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.id", is(booking.getId().intValue())))
                .andExpect(jsonPath("$.data.status", is("CONFIRMED")))
                .andExpect(jsonPath("$.data.estimatedCost", notNullValue()));
    }

    @Test
    @DisplayName("2. Update Booking: Non-owner Co-owner receives 403 Forbidden")
    void testUpdateBooking_NonOwnerForbidden() throws Exception {
        Instant origStart = Instant.now().plus(3, ChronoUnit.DAYS).truncatedTo(ChronoUnit.HOURS);
        Instant origEnd = origStart.plus(2, ChronoUnit.HOURS);
        Booking booking = createTestBooking(memberCoOwner, origStart, origEnd, BookingStatus.CONFIRMED, new BigDecimal("100000.00"));

        UpdateBookingRequest req = new UpdateBookingRequest(origStart.plus(1, ChronoUnit.HOURS), origEnd.plus(1, ChronoUnit.HOURS));

        mockMvc.perform(put("/api/v1/bookings/" + booking.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req))
                        .with(user(toPrincipal(otherMemberCoOwner))))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("3. Update Booking: Admin can reschedule any reservation (200 OK)")
    void testUpdateBooking_AdminCanUpdate() throws Exception {
        Instant origStart = Instant.now().plus(3, ChronoUnit.DAYS).truncatedTo(ChronoUnit.HOURS);
        Instant origEnd = origStart.plus(2, ChronoUnit.HOURS);
        Booking booking = createTestBooking(memberCoOwner, origStart, origEnd, BookingStatus.CONFIRMED, new BigDecimal("100000.00"));

        UpdateBookingRequest req = new UpdateBookingRequest(origStart.plus(2, ChronoUnit.HOURS), origEnd.plus(2, ChronoUnit.HOURS));

        mockMvc.perform(put("/api/v1/bookings/" + booking.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req))
                        .with(user(toPrincipal(adminUser))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)));
    }

    @Test
    @DisplayName("4. Update Invariant: Cannot modify completed historical booking (400 Bad Request)")
    void testUpdateBooking_CannotModifyCompleted() throws Exception {
        Instant origStart = Instant.now().minus(2, ChronoUnit.DAYS).truncatedTo(ChronoUnit.HOURS);
        Instant origEnd = origStart.plus(2, ChronoUnit.HOURS);
        Booking booking = createTestBooking(memberCoOwner, origStart, origEnd, BookingStatus.COMPLETED, new BigDecimal("100000.00"));

        Instant newStart = Instant.now().plus(1, ChronoUnit.DAYS).truncatedTo(ChronoUnit.HOURS);
        Instant newEnd = newStart.plus(2, ChronoUnit.HOURS);
        UpdateBookingRequest req = new UpdateBookingRequest(newStart, newEnd);

        mockMvc.perform(put("/api/v1/bookings/" + booking.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req))
                        .with(user(toPrincipal(memberCoOwner))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("Cannot modify completed historical booking")));
    }

    @Test
    @DisplayName("5. Update Invariant: Cannot modify cancelled booking (400 Bad Request)")
    void testUpdateBooking_CannotModifyCancelled() throws Exception {
        Instant origStart = Instant.now().plus(2, ChronoUnit.DAYS).truncatedTo(ChronoUnit.HOURS);
        Instant origEnd = origStart.plus(2, ChronoUnit.HOURS);
        Booking booking = createTestBooking(memberCoOwner, origStart, origEnd, BookingStatus.CANCELLED, new BigDecimal("100000.00"));

        UpdateBookingRequest req = new UpdateBookingRequest(origStart.plus(1, ChronoUnit.HOURS), origEnd.plus(1, ChronoUnit.HOURS));

        mockMvc.perform(put("/api/v1/bookings/" + booking.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req))
                        .with(user(toPrincipal(memberCoOwner))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("Cannot modify cancelled booking")));
    }

    @Test
    @DisplayName("6. Update Invariant: Cannot modify in-progress booking (400 Bad Request)")
    void testUpdateBooking_CannotModifyInUse() throws Exception {
        Instant origStart = Instant.now().minus(30, ChronoUnit.MINUTES);
        Instant origEnd = origStart.plus(2, ChronoUnit.HOURS);
        Booking booking = createTestBooking(memberCoOwner, origStart, origEnd, BookingStatus.IN_USE, new BigDecimal("100000.00"));

        UpdateBookingRequest req = new UpdateBookingRequest(Instant.now().plus(1, ChronoUnit.DAYS), Instant.now().plus(1, ChronoUnit.DAYS).plus(2, ChronoUnit.HOURS));

        mockMvc.perform(put("/api/v1/bookings/" + booking.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req))
                        .with(user(toPrincipal(memberCoOwner))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("Cannot modify booking currently in progress")));
    }

    @Test
    @DisplayName("7. Update Conflict: Rescheduling collides with another booking turnaround buffer (409 Conflict)")
    void testUpdateBooking_ConflictWithAnotherBooking() throws Exception {
        Instant start1 = Instant.now().plus(4, ChronoUnit.DAYS).truncatedTo(ChronoUnit.HOURS);
        Instant end1 = start1.plus(2, ChronoUnit.HOURS);
        Booking booking1 = createTestBooking(memberCoOwner, start1, end1, BookingStatus.CONFIRMED, new BigDecimal("100000.00"));

        Instant start2 = start1.plus(6, ChronoUnit.HOURS);
        Instant end2 = start2.plus(2, ChronoUnit.HOURS);
        createTestBooking(otherMemberCoOwner, start2, end2, BookingStatus.CONFIRMED, new BigDecimal("100000.00"));

        // Booking 1 tries to reschedule to end right before Booking 2 without 30-min buffer (ends 10m before start2)
        Instant newStart = start2.minus(2, ChronoUnit.HOURS).minus(10, ChronoUnit.MINUTES);
        Instant newEnd = start2.minus(10, ChronoUnit.MINUTES); // Inside 30 min buffer!

        UpdateBookingRequest req = new UpdateBookingRequest(newStart, newEnd);

        mockMvc.perform(put("/api/v1/bookings/" + booking1.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req))
                        .with(user(toPrincipal(memberCoOwner))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message", containsString("30-minute turnaround buffer")));
    }

    @Test
    @DisplayName("8. Free Cancellation: Cancel >= 12h before start incurs zero fee and zero penalty (200 OK)")
    void testCancelBooking_FreeCancellation() throws Exception {
        // Starts 24 hours in the future (>= 12h)
        Instant start = Instant.now().plus(24, ChronoUnit.HOURS).truncatedTo(ChronoUnit.HOURS);
        Instant end = start.plus(3, ChronoUnit.HOURS);
        Booking booking = createTestBooking(memberCoOwner, start, end, BookingStatus.CONFIRMED, new BigDecimal("150000.00"));

        CancelBookingRequest req = new CancelBookingRequest("Plans changed well ahead");

        mockMvc.perform(post("/api/v1/bookings/" + booking.getId() + "/cancel")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req))
                        .with(user(toPrincipal(memberCoOwner))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.bookingId", is(booking.getId().intValue())))
                .andExpect(jsonPath("$.data.status", is("CANCELLED")))
                .andExpect(jsonPath("$.data.cancellationFee", is(0.0)))
                .andExpect(jsonPath("$.data.isLateCancellation", is(false)))
                .andExpect(jsonPath("$.data.penaltyApplied", is(false)))
                .andExpect(jsonPath("$.data.cancellationReason", is("Plans changed well ahead")))
                .andExpect(jsonPath("$.data.message", containsString("zero penalty")));
    }

    @Test
    @DisplayName("9. Late Cancellation: Cancel < 12h before start incurs 20% fee and penalty per BR-BKG-03 (200 OK)")
    void testCancelBooking_LateCancellation() throws Exception {
        // Starts in 4 hours (< 12h)
        Instant start = Instant.now().plus(4, ChronoUnit.HOURS);
        Instant end = start.plus(3, ChronoUnit.HOURS);
        Booking booking = createTestBooking(memberCoOwner, start, end, BookingStatus.CONFIRMED, new BigDecimal("150000.00"));

        CancelBookingRequest req = new CancelBookingRequest("Emergency cancellation");

        // 20% of 150000.00 = 30000.00
        mockMvc.perform(post("/api/v1/bookings/" + booking.getId() + "/cancel")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req))
                        .with(user(toPrincipal(memberCoOwner))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.bookingId", is(booking.getId().intValue())))
                .andExpect(jsonPath("$.data.status", is("CANCELLED")))
                .andExpect(jsonPath("$.data.cancellationFee", is(30000.0)))
                .andExpect(jsonPath("$.data.isLateCancellation", is(true)))
                .andExpect(jsonPath("$.data.penaltyApplied", is(true)))
                .andExpect(jsonPath("$.data.message", containsString("20% reservation fee assessed")));
    }

    @Test
    @DisplayName("10. Cancel Authorization: Non-owner Co-owner receives 403 Forbidden")
    void testCancelBooking_NonOwnerForbidden() throws Exception {
        Instant start = Instant.now().plus(20, ChronoUnit.HOURS);
        Instant end = start.plus(2, ChronoUnit.HOURS);
        Booking booking = createTestBooking(memberCoOwner, start, end, BookingStatus.CONFIRMED, new BigDecimal("100000.00"));

        mockMvc.perform(post("/api/v1/bookings/" + booking.getId() + "/cancel")
                        .with(user(toPrincipal(otherMemberCoOwner))))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("11. Cancel Invariant: Cannot cancel already cancelled booking (400 Bad Request)")
    void testCancelBooking_AlreadyCancelled_BadRequest() throws Exception {
        Instant start = Instant.now().plus(20, ChronoUnit.HOURS);
        Instant end = start.plus(2, ChronoUnit.HOURS);
        Booking booking = createTestBooking(memberCoOwner, start, end, BookingStatus.CANCELLED, new BigDecimal("100000.00"));

        mockMvc.perform(post("/api/v1/bookings/" + booking.getId() + "/cancel")
                        .with(user(toPrincipal(memberCoOwner))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("already cancelled")));
    }

    @Test
    @DisplayName("12. Cancel Invariant: Cannot cancel completed booking (400 Bad Request)")
    void testCancelBooking_Completed_BadRequest() throws Exception {
        Instant start = Instant.now().minus(2, ChronoUnit.DAYS);
        Instant end = start.plus(2, ChronoUnit.HOURS);
        Booking booking = createTestBooking(memberCoOwner, start, end, BookingStatus.COMPLETED, new BigDecimal("100000.00"));

        mockMvc.perform(post("/api/v1/bookings/" + booking.getId() + "/cancel")
                        .with(user(toPrincipal(memberCoOwner))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("Cannot cancel a completed booking")));
    }

    @Test
    @DisplayName("13. Audit History: Audit trail records updates and cancellations (200 OK)")
    void testGetBookingHistory() throws Exception {
        Instant start = Instant.now().plus(30, ChronoUnit.HOURS);
        Instant end = start.plus(2, ChronoUnit.HOURS);
        Booking booking = createTestBooking(memberCoOwner, start, end, BookingStatus.CONFIRMED, new BigDecimal("100000.00"));

        // Trigger update
        UpdateBookingRequest updateReq = new UpdateBookingRequest(start.plus(1, ChronoUnit.HOURS), end.plus(1, ChronoUnit.HOURS));
        mockMvc.perform(put("/api/v1/bookings/" + booking.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateReq))
                        .with(user(toPrincipal(memberCoOwner))))
                .andExpect(status().isOk());

        // Trigger cancel
        mockMvc.perform(post("/api/v1/bookings/" + booking.getId() + "/cancel")
                        .with(user(toPrincipal(memberCoOwner))))
                .andExpect(status().isOk());

        // Fetch audit history
        mockMvc.perform(get("/api/v1/bookings/" + booking.getId() + "/history")
                        .with(user(toPrincipal(memberCoOwner))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data", hasSize(greaterThanOrEqualTo(2))))
                .andExpect(jsonPath("$.data[0].action", is("CANCEL_BOOKING")))
                .andExpect(jsonPath("$.data[1].action", is("UPDATE_BOOKING")));
    }

    @Test
    @DisplayName("14. User Reservations: Authenticated co-owner retrieves paged bookings (200 OK)")
    void testGetMyBookings() throws Exception {
        Instant start1 = Instant.now().plus(1, ChronoUnit.DAYS);
        createTestBooking(memberCoOwner, start1, start1.plus(2, ChronoUnit.HOURS), BookingStatus.CONFIRMED, new BigDecimal("100000.00"));

        mockMvc.perform(get("/api/v1/bookings/my-bookings")
                        .with(user(toPrincipal(memberCoOwner))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.items", not(empty())))
                .andExpect(jsonPath("$.data.totalElements", greaterThanOrEqualTo(1)));
    }

    @Test
    @DisplayName("15. Vehicle Reservations: Syndicate member retrieves vehicle bookings (200 OK)")
    void testGetVehicleBookings_SyndicateMember() throws Exception {
        Instant start = Instant.now().plus(2, ChronoUnit.DAYS);
        createTestBooking(memberCoOwner, start, start.plus(2, ChronoUnit.HOURS), BookingStatus.CONFIRMED, new BigDecimal("100000.00"));

        mockMvc.perform(get("/api/v1/bookings/vehicle/" + testVehicle.getId())
                        .with(user(toPrincipal(otherMemberCoOwner))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.items", not(empty())));
    }

    @Test
    @DisplayName("16. Vehicle Reservations: Non-member co-owner receives 403 Forbidden")
    void testGetVehicleBookings_NonMemberForbidden() throws Exception {
        mockMvc.perform(get("/api/v1/bookings/vehicle/" + testVehicle.getId())
                        .with(user(toPrincipal(nonMemberCoOwner))))
                .andExpect(status().isForbidden());
    }
}
