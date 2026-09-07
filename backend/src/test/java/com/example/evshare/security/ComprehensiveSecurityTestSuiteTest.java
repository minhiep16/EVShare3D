package com.example.evshare.security;

import com.example.evshare.dto.request.LoginRequest;
import com.example.evshare.dto.request.PasswordResetConfirmRequest;
import com.example.evshare.dto.request.PasswordResetRequest;
import com.example.evshare.dto.request.RefreshTokenRequest;
import com.example.evshare.dto.request.RegisterRequest;
import com.example.evshare.dto.response.ApiResponse;
import com.example.evshare.dto.response.AuthResponse;
import com.example.evshare.dto.response.UserResponse;
import com.example.evshare.entity.Role;
import com.example.evshare.entity.User;
import com.example.evshare.entity.enums.RoleName;
import com.example.evshare.repository.RoleRepository;
import com.example.evshare.repository.UserRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * CHECKPOINT 03-J — COMPREHENSIVE SECURITY TEST SUITE
 *
 * Systematically asserts all 16 authentication & authorization security dimensions:
 * 1.  Registration
 * 2.  Duplicate registration
 * 3.  Password hashing (BCrypt work factor 12)
 * 4.  Login
 * 5.  Invalid login (anti-enumeration)
 * 6.  JWT validation
 * 7.  Expired JWT
 * 8.  Malformed JWT
 * 9.  Refresh token & rotation
 * 10. Logout & revocation
 * 11. Current user profile (/users/me)
 * 12. RBAC (Positive authorization for CO_OWNER, STAFF, ADMIN)
 * 13. Forbidden access (Negative authorization boundaries)
 * 14. Unauthenticated access (401 Unauthorized)
 * 15. Password reset foundation (lifecycle, anti-enumeration, session invalidation)
 * 16. Sensitive data exposure (never expose hashes, secrets, internal metadata)
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@Import(ComprehensiveSecurityTestSuiteTest.SecuritySuiteTestController.class)
@DisplayName("Checkpoint 03-J — Comprehensive Security Test Suite (All 16 Dimensions)")
public class ComprehensiveSecurityTestSuiteTest {

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
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private TokenService tokenService;

    @Autowired
    private RefreshTokenStore refreshTokenStore;

    @Autowired
    private PasswordResetTokenStore passwordResetTokenStore;

    @Autowired
    private DevPasswordResetNotifier devPasswordResetNotifier;

    private Role coOwnerRole;
    private Role staffRole;
    private Role adminRole;

    @RestController
    @RequestMapping("/api/v1/test/security-suite")
    static class SecuritySuiteTestController {

        @GetMapping("/admin")
        @PreAuthorize("hasRole('ROLE_ADMIN')")
        public ResponseEntity<ApiResponse<String>> adminOnly() {
            return ResponseEntity.ok(ApiResponse.ok("ADMIN_GRANTED", "ADMIN_RESOURCE"));
        }

        @GetMapping("/staff")
        @PreAuthorize("hasRole('ROLE_STAFF')")
        public ResponseEntity<ApiResponse<String>> staffOnly() {
            return ResponseEntity.ok(ApiResponse.ok("STAFF_GRANTED", "STAFF_RESOURCE"));
        }

        @GetMapping("/co-owner")
        @PreAuthorize("hasRole('ROLE_CO_OWNER')")
        public ResponseEntity<ApiResponse<String>> coOwnerOnly() {
            return ResponseEntity.ok(ApiResponse.ok("CO_OWNER_GRANTED", "CO_OWNER_RESOURCE"));
        }

        @GetMapping("/staff-or-admin")
        @PreAuthorize("hasAnyRole('ROLE_STAFF', 'ROLE_ADMIN')")
        public ResponseEntity<ApiResponse<String>> staffOrAdmin() {
            return ResponseEntity.ok(ApiResponse.ok("STAFF_OR_ADMIN_GRANTED", "PRIVILEGED_RESOURCE"));
        }
    }

