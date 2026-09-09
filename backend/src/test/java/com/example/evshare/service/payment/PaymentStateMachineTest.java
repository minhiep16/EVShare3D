package com.example.evshare.service.payment;

import com.example.evshare.entity.enums.PaymentStatus;
import com.example.evshare.exception.InvalidPaymentStateTransitionException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Checkpoint 06-L — Payment Lifecycle State Machine Tests")
class PaymentStateMachineTest {

    private PaymentStateMachine stateMachine;

    private static final List<PaymentStatus> CANONICAL_STATES = List.of(
            PaymentStatus.PENDING,
            PaymentStatus.PROCESSING,
            PaymentStatus.SUCCESS,
            PaymentStatus.FAILED,
            PaymentStatus.REFUNDED,
            PaymentStatus.CANCELLED
    );

    @BeforeEach
    void setUp() {
        stateMachine = new PaymentStateMachine();
    }

    // =========================================================================
    // 1. ALL 8 VALID CANONICAL TRANSITIONS
    // =========================================================================

    @Nested
    @DisplayName("1. Valid Transitions Tests")
    class ValidTransitionsTests {

        @Test
        @DisplayName("1.1. PENDING -> PROCESSING (Valid: Gateway checkout opened / async clearing)")
        void testPendingToProcessing() {
            assertTrue(stateMachine.isValidTransition(PaymentStatus.PENDING, PaymentStatus.PROCESSING));
            assertDoesNotThrow(() -> stateMachine.validateTransition(PaymentStatus.PENDING, PaymentStatus.PROCESSING, "TX-001"));
        }

        @Test
        @DisplayName("1.2. PENDING -> SUCCESS (Valid: Instant settlement / direct confirmation)")
        void testPendingToSuccess() {
            assertTrue(stateMachine.isValidTransition(PaymentStatus.PENDING, PaymentStatus.SUCCESS));
            assertDoesNotThrow(() -> stateMachine.validateTransition(PaymentStatus.PENDING, PaymentStatus.SUCCESS, "TX-002"));
        }

        @Test
        @DisplayName("1.3. PENDING -> FAILED (Valid: Immediate validation failure or card error)")
        void testPendingToFailed() {
            assertTrue(stateMachine.isValidTransition(PaymentStatus.PENDING, PaymentStatus.FAILED));
            assertDoesNotThrow(() -> stateMachine.validateTransition(PaymentStatus.PENDING, PaymentStatus.FAILED, "TX-003"));
        }

        @Test
        @DisplayName("1.4. PENDING -> CANCELLED (Valid: User cancels checkout modal before processing)")
        void testPendingToCancelled() {
            assertTrue(stateMachine.isValidTransition(PaymentStatus.PENDING, PaymentStatus.CANCELLED));
            assertDoesNotThrow(() -> stateMachine.validateTransition(PaymentStatus.PENDING, PaymentStatus.CANCELLED, "TX-004"));
        }

        @Test
        @DisplayName("1.5. PROCESSING -> SUCCESS (Valid: Async clearing confirmed and captured)")
        void testProcessingToSuccess() {
            assertTrue(stateMachine.isValidTransition(PaymentStatus.PROCESSING, PaymentStatus.SUCCESS));
            assertDoesNotThrow(() -> stateMachine.validateTransition(PaymentStatus.PROCESSING, PaymentStatus.SUCCESS, "TX-005"));
        }

        @Test
        @DisplayName("1.6. PROCESSING -> FAILED (Valid: Gateway decline / bank timeout / 3DS decline)")
        void testProcessingToFailed() {
            assertTrue(stateMachine.isValidTransition(PaymentStatus.PROCESSING, PaymentStatus.FAILED));
            assertDoesNotThrow(() -> stateMachine.validateTransition(PaymentStatus.PROCESSING, PaymentStatus.FAILED, "TX-006"));
        }

        @Test
        @DisplayName("1.7. PROCESSING -> CANCELLED (Valid: User aborts during checkout / session expired)")
        void testProcessingToCancelled() {
            assertTrue(stateMachine.isValidTransition(PaymentStatus.PROCESSING, PaymentStatus.CANCELLED));
            assertDoesNotThrow(() -> stateMachine.validateTransition(PaymentStatus.PROCESSING, PaymentStatus.CANCELLED, "TX-007"));
        }

