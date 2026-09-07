package com.example.evshare.controller;

import com.example.evshare.dto.request.*;
import com.example.evshare.entity.*;
import com.example.evshare.entity.enums.ContractStatus;
import com.example.evshare.entity.enums.RoleName;
import com.example.evshare.entity.enums.VehicleStatus;
import com.example.evshare.repository.*;
import com.example.evshare.security.TokenService;
import com.example.evshare.service.ContractService;
import com.example.evshare.service.OwnershipGroupService;
import com.example.evshare.service.OwnershipShareService;
import com.example.evshare.service.VehicleService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.*;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * CHECKPOINT 04-M — COMPREHENSIVE PHASE 04 MASTER TEST SUITE
 *
 * Systematically verifies all 13 Phase 04 domains:
 * 1.  Vehicle CRUD
 * 2.  Vehicle States (Lifecycle State Machine)
 * 3.  Ownership Group (Syndicate Formation & 1:1 Vehicle Binding)
 * 4.  Ownership Share (Certificates, Active/Inactive Toggles)
 * 5.  Ownership = 100% Invariant (Strict Equity Allocation)
 * 6.  Invalid Ownership (Rejection of Non-100% Invariants & Overselling)
 * 7.  Ownership History (Append-Only Audit Provenance)
 * 8.  Contract Creation (Sequential Versioning & Terms Immutability)
 * 9.  Signatures (Cryptographic Term Binding & Duplicate Protection)
 * 10. Contract Lifecycle (7 Canonical States & Superseding Activation)
 * 11. RBAC (Method Security Boundaries & Data Scoping ACLs)
 * 12. Validation (DTO Bean Constraints & Format Integrity)
 * 13. Transactions (Transactional Rollback & Invariant Atomicity)
 */
