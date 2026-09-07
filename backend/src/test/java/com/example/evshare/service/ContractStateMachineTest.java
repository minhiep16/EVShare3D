package com.example.evshare.service;

import com.example.evshare.entity.enums.ContractStatus;
import com.example.evshare.exception.InvalidContractTransitionException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;

import java.util.EnumSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Checkpoint 04-K — Contract State Machine Unit Tests")
class ContractStateMachineTest {

    private ContractStateMachine stateMachine;

    @BeforeEach
    void setUp() {
        stateMachine = new ContractStateMachine();
    }

    @ParameterizedTest(name = "Valid transition: {0} -> {1}")
    @CsvSource({
            // DRAFT transitions
            "DRAFT, PENDING_SIGNATURE",
            "DRAFT, TERMINATED",

            // PENDING_SIGNATURE transitions
            "PENDING_SIGNATURE, SIGNED",
            "PENDING_SIGNATURE, TERMINATED",
            "PENDING_SIGNATURE, REJECTED",

            // SIGNED transitions
            "SIGNED, ACTIVE",
            "SIGNED, TERMINATED",

            // ACTIVE transitions
            "ACTIVE, EXPIRED",
            "ACTIVE, TERMINATED"
    })
    @DisplayName("1. Valid Transitions: Must validate successfully and return true")
    void testValidTransitions(ContractStatus from, ContractStatus to) {
        assertTrue(stateMachine.isValidTransition(from, to),
                String.format("Expected transition from %s to %s to be valid", from, to));
        assertDoesNotThrow(() -> stateMachine.validateTransition(from, to),
                String.format("validateTransition should not throw for valid transition from %s to %s", from, to));
    }

    @ParameterizedTest(name = "Invalid transition: {0} -> {1}")
    @CsvSource({
            // DRAFT cannot skip to SIGNED, ACTIVE, EXPIRED, or REJECTED
            "DRAFT, SIGNED",
            "DRAFT, ACTIVE",
            "DRAFT, EXPIRED",
            "DRAFT, REJECTED",

            // PENDING_SIGNATURE cannot revert to DRAFT or jump to ACTIVE / EXPIRED
            "PENDING_SIGNATURE, DRAFT",
            "PENDING_SIGNATURE, ACTIVE",
            "PENDING_SIGNATURE, EXPIRED",

            // SIGNED cannot revert to DRAFT or PENDING_SIGNATURE or jump to EXPIRED / REJECTED
            "SIGNED, DRAFT",
            "SIGNED, PENDING_SIGNATURE",
            "SIGNED, EXPIRED",
            "SIGNED, REJECTED",

            // ACTIVE cannot revert to DRAFT, PENDING_SIGNATURE, or SIGNED
            "ACTIVE, DRAFT",
            "ACTIVE, PENDING_SIGNATURE",
            "ACTIVE, SIGNED",
            "ACTIVE, REJECTED",

            // EXPIRED is terminal; cannot transition anywhere
            "EXPIRED, DRAFT",
            "EXPIRED, PENDING_SIGNATURE",
            "EXPIRED, SIGNED",
            "EXPIRED, ACTIVE",
            "EXPIRED, TERMINATED",
            "EXPIRED, REJECTED",

            // TERMINATED is terminal; cannot transition anywhere
            "TERMINATED, DRAFT",
            "TERMINATED, PENDING_SIGNATURE",
            "TERMINATED, SIGNED",
            "TERMINATED, ACTIVE",
            "TERMINATED, EXPIRED",
            "TERMINATED, REJECTED",

            // REJECTED is terminal; cannot transition anywhere
            "REJECTED, DRAFT",
            "REJECTED, PENDING_SIGNATURE",
            "REJECTED, SIGNED",
            "REJECTED, ACTIVE",
            "REJECTED, EXPIRED",
            "REJECTED, TERMINATED"
    })
    @DisplayName("2. Invalid Transitions: Must reject with InvalidContractTransitionException")
    void testInvalidTransitions(ContractStatus from, ContractStatus to) {
        assertFalse(stateMachine.isValidTransition(from, to),
                String.format("Expected transition from %s to %s to be invalid", from, to));

        InvalidContractTransitionException ex = assertThrows(
                InvalidContractTransitionException.class,
                () -> stateMachine.validateTransition(from, to)
        );
        assertEquals(from, ex.getCurrentStatus());
        assertEquals(to, ex.getTargetStatus());
    }

