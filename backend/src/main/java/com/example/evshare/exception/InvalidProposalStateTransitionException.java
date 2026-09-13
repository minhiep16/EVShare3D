package com.example.evshare.exception;

import com.example.evshare.entity.enums.ProposalStatus;
import org.springframework.http.HttpStatus;

/**
 * Domain exception thrown when an attempted proposal lifecycle transition violates
 * the permitted state machine rules.
 */
public class InvalidProposalStateTransitionException extends BusinessException {

    private final ProposalStatus currentStatus;
    private final ProposalStatus targetStatus;
    private final Long proposalId;

    public InvalidProposalStateTransitionException(ProposalStatus currentStatus, ProposalStatus targetStatus) {
        super(String.format("Invalid proposal state transition: Cannot transition proposal from status '%s' to '%s'",
                currentStatus, targetStatus), HttpStatus.CONFLICT);
        this.currentStatus = currentStatus;
        this.targetStatus = targetStatus;
        this.proposalId = null;
    }

    public InvalidProposalStateTransitionException(ProposalStatus currentStatus, ProposalStatus targetStatus, Long proposalId) {
        super(String.format("Invalid proposal state transition: Cannot transition proposal [%s] from status '%s' to '%s'",
                proposalId != null ? proposalId : "N/A", currentStatus, targetStatus), HttpStatus.CONFLICT);
        this.currentStatus = currentStatus;
        this.targetStatus = targetStatus;
        this.proposalId = proposalId;
    }

    public InvalidProposalStateTransitionException(String message, ProposalStatus currentStatus, ProposalStatus targetStatus, Long proposalId) {
        super(message, HttpStatus.CONFLICT);
        this.currentStatus = currentStatus;
        this.targetStatus = targetStatus;
        this.proposalId = proposalId;
    }

    public ProposalStatus getCurrentStatus() {
        return currentStatus;
    }

    public ProposalStatus getTargetStatus() {
        return targetStatus;
    }

    public Long getProposalId() {
        return proposalId;
    }
}
