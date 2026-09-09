package com.example.evshare.service;

import com.example.evshare.dto.request.CreateExpenseRequest;
import com.example.evshare.dto.request.FundContributionRequest;
import com.example.evshare.dto.request.FundWithdrawalRequest;
import com.example.evshare.dto.request.InitiatePaymentRequest;
import com.example.evshare.dto.response.FundReconciliationResponse;
import com.example.evshare.dto.response.PaymentResponse;
import com.example.evshare.entity.*;
import com.example.evshare.entity.enums.*;
import com.example.evshare.exception.BusinessException;
import com.example.evshare.exception.InsufficientFundBalanceException;
import com.example.evshare.exception.InvalidPaymentStateTransitionException;
import com.example.evshare.repository.*;
import com.example.evshare.service.payment.PaymentLifecycleService;
import com.example.evshare.service.payment.PaymentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@DisplayName("Checkpoint 06-N — Financial Transaction Safety Integration Tests")
class FinancialTransactionSafetyIntegrationTest {

    @Autowired
    private SharedFundService sharedFundService;

    @Autowired
    private SharedFundRepository sharedFundRepository;

    @Autowired
    private FundTransactionRepository fundTransactionRepository;

    @Autowired
    private ExpenseService expenseService;

    @Autowired
    private ExpenseRepository expenseRepository;

    @Autowired
    private CostAllocationService costAllocationService;

    @Autowired
    private ExpenseAllocationRepository expenseAllocationRepository;

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private PaymentLifecycleService paymentLifecycleService;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private VehicleRepository vehicleRepository;

    @Autowired
    private OwnershipGroupRepository ownershipGroupRepository;

    @Autowired
    private OwnershipShareRepository ownershipShareRepository;

    @Autowired
    private PlatformTransactionManager transactionManager;

    private User owner1;
    private User owner2;
    private OwnershipGroup testGroup;
    private SharedFund testFund;
    private Vehicle testVehicle;

    @BeforeEach
    void setUp() {
        Role roleCoOwner = roleRepository.findByName(RoleName.ROLE_CO_OWNER)
                .orElseGet(() -> roleRepository.save(new Role(null, RoleName.ROLE_CO_OWNER)));

        String uid = UUID.randomUUID().toString().substring(0, 8);

        owner1 = new User();
        owner1.setEmail("safety.owner1." + uid + "@evshare.io");
        owner1.setFullName("Safety Owner 1");
        owner1.setPasswordHash("pwd");
        owner1.setPhoneNumber("09" + (int)(Math.random() * 90000000 + 10000000));
        owner1.setIsActive(true);
        owner1.setRoles(Set.of(roleCoOwner));
        owner1 = userRepository.saveAndFlush(owner1);

        owner2 = new User();
        owner2.setEmail("safety.owner2." + uid + "@evshare.io");
        owner2.setFullName("Safety Owner 2");
        owner2.setPasswordHash("pwd");
        owner2.setPhoneNumber("09" + (int)(Math.random() * 90000000 + 10000000));
        owner2.setIsActive(true);
        owner2.setRoles(Set.of(roleCoOwner));
        owner2 = userRepository.saveAndFlush(owner2);

        testVehicle = new Vehicle();
        testVehicle.setVin("VIN" + uid.toUpperCase() + "SAFE0000");
        testVehicle.setLicensePlate("51K-SAFE" + uid.substring(0, 4).toUpperCase());
        testVehicle.setModelName("VinFast VF9");
        testVehicle.setManufacturer("VinFast");
        testVehicle.setModel3dAssetPath("models/vehicles/vf9.glb");
        testVehicle.setStatus(VehicleStatus.AVAILABLE);
        testVehicle = vehicleRepository.saveAndFlush(testVehicle);

        testGroup = new OwnershipGroup();
        testGroup.setGroupName("Safety Syndicate " + uid);
        testGroup.setVehicle(testVehicle);
        testGroup.setFormationDate(LocalDate.now());
        testGroup.setIsActive(true);
        testGroup = ownershipGroupRepository.saveAndFlush(testGroup);

        OwnershipShare share1 = new OwnershipShare(null, testGroup, owner1, new BigDecimal("60.00"), "CERT-" + uid + "-1", null, true);
        OwnershipShare share2 = new OwnershipShare(null, testGroup, owner2, new BigDecimal("40.00"), "CERT-" + uid + "-2", null, true);
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
        initContrib.setDescription("Initial vault capital");
        sharedFundService.contribute(testGroup.getId(), initContrib, owner1.getId(), "127.0.0.1");

        testFund = sharedFundRepository.findById(testFund.getId()).orElseThrow();
    }