    @ParameterizedTest
    @EnumSource(ContractStatus.class)
    @DisplayName("3. Redundant Self-Transitions: Disallowed (cannot transition to same status)")
    void testSelfTransitionsDisallowed(ContractStatus status) {
        assertFalse(stateMachine.isValidTransition(status, status),
                "Self transition should evaluate to false");

        InvalidContractTransitionException ex = assertThrows(
                InvalidContractTransitionException.class,
                () -> stateMachine.validateTransition(status, status)
        );
        assertTrue(ex.getMessage().contains("already in status"));
    }

    @Test
    @DisplayName("4. Exhaustive 7x7 Matrix Validation: Tests all 49 state permutations")
    void testExhaustiveStateMatrix() {
        // Explicit authoritative valid transition pairs
        Set<String> validPairs = Set.of(
                "DRAFT->PENDING_SIGNATURE",
                "DRAFT->TERMINATED",
                "PENDING_SIGNATURE->SIGNED",
                "PENDING_SIGNATURE->TERMINATED",
                "PENDING_SIGNATURE->REJECTED",
                "SIGNED->ACTIVE",
                "SIGNED->TERMINATED",
                "ACTIVE->EXPIRED",
                "ACTIVE->TERMINATED"
        );

        for (ContractStatus from : ContractStatus.values()) {
            for (ContractStatus to : ContractStatus.values()) {
                String pair = from.name() + "->" + to.name();
                if (validPairs.contains(pair)) {
                    assertTrue(stateMachine.isValidTransition(from, to), "Should be valid: " + pair);
                    assertDoesNotThrow(() -> stateMachine.validateTransition(from, to), "Should not throw: " + pair);
                } else {
                    assertFalse(stateMachine.isValidTransition(from, to), "Should be invalid: " + pair);
                    assertThrows(InvalidContractTransitionException.class,
                            () -> stateMachine.validateTransition(from, to), "Should throw: " + pair);
                }
            }
        }
    }

    @Test
    @DisplayName("5. Null Safety: Throws on null status inputs")
    void testNullStatusSafety() {
        assertFalse(stateMachine.isValidTransition(null, ContractStatus.DRAFT));
        assertFalse(stateMachine.isValidTransition(ContractStatus.DRAFT, null));
        assertFalse(stateMachine.isValidTransition(null, null));

        assertThrows(InvalidContractTransitionException.class,
                () -> stateMachine.validateTransition(null, ContractStatus.DRAFT));
        assertThrows(InvalidContractTransitionException.class,
                () -> stateMachine.validateTransition(ContractStatus.DRAFT, null));
    }

    @Test
    @DisplayName("6. Lookup Allowed Transitions: Verify correct target sets")
    void testGetAllowedTransitions() {
        Set<ContractStatus> draftTargets = stateMachine.getAllowedTransitions(ContractStatus.DRAFT);
        assertEquals(EnumSet.of(ContractStatus.PENDING_SIGNATURE, ContractStatus.TERMINATED), draftTargets);

        Set<ContractStatus> pendingTargets = stateMachine.getAllowedTransitions(ContractStatus.PENDING_SIGNATURE);
        assertEquals(EnumSet.of(ContractStatus.SIGNED, ContractStatus.TERMINATED, ContractStatus.REJECTED), pendingTargets);

        Set<ContractStatus> signedTargets = stateMachine.getAllowedTransitions(ContractStatus.SIGNED);
        assertEquals(EnumSet.of(ContractStatus.ACTIVE, ContractStatus.TERMINATED), signedTargets);

        Set<ContractStatus> activeTargets = stateMachine.getAllowedTransitions(ContractStatus.ACTIVE);
        assertEquals(EnumSet.of(ContractStatus.EXPIRED, ContractStatus.TERMINATED), activeTargets);

        assertTrue(stateMachine.getAllowedTransitions(ContractStatus.EXPIRED).isEmpty());
        assertTrue(stateMachine.getAllowedTransitions(ContractStatus.TERMINATED).isEmpty());
        assertTrue(stateMachine.getAllowedTransitions(ContractStatus.REJECTED).isEmpty());
        assertTrue(stateMachine.getAllowedTransitions(null).isEmpty());
    }
}
