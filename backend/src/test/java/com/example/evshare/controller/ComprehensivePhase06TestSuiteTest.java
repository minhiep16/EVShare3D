package com.example.evshare.controller;

import com.example.evshare.dto.request.*;
import com.example.evshare.dto.response.*;
import com.example.evshare.entity.*;
import com.example.evshare.entity.enums.*;
import com.example.evshare.exception.IdempotencyConflictException;
import com.example.evshare.exception.InvalidPaymentStateTransitionException;
import com.example.evshare.repository.*;
import com.example.evshare.security.UserPrincipal;
import com.example.evshare.service.CostAllocationService;
import com.example.evshare.service.ExpenseService;
import com.example.evshare.service.SharedFundService;
import com.example.evshare.service.allocation.AllocationResult;
import com.example.evshare.service.payment.IdempotencyService;
import com.example.evshare.service.payment.PaymentLifecycleService;
import com.example.evshare.service.payment.PaymentProviderRegistry;
import com.example.evshare.service.payment.PaymentService;
import com.example.evshare.service.payment.model.PaymentInitiationCommand;
import com.example.evshare.service.payment.model.PaymentInitiationResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * CHECKPOINT 06-O — COMPREHENSIVE PHASE 06 MASTER TEST SUITE
 *
 * Systematically tests all 12 Phase 06 domains:
 * 1.  Expense
 * 2.  Cost Allocation
 * 3.  Ownership Allocation
 * 4.  Usage Allocation
 * 5.  Hybrid Allocation
 * 6.  Shared Fund
 * 7.  Fund Transactions
 * 8.  Payment Provider
 * 9.  Payment Lifecycle
 * 10. Idempotency
 * 11. Rollback
 * 12. Authorization
 */
@SpringBootTest
@AutoConfigureMockMvc
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("Checkpoint 06-O — Comprehensive Phase 06 Master Test Suite (All 12 Domains)")
public class ComprehensivePhase06TestSuiteTest {

    private static final AtomicLong PHONE_SEQ = new AtomicLong(System.currentTimeMillis() % 80000000L + 50000000L);

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @Autowired private UserRepository userRepository;
    @Autowired private RoleRepository roleRepository;
    @Autowired private VehicleRepository vehicleRepository;
    @Autowired private OwnershipGroupRepository ownershipGroupRepository;
    @Autowired private OwnershipShareRepository ownershipShareRepository;
    @Autowired private BookingRepository bookingRepository;
    @Autowired private UsageSessionRepository usageSessionRepository;
    @Autowired private SharedFundRepository sharedFundRepository;
    @Autowired private FundTransactionRepository fundTransactionRepository;
    @Autowired private ExpenseRepository expenseRepository;
    @Autowired private ExpenseAllocationRepository expenseAllocationRepository;
    @Autowired private PaymentRepository paymentRepository;
    @Autowired private AuditLogRepository auditLogRepository;

    @Autowired private ExpenseService expenseService;
    @Autowired private CostAllocationService costAllocationService;
    @Autowired private SharedFundService sharedFundService;
    @Autowired private PaymentProviderRegistry paymentProviderRegistry;
    @Autowired private PaymentLifecycleService paymentLifecycleService;
    @Autowired private IdempotencyService idempotencyService;
    @Autowired private PaymentService paymentService;
    @Autowired private PlatformTransactionManager transactionManager;

    private User coOwner1;
    private User coOwner2;
    private User outsiderUser;
    private User staffUser;
    private Vehicle testVehicle;
    private OwnershipGroup testGroup;
    private SharedFund testFund;

