package com.example.evshare.security;

import com.example.evshare.entity.User;
import io.jsonwebtoken.Claims;

import java.util.List;

public interface TokenService {

    /**
     * Generates a short-lived Access JWT for the authenticated user.
     *
     * @param user the authenticated user entity
     * @return the signed JWT string
     */
    String generateAccessToken(User user);

    /**
     * Generates a short-lived Access JWT from explicit claims.
     *
     * @param userId the user ID
     * @param email the user email (subject)
     * @param roles the user roles
     * @return the signed JWT string
     */
    String generateAccessToken(Long userId, String email, List<String> roles);

    /**
     * Generates a persistent Refresh JWT for session renewal.
     *
     * @param user the authenticated user entity
     * @return the signed Refresh JWT string
     */
    String generateRefreshToken(User user);

    /**
     * Generates a persistent Refresh JWT from explicit userId and email.
     *
     * @param userId the user ID
     * @param email the user email (subject)
     * @return the signed Refresh JWT string
     */
    String generateRefreshToken(Long userId, String email);

    /**
     * Validates signature, expiration, token structure, and issuer.
     *
     * @param token the JWT string (must not be logged)
     * @return true if valid and authentic; false otherwise
     */
    boolean validateToken(String token);

    /**
     * Extracts subject email from a validated token.
     */
    String extractEmail(String token);

    /**
     * Extracts user ID claim from a validated token.
     */
    Long extractUserId(String token);

    /**
     * Extracts roles list claim from a validated token.
     */
    List<String> extractRoles(String token);

    /**
     * Extracts the unique token identifier (jti) claim from a validated token.
     */
    String extractTokenId(String token);

    /**
     * Verifies if the token is explicitly a Refresh Token (tokenType == 'REFRESH').
     */
    boolean isRefreshToken(String token);

    /**
     * Verifies if the token is an Access Token (not a refresh token).
     */
    boolean isAccessToken(String token);

    /**
     * Returns the access token lifespan in seconds.
     */
    long getAccessTokenExpirationSeconds();

    /**
     * Returns the refresh token lifespan in seconds.
     */
    long getRefreshTokenExpirationSeconds();

    /**
     * Extracts all payload claims from a signed token.
     */
    Claims extractAllClaims(String token);

    /**
     * Generates a short-lived cryptographic QR check-in token (valid for specified duration).
     * Contains only non-sensitive identifiers and tokenType 'QR_CHECK_IN'.
     *
     * @param bookingId the target booking reservation ID
     * @param vehicleId the target vehicle ID
     * @param userId the user authorized for the session
     * @param ttl time-to-live duration (e.g. 5 minutes per BR-OPS-01)
     * @return the signed compact JWT string
     */
    String generateQrToken(Long bookingId, Long vehicleId, Long userId, java.time.Duration ttl);
}
