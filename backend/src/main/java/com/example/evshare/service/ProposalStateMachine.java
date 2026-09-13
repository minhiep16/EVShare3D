package com.example.evshare.service;

import com.example.evshare.entity.enums.ProposalStatus;
import com.example.evshare.exception.InvalidProposalStateTransitionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * Authoritative Proposal Lifecycle State Machine governing transitions between:
 * ACTIVE, PASSED, REJECTED, EXPIRED.
 *
 * Rules:
 * - ACTIVE -> PASSED, REJECTED, EXPIRED
 * - PASSED -> Terminal (no further transitions permitted)
 * - REJECTED -> Terminal (no further transitions permitted)
 * - EXPIRED -> Terminal (no further transitions permitted)
 * - Redundant transitions (currentStatus == targetStatus) are strictly rejected.
 */
@Component
public class ProposalStateMachine {

    private static final Logger log = LoggerFactory.getLogger(ProposalStateMachine.class);

    private static final Map<ProposalStatus, Set<ProposalStatus>> ALLOWED_TRANSITIONS = new EnumMap<>(ProposalStatus.class);

    static {
        // ACTIVE: Proposal is open for voting deliberation;
        // can transition to PASSED, REJECTED, or EXPIRED.
        ALLOWED_TRANSITIONS.put(ProposalStatus.ACTIVE, EnumSet.of(
                ProposalStatus.PASSED,
                ProposalStatus.REJECTED,
                ProposalStatus.EXPIRED
        ));

        // PASSED: Terminal state; proposal has been approved by syndicate vote.
        ALLOWED_TRANSITIONS.put(ProposalStatus.PASSED, Collections.emptySet());

        // REJECTED: Terminal state; proposal failed to achieve required approval threshold.
        ALLOWED_TRANSITIONS.put(ProposalStatus.REJECTED, Collections.emptySet());

        // EXPIRED: Terminal state; voting deadline elapsed without meeting mandatory quorum.
        ALLOWED_TRANSITIONS.put(ProposalStatus.EXPIRED, Collections.emptySet());
    }

    /**
     * Checks if transitioning from currentStatus to targetStatus is permitted.
     *
     * @param currentStatus the current lifecycle status
     * @param targetStatus the desired target status
     * @return true if permitted; false otherwise
     */
    public boolean isValidTransition(ProposalStatus currentStatus, ProposalStatus targetStatus) {
        if (currentStatus == null || targetStatus == null) {
            return false;
        }
        if (currentStatus == targetStatus) {
            return false;
        }
        Set<ProposalStatus> allowed = ALLOWED_TRANSITIONS.get(currentStatus);
        return allowed != null && allowed.contains(targetStatus);
    }

    /**
     * Validates that the requested transition is permitted, throwing an
     * {@link InvalidProposalStateTransitionException} if rejected.
     *
     * @param currentStatus the current status
     * @param targetStatus the target status
     * @param proposalId the ID of the proposal being evaluated
     * @throws InvalidProposalStateTransitionException if transition is disallowed
     */
    public void validateTransition(ProposalStatus currentStatus, ProposalStatus targetStatus, Long proposalId) {
        if (currentStatus == null) {
            throw new InvalidProposalStateTransitionException("Current proposal status cannot be null", null, targetStatus, proposalId);
        }
        if (targetStatus == null) {
            throw new InvalidProposalStateTransitionException("Target proposal status cannot be null", currentStatus, null, proposalId);
        }
        if (currentStatus == targetStatus) {
            throw new InvalidProposalStateTransitionException(
                    String.format("Proposal [%s] is already in status '%s'", proposalId != null ? proposalId : "N/A", currentStatus),
                    currentStatus, targetStatus, proposalId
            );
        }
        if (isTerminal(currentStatus)) {
            throw new InvalidProposalStateTransitionException(
                    String.format("Cannot transition proposal [%s] from terminal status '%s' to '%s'",
                            proposalId != null ? proposalId : "N/A", currentStatus, targetStatus),
                    currentStatus, targetStatus, proposalId
            );
        }
        if (!isValidTransition(currentStatus, targetStatus)) {
            log.warn("Invalid proposal transition rejected: [{}] -> [{}] for proposal [{}]",
                    currentStatus, targetStatus, proposalId);
            throw new InvalidProposalStateTransitionException(currentStatus, targetStatus, proposalId);
        }
    }

    /**
     * Checks if a proposal status represents an immutable terminal state.
     *
     * @param status the status to test
     * @return true if terminal (PASSED, REJECTED, EXPIRED); false otherwise
     */
    public boolean isTerminal(ProposalStatus status) {
        if (status == null) {
            return false;
        }
        return status == ProposalStatus.PASSED
                || status == ProposalStatus.REJECTED
                || status == ProposalStatus.EXPIRED;
    }

    /**
     * Returns the unmodifiable set of allowed next statuses from the given status.
     *
     * @param currentStatus the current status
     * @return unmodifiable set of permissible next statuses
     */
    public Set<ProposalStatus> getAllowedTargetStatuses(ProposalStatus currentStatus) {
        if (currentStatus == null) {
            return Collections.emptySet();
        }
        return Collections.unmodifiableSet(ALLOWED_TRANSITIONS.getOrDefault(currentStatus, Collections.emptySet()));
    }
}
