package com.example.evshare.service.payment;

import com.example.evshare.dto.request.InitiatePaymentRequest;
import com.example.evshare.dto.response.PaymentResponse;
import com.example.evshare.service.payment.model.PaymentRefundCommand;
import com.example.evshare.service.payment.model.PaymentVerificationCommand;

/**
 * Domain service managing payment initiation, verification, refunds, and query operations
 * with integrated idempotency guarantees.
 */
public interface PaymentService {

    /**
     * Initiates a payment idempotently:
     * - Repeated requests with identical idempotencyKey and payload return the exact same PaymentResponse.
     * - Mismatched payloads with an existing idempotencyKey are rejected with IdempotencyConflictException.
     * - Concurrent duplicate submissions are transaction-safely deduplicated.
     *
     * @param request        Payment details
     * @param idempotencyKey Optional explicit client-supplied idempotency key
     * @return Payment response with reference, provider instructions, and initial status
     */
    PaymentResponse initiatePayment(InitiatePaymentRequest request, String idempotencyKey);

    /**
     * Overload reading idempotencyKey from request object.
     */
    default PaymentResponse initiatePayment(InitiatePaymentRequest request) {
        return initiatePayment(request, request != null ? request.getIdempotencyKey() : null);
    }

    /**
     * Retrieves payment by primary key ID.
     */
    PaymentResponse getPaymentById(Long paymentId);

    /**
     * Retrieves payment by transaction reference.
     */
    PaymentResponse getPaymentByReference(String transactionReference);

    /**
     * Verifies payment status with provider.
     */
    PaymentResponse verifyPayment(PaymentVerificationCommand command, String idempotencyKey);

    /**
     * Refunds payment idempotently.
     */
    PaymentResponse refundPayment(PaymentRefundCommand command, String idempotencyKey);
}