        @Test
        @DisplayName("1.8. SUCCESS -> REFUNDED (Valid: Settled transaction is refunded/reversed)")
        void testSuccessToRefunded() {
            assertTrue(stateMachine.isValidTransition(PaymentStatus.SUCCESS, PaymentStatus.REFUNDED));
            assertDoesNotThrow(() -> stateMachine.validateTransition(PaymentStatus.SUCCESS, PaymentStatus.REFUNDED, "TX-008"));
        }
    }

    // =========================================================================
    // 2. BACKWARD COMPATIBILITY WITH COMPLETED ALIAS
    // =========================================================================

    @Nested
    @DisplayName("2. Backward Compatibility with COMPLETED Alias")
    class CompletedAliasTests {

        @Test
        @DisplayName("2.1. PENDING -> COMPLETED is valid")
        void testPendingToCompleted() {
            assertTrue(stateMachine.isValidTransition(PaymentStatus.PENDING, PaymentStatus.COMPLETED));
            assertDoesNotThrow(() -> stateMachine.validateTransition(PaymentStatus.PENDING, PaymentStatus.COMPLETED, "TX-CMP-1"));
        }

        @Test
        @DisplayName("2.2. PROCESSING -> COMPLETED is valid")
        void testProcessingToCompleted() {
            assertTrue(stateMachine.isValidTransition(PaymentStatus.PROCESSING, PaymentStatus.COMPLETED));
            assertDoesNotThrow(() -> stateMachine.validateTransition(PaymentStatus.PROCESSING, PaymentStatus.COMPLETED, "TX-CMP-2"));
        }

        @Test
        @DisplayName("2.3. COMPLETED -> REFUNDED is valid")
        void testCompletedToRefunded() {
            assertTrue(stateMachine.isValidTransition(PaymentStatus.COMPLETED, PaymentStatus.REFUNDED));
            assertDoesNotThrow(() -> stateMachine.validateTransition(PaymentStatus.COMPLETED, PaymentStatus.REFUNDED, "TX-CMP-3"));
        }

        @Test
        @DisplayName("2.4. SUCCESS -> COMPLETED is rejected as redundant settled transition")
        void testSuccessToCompletedRejected() {
            assertFalse(stateMachine.isValidTransition(PaymentStatus.SUCCESS, PaymentStatus.COMPLETED));
            assertThrows(InvalidPaymentStateTransitionException.class,
                    () -> stateMachine.validateTransition(PaymentStatus.SUCCESS, PaymentStatus.COMPLETED, "TX-CMP-4"));
        }

        @Test
        @DisplayName("2.5. COMPLETED -> SUCCESS is rejected as redundant settled transition")
        void testCompletedToSuccessRejected() {
            assertFalse(stateMachine.isValidTransition(PaymentStatus.COMPLETED, PaymentStatus.SUCCESS));
            assertThrows(InvalidPaymentStateTransitionException.class,
                    () -> stateMachine.validateTransition(PaymentStatus.COMPLETED, PaymentStatus.SUCCESS, "TX-CMP-5"));
        }
    }

    // =========================================================================
    // 3. EXPLICIT INVALID TRANSITIONS ACROSS ALL CANONICAL COMBINATIONS
    // =========================================================================

    @Nested
    @DisplayName("3. Invalid Transitions by Origin State")
    class InvalidTransitionsByOriginTests {

        @Test
        @DisplayName("3.1. PENDING invalid transitions (PENDING -> PENDING, PENDING -> REFUNDED)")
        void testPendingInvalidTransitions() {
            // Self-transition
            assertFalse(stateMachine.isValidTransition(PaymentStatus.PENDING, PaymentStatus.PENDING));
            InvalidPaymentStateTransitionException ex1 = assertThrows(InvalidPaymentStateTransitionException.class,
                    () -> stateMachine.validateTransition(PaymentStatus.PENDING, PaymentStatus.PENDING, "TX-P-P"));
            assertTrue(ex1.getMessage().contains("Redundant payment state transition"));

            // Cannot refund unpaid
            assertFalse(stateMachine.isValidTransition(PaymentStatus.PENDING, PaymentStatus.REFUNDED));
            InvalidPaymentStateTransitionException ex2 = assertThrows(InvalidPaymentStateTransitionException.class,
                    () -> stateMachine.validateTransition(PaymentStatus.PENDING, PaymentStatus.REFUNDED, "TX-P-R"));
            assertTrue(ex2.getMessage().contains("Cannot transition payment"));
        }

