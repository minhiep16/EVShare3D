package com.example.evshare.controller;

import com.example.evshare.dto.request.IssueOwnershipShareRequest;
import com.example.evshare.dto.request.RebalanceSharesRequest;
import com.example.evshare.dto.request.TransferShareRequest;
import com.example.evshare.dto.request.UpdateOwnershipShareRequest;
import com.example.evshare.entity.AuditLog;
import com.example.evshare.entity.OwnershipGroup;
import com.example.evshare.entity.OwnershipShare;
import com.example.evshare.entity.Role;
import com.example.evshare.entity.User;
import com.example.evshare.entity.Vehicle;
import com.example.evshare.entity.enums.RoleName;
import com.example.evshare.entity.enums.VehicleStatus;
import com.example.evshare.repository.AuditLogRepository;
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
import java.time.Instant;
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
@DisplayName("Checkpoint 04-H — Ownership History & Audit Integrity Integration Tests")
class OwnershipHistoryIntegrationTest {

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
    private AuditLogRepository auditLogRepository;

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
    private OwnershipShare initialShare;

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
        adminUser = createUser("admin_hist_" + uid + "@evshare.vn", RoleName.ROLE_ADMIN);
        staffUser = createUser("staff_hist_" + uid + "@evshare.vn", RoleName.ROLE_STAFF);
        memberCoOwner = createUser("member_hist_" + uid + "@evshare.vn", RoleName.ROLE_CO_OWNER);
        candidateCoOwner = createUser("candidate_hist_" + uid + "@evshare.vn", RoleName.ROLE_CO_OWNER);
        nonMemberCoOwner = createUser("nonmember_hist_" + uid + "@evshare.vn", RoleName.ROLE_CO_OWNER);

        adminToken = tokenService.generateAccessToken(adminUser.getId(), adminUser.getEmail(), Collections.singletonList("ROLE_ADMIN"));
        staffToken = tokenService.generateAccessToken(staffUser.getId(), staffUser.getEmail(), Collections.singletonList("ROLE_STAFF"));
        memberCoOwnerToken = tokenService.generateAccessToken(memberCoOwner.getId(), memberCoOwner.getEmail(), Collections.singletonList("ROLE_CO_OWNER"));
        nonMemberCoOwnerToken = tokenService.generateAccessToken(nonMemberCoOwner.getId(), nonMemberCoOwner.getEmail(), Collections.singletonList("ROLE_CO_OWNER"));

        Vehicle vehicle = createVehicle("VIN_HIST_" + uid, "51H-" + (10000 + (int) (Math.random() * 89999)));

        OwnershipGroup group = new OwnershipGroup();
        group.setGroupName("Saigon EV Co-Owners Syndicate");
        group.setVehicle(vehicle);
        group.setFormationDate(LocalDate.now());
        group.setIsActive(true);
        testGroup = ownershipGroupRepository.saveAndFlush(group);

