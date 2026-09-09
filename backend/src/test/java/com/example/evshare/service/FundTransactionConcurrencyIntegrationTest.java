package com.example.evshare.service;

import com.example.evshare.dto.request.FundContributionRequest;
import com.example.evshare.dto.request.FundWithdrawalRequest;
import com.example.evshare.dto.response.FundReconciliationResponse;
import com.example.evshare.dto.response.FundTransactionResponse;
import com.example.evshare.entity.*;
import com.example.evshare.entity.enums.FundTransactionSource;
import com.example.evshare.entity.enums.RoleName;
import com.example.evshare.entity.enums.TransactionEntryType;
import com.example.evshare.entity.enums.VehicleStatus;
import com.example.evshare.exception.InsufficientFundBalanceException;
import com.example.evshare.repository.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@DisplayName("06-I — Fund Transactions & Concurrency Integration Tests")
class FundTransactionConcurrencyIntegrationTest {

    private static final AtomicLong PHONE_SEQ = new AtomicLong(System.currentTimeMillis() % 80000000L + 10000000L);

    @Autowired private SharedFundService sharedFundService;
    @Autowired private UserRepository userRepository;
    @Autowired private RoleRepository roleRepository;
    @Autowired private VehicleRepository vehicleRepository;
    @Autowired private OwnershipGroupRepository ownershipGroupRepository;
    @Autowired private OwnershipShareRepository ownershipShareRepository;
    @Autowired private SharedFundRepository sharedFundRepository;
    @Autowired private FundTransactionRepository fundTransactionRepository;
    @Autowired private AuditLogRepository auditLogRepository;

    private User coOwner;
    private OwnershipGroup group;
    private SharedFund fund;
    private Vehicle vehicle;
    private OwnershipShare share;

