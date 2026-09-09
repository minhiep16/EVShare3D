package com.example.evshare.entity.enums;

/**
 * Authoritative Payment Status Enum for the EVShare 3D platform.
 * Governed by the Payment Lifecycle State Machine.
 */
public enum PaymentStatus {
    /**
     * Initial status: Payment created, awaiting checkout dispatch or asynchronous clearance.
     */
    PENDING,

    /**
     * Intermediate status: Asynchronous gateway clearance started, 3DS challenge active,
     * or awaiting bank transfer settlement.
     */
    PROCESSING,

    /**
     * Canonical settled status: Payment captured and settled successfully.
     */
    SUCCESS,

    /**
     * Settled status retained for backward compatibility (equivalent to SUCCESS).
     */
    COMPLETED,

    /**
     * Terminal status: Payment failed, declined, or timed out.
     */
    FAILED,

    /**
     * Terminal status: Settled payment was refunded or reversed.
     */
    REFUNDED,

    /**
     * Terminal status: Unsettled payment was explicitly cancelled by the user or session expired.
     */
    CANCELLED;

    /**
     * Returns true if this state is terminal (no further transitions are allowed).
     */
    public boolean isTerminal() {
        return this == FAILED || this == REFUNDED || this == CANCELLED;
    }

    /**
     * Returns true if this state represents settled/cleared funds.
     */
    public boolean isSettled() {
        return this == SUCCESS || this == COMPLETED;
    }
}
