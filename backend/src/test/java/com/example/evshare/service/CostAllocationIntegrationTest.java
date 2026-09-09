package com.example.evshare.service;

import com.example.evshare.entity.*;
import com.example.evshare.entity.enums.AllocationStrategy;
import com.example.evshare.entity.enums.ExpenseCategory;
import com.example.evshare.entity.enums.RoleName;
import com.example.evshare.entity.enums.VehicleStatus;
import com.example.evshare.exception.InvalidOwnershipDistributionException;
import com.example.evshare.repository.*;
import com.example.evshare.service.allocation.AllocatedMemberShare;
import com.example.evshare.service.allocation.AllocationResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
@DisplayName("Checkpoints 06-D & 06-E — Cost Allocation Engine & Ownership Allocation Integration Tests")
class CostAllocationIntegrationTest {

    private static final AtomicLong PHONE_COUNTER = new AtomicLong(System.currentTimeMillis() % 80000000L + 20000000L);

    @Autowired private CostAllocationService costAllocationService;
    @Autowired private ExpenseRepository expenseRepository;
    @Autowired private ExpenseAllocationRepository expenseAllocationRepository;
    @Autowired private OwnershipGroupRepository ownershipGroupRepository;
    @Autowired private OwnershipShareRepository ownershipShareRepository;
    @Autowired private VehicleRepository vehicleRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private RoleRepository roleRepository;
    @Autowired private SharedFundRepository sharedFundRepository;
    @Autowired private BookingRepository bookingRepository;
    @Autowired private UsageSessionRepository usageSessionRepository;

    private Role coOwnerRole;
    private Vehicle vehicle;
    private OwnershipGroup group;
    private User owner1;
    private User owner2;
    private User owner3;

    @BeforeEach
    void setUp() {
        coOwnerRole = roleRepository.findByName(RoleName.ROLE_CO_OWNER)
                .orElseGet(() -> roleRepository.save(new Role(null, RoleName.ROLE_CO_OWNER)));

        String uid = UUID.randomUUID().toString().substring(0, 8);
        vehicle = new Vehicle();
        vehicle.setVin("VIN-" + uid);
        vehicle.setLicensePlate("30E-" + uid.toUpperCase());
        vehicle.setModelName("VF9 Integration Test");
        vehicle.setManufacturer("VinFast");
        vehicle.setModel3dAssetPath("models/vehicles/vf9.glb");
        vehicle.setStatus(VehicleStatus.AVAILABLE);
        vehicle = vehicleRepository.save(vehicle);

        group = new OwnershipGroup();
        group.setGroupName("Syndicate " + uid);
        group.setVehicle(vehicle);
        group.setFormationDate(LocalDate.now());
        group.setIsActive(true);
        group = ownershipGroupRepository.save(group);

        SharedFund fund = new SharedFund();
        fund.setGroup(group);
        fund.setCurrentBalance(new BigDecimal("10000000.00"));
        fund.setCurrency("VND");
        sharedFundRepository.save(fund);

        owner1 = createUser("owner1-" + uid);
        owner2 = createUser("owner2-" + uid);
        owner3 = createUser("owner3-" + uid);
    }

    private User createUser(String prefix) {
        User u = new User();
        u.setEmail(prefix + "@evshare.io");
        u.setFullName("User " + prefix);
        u.setPasswordHash("dummyhash");
        u.setPhoneNumber("09" + PHONE_COUNTER.incrementAndGet());
        u.setIsActive(true);
        u.setRoles(Set.of(coOwnerRole));
        return userRepository.save(u);
    }

    private OwnershipShare createShare(User user, BigDecimal percentage, boolean isActive) {
        OwnershipShare share = new OwnershipShare();
        share.setGroup(group);
        share.setUser(user);
        share.setPercentage(percentage);
        share.setShareCertificateNumber("CERT-" + UUID.randomUUID().toString().substring(0, 10));
        share.setAcquiredAt(Instant.now());
        share.setIsActive(isActive);
        return ownershipShareRepository.save(share);
    }