    @BeforeEach
    void setUp() {
        Role roleCoOwner = roleRepository.findByName(RoleName.ROLE_CO_OWNER)
                .orElseGet(() -> roleRepository.save(new Role(null, RoleName.ROLE_CO_OWNER)));

        String uid = UUID.randomUUID().toString().substring(0, 8);
        coOwner = new User();
        coOwner.setEmail("concur.owner." + uid + "@evshare.io");
        coOwner.setFullName("Concurrency Tester");
        coOwner.setPasswordHash("hashed_secret");
        coOwner.setPhoneNumber("09" + PHONE_SEQ.incrementAndGet());
        coOwner.setIsActive(true);
        coOwner.setRoles(Set.of(roleCoOwner));
        coOwner = userRepository.saveAndFlush(coOwner);

        vehicle = new Vehicle();
        vehicle.setVin("VIN" + uid.toUpperCase() + "998877");
        vehicle.setLicensePlate("30A-" + uid.toUpperCase());
        vehicle.setModelName("VinFast VF8 Eco");
        vehicle.setManufacturer("VinFast");
        vehicle.setModel3dAssetPath("models/vehicles/vf8.glb");
        vehicle.setStatus(VehicleStatus.AVAILABLE);
        vehicle = vehicleRepository.saveAndFlush(vehicle);

        group = new OwnershipGroup();
        group.setGroupName("Concurrency Syndicate " + uid);
        group.setVehicle(vehicle);
        group.setFormationDate(LocalDate.now());
        group.setIsActive(true);
        group = ownershipGroupRepository.saveAndFlush(group);

        share = new OwnershipShare();
        share.setGroup(group);
        share.setUser(coOwner);
        share.setPercentage(new BigDecimal("100.00"));
        share.setShareCertificateNumber("CERT-CONCUR-" + uid);
        share.setIsActive(true);
        share = ownershipShareRepository.saveAndFlush(share);

        fund = new SharedFund();
        fund.setGroup(group);
        fund.setCurrentBalance(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_EVEN));
        fund.setMinimumReserveThreshold(new BigDecimal("5000000.00"));
        fund.setCurrency("VND");
        fund = sharedFundRepository.saveAndFlush(fund);
    }

    @AfterEach
    void tearDown() {
        try {
            if (fund != null && fund.getId() != null) {
                auditLogRepository.deleteAll(auditLogRepository.findByEntityNameAndEntityIdOrderByIdDesc("SharedFund", fund.getId()));
                fundTransactionRepository.deleteAll(fundTransactionRepository.findByFundId(fund.getId()));
                sharedFundRepository.deleteById(fund.getId());
            }
            if (share != null && share.getId() != null) {
                ownershipShareRepository.deleteById(share.getId());
            }
            if (group != null && group.getId() != null) {
                ownershipGroupRepository.deleteById(group.getId());
            }
            if (vehicle != null && vehicle.getId() != null) {
                vehicleRepository.deleteById(vehicle.getId());
            }
            if (coOwner != null && coOwner.getId() != null) {
                userRepository.deleteById(coOwner.getId());
            }
        } catch (Exception ignored) {
            // Clean up best effort
        }
    }

    @Test
    @DisplayName("Concurrency & Reconciliation: Parallel contributions and withdrawals maintain exact balance without lost updates")
    void testConcurrentTransactions_AndReconciliation() throws Exception {
        // 1. Initial baseline deposit: 10,000,000.00 VND
        FundContributionRequest initReq = new FundContributionRequest(
                new BigDecimal("10000000.00"),
                "Initial syndicate seed vault funding",
                "TX-INIT-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase(),
                FundTransactionSource.VAULT_INITIALIZATION
        );
        FundTransactionResponse initTx = sharedFundService.contribute(group.getId(), initReq, coOwner.getId(), "127.0.0.1");
        assertNotNull(initTx);
        assertEquals(TransactionEntryType.CREDIT, initTx.getEntryType());

        // 2. Launch 10 concurrent transactions:
        //    5 threads depositing 1,000,000.00 VND each (+5,000,000.00 VND)
        //    5 threads withdrawing 500,000.00 VND each (-2,500,000.00 VND)
        int threadCount = 10;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch readyLatch = new CountDownLatch(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);

        AtomicInteger successCount = new AtomicInteger(0);
        List<Throwable> errors = new CopyOnWriteArrayList<>();

        for (int i = 0; i < 5; i++) {
            final int index = i;
            executor.submit(() -> {
                readyLatch.countDown();
                try {
                    startLatch.await();
                    FundContributionRequest req = new FundContributionRequest(
                            new BigDecimal("1000000.00"),
                            "Concurrent contribution #" + index,
                            "TX-CONC-CRD-" + index + "-" + UUID.randomUUID().toString().substring(0, 6).toUpperCase(),
                            FundTransactionSource.MEMBER_CONTRIBUTION
                    );
                    sharedFundService.contribute(group.getId(), req, coOwner.getId(), "127.0.0.1");
                    successCount.incrementAndGet();
                } catch (Throwable t) {
                    errors.add(t);
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        for (int i = 0; i < 5; i++) {
            final int index = i;
            executor.submit(() -> {
                readyLatch.countDown();
                try {
                    startLatch.await();
                    FundWithdrawalRequest req = new FundWithdrawalRequest(
                            new BigDecimal("500000.00"),
                            "Concurrent withdrawal #" + index,
                            false,
                            "TX-CONC-DBT-" + index + "-" + UUID.randomUUID().toString().substring(0, 6).toUpperCase(),
                            FundTransactionSource.EXPENSE_PAYOUT
                    );
                    sharedFundService.withdraw(group.getId(), req, coOwner.getId(), "127.0.0.1");
                    successCount.incrementAndGet();
                } catch (Throwable t) {
                    errors.add(t);
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        // Ready, set, fire simultaneously
        assertTrue(readyLatch.await(5, TimeUnit.SECONDS));
        startLatch.countDown();
        assertTrue(doneLatch.await(15, TimeUnit.SECONDS));
        executor.shutdown();

        if (!errors.isEmpty()) {
            fail("Errors encountered during concurrent execution: " + errors.get(0).getMessage());
        }

        assertEquals(10, successCount.get(), "All 10 concurrent transactions should complete successfully");

        // 3. Mathematical verification of final account balance:
        //    10,000,000.00 + (5 * 1,000,000.00) - (5 * 500,000.00) = 12,500,000.00 VND
        BigDecimal expectedBalance = new BigDecimal("12500000.00");
        SharedFund updatedFund = sharedFundRepository.findById(fund.getId()).orElseThrow();
        assertEquals(0, expectedBalance.compareTo(updatedFund.getCurrentBalance()),
                "Balance must be exactly 12,500,000.00 with zero lost updates");

        // 4. Mathematical balance reconciliation:
        //    sum(CREDITS) - sum(DEBITS) == currentBalance
        FundReconciliationResponse reconciliation = sharedFundService.reconcileFundBalance(group.getId(), coOwner.getId());
        assertNotNull(reconciliation);
        assertTrue(reconciliation.getIsReconciled(), "Ledger must be fully reconciled");
        assertEquals(0, BigDecimal.ZERO.compareTo(reconciliation.getReconciliationDelta()), "Reconciliation delta must be 0.00");
        assertEquals(expectedBalance, reconciliation.getCurrentBalance());
        assertEquals(expectedBalance, reconciliation.getCalculatedLedgerBalance());
        assertEquals(new BigDecimal("15000000.00"), reconciliation.getTotalCredits());
        assertEquals(new BigDecimal("2500000.00"), reconciliation.getTotalDebits());
        assertEquals(6, reconciliation.getCreditCount()); // 1 initial + 5 concurrent
        assertEquals(5, reconciliation.getDebitCount());
        assertEquals(11, reconciliation.getTransactionCount());
    }

    @Test
    @DisplayName("Insufficient Balance: Excessive withdrawal is rejected without modifying balance or ledger")
    void testInsufficientBalance_Rejected() {
        // Initial fund balance is 0.00 VND
        FundWithdrawalRequest req = new FundWithdrawalRequest(
                new BigDecimal("5000000.00"),
                "Unauthorized deficit withdrawal",
                false // allowOverdraft = false
        );

        assertThrows(InsufficientFundBalanceException.class, () ->
                sharedFundService.withdraw(group.getId(), req, coOwner.getId(), "127.0.0.1")
        );

        // Verify balance remains zero
        SharedFund currentFund = sharedFundRepository.findById(fund.getId()).orElseThrow();
        assertEquals(0, BigDecimal.ZERO.compareTo(currentFund.getCurrentBalance()));

        // Verify no transaction recorded
        List<FundTransaction> txs = fundTransactionRepository.findByFundId(fund.getId());
        assertTrue(txs.isEmpty());
    }
}
