package com.example.evshare.service.payment;

import java.util.function.Supplier;

/**
 * Service for coordinating persistent, transaction-safe idempotency guarantees.
 */
public interface IdempotencyService {

    /**
     * Computes deterministic SHA-256 fingerprint of the request payload.
     *
     * @param requestPayload Request payload object
     * @return 64-character lowercase hex string
     */
    String computeRequestHash(Object requestPayload);

    /**
     * Executes the given business supplier within an idempotent lifecycle:
     * - Returns cached result if identical key and payload were previously executed.
     * - Rejects with IdempotencyConflictException if same key is used with a different payload.
     * - Protects against concurrent in-flight execution race conditions.
     *
     * @param idempotencyKey Client-supplied unique idempotency key
     * @param operation      Name of the operation (e.g., PAYMENT_INITIATION)
     * @param requestPayload Request object for fingerprinting
     * @param responseType   Class type of the response for deserialization
     * @param businessLogic  Supplier executing the underlying business transaction
     * @param <T>            Type of the response
     * @return Execution result
     */
    <T> T executeIdempotent(String idempotencyKey, String operation, Object requestPayload, Class<T> responseType, Supplier<T> businessLogic);
}
