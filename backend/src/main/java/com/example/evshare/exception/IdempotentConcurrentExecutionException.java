package com.example.evshare.exception;

import org.springframework.http.HttpStatus;

/**
 * Exception thrown when a concurrent in-flight request is already being processed
 * for the given idempotency key.
 */
public class IdempotentConcurrentExecutionException extends BusinessException {

    private final String idempotencyKey;

    public IdempotentConcurrentExecutionException(String idempotencyKey) {
        super(String.format("An in-flight operation is already processing for idempotency key '%s'. Please retry shortly.", idempotencyKey), HttpStatus.CONFLICT);
        this.idempotencyKey = idempotencyKey;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }
}
