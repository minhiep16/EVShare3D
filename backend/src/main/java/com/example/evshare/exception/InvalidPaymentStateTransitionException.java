package com.example.evshare.exception;

import com.example.evshare.entity.enums.PaymentStatus;
import org.springframework.http.HttpStatus;

/**
 * Domain exception thrown when an attempted payment lifecycle transition violates
 * the permitted state machine rules.
 */
public class InvalidPaymentStateTransitionException extends BusinessException {

    private final PaymentStatus currentStatus;
    private final PaymentStatus targetStatus;
    private final String transactionReference;

    public InvalidPaymentStateTransitionException(PaymentStatus currentStatus, PaymentStatus targetStatus) {
        super(String.format("Invalid payment state transition: Cannot transition payment from status '%s' to '%s'", currentStatus, targetStatus), HttpStatus.CONFLICT);
        this.currentStatus = currentStatus;
        this.targetStatus = targetStatus;
        this.transactionReference = null;
    }

    public InvalidPaymentStateTransitionException(PaymentStatus currentStatus, PaymentStatus targetStatus, String transactionReference) {
        super(String.format("Invalid payment state transition: Cannot transition payment [%s] from status '%s' to '%s'",
                transactionReference != null ? transactionReference : "N/A", currentStatus, targetStatus), HttpStatus.CONFLICT);
        this.currentStatus = currentStatus;
        this.targetStatus = targetStatus;
        this.transactionReference = transactionReference;
    }

    public InvalidPaymentStateTransitionException(String message, PaymentStatus currentStatus, PaymentStatus targetStatus) {
        super(message, HttpStatus.CONFLICT);
        this.currentStatus = currentStatus;
        this.targetStatus = targetStatus;
        this.transactionReference = null;
    }

    public InvalidPaymentStateTransitionException(String message, PaymentStatus currentStatus, PaymentStatus targetStatus, String transactionReference) {
        super(message, HttpStatus.CONFLICT);
        this.currentStatus = currentStatus;
        this.targetStatus = targetStatus;
        this.transactionReference = transactionReference;
    }

    public PaymentStatus getCurrentStatus() {
        return currentStatus;
    }

    public PaymentStatus getTargetStatus() {
        return targetStatus;
    }

    public String getTransactionReference() {
        return transactionReference;
    }
}
