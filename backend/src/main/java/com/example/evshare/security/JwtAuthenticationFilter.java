package com.example.evshare.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);
    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    private final TokenService tokenService;

    public JwtAuthenticationFilter(TokenService tokenService) {
        this.tokenService = tokenService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String token = resolveToken(request);

        // Validate token integrity, signature, expiration, structure, and issuer
        // Refresh tokens must NOT be accepted as Bearer access tokens for protected API routes (Rule 2)
        // Note: The raw token string itself is NEVER logged in compliance with security guidelines
        if (StringUtils.hasText(token) && tokenService.validateToken(token)) {
            if (!tokenService.isAccessToken(token)) {
                log.warn("Security Alert: Attempted use of Refresh Token as Bearer Access Token on URI: [{}]", request.getRequestURI());
                filterChain.doFilter(request, response);
                return;
            }

            try {
                String email = tokenService.extractEmail(token);
                Long userId = tokenService.extractUserId(token);
                List<String> roles = tokenService.extractRoles(token);

                UserPrincipal principal = UserPrincipal.fromClaims(userId, email, roles);

                UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                        principal,
                        null,
                        principal.getAuthorities()
                );
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                SecurityContextHolder.getContext().setAuthentication(authentication);
                log.debug("SecurityContextHolder populated for user: [{}] with roles: {}", email, roles);
            } catch (Exception ex) {
                log.warn("Could not set user authentication in security context: {}", ex.getMessage());
            }
        }

        filterChain.doFilter(request, response);
    }

    private String resolveToken(HttpServletRequest request) {
        String bearerToken = request.getHeader(AUTHORIZATION_HEADER);
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith(BEARER_PREFIX)) {
            return bearerToken.substring(BEARER_PREFIX.length()).trim();
        }
        return null;
    }
}
