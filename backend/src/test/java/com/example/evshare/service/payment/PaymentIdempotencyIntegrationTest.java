package com.example.evshare.service.payment;

import com.example.evshare.dto.request.InitiatePaymentRequest;
import com.example.evshare.dto.response.PaymentResponse;
import com.example.evshare.entity.*;
import com.example.evshare.entity.enums.PaymentMethod;
import com.example.evshare.entity.enums.PaymentStatus;
import com.example.evshare.entity.enums.RoleName;
import com.example.evshare.entity.enums.VehicleStatus;
import com.example.evshare.exception.IdempotencyConflictException;
import com.example.evshare.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@DisplayName("Checkpoint 06-M — Payment Idempotency & Concurrency Integration Tests")
class PaymentIdempotencyIntegrationTest {

    private static final AtomicLong PHONE_SEQ = new AtomicLong(System.currentTimeMillis() % 80000000L + 10000000L);

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private IdempotencyRecordRepository idempotencyRecordRepository;

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

    private User payer;
    private SharedFund fund;

    @BeforeEach
    void setUp() {
        Role roleCoOwner = roleRepository.findByName(RoleName.ROLE_CO_OWNER)
                .orElseGet(() -> roleRepository.save(new Role(null, RoleName.ROLE_CO_OWNER)));

        String uid = UUID.randomUUID().toString().substring(0, 8);
        payer = new User();
        payer.setEmail("idemp.payer." + uid + "@evshare.io");
        payer.setFullName("Tran Idempotent");
        payer.setPasswordHash("hashed_pass");
        payer.setPhoneNumber("09" + PHONE_SEQ.incrementAndGet());
        payer.setIsActive(true);
        payer.setRoles(Set.of(roleCoOwner));
        payer = userRepository.saveAndFlush(payer);

        Vehicle vehicle = new Vehicle();
        vehicle.setVin("VINIDEMP" + uid.toUpperCase() + "000000");
        vehicle.setLicensePlate("51H-" + uid.toUpperCase());
        vehicle.setModelName("VinFast VF9 Plus");
        vehicle.setManufacturer("VinFast");
        vehicle.setModel3dAssetPath("models/vehicles/vf9.glb");
        vehicle.setStatus(VehicleStatus.AVAILABLE);
        vehicle = vehicleRepository.saveAndFlush(vehicle);

        OwnershipGroup group = new OwnershipGroup();
        group.setGroupName("Syndicate " + uid);
        group.setVehicle(vehicle);
        group.setFormationDate(LocalDate.now());
        group.setIsActive(true);
        group = ownershipGroupRepository.saveAndFlush(group);

        fund = new SharedFund();
        fund.setGroup(group);
        fund.setCurrentBalance(new BigDecimal("20000000.00"));
        fund.setMinimumReserveThreshold(new BigDecimal("10000000.00"));
        fund.setCurrency("VND");
        fund = sharedFundRepository.saveAndFlush(fund);
    }

    private InitiatePaymentRequest buildRequest(BigDecimal amount, String key) {
        return new InitiatePaymentRequest(
                payer.getId(),
                fund.getId(),
                null,
                amount,
                PaymentMethod.MOCK,
                "Syndicate Fund Contribution",
                key,
                Map.of("simulationMode", "SUCCESS", "autoConfirm", true)
        );
    }

    // =========================================================================
    // 1. REPEATED REQUESTS (DEDUPLICATION & EXACT RESULT PRESERVATION)
    // =========================================================================

    @Nested
    @DisplayName("1. Repeated Request Deduplication Tests")
    class RepeatedRequestTests {

