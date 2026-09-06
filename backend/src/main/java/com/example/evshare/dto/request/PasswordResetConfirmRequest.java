package com.example.evshare.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class PasswordResetConfirmRequest {

    @NotBlank(message = "Password reset token is required")
    private String token;

    @NotBlank(message = "New password is required")
    @Size(min = 8, max = 100, message = "New password must be between 8 and 100 characters")
    private String newPassword;

    public PasswordResetConfirmRequest() {
    }

    public PasswordResetConfirmRequest(String token, String newPassword) {
        this.token = token;
        this.newPassword = newPassword;
    }

    public static PasswordResetConfirmRequestBuilder builder() {
        return new PasswordResetConfirmRequestBuilder();
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getNewPassword() {
        return newPassword;
    }

    public void setNewPassword(String newPassword) {
        this.newPassword = newPassword;
    }

    public static class PasswordResetConfirmRequestBuilder {
        private String token;
        private String newPassword;

        PasswordResetConfirmRequestBuilder() {
        }

        public PasswordResetConfirmRequestBuilder token(String token) {
            this.token = token;
            return this;
        }

        public PasswordResetConfirmRequestBuilder newPassword(String newPassword) {
            this.newPassword = newPassword;
            return this;
        }

        public PasswordResetConfirmRequest build() {
            return new PasswordResetConfirmRequest(token, newPassword);
        }
    }
}