        @Test
        @DisplayName("3.2. PROCESSING invalid transitions (PROCESSING -> PENDING, PROCESSING -> PROCESSING, PROCESSING -> REFUNDED)")
        void testProcessingInvalidTransitions() {
            // Backward to pending
            assertFalse(stateMachine.isValidTransition(PaymentStatus.PROCESSING, PaymentStatus.PENDING));
            assertThrows(InvalidPaymentStateTransitionException.class,
                    () -> stateMachine.validateTransition(PaymentStatus.PROCESSING, PaymentStatus.PENDING, "TX-PR-P"));

            // Self transition
            assertFalse(stateMachine.isValidTransition(PaymentStatus.PROCESSING, PaymentStatus.PROCESSING));
            assertThrows(InvalidPaymentStateTransitionException.class,
                    () -> stateMachine.validateTransition(PaymentStatus.PROCESSING, PaymentStatus.PROCESSING, "TX-PR-PR"));

            // Cannot refund uncaptured processing payment
            assertFalse(stateMachine.isValidTransition(PaymentStatus.PROCESSING, PaymentStatus.REFUNDED));
            assertThrows(InvalidPaymentStateTransitionException.class,
                    () -> stateMachine.validateTransition(PaymentStatus.PROCESSING, PaymentStatus.REFUNDED, "TX-PR-R"));
        }

        @Test
        @DisplayName("3.3. SUCCESS invalid transitions (SUCCESS -> PENDING, PROCESSING, SUCCESS, FAILED, CANCELLED)")
        void testSuccessInvalidTransitions() {
            // SUCCESS -> PENDING
            assertFalse(stateMachine.isValidTransition(PaymentStatus.SUCCESS, PaymentStatus.PENDING));
            assertThrows(InvalidPaymentStateTransitionException.class,
                    () -> stateMachine.validateTransition(PaymentStatus.SUCCESS, PaymentStatus.PENDING, "TX-S-P"));

            // SUCCESS -> PROCESSING
            assertFalse(stateMachine.isValidTransition(PaymentStatus.SUCCESS, PaymentStatus.PROCESSING));
            assertThrows(InvalidPaymentStateTransitionException.class,
                    () -> stateMachine.validateTransition(PaymentStatus.SUCCESS, PaymentStatus.PROCESSING, "TX-S-PR"));

            // SUCCESS -> SUCCESS (Self)
            assertFalse(stateMachine.isValidTransition(PaymentStatus.SUCCESS, PaymentStatus.SUCCESS));
            assertThrows(InvalidPaymentStateTransitionException.class,
                    () -> stateMachine.validateTransition(PaymentStatus.SUCCESS, PaymentStatus.SUCCESS, "TX-S-S"));

            // SUCCESS -> FAILED
            assertFalse(stateMachine.isValidTransition(PaymentStatus.SUCCESS, PaymentStatus.FAILED));
            assertThrows(InvalidPaymentStateTransitionException.class,
                    () -> stateMachine.validateTransition(PaymentStatus.SUCCESS, PaymentStatus.FAILED, "TX-S-F"));

            // SUCCESS -> CANCELLED (Settled payments must be refunded, cannot be cancelled)
            assertFalse(stateMachine.isValidTransition(PaymentStatus.SUCCESS, PaymentStatus.CANCELLED));
            assertThrows(InvalidPaymentStateTransitionException.class,
                    () -> stateMachine.validateTransition(PaymentStatus.SUCCESS, PaymentStatus.CANCELLED, "TX-S-C"));
        }

        @Test
        @DisplayName("3.4. FAILED terminal transitions (Cannot transition to ANY state)")
        void testFailedTerminalTransitions() {
            for (PaymentStatus target : CANONICAL_STATES) {
                assertFalse(stateMachine.isValidTransition(PaymentStatus.FAILED, target),
                        "FAILED should not transition to " + target);
                InvalidPaymentStateTransitionException ex = assertThrows(InvalidPaymentStateTransitionException.class,
                        () -> stateMachine.validateTransition(PaymentStatus.FAILED, target, "TX-F-" + target.name()));
                assertTrue(ex.getMessage().contains("Cannot transition payment") || ex.getMessage().contains("terminal"));
            }
        }