        @Test
        @DisplayName("1.1. Repeated request does not create duplicate payment and returns exact same result")
        void testRepeatedRequestDeduplication() {
            String key = "IDEMP-REPEAT-" + UUID.randomUUID();
            InitiatePaymentRequest request = buildRequest(new BigDecimal("2500000.00"), key);

            long initialPaymentCount = paymentRepository.count();

            // First call -> creates payment
            PaymentResponse response1 = paymentService.initiatePayment(request, key);
            assertNotNull(response1);
            assertNotNull(response1.getId());
            assertNotNull(response1.getTransactionReference());

            long afterFirstCount = paymentRepository.count();
            assertEquals(initialPaymentCount + 1, afterFirstCount, "First call must create exactly one payment");

            // Repeated call with SAME key + SAME request
            PaymentResponse response2 = paymentService.initiatePayment(request, key);
            assertNotNull(response2);

            long afterSecondCount = paymentRepository.count();
            assertEquals(afterFirstCount, afterSecondCount, "Repeated call must NOT create a duplicate payment");

            // Same key + same request returns same result
            assertEquals(response1.getId(), response2.getId(), "Returned payment ID must match");
            assertEquals(response1.getTransactionReference(), response2.getTransactionReference(), "Transaction reference must match");
            assertEquals(response1.getAmount(), response2.getAmount(), "Amount must match");
            assertEquals(response1.getStatus(), response2.getStatus(), "Status must match");
        }

        @Test
        @DisplayName("1.2. Idempotency record is persisted with request hash and response body")
        void testIdempotencyRecordPersistence() {
            String key = "IDEMP-PERSIST-" + UUID.randomUUID();
            InitiatePaymentRequest request = buildRequest(new BigDecimal("1500000.00"), key);

            PaymentResponse response = paymentService.initiatePayment(request, key);

            var recordOpt = idempotencyRecordRepository.findByIdempotencyKey(key);
            assertTrue(recordOpt.isPresent(), "Idempotency record must be persisted in database");

            IdempotencyRecord record = recordOpt.get();
            assertEquals(key, record.getIdempotencyKey());
            assertEquals("PAYMENT_INITIATION", record.getOperation());
            assertNotNull(record.getRequestHash());
            assertEquals(64, record.getRequestHash().length());
            assertNotNull(record.getResponseBody());
            assertTrue(record.getResponseBody().contains(response.getTransactionReference()));
        }
    }

    // =========================================================================
    // 2. SAME KEY + DIFFERENT REQUEST REJECTION
    // =========================================================================

    @Nested
    @DisplayName("2. Mismatched Payload Rejection Tests")
    class MismatchedPayloadTests {

        @Test
        @DisplayName("2.1. Same key + different amount must be rejected with IdempotencyConflictException")
        void testDifferentAmountRejected() {
            String key = "IDEMP-MISMATCH-AMT-" + UUID.randomUUID();
            InitiatePaymentRequest req1 = buildRequest(new BigDecimal("1000000.00"), key);
            InitiatePaymentRequest req2 = buildRequest(new BigDecimal("2000000.00"), key); // Altered amount

            paymentService.initiatePayment(req1, key);
            long countAfterFirst = paymentRepository.count();

            IdempotencyConflictException ex = assertThrows(
                    IdempotencyConflictException.class,
                    () -> paymentService.initiatePayment(req2, key)
            );

            assertEquals(key, ex.getIdempotencyKey());
            assertTrue(ex.getMessage().contains("was already used with a different request payload"));
            assertEquals(countAfterFirst, paymentRepository.count(), "Conflicting request must not alter payment count");
        }

        @Test
        @DisplayName("2.2. Same key + different payment method must be rejected with IdempotencyConflictException")
        void testDifferentMethodRejected() {
            String key = "IDEMP-MISMATCH-MTH-" + UUID.randomUUID();
            InitiatePaymentRequest req1 = buildRequest(new BigDecimal("1000000.00"), key);
            InitiatePaymentRequest req2 = new InitiatePaymentRequest(
                    payer.getId(), fund.getId(), null, new BigDecimal("1000000.00"),
                    PaymentMethod.BANK_TRANSFER, // Different method
                    "Contribution", key, null
            );

            paymentService.initiatePayment(req1, key);

            assertThrows(IdempotencyConflictException.class, () ->
                    paymentService.initiatePayment(req2, key));
        }
    }

    // =========================================================================
    // 3. TRANSACTION-SAFE CONCURRENT REQUESTS
    // =========================================================================

