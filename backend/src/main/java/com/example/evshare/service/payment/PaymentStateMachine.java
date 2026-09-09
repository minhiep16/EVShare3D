package com.example.evshare.service.payment;

import com.example.evshare.entity.enums.PaymentStatus;
import com.example.evshare.exception.InvalidPaymentStateTransitionException;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * Authoritative Payment Lifecycle State Machine governing transitions between:
 * PENDING, PROCESSING, SUCCESS, FAILED, REFUNDED, CANCELLED (and COMPLETED alias).
 *
 * Rules:
 * - PENDING -> PROCESSING, SUCCESS (or COMPLETED), FAILED, CANCELLED
 * - PROCESSING -> SUCCESS (or COMPLETED), FAILED, CANCELLED
 * - SUCCESS / COMPLETED -> REFUNDED
 * - FAILED -> Terminal (no further transitions)
 * - REFUNDED -> Terminal (no further transitions)
 * - CANCELLED -> Terminal (no further transitions)
 * - Redundant transitions (currentStatus == targetStatus) are strictly rejected.
 */
@Component
public class PaymentStateMachine {

    private static final Map<PaymentStatus, Set<PaymentStatus>> ALLOWED_TRANSITIONS = new EnumMap<>(PaymentStatus.class);

    static {
        // PENDING: Payment initiated; awaiting user interaction or gateway dispatch
        ALLOWED_TRANSITIONS.put(PaymentStatus.PENDING, EnumSet.of(
                PaymentStatus.PROCESSING,
                PaymentStatus.SUCCESS,
                PaymentStatus.COMPLETED,
                PaymentStatus.FAILED,
                PaymentStatus.CANCELLED
        ));

        // PROCESSING: In-flight with payment provider, bank clearance, or 3DS verification
        ALLOWED_TRANSITIONS.put(PaymentStatus.PROCESSING, EnumSet.of(
                PaymentStatus.SUCCESS,
                PaymentStatus.COMPLETED,
                PaymentStatus.FAILED,
                PaymentStatus.CANCELLED
        ));

        // SUCCESS: Captured and settled; can only be transitioned to REFUNDED
        ALLOWED_TRANSITIONS.put(PaymentStatus.SUCCESS, EnumSet.of(
                PaymentStatus.REFUNDED
        ));

        // COMPLETED: Alias for SUCCESS; retained for backward compatibility
        ALLOWED_TRANSITIONS.put(PaymentStatus.COMPLETED, EnumSet.of(
                PaymentStatus.REFUNDED
        ));

        // FAILED: Terminal state; payment attempt permanently failed
        ALLOWED_TRANSITIONS.put(PaymentStatus.FAILED, Collections.emptySet());

        // REFUNDED: Terminal state; settled funds returned/reversed
        ALLOWED_TRANSITIONS.put(PaymentStatus.REFUNDED, Collections.emptySet());

        // CANCELLED: Terminal state; payment cancelled prior to capture
        ALLOWED_TRANSITIONS.put(PaymentStatus.CANCELLED, Collections.emptySet());
    }

    /**
     * Checks whether a transition from currentStatus to targetStatus is valid.
     *
     * @param currentStatus Current status of the payment
     * @param targetStatus  Target status to transition to
     * @return true if permitted, false otherwise
     */
    public boolean isValidTransition(PaymentStatus currentStatus, PaymentStatus targetStatus) {
        if (currentStatus == null || targetStatus == null) {
            return false;
        }
        // Self-transition or alias self-transition (SUCCESS <-> COMPLETED)
        if (currentStatus == targetStatus || (currentStatus.isSettled() && targetStatus.isSettled())) {
            return false;
        }
        Set<PaymentStatus> permitted = ALLOWED_TRANSITIONS.get(currentStatus);
        return permitted != null && permitted.contains(targetStatus);
    }

    /**
     * Validates that the requested transition is permitted according to state machine rules.
     *
     * @param currentStatus Current status of the payment
     * @param targetStatus  Target status to transition to
     * @throws InvalidPaymentStateTransitionException if transition is disallowed or redundant
     */
    public void validateTransition(PaymentStatus currentStatus, PaymentStatus targetStatus) {
        validateTransition(currentStatus, targetStatus, null);
    }

    /**
     * Validates that the requested transition is permitted according to state machine rules,
     * including the transaction reference for precise diagnostics.
     *
     * @param currentStatus        Current status of the payment
     * @param targetStatus         Target status to transition to
     * @param transactionReference Transaction reference code
     * @throws InvalidPaymentStateTransitionException if transition is disallowed or redundant
     */
    public void validateTransition(PaymentStatus currentStatus, PaymentStatus targetStatus, String transactionReference) {
        if (currentStatus == null) {
            throw new InvalidPaymentStateTransitionException(
                    "Current payment status cannot be null",
                    null,
                    targetStatus,
                    transactionReference
            );
        }
        if (targetStatus == null) {
            throw new InvalidPaymentStateTransitionException(
                    "Target payment status cannot be null",
                    currentStatus,
                    null,
                    transactionReference
            );
        }
        if (isTerminal(currentStatus)) {
            throw new InvalidPaymentStateTransitionException(
                    String.format("Cannot transition payment [%s] from terminal status '%s' to '%s'",
                            transactionReference != null ? transactionReference : "N/A", currentStatus, targetStatus),
                    currentStatus,
                    targetStatus,
                    transactionReference
            );
        }
        if (currentStatus == targetStatus || (currentStatus.isSettled() && targetStatus.isSettled())) {
            throw new InvalidPaymentStateTransitionException(
                    String.format("Redundant payment state transition: Payment [%s] is already in settled/active status '%s'",
                            transactionReference != null ? transactionReference : "N/A", currentStatus),
                    currentStatus,
                    targetStatus,
                    transactionReference
            );
        }
        Set<PaymentStatus> permitted = ALLOWED_TRANSITIONS.get(currentStatus);
        if (permitted == null || !permitted.contains(targetStatus)) {
            throw new InvalidPaymentStateTransitionException(
                    String.format("Invalid payment state transition: Cannot transition payment [%s] from status '%s' to '%s'",
                            transactionReference != null ? transactionReference : "N/A", currentStatus, targetStatus),
                    currentStatus,
                    targetStatus,
                    transactionReference
            );
        }
    }

    /**
     * Returns the unmodifiable set of allowed next states from the given current state.
     */
    public Set<PaymentStatus> getAllowedTransitions(PaymentStatus currentStatus) {
        if (currentStatus == null) {
            return Collections.emptySet();
        }
        return Collections.unmodifiableSet(ALLOWED_TRANSITIONS.getOrDefault(currentStatus, Collections.emptySet()));
    }

    /**
     * Checks if the given status is a terminal state.
     */
    public boolean isTerminal(PaymentStatus status) {
        return status != null && status.isTerminal();
    }

    /**
     * Checks if the given status is a settled state (SUCCESS or COMPLETED).
     */
    public boolean isSettled(PaymentStatus status) {
        return status != null && status.isSettled();
    }
}
