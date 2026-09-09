package com.example.evshare.service.payment.model;

/**
 * Simulation scenarios supported by MockPaymentProvider for testing and sandbox validation.
 */
public enum MockSimulationMode {
    SUCCESS,
    FAILURE,
    PROCESSING,
    REFUND
}
