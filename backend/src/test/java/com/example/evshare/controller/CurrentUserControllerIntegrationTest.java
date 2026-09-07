package com.example.evshare.controller;

import com.example.evshare.dto.request.LoginRequest;
import com.example.evshare.dto.response.ApiResponse;
import com.example.evshare.dto.response.AuthResponse;
import com.example.evshare.entity.Role;
import com.example.evshare.entity.User;
import com.example.evshare.entity.enums.RoleName;
import com.example.evshare.repository.RoleRepository;
import com.example.evshare.repository.UserRepository;
import com.example.evshare.security.JwtTokenProvider;
import com.example.evshare.security.TokenService;
import com.fasterxml.jackson.core.type.TypeReference;
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
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@DisplayName("Checkpoint 03-G — Current User Profile Integration Tests (GET /api/v1/users/me)")
class CurrentUserControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private TokenService tokenService;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    private User primaryUser;
    private User secondaryUser;
    private final String commonPassword = "SecurePassword123!";

    @BeforeEach
    void setUp() {
        Role coOwnerRole = roleRepository.findByName(RoleName.ROLE_CO_OWNER)
                .orElseGet(() -> roleRepository.save(new Role(RoleName.ROLE_CO_OWNER)));

        primaryUser = new User();
        primaryUser.setEmail("primary_" + UUID.randomUUID() + "@evshare3d.com");
        primaryUser.setPasswordHash(passwordEncoder.encode(commonPassword));
        primaryUser.setFullName("Primary Pilot");
        primaryUser.setPhoneNumber("+84901111111");
        primaryUser.setAvatar3dUrl("https://assets.evshare3d.com/avatars/pilot1.glb");
        primaryUser.setIsActive(true);
        primaryUser.setCreatedAt(Instant.now());
        primaryUser.setUpdatedAt(Instant.now());
        primaryUser.setRoles(new HashSet<>(Set.of(coOwnerRole)));
        primaryUser = userRepository.save(primaryUser);

        secondaryUser = new User();
        secondaryUser.setEmail("secondary_" + UUID.randomUUID() + "@evshare3d.com");
        secondaryUser.setPasswordHash(passwordEncoder.encode(commonPassword));
        secondaryUser.setFullName("Secondary Pilot");
        secondaryUser.setPhoneNumber("+84902222222");
        secondaryUser.setAvatar3dUrl("https://assets.evshare3d.com/avatars/pilot2.glb");
        secondaryUser.setIsActive(true);
        secondaryUser.setCreatedAt(Instant.now());
        secondaryUser.setUpdatedAt(Instant.now());
        secondaryUser.setRoles(new HashSet<>(Set.of(coOwnerRole)));
        secondaryUser = userRepository.save(secondaryUser);
    }

    private String loginAndGetAccessToken(String email, String password) throws Exception {
        LoginRequest request = new LoginRequest(email, password);
        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andReturn();

        ApiResponse<AuthResponse> response = objectMapper.readValue(
                result.getResponse().getContentAsString(),
                new TypeReference<ApiResponse<AuthResponse>>() {}
        );
        return response.getData().getAccessToken();
    }

    @Test
    @DisplayName("1. Authenticated User: Returns 200 OK with safe user details and assigned roles")
    void testAuthenticatedUser() throws Exception {
        String token = loginAndGetAccessToken(primaryUser.getEmail(), commonPassword);

        mockMvc.perform(get("/api/v1/users/me")
                        .header("Authorization", "Bearer " + token)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.message", containsString("User profile retrieved successfully")))
                .andExpect(jsonPath("$.data.id", is(primaryUser.getId().intValue())))
                .andExpect(jsonPath("$.data.email", is(primaryUser.getEmail())))
                .andExpect(jsonPath("$.data.fullName", is("Primary Pilot")))
                .andExpect(jsonPath("$.data.phoneNumber", is("+84901111111")))
                .andExpect(jsonPath("$.data.avatar3dUrl", is("https://assets.evshare3d.com/avatars/pilot1.glb")))
                .andExpect(jsonPath("$.data.isActive", is(true)))
                .andExpect(jsonPath("$.data.roles", hasItem("ROLE_CO_OWNER")))
                // Invariant: Never expose passwordHash or secret credentials
                .andExpect(jsonPath("$.data.passwordHash").doesNotExist())
                .andExpect(jsonPath("$.data.password").doesNotExist());
    }

    @Test
    @DisplayName("2. Missing Token: Returns 401 Unauthorized when Authorization header is absent")
    void testMissingToken() throws Exception {
        mockMvc.perform(get("/api/v1/users/me")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.status", is(401)))
                .andExpect(jsonPath("$.message", containsString("Authentication required")));
    }

    @Test
    @DisplayName("3. Invalid Token: Returns 401 Unauthorized for malformed or tampered token")
    void testInvalidToken() throws Exception {
        mockMvc.perform(get("/api/v1/users/me")
                        .header("Authorization", "Bearer not.a.valid.jwt.signature")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.status", is(401)))
                .andExpect(jsonPath("$.message", containsString("Authentication required")));
    }

    @Test
    @DisplayName("4. Expired Token: Returns 401 Unauthorized when access token has expired")
    void testExpiredToken() throws Exception {
        String expiredToken = jwtTokenProvider.generateCustomToken(
                primaryUser.getId(),
                primaryUser.getEmail(),
                List.of("ROLE_CO_OWNER"),
                -10000 // Expired 10 seconds ago
        );

        mockMvc.perform(get("/api/v1/users/me")
                        .header("Authorization", "Bearer " + expiredToken)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.status", is(401)));
    }

    @Test
    @DisplayName("5. Correct User Returned: Distinct tokens correctly resolve to distinct user records")
    void testCorrectUserReturned() throws Exception {
        String primaryToken = loginAndGetAccessToken(primaryUser.getEmail(), commonPassword);
        String secondaryToken = loginAndGetAccessToken(secondaryUser.getEmail(), commonPassword);

        // Fetch primary user profile
        mockMvc.perform(get("/api/v1/users/me")
                        .header("Authorization", "Bearer " + primaryToken)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id", is(primaryUser.getId().intValue())))
                .andExpect(jsonPath("$.data.email", is(primaryUser.getEmail())));

        // Fetch secondary user profile
        mockMvc.perform(get("/api/v1/users/me")
                        .header("Authorization", "Bearer " + secondaryToken)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id", is(secondaryUser.getId().intValue())))
                .andExpect(jsonPath("$.data.email", is(secondaryUser.getEmail())));
    }

    @Test
    @DisplayName("6. Anti-Tampering: Client cannot access another user's data by passing query parameter")
    void testCannotAccessAnotherUserDataThroughThisEndpoint() throws Exception {
        String primaryToken = loginAndGetAccessToken(primaryUser.getEmail(), commonPassword);

        // Primary user passes ?userId=<secondaryUser.id> attempting to spoof or hijack data
        mockMvc.perform(get("/api/v1/users/me")
                        .param("userId", secondaryUser.getId().toString())
                        .header("Authorization", "Bearer " + primaryToken)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                // Response must strictly remain Primary User, completely ignoring query parameter
                .andExpect(jsonPath("$.data.id", is(primaryUser.getId().intValue())))
                .andExpect(jsonPath("$.data.email", is(primaryUser.getEmail())))
                .andExpect(jsonPath("$.data.id", not(is(secondaryUser.getId().intValue()))));
    }

    @Test
    @DisplayName("7. Role Inclusion: Roles are included by default and can be omitted when specified")
    void testRoleInclusion() throws Exception {
        String token = loginAndGetAccessToken(primaryUser.getEmail(), commonPassword);

        // Default: roles included
        mockMvc.perform(get("/api/v1/users/me")
                        .header("Authorization", "Bearer " + token)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.roles", hasItem("ROLE_CO_OWNER")));

        // Explicit includeRoles=false: roles omitted from response
        mockMvc.perform(get("/api/v1/users/me")
                        .param("includeRoles", "false")
                        .header("Authorization", "Bearer " + token)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.roles").doesNotExist());
    }

    @Test
    @DisplayName("8. Deactivated Account: Blocked with 403 Forbidden even with valid token")
    void testDeactivatedAccountAccess() throws Exception {
        primaryUser.setIsActive(false);
        userRepository.save(primaryUser);

        String token = tokenService.generateAccessToken(primaryUser);

        mockMvc.perform(get("/api/v1/users/me")
                        .header("Authorization", "Bearer " + token)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.status", is(403)))
                .andExpect(jsonPath("$.message", containsString("disabled or deactivated")));
    }
}
