package com.example.evshare.entity.enums;

/**
 * Execution status for persistent idempotency keys.
 */
public enum IdempotencyStatus {
    /**
     * Initial in-flight execution state.
     */
    PROCESSING,

    /**
     * Operation executed successfully and response cached.
     */
    COMPLETED,

    /**
     * Operation failed with an error.
     */
    FAILED
}