    // =========================================================================
    // 1. TRANSACTION BOUNDARY & ROLLBACK VERIFICATION
    // =========================================================================

    @Nested
    @DisplayName("1. Transaction Rollback & Partial State Protection")
    class RollbackProtectionTests {

        @Test
        @DisplayName("1.1. Contribution failure in transaction rolls back balance, ledger, and audit log")
        void testContributionRollbackOnDownstreamException() {
            BigDecimal initialBalance = testFund.getCurrentBalance();
            long initialTxCount = fundTransactionRepository.count();
            long initialAuditCount = auditLogRepository.count();

            TransactionTemplate tt = new TransactionTemplate(transactionManager);

            assertThrows(RuntimeException.class, () -> {
                tt.execute(status -> {
                    FundContributionRequest request = new FundContributionRequest();
                    request.setAmount(new BigDecimal("2000000.00"));
                    request.setDescription("Contribution slated to abort");
                    request.setSource(FundTransactionSource.MEMBER_CONTRIBUTION);

                    sharedFundService.contribute(testGroup.getId(), request, owner1.getId(), "127.0.0.1");

                    // Simulate downstream failure inside transaction boundary
                    throw new RuntimeException("Simulated network / database failure downstream");
                });
            });

            // Reload from DB to verify rollback integrity
            SharedFund reloadedFund = sharedFundRepository.findById(testFund.getId()).orElseThrow();
            assertEquals(0, initialBalance.compareTo(reloadedFund.getCurrentBalance()),
                    "SharedFund balance must NOT change after rolled-back transaction");
            assertEquals(initialTxCount, fundTransactionRepository.count(),
                    "FundTransaction row must NOT be committed on rollback");
            assertEquals(initialAuditCount, auditLogRepository.count(),
                    "AuditLog entry must NOT be committed on rollback");
        }

        @Test
        @DisplayName("1.2. Withdrawal overdraft rejection cleanly rolls back without partial financial state")
        void testWithdrawalRollbackOnInsufficientBalance() {
            BigDecimal initialBalance = testFund.getCurrentBalance(); // 10,000,000.00
            long initialTxCount = fundTransactionRepository.count();

            FundWithdrawalRequest overdraftRequest = new FundWithdrawalRequest();
            overdraftRequest.setAmount(new BigDecimal("25000000.00")); // Exceeds 10,000,000.00
            overdraftRequest.setAllowOverdraft(false);
            overdraftRequest.setDescription("Illegal overdraft attempt");

            assertThrows(InsufficientFundBalanceException.class, () -> {
                sharedFundService.withdraw(testGroup.getId(), overdraftRequest, owner1.getId(), "127.0.0.1");
            });

            SharedFund reloadedFund = sharedFundRepository.findById(testFund.getId()).orElseThrow();
            assertEquals(0, initialBalance.compareTo(reloadedFund.getCurrentBalance()),
                    "Fund balance must remain strictly unchanged upon overdraft rejection");
            assertEquals(initialTxCount, fundTransactionRepository.count(),
                    "No transaction ledger row should be recorded on overdraft rejection");
        }

        @Test
        @DisplayName("1.3. Payment settlement rollback cleanly prevents partial financial updates")
        void testPaymentSettlementRollbackOnException() {
            BigDecimal initialFundBalance = testFund.getCurrentBalance();
            long initialPaymentCount = paymentRepository.count();

            TransactionTemplate tt = new TransactionTemplate(transactionManager);

            assertThrows(RuntimeException.class, () -> {
                tt.execute(status -> {
                    InitiatePaymentRequest req = new InitiatePaymentRequest();
                    req.setUserId(owner1.getId());
                    req.setFundId(testFund.getId());
                    req.setAmount(new BigDecimal("1500000.00"));
                    req.setPaymentMethod(PaymentMethod.MOCK);
                    req.setDescription("Payment doomed to fail");

                    // Initiates payment
                    paymentService.initiatePayment(req, "IDEM-FAIL-" + UUID.randomUUID());

                    // Force transaction abort
                    throw new RuntimeException("Forced rollback during payment orchestration");
                });
            });

            SharedFund reloadedFund = sharedFundRepository.findById(testFund.getId()).orElseThrow();
            assertEquals(0, initialFundBalance.compareTo(reloadedFund.getCurrentBalance()),
                    "Fund balance must roll back if payment transaction fails");
            assertEquals(initialPaymentCount, paymentRepository.count(),
                    "Payment entity must NOT be persisted if transaction aborts");
        }
    }

