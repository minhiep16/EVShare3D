package com.example.evshare.exception;

import org.springframework.http.HttpStatus;

/**
 * Domain exception thrown when an operation violates usage session telemetry or operational validation rules.
 */
public class InvalidUsageSessionException extends BusinessException {

    public InvalidUsageSessionException(String message) {
        super(message, HttpStatus.BAD_REQUEST);
    }

    public InvalidUsageSessionException(String message, Throwable cause) {
        super(message, HttpStatus.BAD_REQUEST, cause);
    }
}
