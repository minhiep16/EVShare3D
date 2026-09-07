package com.example.evshare.controller;

import com.example.evshare.dto.request.LoginRequest;
import com.example.evshare.dto.request.RefreshTokenRequest;
import com.example.evshare.dto.response.ApiResponse;
import com.example.evshare.dto.response.AuthResponse;
import com.example.evshare.entity.Role;
import com.example.evshare.entity.User;
import com.example.evshare.entity.enums.RoleName;
import com.example.evshare.repository.RoleRepository;
import com.example.evshare.repository.UserRepository;
import com.example.evshare.security.JwtTokenProvider;
import com.example.evshare.security.RefreshTokenStore;
import com.example.evshare.security.TokenService;
import com.example.evshare.security.UserPrincipal;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
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
@Import(AuthRefreshAndLogoutIntegrationTest.ProtectedResourceController.class)
@DisplayName("Checkpoint 03-F — Refresh Token & Logout Integration Tests")
class AuthRefreshAndLogoutIntegrationTest {

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

    @Autowired
    private RefreshTokenStore refreshTokenStore;

    private User testUser;
    private String testUserPassword = "SecurePassword123!";

    @RestController
    @RequestMapping("/api/v1/test/auth-guard")
    static class ProtectedResourceController {
        @GetMapping
        public ResponseEntity<ApiResponse<String>> accessProtected(@AuthenticationPrincipal UserPrincipal principal) {
            return ResponseEntity.ok(ApiResponse.ok("Access granted to " + principal.getUsername()));
        }
    }

    @BeforeEach
    void setUp() {
        Role role = roleRepository.findByName(RoleName.ROLE_CO_OWNER)
                .orElseGet(() -> roleRepository.save(new Role(RoleName.ROLE_CO_OWNER)));

        testUser = new User();
        testUser.setEmail("refresh_test_" + UUID.randomUUID() + "@evshare3d.com");
        testUser.setPasswordHash(passwordEncoder.encode(testUserPassword));
        testUser.setFullName("Refresh Pilot");
        testUser.setIsActive(true);
        testUser.setCreatedAt(Instant.now());
        testUser.setUpdatedAt(Instant.now());
        testUser.setRoles(Set.of(role));

        testUser = userRepository.save(testUser);
    }

    private AuthResponse authenticateUser() throws Exception {
        LoginRequest loginRequest = new LoginRequest(testUser.getEmail(), testUserPassword);
        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn();

        ApiResponse<AuthResponse> apiResponse = objectMapper.readValue(
                result.getResponse().getContentAsString(),
                new TypeReference<ApiResponse<AuthResponse>>() {}
        );
        return apiResponse.getData();
    }

