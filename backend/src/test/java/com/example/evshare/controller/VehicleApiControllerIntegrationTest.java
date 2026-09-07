package com.example.evshare.controller;

import com.example.evshare.dto.request.CreateVehicleRequest;
import com.example.evshare.entity.Role;
import com.example.evshare.entity.User;
import com.example.evshare.entity.Vehicle;
import com.example.evshare.entity.enums.RoleName;
import com.example.evshare.entity.enums.VehicleStatus;
import com.example.evshare.repository.RoleRepository;
import com.example.evshare.repository.UserRepository;
import com.example.evshare.repository.VehicleRepository;
import com.example.evshare.security.TokenService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.HashSet;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("Checkpoint 04-L — Vehicle REST API Integration Tests")
class VehicleApiControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private VehicleRepository vehicleRepository;

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

    private String generateUniqueVin() {
        return "1HG" + UUID.randomUUID().toString().replace("-", "").substring(0, 14).toUpperCase();
    }

    private String generateUniquePlate() {
        return "P" + UUID.randomUUID().toString().replace("-", "").substring(0, 7).toUpperCase();
    }

    private User createUserWithRole(String email, Role role) {
        return userRepository.findByEmail(email).orElseGet(() -> {
            User user = new User();
            user.setEmail(email);
            user.setPasswordHash(passwordEncoder.encode("Password123!"));
            user.setFullName("Test User " + role.getName().name());
            user.setIsActive(true);
            user.setRoles(new HashSet<>(Collections.singletonList(role)));
            return userRepository.saveAndFlush(user);
        });
    }

    private Vehicle createVehicle(String vin, String plate, String manufacturer, String model, VehicleStatus status) {
        Vehicle vehicle = new Vehicle();
        vehicle.setVin(vin);
        vehicle.setLicensePlate(plate);
        vehicle.setModelName(model);
        vehicle.setManufacturer(manufacturer);
        vehicle.setModel3dAssetPath("models/vehicles/" + model.toLowerCase().replace(" ", "_") + ".glb");
        vehicle.setStatus(status);
        vehicle.setBatteryLevel(85);
        vehicle.setOdometerKm(new BigDecimal("12500.50"));
        vehicle.setStallLocationCode("BAY-E05");
        return vehicleRepository.saveAndFlush(vehicle);
    }

    @BeforeEach
    void setUp() {
        adminRole = roleRepository.findByName(RoleName.ROLE_ADMIN)
                .orElseGet(() -> roleRepository.saveAndFlush(new Role(RoleName.ROLE_ADMIN)));
        staffRole = roleRepository.findByName(RoleName.ROLE_STAFF)
                .orElseGet(() -> roleRepository.saveAndFlush(new Role(RoleName.ROLE_STAFF)));
        coOwnerRole = roleRepository.findByName(RoleName.ROLE_CO_OWNER)
                .orElseGet(() -> roleRepository.saveAndFlush(new Role(RoleName.ROLE_CO_OWNER)));

        String uid = UUID.randomUUID().toString().substring(0, 8);
        User adminUser = createUserWithRole("admin.veh." + uid + "@evshare.io", adminRole);
        User staffUser = createUserWithRole("staff.veh." + uid + "@evshare.io", staffRole);
        User coOwnerUser = createUserWithRole("owner.veh." + uid + "@evshare.io", coOwnerRole);

        adminToken = tokenService.generateAccessToken(adminUser);
        staffToken = tokenService.generateAccessToken(staffUser);
        coOwnerToken = tokenService.generateAccessToken(coOwnerUser);
    }

    // ==========================================
    // POST /api/v1/vehicles (Registration)
    // ==========================================

    @Test
    @DisplayName("Admin registers new vehicle successfully (201 Created)")
    void createVehicle_success_asAdmin() throws Exception {
        String uniqueVin = generateUniqueVin();
        String uniquePlate = generateUniquePlate();

        CreateVehicleRequest request = new CreateVehicleRequest();
        request.setVin(uniqueVin);
        request.setLicensePlate(uniquePlate);
        request.setModelName("Porsche Taycan 4S");
        request.setManufacturer("Porsche");
        request.setModel3dAssetPath("models/vehicles/porsche_taycan.glb");
        request.setBatteryLevel(95);
        request.setOdometerKm(new BigDecimal("230.00"));
        request.setStallLocationCode("BAY-P01");

        mockMvc.perform(post("/api/v1/vehicles")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.message", is("Vehicle registered successfully")))
                .andExpect(jsonPath("$.data.vin", is(uniqueVin)))
                .andExpect(jsonPath("$.data.licensePlate", is(uniquePlate)))
                .andExpect(jsonPath("$.data.modelName", is("Porsche Taycan 4S")))
                .andExpect(jsonPath("$.data.manufacturer", is("Porsche")))
                .andExpect(jsonPath("$.data.status", is("AVAILABLE")))
                .andExpect(jsonPath("$.data.batteryLevel", is(95)))
                .andExpect(jsonPath("$.data.stallLocationCode", is("BAY-P01")));
    }

    @Test
    @DisplayName("Co-owner cannot register a vehicle (403 Forbidden)")
    void createVehicle_forbidden_asCoOwner() throws Exception {
        CreateVehicleRequest request = new CreateVehicleRequest();
        request.setVin(generateUniqueVin());
        request.setLicensePlate(generateUniquePlate());
        request.setModelName("Tesla Model 3");
        request.setManufacturer("Tesla");
        request.setModel3dAssetPath("models/tesla.glb");

        mockMvc.perform(post("/api/v1/vehicles")
                        .header("Authorization", "Bearer " + coOwnerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Staff cannot register a vehicle (403 Forbidden)")
    void createVehicle_forbidden_asStaff() throws Exception {
        CreateVehicleRequest request = new CreateVehicleRequest();
        request.setVin(generateUniqueVin());
        request.setLicensePlate(generateUniquePlate());
        request.setModelName("Tesla Model 3");
        request.setManufacturer("Tesla");
        request.setModel3dAssetPath("models/tesla.glb");

        mockMvc.perform(post("/api/v1/vehicles")
                        .header("Authorization", "Bearer " + staffToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Unauthenticated request to register vehicle fails (401 Unauthorized)")
    void createVehicle_unauthorized() throws Exception {
        CreateVehicleRequest request = new CreateVehicleRequest();
        request.setVin(generateUniqueVin());
        request.setLicensePlate(generateUniquePlate());
        request.setModelName("Tesla Model 3");
        request.setManufacturer("Tesla");
        request.setModel3dAssetPath("models/tesla.glb");

        mockMvc.perform(post("/api/v1/vehicles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Reject vehicle registration with invalid VIN format (400 Bad Request)")
    void createVehicle_invalidVin() throws Exception {
        CreateVehicleRequest request = new CreateVehicleRequest();
        request.setVin("SHORT_VIN"); // Less than 17 chars
        request.setLicensePlate("30E-12345");
        request.setModelName("VinFast VF8");
        request.setManufacturer("VinFast");
        request.setModel3dAssetPath("models/vf8.glb");

        mockMvc.perform(post("/api/v1/vehicles")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success", is(false)));
    }

    @Test
    @DisplayName("Reject duplicate VIN (409 Conflict)")
    void createVehicle_duplicateVin() throws Exception {
        String existingVin = generateUniqueVin();
        createVehicle(existingVin, generateUniquePlate(), "Honda", "Honda e", VehicleStatus.AVAILABLE);

        CreateVehicleRequest request = new CreateVehicleRequest();
        request.setVin(existingVin);
        request.setLicensePlate(generateUniquePlate());
        request.setModelName("Honda e Advance");
        request.setManufacturer("Honda");
        request.setModel3dAssetPath("models/honda_e.glb");

        mockMvc.perform(post("/api/v1/vehicles")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success", is(false)));
    }

    @Test
    @DisplayName("Reject duplicate license plate (409 Conflict)")
    void createVehicle_duplicatePlate() throws Exception {
        String existingPlate = generateUniquePlate();
        createVehicle(generateUniqueVin(), existingPlate, "Hyundai", "Ioniq 5", VehicleStatus.AVAILABLE);

        CreateVehicleRequest request = new CreateVehicleRequest();
        request.setVin(generateUniqueVin());
        request.setLicensePlate(existingPlate);
        request.setModelName("Hyundai Ioniq 6");
        request.setManufacturer("Hyundai");
        request.setModel3dAssetPath("models/ioniq6.glb");

        mockMvc.perform(post("/api/v1/vehicles")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success", is(false)));
    }

    // ==========================================
    // GET /api/v1/vehicles (Paged & Filtered List)
    // ==========================================

    @Test
    @DisplayName("Authenticated user lists vehicles with pagination (200 OK)")
    void getVehicles_paginated_success() throws Exception {
        mockMvc.perform(get("/api/v1/vehicles")
                        .header("Authorization", "Bearer " + coOwnerToken)
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.items", notNullValue()))
                .andExpect(jsonPath("$.data.page", is(0)))
                .andExpect(jsonPath("$.data.size", is(10)))
                .andExpect(jsonPath("$.data.totalElements", greaterThanOrEqualTo(0)));
    }

    @Test
    @DisplayName("Filter vehicles by status (200 OK)")
    void getVehicles_filterByStatus() throws Exception {
        createVehicle(generateUniqueVin(), generateUniquePlate(), "Kia", "EV6", VehicleStatus.CHARGING);

        mockMvc.perform(get("/api/v1/vehicles")
                        .header("Authorization", "Bearer " + coOwnerToken)
                        .param("status", "CHARGING"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.items", not(empty())))
                .andExpect(jsonPath("$.data.items[*].status", everyItem(is("CHARGING"))));
    }

    @Test
    @DisplayName("Filter vehicles by manufacturer (200 OK, case-insensitive)")
    void getVehicles_filterByManufacturer() throws Exception {
        createVehicle(generateUniqueVin(), generateUniquePlate(), "Rivian", "R1T", VehicleStatus.AVAILABLE);

        mockMvc.perform(get("/api/v1/vehicles")
                        .header("Authorization", "Bearer " + coOwnerToken)
                        .param("manufacturer", "rivian"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.items", not(empty())))
                .andExpect(jsonPath("$.data.items[*].manufacturer", everyItem(is("Rivian"))));
    }

    @Test
    @DisplayName("Unauthenticated request to list vehicles fails (401 Unauthorized)")
    void getVehicles_unauthorized() throws Exception {
        mockMvc.perform(get("/api/v1/vehicles"))
                .andExpect(status().isUnauthorized());
    }

    // ==========================================
    // GET /api/v1/vehicles/{id} (Details)
    // ==========================================

    @Test
    @DisplayName("Retrieve vehicle details by ID (200 OK)")
    void getVehicleById_success() throws Exception {
        String vin = generateUniqueVin();
        Vehicle vehicle = createVehicle(vin, generateUniquePlate(), "Lucid", "Lucid Air Pure", VehicleStatus.AVAILABLE);

        mockMvc.perform(get("/api/v1/vehicles/" + vehicle.getId())
                        .header("Authorization", "Bearer " + coOwnerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.id", is(vehicle.getId().intValue())))
                .andExpect(jsonPath("$.data.vin", is(vin)))
                .andExpect(jsonPath("$.data.modelName", is("Lucid Air Pure")));
    }

    @Test
    @DisplayName("Retrieve non-existent vehicle returns 404 Not Found")
    void getVehicleById_notFound() throws Exception {
        mockMvc.perform(get("/api/v1/vehicles/999999")
                        .header("Authorization", "Bearer " + coOwnerToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success", is(false)));
    }

    // ==========================================
    // GET /api/v1/vehicles/{id}/telemetry (Real-time Telemetry)
    // ==========================================

    @Test
    @DisplayName("Retrieve vehicle telemetry (200 OK)")
    void getVehicleTelemetry_success() throws Exception {
        String vin = generateUniqueVin();
        Vehicle vehicle = createVehicle(vin, generateUniquePlate(), "BMW", "i4 M50", VehicleStatus.AVAILABLE);

        mockMvc.perform(get("/api/v1/vehicles/" + vehicle.getId() + "/telemetry")
                        .header("Authorization", "Bearer " + coOwnerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.vehicleId", is(vehicle.getId().intValue())))
                .andExpect(jsonPath("$.data.batteryLevel", is(85)))
                .andExpect(jsonPath("$.data.odometerKm", is(12500.50)))
                .andExpect(jsonPath("$.data.stallLocationCode", is("BAY-E05")))
                .andExpect(jsonPath("$.data.status", is("AVAILABLE")));
    }

    @Test
    @DisplayName("Retrieve telemetry for non-existent vehicle returns 404 Not Found")
    void getVehicleTelemetry_notFound() throws Exception {
        mockMvc.perform(get("/api/v1/vehicles/999999/telemetry")
                        .header("Authorization", "Bearer " + coOwnerToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success", is(false)));
    }

    @Test
    @DisplayName("Unauthenticated telemetry access fails (401 Unauthorized)")
    void getVehicleTelemetry_unauthorized() throws Exception {
        mockMvc.perform(get("/api/v1/vehicles/1/telemetry"))
                .andExpect(status().isUnauthorized());
    }
}
