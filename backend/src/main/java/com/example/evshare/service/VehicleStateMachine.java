package com.example.evshare.service;

import com.example.evshare.entity.enums.VehicleStatus;
import com.example.evshare.exception.InvalidStateTransitionException;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * Authoritative Vehicle State Machine governing lifecycle transitions between
 * the seven canonical states: AVAILABLE, BOOKED, IN_USE, CHARGING, MAINTENANCE, DAMAGED, UNAVAILABLE.
 *
 * Direct arbitrary transitions are strictly prohibited.
 */
@Component
public class VehicleStateMachine {

    private static final Map<VehicleStatus, Set<VehicleStatus>> ALLOWED_TRANSITIONS = new EnumMap<>(VehicleStatus.class);

    static {
        // AVAILABLE: Ready for booking, charging, routine maintenance, or admin lockout
        ALLOWED_TRANSITIONS.put(VehicleStatus.AVAILABLE, EnumSet.of(
                VehicleStatus.BOOKED,
                VehicleStatus.CHARGING,
                VehicleStatus.MAINTENANCE,
                VehicleStatus.UNAVAILABLE
        ));

        // BOOKED: Reserved for upcoming trip; can start trip (IN_USE), cancel back to AVAILABLE, or undergo urgent maintenance/lockout
        ALLOWED_TRANSITIONS.put(VehicleStatus.BOOKED, EnumSet.of(
                VehicleStatus.IN_USE,
                VehicleStatus.AVAILABLE,
                VehicleStatus.MAINTENANCE,
                VehicleStatus.UNAVAILABLE
        ));

        // IN_USE: Actively checked out; return to AVAILABLE, plug in (CHARGING), flag for MAINTENANCE, or log physical damage (DAMAGED)
        ALLOWED_TRANSITIONS.put(VehicleStatus.IN_USE, EnumSet.of(
                VehicleStatus.AVAILABLE,
                VehicleStatus.CHARGING,
                VehicleStatus.MAINTENANCE,
                VehicleStatus.DAMAGED
        ));

        // CHARGING: Connected to stall charger; unplug to AVAILABLE, direct to MAINTENANCE on fault, or admin UNAVAILABLE
        ALLOWED_TRANSITIONS.put(VehicleStatus.CHARGING, EnumSet.of(
                VehicleStatus.AVAILABLE,
                VehicleStatus.MAINTENANCE,
                VehicleStatus.UNAVAILABLE
        ));

        // MAINTENANCE: In workshop; upon completion return to AVAILABLE, CHARGING (top-up battery), or mark UNAVAILABLE (decommissioned)
        ALLOWED_TRANSITIONS.put(VehicleStatus.MAINTENANCE, EnumSet.of(
                VehicleStatus.AVAILABLE,
                VehicleStatus.CHARGING,
                VehicleStatus.UNAVAILABLE
        ));

        // DAMAGED: Defect detected; must route to MAINTENANCE for repairs, or UNAVAILABLE if total loss (cannot jump directly to AVAILABLE/BOOKED/IN_USE)
        ALLOWED_TRANSITIONS.put(VehicleStatus.DAMAGED, EnumSet.of(
                VehicleStatus.MAINTENANCE,
                VehicleStatus.UNAVAILABLE
        ));

        // UNAVAILABLE: Admin lockout; can be reactivated to AVAILABLE or sent for checkup in MAINTENANCE
        ALLOWED_TRANSITIONS.put(VehicleStatus.UNAVAILABLE, EnumSet.of(
                VehicleStatus.AVAILABLE,
                VehicleStatus.MAINTENANCE
        ));
    }

    /**
     * Checks whether a transition from currentStatus to targetStatus is valid.
     *
     * @param currentStatus Current status of the vehicle
     * @param targetStatus  Target status to transition to
     * @return true if permitted, false otherwise
     */
    public boolean isValidTransition(VehicleStatus currentStatus, VehicleStatus targetStatus) {
        if (currentStatus == null || targetStatus == null) {
            return false;
        }
        if (currentStatus == targetStatus) {
            return false;
        }
        Set<VehicleStatus> permitted = ALLOWED_TRANSITIONS.get(currentStatus);
        return permitted != null && permitted.contains(targetStatus);
    }

    /**
     * Validates that the requested transition is permitted according to state machine rules.
     *
     * @param currentStatus Current status of the vehicle
     * @param targetStatus  Target status to transition to
     * @throws InvalidStateTransitionException if transition is disallowed or redundant
     */
    public void validateTransition(VehicleStatus currentStatus, VehicleStatus targetStatus) {
        if (currentStatus == null) {
            throw new InvalidStateTransitionException("Vehicle current status is null", null, targetStatus);
        }
        if (targetStatus == null) {
            throw new InvalidStateTransitionException("Target vehicle status cannot be null", currentStatus, null);
        }
        if (currentStatus == targetStatus) {
            throw new InvalidStateTransitionException(
                    String.format("Redundant state transition: Vehicle is already in status '%s'", currentStatus),
                    currentStatus,
                    targetStatus
            );
        }
        if (!isValidTransition(currentStatus, targetStatus)) {
            throw new InvalidStateTransitionException(currentStatus, targetStatus);
        }
    }

    /**
     * Retrieves the set of permitted target states from a given status.
     *
     * @param currentStatus Current status
     * @return Read-only set of permitted target states
     */
    public Set<VehicleStatus> getPermittedTransitions(VehicleStatus currentStatus) {
        if (currentStatus == null) {
            return Collections.emptySet();
        }
        return Collections.unmodifiableSet(ALLOWED_TRANSITIONS.getOrDefault(currentStatus, Collections.emptySet()));
    }
}
