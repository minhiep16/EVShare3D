package com.example.evshare.service;

import com.example.evshare.dto.request.LoginRequest;
import com.example.evshare.dto.request.PasswordResetConfirmRequest;
import com.example.evshare.dto.request.PasswordResetRequest;
import com.example.evshare.dto.request.RefreshTokenRequest;
import com.example.evshare.dto.request.RegisterRequest;
import com.example.evshare.dto.response.AuthResponse;
import com.example.evshare.dto.response.UserResponse;
import com.example.evshare.security.UserPrincipal;

public interface AuthService {

    /**
     * Registers a new user account with default ROLE_CO_OWNER role.
     *
     * @param request the registration request DTO
     * @return the sanitized UserResponse
     */
    UserResponse register(RegisterRequest request);

    /**
     * Authenticates user credentials and issues dual JWT access and refresh tokens.
     *
     * @param request the login request DTO
     * @return the AuthResponse containing tokens, expiry, and user summary
     */
    AuthResponse login(LoginRequest request);

    /**
     * Exchanges a valid refresh token for a fresh access token and rotated refresh token
     * in compliance with ADR-04 and RFC 6819.
     *
     * @param request the refresh token request DTO
     * @return the AuthResponse containing new access and rotated refresh tokens
     */
    AuthResponse refresh(RefreshTokenRequest request);

    /**
     * Terminates an active session by invalidating the supplied refresh token
     * and recording revocation in the server-side store.
     *
     * @param request the logout request containing the refresh token
     * @param principal the authenticated user principal (if present)
     */
    void logout(RefreshTokenRequest request, UserPrincipal principal);

    /**
     * Initiates a password reset flow. Follows strict anti-enumeration guarantees:
     * returns uniformly regardless of whether the email exists or not.
     *
     * @param request the password reset request containing user email
     */
    void requestPasswordReset(PasswordResetRequest request);

    /**
     * Confirms password reset using a single-use token and updates the user password hash.
     * Invalidates all existing refresh tokens and sessions for the user upon success.
     *
     * @param request the confirmation request containing token and new password
     */
    void confirmPasswordReset(PasswordResetConfirmRequest request);
}
