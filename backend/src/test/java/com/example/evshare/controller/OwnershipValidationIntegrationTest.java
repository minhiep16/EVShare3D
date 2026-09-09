package com.example.evshare.controller;

import com.example.evshare.dto.request.IssueOwnershipShareRequest;
import com.example.evshare.dto.request.RebalanceSharesRequest;
import com.example.evshare.dto.request.TransferShareRequest;
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

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("Checkpoint 04-G — Ownership 100% Validation Integration Tests")
class OwnershipValidationIntegrationTest {

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
    private User coOwnerA;
    private User coOwnerB;
    private User coOwnerC;

    private String adminToken;

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
        vehicle.setModel3dAssetPath("models/vehicles/vf8.glb");
        vehicle.setStatus(VehicleStatus.AVAILABLE);
        vehicle.setBatteryLevel(100);
        vehicle.setOdometerKm(BigDecimal.ZERO);
        vehicle.setStallLocationCode("BAY-10");
        return vehicleRepository.saveAndFlush(vehicle);
    }

    private OwnershipGroup createGroup(String name) {
        String uid = UUID.randomUUID().toString().substring(0, 8);
        Vehicle vehicle = createVehicle("VIN_VLD_" + uid, "51H-" + uid);
        OwnershipGroup group = new OwnershipGroup();
        group.setGroupName(name);
        group.setVehicle(vehicle);
        group.setFormationDate(LocalDate.now());
        group.setIsActive(true);
        return ownershipGroupRepository.saveAndFlush(group);
    }

    private OwnershipShare createShare(OwnershipGroup group, User user, BigDecimal percentage, boolean active) {
        OwnershipShare share = new OwnershipShare();
        share.setGroup(group);
        share.setUser(user);
        share.setPercentage(percentage);
        share.setShareCertificateNumber("CERT-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        share.setAcquiredAt(Instant.now());
        share.setIsActive(active);
        return ownershipShareRepository.saveAndFlush(share);
    }

    @BeforeEach
    void setUp() {
        String uid = UUID.randomUUID().toString().substring(0, 8);
        adminUser = createUser("admin_vld_" + uid + "@evshare.vn", RoleName.ROLE_ADMIN);
        coOwnerA = createUser("coownera_" + uid + "@evshare.vn", RoleName.ROLE_CO_OWNER);
        coOwnerB = createUser("coownerb_" + uid + "@evshare.vn", RoleName.ROLE_CO_OWNER);
        coOwnerC = createUser("coownerc_" + uid + "@evshare.vn", RoleName.ROLE_CO_OWNER);

        adminToken = tokenService.generateAccessToken(adminUser.getId(), adminUser.getEmail(), Collections.singletonList("ROLE_ADMIN"));
    }

    // =========================================================================
    // 1. EXACTLY 100% TESTS
    // =========================================================================

    @Test
    @DisplayName("1.1. Exactly 100.00%: Group with shares summing to 100.00% passes validation")
    void testValidateExactly100Percent() throws Exception {
        OwnershipGroup group = createGroup("Balanced Syndicate");
        createShare(group, coOwnerA, new BigDecimal("60.00"), true);
        createShare(group, coOwnerB, new BigDecimal("40.00"), true);

        mockMvc.perform(post("/api/v1/ownership-groups/{id}/validate", group.getId())
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data", is(100.0)));
    }

    @Test
    @DisplayName("1.2. Exactly 100.00%: Multi-owner split (33.34% + 33.33% + 33.33% = 100.00%) passes")
    void testValidateThreeWaySplit100Percent() throws Exception {
        OwnershipGroup group = createGroup("Tri-Share Syndicate");
        createShare(group, coOwnerA, new BigDecimal("33.34"), true);
        createShare(group, coOwnerB, new BigDecimal("33.33"), true);
        createShare(group, coOwnerC, new BigDecimal("33.33"), true);

        mockMvc.perform(post("/api/v1/ownership-groups/{id}/validate", group.getId())
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", is(100.0)));
    }

    // =========================================================================
    // 2. LESS THAN 100% TESTS
    // =========================================================================

    @Test
    @DisplayName("2.1. Less than 100%: Sum of 99.99% is rejected with meaningful error message")
    void testValidateLessThan100PercentBoundary() throws Exception {
        OwnershipGroup group = createGroup("Underallocated Syndicate");
        createShare(group, coOwnerA, new BigDecimal("50.00"), true);
        createShare(group, coOwnerB, new BigDecimal("49.99"), true); // Total = 99.99%

        mockMvc.perform(post("/api/v1/ownership-groups/{id}/validate", group.getId())
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.message", containsString("less than 100.00%")))
                .andExpect(jsonPath("$.message", containsString("99.99")));
    }

    @Test
    @DisplayName("2.2. Less than 100%: Incomplete group with 60.00% triggers InvalidOwnershipDistributionException")
    void testValidateSubstantiallyUnderallocated() throws Exception {
        OwnershipGroup group = createGroup("Partial Syndicate");
        createShare(group, coOwnerA, new BigDecimal("60.00"), true);

        mockMvc.perform(post("/api/v1/ownership-groups/{id}/validate", group.getId())
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.message", containsString("less than 100.00%")))
                .andExpect(jsonPath("$.message", containsString("60.00")));
    }

    // =========================================================================
    // 3. GREATER THAN 100% TESTS
    // =========================================================================

    @Test
    @DisplayName("3.1. Greater than 100%: Sum of 100.01% is rejected with meaningful error message")
    void testValidateGreaterThan100PercentBoundary() throws Exception {
        OwnershipGroup group = createGroup("Overallocated Syndicate");
        createShare(group, coOwnerA, new BigDecimal("50.01"), true);
        createShare(group, coOwnerB, new BigDecimal("50.00"), true); // Total = 100.01%

        mockMvc.perform(post("/api/v1/ownership-groups/{id}/validate", group.getId())
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.message", containsString("greater than 100.00%")))
                .andExpect(jsonPath("$.message", containsString("100.01")));
    }

    @Test
    @DisplayName("3.2. Greater than 100%: Total of 120.00% is rejected")
    void testValidateSubstantiallyOverallocated() throws Exception {
        OwnershipGroup group = createGroup("Massive Overallocated Syndicate");
        createShare(group, coOwnerA, new BigDecimal("70.00"), true);
        createShare(group, coOwnerB, new BigDecimal("50.00"), true); // Total = 120.00%

        mockMvc.perform(post("/api/v1/ownership-groups/{id}/validate", group.getId())
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.message", containsString("greater than 100.00%")));
    }

    // =========================================================================
    // 4. ADDING SHARE TESTS
    // =========================================================================

    @Test
    @DisplayName("4.1. Adding share: Issuing share that brings total to exactly 100.00% succeeds")
    void testAddShareBringingTotalTo100Succeeds() throws Exception {
        OwnershipGroup group = createGroup("Growing Syndicate");
        createShare(group, coOwnerA, new BigDecimal("70.00"), true);

        // Add coOwnerB with 30.00% -> Total = 70.00 + 30.00 = 100.00%
        IssueOwnershipShareRequest request = new IssueOwnershipShareRequest(coOwnerB.getId(), new BigDecimal("30.00"));

        mockMvc.perform(post("/api/v1/ownership-groups/{groupId}/shares", group.getId())
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.percentage", is(30.00)));

        BigDecimal sum = ownershipShareRepository.sumActivePercentagesByGroupId(group.getId());
        assertEquals(new BigDecimal("100.00"), sum);
    }

    @Test
    @DisplayName("4.2. Adding share: Issuing share that leaves total < 100.00% fails and prevents invalid state")
    void testAddShareLeavingTotalUnder100Fails() throws Exception {
        OwnershipGroup group = createGroup("Underallocated Adding Syndicate");
        createShare(group, coOwnerA, new BigDecimal("60.00"), true);

        // Add coOwnerB with 20.00% -> Total would be 80.00% (< 100.00%)
        IssueOwnershipShareRequest request = new IssueOwnershipShareRequest(coOwnerB.getId(), new BigDecimal("20.00"));

        mockMvc.perform(post("/api/v1/ownership-groups/{groupId}/shares", group.getId())
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.message", containsString("less than 100.00%")))
                .andExpect(jsonPath("$.message", containsString("80.00")));

        // Verify share was NOT added
        assertFalse(ownershipShareRepository.findByGroupIdAndUserId(group.getId(), coOwnerB.getId()).isPresent());
    }

    @Test
    @DisplayName("4.3. Adding share: Issuing share that causes total > 100.00% fails and rolls back")
    void testAddShareExceeding100Fails() throws Exception {
        OwnershipGroup group = createGroup("Cap Exceeded Syndicate");
        createShare(group, coOwnerA, new BigDecimal("60.00"), true);

        // Add coOwnerB with 50.00% -> Total would be 110.00% (> 100.00%)
        IssueOwnershipShareRequest request = new IssueOwnershipShareRequest(coOwnerB.getId(), new BigDecimal("50.00"));

        mockMvc.perform(post("/api/v1/ownership-groups/{groupId}/shares", group.getId())
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.message", containsString("greater than 100.00%")))
                .andExpect(jsonPath("$.message", containsString("110.00")));

        assertFalse(ownershipShareRepository.findByGroupIdAndUserId(group.getId(), coOwnerB.getId()).isPresent());
    }

    // =========================================================================
    // 5. UPDATING SHARE TESTS
    // =========================================================================

    @Test
    @DisplayName("5.1. Updating share: Updating share to achieve exact 100.00% succeeds")
    void testUpdateShareToExact100Succeeds() throws Exception {
        OwnershipGroup group = createGroup("Update Balanced Syndicate");
        OwnershipShare shareA = createShare(group, coOwnerA, new BigDecimal("60.00"), true);
        OwnershipShare shareB = createShare(group, coOwnerB, new BigDecimal("30.00"), true); // Sum = 90.00%

        // Update shareB from 30.00% to 40.00% -> Sum becomes 60.00 + 40.00 = 100.00%
        UpdateOwnershipShareRequest updateReq = new UpdateOwnershipShareRequest(new BigDecimal("40.00"), null);

        mockMvc.perform(patch("/api/v1/ownership-groups/{groupId}/shares/{shareId}", group.getId(), shareB.getId())
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.percentage", is(40.00)));

        BigDecimal sum = ownershipShareRepository.sumActivePercentagesByGroupId(group.getId());
        assertEquals(new BigDecimal("100.00"), sum);
    }

    @Test
    @DisplayName("5.2. Updating share: Updating share resulting in < 100.00% fails and preserves original")
    void testUpdateShareResultingUnder100Fails() throws Exception {
        OwnershipGroup group = createGroup("Update Under Syndicate");
        OwnershipShare shareA = createShare(group, coOwnerA, new BigDecimal("60.00"), true);
        OwnershipShare shareB = createShare(group, coOwnerB, new BigDecimal("40.00"), true); // Sum = 100.00%

        // Update shareB to 30.00% -> Total would be 60.00 + 30.00 = 90.00%
        UpdateOwnershipShareRequest updateReq = new UpdateOwnershipShareRequest(new BigDecimal("30.00"), null);

        mockMvc.perform(patch("/api/v1/ownership-groups/{groupId}/shares/{shareId}", group.getId(), shareB.getId())
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateReq)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.message", containsString("less than 100.00%")))
                .andExpect(jsonPath("$.message", containsString("90.00")));

        OwnershipShare fromDb = ownershipShareRepository.findById(shareB.getId()).orElseThrow();
        assertEquals(new BigDecimal("40.00"), fromDb.getPercentage());
    }

    @Test
    @DisplayName("5.3. Updating share: Updating share resulting in > 100.00% fails and preserves original")
    void testUpdateShareResultingOver100Fails() throws Exception {
        OwnershipGroup group = createGroup("Update Over Syndicate");
        OwnershipShare shareA = createShare(group, coOwnerA, new BigDecimal("60.00"), true);
        OwnershipShare shareB = createShare(group, coOwnerB, new BigDecimal("40.00"), true); // Sum = 100.00%

        // Update shareB to 50.00% -> Total would be 60.00 + 50.00 = 110.00%
        UpdateOwnershipShareRequest updateReq = new UpdateOwnershipShareRequest(new BigDecimal("50.00"), null);

        mockMvc.perform(patch("/api/v1/ownership-groups/{groupId}/shares/{shareId}", group.getId(), shareB.getId())
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateReq)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.message", containsString("greater than 100.00%")))
                .andExpect(jsonPath("$.message", containsString("110.00")));

        OwnershipShare fromDb = ownershipShareRepository.findById(shareB.getId()).orElseThrow();
        assertEquals(new BigDecimal("40.00"), fromDb.getPercentage());
    }

    // =========================================================================
    // 6. REMOVING SHARE TESTS (PREVENT INVALID FINAL STATE)
    // =========================================================================

    @Test
    @DisplayName("6.1. Removing share: Deactivating an active share from 100% group is prevented to avoid invalid final state")
    void testRemoveActiveSharePrevented() throws Exception {
        OwnershipGroup group = createGroup("Removal Invariant Syndicate");
        OwnershipShare shareA = createShare(group, coOwnerA, new BigDecimal("60.00"), true);
        OwnershipShare shareB = createShare(group, coOwnerB, new BigDecimal("40.00"), true); // Sum = 100.00%

        // Deactivating shareB would leave group with 60.00% (< 100.00%)
        mockMvc.perform(delete("/api/v1/ownership-groups/{groupId}/shares/{shareId}", group.getId(), shareB.getId())
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.message", containsString("Cannot remove ownership share")))
                .andExpect(jsonPath("$.message", containsString("violates 100% equity invariant")));

        OwnershipShare fromDb = ownershipShareRepository.findById(shareB.getId()).orElseThrow();
        assertTrue(fromDb.getIsActive(), "Share must remain active because removal was rejected");
    }

    // =========================================================================
    // 7. CONCURRENT TRANSACTION SCENARIO
    // =========================================================================

    @Test
    @DisplayName("7.1. Concurrency: Two concurrent transactions attempting to add shares to same group are serialized without invariant corruption")
    void testConcurrentShareAdditionSerializesCorrectly() throws Exception {
        OwnershipGroup group = createGroup("Concurrent Race Syndicate");
        createShare(group, coOwnerA, new BigDecimal("50.00"), true); // Group currently has 50.00%

        // Both threads attempt to issue 50.00% to fill the remaining equity
        IssueOwnershipShareRequest requestB = new IssueOwnershipShareRequest(coOwnerB.getId(), new BigDecimal("50.00"));
        IssueOwnershipShareRequest requestC = new IssueOwnershipShareRequest(coOwnerC.getId(), new BigDecimal("50.00"));

        int numThreads = 2;
        ExecutorService executor = Executors.newFixedThreadPool(numThreads);
        CountDownLatch readyLatch = new CountDownLatch(numThreads);
        CountDownLatch startLatch = new CountDownLatch(1);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);

        Callable<Integer> taskB = () -> {
            readyLatch.countDown();
            startLatch.await();
            try {
                mockMvc.perform(post("/api/v1/ownership-groups/{groupId}/shares", group.getId())
                                .header("Authorization", "Bearer " + adminToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(requestB)))
                        .andExpect(result -> {
                            int status = result.getResponse().getStatus();
                            if (status == 201) successCount.incrementAndGet();
                            else if (status == 400) failureCount.incrementAndGet();
                        });
                return 201;
            } catch (Exception e) {
                failureCount.incrementAndGet();
                return 500;
            }
        };

        Callable<Integer> taskC = () -> {
            readyLatch.countDown();
            startLatch.await();
            try {
                mockMvc.perform(post("/api/v1/ownership-groups/{groupId}/shares", group.getId())
                                .header("Authorization", "Bearer " + adminToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(requestC)))
                        .andExpect(result -> {
                            int status = result.getResponse().getStatus();
                            if (status == 201) successCount.incrementAndGet();
                            else if (status == 400) failureCount.incrementAndGet();
                        });
                return 201;
            } catch (Exception e) {
                failureCount.incrementAndGet();
                return 500;
            }
        };

        Future<Integer> f1 = executor.submit(taskB);
        Future<Integer> f2 = executor.submit(taskC);

        readyLatch.await(5, TimeUnit.SECONDS);
        startLatch.countDown(); // Release both threads at the exact same instant

        f1.get(10, TimeUnit.SECONDS);
        f2.get(10, TimeUnit.SECONDS);
        executor.shutdown();

        // Exactly one transaction must succeed (201 Created) and one must fail (400 Bad Request)
        assertEquals(1, successCount.get(), "Exactly one concurrent share issuance must succeed");
        assertEquals(1, failureCount.get(), "Competing concurrent share issuance must be rejected with 400");

        // The final active sum in database must equal exactly 100.00% (50.00 + 50.00 = 100.00, NEVER 150.00%)
        BigDecimal finalSum = ownershipShareRepository.sumActivePercentagesByGroupId(group.getId());
        assertEquals(new BigDecimal("100.00"), finalSum, "Active percentages must strictly sum to 100.00% under concurrent race");
    }

    // =========================================================================
    // 8. EQUITY TRANSFER & BATCH REBALANCING TESTS
    // =========================================================================

    @Test
    @DisplayName("8.1. Transfer share: Admin transfers 20.00% from Co-Owner A to Co-Owner B, preserving 100.00%")
    void testTransferSharePreserves100Percent() throws Exception {
        OwnershipGroup group = createGroup("Transfer Syndicate");
        createShare(group, coOwnerA, new BigDecimal("60.00"), true);
        createShare(group, coOwnerB, new BigDecimal("40.00"), true); // Sum = 100.00%

        TransferShareRequest transferReq = new TransferShareRequest(coOwnerA.getId(), coOwnerB.getId(), new BigDecimal("20.00"));

        mockMvc.perform(post("/api/v1/ownership-groups/{id}/transfer-share", group.getId())
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(transferReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.memberShares", hasSize(2)));

        OwnershipShare aDb = ownershipShareRepository.findByGroupIdAndUserId(group.getId(), coOwnerA.getId()).orElseThrow();
        OwnershipShare bDb = ownershipShareRepository.findByGroupIdAndUserId(group.getId(), coOwnerB.getId()).orElseThrow();

        assertEquals(new BigDecimal("40.00"), aDb.getPercentage());
        assertEquals(new BigDecimal("60.00"), bDb.getPercentage());

        BigDecimal total = ownershipShareRepository.sumActivePercentagesByGroupId(group.getId());
        assertEquals(new BigDecimal("100.00"), total);
    }

    @Test
    @DisplayName("8.2. Transfer share: Transfer exceeding seller's stake is rejected with HTTP 400")
    void testTransferExceedingSellerStakeRejected() throws Exception {
        OwnershipGroup group = createGroup("Overdraw Transfer Syndicate");
        createShare(group, coOwnerA, new BigDecimal("30.00"), true);
        createShare(group, coOwnerB, new BigDecimal("70.00"), true);

        // Seller has only 30.00%, attempts to transfer 40.00%
        TransferShareRequest transferReq = new TransferShareRequest(coOwnerA.getId(), coOwnerB.getId(), new BigDecimal("40.00"));

        mockMvc.perform(post("/api/v1/ownership-groups/{id}/transfer-share", group.getId())
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(transferReq)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.message", containsString("insufficient for transfer")));
    }

    @Test
    @DisplayName("8.3. Rebalancing: Admin atomically rebalances group across 3 co-owners to 100.00%")
    void testAtomicRebalanceSucceeds() throws Exception {
        OwnershipGroup group = createGroup("Rebalance Syndicate");
        createShare(group, coOwnerA, new BigDecimal("50.00"), true);
        createShare(group, coOwnerB, new BigDecimal("50.00"), true);

        List<RebalanceSharesRequest.ShareAllocation> allocations = Arrays.asList(
                new RebalanceSharesRequest.ShareAllocation(coOwnerA.getId(), new BigDecimal("40.00")),
                new RebalanceSharesRequest.ShareAllocation(coOwnerB.getId(), new BigDecimal("35.00")),
                new RebalanceSharesRequest.ShareAllocation(coOwnerC.getId(), new BigDecimal("25.00"))
        );
        RebalanceSharesRequest rebalanceReq = new RebalanceSharesRequest(allocations);

        mockMvc.perform(post("/api/v1/ownership-groups/{groupId}/shares/rebalance", group.getId())
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(rebalanceReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.memberShares", hasSize(3)));

        BigDecimal total = ownershipShareRepository.sumActivePercentagesByGroupId(group.getId());
        assertEquals(new BigDecimal("100.00"), total);
    }

    @Test
    @DisplayName("8.4. Rebalancing: Rebalance allocations summing to 95.00% (< 100%) rejected")
    void testAtomicRebalanceUnder100Rejected() throws Exception {
        OwnershipGroup group = createGroup("Bad Rebalance Syndicate");
        createShare(group, coOwnerA, new BigDecimal("50.00"), true);
        createShare(group, coOwnerB, new BigDecimal("50.00"), true);

        List<RebalanceSharesRequest.ShareAllocation> allocations = Arrays.asList(
                new RebalanceSharesRequest.ShareAllocation(coOwnerA.getId(), new BigDecimal("40.00")),
                new RebalanceSharesRequest.ShareAllocation(coOwnerB.getId(), new BigDecimal("35.00")),
                new RebalanceSharesRequest.ShareAllocation(coOwnerC.getId(), new BigDecimal("20.00")) // Sum = 95.00%
        );
        RebalanceSharesRequest rebalanceReq = new RebalanceSharesRequest(allocations);

        mockMvc.perform(post("/api/v1/ownership-groups/{groupId}/shares/rebalance", group.getId())
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(rebalanceReq)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.message", containsString("Total allocations must sum to exactly 100.00%")));
    }
}
