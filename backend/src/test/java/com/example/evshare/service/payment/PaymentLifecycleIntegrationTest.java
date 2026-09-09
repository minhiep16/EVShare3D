package com.example.evshare.service.payment;

import com.example.evshare.dto.response.PaymentAuditLogResponse;
import com.example.evshare.entity.*;
import com.example.evshare.entity.enums.PaymentMethod;
import com.example.evshare.entity.enums.PaymentStatus;
import com.example.evshare.entity.enums.RoleName;
import com.example.evshare.entity.enums.VehicleStatus;
import com.example.evshare.exception.InvalidPaymentStateTransitionException;
import com.example.evshare.exception.ResourceNotFoundException;
import com.example.evshare.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
@DisplayName("Checkpoint 06-L — Payment Lifecycle & History Preservation Integration Tests")
class PaymentLifecycleIntegrationTest {

    @Autowired
    private PaymentLifecycleService paymentLifecycleService;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private VehicleRepository vehicleRepository;

    @Autowired
    private OwnershipGroupRepository ownershipGroupRepository;

    @Autowired
    private SharedFundRepository sharedFundRepository;

    private User testUser;
    private SharedFund testFund;

    @BeforeEach
    void setUp() {
        Role roleCoOwner = roleRepository.findByName(RoleName.ROLE_CO_OWNER)
                .orElseGet(() -> roleRepository.save(new Role(null, RoleName.ROLE_CO_OWNER)));

        String uid = UUID.randomUUID().toString().substring(0, 8);
        testUser = new User();
        testUser.setEmail("pay.owner." + uid + "@evshare.io");
        testUser.setFullName("Nguyen Van Thanh");
        testUser.setPasswordHash("hashed_pwd_placeholder");
        testUser.setPhoneNumber("09" + (int)(Math.random() * 90000000 + 10000000));
        testUser.setIsActive(true);
        testUser.setRoles(java.util.Set.of(roleCoOwner));
        testUser = userRepository.saveAndFlush(testUser);

        Vehicle vehicle = new Vehicle();
        vehicle.setVin("VIN" + uid.toUpperCase() + "00000000");
        vehicle.setLicensePlate("51K-" + uid.toUpperCase());
        vehicle.setModelName("VinFast VF8 Plus");
        vehicle.setManufacturer("VinFast");
        vehicle.setModel3dAssetPath("models/vehicles/vf8.glb");
        vehicle.setStatus(VehicleStatus.AVAILABLE);
        vehicle = vehicleRepository.saveAndFlush(vehicle);

        OwnershipGroup group = new OwnershipGroup();
        group.setGroupName("Syndicate " + uid);
        group.setVehicle(vehicle);
        group.setFormationDate(LocalDate.now());
        group.setIsActive(true);
        group = ownershipGroupRepository.saveAndFlush(group);

        testFund = new SharedFund();
        testFund.setGroup(group);
        testFund.setCurrentBalance(new BigDecimal("15000000.00"));
        testFund.setMinimumReserveThreshold(new BigDecimal("10000000.00"));
        testFund.setCurrency("VND");
        testFund = sharedFundRepository.saveAndFlush(testFund);
    }

    private Payment createTestPayment(PaymentStatus initialStatus, String refSuffix) {
        String ref = "TX-LIFECYCLE-" + UUID.randomUUID().toString().substring(0, 8) + "-" + refSuffix;
        Payment payment = new Payment();
        payment.setUser(testUser);
        payment.setFund(testFund);
        payment.setAmount(new BigDecimal("2500000.00"));
        payment.setPaymentMethod(PaymentMethod.BANK_TRANSFER);
        payment.setTransactionReference(ref);
        payment.setStatus(initialStatus);
        return paymentRepository.saveAndFlush(payment);
    }

    // =========================================================================
    // 1. COMPLETE VALID LIFECYCLE TRAJECTORIES
    // =========================================================================

    @Nested
    @DisplayName("1. Valid Lifecycle Trajectories")
    class ValidTrajectoriesTests {

