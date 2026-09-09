package com.example.evshare.service;

import com.example.evshare.dto.request.FundContributionRequest;
import com.example.evshare.dto.request.FundWithdrawalRequest;
import com.example.evshare.dto.response.FundAuditLogResponse;
import com.example.evshare.dto.response.FundReconciliationResponse;
import com.example.evshare.dto.response.FundTransactionResponse;
import com.example.evshare.dto.response.SharedFundResponse;
import com.example.evshare.entity.*;
import com.example.evshare.entity.enums.FundTransactionSource;
import com.example.evshare.entity.enums.RoleName;
import com.example.evshare.entity.enums.TransactionEntryType;
import com.example.evshare.entity.enums.TransactionType;
import com.example.evshare.exception.BusinessException;
import com.example.evshare.exception.InsufficientFundBalanceException;
import com.example.evshare.repository.*;
import com.example.evshare.service.impl.SharedFundServiceImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("06-H — Shared Fund Unit Tests")
class SharedFundServiceTest {

    @Mock private SharedFundRepository sharedFundRepository;
    @Mock private FundTransactionRepository fundTransactionRepository;
    @Mock private OwnershipGroupRepository ownershipGroupRepository;
    @Mock private OwnershipShareRepository ownershipShareRepository;
    @Mock private UserRepository userRepository;
    @Mock private AuditLogRepository auditLogRepository;

    private SharedFundService sharedFundService;
    private ObjectMapper objectMapper;

    private OwnershipGroup group;
    private User coOwner1;
    private User nonMember;
    private User adminUser;
    private SharedFund sharedFund;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        sharedFundService = new SharedFundServiceImpl(
                sharedFundRepository,
                fundTransactionRepository,
                ownershipGroupRepository,
                ownershipShareRepository,
                userRepository,
                auditLogRepository,
                objectMapper
        );

        Role coOwnerRole = new Role(3L, RoleName.ROLE_CO_OWNER);
        Role adminRole = new Role(1L, RoleName.ROLE_ADMIN);

        coOwner1 = new User();
        coOwner1.setId(101L);
        coOwner1.setFullName("Nguyen Van A");
        coOwner1.setEmail("a@evshare.io");
        coOwner1.setRoles(Set.of(coOwnerRole));

        nonMember = new User();
        nonMember.setId(999L);
        nonMember.setFullName("Stranger User");
        nonMember.setEmail("stranger@evshare.io");
        nonMember.setRoles(Set.of(coOwnerRole));

        adminUser = new User();
        adminUser.setId(1L);
        adminUser.setFullName("Admin Root");
        adminUser.setEmail("admin@evshare.io");
        adminUser.setRoles(Set.of(adminRole));

        group = new OwnershipGroup();
        group.setId(10L);
        group.setGroupName("VF9 Synergy Syndicate");

