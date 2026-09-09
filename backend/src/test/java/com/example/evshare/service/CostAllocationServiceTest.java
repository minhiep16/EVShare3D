package com.example.evshare.service;

import com.example.evshare.entity.*;
import com.example.evshare.entity.enums.AllocationStrategy;
import com.example.evshare.entity.enums.ExpenseCategory;
import com.example.evshare.entity.enums.RoleName;
import com.example.evshare.entity.enums.UsageSessionStatus;
import com.example.evshare.exception.BusinessException;
import com.example.evshare.exception.InvalidOwnershipDistributionException;
import com.example.evshare.repository.ExpenseAllocationRepository;
import com.example.evshare.repository.ExpenseRepository;
import com.example.evshare.repository.OwnershipShareRepository;
import com.example.evshare.repository.UsageSessionRepository;
import com.example.evshare.service.allocation.*;
import com.example.evshare.service.impl.CostAllocationServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Cost Allocation Engine (06-D & 06-E) Unit Tests")
class CostAllocationServiceTest {

    @Mock private ExpenseRepository expenseRepository;
    @Mock private ExpenseAllocationRepository expenseAllocationRepository;
    @Mock private OwnershipShareRepository ownershipShareRepository;
    @Mock private UsageSessionRepository usageSessionRepository;

    private OwnershipBasedAllocationStrategy ownershipStrategy;
    private UsageBasedAllocationStrategy usageStrategy;
    private HybridAllocationStrategy hybridStrategy;
    private CostAllocationService costAllocationService;

    private OwnershipGroup group;
    private Vehicle vehicle;
    private User user1;
    private User user2;
    private User user3;
    private User user4;

    @BeforeEach
    void setUp() {
        ownershipStrategy = new OwnershipBasedAllocationStrategy(ownershipShareRepository);
        usageStrategy = new UsageBasedAllocationStrategy(usageSessionRepository, ownershipShareRepository, ownershipStrategy);
        hybridStrategy = new HybridAllocationStrategy(ownershipStrategy, usageStrategy, ownershipShareRepository, usageSessionRepository);

        List<CostAllocationStrategy> strategies = List.of(ownershipStrategy, usageStrategy, hybridStrategy);
        costAllocationService = new CostAllocationServiceImpl(expenseRepository, expenseAllocationRepository, strategies);

        Role coOwnerRole = new Role(3L, RoleName.ROLE_CO_OWNER);

        user1 = new User();
        user1.setId(101L);
        user1.setFullName("Nguyen Van A");
        user1.setEmail("a@evshare.io");
        user1.setIsActive(true);
        user1.setRoles(Set.of(coOwnerRole));

        user2 = new User();
        user2.setId(102L);
        user2.setFullName("Tran Thi B");
        user2.setEmail("b@evshare.io");
        user2.setIsActive(true);
        user2.setRoles(Set.of(coOwnerRole));

        user3 = new User();
        user3.setId(103L);
        user3.setFullName("Le Van C");
        user3.setEmail("c@evshare.io");
        user3.setIsActive(true);
        user3.setRoles(Set.of(coOwnerRole));

        user4 = new User();
        user4.setId(104L);
        user4.setFullName("Pham Thi D");
        user4.setEmail("d@evshare.io");
        user4.setIsActive(true);
        user4.setRoles(Set.of(coOwnerRole));

        vehicle = new Vehicle();
        vehicle.setId(10L);
        vehicle.setModelName("VinFast VF9");
        vehicle.setLicensePlate("51K-999.99");
        vehicle.setVin("VINVF90001");

        group = new OwnershipGroup();
        group.setId(1L);
        group.setGroupName("VinFast VF9 Syndicate");
        group.setVehicle(vehicle);
        group.setIsActive(true);
    }

    private Expense createExpense(BigDecimal amount, AllocationStrategy strategy) {
        Expense expense = new Expense();
        expense.setId(500L);
        expense.setGroup(group);
        expense.setVehicle(vehicle);
        expense.setTitle("Monthly Service & Maintenance");
        expense.setCategory(ExpenseCategory.MAINTENANCE);
        expense.setTotalAmount(amount);
        expense.setCurrency("VND");
        expense.setAllocationStrategy(strategy);
        expense.setIncurredDate(LocalDate.now());
        expense.setLoggedByUser(user1);
        return expense;
    }

    private OwnershipShare createShare(Long id, User user, BigDecimal percentage, boolean isActive) {
        OwnershipShare share = new OwnershipShare();
        share.setId(id);
        share.setGroup(group);
        share.setUser(user);
        share.setPercentage(percentage);
        share.setShareCertificateNumber("CERT-" + id);
        share.setIsActive(isActive);
        return share;
    }

    // =========================================================================
    // 06-D — COST ALLOCATION ENGINE CORE REQUIREMENTS
    // =========================================================================
    @Nested
    @DisplayName("06-D: Cost Allocation Engine Abstraction & Invariants")
    class CostAllocationEngineTests {

        @Test
        @DisplayName("Should resolve strategies for all supported modes: OWNERSHIP_BASED, USAGE_BASED, HYBRID")
        void testStrategyLookupAllSupportedModes() {
            assertNotNull(costAllocationService.getStrategy(AllocationStrategy.OWNERSHIP_BASED));
            assertEquals(AllocationStrategy.OWNERSHIP_BASED, costAllocationService.getStrategy(AllocationStrategy.OWNERSHIP_BASED).getStrategyMode());

            assertNotNull(costAllocationService.getStrategy(AllocationStrategy.USAGE_BASED));
            assertEquals(AllocationStrategy.USAGE_BASED, costAllocationService.getStrategy(AllocationStrategy.USAGE_BASED).getStrategyMode());

            assertNotNull(costAllocationService.getStrategy(AllocationStrategy.HYBRID));
            assertEquals(AllocationStrategy.HYBRID, costAllocationService.getStrategy(AllocationStrategy.HYBRID).getStrategyMode());
        }

        @Test
        @DisplayName("Should document rounding strategy explicitly in all allocation results")
        void testRoundingStrategyDocumented() {
            List<OwnershipShare> shares = List.of(
                    createShare(1L, user1, new BigDecimal("50.00"), true),
                    createShare(2L, user2, new BigDecimal("50.00"), true)
            );
            when(ownershipShareRepository.findByGroupIdAndIsActiveTrue(group.getId())).thenReturn(shares);

            Expense expense = createExpense(new BigDecimal("100000.00"), AllocationStrategy.OWNERSHIP_BASED);
            AllocationResult result = costAllocationService.calculateAllocation(expense);

            assertNotNull(result.getRoundingStrategyDescription());
            assertTrue(result.getRoundingStrategyDescription().contains("HALF_EVEN"),
                    "Rounding strategy description must explicitly specify Banker's Rounding (HALF_EVEN)");
            assertTrue(result.getRoundingStrategyDescription().toLowerCase().contains("residual"),
                    "Rounding strategy description must document residual penny absorption");
        }

