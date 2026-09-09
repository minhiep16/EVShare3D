package com.example.evshare.service.payment;

import com.example.evshare.dto.response.PaymentResponse;
import com.example.evshare.entity.IdempotencyRecord;
import com.example.evshare.entity.enums.IdempotencyStatus;
import com.example.evshare.entity.enums.PaymentMethod;
import com.example.evshare.entity.enums.PaymentStatus;
import com.example.evshare.exception.IdempotencyConflictException;
import com.example.evshare.exception.IdempotentConcurrentExecutionException;
import com.example.evshare.repository.IdempotencyRecordRepository;
import com.example.evshare.repository.PaymentRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Checkpoint 06-M — Idempotency Service Unit Tests")
class IdempotencyServiceTest {

    @Mock
    private IdempotencyRecordRepository idempotencyRecordRepository;

    @Mock
    private PaymentRepository paymentRepository;

    private ObjectMapper objectMapper;
    private IdempotencyServiceImpl idempotencyService;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper().findAndRegisterModules();
        idempotencyService = new IdempotencyServiceImpl(
                idempotencyRecordRepository,
                paymentRepository,
                objectMapper
        );
    }

    // =========================================================================
    // 1. REQUEST HASHING DETERMINISM
    // =========================================================================

    @Nested
    @DisplayName("1. Request Hashing Determinism Tests")
    class RequestHashingTests {

        @Test
        @DisplayName("1.1. Same payload produces identical SHA-256 hash across repeated invocations")
        void testHashDeterminism() {
            Map<String, Object> payload1 = Map.of("amount", 2500000, "fundId", 10, "userId", 5);
            Map<String, Object> payload2 = Map.of("amount", 2500000, "fundId", 10, "userId", 5);

            String hash1 = idempotencyService.computeRequestHash(payload1);
            String hash2 = idempotencyService.computeRequestHash(payload2);

            assertNotNull(hash1);
            assertEquals(64, hash1.length(), "SHA-256 hex string should be 64 characters");
            assertEquals(hash1, hash2, "Identical payloads must generate identical hashes");
        }

        @Test
        @DisplayName("1.2. Different payloads produce distinct SHA-256 hashes")
        void testDistinctHashesForDifferentPayloads() {
            Map<String, Object> payload1 = Map.of("amount", 2500000, "fundId", 10);
            Map<String, Object> payload2 = Map.of("amount", 5000000, "fundId", 10);

            String hash1 = idempotencyService.computeRequestHash(payload1);
            String hash2 = idempotencyService.computeRequestHash(payload2);

            assertNotEquals(hash1, hash2, "Different amounts must generate distinct hashes");
        }

        @Test
        @DisplayName("1.3. Null payload produces deterministic empty hash")
        void testNullPayloadHash() {
            String hash = idempotencyService.computeRequestHash(null);
            assertEquals("e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855", hash);
        }
    }

    // =========================================================================
    // 2. IDEMPOTENCY EXECUTION & CACHE RETRIEVAL
    // =========================================================================

    @Nested
    @DisplayName("2. Idempotent Execution & Deduplication Tests")
    class IdempotentExecutionTests {

        @Test
        @DisplayName("2.1. First invocation executes business logic, saves record, and returns result")
        void testFirstInvocationExecutesLogic() {
            String key = "IDEMP-KEY-001";
            Map<String, Object> payload = Map.of("amount", 1000000);
            PaymentResponse expected = new PaymentResponse(1L, "TX-1", 10L, 5L, new BigDecimal("1000000"),
                    PaymentMethod.BANK_TRANSFER, PaymentStatus.PENDING, null, null, null, null, Instant.now());

            when(idempotencyRecordRepository.findByIdempotencyKey(key)).thenReturn(Optional.empty());
            when(idempotencyRecordRepository.saveAndFlush(any(IdempotencyRecord.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            AtomicInteger executionCount = new AtomicInteger(0);
            PaymentResponse actual = idempotencyService.executeIdempotent(
                    key, "PAYMENT_INITIATE", payload, PaymentResponse.class, () -> {
                        executionCount.incrementAndGet();
                        return expected;
                    });

            assertEquals(1, executionCount.get(), "Business logic must be invoked once");
            assertEquals(expected.getTransactionReference(), actual.getTransactionReference());
            verify(idempotencyRecordRepository, times(2)).saveAndFlush(any(IdempotencyRecord.class));
        }

        @Test
        @DisplayName("2.2. Same key + same request returns cached result without executing business logic")
        void testSameKeySameRequestReturnsCachedResult() throws Exception {
            String key = "IDEMP-KEY-CACHED";
            Map<String, Object> payload = Map.of("amount", 2000000, "fundId", 5);
            String hash = idempotencyService.computeRequestHash(payload);

            PaymentResponse cachedResponse = new PaymentResponse(99L, "TX-CACHED-99", 5L, 2L,
                    new BigDecimal("2000000"), PaymentMethod.MOCK, PaymentStatus.SUCCESS,
                    "https://sandbox.evshare.io/pay", null, "Instructions", null, Instant.now());
            String responseJson = objectMapper.writeValueAsString(cachedResponse);

            IdempotencyRecord existingRecord = new IdempotencyRecord(key, "PAYMENT_INITIATE", hash, IdempotencyStatus.COMPLETED, Instant.now().plusSeconds(3600));
            existingRecord.setResponseBody(responseJson);

            when(idempotencyRecordRepository.findByIdempotencyKey(key)).thenReturn(Optional.of(existingRecord));

            AtomicInteger executionCount = new AtomicInteger(0);
            PaymentResponse result = idempotencyService.executeIdempotent(
                    key, "PAYMENT_INITIATE", payload, PaymentResponse.class, () -> {
                        executionCount.incrementAndGet();
                        return new PaymentResponse();
                    });

            assertEquals(0, executionCount.get(), "Business logic must NOT be executed on cache hit");
            assertNotNull(result);
            assertEquals(99L, result.getId());
            assertEquals("TX-CACHED-99", result.getTransactionReference());
            assertEquals(PaymentStatus.SUCCESS, result.getStatus());
        }

        @Test
        @DisplayName("2.3. Same key + different request must be rejected with IdempotencyConflictException")
        void testSameKeyDifferentRequestThrowsConflict() {
            String key = "IDEMP-KEY-CONFLICT";
            Map<String, Object> originalPayload = Map.of("amount", 1000000, "fundId", 5);
            Map<String, Object> alteredPayload = Map.of("amount", 9999999, "fundId", 5); // Tampered amount!

            String originalHash = idempotencyService.computeRequestHash(originalPayload);

            IdempotencyRecord existingRecord = new IdempotencyRecord(
                    key, "PAYMENT_INITIATE", originalHash, IdempotencyStatus.COMPLETED, Instant.now().plusSeconds(3600));
            existingRecord.setResponseBody("{}");

            when(idempotencyRecordRepository.findByIdempotencyKey(key)).thenReturn(Optional.of(existingRecord));

            IdempotencyConflictException ex = assertThrows(IdempotencyConflictException.class, () ->
                    idempotencyService.executeIdempotent(key, "PAYMENT_INITIATE", alteredPayload, PaymentResponse.class, () -> {
                        fail("Business logic must not be reached on conflict");
                        return null;
                    })
            );

            assertEquals(key, ex.getIdempotencyKey());
            assertTrue(ex.getMessage().contains("was already used with a different request payload"));
        }

        @Test
        @DisplayName("2.4. Null or blank idempotency key bypasses idempotency tracking")
        void testNullKeyBypassesTracking() {
            AtomicInteger count = new AtomicInteger(0);
            String result = idempotencyService.executeIdempotent(
                    null, "NO_KEY_OP", "payload", String.class, () -> {
                        count.incrementAndGet();
                        return "DirectResult";
                    });

            assertEquals("DirectResult", result);
            assertEquals(1, count.get());
            verifyNoInteractions(idempotencyRecordRepository);
        }
    }

    // =========================================================================
    // 3. CONCURRENCY & FAILURE HANDLING
    // =========================================================================

    @Nested
    @DisplayName("3. Concurrency & Failure States")
    class ConcurrencyAndFailureTests {

        @Test
        @DisplayName("3.1. In-flight processing with mismatched payload throws IdempotencyConflictException")
        void testInFlightMismatchedPayloadThrowsConflict() {
            String key = "IDEMP-PROCESSING-MISMATCH";
            Map<String, Object> orig = Map.of("amount", 100);
            Map<String, Object> diff = Map.of("amount", 200);

            IdempotencyRecord record = new IdempotencyRecord(key, "OP", idempotencyService.computeRequestHash(orig), IdempotencyStatus.PROCESSING, Instant.now().plusSeconds(3600));
            when(idempotencyRecordRepository.findByIdempotencyKey(key)).thenReturn(Optional.of(record));

            assertThrows(IdempotencyConflictException.class, () ->
                    idempotencyService.executeIdempotent(key, "OP", diff, String.class, () -> "res"));
        }

        @Test
        @DisplayName("3.2. Failed execution marks record status as FAILED and propagates exception")
        void testFailedExecutionUpdatesRecordStatus() {
            String key = "IDEMP-FAIL-OP";
            Map<String, Object> payload = Map.of("data", "fail");

            when(idempotencyRecordRepository.findByIdempotencyKey(key)).thenReturn(Optional.empty());
            when(idempotencyRecordRepository.saveAndFlush(any(IdempotencyRecord.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
                    idempotencyService.executeIdempotent(key, "OP", payload, String.class, () -> {
                        throw new IllegalStateException("Simulated backend failure");
                    }));

            assertEquals("Simulated backend failure", ex.getMessage());
            verify(idempotencyRecordRepository, times(2)).saveAndFlush(any(IdempotencyRecord.class));
        }
    }
}
