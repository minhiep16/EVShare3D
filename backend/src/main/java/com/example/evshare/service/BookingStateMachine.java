package com.example.evshare.service;

import com.example.evshare.entity.enums.BookingStatus;
import com.example.evshare.exception.InvalidBookingTransitionException;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * Authoritative Booking State Machine governing lifecycle transitions between
 * the eight canonical states: PENDING, APPROVED, CONFIRMED, IN_USE, COMPLETED, CANCELLED, REJECTED, NO_SHOW.
 *
 * Direct arbitrary transitions are strictly prohibited according to EVShare 3D
 * business rules (BR-BKG-01, BR-BKG-02, BR-BKG-03, BR-OPS-01, BR-OPS-02).
 */
@Component
public class BookingStateMachine {

    private static final Map<BookingStatus, Set<BookingStatus>> ALLOWED_TRANSITIONS = new EnumMap<>(BookingStatus.class);

    static {
        // PENDING: Initial booking request awaiting approval or automatic confirmation;
        // can transition to APPROVED, direct CONFIRMED, REJECTED, or user CANCELLED.
        ALLOWED_TRANSITIONS.put(BookingStatus.PENDING, EnumSet.of(
                BookingStatus.APPROVED,
                BookingStatus.CONFIRMED,
                BookingStatus.REJECTED,
                BookingStatus.CANCELLED
        ));

        // APPROVED: Approved by syndicate / policy;
        // can transition to CONFIRMED (calendar locked), CANCELLED by user, or REJECTED by admin.
        ALLOWED_TRANSITIONS.put(BookingStatus.APPROVED, EnumSet.of(
                BookingStatus.CONFIRMED,
                BookingStatus.CANCELLED,
                BookingStatus.REJECTED
        ));

        // CONFIRMED: Locked on vehicle calendar;
        // co-owner checks in (IN_USE), cancels booking (CANCELLED), misses grace period (NO_SHOW),
        // or admin rejects/revokes (REJECTED).
        ALLOWED_TRANSITIONS.put(BookingStatus.CONFIRMED, EnumSet.of(
                BookingStatus.IN_USE,
                BookingStatus.CANCELLED,
                BookingStatus.NO_SHOW,
                BookingStatus.REJECTED
        ));

        // IN_USE: Actively checked out and on the road;
        // returns vehicle and completes check-out telemetry (COMPLETED).
        // Active trips cannot be cancelled, rejected, or marked as no-show.
        ALLOWED_TRANSITIONS.put(BookingStatus.IN_USE, EnumSet.of(
                BookingStatus.COMPLETED
        ));

        // COMPLETED: Terminal state; historical trip finalized and immutable.
        ALLOWED_TRANSITIONS.put(BookingStatus.COMPLETED, Collections.emptySet());

        // CANCELLED: Terminal state; reservation cancelled before or during check-in window.
        ALLOWED_TRANSITIONS.put(BookingStatus.CANCELLED, Collections.emptySet());

        // REJECTED: Terminal state; reservation rejected by syndicate, policy, or admin.
        ALLOWED_TRANSITIONS.put(BookingStatus.REJECTED, Collections.emptySet());

        // NO_SHOW: Terminal state; 30-min check-in grace period expired with full penalty.
        ALLOWED_TRANSITIONS.put(BookingStatus.NO_SHOW, Collections.emptySet());
    }

    /**
     * Checks whether a transition from currentStatus to targetStatus is valid.
     *
     * @param currentStatus Current status of the booking
     * @param targetStatus  Target status to transition to
     * @return true if permitted, false otherwise
     */
    public boolean isValidTransition(BookingStatus currentStatus, BookingStatus targetStatus) {
        if (currentStatus == null || targetStatus == null) {
            return false;
        }
        if (currentStatus == targetStatus) {
            return false;
        }
        Set<BookingStatus> permitted = ALLOWED_TRANSITIONS.get(currentStatus);
        return permitted != null && permitted.contains(targetStatus);
    }

    /**
     * Validates that the requested transition is permitted according to state machine rules.
     *
     * @param currentStatus Current status of the booking
     * @param targetStatus  Target status to transition to
     * @throws InvalidBookingTransitionException if transition is disallowed, redundant, or inputs are null
     */
    public void validateTransition(BookingStatus currentStatus, BookingStatus targetStatus) {
        if (currentStatus == null) {
            throw new InvalidBookingTransitionException("Current booking status cannot be null", null, targetStatus);
        }
        if (targetStatus == null) {
            throw new InvalidBookingTransitionException("Target booking status cannot be null", currentStatus, null);
        }
        if (currentStatus == targetStatus) {
            throw new InvalidBookingTransitionException(
                    String.format("Redundant state transition: Booking is already in status '%s'", currentStatus),
                    currentStatus,
                    targetStatus
            );
        }
        if (!isValidTransition(currentStatus, targetStatus)) {
            throw new InvalidBookingTransitionException(
                    String.format("Invalid booking status transition: Cannot transition booking from status '%s' to '%s'", currentStatus, targetStatus),
                    currentStatus,
                    targetStatus
            );
        }
    }

    /**
     * Returns an unmodifiable set of allowed target statuses from the given current status.
     *
     * @param currentStatus Current status of the booking
     * @return Set of allowed target statuses, or an empty set if terminal or unknown
     */
    public Set<BookingStatus> getAllowedTransitions(BookingStatus currentStatus) {
        if (currentStatus == null) {
            return Collections.emptySet();
        }
        Set<BookingStatus> transitions = ALLOWED_TRANSITIONS.get(currentStatus);
        return transitions != null ? Collections.unmodifiableSet(transitions) : Collections.emptySet();
    }

    /**
     * Checks if a booking status is a terminal state (no transitions allowed out of this state).
     *
     * @param status Status to evaluate
     * @return true if terminal, false otherwise
     */
    public boolean isTerminalState(BookingStatus status) {
        if (status == null) {
            return false;
        }
        Set<BookingStatus> transitions = ALLOWED_TRANSITIONS.get(status);
        return transitions != null && transitions.isEmpty();
    }
}
