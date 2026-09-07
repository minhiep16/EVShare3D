package com.example.evshare.controller;

import com.example.evshare.dto.request.RegisterRequest;
import com.example.evshare.entity.User;
import com.example.evshare.repository.UserRepository;
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

import java.util.Optional;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@DisplayName("Checkpoint 03-C — User Registration Integration Tests")
class UserRegistrationIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    @DisplayName("1. Valid Registration: Successfully registers user with default ROLE_CO_OWNER and returns 201 Created")
    void testValidRegistration() throws Exception {
        RegisterRequest request = RegisterRequest.builder()
                .email("pilot_reg_1@evshare3d.com")
                .password("StrongPassword123!")
                .fullName("Le Van Pilot")
                .phoneNumber("+84988111222")
                .avatar3dUrl("https://assets.evshare3d.com/avatars/pilot.glb")
                .build();

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.message", containsString("registered successfully")))
                .andExpect(jsonPath("$.data.id", notNullValue()))
                .andExpect(jsonPath("$.data.email", is("pilot_reg_1@evshare3d.com")))
                .andExpect(jsonPath("$.data.fullName", is("Le Van Pilot")))
                .andExpect(jsonPath("$.data.phoneNumber", is("+84988111222")))
                .andExpect(jsonPath("$.data.avatar3dUrl", is("https://assets.evshare3d.com/avatars/pilot.glb")))
                .andExpect(jsonPath("$.data.isActive", is(true)))
                .andExpect(jsonPath("$.data.roles", hasItem("ROLE_CO_OWNER")));

        // Verify persistence in repository
        Optional<User> savedUser = userRepository.findByEmail("pilot_reg_1@evshare3d.com");
        assertTrue(savedUser.isPresent(), "User must be persisted in database");
        assertEquals("Le Van Pilot", savedUser.get().getFullName());
    }

    @Test
    @DisplayName("2. Duplicate Email: Registration with existing email returns 409 Conflict")
    void testDuplicateEmailRegistrationFailsWith409() throws Exception {
        RegisterRequest initialRequest = RegisterRequest.builder()
                .email("duplicate_test@evshare3d.com")
                .password("Password123!")
                .fullName("First Registrant")
                .build();

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(initialRequest)))
                .andExpect(status().isCreated());

        // Attempt duplicate registration (case-insensitive duplicate check)
        RegisterRequest duplicateRequest = RegisterRequest.builder()
                .email("DUPLICATE_TEST@evshare3d.com")
                .password("AnotherPassword456!")
                .fullName("Second Registrant")
                .build();

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(duplicateRequest)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.status", is(409)))
                .andExpect(jsonPath("$.message", containsString("Email is already in use")));
    }

    @Test
    @DisplayName("3. Invalid Email: Invalid email syntax returns 400 Bad Request with validation details")
    void testInvalidEmailFormatFailsWith400() throws Exception {
        RegisterRequest request = RegisterRequest.builder()
                .email("not-a-valid-email-string")
                .password("ValidPass123!")
                .fullName("Invalid Email User")
                .build();

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.validationErrors", hasItem(hasEntry("field", "email"))));
    }

    @Test
    @DisplayName("4. Weak Password: Password shorter than 8 characters returns 400 Bad Request")
    void testWeakPasswordFailsWith400() throws Exception {
        RegisterRequest request = RegisterRequest.builder()
                .email("weak_pass@evshare3d.com")
                .password("short") // less than 8 characters
                .fullName("Weak Password User")
                .build();

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.status", is(400)));
    }

    @Test
    @DisplayName("5. Missing Required Fields: Missing full name or password returns 400 Bad Request")
    void testMissingRequiredFieldsFailsWith400() throws Exception {
        RegisterRequest request = RegisterRequest.builder()
                .email("missing_fields@evshare3d.com")
                .fullName("") // blank full name
                .password(null) // missing password
                .build();

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.validationErrors", hasSize(greaterThanOrEqualTo(2))));
    }

    @Test
    @DisplayName("6. Attempt to Assign ADMIN: Self-assignment of privileged roles is forbidden (403)")
    void testAttemptToSelfAssignAdminFailsWith403() throws Exception {
        RegisterRequest request = RegisterRequest.builder()
                .email("hacker_admin@evshare3d.com")
                .password("MaliciousPass123!")
                .fullName("Privilege Escalation Attacker")
                .role("ROLE_ADMIN")
                .build();

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.status", is(403)))
                .andExpect(jsonPath("$.message", containsString("Self-assignment of administrative or staff roles is strictly prohibited")));

        // Verify attacker account was NOT created
        Optional<User> attacker = userRepository.findByEmail("hacker_admin@evshare3d.com");
        assertTrue(attacker.isEmpty(), "Attacker attempting to self-assign ADMIN must not be persisted");
    }

    @Test
    @DisplayName("7. Password Hashing: Password stored in database is securely hashed with BCrypt (not plaintext)")
    void testPasswordIsHashedInDatabase() throws Exception {
        String rawPassword = "RawSecretPassword123#";

        RegisterRequest request = RegisterRequest.builder()
                .email("bcrypt_verify@evshare3d.com")
                .password(rawPassword)
                .fullName("BCrypt Verification User")
                .build();

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        Optional<User> userOpt = userRepository.findByEmail("bcrypt_verify@evshare3d.com");
        assertTrue(userOpt.isPresent());
        User user = userOpt.get();

        // 1. Must NOT equal raw password
        assertNotEquals(rawPassword, user.getPasswordHash(), "Plaintext password must NEVER be stored in the database");

        // 2. Must start with standard BCrypt prefix ($2a$ or $2b$)
        assertTrue(user.getPasswordHash().startsWith("$2a$") || user.getPasswordHash().startsWith("$2b$"),
                "Password hash must follow BCrypt standard format");

        // 3. PasswordEncoder must match raw password against the stored hash
        assertTrue(passwordEncoder.matches(rawPassword, user.getPasswordHash()),
                "BCryptPasswordEncoder must successfully verify the hashed credentials");
    }

    @Test
    @DisplayName("8. Sensitive Fields Leaks Prevention: Response payload does not contain password or security internals")
    void testSensitiveFieldsAreNotReturnedInResponse() throws Exception {
        RegisterRequest request = RegisterRequest.builder()
                .email("leak_check@evshare3d.com")
                .password("SuperSecretKey999$")
                .fullName("Safe Response Member")
                .build();

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.password").doesNotExist())
                .andExpect(jsonPath("$.data.passwordHash").doesNotExist())
                .andExpect(jsonPath("$.data.secret").doesNotExist())
                .andExpect(jsonPath("$.data.credentials").doesNotExist());
    }
}
