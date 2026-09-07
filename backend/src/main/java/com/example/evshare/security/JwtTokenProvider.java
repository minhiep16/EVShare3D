package com.example.evshare.security;

import com.example.evshare.entity.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Component
public class JwtTokenProvider implements TokenService {

    private static final Logger log = LoggerFactory.getLogger(JwtTokenProvider.class);

    private final SecretKey key;
    private final String issuer;
    private final long accessTokenExpirationMs;
    private final long refreshTokenExpirationMs;

    public JwtTokenProvider(
            @Value("${jwt.secret:EVShare3DSuperSecretKeyWithMinimum256BitsLengthForHmacSha256Security12345}") String secret,
            @Value("${jwt.issuer:evshare3d-backend}") String issuer,
            @Value("${jwt.access-token-expiration-ms:900000}") long accessTokenExpirationMs,
            @Value("${jwt.refresh-token-expiration-ms:604800000}") long refreshTokenExpirationMs
    ) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.issuer = issuer;
        this.accessTokenExpirationMs = accessTokenExpirationMs;
        this.refreshTokenExpirationMs = refreshTokenExpirationMs;
    }

    @Override
    public String generateAccessToken(User user) {
        Set<String> roleNames = user.getRoles() != null
                ? user.getRoles().stream()
                .filter(r -> r != null && r.getName() != null)
                .map(r -> r.getName().name())
                .collect(Collectors.toSet())
                : Collections.emptySet();

        return generateAccessToken(user.getId(), user.getEmail(), new ArrayList<>(roleNames));
    }

    @Override
    public String generateAccessToken(Long userId, String email, List<String> roles) {
        Instant now = Instant.now();
        Instant expiry = now.plusMillis(accessTokenExpirationMs);

        return Jwts.builder()
                .subject(email)
                .issuer(issuer)
                .claim("userId", userId)
                .claim("roles", roles != null ? roles : Collections.emptyList())
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiry))
                .signWith(key)
                .compact();
    }

    @Override
    public String generateRefreshToken(User user) {
        return generateRefreshToken(user.getId(), user.getEmail());
    }

    @Override
    public String generateRefreshToken(Long userId, String email) {
        Instant now = Instant.now();
        Instant expiry = now.plusMillis(refreshTokenExpirationMs);
        String jti = UUID.randomUUID().toString();

        return Jwts.builder()
                .id(jti)
                .subject(email)
                .issuer(issuer)
                .claim("userId", userId)
                .claim("tokenType", "REFRESH")
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiry))
                .signWith(key)
                .compact();
    }

    /**
     * Helper method to generate expired refresh tokens for integration testing.
     */
    public String generateExpiredRefreshToken(Long userId, String email, long expirationOffsetMs) {
        Instant now = Instant.now();
        Instant expiry = now.plusMillis(expirationOffsetMs);
        String jti = UUID.randomUUID().toString();

        return Jwts.builder()
                .id(jti)
                .subject(email)
                .issuer(issuer)
                .claim("userId", userId)
                .claim("tokenType", "REFRESH")
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiry))
                .signWith(key)
                .compact();
    }

    /**
     * Helper method to generate tokens with custom expiration for testing expired tokens.
     */
    public String generateCustomToken(Long userId, String email, List<String> roles, long expirationOffsetMs) {
        Instant now = Instant.now();
        Instant expiry = now.plusMillis(expirationOffsetMs);

        return Jwts.builder()
                .subject(email)
                .issuer(issuer)
                .claim("userId", userId)
                .claim("roles", roles != null ? roles : Collections.emptyList())
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiry))
                .signWith(key)
                .compact();
    }

    @Override
    public boolean validateToken(String token) {
        if (token == null || token.trim().isEmpty()) {
            return false;
        }
        try {
            Jwts.parser()
                    .verifyWith(key)
                    .requireIssuer(issuer)
                    .build()
                    .parseSignedClaims(token);
            return true;
        } catch (ExpiredJwtException e) {
            log.warn("JWT token validation failed: Token has expired");
        } catch (SignatureException e) {
            log.warn("JWT token validation failed: Signature verification failed");
        } catch (MalformedJwtException e) {
            log.warn("JWT token validation failed: Malformed token structure");
        } catch (JwtException e) {
            log.warn("JWT token validation failed: General JWT processing error");
            log.debug("JWT validation error detail: {}", e.getMessage());
        } catch (IllegalArgumentException e) {
            log.warn("JWT token validation failed: Token compact string is empty or illegal");
        }
        return false;
    }

    @Override
    public String extractEmail(String token) {
        return extractAllClaims(token).getSubject();
    }

    @Override
    public Long extractUserId(String token) {
        Object userId = extractAllClaims(token).get("userId");
        if (userId instanceof Number number) {
            return number.longValue();
        }
        return null;
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<String> extractRoles(String token) {
        Object roles = extractAllClaims(token).get("roles");
        if (roles instanceof List<?> list) {
            return list.stream().map(Object::toString).toList();
        }
        return Collections.emptyList();
    }

    @Override
    public Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .requireIssuer(issuer)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    @Override
    public String extractTokenId(String token) {
        Claims claims = extractAllClaims(token);
        String id = claims.getId();
        if (id != null && !id.trim().isEmpty()) {
            return id;
        }
        Object jti = claims.get("jti");
        if (jti != null) {
            return jti.toString();
        }
        return claims.getSubject() + "_" + (claims.getIssuedAt() != null ? claims.getIssuedAt().getTime() : System.currentTimeMillis());
    }

    @Override
    public boolean isRefreshToken(String token) {
        if (!validateToken(token)) {
            return false;
        }
        try {
            Claims claims = extractAllClaims(token);
            return "REFRESH".equalsIgnoreCase(claims.get("tokenType", String.class));
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public boolean isAccessToken(String token) {
        if (!validateToken(token)) {
            return false;
        }
        try {
            Claims claims = extractAllClaims(token);
            String tokenType = claims.get("tokenType", String.class);
            return !"REFRESH".equalsIgnoreCase(tokenType);
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public long getAccessTokenExpirationSeconds() {
        return accessTokenExpirationMs / 1000;
    }

    @Override
    public long getRefreshTokenExpirationSeconds() {
        return refreshTokenExpirationMs / 1000;
    }

    public String getIssuer() {
        return issuer;
    }
}
