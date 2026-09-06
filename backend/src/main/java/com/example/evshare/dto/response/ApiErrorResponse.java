package com.example.evshare.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiErrorResponse {

    private boolean success = false;
    private int status;
    private String error;
    private String message;
    private String path;
    private Instant timestamp = Instant.now();
    private List<ValidationError> validationErrors;

    public ApiErrorResponse() {
        this.timestamp = Instant.now();
    }

    public ApiErrorResponse(boolean success, int status, String error, String message, String path, Instant timestamp, List<ValidationError> validationErrors) {
        this.success = success;
        this.status = status;
        this.error = error;
        this.message = message;
        this.path = path;
        this.timestamp = timestamp != null ? timestamp : Instant.now();
        this.validationErrors = validationErrors;
    }

    public static ApiErrorResponse of(int status, String error, String message, String path) {
        return ApiErrorResponse.builder()
                .success(false)
                .status(status)
                .error(error)
                .message(message)
                .path(path)
                .timestamp(Instant.now())
                .build();
    }

    public static ApiErrorResponse of(int status, String error, String message, String path, List<ValidationError> validationErrors) {
        return ApiErrorResponse.builder()
                .success(false)
                .status(status)
                .error(error)
                .message(message)
                .path(path)
                .timestamp(Instant.now())
                .validationErrors(validationErrors)
                .build();
    }

    public static ApiErrorResponseBuilder builder() {
        return new ApiErrorResponseBuilder();
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }

    public List<ValidationError> getValidationErrors() {
        return validationErrors;
    }

    public void setValidationErrors(List<ValidationError> validationErrors) {
        this.validationErrors = validationErrors;
    }

    public static class ApiErrorResponseBuilder {
        private boolean success = false;
        private int status;
        private String error;
        private String message;
        private String path;
        private Instant timestamp = Instant.now();
        private List<ValidationError> validationErrors;

        ApiErrorResponseBuilder() {
        }

        public ApiErrorResponseBuilder success(boolean success) {
            this.success = success;
            return this;
        }

        public ApiErrorResponseBuilder status(int status) {
            this.status = status;
            return this;
        }

        public ApiErrorResponseBuilder error(String error) {
            this.error = error;
            return this;
        }

        public ApiErrorResponseBuilder message(String message) {
            this.message = message;
            return this;
        }

        public ApiErrorResponseBuilder path(String path) {
            this.path = path;
            return this;
        }

        public ApiErrorResponseBuilder timestamp(Instant timestamp) {
            this.timestamp = timestamp;
            return this;
        }

        public ApiErrorResponseBuilder validationErrors(List<ValidationError> validationErrors) {
            this.validationErrors = validationErrors;
            return this;
        }

        public ApiErrorResponse build() {
            return new ApiErrorResponse(this.success, this.status, this.error, this.message, this.path, this.timestamp, this.validationErrors);
        }
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class ValidationError {
        private String field;
        private String rejectedValue;
        private String message;

        public ValidationError() {
        }

        public ValidationError(String field, String rejectedValue, String message) {
            this.field = field;
            this.rejectedValue = rejectedValue;
            this.message = message;
        }

        public static ValidationErrorBuilder builder() {
            return new ValidationErrorBuilder();
        }

        public String getField() {
            return field;
        }

        public void setField(String field) {
            this.field = field;
        }

        public String getRejectedValue() {
            return rejectedValue;
        }

        public void setRejectedValue(String rejectedValue) {
            this.rejectedValue = rejectedValue;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }

        public static class ValidationErrorBuilder {
            private String field;
            private String rejectedValue;
            private String message;

            ValidationErrorBuilder() {
            }

            public ValidationErrorBuilder field(String field) {
                this.field = field;
                return this;
            }

            public ValidationErrorBuilder rejectedValue(String rejectedValue) {
                this.rejectedValue = rejectedValue;
                return this;
            }

            public ValidationErrorBuilder message(String message) {
                this.message = message;
                return this;
            }

            public ValidationError build() {
                return new ValidationError(this.field, this.rejectedValue, this.message);
            }
        }
    }
}
