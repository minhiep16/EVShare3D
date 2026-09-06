package com.example.evshare.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class PasswordResetRequest {

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    @Size(max = 150, message = "Email cannot exceed 150 characters")
    private String email;

    public PasswordResetRequest() {
    }

    public PasswordResetRequest(String email) {
        this.email = email;
    }

    public static PasswordResetRequestBuilder builder() {
        return new PasswordResetRequestBuilder();
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public static class PasswordResetRequestBuilder {
        private String email;

        PasswordResetRequestBuilder() {
        }

        public PasswordResetRequestBuilder email(String email) {
            this.email = email;
            return this;
        }

        public PasswordResetRequest build() {
            return new PasswordResetRequest(email);
        }
    }
}