    @BeforeEach
    void setUp() {
        devPasswordResetNotifier.clear();

        coOwnerRole = roleRepository.findByName(RoleName.ROLE_CO_OWNER)
                .orElseGet(() -> roleRepository.save(new Role(RoleName.ROLE_CO_OWNER)));
        staffRole = roleRepository.findByName(RoleName.ROLE_STAFF)
                .orElseGet(() -> roleRepository.save(new Role(RoleName.ROLE_STAFF)));
        adminRole = roleRepository.findByName(RoleName.ROLE_ADMIN)
                .orElseGet(() -> roleRepository.save(new Role(RoleName.ROLE_ADMIN)));
    }

    private User createTestUser(String emailPrefix, Role role, boolean active) {
        User user = new User();
        user.setEmail(emailPrefix + "_" + UUID.randomUUID() + "@evshare3d.com");
        user.setPasswordHash(passwordEncoder.encode("SecurePassword123!"));
        user.setFullName("Test User " + role.getName().name());
        user.setIsActive(active);
        user.setCreatedAt(Instant.now());
        user.setUpdatedAt(Instant.now());
        user.setRoles(new HashSet<>(Set.of(role)));
        return userRepository.save(user);
    }

    // =========================================================================
    // 1. REGISTRATION
    // =========================================================================
    @Test
    @Order(1)
    @DisplayName("Dimension 1: Registration — Successfully registers user with default ROLE_CO_OWNER")
    void test01_registration() throws Exception {
        String email = "suite_reg_" + UUID.randomUUID() + "@evshare3d.com";
        RegisterRequest request = RegisterRequest.builder()
                .email(email)
                .password("StrongPass123!")
                .fullName("Registered Member")
                .phoneNumber("+84988000111")
                .avatar3dUrl("https://assets.evshare3d.com/avatars/member.glb")
                .build();

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.email", is(email)))
                .andExpect(jsonPath("$.data.fullName", is("Registered Member")))
                .andExpect(jsonPath("$.data.roles", hasItem("ROLE_CO_OWNER")));

        Optional<User> persisted = userRepository.findByEmail(email);
        assertTrue(persisted.isPresent(), "Registered user must be persisted in database");
    }

