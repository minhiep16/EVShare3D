package com.example.evshare.service;

import com.example.evshare.entity.enums.BookingStatus;
import com.example.evshare.exception.InvalidBookingTransitionException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;

import java.util.EnumSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Checkpoint 05-G — Booking State Machine Unit Tests")
class BookingStateMachineTest {

    private BookingStateMachine stateMachine;

    @BeforeEach
    void setUp() {
        stateMachine = new BookingStateMachine();
    }

    @ParameterizedTest(name = "Valid transition: {0} -> {1}")
    @CsvSource({
            // PENDING transitions (4 valid)
            "PENDING, APPROVED",
            "PENDING, CONFIRMED",
            "PENDING, REJECTED",
            "PENDING, CANCELLED",

            // APPROVED transitions (3 valid)
            "APPROVED, CONFIRMED",
            "APPROVED, CANCELLED",
            "APPROVED, REJECTED",

            // CONFIRMED transitions (4 valid)
            "CONFIRMED, IN_USE",
            "CONFIRMED, CANCELLED",
            "CONFIRMED, NO_SHOW",
            "CONFIRMED, REJECTED",

            // IN_USE transitions (1 valid)
            "IN_USE, COMPLETED"
    })
    @DisplayName("1. Valid Transitions: Must validate successfully and return true")
    void testValidTransitions(BookingStatus from, BookingStatus to) {
        assertTrue(stateMachine.isValidTransition(from, to),
                String.format("Expected transition from %s to %s to be valid", from, to));
        assertDoesNotThrow(() -> stateMachine.validateTransition(from, to),
                String.format("validateTransition should not throw for valid transition from %s to %s", from, to));
    }

    @ParameterizedTest(name = "Invalid transition: {0} -> {1}")
    @CsvSource({
            // PENDING cannot skip directly to IN_USE or COMPLETED
            "PENDING, IN_USE",
            "PENDING, COMPLETED",
            "PENDING, NO_SHOW",

            // APPROVED cannot jump to IN_USE without confirmation
            "APPROVED, IN_USE",
            "APPROVED, COMPLETED",
            "APPROVED, NO_SHOW",

            // CONFIRMED cannot jump directly to COMPLETED without IN_USE
            "CONFIRMED, COMPLETED",
            "CONFIRMED, PENDING",
            "CONFIRMED, APPROVED",

            // IN_USE cannot be cancelled, rejected, or marked no-show while driving
            "IN_USE, CANCELLED",
            "IN_USE, REJECTED",
            "IN_USE, NO_SHOW",
            "IN_USE, PENDING",
            "IN_USE, APPROVED",
            "IN_USE, CONFIRMED",

            // COMPLETED is terminal; cannot transition anywhere
            "COMPLETED, PENDING",
            "COMPLETED, APPROVED",
            "COMPLETED, CONFIRMED",
            "COMPLETED, IN_USE",
            "COMPLETED, CANCELLED",
            "COMPLETED, REJECTED",
            "COMPLETED, NO_SHOW",

            // CANCELLED is terminal; cannot transition anywhere
            "CANCELLED, PENDING",
            "CANCELLED, APPROVED",
            "CANCELLED, CONFIRMED",
            "CANCELLED, IN_USE",
            "CANCELLED, COMPLETED",
            "CANCELLED, REJECTED",
            "CANCELLED, NO_SHOW",

            // REJECTED is terminal; cannot transition anywhere
            "REJECTED, PENDING",
            "REJECTED, APPROVED",
            "REJECTED, CONFIRMED",
            "REJECTED, IN_USE",
            "REJECTED, COMPLETED",
            "REJECTED, CANCELLED",
            "REJECTED, NO_SHOW",

            // NO_SHOW is terminal; cannot transition anywhere
            "NO_SHOW, PENDING",
            "NO_SHOW, APPROVED",
            "NO_SHOW, CONFIRMED",
            "NO_SHOW, IN_USE",
            "NO_SHOW, COMPLETED",
            "NO_SHOW, CANCELLED",
            "NO_SHOW, REJECTED"
    })
    @DisplayName("2. Invalid Transitions: Must reject with InvalidBookingTransitionException")
    void testInvalidTransitions(BookingStatus from, BookingStatus to) {
        assertFalse(stateMachine.isValidTransition(from, to),
                String.format("Expected transition from %s to %s to be invalid", from, to));

        InvalidBookingTransitionException ex = assertThrows(
                InvalidBookingTransitionException.class,
                () -> stateMachine.validateTransition(from, to)
        );
        assertEquals(from, ex.getCurrentStatus());
        assertEquals(to, ex.getTargetStatus());
    }

    @ParameterizedTest
    @EnumSource(BookingStatus.class)
    @DisplayName("3. Redundant Self-Transitions: Disallowed (cannot transition to same status)")
    void testSelfTransitionsDisallowed(BookingStatus status) {
        assertFalse(stateMachine.isValidTransition(status, status),
                "Self transition should evaluate to false");

        InvalidBookingTransitionException ex = assertThrows(
                InvalidBookingTransitionException.class,
                () -> stateMachine.validateTransition(status, status)
        );
        assertTrue(ex.getMessage().contains("already in status"));
        assertEquals(status, ex.getCurrentStatus());
        assertEquals(status, ex.getTargetStatus());
    }