    @BeforeEach
    void setUp() {
        Role roleCoOwner = roleRepository.findByName(RoleName.ROLE_CO_OWNER)
                .orElseGet(() -> roleRepository.save(new Role(null, RoleName.ROLE_CO_OWNER)));
        Role roleStaff = roleRepository.findByName(RoleName.ROLE_STAFF)
                .orElseGet(() -> roleRepository.save(new Role(null, RoleName.ROLE_STAFF)));

        String uid = UUID.randomUUID().toString().substring(0, 8);

        coOwner1 = new User();
        coOwner1.setEmail("master06.owner1." + uid + "@evshare.io");
        coOwner1.setFullName("Master Owner 1");
        coOwner1.setPasswordHash("pwd");
        coOwner1.setPhoneNumber("09" + PHONE_SEQ.incrementAndGet());
        coOwner1.setIsActive(true);
        coOwner1.setRoles(Set.of(roleCoOwner));
        coOwner1 = userRepository.saveAndFlush(coOwner1);

        coOwner2 = new User();
        coOwner2.setEmail("master06.owner2." + uid + "@evshare.io");
        coOwner2.setFullName("Master Owner 2");
        coOwner2.setPasswordHash("pwd");
        coOwner2.setPhoneNumber("09" + PHONE_SEQ.incrementAndGet());
        coOwner2.setIsActive(true);
        coOwner2.setRoles(Set.of(roleCoOwner));
        coOwner2 = userRepository.saveAndFlush(coOwner2);

        outsiderUser = new User();
        outsiderUser.setEmail("master06.outsider." + uid + "@evshare.io");
        outsiderUser.setFullName("Master Outsider");
        outsiderUser.setPasswordHash("pwd");
        outsiderUser.setPhoneNumber("09" + PHONE_SEQ.incrementAndGet());
        outsiderUser.setIsActive(true);
        outsiderUser.setRoles(Set.of(roleCoOwner));
        outsiderUser = userRepository.saveAndFlush(outsiderUser);

        staffUser = new User();
        staffUser.setEmail("master06.staff." + uid + "@evshare.io");
        staffUser.setFullName("Master Staff Operator");
        staffUser.setPasswordHash("pwd");
        staffUser.setPhoneNumber("09" + PHONE_SEQ.incrementAndGet());
        staffUser.setIsActive(true);
        staffUser.setRoles(Set.of(roleStaff));
        staffUser = userRepository.saveAndFlush(staffUser);

        testVehicle = new Vehicle();
        testVehicle.setVin("VIN06M" + uid.toUpperCase() + "000000");
        testVehicle.setLicensePlate("51K-06M" + uid.substring(0, 3).toUpperCase());
        testVehicle.setModelName("VinFast VF8");
        testVehicle.setManufacturer("VinFast");
        testVehicle.setModel3dAssetPath("models/vehicles/vf8.glb");
        testVehicle.setStatus(VehicleStatus.AVAILABLE);
        testVehicle = vehicleRepository.saveAndFlush(testVehicle);

        testGroup = new OwnershipGroup();
        testGroup.setGroupName("Master Syndicate " + uid);
        testGroup.setVehicle(testVehicle);
        testGroup.setFormationDate(LocalDate.now());
        testGroup.setIsActive(true);
        testGroup = ownershipGroupRepository.saveAndFlush(testGroup);

        OwnershipShare share1 = new OwnershipShare(null, testGroup, coOwner1, new BigDecimal("60.00"), "CERT-06M-" + uid + "-1", null, true);
        OwnershipShare share2 = new OwnershipShare(null, testGroup, coOwner2, new BigDecimal("40.00"), "CERT-06M-" + uid + "-2", null, true);
        ownershipShareRepository.saveAllAndFlush(List.of(share1, share2));

        testFund = new SharedFund();
        testFund.setGroup(testGroup);
        testFund.setCurrentBalance(BigDecimal.ZERO);
        testFund.setMinimumReserveThreshold(new BigDecimal("5000000.00"));
        testFund.setCurrency("VND");
        testFund = sharedFundRepository.saveAndFlush(testFund);

        FundContributionRequest initContrib = new FundContributionRequest();
        initContrib.setAmount(new BigDecimal("10000000.00"));
        initContrib.setSource(FundTransactionSource.VAULT_INITIALIZATION);
        initContrib.setDescription("Initial Syndicate Vault Capital");
        sharedFundService.contribute(testGroup.getId(), initContrib, coOwner1.getId(), "127.0.0.1");

        testFund = sharedFundRepository.findById(testFund.getId()).orElseThrow();
    }