    // =========================================================================
    // 2. CONCURRENT UPDATES & PESSIMISTIC LOCKING
    // =========================================================================

    @Nested
    @DisplayName("2. Concurrency Safety & Race Condition Protection")
    class ConcurrencyProtectionTests {

        @Test
        @DisplayName("2.1. Concurrent multi-threaded deposits and withdrawals execute without lost updates")
        void testConcurrentFundDepositsAndWithdrawals() throws InterruptedException {
            int threadCount = 10;
            ExecutorService executor = Executors.newFixedThreadPool(threadCount);
            CountDownLatch readyLatch = new CountDownLatch(threadCount);
            CountDownLatch startLatch = new CountDownLatch(1);
            CountDownLatch doneLatch = new CountDownLatch(threadCount);

            AtomicInteger successCount = new AtomicInteger(0);
            AtomicInteger failureCount = new AtomicInteger(0);

            BigDecimal depositAmount = new BigDecimal("1000000.00");
            BigDecimal withdrawAmount = new BigDecimal("500000.00");

            // 5 deposits of 1,000,000 and 5 withdrawals of 500,000
            // Initial: 10,000,000.00
            // Expected: 10,000,000 + (5 * 1,000,000) - (5 * 500,000) = 12,500,000.00
            for (int i = 0; i < threadCount; i++) {
                final boolean isDeposit = (i % 2 == 0);
                executor.submit(() -> {
                    readyLatch.countDown();
                    try {
                        startLatch.await();
                        if (isDeposit) {
                            FundContributionRequest req = new FundContributionRequest();
                            req.setAmount(depositAmount);
                            req.setDescription("Concurrent deposit");
                            req.setSource(FundTransactionSource.MEMBER_CONTRIBUTION);
                            sharedFundService.contribute(testGroup.getId(), req, owner1.getId(), "127.0.0.1");
                        } else {
                            FundWithdrawalRequest req = new FundWithdrawalRequest();
                            req.setAmount(withdrawAmount);
                            req.setDescription("Concurrent withdrawal");
                            req.setSource(FundTransactionSource.EXPENSE_PAYOUT);
                            sharedFundService.withdraw(testGroup.getId(), req, owner2.getId(), "127.0.0.1");
                        }
                        successCount.incrementAndGet();
                    } catch (Exception e) {
                        failureCount.incrementAndGet();
                    } finally {
                        doneLatch.countDown();
                    }
                });
            }

            readyLatch.await();
            startLatch.countDown();
            assertTrue(doneLatch.await(30, TimeUnit.SECONDS), "Concurrent operations should complete within timeout");
            executor.shutdown();

            assertEquals(threadCount, successCount.get(), "All 10 concurrent operations must succeed under pessimistic write locking");
            assertEquals(0, failureCount.get(), "Zero operations should fail");

            // Verify exact balance equality
            SharedFund reloaded = sharedFundRepository.findById(testFund.getId()).orElseThrow();
            BigDecimal expectedBalance = new BigDecimal("12500000.00");
            assertEquals(0, expectedBalance.compareTo(reloaded.getCurrentBalance()),
                    "Final balance must match exact sum of concurrent credits and debits");

            // Verify mathematical reconciliation
            FundReconciliationResponse recon = sharedFundService.reconcileFundBalance(testGroup.getId(), owner1.getId());
            assertTrue(recon.getIsReconciled(), "Ledger reconciliation must succeed");
            assertEquals(0, BigDecimal.ZERO.compareTo(recon.getReconciliationDelta()),
                    "Reconciliation delta must be 0.00 VND");
            assertEquals(6, recon.getCreditCount(), "1 initial vault credit + 5 concurrent deposits = 6 credits");
            assertEquals(5, recon.getDebitCount(), "5 concurrent withdrawals = 5 debits");
        }

