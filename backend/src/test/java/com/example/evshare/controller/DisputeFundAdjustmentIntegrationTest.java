package com.example.evshare.controller;

import com.example.evshare.dto.request.AdminArbitrateDisputeRequest;
import com.example.evshare.dto.request.CreateDisputeEvidenceRequest;
import com.example.evshare.dto.request.CreateDisputeRequest;
import com.example.evshare.dto.request.DisputeFundAdjustmentRequest;
import com.example.evshare.entity.*;
import com.example.evshare.entity.enums.*;
import com.example.evshare.exception.InsufficientFundBalanceException;
import com.example.evshare.repository.*;
import com.example.evshare.security.UserPrincipal;
import com.example.evshare.service.DisputeService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@DisplayName("Checkpoint 07-O — Dispute SharedFund Adjustment Integration Tests")
class DisputeFundAdjustmentIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @Autowired private UserRepository userRepository;
    @Autowired private RoleRepository roleRepository;
    @Autowired private VehicleRepository vehicleRepository;
    @Autowired private OwnershipGroupRepository ownershipGroupRepository;
    @Autowired private OwnershipShareRepository ownershipShareRepository;
    @Autowired private DisputeRepository disputeRepository;
    @Autowired private DisputeEvidenceRepository disputeEvidenceRepository;
    @Autowired private SharedFundRepository sharedFundRepository;
    @Autowired private FundTransactionRepository fundTransactionRepository;
    @Autowired private AuditLogRepository auditLogRepository;
    @Autowired private DisputeService disputeService;
    @Autowired private PlatformTransactionManager transactionManager;

    private static final AtomicLong PHONE_SEQ = new AtomicLong(System.currentTimeMillis() % 80000000L + 20000000L);

    private User activeOwner;
    private User respondentOwner;
    private User staffUser;
    private User adminUser;
    private OwnershipGroup testGroup;
    private Vehicle testVehicle;
    private SharedFund testFund;

    @BeforeEach
    void setUp() {
        Role roleCoOwner = roleRepository.findByName(RoleName.ROLE_CO_OWNER)
                .orElseGet(() -> roleRepository.save(new Role(null, RoleName.ROLE_CO_OWNER)));
        Role roleStaff = roleRepository.findByName(RoleName.ROLE_STAFF)
                .orElseGet(() -> roleRepository.save(new Role(null, RoleName.ROLE_STAFF)));
        Role roleAdmin = roleRepository.findByName(RoleName.ROLE_ADMIN)
                .orElseGet(() -> roleRepository.save(new Role(null, RoleName.ROLE_ADMIN)));

        String uid = UUID.randomUUID().toString().substring(0, 8);

        activeOwner = createTestUser("fa.active." + uid + "@evshare.io", "Active Owner " + uid, roleCoOwner);
        respondentOwner = createTestUser("fa.resp." + uid + "@evshare.io", "Respondent Owner " + uid, roleCoOwner);
        staffUser = createTestUser("fa.staff." + uid + "@evshare.io", "Staff Reviewer " + uid, roleStaff);
        adminUser = createTestUser("fa.admin." + uid + "@evshare.io", "Admin Operator " + uid, roleAdmin);

        testVehicle = new Vehicle();
        testVehicle.setVin("VIN" + uid.toUpperCase() + "DISP00");
        testVehicle.setLicensePlate("51H-" + uid.substring(0, 4).toUpperCase());
        testVehicle.setModelName("VinFast VF8 Plus");
        testVehicle.setManufacturer("VinFast");
        testVehicle.setModel3dAssetPath("models/vehicles/vf8.glb");
        testVehicle.setStatus(VehicleStatus.AVAILABLE);
        testVehicle.setBatteryLevel(90);
        testVehicle.setOdometerKm(new BigDecimal("12000.00"));
        testVehicle.setStallLocationCode("BAY-15");
        testVehicle = vehicleRepository.saveAndFlush(testVehicle);

        testGroup = new OwnershipGroup();
        testGroup.setGroupName("Syndicate Fund Adj " + uid);
        testGroup.setVehicle(testVehicle);
        testGroup.setFormationDate(LocalDate.of(2026, 1, 1));
        testGroup.setIsActive(true);
        testGroup = ownershipGroupRepository.saveAndFlush(testGroup);

        assignShare(testGroup, activeOwner, new BigDecimal("60.00"), true);
        assignShare(testGroup, respondentOwner, new BigDecimal("40.00"), true);

        testFund = new SharedFund();
        testFund.setGroup(testGroup);
        testFund.setCurrentBalance(new BigDecimal("5000000.00"));
        testFund.setMinimumReserveThreshold(new BigDecimal("1000000.00"));
        testFund.setCurrency("VND");
        testFund.setUpdatedAt(Instant.now());
        testFund = sharedFundRepository.saveAndFlush(testFund);
    }

    private User createTestUser(String email, String fullName, Role role) {
        User u = new User();
        u.setEmail(email);
        u.setPasswordHash("$2a$12$dummyPasswordHashForDisputeFundAdjustmentTesting12345");
        u.setFullName(fullName);
        u.setPhoneNumber(String.format("09%08d", PHONE_SEQ.incrementAndGet() % 100000000L));
        u.setIsActive(true);
        u.getRoles().add(role);
        return userRepository.saveAndFlush(u);
    }

    private OwnershipShare assignShare(OwnershipGroup group, User user, BigDecimal percentage, boolean isActive) {
        OwnershipShare share = new OwnershipShare();
        share.setGroup(group);
        share.setUser(user);
        share.setPercentage(percentage);
        share.setShareCertificateNumber("CERT-" + UUID.randomUUID().toString().substring(0, 8));
        share.setIsActive(isActive);
        return ownershipShareRepository.saveAndFlush(share);
    }

    private Dispute createDisputeWithEvidence(DisputeStatus status) {
        Dispute d = new Dispute();
        d.setGroup(testGroup);
        d.setComplainantUser(activeOwner);
        d.setRespondentUser(respondentOwner);
        d.setTitle("Dispute on vehicle rear bumper scratch");
        d.setDescription("Rear bumper scratched during respondent usage window");
        d.setStatus(status);
        d.setCreatedAt(Instant.now());
        d = disputeRepository.saveAndFlush(d);

        DisputeEvidence ev = new DisputeEvidence();
        ev.setDispute(d);
        ev.setUploadedByUser(activeOwner);
        ev.setFileUrl("https://storage.evshare.io/evidences/rear_bumper.jpg");
        ev.setMesh3dDefectCoordinates("{\"x\": -0.2, \"y\": 0.8, \"z\": -1.5}");
        ev.setDescription("Rear bumper scratch photo with coordinates");
        ev.setCreatedAt(Instant.now());
        disputeEvidenceRepository.saveAndFlush(ev);

        return d;
    }

    @Nested
    @DisplayName("1. Successful Dispute Fund Adjustment")
    class SuccessfulFundAdjustmentTests {

        @Test
        @DisplayName("Admin executes DEBIT adjustment: balance deducted, transaction recorded, dispute resolved")
        void testSuccessfulDebitAdjustment() throws Exception {
            Dispute dispute = createDisputeWithEvidence(DisputeStatus.UNDER_REVIEW);
            BigDecimal debitAmount = new BigDecimal("1000000.00");

            DisputeFundAdjustmentRequest request = new DisputeFundAdjustmentRequest(
                    "Inspection confirms bumper scratch during rental period",
                    "Award 1,000,000 VND reimbursement from syndicate shared fund to cover bumper repair",
                    debitAmount,
                    TransactionEntryType.DEBIT
            );

            mockMvc.perform(post("/api/v1/disputes/{id}/fund-adjustment", dispute.getId())
                            .with(user(UserPrincipal.create(adminUser)))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success", is(true)))
                    .andExpect(jsonPath("$.data.status", is("RESOLVED")))
                    .andExpect(jsonPath("$.data.fundAdjustmentAmount", is(1000000.00)))
                    .andExpect(jsonPath("$.data.fundTransactionId").isNumber())
                    .andExpect(jsonPath("$.data.fundTransactionReference", startsWith("DISP-" + dispute.getId() + "-")));

            // Reload Dispute from database
            Dispute reloadedDispute = disputeRepository.findById(dispute.getId()).orElseThrow();
            assertEquals(DisputeStatus.RESOLVED, reloadedDispute.getStatus());
            assertEquals(debitAmount, reloadedDispute.getFundAdjustmentAmount());
            assertNotNull(reloadedDispute.getFundTransaction());
            assertNotNull(reloadedDispute.getResolvedAt());
            assertEquals(adminUser.getId(), reloadedDispute.getArbitratorUser().getId());

            // Reload SharedFund from database
            SharedFund reloadedFund = sharedFundRepository.findById(testFund.getId()).orElseThrow();
            assertEquals(new BigDecimal("4000000.00"), reloadedFund.getCurrentBalance());

            // Verify immutable FundTransaction ledger row
            FundTransaction tx = fundTransactionRepository.findById(reloadedDispute.getFundTransaction().getId()).orElseThrow();
            assertEquals(TransactionType.DISPUTE_ADJUSTMENT, tx.getTransactionType());
            assertEquals(FundTransactionSource.DISPUTE_RESOLUTION, tx.getSource());
            assertEquals(TransactionEntryType.DEBIT, tx.getEntryType());
            assertEquals(debitAmount, tx.getAmount());
            assertEquals(new BigDecimal("4000000.00"), tx.getBalanceAfter());
            assertEquals(adminUser.getId(), tx.getUser().getId());
            assertTrue(tx.getTransactionReference().startsWith("DISP-" + dispute.getId() + "-"));

            // Verify dual audit logs
            List<AuditLog> disputeLogs = auditLogRepository.findByEntityNameAndEntityIdOrderByCreatedAtDesc("Dispute", dispute.getId());
            assertTrue(disputeLogs.stream().anyMatch(l -> "DISPUTE_ARBITRATED".equals(l.getAction())));

            List<AuditLog> fundLogs = auditLogRepository.findByEntityNameAndEntityIdOrderByCreatedAtDesc("SharedFund", testFund.getId());
            assertTrue(fundLogs.stream().anyMatch(l -> "SHARED_FUND_DISPUTE_ADJUSTMENT".equals(l.getAction())));
        }

        @Test
        @DisplayName("Admin executes CREDIT adjustment: balance credited into shared fund")
        void testSuccessfulCreditAdjustment() throws Exception {
            Dispute dispute = createDisputeWithEvidence(DisputeStatus.OPEN);
            BigDecimal creditAmount = new BigDecimal("750000.00");

            DisputeFundAdjustmentRequest request = new DisputeFundAdjustmentRequest(
                    "Penalty fee assessed to respondent for rule non-compliance",
                    "Deposit 750,000 VND penalty fee from respondent into syndicate shared fund",
                    creditAmount,
                    TransactionEntryType.CREDIT
            );

            mockMvc.perform(post("/api/v1/disputes/{id}/fund-adjustment", dispute.getId())
                            .with(user(UserPrincipal.create(adminUser)))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success", is(true)))
                    .andExpect(jsonPath("$.data.status", is("RESOLVED")))
                    .andExpect(jsonPath("$.data.fundAdjustmentAmount", is(750000.00)));

            SharedFund reloadedFund = sharedFundRepository.findById(testFund.getId()).orElseThrow();
            assertEquals(new BigDecimal("5750000.00"), reloadedFund.getCurrentBalance());

            Dispute reloadedDispute = disputeRepository.findById(dispute.getId()).orElseThrow();
            FundTransaction tx = reloadedDispute.getFundTransaction();
            assertNotNull(tx);
            assertEquals(TransactionEntryType.CREDIT, tx.getEntryType());
            assertEquals(new BigDecimal("5750000.00"), tx.getBalanceAfter());
        }

        @Test
        @DisplayName("Admin executes arbitration with embedded fund adjustment via /arbitrate endpoint")
        void testSuccessfulAdjustmentViaArbitrateEndpoint() throws Exception {
            Dispute dispute = createDisputeWithEvidence(DisputeStatus.ESCALATED);
            BigDecimal debitAmount = new BigDecimal("400000.00");

            AdminArbitrateDisputeRequest request = new AdminArbitrateDisputeRequest(
                    "Escalated deadlock broken by admin investigation",
                    "Arbitration ruling: award 400,000 VND compensation from SharedFund",
                    debitAmount,
                    TransactionEntryType.DEBIT
            );

            mockMvc.perform(post("/api/v1/disputes/{id}/arbitrate", dispute.getId())
                            .with(user(UserPrincipal.create(adminUser)))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success", is(true)))
                    .andExpect(jsonPath("$.data.status", is("RESOLVED")))
                    .andExpect(jsonPath("$.data.fundAdjustmentAmount", is(400000.00)));

            SharedFund reloadedFund = sharedFundRepository.findById(testFund.getId()).orElseThrow();
            assertEquals(new BigDecimal("4600000.00"), reloadedFund.getCurrentBalance());
        }
    }

    @Nested
    @DisplayName("2. Balance Validation (Insufficient Balance Rejection)")
    class BalanceValidationTests {

        @Test
        @DisplayName("DEBIT exceeding current balance is rejected with HTTP 400 Bad Request")
        void testInsufficientBalanceRejected() throws Exception {
            testFund.setCurrentBalance(new BigDecimal("200000.00"));
            testFund = sharedFundRepository.saveAndFlush(testFund);

            Dispute dispute = createDisputeWithEvidence(DisputeStatus.UNDER_REVIEW);
            BigDecimal requestedAmount = new BigDecimal("1000000.00"); // 1,000,000 > 200,000

            DisputeFundAdjustmentRequest request = new DisputeFundAdjustmentRequest(
                    "Excessive payout requested by complainant",
                    "Attempting 1,000,000 VND reimbursement when vault has only 200,000 VND",
                    requestedAmount,
                    TransactionEntryType.DEBIT
            );

            mockMvc.perform(post("/api/v1/disputes/{id}/fund-adjustment", dispute.getId())
                            .with(user(UserPrincipal.create(adminUser)))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success", is(false)))
                    .andExpect(jsonPath("$.message", containsString("Insufficient fund balance")));

            // Verify database state: fund balance untouched, dispute status untouched, 0 fund transactions
            SharedFund reloadedFund = sharedFundRepository.findById(testFund.getId()).orElseThrow();
            assertEquals(new BigDecimal("200000.00"), reloadedFund.getCurrentBalance());

            Dispute reloadedDispute = disputeRepository.findById(dispute.getId()).orElseThrow();
            assertEquals(DisputeStatus.UNDER_REVIEW, reloadedDispute.getStatus());
            assertNull(reloadedDispute.getFundTransaction());
            assertNull(reloadedDispute.getFundAdjustmentAmount());
        }
    }

    @Nested
    @DisplayName("3. Rollback on Failure (Transactional Atomicity)")
    class RollbackVerificationTests {

        @Test
        @DisplayName("Insufficient balance triggers complete rollback: dispute unchanged, fund balance unchanged, zero ledger rows")
        void testRollbackOnInsufficientBalance() {
            testFund.setCurrentBalance(new BigDecimal("300000.00"));
            testFund = sharedFundRepository.saveAndFlush(testFund);

            Dispute dispute = createDisputeWithEvidence(DisputeStatus.UNDER_REVIEW);
            long initialTxCount = fundTransactionRepository.count();
            long initialAuditCount = auditLogRepository.count();

            DisputeFundAdjustmentRequest request = new DisputeFundAdjustmentRequest(
                    "Payout exceeding available reserves",
                    "Attempting 800,000 VND reimbursement against 300,000 VND balance",
                    new BigDecimal("800000.00"),
                    TransactionEntryType.DEBIT
            );

            // Execute service call and expect InsufficientFundBalanceException
            assertThrows(InsufficientFundBalanceException.class, () -> {
                disputeService.arbitrateDisputeWithFundAdjustment(dispute.getId(), request, adminUser.getId());
            });

            // Reload entities and verify strict rollback
            Dispute reloadedDispute = disputeRepository.findById(dispute.getId()).orElseThrow();
            assertEquals(DisputeStatus.UNDER_REVIEW, reloadedDispute.getStatus(),
                    "Dispute status must remain UNDER_REVIEW after rollback");
            assertNull(reloadedDispute.getFundTransaction(),
                    "Dispute fundTransaction must be null after rollback");
            assertNull(reloadedDispute.getFundAdjustmentAmount(),
                    "Dispute fundAdjustmentAmount must be null after rollback");

            SharedFund reloadedFund = sharedFundRepository.findById(testFund.getId()).orElseThrow();
            assertEquals(new BigDecimal("300000.00"), reloadedFund.getCurrentBalance(),
                    "SharedFund balance must remain strictly unchanged after rollback");

            assertEquals(initialTxCount, fundTransactionRepository.count(),
                    "Zero FundTransaction rows should be committed after rollback");
        }
    }

    @Nested
    @DisplayName("4. Duplicate Resolution & Double Adjustment Prevention")
    class DuplicateResolutionTests {

        @Test
        @DisplayName("Duplicate fund adjustment on already RESOLVED dispute is rejected with HTTP 409 Conflict")
        void testDuplicateFundAdjustmentRejectedWithConflict() throws Exception {
            Dispute dispute = createDisputeWithEvidence(DisputeStatus.UNDER_REVIEW);
            BigDecimal amount = new BigDecimal("500000.00");

            DisputeFundAdjustmentRequest firstReq = new DisputeFundAdjustmentRequest(
                    "First legitimate resolution and repair compensation",
                    "Reimburse 500,000 VND and close dispute as RESOLVED",
                    amount,
                    TransactionEntryType.DEBIT
            );

            // First adjustment succeeds
            mockMvc.perform(post("/api/v1/disputes/{id}/fund-adjustment", dispute.getId())
                            .with(user(UserPrincipal.create(adminUser)))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(firstReq)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.status", is("RESOLVED")));

            SharedFund fundAfterFirst = sharedFundRepository.findById(testFund.getId()).orElseThrow();
            assertEquals(new BigDecimal("4500000.00"), fundAfterFirst.getCurrentBalance());
            long txCountAfterFirst = fundTransactionRepository.count();

            // Second adjustment attempt on same dispute
            DisputeFundAdjustmentRequest secondReq = new DisputeFundAdjustmentRequest(
                    "Duplicate resolution attempt",
                    "Attempting second reimbursement of 500,000 VND",
                    amount,
                    TransactionEntryType.DEBIT
            );

            mockMvc.perform(post("/api/v1/disputes/{id}/fund-adjustment", dispute.getId())
                            .with(user(UserPrincipal.create(adminUser)))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(secondReq)))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.success", is(false)));

            // Verify fund was NOT deducted a second time
            SharedFund fundAfterSecond = sharedFundRepository.findById(testFund.getId()).orElseThrow();
            assertEquals(new BigDecimal("4500000.00"), fundAfterSecond.getCurrentBalance(),
                    "Fund balance must NOT be debited on duplicate resolution attempt");

            assertEquals(txCountAfterFirst, fundTransactionRepository.count(),
                    "No second FundTransaction row should be created");
        }
    }

    @Nested
    @DisplayName("5. RBAC Authorization Enforcement")
    class RbacAuthorizationTests {

        @Test
        @DisplayName("Staff user is forbidden from executing fund adjustment (HTTP 403 Forbidden)")
        void testStaffForbiddenFromFundAdjustment() throws Exception {
            Dispute dispute = createDisputeWithEvidence(DisputeStatus.UNDER_REVIEW);

            DisputeFundAdjustmentRequest request = new DisputeFundAdjustmentRequest(
                    "Staff trying to perform arbitration",
                    "Unauthorized resolution terms",
                    new BigDecimal("250000.00"),
                    TransactionEntryType.DEBIT
            );

            mockMvc.perform(post("/api/v1/disputes/{id}/fund-adjustment", dispute.getId())
                            .with(user(UserPrincipal.create(staffUser)))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("Co-owner user is forbidden from executing fund adjustment (HTTP 403 Forbidden)")
        void testCoOwnerForbiddenFromFundAdjustment() throws Exception {
            Dispute dispute = createDisputeWithEvidence(DisputeStatus.UNDER_REVIEW);

            DisputeFundAdjustmentRequest request = new DisputeFundAdjustmentRequest(
                    "Co-owner trying to perform arbitration",
                    "Unauthorized resolution terms",
                    new BigDecimal("250000.00"),
                    TransactionEntryType.DEBIT
            );

            mockMvc.perform(post("/api/v1/disputes/{id}/fund-adjustment", dispute.getId())
                            .with(user(UserPrincipal.create(activeOwner)))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("Unauthenticated caller receives HTTP 401 Unauthorized")
        void testUnauthenticatedForbiddenFromFundAdjustment() throws Exception {
            Dispute dispute = createDisputeWithEvidence(DisputeStatus.UNDER_REVIEW);

            DisputeFundAdjustmentRequest request = new DisputeFundAdjustmentRequest(
                    "Anonymous trying to perform arbitration",
                    "Unauthorized resolution terms",
                    new BigDecimal("250000.00"),
                    TransactionEntryType.DEBIT
            );

            mockMvc.perform(post("/api/v1/disputes/{id}/fund-adjustment", dispute.getId())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isUnauthorized());
        }
    }
}