@SpringBootTest
@AutoConfigureMockMvc
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("Checkpoint 04-M — Comprehensive Phase 04 Master Test Suite (All 13 Domains)")
public class ComprehensivePhase04TestSuiteTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private VehicleRepository vehicleRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private OwnershipGroupRepository groupRepository;

    @Autowired
    private OwnershipShareRepository shareRepository;

    @Autowired
    private CoOwnershipContractRepository contractRepository;

    @Autowired
    private ContractSignatureRepository signatureRepository;

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Autowired
    private VehicleService vehicleService;

    @Autowired
    private OwnershipGroupService groupService;

    @Autowired
    private OwnershipShareService shareService;

    @Autowired
    private ContractService contractService;

    @Autowired
    private TokenService tokenService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private Role adminRole;
    private Role staffRole;
    private Role coOwnerRole;

    private User adminUser;
    private User staffUser;
    private User coOwnerUser1;
    private User coOwnerUser2;
    private User nonMemberUser;

    private String adminToken;
    private String staffToken;
    private String coOwnerToken1;
    private String coOwnerToken2;
    private String nonMemberToken;

    private String generateUniqueVin() {
        return "1HG" + UUID.randomUUID().toString().replace("-", "").substring(0, 14).toUpperCase();
    }

    private String generateUniquePlate() {
        return "P" + UUID.randomUUID().toString().replace("-", "").substring(0, 7).toUpperCase();
    }

    private String generateUniqueCert() {
        return "CERT-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    private User createTestUser(String emailPrefix, Role role) {
        String email = emailPrefix + "_" + UUID.randomUUID().toString().substring(0, 8) + "@evshare.io";
        User user = new User();
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode("SecurePass123!"));
        user.setFullName("Test " + role.getName().name());
        user.setIsActive(true);
        user.setRoles(new HashSet<>(Collections.singletonList(role)));
        return userRepository.saveAndFlush(user);
    }

    private Vehicle createTestVehicle(String model, String manufacturer, VehicleStatus status) {
        Vehicle vehicle = new Vehicle();
        vehicle.setVin(generateUniqueVin());
        vehicle.setLicensePlate(generateUniquePlate());
        vehicle.setModelName(model);
        vehicle.setManufacturer(manufacturer);
        vehicle.setModel3dAssetPath("models/" + model.toLowerCase().replace(" ", "_") + ".glb");
        vehicle.setStatus(status);
        vehicle.setBatteryLevel(90);
        vehicle.setOdometerKm(new BigDecimal("1500.00"));
        vehicle.setStallLocationCode("STALL-A1");
        return vehicleRepository.saveAndFlush(vehicle);
    }

    @BeforeEach
    void setUp() {
        adminRole = roleRepository.findByName(RoleName.ROLE_ADMIN)
                .orElseGet(() -> roleRepository.saveAndFlush(new Role(RoleName.ROLE_ADMIN)));
        staffRole = roleRepository.findByName(RoleName.ROLE_STAFF)
                .orElseGet(() -> roleRepository.saveAndFlush(new Role(RoleName.ROLE_STAFF)));
        coOwnerRole = roleRepository.findByName(RoleName.ROLE_CO_OWNER)
                .orElseGet(() -> roleRepository.saveAndFlush(new Role(RoleName.ROLE_CO_OWNER)));

        adminUser = createTestUser("admin.p4", adminRole);
        staffUser = createTestUser("staff.p4", staffRole);
        coOwnerUser1 = createTestUser("owner1.p4", coOwnerRole);
        coOwnerUser2 = createTestUser("owner2.p4", coOwnerRole);
        nonMemberUser = createTestUser("nonmember.p4", coOwnerRole);

        adminToken = tokenService.generateAccessToken(adminUser);
        staffToken = tokenService.generateAccessToken(staffUser);
        coOwnerToken1 = tokenService.generateAccessToken(coOwnerUser1);
        coOwnerToken2 = tokenService.generateAccessToken(coOwnerUser2);
        nonMemberToken = tokenService.generateAccessToken(nonMemberUser);
    }

    // =========================================================================
    // 1. VEHICLE CRUD
    // =========================================================================
    @Test
    @Order(1)
    @DisplayName("Dimension 01: Vehicle CRUD — Register, Read, Update Status, and 404 Verification")
    void test01_vehicleCrud() throws Exception {
        String vin = generateUniqueVin();
        String plate = generateUniquePlate();

        CreateVehicleRequest createRequest = new CreateVehicleRequest();
        createRequest.setVin(vin);
        createRequest.setLicensePlate(plate);
        createRequest.setModelName("Audi e-tron GT");
        createRequest.setManufacturer("Audi");
        createRequest.setModel3dAssetPath("models/etron.glb");
        createRequest.setBatteryLevel(95);
        createRequest.setOdometerKm(new BigDecimal("500.00"));
        createRequest.setStallLocationCode("BAY-AUDI");

        // 1. CREATE (Admin)
        String createResponse = mockMvc.perform(post("/api/v1/vehicles")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.vin", is(vin)))
                .andExpect(jsonPath("$.data.status", is("AVAILABLE")))
                .andReturn().getResponse().getContentAsString();

        Long vehicleId = objectMapper.readTree(createResponse).path("data").path("id").asLong();

        // 2. READ by ID & Telemetry
        mockMvc.perform(get("/api/v1/vehicles/" + vehicleId)
                        .header("Authorization", "Bearer " + coOwnerToken1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.modelName", is("Audi e-tron GT")));

        mockMvc.perform(get("/api/v1/vehicles/" + vehicleId + "/telemetry")
                        .header("Authorization", "Bearer " + coOwnerToken1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.batteryLevel", is(95)));

        // 3. READ Paginated
        mockMvc.perform(get("/api/v1/vehicles")
                        .header("Authorization", "Bearer " + coOwnerToken1)
                        .param("manufacturer", "Audi"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items", not(empty())));

        // 4. UPDATE Status
        UpdateVehicleStatusRequest statusReq = new UpdateVehicleStatusRequest(VehicleStatus.MAINTENANCE, "Scheduled maintenance check");
        mockMvc.perform(patch("/api/v1/vehicles/" + vehicleId + "/status")
                        .header("Authorization", "Bearer " + staffToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(statusReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status", is("MAINTENANCE")));

        // 5. 404 Not Found for non-existent vehicle
        mockMvc.perform(get("/api/v1/vehicles/9999999")
                        .header("Authorization", "Bearer " + coOwnerToken1))
                .andExpect(status().isNotFound());
    }

    private OwnershipShare createShare(OwnershipGroup group, User user, BigDecimal percentage, boolean active) {
        OwnershipShare share = new OwnershipShare();
        share.setGroup(group);
        share.setUser(user);
        share.setPercentage(percentage);
        share.setShareCertificateNumber("CERT-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        share.setAcquiredAt(Instant.now());
        share.setIsActive(active);
        return shareRepository.saveAndFlush(share);
    }

    // =========================================================================
    // 2. VEHICLE STATES
    // =========================================================================
    @Test
    @Order(2)
    @DisplayName("Dimension 02: Vehicle States — State Machine Operational Cycle & Invalid Transition Guard")
    void test02_vehicleStates() throws Exception {
        Vehicle vehicle = createTestVehicle("Lucid Gravity", "Lucid", VehicleStatus.AVAILABLE);

        // Valid: AVAILABLE -> BOOKED
        mockMvc.perform(patch("/api/v1/vehicles/" + vehicle.getId() + "/status")
                        .header("Authorization", "Bearer " + staffToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UpdateVehicleStatusRequest(VehicleStatus.BOOKED, "Reserved"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status", is("BOOKED")));

        // Valid: BOOKED -> IN_USE
        mockMvc.perform(patch("/api/v1/vehicles/" + vehicle.getId() + "/status")
                        .header("Authorization", "Bearer " + staffToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UpdateVehicleStatusRequest(VehicleStatus.IN_USE, "Trip started"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status", is("IN_USE")));

        // Valid: IN_USE -> CHARGING
        mockMvc.perform(patch("/api/v1/vehicles/" + vehicle.getId() + "/status")
                        .header("Authorization", "Bearer " + staffToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UpdateVehicleStatusRequest(VehicleStatus.CHARGING, "Plugged into DC fast charger"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status", is("CHARGING")));

        // Valid: CHARGING -> AVAILABLE
        mockMvc.perform(patch("/api/v1/vehicles/" + vehicle.getId() + "/status")
                        .header("Authorization", "Bearer " + staffToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UpdateVehicleStatusRequest(VehicleStatus.AVAILABLE, "Charge complete"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status", is("AVAILABLE")));

        // Invalid: AVAILABLE -> IN_USE directly (Forbidden transition, must be BOOKED first)
        mockMvc.perform(patch("/api/v1/vehicles/" + vehicle.getId() + "/status")
                        .header("Authorization", "Bearer " + staffToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UpdateVehicleStatusRequest(VehicleStatus.IN_USE, "Invalid skip"))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success", is(false)));
    }

    // =========================================================================
    // 3. OWNERSHIP GROUP
    // =========================================================================
    @Test
    @Order(3)
    @DisplayName("Dimension 03: Ownership Group — Syndicate Formation & 1:1 Vehicle Binding")
    void test03_ownershipGroup() throws Exception {
        Vehicle vehicle = createTestVehicle("Mercedes EQS", "Mercedes", VehicleStatus.AVAILABLE);

        CreateOwnershipGroupRequest groupReq = new CreateOwnershipGroupRequest();
        groupReq.setGroupName("EQS Syndicate Alpha");
        groupReq.setVehicleId(vehicle.getId());

        // 1. Create Syndicate (Admin)
        String resp = mockMvc.perform(post("/api/v1/ownership-groups")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(groupReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.groupName", is("EQS Syndicate Alpha")))
                .andExpect(jsonPath("$.data.vehicleId", is(vehicle.getId().intValue())))
                .andReturn().getResponse().getContentAsString();

        Long groupId = objectMapper.readTree(resp).path("data").path("id").asLong();

        // 2. Reject duplicate binding to same vehicle (1:1 Invariant)
        CreateOwnershipGroupRequest dupReq = new CreateOwnershipGroupRequest();
        dupReq.setGroupName("Duplicate Syndicate");
        dupReq.setVehicleId(vehicle.getId());

        mockMvc.perform(post("/api/v1/ownership-groups")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dupReq)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success", is(false)));

        // 3. Staff/Admin inspection
        mockMvc.perform(get("/api/v1/ownership-groups/" + groupId)
                        .header("Authorization", "Bearer " + staffToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id", is(groupId.intValue())));
    }

    // =========================================================================
    // 4. OWNERSHIP SHARE
    // =========================================================================
    @Test
    @Order(4)
    @DisplayName("Dimension 04: Ownership Share — Certificate Issuance, Deactivation & Reactivation")
    void test04_ownershipShare() throws Exception {
        Vehicle vehicle = createTestVehicle("Polestar 3", "Polestar", VehicleStatus.AVAILABLE);
        OwnershipGroup group = groupRepository.saveAndFlush(new OwnershipGroup(null, "Polestar Syndicate", vehicle, LocalDate.now(), true));

        // Initial setup: active share 75.00%
        createShare(group, coOwnerUser1, new BigDecimal("75.00"), true);

        // Inactive share 25.00%
        OwnershipShare inactiveShare = createShare(group, coOwnerUser2, new BigDecimal("25.00"), false);

        // Deactivate share
        mockMvc.perform(delete("/api/v1/ownership-groups/" + group.getId() + "/shares/" + inactiveShare.getId())
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());

        OwnershipShare deactivated = shareRepository.findById(inactiveShare.getId()).orElseThrow();
        assertFalse(deactivated.getIsActive());

        // Reactivate share (75.00% + 25.00% = 100.00% invariant satisfied)
        mockMvc.perform(post("/api/v1/ownership-groups/" + group.getId() + "/shares/" + inactiveShare.getId() + "/reactivate")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());

        OwnershipShare reactivated = shareRepository.findById(inactiveShare.getId()).orElseThrow();
        assertTrue(reactivated.getIsActive());

        // List shares
        mockMvc.perform(get("/api/v1/ownership-groups/" + group.getId() + "/shares")
                        .header("Authorization", "Bearer " + staffToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(2)));
    }

    // =========================================================================
    // 5. OWNERSHIP = 100% INVARIANT
    // =========================================================================
    @Test
    @Order(5)
    @DisplayName("Dimension 05: Ownership = 100% Invariant — Exact 100.00% Verification & Rebalancing")
    void test05_ownership100Percent() throws Exception {
        Vehicle vehicle = createTestVehicle("Genesis GV60", "Genesis", VehicleStatus.AVAILABLE);
        OwnershipGroup group = groupRepository.saveAndFlush(new OwnershipGroup(null, "GV60 Syndicate", vehicle, LocalDate.now(), true));

        // Issue 60% and 40%
        createShare(group, coOwnerUser1, new BigDecimal("60.00"), true);
        createShare(group, coOwnerUser2, new BigDecimal("40.00"), true);

        // Validate exact 100.00%
        mockMvc.perform(post("/api/v1/ownership-groups/" + group.getId() + "/validate")
                        .header("Authorization", "Bearer " + staffToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data", is(100.00)));

        // Rebalance to 50% - 50%
        RebalanceSharesRequest rebalanceReq = new RebalanceSharesRequest(List.of(
                new RebalanceSharesRequest.ShareAllocation(coOwnerUser1.getId(), new BigDecimal("50.00")),
                new RebalanceSharesRequest.ShareAllocation(coOwnerUser2.getId(), new BigDecimal("50.00"))
        ));

        mockMvc.perform(post("/api/v1/ownership-groups/" + group.getId() + "/shares/rebalance")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(rebalanceReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)));
    }

    // =========================================================================
    // 6. INVALID OWNERSHIP
    // =========================================================================
    @Test
    @Order(6)
    @DisplayName("Dimension 06: Invalid Ownership — Rejection of Partial/Excess Allocation and Overselling")
    void test06_invalidOwnership() throws Exception {
        Vehicle vehicle = createTestVehicle("BMW iX", "BMW", VehicleStatus.AVAILABLE);
        OwnershipGroup group = groupRepository.saveAndFlush(new OwnershipGroup(null, "iX Syndicate", vehicle, LocalDate.now(), true));

        // Issue 70% only
        createShare(group, coOwnerUser1, new BigDecimal("70.00"), true);

        // Validate fails (70% != 100.00%)
        mockMvc.perform(post("/api/v1/ownership-groups/" + group.getId() + "/validate")
                        .header("Authorization", "Bearer " + staffToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success", is(false)));

        // Attempt transfer exceeding seller's owned percentage (70% owned, try to transfer 80%)
        TransferShareRequest invalidTransfer = new TransferShareRequest(coOwnerUser1.getId(), coOwnerUser2.getId(), new BigDecimal("80.00"));
        mockMvc.perform(post("/api/v1/ownership-groups/" + group.getId() + "/transfer-share")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidTransfer)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success", is(false)));
    }

    // =========================================================================
    // 7. OWNERSHIP HISTORY
    // =========================================================================
    @Test
    @Order(7)
    @DisplayName("Dimension 07: Ownership History — Append-Only Audit Provenance & History Endpoints")
    void test07_ownershipHistory() throws Exception {
        Vehicle vehicle = createTestVehicle("Volvo EX90", "Volvo", VehicleStatus.AVAILABLE);
        OwnershipGroup group = groupRepository.saveAndFlush(new OwnershipGroup(null, "EX90 Syndicate", vehicle, LocalDate.now(), true));

        // Issue 100%
        mockMvc.perform(post("/api/v1/ownership-groups/" + group.getId() + "/shares")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new IssueOwnershipShareRequest(coOwnerUser1.getId(), new BigDecimal("100.00")))))
                .andExpect(status().isCreated());

        // Transfer 30% to user 2
        mockMvc.perform(post("/api/v1/ownership-groups/" + group.getId() + "/transfer-share")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new TransferShareRequest(coOwnerUser1.getId(), coOwnerUser2.getId(), new BigDecimal("30.00")))))
                .andExpect(status().isOk());

        // Inspect group history (Should have CREATE_OWNERSHIP_SHARE, TRANSFER_EQUITY_OUT, TRANSFER_EQUITY_IN)
        mockMvc.perform(get("/api/v1/ownership-groups/" + group.getId() + "/shares/history")
                        .header("Authorization", "Bearer " + staffToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", not(empty())))
                .andExpect(jsonPath("$.data[*].action", hasItems("TRANSFER_EQUITY_OUT", "TRANSFER_EQUITY_IN")));
    }

    // =========================================================================
    // 8. CONTRACT CREATION
    // =========================================================================
    @Test
    @Order(8)
    @DisplayName("Dimension 08: Contract Creation — Sequential Versioning & Terms Immutability")
    void test08_contractCreation() throws Exception {
        Vehicle vehicle = createTestVehicle("Lotus Eletre", "Lotus", VehicleStatus.AVAILABLE);
        OwnershipGroup group = groupRepository.saveAndFlush(new OwnershipGroup(null, "Eletre Syndicate", vehicle, LocalDate.now(), true));

        CreateContractRequest req1 = new CreateContractRequest();
        req1.setGroupId(group.getId());
        req1.setContractTitle("Syndicate Agreement V1");
        req1.setContractTermsText("Terms governing Lotus Eletre co-ownership version 1.");

        // Create Contract v1
        String v1Resp = mockMvc.perform(post("/api/v1/contracts")
                        .header("Authorization", "Bearer " + staffToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req1)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.version", is(1)))
                .andExpect(jsonPath("$.data.status", is("DRAFT")))
                .andReturn().getResponse().getContentAsString();

        Long contractId1 = objectMapper.readTree(v1Resp).path("data").path("id").asLong();

        // Create Contract v2 (automated version increment to 2)
        CreateContractRequest req2 = new CreateContractRequest();
        req2.setGroupId(group.getId());
        req2.setContractTitle("Syndicate Agreement V2");
        req2.setContractTermsText("Terms governing Lotus Eletre co-ownership version 2.");

        mockMvc.perform(post("/api/v1/contracts")
                        .header("Authorization", "Bearer " + staffToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req2)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.version", is(2)));

        // Move v1 to PENDING_SIGNATURE
        mockMvc.perform(patch("/api/v1/contracts/" + contractId1 + "/status")
                        .header("Authorization", "Bearer " + staffToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new TransitionContractStatusRequest(ContractStatus.PENDING_SIGNATURE, "Ready for signature"))))
                .andExpect(status().isOk());

        // Terms are now legally immutable: attempting update returns 409 Conflict
        UpdateContractRequest updateReq = new UpdateContractRequest();
        updateReq.setContractTitle("Modified Title");
        updateReq.setContractTermsText("Tampered terms");

        mockMvc.perform(put("/api/v1/contracts/" + contractId1)
                        .header("Authorization", "Bearer " + staffToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateReq)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success", is(false)));
    }

    // =========================================================================
    // 9. CONTRACT SIGNATURES
    // =========================================================================
    @Test
    @Order(9)
    @DisplayName("Dimension 09: Contract Signatures — Digital Signature Binding & Duplicate Protection")
    void test09_contractSignatures() throws Exception {
        Vehicle vehicle = createTestVehicle("Nio ET7", "Nio", VehicleStatus.AVAILABLE);
        OwnershipGroup group = groupRepository.saveAndFlush(new OwnershipGroup(null, "ET7 Syndicate", vehicle, LocalDate.now(), true));
        shareRepository.saveAndFlush(new OwnershipShare(null, group, coOwnerUser1, new BigDecimal("100.00"), generateUniqueCert(), Instant.now(), true));

        CoOwnershipContract contract = new CoOwnershipContract();
        contract.setGroup(group);
        contract.setVersion(1);
        contract.setContractTitle("Nio ET7 Agreement");
        contract.setContractTermsText("Legal co-ownership terms.");
        contract.setStatus(ContractStatus.PENDING_SIGNATURE);
        contract = contractRepository.saveAndFlush(contract);

        // Sign contract
        SignContractRequest signReq = new SignContractRequest(true);
        mockMvc.perform(post("/api/v1/contracts/" + contract.getId() + "/sign")
                        .header("Authorization", "Bearer " + coOwnerToken1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(signReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.signatureHash", notNullValue()));

        // Duplicate signature attempt rejected (409 Conflict)
        mockMvc.perform(post("/api/v1/contracts/" + contract.getId() + "/sign")
                        .header("Authorization", "Bearer " + coOwnerToken1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(signReq)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success", is(false)));

        // Non-member signature attempt rejected (403 Forbidden)
        mockMvc.perform(post("/api/v1/contracts/" + contract.getId() + "/sign")
                        .header("Authorization", "Bearer " + nonMemberToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(signReq)))
                .andExpect(status().isForbidden());
    }

    // =========================================================================
    // 10. CONTRACT LIFECYCLE
    // =========================================================================
    @Test
    @Order(10)
    @DisplayName("Dimension 10: Contract Lifecycle — Full Lifecycle Progression & Superseding Activation")
    void test10_contractLifecycle() throws Exception {
        Vehicle vehicle = createTestVehicle("Lucid Air Dream", "Lucid", VehicleStatus.AVAILABLE);
        OwnershipGroup group = groupRepository.saveAndFlush(new OwnershipGroup(null, "Dream Syndicate", vehicle, LocalDate.now(), true));

        CoOwnershipContract v1 = new CoOwnershipContract();
        v1.setGroup(group);
        v1.setVersion(1);
        v1.setContractTitle("Dream Agreement v1");
        v1.setContractTermsText("Terms v1");
        v1.setStatus(ContractStatus.DRAFT);
        v1 = contractRepository.saveAndFlush(v1);

        // DRAFT -> PENDING_SIGNATURE
        mockMvc.perform(patch("/api/v1/contracts/" + v1.getId() + "/status")
                        .header("Authorization", "Bearer " + staffToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new TransitionContractStatusRequest(ContractStatus.PENDING_SIGNATURE, "Ready"))))
                .andExpect(status().isOk());

        // PENDING_SIGNATURE -> SIGNED
        mockMvc.perform(patch("/api/v1/contracts/" + v1.getId() + "/status")
                        .header("Authorization", "Bearer " + staffToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new TransitionContractStatusRequest(ContractStatus.SIGNED, "All signed"))))
                .andExpect(status().isOk());

        // SIGNED -> ACTIVE
        mockMvc.perform(patch("/api/v1/contracts/" + v1.getId() + "/status")
                        .header("Authorization", "Bearer " + staffToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new TransitionContractStatusRequest(ContractStatus.ACTIVE, "Activated"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status", is("ACTIVE")));

        // Activate Contract v2 -> Automatically supersedes v1 (terminates v1)
        CoOwnershipContract v2 = new CoOwnershipContract();
        v2.setGroup(group);
        v2.setVersion(2);
        v2.setContractTitle("Dream Agreement v2");
        v2.setContractTermsText("Terms v2");
        v2.setStatus(ContractStatus.SIGNED);
        v2 = contractRepository.saveAndFlush(v2);

        mockMvc.perform(patch("/api/v1/contracts/" + v2.getId() + "/status")
                        .header("Authorization", "Bearer " + staffToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new TransitionContractStatusRequest(ContractStatus.ACTIVE, "Superseed activation"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status", is("ACTIVE")));

        // Verify v1 is now TERMINATED
        CoOwnershipContract refreshedV1 = contractRepository.findById(v1.getId()).orElseThrow();
        assertEquals(ContractStatus.TERMINATED, refreshedV1.getStatus());

        // ACTIVE -> EXPIRED
        mockMvc.perform(patch("/api/v1/contracts/" + v2.getId() + "/status")
                        .header("Authorization", "Bearer " + staffToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new TransitionContractStatusRequest(ContractStatus.EXPIRED, "Contract term expired"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status", is("EXPIRED")));
    }

    // =========================================================================
    // 11. ROLE-BASED ACCESS CONTROL (RBAC)
    // =========================================================================
    @Test
    @Order(11)
    @DisplayName("Dimension 11: RBAC — Positive & Negative Authorization Boundaries")
    void test11_rbac() throws Exception {
        // 1. Co-Owner cannot register vehicle (Admin only)
        CreateVehicleRequest vehReq = new CreateVehicleRequest();
        vehReq.setVin(generateUniqueVin());
        vehReq.setLicensePlate(generateUniquePlate());
        vehReq.setModelName("Tesla Model S");
        vehReq.setManufacturer("Tesla");
        vehReq.setModel3dAssetPath("models/s.glb");

        mockMvc.perform(post("/api/v1/vehicles")
                        .header("Authorization", "Bearer " + coOwnerToken1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(vehReq)))
                .andExpect(status().isForbidden());

        // 2. Co-Owner cannot form ownership group (Admin only)
        CreateOwnershipGroupRequest groupReq = new CreateOwnershipGroupRequest();
        groupReq.setGroupName("Unauthorized Group");
        groupReq.setVehicleId(1L);

        mockMvc.perform(post("/api/v1/ownership-groups")
                        .header("Authorization", "Bearer " + coOwnerToken1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(groupReq)))
                .andExpect(status().isForbidden());

        // 3. Data Scoping: Non-member co-owner cannot view group details
        Vehicle vehicle = createTestVehicle("Zeekr 001", "Zeekr", VehicleStatus.AVAILABLE);
        OwnershipGroup privateGroup = groupRepository.saveAndFlush(new OwnershipGroup(null, "Private Syndicate", vehicle, LocalDate.now(), true));
        shareRepository.saveAndFlush(new OwnershipShare(null, privateGroup, coOwnerUser1, new BigDecimal("100.00"), generateUniqueCert(), Instant.now(), true));

        mockMvc.perform(get("/api/v1/ownership-groups/" + privateGroup.getId())
                        .header("Authorization", "Bearer " + nonMemberToken))
                .andExpect(status().isForbidden());

        // 4. Unauthenticated request rejected (401 Unauthorized)
        mockMvc.perform(get("/api/v1/vehicles"))
                .andExpect(status().isUnauthorized());
    }

    // =========================================================================
    // 12. VALIDATION
    // =========================================================================
    @Test
    @Order(12)
    @DisplayName("Dimension 12: Validation — DTO Constraints & Consistent Error Structure")
    void test12_validation() throws Exception {
        // Invalid VIN (Non-17 chars)
        CreateVehicleRequest invalidVinReq = new CreateVehicleRequest();
        invalidVinReq.setVin("BAD_VIN");
        invalidVinReq.setLicensePlate("29A-12345");
        invalidVinReq.setModelName("Smart #1");
        invalidVinReq.setManufacturer("Smart");
        invalidVinReq.setModel3dAssetPath("models/smart.glb");

        mockMvc.perform(post("/api/v1/vehicles")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidVinReq)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.validationErrors", notNullValue()));

        // Invalid Contract Request (Blank title & terms)
        CreateContractRequest invalidContractReq = new CreateContractRequest();
        invalidContractReq.setGroupId(1L);
        invalidContractReq.setContractTitle("");
        invalidContractReq.setContractTermsText("");

        mockMvc.perform(post("/api/v1/contracts")
                        .header("Authorization", "Bearer " + staffToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidContractReq)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success", is(false)));
    }

    // =========================================================================
    // 13. TRANSACTIONS
    // =========================================================================
    @Test
    @Order(13)
    @DisplayName("Dimension 13: Transactions — Atomicity & Rollback Integrity on Failure")
    void test13_transactions() throws Exception {
        Vehicle vehicle = createTestVehicle("Lucid Air Sapphire", "Lucid", VehicleStatus.AVAILABLE);
        OwnershipGroup group = groupRepository.saveAndFlush(new OwnershipGroup(null, "Sapphire Syndicate", vehicle, LocalDate.now(), true));

        // Initial distribution: user1 = 70.00%, user2 = 30.00%
        shareRepository.saveAndFlush(new OwnershipShare(null, group, coOwnerUser1, new BigDecimal("70.00"), generateUniqueCert(), Instant.now(), true));
        shareRepository.saveAndFlush(new OwnershipShare(null, group, coOwnerUser2, new BigDecimal("30.00"), generateUniqueCert(), Instant.now(), true));

        // Attempt transfer that throws BusinessException (transferring 80% when user1 only has 70%)
        TransferShareRequest failingTransfer = new TransferShareRequest(coOwnerUser1.getId(), coOwnerUser2.getId(), new BigDecimal("80.00"));

        mockMvc.perform(post("/api/v1/ownership-groups/" + group.getId() + "/transfer-share")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(failingTransfer)))
                .andExpect(status().isBadRequest());

        // Verify transaction rolled back completely: shares remain untouched at 70.00% and 30.00%
        List<OwnershipShare> shares = shareRepository.findByGroupIdAndIsActiveTrue(group.getId());
        assertEquals(2, shares.size());

        OwnershipShare u1Share = shares.stream().filter(s -> s.getUser().getId().equals(coOwnerUser1.getId())).findFirst().orElseThrow();
        OwnershipShare u2Share = shares.stream().filter(s -> s.getUser().getId().equals(coOwnerUser2.getId())).findFirst().orElseThrow();

        assertEquals(0, new BigDecimal("70.00").compareTo(u1Share.getPercentage()), "User 1 share must remain 70.00% due to transaction rollback");
        assertEquals(0, new BigDecimal("30.00").compareTo(u2Share.getPercentage()), "User 2 share must remain 30.00% due to transaction rollback");
    }
}
