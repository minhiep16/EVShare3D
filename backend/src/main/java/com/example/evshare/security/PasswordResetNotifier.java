package com.example.evshare.security;

/**
 * Contract for dispatching password reset notifications.
 * In development and test profiles, a simulated/in-memory provider is used without external email dependencies.
 */
public interface PasswordResetNotifier {

    /**
     * Dispatches password reset instructions containing the reset token.
     *
     * @param email the recipient email address
     * @param resetToken the generated reset token
     */
    void sendResetInstructions(String email, String resetToken);
}
