package com.example.evshare.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Thread-safe in-memory implementation of {@link RefreshTokenStore}.
 * Provides fast, non-blocking revocation tracking and rotation lifecycle
 * management in compliance with ADR-04 and RFC 6819.
 */
@Component
public class InMemoryRefreshTokenStore implements RefreshTokenStore {

    private static final Logger log = LoggerFactory.getLogger(InMemoryRefreshTokenStore.class);

    public record RefreshTokenRecord(
            String tokenId,
            Long userId,
            Instant expiresAt,
            boolean revoked,
            boolean rotated,
            String replacedByTokenId,
            Instant createdAt
    ) {
        public RefreshTokenRecord withRevoked(boolean newRevoked) {
            return new RefreshTokenRecord(tokenId, userId, expiresAt, newRevoked, rotated, replacedByTokenId, createdAt);
        }

        public RefreshTokenRecord withRotated(String newTokenId) {
            return new RefreshTokenRecord(tokenId, userId, expiresAt, true, true, newTokenId, createdAt);
        }
    }

    private final Map<String, RefreshTokenRecord> tokenRegistry = new ConcurrentHashMap<>();

    @Override
    public void recordToken(String tokenId, Long userId, Instant expiresAt) {
        if (tokenId == null || tokenId.trim().isEmpty()) {
            return;
        }
        RefreshTokenRecord record = new RefreshTokenRecord(
                tokenId,
                userId,
                expiresAt,
                false,
                false,
                null,
                Instant.now()
        );
        tokenRegistry.put(tokenId, record);
        log.debug("Recorded active refresh token [id={}] for user [{}]", tokenId, userId);
    }

    @Override
    public void revokeToken(String tokenId) {
        if (tokenId == null || tokenId.trim().isEmpty()) {
            return;
        }
        tokenRegistry.compute(tokenId, (key, existing) -> {
            if (existing != null) {
                return existing.withRevoked(true);
            } else {
                // If token was not explicitly registered, record it as revoked
                return new RefreshTokenRecord(tokenId, null, Instant.now().plusSeconds(604800), true, false, null, Instant.now());
            }
        });
        log.debug("Marked refresh token [id={}] as revoked", tokenId);
    }

    @Override
    public void revokeAllForUser(Long userId) {
        if (userId == null) {
            return;
        }
        tokenRegistry.forEach((tokenId, record) -> {
            if (userId.equals(record.userId()) && !record.revoked()) {
                tokenRegistry.put(tokenId, record.withRevoked(true));
            }
        });
        log.warn("Revoked all active refresh tokens for user [{}] due to session termination or security trigger", userId);
    }

    @Override
    public boolean isRevoked(String tokenId) {
        if (tokenId == null || tokenId.trim().isEmpty()) {
            return true;
        }
        RefreshTokenRecord record = tokenRegistry.get(tokenId);
        return record != null && record.revoked();
    }

    @Override
    public boolean isRotated(String tokenId) {
        if (tokenId == null || tokenId.trim().isEmpty()) {
            return false;
        }
        RefreshTokenRecord record = tokenRegistry.get(tokenId);
        return record != null && record.rotated();
    }

    @Override
    public void markRotated(String oldTokenId, String newTokenId) {
        if (oldTokenId == null || oldTokenId.trim().isEmpty()) {
            return;
        }
        tokenRegistry.compute(oldTokenId, (key, existing) -> {
            if (existing != null) {
                return existing.withRotated(newTokenId);
            } else {
                return new RefreshTokenRecord(oldTokenId, null, Instant.now().plusSeconds(604800), true, true, newTokenId, Instant.now());
            }
        });
        log.debug("Marked refresh token [id={}] as rotated to replacement [id={}]", oldTokenId, newTokenId);
    }

    @Override
    public void purgeExpired() {
        Instant now = Instant.now();
        tokenRegistry.entrySet().removeIf(entry -> entry.getValue().expiresAt().isBefore(now));
    }
}