        // Initial member share (60.00%)
        initialShare = new OwnershipShare();
        initialShare.setGroup(testGroup);
        initialShare.setUser(memberCoOwner);
        initialShare.setPercentage(new BigDecimal("60.00"));
        initialShare.setShareCertificateNumber("CERT-G" + testGroup.getId() + "-U" + memberCoOwner.getId() + "-INIT");
        initialShare.setAcquiredAt(Instant.now());
        initialShare.setIsActive(true);
        initialShare = ownershipShareRepository.saveAndFlush(initialShare);
    }

    // =========================================================================
    // 1. AUDIT TRAIL LOGGING ON SHARE ISSUANCE
    // =========================================================================

    @Test
    @DisplayName("1.1. Issuing a new share records an immutable audit log with old_state_json as null")
    void testIssueShareAuditLog() throws Exception {
        IssueOwnershipShareRequest request = new IssueOwnershipShareRequest(candidateCoOwner.getId(), new BigDecimal("40.00"));

        mockMvc.perform(post("/api/v1/ownership-groups/{groupId}/shares", testGroup.getId())
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.percentage", is(40.00)));

        OwnershipShare issuedShare = ownershipShareRepository.findByGroupIdAndUserId(testGroup.getId(), candidateCoOwner.getId()).orElseThrow();

        List<AuditLog> logs = auditLogRepository.findByEntityNameAndEntityIdOrderByIdDesc("OwnershipShare", issuedShare.getId());
        assertFalse(logs.isEmpty(), "Audit log must be created upon share issuance");

        AuditLog log = logs.get(0);
        assertEquals("ISSUE_SHARE", log.getAction());
        assertEquals("OwnershipShare", log.getEntityName());
        assertEquals(issuedShare.getId(), log.getEntityId());
        assertNull(log.getOldStateJson(), "old_state_json must be null for first-time issuance");
        assertNotNull(log.getNewStateJson(), "new_state_json must capture post-issuance state");
        assertTrue(log.getNewStateJson().contains("40.00"), "new_state_json must contain issued percentage");
        assertEquals(adminUser.getId(), log.getUser().getId(), "Acting user must be the authenticated admin");
        assertNotNull(log.getCreatedAt(), "Effective timestamp must be recorded");
    }

    // =========================================================================
    // 2. HISTORICAL INTEGRITY & NO SILENT OVERWRITES
    // =========================================================================

    @Test
    @DisplayName("2.1. Updating share percentage does not silently overwrite history and records prior state")
    void testUpdateShareAuditPreservesPriorState() throws Exception {
        // Complete 100% allocation
        OwnershipShare secondShare = new OwnershipShare();
        secondShare.setGroup(testGroup);
        secondShare.setUser(candidateCoOwner);
        secondShare.setPercentage(new BigDecimal("40.00"));
        secondShare.setShareCertificateNumber("CERT-G" + testGroup.getId() + "-U" + candidateCoOwner.getId() + "-SEC");
        secondShare.setAcquiredAt(Instant.now().minusSeconds(3600));
        secondShare.setIsActive(true);
        secondShare = ownershipShareRepository.saveAndFlush(secondShare);

        String originalCert = secondShare.getShareCertificateNumber();

        // Update candidate share to 40.00% (simulate modifying details or rebalancing)
        UpdateOwnershipShareRequest updateReq = new UpdateOwnershipShareRequest(new BigDecimal("40.00"), true);
        mockMvc.perform(patch("/api/v1/ownership-groups/{groupId}/shares/{shareId}", testGroup.getId(), secondShare.getId())
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateReq)))
                .andExpect(status().isOk());

        List<AuditLog> logs = auditLogRepository.findByEntityNameAndEntityIdOrderByIdDesc("OwnershipShare", secondShare.getId());
        assertFalse(logs.isEmpty(), "Update must record audit log");

        AuditLog updateLog = logs.get(0);
        assertEquals("UPDATE_SHARE", updateLog.getAction());
        assertNotNull(updateLog.getOldStateJson(), "old_state_json must preserve prior snapshot");
        assertNotNull(updateLog.getNewStateJson(), "new_state_json must preserve current snapshot");
        assertTrue(updateLog.getOldStateJson().contains(originalCert), "Prior certificate must be preserved in historical record");
    }

    // =========================================================================
    // 3. DUAL AUDIT TRAILS ON EQUITY TRANSFERS
    // =========================================================================

    @Test
    @DisplayName("3.1. Equity transfer generates dual audit trails for seller and buyer with effective dates")
    void testTransferShareGeneratesDualAuditTrails() throws Exception {
        // Complete 100% distribution: 60% member, 40% candidate
        OwnershipShare candidateShare = new OwnershipShare();
        candidateShare.setGroup(testGroup);
        candidateShare.setUser(candidateCoOwner);
        candidateShare.setPercentage(new BigDecimal("40.00"));
        candidateShare.setShareCertificateNumber("CERT-G" + testGroup.getId() + "-U" + candidateCoOwner.getId() + "-INIT");
        candidateShare.setAcquiredAt(Instant.now());
        candidateShare.setIsActive(true);
        candidateShare = ownershipShareRepository.saveAndFlush(candidateShare);

        // Candidate transfers 15.00% to nonMemberCoOwner
        TransferShareRequest transferReq = new TransferShareRequest(
                candidateCoOwner.getId(),
                nonMemberCoOwner.getId(),
                new BigDecimal("15.00")
        );

        mockMvc.perform(post("/api/v1/ownership-groups/{id}/transfer-share", testGroup.getId())
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(transferReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)));

        // 1. Check seller audit trail
        List<AuditLog> sellerLogs = auditLogRepository.findByEntityNameAndEntityIdOrderByIdDesc("OwnershipShare", candidateShare.getId());
        assertFalse(sellerLogs.isEmpty(), "Seller must have audit log");
        AuditLog sellerLog = sellerLogs.get(0);
        assertEquals("TRANSFER_EQUITY_OUT", sellerLog.getAction());
        assertTrue(sellerLog.getOldStateJson().contains("40.00"), "Seller old state must have 40.00%");
        assertTrue(sellerLog.getNewStateJson().contains("25.00"), "Seller new state must have 25.00%");

        // 2. Check buyer audit trail
        OwnershipShare buyerShare = ownershipShareRepository.findByGroupIdAndUserId(testGroup.getId(), nonMemberCoOwner.getId()).orElseThrow();
        List<AuditLog> buyerLogs = auditLogRepository.findByEntityNameAndEntityIdOrderByIdDesc("OwnershipShare", buyerShare.getId());
        assertFalse(buyerLogs.isEmpty(), "Buyer must have audit log");
        AuditLog buyerLog = buyerLogs.get(0);
        assertEquals("TRANSFER_EQUITY_IN", buyerLog.getAction());
        assertNull(buyerLog.getOldStateJson(), "First-time buyer old state is null");
        assertTrue(buyerLog.getNewStateJson().contains("15.00"), "Buyer new state must have 15.00%");
    }

    // =========================================================================
    // 4. DEACTIVATION & REACTIVATION AUDIT TRAILS
    // =========================================================================

    @Test
    @DisplayName("4.1. Deactivating and reactivating a share records status transition audit trails")
    void testDeactivateAndReactivateAuditTrails() throws Exception {
        OwnershipShare inactiveShare = new OwnershipShare();
        inactiveShare.setGroup(testGroup);
        inactiveShare.setUser(candidateCoOwner);
        inactiveShare.setPercentage(new BigDecimal("40.00"));
        inactiveShare.setShareCertificateNumber("CERT-G" + testGroup.getId() + "-U" + candidateCoOwner.getId() + "-INACT");
        inactiveShare.setIsActive(false);
        inactiveShare = ownershipShareRepository.saveAndFlush(inactiveShare);

        // Initial member holds 100%
        initialShare.setPercentage(new BigDecimal("100.00"));
        ownershipShareRepository.saveAndFlush(initialShare);

        // Deactivating inactiveShare (0% active impact)
        mockMvc.perform(delete("/api/v1/ownership-groups/{groupId}/shares/{shareId}", testGroup.getId(), inactiveShare.getId())
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());

        List<AuditLog> deactLogs = auditLogRepository.findByEntityNameAndEntityIdOrderByIdDesc("OwnershipShare", inactiveShare.getId());
        assertFalse(deactLogs.isEmpty());
        assertEquals("DEACTIVATE_SHARE", deactLogs.get(0).getAction());

        // Prepare for reactivation: initial member to 60.00%, inactiveShare has 40.00%
        initialShare.setPercentage(new BigDecimal("60.00"));
        ownershipShareRepository.saveAndFlush(initialShare);

        mockMvc.perform(post("/api/v1/ownership-groups/{groupId}/shares/{shareId}/reactivate", testGroup.getId(), inactiveShare.getId())
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());

        List<AuditLog> reactLogs = auditLogRepository.findByEntityNameAndEntityIdOrderByIdDesc("OwnershipShare", inactiveShare.getId());
        assertEquals("REACTIVATE_SHARE", reactLogs.get(0).getAction());
        assertTrue(reactLogs.get(0).getNewStateJson().contains("\"isActive\":true"));
    }

    // =========================================================================
    // 5. ATOMIC REBALANCE AUDIT TRAILS
    // =========================================================================

    @Test
    @DisplayName("5.1. Atomic rebalance creates audit logs for all modified and deactivated shares")
    void testRebalanceCreatesAuditLogs() throws Exception {
        // Current: member holds 60.00%. Let's create second member with 40.00%.
        OwnershipShare secondShare = new OwnershipShare();
        secondShare.setGroup(testGroup);
        secondShare.setUser(candidateCoOwner);
        secondShare.setPercentage(new BigDecimal("40.00"));
        secondShare.setShareCertificateNumber("CERT-G" + testGroup.getId() + "-U" + candidateCoOwner.getId() + "-REB");
        secondShare.setAcquiredAt(Instant.now());
        secondShare.setIsActive(true);
        secondShare = ownershipShareRepository.saveAndFlush(secondShare);

        // Rebalance to: member 50%, candidate 50%
        RebalanceSharesRequest rebalanceReq = new RebalanceSharesRequest(Arrays.asList(
                new RebalanceSharesRequest.ShareAllocation(memberCoOwner.getId(), new BigDecimal("50.00")),
                new RebalanceSharesRequest.ShareAllocation(candidateCoOwner.getId(), new BigDecimal("50.00"))
        ));

        mockMvc.perform(post("/api/v1/ownership-groups/{groupId}/shares/rebalance", testGroup.getId())
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(rebalanceReq)))
                .andExpect(status().isOk());

        List<AuditLog> memberLogs = auditLogRepository.findByEntityNameAndEntityIdOrderByIdDesc("OwnershipShare", initialShare.getId());
        assertFalse(memberLogs.isEmpty());
        assertEquals("REBALANCE_SHARE", memberLogs.get(0).getAction());
        assertTrue(memberLogs.get(0).getOldStateJson().contains("60.00"));
        assertTrue(memberLogs.get(0).getNewStateJson().contains("50.00"));

        List<AuditLog> candidateLogs = auditLogRepository.findByEntityNameAndEntityIdOrderByIdDesc("OwnershipShare", secondShare.getId());
        assertFalse(candidateLogs.isEmpty());
        assertEquals("REBALANCE_SHARE", candidateLogs.get(0).getAction());
        assertTrue(candidateLogs.get(0).getOldStateJson().contains("40.00"));
        assertTrue(candidateLogs.get(0).getNewStateJson().contains("50.00"));
    }

    // =========================================================================
    // 6. HISTORY RETRIEVAL ENDPOINTS & RBAC ACCESS
    // =========================================================================

    @Test
    @DisplayName("6.1. GET /shares/{shareId}/history returns chronological audit records with previous & new values")
    void testGetSingleShareHistory() throws Exception {
        IssueOwnershipShareRequest request = new IssueOwnershipShareRequest(candidateCoOwner.getId(), new BigDecimal("40.00"));

        mockMvc.perform(post("/api/v1/ownership-groups/{groupId}/shares", testGroup.getId())
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        OwnershipShare issuedShare = ownershipShareRepository.findByGroupIdAndUserId(testGroup.getId(), candidateCoOwner.getId()).orElseThrow();

        mockMvc.perform(get("/api/v1/ownership-groups/{groupId}/shares/{shareId}/history", testGroup.getId(), issuedShare.getId())
                        .header("Authorization", "Bearer " + memberCoOwnerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$.data[0].action", is("ISSUE_SHARE")))
                .andExpect(jsonPath("$.data[0].newPercentage", is(40.00)))
                .andExpect(jsonPath("$.data[0].effectiveDate").exists());
    }

    @Test
    @DisplayName("6.2. GET /shares/history returns complete consolidated group ownership audit history")
    void testGetGroupOwnershipHistory() throws Exception {
        IssueOwnershipShareRequest request = new IssueOwnershipShareRequest(candidateCoOwner.getId(), new BigDecimal("40.00"));

        mockMvc.perform(post("/api/v1/ownership-groups/{groupId}/shares", testGroup.getId())
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/v1/ownership-groups/{groupId}/shares/history", testGroup.getId())
                        .header("Authorization", "Bearer " + memberCoOwnerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data", hasSize(greaterThanOrEqualTo(1))));
    }

    @Test
    @DisplayName("6.3. Non-member co-owner is denied access (HTTP 403) to group ownership history")
    void testNonMemberDeniedHistoryAccess() throws Exception {
        mockMvc.perform(get("/api/v1/ownership-groups/{groupId}/shares/history", testGroup.getId())
                        .header("Authorization", "Bearer " + nonMemberCoOwnerToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("6.4. Unauthenticated request to history returns HTTP 401 Unauthorized")
    void testUnauthenticatedHistoryAccess() throws Exception {
        mockMvc.perform(get("/api/v1/ownership-groups/{groupId}/shares/history", testGroup.getId()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("6.5. Platform Staff has access (HTTP 200) to group ownership history")
    void testStaffCanAccessHistory() throws Exception {
        mockMvc.perform(get("/api/v1/ownership-groups/{groupId}/shares/history", testGroup.getId())
                        .header("Authorization", "Bearer " + staffToken))
                .andExpect(status().isOk());
    }
}