        sharedFund = new SharedFund();
        sharedFund.setId(50L);
        sharedFund.setGroup(group);
        sharedFund.setCurrentBalance(new BigDecimal("15000000.00"));
        sharedFund.setMinimumReserveThreshold(new BigDecimal("10000000.00"));
        sharedFund.setCurrency("VND");
        sharedFund.setUpdatedAt(Instant.now());
    }

    private void mockActiveCoOwner(User user) {
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(ownershipGroupRepository.findById(group.getId())).thenReturn(Optional.of(group));
        OwnershipShare share = new OwnershipShare();
        share.setIsActive(true);
        when(ownershipShareRepository.findByGroupIdAndUserId(group.getId(), user.getId())).thenReturn(Optional.of(share));
    }

    // =========================================================================
    // 1. BALANCE TESTS
    // =========================================================================
    @Nested
    @DisplayName("Balance & Reserve Status Tests")
    class BalanceTests {

        @Test
        @DisplayName("Balance: Normal liquidity state (balance >= reserve threshold)")
        void testBalance_NormalLiquidity() {
            mockActiveCoOwner(coOwner1);
            when(sharedFundRepository.findByGroupId(group.getId())).thenReturn(Optional.of(sharedFund));

            SharedFundResponse response = sharedFundService.getFundBalance(group.getId(), coOwner1.getId());

            assertNotNull(response);
            assertEquals(50L, response.getId());
            assertEquals(new BigDecimal("15000000.00"), response.getCurrentBalance());
            assertEquals(new BigDecimal("10000000.00"), response.getMinimumReserveThreshold());
            assertEquals("VND", response.getCurrency());
            assertFalse(response.getIsLowLiquidity(), "Balance 15M >= 10M threshold, lowLiquidity must be false");
        }

        @Test
        @DisplayName("Balance: Low liquidity warning state (balance < reserve threshold)")
        void testBalance_LowLiquidityWarning() {
            mockActiveCoOwner(coOwner1);
            sharedFund.setCurrentBalance(new BigDecimal("6000000.00")); // < 10,000,000.00
            when(sharedFundRepository.findByGroupId(group.getId())).thenReturn(Optional.of(sharedFund));

            SharedFundResponse response = sharedFundService.getFundBalance(group.getId(), coOwner1.getId());

            assertNotNull(response);
            assertEquals(new BigDecimal("6000000.00"), response.getCurrentBalance());
            assertTrue(response.getIsLowLiquidity(), "Balance 6M < 10M threshold, lowLiquidity must be true");
        }

        @Test
        @DisplayName("Balance: Auto-initializes fund if group does not have one yet")
        void testBalance_AutoInitializesFundIfAbsent() {
            mockActiveCoOwner(coOwner1);
            when(sharedFundRepository.findByGroupId(group.getId())).thenReturn(Optional.empty());

            SharedFund newFund = new SharedFund();
            newFund.setId(77L);
            newFund.setGroup(group);
            newFund.setCurrentBalance(BigDecimal.ZERO);
            newFund.setMinimumReserveThreshold(new BigDecimal("10000000.00"));
            newFund.setCurrency("VND");
            when(sharedFundRepository.save(any(SharedFund.class))).thenReturn(newFund);

            SharedFundResponse response = sharedFundService.getFundBalance(group.getId(), coOwner1.getId());

            assertNotNull(response);
            assertEquals(BigDecimal.ZERO, response.getCurrentBalance());
            assertTrue(response.getIsLowLiquidity());
        }
    }

    // =========================================================================
    // 2. CONTRIBUTION TESTS
    // =========================================================================
    @Nested
    @DisplayName("Contribution Tests")
    class ContributionTests {

        @Test
        @DisplayName("Contribution: Increases balance atomically, records immutable transaction, and logs audit")
        void testContribution_Success() {
            mockActiveCoOwner(coOwner1);
            when(sharedFundRepository.findByGroupIdWithLock(group.getId())).thenReturn(Optional.of(sharedFund));
            when(sharedFundRepository.save(any(SharedFund.class))).thenAnswer(inv -> inv.getArgument(0));

            when(fundTransactionRepository.save(any(FundTransaction.class))).thenAnswer(inv -> {
                FundTransaction tx = inv.getArgument(0);
                tx.setId(1001L);
                return tx;
            });

            FundContributionRequest request = new FundContributionRequest(
                    new BigDecimal("5000000.00"),
                    "Quarterly reserve replenishment"
            );

            FundTransactionResponse response = sharedFundService.contribute(
                    group.getId(), request, coOwner1.getId(), "192.168.1.50"
            );

            // 1. Balance increased from 15M to 20M
            assertEquals(new BigDecimal("20000000.00"), sharedFund.getCurrentBalance());

            // 2. FundTransaction record verified
            assertNotNull(response);
            assertEquals(1001L, response.getId());
            assertEquals(TransactionType.CONTRIBUTION, response.getTransactionType());
            assertEquals(new BigDecimal("5000000.00"), response.getAmount());
            assertEquals(new BigDecimal("20000000.00"), response.getBalanceAfter());
            assertEquals("Quarterly reserve replenishment", response.getDescription());

            // 3. AuditLog record verified
            ArgumentCaptor<AuditLog> auditCaptor = ArgumentCaptor.forClass(AuditLog.class);
            verify(auditLogRepository).save(auditCaptor.capture());
            AuditLog auditLog = auditCaptor.getValue();

            assertEquals("SharedFund", auditLog.getEntityName());
            assertEquals(50L, auditLog.getEntityId());
            assertEquals("SHARED_FUND_CONTRIBUTION", auditLog.getAction());
            assertEquals("192.168.1.50", auditLog.getIpAddress());
            assertTrue(auditLog.getOldStateJson().contains("15000000.00"));
            assertTrue(auditLog.getNewStateJson().contains("20000000.00"));
        }

        @Test
        @DisplayName("Contribution: Rejects non-positive amounts")
        void testContribution_NonPositiveAmount_Rejected() {
            FundContributionRequest zeroRequest = new FundContributionRequest(BigDecimal.ZERO, "Zero deposit");
            assertThrows(BusinessException.class, () ->
                    sharedFundService.contribute(group.getId(), zeroRequest, coOwner1.getId(), "127.0.0.1")
            );

            FundContributionRequest negRequest = new FundContributionRequest(new BigDecimal("-1000.00"), "Negative deposit");
            assertThrows(BusinessException.class, () ->
                    sharedFundService.contribute(group.getId(), negRequest, coOwner1.getId(), "127.0.0.1")
            );
        }

        @Test
        @DisplayName("Contribution: Non-member co-owner is forbidden (403)")
        void testContribution_NonMemberForbidden() {
            when(userRepository.findById(nonMember.getId())).thenReturn(Optional.of(nonMember));
            when(ownershipGroupRepository.findById(group.getId())).thenReturn(Optional.of(group));
            when(ownershipShareRepository.findByGroupIdAndUserId(group.getId(), nonMember.getId())).thenReturn(Optional.empty());

            FundContributionRequest request = new FundContributionRequest(new BigDecimal("100000.00"), "Deposit");
            BusinessException ex = assertThrows(BusinessException.class, () ->
                    sharedFundService.contribute(group.getId(), request, nonMember.getId(), "127.0.0.1")
            );
            assertEquals(HttpStatus.FORBIDDEN, ex.getStatus());
        }
    }

    // =========================================================================
    // 3. WITHDRAWAL TESTS (NO NEGATIVE BALANCE UNLESS PERMITTED)
    // =========================================================================
    @Nested
    @DisplayName("Withdrawal Tests & Overdraft Policy")
    class WithdrawalTests {

        @Test
        @DisplayName("Withdrawal: Decreases balance atomically, records immutable transaction, and logs audit")
        void testWithdrawal_Success() {
            mockActiveCoOwner(coOwner1);
            when(sharedFundRepository.findByGroupIdWithLock(group.getId())).thenReturn(Optional.of(sharedFund));
            when(sharedFundRepository.save(any(SharedFund.class))).thenAnswer(inv -> inv.getArgument(0));

            when(fundTransactionRepository.save(any(FundTransaction.class))).thenAnswer(inv -> {
                FundTransaction tx = inv.getArgument(0);
                tx.setId(2001L);
                return tx;
            });

            FundWithdrawalRequest request = new FundWithdrawalRequest(
                    new BigDecimal("3000000.00"),
                    "Garage parking rent payment",
                    false
            );

            FundTransactionResponse response = sharedFundService.withdraw(
                    group.getId(), request, coOwner1.getId(), "192.168.1.50"
            );

            // 1. Balance decreased from 15M to 12M
            assertEquals(new BigDecimal("12000000.00"), sharedFund.getCurrentBalance());

            // 2. Transaction details
            assertNotNull(response);
            assertEquals(2001L, response.getId());
            assertEquals(TransactionType.WITHDRAWAL, response.getTransactionType());
            assertEquals(new BigDecimal("3000000.00"), response.getAmount());
            assertEquals(new BigDecimal("12000000.00"), response.getBalanceAfter());

            // 3. Audit log details
            ArgumentCaptor<AuditLog> auditCaptor = ArgumentCaptor.forClass(AuditLog.class);
            verify(auditLogRepository).save(auditCaptor.capture());
            AuditLog auditLog = auditCaptor.getValue();
            assertEquals("SHARED_FUND_WITHDRAWAL", auditLog.getAction());
            assertTrue(auditLog.getOldStateJson().contains("15000000.00"));
            assertTrue(auditLog.getNewStateJson().contains("12000000.00"));
        }

        @Test
        @DisplayName("Withdrawal: Strict rejection of negative balance when allowOverdraft is false")
        void testWithdrawal_InsufficientBalance_ThrowsException() {
            mockActiveCoOwner(coOwner1);
            // Current balance is 15,000,000.00 VND
            when(sharedFundRepository.findByGroupIdWithLock(group.getId())).thenReturn(Optional.of(sharedFund));

            FundWithdrawalRequest request = new FundWithdrawalRequest(
                    new BigDecimal("16000000.00"), // Exceeds balance by 1M
                    "Traction battery emergency replacement",
                    false // Overdraft strictly forbidden
            );

            InsufficientFundBalanceException ex = assertThrows(InsufficientFundBalanceException.class, () ->
                    sharedFundService.withdraw(group.getId(), request, coOwner1.getId(), "127.0.0.1")
            );

            assertEquals(50L, ex.getFundId());
            assertEquals(new BigDecimal("15000000.00"), ex.getCurrentBalance());
            assertEquals(new BigDecimal("16000000.00"), ex.getRequestedAmount());
            assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());

            // Verify fund balance unchanged
            assertEquals(new BigDecimal("15000000.00"), sharedFund.getCurrentBalance());
            verify(fundTransactionRepository, never()).save(any());
            verify(auditLogRepository, never()).save(any());
        }

        @Test
        @DisplayName("Withdrawal: Negative balance allowed ONLY when allowOverdraft is explicitly true")
        void testWithdrawal_OverdraftExplicitlyPermitted_NegativeBalanceAllowed() {
            mockActiveCoOwner(coOwner1);
            when(sharedFundRepository.findByGroupIdWithLock(group.getId())).thenReturn(Optional.of(sharedFund));
            when(sharedFundRepository.save(any(SharedFund.class))).thenAnswer(inv -> inv.getArgument(0));
            when(fundTransactionRepository.save(any(FundTransaction.class))).thenAnswer(inv -> inv.getArgument(0));

            FundWithdrawalRequest request = new FundWithdrawalRequest(
                    new BigDecimal("18000000.00"), // Exceeds balance by 3M
                    "Authorized emergency repair with syndicate overdraft agreement",
                    true // EXPLICIT OVERDRAFT PERMITTED
            );

            FundTransactionResponse response = sharedFundService.withdraw(
                    group.getId(), request, coOwner1.getId(), "127.0.0.1"
            );

            // New balance is negative (-3,000,000.00 VND)
            assertEquals(new BigDecimal("-3000000.00"), sharedFund.getCurrentBalance());
            assertEquals(new BigDecimal("-3000000.00"), response.getBalanceAfter());

            ArgumentCaptor<AuditLog> auditCaptor = ArgumentCaptor.forClass(AuditLog.class);
            verify(auditLogRepository).save(auditCaptor.capture());
            assertTrue(auditCaptor.getValue().getNewStateJson().contains("WITHDRAWAL_OVERDRAFT"));
        }

        @Test
        @DisplayName("Withdrawal: Rejects non-positive amounts")
        void testWithdrawal_NonPositiveAmount_Rejected() {
            FundWithdrawalRequest zeroRequest = new FundWithdrawalRequest(BigDecimal.ZERO, "Zero withdrawal", false);
            assertThrows(BusinessException.class, () ->
                    sharedFundService.withdraw(group.getId(), zeroRequest, coOwner1.getId(), "127.0.0.1")
            );

            FundWithdrawalRequest negRequest = new FundWithdrawalRequest(new BigDecimal("-500.00"), "Negative withdrawal", false);
            assertThrows(BusinessException.class, () ->
                    sharedFundService.withdraw(group.getId(), negRequest, coOwner1.getId(), "127.0.0.1")
            );
        }
    }

    // =========================================================================
    // 4. TRANSACTION IMMUTABILITY TESTS
    // =========================================================================
    @Nested
    @DisplayName("Transaction Immutability Tests")
    class ImmutabilityTests {

        @Test
        @DisplayName("Immutability: Attempting to update or delete a FundTransaction throws IllegalStateException")
        void testFundTransaction_ImmutabilityEnforced() {
            FundTransaction tx = new FundTransaction();
            tx.setId(100L);
            tx.setAmount(new BigDecimal("1000000.00"));
            tx.setDescription("Original ledger entry");

            // Verify lifecycle callback throws IllegalStateException on update
            IllegalStateException updateEx = assertThrows(IllegalStateException.class, tx::onPreUpdate);
            assertTrue(updateEx.getMessage().contains("strictly immutable and cannot be updated"));

            // Verify lifecycle callback throws IllegalStateException on delete
            IllegalStateException removeEx = assertThrows(IllegalStateException.class, tx::onPreRemove);
            assertTrue(removeEx.getMessage().contains("strictly immutable and cannot be deleted"));
        }
    }

    // =========================================================================
    // 5. HISTORY & AUDIT RETRIEVAL TESTS
    // =========================================================================
    @Nested
    @DisplayName("History & Audit Trail Retrieval Tests")
    class HistoryAndAuditTests {

        @Test
        @DisplayName("Transaction History: Returns chronological transactions in descending order")
        void testGetTransactionHistory() {
            mockActiveCoOwner(coOwner1);
            when(sharedFundRepository.findByGroupId(group.getId())).thenReturn(Optional.of(sharedFund));

            FundTransaction tx1 = new FundTransaction(1L, sharedFund, coOwner1, TransactionType.CONTRIBUTION,
                    new BigDecimal("5000000.00"), new BigDecimal("15000000.00"), "Contribution 1", Instant.now().minusSeconds(3600));
            FundTransaction tx2 = new FundTransaction(2L, sharedFund, coOwner1, TransactionType.WITHDRAWAL,
                    new BigDecimal("2000000.00"), new BigDecimal("13000000.00"), "Withdrawal 1", Instant.now());

            when(fundTransactionRepository.findByFundIdOrderByCreatedAtDesc(sharedFund.getId()))
                    .thenReturn(List.of(tx2, tx1));

            List<FundTransactionResponse> history = sharedFundService.getTransactionHistory(group.getId(), coOwner1.getId());

            assertEquals(2, history.size());
            assertEquals(2L, history.get(0).getId());
            assertEquals(TransactionType.WITHDRAWAL, history.get(0).getTransactionType());
            assertEquals(1L, history.get(1).getId());
            assertEquals(TransactionType.CONTRIBUTION, history.get(1).getTransactionType());
        }

        @Test
        @DisplayName("Audit History: Returns audit trail records for the SharedFund")
        void testGetFundAuditHistory() {
            mockActiveCoOwner(coOwner1);
            when(sharedFundRepository.findByGroupId(group.getId())).thenReturn(Optional.of(sharedFund));

            AuditLog log1 = new AuditLog();
            log1.setId(10L);
            log1.setUser(coOwner1);
            log1.setAction("SHARED_FUND_CONTRIBUTION");
            log1.setEntityName("SharedFund");
            log1.setEntityId(sharedFund.getId());
            log1.setNewStateJson("{\"balance\":\"15000000.00\"}");
            log1.setCreatedAt(Instant.now());

            when(auditLogRepository.findByEntityNameAndEntityIdOrderByIdDesc("SharedFund", sharedFund.getId()))
                    .thenReturn(List.of(log1));

            List<FundAuditLogResponse> auditHistory = sharedFundService.getFundAuditHistory(group.getId(), coOwner1.getId());

            assertEquals(1, auditHistory.size());
            assertEquals(10L, auditHistory.get(0).getId());
            assertEquals("SHARED_FUND_CONTRIBUTION", auditHistory.get(0).getAction());
            assertEquals(coOwner1.getId(), auditHistory.get(0).getActorId());
        }
    }

    // =========================================================================
    // 6. CHECKPOINT 06-I — FUND TRANSACTIONS & RECONCILIATION TESTS
    // =========================================================================
    @Nested
    @DisplayName("06-I — Ledger Semantics & Balance Reconciliation Tests")
    class FundTransactionsAndReconciliationTests {

        @Test
        @DisplayName("Transaction: Contribution sets CREDIT entryType, custom reference, source, actor, and timestamp")
        void testContribution_ExplicitSemantics_CreditAndCustomReference() {
            mockActiveCoOwner(coOwner1);
            when(sharedFundRepository.findByGroupIdWithLock(group.getId())).thenReturn(Optional.of(sharedFund));
            when(sharedFundRepository.save(any(SharedFund.class))).thenAnswer(inv -> inv.getArgument(0));

            when(fundTransactionRepository.existsByTransactionReference("TX-REF-CONTRIB-01")).thenReturn(false);
            when(fundTransactionRepository.save(any(FundTransaction.class))).thenAnswer(inv -> {
                FundTransaction tx = inv.getArgument(0);
                tx.setId(201L);
                return tx;
            });

            FundContributionRequest request = new FundContributionRequest(
                    new BigDecimal("3000000.00"),
                    "Syndicate solar charging deposit",
                    "TX-REF-CONTRIB-01",
                    FundTransactionSource.MEMBER_CONTRIBUTION
            );

            FundTransactionResponse response = sharedFundService.contribute(
                    group.getId(), request, coOwner1.getId(), "10.0.0.1"
            );

            assertNotNull(response);
            assertEquals(201L, response.getId());
            assertEquals(TransactionEntryType.CREDIT, response.getEntryType());
            assertEquals(TransactionType.CONTRIBUTION, response.getTransactionType());
            assertEquals("TX-REF-CONTRIB-01", response.getTransactionReference());
            assertEquals(FundTransactionSource.MEMBER_CONTRIBUTION, response.getSource());
            assertEquals(coOwner1.getId(), response.getUserId());
            assertEquals(coOwner1.getFullName(), response.getUserName());
            assertNotNull(response.getCreatedAt());
            assertEquals(new BigDecimal("18000000.00"), response.getBalanceAfter());
        }

        @Test
        @DisplayName("Transaction: Withdrawal sets DEBIT entryType, custom reference, and EXPENSE_PAYOUT source")
        void testWithdrawal_ExplicitSemantics_DebitAndSource() {
            mockActiveCoOwner(coOwner1);
            when(sharedFundRepository.findByGroupIdWithLock(group.getId())).thenReturn(Optional.of(sharedFund));
            when(sharedFundRepository.save(any(SharedFund.class))).thenAnswer(inv -> inv.getArgument(0));

            when(fundTransactionRepository.existsByTransactionReference("TX-REF-DEBIT-01")).thenReturn(false);
            when(fundTransactionRepository.save(any(FundTransaction.class))).thenAnswer(inv -> {
                FundTransaction tx = inv.getArgument(0);
                tx.setId(202L);
                return tx;
            });

            FundWithdrawalRequest request = new FundWithdrawalRequest(
                    new BigDecimal("2000000.00"),
                    "Tire replacement payout",
                    false,
                    "TX-REF-DEBIT-01",
                    FundTransactionSource.EXPENSE_PAYOUT
            );

            FundTransactionResponse response = sharedFundService.withdraw(
                    group.getId(), request, coOwner1.getId(), "10.0.0.1"
            );

            assertNotNull(response);
            assertEquals(202L, response.getId());
            assertEquals(TransactionEntryType.DEBIT, response.getEntryType());
            assertEquals(TransactionType.WITHDRAWAL, response.getTransactionType());
            assertEquals("TX-REF-DEBIT-01", response.getTransactionReference());
            assertEquals(FundTransactionSource.EXPENSE_PAYOUT, response.getSource());
            assertEquals(new BigDecimal("13000000.00"), response.getBalanceAfter());
        }

        @Test
        @DisplayName("Transaction: Duplicate transaction reference throws CONFLICT (409)")
        void testTransaction_DuplicateReference_ThrowsConflict() {
            mockActiveCoOwner(coOwner1);
            when(sharedFundRepository.findByGroupIdWithLock(group.getId())).thenReturn(Optional.of(sharedFund));
            when(fundTransactionRepository.existsByTransactionReference("TX-DUPLICATE-001")).thenReturn(true);

            FundContributionRequest request = new FundContributionRequest(
                    new BigDecimal("1000000.00"),
                    "Duplicate test",
                    "TX-DUPLICATE-001",
                    FundTransactionSource.MEMBER_CONTRIBUTION
            );

            BusinessException ex = assertThrows(BusinessException.class, () ->
                    sharedFundService.contribute(group.getId(), request, coOwner1.getId(), "127.0.0.1")
            );

            assertEquals(HttpStatus.CONFLICT, ex.getStatus());
            assertTrue(ex.getMessage().contains("already exists"));
        }

        @Test
        @DisplayName("Reconciliation: Perfect mathematical audit match (sum(credits) - sum(debits) == currentBalance)")
        void testReconciliation_Success() {
            mockActiveCoOwner(coOwner1);
            sharedFund.setCurrentBalance(new BigDecimal("17000000.00"));
            when(sharedFundRepository.findByGroupId(group.getId())).thenReturn(Optional.of(sharedFund));

            FundTransaction tx1 = new FundTransaction(1L, sharedFund, coOwner1, TransactionType.CONTRIBUTION,
                    TransactionEntryType.CREDIT, new BigDecimal("15000000.00"), new BigDecimal("15000000.00"),
                    "TX-REF-1", "Initial deposit", FundTransactionSource.MEMBER_CONTRIBUTION, Instant.now().minusSeconds(7200));

            FundTransaction tx2 = new FundTransaction(2L, sharedFund, coOwner1, TransactionType.CONTRIBUTION,
                    TransactionEntryType.CREDIT, new BigDecimal("5000000.00"), new BigDecimal("20000000.00"),
                    "TX-REF-2", "Replenishment", FundTransactionSource.MEMBER_CONTRIBUTION, Instant.now().minusSeconds(3600));

            FundTransaction tx3 = new FundTransaction(3L, sharedFund, coOwner1, TransactionType.WITHDRAWAL,
                    TransactionEntryType.DEBIT, new BigDecimal("3000000.00"), new BigDecimal("17000000.00"),
                    "TX-REF-3", "Maintenance payout", FundTransactionSource.EXPENSE_PAYOUT, Instant.now());

            when(fundTransactionRepository.findByFundIdOrderByCreatedAtAscIdAsc(sharedFund.getId()))
                    .thenReturn(List.of(tx1, tx2, tx3));

            FundReconciliationResponse reconciliation = sharedFundService.reconcileFundBalance(group.getId(), coOwner1.getId());

            assertNotNull(reconciliation);
            assertEquals(sharedFund.getId(), reconciliation.getFundId());
            assertEquals(group.getId(), reconciliation.getGroupId());
            assertTrue(reconciliation.getIsReconciled(), "Ledger balance matches fund balance exactly");
            assertEquals(0, BigDecimal.ZERO.compareTo(reconciliation.getReconciliationDelta()));
            assertEquals(new BigDecimal("17000000.00"), reconciliation.getCurrentBalance());
            assertEquals(new BigDecimal("17000000.00"), reconciliation.getCalculatedLedgerBalance());
            assertEquals(new BigDecimal("20000000.00"), reconciliation.getTotalCredits());
            assertEquals(new BigDecimal("3000000.00"), reconciliation.getTotalDebits());
            assertEquals(2, reconciliation.getCreditCount());
            assertEquals(1, reconciliation.getDebitCount());
            assertEquals(3, reconciliation.getTransactionCount());
            assertTrue(reconciliation.getSummary().contains("reconciled successfully"));
        }

        @Test
        @DisplayName("Reconciliation: Discrepancy detected when ledger math does not equal current balance")
        void testReconciliation_DiscrepancyDetected() {
            mockActiveCoOwner(coOwner1);
            // Balance is 18M, but ledger sum is 17M -> discrepancy of 1,000,000.00
            sharedFund.setCurrentBalance(new BigDecimal("18000000.00"));
            when(sharedFundRepository.findByGroupId(group.getId())).thenReturn(Optional.of(sharedFund));

            FundTransaction tx1 = new FundTransaction(1L, sharedFund, coOwner1, TransactionType.CONTRIBUTION,
                    TransactionEntryType.CREDIT, new BigDecimal("20000000.00"), new BigDecimal("20000000.00"),
                    "TX-REF-1", "Initial deposit", FundTransactionSource.MEMBER_CONTRIBUTION, Instant.now().minusSeconds(7200));

            FundTransaction tx2 = new FundTransaction(2L, sharedFund, coOwner1, TransactionType.WITHDRAWAL,
                    TransactionEntryType.DEBIT, new BigDecimal("3000000.00"), new BigDecimal("17000000.00"),
                    "TX-REF-2", "Expense", FundTransactionSource.EXPENSE_PAYOUT, Instant.now());

            when(fundTransactionRepository.findByFundIdOrderByCreatedAtAscIdAsc(sharedFund.getId()))
                    .thenReturn(List.of(tx1, tx2));

            FundReconciliationResponse reconciliation = sharedFundService.reconcileFundBalance(group.getId(), coOwner1.getId());

            assertNotNull(reconciliation);
            assertFalse(reconciliation.getIsReconciled(), "Ledger should fail reconciliation due to discrepancy");
            assertEquals(new BigDecimal("1000000.00"), reconciliation.getReconciliationDelta());
            assertEquals(new BigDecimal("18000000.00"), reconciliation.getCurrentBalance());
            assertEquals(new BigDecimal("17000000.00"), reconciliation.getCalculatedLedgerBalance());
            assertTrue(reconciliation.getSummary().contains("discrepancy detected"));
        }

        @Test
        @DisplayName("Lookup: Successfully retrieve transaction by reference code")
        void testGetTransactionByReference_Success() {
            mockActiveCoOwner(coOwner1);
            when(sharedFundRepository.findByGroupId(group.getId())).thenReturn(Optional.of(sharedFund));

            FundTransaction tx = new FundTransaction(10L, sharedFund, coOwner1, TransactionType.CONTRIBUTION,
                    TransactionEntryType.CREDIT, new BigDecimal("5000000.00"), new BigDecimal("15000000.00"),
                    "TX-UNIQ-REF-999", "Unique Ref Test", FundTransactionSource.MEMBER_CONTRIBUTION, Instant.now());

            when(fundTransactionRepository.findByTransactionReference("TX-UNIQ-REF-999"))
                    .thenReturn(Optional.of(tx));

            FundTransactionResponse response = sharedFundService.getTransactionByReference(group.getId(), "TX-UNIQ-REF-999", coOwner1.getId());

            assertNotNull(response);
            assertEquals("TX-UNIQ-REF-999", response.getTransactionReference());
            assertEquals(TransactionEntryType.CREDIT, response.getEntryType());
            assertEquals(new BigDecimal("5000000.00"), response.getAmount());
        }

        @Test
        @DisplayName("Lookup: Fails if transaction belongs to another ownership group fund")
        void testGetTransactionByReference_WrongFund_Rejected() {
            mockActiveCoOwner(coOwner1);
            when(sharedFundRepository.findByGroupId(group.getId())).thenReturn(Optional.of(sharedFund));

            SharedFund otherFund = new SharedFund();
            otherFund.setId(9999L);

            FundTransaction tx = new FundTransaction(10L, otherFund, coOwner1, TransactionType.CONTRIBUTION,
                    TransactionEntryType.CREDIT, new BigDecimal("5000000.00"), new BigDecimal("15000000.00"),
                    "TX-OTHER-FUND-REF", "Other Fund", FundTransactionSource.MEMBER_CONTRIBUTION, Instant.now());

            when(fundTransactionRepository.findByTransactionReference("TX-OTHER-FUND-REF"))
                    .thenReturn(Optional.of(tx));

            BusinessException ex = assertThrows(BusinessException.class, () ->
                    sharedFundService.getTransactionByReference(group.getId(), "TX-OTHER-FUND-REF", coOwner1.getId())
            );

            assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
            assertTrue(ex.getMessage().contains("does not belong"));
        }
    }
}
