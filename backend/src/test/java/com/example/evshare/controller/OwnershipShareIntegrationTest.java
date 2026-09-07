package com.example.evshare.controller;

import com.example.evshare.dto.request.IssueOwnershipShareRequest;
import com.example.evshare.dto.request.UpdateOwnershipShareRequest;
import com.example.evshare.entity.OwnershipGroup;
import com.example.evshare.entity.OwnershipShare;
import com.example.evshare.entity.Role;
import com.example.evshare.entity.User;
import com.example.evshare.entity.Vehicle;
import com.example.evshare.entity.enums.RoleName;
import com.example.evshare.entity.enums.VehicleStatus;
import com.example.evshare.repository.OwnershipGroupRepository;
import com.example.evshare.repository.OwnershipShareRepository;
import com.example.evshare.repository.RoleRepository;
import com.example.evshare.repository.UserRepository;
import com.example.evshare.repository.VehicleRepository;
import com.example.evshare.security.TokenService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@DisplayName("Checkpoint 04-F — Ownership Share Integration Tests")
class OwnershipShareIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

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
    private PasswordEncoder passwordEncoder;

    @Autowired
    private TokenService tokenService;

    private User adminUser;
    private User staffUser;
    private User memberCoOwner;
    private User candidateCoOwner;
    private User nonMemberCoOwner;

    private String adminToken;
    private String staffToken;
    private String memberCoOwnerToken;
    private String nonMemberCoOwnerToken;

    private OwnershipGroup testGroup;

    private User createUser(String email, RoleName roleName) {
        Role role = roleRepository.findByName(roleName)
                .orElseGet(() -> roleRepository.save(new Role(roleName)));

        User user = new User();
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode("Password123!"));
        user.setFullName("User " + roleName.name());
        user.setIsActive(true);
        user.setRoles(new HashSet<>(Collections.singletonList(role)));
        return userRepository.saveAndFlush(user);
    }

    private Vehicle createVehicle(String vin, String plate) {
        Vehicle vehicle = new Vehicle();
        vehicle.setVin(vin);
        vehicle.setLicensePlate(plate);
        vehicle.setModelName("Tesla Model 3 Highland");
        vehicle.setManufacturer("Tesla");
        vehicle.setModel3dAssetPath("models/vehicles/tesla_model_3.glb");
        vehicle.setStatus(VehicleStatus.AVAILABLE);
        vehicle.setBatteryLevel(95);
        vehicle.setOdometerKm(BigDecimal.ZERO);
        vehicle.setStallLocationCode("BAY-04");
        return vehicleRepository.saveAndFlush(vehicle);
    }

    @BeforeEach
    void setUp() {
        String uid = UUID.randomUUID().toString().substring(0, 8);
        adminUser = createUser("admin_" + uid + "@evshare.vn", RoleName.ROLE_ADMIN);
        staffUser = createUser("staff_" + uid + "@evshare.vn", RoleName.ROLE_STAFF);
        memberCoOwner = createUser("member_" + uid + "@evshare.vn", RoleName.ROLE_CO_OWNER);
        candidateCoOwner = createUser("candidate_" + uid + "@evshare.vn", RoleName.ROLE_CO_OWNER);
        nonMemberCoOwner = createUser("nonmember_" + uid + "@evshare.vn", RoleName.ROLE_CO_OWNER);

        adminToken = tokenService.generateAccessToken(adminUser.getId(), adminUser.getEmail(), Collections.singletonList("ROLE_ADMIN"));
        staffToken = tokenService.generateAccessToken(staffUser.getId(), staffUser.getEmail(), Collections.singletonList("ROLE_STAFF"));
        memberCoOwnerToken = tokenService.generateAccessToken(memberCoOwner.getId(), memberCoOwner.getEmail(), Collections.singletonList("ROLE_CO_OWNER"));
        nonMemberCoOwnerToken = tokenService.generateAccessToken(nonMemberCoOwner.getId(), nonMemberCoOwner.getEmail(), Collections.singletonList("ROLE_CO_OWNER"));

        Vehicle vehicle = createVehicle("VIN_SHR_" + uid, "51G-" + (10000 + (int) (Math.random() * 89999)));

        OwnershipGroup group = new OwnershipGroup();
        group.setGroupName("Hanoi Green Mobility Syndicate");
        group.setVehicle(vehicle);
        group.setFormationDate(LocalDate.now());
        group.setIsActive(true);
        testGroup = ownershipGroupRepository.saveAndFlush(group);

        // Initial member share (74.50% so candidate issuance of 25.50% satisfies 100.00% invariant)
        OwnershipShare initialShare = new OwnershipShare();
        initialShare.setGroup(testGroup);
        initialShare.setUser(memberCoOwner);
        initialShare.setPercentage(new BigDecimal("74.50"));
        initialShare.setShareCertificateNumber("CERT-G" + testGroup.getId() + "-U" + memberCoOwner.getId() + "-INIT");
        initialShare.setIsActive(true);
        ownershipShareRepository.saveAndFlush(initialShare);
    }

    // =========================================================================
    // 1. VALID SHARE ISSUANCE
    // =========================================================================

    @Test
    @DisplayName("1.1. Admin issues valid equity share certificate with metadata")
    void testIssueValidShare() throws Exception {
        IssueOwnershipShareRequest request = new IssueOwnershipShareRequest(candidateCoOwner.getId(), new BigDecimal("25.50"));

        mockMvc.perform(post("/api/v1/ownership-groups/{groupId}/shares", testGroup.getId())
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.userId", is(candidateCoOwner.getId().intValue())))
                .andExpect(jsonPath("$.data.percentage", is(25.50)))
                .andExpect(jsonPath("$.data.shareCertificateNumber", notNullValue()))
                .andExpect(jsonPath("$.data.acquiredAt", notNullValue()))
                .andExpect(jsonPath("$.data.isActive", is(true)));

        Optional<OwnershipShare> fromDb = ownershipShareRepository.findByGroupIdAndUserId(testGroup.getId(), candidateCoOwner.getId());
        assertTrue(fromDb.isPresent());
        assertEquals(new BigDecimal("25.50"), fromDb.get().getPercentage());
    }

    // =========================================================================
    // 2. PERCENTAGE VALIDATION (NEVER ALLOW NEGATIVE OR INVALID PERCENTAGES)
    // =========================================================================

    @Test
    @DisplayName("2.1. Rejects negative ownership percentage with HTTP 400")
    void testRejectNegativePercentage() throws Exception {
        IssueOwnershipShareRequest request = new IssueOwnershipShareRequest(candidateCoOwner.getId(), new BigDecimal("-10.00"));

        mockMvc.perform(post("/api/v1/ownership-groups/{groupId}/shares", testGroup.getId())
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)));
    }

    @Test
    @DisplayName("2.2. Rejects zero ownership percentage with HTTP 400 (must be strictly > 0.00%)")
    void testRejectZeroPercentage() throws Exception {
        IssueOwnershipShareRequest request = new IssueOwnershipShareRequest(candidateCoOwner.getId(), new BigDecimal("0.00"));

        mockMvc.perform(post("/api/v1/ownership-groups/{groupId}/shares", testGroup.getId())
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)));
    }

    @Test
    @DisplayName("2.3. Rejects percentage exceeding 100.00% with HTTP 400")
    void testRejectExcessPercentage() throws Exception {
        IssueOwnershipShareRequest request = new IssueOwnershipShareRequest(candidateCoOwner.getId(), new BigDecimal("105.00"));

        mockMvc.perform(post("/api/v1/ownership-groups/{groupId}/shares", testGroup.getId())
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)));
    }

    @Test
    @DisplayName("2.4. Rejects percentage with more than 2 decimal places with HTTP 400")
    void testRejectExcessivePrecision() throws Exception {
        IssueOwnershipShareRequest request = new IssueOwnershipShareRequest(candidateCoOwner.getId(), new BigDecimal("15.125"));

        mockMvc.perform(post("/api/v1/ownership-groups/{groupId}/shares", testGroup.getId())
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)));
    }

    // =========================================================================
    // 3. TRANSACTIONAL UPDATES & ACTIVE/INACTIVE STATUS
    // =========================================================================

    @Test
    @DisplayName("3.1. Admin updates share percentage transactionally")
    void testUpdateSharePercentage() throws Exception {
        OwnershipShare share = ownershipShareRepository.findByGroupIdAndUserId(testGroup.getId(), memberCoOwner.getId()).orElseThrow();
        share.setPercentage(new BigDecimal("40.00"));
        ownershipShareRepository.saveAndFlush(share);

        OwnershipShare secondShare = new OwnershipShare();
        secondShare.setGroup(testGroup);
        secondShare.setUser(candidateCoOwner);
        secondShare.setPercentage(new BigDecimal("55.00"));
        secondShare.setShareCertificateNumber("CERT-G" + testGroup.getId() + "-U" + candidateCoOwner.getId() + "-SEC");
        secondShare.setIsActive(true);
        ownershipShareRepository.saveAndFlush(secondShare);

        UpdateOwnershipShareRequest updateReq = new UpdateOwnershipShareRequest(new BigDecimal("45.00"), null);
        mockMvc.perform(patch("/api/v1/ownership-groups/{groupId}/shares/{shareId}", testGroup.getId(), share.getId())
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.percentage", is(45.00)));

        OwnershipShare updated = ownershipShareRepository.findById(share.getId()).orElseThrow();
        assertEquals(new BigDecimal("45.00"), updated.getPercentage());
    }

    @Test
    @DisplayName("3.2. Deactivating share sets isActive to false")
    void testDeactivateShare() throws Exception {
        OwnershipShare share = ownershipShareRepository.findByGroupIdAndUserId(testGroup.getId(), memberCoOwner.getId()).orElseThrow();
        share.setPercentage(new BigDecimal("75.00"));
        ownershipShareRepository.saveAndFlush(share);

        OwnershipShare inactiveShare = new OwnershipShare();
        inactiveShare.setGroup(testGroup);
        inactiveShare.setUser(candidateCoOwner);
        inactiveShare.setPercentage(new BigDecimal("25.00"));
        inactiveShare.setShareCertificateNumber("CERT-G" + testGroup.getId() + "-U" + candidateCoOwner.getId() + "-INACT");
        inactiveShare.setIsActive(false);
        ownershipShareRepository.saveAndFlush(inactiveShare);

        mockMvc.perform(delete("/api/v1/ownership-groups/{groupId}/shares/{shareId}", testGroup.getId(), inactiveShare.getId())
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());

        OwnershipShare deactivated = ownershipShareRepository.findById(inactiveShare.getId()).orElseThrow();
        assertFalse(deactivated.getIsActive());

        // Reactivate share (75.00% + 25.00% = 100.00%)
        mockMvc.perform(post("/api/v1/ownership-groups/{groupId}/shares/{shareId}/reactivate", testGroup.getId(), inactiveShare.getId())
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());

        OwnershipShare reactivated = ownershipShareRepository.findById(inactiveShare.getId()).orElseThrow();
        assertTrue(reactivated.getIsActive());
    }

    // =========================================================================
    // 4. AUTHORIZATION & DATA-SCOPED ACCESS
    // =========================================================================

    @Test
    @DisplayName("4.1. Unauthenticated request to shares returns 401 Unauthorized")
    void testUnauthenticatedAccessRejected() throws Exception {
        mockMvc.perform(get("/api/v1/ownership-groups/{groupId}/shares", testGroup.getId()))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/api/v1/ownership-groups/{groupId}/shares", testGroup.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("4.2. Co-Owner cannot issue or update shares (Admin only, returns 403)")
    void testCoOwnerCannotIssueOrUpdateShares() throws Exception {
        IssueOwnershipShareRequest issueReq = new IssueOwnershipShareRequest(candidateCoOwner.getId(), new BigDecimal("20.00"));
        mockMvc.perform(post("/api/v1/ownership-groups/{groupId}/shares", testGroup.getId())
                        .header("Authorization", "Bearer " + memberCoOwnerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(issueReq)))
                .andExpect(status().isForbidden());

        OwnershipShare share = ownershipShareRepository.findByGroupIdAndUserId(testGroup.getId(), memberCoOwner.getId()).orElseThrow();
        UpdateOwnershipShareRequest updateReq = new UpdateOwnershipShareRequest(new BigDecimal("50.00"), null);
        mockMvc.perform(patch("/api/v1/ownership-groups/{groupId}/shares/{shareId}", testGroup.getId(), share.getId())
                        .header("Authorization", "Bearer " + memberCoOwnerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateReq)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("4.3. Non-member Co-Owner cannot view group shares (Ownership ACL 403)")
    void testNonMemberForbiddenByAcl() throws Exception {
        mockMvc.perform(get("/api/v1/ownership-groups/{groupId}/shares", testGroup.getId())
                        .header("Authorization", "Bearer " + nonMemberCoOwnerToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status", is(403)));
    }

    @Test
    @DisplayName("4.4. Enrolled Co-Owner can view shares of their own group")
    void testEnrolledCoOwnerCanViewShares() throws Exception {
        mockMvc.perform(get("/api/v1/ownership-groups/{groupId}/shares", testGroup.getId())
                        .header("Authorization", "Bearer " + memberCoOwnerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$.data[0].userId", is(memberCoOwner.getId().intValue())));
    }
}