        @Test
        @DisplayName("3.5. REFUNDED terminal transitions (Cannot transition to ANY state)")
        void testRefundedTerminalTransitions() {
            for (PaymentStatus target : CANONICAL_STATES) {
                assertFalse(stateMachine.isValidTransition(PaymentStatus.REFUNDED, target),
                        "REFUNDED should not transition to " + target);
                InvalidPaymentStateTransitionException ex = assertThrows(InvalidPaymentStateTransitionException.class,
                        () -> stateMachine.validateTransition(PaymentStatus.REFUNDED, target, "TX-R-" + target.name()));
                assertTrue(ex.getMessage().contains("Cannot transition payment") || ex.getMessage().contains("terminal"));
            }
        }

        @Test
        @DisplayName("3.6. CANCELLED terminal transitions (Cannot transition to ANY state)")
        void testCancelledTerminalTransitions() {
            for (PaymentStatus target : CANONICAL_STATES) {
                assertFalse(stateMachine.isValidTransition(PaymentStatus.CANCELLED, target),
                        "CANCELLED should not transition to " + target);
                InvalidPaymentStateTransitionException ex = assertThrows(InvalidPaymentStateTransitionException.class,
                        () -> stateMachine.validateTransition(PaymentStatus.CANCELLED, target, "TX-C-" + target.name()));
                assertTrue(ex.getMessage().contains("Cannot transition payment") || ex.getMessage().contains("terminal"));
            }
        }
    }

    // =========================================================================
    // 4. EXHAUSTIVE 36-COMBINATION PERMUTATION AUDIT
    // =========================================================================

    @Nested
    @DisplayName("4. Exhaustive 36-Permutation Matrix Audit")
    class ExhaustiveMatrixAuditTests {

        @ParameterizedTest(name = "{0} -> {1} must be VALID")
        @CsvSource({
                "PENDING, PROCESSING",
                "PENDING, SUCCESS",
                "PENDING, FAILED",
                "PENDING, CANCELLED",
                "PROCESSING, SUCCESS",
                "PROCESSING, FAILED",
                "PROCESSING, CANCELLED",
                "SUCCESS, REFUNDED"
        })
        @DisplayName("4.1. Exactly the 8 canonical permitted transitions must succeed")
        void testExactlyEightValidTransitions(PaymentStatus from, PaymentStatus to) {
            assertTrue(stateMachine.isValidTransition(from, to));
            assertDoesNotThrow(() -> stateMachine.validateTransition(from, to, "TX-VALID"));
        }

        @Test
        @DisplayName("4.2. Verify the remaining 28 of 36 permutations are strictly REJECTED")
        void testRemainingTwentyEightInvalidTransitions() {
            int validCount = 0;
            int invalidCount = 0;

            for (PaymentStatus from : CANONICAL_STATES) {
                for (PaymentStatus to : CANONICAL_STATES) {
                    if (stateMachine.isValidTransition(from, to)) {
                        validCount++;
                        assertDoesNotThrow(() -> stateMachine.validateTransition(from, to, "TEST-VALID"));
                    } else {
                        invalidCount++;
                        InvalidPaymentStateTransitionException ex = assertThrows(
                                InvalidPaymentStateTransitionException.class,
                                () -> stateMachine.validateTransition(from, to, "TEST-INVALID"),
                                String.format("Expected transition %s -> %s to throw exception", from, to)
                        );
                        assertNotNull(ex.getMessage());
                        assertEquals(from, ex.getCurrentStatus());
                        assertEquals(to, ex.getTargetStatus());
                    }
                }
            }

            assertEquals(8, validCount, "There must be exactly 8 valid canonical transitions");
            assertEquals(28, invalidCount, "There must be exactly 28 invalid canonical transitions");
            assertEquals(36, validCount + invalidCount, "Total canonical permutations must equal 36");
        }
    }

    // =========================================================================
    // 5. NULL GUARDS & TERMINAL STATE INSPECTION
    // =========================================================================

    @Nested
    @DisplayName("5. Null Guards & State Property Checks")
    class NullAndPropertyTests {

