package com.example.evshare.controller;

import com.example.evshare.entity.*;
import com.example.evshare.entity.enums.BookingStatus;
import com.example.evshare.entity.enums.RoleName;
import com.example.evshare.entity.enums.VehicleStatus;
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
@DisplayName("Checkpoint 05-H — Fair Usage Analytics Integration Tests")
class FairUsageIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private OwnershipGroupRepository ownershipGroupRepository;

    @Autowired
    private OwnershipShareRepository ownershipShareRepository;

    @Autowired
    private VehicleRepository vehicleRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private BookingRepository bookingRepository;

    private User memberOne;
    private User memberTwo;
    private User nonMember;
    private User adminUser;
    private Vehicle testVehicle;
    private OwnershipGroup testGroup;

    @BeforeEach
    void setUp() {
        Role roleCoOwner = roleRepository.findByName(RoleName.ROLE_CO_OWNER)
                .orElseGet(() -> roleRepository.save(new Role(null, RoleName.ROLE_CO_OWNER)));
        Role roleAdmin = roleRepository.findByName(RoleName.ROLE_ADMIN)
                .orElseGet(() -> roleRepository.save(new Role(null, RoleName.ROLE_ADMIN)));

        memberOne = createTestUser("fair.member1." + UUID.randomUUID() + "@evshare.io", "Alice CoOwner", roleCoOwner);
        memberTwo = createTestUser("fair.member2." + UUID.randomUUID() + "@evshare.io", "Bob CoOwner", roleCoOwner);
        nonMember = createTestUser("fair.nonmember." + UUID.randomUUID() + "@evshare.io", "Charlie NonMember", roleCoOwner);
        adminUser = createTestUser("fair.admin." + UUID.randomUUID() + "@evshare.io", "Platform Administrator", roleAdmin);

        testVehicle = new Vehicle();
        testVehicle.setVin("VIN" + UUID.randomUUID().toString().replace("-", "").substring(0, 14).toUpperCase());
        testVehicle.setLicensePlate("51L-" + (int) (10000 + Math.random() * 89999));
        testVehicle.setModelName("Audi e-tron GT");
        testVehicle.setManufacturer("Audi");
        testVehicle.setModel3dAssetPath("models/vehicles/models.glb");
        testVehicle.setStatus(VehicleStatus.AVAILABLE);
        testVehicle.setBatteryLevel(88);
        testVehicle.setOdometerKm(new BigDecimal("12000.00"));
        testVehicle.setStallLocationCode("BAY-AUDI-01");
        testVehicle = vehicleRepository.saveAndFlush(testVehicle);

        testGroup = new OwnershipGroup();
        testGroup.setGroupName("Audi Syndicate " + UUID.randomUUID().toString().substring(0, 8));
        testGroup.setVehicle(testVehicle);
        testGroup.setFormationDate(LocalDate.now());
        testGroup.setIsActive(true);
        testGroup = ownershipGroupRepository.saveAndFlush(testGroup);

        OwnershipShare share1 = new OwnershipShare();
        share1.setGroup(testGroup);
        share1.setUser(memberOne);
        share1.setPercentage(new BigDecimal("60.00"));
        share1.setShareCertificateNumber("CERT-" + UUID.randomUUID().toString().substring(0, 8));
        share1.setIsActive(true);
        ownershipShareRepository.saveAndFlush(share1);

        OwnershipShare share2 = new OwnershipShare();
        share2.setGroup(testGroup);
        share2.setUser(memberTwo);
        share2.setPercentage(new BigDecimal("40.00"));
        share2.setShareCertificateNumber("CERT-" + UUID.randomUUID().toString().substring(0, 8));
        share2.setIsActive(true);
        ownershipShareRepository.saveAndFlush(share2);

        // Add bookings within trailing window
        Instant now = Instant.now();
        createTestBooking(memberOne, now.minus(5, ChronoUnit.DAYS), now.minus(5, ChronoUnit.DAYS).plus(4, ChronoUnit.HOURS));
        createTestBooking(memberTwo, now.minus(3, ChronoUnit.DAYS), now.minus(3, ChronoUnit.DAYS).plus(2, ChronoUnit.HOURS));
    }

    private User createTestUser(String email, String fullName, Role role) {
        User u = new User();
        u.setEmail(email);
        u.setPasswordHash("$2a$12$dummyHashForFairUsageIntegrationTesting12345");
        u.setFullName(fullName);
        u.setIsActive(true);
        u.getRoles().add(role);
        return userRepository.saveAndFlush(u);
    }

    private UserPrincipal toPrincipal(User u) {
        return UserPrincipal.create(u);
    }

    private Booking createTestBooking(User user, Instant start, Instant end) {
        Booking b = Booking.builder()
                .vehicle(testVehicle)
                .user(user)
                .startTime(start)
                .endTime(end)
                .status(BookingStatus.COMPLETED)
                .estimatedCost(new BigDecimal("200000.00"))
                .createdAt(Instant.now())
                .build();
        return bookingRepository.saveAndFlush(b);
    }

    @Test
    @DisplayName("1. Syndicate Fair Usage: Authorized co-owner retrieves group analytics (200 OK)")
    void testGetGroupFairUsage_Success() throws Exception {
        mockMvc.perform(get("/api/v1/analytics/fair-usage/" + testGroup.getId())
                        .with(user(toPrincipal(memberOne))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.groupId", is(testGroup.getId().intValue())))
                .andExpect(jsonPath("$.data.vehicleId", is(testVehicle.getId().intValue())))
                .andExpect(jsonPath("$.data.evaluationWindowDays", is(30)))
                .andExpect(jsonPath("$.data.totalAvailableHours", is(720.0)))
                .andExpect(jsonPath("$.data.memberMetrics", hasSize(2)))
                .andExpect(jsonPath("$.data.memberMetrics[0].userId", is(memberOne.getId().intValue())))
                .andExpect(jsonPath("$.data.memberMetrics[0].ownershipPercentage", is(60.0)))
                .andExpect(jsonPath("$.data.memberMetrics[0].equityQuotaHours", is(432.0)))
                .andExpect(jsonPath("$.data.memberMetrics[1].userId", is(memberTwo.getId().intValue())))
                .andExpect(jsonPath("$.data.memberMetrics[1].ownershipPercentage", is(40.0)))
                .andExpect(jsonPath("$.data.memberMetrics[1].equityQuotaHours", is(288.0)));
    }

    @Test
    @DisplayName("2. Personal Fair Usage: Co-owner retrieves personal score and recommendation (200 OK)")
    void testGetMyFairUsage_Success() throws Exception {
        mockMvc.perform(get("/api/v1/analytics/fair-usage/" + testGroup.getId() + "/my-score")
                        .with(user(toPrincipal(memberOne))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.userId", is(memberOne.getId().intValue())))
                .andExpect(jsonPath("$.data.ownershipPercentage", is(60.0)))
                .andExpect(jsonPath("$.data.fairnessScore", notNullValue()))
                .andExpect(jsonPath("$.data.imbalanceLevel", notNullValue()))
                .andExpect(jsonPath("$.data.recommendation", notNullValue()));
    }

    @Test
    @DisplayName("3. Authorization: Non-syndicate co-owner receives 403 Forbidden")
    void testGetGroupFairUsage_NonMemberForbidden() throws Exception {
        mockMvc.perform(get("/api/v1/analytics/fair-usage/" + testGroup.getId())
                        .with(user(toPrincipal(nonMember))))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("4. Admin Access: Platform Admin can view group analytics (200 OK)")
    void testGetGroupFairUsage_AdminSuccess() throws Exception {
        mockMvc.perform(get("/api/v1/analytics/fair-usage/" + testGroup.getId())
                        .with(user(toPrincipal(adminUser))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.memberMetrics", hasSize(2)));
    }

    @Test
    @DisplayName("5. Not Found: Non-existent group ID returns 404 Not Found")
    void testGetGroupFairUsage_NotFound() throws Exception {
        mockMvc.perform(get("/api/v1/analytics/fair-usage/999999")
                        .with(user(toPrincipal(adminUser))))
                .andExpect(status().isNotFound());
    }
}