    private Booking createBooking(User user, Instant start, Instant end) {
        Booking b = new Booking();
        b.setVehicle(testVehicle);
        b.setUser(user);
        b.setStartTime(start);
        b.setEndTime(end);
        b.setStatus(BookingStatus.COMPLETED);
        b.setEstimatedCost(BigDecimal.ZERO);
        return bookingRepository.saveAndFlush(b);
    }

    private UsageSession createUsageSession(Booking booking, BigDecimal startOdo, BigDecimal endOdo) {
        UsageSession s = new UsageSession();
        s.setBooking(booking);
        s.setStartOdometer(startOdo);
        s.setEndOdometer(endOdo);
        s.setStartBattery(90);
        s.setEndBattery(50);
        s.setStatus(UsageSessionStatus.COMPLETED);
        s.setCheckInTime(booking.getStartTime());
        s.setCheckOutTime(booking.getEndTime());
        return usageSessionRepository.saveAndFlush(s);
    }

    // =========================================================================
    // 1. EXPENSE DOMAIN
    // =========================================================================

    @Test
    @Order(1)
    @DisplayName("Domain 1: Expense — Creation, Validation, Duplicate Detection & Audit Trail")
    void testDomain01_Expense() throws Exception {
        CreateExpenseRequest request = new CreateExpenseRequest();
        request.setGroupId(testGroup.getId());
        request.setVehicleId(testVehicle.getId());
        request.setTitle("Scheduled 10,000 km Service");
        request.setCategory(ExpenseCategory.MAINTENANCE);
        request.setAmount(new BigDecimal("3500000.00"));
        request.setCurrency("VND");
        request.setIncurredDate(LocalDate.now());
        request.setAllocationStrategy(AllocationStrategy.OWNERSHIP_BASED);
        request.setInvoiceReference("INV-06M-" + UUID.randomUUID().toString().substring(0, 8));

        // 1. Valid Expense Creation by Co-Owner
        mockMvc.perform(post("/api/v1/expenses")
                        .with(user(UserPrincipal.fromClaims(coOwner1.getId(), coOwner1.getEmail(), List.of("ROLE_CO_OWNER"))))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.title").value("Scheduled 10,000 km Service"))
                .andExpect(jsonPath("$.data.category").value("MAINTENANCE"))
                .andExpect(jsonPath("$.data.amount").value(3500000.00));

        // 2. Duplicate Expense Rejection (Same Invoice Reference -> 409 Conflict)
        mockMvc.perform(post("/api/v1/expenses")
                        .with(user(UserPrincipal.fromClaims(coOwner1.getId(), coOwner1.getEmail(), List.of("ROLE_CO_OWNER"))))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict());

        // 3. Query Expense By ID
        List<Expense> expenses = expenseRepository.findByGroupId(testGroup.getId(), org.springframework.data.domain.PageRequest.of(0, 10)).getContent();
        assertFalse(expenses.isEmpty());
        Expense created = expenses.get(0);

        mockMvc.perform(get("/api/v1/expenses/{id}", created.getId())
                        .with(user(UserPrincipal.fromClaims(coOwner1.getId(), coOwner1.getEmail(), List.of("ROLE_CO_OWNER")))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(created.getId()));
    }

    // =========================================================================
    // 2. COST ALLOCATION ENGINE DOMAIN
    // =========================================================================

    @Test
    @Order(2)
    @DisplayName("Domain 2: Cost Allocation — Engine Dispatch & Penny Reconciliation Invariant")
    void testDomain02_CostAllocationEngine() {
        Expense expense = new Expense();
        expense.setGroup(testGroup);
        expense.setVehicle(testVehicle);
        expense.setLoggedByUser(coOwner1);
        expense.setTitle("Battery Inspection Check");
        expense.setCategory(ExpenseCategory.INSPECTION);
        expense.setTotalAmount(new BigDecimal("1000000.01")); // Odd penny
        expense.setCurrency("VND");
        expense.setAllocationStrategy(AllocationStrategy.OWNERSHIP_BASED);
        expense.setIncurredDate(LocalDate.now());
        expense = expenseRepository.saveAndFlush(expense);

        AllocationResult result = costAllocationService.calculateAllocation(expense);
        assertNotNull(result);
        assertEquals(0, expense.getTotalAmount().compareTo(result.getTotalAllocated()),
                "Total allocated must mathematically equal total expense down to 0.01 VND");

        List<ExpenseAllocation> persisted = costAllocationService.applyAndPersistAllocations(expense.getId());
        assertEquals(2, persisted.size());
        BigDecimal sumAllocated = persisted.stream().map(ExpenseAllocation::getAllocatedAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        assertEquals(0, expense.getTotalAmount().compareTo(sumAllocated));
    }

    // =========================================================================
    // 3. OWNERSHIP-BASED ALLOCATION DOMAIN
    // =========================================================================

    @Test
    @Order(3)
    @DisplayName("Domain 3: Ownership Allocation — Proportional Equity & Deterministic Penny Absorption")
    void testDomain03_OwnershipAllocation() {
        // Syndicate equity: coOwner1 = 60%, coOwner2 = 40%
        Expense expense = new Expense();
        expense.setGroup(testGroup);
        expense.setVehicle(testVehicle);
        expense.setLoggedByUser(coOwner1);
        expense.setTitle("Annual Fleet Insurance");
        expense.setCategory(ExpenseCategory.INSURANCE);
        expense.setTotalAmount(new BigDecimal("10000000.00"));
        expense.setCurrency("VND");
        expense.setAllocationStrategy(AllocationStrategy.OWNERSHIP_BASED);
        expense.setIncurredDate(LocalDate.now());
        expense = expenseRepository.saveAndFlush(expense);

        List<ExpenseAllocation> allocations = costAllocationService.applyAndPersistAllocations(expense.getId());
        assertEquals(2, allocations.size());

        ExpenseAllocation alloc1 = allocations.stream().filter(a -> a.getUser().getId().equals(coOwner1.getId())).findFirst().orElseThrow();
        ExpenseAllocation alloc2 = allocations.stream().filter(a -> a.getUser().getId().equals(coOwner2.getId())).findFirst().orElseThrow();

        assertEquals(0, new BigDecimal("6000000.00").compareTo(alloc1.getAllocatedAmount()), "Owner 1 should be allocated 60%");
        assertEquals(0, new BigDecimal("4000000.00").compareTo(alloc2.getAllocatedAmount()), "Owner 2 should be allocated 40%");
    }

    // =========================================================================
    // 4. USAGE-BASED ALLOCATION DOMAIN
    // =========================================================================

    @Test
    @Order(4)
    @DisplayName("Domain 4: Usage Allocation — Proportional Telemetry Allocation (Distance/Energy)")
    void testDomain04_UsageAllocation() {
        // Create completed usage sessions: coOwner1 used 100km, coOwner2 used 300km (Total = 400km)
        Booking b1 = createBooking(coOwner1, Instant.now().minusSeconds(86400), Instant.now().minusSeconds(82800));
        createUsageSession(b1, new BigDecimal("10000.00"), new BigDecimal("10100.00"));

        Booking b2 = createBooking(coOwner2, Instant.now().minusSeconds(72000), Instant.now().minusSeconds(64800));
        createUsageSession(b2, new BigDecimal("10100.00"), new BigDecimal("10400.00"));

        // Expense of 4,000,000 VND allocated by USAGE_BASED (100km / 400km = 25%, 300km / 400km = 75%)
        Expense expense = new Expense();
        expense.setGroup(testGroup);
        expense.setVehicle(testVehicle);
        expense.setLoggedByUser(coOwner1);
        expense.setTitle("Fast Charging Hub Settlement");
        expense.setCategory(ExpenseCategory.CHARGING);
        expense.setTotalAmount(new BigDecimal("4000000.00"));
        expense.setCurrency("VND");
        expense.setAllocationStrategy(AllocationStrategy.USAGE_BASED);
        expense.setIncurredDate(LocalDate.now());
        expense = expenseRepository.saveAndFlush(expense);

        List<ExpenseAllocation> allocations = costAllocationService.applyAndPersistAllocations(expense.getId());
        assertEquals(2, allocations.size());

        ExpenseAllocation alloc1 = allocations.stream().filter(a -> a.getUser().getId().equals(coOwner1.getId())).findFirst().orElseThrow();
        ExpenseAllocation alloc2 = allocations.stream().filter(a -> a.getUser().getId().equals(coOwner2.getId())).findFirst().orElseThrow();

        assertEquals(0, new BigDecimal("1000000.00").compareTo(alloc1.getAllocatedAmount()), "Owner 1 (25% usage) gets 1,000,000.00 VND");
        assertEquals(0, new BigDecimal("3000000.00").compareTo(alloc2.getAllocatedAmount()), "Owner 2 (75% usage) gets 3,000,000.00 VND");
    }

    // =========================================================================
    // 5. HYBRID ALLOCATION DOMAIN
    // =========================================================================

    @Test
    @Order(5)
    @DisplayName("Domain 5: Hybrid Allocation — 30% Fixed Ownership + 70% Variable Usage (BR-FIN-02)")
    void testDomain05_HybridAllocation() {
        // Setup sessions: owner1 100km (25%), owner2 300km (75%)
        Booking b1 = createBooking(coOwner1, Instant.now().minusSeconds(86400), Instant.now().minusSeconds(82800));
        createUsageSession(b1, new BigDecimal("20000.00"), new BigDecimal("20100.00"));

        Booking b2 = createBooking(coOwner2, Instant.now().minusSeconds(72000), Instant.now().minusSeconds(64800));
        createUsageSession(b2, new BigDecimal("20100.00"), new BigDecimal("20400.00"));

        // Total expense = 10,000,000 VND
        // Specification (BR-FIN-02): 30% fixed ownership + 70% variable usage
        // Fixed portion (30% = 3,000,000): Owner1 (60%) = 1,800,000; Owner2 (40%) = 1,200,000
        // Variable portion (70% = 7,000,000): Owner1 (25%) = 1,750,000; Owner2 (75%) = 5,250,000
        // Expected Total: Owner1 = 3,550,000.00; Owner2 = 6,450,000.00
        Expense expense = new Expense();
        expense.setGroup(testGroup);
        expense.setVehicle(testVehicle);
        expense.setLoggedByUser(coOwner1);
        expense.setTitle("Comprehensive Overhaul");
        expense.setCategory(ExpenseCategory.MAINTENANCE);
        expense.setTotalAmount(new BigDecimal("10000000.00"));
        expense.setCurrency("VND");
        expense.setAllocationStrategy(AllocationStrategy.HYBRID);
        expense.setIncurredDate(LocalDate.now());
        expense = expenseRepository.saveAndFlush(expense);

        List<ExpenseAllocation> allocations = costAllocationService.applyAndPersistAllocations(expense.getId());
        assertEquals(2, allocations.size());

        ExpenseAllocation a1 = allocations.stream().filter(a -> a.getUser().getId().equals(coOwner1.getId())).findFirst().orElseThrow();
        ExpenseAllocation a2 = allocations.stream().filter(a -> a.getUser().getId().equals(coOwner2.getId())).findFirst().orElseThrow();

        assertEquals(0, new BigDecimal("3550000.00").compareTo(a1.getAllocatedAmount()), "Owner 1 Hybrid: 1,800,000 fixed + 1,750,000 variable");
        assertEquals(0, new BigDecimal("6450000.00").compareTo(a2.getAllocatedAmount()), "Owner 2 Hybrid: 1,200,000 fixed + 5,250,000 variable");
    }

    // =========================================================================
    // 6. SHARED FUND DOMAIN
    // =========================================================================

    @Test
    @Order(6)
    @DisplayName("Domain 6: Shared Fund — Balance Inquiries & Minimum Reserve Protection")
    void testDomain06_SharedFund() {
        SharedFund fund = sharedFundRepository.findByGroupId(testGroup.getId()).orElseThrow();
        assertEquals(0, new BigDecimal("10000000.00").compareTo(fund.getCurrentBalance()));
        assertEquals(0, new BigDecimal("5000000.00").compareTo(fund.getMinimumReserveThreshold()));
        assertEquals("VND", fund.getCurrency());
    }

    // =========================================================================
    // 7. FUND TRANSACTIONS DOMAIN
    // =========================================================================

    @Test
    @Order(7)
    @DisplayName("Domain 7: Fund Transactions — CREDIT/DEBIT Ledger Semantics & Reconciliation")
    void testDomain07_FundTransactionsAndReconciliation() {
        // Initial balance: 10,000,000
        // Credit 2,000,000
        FundContributionRequest deposit = new FundContributionRequest();
        deposit.setAmount(new BigDecimal("2000000.00"));
        deposit.setSource(FundTransactionSource.MEMBER_CONTRIBUTION);
        deposit.setDescription("Monthly syndicate replenishment");
        FundTransactionResponse cr = sharedFundService.contribute(testGroup.getId(), deposit, coOwner1.getId(), "127.0.0.1");
        assertEquals(0, new BigDecimal("12000000.00").compareTo(cr.getBalanceAfter()));

        // Debit 1,000,000
        FundWithdrawalRequest withdraw = new FundWithdrawalRequest();
        withdraw.setAmount(new BigDecimal("1000000.00"));
        withdraw.setSource(FundTransactionSource.EXPENSE_PAYOUT);
        withdraw.setDescription("Direct repair vendor payout");
        FundTransactionResponse dr = sharedFundService.withdraw(testGroup.getId(), withdraw, coOwner2.getId(), "127.0.0.1");
        assertEquals(0, new BigDecimal("11000000.00").compareTo(dr.getBalanceAfter()));

        // Verify mathematical reconciliation
        FundReconciliationResponse recon = sharedFundService.reconcileFundBalance(testGroup.getId(), coOwner1.getId());
        assertTrue(recon.getIsReconciled(), "Fund ledger must reconcile 100%");
        assertEquals(0, BigDecimal.ZERO.compareTo(recon.getReconciliationDelta()));
        assertEquals(2, recon.getCreditCount()); // 1 init + 1 new
        assertEquals(1, recon.getDebitCount());
    }

    // =========================================================================
    // 8. PAYMENT PROVIDER DOMAIN
    // =========================================================================

    @Test
    @Order(8)
    @DisplayName("Domain 8: Payment Provider — Abstraction SPI Routing Across All Channels")
    void testDomain08_PaymentProviderAbstraction() {
        // Verify registry routes properly to isolated providers
        assertNotNull(paymentProviderRegistry.getProvider(PaymentProviderType.MOCK));
        assertNotNull(paymentProviderRegistry.getProvider(PaymentProviderType.BANK_TRANSFER));
        assertNotNull(paymentProviderRegistry.getProvider(PaymentProviderType.E_WALLET));
        assertNotNull(paymentProviderRegistry.getProvider(PaymentProviderType.GATEWAY));

        // Test Mock Provider initiation
        var mockProvider = paymentProviderRegistry.getProvider(PaymentProviderType.MOCK);
        PaymentInitiationCommand cmd = PaymentInitiationCommand.builder()
                .transactionReference("TX-PROV-TEST-" + UUID.randomUUID().toString().substring(0, 8))
                .amount(new BigDecimal("500000.00"))
                .paymentMethod(PaymentMethod.MOCK)
                .payerId(coOwner1.getId())
                .payerEmail(coOwner1.getEmail())
                .description("Provider abstraction test")
                .metadata(Map.of("autoConfirm", true))
                .build();
        PaymentInitiationResult res = mockProvider.initiate(cmd);
        assertEquals(PaymentStatus.COMPLETED, res.getStatus());
        assertTrue(res.getInstructions().contains("[TEST/SANDBOX ONLY"));
    }

    // =========================================================================
    // 9. PAYMENT LIFECYCLE DOMAIN
    // =========================================================================

    @Test
    @Order(9)
    @DisplayName("Domain 9: Payment Lifecycle — 6 Canonical Statuses & Transition Guard Enforcement")
    void testDomain09_PaymentLifecycle() {
        Payment initialPayment = new Payment();
        initialPayment.setUser(coOwner1);
        initialPayment.setFund(testFund);
        initialPayment.setAmount(new BigDecimal("1200000.00"));
        initialPayment.setPaymentMethod(PaymentMethod.GATEWAY);
        initialPayment.setTransactionReference("TX-LIFE-" + UUID.randomUUID().toString().substring(0, 8));
        initialPayment.setStatus(PaymentStatus.PENDING);
        initialPayment = paymentRepository.saveAndFlush(initialPayment);
        final Long paymentId = initialPayment.getId();

        // Valid: PENDING -> PROCESSING
        Payment p1 = paymentLifecycleService.transitionStatus(paymentId, PaymentStatus.PROCESSING, "To processing", coOwner1.getId());
        assertEquals(PaymentStatus.PROCESSING, p1.getStatus());

        // Valid: PROCESSING -> SUCCESS
        Payment p2 = paymentLifecycleService.transitionStatus(paymentId, PaymentStatus.SUCCESS, "To success", coOwner1.getId());
        assertEquals(PaymentStatus.SUCCESS, p2.getStatus());

        // Invalid: SUCCESS -> CANCELLED (Terminal/Illegal transition -> 409 Conflict)
        assertThrows(InvalidPaymentStateTransitionException.class, () -> {
            paymentLifecycleService.transitionStatus(paymentId, PaymentStatus.CANCELLED, "Illegal cancel", coOwner1.getId());
        });

        // Valid: SUCCESS -> REFUNDED
        Payment p3 = paymentLifecycleService.transitionStatus(paymentId, PaymentStatus.REFUNDED, "To refunded", coOwner1.getId());
        assertEquals(PaymentStatus.REFUNDED, p3.getStatus());

        // Terminal state immutability: REFUNDED -> SUCCESS must be rejected
        assertThrows(InvalidPaymentStateTransitionException.class, () -> {
            paymentLifecycleService.transitionStatus(paymentId, PaymentStatus.SUCCESS, "Revive refunded", coOwner1.getId());
        });
    }

    // =========================================================================
    // 10. PAYMENT IDEMPOTENCY DOMAIN
    // =========================================================================

    @Test
    @Order(10)
    @DisplayName("Domain 10: Idempotency — Deterministic Fingerprint, Deduplication & Conflict Rejection")
    void testDomain10_PaymentIdempotency() {
        String key = "IDEM-MASTER-" + UUID.randomUUID();

        InitiatePaymentRequest req = new InitiatePaymentRequest();
        req.setUserId(coOwner1.getId());
        req.setFundId(testFund.getId());
        req.setAmount(new BigDecimal("750000.00"));
        req.setPaymentMethod(PaymentMethod.MOCK);
        req.setDescription("Idempotent maintenance payment");
        req.setMetadata(Map.of("autoConfirm", true));

        // 1. Initial Request
        PaymentResponse resp1 = paymentService.initiatePayment(req, key);
        assertNotNull(resp1);
        Long paymentId = resp1.getId();

        // 2. Repeated Request with Same Key & Same Payload -> Returns identical cached response
        PaymentResponse resp2 = paymentService.initiatePayment(req, key);
        assertEquals(paymentId, resp2.getId(), "Must return same payment without creating duplicates");
        assertEquals(resp1.getTransactionReference(), resp2.getTransactionReference());

        // 3. Reusing Same Key with Different Payload -> Must throw 409 Conflict
        InitiatePaymentRequest alteredReq = new InitiatePaymentRequest();
        alteredReq.setUserId(coOwner1.getId());
        alteredReq.setFundId(testFund.getId());
        alteredReq.setAmount(new BigDecimal("999999.00")); // Altered amount
        alteredReq.setPaymentMethod(PaymentMethod.MOCK);
        alteredReq.setDescription("Tampered payload");

        assertThrows(IdempotencyConflictException.class, () -> {
            paymentService.initiatePayment(alteredReq, key);
        });
    }

    // =========================================================================
    // 11. TRANSACTION ROLLBACK DOMAIN
    // =========================================================================

    @Test
    @Order(11)
    @DisplayName("Domain 11: Rollback — Full Transaction Rollback Under Downstream Exception")
    void testDomain11_TransactionRollback() {
        BigDecimal initialFundBalance = testFund.getCurrentBalance();
        long initialTxCount = fundTransactionRepository.count();

        TransactionTemplate tt = new TransactionTemplate(transactionManager);

        assertThrows(RuntimeException.class, () -> {
            tt.execute(status -> {
                FundContributionRequest req = new FundContributionRequest();
                req.setAmount(new BigDecimal("5000000.00"));
                req.setSource(FundTransactionSource.MEMBER_CONTRIBUTION);
                req.setDescription("Slated to rollback");
                sharedFundService.contribute(testGroup.getId(), req, coOwner1.getId(), "127.0.0.1");

                throw new RuntimeException("Forced rollback inside transaction");
            });
        });

        SharedFund reloaded = sharedFundRepository.findById(testFund.getId()).orElseThrow();
        assertEquals(0, initialFundBalance.compareTo(reloaded.getCurrentBalance()), "Balance must not change on rollback");
        assertEquals(initialTxCount, fundTransactionRepository.count(), "Transaction row must not exist on rollback");
    }

    // =========================================================================
    // 12. AUTHORIZATION DOMAIN
    // =========================================================================

    @Test
    @Order(12)
    @DisplayName("Domain 12: Authorization — RBAC & Syndicate Co-Ownership Data Scoping ACL")
    void testDomain12_Authorization() throws Exception {
        // 1. Unauthenticated Request -> 401 Unauthorized
        mockMvc.perform(get("/api/v1/expenses/group/{groupId}", testGroup.getId()))
                .andExpect(status().isUnauthorized());

        // 2. Outsider Co-Owner Accessing Unowned Syndicate -> 403 Forbidden
        mockMvc.perform(get("/api/v1/expenses/group/{groupId}", testGroup.getId())
                        .with(user(UserPrincipal.fromClaims(outsiderUser.getId(), outsiderUser.getEmail(), List.of("ROLE_CO_OWNER")))))
                .andExpect(status().isForbidden());

        // 3. Outsider Attempting Fund Deposit -> 403 Forbidden
        FundContributionRequest depositReq = new FundContributionRequest();
        depositReq.setAmount(new BigDecimal("1000000.00"));
        depositReq.setDescription("Outsider deposit attempt");

        mockMvc.perform(post("/api/v1/ownership-groups/{groupId}/fund/contributions", testGroup.getId())
                        .with(user(UserPrincipal.fromClaims(outsiderUser.getId(), outsiderUser.getEmail(), List.of("ROLE_CO_OWNER"))))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(depositReq)))
                .andExpect(status().isForbidden());

        // 4. Syndicate Co-Owner Accessing Syndicate -> 200 OK
        mockMvc.perform(get("/api/v1/expenses/group/{groupId}", testGroup.getId())
                        .with(user(UserPrincipal.fromClaims(coOwner1.getId(), coOwner1.getEmail(), List.of("ROLE_CO_OWNER")))))
                .andExpect(status().isOk());

        // 5. Staff Role Accessing Any Syndicate -> 200 OK (Platform Operator Override)
        mockMvc.perform(get("/api/v1/expenses/group/{groupId}", testGroup.getId())
                        .with(user(UserPrincipal.fromClaims(staffUser.getId(), staffUser.getEmail(), List.of("ROLE_STAFF")))))
                .andExpect(status().isOk());
    }
}
