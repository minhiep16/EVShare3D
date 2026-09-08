package com.example.evshare.controller;

import com.example.evshare.dto.request.CheckInRequest;
import com.example.evshare.dto.request.CheckOutRequest;
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
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@DisplayName("Checkpoint 05-I — Usage Session & Telemetry Integration Tests")
class UsageSessionIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @Autowired private UserRepository userRepository;
    @Autowired private RoleRepository roleRepository;
    @Autowired private VehicleRepository vehicleRepository;
    @Autowired private OwnershipGroupRepository ownershipGroupRepository;
    @Autowired private OwnershipShareRepository ownershipShareRepository;
    @Autowired private BookingRepository bookingRepository;
    @Autowired private UsageSessionRepository usageSessionRepository;
    @Autowired private VehicleInspectionRepository vehicleInspectionRepository;

    private static final java.util.concurrent.atomic.AtomicLong PHONE_SEQ = new java.util.concurrent.atomic.AtomicLong(System.currentTimeMillis() % 80000000L + 10000000L);

    private User coOwner;
    private User otherUser;
    private Vehicle testVehicle;
    private OwnershipGroup testGroup;
    private Booking testBooking;

    @BeforeEach
    void setUp() {
        Role roleCoOwner = roleRepository.findByName(RoleName.ROLE_CO_OWNER)
                .orElseGet(() -> roleRepository.save(new Role(null, RoleName.ROLE_CO_OWNER)));

        coOwner = createTestUser("session.owner." + UUID.randomUUID() + "@evshare.io", "Alice Driver", roleCoOwner);
        otherUser = createTestUser("session.other." + UUID.randomUUID() + "@evshare.io", "Bob Outsider", roleCoOwner);

        testVehicle = new Vehicle();
        testVehicle.setVin("VIN" + UUID.randomUUID().toString().replace("-", "").substring(0, 14).toUpperCase());
        testVehicle.setLicensePlate("51H-" + UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase());
        testVehicle.setModelName("VinFast VF9 Plus");
        testVehicle.setManufacturer("VinFast");
        testVehicle.setModel3dAssetPath("models/vehicles/vf9.glb");
        testVehicle.setStatus(VehicleStatus.AVAILABLE);
        testVehicle.setBatteryLevel(95);
        testVehicle.setOdometerKm(new BigDecimal("15000.00"));
        testVehicle.setStallLocationCode("BAY-VF9-01");
        testVehicle = vehicleRepository.saveAndFlush(testVehicle);

        testGroup = new OwnershipGroup();
        testGroup.setGroupName("VF9 Syndicate " + UUID.randomUUID().toString().substring(0, 8));
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

        testBooking = new Booking();
        testBooking.setUser(coOwner);
        testBooking.setVehicle(testVehicle);
        testBooking.setStartTime(Instant.now().minus(10, ChronoUnit.MINUTES));
        testBooking.setEndTime(Instant.now().plus(2, ChronoUnit.HOURS));
        testBooking.setStatus(BookingStatus.CONFIRMED);
        testBooking.setEstimatedCost(new BigDecimal("250000.00"));
        testBooking = bookingRepository.saveAndFlush(testBooking);
    }

    private User createTestUser(String email, String fullName, Role role) {
        User u = new User();
        u.setEmail(email);
        u.setPasswordHash("$2a$12$dummyPasswordHashForSessionTesting12345");
        u.setFullName(fullName);
        u.setPhoneNumber(String.format("09%08d", PHONE_SEQ.incrementAndGet() % 100000000L));
        u.setIsActive(true);
        u.getRoles().add(role);
        return userRepository.saveAndFlush(u);
    }

    @Test
    @DisplayName("Check-In API: Successfully commences trip and updates vehicle/booking state")
    void testCheckIn_Success() throws Exception {
        CheckInRequest request = new CheckInRequest(
                testBooking.getId(),
                new BigDecimal("15000.00"),
                95,
                "{\"defects\": [\"BUMPER_SCRATCH\"]}",
                "Vehicle inspected, clean condition",
                List.of("https://s3.evshare.vn/checkin-1.jpg")
        );

        UserPrincipal principal = UserPrincipal.create(coOwner);

        mockMvc.perform(post("/api/v1/usage-sessions/check-in")
                        .with(user(principal))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").isNumber())
                .andExpect(jsonPath("$.data.bookingId").value(testBooking.getId()))
                .andExpect(jsonPath("$.data.startOdometer").value(15000.00))
                .andExpect(jsonPath("$.data.startBattery").value(95))
                .andExpect(jsonPath("$.data.status").value("ACTIVE"))
                .andExpect(jsonPath("$.data.inspections", hasSize(1)))
                .andExpect(jsonPath("$.data.inspections[0].inspectionType").value("CHECK_IN"));

        // Verify entity mutations in DB
        Booking updatedBooking = bookingRepository.findById(testBooking.getId()).orElseThrow();
        assertEquals(BookingStatus.IN_USE, updatedBooking.getStatus());

        Vehicle updatedVehicle = vehicleRepository.findById(testVehicle.getId()).orElseThrow();
        assertEquals(VehicleStatus.IN_USE, updatedVehicle.getStatus());
    }

    @Test
    @DisplayName("Check-In API: Rejects duplicate check-in for the same booking reservation")
    void testCheckIn_RejectsDuplicate() throws Exception {
        CheckInRequest request = new CheckInRequest(testBooking.getId(), new BigDecimal("15000.00"), 95);
        UserPrincipal principal = UserPrincipal.create(coOwner);

        // First check-in
        mockMvc.perform(post("/api/v1/usage-sessions/check-in")
                        .with(user(principal))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        // Duplicate check-in attempt
        mockMvc.perform(post("/api/v1/usage-sessions/check-in")
                        .with(user(principal))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message", anyOf(
                        containsString("Only CONFIRMED bookings can be checked in"),
                        containsString("already active"),
                        containsString("already exists")
                )));
    }

    @Test
    @DisplayName("Check-Out API: Balanced usage (>20% SoC) returns vehicle to AVAILABLE with zero surcharge")
    void testCheckOut_Balanced_Success() throws Exception {
        UserPrincipal principal = UserPrincipal.create(coOwner);

        // Check in first
        CheckInRequest checkInReq = new CheckInRequest(testBooking.getId(), new BigDecimal("15000.00"), 95);
        MvcResult checkInResult = mockMvc.perform(post("/api/v1/usage-sessions/check-in")
                        .with(user(principal))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(checkInReq)))
                .andExpect(status().isCreated())
                .andReturn();

        Number sessionId = com.jayway.jsonpath.JsonPath.read(checkInResult.getResponse().getContentAsString(), "$.data.id");

        // Check out with 65% battery (>20%) and 85 km driven
        CheckOutRequest checkOutReq = new CheckOutRequest(
                new BigDecimal("15085.00"),
                65,
                false,
                false,
                null,
                "Returned in perfect condition",
                null,
                null
        );

        mockMvc.perform(post("/api/v1/usage-sessions/" + sessionId + "/check-out")
                        .with(user(principal))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(checkOutReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("COMPLETED"))
                .andExpect(jsonPath("$.data.endOdometer").value(15085.00))
                .andExpect(jsonPath("$.data.mileage").value(85.00))
                .andExpect(jsonPath("$.data.endBattery").value(65))
                .andExpect(jsonPath("$.data.batteryDelta").value(30))
                .andExpect(jsonPath("$.data.additionalCost").value(0.00));

        // DB verifications
        Booking completedBooking = bookingRepository.findById(testBooking.getId()).orElseThrow();
        assertEquals(BookingStatus.COMPLETED, completedBooking.getStatus());

        Vehicle availableVehicle = vehicleRepository.findById(testVehicle.getId()).orElseThrow();
        assertEquals(VehicleStatus.AVAILABLE, availableVehicle.getStatus());
        assertEquals(new BigDecimal("15085.00"), availableVehicle.getOdometerKm());
        assertEquals(65, availableVehicle.getBatteryLevel());
    }

    @Test
    @DisplayName("Check-Out API: BR-OPS-02 Low battery (<20%) without plugging in assesses 150,000 VND surcharge")
    void testCheckOut_LowBatteryPenalty_Assesses150k() throws Exception {
        UserPrincipal principal = UserPrincipal.create(coOwner);

        // Check in
        CheckInRequest checkInReq = new CheckInRequest(testBooking.getId(), new BigDecimal("15000.00"), 95);
        MvcResult checkInResult = mockMvc.perform(post("/api/v1/usage-sessions/check-in")
                        .with(user(principal))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(checkInReq)))
                .andExpect(status().isCreated())
                .andReturn();

        Number sessionId = com.jayway.jsonpath.JsonPath.read(checkInResult.getResponse().getContentAsString(), "$.data.id");

        // Check out with 12% SoC (<20%) and NOT plugged in
        CheckOutRequest checkOutReq = new CheckOutRequest(
                new BigDecimal("15200.00"),
                12,
                false, // not plugged in
                false,
                null,
                "Returned low battery",
                null,
                null
        );

        mockMvc.perform(post("/api/v1/usage-sessions/" + sessionId + "/check-out")
                        .with(user(principal))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(checkOutReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("COMPLETED"))
                .andExpect(jsonPath("$.data.endBattery").value(12))
                .andExpect(jsonPath("$.data.additionalCost").value(150000.00))
                .andExpect(jsonPath("$.data.costBreakdown.LOW_BATTERY_SURCHARGE").value(150000.00));
    }

    @Test
    @DisplayName("Historical Immutability API: Second check-out on completed session is rejected with HTTP 409 Conflict")
    void testHistoricalUsage_Immutable_RejectsReCheckoutWith409() throws Exception {
        UserPrincipal principal = UserPrincipal.create(coOwner);

        // Check in
        CheckInRequest checkInReq = new CheckInRequest(testBooking.getId(), new BigDecimal("15000.00"), 95);
        MvcResult checkInResult = mockMvc.perform(post("/api/v1/usage-sessions/check-in")
                        .with(user(principal))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(checkInReq)))
                .andExpect(status().isCreated())
                .andReturn();

        Number sessionId = com.jayway.jsonpath.JsonPath.read(checkInResult.getResponse().getContentAsString(), "$.data.id");

        // Complete check out
        CheckOutRequest checkOutReq = new CheckOutRequest(new BigDecimal("15050.00"), 80);
        mockMvc.perform(post("/api/v1/usage-sessions/" + sessionId + "/check-out")
                        .with(user(principal))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(checkOutReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("COMPLETED"));

        // Attempt second check out (Tampering / Rewriting historical usage)
        CheckOutRequest tamperReq = new CheckOutRequest(new BigDecimal("15200.00"), 50);
        mockMvc.perform(post("/api/v1/usage-sessions/" + sessionId + "/check-out")
                        .with(user(principal))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(tamperReq)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message", containsString("immutable")))
                .andExpect(jsonPath("$.message", containsString("Historical usage telemetry must not be rewritten")));
    }

    @Test
    @DisplayName("Query APIs: Session details, booking lookup, and vehicle history operate correctly")
    void testQueryEndpoints_Success() throws Exception {
        UserPrincipal principal = UserPrincipal.create(coOwner);

        // Check in
        CheckInRequest checkInReq = new CheckInRequest(
                testBooking.getId(),
                new BigDecimal("15000.00"),
                95,
                "{\"defects\": []}",
                "Routine check in inspection",
                null
        );
        MvcResult checkInResult = mockMvc.perform(post("/api/v1/usage-sessions/check-in")
                        .with(user(principal))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(checkInReq)))
                .andExpect(status().isCreated())
                .andReturn();

        Number sessionId = com.jayway.jsonpath.JsonPath.read(checkInResult.getResponse().getContentAsString(), "$.data.id");

        // GET by ID
        mockMvc.perform(get("/api/v1/usage-sessions/" + sessionId)
                        .with(user(principal)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(sessionId))
                .andExpect(jsonPath("$.data.status").value("ACTIVE"));

        // GET by Booking ID
        mockMvc.perform(get("/api/v1/usage-sessions/booking/" + testBooking.getId())
                        .with(user(principal)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(sessionId));

        // GET by Vehicle ID
        mockMvc.perform(get("/api/v1/usage-sessions/vehicle/" + testVehicle.getId())
                        .with(user(principal)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(greaterThanOrEqualTo(1))));

        // GET My Sessions
        mockMvc.perform(get("/api/v1/usage-sessions/my-sessions")
                        .with(user(principal)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(greaterThanOrEqualTo(1))));

        // GET Inspections
        mockMvc.perform(get("/api/v1/usage-sessions/" + sessionId + "/inspections")
                        .with(user(principal)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].inspectionType").value("CHECK_IN"));
    }

    @Test
    @DisplayName("Check-In API: Rejects check-in when target vehicle does not match booking reservation")
    void testCheckIn_VehicleMismatch_ReturnsBadRequest() throws Exception {
        CheckInRequest request = new CheckInRequest(testBooking.getId(), 99999L, new BigDecimal("15000.00"), 95);
        UserPrincipal principal = UserPrincipal.create(coOwner);

        mockMvc.perform(post("/api/v1/usage-sessions/check-in")
                        .with(user(principal))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message", containsString("Vehicle mismatch")));
    }

    @Test
    @DisplayName("Check-In API: Rejects check-in when time is earlier than 15 minutes before booking start")
    void testCheckIn_TimeWindowTooEarly_ReturnsBadRequest() throws Exception {
        testBooking.setStartTime(Instant.now().plus(40, ChronoUnit.MINUTES));
        bookingRepository.saveAndFlush(testBooking);

        CheckInRequest request = new CheckInRequest(testBooking.getId(), new BigDecimal("15000.00"), 95);
        UserPrincipal principal = UserPrincipal.create(coOwner);

        mockMvc.perform(post("/api/v1/usage-sessions/check-in")
                        .with(user(principal))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message", containsString("Check-in is not yet open")));
    }

    @Test
    @DisplayName("Check-In API: BR-OPS-01 Expired check-in window (>30 min late) transitions booking to NO_SHOW and rejects")
    void testCheckIn_TimeWindowExpired_MarksNoShow_ReturnsBadRequest() throws Exception {
        testBooking.setStartTime(Instant.now().minus(45, ChronoUnit.MINUTES));
        bookingRepository.saveAndFlush(testBooking);

        CheckInRequest request = new CheckInRequest(testBooking.getId(), new BigDecimal("15000.00"), 95);
        UserPrincipal principal = UserPrincipal.create(coOwner);

        mockMvc.perform(post("/api/v1/usage-sessions/check-in")
                        .with(user(principal))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message", containsString("marked as NO_SHOW")));

        // Verify DB status transitioned to NO_SHOW
        Booking noShowBooking = bookingRepository.findById(testBooking.getId()).orElseThrow();
        assertEquals(BookingStatus.NO_SHOW, noShowBooking.getStatus());
    }

    @Test
    @DisplayName("Check-In API: Rejects check-in when vehicle is already in IN_USE state")
    void testCheckIn_VehicleAlreadyInUse_ReturnsBadRequest() throws Exception {
        testVehicle.setStatus(VehicleStatus.IN_USE);
        vehicleRepository.saveAndFlush(testVehicle);

        CheckInRequest request = new CheckInRequest(testBooking.getId(), new BigDecimal("15000.00"), 95);
        UserPrincipal principal = UserPrincipal.create(coOwner);

        mockMvc.perform(post("/api/v1/usage-sessions/check-in")
                        .with(user(principal))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message", containsString("currently in use")));
    }

    @Test
    @DisplayName("Check-In API: Rejects check-in when vehicle is in MAINTENANCE")
    void testCheckIn_VehicleInMaintenance_ReturnsBadRequest() throws Exception {
        testVehicle.setStatus(VehicleStatus.MAINTENANCE);
        vehicleRepository.saveAndFlush(testVehicle);

        CheckInRequest request = new CheckInRequest(testBooking.getId(), new BigDecimal("15000.00"), 95);
        UserPrincipal principal = UserPrincipal.create(coOwner);

        mockMvc.perform(post("/api/v1/usage-sessions/check-in")
                        .with(user(principal))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message", containsString("not ready for check-in")));
    }

    @Test
    @DisplayName("Check-In API: Rejects check-in attempt by an unauthorized third party")
    void testCheckIn_UnauthorizedUser_ReturnsForbidden() throws Exception {
        CheckInRequest request = new CheckInRequest(testBooking.getId(), new BigDecimal("15000.00"), 95);
        UserPrincipal principal = UserPrincipal.create(otherUser);

        mockMvc.perform(post("/api/v1/usage-sessions/check-in")
                        .with(user(principal))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message", containsString("Access denied")));
    }

    @Test
    @DisplayName("Check-In API: Rejects check-in if vehicle already has an active session from another booking")
    void testCheckIn_DuplicateActiveSessionOnVehicle_ReturnsBadRequest() throws Exception {
        // Create second booking for same vehicle
        Booking booking2 = new Booking();
        booking2.setUser(coOwner);
        booking2.setVehicle(testVehicle);
        booking2.setStartTime(Instant.now().minus(5, ChronoUnit.MINUTES));
        booking2.setEndTime(Instant.now().plus(4, ChronoUnit.HOURS));
        booking2.setStatus(BookingStatus.CONFIRMED);
        booking2.setEstimatedCost(new BigDecimal("300000.00"));
        booking2 = bookingRepository.saveAndFlush(booking2);

        // Check in first booking
        CheckInRequest request1 = new CheckInRequest(testBooking.getId(), new BigDecimal("15000.00"), 95);
        UserPrincipal principal = UserPrincipal.create(coOwner);

        mockMvc.perform(post("/api/v1/usage-sessions/check-in")
                        .with(user(principal))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request1)))
                .andExpect(status().isCreated());

        // Check in second booking on same vehicle
        CheckInRequest request2 = new CheckInRequest(booking2.getId(), new BigDecimal("15000.00"), 95);
        mockMvc.perform(post("/api/v1/usage-sessions/check-in")
                        .with(user(principal))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request2)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message", anyOf(
                        containsString("currently in use"),
                        containsString("already has an active usage session")
                )));
    }

    @Test
    @DisplayName("Check-Out API: Rejects check-out when target vehicleId does not match session vehicle")
    void testCheckOut_Rejects_VehicleMismatch() throws Exception {
        UserPrincipal principal = UserPrincipal.create(coOwner);

        // Check in
        CheckInRequest checkInReq = new CheckInRequest(testBooking.getId(), new BigDecimal("15000.00"), 95);
        MvcResult checkInResult = mockMvc.perform(post("/api/v1/usage-sessions/check-in")
                        .with(user(principal))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(checkInReq)))
                .andExpect(status().isCreated())
                .andReturn();

        Number sessionId = com.jayway.jsonpath.JsonPath.read(checkInResult.getResponse().getContentAsString(), "$.data.id");

        // Attempt checkout with mismatched vehicleId (99999L)
        CheckOutRequest checkOutReq = new CheckOutRequest(
                new BigDecimal("15080.00"),
                70,
                false,
                false,
                null,
                "Return notes",
                null,
                null,
                99999L,
                coOwner.getId()
        );

        mockMvc.perform(post("/api/v1/usage-sessions/" + sessionId + "/check-out")
                        .with(user(principal))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(checkOutReq)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message", containsString("Vehicle mismatch")));
    }

    @Test
    @DisplayName("Check-Out API: Rejects check-out when target userId does not match session booking")
    void testCheckOut_Rejects_UserMismatch() throws Exception {
        UserPrincipal principal = UserPrincipal.create(coOwner);

        // Check in
        CheckInRequest checkInReq = new CheckInRequest(testBooking.getId(), new BigDecimal("15000.00"), 95);
        MvcResult checkInResult = mockMvc.perform(post("/api/v1/usage-sessions/check-in")
                        .with(user(principal))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(checkInReq)))
                .andExpect(status().isCreated())
                .andReturn();

        Number sessionId = com.jayway.jsonpath.JsonPath.read(checkInResult.getResponse().getContentAsString(), "$.data.id");

        // Attempt checkout with mismatched userId (99999L)
        CheckOutRequest checkOutReq = new CheckOutRequest(
                new BigDecimal("15080.00"),
                70,
                false,
                false,
                null,
                "Return notes",
                null,
                null,
                testVehicle.getId(),
                99999L
        );

        mockMvc.perform(post("/api/v1/usage-sessions/" + sessionId + "/check-out")
                        .with(user(principal))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(checkOutReq)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message", containsString("User mismatch")));
    }

    @Test
    @DisplayName("Check-Out API: Rejects check-out when associated booking is not in IN_USE status")
    void testCheckOut_Rejects_BookingNotInUse() throws Exception {
        UserPrincipal principal = UserPrincipal.create(coOwner);

        // Check in
        CheckInRequest checkInReq = new CheckInRequest(testBooking.getId(), new BigDecimal("15000.00"), 95);
        MvcResult checkInResult = mockMvc.perform(post("/api/v1/usage-sessions/check-in")
                        .with(user(principal))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(checkInReq)))
                .andExpect(status().isCreated())
                .andReturn();

        Number sessionId = com.jayway.jsonpath.JsonPath.read(checkInResult.getResponse().getContentAsString(), "$.data.id");

        // Force booking to non-IN_USE status (e.g. CANCELLED)
        Booking booking = bookingRepository.findById(testBooking.getId()).orElseThrow();
        booking.setStatus(BookingStatus.CANCELLED);
        bookingRepository.saveAndFlush(booking);

        CheckOutRequest checkOutReq = new CheckOutRequest(new BigDecimal("15080.00"), 70);

        mockMvc.perform(post("/api/v1/usage-sessions/" + sessionId + "/check-out")
                        .with(user(principal))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(checkOutReq)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message", containsString("expected 'IN_USE'")));
    }

    @Test
    @DisplayName("Check-Out API: Rejects check-out when vehicle is not in IN_USE status")
    void testCheckOut_Rejects_VehicleNotInUse() throws Exception {
        UserPrincipal principal = UserPrincipal.create(coOwner);

        // Check in
        CheckInRequest checkInReq = new CheckInRequest(testBooking.getId(), new BigDecimal("15000.00"), 95);
        MvcResult checkInResult = mockMvc.perform(post("/api/v1/usage-sessions/check-in")
                        .with(user(principal))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(checkInReq)))
                .andExpect(status().isCreated())
                .andReturn();

        Number sessionId = com.jayway.jsonpath.JsonPath.read(checkInResult.getResponse().getContentAsString(), "$.data.id");

        // Force vehicle to non-IN_USE status (e.g. MAINTENANCE)
        Vehicle vehicle = vehicleRepository.findById(testVehicle.getId()).orElseThrow();
        vehicle.setStatus(VehicleStatus.MAINTENANCE);
        vehicleRepository.saveAndFlush(vehicle);

        CheckOutRequest checkOutReq = new CheckOutRequest(new BigDecimal("15080.00"), 70);

        mockMvc.perform(post("/api/v1/usage-sessions/" + sessionId + "/check-out")
                        .with(user(principal))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(checkOutReq)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message", containsString("expected 'IN_USE'")));
    }

    @Test
    @DisplayName("Check-Out API: Rejects check-out attempt by an unauthorized third party")
    void testCheckOut_Rejects_UnauthorizedUser() throws Exception {
        UserPrincipal coOwnerPrincipal = UserPrincipal.create(coOwner);
        UserPrincipal otherPrincipal = UserPrincipal.create(otherUser);

        // Check in by coOwner
        CheckInRequest checkInReq = new CheckInRequest(testBooking.getId(), new BigDecimal("15000.00"), 95);
        MvcResult checkInResult = mockMvc.perform(post("/api/v1/usage-sessions/check-in")
                        .with(user(coOwnerPrincipal))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(checkInReq)))
                .andExpect(status().isCreated())
                .andReturn();

        Number sessionId = com.jayway.jsonpath.JsonPath.read(checkInResult.getResponse().getContentAsString(), "$.data.id");

        // Check out attempt by otherUser
        CheckOutRequest checkOutReq = new CheckOutRequest(new BigDecimal("15080.00"), 70);

        mockMvc.perform(post("/api/v1/usage-sessions/" + sessionId + "/check-out")
                        .with(user(otherPrincipal))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(checkOutReq)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message", containsString("Access denied")));
    }
}
