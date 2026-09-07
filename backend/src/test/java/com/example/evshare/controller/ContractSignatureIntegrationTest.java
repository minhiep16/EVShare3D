package com.example.evshare.controller;

import com.example.evshare.dto.request.SignContractRequest;
import com.example.evshare.entity.AuditLog;
import com.example.evshare.entity.CoOwnershipContract;
import com.example.evshare.entity.ContractSignature;
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
import com.example.evshare.repository.ContractSignatureRepository;
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
@DisplayName("Checkpoint 04-J — Contract Signature Integration Tests")
class ContractSignatureIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private CoOwnershipContractRepository coOwnershipContractRepository;

    @Autowired
    private ContractSignatureRepository contractSignatureRepository;

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
    private User coOwnerA;
    private User coOwnerB;
    private User nonMemberCoOwner;

    private String adminToken;
    private String staffToken;
    private String tokenA;
    private String tokenB;
    private String nonMemberToken;

    private OwnershipGroup testGroup;
    private CoOwnershipContract pendingContract;

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
        vehicle.setModelName("VinFast VF 9 Plus");
        vehicle.setManufacturer("VinFast");
        vehicle.setModel3dAssetPath("models/vehicles/vinfast_vf9.glb");
        vehicle.setStatus(VehicleStatus.AVAILABLE);
        vehicle.setBatteryLevel(95);
        vehicle.setOdometerKm(new BigDecimal("1200.00"));
        vehicle.setStallLocationCode("BAY-01");
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

    private OwnershipShare createShare(OwnershipGroup group, User user, BigDecimal percentage, String cert) {
        OwnershipShare share = new OwnershipShare();
        share.setGroup(group);
        share.setUser(user);
        share.setPercentage(percentage);
        share.setShareCertificateNumber(cert);
        share.setIsActive(true);
        share.setAcquiredAt(Instant.now());
        return ownershipShareRepository.saveAndFlush(share);
    }

    private CoOwnershipContract createContract(OwnershipGroup group, int version, ContractStatus status) {
        CoOwnershipContract contract = new CoOwnershipContract();
        contract.setGroup(group);
        contract.setContractTitle("Master Syndicate Agreement v" + version);
        contract.setContractTermsText("# Syndicate Terms\n\nAll members agree to pro-rata operating terms.");
        contract.setVersion(version);
        contract.setStatus(status);
        contract.setEffectiveDate(LocalDate.now());
        contract.setExpiryDate(LocalDate.now().plusYears(1));
        return coOwnershipContractRepository.saveAndFlush(contract);
    }

    @BeforeEach
    void setUp() {
        String uid = UUID.randomUUID().toString().substring(0, 8);
        adminUser = createUser("admin_sig_" + uid + "@evshare.vn", RoleName.ROLE_ADMIN);
        staffUser = createUser("staff_sig_" + uid + "@evshare.vn", RoleName.ROLE_STAFF);
        coOwnerA = createUser("coowner_a_" + uid + "@evshare.vn", RoleName.ROLE_CO_OWNER);
        coOwnerB = createUser("coowner_b_" + uid + "@evshare.vn", RoleName.ROLE_CO_OWNER);
        nonMemberCoOwner = createUser("nonmember_" + uid + "@evshare.vn", RoleName.ROLE_CO_OWNER);

        adminToken = tokenService.generateAccessToken(adminUser);
        staffToken = tokenService.generateAccessToken(staffUser);
        tokenA = tokenService.generateAccessToken(coOwnerA);
        tokenB = tokenService.generateAccessToken(coOwnerB);
        nonMemberToken = tokenService.generateAccessToken(nonMemberCoOwner);

        Vehicle vehicle = createVehicle("VIN-SIG-" + uid, "30A-" + uid.toUpperCase());
        testGroup = createGroup(vehicle);

        createShare(testGroup, coOwnerA, new BigDecimal("60.00"), "CERT-SIG-A-" + uid);
        createShare(testGroup, coOwnerB, new BigDecimal("40.00"), "CERT-SIG-B-" + uid);

        pendingContract = createContract(testGroup, 1, ContractStatus.PENDING_SIGNATURE);
    }

    @Test
    @DisplayName("1. Co-owner signs contract successfully in PENDING_SIGNATURE status")
    void shouldSignContractSuccessfully_WhenAuthorizedCoOwner() throws Exception {
        SignContractRequest request = new SignContractRequest(true);

        mockMvc.perform(post("/api/v1/contracts/{id}/sign", pendingContract.getId())
                        .header("Authorization", "Bearer " + tokenA)
                        .header("X-Forwarded-For", "203.0.113.195")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.message", containsString("Contract signed successfully")))
                .andExpect(jsonPath("$.data.contractId", is(pendingContract.getId().intValue())))
                .andExpect(jsonPath("$.data.contractVersion", is(1)))
                .andExpect(jsonPath("$.data.userId", is(coOwnerA.getId().intValue())))
                .andExpect(jsonPath("$.data.userFullName", is(coOwnerA.getFullName())))
                .andExpect(jsonPath("$.data.userEmail", is(coOwnerA.getEmail())))
                .andExpect(jsonPath("$.data.ipAddress", is("203.0.113.195")))
                .andExpect(jsonPath("$.data.signedAt", notNullValue()))
                .andExpect(jsonPath("$.data.signatureHash", matchesPattern("^[a-f0-9]{64}$")));

        // Contract remains in PENDING_SIGNATURE until all active co-owners sign
        CoOwnershipContract updated = coOwnershipContractRepository.findById(pendingContract.getId()).orElseThrow();
        assertEquals(ContractStatus.PENDING_SIGNATURE, updated.getStatus());
    }

    @Test
    @DisplayName("2. Contract automatically transitions to SIGNED when all active co-owners sign")
    void shouldAutomaticallyTransitionToSigned_WhenAllActiveCoOwnersSign() throws Exception {
        SignContractRequest request = new SignContractRequest(true);

        // Signer A signs
        mockMvc.perform(post("/api/v1/contracts/{id}/sign", pendingContract.getId())
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        CoOwnershipContract afterA = coOwnershipContractRepository.findById(pendingContract.getId()).orElseThrow();
        assertEquals(ContractStatus.PENDING_SIGNATURE, afterA.getStatus());

        // Signer B signs -> complete syndicate participation
        mockMvc.perform(post("/api/v1/contracts/{id}/sign", pendingContract.getId())
                        .header("Authorization", "Bearer " + tokenB)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        CoOwnershipContract afterB = coOwnershipContractRepository.findById(pendingContract.getId()).orElseThrow();
        assertEquals(ContractStatus.SIGNED, afterB.getStatus());
    }

    @Test
    @DisplayName("3. Reject duplicate signature by same co-owner on same contract version")
    void shouldRejectDuplicateSignature_WhenSameCoOwnerSignsTwice() throws Exception {
        SignContractRequest request = new SignContractRequest(true);

        // First signature succeeds
        mockMvc.perform(post("/api/v1/contracts/{id}/sign", pendingContract.getId())
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        // Second signature on same contract rejected with 409 Conflict
        mockMvc.perform(post("/api/v1/contracts/{id}/sign", pendingContract.getId())
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.message", containsString("already signed contract version")));
    }

    @Test
    @DisplayName("4. Reject signing when contract is in DRAFT status")
    void shouldRejectSigning_WhenContractIsInDraftStatus() throws Exception {
        CoOwnershipContract draftContract = createContract(testGroup, 2, ContractStatus.DRAFT);
        SignContractRequest request = new SignContractRequest(true);

        mockMvc.perform(post("/api/v1/contracts/{id}/sign", draftContract.getId())
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.message", containsString("DRAFT status and is not yet open for signatures")));
    }

    @Test
    @DisplayName("5. Reject signing when contract is in ACTIVE, TERMINATED or EXPIRED status")
    void shouldRejectSigning_WhenContractIsNotInPendingSignatureStatus() throws Exception {
        CoOwnershipContract activeContract = createContract(testGroup, 2, ContractStatus.ACTIVE);
        CoOwnershipContract terminatedContract = createContract(testGroup, 3, ContractStatus.TERMINATED);
        CoOwnershipContract expiredContract = createContract(testGroup, 4, ContractStatus.EXPIRED);

        SignContractRequest request = new SignContractRequest(true);

        mockMvc.perform(post("/api/v1/contracts/{id}/sign", activeContract.getId())
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message", containsString("ACTIVE status and cannot be signed")));

        mockMvc.perform(post("/api/v1/contracts/{id}/sign", terminatedContract.getId())
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message", containsString("TERMINATED status and cannot be signed")));

        mockMvc.perform(post("/api/v1/contracts/{id}/sign", expiredContract.getId())
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message", containsString("EXPIRED status and cannot be signed")));
    }

    @Test
    @DisplayName("6. Prevent unauthorized signing: non-member co-owner is rejected with 403 Forbidden")
    void shouldRejectSigning_WhenUserIsNotMemberOfSyndicate() throws Exception {
        SignContractRequest request = new SignContractRequest(true);

        mockMvc.perform(post("/api/v1/contracts/{id}/sign", pendingContract.getId())
                        .header("Authorization", "Bearer " + nonMemberToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("7. Prevent unauthenticated signing: request rejected with 401 Unauthorized")
    void shouldRejectSigning_WhenUnauthenticated() throws Exception {
        SignContractRequest request = new SignContractRequest(true);

        mockMvc.perform(post("/api/v1/contracts/{id}/sign", pendingContract.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("8. Reject signing when acceptTerms is null or false")
    void shouldRejectSigning_WhenTermsNotAccepted() throws Exception {
        SignContractRequest request = new SignContractRequest(false);

        mockMvc.perform(post("/api/v1/contracts/{id}/sign", pendingContract.getId())
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("9. Signatures overview returns progress, submitted signatures, and pending co-owners")
    void shouldReturnSignaturesOverview_WithProgressAndPendingSigners() throws Exception {
        // Signer A signs
        SignContractRequest request = new SignContractRequest(true);
        mockMvc.perform(post("/api/v1/contracts/{id}/sign", pendingContract.getId())
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        // Call GET /api/v1/contracts/{id}/signatures
        mockMvc.perform(get("/api/v1/contracts/{id}/signatures", pendingContract.getId())
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.contractId", is(pendingContract.getId().intValue())))
                .andExpect(jsonPath("$.data.contractVersion", is(1)))
                .andExpect(jsonPath("$.data.contractStatus", is("PENDING_SIGNATURE")))
                .andExpect(jsonPath("$.data.totalRequiredSignatures", is(2)))
                .andExpect(jsonPath("$.data.totalSubmittedSignatures", is(1)))
                .andExpect(jsonPath("$.data.allSigned", is(false)))
                .andExpect(jsonPath("$.data.signatures", hasSize(1)))
                .andExpect(jsonPath("$.data.signatures[0].userId", is(coOwnerA.getId().intValue())))
                .andExpect(jsonPath("$.data.signatures[0].signatureHash", matchesPattern("^[a-f0-9]{64}$")))
                .andExpect(jsonPath("$.data.pendingSigners", hasSize(1)))
                .andExpect(jsonPath("$.data.pendingSigners[0].userId", is(coOwnerB.getId().intValue())))
                .andExpect(jsonPath("$.data.pendingSigners[0].email", is(coOwnerB.getEmail())));
    }

    @Test
    @DisplayName("10. Signatures overview shows allSigned true when both co-owners have signed")
    void shouldReturnAllSignedTrue_WhenBothCoOwnersSign() throws Exception {
        SignContractRequest request = new SignContractRequest(true);
        mockMvc.perform(post("/api/v1/contracts/{id}/sign", pendingContract.getId())
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/contracts/{id}/sign", pendingContract.getId())
                        .header("Authorization", "Bearer " + tokenB)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/v1/contracts/{id}/signatures", pendingContract.getId())
                        .header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalRequiredSignatures", is(2)))
                .andExpect(jsonPath("$.data.totalSubmittedSignatures", is(2)))
                .andExpect(jsonPath("$.data.allSigned", is(true)))
                .andExpect(jsonPath("$.data.contractStatus", is("SIGNED")))
                .andExpect(jsonPath("$.data.signatures", hasSize(2)))
                .andExpect(jsonPath("$.data.pendingSigners", hasSize(0)));
    }

    @Test
    @DisplayName("11. Non-member co-owner is forbidden from viewing contract signatures overview")
    void shouldDenySignaturesOverview_ForNonMemberCoOwner() throws Exception {
        mockMvc.perform(get("/api/v1/contracts/{id}/signatures", pendingContract.getId())
                        .header("Authorization", "Bearer " + nonMemberToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("12. Staff and Admin can view signatures overview for any contract")
    void shouldAllowStaffAndAdmin_ToViewSignaturesOverview() throws Exception {
        mockMvc.perform(get("/api/v1/contracts/{id}/signatures", pendingContract.getId())
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalRequiredSignatures", is(2)));

        mockMvc.perform(get("/api/v1/contracts/{id}/signatures", pendingContract.getId())
                        .header("Authorization", "Bearer " + staffToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalRequiredSignatures", is(2)));
    }

    @Test
    @DisplayName("13. Version isolation and history preservation: v1 signatures preserved when v2 is created")
    void shouldPreserveHistoricalSignatures_AcrossContractVersions() throws Exception {
        SignContractRequest request = new SignContractRequest(true);

        // Both sign v1
        mockMvc.perform(post("/api/v1/contracts/{id}/sign", pendingContract.getId())
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/contracts/{id}/sign", pendingContract.getId())
                        .header("Authorization", "Bearer " + tokenB)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        // Create version 2 contract for the group
        CoOwnershipContract contractV2 = createContract(testGroup, 2, ContractStatus.PENDING_SIGNATURE);

        // Inspect v1 signatures: Still has 2 signatures
        mockMvc.perform(get("/api/v1/contracts/{id}/signatures", pendingContract.getId())
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.contractVersion", is(1)))
                .andExpect(jsonPath("$.data.totalSubmittedSignatures", is(2)))
                .andExpect(jsonPath("$.data.allSigned", is(true)));

        // Inspect v2 signatures: 0 submitted signatures, 2 pending
        mockMvc.perform(get("/api/v1/contracts/{id}/signatures", contractV2.getId())
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.contractVersion", is(2)))
                .andExpect(jsonPath("$.data.totalSubmittedSignatures", is(0)))
                .andExpect(jsonPath("$.data.allSigned", is(false)))
                .andExpect(jsonPath("$.data.pendingSigners", hasSize(2)));

        // CoOwnerA signs v2 independently
        mockMvc.perform(post("/api/v1/contracts/{id}/sign", contractV2.getId())
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.contractVersion", is(2)));

        // Verify total signatures in repository: 2 on v1, 1 on v2
        List<ContractSignature> v1Sigs = contractSignatureRepository.findByContractId(pendingContract.getId());
        List<ContractSignature> v2Sigs = contractSignatureRepository.findByContractId(contractV2.getId());
        assertEquals(2, v1Sigs.size());
        assertEquals(1, v2Sigs.size());
    }

    @Test
    @DisplayName("14. Audit log entry recorded with SIGN_CONTRACT action")
    void shouldRecordAuditLog_WhenContractSigned() throws Exception {
        SignContractRequest request = new SignContractRequest(true);

        mockMvc.perform(post("/api/v1/contracts/{id}/sign", pendingContract.getId())
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        List<AuditLog> logs = auditLogRepository.findByEntityNameAndEntityIdInOrderByIdDesc(
                "CoOwnershipContract", Collections.singletonList(pendingContract.getId()));

        boolean foundSignAudit = logs.stream()
                .anyMatch(l -> "SIGN_CONTRACT".equals(l.getAction()) &&
                        l.getNewStateJson() != null &&
                        l.getNewStateJson().contains("signatureHash"));

        assertTrue(foundSignAudit, "Audit log with action SIGN_CONTRACT must be recorded");
    }

    @Test
    @DisplayName("15. Historical data preservation: Contract deletion is rejected, preserving signatures")
    void shouldPreserveSignatures_AgainstContractDeletion() throws Exception {
        SignContractRequest request = new SignContractRequest(true);

        mockMvc.perform(post("/api/v1/contracts/{id}/sign", pendingContract.getId())
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        // Deletion request rejected
        mockMvc.perform(delete("/api/v1/contracts/{id}", pendingContract.getId())
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("Historical contract data cannot be deleted")));

        // Verify signature still exists in database
        assertTrue(contractSignatureRepository.existsByContractIdAndUserId(pendingContract.getId(), coOwnerA.getId()));
    }
}
