package com.example.evshare.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * [DEVELOPMENT & TESTING NOTIFICATION MECHANISM]
 * Simulated password reset notifier that holds issued reset tokens in memory for
 * local development and automated integration testing.
 *
 * NOTE: Does NOT connect to external SMTP servers or third-party email providers.
 * Conforms to Requirement 3: Raw token values are NEVER logged to log streams.
 */
@Component
public class DevPasswordResetNotifier implements PasswordResetNotifier {

    private static final Logger log = LoggerFactory.getLogger(DevPasswordResetNotifier.class);

    // In-memory registry storing the latest issued token per email for test assertions
    private final Map<String, String> latestIssuedTokens = new ConcurrentHashMap<>();

    @Override
    public void sendResetInstructions(String email, String resetToken) {
        if (email == null || resetToken == null) {
            return;
        }

        String normalizedEmail = email.trim().toLowerCase();
        latestIssuedTokens.put(normalizedEmail, resetToken);

        // Security rule 3: NEVER log raw token values!
        log.info("[DEV-NOTIFICATION] Password reset instructions dispatched to simulated inbox for: {}", normalizedEmail);
    }

    /**
     * Retrieves the latest reset token dispatched to an email address.
     * Intended exclusively for automated tests and development harness.
     *
     * @param email the user email
     * @return the raw token string, or null if none was issued
     */
    public String getLatestToken(String email) {
        if (email == null) {
            return null;
        }
        return latestIssuedTokens.get(email.trim().toLowerCase());
    }

    /**
     * Clears recorded tokens.
     */
    public void clear() {
        latestIssuedTokens.clear();
    }
}
