package com.example.evshare.exception;

import com.example.evshare.entity.enums.DisputeStatus;
import org.springframework.http.HttpStatus;

/**
 * Domain exception thrown when an attempted dispute lifecycle transition violates
 * the permitted state machine rules.
 */
public class InvalidDisputeStateTransitionException extends BusinessException {

    private final DisputeStatus currentStatus;
    private final DisputeStatus targetStatus;
    private final Long disputeId;

    public InvalidDisputeStateTransitionException(DisputeStatus currentStatus, DisputeStatus targetStatus) {
        super(String.format("Invalid dispute state transition: Cannot transition dispute from status '%s' to '%s'",
                currentStatus, targetStatus), HttpStatus.CONFLICT);
        this.currentStatus = currentStatus;
        this.targetStatus = targetStatus;
        this.disputeId = null;
    }

    public InvalidDisputeStateTransitionException(DisputeStatus currentStatus, DisputeStatus targetStatus, Long disputeId) {
        super(String.format("Invalid dispute state transition: Cannot transition dispute [%s] from status '%s' to '%s'",
                disputeId != null ? disputeId : "N/A", currentStatus, targetStatus), HttpStatus.CONFLICT);
        this.currentStatus = currentStatus;
        this.targetStatus = targetStatus;
        this.disputeId = disputeId;
    }

    public InvalidDisputeStateTransitionException(String message, DisputeStatus currentStatus, DisputeStatus targetStatus, Long disputeId) {
        super(message, HttpStatus.CONFLICT);
        this.currentStatus = currentStatus;
        this.targetStatus = targetStatus;
        this.disputeId = disputeId;
    }

    public DisputeStatus getCurrentStatus() {
        return currentStatus;
    }

    public DisputeStatus getTargetStatus() {
        return targetStatus;
    }

    public Long getDisputeId() {
        return disputeId;
    }
}
