package com.example.evshare.security;

import com.example.evshare.dto.request.LoginRequest;
import com.example.evshare.dto.request.RegisterRequest;
import com.example.evshare.dto.response.ApiResponse;
import com.example.evshare.dto.response.AuthResponse;
import com.example.evshare.entity.OwnershipGroup;
import com.example.evshare.entity.OwnershipShare;
import com.example.evshare.entity.Role;
import com.example.evshare.entity.User;
import com.example.evshare.entity.Vehicle;
import com.example.evshare.entity.enums.RoleName;
import com.example.evshare.entity.enums.VehicleStatus;
import com.example.evshare.repository.OwnershipGroupRepository;
import com.example.evshare.repository.OwnershipShareRepository;
import com.example.evshare.repository.RoleRepository;
import com.example.evshare.repository.UserRepository;
import com.example.evshare.repository.VehicleRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@Import(RbacAuthorizationTest.RbacTestController.class)
@DisplayName("Checkpoint 03-H — Role-Based Access Control (RBAC) & Ownership ACL Tests")
class RbacAuthorizationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

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
    private PasswordEncoder passwordEncoder;

    private User adminUser;
    private User staffUser;
    private User coOwnerA;
    private User coOwnerB;
    private OwnershipGroup testGroup;

    private final String commonPassword = "P@ssword123Secure!";

    @RestController
    @RequestMapping("/api/v1/test/rbac")
    static class RbacTestController {

        @GetMapping("/admin-only")
        @PreAuthorize("hasRole('ADMIN')")
        public ResponseEntity<ApiResponse<String>> adminOnlyEndpoint() {
            return ResponseEntity.ok(ApiResponse.ok("ADMIN access granted"));
        }

        @GetMapping("/staff-only")
        @PreAuthorize("hasRole('STAFF')")
        public ResponseEntity<ApiResponse<String>> staffOnlyEndpoint() {
            return ResponseEntity.ok(ApiResponse.ok("STAFF access granted"));
        }

        @GetMapping("/staff-or-admin")
        @PreAuthorize("hasAnyRole('STAFF', 'ADMIN')")
        public ResponseEntity<ApiResponse<String>> staffOrAdminEndpoint() {
            return ResponseEntity.ok(ApiResponse.ok("STAFF or ADMIN access granted"));
        }

        @GetMapping("/co-owner-only")
        @PreAuthorize("hasRole('CO_OWNER')")
        public ResponseEntity<ApiResponse<String>> coOwnerOnlyEndpoint() {
            return ResponseEntity.ok(ApiResponse.ok("CO_OWNER access granted"));
        }

        @GetMapping("/group/{groupId}")
        @PreAuthorize("hasRole('ADMIN') or (hasRole('CO_OWNER') and @ownershipSecurity.isGroupMember(#groupId, principal.id))")
        public ResponseEntity<ApiResponse<String>> groupScopedEndpoint(@PathVariable("groupId") Long groupId) {
            return ResponseEntity.ok(ApiResponse.ok("Group scoped access granted for group " + groupId));
        }
    }

    @BeforeEach
    void setUp() {
        Role adminRole = roleRepository.findByName(RoleName.ROLE_ADMIN)
                .orElseGet(() -> roleRepository.save(new Role(RoleName.ROLE_ADMIN)));
        Role staffRole = roleRepository.findByName(RoleName.ROLE_STAFF)
                .orElseGet(() -> roleRepository.save(new Role(RoleName.ROLE_STAFF)));
        Role coOwnerRole = roleRepository.findByName(RoleName.ROLE_CO_OWNER)
                .orElseGet(() -> roleRepository.save(new Role(RoleName.ROLE_CO_OWNER)));

        // Create ADMIN
        adminUser = new User();
        adminUser.setEmail("admin_" + UUID.randomUUID() + "@evshare3d.com");
        adminUser.setPasswordHash(passwordEncoder.encode(commonPassword));
        adminUser.setFullName("Platform Admin");
        adminUser.setIsActive(true);
        adminUser.setRoles(new HashSet<>(Set.of(adminRole)));
        adminUser = userRepository.save(adminUser);

        // Create STAFF
        staffUser = new User();
        staffUser.setEmail("staff_" + UUID.randomUUID() + "@evshare3d.com");
        staffUser.setPasswordHash(passwordEncoder.encode(commonPassword));
        staffUser.setFullName("Garage Staff");
        staffUser.setIsActive(true);
        staffUser.setRoles(new HashSet<>(Set.of(staffRole)));
        staffUser = userRepository.save(staffUser);

        // Create CO_OWNER A (Group Member)
        coOwnerA = new User();
        coOwnerA.setEmail("coowner_a_" + UUID.randomUUID() + "@evshare3d.com");
        coOwnerA.setPasswordHash(passwordEncoder.encode(commonPassword));
        coOwnerA.setFullName("Co-Owner Alpha");
        coOwnerA.setIsActive(true);
        coOwnerA.setRoles(new HashSet<>(Set.of(coOwnerRole)));
        coOwnerA = userRepository.save(coOwnerA);

        // Create CO_OWNER B (Non-Member)
        coOwnerB = new User();
        coOwnerB.setEmail("coowner_b_" + UUID.randomUUID() + "@evshare3d.com");
        coOwnerB.setPasswordHash(passwordEncoder.encode(commonPassword));
        coOwnerB.setFullName("Co-Owner Beta");
        coOwnerB.setIsActive(true);
        coOwnerB.setRoles(new HashSet<>(Set.of(coOwnerRole)));
        coOwnerB = userRepository.save(coOwnerB);

        // Seed Vehicle and OwnershipGroup
        Vehicle vehicle = new Vehicle();
        vehicle.setVin("VIN-" + UUID.randomUUID().toString().substring(0, 10));
        vehicle.setModelName("VF 9 Plus Digital Twin");
        vehicle.setManufacturer("VinFast");
        vehicle.setModel3dAssetPath("/models/vf9.glb");
        vehicle.setLicensePlate("29A-" + (int)(Math.random() * 90000 + 10000));
        vehicle.setStatus(VehicleStatus.AVAILABLE);
        vehicle.setBatteryLevel(95);
        vehicle.setOdometerKm(new BigDecimal("500.00"));
        vehicle.setStallLocationCode("BAY-01");
        vehicle.setCreatedAt(Instant.now());
        vehicle.setUpdatedAt(Instant.now());
        vehicle = vehicleRepository.save(vehicle);

        testGroup = new OwnershipGroup();
        testGroup.setGroupName("VinFast Sector 1 Pod");
        testGroup.setVehicle(vehicle);
        testGroup.setFormationDate(LocalDate.now());
        testGroup.setIsActive(true);
        testGroup = ownershipGroupRepository.save(testGroup);

        // Assign active equity share to Co-Owner A ONLY
        OwnershipShare shareA = new OwnershipShare();
        shareA.setGroup(testGroup);
        shareA.setUser(coOwnerA);
        shareA.setPercentage(new BigDecimal("25.00"));
        shareA.setShareCertificateNumber("CERT-" + UUID.randomUUID());
        shareA.setAcquiredAt(Instant.now());
        shareA.setIsActive(true);
        ownershipShareRepository.save(shareA);
    }

    private String login(String email) throws Exception {
        LoginRequest loginRequest = new LoginRequest(email, commonPassword);
        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn();

        ApiResponse<AuthResponse> apiResponse = objectMapper.readValue(
                result.getResponse().getContentAsString(),
                new TypeReference<ApiResponse<AuthResponse>>() {}
        );
        return apiResponse.getData().getAccessToken();
    }

    // =========================================================================
    // 1. ADMIN-ONLY OPERATIONS
    // =========================================================================

    @Test
    @DisplayName("1. ADMIN: Can access ADMIN-only operations successfully (HTTP 200)")
    void testAdminAccessAdminEndpoint_Success() throws Exception {
        String token = login(adminUser.getEmail());

        mockMvc.perform(get("/api/v1/test/rbac/admin-only")
                        .header("Authorization", "Bearer " + token)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data", containsString("ADMIN access granted")));
    }

    @Test
    @DisplayName("2. STAFF: Access to ADMIN-only operation is forbidden (HTTP 403)")
    void testStaffAccessAdminEndpoint_Forbidden() throws Exception {
        String token = login(staffUser.getEmail());

        mockMvc.perform(get("/api/v1/test/rbac/admin-only")
                        .header("Authorization", "Bearer " + token)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.status", is(403)))
                .andExpect(jsonPath("$.message", containsString("Access denied")));
    }

    @Test
    @DisplayName("3. CO_OWNER: Access to ADMIN-only operation is forbidden (HTTP 403)")
    void testCoOwnerAccessAdminEndpoint_Forbidden() throws Exception {
        String token = login(coOwnerA.getEmail());

        mockMvc.perform(get("/api/v1/test/rbac/admin-only")
                        .header("Authorization", "Bearer " + token)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.status", is(403)));
    }

    @Test
    @DisplayName("4. Unauthenticated: Access to ADMIN-only operation returns HTTP 401 Unauthorized")
    void testUnauthenticatedAccessAdminEndpoint_Unauthorized() throws Exception {
        mockMvc.perform(get("/api/v1/test/rbac/admin-only")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.status", is(401)));
    }

    // =========================================================================
    // 2. STAFF-ONLY & STAFF/ADMIN OPERATIONS
    // =========================================================================

    @Test
    @DisplayName("5. STAFF: Can access STAFF-only operation successfully (HTTP 200)")
    void testStaffAccessStaffEndpoint_Success() throws Exception {
        String token = login(staffUser.getEmail());

        mockMvc.perform(get("/api/v1/test/rbac/staff-only")
                        .header("Authorization", "Bearer " + token)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data", containsString("STAFF access granted")));
    }

    @Test
    @DisplayName("6. CO_OWNER: Access to STAFF-only operation is forbidden (HTTP 403)")
    void testCoOwnerAccessStaffEndpoint_Forbidden() throws Exception {
        String token = login(coOwnerA.getEmail());

        mockMvc.perform(get("/api/v1/test/rbac/staff-only")
                        .header("Authorization", "Bearer " + token)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.status", is(403)));
    }

    @Test
    @DisplayName("7. STAFF & ADMIN on Production Route: Both can access GET /api/v1/users/{id} (HTTP 200)")
    void testStaffAndAdminAccessUserById_Success() throws Exception {
        String staffToken = login(staffUser.getEmail());
        String adminToken = login(adminUser.getEmail());

        // Staff can inspect user profile
        mockMvc.perform(get("/api/v1/users/" + coOwnerA.getId())
                        .header("Authorization", "Bearer " + staffToken)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.id", is(coOwnerA.getId().intValue())));

        // Admin can inspect user profile
        mockMvc.perform(get("/api/v1/users/" + coOwnerA.getId())
                        .header("Authorization", "Bearer " + adminToken)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.id", is(coOwnerA.getId().intValue())));
    }

    @Test
    @DisplayName("8. CO_OWNER on Production Route: Cannot access GET /api/v1/users/{id} (HTTP 403 Forbidden)")
    void testCoOwnerAccessUserById_Forbidden() throws Exception {
        String coOwnerToken = login(coOwnerA.getEmail());

        mockMvc.perform(get("/api/v1/users/" + coOwnerB.getId())
                        .header("Authorization", "Bearer " + coOwnerToken)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.status", is(403)));
    }

    // =========================================================================
    // 3. CO-OWNER ONLY OPERATIONS
    // =========================================================================

    @Test
    @DisplayName("9. CO_OWNER: Can access CO_OWNER operations successfully (HTTP 200)")
    void testCoOwnerAccessCoOwnerEndpoint_Success() throws Exception {
        String token = login(coOwnerA.getEmail());

        mockMvc.perform(get("/api/v1/test/rbac/co-owner-only")
                        .header("Authorization", "Bearer " + token)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data", containsString("CO_OWNER access granted")));
    }

    @Test
    @DisplayName("10. STAFF: Access to CO_OWNER-only operation is forbidden (HTTP 403)")
    void testStaffAccessCoOwnerEndpoint_Forbidden() throws Exception {
        String token = login(staffUser.getEmail());

        mockMvc.perform(get("/api/v1/test/rbac/co-owner-only")
                        .header("Authorization", "Bearer " + token)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.status", is(403)));
    }

    // =========================================================================
    // 4. OWNERSHIP ACL & DATA SCOPING (@ownershipSecurity.isGroupMember)
    // =========================================================================

    @Test
    @DisplayName("11. Ownership ACL (Positive): Co-Owner with active equity in Group can access group resource (HTTP 200)")
    void testCoOwnerMemberAccessGroup_Success() throws Exception {
        String token = login(coOwnerA.getEmail());

        mockMvc.perform(get("/api/v1/test/rbac/group/" + testGroup.getId())
                        .header("Authorization", "Bearer " + token)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data", containsString("Group scoped access granted")));
    }

    @Test
    @DisplayName("12. Ownership ACL (Negative): Co-Owner without equity in Group is denied (HTTP 403 Forbidden)")
    void testCoOwnerNonMemberAccessGroup_Forbidden() throws Exception {
        String token = login(coOwnerB.getEmail()); // User B has no share in testGroup

        mockMvc.perform(get("/api/v1/test/rbac/group/" + testGroup.getId())
                        .header("Authorization", "Bearer " + token)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.status", is(403)));
    }

    @Test
    @DisplayName("13. Ownership ACL (Admin Bypass): Admin can access any group resource regardless of membership (HTTP 200)")
    void testAdminBypassesGroupScoping_Success() throws Exception {
        String token = login(adminUser.getEmail());

        mockMvc.perform(get("/api/v1/test/rbac/group/" + testGroup.getId())
                        .header("Authorization", "Bearer " + token)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)));
    }

    @Test
    @DisplayName("14. Ownership ACL (Staff Restriction): Staff without Co-Owner equity is denied group access (HTTP 403)")
    void testStaffAccessGroupWithoutEquity_Forbidden() throws Exception {
        String token = login(staffUser.getEmail());

        mockMvc.perform(get("/api/v1/test/rbac/group/" + testGroup.getId())
                        .header("Authorization", "Bearer " + token)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.status", is(403)));
    }

    // =========================================================================
    // 5. ACTUATOR RBAC PROTECTION
    // =========================================================================

    @Test
    @DisplayName("15. Actuator Health: Publicly accessible without token (HTTP 200)")
    void testActuatorHealth_Public() throws Exception {
        mockMvc.perform(get("/actuator/health")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("UP")));
    }

    @Test
    @DisplayName("16. Actuator Management: ADMIN can access /actuator (HTTP 200)")
    void testAdminAccessActuator_Success() throws Exception {
        String token = login(adminUser.getEmail());

        mockMvc.perform(get("/actuator")
                        .header("Authorization", "Bearer " + token)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("17. Actuator Management: CO_OWNER cannot access /actuator (HTTP 403)")
    void testCoOwnerAccessActuator_Forbidden() throws Exception {
        String token = login(coOwnerA.getEmail());

        mockMvc.perform(get("/actuator")
                        .header("Authorization", "Bearer " + token)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("18. Actuator Management: STAFF cannot access /actuator (HTTP 403)")
    void testStaffAccessActuator_Forbidden() throws Exception {
        String token = login(staffUser.getEmail());

        mockMvc.perform(get("/actuator")
                        .header("Authorization", "Bearer " + token)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    // =========================================================================
    // 6. PRIVILEGE ESCALATION PREVENTION
    // =========================================================================

    @Test
    @DisplayName("19. Anti-Escalation: Public self-registration cannot assign ROLE_ADMIN (HTTP 403 Forbidden)")
    void testRegistrationPrivilegeEscalation_AdminForbidden() throws Exception {
        RegisterRequest request = RegisterRequest.builder()
                .email("hacker_admin@evshare3d.com")
                .password("Password123!Secure")
                .fullName("Evil Admin")
                .role("ROLE_ADMIN")
                .build();

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.status", is(403)))
                .andExpect(jsonPath("$.message", containsString("Self-assignment of administrative or staff roles is strictly prohibited")));
    }

    @Test
    @DisplayName("20. Anti-Escalation: Public self-registration cannot assign ROLE_STAFF (HTTP 403 Forbidden)")
    void testRegistrationPrivilegeEscalation_StaffForbidden() throws Exception {
        RegisterRequest request = RegisterRequest.builder()
                .email("hacker_staff@evshare3d.com")
                .password("Password123!Secure")
                .fullName("Evil Staff")
                .role("ROLE_STAFF")
                .build();

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.status", is(403)));
    }
}
