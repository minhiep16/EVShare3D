package com.example.evshare.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class RegisterRequest {

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    @Size(max = 150, message = "Email cannot exceed 150 characters")
    private String email;

    @NotBlank(message = "Password is required")
    @Size(min = 8, max = 100, message = "Password must be between 8 and 100 characters")
    private String password;

    @NotBlank(message = "Full name is required")
    @Size(max = 100, message = "Full name cannot exceed 100 characters")
    private String fullName;

    @Pattern(regexp = "^\\+?[0-9]{8,20}$", message = "Phone number must be valid digits between 8 and 20 characters")
    @Size(max = 20, message = "Phone number cannot exceed 20 characters")
    private String phoneNumber;

    @Size(max = 255, message = "Avatar 3D URL cannot exceed 255 characters")
    private String avatar3dUrl;

    public RegisterRequest() {
    }

    public RegisterRequest(String email, String password, String fullName, String phoneNumber, String avatar3dUrl) {
        this.email = email;
        this.password = password;
        this.fullName = fullName;
        this.phoneNumber = phoneNumber;
        this.avatar3dUrl = avatar3dUrl;
    }

    public static RegisterRequestBuilder builder() {
        return new RegisterRequestBuilder();
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public String getAvatar3dUrl() {
        return avatar3dUrl;
    }

    public void setAvatar3dUrl(String avatar3dUrl) {
        this.avatar3dUrl = avatar3dUrl;
    }

    public static class RegisterRequestBuilder {
        private String email;
        private String password;
        private String fullName;
        private String phoneNumber;
        private String avatar3dUrl;

        RegisterRequestBuilder() {
        }

        public RegisterRequestBuilder email(String email) {
            this.email = email;
            return this;
        }

        public RegisterRequestBuilder password(String password) {
            this.password = password;
            return this;
        }

        public RegisterRequestBuilder fullName(String fullName) {
            this.fullName = fullName;
            return this;
        }

        public RegisterRequestBuilder phoneNumber(String phoneNumber) {
            this.phoneNumber = phoneNumber;
            return this;
        }

        public RegisterRequestBuilder avatar3dUrl(String avatar3dUrl) {
            this.avatar3dUrl = avatar3dUrl;
            return this;
        }

        public RegisterRequest build() {
            return new RegisterRequest(email, password, fullName, phoneNumber, avatar3dUrl);
        }
    }
}
