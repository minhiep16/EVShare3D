package com.example.evshare.dto.request;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class UpdateProfileRequest {

    @Size(max = 100, message = "Full name cannot exceed 100 characters")
    private String fullName;

    @Pattern(regexp = "^\\+?[0-9]{8,20}$", message = "Phone number must be valid digits between 8 and 20 characters")
    @Size(max = 20, message = "Phone number cannot exceed 20 characters")
    private String phoneNumber;

    @Size(max = 255, message = "Avatar 3D URL cannot exceed 255 characters")
    private String avatar3dUrl;

    public UpdateProfileRequest() {
    }

    public UpdateProfileRequest(String fullName, String phoneNumber, String avatar3dUrl) {
        this.fullName = fullName;
        this.phoneNumber = phoneNumber;
        this.avatar3dUrl = avatar3dUrl;
    }

    public static UpdateProfileRequestBuilder builder() {
        return new UpdateProfileRequestBuilder();
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

    public static class UpdateProfileRequestBuilder {
        private String fullName;
        private String phoneNumber;
        private String avatar3dUrl;

        UpdateProfileRequestBuilder() {
        }

        public UpdateProfileRequestBuilder fullName(String fullName) {
            this.fullName = fullName;
            return this;
        }

        public UpdateProfileRequestBuilder phoneNumber(String phoneNumber) {
            this.phoneNumber = phoneNumber;
            return this;
        }

        public UpdateProfileRequestBuilder avatar3dUrl(String avatar3dUrl) {
            this.avatar3dUrl = avatar3dUrl;
            return this;
        }

        public UpdateProfileRequest build() {
            return new UpdateProfileRequest(fullName, phoneNumber, avatar3dUrl);
        }
    }
}
