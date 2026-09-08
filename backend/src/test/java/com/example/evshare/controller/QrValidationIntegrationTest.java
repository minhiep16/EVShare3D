package com.example.evshare.controller;

import com.example.evshare.dto.request.GenerateQrRequest;
import com.example.evshare.dto.request.QrValidationRequest;
import com.example.evshare.entity.*;
import com.example.evshare.entity.enums.BookingStatus;
import com.example.evshare.entity.enums.RoleName;
import com.example.evshare.entity.enums.VehicleStatus;
import com.example.evshare.repository.*;
import com.example.evshare.security.JwtTokenProvider;
import com.example.evshare.security.UserPrincipal;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Claims;
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
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@DisplayName("Checkpoint 05-L — QR Validation Integration Tests")
class QrValidationIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @Autowired private UserRepository userRepository;
    @Autowired private RoleRepository roleRepository;
    @Autowired private VehicleRepository vehicleRepository;
    @Autowired private OwnershipGroupRepository ownershipGroupRepository;
    @Autowired private OwnershipShareRepository ownershipShareRepository;
    @Autowired private BookingRepository bookingRepository;
    @Autowired private JwtTokenProvider jwtTokenProvider;

    private static final java.util.concurrent.atomic.AtomicLong PHONE_SEQ =
            new java.util.concurrent.atomic.AtomicLong(System.currentTimeMillis() % 80000000L + 10000000L);

    private User coOwner;
    private User otherUser;
    private User staffUser;
    private Vehicle testVehicle;
    private OwnershipGroup testGroup;
    private Booking testBooking;

    @BeforeEach
    void setUp() {
        Role roleCoOwner = roleRepository.findByName(RoleName.ROLE_CO_OWNER)
                .orElseGet(() -> roleRepository.save(new Role(null, RoleName.ROLE_CO_OWNER)));
        Role roleStaff = roleRepository.findByName(RoleName.ROLE_STAFF)
                .orElseGet(() -> roleRepository.save(new Role(null, RoleName.ROLE_STAFF)));

        coOwner = createTestUser("qr.coowner." + UUID.randomUUID() + "@evshare.io", "Le CoOwner", roleCoOwner);
        otherUser = createTestUser("qr.other." + UUID.randomUUID() + "@evshare.io", "Nguyen Outsider", roleCoOwner);
        staffUser = createTestUser("qr.staff." + UUID.randomUUID() + "@evshare.io", "Staff Station", roleStaff);

        testVehicle = new Vehicle();
        testVehicle.setVin("VIN" + UUID.randomUUID().toString().replace("-", "").substring(0, 14).toUpperCase());
        testVehicle.setLicensePlate("51H-" + UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase());
        testVehicle.setModelName("VinFast VF8 Plus");
        testVehicle.setManufacturer("VinFast");
        testVehicle.setModel3dAssetPath("models/vehicles/vf8.glb");
        testVehicle.setStatus(VehicleStatus.AVAILABLE);
        testVehicle.setBatteryLevel(90);
        testVehicle.setOdometerKm(new BigDecimal("12000.00"));
        testVehicle.setStallLocationCode("STALL-A1");
        testVehicle = vehicleRepository.saveAndFlush(testVehicle);

        testGroup = new OwnershipGroup();
        testGroup.setGroupName("VF8 Co-op " + UUID.randomUUID().toString().substring(0, 8));
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

        // Booking active right now (within check-in window [-15m, +30m])
        testBooking = new Booking();
        testBooking.setUser(coOwner);
        testBooking.setVehicle(testVehicle);
        testBooking.setStartTime(Instant.now());
        testBooking.setEndTime(Instant.now().plus(3, ChronoUnit.HOURS));
        testBooking.setStatus(BookingStatus.CONFIRMED);
        testBooking.setEstimatedCost(new BigDecimal("300000.00"));
        testBooking = bookingRepository.saveAndFlush(testBooking);
    }

    private User createTestUser(String email, String fullName, Role role) {
        User u = new User();
        u.setEmail(email);
        u.setPasswordHash("$2a$12$dummyPasswordHashForTesting12345");
        u.setFullName(fullName);
        u.setPhoneNumber(String.format("09%08d", PHONE_SEQ.incrementAndGet() % 100000000L));
        u.setIsActive(true);
        u.getRoles().add(role);
        return userRepository.saveAndFlush(u);
    }

    // =========================================================================
    // 1. GENERATE QR INTEGRATION TESTS
    // =========================================================================
    @Test
    @DisplayName("Generate QR: Successfully generates signed 5-minute token with zero sensitive data")
    void testGenerateQr_Success() throws Exception {
        GenerateQrRequest request = new GenerateQrRequest(testBooking.getId());
        UserPrincipal principal = UserPrincipal.create(coOwner);

        MvcResult result = mockMvc.perform(post("/api/v1/usage-sessions/generate-qr")
                        .with(user(principal))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.qrToken").isString())
                .andExpect(jsonPath("$.data.bookingId").value(testBooking.getId()))
                .andExpect(jsonPath("$.data.vehicleId").value(testVehicle.getId()))
                .andExpect(jsonPath("$.data.vehicleLicensePlate").value(testVehicle.getLicensePlate()))
                .andExpect(jsonPath("$.data.expiresInSeconds").value(300))
                .andReturn();

        JsonNode root = objectMapper.readTree(result.getResponse().getContentAsString());
        String qrToken = root.path("data").path("qrToken").asText();
        assertThat(qrToken).isNotBlank();

        // Invariant: Zero sensitive information inside QR token
        Claims claims = jwtTokenProvider.extractAllClaims(qrToken);
        assertThat(claims.get("tokenType")).isEqualTo("QR_CHECK_IN");
        assertThat(claims.get("bookingId")).isEqualTo(testBooking.getId().intValue());
        assertThat(claims.get("vehicleId")).isEqualTo(testVehicle.getId().intValue());
        assertThat(claims.get("userId")).isEqualTo(coOwner.getId().intValue());

        // Verify NO sensitive PII or credentials are in claims
        assertThat(claims.get("email")).isNull();
        assertThat(claims.get("password")).isNull();
        assertThat(claims.get("passwordHash")).isNull();
        assertThat(claims.get("phoneNumber")).isNull();
        assertThat(claims.get("creditCard")).isNull();
    }

    // =========================================================================
    // 2. VALIDATE QR INTEGRATION TESTS
    // =========================================================================

    @Test
    @DisplayName("1. Valid QR: Co-owner scans valid QR at station -> Returns 200 OK valid")
    void testValidateQr_Success_CoOwner() throws Exception {
        String token = jwtTokenProvider.generateQrToken(testBooking.getId(), testVehicle.getId(), coOwner.getId(), Duration.ofMinutes(5));
        QrValidationRequest request = new QrValidationRequest(token, testVehicle.getId(), testBooking.getId());

        UserPrincipal principal = UserPrincipal.create(coOwner);

        mockMvc.perform(post("/api/v1/usage-sessions/validate-qr")
                        .with(user(principal))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.valid").value(true))
                .andExpect(jsonPath("$.data.bookingId").value(testBooking.getId()))
                .andExpect(jsonPath("$.data.vehicleId").value(testVehicle.getId()))
                .andExpect(jsonPath("$.data.vehicleLicensePlate").value(testVehicle.getLicensePlate()))
                .andExpect(jsonPath("$.data.userId").value(coOwner.getId()))
                .andExpect(jsonPath("$.data.message").value(containsString("QR validation successful")));
    }

    @Test
    @DisplayName("1b. Valid QR: Staff operator scans QR on behalf of co-owner -> Returns 200 OK valid")
    void testValidateQr_Success_Staff() throws Exception {
        String token = jwtTokenProvider.generateQrToken(testBooking.getId(), testVehicle.getId(), coOwner.getId(), Duration.ofMinutes(5));
        QrValidationRequest request = new QrValidationRequest(token, testVehicle.getId(), testBooking.getId());

        UserPrincipal principal = UserPrincipal.create(staffUser);

        mockMvc.perform(post("/api/v1/usage-sessions/validate-qr")
                        .with(user(principal))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.valid").value(true))
                .andExpect(jsonPath("$.data.bookingId").value(testBooking.getId()));
    }

    @Test
    @DisplayName("2. Invalid QR: Malformed or forged signature -> Returns 400 Bad Request")
    void testValidateQr_InvalidToken_ReturnsBadRequest() throws Exception {
        QrValidationRequest request = new QrValidationRequest("eyMalformed.Token.InvalidSignature", testVehicle.getId(), testBooking.getId());
        UserPrincipal principal = UserPrincipal.create(coOwner);

        mockMvc.perform(post("/api/v1/usage-sessions/validate-qr")
                        .with(user(principal))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value(containsString("Invalid QR token")));
    }

    @Test
    @DisplayName("3. Expired QR: Past 5-minute TTL -> Returns 400 Bad Request")
    void testValidateQr_ExpiredToken_ReturnsBadRequest() throws Exception {
        // Generate an expired QR token (expired 10 seconds ago)
        String expiredToken = jwtTokenProvider.generateExpiredQrToken(testBooking.getId(), testVehicle.getId(), coOwner.getId(), -10000L);
        QrValidationRequest request = new QrValidationRequest(expiredToken, testVehicle.getId(), testBooking.getId());

        UserPrincipal principal = UserPrincipal.create(coOwner);

        mockMvc.perform(post("/api/v1/usage-sessions/validate-qr")
                        .with(user(principal))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value(containsString("QR token has expired")));
    }

    @Test
    @DisplayName("4. Wrong Vehicle: Physical station vehicle does not match booking vehicle -> Returns 400 Bad Request")
    void testValidateQr_WrongVehicle_ReturnsBadRequest() throws Exception {
        String token = jwtTokenProvider.generateQrToken(testBooking.getId(), testVehicle.getId(), coOwner.getId(), Duration.ofMinutes(5));

        // Station is vehicle 99999L instead of testVehicle.getId()
        QrValidationRequest request = new QrValidationRequest(token, 99999L, testBooking.getId());
        UserPrincipal principal = UserPrincipal.create(coOwner);

        mockMvc.perform(post("/api/v1/usage-sessions/validate-qr")
                        .with(user(principal))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value(containsString("Vehicle mismatch")));
    }

    @Test
    @DisplayName("5. Wrong Booking: Target booking ID mismatch -> Returns 400 Bad Request")
    void testValidateQr_WrongBooking_ReturnsBadRequest() throws Exception {
        String token = jwtTokenProvider.generateQrToken(testBooking.getId(), testVehicle.getId(), coOwner.getId(), Duration.ofMinutes(5));

        // Scanned request specifies booking 88888L instead of testBooking.getId()
        QrValidationRequest request = new QrValidationRequest(token, testVehicle.getId(), 88888L);
        UserPrincipal principal = UserPrincipal.create(coOwner);

        mockMvc.perform(post("/api/v1/usage-sessions/validate-qr")
                        .with(user(principal))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value(containsString("Booking mismatch")));
    }

    @Test
    @DisplayName("6. Unauthorized User: Outsider user has no syndicate share or admin role -> Returns 403 Forbidden")
    void testValidateQr_UnauthorizedUser_ReturnsForbidden() throws Exception {
        String token = jwtTokenProvider.generateQrToken(testBooking.getId(), testVehicle.getId(), coOwner.getId(), Duration.ofMinutes(5));
        QrValidationRequest request = new QrValidationRequest(token, testVehicle.getId(), testBooking.getId());

        // otherUser is not a member of the syndicate group and did not create the booking
        UserPrincipal principal = UserPrincipal.create(otherUser);

        mockMvc.perform(post("/api/v1/usage-sessions/validate-qr")
                        .with(user(principal))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value(containsString("Access denied")));
    }

    @Test
    @DisplayName("7. Unauthenticated User: Missing authentication -> Returns 401 Unauthorized")
    void testValidateQr_UnauthenticatedUser_ReturnsUnauthorized() throws Exception {
        String token = jwtTokenProvider.generateQrToken(testBooking.getId(), testVehicle.getId(), coOwner.getId(), Duration.ofMinutes(5));
        QrValidationRequest request = new QrValidationRequest(token, testVehicle.getId(), testBooking.getId());

        mockMvc.perform(post("/api/v1/usage-sessions/validate-qr")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("8. Time Window Expired: Late check-in (>30 min) marks booking as NO_SHOW -> Returns 400 Bad Request")
    void testValidateQr_TimeWindowExpired_MarksNoShow_ReturnsBadRequest() throws Exception {
        // Set booking startTime to 45 minutes ago
        testBooking.setStartTime(Instant.now().minus(45, ChronoUnit.MINUTES));
        bookingRepository.saveAndFlush(testBooking);

        String token = jwtTokenProvider.generateQrToken(testBooking.getId(), testVehicle.getId(), coOwner.getId(), Duration.ofMinutes(5));
        QrValidationRequest request = new QrValidationRequest(token, testVehicle.getId(), testBooking.getId());

        UserPrincipal principal = UserPrincipal.create(coOwner);

        mockMvc.perform(post("/api/v1/usage-sessions/validate-qr")
                        .with(user(principal))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value(containsString("Check-in window has expired")))
                .andExpect(jsonPath("$.message").value(containsString("NO_SHOW")));

        // Verify authoritative database mutation: Booking transitioned to NO_SHOW
        Booking updatedBooking = bookingRepository.findById(testBooking.getId()).orElseThrow();
        assertThat(updatedBooking.getStatus()).isEqualTo(BookingStatus.NO_SHOW);
    }
}
