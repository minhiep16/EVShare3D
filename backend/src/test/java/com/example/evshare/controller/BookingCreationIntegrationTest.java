package com.example.evshare.controller;

import com.example.evshare.dto.request.CreateBookingRequest;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@DisplayName("Checkpoint 05-D — Create Booking Integration Tests")
class BookingCreationIntegrationTest {

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

    private User memberCoOwner;
    private User nonMemberCoOwner;
    private User deactivatedCoOwner;
    private User adminUser;
    private Vehicle testVehicle;
    private OwnershipGroup testGroup;

    @BeforeEach
    void setUp() {
        Role roleCoOwner = roleRepository.findByName(RoleName.ROLE_CO_OWNER)
                .orElseGet(() -> roleRepository.save(new Role(null, RoleName.ROLE_CO_OWNER)));
        Role roleAdmin = roleRepository.findByName(RoleName.ROLE_ADMIN)
                .orElseGet(() -> roleRepository.save(new Role(null, RoleName.ROLE_ADMIN)));

        memberCoOwner = createTestUser("bkg.member." + UUID.randomUUID() + "@evshare.io", "Member Co-Owner", roleCoOwner, true);
        nonMemberCoOwner = createTestUser("bkg.nonmember." + UUID.randomUUID() + "@evshare.io", "Non-Member Co-Owner", roleCoOwner, true);
        deactivatedCoOwner = createTestUser("bkg.deactivated." + UUID.randomUUID() + "@evshare.io", "Deactivated Co-Owner", roleCoOwner, false);
        adminUser = createTestUser("bkg.admin." + UUID.randomUUID() + "@evshare.io", "Platform Admin", roleAdmin, true);

        testVehicle = new Vehicle();
        testVehicle.setVin("VIN" + UUID.randomUUID().toString().replace("-", "").substring(0, 14).toUpperCase());
        testVehicle.setLicensePlate("51K-" + (int) (10000 + Math.random() * 89999));
        testVehicle.setModelName("Porsche Taycan Cross Turismo");
        testVehicle.setManufacturer("Porsche");
        testVehicle.setModel3dAssetPath("models/vehicles/taycan.glb");
        testVehicle.setStatus(VehicleStatus.AVAILABLE);
        testVehicle.setBatteryLevel(95);
        testVehicle.setOdometerKm(new BigDecimal("12000.00"));
        testVehicle.setStallLocationCode("BAY-PORSCHE-01");
        testVehicle = vehicleRepository.saveAndFlush(testVehicle);

        testGroup = new OwnershipGroup();
        testGroup.setGroupName("Taycan Syndicate " + UUID.randomUUID().toString().substring(0, 8));
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

    private User createTestUser(String email, String fullName, Role role, boolean isActive) {
        User u = new User();
        u.setEmail(email);
        u.setPasswordHash("$2a$12$dummyHashForBookingCreationTestingPurpose12345");
        u.setFullName(fullName);
        u.setIsActive(isActive);
        u.getRoles().add(role);
        return userRepository.saveAndFlush(u);
    }

    private UserPrincipal toPrincipal(User u) {
        return UserPrincipal.create(u);
    }

    @Test
    @DisplayName("1. Valid Creation: Co-owner successfully creates reservation")
    void testCreateBookingSuccess_CoOwner() throws Exception {
        Instant start = Instant.now().plus(1, ChronoUnit.DAYS).truncatedTo(ChronoUnit.HOURS);
        Instant end = start.plus(4, ChronoUnit.HOURS);

        CreateBookingRequest req = new CreateBookingRequest(testVehicle.getId(), start, end);

        mockMvc.perform(post("/api/v1/bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req))
                        .with(user(toPrincipal(memberCoOwner))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.id", notNullValue()))
                .andExpect(jsonPath("$.data.vehicleId", is(testVehicle.getId().intValue())))
                .andExpect(jsonPath("$.data.vehicleModel", is("Porsche Taycan Cross Turismo")))
                .andExpect(jsonPath("$.data.userId", is(memberCoOwner.getId().intValue())))
                .andExpect(jsonPath("$.data.status", is("CONFIRMED")))
                .andExpect(jsonPath("$.data.estimatedCost", notNullValue()))
                .andExpect(jsonPath("$.data.startTime", notNullValue()))
                .andExpect(jsonPath("$.data.endTime", notNullValue()))
                .andExpect(jsonPath("$.data.bufferedEndTime", notNullValue()));
    }

    @Test
    @DisplayName("2. Security: Non-member Co-owner cannot book vehicle (403 Forbidden)")
    void testCreateBooking_NonMemberForbidden() throws Exception {
        Instant start = Instant.now().plus(1, ChronoUnit.DAYS).truncatedTo(ChronoUnit.HOURS);
        Instant end = start.plus(2, ChronoUnit.HOURS);

        CreateBookingRequest req = new CreateBookingRequest(testVehicle.getId(), start, end);

        mockMvc.perform(post("/api/v1/bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req))
                        .with(user(toPrincipal(nonMemberCoOwner))))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("3. Admin: Can book for fleet vehicle")
    void testCreateBooking_AdminCanBook() throws Exception {
        Instant start = Instant.now().plus(2, ChronoUnit.DAYS).truncatedTo(ChronoUnit.HOURS);
        Instant end = start.plus(2, ChronoUnit.HOURS);

        CreateBookingRequest req = new CreateBookingRequest(testVehicle.getId(), start, end);

        mockMvc.perform(post("/api/v1/bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req))
                        .with(user(toPrincipal(adminUser))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.userId", is(adminUser.getId().intValue())));
    }

    @Test
    @DisplayName("4. Admin: Can book on behalf of member user")
    void testCreateBooking_AdminCanBookForOtherUser() throws Exception {
        Instant start = Instant.now().plus(3, ChronoUnit.DAYS).truncatedTo(ChronoUnit.HOURS);
        Instant end = start.plus(3, ChronoUnit.HOURS);

        CreateBookingRequest req = new CreateBookingRequest(testVehicle.getId(), start, end, memberCoOwner.getId());

        mockMvc.perform(post("/api/v1/bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req))
                        .with(user(toPrincipal(adminUser))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.userId", is(memberCoOwner.getId().intValue())));
    }

    @Test
    @DisplayName("5. Anti-Spoofing: Co-owner cannot override target userId via payload")
    void testCreateBooking_FrontendSpoofingIgnored() throws Exception {
        Instant start = Instant.now().plus(4, ChronoUnit.DAYS).truncatedTo(ChronoUnit.HOURS);
        Instant end = start.plus(2, ChronoUnit.HOURS);

        // Member attempts to inject another user's ID
        CreateBookingRequest req = new CreateBookingRequest(testVehicle.getId(), start, end, nonMemberCoOwner.getId());

        mockMvc.perform(post("/api/v1/bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req))
                        .with(user(toPrincipal(memberCoOwner))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success", is(true)))
                // Should strictly assign to memberCoOwner, ignoring injected nonMemberCoOwner ID
                .andExpect(jsonPath("$.data.userId", is(memberCoOwner.getId().intValue())));
    }

    @Test
    @DisplayName("6. Business Rule: Deactivated user cannot create reservation (403 Forbidden)")
    void testCreateBooking_DeactivatedUserForbidden() throws Exception {
        Instant start = Instant.now().plus(1, ChronoUnit.DAYS).truncatedTo(ChronoUnit.HOURS);
        Instant end = start.plus(2, ChronoUnit.HOURS);

        CreateBookingRequest req = new CreateBookingRequest(testVehicle.getId(), start, end);

        mockMvc.perform(post("/api/v1/bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req))
                        .with(user(toPrincipal(deactivatedCoOwner))))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("7. BR-BKG-01: Duration under 30 minutes rejected (400 Bad Request)")
    void testCreateBooking_DurationUnder30Min_BadRequest() throws Exception {
        Instant start = Instant.now().plus(1, ChronoUnit.DAYS).truncatedTo(ChronoUnit.HOURS);
        Instant end = start.plus(15, ChronoUnit.MINUTES); // 15 min < 30 min

        CreateBookingRequest req = new CreateBookingRequest(testVehicle.getId(), start, end);

        mockMvc.perform(post("/api/v1/bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req))
                        .with(user(toPrincipal(memberCoOwner))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("Minimum booking duration is 30 minutes")));
    }

    @Test
    @DisplayName("8. BR-BKG-01: Duration over 72 hours rejected (400 Bad Request)")
    void testCreateBooking_DurationOver72Hours_BadRequest() throws Exception {
        Instant start = Instant.now().plus(1, ChronoUnit.DAYS).truncatedTo(ChronoUnit.HOURS);
        Instant end = start.plus(73, ChronoUnit.HOURS); // 73 hours > 72 hours

        CreateBookingRequest req = new CreateBookingRequest(testVehicle.getId(), start, end);

        mockMvc.perform(post("/api/v1/bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req))
                        .with(user(toPrincipal(memberCoOwner))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("Maximum continuous booking duration is 72 hours")));
    }

    @Test
    @DisplayName("9. Timing: Cannot schedule booking in the past (400 Bad Request)")
    void testCreateBooking_StartTimeInPast_BadRequest() throws Exception {
        Instant start = Instant.now().minus(2, ChronoUnit.HOURS);
        Instant end = start.plus(2, ChronoUnit.HOURS);

        CreateBookingRequest req = new CreateBookingRequest(testVehicle.getId(), start, end);

        mockMvc.perform(post("/api/v1/bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req))
                        .with(user(toPrincipal(memberCoOwner))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("Cannot schedule a reservation in the past")));
    }

    @Test
    @DisplayName("10. Timing: End time before start time rejected (400 Bad Request)")
    void testCreateBooking_EndTimeBeforeStartTime_BadRequest() throws Exception {
        Instant start = Instant.now().plus(2, ChronoUnit.DAYS);
        Instant end = start.minus(1, ChronoUnit.HOURS);

        CreateBookingRequest req = new CreateBookingRequest(testVehicle.getId(), start, end);

        mockMvc.perform(post("/api/v1/bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req))
                        .with(user(toPrincipal(memberCoOwner))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("End time must be strictly after start time")));
    }

    @Test
    @DisplayName("11. BR-BKG-01: Advance window exceeded (>30 days) rejected (400 Bad Request)")
    void testCreateBooking_AdvanceWindowExceeded_BadRequest() throws Exception {
        Instant start = Instant.now().plus(35, ChronoUnit.DAYS);
        Instant end = start.plus(2, ChronoUnit.HOURS);

        CreateBookingRequest req = new CreateBookingRequest(testVehicle.getId(), start, end);

        mockMvc.perform(post("/api/v1/bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req))
                        .with(user(toPrincipal(memberCoOwner))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("Bookings can only be scheduled up to 30 days in advance")));
    }

    @Test
    @DisplayName("12. Operational State: Vehicle under MAINTENANCE rejected (409 Conflict)")
    void testCreateBooking_VehicleInMaintenance_Conflict() throws Exception {
        testVehicle.setStatus(VehicleStatus.MAINTENANCE);
        vehicleRepository.saveAndFlush(testVehicle);

        Instant start = Instant.now().plus(1, ChronoUnit.DAYS).truncatedTo(ChronoUnit.HOURS);
        Instant end = start.plus(2, ChronoUnit.HOURS);

        CreateBookingRequest req = new CreateBookingRequest(testVehicle.getId(), start, end);

        mockMvc.perform(post("/api/v1/bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req))
                        .with(user(toPrincipal(memberCoOwner))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message", containsString("under maintenance")));
    }

    @Test
    @DisplayName("13. Operational State: Vehicle flagged as DAMAGED rejected (409 Conflict)")
    void testCreateBooking_VehicleInDamaged_Conflict() throws Exception {
        testVehicle.setStatus(VehicleStatus.DAMAGED);
        vehicleRepository.saveAndFlush(testVehicle);

        Instant start = Instant.now().plus(1, ChronoUnit.DAYS).truncatedTo(ChronoUnit.HOURS);
        Instant end = start.plus(2, ChronoUnit.HOURS);

        CreateBookingRequest req = new CreateBookingRequest(testVehicle.getId(), start, end);

        mockMvc.perform(post("/api/v1/bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req))
                        .with(user(toPrincipal(memberCoOwner))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message", containsString("damaged")));
    }

    @Test
    @DisplayName("14. BR-BKG-02: Direct overlap conflict rejected (409 Conflict)")
    void testCreateBooking_DirectOverlapConflict() throws Exception {
        Instant bookedStart = Instant.now().plus(5, ChronoUnit.DAYS).truncatedTo(ChronoUnit.HOURS);
        Instant bookedEnd = bookedStart.plus(3, ChronoUnit.HOURS);

        bookingRepository.saveAndFlush(
                Booking.builder()
                        .vehicle(testVehicle)
                        .user(memberCoOwner)
                        .startTime(bookedStart)
                        .endTime(bookedEnd)
                        .status(BookingStatus.CONFIRMED)
                        .build()
        );

        // Collides directly [bookedStart + 1h, bookedEnd + 1h]
        CreateBookingRequest req = new CreateBookingRequest(testVehicle.getId(),
                bookedStart.plus(1, ChronoUnit.HOURS),
                bookedEnd.plus(1, ChronoUnit.HOURS));

        mockMvc.perform(post("/api/v1/bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req))
                        .with(user(toPrincipal(memberCoOwner))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message", containsString("30-minute turnaround buffer")));
    }

    @Test
    @DisplayName("15. BR-BKG-02: Turnaround buffer conflict (inside 30m window) rejected (409 Conflict)")
    void testCreateBooking_TurnaroundBufferConflict() throws Exception {
        Instant bookedStart = Instant.now().plus(6, ChronoUnit.DAYS).truncatedTo(ChronoUnit.HOURS);
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

        // Requested start is only 15 minutes after existing booking ends (< 30 min buffer)
        Instant reqStart = bookedEnd.plus(15, ChronoUnit.MINUTES);
        Instant reqEnd = reqStart.plus(2, ChronoUnit.HOURS);

        CreateBookingRequest req = new CreateBookingRequest(testVehicle.getId(), reqStart, reqEnd);

        mockMvc.perform(post("/api/v1/bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req))
                        .with(user(toPrincipal(memberCoOwner))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message", containsString("30-minute turnaround buffer")));
    }

    @Test
    @DisplayName("16. BR-BKG-02: Slot after turnaround buffer (35m later) succeeds (201 Created)")
    void testCreateBooking_SlotAfterBufferSucceeds() throws Exception {
        Instant bookedStart = Instant.now().plus(7, ChronoUnit.DAYS).truncatedTo(ChronoUnit.HOURS);
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

        // Starts 35 minutes after previous ends (> 30 min buffer)
        Instant reqStart = bookedEnd.plus(35, ChronoUnit.MINUTES);
        Instant reqEnd = reqStart.plus(2, ChronoUnit.HOURS);

        CreateBookingRequest req = new CreateBookingRequest(testVehicle.getId(), reqStart, reqEnd);

        mockMvc.perform(post("/api/v1/bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req))
                        .with(user(toPrincipal(memberCoOwner))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success", is(true)));
    }

    @Test
    @DisplayName("17. Get Booking: Authorized owner can fetch reservation by ID (200 OK)")
    void testGetBookingById_AuthorizedOwner() throws Exception {
        Instant bookedStart = Instant.now().plus(8, ChronoUnit.DAYS).truncatedTo(ChronoUnit.HOURS);
        Instant bookedEnd = bookedStart.plus(2, ChronoUnit.HOURS);

        Booking booking = bookingRepository.saveAndFlush(
                Booking.builder()
                        .vehicle(testVehicle)
                        .user(memberCoOwner)
                        .startTime(bookedStart)
                        .endTime(bookedEnd)
                        .status(BookingStatus.CONFIRMED)
                        .estimatedCost(new BigDecimal("100000.00"))
                        .build()
        );

        mockMvc.perform(get("/api/v1/bookings/" + booking.getId())
                        .with(user(toPrincipal(memberCoOwner))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.id", is(booking.getId().intValue())))
                .andExpect(jsonPath("$.data.vehicleModel", is("Porsche Taycan Cross Turismo")))
                .andExpect(jsonPath("$.data.userId", is(memberCoOwner.getId().intValue())));
    }

    @Test
    @DisplayName("18. Get Booking: Non-member Co-owner cannot fetch another's reservation (403 Forbidden)")
    void testGetBookingById_UnauthorizedForbidden() throws Exception {
        Instant bookedStart = Instant.now().plus(9, ChronoUnit.DAYS).truncatedTo(ChronoUnit.HOURS);
        Instant bookedEnd = bookedStart.plus(2, ChronoUnit.HOURS);

        Booking booking = bookingRepository.saveAndFlush(
                Booking.builder()
                        .vehicle(testVehicle)
                        .user(memberCoOwner)
                        .startTime(bookedStart)
                        .endTime(bookedEnd)
                        .status(BookingStatus.CONFIRMED)
                        .build()
        );

        mockMvc.perform(get("/api/v1/bookings/" + booking.getId())
                        .with(user(toPrincipal(nonMemberCoOwner))))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("19. Get Booking: Non-existent ID returns 404 Not Found")
    void testGetBookingById_NotFound() throws Exception {
        mockMvc.perform(get("/api/v1/bookings/999999")
                        .with(user(toPrincipal(memberCoOwner))))
                .andExpect(status().isNotFound());
    }
}