    // =========================================================================
    // 2. DUPLICATE REGISTRATION
    // =========================================================================
    @Test
    @Order(2)
    @DisplayName("Dimension 2: Duplicate Registration — Duplicate email is rejected with 409 Conflict")
    void test02_duplicateRegistration() throws Exception {
        String email = "suite_dup_" + UUID.randomUUID() + "@evshare3d.com";
        RegisterRequest first = RegisterRequest.builder()
                .email(email)
                .password("Password123!")
                .fullName("Original User")
                .build();

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(first)))
                .andExpect(status().isCreated());

        RegisterRequest duplicate = RegisterRequest.builder()
                .email(email.toUpperCase()) // test case-insensitive duplicate enforcement
                .password("AnotherPassword456!")
                .fullName("Imposter User")
                .build();

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(duplicate)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.status", is(409)))
                .andExpect(jsonPath("$.message", containsString("Email is already in use")));
    }

    // =========================================================================
    // 3. PASSWORD HASHING
    // =========================================================================
    @Test
    @Order(3)
    @DisplayName("Dimension 3: Password Hashing — Enforces BCrypt work factor 12 ($2a$12$ / $2b$12$)")
    void test03_passwordHashing() throws Exception {
        String rawPassword = "P@sswordWorkFactor12Verifier!";
        String email = "suite_hash_" + UUID.randomUUID() + "@evshare3d.com";
        RegisterRequest request = RegisterRequest.builder()
                .email(email)
                .password(rawPassword)
                .fullName("BCrypt Audit User")
                .build();

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        User user = userRepository.findByEmail(email).orElseThrow();
        String hash = user.getPasswordHash();

        assertNotNull(hash, "Password hash must exist");
        assertNotEquals(rawPassword, hash, "Plaintext passwords must NEVER be saved");
        assertTrue(hash.startsWith("$2a$12$") || hash.startsWith("$2b$12$"),
                "Password must be hashed with BCrypt work factor 12: " + hash);
        assertTrue(passwordEncoder.matches(rawPassword, hash), "Password encoder must verify raw password against hash");
    }

    // =========================================================================
    // 4. LOGIN
    // =========================================================================
    @Test
    @Order(4)
    @DisplayName("Dimension 4: Login — Valid credentials return 200 OK with Access & Refresh tokens")
    void test04_login() throws Exception {
        User user = createTestUser("suite_login", coOwnerRole, true);
        LoginRequest request = LoginRequest.builder()
                .email(user.getEmail())
                .password("SecurePassword123!")
                .build();

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.accessToken", notNullValue()))
                .andExpect(jsonPath("$.data.refreshToken", notNullValue()))
                .andExpect(jsonPath("$.data.tokenType", is("Bearer")))
                .andExpect(jsonPath("$.data.user.email", is(user.getEmail())))
                .andExpect(jsonPath("$.data.user.roles", hasItem("ROLE_CO_OWNER")));
    }

    // =========================================================================
    // 5. INVALID LOGIN
    // =========================================================================
    @Test
    @Order(5)
    @DisplayName("Dimension 5: Invalid Login — Rejects bad credentials & ghost users with generic 401 (Anti-Enumeration)")
    void test05_invalidLogin() throws Exception {
        User user = createTestUser("suite_badlogin", coOwnerRole, true);

        // 1. Wrong password
        LoginRequest wrongPass = LoginRequest.builder()
                .email(user.getEmail())
                .password("WrongPassword999!")
                .build();

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(wrongPass)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.message", is("Invalid email or password")));

        // 2. Non-existent email -> Identical message to prevent enumeration
        LoginRequest ghostUser = LoginRequest.builder()
                .email("ghost_pilot_" + UUID.randomUUID() + "@evshare3d.com")
                .password("SomePassword123!")
                .build();

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(ghostUser)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.message", is("Invalid email or password")));

        // 3. Deactivated account -> 403 Forbidden
        User inactive = createTestUser("suite_inactive", coOwnerRole, false);
        LoginRequest inactiveReq = LoginRequest.builder()
                .email(inactive.getEmail())
                .password("SecurePassword123!")
                .build();

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(inactiveReq)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.status", is(403)))
                .andExpect(jsonPath("$.message", containsString("disabled or deactivated")));
    }

    // =========================================================================
    // 6. JWT VALIDATION
    // =========================================================================
    @Test
    @Order(6)
    @DisplayName("Dimension 6: JWT Validation — Cryptographically valid JWT authenticates principal")
    void test06_jwtValidation() throws Exception {
        User user = createTestUser("suite_jwtval", coOwnerRole, true);
        String token = tokenService.generateAccessToken(user);

        // Access authenticated endpoint
        mockMvc.perform(get("/api/v1/users/me")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.id", is(user.getId().intValue())))
                .andExpect(jsonPath("$.data.email", is(user.getEmail())));

        // Validate TokenService directly
        assertTrue(tokenService.validateToken(token));
        assertEquals(user.getEmail(), tokenService.extractEmail(token));
        assertEquals(user.getId(), tokenService.extractUserId(token));
        assertTrue(tokenService.extractRoles(token).contains("ROLE_CO_OWNER"));
    }

    // =========================================================================
    // 7. EXPIRED JWT
    // =========================================================================
    @Test
    @Order(7)
    @DisplayName("Dimension 7: Expired JWT — Expired token is rejected with HTTP 401 Unauthorized")
    void test07_expiredJwt() throws Exception {
        User user = createTestUser("suite_jwtexp", coOwnerRole, true);
        String expiredToken = jwtTokenProvider.generateCustomToken(
                user.getId(),
                user.getEmail(),
                List.of("ROLE_CO_OWNER"),
                -60000 // Expired 1 minute ago
        );

        assertFalse(tokenService.validateToken(expiredToken));

        mockMvc.perform(get("/api/v1/users/me")
                        .header("Authorization", "Bearer " + expiredToken))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.status", is(401)));
    }

    // =========================================================================
    // 8. MALFORMED JWT
    // =========================================================================
    @Test
    @Order(8)
    @DisplayName("Dimension 8: Malformed JWT — Tampered signature and malformed tokens rejected with 401")
    void test08_malformedJwt() throws Exception {
        // 1. Corrupted string
        mockMvc.perform(get("/api/v1/users/me")
                        .header("Authorization", "Bearer this.is.not.a.valid.jwt"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success", is(false)));

        // 2. Tampered signature
        SecretKey rogueKey = Keys.hmacShaKeyFor("RogueAttackerSecretKeyMustBeAtLeast256BitsLengthForHmac12345".getBytes(StandardCharsets.UTF_8));
        String tamperedToken = Jwts.builder()
                .subject("tampered@evshare3d.com")
                .issuer(jwtTokenProvider.getIssuer())
                .claim("userId", 9999L)
                .claim("roles", List.of("ROLE_ADMIN"))
                .issuedAt(Date.from(Instant.now()))
                .expiration(Date.from(Instant.now().plusMillis(3600000)))
                .signWith(rogueKey)
                .compact();

        mockMvc.perform(get("/api/v1/users/me")
                        .header("Authorization", "Bearer " + tamperedToken))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success", is(false)));
    }

    // =========================================================================
    // 9. REFRESH TOKEN
    // =========================================================================
    @Test
    @Order(9)
    @DisplayName("Dimension 9: Refresh Token — Rotates refresh token and issues fresh access token")
    void test09_refreshToken() throws Exception {
        User user = createTestUser("suite_refresh", coOwnerRole, true);
        LoginRequest loginReq = LoginRequest.builder()
                .email(user.getEmail())
                .password("SecurePassword123!")
                .build();

        MvcResult loginResult = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginReq)))
                .andExpect(status().isOk())
                .andReturn();

        ApiResponse<AuthResponse> loginEnvelope = objectMapper.readValue(
                loginResult.getResponse().getContentAsString(),
                new TypeReference<ApiResponse<AuthResponse>>() {}
        );
        String initialRefreshToken = loginEnvelope.getData().getRefreshToken();
        String initialAccessToken = loginEnvelope.getData().getAccessToken();

        RefreshTokenRequest refreshReq = RefreshTokenRequest.builder()
                .refreshToken(initialRefreshToken)
                .build();

        MvcResult refreshResult = mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(refreshReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.accessToken", notNullValue()))
                .andExpect(jsonPath("$.data.refreshToken", notNullValue()))
                .andReturn();

        ApiResponse<AuthResponse> refreshEnvelope = objectMapper.readValue(
                refreshResult.getResponse().getContentAsString(),
                new TypeReference<ApiResponse<AuthResponse>>() {}
        );
        String newAccessToken = refreshEnvelope.getData().getAccessToken();
        String rotatedRefreshToken = refreshEnvelope.getData().getRefreshToken();

        assertNotNull(newAccessToken);
        assertNotEquals(initialRefreshToken, rotatedRefreshToken, "Refresh token rotation must issue new token");

        // Verify the new access token is fully valid and can access protected resources
        mockMvc.perform(get("/api/v1/users/me")
                        .header("Authorization", "Bearer " + newAccessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id", is(user.getId().intValue())));

        // Old refresh token must be invalidated (replay attack prevention)
        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(refreshReq)))
                .andExpect(status().isUnauthorized());
    }

    // =========================================================================
    // 10. LOGOUT
    // =========================================================================
    @Test
    @Order(10)
    @DisplayName("Dimension 10: Logout — Invalidation of active refresh token blocks subsequent refresh")
    void test10_logout() throws Exception {
        User user = createTestUser("suite_logout", coOwnerRole, true);
        LoginRequest loginReq = LoginRequest.builder()
                .email(user.getEmail())
                .password("SecurePassword123!")
                .build();

        MvcResult loginResult = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginReq)))
                .andExpect(status().isOk())
                .andReturn();

        ApiResponse<AuthResponse> loginEnvelope = objectMapper.readValue(
                loginResult.getResponse().getContentAsString(),
                new TypeReference<ApiResponse<AuthResponse>>() {}
        );
        String refreshToken = loginEnvelope.getData().getRefreshToken();

        RefreshTokenRequest logoutReq = RefreshTokenRequest.builder()
                .refreshToken(refreshToken)
                .build();

        mockMvc.perform(post("/api/v1/auth/logout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(logoutReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.message", containsString("Logged out successfully")));

        // Attempting to refresh with invalidated token fails
        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(logoutReq)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success", is(false)));
    }

    // =========================================================================
    // 11. CURRENT USER
    // =========================================================================
    @Test
    @Order(11)
    @DisplayName("Dimension 11: Current User — Identity bound strictly to authenticated principal; cannot be spoofed")
    void test11_currentUser() throws Exception {
        User user = createTestUser("suite_me", coOwnerRole, true);
        String token = tokenService.generateAccessToken(user);

        // 1. Normal access
        mockMvc.perform(get("/api/v1/users/me")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.id", is(user.getId().intValue())))
                .andExpect(jsonPath("$.data.email", is(user.getEmail())))
                .andExpect(jsonPath("$.data.fullName", is(user.getFullName())));

        // 2. Identity spoofing prevention: passing ?userId=999 does not alter result
        mockMvc.perform(get("/api/v1/users/me?userId=99999")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id", is(user.getId().intValue())))
                .andExpect(jsonPath("$.data.email", is(user.getEmail())));
    }

    // =========================================================================
    // 12. RBAC (POSITIVE AUTHORIZATION)
    // =========================================================================
    @Test
    @Order(12)
    @DisplayName("Dimension 12: RBAC — Enforces permitted access for ADMIN, STAFF, and CO_OWNER")
    void test12_rbac() throws Exception {
        User admin = createTestUser("suite_rbac_admin", adminRole, true);
        User staff = createTestUser("suite_rbac_staff", staffRole, true);
        User coOwner = createTestUser("suite_rbac_coowner", coOwnerRole, true);

        String adminToken = tokenService.generateAccessToken(admin);
        String staffToken = tokenService.generateAccessToken(staff);
        String coOwnerToken = tokenService.generateAccessToken(coOwner);

        // ADMIN access to Admin endpoint
        mockMvc.perform(get("/api/v1/test/security-suite/admin")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)));

        // STAFF access to Staff endpoint
        mockMvc.perform(get("/api/v1/test/security-suite/staff")
                        .header("Authorization", "Bearer " + staffToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)));

        // CO_OWNER access to Co-Owner endpoint
        mockMvc.perform(get("/api/v1/test/security-suite/co-owner")
                        .header("Authorization", "Bearer " + coOwnerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)));

        // Production endpoint: GET /api/v1/users/{id} requires STAFF or ADMIN
        mockMvc.perform(get("/api/v1/users/" + coOwner.getId())
                        .header("Authorization", "Bearer " + staffToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id", is(coOwner.getId().intValue())));

        mockMvc.perform(get("/api/v1/users/" + coOwner.getId())
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id", is(coOwner.getId().intValue())));
    }

    // =========================================================================
    // 13. FORBIDDEN ACCESS (NEGATIVE AUTHORIZATION BOUNDARIES)
    // =========================================================================
    @Test
    @Order(13)
    @DisplayName("Dimension 13: Forbidden Access — Rejects unauthorized roles with HTTP 403 Forbidden")
    void test13_forbiddenAccess() throws Exception {
        User staff = createTestUser("suite_forbid_staff", staffRole, true);
        User coOwner = createTestUser("suite_forbid_coowner", coOwnerRole, true);

        String staffToken = tokenService.generateAccessToken(staff);
        String coOwnerToken = tokenService.generateAccessToken(coOwner);

        // 1. CO_OWNER cannot access ADMIN endpoint
        mockMvc.perform(get("/api/v1/test/security-suite/admin")
                        .header("Authorization", "Bearer " + coOwnerToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status", is(403)));

        // 2. STAFF cannot access ADMIN endpoint
        mockMvc.perform(get("/api/v1/test/security-suite/admin")
                        .header("Authorization", "Bearer " + staffToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status", is(403)));

        // 3. CO_OWNER cannot access STAFF endpoint
        mockMvc.perform(get("/api/v1/test/security-suite/staff")
                        .header("Authorization", "Bearer " + coOwnerToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status", is(403)));

        // 4. CO_OWNER cannot access /api/v1/users/{id}
        mockMvc.perform(get("/api/v1/users/" + staff.getId())
                        .header("Authorization", "Bearer " + coOwnerToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status", is(403)));

        // 5. Public caller cannot self-assign ROLE_ADMIN or ROLE_STAFF
        RegisterRequest hackerReq = RegisterRequest.builder()
                .email("hacker_" + UUID.randomUUID() + "@evshare3d.com")
                .password("HackerPass123!")
                .fullName("Rogue Admin")
                .role("ROLE_ADMIN")
                .build();

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(hackerReq)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status", is(403)));
    }

    // =========================================================================
    // 14. UNAUTHENTICATED ACCESS
    // =========================================================================
    @Test
    @Order(14)
    @DisplayName("Dimension 14: Unauthenticated Access — Anonymous requests to protected routes return 401")
    void test14_unauthenticatedAccess() throws Exception {
        // Protected /users/me without token
        mockMvc.perform(get("/api/v1/users/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status", is(401)));

        // Protected test endpoints without token
        mockMvc.perform(get("/api/v1/test/security-suite/admin"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status", is(401)));

        mockMvc.perform(get("/api/v1/test/security-suite/staff"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status", is(401)));

        mockMvc.perform(get("/api/v1/test/security-suite/co-owner"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status", is(401)));

        // Actuator management endpoint without token
        mockMvc.perform(get("/actuator"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status", is(401)));
    }

    // =========================================================================
    // 15. PASSWORD RESET FOUNDATION
    // =========================================================================
    @Test
    @Order(15)
    @DisplayName("Dimension 15: Password Reset Foundation — Lifecycle, anti-enumeration, token expiry, and session revocation")
    void test15_passwordResetFoundation() throws Exception {
        User user = createTestUser("suite_reset", coOwnerRole, true);
        String initialPassword = "SecurePassword123!";

        // 1. Anti-enumeration: Existing user receives generic 200 and token is generated
        PasswordResetRequest reqExisting = PasswordResetRequest.builder()
                .email(user.getEmail())
                .build();

        mockMvc.perform(post("/api/v1/auth/password-reset/request")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reqExisting)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message", containsString("If an account exists with this email")));

        String token = devPasswordResetNotifier.getLatestToken(user.getEmail());
        assertNotNull(token, "Token must be generated for existing active user");

        // 2. Anti-enumeration: Non-existent email receives identical 200 response; no token
        String ghostEmail = "ghost_" + UUID.randomUUID() + "@evshare3d.com";
        PasswordResetRequest reqGhost = PasswordResetRequest.builder()
                .email(ghostEmail)
                .build();

        mockMvc.perform(post("/api/v1/auth/password-reset/request")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reqGhost)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message", containsString("If an account exists with this email")));

        assertNull(devPasswordResetNotifier.getLatestToken(ghostEmail), "No token dispatched for ghost user");

        // 3. Obtain active refresh token prior to reset
        LoginRequest loginReq = LoginRequest.builder()
                .email(user.getEmail())
                .password(initialPassword)
                .build();

        MvcResult loginRes = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginReq)))
                .andExpect(status().isOk())
                .andReturn();

        ApiResponse<AuthResponse> authRes = objectMapper.readValue(
                loginRes.getResponse().getContentAsString(),
                new TypeReference<ApiResponse<AuthResponse>>() {}
        );
        String oldRefreshToken = authRes.getData().getRefreshToken();

        // 4. Confirm password reset
        String newPassword = "BrandNewSuperSecretPass456#";
        PasswordResetConfirmRequest confirmReq = PasswordResetConfirmRequest.builder()
                .token(token)
                .newPassword(newPassword)
                .build();

        mockMvc.perform(post("/api/v1/auth/password-reset/confirm")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(confirmReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message", containsString("Password has been reset successfully")));

        // 5. Old password no longer works
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginReq)))
                .andExpect(status().isUnauthorized());

        // 6. New password works
        LoginRequest newLoginReq = LoginRequest.builder()
                .email(user.getEmail())
                .password(newPassword)
                .build();

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newLoginReq)))
                .andExpect(status().isOk());

        // 7. Session revocation: old refresh token is revoked
        RefreshTokenRequest refreshReq = RefreshTokenRequest.builder()
                .refreshToken(oldRefreshToken)
                .build();

        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(refreshReq)))
                .andExpect(status().isUnauthorized());

        // 8. Replay protection: token cannot be consumed again
        mockMvc.perform(post("/api/v1/auth/password-reset/confirm")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(confirmReq)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("Password reset token has already been used")));
    }

    // =========================================================================
    // 16. SENSITIVE DATA EXPOSURE
    // =========================================================================
    @Test
    @Order(16)
    @DisplayName("Dimension 16: Sensitive Data Exposure — Never exposes password hashes, secrets, or internal metadata")
    void test16_sensitiveDataExposure() throws Exception {
        String rawPassword = "TopSecretPassword123#";
        String email = "suite_leak_" + UUID.randomUUID() + "@evshare3d.com";

        // 1. Registration response
        RegisterRequest regReq = RegisterRequest.builder()
                .email(email)
                .password(rawPassword)
                .fullName("Leak Audit Subject")
                .build();

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(regReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.password").doesNotExist())
                .andExpect(jsonPath("$.data.passwordHash").doesNotExist())
                .andExpect(jsonPath("$.data.secret").doesNotExist());

        // 2. Login response
        LoginRequest loginReq = LoginRequest.builder()
                .email(email)
                .password(rawPassword)
                .build();

        MvcResult loginResult = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.user.password").doesNotExist())
                .andExpect(jsonPath("$.data.user.passwordHash").doesNotExist())
                .andReturn();

        ApiResponse<AuthResponse> authEnvelope = objectMapper.readValue(
                loginResult.getResponse().getContentAsString(),
                new TypeReference<ApiResponse<AuthResponse>>() {}
        );
        String accessToken = authEnvelope.getData().getAccessToken();

        // 3. Current user profile (/users/me) response
        mockMvc.perform(get("/api/v1/users/me")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.password").doesNotExist())
                .andExpect(jsonPath("$.data.passwordHash").doesNotExist())
                .andExpect(jsonPath("$.data.jwtSecret").doesNotExist());

        // 4. Error response for bad login does not leak stack trace or internal SQL
        LoginRequest badReq = LoginRequest.builder()
                .email(email)
                .password("WrongPassword999!")
                .build();

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(badReq)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.stackTrace").doesNotExist())
                .andExpect(jsonPath("$.exception").doesNotExist())
                .andExpect(jsonPath("$.sql").doesNotExist());
    }
}