        @Test
        @DisplayName("2.2. Concurrent payment state transitions serialize under pessimistic locking")
        void testConcurrentPaymentStateTransitions() throws InterruptedException {
            // Create a payment in PENDING
            Payment payment = new Payment();
            payment.setUser(owner1);
            payment.setFund(testFund);
            payment.setAmount(new BigDecimal("2000000.00"));
            payment.setPaymentMethod(PaymentMethod.GATEWAY);
            payment.setTransactionReference("TX-RACE-" + UUID.randomUUID());
            payment.setStatus(PaymentStatus.PENDING);
            Payment saved = paymentRepository.saveAndFlush(payment);

            Long paymentId = saved.getId();
            int threads = 2;
            ExecutorService executor = Executors.newFixedThreadPool(threads);
            CountDownLatch readyLatch = new CountDownLatch(threads);
            CountDownLatch startLatch = new CountDownLatch(1);
            CountDownLatch doneLatch = new CountDownLatch(threads);

            AtomicInteger successTransitions = new AtomicInteger(0);
            AtomicInteger conflictTransitions = new AtomicInteger(0);

            // Thread 1 attempts: PENDING -> SUCCESS
            executor.submit(() -> {
                readyLatch.countDown();
                try {
                    startLatch.await();
                    paymentLifecycleService.transitionStatus(paymentId, PaymentStatus.SUCCESS, "Winner confirmation", owner1.getId());
                    successTransitions.incrementAndGet();
                } catch (Exception e) {
                    conflictTransitions.incrementAndGet();
                } finally {
                    doneLatch.countDown();
                }
            });

            // Thread 2 attempts: PENDING -> CANCELLED
            executor.submit(() -> {
                readyLatch.countDown();
                try {
                    startLatch.await();
                    paymentLifecycleService.transitionStatus(paymentId, PaymentStatus.CANCELLED, "Competing cancellation", owner1.getId());
                    successTransitions.incrementAndGet();
                } catch (Exception e) {
                    conflictTransitions.incrementAndGet();
                } finally {
                    doneLatch.countDown();
                }
            });

            readyLatch.await();
            startLatch.countDown();
            assertTrue(doneLatch.await(10, TimeUnit.SECONDS));
            executor.shutdown();

            // Under pessimistic write lock, one thread wins and the other thread encounters an invalid transition
            assertEquals(1, successTransitions.get(), "Exactly ONE transition should succeed");
            assertEquals(1, conflictTransitions.get(), "The competing transition must be rejected by state machine guards");

            Payment finalPayment = paymentRepository.findById(paymentId).orElseThrow();
            assertTrue(finalPayment.getStatus() == PaymentStatus.SUCCESS || finalPayment.getStatus() == PaymentStatus.CANCELLED);
        }
    }

    // =========================================================================
    // 3. PAYMENT / EXPENSE / FUND END-TO-END CONSISTENCY
    // =========================================================================

    @Nested
    @DisplayName("3. Cross-Entity Financial Consistency")
    class CrossEntityConsistencyTests {

