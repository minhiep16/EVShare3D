package com.example.evshare.service;

import com.example.evshare.dto.request.CreateExpenseRequest;
import com.example.evshare.dto.response.ExpenseAuditLogResponse;
import com.example.evshare.dto.response.ExpenseResponse;
import com.example.evshare.dto.response.PagedData;
import com.example.evshare.entity.*;
import com.example.evshare.entity.enums.AllocationStrategy;
import com.example.evshare.entity.enums.ExpenseCategory;
import com.example.evshare.entity.enums.RoleName;
import com.example.evshare.exception.BusinessException;
import com.example.evshare.repository.*;
import com.example.evshare.security.OwnershipSecurity;
import com.example.evshare.service.impl.ExpenseServiceImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ExpenseServiceTest {

    @Mock private ExpenseRepository expenseRepository;
    @Mock private ExpenseAllocationRepository expenseAllocationRepository;
    @Mock private OwnershipGroupRepository ownershipGroupRepository;
    @Mock private OwnershipShareRepository ownershipShareRepository;
    @Mock private VehicleRepository vehicleRepository;
    @Mock private UserRepository userRepository;
    @Mock private SharedFundRepository sharedFundRepository;
    @Mock private AuditLogRepository auditLogRepository;

    private OwnershipSecurity ownershipSecurity;
    private ObjectMapper objectMapper;
    private ExpenseService expenseService;

    private User coOwnerUser;
    private User staffUser;
    private OwnershipGroup group;
    private Vehicle vehicle;
    private SharedFund sharedFund;
    private OwnershipShare share1;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        ownershipSecurity = new OwnershipSecurity(ownershipShareRepository, null, ownershipGroupRepository, expenseRepository);

        expenseService = new ExpenseServiceImpl(
                expenseRepository,
                expenseAllocationRepository,
                ownershipGroupRepository,
                ownershipShareRepository,
                vehicleRepository,
                userRepository,
                sharedFundRepository,
                auditLogRepository,
                ownershipSecurity,
                objectMapper
        );

        // Setup common entities
        coOwnerUser = new User();
        coOwnerUser.setId(10L);
        coOwnerUser.setEmail("owner@evshare.io");
        coOwnerUser.setFullName("Co Owner One");
        coOwnerUser.setIsActive(true);
        Role coOwnerRole = new Role(1L, RoleName.ROLE_CO_OWNER);
        coOwnerUser.setRoles(Set.of(coOwnerRole));

        staffUser = new User();
        staffUser.setId(20L);
        staffUser.setEmail("staff@evshare.io");
        staffUser.setFullName("Operations Staff");
        staffUser.setIsActive(true);
        Role staffRole = new Role(2L, RoleName.ROLE_STAFF);
        staffUser.setRoles(Set.of(staffRole));

        vehicle = new Vehicle();
        vehicle.setId(100L);
        vehicle.setVin("VF8-VN-2026-0001");
        vehicle.setLicensePlate("51K-999.88");
        vehicle.setModelName("VinFast VF8 Plus");

        group = new OwnershipGroup();
        group.setId(1L);
        group.setGroupName("VF8 Syndicate Alpha");
        group.setVehicle(vehicle);
        group.setFormationDate(LocalDate.of(2026, 1, 1));
        group.setIsActive(true);

        sharedFund = new SharedFund();
        sharedFund.setId(1L);
        sharedFund.setGroup(group);
        sharedFund.setCurrentBalance(new BigDecimal("15000000.00"));
        sharedFund.setCurrency("VND");

        share1 = new OwnershipShare(1L, group, coOwnerUser, new BigDecimal("100.00"), "CERT-001", Instant.now(), true);
    }

    @Nested
    @DisplayName("Expense Creation Validation Tests")
    class ValidationTests {

        @Test
        @DisplayName("Should create valid expense and record audit log without allocating costs yet")
        void shouldCreateChargingExpenseSuccessfully() {
            CreateExpenseRequest request = new CreateExpenseRequest(
                    1L, 100L, "Vincom DC Fast Charge", ExpenseCategory.CHARGING,
                    new BigDecimal("350000.00"), "VND", AllocationStrategy.OWNERSHIP_BASED,
                    LocalDate.now(), "INV-001", null, false
            );

            when(userRepository.findById(10L)).thenReturn(Optional.of(coOwnerUser));
            when(ownershipGroupRepository.findById(1L)).thenReturn(Optional.of(group));
            when(ownershipShareRepository.findByGroupIdAndUserId(1L, 10L)).thenReturn(Optional.of(share1));
            when(vehicleRepository.findById(100L)).thenReturn(Optional.of(vehicle));
            when(sharedFundRepository.findByGroupId(1L)).thenReturn(Optional.of(sharedFund));
            when(expenseRepository.existsByInvoiceReference("INV-001")).thenReturn(false);
            when(expenseRepository.existsDuplicateExpense(any(), any(), any(), any(), any(), any())).thenReturn(false);
            when(expenseRepository.save(any(Expense.class))).thenAnswer(invocation -> {
                Expense e = invocation.getArgument(0);
                e.setId(500L);
                return e;
            });

            ExpenseResponse response = expenseService.createExpense(request, 10L, "127.0.0.1");

            assertNotNull(response);
            assertEquals(500L, response.getId());
            assertEquals(new BigDecimal("350000.00"), response.getAmount());
            assertEquals("VND", response.getCurrency());
            assertEquals(ExpenseCategory.CHARGING, response.getCategory());
            // In accordance with Checkpoint 06-C: "Do not allocate costs yet"
            assertTrue(response.getAllocations().isEmpty(), "Cost allocations must be deferred in Checkpoint 06-C");

            // Verify Audit Log
            ArgumentCaptor<AuditLog> auditCaptor = ArgumentCaptor.forClass(AuditLog.class);
            verify(auditLogRepository).save(auditCaptor.capture());
            AuditLog savedLog = auditCaptor.getValue();
            assertEquals("EXPENSE_CREATED", savedLog.getAction());
            assertEquals("Expense", savedLog.getEntityName());
            assertEquals(500L, savedLog.getEntityId());
        }

        @Test
        @DisplayName("Should reject duplicate invoice reference with 409 Conflict")
        void shouldRejectDuplicateInvoiceReference() {
            CreateExpenseRequest request = new CreateExpenseRequest(
                    1L, 100L, "Vincom DC Fast Charge", ExpenseCategory.CHARGING,
                    new BigDecimal("350000.00"), "VND", AllocationStrategy.OWNERSHIP_BASED,
                    LocalDate.now(), "INV-DUPLICATE-01", null, false
            );

            when(userRepository.findById(10L)).thenReturn(Optional.of(coOwnerUser));
            when(ownershipGroupRepository.findById(1L)).thenReturn(Optional.of(group));
            when(ownershipShareRepository.findByGroupIdAndUserId(1L, 10L)).thenReturn(Optional.of(share1));
            when(vehicleRepository.findById(100L)).thenReturn(Optional.of(vehicle));
            when(sharedFundRepository.findByGroupId(1L)).thenReturn(Optional.of(sharedFund));
            when(expenseRepository.existsByInvoiceReference("INV-DUPLICATE-01")).thenReturn(true);

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> expenseService.createExpense(request, 10L, "127.0.0.1"));
            assertEquals(HttpStatus.CONFLICT, ex.getStatus());
            assertTrue(ex.getMessage().contains("Duplicate expense detected"));
        }

        @Test
        @DisplayName("Should reject duplicate identical expense without invoice with 409 Conflict")
        void shouldRejectDuplicateIdenticalExpense() {
            CreateExpenseRequest request = new CreateExpenseRequest(
                    1L, 100L, "Parking at Landmark 81", ExpenseCategory.PARKING,
                    new BigDecimal("50000.00"), "VND", AllocationStrategy.OWNERSHIP_BASED,
                    LocalDate.now(), null, null, false
            );

            when(userRepository.findById(10L)).thenReturn(Optional.of(coOwnerUser));
            when(ownershipGroupRepository.findById(1L)).thenReturn(Optional.of(group));
            when(ownershipShareRepository.findByGroupIdAndUserId(1L, 10L)).thenReturn(Optional.of(share1));
            when(vehicleRepository.findById(100L)).thenReturn(Optional.of(vehicle));
            when(sharedFundRepository.findByGroupId(1L)).thenReturn(Optional.of(sharedFund));
            when(expenseRepository.existsDuplicateExpense(eq(1L), eq(100L), eq(ExpenseCategory.PARKING), eq(new BigDecimal("50000.00")), any(LocalDate.class), eq("Parking at Landmark 81"))).thenReturn(true);

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> expenseService.createExpense(request, 10L, "127.0.0.1"));
            assertEquals(HttpStatus.CONFLICT, ex.getStatus());
            assertTrue(ex.getMessage().contains("Duplicate expense detected"));
        }

        @Test
        @DisplayName("Should reject non-positive or zero amount")
        void shouldRejectNonPositiveAmount() {
            CreateExpenseRequest request = new CreateExpenseRequest(
                    1L, 100L, "Zero charge", ExpenseCategory.CHARGING,
                    BigDecimal.ZERO, "VND", AllocationStrategy.OWNERSHIP_BASED,
                    LocalDate.now(), null, null, false
            );

            when(userRepository.findById(10L)).thenReturn(Optional.of(coOwnerUser));
            when(ownershipGroupRepository.findById(1L)).thenReturn(Optional.of(group));
            when(ownershipShareRepository.findByGroupIdAndUserId(1L, 10L)).thenReturn(Optional.of(share1));
            when(vehicleRepository.findById(100L)).thenReturn(Optional.of(vehicle));

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> expenseService.createExpense(request, 10L, "127.0.0.1"));
            assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
            assertTrue(ex.getMessage().contains("strictly greater than 0.00"));
        }

        @Test
        @DisplayName("Should reject invalid currency or currency mismatch with syndicate fund")
        void shouldRejectCurrencyMismatch() {
            CreateExpenseRequest request = new CreateExpenseRequest(
                    1L, 100L, "USD charge", ExpenseCategory.CHARGING,
                    new BigDecimal("100.00"), "USD", AllocationStrategy.OWNERSHIP_BASED,
                    LocalDate.now(), null, null, false
            );

            when(userRepository.findById(10L)).thenReturn(Optional.of(coOwnerUser));
            when(ownershipGroupRepository.findById(1L)).thenReturn(Optional.of(group));
            when(ownershipShareRepository.findByGroupIdAndUserId(1L, 10L)).thenReturn(Optional.of(share1));
            when(vehicleRepository.findById(100L)).thenReturn(Optional.of(vehicle));
            when(sharedFundRepository.findByGroupId(1L)).thenReturn(Optional.of(sharedFund));

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> expenseService.createExpense(request, 10L, "127.0.0.1"));
            assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
            assertTrue(ex.getMessage().contains("does not match syndicate fund currency"));
        }

        @Test
        @DisplayName("Should reject inactive ownership group")
        void shouldRejectInactiveGroup() {
            group.setIsActive(false);
            CreateExpenseRequest request = new CreateExpenseRequest(
                    1L, 100L, "Charge", ExpenseCategory.CHARGING,
                    new BigDecimal("100000.00"), "VND", AllocationStrategy.OWNERSHIP_BASED,
                    LocalDate.now(), null, null, false
            );

            when(userRepository.findById(10L)).thenReturn(Optional.of(coOwnerUser));
            when(ownershipGroupRepository.findById(1L)).thenReturn(Optional.of(group));

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> expenseService.createExpense(request, 10L, "127.0.0.1"));
            assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
            assertTrue(ex.getMessage().contains("inactive"));
        }

        @Test
        @DisplayName("Should reject vehicle that does not belong to the ownership group")
        void shouldRejectMismatchedVehicle() {
            Vehicle otherVehicle = new Vehicle();
            otherVehicle.setId(999L);

            CreateExpenseRequest request = new CreateExpenseRequest(
                    1L, 999L, "Charge", ExpenseCategory.CHARGING,
                    new BigDecimal("100000.00"), "VND", AllocationStrategy.OWNERSHIP_BASED,
                    LocalDate.now(), null, null, false
            );

            when(userRepository.findById(10L)).thenReturn(Optional.of(coOwnerUser));
            when(ownershipGroupRepository.findById(1L)).thenReturn(Optional.of(group));
            when(ownershipShareRepository.findByGroupIdAndUserId(1L, 10L)).thenReturn(Optional.of(share1));
            when(vehicleRepository.findById(999L)).thenReturn(Optional.of(otherVehicle));

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> expenseService.createExpense(request, 10L, "127.0.0.1"));
            assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
            assertTrue(ex.getMessage().contains("does not belong to syndicate group"));
        }

        @Test
        @DisplayName("Should reject unpermitted actor (co-owner not member of group) with 403 Forbidden")
        void shouldRejectUnauthorizedCoOwner() {
            CreateExpenseRequest request = new CreateExpenseRequest(
                    1L, 100L, "Charge", ExpenseCategory.CHARGING,
                    new BigDecimal("100000.00"), "VND", AllocationStrategy.OWNERSHIP_BASED,
                    LocalDate.now(), null, null, false
            );

            when(userRepository.findById(10L)).thenReturn(Optional.of(coOwnerUser));
            when(ownershipGroupRepository.findById(1L)).thenReturn(Optional.of(group));
            when(ownershipShareRepository.findByGroupIdAndUserId(1L, 10L)).thenReturn(Optional.empty());

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> expenseService.createExpense(request, 10L, "127.0.0.1"));
            assertEquals(HttpStatus.FORBIDDEN, ex.getStatus());
        }

        @Test
        @DisplayName("Should reject inactive user actor with 403 Forbidden")
        void shouldRejectInactiveUser() {
            coOwnerUser.setIsActive(false);
            CreateExpenseRequest request = new CreateExpenseRequest(
                    1L, 100L, "Charge", ExpenseCategory.CHARGING,
                    new BigDecimal("100000.00"), "VND", AllocationStrategy.OWNERSHIP_BASED,
                    LocalDate.now(), null, null, false
            );

            when(userRepository.findById(10L)).thenReturn(Optional.of(coOwnerUser));

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> expenseService.createExpense(request, 10L, "127.0.0.1"));
            assertEquals(HttpStatus.FORBIDDEN, ex.getStatus());
            assertTrue(ex.getMessage().contains("inactive"));
        }

        @Test
        @DisplayName("Should allow Staff to record expense without being a co-owner")
        void shouldAllowStaffToRecordExpense() {
            CreateExpenseRequest request = new CreateExpenseRequest(
                    1L, 100L, "Staff Tire Inspection", ExpenseCategory.INSPECTION,
                    new BigDecimal("200000.00"), "VND", AllocationStrategy.OWNERSHIP_BASED,
                    LocalDate.now(), "INSPECT-01", null, false
            );

            when(userRepository.findById(20L)).thenReturn(Optional.of(staffUser));
            when(ownershipGroupRepository.findById(1L)).thenReturn(Optional.of(group));
            when(vehicleRepository.findById(100L)).thenReturn(Optional.of(vehicle));
            when(sharedFundRepository.findByGroupId(1L)).thenReturn(Optional.of(sharedFund));
            when(expenseRepository.existsByInvoiceReference("INSPECT-01")).thenReturn(false);
            when(expenseRepository.existsDuplicateExpense(any(), any(), any(), any(), any(), any())).thenReturn(false);
            when(expenseRepository.save(any(Expense.class))).thenAnswer(i -> {
                Expense e = i.getArgument(0);
                e.setId(501L);
                return e;
            });

            ExpenseResponse resp = expenseService.createExpense(request, 20L, "127.0.0.1");
            assertNotNull(resp);
            assertEquals(501L, resp.getId());
        }

        @Test
        @DisplayName("Should reject future incurred date")
        void shouldRejectFutureIncurredDate() {
            CreateExpenseRequest request = new CreateExpenseRequest(
                    1L, 100L, "Future expense", ExpenseCategory.CHARGING,
                    new BigDecimal("100000.00"), "VND", AllocationStrategy.OWNERSHIP_BASED,
                    LocalDate.now().plusDays(2), null, null, false
            );

            when(userRepository.findById(10L)).thenReturn(Optional.of(coOwnerUser));
            when(ownershipGroupRepository.findById(1L)).thenReturn(Optional.of(group));
            when(ownershipShareRepository.findByGroupIdAndUserId(1L, 10L)).thenReturn(Optional.of(share1));
            when(vehicleRepository.findById(100L)).thenReturn(Optional.of(vehicle));
            when(sharedFundRepository.findByGroupId(1L)).thenReturn(Optional.of(sharedFund));

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> expenseService.createExpense(request, 10L, "127.0.0.1"));
            assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
            assertTrue(ex.getMessage().contains("cannot be in the future"));
        }

        @Test
        @DisplayName("Should reject incurred date before group formation date")
        void shouldRejectDateBeforeFormation() {
            CreateExpenseRequest request = new CreateExpenseRequest(
                    1L, 100L, "Pre-formation charge", ExpenseCategory.CHARGING,
                    new BigDecimal("100000.00"), "VND", AllocationStrategy.OWNERSHIP_BASED,
                    LocalDate.of(2025, 12, 31), null, null, false
            );

            when(userRepository.findById(10L)).thenReturn(Optional.of(coOwnerUser));
            when(ownershipGroupRepository.findById(1L)).thenReturn(Optional.of(group));
            when(ownershipShareRepository.findByGroupIdAndUserId(1L, 10L)).thenReturn(Optional.of(share1));
            when(vehicleRepository.findById(100L)).thenReturn(Optional.of(vehicle));
            when(sharedFundRepository.findByGroupId(1L)).thenReturn(Optional.of(sharedFund));

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> expenseService.createExpense(request, 10L, "127.0.0.1"));
            assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
            assertTrue(ex.getMessage().contains("cannot precede group formation date"));
        }

        @Test
        @DisplayName("Should reject MAINTENANCE without evidence")
        void shouldRejectMaintenanceWithoutEvidence() {
            CreateExpenseRequest request = new CreateExpenseRequest(
                    1L, 100L, "Brake Fluid Flush", ExpenseCategory.MAINTENANCE,
                    new BigDecimal("500000.00"), "VND", AllocationStrategy.OWNERSHIP_BASED,
                    LocalDate.now(), null, null, false
            );

            when(userRepository.findById(10L)).thenReturn(Optional.of(coOwnerUser));
            when(ownershipGroupRepository.findById(1L)).thenReturn(Optional.of(group));
            when(ownershipShareRepository.findByGroupIdAndUserId(1L, 10L)).thenReturn(Optional.of(share1));
            when(vehicleRepository.findById(100L)).thenReturn(Optional.of(vehicle));
            when(sharedFundRepository.findByGroupId(1L)).thenReturn(Optional.of(sharedFund));

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> expenseService.createExpense(request, 10L, "127.0.0.1"));
            assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
            assertTrue(ex.getMessage().contains("evidence is strictly required"));
        }

        @Test
        @DisplayName("Should reject REPAIR without evidence")
        void shouldRejectRepairWithoutEvidence() {
            CreateExpenseRequest request = new CreateExpenseRequest(
                    1L, 100L, "Body Dent Repair", ExpenseCategory.REPAIR,
                    new BigDecimal("750000.00"), "VND", AllocationStrategy.OWNERSHIP_BASED,
                    LocalDate.now(), null, null, false
            );

            when(userRepository.findById(10L)).thenReturn(Optional.of(coOwnerUser));
            when(ownershipGroupRepository.findById(1L)).thenReturn(Optional.of(group));
            when(ownershipShareRepository.findByGroupIdAndUserId(1L, 10L)).thenReturn(Optional.of(share1));
            when(vehicleRepository.findById(100L)).thenReturn(Optional.of(vehicle));
            when(sharedFundRepository.findByGroupId(1L)).thenReturn(Optional.of(sharedFund));

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> expenseService.createExpense(request, 10L, "127.0.0.1"));
            assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
            assertTrue(ex.getMessage().contains("evidence is strictly required"));
        }

        @Test
        @DisplayName("Should reject INSPECTION without evidence")
        void shouldRejectInspectionWithoutEvidence() {
            CreateExpenseRequest request = new CreateExpenseRequest(
                    1L, 100L, "Quarterly Alignment Inspection", ExpenseCategory.INSPECTION,
                    new BigDecimal("250000.00"), "VND", AllocationStrategy.OWNERSHIP_BASED,
                    LocalDate.now(), null, null, false
            );

            when(userRepository.findById(10L)).thenReturn(Optional.of(coOwnerUser));
            when(ownershipGroupRepository.findById(1L)).thenReturn(Optional.of(group));
            when(ownershipShareRepository.findByGroupIdAndUserId(1L, 10L)).thenReturn(Optional.of(share1));
            when(vehicleRepository.findById(100L)).thenReturn(Optional.of(vehicle));
            when(sharedFundRepository.findByGroupId(1L)).thenReturn(Optional.of(sharedFund));

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> expenseService.createExpense(request, 10L, "127.0.0.1"));
            assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
            assertTrue(ex.getMessage().contains("evidence is strictly required"));
        }

        @Test
        @DisplayName("Should reject INSURANCE without evidence")
        void shouldRejectInsuranceWithoutEvidence() {
            CreateExpenseRequest request = new CreateExpenseRequest(
                    1L, 100L, "Compulsory Civil Liability Insurance", ExpenseCategory.INSURANCE,
                    new BigDecimal("800000.00"), "VND", AllocationStrategy.OWNERSHIP_BASED,
                    LocalDate.now(), null, null, false
            );

            when(userRepository.findById(10L)).thenReturn(Optional.of(coOwnerUser));
            when(ownershipGroupRepository.findById(1L)).thenReturn(Optional.of(group));
            when(ownershipShareRepository.findByGroupIdAndUserId(1L, 10L)).thenReturn(Optional.of(share1));
            when(vehicleRepository.findById(100L)).thenReturn(Optional.of(vehicle));
            when(sharedFundRepository.findByGroupId(1L)).thenReturn(Optional.of(sharedFund));

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> expenseService.createExpense(request, 10L, "127.0.0.1"));
            assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
            assertTrue(ex.getMessage().contains("evidence is strictly required"));
        }

        @Test
        @DisplayName("Should reject high-value expense (>= 1,000,000 VND) without evidence even for OTHER")
        void shouldRejectHighValueWithoutEvidence() {
            CreateExpenseRequest request = new CreateExpenseRequest(
                    1L, 100L, "Major Hardware Upgrade", ExpenseCategory.OTHER,
                    new BigDecimal("1500000.00"), "VND", AllocationStrategy.OWNERSHIP_BASED,
                    LocalDate.now(), null, null, false
            );

            when(userRepository.findById(10L)).thenReturn(Optional.of(coOwnerUser));
            when(ownershipGroupRepository.findById(1L)).thenReturn(Optional.of(group));
            when(ownershipShareRepository.findByGroupIdAndUserId(1L, 10L)).thenReturn(Optional.of(share1));
            when(vehicleRepository.findById(100L)).thenReturn(Optional.of(vehicle));
            when(sharedFundRepository.findByGroupId(1L)).thenReturn(Optional.of(sharedFund));

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> expenseService.createExpense(request, 10L, "127.0.0.1"));
            assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
            assertTrue(ex.getMessage().contains("exceeding 1000000.00 VND"));
        }

        @Test
        @DisplayName("Should accept REPAIR with evidenceUrl")
        void shouldAcceptRepairWithEvidenceUrl() {
            CreateExpenseRequest request = new CreateExpenseRequest(
                    1L, 100L, "Windshield wiper replacement", ExpenseCategory.REPAIR,
                    new BigDecimal("450000.00"), "VND", AllocationStrategy.OWNERSHIP_BASED,
                    LocalDate.now(), null, "https://storage.evshare.io/invoices/wiper.pdf", false
            );

            when(userRepository.findById(10L)).thenReturn(Optional.of(coOwnerUser));
            when(ownershipGroupRepository.findById(1L)).thenReturn(Optional.of(group));
            when(ownershipShareRepository.findByGroupIdAndUserId(1L, 10L)).thenReturn(Optional.of(share1));
            when(vehicleRepository.findById(100L)).thenReturn(Optional.of(vehicle));
            when(sharedFundRepository.findByGroupId(1L)).thenReturn(Optional.of(sharedFund));
            when(expenseRepository.existsDuplicateExpense(any(), any(), any(), any(), any(), any())).thenReturn(false);
            when(expenseRepository.save(any(Expense.class))).thenAnswer(i -> {
                Expense e = i.getArgument(0);
                e.setId(502L);
                return e;
            });

            ExpenseResponse resp = expenseService.createExpense(request, 10L, "127.0.0.1");
            assertNotNull(resp);
            assertEquals(502L, resp.getId());
            assertTrue(resp.getEvidenceProvided());
        }

        @Test
        @DisplayName("Should verify support for all specified expense types")
        void shouldSupportAllExpenseTypes() {
            ExpenseCategory[] categories = {
                    ExpenseCategory.CHARGING,
                    ExpenseCategory.MAINTENANCE,
                    ExpenseCategory.INSURANCE,
                    ExpenseCategory.INSPECTION,
                    ExpenseCategory.CLEANING,
                    ExpenseCategory.REPAIR,
                    ExpenseCategory.PARKING,
                    ExpenseCategory.TOLL,
                    ExpenseCategory.OTHER
            };

            for (ExpenseCategory cat : categories) {
                CreateExpenseRequest req = new CreateExpenseRequest(
                        1L, 100L, "Test " + cat.name(), cat,
                        new BigDecimal("20000.00"), "VND", AllocationStrategy.OWNERSHIP_BASED,
                        LocalDate.now(), "INV-TEST-" + cat.name(), "https://evidence.png", false
                );

                when(userRepository.findById(10L)).thenReturn(Optional.of(coOwnerUser));
                when(ownershipGroupRepository.findById(1L)).thenReturn(Optional.of(group));
                when(ownershipShareRepository.findByGroupIdAndUserId(1L, 10L)).thenReturn(Optional.of(share1));
                when(vehicleRepository.findById(100L)).thenReturn(Optional.of(vehicle));
                when(sharedFundRepository.findByGroupId(1L)).thenReturn(Optional.of(sharedFund));
                when(expenseRepository.existsByInvoiceReference("INV-TEST-" + cat.name())).thenReturn(false);
                when(expenseRepository.existsDuplicateExpense(any(), any(), any(), any(), any(), any())).thenReturn(false);
                when(expenseRepository.save(any(Expense.class))).thenAnswer(i -> {
                    Expense e = i.getArgument(0);
                    e.setId(999L);
                    return e;
                });

                ExpenseResponse resp = expenseService.createExpense(req, 10L, "127.0.0.1");
                assertEquals(cat, resp.getCategory());
            }
        }
    }

    @Nested
    @DisplayName("Historical Queries & Audit Tests")
    class AuditAndQueryTests {

        @Test
        @DisplayName("Should retrieve group expenses paged")
        void shouldRetrieveGroupExpensesPaged() {
            Expense e1 = new Expense(1L, group, vehicle, "Toll", ExpenseCategory.TOLL,
                    new BigDecimal("35000.00"), "VND", AllocationStrategy.OWNERSHIP_BASED,
                    "T-01", null, coOwnerUser, LocalDate.now(), Instant.now());

            when(userRepository.findById(10L)).thenReturn(Optional.of(coOwnerUser));
            when(ownershipShareRepository.findByGroupIdAndUserId(1L, 10L)).thenReturn(Optional.of(share1));
            when(expenseRepository.findByGroupId(eq(1L), any(Pageable.class)))
                    .thenReturn(new PageImpl<>(List.of(e1), PageRequest.of(0, 10), 1));

            PagedData<ExpenseResponse> result = expenseService.getGroupExpenses(1L, null, null, null, PageRequest.of(0, 10), 10L);

            assertNotNull(result);
            assertEquals(1, result.getItems().size());
            assertEquals("Toll", result.getItems().get(0).getTitle());
        }

        @Test
        @DisplayName("Should retrieve expense history chronologically")
        void shouldRetrieveExpenseHistory() {
            Expense e1 = new Expense(1L, group, vehicle, "Parking", ExpenseCategory.PARKING,
                    new BigDecimal("50000.00"), "VND", AllocationStrategy.OWNERSHIP_BASED,
                    null, null, coOwnerUser, LocalDate.now(), Instant.now());

            AuditLog log1 = new AuditLog();
            log1.setId(201L);
            log1.setAction("EXPENSE_CREATED");
            log1.setEntityName("Expense");
            log1.setEntityId(1L);
            log1.setUser(coOwnerUser);
            log1.setNewStateJson("{\"amount\": 50000.00}");
            log1.setCreatedAt(Instant.now());

            when(expenseRepository.findById(1L)).thenReturn(Optional.of(e1));
            when(userRepository.findById(10L)).thenReturn(Optional.of(coOwnerUser));
            when(ownershipShareRepository.findByGroupIdAndUserId(1L, 10L)).thenReturn(Optional.of(share1));
            when(auditLogRepository.findByEntityNameAndEntityIdOrderByIdDesc("Expense", 1L))
                    .thenReturn(List.of(log1));

            List<ExpenseAuditLogResponse> history = expenseService.getExpenseHistory(1L, 10L);

            assertNotNull(history);
            assertEquals(1, history.size());
            assertEquals("EXPENSE_CREATED", history.get(0).getAction());
            assertEquals(1L, history.get(0).getExpenseId());
        }
    }
}
