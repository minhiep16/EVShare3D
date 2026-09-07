package com.example.evshare.controller;

import com.example.evshare.dto.request.LoginRequest;
import com.example.evshare.dto.request.PasswordResetConfirmRequest;
import com.example.evshare.dto.request.PasswordResetRequest;
import com.example.evshare.dto.request.RefreshTokenRequest;
import com.example.evshare.dto.response.ApiResponse;
import com.example.evshare.dto.response.AuthResponse;
import com.example.evshare.entity.Role;
import com.example.evshare.entity.User;
import com.example.evshare.entity.enums.RoleName;
import com.example.evshare.repository.RoleRepository;
import com.example.evshare.repository.UserRepository;
import com.example.evshare.security.DevPasswordResetNotifier;
import com.example.evshare.security.PasswordResetTokenStore;
import com.example.evshare.security.RefreshTokenStore;
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

import java.time.Duration;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@DisplayName("Checkpoint 03-I — Password Reset Foundation Integration Tests")
class PasswordResetIntegrationTest {

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
    private PasswordResetTokenStore passwordResetTokenStore;

    @Autowired
    private DevPasswordResetNotifier devPasswordResetNotifier;

    @Autowired
    private RefreshTokenStore refreshTokenStore;

    private User activeUser;
    private final String initialPassword = "InitialPassword123!";

    @BeforeEach
    void setUp() {
        devPasswordResetNotifier.clear();

        Role coOwnerRole = roleRepository.findByName(RoleName.ROLE_CO_OWNER)
                .orElseGet(() -> roleRepository.save(new Role(RoleName.ROLE_CO_OWNER)));

        activeUser = new User();
        activeUser.setEmail("reset_pilot_" + UUID.randomUUID() + "@evshare3d.com");
        activeUser.setPasswordHash(passwordEncoder.encode(initialPassword));
        activeUser.setFullName("Reset Test User");
        activeUser.setIsActive(true);
        activeUser.setCreatedAt(Instant.now());
        activeUser.setUpdatedAt(Instant.now());
        activeUser.setRoles(new HashSet<>(Set.of(coOwnerRole)));
        activeUser = userRepository.save(activeUser);
    }

    // =========================================================================
    // 1. PASSWORD RESET REQUEST TESTS & ANTI-ENUMERATION
    // =========================================================================

