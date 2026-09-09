package com.example.evshare.controller;

import com.example.evshare.dto.request.CreateExpenseRequest;
import com.example.evshare.entity.*;
import com.example.evshare.entity.enums.AllocationStrategy;
import com.example.evshare.entity.enums.ExpenseCategory;
import com.example.evshare.entity.enums.RoleName;
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
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@DisplayName("Checkpoint 06-C — Expense Business Validation & Duplicate Detection Integration Tests")
class ExpenseIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @Autowired private UserRepository userRepository;
    @Autowired private RoleRepository roleRepository;
    @Autowired private VehicleRepository vehicleRepository;
    @Autowired private OwnershipGroupRepository ownershipGroupRepository;
    @Autowired private OwnershipShareRepository ownershipShareRepository;
    @Autowired private SharedFundRepository sharedFundRepository;
    @Autowired private ExpenseRepository expenseRepository;
    @Autowired private AuditLogRepository auditLogRepository;

    private static final java.util.concurrent.atomic.AtomicLong PHONE_SEQ = new java.util.concurrent.atomic.AtomicLong(System.currentTimeMillis() % 80000000L + 10000000L);

    private User coOwner;
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

        coOwner = createTestUser("expense.owner." + UUID.randomUUID() + "@evshare.io", "Nguyen Owner", roleCoOwner);
        outsiderUser = createTestUser("expense.outsider." + UUID.randomUUID() + "@evshare.io", "Le Outsider", roleCoOwner);
        staffUser = createTestUser("expense.staff." + UUID.randomUUID() + "@evshare.io", "Tran Staff", roleStaff);

        testVehicle = new Vehicle();
        testVehicle.setVin("VIN" + UUID.randomUUID().toString().replace("-", "").substring(0, 14).toUpperCase());
        testVehicle.setLicensePlate("51H-" + UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase());
        testVehicle.setModelName("VinFast VF8 Eco");
        testVehicle.setManufacturer("VinFast");
        testVehicle.setModel3dAssetPath("models/vehicles/vf8.glb");
        testVehicle.setStatus(VehicleStatus.AVAILABLE);
        testVehicle.setBatteryLevel(90);
        testVehicle.setOdometerKm(new BigDecimal("12000.00"));
        testVehicle.setStallLocationCode("BAY-01");
        testVehicle = vehicleRepository.saveAndFlush(testVehicle);

        testGroup = new OwnershipGroup();
        testGroup.setGroupName("Syndicate " + UUID.randomUUID().toString().substring(0, 8));
        testGroup.setVehicle(testVehicle);
        testGroup.setFormationDate(LocalDate.of(2026, 1, 1));
        testGroup.setIsActive(true);
        testGroup = ownershipGroupRepository.saveAndFlush(testGroup);

        OwnershipShare share = new OwnershipShare();
        share.setGroup(testGroup);
        share.setUser(coOwner);
        share.setPercentage(new BigDecimal("100.00"));
        share.setShareCertificateNumber("CERT-" + UUID.randomUUID().toString().substring(0, 8));
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
        u.setPasswordHash("$2a$12$dummyPasswordHashForSessionTesting12345");
        u.setFullName(fullName);
        u.setPhoneNumber(String.format("09%08d", PHONE_SEQ.incrementAndGet() % 100000000L));
        u.setIsActive(true);
        u.getRoles().add(role);
        return userRepository.saveAndFlush(u);
    }

    @Test
    @DisplayName("POST /api/v1/expenses: Co-owner creates valid CHARGING expense (allocations deferred)")
    void testCreateExpense_Success_CoOwner() throws Exception {
        CreateExpenseRequest request = new CreateExpenseRequest(
                testGroup.getId(), testVehicle.getId(), "Vincom Mega Mall Fast Charging",
                ExpenseCategory.CHARGING, new BigDecimal("150000.00"), "VND",
                AllocationStrategy.OWNERSHIP_BASED, LocalDate.now(), "INV-CHARGING-01", null, false
        );

        mockMvc.perform(post("/api/v1/expenses")
                        .with(user(UserPrincipal.create(coOwner)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.title", is("Vincom Mega Mall Fast Charging")))
                .andExpect(jsonPath("$.data.category", is("CHARGING")))
                .andExpect(jsonPath("$.data.amount", is(150000.00)))
                .andExpect(jsonPath("$.data.currency", is("VND")))
                // Checkpoint 06-C: "Do not allocate costs yet"
                .andExpect(jsonPath("$.data.allocations", hasSize(0)));
    }

    @Test
    @DisplayName("POST /api/v1/expenses: Staff creates valid INSPECTION expense with invoice")
    void testCreateExpense_Success_Staff() throws Exception {
        CreateExpenseRequest request = new CreateExpenseRequest(
                testGroup.getId(), testVehicle.getId(), "Quarterly Safety Inspection",
                ExpenseCategory.INSPECTION, new BigDecimal("500000.00"), "VND",
                AllocationStrategy.OWNERSHIP_BASED, LocalDate.now(), "INSPECT-2026-Q3", null, false
        );

        mockMvc.perform(post("/api/v1/expenses")
                        .with(user(UserPrincipal.create(staffUser)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.category", is("INSPECTION")))
                .andExpect(jsonPath("$.data.amount", is(500000.00)))
                .andExpect(jsonPath("$.data.allocations", hasSize(0)));
    }

    @Test
    @DisplayName("POST /api/v1/expenses: Duplicate invoice reference rejected with 409 Conflict")
    void testCreateExpense_DuplicateInvoiceReference_Conflict() throws Exception {
        CreateExpenseRequest request1 = new CreateExpenseRequest(
                testGroup.getId(), testVehicle.getId(), "First Charge",
                ExpenseCategory.CHARGING, new BigDecimal("150000.00"), "VND",
                AllocationStrategy.OWNERSHIP_BASED, LocalDate.now(), "INV-UNIQUE-REF-01", null, false
        );

        mockMvc.perform(post("/api/v1/expenses")
                        .with(user(UserPrincipal.create(coOwner)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request1)))
                .andExpect(status().isCreated());

        CreateExpenseRequest request2 = new CreateExpenseRequest(
                testGroup.getId(), testVehicle.getId(), "Second Charge with Same Invoice",
                ExpenseCategory.CHARGING, new BigDecimal("200000.00"), "VND",
                AllocationStrategy.OWNERSHIP_BASED, LocalDate.now(), "INV-UNIQUE-REF-01", null, false
        );

        mockMvc.perform(post("/api/v1/expenses")
                        .with(user(UserPrincipal.create(coOwner)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request2)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message", containsString("Duplicate expense detected")));
    }

    @Test
    @DisplayName("POST /api/v1/expenses: Duplicate identical expense rejected with 409 Conflict")
    void testCreateExpense_DuplicateIdenticalExpense_Conflict() throws Exception {
        CreateExpenseRequest request1 = new CreateExpenseRequest(
                testGroup.getId(), testVehicle.getId(), "Toll Fee Cau Sai Gon",
                ExpenseCategory.TOLL, new BigDecimal("35000.00"), "VND",
                AllocationStrategy.OWNERSHIP_BASED, LocalDate.now(), null, null, false
        );

        mockMvc.perform(post("/api/v1/expenses")
                        .with(user(UserPrincipal.create(coOwner)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request1)))
                .andExpect(status().isCreated());

        CreateExpenseRequest request2 = new CreateExpenseRequest(
                testGroup.getId(), testVehicle.getId(), "Toll Fee Cau Sai Gon",
                ExpenseCategory.TOLL, new BigDecimal("35000.00"), "VND",
                AllocationStrategy.OWNERSHIP_BASED, LocalDate.now(), null, null, false
        );

        mockMvc.perform(post("/api/v1/expenses")
                        .with(user(UserPrincipal.create(coOwner)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request2)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message", containsString("Duplicate expense detected")));
    }

    @Test
    @DisplayName("POST /api/v1/expenses: Negative amount rejected with 400 Bad Request")
    void testCreateExpense_NegativeAmount_BadRequest() throws Exception {
        CreateExpenseRequest request = new CreateExpenseRequest(
                testGroup.getId(), testVehicle.getId(), "Negative charge",
                ExpenseCategory.CHARGING, new BigDecimal("-50000.00"), "VND",
                AllocationStrategy.OWNERSHIP_BASED, LocalDate.now(), null, null, false
        );

        mockMvc.perform(post("/api/v1/expenses")
                        .with(user(UserPrincipal.create(coOwner)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /api/v1/expenses: Zero amount rejected with 400 Bad Request")
    void testCreateExpense_ZeroAmount_BadRequest() throws Exception {
        CreateExpenseRequest request = new CreateExpenseRequest(
                testGroup.getId(), testVehicle.getId(), "Zero charge",
                ExpenseCategory.CHARGING, BigDecimal.ZERO, "VND",
                AllocationStrategy.OWNERSHIP_BASED, LocalDate.now(), null, null, false
        );

        mockMvc.perform(post("/api/v1/expenses")
                        .with(user(UserPrincipal.create(coOwner)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /api/v1/expenses: Future incurred date rejected with 400 Bad Request")
    void testCreateExpense_FutureDate_BadRequest() throws Exception {
        CreateExpenseRequest request = new CreateExpenseRequest(
                testGroup.getId(), testVehicle.getId(), "Future charge",
                ExpenseCategory.CHARGING, new BigDecimal("100000.00"), "VND",
                AllocationStrategy.OWNERSHIP_BASED, LocalDate.now().plusDays(3), null, null, false
        );

        mockMvc.perform(post("/api/v1/expenses")
                        .with(user(UserPrincipal.create(coOwner)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("cannot be in the future")));
    }

    @Test
    @DisplayName("POST /api/v1/expenses: Date before group formation rejected with 400 Bad Request")
    void testCreateExpense_DateBeforeFormation_BadRequest() throws Exception {
        CreateExpenseRequest request = new CreateExpenseRequest(
                testGroup.getId(), testVehicle.getId(), "Pre-formation expense",
                ExpenseCategory.PARKING, new BigDecimal("50000.00"), "VND",
                AllocationStrategy.OWNERSHIP_BASED, LocalDate.of(2025, 12, 1), null, null, false
        );

        mockMvc.perform(post("/api/v1/expenses")
                        .with(user(UserPrincipal.create(coOwner)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("cannot precede group formation date")));
    }

    @Test
    @DisplayName("POST /api/v1/expenses: Mismatched vehicle rejected with 400 Bad Request")
    void testCreateExpense_MismatchedVehicle_BadRequest() throws Exception {
        Vehicle otherVehicle = new Vehicle();
        otherVehicle.setVin("VIN" + UUID.randomUUID().toString().replace("-", "").substring(0, 14).toUpperCase());
        otherVehicle.setLicensePlate("51H-" + UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase());
        otherVehicle.setModelName("VinFast VF9");
        otherVehicle.setManufacturer("VinFast");
        otherVehicle.setModel3dAssetPath("models/vehicles/vf9.glb");
        otherVehicle = vehicleRepository.saveAndFlush(otherVehicle);

        CreateExpenseRequest request = new CreateExpenseRequest(
                testGroup.getId(), otherVehicle.getId(), "Wrong car charging",
                ExpenseCategory.CHARGING, new BigDecimal("100000.00"), "VND",
                AllocationStrategy.OWNERSHIP_BASED, LocalDate.now(), null, null, false
        );

        mockMvc.perform(post("/api/v1/expenses")
                        .with(user(UserPrincipal.create(coOwner)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("does not belong to syndicate group")));
    }

    @Test
    @DisplayName("POST /api/v1/expenses: MAINTENANCE without evidence rejected with 400 Bad Request")
    void testCreateExpense_MaintenanceMissingEvidence_BadRequest() throws Exception {
        CreateExpenseRequest request = new CreateExpenseRequest(
                testGroup.getId(), testVehicle.getId(), "Suspension check",
                ExpenseCategory.MAINTENANCE, new BigDecimal("300000.00"), "VND",
                AllocationStrategy.OWNERSHIP_BASED, LocalDate.now(), null, null, false
        );

        mockMvc.perform(post("/api/v1/expenses")
                        .with(user(UserPrincipal.create(coOwner)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("evidence is strictly required")));
    }

    @Test
    @DisplayName("POST /api/v1/expenses: INSURANCE without evidence rejected with 400 Bad Request")
    void testCreateExpense_InsuranceMissingEvidence_BadRequest() throws Exception {
        CreateExpenseRequest request = new CreateExpenseRequest(
                testGroup.getId(), testVehicle.getId(), "Vehicle Insurance",
                ExpenseCategory.INSURANCE, new BigDecimal("400000.00"), "VND",
                AllocationStrategy.OWNERSHIP_BASED, LocalDate.now(), null, null, false
        );

        mockMvc.perform(post("/api/v1/expenses")
                        .with(user(UserPrincipal.create(coOwner)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("evidence is strictly required")));
    }

    @Test
    @DisplayName("POST /api/v1/expenses: High-value expense (> 1,000,000 VND) without evidence rejected")
    void testCreateExpense_HighValueMissingEvidence_BadRequest() throws Exception {
        CreateExpenseRequest request = new CreateExpenseRequest(
                testGroup.getId(), testVehicle.getId(), "Sound system upgrade",
                ExpenseCategory.OTHER, new BigDecimal("2500000.00"), "VND",
                AllocationStrategy.OWNERSHIP_BASED, LocalDate.now(), null, null, false
        );

        mockMvc.perform(post("/api/v1/expenses")
                        .with(user(UserPrincipal.create(coOwner)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("exceeding 1000000.00 VND")));
    }

    @Test
    @DisplayName("POST /api/v1/expenses: Unauthorized co-owner rejected with 403 Forbidden")
    void testCreateExpense_UnauthorizedCoOwner_Forbidden() throws Exception {
        CreateExpenseRequest request = new CreateExpenseRequest(
                testGroup.getId(), testVehicle.getId(), "Outsider charge",
                ExpenseCategory.CHARGING, new BigDecimal("100000.00"), "VND",
                AllocationStrategy.OWNERSHIP_BASED, LocalDate.now(), null, null, false
        );

        mockMvc.perform(post("/api/v1/expenses")
                        .with(user(UserPrincipal.create(outsiderUser)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /api/v1/expenses/group/{groupId}: List syndicate expenses paged")
    void testGetGroupExpenses_Success() throws Exception {
        // Record an expense first
        Expense expense = new Expense();
        expense.setGroup(testGroup);
        expense.setVehicle(testVehicle);
        expense.setTitle("Monthly Car Wash");
        expense.setCategory(ExpenseCategory.CLEANING);
        expense.setTotalAmount(new BigDecimal("120000.00"));
        expense.setCurrency("VND");
        expense.setAllocationStrategy(AllocationStrategy.OWNERSHIP_BASED);
        expense.setLoggedByUser(coOwner);
        expense.setIncurredDate(LocalDate.now());
        expenseRepository.saveAndFlush(expense);

        mockMvc.perform(get("/api/v1/expenses/group/" + testGroup.getId())
                        .with(user(UserPrincipal.create(coOwner))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.items", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$.data.items[0].title", is("Monthly Car Wash")));
    }

    @Test
    @DisplayName("GET /api/v1/expenses/{id}/history: Retrieve chronological immutable audit trail")
    void testGetExpenseHistory_Success() throws Exception {
        // Create an expense via API to trigger audit log recording
        CreateExpenseRequest request = new CreateExpenseRequest(
                testGroup.getId(), testVehicle.getId(), "Toll Fee Highway 51",
                ExpenseCategory.TOLL, new BigDecimal("35000.00"), "VND",
                AllocationStrategy.OWNERSHIP_BASED, LocalDate.now(), "TOLL-01", null, false
        );

        String jsonResponse = mockMvc.perform(post("/api/v1/expenses")
                        .with(user(UserPrincipal.create(coOwner)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        Long expenseId = objectMapper.readTree(jsonResponse).path("data").path("id").asLong();

        mockMvc.perform(get("/api/v1/expenses/" + expenseId + "/history")
                        .with(user(UserPrincipal.create(coOwner))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].action", is("EXPENSE_CREATED")))
                .andExpect(jsonPath("$.data[0].expenseId", is(expenseId.intValue())));
    }
}
