package com.example.evshare.security;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

/**
 * Contract for managing the lifecycle, storage, expiration, and one-time consumption
 * of password reset tokens.
 */
public interface PasswordResetTokenStore {

    /**
     * Creates and records a cryptographically secure password reset token for the specified user.
     *
     * @param userId the user ID
     * @param email the user email
     * @param ttl the time-to-live duration for the token
     * @return the newly generated raw token string
     */
    String createToken(Long userId, String email, Duration ttl);

    /**
     * Retrieves the active token record if it exists and has not expired or been consumed.
     *
     * @param token the raw token string
     * @return an Optional containing the token record, or empty if invalid
     */
    Optional<PasswordResetTokenRecord> getValidToken(String token);

    /**
     * Marks the token as consumed (used), preventing any token replay or reuse.
     *
     * @param token the token string
     */
    void markUsed(String token);

    /**
     * Revokes all active reset tokens previously issued for a user.
     *
     * @param userId the user ID
     */
    void revokeAllForUser(Long userId);

    /**
     * Purges expired or consumed token records from the store.
     */
    void purgeExpired();

    /**
     * Immutable value record representing an issued password reset token.
     */
    record PasswordResetTokenRecord(
            String token,
            Long userId,
            String email,
            Instant createdAt,
            Instant expiresAt,
            boolean used
    ) {
        public boolean isExpired() {
            return Instant.now().isAfter(expiresAt);
        }

        public boolean isValid() {
            return !used && !isExpired();
        }
    }
}
