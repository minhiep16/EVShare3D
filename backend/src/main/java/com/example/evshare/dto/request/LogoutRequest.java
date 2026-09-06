package com.example.evshare.dto.request;

import jakarta.validation.constraints.NotBlank;

public class LogoutRequest {

    @NotBlank(message = "Refresh token is required to invalidate session")
    private String refreshToken;

    public LogoutRequest() {
    }

    public LogoutRequest(String refreshToken) {
        this.refreshToken = refreshToken;
    }

    public static LogoutRequestBuilder builder() {
        return new LogoutRequestBuilder();
    }

    public String getRefreshToken() {
        return refreshToken;
    }

    public void setRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }

    public static class LogoutRequestBuilder {
        private String refreshToken;

        LogoutRequestBuilder() {
        }

        public LogoutRequestBuilder refreshToken(String refreshToken) {
            this.refreshToken = refreshToken;
            return this;
        }

        public LogoutRequest build() {
            return new LogoutRequest(refreshToken);
        }
    }
}
