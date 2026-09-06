package com.example.evshare.dto.response;

import com.example.evshare.entity.User;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class UserResponse {

    private Long id;
    private String email;
    private String fullName;
    private String phoneNumber;
    private String avatar3dUrl;
    private Boolean isActive;
    private Instant createdAt;
    private Instant updatedAt;
    private Set<String> roles = new HashSet<>();

    public UserResponse() {
    }

    public UserResponse(Long id, String email, String fullName, String phoneNumber, String avatar3dUrl, Boolean isActive, Instant createdAt, Instant updatedAt, Set<String> roles) {
        this.id = id;
        this.email = email;
        this.fullName = fullName;
        this.phoneNumber = phoneNumber;
        this.avatar3dUrl = avatar3dUrl;
        this.isActive = isActive;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.roles = roles != null ? roles : new HashSet<>();
    }

    public static UserResponse fromEntity(User user) {
        if (user == null) {
            return null;
        }
        Set<String> roleNames = user.getRoles() != null
                ? user.getRoles().stream()
                .filter(role -> role != null && role.getName() != null)
                .map(role -> role.getName().name())
                .collect(Collectors.toSet())
                : Collections.emptySet();

        return UserResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .phoneNumber(user.getPhoneNumber())
                .avatar3dUrl(user.getAvatar3dUrl())
                .isActive(user.getIsActive())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .roles(roleNames)
                .build();
    }

    public static UserResponseBuilder builder() {
        return new UserResponseBuilder();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
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

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    public Set<String> getRoles() {
        return roles;
    }

    public void setRoles(Set<String> roles) {
        this.roles = roles;
    }

    public static class UserResponseBuilder {
        private Long id;
        private String email;
        private String fullName;
        private String phoneNumber;
        private String avatar3dUrl;
        private Boolean isActive;
        private Instant createdAt;
        private Instant updatedAt;
        private Set<String> roles = new HashSet<>();

        UserResponseBuilder() {
        }

        public UserResponseBuilder id(Long id) {
            this.id = id;
            return this;
        }

        public UserResponseBuilder email(String email) {
            this.email = email;
            return this;
        }

        public UserResponseBuilder fullName(String fullName) {
            this.fullName = fullName;
            return this;
        }

        public UserResponseBuilder phoneNumber(String phoneNumber) {
            this.phoneNumber = phoneNumber;
            return this;
        }

        public UserResponseBuilder avatar3dUrl(String avatar3dUrl) {
            this.avatar3dUrl = avatar3dUrl;
            return this;
        }

        public UserResponseBuilder isActive(Boolean isActive) {
            this.isActive = isActive;
            return this;
        }

        public UserResponseBuilder createdAt(Instant createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public UserResponseBuilder updatedAt(Instant updatedAt) {
            this.updatedAt = updatedAt;
            return this;
        }

        public UserResponseBuilder roles(Set<String> roles) {
            this.roles = roles;
            return this;
        }

        public UserResponse build() {
            return new UserResponse(id, email, fullName, phoneNumber, avatar3dUrl, isActive, createdAt, updatedAt, roles);
        }
    }
}