        @Test
        @DisplayName("Should provide transparent and itemized mathematical explanation for each member share")
        void testExplainabilityAcrossStrategies() {
            List<OwnershipShare> shares = List.of(
                    createShare(1L, user1, new BigDecimal("60.00"), true),
                    createShare(2L, user2, new BigDecimal("40.00"), true)
            );
            when(ownershipShareRepository.findByGroupIdAndIsActiveTrue(group.getId())).thenReturn(shares);

            Expense expense = createExpense(new BigDecimal("200000.00"), AllocationStrategy.OWNERSHIP_BASED);
            AllocationResult result = costAllocationService.calculateAllocation(expense);

            assertEquals(2, result.getShares().size());
            for (AllocatedMemberShare share : result.getShares()) {
                assertNotNull(share.getExplanation());
                assertFalse(share.getExplanation().isBlank());
                assertTrue(share.getExplanation().contains("Ownership-based allocation"));
                assertTrue(share.getExplanation().contains(expense.getCurrency()));
            }
        }

        @Test
        @DisplayName("Deterministic execution: 1,000 runs produce 100% identical outputs with no random variation")
        void testNoHiddenRandomBehavior_Deterministic1000Runs() {
            List<OwnershipShare> shares = List.of(
                    createShare(1L, user1, new BigDecimal("33.33"), true),
                    createShare(2L, user2, new BigDecimal("33.33"), true),
                    createShare(3L, user3, new BigDecimal("33.34"), true)
            );
            when(ownershipShareRepository.findByGroupIdAndIsActiveTrue(group.getId())).thenReturn(shares);

            Expense expense = createExpense(new BigDecimal("100.00"), AllocationStrategy.OWNERSHIP_BASED);

            AllocationResult baseline = costAllocationService.calculateAllocation(expense);
            BigDecimal u1Expected = baseline.getShareForUser(user1.getId()).orElseThrow().getAllocatedAmount();
            BigDecimal u2Expected = baseline.getShareForUser(user2.getId()).orElseThrow().getAllocatedAmount();
            BigDecimal u3Expected = baseline.getShareForUser(user3.getId()).orElseThrow().getAllocatedAmount();

            for (int i = 0; i < 1000; i++) {
                AllocationResult current = costAllocationService.calculateAllocation(expense);
                assertEquals(u1Expected, current.getShareForUser(user1.getId()).orElseThrow().getAllocatedAmount());
                assertEquals(u2Expected, current.getShareForUser(user2.getId()).orElseThrow().getAllocatedAmount());
                assertEquals(u3Expected, current.getShareForUser(user3.getId()).orElseThrow().getAllocatedAmount());
                assertEquals(baseline.getTotalAllocated(), current.getTotalAllocated());
                assertEquals(baseline.getRoundingAdjustment(), current.getRoundingAdjustment());
            }
        }

        @Test
        @DisplayName("Reconciliation invariant: sum of member shares strictly equals total expense amount")
        void testExactTotalAllocationReconciliation() {
            List<OwnershipShare> shares = List.of(
                    createShare(1L, user1, new BigDecimal("70.00"), true),
                    createShare(2L, user2, new BigDecimal("20.00"), true),
                    createShare(3L, user3, new BigDecimal("10.00"), true)
            );
            when(ownershipShareRepository.findByGroupIdAndIsActiveTrue(group.getId())).thenReturn(shares);

            BigDecimal[] testAmounts = {
                    new BigDecimal("100000.00"),
                    new BigDecimal("333333.33"),
                    new BigDecimal("777777.77"),
                    new BigDecimal("1.00"),
                    new BigDecimal("0.05"),
                    new BigDecimal("12345678.91")
            };

            for (BigDecimal amount : testAmounts) {
                Expense expense = createExpense(amount, AllocationStrategy.OWNERSHIP_BASED);
                AllocationResult result = costAllocationService.calculateAllocation(expense);

                assertTrue(result.isReconciled());
                BigDecimal sum = result.getShares().stream()
                        .map(AllocatedMemberShare::getAllocatedAmount)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);

                assertEquals(0, amount.compareTo(sum),
                        "Sum of allocated shares (" + sum + ") must strictly equal expense total (" + amount + ")");
            }
        }

        @Test
        @DisplayName("Should persist and link ExpenseAllocation records when requested")
        void testApplyAndPersistAllocations() {
            List<OwnershipShare> shares = List.of(
                    createShare(1L, user1, new BigDecimal("60.00"), true),
                    createShare(2L, user2, new BigDecimal("40.00"), true)
            );
            when(ownershipShareRepository.findByGroupIdAndIsActiveTrue(group.getId())).thenReturn(shares);

            Expense expense = createExpense(new BigDecimal("500000.00"), AllocationStrategy.OWNERSHIP_BASED);
            when(expenseAllocationRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));

            List<ExpenseAllocation> persisted = costAllocationService.applyAndPersistAllocations(expense);

