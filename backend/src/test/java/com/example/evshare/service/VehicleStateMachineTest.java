package com.example.evshare.service;

import com.example.evshare.entity.enums.VehicleStatus;
import com.example.evshare.exception.InvalidStateTransitionException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Checkpoint 04-D — Vehicle State Machine Unit Tests")
class VehicleStateMachineTest {

    private VehicleStateMachine stateMachine;

    @BeforeEach
    void setUp() {
        stateMachine = new VehicleStateMachine();
    }

    @ParameterizedTest(name = "Valid transition: {0} -> {1}")
    @CsvSource({
            // AVAILABLE transitions
            "AVAILABLE, BOOKED",
            "AVAILABLE, CHARGING",
            "AVAILABLE, MAINTENANCE",
            "AVAILABLE, UNAVAILABLE",

            // BOOKED transitions
            "BOOKED, IN_USE",
            "BOOKED, AVAILABLE",
            "BOOKED, MAINTENANCE",
            "BOOKED, UNAVAILABLE",

            // IN_USE transitions
            "IN_USE, AVAILABLE",
            "IN_USE, CHARGING",
            "IN_USE, MAINTENANCE",
            "IN_USE, DAMAGED",

            // CHARGING transitions
            "CHARGING, AVAILABLE",
            "CHARGING, MAINTENANCE",
            "CHARGING, UNAVAILABLE",

            // MAINTENANCE transitions
            "MAINTENANCE, AVAILABLE",
            "MAINTENANCE, CHARGING",
            "MAINTENANCE, UNAVAILABLE",

            // DAMAGED transitions
            "DAMAGED, MAINTENANCE",
            "DAMAGED, UNAVAILABLE",

            // UNAVAILABLE transitions
            "UNAVAILABLE, AVAILABLE",
            "UNAVAILABLE, MAINTENANCE"
    })
    @DisplayName("1. Valid Transitions: Must validate and return true")
    void testValidTransitions(VehicleStatus from, VehicleStatus to) {
        assertTrue(stateMachine.isValidTransition(from, to),
                String.format("Expected transition from %s to %s to be valid", from, to));
        assertDoesNotThrow(() -> stateMachine.validateTransition(from, to),
                String.format("validateTransition should not throw for valid transition %s to %s", from, to));
    }

    @ParameterizedTest(name = "Invalid transition: {0} -> {1}")
    @CsvSource({
            // AVAILABLE cannot jump straight to IN_USE without booking
            "AVAILABLE, IN_USE",
            // AVAILABLE cannot arbitrarily be marked DAMAGED without usage
            "AVAILABLE, DAMAGED",

            // BOOKED cannot plug in or damage without session
            "BOOKED, CHARGING",
            "BOOKED, DAMAGED",

            // IN_USE cannot jump to BOOKED or direct UNAVAILABLE without checkout
            "IN_USE, BOOKED",
            "IN_USE, UNAVAILABLE",

            // CHARGING cannot drive or book directly
            "CHARGING, IN_USE",
            "CHARGING, BOOKED",
            "CHARGING, DAMAGED",

            // MAINTENANCE cannot drive or book directly
            "MAINTENANCE, IN_USE",
            "MAINTENANCE, BOOKED",
            "MAINTENANCE, DAMAGED",

            // DAMAGED cannot become AVAILABLE or BOOKED directly (must pass through MAINTENANCE)
            "DAMAGED, AVAILABLE",
            "DAMAGED, BOOKED",
            "DAMAGED, IN_USE",
            "DAMAGED, CHARGING",

            // UNAVAILABLE cannot book or drive directly
            "UNAVAILABLE, BOOKED",
            "UNAVAILABLE, IN_USE",
            "UNAVAILABLE, CHARGING",
            "UNAVAILABLE, DAMAGED"
    })
    @DisplayName("2. Invalid Transitions: Must reject with InvalidStateTransitionException")
    void testInvalidTransitions(VehicleStatus from, VehicleStatus to) {
        assertFalse(stateMachine.isValidTransition(from, to),
                String.format("Expected transition from %s to %s to be invalid", from, to));

        InvalidStateTransitionException ex = assertThrows(
                InvalidStateTransitionException.class,
                () -> stateMachine.validateTransition(from, to)
        );
        assertEquals(from, ex.getCurrentStatus());
        assertEquals(to, ex.getTargetStatus());
    }

    @ParameterizedTest
    @EnumSource(VehicleStatus.class)
    @DisplayName("3. Redundant Self-Transitions: Disallowed (transitioning to same status)")
    void testSelfTransitionsDisallowed(VehicleStatus status) {
        assertFalse(stateMachine.isValidTransition(status, status),
                "Self transition should be evaluated as not a valid state change");

        InvalidStateTransitionException ex = assertThrows(
                InvalidStateTransitionException.class,
                () -> stateMachine.validateTransition(status, status)
        );
        assertTrue(ex.getMessage().contains("already in status"));
    }

    @Test
    @DisplayName("4. Null Status Safety: Correctly throws on null values")
    void testNullStatusSafety() {
        assertFalse(stateMachine.isValidTransition(null, VehicleStatus.AVAILABLE));
        assertFalse(stateMachine.isValidTransition(VehicleStatus.AVAILABLE, null));
        assertFalse(stateMachine.isValidTransition(null, null));

        assertThrows(InvalidStateTransitionException.class, () -> stateMachine.validateTransition(null, VehicleStatus.AVAILABLE));
        assertThrows(InvalidStateTransitionException.class, () -> stateMachine.validateTransition(VehicleStatus.AVAILABLE, null));
    }

    @Test
    @DisplayName("5. Permitted Transitions Lookup: Inspect target states")
    void testGetPermittedTransitions() {
        Set<VehicleStatus> availableTargets = stateMachine.getPermittedTransitions(VehicleStatus.AVAILABLE);
        assertEquals(4, availableTargets.size());
        assertTrue(availableTargets.contains(VehicleStatus.BOOKED));
        assertTrue(availableTargets.contains(VehicleStatus.CHARGING));
        assertTrue(availableTargets.contains(VehicleStatus.MAINTENANCE));
        assertTrue(availableTargets.contains(VehicleStatus.UNAVAILABLE));

        Set<VehicleStatus> damagedTargets = stateMachine.getPermittedTransitions(VehicleStatus.DAMAGED);
        assertEquals(2, damagedTargets.size());
        assertTrue(damagedTargets.contains(VehicleStatus.MAINTENANCE));
        assertTrue(damagedTargets.contains(VehicleStatus.UNAVAILABLE));
        assertFalse(damagedTargets.contains(VehicleStatus.AVAILABLE));

        assertTrue(stateMachine.getPermittedTransitions(null).isEmpty());
    }
}
