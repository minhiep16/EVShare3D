package com.example.evshare.service.payment;

import com.example.evshare.dto.response.PaymentAuditLogResponse;
import com.example.evshare.entity.Payment;
import com.example.evshare.entity.enums.PaymentStatus;

import java.util.List;

/**
 * Service managing transactional payment state transitions, enforcing state machine rules,
 * and preserving immutable audit history.
 */
public interface PaymentLifecycleService {

    /**
     * Transitions payment status by payment ID, validating transition legality and recording audit history.
     *
     * @param paymentId    ID of the payment
     * @param targetStatus Intended destination status
     * @param reason       Explanation/reason for state change
     * @param actorUserId  User ID of the actor performing the transition (or null for automated system)
     * @return Updated payment entity
     */
    Payment transitionStatus(Long paymentId, PaymentStatus targetStatus, String reason, Long actorUserId);

    /**
     * Transitions payment status by unique transaction reference.
     *
     * @param transactionReference Unique payment transaction reference
     * @param targetStatus         Intended destination status
     * @param reason               Explanation/reason for state change
     * @param actorUserId          User ID of the actor performing the transition
     * @return Updated payment entity
     */
    Payment transitionStatusByReference(String transactionReference, PaymentStatus targetStatus, String reason, Long actorUserId);

    /**
     * Retrieves the complete immutable transition history for a payment.
     *
     * @param paymentId     Payment entity ID
     * @param currentUserId Requesting user ID for authorization check
     * @return List of audit trail responses ordered by ID/created timestamp
     */
    List<PaymentAuditLogResponse> getPaymentHistory(Long paymentId, Long currentUserId);

    /**
     * Retrieves the complete immutable transition history for a payment by transaction reference.
     *
     * @param transactionReference Payment transaction reference
     * @param currentUserId        Requesting user ID for authorization check
     * @return List of audit trail responses ordered by ID/created timestamp
     */
    List<PaymentAuditLogResponse> getPaymentHistoryByReference(String transactionReference, Long currentUserId);
}
