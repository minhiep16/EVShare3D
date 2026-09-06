package com.example.evshare.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class ChangePasswordRequest {

    @NotBlank(message = "Current password is required")
    private String currentPassword;

    @NotBlank(message = "New password is required")
    @Size(min = 8, max = 100, message = "New password must be between 8 and 100 characters")
    private String newPassword;

    public ChangePasswordRequest() {
    }

    public ChangePasswordRequest(String currentPassword, String newPassword) {
        this.currentPassword = currentPassword;
        this.newPassword = newPassword;
    }

    public static ChangePasswordRequestBuilder builder() {
        return new ChangePasswordRequestBuilder();
    }

    public String getCurrentPassword() {
        return currentPassword;
    }

    public void setCurrentPassword(String currentPassword) {
        this.currentPassword = currentPassword;
    }

    public String getNewPassword() {
        return newPassword;
    }

    public void setNewPassword(String newPassword) {
        this.newPassword = newPassword;
    }

    public static class ChangePasswordRequestBuilder {
        private String currentPassword;
        private String newPassword;

        ChangePasswordRequestBuilder() {
        }

        public ChangePasswordRequestBuilder currentPassword(String currentPassword) {
            this.currentPassword = currentPassword;
            return this;
        }

        public ChangePasswordRequestBuilder newPassword(String newPassword) {
            this.newPassword = newPassword;
            return this;
        }

        public ChangePasswordRequest build() {
            return new ChangePasswordRequest(currentPassword, newPassword);
        }
    }
}
