package com.example.evshare.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Thread-safe in-memory store for managing password reset tokens.
 * Enforces single-use token consumption, TTL expiration, and strict non-logging of token values.
 */
@Component
public class InMemoryPasswordResetTokenStore implements PasswordResetTokenStore {

    private static final Logger log = LoggerFactory.getLogger(InMemoryPasswordResetTokenStore.class);
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final Map<String, PasswordResetTokenRecord> tokens = new ConcurrentHashMap<>();

    @Override
    public String createToken(Long userId, String email, Duration ttl) {
        // Generate a cryptographically secure 32-byte URL-safe random token
        byte[] randomBytes = new byte[32];
        SECURE_RANDOM.nextBytes(randomBytes);
        String rawToken = Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);

        Instant now = Instant.now();
        Instant expiresAt = now.plus(ttl);

        PasswordResetTokenRecord record = new PasswordResetTokenRecord(
                rawToken,
                userId,
                email,
                now,
                expiresAt,
                false
        );

        tokens.put(rawToken, record);
        log.debug("Recorded password reset token for userId: {}, expiresAt: {}", userId, expiresAt);
        return rawToken;
    }

    @Override
    public Optional<PasswordResetTokenRecord> getValidToken(String token) {
        if (token == null || token.trim().isEmpty()) {
            return Optional.empty();
        }

        PasswordResetTokenRecord record = tokens.get(token.trim());
        if (record == null) {
            return Optional.empty();
        }

        if (record.isExpired()) {
            log.debug("Password reset token has expired for userId: {}", record.userId());
            return Optional.of(record); // return so service can distinguish expired vs non-existent
        }

        return Optional.of(record);
    }

    @Override
    public void markUsed(String token) {
        if (token == null || token.trim().isEmpty()) {
            return;
        }

        tokens.computeIfPresent(token.trim(), (k, existing) -> new PasswordResetTokenRecord(
                existing.token(),
                existing.userId(),
                existing.email(),
                existing.createdAt(),
                existing.expiresAt(),
                true
        ));
        log.debug("Password reset token marked as consumed");
    }

    @Override
    public void revokeAllForUser(Long userId) {
        if (userId == null) {
            return;
        }

        tokens.entrySet().removeIf(entry -> entry.getValue().userId().equals(userId));
        log.debug("Revoked all active password reset tokens for userId: {}", userId);
    }

    @Override
    public void purgeExpired() {
        Instant now = Instant.now();
        int initialSize = tokens.size();
        tokens.entrySet().removeIf(entry -> entry.getValue().expiresAt().isBefore(now) || entry.getValue().used());
        log.debug("Purged expired/used password reset tokens. Count before: {}, count after: {}", initialSize, tokens.size());
    }
}