    @Test
    @DisplayName("1. Valid Refresh: Successfully exchanges valid refresh token for fresh access and rotated refresh token")
    void testValidRefresh() throws Exception {
        AuthResponse loginAuth = authenticateUser();
        assertNotNull(loginAuth.getRefreshToken());

        RefreshTokenRequest refreshRequest = new RefreshTokenRequest(loginAuth.getRefreshToken());

        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(refreshRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.message", containsString("Token refreshed successfully")))
                .andExpect(jsonPath("$.data.accessToken", notNullValue()))
                .andExpect(jsonPath("$.data.refreshToken", notNullValue()))
                .andExpect(jsonPath("$.data.tokenType", is("Bearer")))
                .andExpect(jsonPath("$.data.expiresIn", is(900)))
                .andExpect(jsonPath("$.data.user.id", is(testUser.getId().intValue())))
                .andExpect(jsonPath("$.data.user.email", is(testUser.getEmail())));
    }

    @Test
    @DisplayName("2. Expired Refresh: Expired refresh token is rejected with HTTP 401 Unauthorized")
    void testExpiredRefresh() throws Exception {
        String expiredRefreshToken = jwtTokenProvider.generateExpiredRefreshToken(
                testUser.getId(),
                testUser.getEmail(),
                -60000 // Expired 1 minute ago
        );

        RefreshTokenRequest refreshRequest = new RefreshTokenRequest(expiredRefreshToken);

        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(refreshRequest)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.status", is(401)))
                .andExpect(jsonPath("$.message", containsString("Invalid or expired refresh token")));
    }

    @Test
    @DisplayName("3. Invalid Refresh (Malformed): Corrupted or non-JWT strings return HTTP 401")
    void testMalformedRefresh() throws Exception {
        RefreshTokenRequest refreshRequest = new RefreshTokenRequest("corrupted.malformed.jwt.token");

        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(refreshRequest)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.status", is(401)));
    }

    @Test
    @DisplayName("4. Invalid Refresh (Access Token Provided): Access token passed as refresh token is rejected with HTTP 400")
    void testAccessTokenRejectedAsRefreshToken() throws Exception {
        String accessToken = tokenService.generateAccessToken(testUser);

        RefreshTokenRequest refreshRequest = new RefreshTokenRequest(accessToken);

        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(refreshRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.message", containsString("refresh token expected")));
    }

    @Test
    @DisplayName("5. Invalid Signature: Refresh token signed with unauthorized key is rejected with HTTP 401")
    void testInvalidSignatureRefresh() throws Exception {
        SecretKey rogueKey = Keys.hmacShaKeyFor("RogueSecretKeyMustBeAtLeast256BitsLongForHmacSha256Security12345".getBytes(StandardCharsets.UTF_8));
        String tamperedToken = Jwts.builder()
                .id(UUID.randomUUID().toString())
                .subject(testUser.getEmail())
                .issuer(jwtTokenProvider.getIssuer())
                .claim("userId", testUser.getId())
                .claim("tokenType", "REFRESH")
                .issuedAt(new Date())
                .expiration(Date.from(Instant.now().plusSeconds(3600)))
                .signWith(rogueKey)
                .compact();

        RefreshTokenRequest refreshRequest = new RefreshTokenRequest(tamperedToken);

        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(refreshRequest)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.status", is(401)));
    }

    @Test
    @DisplayName("6. Revoked Refresh: Explicitly revoked refresh token is rejected with HTTP 401")
    void testRevokedRefresh() throws Exception {
        AuthResponse loginAuth = authenticateUser();
        String refreshToken = loginAuth.getRefreshToken();
        String tokenId = tokenService.extractTokenId(refreshToken);

        // Manually revoke the token in the store
        refreshTokenStore.revokeToken(tokenId);

        RefreshTokenRequest refreshRequest = new RefreshTokenRequest(refreshToken);

        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(refreshRequest)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.status", is(401)))
                .andExpect(jsonPath("$.message", containsString("revoked")));
    }

    @Test
    @DisplayName("7. Logout: Successfully terminates session and invalidates active refresh token (HTTP 200)")
    void testLogout() throws Exception {
        AuthResponse loginAuth = authenticateUser();
        String refreshToken = loginAuth.getRefreshToken();
        String tokenId = tokenService.extractTokenId(refreshToken);

        RefreshTokenRequest logoutRequest = new RefreshTokenRequest(refreshToken);

        mockMvc.perform(post("/api/v1/auth/logout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(logoutRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.message", containsString("Logged out successfully")));

        assertTrue(refreshTokenStore.isRevoked(tokenId), "Refresh token must be marked as revoked after logout");
    }

    @Test
    @DisplayName("8. Refresh After Logout: Attempting refresh with logged-out token fails with HTTP 401")
    void testRefreshAfterLogout() throws Exception {
        AuthResponse loginAuth = authenticateUser();
        String refreshToken = loginAuth.getRefreshToken();

        // 1. Perform logout
        RefreshTokenRequest logoutRequest = new RefreshTokenRequest(refreshToken);
        mockMvc.perform(post("/api/v1/auth/logout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(logoutRequest)))
                .andExpect(status().isOk());

        // 2. Attempt refresh using the logged-out token
        RefreshTokenRequest refreshRequest = new RefreshTokenRequest(refreshToken);
        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(refreshRequest)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.status", is(401)))
                .andExpect(jsonPath("$.message", containsString("revoked")));
    }

    @Test
    @DisplayName("9. Token Rotation & Reuse Prevention: Reusing a rotated refresh token is detected and rejected")
    void testTokenRotationAndReusePrevention() throws Exception {
        AuthResponse loginAuth = authenticateUser();
        String originalRefreshToken = loginAuth.getRefreshToken();

        // First refresh: Exchanges R1 for (A2, R2)
        RefreshTokenRequest firstRefreshRequest = new RefreshTokenRequest(originalRefreshToken);
        MvcResult firstRefreshResult = mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(firstRefreshRequest)))
                .andExpect(status().isOk())
                .andReturn();

        ApiResponse<AuthResponse> firstRefreshResponse = objectMapper.readValue(
                firstRefreshResult.getResponse().getContentAsString(),
                new TypeReference<ApiResponse<AuthResponse>>() {}
        );
        String rotatedRefreshToken = firstRefreshResponse.getData().getRefreshToken();
        assertNotEquals(originalRefreshToken, rotatedRefreshToken, "Rotated refresh token must be distinct");

        // Attempt to reuse original token R1 -> MUST BE REJECTED (RFC 6819 Token Reuse Detection)
        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(firstRefreshRequest)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.status", is(401)))
                .andExpect(jsonPath("$.message", containsString("already been rotated")));

        // Attempting to refresh with new token R2 is now also invalidated due to reuse breach mitigation
        RefreshTokenRequest secondRefreshRequest = new RefreshTokenRequest(rotatedRefreshToken);
        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(secondRefreshRequest)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.status", is(401)));
    }

    @Test
    @DisplayName("10. Segregation: Refresh token cannot be used as Bearer access token for protected API routes")
    void testRefreshTokenCannotBeUsedAsAccessToken() throws Exception {
        AuthResponse loginAuth = authenticateUser();
        String refreshToken = loginAuth.getRefreshToken();

        // Attempt to call protected endpoint with Refresh Token in Authorization header
        mockMvc.perform(get("/api/v1/test/auth-guard")
                        .header("Authorization", "Bearer " + refreshToken)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.status", is(401)))
                .andExpect(jsonPath("$.message", containsString("Authentication required")));

        // Valid access token succeeds on the same route
        mockMvc.perform(get("/api/v1/test/auth-guard")
                        .header("Authorization", "Bearer " + loginAuth.getAccessToken())
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data", containsString("Access granted to " + testUser.getEmail())));
    }
}
