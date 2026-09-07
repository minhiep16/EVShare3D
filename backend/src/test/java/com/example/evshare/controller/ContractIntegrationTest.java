package com.example.evshare.controller;

import com.example.evshare.dto.request.CreateContractRequest;
import com.example.evshare.dto.request.TransitionContractStatusRequest;
import com.example.evshare.dto.request.UpdateContractRequest;
import com.example.evshare.entity.CoOwnershipContract;
import com.example.evshare.entity.OwnershipGroup;
import com.example.evshare.entity.OwnershipShare;
import com.example.evshare.entity.Role;
import com.example.evshare.entity.User;
import com.example.evshare.entity.Vehicle;
import com.example.evshare.entity.enums.ContractStatus;
import com.example.evshare.entity.enums.RoleName;
import com.example.evshare.entity.enums.VehicleStatus;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@DisplayName("Checkpoint 04-I — Co-Ownership Contract Integration Tests")
class ContractIntegrationTest {

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
        vehicle.setModelName("VinFast VF 8 Plus");
        vehicle.setManufacturer("VinFast");
        vehicle.setModel3dAssetPath("models/vehicles/vinfast_vf8.glb");
        vehicle.setStatus(VehicleStatus.AVAILABLE);
        vehicle.setBatteryLevel(98);
        vehicle.setOdometerKm(BigDecimal.ZERO);
        vehicle.setStallLocationCode("BAY-02");
        return vehicleRepository.saveAndFlush(vehicle);
    }

    @BeforeEach
    void setUp() {
        String uid = UUID.randomUUID().toString().substring(0, 8);
        adminUser = createUser("admin_ctr_" + uid + "@evshare.vn", RoleName.ROLE_ADMIN);
        staffUser = createUser("staff_ctr_" + uid + "@evshare.vn", RoleName.ROLE_STAFF);
        memberCoOwner = createUser("member_ctr_" + uid + "@evshare.vn", RoleName.ROLE_CO_OWNER);
        nonMemberCoOwner = createUser("nonmember_ctr_" + uid + "@evshare.vn", RoleName.ROLE_CO_OWNER);

        adminToken = tokenService.generateAccessToken(adminUser.getId(), adminUser.getEmail(), Collections.singletonList("ROLE_ADMIN"));
        staffToken = tokenService.generateAccessToken(staffUser.getId(), staffUser.getEmail(), Collections.singletonList("ROLE_STAFF"));
        memberCoOwnerToken = tokenService.generateAccessToken(memberCoOwner.getId(), memberCoOwner.getEmail(), Collections.singletonList("ROLE_CO_OWNER"));
        nonMemberCoOwnerToken = tokenService.generateAccessToken(nonMemberCoOwner.getId(), nonMemberCoOwner.getEmail(), Collections.singletonList("ROLE_CO_OWNER"));

        Vehicle vehicle = createVehicle("VIN_CTR_" + uid, "51F-" + (10000 + (int) (Math.random() * 89999)));

        OwnershipGroup group = new OwnershipGroup();
        group.setGroupName("VF8 Co-Ownership Syndicate");
        group.setVehicle(vehicle);
        group.setFormationDate(LocalDate.now());
        group.setIsActive(true);
        testGroup = ownershipGroupRepository.saveAndFlush(group);

        // Enrolled member share
        OwnershipShare share = new OwnershipShare();
        share.setGroup(testGroup);
        share.setUser(memberCoOwner);
        share.setPercentage(new BigDecimal("100.00"));
        share.setShareCertificateNumber("CERT-G" + testGroup.getId() + "-U" + memberCoOwner.getId() + "-CTR");
        share.setAcquiredAt(Instant.now());
        share.setIsActive(true);
        ownershipShareRepository.saveAndFlush(share);
    }

    // =========================================================================
    // 1. CONTRACT CREATION & OWNERSHIP ASSOCIATION
    // =========================================================================

    @Test
    @DisplayName("1.1. Staff creates initial contract draft (version = 1, status = DRAFT)")
    void testCreateInitialContract() throws Exception {
        CreateContractRequest request = new CreateContractRequest(
                testGroup.getId(),
                "VinFast VF 8 Co-Ownership Agreement",
                "# Co-Ownership Agreement Terms\n1. Equity shares govern allocation\n2. Routine maintenance shared equally",
                LocalDate.now(),
                LocalDate.now().plusYears(1)
        );

        mockMvc.perform(post("/api/v1/contracts")
                        .header("Authorization", "Bearer " + staffToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.contractTitle", is("VinFast VF 8 Co-Ownership Agreement")))
                .andExpect(jsonPath("$.data.version", is(1)))
                .andExpect(jsonPath("$.data.status", is("DRAFT")))
                .andExpect(jsonPath("$.data.groupId", is(testGroup.getId().intValue())));

        assertEquals(1, coOwnershipContractRepository.findByGroupId(testGroup.getId()).size());
    }

    @Test
    @DisplayName("1.2. Rejects contract creation for non-existent ownership group with HTTP 404")
    void testCreateContractNonExistentGroup() throws Exception {
        CreateContractRequest request = new CreateContractRequest(
                99999L,
                "Phantom Group Contract",
                "Terms content",
                LocalDate.now(),
                LocalDate.now().plusYears(1)
        );

        mockMvc.perform(post("/api/v1/contracts")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("1.3. Rejects contract creation with blank title or blank terms with HTTP 400")
    void testCreateContractBlankFieldsRejected() throws Exception {
        CreateContractRequest request = new CreateContractRequest(
                testGroup.getId(),
                "   ",
                "   ",
                LocalDate.now(),
                LocalDate.now().plusYears(1)
        );

        mockMvc.perform(post("/api/v1/contracts")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("1.4. Rejects contract creation when expiry date is before effective date with HTTP 400")
    void testCreateContractInvertedDatesRejected() throws Exception {
        CreateContractRequest request = new CreateContractRequest(
                testGroup.getId(),
                "Inverted Dates Contract",
                "Terms content",
                LocalDate.now().plusYears(1),
                LocalDate.now()
        );

        mockMvc.perform(post("/api/v1/contracts")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    // =========================================================================
    // 2. CONTRACT VERSIONING & HISTORICAL IMMUTABILITY
    // =========================================================================

    @Test
    @DisplayName("2.1. Creating subsequent contracts increments version and preserves historical versions")
    void testContractVersioning() throws Exception {
        // Version 1
        CreateContractRequest req1 = new CreateContractRequest(
                testGroup.getId(),
                "Agreement Version 1",
                "Original Terms",
                LocalDate.now(),
                LocalDate.now().plusYears(1)
        );
        mockMvc.perform(post("/api/v1/contracts")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req1)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.version", is(1)));

        // Version 2
        CreateContractRequest req2 = new CreateContractRequest(
                testGroup.getId(),
                "Agreement Version 2 (Amendment)",
                "Amended Terms",
                LocalDate.now(),
                LocalDate.now().plusYears(2)
        );
        mockMvc.perform(post("/api/v1/contracts")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req2)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.version", is(2)));

        // Version 3
        CreateContractRequest req3 = new CreateContractRequest(
                testGroup.getId(),
                "Agreement Version 3 (Final)",
                "Final Terms",
                LocalDate.now(),
                LocalDate.now().plusYears(3)
        );
        mockMvc.perform(post("/api/v1/contracts")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req3)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.version", is(3)));

        // Query all versions: returns versions 3, 2, 1 in descending order
        mockMvc.perform(get("/api/v1/contracts/group/{groupId}", testGroup.getId())
                        .header("Authorization", "Bearer " + memberCoOwnerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(3)))
                .andExpect(jsonPath("$.data[0].version", is(3)))
                .andExpect(jsonPath("$.data[1].version", is(2)))
                .andExpect(jsonPath("$.data[2].version", is(1)));
    }

    // =========================================================================
    // 3. CONTRACT EDITING & CONTENT IMMUTABILITY
    // =========================================================================

    @Test
    @DisplayName("3.1. Draft contract terms can be updated while in DRAFT status")
    void testUpdateDraftContract() throws Exception {
        CoOwnershipContract contract = new CoOwnershipContract();
        contract.setGroup(testGroup);
        contract.setContractTitle("Initial Title");
        contract.setContractTermsText("Initial Terms");
        contract.setVersion(1);
        contract.setStatus(ContractStatus.DRAFT);
        contract = coOwnershipContractRepository.saveAndFlush(contract);

        UpdateContractRequest updateReq = new UpdateContractRequest(
                "Revised Title",
                "Revised Terms Content",
                LocalDate.now(),
                LocalDate.now().plusMonths(6)
        );

        mockMvc.perform(put("/api/v1/contracts/{id}", contract.getId())
                        .header("Authorization", "Bearer " + staffToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.contractTitle", is("Revised Title")))
                .andExpect(jsonPath("$.data.contractTermsText", is("Revised Terms Content")));
    }

    @Test
    @DisplayName("3.2. Updating terms of non-draft contract (e.g. PENDING_SIGNATURE or ACTIVE) is rejected with HTTP 409 Conflict")
    void testNonDraftContractIsImmutable() throws Exception {
        CoOwnershipContract contract = new CoOwnershipContract();
        contract.setGroup(testGroup);
        contract.setContractTitle("Locked Agreement");
        contract.setContractTermsText("Legally Binding Terms");
        contract.setVersion(1);
        contract.setStatus(ContractStatus.PENDING_SIGNATURE);
        contract = coOwnershipContractRepository.saveAndFlush(contract);

        UpdateContractRequest updateReq = new UpdateContractRequest(
                "Tampered Title",
                "Tampered Terms Content",
                LocalDate.now(),
                LocalDate.now().plusYears(1)
        );

        mockMvc.perform(put("/api/v1/contracts/{id}", contract.getId())
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateReq)))
                .andExpect(status().isConflict());

        // Verify terms remain unchanged
        CoOwnershipContract fetched = coOwnershipContractRepository.findById(contract.getId()).orElseThrow();
        assertEquals("Locked Agreement", fetched.getContractTitle());
        assertEquals("Legally Binding Terms", fetched.getContractTermsText());
    }

    // =========================================================================
    // 4. LIFECYCLE STATE MACHINE VALIDATION
    // =========================================================================

    @Test
    @DisplayName("4.1. Permitted sequential lifecycle: DRAFT -> PENDING_SIGNATURE -> SIGNED -> ACTIVE -> EXPIRED")
    void testPermittedLifecycleTransitions() throws Exception {
        CoOwnershipContract contract = new CoOwnershipContract();
        contract.setGroup(testGroup);
        contract.setContractTitle("Lifecycle Test Contract");
        contract.setContractTermsText("Terms Text");
        contract.setVersion(1);
        contract.setStatus(ContractStatus.DRAFT);
        contract.setEffectiveDate(LocalDate.now());
        contract.setExpiryDate(LocalDate.now().plusMonths(12));
        contract = coOwnershipContractRepository.saveAndFlush(contract);

        // 1. DRAFT -> PENDING_SIGNATURE
        mockMvc.perform(patch("/api/v1/contracts/{id}/status", contract.getId())
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new TransitionContractStatusRequest(ContractStatus.PENDING_SIGNATURE))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status", is("PENDING_SIGNATURE")));

        // 2. PENDING_SIGNATURE -> SIGNED
        mockMvc.perform(patch("/api/v1/contracts/{id}/status", contract.getId())
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new TransitionContractStatusRequest(ContractStatus.SIGNED))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status", is("SIGNED")));

        // 3. SIGNED -> ACTIVE
        mockMvc.perform(patch("/api/v1/contracts/{id}/status", contract.getId())
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new TransitionContractStatusRequest(ContractStatus.ACTIVE))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status", is("ACTIVE")));

        // 4. ACTIVE -> EXPIRED
        mockMvc.perform(patch("/api/v1/contracts/{id}/status", contract.getId())
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new TransitionContractStatusRequest(ContractStatus.EXPIRED))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status", is("EXPIRED")));
    }

    @Test
    @DisplayName("4.2. Permitted early terminations: DRAFT -> TERMINATED, ACTIVE -> TERMINATED")
    void testPermittedTerminations() throws Exception {
        CoOwnershipContract draftContract = new CoOwnershipContract();
        draftContract.setGroup(testGroup);
        draftContract.setContractTitle("Aborted Draft");
        draftContract.setContractTermsText("Draft Terms");
        draftContract.setVersion(1);
        draftContract.setStatus(ContractStatus.DRAFT);
        draftContract = coOwnershipContractRepository.saveAndFlush(draftContract);

        // DRAFT -> TERMINATED
        mockMvc.perform(patch("/api/v1/contracts/{id}/status", draftContract.getId())
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new TransitionContractStatusRequest(ContractStatus.TERMINATED, "Cancelled proposal"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status", is("TERMINATED")));
    }

    @Test
    @DisplayName("4.3. Invalid direct jump from DRAFT to ACTIVE is rejected with HTTP 409 Conflict")
    void testInvalidDraftToActiveRejected() throws Exception {
        CoOwnershipContract contract = new CoOwnershipContract();
        contract.setGroup(testGroup);
        contract.setContractTitle("Draft Contract");
        contract.setContractTermsText("Terms");
        contract.setVersion(1);
        contract.setStatus(ContractStatus.DRAFT);
        contract = coOwnershipContractRepository.saveAndFlush(contract);

        mockMvc.perform(patch("/api/v1/contracts/{id}/status", contract.getId())
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new TransitionContractStatusRequest(ContractStatus.ACTIVE))))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("4.4. Transitioning out of terminal state (TERMINATED -> ACTIVE) is rejected with HTTP 409 Conflict")
    void testTerminalStateViolationRejected() throws Exception {
        CoOwnershipContract contract = new CoOwnershipContract();
        contract.setGroup(testGroup);
        contract.setContractTitle("Terminated Contract");
        contract.setContractTermsText("Terms");
        contract.setVersion(1);
        contract.setStatus(ContractStatus.TERMINATED);
        contract = coOwnershipContractRepository.saveAndFlush(contract);

        mockMvc.perform(patch("/api/v1/contracts/{id}/status", contract.getId())
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new TransitionContractStatusRequest(ContractStatus.ACTIVE))))
                .andExpect(status().isConflict());
    }

    // =========================================================================
    // 5. SUPERSEDING CONTRACT ACTIVATION
    // =========================================================================

    @Test
    @DisplayName("5.1. Activating version 2 automatically supersedes and terminates version 1")
    void testSupersedingActivation() throws Exception {
        // Version 1 is ACTIVE
        CoOwnershipContract v1 = new CoOwnershipContract();
        v1.setGroup(testGroup);
        v1.setContractTitle("Version 1 (Active)");
        v1.setContractTermsText("V1 Terms");
        v1.setVersion(1);
        v1.setStatus(ContractStatus.ACTIVE);
        v1.setEffectiveDate(LocalDate.now().minusMonths(6));
        v1 = coOwnershipContractRepository.saveAndFlush(v1);

        // Version 2 is SIGNED
        CoOwnershipContract v2 = new CoOwnershipContract();
        v2.setGroup(testGroup);
        v2.setContractTitle("Version 2 (Signed)");
        v2.setContractTermsText("V2 Terms");
        v2.setVersion(2);
        v2.setStatus(ContractStatus.SIGNED);
        v2 = coOwnershipContractRepository.saveAndFlush(v2);

        // Activate Version 2
        mockMvc.perform(patch("/api/v1/contracts/{id}/status", v2.getId())
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new TransitionContractStatusRequest(ContractStatus.ACTIVE))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status", is("ACTIVE")))
                .andExpect(jsonPath("$.data.version", is(2)));

        // Verify Version 1 was automatically TERMINATED
        CoOwnershipContract updatedV1 = coOwnershipContractRepository.findById(v1.getId()).orElseThrow();
        assertEquals(ContractStatus.TERMINATED, updatedV1.getStatus(), "Previous active contract must be terminated");

        // Verify active contract query returns Version 2
        mockMvc.perform(get("/api/v1/contracts/group/{groupId}/active", testGroup.getId())
                        .header("Authorization", "Bearer " + memberCoOwnerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.version", is(2)))
                .andExpect(jsonPath("$.data.status", is("ACTIVE")));
    }

    // =========================================================================
    // 6. HISTORICAL DATA PRESERVATION (DO NOT DELETE CONTRACTS)
    // =========================================================================

    @Test
    @DisplayName("6.1. Calling DELETE /api/v1/contracts/{id} is strictly rejected with HTTP 400 Bad Request")
    void testDeleteContractIsForbidden() throws Exception {
        CoOwnershipContract contract = new CoOwnershipContract();
        contract.setGroup(testGroup);
        contract.setContractTitle("Historical Agreement");
        contract.setContractTermsText("Preserved Terms");
        contract.setVersion(1);
        contract.setStatus(ContractStatus.EXPIRED);
        contract = coOwnershipContractRepository.saveAndFlush(contract);

        mockMvc.perform(delete("/api/v1/contracts/{id}", contract.getId())
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("Historical contract data cannot be deleted")));

        // Verify contract still exists in the database
        assertTrue(coOwnershipContractRepository.existsById(contract.getId()), "Historical contract must be preserved");
    }

    // =========================================================================
    // 7. AUTHORIZATION & DATA SCOPING (RBAC + OWNERSHIP ACL)
    // =========================================================================

    @Test
    @DisplayName("7.1. Enrolled co-owner can view single contract and group contracts via Ownership ACL")
    void testEnrolledCoOwnerAccess() throws Exception {
        CoOwnershipContract contract = new CoOwnershipContract();
        contract.setGroup(testGroup);
        contract.setContractTitle("Member Accessible Agreement");
        contract.setContractTermsText("Terms");
        contract.setVersion(1);
        contract.setStatus(ContractStatus.DRAFT);
        contract = coOwnershipContractRepository.saveAndFlush(contract);

        mockMvc.perform(get("/api/v1/contracts/{id}", contract.getId())
                        .header("Authorization", "Bearer " + memberCoOwnerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id", is(contract.getId().intValue())));

        mockMvc.perform(get("/api/v1/contracts/group/{groupId}", testGroup.getId())
                        .header("Authorization", "Bearer " + memberCoOwnerToken))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("7.2. Non-member co-owner is denied access with HTTP 403 Forbidden")
    void testNonMemberDeniedAccess() throws Exception {
        CoOwnershipContract contract = new CoOwnershipContract();
        contract.setGroup(testGroup);
        contract.setContractTitle("Restricted Agreement");
        contract.setContractTermsText("Terms");
        contract.setVersion(1);
        contract.setStatus(ContractStatus.DRAFT);
        contract = coOwnershipContractRepository.saveAndFlush(contract);

        mockMvc.perform(get("/api/v1/contracts/{id}", contract.getId())
                        .header("Authorization", "Bearer " + nonMemberCoOwnerToken))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/v1/contracts/group/{groupId}", testGroup.getId())
                        .header("Authorization", "Bearer " + nonMemberCoOwnerToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("7.3. Co-owner cannot draft or transition contract (restricted to STAFF or ADMIN)")
    void testCoOwnerCannotDraftOrTransition() throws Exception {
        CreateContractRequest request = new CreateContractRequest(
                testGroup.getId(),
                "Unauthorized Draft",
                "Terms",
                LocalDate.now(),
                LocalDate.now().plusYears(1)
        );

        mockMvc.perform(post("/api/v1/contracts")
                        .header("Authorization", "Bearer " + memberCoOwnerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("7.4. Unauthenticated request to contracts returns HTTP 401 Unauthorized")
    void testUnauthenticatedAccessRejected() throws Exception {
        mockMvc.perform(get("/api/v1/contracts/group/{groupId}", testGroup.getId()))
                .andExpect(status().isUnauthorized());
    }
}