        @Test
        @DisplayName("1.1. Full Lifecycle Trajectory: PENDING -> PROCESSING -> SUCCESS -> REFUNDED")
        void testFullLifecycleTrajectoryWithRefund() {
            Payment payment = createTestPayment(PaymentStatus.PENDING, "FULL");
            Long paymentId = payment.getId();

            // Step 1: PENDING -> PROCESSING
            Payment p1 = paymentLifecycleService.transitionStatus(
                    paymentId, PaymentStatus.PROCESSING, "Customer redirected to gateway checkout", testUser.getId());
            assertEquals(PaymentStatus.PROCESSING, p1.getStatus());

            // Step 2: PROCESSING -> SUCCESS
            Payment p2 = paymentLifecycleService.transitionStatus(
                    paymentId, PaymentStatus.SUCCESS, "Webhook verified capture settlement", testUser.getId());
            assertEquals(PaymentStatus.SUCCESS, p2.getStatus());

            // Step 3: SUCCESS -> REFUNDED
            Payment p3 = paymentLifecycleService.transitionStatus(
                    paymentId, PaymentStatus.REFUNDED, "Customer requested booking cancellation refund", testUser.getId());
            assertEquals(PaymentStatus.REFUNDED, p3.getStatus());

            // Verify persistence in DB
            Payment reloaded = paymentRepository.findById(paymentId).orElseThrow();
            assertEquals(PaymentStatus.REFUNDED, reloaded.getStatus());

            // Verify history preservation
            List<PaymentAuditLogResponse> history = paymentLifecycleService.getPaymentHistory(paymentId, testUser.getId());
            assertEquals(3, history.size(), "Should have exactly 3 audit trail records");

            // Most recent first (order by id desc)
            assertEquals("PAYMENT_TRANSITION_REFUNDED", history.get(0).getAction());
            assertTrue(history.get(0).getNewStateJson().contains("REFUNDED"));
            assertTrue(history.get(0).getNewStateJson().contains("Customer requested booking cancellation refund"));

            assertEquals("PAYMENT_TRANSITION_SUCCESS", history.get(1).getAction());
            assertTrue(history.get(1).getNewStateJson().contains("SUCCESS"));

            assertEquals("PAYMENT_TRANSITION_PROCESSING", history.get(2).getAction());
            assertTrue(history.get(2).getNewStateJson().contains("PROCESSING"));
        }

        @Test
        @DisplayName("1.2. Direct Instant Capture: PENDING -> SUCCESS")
        void testDirectInstantSettlementTrajectory() {
            Payment payment = createTestPayment(PaymentStatus.PENDING, "DIRECT");

            Payment settled = paymentLifecycleService.transitionStatus(
                    payment.getId(), PaymentStatus.SUCCESS, "Direct instant authorization", testUser.getId());
            assertEquals(PaymentStatus.SUCCESS, settled.getStatus());

            List<PaymentAuditLogResponse> history = paymentLifecycleService.getPaymentHistory(payment.getId(), testUser.getId());
            assertEquals(1, history.size());
            assertEquals("PAYMENT_TRANSITION_SUCCESS", history.get(0).getAction());
            assertTrue(history.get(0).getOldStateJson().contains("PENDING"));
            assertTrue(history.get(0).getNewStateJson().contains("SUCCESS"));
        }

        @Test
        @DisplayName("1.3. User Pre-Processing Cancellation: PENDING -> CANCELLED")
        void testPendingToCancelledTrajectory() {
            Payment payment = createTestPayment(PaymentStatus.PENDING, "CANCEL1");

            Payment cancelled = paymentLifecycleService.transitionStatus(
                    payment.getId(), PaymentStatus.CANCELLED, "User closed checkout modal", testUser.getId());
            assertEquals(PaymentStatus.CANCELLED, cancelled.getStatus());

            // Verify terminal state: no further transitions permitted
            assertThrows(InvalidPaymentStateTransitionException.class, () ->
                    paymentLifecycleService.transitionStatus(payment.getId(), PaymentStatus.PROCESSING, "Retry", testUser.getId()));
            assertThrows(InvalidPaymentStateTransitionException.class, () ->
                    paymentLifecycleService.transitionStatus(payment.getId(), PaymentStatus.SUCCESS, "Late pay", testUser.getId()));

            Payment reloaded = paymentRepository.findById(payment.getId()).orElseThrow();
            assertEquals(PaymentStatus.CANCELLED, reloaded.getStatus());
        }

        @Test
        @DisplayName("1.4. In-Flight Cancellation: PENDING -> PROCESSING -> CANCELLED")
        void testProcessingToCancelledTrajectory() {
            Payment payment = createTestPayment(PaymentStatus.PENDING, "CANCEL2");

            paymentLifecycleService.transitionStatus(
                    payment.getId(), PaymentStatus.PROCESSING, "Clearing initiated", testUser.getId());
            Payment cancelled = paymentLifecycleService.transitionStatus(
                    payment.getId(), PaymentStatus.CANCELLED, "Gateway session expired by timeout", testUser.getId());
            assertEquals(PaymentStatus.CANCELLED, cancelled.getStatus());

            // Verify terminal state
            assertThrows(InvalidPaymentStateTransitionException.class, () ->
                    paymentLifecycleService.transitionStatus(payment.getId(), PaymentStatus.SUCCESS, "Late pay", testUser.getId()));
        }

