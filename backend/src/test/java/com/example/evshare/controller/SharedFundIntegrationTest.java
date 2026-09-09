package com.example.evshare.controller;

import com.example.evshare.dto.request.FundContributionRequest;
import com.example.evshare.dto.request.FundWithdrawalRequest;
import com.example.evshare.entity.*;
import com.example.evshare.entity.enums.RoleName;
import com.example.evshare.entity.enums.TransactionType;
import com.example.evshare.entity.enums.VehicleStatus;
import com.example.evshare.repository.*;
import com.example.evshare.security.UserPrincipal;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@DisplayName("06-H — Shared Fund End-to-End Integration Tests")
class SharedFundIntegrationTest {

    private static final AtomicLong PHONE_SEQ = new AtomicLong(System.currentTimeMillis() % 80000000L + 20000000L);

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @Autowired private UserRepository userRepository;
    @Autowired private RoleRepository roleRepository;
    @Autowired private VehicleRepository vehicleRepository;
    @Autowired private OwnershipGroupRepository ownershipGroupRepository;
    @Autowired private OwnershipShareRepository ownershipShareRepository;
    @Autowired private SharedFundRepository sharedFundRepository;
    @Autowired private FundTransactionRepository fundTransactionRepository;
    @Autowired private AuditLogRepository auditLogRepository;

    private User coOwner;
    private User outsider;
    private OwnershipGroup testGroup;
    private SharedFund testFund;

    @BeforeEach
    void setUp() {
        Role roleCoOwner = roleRepository.findByName(RoleName.ROLE_CO_OWNER)
                .orElseGet(() -> roleRepository.save(new Role(null, RoleName.ROLE_CO_OWNER)));

        String uid = UUID.randomUUID().toString().substring(0, 8);
        coOwner = createTestUser("fund.owner." + uid + "@evshare.io", "Nguyen Owner", roleCoOwner);
        outsider = createTestUser("fund.outsider." + uid + "@evshare.io", "Le Outsider", roleCoOwner);

        Vehicle vehicle = new Vehicle();
        vehicle.setVin("VIN" + uid.toUpperCase() + "12345678");
        vehicle.setLicensePlate("51K-" + uid.toUpperCase());
        vehicle.setModelName("VinFast VF9 Plus");
        vehicle.setManufacturer("VinFast");
        vehicle.setModel3dAssetPath("models/vehicles/vf9.glb");
        vehicle.setStatus(VehicleStatus.AVAILABLE);
        vehicle = vehicleRepository.saveAndFlush(vehicle);

        testGroup = new OwnershipGroup();
        testGroup.setGroupName("Syndicate " + uid);
        testGroup.setVehicle(vehicle);
        testGroup.setFormationDate(LocalDate.now());
        testGroup.setIsActive(true);
        testGroup = ownershipGroupRepository.saveAndFlush(testGroup);

        OwnershipShare share = new OwnershipShare();
        share.setGroup(testGroup);
        share.setUser(coOwner);
        share.setPercentage(new BigDecimal("100.00"));
        share.setShareCertificateNumber("CERT-" + uid);
        share.setIsActive(true);
        ownershipShareRepository.saveAndFlush(share);

        testFund = new SharedFund();
        testFund.setGroup(testGroup);
        testFund.setCurrentBalance(new BigDecimal("10000000.00"));
        testFund.setMinimumReserveThreshold(new BigDecimal("10000000.00"));
        testFund.setCurrency("VND");
        testFund = sharedFundRepository.saveAndFlush(testFund);
    }

    private User createTestUser(String email, String fullName, Role role) {
        User u = new User();
        u.setEmail(email);
        u.setFullName(fullName);
        u.setPasswordHash("hashed_pass");
        u.setPhoneNumber("09" + PHONE_SEQ.incrementAndGet());
        u.setIsActive(true);
        u.setRoles(Set.of(role));
        return userRepository.saveAndFlush(u);
    }