            assertEquals(2, persisted.size());
            BigDecimal totalPersisted = persisted.stream()
                    .map(ExpenseAllocation::getAllocatedAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            assertEquals(0, new BigDecimal("500000.00").compareTo(totalPersisted));
            verify(expenseAllocationRepository).saveAll(any());
        }
    }

    // =========================================================================
    // 06-E — OWNERSHIP-BASED ALLOCATION REQUIREMENTS
    // =========================================================================
    @Nested
    @DisplayName("06-E: Ownership-Based Allocation & Distributions")
    class OwnershipBasedAllocationTests {

        @Test
        @DisplayName("Distribution 1: 50% / 50% Two-Owner Syndicate")
        void testDistribution_50_50() {
            List<OwnershipShare> shares = List.of(
                    createShare(1L, user1, new BigDecimal("50.00"), true),
                    createShare(2L, user2, new BigDecimal("50.00"), true)
            );
            when(ownershipShareRepository.findByGroupIdAndIsActiveTrue(group.getId())).thenReturn(shares);

            // Even amount
            Expense evenExpense = createExpense(new BigDecimal("100000.00"), AllocationStrategy.OWNERSHIP_BASED);
            AllocationResult evenResult = costAllocationService.calculateAllocation(evenExpense);

            assertEquals(new BigDecimal("50000.00"), evenResult.getShareForUser(user1.getId()).orElseThrow().getAllocatedAmount());
            assertEquals(new BigDecimal("50000.00"), evenResult.getShareForUser(user2.getId()).orElseThrow().getAllocatedAmount());
            assertEquals(new BigDecimal("100000.00"), evenResult.getTotalAllocated());

            // Odd penny amount (100,000.01 VND) - tie-breaker lowest userId (user1: 101 < user2: 102)
            Expense oddExpense = createExpense(new BigDecimal("100000.01"), AllocationStrategy.OWNERSHIP_BASED);
            AllocationResult oddResult = costAllocationService.calculateAllocation(oddExpense);

            assertEquals(new BigDecimal("50000.01"), oddResult.getShareForUser(user1.getId()).orElseThrow().getAllocatedAmount());
            assertEquals(new BigDecimal("50000.00"), oddResult.getShareForUser(user2.getId()).orElseThrow().getAllocatedAmount());
            assertEquals(new BigDecimal("100000.01"), oddResult.getTotalAllocated());
        }

        @Test
        @DisplayName("Distribution 2: 60% / 40% Two-Owner Syndicate")
        void testDistribution_60_40() {
            List<OwnershipShare> shares = List.of(
                    createShare(1L, user1, new BigDecimal("60.00"), true),
                    createShare(2L, user2, new BigDecimal("40.00"), true)
            );
            when(ownershipShareRepository.findByGroupIdAndIsActiveTrue(group.getId())).thenReturn(shares);

            Expense expense = createExpense(new BigDecimal("150000.00"), AllocationStrategy.OWNERSHIP_BASED);
            AllocationResult result = costAllocationService.calculateAllocation(expense);

            assertEquals(new BigDecimal("90000.00"), result.getShareForUser(user1.getId()).orElseThrow().getAllocatedAmount());
            assertEquals(new BigDecimal("60000.00"), result.getShareForUser(user2.getId()).orElseThrow().getAllocatedAmount());
            assertEquals(new BigDecimal("150000.00"), result.getTotalAllocated());
        }

        @Test
        @DisplayName("Distribution 3: 33.33% / 33.33% / 33.34% Three-Owner Syndicate with Penny Absorption")
        void testDistribution_3333_3333_3334() {
            List<OwnershipShare> shares = List.of(
                    createShare(1L, user1, new BigDecimal("33.33"), true),
                    createShare(2L, user2, new BigDecimal("33.33"), true),
                    createShare(3L, user3, new BigDecimal("33.34"), true)
            );
            when(ownershipShareRepository.findByGroupIdAndIsActiveTrue(group.getId())).thenReturn(shares);

            // On 10.00 VND:
            // user1: 10 * 33.33% = 3.333 -> 3.33
            // user2: 10 * 33.33% = 3.333 -> 3.33
            // user3: 10 * 33.34% = 3.334 -> 3.33, residual +0.01 absorbed by user3 (highest percentage 33.34%) -> 3.34
            Expense expense = createExpense(new BigDecimal("10.00"), AllocationStrategy.OWNERSHIP_BASED);
            AllocationResult result = costAllocationService.calculateAllocation(expense);

            assertEquals(new BigDecimal("3.33"), result.getShareForUser(user1.getId()).orElseThrow().getAllocatedAmount());
            assertEquals(new BigDecimal("3.33"), result.getShareForUser(user2.getId()).orElseThrow().getAllocatedAmount());
            assertEquals(new BigDecimal("3.34"), result.getShareForUser(user3.getId()).orElseThrow().getAllocatedAmount());
            assertEquals(new BigDecimal("10.00"), result.getTotalAllocated());

            AllocatedMemberShare u3Share = result.getShareForUser(user3.getId()).orElseThrow();
            assertTrue(u3Share.getExplanation().contains("residual"));
        }

        @Test
        @DisplayName("Distribution 4: 70% / 20% / 10% Asymmetric Three-Owner Syndicate")
        void testDistribution_70_20_10() {
            List<OwnershipShare> shares = List.of(
                    createShare(1L, user1, new BigDecimal("70.00"), true),
                    createShare(2L, user2, new BigDecimal("20.00"), true),
                    createShare(3L, user3, new BigDecimal("10.00"), true)
            );
            when(ownershipShareRepository.findByGroupIdAndIsActiveTrue(group.getId())).thenReturn(shares);

            Expense expense = createExpense(new BigDecimal("2500000.00"), AllocationStrategy.OWNERSHIP_BASED);
            AllocationResult result = costAllocationService.calculateAllocation(expense);

            assertEquals(new BigDecimal("1750000.00"), result.getShareForUser(user1.getId()).orElseThrow().getAllocatedAmount());
            assertEquals(new BigDecimal("500000.00"), result.getShareForUser(user2.getId()).orElseThrow().getAllocatedAmount());
            assertEquals(new BigDecimal("250000.00"), result.getShareForUser(user3.getId()).orElseThrow().getAllocatedAmount());
            assertEquals(new BigDecimal("2500000.00"), result.getTotalAllocated());
        }

        @Test
        @DisplayName("Distribution 5: 25% / 25% / 25% / 25% Four Equal Owners with Deterministic Tie-Break")
        void testDistribution_25_25_25_25_DeterministicTieBreak() {
            List<OwnershipShare> shares = List.of(
                    createShare(1L, user1, new BigDecimal("25.00"), true), // id 101
                    createShare(2L, user2, new BigDecimal("25.00"), true), // id 102
                    createShare(3L, user3, new BigDecimal("25.00"), true), // id 103
                    createShare(4L, user4, new BigDecimal("25.00"), true)  // id 104
            );
            when(ownershipShareRepository.findByGroupIdAndIsActiveTrue(group.getId())).thenReturn(shares);

            // On 100.01 VND:
            // Each pro-rata = 25.0025 -> 25.00 (sum = 100.00, residual +0.01)
            // Equal percentages -> tie broken by lowest userId (user1 id 101)
            Expense expense = createExpense(new BigDecimal("100.01"), AllocationStrategy.OWNERSHIP_BASED);
            AllocationResult result = costAllocationService.calculateAllocation(expense);

            assertEquals(new BigDecimal("25.01"), result.getShareForUser(user1.getId()).orElseThrow().getAllocatedAmount());
            assertEquals(new BigDecimal("25.00"), result.getShareForUser(user2.getId()).orElseThrow().getAllocatedAmount());
            assertEquals(new BigDecimal("25.00"), result.getShareForUser(user3.getId()).orElseThrow().getAllocatedAmount());
            assertEquals(new BigDecimal("25.00"), result.getShareForUser(user4.getId()).orElseThrow().getAllocatedAmount());
            assertEquals(new BigDecimal("100.01"), result.getTotalAllocated());
        }

        @Test
        @DisplayName("Active Ownership Only: Inactive shares are strictly excluded from allocation")
        void testActiveOwnershipOnly_ExcludesInactiveShares() {
            OwnershipShare active1 = createShare(1L, user1, new BigDecimal("60.00"), true);
            OwnershipShare active2 = createShare(2L, user2, new BigDecimal("40.00"), true);
            // Repository returns only active shares per method contract findByGroupIdAndIsActiveTrue
            when(ownershipShareRepository.findByGroupIdAndIsActiveTrue(group.getId()))
                    .thenReturn(List.of(active1, active2));

            Expense expense = createExpense(new BigDecimal("100000.00"), AllocationStrategy.OWNERSHIP_BASED);
            AllocationResult result = costAllocationService.calculateAllocation(expense);

            assertEquals(2, result.getShares().size());
            assertTrue(result.getShareForUser(user1.getId()).isPresent());
            assertTrue(result.getShareForUser(user2.getId()).isPresent());
            assertTrue(result.getShareForUser(user3.getId()).isEmpty(), "Inactive member user3 must NOT receive any allocation");
            assertEquals(new BigDecimal("100000.00"), result.getTotalAllocated());
        }

        @Test
        @DisplayName("Invariant: Total active ownership != 100.00% throws InvalidOwnershipDistributionException")
        void testOwnershipTotalNot100_ThrowsException() {
            // Under-allocated (90.00%)
            List<OwnershipShare> underShares = List.of(
                    createShare(1L, user1, new BigDecimal("50.00"), true),
                    createShare(2L, user2, new BigDecimal("40.00"), true)
            );
            when(ownershipShareRepository.findByGroupIdAndIsActiveTrue(group.getId())).thenReturn(underShares);

            Expense expense = createExpense(new BigDecimal("100000.00"), AllocationStrategy.OWNERSHIP_BASED);
            assertThrows(InvalidOwnershipDistributionException.class,
                    () -> costAllocationService.calculateAllocation(expense),
                    "Should fail when active shares sum to 90.00%");

            // Over-allocated (110.00%)
            List<OwnershipShare> overShares = List.of(
                    createShare(1L, user1, new BigDecimal("60.00"), true),
                    createShare(2L, user2, new BigDecimal("50.00"), true)
            );
            when(ownershipShareRepository.findByGroupIdAndIsActiveTrue(group.getId())).thenReturn(overShares);

            assertThrows(InvalidOwnershipDistributionException.class,
                    () -> costAllocationService.calculateAllocation(expense),
                    "Should fail when active shares sum to 110.00%");
        }

        @Test
        @DisplayName("Edge Case: Odd Amounts and Micro-Pennies reconcile perfectly")
        void testOddAmountsReconciliation() {
            List<OwnershipShare> shares = List.of(
                    createShare(1L, user1, new BigDecimal("60.00"), true),
                    createShare(2L, user2, new BigDecimal("40.00"), true)
            );
            when(ownershipShareRepository.findByGroupIdAndIsActiveTrue(group.getId())).thenReturn(shares);

            Expense expense = createExpense(new BigDecimal("777777.77"), AllocationStrategy.OWNERSHIP_BASED);
            AllocationResult result = costAllocationService.calculateAllocation(expense);

            BigDecimal u1 = result.getShareForUser(user1.getId()).orElseThrow().getAllocatedAmount();
            BigDecimal u2 = result.getShareForUser(user2.getId()).orElseThrow().getAllocatedAmount();

            assertEquals(new BigDecimal("777777.77"), u1.add(u2));
            assertTrue(result.isReconciled());
        }

        @Test
        @DisplayName("Distribution 6: Single Owner 100.00% Equity Syndicate")
        void testDistribution_SoleOwner_100Percent() {
            List<OwnershipShare> shares = List.of(
                    createShare(1L, user1, new BigDecimal("100.00"), true)
            );
            when(ownershipShareRepository.findByGroupIdAndIsActiveTrue(group.getId())).thenReturn(shares);

            Expense expense = createExpense(new BigDecimal("450000.00"), AllocationStrategy.OWNERSHIP_BASED);
            AllocationResult result = costAllocationService.calculateAllocation(expense);

            assertEquals(1, result.getShares().size());
            assertEquals(new BigDecimal("450000.00"), result.getShareForUser(user1.getId()).orElseThrow().getAllocatedAmount());
            assertEquals(new BigDecimal("450000.00"), result.getTotalAllocated());
            assertTrue(result.isReconciled());
        }

        @Test
        @DisplayName("Distribution 7: Five Equal Owners (20% each) with Odd Penny Reconciled")
        void testDistribution_FiveEqualOwners_20Each_OddPenny() {
            User user5 = new User();
            user5.setId(105L);
            user5.setFullName("Hoang E");
            user5.setEmail("e@evshare.io");
            user5.setIsActive(true);

            List<OwnershipShare> shares = List.of(
                    createShare(1L, user1, new BigDecimal("20.00"), true), // id 101
                    createShare(2L, user2, new BigDecimal("20.00"), true), // id 102
                    createShare(3L, user3, new BigDecimal("20.00"), true), // id 103
                    createShare(4L, user4, new BigDecimal("20.00"), true), // id 104
                    createShare(5L, user5, new BigDecimal("20.00"), true)  // id 105
            );
            when(ownershipShareRepository.findByGroupIdAndIsActiveTrue(group.getId())).thenReturn(shares);

            // On 100.01 VND: 100.01 * 0.20 = 20.002 -> 20.00 each (sum 100.00, residual +0.01)
            // Equal percentages -> tie broken by lowest userId (user1: 101)
            Expense expense = createExpense(new BigDecimal("100.01"), AllocationStrategy.OWNERSHIP_BASED);
            AllocationResult result = costAllocationService.calculateAllocation(expense);

            assertEquals(5, result.getShares().size());
            assertEquals(new BigDecimal("20.01"), result.getShareForUser(user1.getId()).orElseThrow().getAllocatedAmount());
            assertEquals(new BigDecimal("20.00"), result.getShareForUser(user2.getId()).orElseThrow().getAllocatedAmount());
            assertEquals(new BigDecimal("20.00"), result.getShareForUser(user3.getId()).orElseThrow().getAllocatedAmount());
            assertEquals(new BigDecimal("20.00"), result.getShareForUser(user4.getId()).orElseThrow().getAllocatedAmount());
            assertEquals(new BigDecimal("20.00"), result.getShareForUser(user5.getId()).orElseThrow().getAllocatedAmount());
            assertEquals(new BigDecimal("100.01"), result.getTotalAllocated());
            assertTrue(result.isReconciled());
        }

        @Test
        @DisplayName("Distribution 8: Seven-Member Syndicate (6 x 14.28% + 1 x 14.32% = 100.00%)")
        void testDistribution_SevenMemberSyndicate() {
            List<OwnershipShare> shares = new ArrayList<>();
            for (int i = 1; i <= 6; i++) {
                User u = new User();
                u.setId((long) (200 + i));
                u.setFullName("Member " + i);
                shares.add(createShare((long) i, u, new BigDecimal("14.28"), true));
            }
            User u7 = new User();
            u7.setId(207L);
            u7.setFullName("Member 7");
            shares.add(createShare(7L, u7, new BigDecimal("14.32"), true));

            when(ownershipShareRepository.findByGroupIdAndIsActiveTrue(group.getId())).thenReturn(shares);

            Expense expense = createExpense(new BigDecimal("10000000.00"), AllocationStrategy.OWNERSHIP_BASED);
            AllocationResult result = costAllocationService.calculateAllocation(expense);

            assertEquals(7, result.getShares().size());
            assertTrue(result.isReconciled());
            BigDecimal totalAllocated = result.getTotalAllocated();
            assertEquals(new BigDecimal("10000000.00"), totalAllocated);
        }

        @Test
        @DisplayName("Distribution 9: Micro Expense 0.01 VND across 3 Owners")
        void testDistribution_MicroExpense_001VND() {
            List<OwnershipShare> shares = List.of(
                    createShare(1L, user1, new BigDecimal("33.33"), true),
                    createShare(2L, user2, new BigDecimal("33.33"), true),
                    createShare(3L, user3, new BigDecimal("33.34"), true)
            );
            when(ownershipShareRepository.findByGroupIdAndIsActiveTrue(group.getId())).thenReturn(shares);

            Expense expense = createExpense(new BigDecimal("0.01"), AllocationStrategy.OWNERSHIP_BASED);
            AllocationResult result = costAllocationService.calculateAllocation(expense);

            assertEquals(new BigDecimal("0.00"), result.getShareForUser(user1.getId()).orElseThrow().getAllocatedAmount());
            assertEquals(new BigDecimal("0.00"), result.getShareForUser(user2.getId()).orElseThrow().getAllocatedAmount());
            assertEquals(new BigDecimal("0.01"), result.getShareForUser(user3.getId()).orElseThrow().getAllocatedAmount());
            assertEquals(new BigDecimal("0.01"), result.getTotalAllocated());
            assertTrue(result.isReconciled());
        }
    }

    // =========================================================================
    // 06-D — USAGE-BASED & HYBRID STRATEGY TESTS
    // =========================================================================
    @Nested
    @DisplayName("06-D: Usage-Based & Hybrid Strategies")
    class UsageAndHybridStrategyTests {

        @Test
        @DisplayName("Usage-Based: Pro-rata allocation based on odometer distance logged")
        void testUsageBasedAllocation_WithCompletedSessions() {
            List<OwnershipShare> shares = List.of(
                    createShare(1L, user1, new BigDecimal("50.00"), true),
                    createShare(2L, user2, new BigDecimal("50.00"), true)
            );
            when(ownershipShareRepository.findByGroupIdAndIsActiveTrue(group.getId())).thenReturn(shares);

            Booking booking1 = new Booking();
            booking1.setId(1L);
            booking1.setVehicle(vehicle);
            booking1.setUser(user1);

            Booking booking2 = new Booking();
            booking2.setId(2L);
            booking2.setVehicle(vehicle);
            booking2.setUser(user2);

            // User 1 drove 300 km (1000 -> 1300)
            UsageSession session1 = new UsageSession();
            session1.setId(10L);
            session1.setBooking(booking1);
            session1.setStartOdometer(new BigDecimal("1000.0"));
            session1.setEndOdometer(new BigDecimal("1300.0"));
            session1.setStatus(UsageSessionStatus.COMPLETED);

            // User 2 drove 100 km (1300 -> 1400)
            UsageSession session2 = new UsageSession();
            session2.setId(11L);
            session2.setBooking(booking2);
            session2.setStartOdometer(new BigDecimal("1300.0"));
            session2.setEndOdometer(new BigDecimal("1400.0"));
            session2.setStatus(UsageSessionStatus.COMPLETED);

            when(usageSessionRepository.findByBookingVehicleIdOrderByCheckInTimeDesc(vehicle.getId()))
                    .thenReturn(List.of(session1, session2));

            // Total 400 km: User 1 is 75.00%, User 2 is 25.00%
            Expense expense = createExpense(new BigDecimal("400000.00"), AllocationStrategy.USAGE_BASED);
            AllocationResult result = costAllocationService.calculateAllocation(expense);

            assertEquals(new BigDecimal("300000.00"), result.getShareForUser(user1.getId()).orElseThrow().getAllocatedAmount());
            assertEquals(new BigDecimal("100000.00"), result.getShareForUser(user2.getId()).orElseThrow().getAllocatedAmount());
            assertEquals(new BigDecimal("400000.00"), result.getTotalAllocated());
            assertTrue(result.isReconciled());
        }

        @Test
        @DisplayName("Usage-Based Fallback: Idle vehicle with 0 km falls back gracefully to ownership percentages")
        void testUsageBasedAllocation_ZeroKmFallbackToOwnership() {
            List<OwnershipShare> shares = List.of(
                    createShare(1L, user1, new BigDecimal("70.00"), true),
                    createShare(2L, user2, new BigDecimal("30.00"), true)
            );
            when(ownershipShareRepository.findByGroupIdAndIsActiveTrue(group.getId())).thenReturn(shares);
            when(usageSessionRepository.findByBookingVehicleIdOrderByCheckInTimeDesc(vehicle.getId()))
                    .thenReturn(Collections.emptyList());

            Expense expense = createExpense(new BigDecimal("100000.00"), AllocationStrategy.USAGE_BASED);
            AllocationResult result = costAllocationService.calculateAllocation(expense);

            assertEquals(new BigDecimal("70000.00"), result.getShareForUser(user1.getId()).orElseThrow().getAllocatedAmount());
            assertEquals(new BigDecimal("30000.00"), result.getShareForUser(user2.getId()).orElseThrow().getAllocatedAmount());
            assertEquals(new BigDecimal("100000.00"), result.getTotalAllocated());
            assertTrue(result.getSummary().contains("fallback"));
        }

        @Test
        @DisplayName("Usage-Based: Pro-rata allocation based on operating duration (HoursUsed) per BR-FIN-02")
        void testUsageBasedAllocation_DurationMetric() {
            List<OwnershipShare> shares = List.of(
                    createShare(1L, user1, new BigDecimal("50.00"), true),
                    createShare(2L, user2, new BigDecimal("50.00"), true)
            );
            when(ownershipShareRepository.findByGroupIdAndIsActiveTrue(group.getId())).thenReturn(shares);

            Booking booking1 = new Booking();
            booking1.setId(1L);
            booking1.setVehicle(vehicle);
            booking1.setUser(user1);

            Booking booking2 = new Booking();
            booking2.setId(2L);
            booking2.setVehicle(vehicle);
            booking2.setUser(user2);

            Instant now = Instant.now();
            // User 1 used vehicle for 2.0 hours (120 minutes)
            UsageSession session1 = new UsageSession();
            session1.setId(10L);
            session1.setBooking(booking1);
            session1.setCheckInTime(now.minusSeconds(7200));
            session1.setCheckOutTime(now);
            session1.setStatus(UsageSessionStatus.COMPLETED);

            // User 2 used vehicle for 6.0 hours (360 minutes)
            UsageSession session2 = new UsageSession();
            session2.setId(11L);
            session2.setBooking(booking2);
            session2.setCheckInTime(now.minusSeconds(21600));
            session2.setCheckOutTime(now);
            session2.setStatus(UsageSessionStatus.COMPLETED);

            when(usageSessionRepository.findByBookingVehicleIdOrderByCheckInTimeDesc(vehicle.getId()))
                    .thenReturn(List.of(session1, session2));

            // Expense of 800,000.00 VND: Total 8 hours -> User 1 is 25.00% (200,000), User 2 is 75.00% (600,000)
            Expense expense = createExpense(new BigDecimal("800000.00"), AllocationStrategy.USAGE_BASED);
            AllocationResult result = usageStrategy.allocate(expense, UsageMetric.DURATION);

            assertEquals(new BigDecimal("200000.00"), result.getShareForUser(user1.getId()).orElseThrow().getAllocatedAmount());
            assertEquals(new BigDecimal("600000.00"), result.getShareForUser(user2.getId()).orElseThrow().getAllocatedAmount());
            assertEquals(new BigDecimal("800000.00"), result.getTotalAllocated());
            assertTrue(result.isReconciled());
            assertTrue(result.getSummary().contains("duration"));
        }

        @Test
        @DisplayName("Usage-Based: Zero distance recorded automatically degrades to duration metric")
        void testUsageBasedAllocation_ZeroDistanceDegradesToDuration() {
            List<OwnershipShare> shares = List.of(
                    createShare(1L, user1, new BigDecimal("60.00"), true),
                    createShare(2L, user2, new BigDecimal("40.00"), true)
            );
            when(ownershipShareRepository.findByGroupIdAndIsActiveTrue(group.getId())).thenReturn(shares);

            Booking booking1 = new Booking();
            booking1.setId(1L);
            booking1.setVehicle(vehicle);
            booking1.setUser(user1);

            Booking booking2 = new Booking();
            booking2.setId(2L);
            booking2.setVehicle(vehicle);
            booking2.setUser(user2);

            Instant now = Instant.now();
            // Start odo == End odo (0.0 km logged), but 3 hours used for stationary power/testing
            UsageSession session1 = new UsageSession();
            session1.setId(10L);
            session1.setBooking(booking1);
            session1.setStartOdometer(new BigDecimal("5000.0"));
            session1.setEndOdometer(new BigDecimal("5000.0"));
            session1.setCheckInTime(now.minusSeconds(10800)); // 3 hours
            session1.setCheckOutTime(now);
            session1.setStatus(UsageSessionStatus.COMPLETED);

            // 1 hour used
            UsageSession session2 = new UsageSession();
            session2.setId(11L);
            session2.setBooking(booking2);
            session2.setStartOdometer(new BigDecimal("5000.0"));
            session2.setEndOdometer(new BigDecimal("5000.0"));
            session2.setCheckInTime(now.minusSeconds(3600)); // 1 hour
            session2.setCheckOutTime(now);
            session2.setStatus(UsageSessionStatus.COMPLETED);

            when(usageSessionRepository.findByBookingVehicleIdOrderByCheckInTimeDesc(vehicle.getId()))
                    .thenReturn(List.of(session1, session2));

            // Total 4.0 hours: User 1 is 75%, User 2 is 25%
            Expense expense = createExpense(new BigDecimal("400000.00"), AllocationStrategy.USAGE_BASED);
            AllocationResult result = costAllocationService.calculateAllocation(expense);

            assertEquals(new BigDecimal("300000.00"), result.getShareForUser(user1.getId()).orElseThrow().getAllocatedAmount());
            assertEquals(new BigDecimal("100000.00"), result.getShareForUser(user2.getId()).orElseThrow().getAllocatedAmount());
            assertEquals(new BigDecimal("400000.00"), result.getTotalAllocated());
            assertTrue(result.isReconciled());
            assertTrue(result.getSummary().contains("duration"));
        }

        @Test
        @DisplayName("Usage-Based: Asymmetric distances with deterministic residual penny absorption")
        void testUsageBasedAllocation_OddDistanceResidualPennyAbsorption() {
            List<OwnershipShare> shares = List.of(
                    createShare(1L, user1, new BigDecimal("33.33"), true),
                    createShare(2L, user2, new BigDecimal("33.33"), true),
                    createShare(3L, user3, new BigDecimal("33.34"), true)
            );
            when(ownershipShareRepository.findByGroupIdAndIsActiveTrue(group.getId())).thenReturn(shares);

            Booking booking1 = new Booking();
            booking1.setId(1L);
            booking1.setVehicle(vehicle);
            booking1.setUser(user1);

            Booking booking2 = new Booking();
            booking2.setId(2L);
            booking2.setVehicle(vehicle);
            booking2.setUser(user2);

            Booking booking3 = new Booking();
            booking3.setId(3L);
            booking3.setVehicle(vehicle);
            booking3.setUser(user3);

            // User 1: 33.3 km, User 2: 33.3 km, User 3: 33.4 km -> Total 100.0 km
            UsageSession session1 = new UsageSession();
            session1.setId(10L);
            session1.setBooking(booking1);
            session1.setStartOdometer(BigDecimal.ZERO);
            session1.setEndOdometer(new BigDecimal("33.3"));
            session1.setStatus(UsageSessionStatus.COMPLETED);

            UsageSession session2 = new UsageSession();
            session2.setId(11L);
            session2.setBooking(booking2);
            session2.setStartOdometer(BigDecimal.ZERO);
            session2.setEndOdometer(new BigDecimal("33.3"));
            session2.setStatus(UsageSessionStatus.COMPLETED);

            UsageSession session3 = new UsageSession();
            session3.setId(12L);
            session3.setBooking(booking3);
            session3.setStartOdometer(BigDecimal.ZERO);
            session3.setEndOdometer(new BigDecimal("33.4"));
            session3.setStatus(UsageSessionStatus.COMPLETED);

            when(usageSessionRepository.findByBookingVehicleIdOrderByCheckInTimeDesc(vehicle.getId()))
                    .thenReturn(List.of(session1, session2, session3));

            // On 10.00 VND:
            // User 1 (33.3%): 3.33
            // User 2 (33.3%): 3.33
            // User 3 (33.4%): 3.34 (absorbs residual penny as highest utilization member)
            Expense expense = createExpense(new BigDecimal("10.00"), AllocationStrategy.USAGE_BASED);
            AllocationResult result = costAllocationService.calculateAllocation(expense);

            assertEquals(new BigDecimal("3.33"), result.getShareForUser(user1.getId()).orElseThrow().getAllocatedAmount());
            assertEquals(new BigDecimal("3.33"), result.getShareForUser(user2.getId()).orElseThrow().getAllocatedAmount());
            assertEquals(new BigDecimal("3.34"), result.getShareForUser(user3.getId()).orElseThrow().getAllocatedAmount());
            assertEquals(new BigDecimal("10.00"), result.getTotalAllocated());
            assertTrue(result.isReconciled());
        }
    }

    // =========================================================================
    // 06-G — HYBRID ALLOCATION STRATEGY TESTS (BR-FIN-02)
    // =========================================================================
    @Nested
    @DisplayName("06-G: Hybrid Allocation Tests")
    class HybridAllocationTests {

        @Test
        @DisplayName("Hybrid Normal: Exactly 30% Fixed (Ownership) + 70% Variable (Usage) model reconciles perfectly")
        void testHybrid_Normal() {
            List<OwnershipShare> shares = List.of(
                    createShare(1L, user1, new BigDecimal("60.00"), true),
                    createShare(2L, user2, new BigDecimal("40.00"), true)
            );
            when(ownershipShareRepository.findByGroupIdAndIsActiveTrue(group.getId())).thenReturn(shares);

            Booking booking1 = new Booking();
            booking1.setId(1L);
            booking1.setVehicle(vehicle);
            booking1.setUser(user1);

            Booking booking2 = new Booking();
            booking2.setId(2L);
            booking2.setVehicle(vehicle);
            booking2.setUser(user2);

            // User 1 drove 100 km (10%), User 2 drove 900 km (90%)
            UsageSession session1 = new UsageSession();
            session1.setId(10L);
            session1.setBooking(booking1);
            session1.setStartOdometer(new BigDecimal("0.0"));
            session1.setEndOdometer(new BigDecimal("100.0"));
            session1.setStatus(UsageSessionStatus.COMPLETED);

            UsageSession session2 = new UsageSession();
            session2.setId(11L);
            session2.setBooking(booking2);
            session2.setStartOdometer(new BigDecimal("100.0"));
            session2.setEndOdometer(new BigDecimal("1000.0"));
            session2.setStatus(UsageSessionStatus.COMPLETED);

            when(usageSessionRepository.findByBookingVehicleIdOrderByCheckInTimeDesc(vehicle.getId()))
                    .thenReturn(List.of(session1, session2));

            // Total 1,000,000.00 VND:
            // Fixed 30% = 300,000.00 VND -> User 1 (60%): 180,000.00, User 2 (40%): 120,000.00
            // Variable 70% = 700,000.00 VND -> User 1 (10%): 70,000.00, User 2 (90%): 630,000.00
            // Total:
            // User 1: 180,000 + 70,000 = 250,000.00 VND (25.00%)
            // User 2: 120,000 + 630,000 = 750,000.00 VND (75.00%)
            Expense expense = createExpense(new BigDecimal("1000000.00"), AllocationStrategy.HYBRID);
            AllocationResult result = costAllocationService.calculateAllocation(expense);

            assertEquals(new BigDecimal("250000.00"), result.getShareForUser(user1.getId()).orElseThrow().getAllocatedAmount());
            assertEquals(new BigDecimal("750000.00"), result.getShareForUser(user2.getId()).orElseThrow().getAllocatedAmount());
            assertEquals(new BigDecimal("1000000.00"), result.getTotalAllocated());
            assertTrue(result.isReconciled());
            assertTrue(result.getSummary().contains("30% fixed: 300000.00 VND, 70% variable: 700000.00 VND"));

            AllocatedMemberShare share1 = result.getShareForUser(user1.getId()).orElseThrow();
            assertTrue(share1.getExplanation().contains("Fixed (30% equity: 180000.00 VND)"));
            assertTrue(share1.getExplanation().contains("Variable (70% usage: 70000.00 VND)"));
        }

        @Test
        @DisplayName("Hybrid Zero Usage: No completed sessions falls back gracefully to ownership equity")
        void testHybrid_ZeroUsage_NoSessions() {
            List<OwnershipShare> shares = List.of(
                    createShare(1L, user1, new BigDecimal("70.00"), true),
                    createShare(2L, user2, new BigDecimal("30.00"), true)
            );
            when(ownershipShareRepository.findByGroupIdAndIsActiveTrue(group.getId())).thenReturn(shares);
            when(usageSessionRepository.findByBookingVehicleIdOrderByCheckInTimeDesc(vehicle.getId()))
                    .thenReturn(Collections.emptyList());

            // Expense 500,000.00 VND:
            // Fixed 30% (150,000.00): User 1 = 105,000.00, User 2 = 45,000.00
            // Variable 70% (350,000.00): Zero usage fallback -> User 1 = 245,000.00, User 2 = 105,000.00
            // Combined: User 1 = 350,000.00 (70.00%), User 2 = 150,000.00 (30.00%)
            Expense expense = createExpense(new BigDecimal("500000.00"), AllocationStrategy.HYBRID);
            AllocationResult result = costAllocationService.calculateAllocation(expense);

            assertEquals(new BigDecimal("350000.00"), result.getShareForUser(user1.getId()).orElseThrow().getAllocatedAmount());
            assertEquals(new BigDecimal("150000.00"), result.getShareForUser(user2.getId()).orElseThrow().getAllocatedAmount());
            assertEquals(new BigDecimal("500000.00"), result.getTotalAllocated());
            assertTrue(result.isReconciled());
        }

        @Test
        @DisplayName("Hybrid Zero Usage: Sessions with 0.0 km movement falls back to ownership equity")
        void testHybrid_ZeroUsage_ZeroOdometerMovement() {
            List<OwnershipShare> shares = List.of(
                    createShare(1L, user1, new BigDecimal("50.00"), true),
                    createShare(2L, user2, new BigDecimal("50.00"), true)
            );
            when(ownershipShareRepository.findByGroupIdAndIsActiveTrue(group.getId())).thenReturn(shares);

            Booking booking = new Booking();
            booking.setId(1L);
            booking.setVehicle(vehicle);
            booking.setUser(user1);

            UsageSession idleSession = new UsageSession();
            idleSession.setId(10L);
            idleSession.setBooking(booking);
            idleSession.setStartOdometer(new BigDecimal("500.0"));
            idleSession.setEndOdometer(new BigDecimal("500.0")); // 0.0 km
            idleSession.setStatus(UsageSessionStatus.COMPLETED);

            when(usageSessionRepository.findByBookingVehicleIdOrderByCheckInTimeDesc(vehicle.getId()))
                    .thenReturn(List.of(idleSession));

            // Expense 200,000.00 VND: 50% each
            Expense expense = createExpense(new BigDecimal("200000.00"), AllocationStrategy.HYBRID);
            AllocationResult result = costAllocationService.calculateAllocation(expense);

            assertEquals(new BigDecimal("100000.00"), result.getShareForUser(user1.getId()).orElseThrow().getAllocatedAmount());
            assertEquals(new BigDecimal("100000.00"), result.getShareForUser(user2.getId()).orElseThrow().getAllocatedAmount());
            assertEquals(new BigDecimal("200000.00"), result.getTotalAllocated());
            assertTrue(result.isReconciled());
        }

        @Test
        @DisplayName("Hybrid Uneven Ownership: 3 co-owners (70/20/10) with uneven usage (10/40/50)")
        void testHybrid_UnevenOwnership() {
            List<OwnershipShare> shares = List.of(
                    createShare(1L, user1, new BigDecimal("70.00"), true),
                    createShare(2L, user2, new BigDecimal("20.00"), true),
                    createShare(3L, user3, new BigDecimal("10.00"), true)
            );
            when(ownershipShareRepository.findByGroupIdAndIsActiveTrue(group.getId())).thenReturn(shares);

            Booking b1 = new Booking(); b1.setId(1L); b1.setVehicle(vehicle); b1.setUser(user1);
            Booking b2 = new Booking(); b2.setId(2L); b2.setVehicle(vehicle); b2.setUser(user2);
            Booking b3 = new Booking(); b3.setId(3L); b3.setVehicle(vehicle); b3.setUser(user3);

            // User 1: 50 km (10%), User 2: 200 km (40%), User 3: 250 km (50%) -> Total 500 km
            UsageSession s1 = new UsageSession(); s1.setId(1L); s1.setBooking(b1);
            s1.setStartOdometer(new BigDecimal("0.0")); s1.setEndOdometer(new BigDecimal("50.0"));
            s1.setStatus(UsageSessionStatus.COMPLETED);

            UsageSession s2 = new UsageSession(); s2.setId(2L); s2.setBooking(b2);
            s2.setStartOdometer(new BigDecimal("50.0")); s2.setEndOdometer(new BigDecimal("250.0"));
            s2.setStatus(UsageSessionStatus.COMPLETED);

            UsageSession s3 = new UsageSession(); s3.setId(3L); s3.setBooking(b3);
            s3.setStartOdometer(new BigDecimal("250.0")); s3.setEndOdometer(new BigDecimal("500.0"));
            s3.setStatus(UsageSessionStatus.COMPLETED);

            when(usageSessionRepository.findByBookingVehicleIdOrderByCheckInTimeDesc(vehicle.getId()))
                    .thenReturn(List.of(s1, s2, s3));

            // Total 2,000,000.00 VND:
            // Fixed 30% = 600,000.00:
            //   User 1 (70%): 420,000.00
            //   User 2 (20%): 120,000.00
            //   User 3 (10%):  60,000.00
            // Variable 70% = 1,400,000.00:
            //   User 1 (10%): 140,000.00
            //   User 2 (40%): 560,000.00
            //   User 3 (50%): 700,000.00
            // Combined:
            //   User 1: 420,000 + 140,000 = 560,000.00 VND (28.00%)
            //   User 2: 120,000 + 560,000 = 680,000.00 VND (34.00%)
            //   User 3:  60,000 + 700,000 = 760,000.00 VND (38.00%)
            Expense expense = createExpense(new BigDecimal("2000000.00"), AllocationStrategy.HYBRID);
            AllocationResult result = costAllocationService.calculateAllocation(expense);

            assertEquals(new BigDecimal("560000.00"), result.getShareForUser(user1.getId()).orElseThrow().getAllocatedAmount());
            assertEquals(new BigDecimal("680000.00"), result.getShareForUser(user2.getId()).orElseThrow().getAllocatedAmount());
            assertEquals(new BigDecimal("760000.00"), result.getShareForUser(user3.getId()).orElseThrow().getAllocatedAmount());
            assertEquals(new BigDecimal("2000000.00"), result.getTotalAllocated());
            assertTrue(result.isReconciled());
        }

        @Test
        @DisplayName("Hybrid Rounding: Odd amounts (777,777.77 VND) with fractional pennies reconciles strictly")
        void testHybrid_Rounding_OddAmounts() {
            List<OwnershipShare> shares = List.of(
                    createShare(1L, user1, new BigDecimal("33.33"), true),
                    createShare(2L, user2, new BigDecimal("33.33"), true),
                    createShare(3L, user3, new BigDecimal("33.34"), true)
            );
            when(ownershipShareRepository.findByGroupIdAndIsActiveTrue(group.getId())).thenReturn(shares);

            Booking b1 = new Booking(); b1.setId(1L); b1.setVehicle(vehicle); b1.setUser(user1);
            Booking b2 = new Booking(); b2.setId(2L); b2.setVehicle(vehicle); b2.setUser(user2);
            Booking b3 = new Booking(); b3.setId(3L); b3.setVehicle(vehicle); b3.setUser(user3);

            UsageSession s1 = new UsageSession(); s1.setId(1L); s1.setBooking(b1);
            s1.setStartOdometer(new BigDecimal("0.0")); s1.setEndOdometer(new BigDecimal("100.0"));
            s1.setStatus(UsageSessionStatus.COMPLETED);

            UsageSession s2 = new UsageSession(); s2.setId(2L); s2.setBooking(b2);
            s2.setStartOdometer(new BigDecimal("100.0")); s2.setEndOdometer(new BigDecimal("300.0"));
            s2.setStatus(UsageSessionStatus.COMPLETED);

            UsageSession s3 = new UsageSession(); s3.setId(3L); s3.setBooking(b3);
            s3.setStartOdometer(new BigDecimal("300.0")); s3.setEndOdometer(new BigDecimal("600.0"));
            s3.setStatus(UsageSessionStatus.COMPLETED);

            when(usageSessionRepository.findByBookingVehicleIdOrderByCheckInTimeDesc(vehicle.getId()))
                    .thenReturn(List.of(s1, s2, s3));

            Expense expense = createExpense(new BigDecimal("777777.77"), AllocationStrategy.HYBRID);
            AllocationResult result = costAllocationService.calculateAllocation(expense);

            assertEquals(new BigDecimal("777777.77"), result.getTotalAllocated());
            assertTrue(result.isReconciled());
            assertEquals(3, result.getShares().size());

            BigDecimal sumShares = result.getShares().stream()
                    .map(AllocatedMemberShare::getAllocatedAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            assertEquals(new BigDecimal("777777.77"), sumShares);
        }

        @Test
        @DisplayName("Hybrid Rounding: Micro-cent amount (10.01 VND) split 30/70 reconciles strictly")
        void testHybrid_Rounding_MicroAmount() {
            List<OwnershipShare> shares = List.of(
                    createShare(1L, user1, new BigDecimal("50.00"), true),
                    createShare(2L, user2, new BigDecimal("50.00"), true)
            );
            when(ownershipShareRepository.findByGroupIdAndIsActiveTrue(group.getId())).thenReturn(shares);

            Booking b1 = new Booking(); b1.setId(1L); b1.setVehicle(vehicle); b1.setUser(user1);
            Booking b2 = new Booking(); b2.setId(2L); b2.setVehicle(vehicle); b2.setUser(user2);

            UsageSession s1 = new UsageSession(); s1.setId(1L); s1.setBooking(b1);
            s1.setStartOdometer(new BigDecimal("0.0")); s1.setEndOdometer(new BigDecimal("30.0"));
            s1.setStatus(UsageSessionStatus.COMPLETED);

            UsageSession s2 = new UsageSession(); s2.setId(2L); s2.setBooking(b2);
            s2.setStartOdometer(new BigDecimal("30.0")); s2.setEndOdometer(new BigDecimal("100.0"));
            s2.setStatus(UsageSessionStatus.COMPLETED);

            when(usageSessionRepository.findByBookingVehicleIdOrderByCheckInTimeDesc(vehicle.getId()))
                    .thenReturn(List.of(s1, s2));

            Expense expense = createExpense(new BigDecimal("10.01"), AllocationStrategy.HYBRID);
            AllocationResult result = costAllocationService.calculateAllocation(expense);

            assertEquals(new BigDecimal("10.01"), result.getTotalAllocated());
            assertTrue(result.isReconciled());
        }

        @Test
        @DisplayName("Hybrid Invalid Inputs: Null expense throws IllegalArgumentException")
        void testHybrid_InvalidInputs_NullExpense() {
            assertThrows(IllegalArgumentException.class, () -> hybridStrategy.allocate(null));
        }

        @Test
        @DisplayName("Hybrid Invalid Inputs: Missing or unpersisted ownership group throws BusinessException")
        void testHybrid_InvalidInputs_MissingGroup() {
            Expense expense = createExpense(new BigDecimal("100000.00"), AllocationStrategy.HYBRID);
            expense.setGroup(null);
            assertThrows(BusinessException.class, () -> hybridStrategy.allocate(expense));

            OwnershipGroup emptyGroup = new OwnershipGroup();
            expense.setGroup(emptyGroup);
            assertThrows(BusinessException.class, () -> hybridStrategy.allocate(expense));
        }

        @Test
        @DisplayName("Hybrid Invalid Inputs: Zero or negative expense amounts throw BusinessException")
        void testHybrid_InvalidInputs_ZeroOrNegativeAmount() {
            Expense zeroExpense = createExpense(BigDecimal.ZERO, AllocationStrategy.HYBRID);
            assertThrows(BusinessException.class, () -> hybridStrategy.allocate(zeroExpense));

            Expense negExpense = createExpense(new BigDecimal("-50000.00"), AllocationStrategy.HYBRID);
            assertThrows(BusinessException.class, () -> hybridStrategy.allocate(negExpense));
        }

        @Test
        @DisplayName("Hybrid Invalid Inputs: Syndicate equity not totaling 100% throws InvalidOwnershipDistributionException")
        void testHybrid_InvalidInputs_EquityNot100Percent() {
            List<OwnershipShare> shares = List.of(
                    createShare(1L, user1, new BigDecimal("60.00"), true),
                    createShare(2L, user2, new BigDecimal("30.00"), true) // sum = 90%
            );
            when(ownershipShareRepository.findByGroupIdAndIsActiveTrue(group.getId())).thenReturn(shares);

            Expense expense = createExpense(new BigDecimal("100000.00"), AllocationStrategy.HYBRID);
            assertThrows(InvalidOwnershipDistributionException.class, () -> hybridStrategy.allocate(expense));
        }

        @Test
        @DisplayName("Hybrid Invalid Inputs: Empty active shares list throws BusinessException")
        void testHybrid_InvalidInputs_EmptyActiveShares() {
            when(ownershipShareRepository.findByGroupIdAndIsActiveTrue(group.getId())).thenReturn(Collections.emptyList());

            Expense expense = createExpense(new BigDecimal("100000.00"), AllocationStrategy.HYBRID);
            assertThrows(BusinessException.class, () -> hybridStrategy.allocate(expense));
        }
    }
}