        @Test
        @DisplayName("1.5. Processing Failure: PENDING -> PROCESSING -> FAILED")
        void testProcessingToFailedTrajectory() {
            Payment payment = createTestPayment(PaymentStatus.PENDING, "FAIL1");

            paymentLifecycleService.transitionStatus(
                    payment.getId(), PaymentStatus.PROCESSING, "Dispatch to bank", testUser.getId());
            Payment failed = paymentLifecycleService.transitionStatus(
                    payment.getId(), PaymentStatus.FAILED, "Card issuer declined: Insufficient funds", testUser.getId());
            assertEquals(PaymentStatus.FAILED, failed.getStatus());

            // Verify terminal state
            assertThrows(InvalidPaymentStateTransitionException.class, () ->
                    paymentLifecycleService.transitionStatus(payment.getId(), PaymentStatus.SUCCESS, "Revive", testUser.getId()));
            assertThrows(InvalidPaymentStateTransitionException.class, () ->
                    paymentLifecycleService.transitionStatus(payment.getId(), PaymentStatus.REFUNDED, "Refund unpaid", testUser.getId()));
        }

        @Test
        @DisplayName("1.6. Immediate Initiation Failure: PENDING -> FAILED")
        void testPendingToFailedTrajectory() {
            Payment payment = createTestPayment(PaymentStatus.PENDING, "FAIL2");

            Payment failed = paymentLifecycleService.transitionStatus(
                    payment.getId(), PaymentStatus.FAILED, "Invalid card number checksum", testUser.getId());
            assertEquals(PaymentStatus.FAILED, failed.getStatus());

            // Verify terminal state
            assertThrows(InvalidPaymentStateTransitionException.class, () ->
                    paymentLifecycleService.transitionStatus(payment.getId(), PaymentStatus.PROCESSING, "Retry", testUser.getId()));
        }
    }

    // =========================================================================
    // 2. REJECTING INVALID TRANSITIONS & PRESERVING CURRENT STATE
    // =========================================================================

    @Nested
    @DisplayName("2. Rejection of Invalid Transitions & State Protection")
    class InvalidTransitionRejectionTests {

        @Test
        @DisplayName("2.1. PENDING cannot transition directly to REFUNDED")
        void testPendingToRefundedRejected() {
            Payment payment = createTestPayment(PaymentStatus.PENDING, "INV-REF");

            InvalidPaymentStateTransitionException ex = assertThrows(
                    InvalidPaymentStateTransitionException.class,
                    () -> paymentLifecycleService.transitionStatus(payment.getId(), PaymentStatus.REFUNDED, "Illegal refund", testUser.getId())
            );
            assertEquals(PaymentStatus.PENDING, ex.getCurrentStatus());
            assertEquals(PaymentStatus.REFUNDED, ex.getTargetStatus());

            // Assert status remained PENDING and no audit log was created for transition
            Payment reloaded = paymentRepository.findById(payment.getId()).orElseThrow();
            assertEquals(PaymentStatus.PENDING, reloaded.getStatus());
            List<AuditLog> logs = auditLogRepository.findByEntityNameAndEntityIdOrderByIdDesc("Payment", payment.getId());
            assertTrue(logs.isEmpty());
        }

        @Test
        @DisplayName("2.2. SUCCESS cannot be CANCELLED (must use REFUNDED)")
        void testSuccessToCancelledRejected() {
            Payment payment = createTestPayment(PaymentStatus.SUCCESS, "INV-CANC");

            InvalidPaymentStateTransitionException ex = assertThrows(
                    InvalidPaymentStateTransitionException.class,
                    () -> paymentLifecycleService.transitionStatus(payment.getId(), PaymentStatus.CANCELLED, "Illegal cancel", testUser.getId())
            );
            assertEquals(PaymentStatus.SUCCESS, ex.getCurrentStatus());
            assertEquals(PaymentStatus.CANCELLED, ex.getTargetStatus());

            Payment reloaded = paymentRepository.findById(payment.getId()).orElseThrow();
            assertEquals(PaymentStatus.SUCCESS, reloaded.getStatus());
        }

