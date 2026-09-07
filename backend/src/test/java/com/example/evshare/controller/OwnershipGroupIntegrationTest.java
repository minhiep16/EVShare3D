package com.example.evshare.controller;

import com.example.evshare.dto.request.AddGroupMemberRequest;
import com.example.evshare.dto.request.CreateOwnershipGroupRequest;
import com.example.evshare.dto.request.UpdateOwnershipGroupRequest;
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
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@DisplayName("Checkpoint 04-E — Ownership Group Integration Tests")
class OwnershipGroupIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private OwnershipGroupRepository ownershipGroupRepository;

    @Autowired
    private OwnershipShareRepository ownershipShareRepository;

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

    private User adminUser;
    private User staffUser;
    private User memberCoOwner;
    private User nonMemberCoOwner;

    private String adminToken;
    private String staffToken;
    private String memberCoOwnerToken;
    private String nonMemberCoOwnerToken;

    private Vehicle sampleVehicle;

    private User createUser(String email, RoleName roleName) {
        Role role = roleRepository.findByName(roleName)
                .orElseGet(() -> roleRepository.save(new Role(roleName)));

        User user = new User();
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode("Password123!"));
        user.setFullName("User " + roleName.name());
        user.setIsActive(true);
        user.setRoles(new HashSet<>(Collections.singletonList(role)));
        return userRepository.saveAndFlush(user);
    }

    private Vehicle createVehicle(String vin, String plate) {
        Vehicle vehicle = new Vehicle();
        vehicle.setVin(vin);
        vehicle.setLicensePlate(plate);
        vehicle.setModelName("VinFast VF 9 Plus");
        vehicle.setManufacturer("VinFast");
        vehicle.setModel3dAssetPath("models/vehicles/vf9.glb");
        vehicle.setStatus(VehicleStatus.AVAILABLE);
        vehicle.setBatteryLevel(100);
        vehicle.setOdometerKm(BigDecimal.ZERO);
        vehicle.setStallLocationCode("BAY-03");
        return vehicleRepository.saveAndFlush(vehicle);
    }

    @BeforeEach
    void setUp() {
        String uid = UUID.randomUUID().toString().substring(0, 8);
        adminUser = createUser("admin_" + uid + "@evshare.vn", RoleName.ROLE_ADMIN);
        staffUser = createUser("staff_" + uid + "@evshare.vn", RoleName.ROLE_STAFF);
        memberCoOwner = createUser("member_" + uid + "@evshare.vn", RoleName.ROLE_CO_OWNER);
        nonMemberCoOwner = createUser("nonmember_" + uid + "@evshare.vn", RoleName.ROLE_CO_OWNER);

        adminToken = tokenService.generateAccessToken(adminUser.getId(), adminUser.getEmail(), Collections.singletonList("ROLE_ADMIN"));
        staffToken = tokenService.generateAccessToken(staffUser.getId(), staffUser.getEmail(), Collections.singletonList("ROLE_STAFF"));
        memberCoOwnerToken = tokenService.generateAccessToken(memberCoOwner.getId(), memberCoOwner.getEmail(), Collections.singletonList("ROLE_CO_OWNER"));
        nonMemberCoOwnerToken = tokenService.generateAccessToken(nonMemberCoOwner.getId(), nonMemberCoOwner.getEmail(), Collections.singletonList("ROLE_CO_OWNER"));

        sampleVehicle = createVehicle("VIN_GRP_" + uid, "51G-" + (10000 + (int) (Math.random() * 89999)));
    }

    // =========================================================================
    // 1. CREATION TESTS
    // =========================================================================

    @Test
    @DisplayName("1.1. Admin creates ownership group with vehicle and initial member")
    void testCreateOwnershipGroupSuccess() throws Exception {
        CreateOwnershipGroupRequest request = new CreateOwnershipGroupRequest(
                "Saigon Tech Syndicate",
                sampleVehicle.getId(),
                LocalDate.now(),
                Collections.singletonList(memberCoOwner.getId())
        );

        mockMvc.perform(post("/api/v1/ownership-groups")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.groupName", is("Saigon Tech Syndicate")))
                .andExpect(jsonPath("$.data.vehicleId", is(sampleVehicle.getId().intValue())))
                .andExpect(jsonPath("$.data.vehicleModelName", is("VinFast VF 9 Plus")))
                .andExpect(jsonPath("$.data.memberCount", is(1)))
                .andExpect(jsonPath("$.data.memberShares[0].userId", is(memberCoOwner.getId().intValue())));

        assertTrue(ownershipGroupRepository.existsByVehicleId(sampleVehicle.getId()));
    }

    @Test
    @DisplayName("1.2. Creation fails with 404 when vehicle ID does not exist")
    void testCreateGroupNonExistentVehicle() throws Exception {
        CreateOwnershipGroupRequest request = new CreateOwnershipGroupRequest(
                "Orphan Syndicate",
                999999L,
                LocalDate.now(),
                Collections.emptyList()
        );

        mockMvc.perform(post("/api/v1/ownership-groups")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status", is(404)));
    }

    @Test
    @DisplayName("1.3. Creation fails with 409 Conflict when vehicle is already assigned to a group")
    void testCreateGroupDuplicateVehicleBinding() throws Exception {
        // Create first group
        CreateOwnershipGroupRequest req1 = new CreateOwnershipGroupRequest(
                "Syndicate Alpha",
                sampleVehicle.getId()
        );
        mockMvc.perform(post("/api/v1/ownership-groups")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req1)))
                .andExpect(status().isCreated());

        // Attempt second group with same vehicle
        CreateOwnershipGroupRequest req2 = new CreateOwnershipGroupRequest(
                "Syndicate Beta",
                sampleVehicle.getId()
        );
        mockMvc.perform(post("/api/v1/ownership-groups")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req2)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status", is(409)))
                .andExpect(jsonPath("$.message", containsString("already bound")));
    }

    // =========================================================================
    // 2. ACCESS TESTS
    // =========================================================================

    private OwnershipGroup createPersistedGroupWithMember() {
        OwnershipGroup group = new OwnershipGroup();
        group.setGroupName("District 1 Syndicate");
        group.setVehicle(sampleVehicle);
        group.setFormationDate(LocalDate.now());
        group.setIsActive(true);
        OwnershipGroup savedGroup = ownershipGroupRepository.saveAndFlush(group);

        OwnershipShare share = new OwnershipShare();
        share.setGroup(savedGroup);
        share.setUser(memberCoOwner);
        share.setPercentage(new BigDecimal("50.00"));
        share.setShareCertificateNumber("CERT-" + UUID.randomUUID().toString());
        share.setIsActive(true);
        ownershipShareRepository.saveAndFlush(share);

        return savedGroup;
    }

    @Test
    @DisplayName("2.1. Admin and Staff can view any ownership group")
    void testAdminAndStaffViewAccess() throws Exception {
        OwnershipGroup group = createPersistedGroupWithMember();

        // Admin access
        mockMvc.perform(get("/api/v1/ownership-groups/{id}", group.getId())
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id", is(group.getId().intValue())))
                .andExpect(jsonPath("$.data.groupName", is("District 1 Syndicate")));

        // Staff access
        mockMvc.perform(get("/api/v1/ownership-groups/{id}", group.getId())
                        .header("Authorization", "Bearer " + staffToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id", is(group.getId().intValue())));
    }

    @Test
    @DisplayName("2.2. Enrolled Co-Owner can view their own group")
    void testEnrolledCoOwnerViewAccess() throws Exception {
        OwnershipGroup group = createPersistedGroupWithMember();

        mockMvc.perform(get("/api/v1/ownership-groups/{id}", group.getId())
                        .header("Authorization", "Bearer " + memberCoOwnerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id", is(group.getId().intValue())))
                .andExpect(jsonPath("$.data.memberShares[0].userId", is(memberCoOwner.getId().intValue())));
    }

    @Test
    @DisplayName("2.3. Co-Owner can list groups via /my-groups")
    void testCoOwnerListMyGroups() throws Exception {
        createPersistedGroupWithMember();

        mockMvc.perform(get("/api/v1/ownership-groups/my-groups")
                        .header("Authorization", "Bearer " + memberCoOwnerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$.data[0].groupName", is("District 1 Syndicate")));
    }

    // =========================================================================
    // 3. UNAUTHORIZED ACCESS TESTS
    // =========================================================================

    @Test
    @DisplayName("3.1. Unauthenticated request returns 401 Unauthorized")
    void testUnauthenticatedAccessRejected() throws Exception {
        mockMvc.perform(get("/api/v1/ownership-groups/1"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/api/v1/ownership-groups")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("3.2. Co-Owner cannot create ownership group (Admin only, returns 403)")
    void testCoOwnerCannotCreateGroup() throws Exception {
        CreateOwnershipGroupRequest request = new CreateOwnershipGroupRequest(
                "Unauthorized Syndicate",
                sampleVehicle.getId()
        );

        mockMvc.perform(post("/api/v1/ownership-groups")
                        .header("Authorization", "Bearer " + memberCoOwnerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("3.3. Non-member Co-Owner is forbidden from viewing syndicate details (ACL 403)")
    void testNonMemberCoOwnerForbiddenByAcl() throws Exception {
        OwnershipGroup group = createPersistedGroupWithMember();

        mockMvc.perform(get("/api/v1/ownership-groups/{id}", group.getId())
                        .header("Authorization", "Bearer " + nonMemberCoOwnerToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status", is(403)))
                .andExpect(jsonPath("$.message", containsString("Access denied")));
    }

    @Test
    @DisplayName("3.4. Staff cannot create or update ownership groups (Admin only, returns 403)")
    void testStaffCannotCreateOrUpdateGroup() throws Exception {
        OwnershipGroup group = createPersistedGroupWithMember();

        // Staff create attempt
        CreateOwnershipGroupRequest req = new CreateOwnershipGroupRequest("Staff Syndicate", sampleVehicle.getId());
        mockMvc.perform(post("/api/v1/ownership-groups")
                        .header("Authorization", "Bearer " + staffToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isForbidden());

        // Staff update attempt
        UpdateOwnershipGroupRequest updateReq = new UpdateOwnershipGroupRequest("New Name", false);
        mockMvc.perform(patch("/api/v1/ownership-groups/{id}", group.getId())
                        .header("Authorization", "Bearer " + staffToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateReq)))
                .andExpect(status().isForbidden());
    }

    // =========================================================================
    // 4. RELATIONSHIP INTEGRITY & PERMITTED UPDATES
    // =========================================================================

    @Test
    @DisplayName("4.1. Admin updates group name and active flag")
    void testAdminPermittedUpdate() throws Exception {
        OwnershipGroup group = createPersistedGroupWithMember();

        UpdateOwnershipGroupRequest updateReq = new UpdateOwnershipGroupRequest("Updated Syndicate Name", false);
        mockMvc.perform(patch("/api/v1/ownership-groups/{id}", group.getId())
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.groupName", is("Updated Syndicate Name")))
                .andExpect(jsonPath("$.data.isActive", is(false)));

        OwnershipGroup fromDb = ownershipGroupRepository.findById(group.getId()).orElseThrow();
        assertEquals("Updated Syndicate Name", fromDb.getGroupName());
        assertFalse(fromDb.getIsActive());
    }

    @Test
    @DisplayName("4.2. Admin enrolls new member into group and verifies relationship")
    void testAddMemberToGroup() throws Exception {
        OwnershipGroup group = createPersistedGroupWithMember();

        AddGroupMemberRequest addReq = new AddGroupMemberRequest(nonMemberCoOwner.getId(), new BigDecimal("20.00"));
        mockMvc.perform(post("/api/v1/ownership-groups/{id}/members", group.getId())
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(addReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.memberCount", is(2)));

        // Non-member is now a member and should be able to view group via ACL!
        mockMvc.perform(get("/api/v1/ownership-groups/{id}", group.getId())
                        .header("Authorization", "Bearer " + nonMemberCoOwnerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id", is(group.getId().intValue())));
    }

    @Test
    @DisplayName("4.3. Duplicate member enrollment in same group returns 409 Conflict")
    void testDuplicateMemberEnrollmentRejected() throws Exception {
        OwnershipGroup group = createPersistedGroupWithMember();

        // memberCoOwner is already in group
        AddGroupMemberRequest addReq = new AddGroupMemberRequest(memberCoOwner.getId());
        mockMvc.perform(post("/api/v1/ownership-groups/{id}/members", group.getId())
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(addReq)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status", is(409)))
                .andExpect(jsonPath("$.message", containsString("already an active member")));
    }

    @Test
    @DisplayName("4.4. Deactivating/removing a member revokes their ACL access")
    void testRemoveMemberRevokesAclAccess() throws Exception {
        OwnershipGroup group = createPersistedGroupWithMember();

        // First verify member has access
        mockMvc.perform(get("/api/v1/ownership-groups/{id}", group.getId())
                        .header("Authorization", "Bearer " + memberCoOwnerToken))
                .andExpect(status().isOk());

        // Admin removes member
        mockMvc.perform(delete("/api/v1/ownership-groups/{id}/members/{userId}", group.getId(), memberCoOwner.getId())
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());

        // Now member should be denied access via Ownership ACL
        mockMvc.perform(get("/api/v1/ownership-groups/{id}", group.getId())
                        .header("Authorization", "Bearer " + memberCoOwnerToken))
                .andExpect(status().isForbidden());
    }
}
