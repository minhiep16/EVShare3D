package com.example.evshare.controller;

import com.example.evshare.dto.request.TransitionContractStatusRequest;
import com.example.evshare.entity.AuditLog;
import com.example.evshare.entity.CoOwnershipContract;
import com.example.evshare.entity.OwnershipGroup;
import com.example.evshare.entity.OwnershipShare;
import com.example.evshare.entity.Role;
import com.example.evshare.entity.User;
import com.example.evshare.entity.Vehicle;
import com.example.evshare.entity.enums.ContractStatus;
import com.example.evshare.entity.enums.RoleName;
import com.example.evshare.entity.enums.VehicleStatus;
import com.example.evshare.repository.AuditLogRepository;
import com.example.evshare.repository.CoOwnershipContractRepository;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@DisplayName("Checkpoint 04-K — Contract Lifecycle Integration Tests")
class ContractLifecycleIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private CoOwnershipContractRepository coOwnershipContractRepository;

    @Autowired
    private OwnershipGroupRepository ownershipGroupRepository;

    @Autowired
    private OwnershipShareRepository ownershipShareRepository;

    @Autowired
    private AuditLogRepository auditLogRepository;

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
    private User coOwnerUser;

    private String adminToken;
    private String staffToken;
    private String coOwnerToken;

    private OwnershipGroup testGroup;

    private User createUser(String email, RoleName roleName) {
        Role role = roleRepository.findByName(roleName)
                .orElseGet(() -> roleRepository.save(new Role(roleName)));

        User user = new User();
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode("Password123!"));
        user.setFullName("User " + email);
        user.setIsActive(true);
        user.setRoles(new HashSet<>(Collections.singletonList(role)));
        return userRepository.saveAndFlush(user);
    }

    private Vehicle createVehicle(String vin, String plate) {
        Vehicle vehicle = new Vehicle();
        vehicle.setVin(vin);
        vehicle.setLicensePlate(plate);
        vehicle.setModelName("VinFast VF 8 Plus");
        vehicle.setManufacturer("VinFast");
        vehicle.setModel3dAssetPath("models/vehicles/vinfast_vf8.glb");
        vehicle.setStatus(VehicleStatus.AVAILABLE);
        vehicle.setBatteryLevel(98);
        vehicle.setOdometerKm(BigDecimal.ZERO);
        vehicle.setStallLocationCode("BAY-02");
        return vehicleRepository.saveAndFlush(vehicle);
    }

    private OwnershipGroup createGroup(Vehicle vehicle) {
        OwnershipGroup group = new OwnershipGroup();
        group.setGroupName("Syndicate " + UUID.randomUUID().toString().substring(0, 8));
        group.setVehicle(vehicle);
        group.setFormationDate(LocalDate.now());
        group.setIsActive(true);
        return ownershipGroupRepository.saveAndFlush(group);
    }

    private CoOwnershipContract createContract(OwnershipGroup group, int version, ContractStatus status) {
        CoOwnershipContract contract = new CoOwnershipContract();
        contract.setGroup(group);
        contract.setContractTitle("Syndicate Co-Ownership Agreement v" + version);
        contract.setContractTermsText("# Agreement Terms\n\n1. Equity shares govern asset rights.");
        contract.setVersion(version);
        contract.setStatus(status);
        contract.setEffectiveDate(LocalDate.now());
        contract.setExpiryDate(LocalDate.now().plusYears(1));
        return coOwnershipContractRepository.saveAndFlush(contract);
    }

    @BeforeEach
    void setUp() {
        String uid = UUID.randomUUID().toString().substring(0, 8);
        adminUser = createUser("admin_lc_" + uid + "@evshare.vn", RoleName.ROLE_ADMIN);
        staffUser = createUser("staff_lc_" + uid + "@evshare.vn", RoleName.ROLE_STAFF);
        coOwnerUser = createUser("coowner_lc_" + uid + "@evshare.vn", RoleName.ROLE_CO_OWNER);

        adminToken = tokenService.generateAccessToken(adminUser);
        staffToken = tokenService.generateAccessToken(staffUser);
        coOwnerToken = tokenService.generateAccessToken(coOwnerUser);

        Vehicle vehicle = createVehicle("VIN-LC-" + uid, "30E-" + uid.toUpperCase());
        testGroup = createGroup(vehicle);

        OwnershipShare share = new OwnershipShare();
        share.setGroup(testGroup);
        share.setUser(coOwnerUser);
        share.setPercentage(new BigDecimal("100.00"));
        share.setShareCertificateNumber("CERT-LC-" + uid);
        share.setIsActive(true);
        share.setAcquiredAt(Instant.now());
        ownershipShareRepository.saveAndFlush(share);
    }

    // =========================================================================
    // 1. CONTROLLED VALID TRANSITIONS
    // =========================================================================

    @Test
    @DisplayName("1.1. Verify DRAFT -> PENDING_SIGNATURE")
    void shouldTransition_DraftToPendingSignature() throws Exception {
        CoOwnershipContract contract = createContract(testGroup, 1, ContractStatus.DRAFT);

        mockMvc.perform(patch("/api/v1/contracts/{id}/status", contract.getId())
                        .header("Authorization", "Bearer " + staffToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new TransitionContractStatusRequest(ContractStatus.PENDING_SIGNATURE, "Finalized terms"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.status", is("PENDING_SIGNATURE")));

        CoOwnershipContract updated = coOwnershipContractRepository.findById(contract.getId()).orElseThrow();
        assertEquals(ContractStatus.PENDING_SIGNATURE, updated.getStatus());
    }

    @Test
    @DisplayName("1.2. Verify PENDING_SIGNATURE -> SIGNED")
    void shouldTransition_PendingSignatureToSigned() throws Exception {
        CoOwnershipContract contract = createContract(testGroup, 1, ContractStatus.PENDING_SIGNATURE);

        mockMvc.perform(patch("/api/v1/contracts/{id}/status", contract.getId())
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new TransitionContractStatusRequest(ContractStatus.SIGNED, "All members signed"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.status", is("SIGNED")));

        CoOwnershipContract updated = coOwnershipContractRepository.findById(contract.getId()).orElseThrow();
        assertEquals(ContractStatus.SIGNED, updated.getStatus());
    }

    @Test
    @DisplayName("1.3. Verify SIGNED -> ACTIVE")
    void shouldTransition_SignedToActive() throws Exception {
        CoOwnershipContract contract = createContract(testGroup, 1, ContractStatus.SIGNED);

        mockMvc.perform(patch("/api/v1/contracts/{id}/status", contract.getId())
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new TransitionContractStatusRequest(ContractStatus.ACTIVE, "Activated agreement"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.status", is("ACTIVE")))
                .andExpect(jsonPath("$.data.effectiveDate", notNullValue()));

        CoOwnershipContract updated = coOwnershipContractRepository.findById(contract.getId()).orElseThrow();
        assertEquals(ContractStatus.ACTIVE, updated.getStatus());
    }

    @Test
    @DisplayName("1.4. Verify ACTIVE -> EXPIRED")
    void shouldTransition_ActiveToExpired() throws Exception {
        CoOwnershipContract contract = createContract(testGroup, 1, ContractStatus.ACTIVE);

        mockMvc.perform(patch("/api/v1/contracts/{id}/status", contract.getId())
                        .header("Authorization", "Bearer " + staffToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new TransitionContractStatusRequest(ContractStatus.EXPIRED, "Contract tenure concluded"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.status", is("EXPIRED")));

        CoOwnershipContract updated = coOwnershipContractRepository.findById(contract.getId()).orElseThrow();
        assertEquals(ContractStatus.EXPIRED, updated.getStatus());
    }

    @Test
    @DisplayName("1.5. Verify ACTIVE -> TERMINATED")
    void shouldTransition_ActiveToTerminated() throws Exception {
        CoOwnershipContract contract = createContract(testGroup, 1, ContractStatus.ACTIVE);

        mockMvc.perform(patch("/api/v1/contracts/{id}/status", contract.getId())
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new TransitionContractStatusRequest(ContractStatus.TERMINATED, "Vehicle sold"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.status", is("TERMINATED")));

        CoOwnershipContract updated = coOwnershipContractRepository.findById(contract.getId()).orElseThrow();
        assertEquals(ContractStatus.TERMINATED, updated.getStatus());
    }

    @Test
    @DisplayName("1.6. Verify Early Cancellations: DRAFT -> TERMINATED, PENDING_SIGNATURE -> TERMINATED, SIGNED -> TERMINATED")
    void shouldSupportEarlyTerminations() throws Exception {
        CoOwnershipContract draft = createContract(testGroup, 1, ContractStatus.DRAFT);
        CoOwnershipContract pending = createContract(testGroup, 2, ContractStatus.PENDING_SIGNATURE);
        CoOwnershipContract signed = createContract(testGroup, 3, ContractStatus.SIGNED);

        // DRAFT -> TERMINATED
        mockMvc.perform(patch("/api/v1/contracts/{id}/status", draft.getId())
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new TransitionContractStatusRequest(ContractStatus.TERMINATED, "Cancelled draft"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status", is("TERMINATED")));

        // PENDING_SIGNATURE -> TERMINATED
        mockMvc.perform(patch("/api/v1/contracts/{id}/status", pending.getId())
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new TransitionContractStatusRequest(ContractStatus.TERMINATED, "Cancelled proposal"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status", is("TERMINATED")));

        // SIGNED -> TERMINATED
        mockMvc.perform(patch("/api/v1/contracts/{id}/status", signed.getId())
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new TransitionContractStatusRequest(ContractStatus.TERMINATED, "Aborted before activation"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status", is("TERMINATED")));
    }

    @Test
    @DisplayName("1.7. Verify Member Rejection: PENDING_SIGNATURE -> REJECTED")
    void shouldTransition_PendingSignatureToRejected() throws Exception {
        CoOwnershipContract contract = createContract(testGroup, 1, ContractStatus.PENDING_SIGNATURE);

        mockMvc.perform(patch("/api/v1/contracts/{id}/status", contract.getId())
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new TransitionContractStatusRequest(ContractStatus.REJECTED, "Members voted down terms"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status", is("REJECTED")));

        CoOwnershipContract updated = coOwnershipContractRepository.findById(contract.getId()).orElseThrow();
        assertEquals(ContractStatus.REJECTED, updated.getStatus());
    }

    // =========================================================================
    // 2. REJECT INVALID TRANSITIONS (HTTP 409 CONFLICT)
    // =========================================================================

    @Test
    @DisplayName("2.1. Reject DRAFT -> ACTIVE and DRAFT -> SIGNED")
    void shouldRejectInvalidTransitions_FromDraft() throws Exception {
        CoOwnershipContract contract = createContract(testGroup, 1, ContractStatus.DRAFT);

        mockMvc.perform(patch("/api/v1/contracts/{id}/status", contract.getId())
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new TransitionContractStatusRequest(ContractStatus.ACTIVE))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message", containsString("Cannot transition contract from status 'DRAFT' to 'ACTIVE'")));

        mockMvc.perform(patch("/api/v1/contracts/{id}/status", contract.getId())
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new TransitionContractStatusRequest(ContractStatus.SIGNED))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message", containsString("Cannot transition contract from status 'DRAFT' to 'SIGNED'")));
    }

    @Test
    @DisplayName("2.2. Reject PENDING_SIGNATURE -> DRAFT and PENDING_SIGNATURE -> ACTIVE")
    void shouldRejectInvalidTransitions_FromPendingSignature() throws Exception {
        CoOwnershipContract contract = createContract(testGroup, 1, ContractStatus.PENDING_SIGNATURE);

        mockMvc.perform(patch("/api/v1/contracts/{id}/status", contract.getId())
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new TransitionContractStatusRequest(ContractStatus.DRAFT))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message", containsString("Cannot transition contract from status 'PENDING_SIGNATURE' to 'DRAFT'")));

        mockMvc.perform(patch("/api/v1/contracts/{id}/status", contract.getId())
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new TransitionContractStatusRequest(ContractStatus.ACTIVE))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message", containsString("Cannot transition contract from status 'PENDING_SIGNATURE' to 'ACTIVE'")));
    }

    @Test
    @DisplayName("2.3. Reject SIGNED -> DRAFT and SIGNED -> PENDING_SIGNATURE")
    void shouldRejectInvalidTransitions_FromSigned() throws Exception {
        CoOwnershipContract contract = createContract(testGroup, 1, ContractStatus.SIGNED);

        mockMvc.perform(patch("/api/v1/contracts/{id}/status", contract.getId())
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new TransitionContractStatusRequest(ContractStatus.DRAFT))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message", containsString("Cannot transition contract from status 'SIGNED' to 'DRAFT'")));

        mockMvc.perform(patch("/api/v1/contracts/{id}/status", contract.getId())
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new TransitionContractStatusRequest(ContractStatus.PENDING_SIGNATURE))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message", containsString("Cannot transition contract from status 'SIGNED' to 'PENDING_SIGNATURE'")));
    }

    @Test
    @DisplayName("2.4. Reject ACTIVE -> DRAFT and ACTIVE -> SIGNED")
    void shouldRejectInvalidTransitions_FromActive() throws Exception {
        CoOwnershipContract contract = createContract(testGroup, 1, ContractStatus.ACTIVE);

        mockMvc.perform(patch("/api/v1/contracts/{id}/status", contract.getId())
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new TransitionContractStatusRequest(ContractStatus.DRAFT))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message", containsString("Cannot transition contract from status 'ACTIVE' to 'DRAFT'")));

        mockMvc.perform(patch("/api/v1/contracts/{id}/status", contract.getId())
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new TransitionContractStatusRequest(ContractStatus.SIGNED))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message", containsString("Cannot transition contract from status 'ACTIVE' to 'SIGNED'")));
    }

    @Test
    @DisplayName("2.5. Reject outbound transitions from terminal states: EXPIRED, TERMINATED, REJECTED")
    void shouldRejectTransitions_FromTerminalStates() throws Exception {
        CoOwnershipContract expired = createContract(testGroup, 1, ContractStatus.EXPIRED);
        CoOwnershipContract terminated = createContract(testGroup, 2, ContractStatus.TERMINATED);
        CoOwnershipContract rejected = createContract(testGroup, 3, ContractStatus.REJECTED);

        mockMvc.perform(patch("/api/v1/contracts/{id}/status", expired.getId())
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new TransitionContractStatusRequest(ContractStatus.ACTIVE))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message", containsString("Cannot transition contract from status 'EXPIRED' to 'ACTIVE'")));

        mockMvc.perform(patch("/api/v1/contracts/{id}/status", terminated.getId())
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new TransitionContractStatusRequest(ContractStatus.DRAFT))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message", containsString("Cannot transition contract from status 'TERMINATED' to 'DRAFT'")));

        mockMvc.perform(patch("/api/v1/contracts/{id}/status", rejected.getId())
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new TransitionContractStatusRequest(ContractStatus.PENDING_SIGNATURE))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message", containsString("Cannot transition contract from status 'REJECTED' to 'PENDING_SIGNATURE'")));
    }

    @Test
    @DisplayName("2.6. Reject redundant self-transitions: ACTIVE -> ACTIVE")
    void shouldRejectRedundantSelfTransitions() throws Exception {
        CoOwnershipContract contract = createContract(testGroup, 1, ContractStatus.ACTIVE);

        mockMvc.perform(patch("/api/v1/contracts/{id}/status", contract.getId())
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new TransitionContractStatusRequest(ContractStatus.ACTIVE))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message", containsString("already in status 'ACTIVE'")));
    }

    // =========================================================================
    // 3. AUDIT HISTORY PRESERVATION & SUPERSEDING TERMINATION
    // =========================================================================

    @Test
    @DisplayName("3.1. Verify audit logs record old and new state snapshots on status transitions")
    void shouldPreserveAuditHistory_OnStatusTransitions() throws Exception {
        CoOwnershipContract contract = createContract(testGroup, 1, ContractStatus.DRAFT);

        mockMvc.perform(patch("/api/v1/contracts/{id}/status", contract.getId())
                        .header("Authorization", "Bearer " + staffToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new TransitionContractStatusRequest(ContractStatus.PENDING_SIGNATURE, "Final draft terms"))))
                .andExpect(status().isOk());

        List<AuditLog> auditLogs = auditLogRepository.findByEntityNameAndEntityIdInOrderByIdDesc(
                "CoOwnershipContract", Collections.singletonList(contract.getId()));

        assertFalse(auditLogs.isEmpty());
        AuditLog latestLog = auditLogs.get(0);
        assertEquals("TRANSITION_CONTRACT_STATUS", latestLog.getAction());
        assertNotNull(latestLog.getOldStateJson());
        assertNotNull(latestLog.getNewStateJson());
        assertTrue(latestLog.getOldStateJson().contains("\"status\":\"DRAFT\""));
        assertTrue(latestLog.getNewStateJson().contains("\"status\":\"PENDING_SIGNATURE\""));
    }

    @Test
    @DisplayName("3.2. Single Active Contract: Activating v2 automatically terminates v1 and logs audit")
    void shouldTerminatePreviousActiveContract_WhenActivatingNewVersion() throws Exception {
        CoOwnershipContract v1 = createContract(testGroup, 1, ContractStatus.ACTIVE);
        CoOwnershipContract v2 = createContract(testGroup, 2, ContractStatus.SIGNED);

        mockMvc.perform(patch("/api/v1/contracts/{id}/status", v2.getId())
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new TransitionContractStatusRequest(ContractStatus.ACTIVE, "Activate version 2"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status", is("ACTIVE")))
                .andExpect(jsonPath("$.data.version", is(2)));

        CoOwnershipContract updatedV1 = coOwnershipContractRepository.findById(v1.getId()).orElseThrow();
        assertEquals(ContractStatus.TERMINATED, updatedV1.getStatus());

        List<AuditLog> v1Logs = auditLogRepository.findByEntityNameAndEntityIdInOrderByIdDesc(
                "CoOwnershipContract", Collections.singletonList(v1.getId()));
        assertTrue(v1Logs.stream().anyMatch(l -> "TERMINATE_CONTRACT".equals(l.getAction())));
    }

    // =========================================================================
    // 4. RBAC AUTHORIZATION ENFORCEMENT
    // =========================================================================

    @Test
    @DisplayName("4.1. Co-owner cannot execute manual status transition (HTTP 403 Forbidden)")
    void shouldDenyStatusTransition_ForCoOwner() throws Exception {
        CoOwnershipContract contract = createContract(testGroup, 1, ContractStatus.DRAFT);

        mockMvc.perform(patch("/api/v1/contracts/{id}/status", contract.getId())
                        .header("Authorization", "Bearer " + coOwnerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new TransitionContractStatusRequest(ContractStatus.PENDING_SIGNATURE))))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("4.2. Unauthenticated status transition request rejected with HTTP 401 Unauthorized")
    void shouldRejectStatusTransition_WhenUnauthenticated() throws Exception {
        CoOwnershipContract contract = createContract(testGroup, 1, ContractStatus.DRAFT);

        mockMvc.perform(patch("/api/v1/contracts/{id}/status", contract.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new TransitionContractStatusRequest(ContractStatus.PENDING_SIGNATURE))))
                .andExpect(status().isUnauthorized());
    }
}
