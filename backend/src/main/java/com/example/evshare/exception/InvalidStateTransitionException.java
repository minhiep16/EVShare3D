package com.example.evshare.exception;

import com.example.evshare.entity.enums.VehicleStatus;
import org.springframework.http.HttpStatus;

/**
 * Domain exception thrown when an attempted vehicle state transition violates
 * the state machine rules defined in docs/BUSINESS_RULES.md.
 */
public class InvalidStateTransitionException extends BusinessException {

    private final VehicleStatus currentStatus;
    private final VehicleStatus targetStatus;

    public InvalidStateTransitionException(VehicleStatus currentStatus, VehicleStatus targetStatus) {
        super(String.format("Invalid state transition: Cannot transition vehicle from status '%s' to '%s'", currentStatus, targetStatus), HttpStatus.CONFLICT);
        this.currentStatus = currentStatus;
        this.targetStatus = targetStatus;
    }

    public InvalidStateTransitionException(String message, VehicleStatus currentStatus, VehicleStatus targetStatus) {
        super(message, HttpStatus.CONFLICT);
        this.currentStatus = currentStatus;
        this.targetStatus = targetStatus;
    }

    public VehicleStatus getCurrentStatus() {
        return currentStatus;
    }

    public VehicleStatus getTargetStatus() {
        return targetStatus;
    }
}
