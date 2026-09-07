package com.example.evshare.security;

import java.time.Instant;

/**
 * Contract for managing refresh token lifecycle, server-side revocation,
 * and token rotation tracking in compliance with ADR-04 and RFC 6819.
 */
public interface RefreshTokenStore {

    /**
     * Records an active refresh token issuance.
     *
     * @param tokenId the unique token ID (jti)
     * @param userId the associated user ID
     * @param expiresAt the token expiration instant
     */
    void recordToken(String tokenId, Long userId, Instant expiresAt);

    /**
     * Marks a specific refresh token as explicitly revoked.
     *
     * @param tokenId the unique token ID (jti)
     */
    void revokeToken(String tokenId);

    /**
     * Revokes all active refresh tokens for a user (e.g., security breach / token reuse).
     *
     * @param userId the user ID
     */
    void revokeAllForUser(Long userId);

    /**
     * Checks if a refresh token has been revoked.
     *
     * @param tokenId the unique token ID (jti)
     * @return true if revoked; false otherwise
     */
    boolean isRevoked(String tokenId);

    /**
     * Checks if a refresh token has already been rotated (used to exchange for a new pair).
     *
     * @param tokenId the unique token ID (jti)
     * @return true if already rotated; false otherwise
     */
    boolean isRotated(String tokenId);

    /**
     * Marks an old refresh token as rotated and associates it with its replacement token.
     *
     * @param oldTokenId the old token ID
     * @param newTokenId the replacement token ID
     */
    void markRotated(String oldTokenId, String newTokenId);

    /**
     * Purges expired token records from memory.
     */
    void purgeExpired();
}
