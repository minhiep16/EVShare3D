package com.example.evshare.controller;

import com.example.evshare.dto.request.UpdateVehicleStatusRequest;
import com.example.evshare.dto.response.VehicleResponse;
import com.example.evshare.entity.Role;
import com.example.evshare.entity.User;
import com.example.evshare.entity.Vehicle;
import com.example.evshare.entity.enums.RoleName;
import com.example.evshare.entity.enums.VehicleStatus;
import com.example.evshare.exception.InvalidStateTransitionException;
import com.example.evshare.repository.RoleRepository;
import com.example.evshare.repository.UserRepository;
import com.example.evshare.repository.VehicleRepository;
import com.example.evshare.security.TokenService;
import com.example.evshare.service.VehicleService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("Checkpoint 04-D — Vehicle State Machine Integration & Concurrency Tests")
class VehicleStateTransitionIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private VehicleRepository vehicleRepository;

    @Autowired
    private VehicleService vehicleService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private TokenService tokenService;

    private String adminToken;
    private String staffToken;
    private String coOwnerToken;

    private Role adminRole;
    private Role staffRole;
    private Role coOwnerRole;

    private Vehicle createVehicle(String vin, String plate, VehicleStatus initialStatus) {
        Vehicle vehicle = new Vehicle();
        vehicle.setVin(vin);
        vehicle.setLicensePlate(plate);
        vehicle.setModelName("Tesla Model Y Dual Motor");
        vehicle.setManufacturer("Tesla");
        vehicle.setModel3dAssetPath("models/vehicles/tesla_model_y.glb");
        vehicle.setStatus(initialStatus);
        vehicle.setBatteryLevel(90);
        vehicle.setOdometerKm(new BigDecimal("5000.00"));
        vehicle.setStallLocationCode("BAY-02");
        return vehicleRepository.saveAndFlush(vehicle);
    }

    private User createUserWithRole(String email, Role role) {
        User user = new User();
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode("Password123!"));
        user.setFullName("Test User " + role.getName().name());
        user.setIsActive(true);
        user.setRoles(new HashSet<>(Collections.singletonList(role)));
        return userRepository.saveAndFlush(user);
    }

    @BeforeEach
    void setUp() {
        adminRole = roleRepository.findByName(RoleName.ROLE_ADMIN)
                .orElseGet(() -> roleRepository.save(new Role(RoleName.ROLE_ADMIN)));
        staffRole = roleRepository.findByName(RoleName.ROLE_STAFF)
                .orElseGet(() -> roleRepository.save(new Role(RoleName.ROLE_STAFF)));
        coOwnerRole = roleRepository.findByName(RoleName.ROLE_CO_OWNER)
                .orElseGet(() -> roleRepository.save(new Role(RoleName.ROLE_CO_OWNER)));

        String uid = UUID.randomUUID().toString().substring(0, 8);
        User adminUser = createUserWithRole("admin_" + uid + "@evshare.vn", adminRole);
        User staffUser = createUserWithRole("staff_" + uid + "@evshare.vn", staffRole);
        User coOwnerUser = createUserWithRole("owner_" + uid + "@evshare.vn", coOwnerRole);

        adminToken = tokenService.generateAccessToken(adminUser.getId(), adminUser.getEmail(), Collections.singletonList("ROLE_ADMIN"));
        staffToken = tokenService.generateAccessToken(staffUser.getId(), staffUser.getEmail(), Collections.singletonList("ROLE_STAFF"));
        coOwnerToken = tokenService.generateAccessToken(coOwnerUser.getId(), coOwnerUser.getEmail(), Collections.singletonList("ROLE_CO_OWNER"));
    }

    // =========================================================================
    // 1. VALID TRANSITIONS TEST SUITE
    // =========================================================================

    @Test
    @Transactional
    @DisplayName("1.1. Valid Transitions Lifecycle: AVAILABLE -> BOOKED -> IN_USE -> CHARGING -> AVAILABLE")
    void testValidOperationalLifecycle() throws Exception {
        Vehicle vehicle = createVehicle("VIN_VAL_" + UUID.randomUUID().toString().substring(0, 8), "51G-11111", VehicleStatus.AVAILABLE);

        // AVAILABLE -> BOOKED
        UpdateVehicleStatusRequest reqBooked = new UpdateVehicleStatusRequest(VehicleStatus.BOOKED, "User reservation confirmed");
        mockMvc.perform(patch("/api/v1/vehicles/{id}/status", vehicle.getId())
                        .header("Authorization", "Bearer " + staffToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reqBooked)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.status", is("BOOKED")));

        // BOOKED -> IN_USE
        UpdateVehicleStatusRequest reqInUse = new UpdateVehicleStatusRequest(VehicleStatus.IN_USE, "Co-owner QR check-in verified");
        mockMvc.perform(patch("/api/v1/vehicles/{id}/status", vehicle.getId())
                        .header("Authorization", "Bearer " + staffToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reqInUse)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status", is("IN_USE")));

        // IN_USE -> CHARGING
        UpdateVehicleStatusRequest reqCharging = new UpdateVehicleStatusRequest(VehicleStatus.CHARGING, "Returned with low battery; plugged into Stall 2");
        mockMvc.perform(patch("/api/v1/vehicles/{id}/status", vehicle.getId())
                        .header("Authorization", "Bearer " + staffToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reqCharging)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status", is("CHARGING")));

        // CHARGING -> AVAILABLE
        UpdateVehicleStatusRequest reqAvailable = new UpdateVehicleStatusRequest(VehicleStatus.AVAILABLE, "Charging reached 100%; unplugged");
        mockMvc.perform(patch("/api/v1/vehicles/{id}/status", vehicle.getId())
                        .header("Authorization", "Bearer " + staffToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reqAvailable)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status", is("AVAILABLE")));
    }

    @Test
    @Transactional
    @DisplayName("1.2. Valid Incident Lifecycle: IN_USE -> DAMAGED -> MAINTENANCE -> AVAILABLE")
    void testIncidentAndRepairLifecycle() throws Exception {
        Vehicle vehicle = createVehicle("VIN_INC_" + UUID.randomUUID().toString().substring(0, 8), "51G-22222", VehicleStatus.IN_USE);

        // IN_USE -> DAMAGED (Inspection flagged defect)
        UpdateVehicleStatusRequest reqDamaged = new UpdateVehicleStatusRequest(VehicleStatus.DAMAGED, "Scratch and dent reported on rear bumper");
        mockMvc.perform(patch("/api/v1/vehicles/{id}/status", vehicle.getId())
                        .header("Authorization", "Bearer " + staffToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reqDamaged)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status", is("DAMAGED")));

        // DAMAGED -> MAINTENANCE (Moved to repair shop)
        UpdateVehicleStatusRequest reqMaint = new UpdateVehicleStatusRequest(VehicleStatus.MAINTENANCE, "Work order created at garage");
        mockMvc.perform(patch("/api/v1/vehicles/{id}/status", vehicle.getId())
                        .header("Authorization", "Bearer " + staffToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reqMaint)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status", is("MAINTENANCE")));

        // MAINTENANCE -> AVAILABLE (Repairs complete and certified)
        UpdateVehicleStatusRequest reqAvail = new UpdateVehicleStatusRequest(VehicleStatus.AVAILABLE, "Bodywork repaired and inspected");
        mockMvc.perform(patch("/api/v1/vehicles/{id}/status", vehicle.getId())
                        .header("Authorization", "Bearer " + staffToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reqAvail)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status", is("AVAILABLE")));
    }

    @Test
    @Transactional
    @DisplayName("1.3. Valid Admin Lockout: AVAILABLE -> UNAVAILABLE -> AVAILABLE")
    void testAdminLockoutLifecycle() throws Exception {
        Vehicle vehicle = createVehicle("VIN_ADM_" + UUID.randomUUID().toString().substring(0, 8), "51G-33333", VehicleStatus.AVAILABLE);

        // AVAILABLE -> UNAVAILABLE (Admin lockout)
        UpdateVehicleStatusRequest reqLock = new UpdateVehicleStatusRequest(VehicleStatus.UNAVAILABLE, "Administrative recall / software update");
        mockMvc.perform(patch("/api/v1/vehicles/{id}/status", vehicle.getId())
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reqLock)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status", is("UNAVAILABLE")));

        // UNAVAILABLE -> AVAILABLE (Admin unlock)
        UpdateVehicleStatusRequest reqUnlock = new UpdateVehicleStatusRequest(VehicleStatus.AVAILABLE, "Administrative release");
        mockMvc.perform(patch("/api/v1/vehicles/{id}/status", vehicle.getId())
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reqUnlock)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status", is("AVAILABLE")));
    }

    // =========================================================================
    // 2. INVALID TRANSITIONS TEST SUITE
    // =========================================================================

    @Test
    @Transactional
    @DisplayName("2.1. Invalid Transition: AVAILABLE -> IN_USE is rejected with HTTP 409 Conflict")
    void testDirectAvailableToInUseRejected() throws Exception {
        Vehicle vehicle = createVehicle("VIN_INV1_" + UUID.randomUUID().toString().substring(0, 8), "51G-44444", VehicleStatus.AVAILABLE);

        UpdateVehicleStatusRequest req = new UpdateVehicleStatusRequest(VehicleStatus.IN_USE, "Illegal bypass without booking");
        mockMvc.perform(patch("/api/v1/vehicles/{id}/status", vehicle.getId())
                        .header("Authorization", "Bearer " + staffToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status", is(409)))
                .andExpect(jsonPath("$.message", org.hamcrest.Matchers.containsString("Cannot transition vehicle from status 'AVAILABLE' to 'IN_USE'")));

        // Assert database state unchanged (transactional integrity)
        Vehicle current = vehicleRepository.findById(vehicle.getId()).orElseThrow();
        assertEquals(VehicleStatus.AVAILABLE, current.getStatus());
    }

    @Test
    @Transactional
    @DisplayName("2.2. Invalid Transition: DAMAGED -> AVAILABLE is rejected (cannot bypass maintenance)")
    void testDamagedToAvailableRejected() throws Exception {
        Vehicle vehicle = createVehicle("VIN_INV2_" + UUID.randomUUID().toString().substring(0, 8), "51G-55555", VehicleStatus.DAMAGED);

        UpdateVehicleStatusRequest req = new UpdateVehicleStatusRequest(VehicleStatus.AVAILABLE, "Premature release of damaged vehicle");
        mockMvc.perform(patch("/api/v1/vehicles/{id}/status", vehicle.getId())
                        .header("Authorization", "Bearer " + staffToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status", is(409)))
                .andExpect(jsonPath("$.message", org.hamcrest.Matchers.containsString("Cannot transition vehicle from status 'DAMAGED' to 'AVAILABLE'")));

        Vehicle current = vehicleRepository.findById(vehicle.getId()).orElseThrow();
        assertEquals(VehicleStatus.DAMAGED, current.getStatus());
    }

    @Test
    @Transactional
    @DisplayName("2.3. Redundant Transition: AVAILABLE -> AVAILABLE is rejected with HTTP 409 Conflict")
    void testRedundantSelfTransitionRejected() throws Exception {
        Vehicle vehicle = createVehicle("VIN_INV3_" + UUID.randomUUID().toString().substring(0, 8), "51G-66666", VehicleStatus.AVAILABLE);

        UpdateVehicleStatusRequest req = new UpdateVehicleStatusRequest(VehicleStatus.AVAILABLE, "Redundant no-op request");
        mockMvc.perform(patch("/api/v1/vehicles/{id}/status", vehicle.getId())
                        .header("Authorization", "Bearer " + staffToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status", is(409)))
                .andExpect(jsonPath("$.message", org.hamcrest.Matchers.containsString("already in status 'AVAILABLE'")));
    }

    // =========================================================================
    // 3. UNAUTHORIZED TRANSITIONS TEST SUITE
    // =========================================================================

    @Test
    @Transactional
    @DisplayName("3.1. Unauthorized: Unauthenticated request returns HTTP 401 Unauthorized")
    void testUnauthenticatedTransitionRejected() throws Exception {
        Vehicle vehicle = createVehicle("VIN_UNAUTH1_" + UUID.randomUUID().toString().substring(0, 8), "51G-77777", VehicleStatus.AVAILABLE);

        UpdateVehicleStatusRequest req = new UpdateVehicleStatusRequest(VehicleStatus.MAINTENANCE);
        mockMvc.perform(patch("/api/v1/vehicles/{id}/status", vehicle.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @Transactional
    @DisplayName("3.2. Unauthorized: CO_OWNER role returns HTTP 403 Forbidden for vehicle status mutation")
    void testCoOwnerRoleForbidden() throws Exception {
        Vehicle vehicle = createVehicle("VIN_UNAUTH2_" + UUID.randomUUID().toString().substring(0, 8), "51G-88888", VehicleStatus.AVAILABLE);

        UpdateVehicleStatusRequest req = new UpdateVehicleStatusRequest(VehicleStatus.MAINTENANCE, "Owner attempt to take offline");
        mockMvc.perform(patch("/api/v1/vehicles/{id}/status", vehicle.getId())
                        .header("Authorization", "Bearer " + coOwnerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isForbidden());
    }

    @Test
    @Transactional
    @DisplayName("3.3. Unauthorized: STAFF attempting to set UNAVAILABLE returns HTTP 403 (ADMIN only)")
    void testStaffAttemptingUnavailableForbidden() throws Exception {
        Vehicle vehicle = createVehicle("VIN_UNAUTH3_" + UUID.randomUUID().toString().substring(0, 8), "51G-99999", VehicleStatus.AVAILABLE);

        UpdateVehicleStatusRequest req = new UpdateVehicleStatusRequest(VehicleStatus.UNAVAILABLE, "Staff attempt to lock fleet");
        mockMvc.perform(patch("/api/v1/vehicles/{id}/status", vehicle.getId())
                        .header("Authorization", "Bearer " + staffToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status", is(403)))
                .andExpect(jsonPath("$.message", org.hamcrest.Matchers.containsString("Access denied")));
    }

    // =========================================================================
    // 4. CONCURRENT UPDATE CONCERNS (RACE CONDITIONS & TRANSACTIONAL LOCKING)
    // =========================================================================

    @Test
    @DisplayName("4.1. Concurrency: Pessimistic locking prevents conflicting transitions under race condition")
    void testConcurrentTransitionRaceCondition() throws Exception {
        // Create vehicle outside of test transaction so it is visible to concurrent worker threads
        Vehicle vehicle = createVehicle("VIN_CONC_" + UUID.randomUUID().toString().substring(0, 8), "51G-00000", VehicleStatus.AVAILABLE);
        Long vehicleId = vehicle.getId();

        int numThreads = 2;
        ExecutorService executor = Executors.newFixedThreadPool(numThreads);
        CountDownLatch readyLatch = new CountDownLatch(numThreads);
        CountDownLatch startLatch = new CountDownLatch(1);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger conflictCount = new AtomicInteger(0);

        // Thread 1: Wants to transition AVAILABLE -> BOOKED
        // Thread 2: Wants to transition AVAILABLE -> MAINTENANCE
        // Both are valid from AVAILABLE, but once the first commits, the second must see the updated state.
        // If Thread 1 wins (state becomes BOOKED), Thread 2's target (MAINTENANCE) is checked from BOOKED (which is valid: BOOKED -> MAINTENANCE).
        // Let's create two mutually incompatible targets:
        // Thread A wants AVAILABLE -> CHARGING
        // Thread B wants AVAILABLE -> BOOKED
        // If Thread B wins, state is BOOKED. From BOOKED, CHARGING is INVALID!
        Callable<Void> taskBooking = () -> {
            SecurityContextHolder.getContext().setAuthentication(
                    new UsernamePasswordAuthenticationToken("staff", null, Collections.singletonList(new SimpleGrantedAuthority("ROLE_STAFF")))
            );
            readyLatch.countDown();
            startLatch.await();
            try {
                vehicleService.updateVehicleStatus(vehicleId, new UpdateVehicleStatusRequest(VehicleStatus.BOOKED, "Concurrent Booking"));
                successCount.incrementAndGet();
            } catch (InvalidStateTransitionException ex) {
                conflictCount.incrementAndGet();
            }
            return null;
        };

        Callable<Void> taskCharging = () -> {
            SecurityContextHolder.getContext().setAuthentication(
                    new UsernamePasswordAuthenticationToken("staff", null, Collections.singletonList(new SimpleGrantedAuthority("ROLE_STAFF")))
            );
            readyLatch.countDown();
            startLatch.await();
            try {
                vehicleService.updateVehicleStatus(vehicleId, new UpdateVehicleStatusRequest(VehicleStatus.CHARGING, "Concurrent Charging"));
                successCount.incrementAndGet();
            } catch (InvalidStateTransitionException ex) {
                conflictCount.incrementAndGet();
            }
            return null;
        };

        Future<Void> future1 = executor.submit(taskBooking);
        Future<Void> future2 = executor.submit(taskCharging);

        readyLatch.await(5, TimeUnit.SECONDS);
        startLatch.countDown(); // Fire both threads simultaneously

        future1.get(10, TimeUnit.SECONDS);
        future2.get(10, TimeUnit.SECONDS);
        executor.shutdown();

        // One of the transitions MUST succeed, and if the other arrives when vehicle is in the other state:
        // Case 1: BOOKED wins -> CHARGING fails (BOOKED -> CHARGING is invalid). (1 success, 1 conflict)
        // Case 2: CHARGING wins -> BOOKED fails (CHARGING -> BOOKED is invalid). (1 success, 1 conflict)
        // In either case, exactly 1 succeeds and exactly 1 conflict is recorded, guaranteeing atomic isolation!
        assertEquals(1, successCount.get(), "Exactly one concurrent transition must succeed");
        assertEquals(1, conflictCount.get(), "Conflicting concurrent transition must be rejected by pessimistic lock validation");

        // Verify final state in database is consistent (either BOOKED or CHARGING)
        Vehicle finalVehicle = vehicleRepository.findById(vehicleId).orElseThrow();
        assertTrue(finalVehicle.getStatus() == VehicleStatus.BOOKED || finalVehicle.getStatus() == VehicleStatus.CHARGING);

        // Clean up test vehicle
        vehicleRepository.deleteById(vehicleId);
    }
}