    @Test
    @DisplayName("1. Reset Request (Positive): Existing active user receives generic HTTP 200 and token is issued")
    void testResetRequest_ExistingUser_SuccessAntiEnumeration() throws Exception {
        PasswordResetRequest request = PasswordResetRequest.builder()
                .email(activeUser.getEmail())
                .build();

        mockMvc.perform(post("/api/v1/auth/password-reset/request")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.message", containsString("If an account exists with this email")));

        // Verify token was recorded and dispatched via the development notifier
        String token = devPasswordResetNotifier.getLatestToken(activeUser.getEmail());
        assertNotNull(token, "Reset token must be generated for existing active user");
        assertTrue(passwordResetTokenStore.getValidToken(token).isPresent(), "Generated token must exist in store");
    }

    @Test
    @DisplayName("2. Anti-Enumeration: Non-existent email receives identical HTTP 200 response; no token issued")
    void testResetRequest_NonExistentEmail_AntiEnumerationGenericSuccess() throws Exception {
        String nonExistentEmail = "ghost_pilot_" + UUID.randomUUID() + "@evshare3d.com";
        PasswordResetRequest request = PasswordResetRequest.builder()
                .email(nonExistentEmail)
                .build();

        mockMvc.perform(post("/api/v1/auth/password-reset/request")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.message", containsString("If an account exists with this email")));

        // Verify no token was dispatched
        String token = devPasswordResetNotifier.getLatestToken(nonExistentEmail);
        assertNull(token, "No token must be generated for non-existent email");
    }

    @Test
    @DisplayName("3. Anti-Enumeration: Deactivated user receives identical HTTP 200 response; no token dispatched")
    void testResetRequest_DeactivatedUser_AntiEnumerationGenericSuccess() throws Exception {
        activeUser.setIsActive(false);
        userRepository.save(activeUser);

        PasswordResetRequest request = PasswordResetRequest.builder()
                .email(activeUser.getEmail())
                .build();

        mockMvc.perform(post("/api/v1/auth/password-reset/request")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.message", containsString("If an account exists with this email")));

        // Verify notification was suppressed for deactivated account
        String token = devPasswordResetNotifier.getLatestToken(activeUser.getEmail());
        assertNull(token, "No token must be dispatched for deactivated account");
    }

    @Test
    @DisplayName("4. Validation: Malformed email format triggers HTTP 400 Bad Request")
    void testResetRequest_InvalidEmailFormat_BadRequest() throws Exception {
        PasswordResetRequest request = PasswordResetRequest.builder()
                .email("not-a-valid-email")
                .build();

        mockMvc.perform(post("/api/v1/auth/password-reset/request")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.status", is(400)));
    }

    @Test
    @DisplayName("5. Route Alias: /api/v1/auth/forgot-password route operates identically")
    void testResetRequest_RouteAlias_Success() throws Exception {
        PasswordResetRequest request = PasswordResetRequest.builder()
                .email(activeUser.getEmail())
                .build();

        mockMvc.perform(post("/api/v1/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)));
    }

    // =========================================================================
    // 2. PASSWORD RESET CONFIRMATION & SUCCESS TESTS
    // =========================================================================

    @Test
    @DisplayName("6. Confirm Reset (Positive): Successful reset allows login with new password and rejects old password")
    void testConfirmPasswordReset_Success_AllowsLoginWithNewPassword() throws Exception {
        // Step 1: Request reset
        String token = passwordResetTokenStore.createToken(activeUser.getId(), activeUser.getEmail(), Duration.ofMinutes(15));
        String newPassword = "BrandNewSecretPassword123!";

        // Step 2: Confirm password reset
        PasswordResetConfirmRequest confirmRequest = PasswordResetConfirmRequest.builder()
                .token(token)
                .newPassword(newPassword)
                .build();

        mockMvc.perform(post("/api/v1/auth/password-reset/confirm")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(confirmRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.message", containsString("Password has been reset successfully")));

        // Step 3: Verify old password no longer works (HTTP 401)
        LoginRequest oldLogin = LoginRequest.builder()
                .email(activeUser.getEmail())
                .password(initialPassword)
                .build();

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(oldLogin)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success", is(false)));

        // Step 4: Verify new password logs in successfully (HTTP 200)
        LoginRequest newLogin = LoginRequest.builder()
                .email(activeUser.getEmail())
                .password(newPassword)
                .build();

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newLogin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.accessToken").isString());
    }

    @Test
    @DisplayName("7. Password Hashing: Password is encrypted using BCrypt work factor 12")
    void testConfirmPasswordReset_PasswordHashedWithBCrypt() throws Exception {
        String token = passwordResetTokenStore.createToken(activeUser.getId(), activeUser.getEmail(), Duration.ofMinutes(15));
        String newPassword = "CryptoVerifiedPass888!";

        PasswordResetConfirmRequest confirmRequest = PasswordResetConfirmRequest.builder()
                .token(token)
                .newPassword(newPassword)
                .build();

        mockMvc.perform(post("/api/v1/auth/password-reset/confirm")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(confirmRequest)))
                .andExpect(status().isOk());

        // Inspect database state
        User updatedUser = userRepository.findById(activeUser.getId()).orElseThrow();
        String storedHash = updatedUser.getPasswordHash();

        assertNotNull(storedHash);
        assertNotEquals(newPassword, storedHash, "Plaintext password must NEVER be stored in the database");
        assertTrue(storedHash.startsWith("$2a$12$") || storedHash.startsWith("$2b$12$"),
                "Password must be hashed with BCrypt work factor 12 ($2a$12$ or $2b$12$)");
        assertTrue(passwordEncoder.matches(newPassword, storedHash), "Stored hash must match the new password");
    }

    // =========================================================================
    // 3. NEGATIVE TOKEN TESTS: INVALID, EXPIRED, CONSUMED
    // =========================================================================

    @Test
    @DisplayName("8. Invalid Token: Unknown/malformed reset token is rejected (HTTP 400 Bad Request)")
    void testConfirmPasswordReset_InvalidToken_BadRequest() throws Exception {
        PasswordResetConfirmRequest confirmRequest = PasswordResetConfirmRequest.builder()
                .token("totally-invalid-and-unknown-reset-token-xyz")
                .newPassword("ValidPassword123!")
                .build();

        mockMvc.perform(post("/api/v1/auth/password-reset/confirm")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(confirmRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.message", containsString("Invalid or expired password reset token")));
    }

    @Test
    @DisplayName("9. Expired Token: Expired reset token is rejected (HTTP 400 Bad Request)")
    void testConfirmPasswordReset_ExpiredToken_BadRequest() throws Exception {
        // Issue token that already expired 1 minute ago
        String expiredToken = passwordResetTokenStore.createToken(
                activeUser.getId(),
                activeUser.getEmail(),
                Duration.ofMinutes(-1)
        );

        PasswordResetConfirmRequest confirmRequest = PasswordResetConfirmRequest.builder()
                .token(expiredToken)
                .newPassword("ValidPassword123!")
                .build();

        mockMvc.perform(post("/api/v1/auth/password-reset/confirm")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(confirmRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.message", containsString("Password reset token has expired")));
    }

    @Test
    @DisplayName("10. Single-Use Replay Protection: Token cannot be consumed twice (HTTP 400 Bad Request)")
    void testConfirmPasswordReset_TokenReuse_Denied() throws Exception {
        String token = passwordResetTokenStore.createToken(activeUser.getId(), activeUser.getEmail(), Duration.ofMinutes(15));

        PasswordResetConfirmRequest firstUse = PasswordResetConfirmRequest.builder()
                .token(token)
                .newPassword("FirstNewPassword123!")
                .build();

        // First use succeeds
        mockMvc.perform(post("/api/v1/auth/password-reset/confirm")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(firstUse)))
                .andExpect(status().isOk());

        // Replay attempt with same token fails
        PasswordResetConfirmRequest secondUse = PasswordResetConfirmRequest.builder()
                .token(token)
                .newPassword("SecondNewPassword123!")
                .build();

        mockMvc.perform(post("/api/v1/auth/password-reset/confirm")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(secondUse)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.message", containsString("Password reset token has already been used")));
    }

    // =========================================================================
    // 4. PASSWORD POLICY ENFORCEMENT ON RESET
    // =========================================================================

    @Test
    @DisplayName("11. Password Policy: Password < 8 characters is rejected (HTTP 400 Bad Request)")
    void testConfirmPasswordReset_ShortPassword_BadRequest() throws Exception {
        String token = passwordResetTokenStore.createToken(activeUser.getId(), activeUser.getEmail(), Duration.ofMinutes(15));

        PasswordResetConfirmRequest confirmRequest = PasswordResetConfirmRequest.builder()
                .token(token)
                .newPassword("short") // less than 8 characters
                .build();

        mockMvc.perform(post("/api/v1/auth/password-reset/confirm")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(confirmRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success", is(false)));
    }

    @Test
    @DisplayName("12. Password Policy: Password with whitespace is rejected (HTTP 400 Bad Request)")
    void testConfirmPasswordReset_PasswordWithSpaces_BadRequest() throws Exception {
        String token = passwordResetTokenStore.createToken(activeUser.getId(), activeUser.getEmail(), Duration.ofMinutes(15));

        PasswordResetConfirmRequest confirmRequest = PasswordResetConfirmRequest.builder()
                .token(token)
                .newPassword("Invalid Pass 123!") // contains whitespace
                .build();

        mockMvc.perform(post("/api/v1/auth/password-reset/confirm")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(confirmRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.message", containsString("Password cannot contain whitespace")));
    }

    // =========================================================================
    // 5. SESSION & REFRESH TOKEN INVALIDATION (SECURITY REQUIREMENT 6)
    // =========================================================================

    @Test
    @DisplayName("13. Session Revocation: Successful password reset revokes all prior refresh tokens")
    void testConfirmPasswordReset_RevokesPriorRefreshTokens() throws Exception {
        // Step 1: User logs in and acquires active refresh token
        LoginRequest loginRequest = LoginRequest.builder()
                .email(activeUser.getEmail())
                .password(initialPassword)
                .build();

        MvcResult loginResult = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn();

        ApiResponse<AuthResponse> authResponse = objectMapper.readValue(
                loginResult.getResponse().getContentAsString(),
                new TypeReference<ApiResponse<AuthResponse>>() {}
        );
        String oldRefreshToken = authResponse.getData().getRefreshToken();
        assertNotNull(oldRefreshToken);

        // Step 2: Verify old refresh token can refresh prior to password reset
        RefreshTokenRequest refreshReq = RefreshTokenRequest.builder()
                .refreshToken(oldRefreshToken)
                .build();

        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(refreshReq)))
                .andExpect(status().isOk());

        // Step 3: Perform password reset
        String resetToken = passwordResetTokenStore.createToken(activeUser.getId(), activeUser.getEmail(), Duration.ofMinutes(15));
        PasswordResetConfirmRequest confirmRequest = PasswordResetConfirmRequest.builder()
                .token(resetToken)
                .newPassword("SuperSecureReplacementPass123!")
                .build();

        mockMvc.perform(post("/api/v1/auth/password-reset/confirm")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(confirmRequest)))
                .andExpect(status().isOk());

        // Step 4: Attempt to use the old refresh token after password reset -> must fail (HTTP 401 Unauthorized)
        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(refreshReq)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success", is(false)));
    }
}
