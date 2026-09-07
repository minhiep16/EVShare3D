package com.example.evshare.service.impl;

import com.example.evshare.dto.request.LoginRequest;
import com.example.evshare.dto.request.RegisterRequest;
import com.example.evshare.dto.response.AuthResponse;
import com.example.evshare.dto.response.UserResponse;
import com.example.evshare.entity.Role;
import com.example.evshare.entity.User;
import com.example.evshare.entity.enums.RoleName;
import com.example.evshare.exception.BusinessException;
import com.example.evshare.repository.RoleRepository;
import com.example.evshare.repository.UserRepository;
import com.example.evshare.dto.request.PasswordResetConfirmRequest;
import com.example.evshare.dto.request.PasswordResetRequest;
import com.example.evshare.dto.request.RefreshTokenRequest;
import com.example.evshare.security.PasswordResetNotifier;
import com.example.evshare.security.PasswordResetTokenStore;
import com.example.evshare.security.RefreshTokenStore;
import com.example.evshare.security.TokenService;
import com.example.evshare.security.UserPrincipal;
import com.example.evshare.service.AuthService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

@Service
public class AuthServiceImpl implements AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthServiceImpl.class);

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;
    private final RefreshTokenStore refreshTokenStore;
    private final PasswordResetTokenStore passwordResetTokenStore;
    private final PasswordResetNotifier passwordResetNotifier;

    public AuthServiceImpl(
            UserRepository userRepository,
            RoleRepository roleRepository,
            PasswordEncoder passwordEncoder,
            TokenService tokenService,
            RefreshTokenStore refreshTokenStore,
            PasswordResetTokenStore passwordResetTokenStore,
            PasswordResetNotifier passwordResetNotifier
    ) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenService = tokenService;
        this.refreshTokenStore = refreshTokenStore;
        this.passwordResetTokenStore = passwordResetTokenStore;
        this.passwordResetNotifier = passwordResetNotifier;
    }

    @Override
    @Transactional
    public UserResponse register(RegisterRequest request) {
        if (request == null) {
            throw new BusinessException("Registration request cannot be null", HttpStatus.BAD_REQUEST);
        }

        log.info("Processing user registration for email: {}", request.getEmail());

        // Rule 8: Do not allow client to self-assign ADMIN or any privileged role
        if (request.getRole() != null && !request.getRole().trim().isEmpty()) {
            String requestedRole = request.getRole().trim().toUpperCase();
            if (requestedRole.equals("ROLE_ADMIN") || requestedRole.equals("ADMIN")
                    || requestedRole.equals("ROLE_STAFF") || requestedRole.equals("STAFF")) {
                log.warn("Security Alert: Attempted self-assignment of privileged role [{}] during public registration for email: {}",
                        request.getRole(), request.getEmail());
                throw new BusinessException("Self-assignment of administrative or staff roles is strictly prohibited", HttpStatus.FORBIDDEN);
            }
        }

        // Rule 2 & 10: Validate email uniqueness
        String normalizedEmail = request.getEmail() != null ? request.getEmail().trim().toLowerCase() : "";
        if (userRepository.existsByEmail(normalizedEmail)) {
            log.warn("Registration rejected: Duplicate email [{}]", normalizedEmail);
            throw new BusinessException("Email is already in use: " + request.getEmail(), HttpStatus.CONFLICT);
        }

        // Validate phone uniqueness if provided
        String normalizedPhone = null;
        if (request.getPhoneNumber() != null && !request.getPhoneNumber().trim().isEmpty()) {
            normalizedPhone = request.getPhoneNumber().trim();
            if (userRepository.existsByPhoneNumber(normalizedPhone)) {
                log.warn("Registration rejected: Duplicate phone number [{}]", normalizedPhone);
                throw new BusinessException("Phone number is already in use: " + request.getPhoneNumber(), HttpStatus.CONFLICT);
            }
        }

        // Rule 3: Validate password policy from requirements
        validatePasswordPolicy(request.getPassword());

        // Rule 4 & 5: Hash password using BCrypt (work factor 12) - never store plaintext
        String passwordHash = passwordEncoder.encode(request.getPassword());

        // Rule 6: Assign default role ROLE_CO_OWNER per RBAC
        Role coOwnerRole = roleRepository.findByName(RoleName.ROLE_CO_OWNER)
                .orElseGet(() -> roleRepository.save(new Role(RoleName.ROLE_CO_OWNER)));

        Set<Role> roles = new HashSet<>();
        roles.add(coOwnerRole);

        // Rule 7: Persist user transactionally
        User user = new User();
        user.setEmail(normalizedEmail);
        user.setPasswordHash(passwordHash);
        user.setFullName(request.getFullName().trim());
        user.setPhoneNumber(normalizedPhone);
        user.setAvatar3dUrl(request.getAvatar3dUrl() != null && !request.getAvatar3dUrl().trim().isEmpty() ? request.getAvatar3dUrl().trim() : null);
        user.setIsActive(true);
        user.setCreatedAt(Instant.now());
        user.setUpdatedAt(Instant.now());
        user.setRoles(roles);

        User savedUser = userRepository.save(user);
        log.info("User registered successfully with ID: {}, email: {}, default role: ROLE_CO_OWNER", savedUser.getId(), savedUser.getEmail());

        // Rule 9: Return safe User response (no passwordHash or internal security metadata)
        return UserResponse.fromEntity(savedUser);
    }

    @Override
    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        if (request == null) {
            throw new BusinessException("Login request cannot be null", HttpStatus.BAD_REQUEST);
        }

        String normalizedEmail = request.getEmail() != null ? request.getEmail().trim().toLowerCase() : "";
        log.info("Processing authentication for email: {}", normalizedEmail);

        // Rule 1: Find user securely
        User user = userRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> {
                    log.warn("Authentication failed: Unknown email [{}]", normalizedEmail);
                    // Rule 7: Do not reveal whether email or password was the specific failure
                    return new BusinessException("Invalid email or password", HttpStatus.UNAUTHORIZED);
                });

        // Rule 8: Respect account status / business rules
        if (!Boolean.TRUE.equals(user.getIsActive())) {
            log.warn("Authentication blocked: Deactivated account [{}]", normalizedEmail);
            throw new BusinessException("User account is disabled or deactivated", HttpStatus.FORBIDDEN);
        }

        // Rule 2 & 3: Verify password against BCrypt hash & reject invalid credentials
        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            log.warn("Authentication failed: Password mismatch for email [{}]", normalizedEmail);
            // Rule 7: Do not reveal whether email or password was the specific failure
            throw new BusinessException("Invalid email or password", HttpStatus.UNAUTHORIZED);
        }

        // Rule 4: Generate authentication result according to architecture (ADR-04)
        String accessToken = tokenService.generateAccessToken(user);
        String refreshToken = tokenService.generateRefreshToken(user);
        long expiresIn = tokenService.getAccessTokenExpirationSeconds();

        // Record refresh token in server-side revocation store
        String refreshTokenId = tokenService.extractTokenId(refreshToken);
        Instant refreshExpiry = Instant.now().plusSeconds(tokenService.getRefreshTokenExpirationSeconds());
        refreshTokenStore.recordToken(refreshTokenId, user.getId(), refreshExpiry);

        // Rule 5 & 6: Include necessary user/role info only, NEVER return password hash
        UserResponse userResponse = UserResponse.fromEntity(user);

        log.info("User authenticated successfully with ID: {}, roles: {}", user.getId(), userResponse.getRoles());
        return AuthResponse.of(accessToken, refreshToken, expiresIn, userResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public AuthResponse refresh(RefreshTokenRequest request) {
        if (request == null || request.getRefreshToken() == null || request.getRefreshToken().trim().isEmpty()) {
            throw new BusinessException("Refresh token is required", HttpStatus.BAD_REQUEST);
        }

        String rawToken = request.getRefreshToken().trim();

        // Rule 7: Never log token values!
        // Validate token structure, signature, issuer, and expiration
        if (!tokenService.validateToken(rawToken)) {
            log.warn("Token refresh rejected: Invalid, malformed, or expired refresh token");
            throw new BusinessException("Invalid or expired refresh token", HttpStatus.UNAUTHORIZED);
        }

        // Rule 2: Refresh tokens must not be treated like access tokens
        if (!tokenService.isRefreshToken(rawToken)) {
            log.warn("Token refresh rejected: Supplied token is not a refresh token");
            throw new BusinessException("Invalid token type: refresh token expected", HttpStatus.BAD_REQUEST);
        }

        String tokenId = tokenService.extractTokenId(rawToken);
        Long userId = tokenService.extractUserId(rawToken);

        // Rule 6: Check rotation reuse first to detect token replay/breach (RFC 6819)
        if (refreshTokenStore.isRotated(tokenId)) {
            log.warn("Security Alert: Refresh token reuse detected for [id={}, userId={}]. Revoking all active tokens!", tokenId, userId);
            refreshTokenStore.revokeAllForUser(userId);
            throw new BusinessException("Refresh token reuse detected: token has already been rotated", HttpStatus.UNAUTHORIZED);
        }

        // Rule 4: Check explicit revocation
        if (refreshTokenStore.isRevoked(tokenId)) {
            log.warn("Token refresh rejected: Token [id={}] has been revoked", tokenId);
            throw new BusinessException("Refresh token has been revoked", HttpStatus.UNAUTHORIZED);
        }

        // Verify associated user exists and is active
        User user = userRepository.findById(userId)
                .orElseThrow(() -> {
                    log.warn("Token refresh failed: User [id={}] no longer exists", userId);
                    return new BusinessException("User account not found", HttpStatus.UNAUTHORIZED);
                });

        if (!Boolean.TRUE.equals(user.getIsActive())) {
            log.warn("Token refresh blocked: User [id={}] is deactivated", userId);
            throw new BusinessException("User account is disabled or deactivated", HttpStatus.FORBIDDEN);
        }

        // Generate fresh token pair with rotation
        String newAccessToken = tokenService.generateAccessToken(user);
        String newRefreshToken = tokenService.generateRefreshToken(user);
        long expiresIn = tokenService.getAccessTokenExpirationSeconds();

        String newTokenId = tokenService.extractTokenId(newRefreshToken);
        Instant newRefreshExpiry = Instant.now().plusSeconds(tokenService.getRefreshTokenExpirationSeconds());

        // Mark old token as rotated and register new token
        refreshTokenStore.markRotated(tokenId, newTokenId);
        refreshTokenStore.recordToken(newTokenId, user.getId(), newRefreshExpiry);

        log.info("Successfully refreshed and rotated tokens for user [id={}]", user.getId());
        return AuthResponse.of(newAccessToken, newRefreshToken, expiresIn, UserResponse.fromEntity(user));
    }

    @Override
    public void logout(RefreshTokenRequest request, UserPrincipal principal) {
        if (request != null && request.getRefreshToken() != null && !request.getRefreshToken().trim().isEmpty()) {
            String rawToken = request.getRefreshToken().trim();
            if (tokenService.validateToken(rawToken)) {
                String tokenId = tokenService.extractTokenId(rawToken);
                refreshTokenStore.revokeToken(tokenId);
                log.info("Refresh token [id={}] invalidated upon logout", tokenId);
            }
        }

        if (principal != null) {
            log.info("User [{}] logged out session", principal.getUsername());
        }
    }

    @Override
    @Transactional
    public void requestPasswordReset(PasswordResetRequest request) {
        if (request == null || request.getEmail() == null || request.getEmail().trim().isEmpty()) {
            throw new BusinessException("Email is required for password reset", HttpStatus.BAD_REQUEST);
        }

        String normalizedEmail = request.getEmail().trim().toLowerCase();
        log.info("Processing password reset request for email: {}", normalizedEmail);

        // Anti-Enumeration Principle:
        // Check if user exists and is active; if so, issue token & dispatch instructions.
        // If not, proceed silently without leaking non-existence or account status.
        userRepository.findByEmail(normalizedEmail).ifPresent(user -> {
            if (Boolean.TRUE.equals(user.getIsActive())) {
                // Invalidate any previous pending reset tokens for this user
                passwordResetTokenStore.revokeAllForUser(user.getId());

                // Default reset token validity: 15 minutes
                Duration ttl = Duration.ofMinutes(15);
                String token = passwordResetTokenStore.createToken(user.getId(), user.getEmail(), ttl);

                // Send instructions via notifier (Security Rule 3: raw token is NEVER logged)
                passwordResetNotifier.sendResetInstructions(user.getEmail(), token);
                log.info("Password reset token generated and notification dispatched for userId: {}", user.getId());
            } else {
                log.warn("Password reset requested for deactivated account [{}] - notification suppressed", normalizedEmail);
            }
        });
    }

    @Override
    @Transactional
    public void confirmPasswordReset(PasswordResetConfirmRequest request) {
        if (request == null) {
            throw new BusinessException("Password reset confirmation request cannot be null", HttpStatus.BAD_REQUEST);
        }

        if (request.getToken() == null || request.getToken().trim().isEmpty()) {
            throw new BusinessException("Password reset token is required", HttpStatus.BAD_REQUEST);
        }

        String rawToken = request.getToken().trim();

        // 1. Retrieve and validate token record
        var recordOpt = passwordResetTokenStore.getValidToken(rawToken);
        if (recordOpt.isEmpty()) {
            log.warn("Password reset rejected: Unknown or invalid reset token");
            throw new BusinessException("Invalid or expired password reset token", HttpStatus.BAD_REQUEST);
        }

        var record = recordOpt.get();

        // 2. Enforce single-use token policy
        if (record.used()) {
            log.warn("Security Alert: Replay attempt detected on already consumed password reset token for userId: {}", record.userId());
            throw new BusinessException("Password reset token has already been used", HttpStatus.BAD_REQUEST);
        }

        // 3. Enforce expiration policy
        if (record.isExpired()) {
            log.warn("Password reset rejected: Reset token has expired for userId: {}", record.userId());
            throw new BusinessException("Password reset token has expired", HttpStatus.BAD_REQUEST);
        }

        // 4. Retrieve user and verify active status
        User user = userRepository.findById(record.userId())
                .orElseThrow(() -> {
                    log.warn("Password reset failed: User [id={}] no longer exists", record.userId());
                    return new BusinessException("User account not found", HttpStatus.BAD_REQUEST);
                });

        if (!Boolean.TRUE.equals(user.getIsActive())) {
            log.warn("Password reset blocked: User [id={}] is deactivated", user.getId());
            throw new BusinessException("User account is disabled or deactivated", HttpStatus.FORBIDDEN);
        }

        // 5. Enforce identical password policy as registration (at least 8 chars, max 100, no whitespace)
        validatePasswordPolicy(request.getNewPassword());

        // 6. Hash new password using BCrypt (work factor 12)
        String newPasswordHash = passwordEncoder.encode(request.getNewPassword());
        user.setPasswordHash(newPasswordHash);
        user.setUpdatedAt(Instant.now());
        userRepository.save(user);

        // 7. Mark reset token as consumed (single-use)
        passwordResetTokenStore.markUsed(rawToken);

        // 8. Invalidate/revoke all active refresh tokens and sessions for this user (Security Requirement 6)
        refreshTokenStore.revokeAllForUser(user.getId());

        log.info("Password reset successfully completed and all existing refresh sessions revoked for user [id={}]", user.getId());
    }

    private void validatePasswordPolicy(String password) {
        if (password == null || password.trim().isEmpty()) {
            throw new BusinessException("Password is required", HttpStatus.BAD_REQUEST);
        }
        if (password.length() < 8) {
            throw new BusinessException("Password must be at least 8 characters long", HttpStatus.BAD_REQUEST);
        }
        if (password.length() > 100) {
            throw new BusinessException("Password cannot exceed 100 characters", HttpStatus.BAD_REQUEST);
        }
        if (password.contains(" ")) {
            throw new BusinessException("Password cannot contain whitespace", HttpStatus.BAD_REQUEST);
        }
    }
}
