package com.example.evshare.exception;

import com.example.evshare.entity.enums.ContractStatus;
import org.springframework.http.HttpStatus;

/**
 * Domain exception thrown when an attempted co-ownership contract lifecycle transition violates
 * the permitted state machine rules.
 */
public class InvalidContractTransitionException extends BusinessException {

    private final ContractStatus currentStatus;
    private final ContractStatus targetStatus;

    public InvalidContractTransitionException(ContractStatus currentStatus, ContractStatus targetStatus) {
        super(String.format("Invalid contract status transition: Cannot transition contract from status '%s' to '%s'", currentStatus, targetStatus), HttpStatus.CONFLICT);
        this.currentStatus = currentStatus;
        this.targetStatus = targetStatus;
    }

    public InvalidContractTransitionException(String message, ContractStatus currentStatus, ContractStatus targetStatus) {
        super(message, HttpStatus.CONFLICT);
        this.currentStatus = currentStatus;
        this.targetStatus = targetStatus;
    }

    public ContractStatus getCurrentStatus() {
        return currentStatus;
    }

    public ContractStatus getTargetStatus() {
        return targetStatus;
    }
}