    private Expense persistExpense(BigDecimal amount, AllocationStrategy strategy) {
        Expense expense = new Expense();
        expense.setGroup(group);
        expense.setVehicle(vehicle);
        expense.setTitle("Battery Thermal Management Service");
        expense.setCategory(ExpenseCategory.MAINTENANCE);
        expense.setTotalAmount(amount);
        expense.setCurrency("VND");
        expense.setAllocationStrategy(strategy);
        expense.setIncurredDate(LocalDate.now());
        expense.setLoggedByUser(owner1);
        expense.setInvoiceReference("INV-" + UUID.randomUUID().toString().substring(0, 8));
        return expenseRepository.save(expense);
    }

    @Test
    @DisplayName("06-E Integration: Allocate 60/40 distribution and persist to database")
    void testOwnershipAllocation_60_40_EndToEndPersistence() {
        createShare(owner1, new BigDecimal("60.00"), true);
        createShare(owner2, new BigDecimal("40.00"), true);

        Expense expense = persistExpense(new BigDecimal("1000000.00"), AllocationStrategy.OWNERSHIP_BASED);

        List<ExpenseAllocation> allocations = costAllocationService.applyAndPersistAllocations(expense);

        assertEquals(2, allocations.size());
        List<ExpenseAllocation> fromDb = expenseAllocationRepository.findByExpenseId(expense.getId());
        assertEquals(2, fromDb.size());

        BigDecimal totalInDb = fromDb.stream()
                .map(ExpenseAllocation::getAllocatedAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        assertEquals(0, new BigDecimal("1000000.00").compareTo(totalInDb));

        ExpenseAllocation a1 = fromDb.stream().filter(a -> a.getUser().getId().equals(owner1.getId())).findFirst().orElseThrow();
        ExpenseAllocation a2 = fromDb.stream().filter(a -> a.getUser().getId().equals(owner2.getId())).findFirst().orElseThrow();

        assertEquals(new BigDecimal("600000.00"), a1.getAllocatedAmount());
        assertEquals(new BigDecimal("400000.00"), a2.getAllocatedAmount());
        assertFalse(a1.getIsSettled());
        assertFalse(a2.getIsSettled());
    }

    @Test
    @DisplayName("06-E Integration: 33.33 / 33.33 / 33.34 distribution with deterministic residual penny absorption in DB")
    void testOwnershipAllocation_3333_3333_3334_ResidualPennyAbsorption() {
        createShare(owner1, new BigDecimal("33.33"), true);
        createShare(owner2, new BigDecimal("33.33"), true);
        createShare(owner3, new BigDecimal("33.34"), true);

        // Expense of 10.00 VND
        Expense expense = persistExpense(new BigDecimal("10.00"), AllocationStrategy.OWNERSHIP_BASED);

        List<ExpenseAllocation> allocations = costAllocationService.applyAndPersistAllocations(expense);

        assertEquals(3, allocations.size());
        BigDecimal total = allocations.stream()
                .map(ExpenseAllocation::getAllocatedAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        assertEquals(0, new BigDecimal("10.00").compareTo(total));

        ExpenseAllocation a3 = allocations.stream().filter(a -> a.getUser().getId().equals(owner3.getId())).findFirst().orElseThrow();
        assertEquals(new BigDecimal("3.34"), a3.getAllocatedAmount(),
                "Owner 3 with highest equity percentage (33.34%) must absorb the residual penny");
    }

    @Test
    @DisplayName("06-E Integration: Active ownership only (inactive share excluded from allocation)")
    void testOwnershipAllocation_ExcludesInactiveSharesFromDB() {
        createShare(owner1, new BigDecimal("70.00"), true);
        createShare(owner2, new BigDecimal("30.00"), true);
        createShare(owner3, new BigDecimal("20.00"), false); // Inactive share

        Expense expense = persistExpense(new BigDecimal("500000.00"), AllocationStrategy.OWNERSHIP_BASED);

        List<ExpenseAllocation> allocations = costAllocationService.applyAndPersistAllocations(expense);

        assertEquals(2, allocations.size());
        assertTrue(allocations.stream().noneMatch(a -> a.getUser().getId().equals(owner3.getId())),
                "Inactive member must not have an ExpenseAllocation record generated");

        BigDecimal total = allocations.stream()
                .map(ExpenseAllocation::getAllocatedAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        assertEquals(0, new BigDecimal("500000.00").compareTo(total));
    }

    @Test
    @DisplayName("06-E Invariant: Incomplete active equity (not 100%) rejected before database writes")
    void testOwnershipAllocation_InvalidDistributionRejected() {
        createShare(owner1, new BigDecimal("50.00"), true);
        createShare(owner2, new BigDecimal("30.00"), true); // Total 80.00% != 100.00%

        Expense expense = persistExpense(new BigDecimal("250000.00"), AllocationStrategy.OWNERSHIP_BASED);

        assertThrows(InvalidOwnershipDistributionException.class, () ->
                costAllocationService.applyAndPersistAllocations(expense)
        );

        List<ExpenseAllocation> allocationsInDb = expenseAllocationRepository.findByExpenseId(expense.getId());
        assertTrue(allocationsInDb.isEmpty(), "No allocations should be written to database if equity invariant fails");
    }

    @Test
    @DisplayName("06-F Integration: Usage-based allocation based on odometer distance logged in MySQL")
    void testUsageBasedAllocation_EndToEndPersistence() {
        createShare(owner1, new BigDecimal("50.00"), true);
        createShare(owner2, new BigDecimal("50.00"), true);

        Booking b1 = new Booking();
        b1.setUser(owner1);
        b1.setVehicle(vehicle);
        b1.setStartTime(Instant.now().minus(2, java.time.temporal.ChronoUnit.DAYS));
        b1.setEndTime(Instant.now().minus(2, java.time.temporal.ChronoUnit.DAYS).plusSeconds(7200));
        b1.setStatus(com.example.evshare.entity.enums.BookingStatus.COMPLETED);
        b1.setEstimatedCost(new BigDecimal("100000.00"));
        b1 = bookingRepository.saveAndFlush(b1);

        UsageSession s1 = new UsageSession();
        s1.setBooking(b1);
        s1.setStartOdometer(new BigDecimal("1000.00"));
        s1.setEndOdometer(new BigDecimal("1300.00")); // 300 km
        s1.setStartBattery(100);
        s1.setEndBattery(60);
        s1.setStatus(com.example.evshare.entity.enums.UsageSessionStatus.COMPLETED);
        s1.setCheckInTime(b1.getStartTime());
        s1.setCheckOutTime(b1.getEndTime());
        usageSessionRepository.saveAndFlush(s1);

        Booking b2 = new Booking();
        b2.setUser(owner2);
        b2.setVehicle(vehicle);
        b2.setStartTime(Instant.now().minus(1, java.time.temporal.ChronoUnit.DAYS));
        b2.setEndTime(Instant.now().minus(1, java.time.temporal.ChronoUnit.DAYS).plusSeconds(3600));
        b2.setStatus(com.example.evshare.entity.enums.BookingStatus.COMPLETED);
        b2.setEstimatedCost(new BigDecimal("50000.00"));
        b2 = bookingRepository.saveAndFlush(b2);

        UsageSession s2 = new UsageSession();
        s2.setBooking(b2);
        s2.setStartOdometer(new BigDecimal("1300.00"));
        s2.setEndOdometer(new BigDecimal("1400.00")); // 100 km
        s2.setStartBattery(60);
        s2.setEndBattery(40);
        s2.setStatus(com.example.evshare.entity.enums.UsageSessionStatus.COMPLETED);
        s2.setCheckInTime(b2.getStartTime());
        s2.setCheckOutTime(b2.getEndTime());
        usageSessionRepository.saveAndFlush(s2);

        // Total distance: 400.00 km (Owner1 = 75%, Owner2 = 25%)
        Expense expense = persistExpense(new BigDecimal("400000.00"), AllocationStrategy.USAGE_BASED);

        List<ExpenseAllocation> allocations = costAllocationService.applyAndPersistAllocations(expense);

        assertEquals(2, allocations.size());
        List<ExpenseAllocation> fromDb = expenseAllocationRepository.findByExpenseId(expense.getId());
        assertEquals(2, fromDb.size());

        ExpenseAllocation a1 = fromDb.stream().filter(a -> a.getUser().getId().equals(owner1.getId())).findFirst().orElseThrow();
        ExpenseAllocation a2 = fromDb.stream().filter(a -> a.getUser().getId().equals(owner2.getId())).findFirst().orElseThrow();

        assertEquals(new BigDecimal("300000.00"), a1.getAllocatedAmount());
        assertEquals(new BigDecimal("100000.00"), a2.getAllocatedAmount());

        BigDecimal total = fromDb.stream().map(ExpenseAllocation::getAllocatedAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        assertEquals(0, new BigDecimal("400000.00").compareTo(total));
    }

    @Test
    @DisplayName("06-G Integration: Hybrid allocation (30% ownership + 70% usage) persisted to database")
    void testHybridAllocation_EndToEndPersistence() {
        createShare(owner1, new BigDecimal("60.00"), true);
        createShare(owner2, new BigDecimal("40.00"), true);

        Booking b1 = new Booking();
        b1.setUser(owner1);
        b1.setVehicle(vehicle);
        b1.setStartTime(Instant.now().minus(3, java.time.temporal.ChronoUnit.DAYS));
        b1.setEndTime(Instant.now().minus(3, java.time.temporal.ChronoUnit.DAYS).plusSeconds(3600));
        b1.setStatus(com.example.evshare.entity.enums.BookingStatus.COMPLETED);
        b1.setEstimatedCost(new BigDecimal("100000.00"));
        b1 = bookingRepository.saveAndFlush(b1);

        UsageSession s1 = new UsageSession();
        s1.setBooking(b1);
        s1.setStartOdometer(new BigDecimal("0.00"));
        s1.setEndOdometer(new BigDecimal("100.00")); // 100 km (10%)
        s1.setStartBattery(100);
        s1.setEndBattery(80);
        s1.setStatus(com.example.evshare.entity.enums.UsageSessionStatus.COMPLETED);
        s1.setCheckInTime(b1.getStartTime());
        s1.setCheckOutTime(b1.getEndTime());
        usageSessionRepository.saveAndFlush(s1);

        Booking b2 = new Booking();
        b2.setUser(owner2);
        b2.setVehicle(vehicle);
        b2.setStartTime(Instant.now().minus(2, java.time.temporal.ChronoUnit.DAYS));
        b2.setEndTime(Instant.now().minus(2, java.time.temporal.ChronoUnit.DAYS).plusSeconds(7200));
        b2.setStatus(com.example.evshare.entity.enums.BookingStatus.COMPLETED);
        b2.setEstimatedCost(new BigDecimal("200000.00"));
        b2 = bookingRepository.saveAndFlush(b2);

        UsageSession s2 = new UsageSession();
        s2.setBooking(b2);
        s2.setStartOdometer(new BigDecimal("100.00"));
        s2.setEndOdometer(new BigDecimal("1000.00")); // 900 km (90%)
        s2.setStartBattery(80);
        s2.setEndBattery(20);
        s2.setStatus(com.example.evshare.entity.enums.UsageSessionStatus.COMPLETED);
        s2.setCheckInTime(b2.getStartTime());
        s2.setCheckOutTime(b2.getEndTime());
        usageSessionRepository.saveAndFlush(s2);

        // Total 1,000,000.00 VND:
        // Fixed 30% (300,000.00): owner1 (60%) = 180,000.00, owner2 (40%) = 120,000.00
        // Variable 70% (700,000.00): owner1 (10%) = 70,000.00, owner2 (90%) = 630,000.00
        // Combined: owner1 = 250,000.00 VND, owner2 = 750,000.00 VND
        Expense expense = persistExpense(new BigDecimal("1000000.00"), AllocationStrategy.HYBRID);

        List<ExpenseAllocation> allocations = costAllocationService.applyAndPersistAllocations(expense);

        assertEquals(2, allocations.size());
        List<ExpenseAllocation> fromDb = expenseAllocationRepository.findByExpenseId(expense.getId());
        assertEquals(2, fromDb.size());

        ExpenseAllocation a1 = fromDb.stream().filter(a -> a.getUser().getId().equals(owner1.getId())).findFirst().orElseThrow();
        ExpenseAllocation a2 = fromDb.stream().filter(a -> a.getUser().getId().equals(owner2.getId())).findFirst().orElseThrow();

        assertEquals(new BigDecimal("250000.00"), a1.getAllocatedAmount());
        assertEquals(new BigDecimal("750000.00"), a2.getAllocatedAmount());

        BigDecimal total = fromDb.stream().map(ExpenseAllocation::getAllocatedAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        assertEquals(0, new BigDecimal("100000.000").compareTo(total.divide(new BigDecimal("10"), 3, java.math.RoundingMode.HALF_EVEN)));
        assertEquals(new BigDecimal("1000000.00"), total);

        assertNotNull(a1.getId());
        assertNotNull(a2.getId());
        assertFalse(a1.getIsSettled());
        assertFalse(a2.getIsSettled());
    }
}
