package com.example.evshare.service;

import com.example.evshare.entity.enums.DisputeStatus;
import com.example.evshare.exception.InvalidDisputeStateTransitionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * Authoritative Dispute Lifecycle State Machine governing transitions between:
 * OPEN, UNDER_REVIEW, RESOLVED, ESCALATED.
 *
 * Rules per BR-DIS-01:
 * - OPEN -> UNDER_REVIEW, RESOLVED, ESCALATED
 * - UNDER_REVIEW -> RESOLVED, ESCALATED
 * - ESCALATED -> RESOLVED
 * - RESOLVED -> Terminal (no further transitions permitted)
 * - Redundant transitions (currentStatus == targetStatus) are strictly rejected.
 */
@Component
public class DisputeStateMachine {

    private static final Logger log = LoggerFactory.getLogger(DisputeStateMachine.class);

    private static final Map<DisputeStatus, Set<DisputeStatus>> ALLOWED_TRANSITIONS = new EnumMap<>(DisputeStatus.class);

    static {
        // OPEN: Newly filed grievance; can move to UNDER_REVIEW (mediation begins),
        // RESOLVED (early settlement), or ESCALATED (direct admin arbitration).
        ALLOWED_TRANSITIONS.put(DisputeStatus.OPEN, EnumSet.of(
                DisputeStatus.UNDER_REVIEW,
                DisputeStatus.RESOLVED,
                DisputeStatus.ESCALATED
        ));

        // UNDER_REVIEW: In mediation review by staff or peers; can transition to
        // RESOLVED (settlement agreed) or ESCALATED (deadlock or 5 days elapsed).
        ALLOWED_TRANSITIONS.put(DisputeStatus.UNDER_REVIEW, EnumSet.of(
                DisputeStatus.RESOLVED,
                DisputeStatus.ESCALATED
        ));

        // ESCALATED: Escalated to Platform Administrator for binding arbitration;
        // transitions to RESOLVED upon execution of resolution and ledger reconciliation.
        ALLOWED_TRANSITIONS.put(DisputeStatus.ESCALATED, EnumSet.of(
                DisputeStatus.RESOLVED
        ));

        // RESOLVED: Immutable terminal state.
        ALLOWED_TRANSITIONS.put(DisputeStatus.RESOLVED, Collections.emptySet());
    }

    /**
     * Checks if transitioning from currentStatus to targetStatus is permitted.
     *
     * @param currentStatus the current lifecycle status
     * @param targetStatus the desired target status
     * @return true if permitted; false otherwise
     */
    public boolean isValidTransition(DisputeStatus currentStatus, DisputeStatus targetStatus) {
        if (currentStatus == null || targetStatus == null) {
            return false;
        }
        if (currentStatus == targetStatus) {
            return false;
        }
        Set<DisputeStatus> allowed = ALLOWED_TRANSITIONS.get(currentStatus);
        return allowed != null && allowed.contains(targetStatus);
    }

    /**
     * Validates that the requested transition is permitted, throwing an
     * {@link InvalidDisputeStateTransitionException} if rejected.
     *
     * @param currentStatus the current status
     * @param targetStatus the target status
     * @param disputeId the ID of the dispute being evaluated
     * @throws InvalidDisputeStateTransitionException if transition is disallowed
     */
    public void validateTransition(DisputeStatus currentStatus, DisputeStatus targetStatus, Long disputeId) {
        if (currentStatus == null) {
            throw new InvalidDisputeStateTransitionException("Current dispute status cannot be null", null, targetStatus, disputeId);
        }
        if (targetStatus == null) {
            throw new InvalidDisputeStateTransitionException("Target dispute status cannot be null", currentStatus, null, disputeId);
        }
        if (currentStatus == targetStatus) {
            throw new InvalidDisputeStateTransitionException(
                    String.format("Dispute [%s] is already in status '%s'", disputeId != null ? disputeId : "N/A", currentStatus),
                    currentStatus, targetStatus, disputeId
            );
        }
        if (isTerminal(currentStatus)) {
            throw new InvalidDisputeStateTransitionException(
                    String.format("Cannot transition dispute [%s] from terminal status '%s' to '%s'",
                            disputeId != null ? disputeId : "N/A", currentStatus, targetStatus),
                    currentStatus, targetStatus, disputeId
            );
        }
        if (!isValidTransition(currentStatus, targetStatus)) {
            log.warn("Invalid dispute transition rejected: [{}] -> [{}] for dispute [{}]",
                    currentStatus, targetStatus, disputeId);
            throw new InvalidDisputeStateTransitionException(currentStatus, targetStatus, disputeId);
        }
    }

    /**
     * Checks if a dispute status represents an immutable terminal state.
     *
     * @param status the status to test
     * @return true if terminal (RESOLVED); false otherwise
     */
    public boolean isTerminal(DisputeStatus status) {
        if (status == null) {
            return false;
        }
        return status == DisputeStatus.RESOLVED;
    }

    /**
     * Returns the unmodifiable set of allowed next statuses from the given status.
     *
     * @param currentStatus the current status
     * @return unmodifiable set of permissible next statuses
     */
    public Set<DisputeStatus> getAllowedTargetStatuses(DisputeStatus currentStatus) {
        if (currentStatus == null) {
            return Collections.emptySet();
        }
        return Collections.unmodifiableSet(ALLOWED_TRANSITIONS.getOrDefault(currentStatus, Collections.emptySet()));
    }
}
