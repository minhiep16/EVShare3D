package com.example.evshare.controller;

import com.example.evshare.dto.request.LoginRequest;
import com.example.evshare.entity.Role;
import com.example.evshare.entity.User;
import com.example.evshare.entity.enums.RoleName;
import com.example.evshare.repository.RoleRepository;
import com.example.evshare.repository.UserRepository;
import com.example.evshare.security.JwtTokenProvider;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Set;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@DisplayName("Checkpoint 03-D — User Authentication & Login Integration Tests")
class UserLoginIntegrationTest {

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

    @Test
    @DisplayName("1. Successful Login: Returns 200 OK with Access & Refresh JWTs and sanitized User profile")
    void testSuccessfulLogin() throws Exception {
        Role coOwnerRole = roleRepository.findByName(RoleName.ROLE_CO_OWNER)
                .orElseGet(() -> roleRepository.save(new Role(RoleName.ROLE_CO_OWNER)));

        User user = new User();
        user.setEmail("active_user@evshare3d.com");
        user.setPasswordHash(passwordEncoder.encode("ValidSecret123!"));
        user.setFullName("Active Co-Owner");
        user.setIsActive(true);
        user.setCreatedAt(Instant.now());
        user.setUpdatedAt(Instant.now());
        user.setRoles(Set.of(coOwnerRole));
        userRepository.save(user);

        LoginRequest request = LoginRequest.builder()
                .email("active_user@evshare3d.com")
                .password("ValidSecret123!")
                .build();

        String responseBody = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.message", containsString("successful")))
                .andExpect(jsonPath("$.data.accessToken", notNullValue()))
                .andExpect(jsonPath("$.data.refreshToken", notNullValue()))
                .andExpect(jsonPath("$.data.tokenType", is("Bearer")))
                .andExpect(jsonPath("$.data.expiresIn", is(900)))
                .andExpect(jsonPath("$.data.user.email", is("active_user@evshare3d.com")))
                .andExpect(jsonPath("$.data.user.fullName", is("Active Co-Owner")))
                .andExpect(jsonPath("$.data.user.roles", hasItem("ROLE_CO_OWNER")))
                .andReturn().getResponse().getContentAsString();

        // Extract token and verify against JwtTokenProvider
        String accessToken = objectMapper.readTree(responseBody).path("data").path("accessToken").asText();
        assertTrue(jwtTokenProvider.validateToken(accessToken), "Issued access token must be cryptographically valid");
        assertEquals("active_user@evshare3d.com", jwtTokenProvider.extractEmail(accessToken));
    }

    @Test
    @DisplayName("2. Wrong Password: Password mismatch returns 401 Unauthorized with generic message")
    void testWrongPasswordFailsWith401() throws Exception {
        Role coOwnerRole = roleRepository.findByName(RoleName.ROLE_CO_OWNER)
                .orElseGet(() -> roleRepository.save(new Role(RoleName.ROLE_CO_OWNER)));

        User user = new User();
        user.setEmail("user_wrong_pass@evshare3d.com");
        user.setPasswordHash(passwordEncoder.encode("CorrectPass123!"));
        user.setFullName("Password Test User");
        user.setIsActive(true);
        user.setRoles(Set.of(coOwnerRole));
        userRepository.save(user);

        LoginRequest request = LoginRequest.builder()
                .email("user_wrong_pass@evshare3d.com")
                .password("WrongGuessPass999#")
                .build();

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.status", is(401)))
                .andExpect(jsonPath("$.message", is("Invalid email or password")));
    }

    @Test
    @DisplayName("3. Unknown Email: Non-existent email returns identical 401 Unauthorized preventing enumeration")
    void testUnknownEmailFailsWith401() throws Exception {
        LoginRequest request = LoginRequest.builder()
                .email("ghost_user_does_not_exist@evshare3d.com")
                .password("AnyPassword123!")
                .build();

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.status", is(401)))
                // Must be identical message to wrong password to prevent user enumeration attacks
                .andExpect(jsonPath("$.message", is("Invalid email or password")));
    }

    @Test
    @DisplayName("4. Disabled Account: Deactivated user is blocked with 403 Forbidden")
    void testDisabledAccountFailsWith403() throws Exception {
        Role coOwnerRole = roleRepository.findByName(RoleName.ROLE_CO_OWNER)
                .orElseGet(() -> roleRepository.save(new Role(RoleName.ROLE_CO_OWNER)));

        User disabledUser = new User();
        disabledUser.setEmail("banned_user@evshare3d.com");
        disabledUser.setPasswordHash(passwordEncoder.encode("ValidPass123!"));
        disabledUser.setFullName("Banned Account");
        disabledUser.setIsActive(false); // Account disabled
        disabledUser.setRoles(Set.of(coOwnerRole));
        userRepository.save(disabledUser);

        LoginRequest request = LoginRequest.builder()
                .email("banned_user@evshare3d.com")
                .password("ValidPass123!")
                .build();

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.status", is(403)))
                .andExpect(jsonPath("$.message", containsString("disabled or deactivated")));
    }

    @Test
    @DisplayName("5. Correct Role Returned: Admin role is accurately mapped and reflected in response")
    void testCorrectRoleReturned() throws Exception {
        Role adminRole = roleRepository.findByName(RoleName.ROLE_ADMIN)
                .orElseGet(() -> roleRepository.save(new Role(RoleName.ROLE_ADMIN)));

        User adminUser = new User();
        adminUser.setEmail("superadmin@evshare3d.com");
        adminUser.setPasswordHash(passwordEncoder.encode("AdminPass123!"));
        adminUser.setFullName("Platform Admin");
        adminUser.setIsActive(true);
        adminUser.setRoles(Set.of(adminRole));
        userRepository.save(adminUser);

        LoginRequest request = LoginRequest.builder()
                .email("superadmin@evshare3d.com")
                .password("AdminPass123!")
                .build();

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.user.roles", hasItem("ROLE_ADMIN")))
                .andExpect(jsonPath("$.data.user.roles", not(hasItem("ROLE_CO_OWNER"))));
    }

    @Test
    @DisplayName("6. Sensitive Information Not Exposed: Response payload strictly omits password hash and secrets")
    void testSensitiveInformationNotExposedInLoginResponse() throws Exception {
        Role coOwnerRole = roleRepository.findByName(RoleName.ROLE_CO_OWNER)
                .orElseGet(() -> roleRepository.save(new Role(RoleName.ROLE_CO_OWNER)));

        User user = new User();
        user.setEmail("safe_login@evshare3d.com");
        user.setPasswordHash(passwordEncoder.encode("SafePassword123!"));
        user.setFullName("Privacy Preserved User");
        user.setIsActive(true);
        user.setRoles(Set.of(coOwnerRole));
        userRepository.save(user);

        LoginRequest request = LoginRequest.builder()
                .email("safe_login@evshare3d.com")
                .password("SafePassword123!")
                .build();

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.user.password").doesNotExist())
                .andExpect(jsonPath("$.data.user.passwordHash").doesNotExist())
                .andExpect(jsonPath("$.data.user.secret").doesNotExist())
                .andExpect(jsonPath("$.data.secret").doesNotExist())
                .andExpect(jsonPath("$.data.jwtSecret").doesNotExist());
    }

    @Test
    @DisplayName("7. Missing/Blank Credentials: Validation rejects empty email or password with 400")
    void testMissingCredentialsFailsWith400() throws Exception {
        LoginRequest request = LoginRequest.builder()
                .email("")
                .password("")
                .build();

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.validationErrors", hasSize(greaterThanOrEqualTo(2))));
    }
}