        @Test
        @DisplayName("3.1. Settling payment atomically updates Allocation (isSettled=true) and Fund (credited)")
        void testPaymentExpenseFundConsistencyOnSuccess() {
            BigDecimal initialFundBalance = testFund.getCurrentBalance(); // 10,000,000.00

            // 1. Create an Expense
            CreateExpenseRequest expReq = new CreateExpenseRequest();
            expReq.setGroupId(testGroup.getId());
            expReq.setVehicleId(testVehicle.getId());
            expReq.setTitle("Periodic Maintenance Service");
            expReq.setCategory(ExpenseCategory.MAINTENANCE);
            expReq.setAmount(new BigDecimal("5000000.00"));
            expReq.setCurrency("VND");
            expReq.setIncurredDate(LocalDate.now());
            expReq.setAllocationStrategy(AllocationStrategy.OWNERSHIP_BASED);
            expReq.setInvoiceReference("INV-SAFE-" + UUID.randomUUID().toString().substring(0, 8));

            var expenseResponse = expenseService.createExpense(expReq, owner1.getId(), "127.0.0.1");
            assertNotNull(expenseResponse.getId());

            // 2. Allocate expense (60% to owner1 = 3,000,000, 40% to owner2 = 2,000,000)
            List<ExpenseAllocation> allocations = costAllocationService.applyAndPersistAllocations(expenseResponse.getId());
            assertEquals(2, allocations.size());

            ExpenseAllocation owner1Alloc = allocations.stream()
                    .filter(a -> a.getUser().getId().equals(owner1.getId()))
                    .findFirst().orElseThrow();
            assertFalse(owner1Alloc.getIsSettled(), "Allocation must initially be unsettled");
            assertNull(owner1Alloc.getSettledAt());

            // 3. Initiate payment for Owner 1's allocation (3,000,000.00 VND) with MOCK auto-confirm
            InitiatePaymentRequest payReq = new InitiatePaymentRequest();
            payReq.setUserId(owner1.getId());
            payReq.setFundId(testFund.getId());
            payReq.setExpenseAllocationId(owner1Alloc.getId());
            payReq.setAmount(owner1Alloc.getAllocatedAmount());
            payReq.setPaymentMethod(PaymentMethod.MOCK);
            payReq.setDescription("Payment for maintenance allocation");
            payReq.setMetadata(Map.of("autoConfirm", true));

            PaymentResponse payResp = paymentService.initiatePayment(payReq, "IDEM-SAFE-PAY-" + UUID.randomUUID());
            assertEquals(PaymentStatus.SUCCESS, payResp.getStatus(), "Mock payment provider should auto-confirm to SUCCESS");

            // 4. Assert Cross-Entity Financial Consistency:
            // A. Allocation must be marked settled
            ExpenseAllocation reloadedAlloc = expenseAllocationRepository.findById(owner1Alloc.getId()).orElseThrow();
            assertTrue(reloadedAlloc.getIsSettled(), "Allocation must be marked settled upon payment SUCCESS");
            assertNotNull(reloadedAlloc.getSettledAt());

            // B. Fund balance must be credited by 3,000,000.00
            SharedFund reloadedFund = sharedFundRepository.findById(testFund.getId()).orElseThrow();
            BigDecimal expectedNewFundBalance = initialFundBalance.add(new BigDecimal("3000000.00")).setScale(2, RoundingMode.HALF_EVEN);
            assertEquals(0, expectedNewFundBalance.compareTo(reloadedFund.getCurrentBalance()),
                    "SharedFund balance must be credited by payment amount");

            // C. FundTransaction ledger entry must exist with PAYMENT_SETTLEMENT source
            List<FundTransaction> txs = fundTransactionRepository.findByFundIdOrderByCreatedAtDesc(testFund.getId());
            assertFalse(txs.isEmpty());
            FundTransaction latestTx = txs.get(0);
            assertEquals(TransactionType.CONTRIBUTION, latestTx.getTransactionType());
            assertEquals(TransactionEntryType.CREDIT, latestTx.getEntryType());
            assertEquals(FundTransactionSource.PAYMENT_SETTLEMENT, latestTx.getSource());
            assertEquals(0, new BigDecimal("3000000.00").compareTo(latestTx.getAmount()));
            assertEquals(0, expectedNewFundBalance.compareTo(latestTx.getBalanceAfter()));

            // D. Mathematical ledger reconciliation must be 100% consistent
            FundReconciliationResponse recon = sharedFundService.reconcileFundBalance(testGroup.getId(), owner1.getId());
            assertTrue(recon.getIsReconciled());
            assertEquals(0, BigDecimal.ZERO.compareTo(recon.getReconciliationDelta()));
        }

