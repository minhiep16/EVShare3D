package com.example.evshare.dto;

import com.example.evshare.dto.request.*;
import com.example.evshare.dto.response.AuthResponse;
import com.example.evshare.dto.response.UserResponse;
import com.example.evshare.entity.Role;
import com.example.evshare.entity.User;
import com.example.evshare.entity.enums.RoleName;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Authentication DTO Validation & Security Contract Tests")
class AuthDtoValidationTest {

    private static Validator validator;

    @BeforeAll
    static void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Test
    @DisplayName("RegisterRequest: Valid payload produces zero validation violations")
    void testRegisterRequest_Valid() {
        RegisterRequest request = RegisterRequest.builder()
                .email("co_owner@evshare3d.com")
                .password("SecurePass123!")
                .fullName("Nguyen Van A")
                .phoneNumber("+84987654321")
                .avatar3dUrl("https://assets.evshare3d.com/avatars/driver1.glb")
                .build();

        Set<ConstraintViolation<RegisterRequest>> violations = validator.validate(request);
        assertTrue(violations.isEmpty(), "Valid RegisterRequest must produce zero violations");
    }

    @Test
    @DisplayName("RegisterRequest: Missing required fields and invalid formats trigger violations")
    void testRegisterRequest_Invalid() {
        RegisterRequest request = RegisterRequest.builder()
                .email("invalid-email-format")
                .password("short") // less than 8 chars
                .fullName("")      // blank
                .phoneNumber("abc123notaphone")
                .build();

        Set<ConstraintViolation<RegisterRequest>> violations = validator.validate(request);
        assertFalse(violations.isEmpty(), "Invalid RegisterRequest must trigger violations");

        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("email")));
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("password")));
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("fullName")));
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("phoneNumber")));
    }

    @Test
    @DisplayName("LoginRequest: Valid credentials produce zero violations")
    void testLoginRequest_Valid() {
        LoginRequest request = LoginRequest.builder()
                .email("pilot@evshare3d.com")
                .password("PilotPassword123")
                .build();

        Set<ConstraintViolation<LoginRequest>> violations = validator.validate(request);
        assertTrue(violations.isEmpty(), "Valid LoginRequest must have zero violations");
    }

    @Test
    @DisplayName("LoginRequest: Blank email and password trigger violations")
    void testLoginRequest_Blank() {
        LoginRequest request = LoginRequest.builder()
                .email("")
                .password("")
                .build();

        Set<ConstraintViolation<LoginRequest>> violations = validator.validate(request);
        assertEquals(2, violations.size(), "Both email and password must trigger violations when blank");
    }

    @Test
    @DisplayName("RefreshTokenRequest: Valid token passes; blank token fails")
    void testRefreshTokenRequest() {
        RefreshTokenRequest valid = RefreshTokenRequest.builder()
                .refreshToken("valid-jwt-refresh-token-string")
                .build();
        assertTrue(validator.validate(valid).isEmpty());

        RefreshTokenRequest invalid = RefreshTokenRequest.builder()
                .refreshToken("   ")
                .build();
        assertFalse(validator.validate(invalid).isEmpty());
    }

    @Test
    @DisplayName("LogoutRequest: Valid token passes; blank token fails")
    void testLogoutRequest() {
        LogoutRequest valid = LogoutRequest.builder()
                .refreshToken("valid-jwt-refresh-token-string")
                .build();
        assertTrue(validator.validate(valid).isEmpty());

        LogoutRequest invalid = LogoutRequest.builder()
                .refreshToken("")
                .build();
        assertFalse(validator.validate(invalid).isEmpty());
    }

    @Test
    @DisplayName("PasswordResetRequest: Valid email passes; invalid or blank fails")
    void testPasswordResetRequest() {
        PasswordResetRequest valid = PasswordResetRequest.builder()
                .email("user@evshare3d.com")
                .build();
        assertTrue(validator.validate(valid).isEmpty());

        PasswordResetRequest invalid = PasswordResetRequest.builder()
                .email("bad-email")
                .build();
        assertFalse(validator.validate(invalid).isEmpty());
    }

    @Test
    @DisplayName("PasswordResetConfirmRequest: Valid token and password pass; invalid fail")
    void testPasswordResetConfirmRequest() {
        PasswordResetConfirmRequest valid = PasswordResetConfirmRequest.builder()
                .token("crypto-reset-token-xyz")
                .newPassword("NewSecretPass888")
                .build();
        assertTrue(validator.validate(valid).isEmpty());

        PasswordResetConfirmRequest invalid = PasswordResetConfirmRequest.builder()
                .token("")
                .newPassword("123")
                .build();
        assertEquals(2, validator.validate(invalid).size());
    }

    @Test
    @DisplayName("ChangePasswordRequest: Valid passwords pass; short new password fails")
    void testChangePasswordRequest() {
        ChangePasswordRequest valid = ChangePasswordRequest.builder()
                .currentPassword("OldSecretPass123")
                .newPassword("NewSecretPass456")
                .build();
        assertTrue(validator.validate(valid).isEmpty());

        ChangePasswordRequest invalid = ChangePasswordRequest.builder()
                .currentPassword("")
                .newPassword("short")
                .build();
        assertEquals(2, validator.validate(invalid).size());
    }

    @Test
    @DisplayName("UpdateProfileRequest: Valid profile passes; invalid phone fails")
    void testUpdateProfileRequest() {
        UpdateProfileRequest valid = UpdateProfileRequest.builder()
                .fullName("Tran Thi B")
                .phoneNumber("+84901234567")
                .avatar3dUrl("https://evshare3d.com/model.glb")
                .build();
        assertTrue(validator.validate(valid).isEmpty());

        UpdateProfileRequest invalid = UpdateProfileRequest.builder()
                .phoneNumber("invalid-phone")
                .build();
        assertFalse(validator.validate(invalid).isEmpty());
    }

    @Test
    @DisplayName("Security Contract: UserResponse MUST NOT declare or expose passwordHash field")
    void testUserResponse_DoesNotContainSensitiveFields() {
        List<String> declaredFieldNames = Arrays.stream(UserResponse.class.getDeclaredFields())
                .map(Field::getName)
                .toList();

        assertFalse(declaredFieldNames.contains("passwordHash"), "UserResponse must never contain passwordHash");
        assertFalse(declaredFieldNames.contains("password"), "UserResponse must never contain password");
        assertFalse(declaredFieldNames.contains("secret"), "UserResponse must never contain secret");
    }

    @Test
    @DisplayName("UserResponse: Correct mapping from User entity preserving roles and hiding credentials")
    void testUserResponse_FromEntityMapping() {
        Role coOwnerRole = new Role(1L, RoleName.ROLE_CO_OWNER);
        User user = new User(
                100L,
                "member@evshare.vn",
                "$2a$12$eX4mP1eH4sH4sh3dBcryptStringDoNotLeak",
                "Pham Van C",
                "+84912345678",
                "https://models.evshare.vn/avatar1.glb",
                true,
                Instant.now(),
                Instant.now(),
                Set.of(coOwnerRole)
        );

        UserResponse response = UserResponse.fromEntity(user);

        assertNotNull(response);
        assertEquals(100L, response.getId());
        assertEquals("member@evshare.vn", response.getEmail());
        assertEquals("Pham Van C", response.getFullName());
        assertEquals("+84912345678", response.getPhoneNumber());
        assertEquals("https://models.evshare.vn/avatar1.glb", response.getAvatar3dUrl());
        assertTrue(response.getIsActive());
        assertTrue(response.getRoles().contains("ROLE_CO_OWNER"));
    }

    @Test
    @DisplayName("AuthResponse: Encapsulates access token, refresh token, expiry, and sanitized user profile")
    void testAuthResponse_Structure() {
        UserResponse userResponse = UserResponse.builder()
                .id(1L)
                .email("admin@evshare3d.com")
                .fullName("System Administrator")
                .roles(Set.of("ROLE_ADMIN"))
                .build();

        AuthResponse auth = AuthResponse.of(
                "mock-access-token-jwt",
                "mock-refresh-token-jwt",
                900L,
                userResponse
        );

        assertNotNull(auth);
        assertEquals("mock-access-token-jwt", auth.getAccessToken());
        assertEquals("mock-refresh-token-jwt", auth.getRefreshToken());
        assertEquals("Bearer", auth.getTokenType());
        assertEquals(900L, auth.getExpiresIn());
        assertNotNull(auth.getUser());
        assertEquals("admin@evshare3d.com", auth.getUser().getEmail());
    }
}
