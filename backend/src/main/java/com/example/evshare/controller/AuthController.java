package com.example.evshare.controller;

import com.example.evshare.dto.request.LoginRequest;
import com.example.evshare.dto.request.PasswordResetConfirmRequest;
import com.example.evshare.dto.request.PasswordResetRequest;
import com.example.evshare.dto.request.RefreshTokenRequest;
import com.example.evshare.dto.request.RegisterRequest;
import com.example.evshare.dto.response.ApiResponse;
import com.example.evshare.dto.response.AuthResponse;
import com.example.evshare.dto.response.UserResponse;
import com.example.evshare.security.UserPrincipal;
import com.example.evshare.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Authentication", description = "Authentication and identity management endpoints")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    @Operation(summary = "Register new user account", description = "Public self-registration endpoint that creates a new user account with default ROLE_CO_OWNER")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "User registered successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid request payload or weak password"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Attempted self-assignment of privileged role"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Email or phone number already in use")
    })
    public ResponseEntity<ApiResponse<UserResponse>> register(@Valid @RequestBody RegisterRequest request) {
        UserResponse response = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created("User registered successfully", response));
    }

    @PostMapping("/login")
    @Operation(summary = "Authenticate user credentials", description = "Authenticates user with email and password, returning short-lived Access JWT and persistent Refresh JWT")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Authentication successful"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid request payload"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Invalid email or password"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "User account is disabled or deactivated")
    })
    public ResponseEntity<ApiResponse<AuthResponse>> login(@Valid @RequestBody LoginRequest request) {
        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(ApiResponse.ok("Authentication successful", response));
    }

    @PostMapping("/refresh")
    @Operation(summary = "Exchange refresh token for fresh access and rotated refresh token",
            description = "Validates the presented refresh token, checks server-side revocation status, performs token rotation, and returns new access and refresh tokens")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Token refreshed successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid request payload or token type"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Invalid, expired, or revoked refresh token"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "User account is disabled")
    })
    public ResponseEntity<ApiResponse<AuthResponse>> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        AuthResponse response = authService.refresh(request);
        return ResponseEntity.ok(ApiResponse.ok("Token refreshed successfully", response));
    }

    @PostMapping("/logout")
    @Operation(summary = "Invalidate active refresh token session",
            description = "Revokes the active refresh token session in the server-side revocation store")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Logged out successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid request payload")
    })
    public ResponseEntity<ApiResponse<Void>> logout(
            @RequestBody(required = false) RefreshTokenRequest request,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        authService.logout(request, principal);
        return ResponseEntity.ok(ApiResponse.ok("Logged out successfully", null));
    }

    @PostMapping(value = {"/password-reset/request", "/forgot-password"})
    @Operation(summary = "Request password reset instructions",
            description = "Initiates password reset flow with anti-account enumeration guarantees. Returns identical response regardless of whether the email exists.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Password reset instructions dispatched if account exists"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid email format or missing payload")
    })
    public ResponseEntity<ApiResponse<Void>> requestPasswordReset(@Valid @RequestBody PasswordResetRequest request) {
        authService.requestPasswordReset(request);
        return ResponseEntity.ok(ApiResponse.ok(
                "If an account exists with this email, password reset instructions have been dispatched.",
                null
        ));
    }

    @PostMapping(value = {"/password-reset/confirm", "/reset-password"})
    @Operation(summary = "Confirm password reset and set new credentials",
            description = "Validates the one-time reset token, applies the new BCrypt hashed password, and revokes all active sessions/refresh tokens.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Password reset successfully completed"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid, expired, or consumed reset token, or weak password"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Account is deactivated")
    })
    public ResponseEntity<ApiResponse<Void>> confirmPasswordReset(@Valid @RequestBody PasswordResetConfirmRequest request) {
        authService.confirmPasswordReset(request);
        return ResponseEntity.ok(ApiResponse.ok(
                "Password has been reset successfully. Please log in with your new password.",
                null
        ));
    }
}