        @Test
        @DisplayName("3.2. Refunding payment atomically un-settles Allocation and debits Fund")
        void testPaymentRefundReversalConsistency() {
            BigDecimal initialFundBalance = testFund.getCurrentBalance();

            // 1. Setup payment tied to allocation and fund in SUCCESS status
            CreateExpenseRequest expReq = new CreateExpenseRequest();
            expReq.setGroupId(testGroup.getId());
            expReq.setVehicleId(testVehicle.getId());
            expReq.setTitle("Tire Replacement");
            expReq.setCategory(ExpenseCategory.REPAIR);
            expReq.setAmount(new BigDecimal("2000000.00"));
            expReq.setCurrency("VND");
            expReq.setIncurredDate(LocalDate.now());
            expReq.setAllocationStrategy(AllocationStrategy.OWNERSHIP_BASED);
            expReq.setInvoiceReference("INV-REFUND-" + UUID.randomUUID().toString().substring(0, 8));
            var expenseResp = expenseService.createExpense(expReq, owner2.getId(), "127.0.0.1");
            Expense expense = expenseRepository.findById(expenseResp.getId()).orElseThrow();

            ExpenseAllocation allocation = new ExpenseAllocation();
            allocation.setExpense(expense);
            allocation.setUser(owner2);
            allocation.setAllocatedAmount(new BigDecimal("2000000.00"));
            allocation.setIsSettled(false);
            allocation = expenseAllocationRepository.saveAndFlush(allocation);

            Payment payment = new Payment();
            payment.setUser(owner2);
            payment.setFund(testFund);
            payment.setExpenseAllocation(allocation);
            payment.setAmount(new BigDecimal("2000000.00"));
            payment.setPaymentMethod(PaymentMethod.BANK_TRANSFER);
            payment.setTransactionReference("TX-REV-" + UUID.randomUUID().toString().substring(0, 8));
            payment.setStatus(PaymentStatus.PENDING);
            payment = paymentRepository.saveAndFlush(payment);

            // Transition to SUCCESS
            Payment settled = paymentLifecycleService.transitionStatus(
                    payment.getId(), PaymentStatus.SUCCESS, "Direct settlement", owner2.getId());
            assertEquals(PaymentStatus.SUCCESS, settled.getStatus());

            // Verify settled state
            ExpenseAllocation settledAlloc = expenseAllocationRepository.findById(allocation.getId()).orElseThrow();
            assertTrue(settledAlloc.getIsSettled());
            SharedFund creditedFund = sharedFundRepository.findById(testFund.getId()).orElseThrow();
            assertEquals(0, initialFundBalance.add(new BigDecimal("2000000.00")).compareTo(creditedFund.getCurrentBalance()));

            // 2. Execute Refund: SUCCESS -> REFUNDED
            Payment refunded = paymentLifecycleService.transitionStatus(
                    payment.getId(), PaymentStatus.REFUNDED, "Dispute chargeback refund", owner2.getId());
            assertEquals(PaymentStatus.REFUNDED, refunded.getStatus());

            // 3. Verify Cross-Entity Reversal Consistency:
            // A. Allocation settlement is reverted
            ExpenseAllocation revertedAlloc = expenseAllocationRepository.findById(allocation.getId()).orElseThrow();
            assertFalse(revertedAlloc.getIsSettled(), "Allocation must revert to unsettled upon REFUNDED");
            assertNull(revertedAlloc.getSettledAt());

            // B. SharedFund balance is debited back to initial balance
            SharedFund refundedFund = sharedFundRepository.findById(testFund.getId()).orElseThrow();
            assertEquals(0, initialFundBalance.compareTo(refundedFund.getCurrentBalance()),
                    "SharedFund balance must be debited back upon payment REFUNDED");

            // C. Debit FundTransaction recorded
            List<FundTransaction> txs = fundTransactionRepository.findByFundIdOrderByCreatedAtDesc(testFund.getId());
            FundTransaction latestTx = txs.get(0);
            assertEquals(TransactionType.WITHDRAWAL, latestTx.getTransactionType());
            assertEquals(TransactionEntryType.DEBIT, latestTx.getEntryType());
            assertEquals(FundTransactionSource.MANUAL_ADJUSTMENT, latestTx.getSource());
            assertEquals(0, new BigDecimal("2000000.00").compareTo(latestTx.getAmount()));
            assertEquals(0, initialFundBalance.compareTo(latestTx.getBalanceAfter()));

            // D. Mathematical ledger reconciliation remains 100% consistent
            FundReconciliationResponse recon = sharedFundService.reconcileFundBalance(testGroup.getId(), owner2.getId());
            assertTrue(recon.getIsReconciled());
            assertEquals(0, BigDecimal.ZERO.compareTo(recon.getReconciliationDelta()));
        }
    }
}
