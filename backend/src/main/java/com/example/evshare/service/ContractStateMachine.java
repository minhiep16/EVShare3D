package com.example.evshare.service;

import com.example.evshare.entity.enums.ContractStatus;
import com.example.evshare.exception.InvalidContractTransitionException;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * Authoritative Contract State Machine governing lifecycle transitions between
 * canonical contract states: DRAFT, PENDING_SIGNATURE, SIGNED, ACTIVE, EXPIRED, TERMINATED, REJECTED.
 *
 * Direct arbitrary transitions are strictly prohibited.
 */
@Component
public class ContractStateMachine {

    private static final Map<ContractStatus, Set<ContractStatus>> ALLOWED_TRANSITIONS = new EnumMap<>(ContractStatus.class);

    static {
        // DRAFT: Work in progress; can advance to PENDING_SIGNATURE when terms finalized, or TERMINATED if cancelled
        ALLOWED_TRANSITIONS.put(ContractStatus.DRAFT, EnumSet.of(
                ContractStatus.PENDING_SIGNATURE,
                ContractStatus.TERMINATED
        ));

        // PENDING_SIGNATURE: Open for member signing; can advance to SIGNED once all co-owners sign,
        // REJECTED if members vote down terms, or TERMINATED if cancelled
        ALLOWED_TRANSITIONS.put(ContractStatus.PENDING_SIGNATURE, EnumSet.of(
                ContractStatus.SIGNED,
                ContractStatus.TERMINATED,
                ContractStatus.REJECTED
        ));

        // SIGNED: Fully signed by syndicate; can be activated to ACTIVE by platform staff/admin,
        // or TERMINATED before taking legal effect
        ALLOWED_TRANSITIONS.put(ContractStatus.SIGNED, EnumSet.of(
                ContractStatus.ACTIVE,
                ContractStatus.TERMINATED
        ));

        // ACTIVE: Currently governing vehicle co-ownership; can transition to EXPIRED when tenure ends,
        // or TERMINATED upon group dissolution, vehicle sale, or superseding by a newer contract version
        ALLOWED_TRANSITIONS.put(ContractStatus.ACTIVE, EnumSet.of(
                ContractStatus.EXPIRED,
                ContractStatus.TERMINATED
        ));

        // EXPIRED: Terminal state; historical record preserved indefinitely
        ALLOWED_TRANSITIONS.put(ContractStatus.EXPIRED, Collections.emptySet());

        // TERMINATED: Terminal state; historical record preserved indefinitely
        ALLOWED_TRANSITIONS.put(ContractStatus.TERMINATED, Collections.emptySet());

        // REJECTED: Terminal state; historical draft rejected by syndicate
        ALLOWED_TRANSITIONS.put(ContractStatus.REJECTED, Collections.emptySet());
    }

    /**
     * Checks whether a transition from currentStatus to targetStatus is valid.
     *
     * @param currentStatus Current status of the contract
     * @param targetStatus  Target status to transition to
     * @return true if permitted, false otherwise
     */
    public boolean isValidTransition(ContractStatus currentStatus, ContractStatus targetStatus) {
        if (currentStatus == null || targetStatus == null) {
            return false;
        }
        if (currentStatus == targetStatus) {
            return false;
        }
        Set<ContractStatus> permitted = ALLOWED_TRANSITIONS.get(currentStatus);
        return permitted != null && permitted.contains(targetStatus);
    }

    /**
     * Validates that the requested transition is permitted according to state machine rules.
     *
     * @param currentStatus Current status of the contract
     * @param targetStatus  Target status to transition to
     * @throws InvalidContractTransitionException if transition is disallowed or redundant
     */
    public void validateTransition(ContractStatus currentStatus, ContractStatus targetStatus) {
        if (currentStatus == null) {
            throw new InvalidContractTransitionException("Current contract status is null", null, targetStatus);
        }
        if (targetStatus == null) {
            throw new InvalidContractTransitionException("Target contract status cannot be null", currentStatus, null);
        }
        if (currentStatus == targetStatus) {
            throw new InvalidContractTransitionException(
                    String.format("Redundant state transition: Contract is already in status '%s'", currentStatus),
                    currentStatus,
                    targetStatus
            );
        }
        if (!isValidTransition(currentStatus, targetStatus)) {
            throw new InvalidContractTransitionException(currentStatus, targetStatus);
        }
    }

    /**
     * Retrieves the set of allowable next statuses from the given current status.
     *
     * @param currentStatus Current status of the contract
     * @return Immutable set of permitted target statuses
     */
    public Set<ContractStatus> getAllowedTransitions(ContractStatus currentStatus) {
        if (currentStatus == null) {
            return Collections.emptySet();
        }
        Set<ContractStatus> permitted = ALLOWED_TRANSITIONS.get(currentStatus);
        return permitted != null ? Collections.unmodifiableSet(permitted) : Collections.emptySet();
    }
}
