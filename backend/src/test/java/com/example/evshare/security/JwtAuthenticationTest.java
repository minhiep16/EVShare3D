package com.example.evshare.security;

import com.example.evshare.dto.response.ApiResponse;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Import(JwtAuthenticationTest.ProtectedTestController.class)
@DisplayName("Checkpoint 03-E — JWT Implementation & Authentication Filter Tests")
class JwtAuthenticationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TokenService tokenService;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @RestController
    @RequestMapping("/api/v1/test/protected")
    static class ProtectedTestController {
        @GetMapping
        public ResponseEntity<ApiResponse<Map<String, Object>>> getProtectedData(@AuthenticationPrincipal UserPrincipal principal) {
            return ResponseEntity.ok(ApiResponse.ok("Access granted", Map.of(
                    "userId", principal.getId(),
                    "email", principal.getEmail(),
                    "authorities", principal.getAuthorities().stream().map(Object::toString).toList()
            )));
        }
    }

    @Test
    @DisplayName("1. Valid Token: Successfully validates and extracts subject, userId, and roles")
    void testValidToken() {
        String token = tokenService.generateAccessToken(42L, "pilot_valid@evshare3d.com", List.of("ROLE_CO_OWNER"));

        assertNotNull(token);
        assertTrue(tokenService.validateToken(token), "Valid token must pass validation");
        assertEquals("pilot_valid@evshare3d.com", tokenService.extractEmail(token));
        assertEquals(42L, tokenService.extractUserId(token));
        assertTrue(tokenService.extractRoles(token).contains("ROLE_CO_OWNER"));
    }

    @Test
    @DisplayName("2. Expired Token: Rejected by TokenService validation")
    void testExpiredToken() {
        String expiredToken = jwtTokenProvider.generateCustomToken(
                10L,
                "expired_user@evshare3d.com",
                List.of("ROLE_CO_OWNER"),
                -60000 // Expired 1 minute ago
        );

        assertFalse(tokenService.validateToken(expiredToken), "Expired token must be rejected");
    }

    @Test
    @DisplayName("3. Malformed Token: Tampered or invalid base64 structures are rejected")
    void testMalformedToken() {
        assertFalse(tokenService.validateToken("not.a.valid.jwt.token.string"));
        assertFalse(tokenService.validateToken("header.payloadWithoutSignature"));
        assertFalse(tokenService.validateToken("eyJhbGciOiJIUzI1NiJ9.corrupted.signature"));
    }

    @Test
    @DisplayName("4. Invalid Signature: Token signed with unauthorized secret key is rejected")
    void testInvalidSignature() {
        SecretKey rogueKey = Keys.hmacShaKeyFor("RogueAttackerSecretKeyMustBeAtLeast256BitsLengthForHmac12345".getBytes(StandardCharsets.UTF_8));
        String tamperedToken = Jwts.builder()
                .subject("tampered_user@evshare3d.com")
                .issuer(jwtTokenProvider.getIssuer())
                .claim("userId", 999L)
                .claim("roles", List.of("ROLE_ADMIN"))
                .issuedAt(new Date())
                .expiration(Date.from(Instant.now().plusSeconds(3600)))
                .signWith(rogueKey)
                .compact();

        assertFalse(tokenService.validateToken(tamperedToken), "Token signed with foreign secret must be rejected");
    }

    @Test
    @DisplayName("5. Missing Token: Null, empty, or whitespace tokens are rejected safely")
    void testMissingToken() {
        assertFalse(tokenService.validateToken(null));
        assertFalse(tokenService.validateToken(""));
        assertFalse(tokenService.validateToken("   "));
    }

    @Test
    @DisplayName("6. Issuer Validation: Token with mismatched issuer is rejected")
    void testIssuerMismatch() {
        String validSecret = "EVShare3DSuperSecretKeyWithMinimum256BitsLengthForHmacSha256Security12345";
        SecretKey key = Keys.hmacShaKeyFor(validSecret.getBytes(StandardCharsets.UTF_8));

        String rogueIssuerToken = Jwts.builder()
                .subject("attacker@evshare3d.com")
                .issuer("rogue-foreign-issuer")
                .claim("userId", 88L)
                .issuedAt(new Date())
                .expiration(Date.from(Instant.now().plusSeconds(3600)))
                .signWith(key)
                .compact();

        assertFalse(tokenService.validateToken(rogueIssuerToken), "Token with mismatched issuer must fail validation");
    }

    @Test
    @DisplayName("7. Authenticated User Extraction: Filter populates SecurityContext and grants access to protected route")
    void testAuthenticatedUserExtraction() throws Exception {
        String token = tokenService.generateAccessToken(77L, "member77@evshare3d.com", List.of("ROLE_CO_OWNER"));

        mockMvc.perform(get("/api/v1/test/protected")
                        .header("Authorization", "Bearer " + token)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.userId", is(77)))
                .andExpect(jsonPath("$.data.email", is("member77@evshare3d.com")))
                .andExpect(jsonPath("$.data.authorities", hasItem("ROLE_CO_OWNER")));
    }

    @Test
    @DisplayName("8. Missing Authorization Header: Access to protected endpoint is denied with RFC 7807 401 Unauthorized")
    void testMissingAuthorizationHeaderAccessDenied() throws Exception {
        mockMvc.perform(get("/api/v1/test/protected")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.status", is(401)))
                .andExpect(jsonPath("$.error", is("Unauthorized")))
                .andExpect(jsonPath("$.message", containsString("Authentication required")));
    }

    @Test
    @DisplayName("9. Expired Authorization Header: Access to protected endpoint is denied with 401 Unauthorized")
    void testExpiredAuthorizationHeaderAccessDenied() throws Exception {
        String expiredToken = jwtTokenProvider.generateCustomToken(
                55L,
                "expired55@evshare3d.com",
                List.of("ROLE_CO_OWNER"),
                -10000
        );

        mockMvc.perform(get("/api/v1/test/protected")
                        .header("Authorization", "Bearer " + expiredToken)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.status", is(401)))
                .andExpect(jsonPath("$.message", containsString("Authentication required")));
    }
}
