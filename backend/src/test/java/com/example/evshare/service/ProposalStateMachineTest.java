package com.example.evshare.service;

import com.example.evshare.entity.enums.ProposalStatus;
import com.example.evshare.exception.InvalidProposalStateTransitionException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Checkpoint 07-C — Proposal State Machine Unit Tests")
class ProposalStateMachineTest {

    private ProposalStateMachine stateMachine;

    @BeforeEach
    void setUp() {
        stateMachine = new ProposalStateMachine();
    }

    @Nested
    @DisplayName("Valid Transitions Tests")
    class ValidTransitionsTests {

        @Test
        @DisplayName("ACTIVE -> PASSED: Permitted when proposal succeeds in vote")
        void activeToPassed_permitted() {
            assertTrue(stateMachine.isValidTransition(ProposalStatus.ACTIVE, ProposalStatus.PASSED));
            assertDoesNotThrow(() -> stateMachine.validateTransition(ProposalStatus.ACTIVE, ProposalStatus.PASSED, 1L));
        }

        @Test
        @DisplayName("ACTIVE -> REJECTED: Permitted when proposal fails vote")
        void activeToRejected_permitted() {
            assertTrue(stateMachine.isValidTransition(ProposalStatus.ACTIVE, ProposalStatus.REJECTED));
            assertDoesNotThrow(() -> stateMachine.validateTransition(ProposalStatus.ACTIVE, ProposalStatus.REJECTED, 1L));
        }

        @Test
        @DisplayName("ACTIVE -> EXPIRED: Permitted when voting deadline expires without quorum")
        void activeToExpired_permitted() {
            assertTrue(stateMachine.isValidTransition(ProposalStatus.ACTIVE, ProposalStatus.EXPIRED));
            assertDoesNotThrow(() -> stateMachine.validateTransition(ProposalStatus.ACTIVE, ProposalStatus.EXPIRED, 1L));
        }
    }

    @Nested
    @DisplayName("Terminal States Tests")
    class TerminalStatesTests {

        @Test
        @DisplayName("PASSED is a terminal state; all outgoing transitions rejected")
        void passed_isTerminal_rejectsAll() {
            assertTrue(stateMachine.isTerminal(ProposalStatus.PASSED));
            assertEquals(0, stateMachine.getAllowedTargetStatuses(ProposalStatus.PASSED).size());

            for (ProposalStatus target : ProposalStatus.values()) {
                assertFalse(stateMachine.isValidTransition(ProposalStatus.PASSED, target));
                assertThrows(InvalidProposalStateTransitionException.class, () ->
                        stateMachine.validateTransition(ProposalStatus.PASSED, target, 1L));
            }
        }

        @Test
        @DisplayName("REJECTED is a terminal state; all outgoing transitions rejected")
        void rejected_isTerminal_rejectsAll() {
            assertTrue(stateMachine.isTerminal(ProposalStatus.REJECTED));
            assertEquals(0, stateMachine.getAllowedTargetStatuses(ProposalStatus.REJECTED).size());

            for (ProposalStatus target : ProposalStatus.values()) {
                assertFalse(stateMachine.isValidTransition(ProposalStatus.REJECTED, target));
                assertThrows(InvalidProposalStateTransitionException.class, () ->
                        stateMachine.validateTransition(ProposalStatus.REJECTED, target, 1L));
            }
        }

        @Test
        @DisplayName("EXPIRED is a terminal state; all outgoing transitions rejected")
        void expired_isTerminal_rejectsAll() {
            assertTrue(stateMachine.isTerminal(ProposalStatus.EXPIRED));
            assertEquals(0, stateMachine.getAllowedTargetStatuses(ProposalStatus.EXPIRED).size());

            for (ProposalStatus target : ProposalStatus.values()) {
                assertFalse(stateMachine.isValidTransition(ProposalStatus.EXPIRED, target));
                assertThrows(InvalidProposalStateTransitionException.class, () ->
                        stateMachine.validateTransition(ProposalStatus.EXPIRED, target, 1L));
            }
        }

        @Test
        @DisplayName("ACTIVE is not terminal")
        void active_isNotTerminal() {
            assertFalse(stateMachine.isTerminal(ProposalStatus.ACTIVE));
            Set<ProposalStatus> allowed = stateMachine.getAllowedTargetStatuses(ProposalStatus.ACTIVE);
            assertEquals(3, allowed.size());
            assertTrue(allowed.contains(ProposalStatus.PASSED));
            assertTrue(allowed.contains(ProposalStatus.REJECTED));
            assertTrue(allowed.contains(ProposalStatus.EXPIRED));
        }
    }

    @Nested
    @DisplayName("Redundant Transitions Tests")
    class RedundantTransitionsTests {

        @ParameterizedTest
        @EnumSource(ProposalStatus.class)
        @DisplayName("Self-transitions (status -> status) must be strictly rejected")
        void selfTransitions_rejected(ProposalStatus status) {
            assertFalse(stateMachine.isValidTransition(status, status));
            assertThrows(InvalidProposalStateTransitionException.class, () ->
                    stateMachine.validateTransition(status, status, 1L));
        }
    }

    @Nested
    @DisplayName("Null and Bounds Tests")
    class NullAndBoundsTests {

        @Test
        @DisplayName("Null current status handled safely")
        void nullCurrentStatus_handled() {
            assertFalse(stateMachine.isValidTransition(null, ProposalStatus.PASSED));
            assertThrows(InvalidProposalStateTransitionException.class, () ->
                    stateMachine.validateTransition(null, ProposalStatus.PASSED, 1L));
        }

        @Test
        @DisplayName("Null target status handled safely")
        void nullTargetStatus_handled() {
            assertFalse(stateMachine.isValidTransition(ProposalStatus.ACTIVE, null));
            assertThrows(InvalidProposalStateTransitionException.class, () ->
                    stateMachine.validateTransition(ProposalStatus.ACTIVE, null, 1L));
        }

        @Test
        @DisplayName("Null status is not terminal")
        void nullStatus_notTerminal() {
            assertFalse(stateMachine.isTerminal(null));
            assertTrue(stateMachine.getAllowedTargetStatuses(null).isEmpty());
        }
    }

    @Nested
    @DisplayName("Exhaustive Matrix Audit Test")
    class ExhaustiveMatrixAuditTest {

        @Test
        @DisplayName("Exhaustively verify all 16 state permutations: exactly 3 valid, 13 invalid")
        void exhaustiveMatrix_16Permutations() {
            int validCount = 0;
            int invalidCount = 0;

            for (ProposalStatus current : ProposalStatus.values()) {
                for (ProposalStatus target : ProposalStatus.values()) {
                    boolean isValid = stateMachine.isValidTransition(current, target);
                    if (isValid) {
                        validCount++;
                        assertDoesNotThrow(() -> stateMachine.validateTransition(current, target, 99L));
                    } else {
                        invalidCount++;
                        assertThrows(InvalidProposalStateTransitionException.class,
                                () -> stateMachine.validateTransition(current, target, 99L));
                    }
                }
            }

            assertEquals(3, validCount, "Exactly 3 valid transitions must exist (ACTIVE -> PASSED, REJECTED, EXPIRED)");
            assertEquals(13, invalidCount, "Exactly 13 transitions must be invalid");
        }
    }
}