        @Test
        @DisplayName("2.3. SUCCESS cannot transition backwards to PENDING, PROCESSING, or FAILED")
        void testSuccessBackwardsTransitionsRejected() {
            Payment payment = createTestPayment(PaymentStatus.SUCCESS, "INV-BACK");

            assertThrows(InvalidPaymentStateTransitionException.class, () ->
                    paymentLifecycleService.transitionStatus(payment.getId(), PaymentStatus.PENDING, "Back to pending", testUser.getId()));
            assertThrows(InvalidPaymentStateTransitionException.class, () ->
                    paymentLifecycleService.transitionStatus(payment.getId(), PaymentStatus.PROCESSING, "Back to proc", testUser.getId()));
            assertThrows(InvalidPaymentStateTransitionException.class, () ->
                    paymentLifecycleService.transitionStatus(payment.getId(), PaymentStatus.FAILED, "Back to fail", testUser.getId()));

            Payment reloaded = paymentRepository.findById(payment.getId()).orElseThrow();
            assertEquals(PaymentStatus.SUCCESS, reloaded.getStatus());
        }

        @Test
        @DisplayName("2.4. Redundant transition (PENDING -> PENDING or SUCCESS -> SUCCESS) is rejected")
        void testRedundantTransitionRejected() {
            Payment pendingPayment = createTestPayment(PaymentStatus.PENDING, "RED-P");
            assertThrows(InvalidPaymentStateTransitionException.class, () ->
                    paymentLifecycleService.transitionStatus(pendingPayment.getId(), PaymentStatus.PENDING, "Redundant", testUser.getId()));

            Payment successPayment = createTestPayment(PaymentStatus.SUCCESS, "RED-S");
            assertThrows(InvalidPaymentStateTransitionException.class, () ->
                    paymentLifecycleService.transitionStatus(successPayment.getId(), PaymentStatus.SUCCESS, "Redundant", testUser.getId()));
        }

        @Test
        @DisplayName("2.5. Terminal states (FAILED, REFUNDED, CANCELLED) reject transitions to any state")
        void testTerminalStatesRejectAllTransitions() {
            Payment failedPayment = createTestPayment(PaymentStatus.FAILED, "TERM-F");
            Payment refundedPayment = createTestPayment(PaymentStatus.REFUNDED, "TERM-R");
            Payment cancelledPayment = createTestPayment(PaymentStatus.CANCELLED, "TERM-C");

            for (PaymentStatus target : PaymentStatus.values()) {
                assertThrows(InvalidPaymentStateTransitionException.class, () ->
                        paymentLifecycleService.transitionStatus(failedPayment.getId(), target, "Attempt", testUser.getId()));
                assertThrows(InvalidPaymentStateTransitionException.class, () ->
                        paymentLifecycleService.transitionStatus(refundedPayment.getId(), target, "Attempt", testUser.getId()));
                assertThrows(InvalidPaymentStateTransitionException.class, () ->
                        paymentLifecycleService.transitionStatus(cancelledPayment.getId(), target, "Attempt", testUser.getId()));
            }
        }
    }

    // =========================================================================
    // 3. TRANSITIONS & HISTORY RETRIEVAL BY TRANSACTION REFERENCE
    // =========================================================================

    @Nested
    @DisplayName("3. Reference-based Operations & Provenance")
    class ReferenceAndHistoryTests {

        @Test
        @DisplayName("3.1. Transition payment by unique transaction reference")
        void testTransitionByReference() {
            Payment payment = createTestPayment(PaymentStatus.PENDING, "REF-TEST");
            String ref = payment.getTransactionReference();

            Payment updated = paymentLifecycleService.transitionStatusByReference(
                    ref, PaymentStatus.PROCESSING, "Transitioned by reference", testUser.getId());
            assertEquals(PaymentStatus.PROCESSING, updated.getStatus());

            List<PaymentAuditLogResponse> history = paymentLifecycleService.getPaymentHistoryByReference(ref, testUser.getId());
            assertEquals(1, history.size());
            assertEquals("PAYMENT_TRANSITION_PROCESSING", history.get(0).getAction());
            assertEquals(testUser.getId(), history.get(0).getActorId());
            assertEquals(testUser.getFullName(), history.get(0).getActorName());
            assertNotNull(history.get(0).getCreatedAt());
        }

        @Test
        @DisplayName("3.2. Unknown payment ID or reference throws ResourceNotFoundException")
        void testNotFoundHandling() {
            assertThrows(ResourceNotFoundException.class, () ->
                    paymentLifecycleService.transitionStatus(999999L, PaymentStatus.SUCCESS, "None", testUser.getId()));

            assertThrows(ResourceNotFoundException.class, () ->
                    paymentLifecycleService.transitionStatusByReference("TX-NON-EXISTENT", PaymentStatus.SUCCESS, "None", testUser.getId()));

            assertThrows(ResourceNotFoundException.class, () ->
                    paymentLifecycleService.getPaymentHistory(999999L, testUser.getId()));

            assertThrows(ResourceNotFoundException.class, () ->
                    paymentLifecycleService.getPaymentHistoryByReference("TX-NON-EXISTENT", testUser.getId()));
        }
    }
}