    @Test
    @DisplayName("GET /api/v1/ownership-groups/{groupId}/fund: Co-owner retrieves fund details and liquidity state")
    void testGetFund_Success() throws Exception {
        mockMvc.perform(get("/api/v1/ownership-groups/{groupId}/fund", testGroup.getId())
                        .with(user(UserPrincipal.create(coOwner))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.id", is(testFund.getId().intValue())))
                .andExpect(jsonPath("$.data.groupId", is(testGroup.getId().intValue())))
                .andExpect(jsonPath("$.data.currentBalance", is(10000000.00)))
                .andExpect(jsonPath("$.data.minimumReserveThreshold", is(10000000.00)))
                .andExpect(jsonPath("$.data.isLowLiquidity", is(false)));
    }

    @Test
    @DisplayName("GET /api/v1/ownership-groups/{groupId}/fund/balance: Quick balance check")
    void testGetFundBalance_Success() throws Exception {
        mockMvc.perform(get("/api/v1/ownership-groups/{groupId}/fund/balance", testGroup.getId())
                        .with(user(UserPrincipal.create(coOwner))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.currentBalance", is(10000000.00)));
    }

    @Test
    @DisplayName("POST /api/v1/ownership-groups/{groupId}/fund/contributions: Co-owner deposit increases balance, persists transaction, logs audit")
    void testContribute_Success() throws Exception {
        FundContributionRequest request = new FundContributionRequest(
                new BigDecimal("5000000.00"),
                "Monthly vault deposit"
        );

        mockMvc.perform(post("/api/v1/ownership-groups/{groupId}/fund/contributions", testGroup.getId())
                        .with(user(UserPrincipal.create(coOwner)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.transactionType", is("CONTRIBUTION")))
                .andExpect(jsonPath("$.data.amount", is(5000000.00)))
                .andExpect(jsonPath("$.data.balanceAfter", is(15000000.00)))
                .andExpect(jsonPath("$.data.description", is("Monthly vault deposit")));

        // Verify database persistence
        SharedFund updatedFund = sharedFundRepository.findById(testFund.getId()).orElseThrow();
        assertEquals(new BigDecimal("15000000.00"), updatedFund.getCurrentBalance());

        List<FundTransaction> txs = fundTransactionRepository.findByFundId(testFund.getId());
        assertEquals(1, txs.size());
        assertEquals(TransactionType.CONTRIBUTION, txs.get(0).getTransactionType());
        assertEquals(new BigDecimal("5000000.00"), txs.get(0).getAmount());

        List<AuditLog> auditLogs = auditLogRepository.findByEntityNameAndEntityIdOrderByIdDesc("SharedFund", testFund.getId());
        assertEquals(1, auditLogs.size());
        assertEquals("SHARED_FUND_CONTRIBUTION", auditLogs.get(0).getAction());
        assertTrue(auditLogs.get(0).getNewStateJson().contains("15000000.00"));
    }

    @Test
    @DisplayName("POST /api/v1/ownership-groups/{groupId}/fund/withdrawals: Valid withdrawal decreases balance")
    void testWithdraw_Success() throws Exception {
        FundWithdrawalRequest request = new FundWithdrawalRequest(
                new BigDecimal("2000000.00"),
                "Detailing and tire fluid payout",
                false
        );

        mockMvc.perform(post("/api/v1/ownership-groups/{groupId}/fund/withdrawals", testGroup.getId())
                        .with(user(UserPrincipal.create(coOwner)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.transactionType", is("WITHDRAWAL")))
                .andExpect(jsonPath("$.data.amount", is(2000000.00)))
                .andExpect(jsonPath("$.data.balanceAfter", is(8000000.00)));

        SharedFund updatedFund = sharedFundRepository.findById(testFund.getId()).orElseThrow();
        assertEquals(new BigDecimal("8000000.00"), updatedFund.getCurrentBalance());
        assertTrue(updatedFund.isLowLiquidity(), "Balance 8M < 10M threshold, low liquidity flag is true");
    }

    @Test
    @DisplayName("POST /api/v1/ownership-groups/{groupId}/fund/withdrawals: Negative balance rejected without overdraft permission")
    void testWithdraw_InsufficientBalance_Rejected() throws Exception {
        FundWithdrawalRequest request = new FundWithdrawalRequest(
                new BigDecimal("15000000.00"), // Current balance is 10,000,000.00
                "Unauthorized excessive withdrawal",
                false
        );

        mockMvc.perform(post("/api/v1/ownership-groups/{groupId}/fund/withdrawals", testGroup.getId())
                        .with(user(UserPrincipal.create(coOwner)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.message", containsString("Insufficient fund balance")));

        // Balance must remain intact
        SharedFund fund = sharedFundRepository.findById(testFund.getId()).orElseThrow();
        assertEquals(new BigDecimal("10000000.00"), fund.getCurrentBalance());
    }

    @Test
    @DisplayName("POST /api/v1/ownership-groups/{groupId}/fund/withdrawals: Overdraft permitted allows negative balance")
    void testWithdraw_OverdraftPermitted_NegativeBalance() throws Exception {
        FundWithdrawalRequest request = new FundWithdrawalRequest(
                new BigDecimal("14000000.00"), // Exceeds balance by 4M
                "Syndicate agreed emergency overdraft",
                true // allowOverdraft = true
        );

        mockMvc.perform(post("/api/v1/ownership-groups/{groupId}/fund/withdrawals", testGroup.getId())
                        .with(user(UserPrincipal.create(coOwner)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.balanceAfter", is(-4000000.00)));

        SharedFund fund = sharedFundRepository.findById(testFund.getId()).orElseThrow();
        assertEquals(new BigDecimal("-4000000.00"), fund.getCurrentBalance());
    }

    @Test
    @DisplayName("GET /api/v1/ownership-groups/{groupId}/fund/transactions & /audit: Retrieve history")
    void testGetHistoryAndAudit() throws Exception {
        // Perform 1 contribution
        FundContributionRequest req1 = new FundContributionRequest(new BigDecimal("1000000.00"), "Deposit 1");
        mockMvc.perform(post("/api/v1/ownership-groups/{groupId}/fund/contributions", testGroup.getId())
                        .with(user(UserPrincipal.create(coOwner)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req1)))
                .andExpect(status().isCreated());

        // Query transactions
        mockMvc.perform(get("/api/v1/ownership-groups/{groupId}/fund/transactions", testGroup.getId())
                        .with(user(UserPrincipal.create(coOwner))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].amount", is(1000000.00)));

        // Query audit history
        mockMvc.perform(get("/api/v1/ownership-groups/{groupId}/fund/audit", testGroup.getId())
                        .with(user(UserPrincipal.create(coOwner))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].action", is("SHARED_FUND_CONTRIBUTION")));
    }

    @Test
    @DisplayName("Security ACL: Outsider non-member receives 403 Forbidden")
    void testSecurityAcl_OutsiderForbidden() throws Exception {
        mockMvc.perform(get("/api/v1/ownership-groups/{groupId}/fund", testGroup.getId())
                        .with(user(UserPrincipal.create(outsider))))
                .andExpect(status().isForbidden());

        FundContributionRequest req = new FundContributionRequest(new BigDecimal("500000.00"), "Outsider deposit");
        mockMvc.perform(post("/api/v1/ownership-groups/{groupId}/fund/contributions", testGroup.getId())
                        .with(user(UserPrincipal.create(outsider)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("06-I: GET /reconciliation & GET /transactions/{ref}: Ledger reconciliation and transaction lookup")
    void testReconciliationAndReferenceLookup_Integration() throws Exception {
        // 1. Initial fund has 10M, record initial transaction to balance ledger
        FundContributionRequest req1 = new FundContributionRequest(
                new BigDecimal("2000000.00"),
                "Deposit test",
                "TX-INTEG-CRD-01",
                com.example.evshare.entity.enums.FundTransactionSource.MEMBER_CONTRIBUTION
        );
        mockMvc.perform(post("/api/v1/ownership-groups/{groupId}/fund/contributions", testGroup.getId())
                        .with(user(UserPrincipal.create(coOwner)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req1)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.entryType", is("CREDIT")))
                .andExpect(jsonPath("$.data.transactionReference", is("TX-INTEG-CRD-01")))
                .andExpect(jsonPath("$.data.source", is("MEMBER_CONTRIBUTION")));

        // 2. Perform withdrawal
        FundWithdrawalRequest req2 = new FundWithdrawalRequest(
                new BigDecimal("1000000.00"),
                "Withdrawal test",
                false,
                "TX-INTEG-DBT-01",
                com.example.evshare.entity.enums.FundTransactionSource.EXPENSE_PAYOUT
        );
        mockMvc.perform(post("/api/v1/ownership-groups/{groupId}/fund/withdrawals", testGroup.getId())
                        .with(user(UserPrincipal.create(coOwner)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req2)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.entryType", is("DEBIT")))
                .andExpect(jsonPath("$.data.transactionReference", is("TX-INTEG-DBT-01")))
                .andExpect(jsonPath("$.data.source", is("EXPENSE_PAYOUT")));

        // 3. Lookup transaction by reference
        mockMvc.perform(get("/api/v1/ownership-groups/{groupId}/fund/transactions/{ref}", testGroup.getId(), "TX-INTEG-CRD-01")
                        .with(user(UserPrincipal.create(coOwner))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.transactionReference", is("TX-INTEG-CRD-01")))
                .andExpect(jsonPath("$.data.entryType", is("CREDIT")))
                .andExpect(jsonPath("$.data.amount", is(2000000.00)));

        // 4. Query reconciliation
        mockMvc.perform(get("/api/v1/ownership-groups/{groupId}/fund/reconciliation", testGroup.getId())
                        .with(user(UserPrincipal.create(coOwner))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.totalCredits", is(2000000.00)))
                .andExpect(jsonPath("$.data.totalDebits", is(1000000.00)))
                .andExpect(jsonPath("$.data.creditCount", is(1)))
                .andExpect(jsonPath("$.data.debitCount", is(1)))
                .andExpect(jsonPath("$.data.transactionCount", is(2)));
    }
}