        @Test
        @DisplayName("5.1. Null current status throws structured exception")
        void testNullCurrentStatus() {
            assertFalse(stateMachine.isValidTransition(null, PaymentStatus.SUCCESS));
            InvalidPaymentStateTransitionException ex = assertThrows(InvalidPaymentStateTransitionException.class,
                    () -> stateMachine.validateTransition(null, PaymentStatus.SUCCESS, "TX-NULL-CUR"));
            assertTrue(ex.getMessage().contains("Current payment status cannot be null"));
            assertNull(ex.getCurrentStatus());
            assertEquals(PaymentStatus.SUCCESS, ex.getTargetStatus());
        }

        @Test
        @DisplayName("5.2. Null target status throws structured exception")
        void testNullTargetStatus() {
            assertFalse(stateMachine.isValidTransition(PaymentStatus.PENDING, null));
            InvalidPaymentStateTransitionException ex = assertThrows(InvalidPaymentStateTransitionException.class,
                    () -> stateMachine.validateTransition(PaymentStatus.PENDING, null, "TX-NULL-TGT"));
            assertTrue(ex.getMessage().contains("Target payment status cannot be null"));
            assertEquals(PaymentStatus.PENDING, ex.getCurrentStatus());
            assertNull(ex.getTargetStatus());
        }

        @Test
        @DisplayName("5.3. Null current and target status throws structured exception")
        void testBothNullStatuses() {
            assertFalse(stateMachine.isValidTransition(null, null));
            assertThrows(InvalidPaymentStateTransitionException.class,
                    () -> stateMachine.validateTransition(null, null, "TX-NULL-BOTH"));
        }

        @Test
        @DisplayName("5.4. isTerminal checks correctly identify terminal and non-terminal states")
        void testIsTerminalChecks() {
            assertFalse(stateMachine.isTerminal(PaymentStatus.PENDING));
            assertFalse(stateMachine.isTerminal(PaymentStatus.PROCESSING));
            assertFalse(stateMachine.isTerminal(PaymentStatus.SUCCESS));
            assertFalse(stateMachine.isTerminal(PaymentStatus.COMPLETED));

            assertTrue(stateMachine.isTerminal(PaymentStatus.FAILED));
            assertTrue(stateMachine.isTerminal(PaymentStatus.REFUNDED));
            assertTrue(stateMachine.isTerminal(PaymentStatus.CANCELLED));

            assertFalse(stateMachine.isTerminal(null));
        }

        @Test
        @DisplayName("5.5. isSettled checks identify settled states")
        void testIsSettledChecks() {
            assertTrue(stateMachine.isSettled(PaymentStatus.SUCCESS));
            assertTrue(stateMachine.isSettled(PaymentStatus.COMPLETED));

            assertFalse(stateMachine.isSettled(PaymentStatus.PENDING));
            assertFalse(stateMachine.isSettled(PaymentStatus.PROCESSING));
            assertFalse(stateMachine.isSettled(PaymentStatus.FAILED));
            assertFalse(stateMachine.isSettled(PaymentStatus.REFUNDED));
            assertFalse(stateMachine.isSettled(PaymentStatus.CANCELLED));
            assertFalse(stateMachine.isSettled(null));
        }

        @Test
        @DisplayName("5.6. getAllowedTransitions returns immutable and accurate sets")
        void testGetAllowedTransitions() {
            Set<PaymentStatus> pendingAllowed = stateMachine.getAllowedTransitions(PaymentStatus.PENDING);
            assertTrue(pendingAllowed.contains(PaymentStatus.PROCESSING));
            assertTrue(pendingAllowed.contains(PaymentStatus.SUCCESS));
            assertTrue(pendingAllowed.contains(PaymentStatus.FAILED));
            assertTrue(pendingAllowed.contains(PaymentStatus.CANCELLED));

            Set<PaymentStatus> failedAllowed = stateMachine.getAllowedTransitions(PaymentStatus.FAILED);
            assertTrue(failedAllowed.isEmpty());

            Set<PaymentStatus> refundedAllowed = stateMachine.getAllowedTransitions(PaymentStatus.REFUNDED);
            assertTrue(refundedAllowed.isEmpty());

            Set<PaymentStatus> cancelledAllowed = stateMachine.getAllowedTransitions(PaymentStatus.CANCELLED);
            assertTrue(cancelledAllowed.isEmpty());

            Set<PaymentStatus> nullAllowed = stateMachine.getAllowedTransitions(null);
            assertTrue(nullAllowed.isEmpty());
        }
    }
}
