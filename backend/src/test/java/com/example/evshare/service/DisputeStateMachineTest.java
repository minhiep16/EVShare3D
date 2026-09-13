package com.example.evshare.service;

import com.example.evshare.entity.enums.DisputeStatus;
import com.example.evshare.exception.InvalidDisputeStateTransitionException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class DisputeStateMachineTest {

    private DisputeStateMachine stateMachine;

    @BeforeEach
    void setUp() {
        stateMachine = new DisputeStateMachine();
    }

    @ParameterizedTest(name = "Valid: {0} -> {1}")
    @CsvSource({
            "OPEN, UNDER_REVIEW",
            "OPEN, RESOLVED",
            "OPEN, ESCALATED",
            "UNDER_REVIEW, RESOLVED",
            "UNDER_REVIEW, ESCALATED",
            "ESCALATED, RESOLVED"
    })
    @DisplayName("07-K: Permitted dispute transitions pass validation")
    void testValidTransitions(DisputeStatus from, DisputeStatus to) {
        assertTrue(stateMachine.isValidTransition(from, to),
                String.format("Expected %s -> %s to be valid", from, to));
        assertDoesNotThrow(() -> stateMachine.validateTransition(from, to, 1L));
    }

    @ParameterizedTest(name = "Invalid: {0} -> {1}")
    @CsvSource({
            "RESOLVED, OPEN",
            "RESOLVED, UNDER_REVIEW",
            "RESOLVED, ESCALATED",
            "ESCALATED, OPEN",
            "ESCALATED, UNDER_REVIEW",
            "UNDER_REVIEW, OPEN"
    })
    @DisplayName("07-K: Disallowed dispute transitions throw InvalidDisputeStateTransitionException")
    void testInvalidTransitions(DisputeStatus from, DisputeStatus to) {
        assertFalse(stateMachine.isValidTransition(from, to),
                String.format("Expected %s -> %s to be invalid", from, to));
        assertThrows(InvalidDisputeStateTransitionException.class,
                () -> stateMachine.validateTransition(from, to, 1L));
    }

    @Test
    @DisplayName("07-K: Redundant transitions (same status) are rejected")
    void testRedundantTransitions_rejected() {
        for (DisputeStatus status : DisputeStatus.values()) {
            assertFalse(stateMachine.isValidTransition(status, status));
            assertThrows(InvalidDisputeStateTransitionException.class,
                    () -> stateMachine.validateTransition(status, status, 10L));
        }
    }

    @Test
    @DisplayName("07-K: Terminal state immutability - RESOLVED is terminal")
    void testTerminalState() {
        assertTrue(stateMachine.isTerminal(DisputeStatus.RESOLVED));
        assertFalse(stateMachine.isTerminal(DisputeStatus.OPEN));
        assertFalse(stateMachine.isTerminal(DisputeStatus.UNDER_REVIEW));
        assertFalse(stateMachine.isTerminal(DisputeStatus.ESCALATED));
        assertFalse(stateMachine.isTerminal(null));
    }

    @Test
    @DisplayName("07-K: Null arguments handling")
    void testNullArguments() {
        assertFalse(stateMachine.isValidTransition(null, DisputeStatus.OPEN));
        assertFalse(stateMachine.isValidTransition(DisputeStatus.OPEN, null));
        assertFalse(stateMachine.isValidTransition(null, null));

        assertThrows(InvalidDisputeStateTransitionException.class,
                () -> stateMachine.validateTransition(null, DisputeStatus.OPEN, 1L));
        assertThrows(InvalidDisputeStateTransitionException.class,
                () -> stateMachine.validateTransition(DisputeStatus.OPEN, null, 1L));
    }

    @Test
    @DisplayName("07-K: Permissible target statuses set verification")
    void testGetAllowedTargetStatuses() {
        Set<DisputeStatus> fromOpen = stateMachine.getAllowedTargetStatuses(DisputeStatus.OPEN);
        assertEquals(3, fromOpen.size());
        assertTrue(fromOpen.contains(DisputeStatus.UNDER_REVIEW));
        assertTrue(fromOpen.contains(DisputeStatus.RESOLVED));
        assertTrue(fromOpen.contains(DisputeStatus.ESCALATED));

        Set<DisputeStatus> fromUnderReview = stateMachine.getAllowedTargetStatuses(DisputeStatus.UNDER_REVIEW);
        assertEquals(2, fromUnderReview.size());
        assertTrue(fromUnderReview.contains(DisputeStatus.RESOLVED));
        assertTrue(fromUnderReview.contains(DisputeStatus.ESCALATED));

        Set<DisputeStatus> fromEscalated = stateMachine.getAllowedTargetStatuses(DisputeStatus.ESCALATED);
        assertEquals(1, fromEscalated.size());
        assertTrue(fromEscalated.contains(DisputeStatus.RESOLVED));

        Set<DisputeStatus> fromResolved = stateMachine.getAllowedTargetStatuses(DisputeStatus.RESOLVED);
        assertTrue(fromResolved.isEmpty(), "RESOLVED is terminal and must have empty allowed next targets");
    }
}