    @Nested
    @DisplayName("3. Multi-Threaded Concurrency Tests")
    class ConcurrencyTests {

        @Test
        @DisplayName("3.1. 10 concurrent threads submitting the exact same request produce exactly 1 payment and uniform responses")
        void testConcurrentRequestsProduceSinglePayment() throws Exception {
            int threadCount = 10;
            String key = "IDEMP-CONCUR-" + UUID.randomUUID();
            InitiatePaymentRequest request = buildRequest(new BigDecimal("3000000.00"), key);

            long beforeCount = paymentRepository.count();

            ExecutorService executor = Executors.newFixedThreadPool(threadCount);
            CountDownLatch startLatch = new CountDownLatch(1);
            CountDownLatch doneLatch = new CountDownLatch(threadCount);

            List<Future<PaymentResponse>> futures = new ArrayList<>();
            AtomicInteger successCount = new AtomicInteger(0);
            AtomicInteger conflictCount = new AtomicInteger(0);

            for (int i = 0; i < threadCount; i++) {
                futures.add(executor.submit(() -> {
                    try {
                        startLatch.await();
                        PaymentResponse res = paymentService.initiatePayment(request, key);
                        successCount.incrementAndGet();
                        return res;
                    } finally {
                        doneLatch.countDown();
                    }
                }));
            }

            // Release all threads simultaneously
            startLatch.countDown();
            assertTrue(doneLatch.await(15, TimeUnit.SECONDS), "Concurrent executions must complete within timeout");
            executor.shutdown();

            List<PaymentResponse> results = new ArrayList<>();
            for (Future<PaymentResponse> future : futures) {
                try {
                    PaymentResponse res = future.get();
                    if (res != null) {
                        results.add(res);
                    }
                } catch (ExecutionException e) {
                    if (e.getCause() instanceof IdempotencyConflictException) {
                        conflictCount.incrementAndGet();
                    }
                }
            }

            // Exactly ONE payment must be created in the database
            long afterCount = paymentRepository.count();
            assertEquals(beforeCount + 1, afterCount, "Exactly ONE payment must be created in DB under concurrency");

            // All successful responses must refer to the EXACT same payment
            assertFalse(results.isEmpty(), "At least one thread must successfully receive the payment response");
            Long expectedPaymentId = results.get(0).getId();
            String expectedReference = results.get(0).getTransactionReference();

            for (PaymentResponse res : results) {
                assertEquals(expectedPaymentId, res.getId(), "Every thread must receive identical payment ID");
                assertEquals(expectedReference, res.getTransactionReference(), "Every thread must receive identical reference");
                assertEquals(new BigDecimal("3000000.00"), res.getAmount(), "Amount must match");
            }
        }
    }

    // =========================================================================
    // 4. DISTINCT KEYS CREATE DISTINCT PAYMENTS
    // =========================================================================

    @Nested
    @DisplayName("4. Distinct Key Independence Tests")
    class DistinctKeysTests {

        @Test
        @DisplayName("4.1. Distinct idempotency keys create separate independent payments")
        void testDistinctKeysCreateDistinctPayments() {
            String keyA = "IDEMP-KEY-A-" + UUID.randomUUID();
            String keyB = "IDEMP-KEY-B-" + UUID.randomUUID();

            InitiatePaymentRequest reqA = buildRequest(new BigDecimal("1000000.00"), keyA);
            InitiatePaymentRequest reqB = buildRequest(new BigDecimal("1000000.00"), keyB);

            long beforeCount = paymentRepository.count();

            PaymentResponse resA = paymentService.initiatePayment(reqA, keyA);
            PaymentResponse resB = paymentService.initiatePayment(reqB, keyB);

            long afterCount = paymentRepository.count();
            assertEquals(beforeCount + 2, afterCount, "Distinct keys must create two distinct payments");

            assertNotEquals(resA.getId(), resB.getId(), "Payment IDs must differ");
            assertNotEquals(resA.getTransactionReference(), resB.getTransactionReference(), "References must differ");
        }
    }
}