    @Test
    @DisplayName("4. Exhaustive 8x8 Matrix Validation: Tests all 64 state permutations")
    void testExhaustiveStateMatrix() {
        // Authoritative set of the 12 valid transition pairs
        Set<String> validPairs = Set.of(
                "PENDING->APPROVED",
                "PENDING->CONFIRMED",
                "PENDING->REJECTED",
                "PENDING->CANCELLED",
                "APPROVED->CONFIRMED",
                "APPROVED->CANCELLED",
                "APPROVED->REJECTED",
                "CONFIRMED->IN_USE",
                "CONFIRMED->CANCELLED",
                "CONFIRMED->NO_SHOW",
                "CONFIRMED->REJECTED",
                "IN_USE->COMPLETED"
        );

        int validCount = 0;
        int invalidCount = 0;

        for (BookingStatus from : BookingStatus.values()) {
            for (BookingStatus to : BookingStatus.values()) {
                String pair = from.name() + "->" + to.name();
                if (validPairs.contains(pair)) {
                    validCount++;
                    assertTrue(stateMachine.isValidTransition(from, to), "Should be valid: " + pair);
                    assertDoesNotThrow(() -> stateMachine.validateTransition(from, to), "Should not throw: " + pair);
                } else {
                    invalidCount++;
                    assertFalse(stateMachine.isValidTransition(from, to), "Should be invalid: " + pair);
                    assertThrows(InvalidBookingTransitionException.class,
                            () -> stateMachine.validateTransition(from, to), "Should throw: " + pair);
                }
            }
        }

        assertEquals(12, validCount, "Must have exactly 12 valid state transitions");
        assertEquals(52, invalidCount, "Must have exactly 52 invalid state transitions (64 total - 12 valid)");
    }

    @Test
    @DisplayName("5. Null Safety: Throws on null status inputs")
    void testNullStatusSafety() {
        assertFalse(stateMachine.isValidTransition(null, BookingStatus.CONFIRMED));
        assertFalse(stateMachine.isValidTransition(BookingStatus.CONFIRMED, null));
        assertFalse(stateMachine.isValidTransition(null, null));

        assertThrows(InvalidBookingTransitionException.class,
                () -> stateMachine.validateTransition(null, BookingStatus.CONFIRMED));
        assertThrows(InvalidBookingTransitionException.class,
                () -> stateMachine.validateTransition(BookingStatus.CONFIRMED, null));
    }

    @Test
    @DisplayName("6. Lookup Allowed Transitions: Verify correct target sets")
    void testGetAllowedTransitions() {
        Set<BookingStatus> pendingTargets = stateMachine.getAllowedTransitions(BookingStatus.PENDING);
        assertEquals(EnumSet.of(BookingStatus.APPROVED, BookingStatus.CONFIRMED, BookingStatus.REJECTED, BookingStatus.CANCELLED), pendingTargets);

        Set<BookingStatus> approvedTargets = stateMachine.getAllowedTransitions(BookingStatus.APPROVED);
        assertEquals(EnumSet.of(BookingStatus.CONFIRMED, BookingStatus.CANCELLED, BookingStatus.REJECTED), approvedTargets);

        Set<BookingStatus> confirmedTargets = stateMachine.getAllowedTransitions(BookingStatus.CONFIRMED);
        assertEquals(EnumSet.of(BookingStatus.IN_USE, BookingStatus.CANCELLED, BookingStatus.NO_SHOW, BookingStatus.REJECTED), confirmedTargets);

        Set<BookingStatus> inUseTargets = stateMachine.getAllowedTransitions(BookingStatus.IN_USE);
        assertEquals(EnumSet.of(BookingStatus.COMPLETED), inUseTargets);

        assertTrue(stateMachine.getAllowedTransitions(BookingStatus.COMPLETED).isEmpty());
        assertTrue(stateMachine.getAllowedTransitions(BookingStatus.CANCELLED).isEmpty());
        assertTrue(stateMachine.getAllowedTransitions(BookingStatus.REJECTED).isEmpty());
        assertTrue(stateMachine.getAllowedTransitions(BookingStatus.NO_SHOW).isEmpty());
        assertTrue(stateMachine.getAllowedTransitions(null).isEmpty());
    }

    @Test
    @DisplayName("7. Terminal State Predicate: Correctly identifies immutable end states")
    void testTerminalStates() {
        assertFalse(stateMachine.isTerminalState(BookingStatus.PENDING));
        assertFalse(stateMachine.isTerminalState(BookingStatus.APPROVED));
        assertFalse(stateMachine.isTerminalState(BookingStatus.CONFIRMED));
        assertFalse(stateMachine.isTerminalState(BookingStatus.IN_USE));

        assertTrue(stateMachine.isTerminalState(BookingStatus.COMPLETED));
        assertTrue(stateMachine.isTerminalState(BookingStatus.CANCELLED));
        assertTrue(stateMachine.isTerminalState(BookingStatus.REJECTED));
        assertTrue(stateMachine.isTerminalState(BookingStatus.NO_SHOW));

        assertFalse(stateMachine.isTerminalState(null));
    }
}
