package com.example.evshare.exception;

import org.springframework.http.HttpStatus;

/**
 * Exception thrown when an idempotency key is reused with a different request payload
 * or conflicting parameters.
 */
public class IdempotencyConflictException extends BusinessException {

    private final String idempotencyKey;

    public IdempotencyConflictException(String idempotencyKey) {
        super(String.format("Idempotency key '%s' was already used with a different request payload", idempotencyKey), HttpStatus.CONFLICT);
        this.idempotencyKey = idempotencyKey;
    }

    public IdempotencyConflictException(String idempotencyKey, String message) {
        super(message, HttpStatus.CONFLICT);
        this.idempotencyKey = idempotencyKey;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }
}
