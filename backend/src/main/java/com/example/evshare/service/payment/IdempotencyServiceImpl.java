package com.example.evshare.service.payment;

import com.example.evshare.dto.response.PaymentResponse;
import com.example.evshare.entity.IdempotencyRecord;
import com.example.evshare.entity.Payment;
import com.example.evshare.entity.enums.IdempotencyStatus;
import com.example.evshare.exception.IdempotencyConflictException;
import com.example.evshare.exception.IdempotentConcurrentExecutionException;
import com.example.evshare.repository.IdempotencyRecordRepository;
import com.example.evshare.repository.PaymentRepository;
import com.example.evshare.service.payment.model.PaymentInitiationResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.function.Supplier;

@Service
public class IdempotencyServiceImpl implements IdempotencyService {

    private static final Logger log = LoggerFactory.getLogger(IdempotencyServiceImpl.class);
    private static final Duration DEFAULT_TTL = Duration.ofHours(24);

    private final IdempotencyRecordRepository idempotencyRecordRepository;
    private final PaymentRepository paymentRepository;
    private final ObjectMapper objectMapper;

    public IdempotencyServiceImpl(
            IdempotencyRecordRepository idempotencyRecordRepository,
            PaymentRepository paymentRepository,
            ObjectMapper objectMapper) {
        this.idempotencyRecordRepository = idempotencyRecordRepository;
        this.paymentRepository = paymentRepository;
        this.objectMapper = objectMapper != null
                ? objectMapper.copy().findAndRegisterModules()
                : new ObjectMapper().findAndRegisterModules();
    }

    @Override
    public String computeRequestHash(Object requestPayload) {
        if (requestPayload == null) {
            return "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855"; // SHA-256 of empty string
        }
        try {
            byte[] bytes = objectMapper.writeValueAsString(requestPayload).getBytes(StandardCharsets.UTF_8);
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(bytes);
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) {
            log.error("Failed to compute request hash", e);
            throw new RuntimeException("Failed to compute request hash", e);
        }
    }

    @Override
    public <T> T executeIdempotent(String idempotencyKey, String operation, Object requestPayload, Class<T> responseType, Supplier<T> businessLogic) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            return businessLogic.get();
        }

        String requestHash = computeRequestHash(requestPayload);

        // 1. Check existing record
        Optional<IdempotencyRecord> existingOpt = idempotencyRecordRepository.findByIdempotencyKey(idempotencyKey);
        if (existingOpt.isPresent()) {
            IdempotencyRecord existing = existingOpt.get();

            // Check payload match
            if (!existing.getRequestHash().equals(requestHash)) {
                throw new IdempotencyConflictException(idempotencyKey);
            }

            // If already completed, return cached response
            if (existing.getStatus() == IdempotencyStatus.COMPLETED) {
                log.info("Idempotency cache hit for key: [{}], operation: [{}]", idempotencyKey, operation);
                return deserialize(existing.getResponseBody(), responseType);
            }

            // If processing, wait briefly or throw concurrency exception
            if (existing.getStatus() == IdempotencyStatus.PROCESSING) {
                T result = waitForCompletion(idempotencyKey, requestHash, responseType);
                if (result != null) {
                    return result;
                }
                throw new IdempotentConcurrentExecutionException(idempotencyKey);
            }
        }

        // 2. Try to insert record with status PROCESSING
        IdempotencyRecord record;
        try {
            record = new IdempotencyRecord(idempotencyKey, operation, requestHash, IdempotencyStatus.PROCESSING, Instant.now().plus(DEFAULT_TTL));
            record = idempotencyRecordRepository.saveAndFlush(record);
        } catch (DataIntegrityViolationException ex) {
            // Concurrent race condition: another thread inserted first
            log.info("Concurrent insert detected for idempotency key: [{}]", idempotencyKey);
            Optional<IdempotencyRecord> raceRecordOpt = idempotencyRecordRepository.findByIdempotencyKey(idempotencyKey);
            if (raceRecordOpt.isPresent()) {
                IdempotencyRecord raceRecord = raceRecordOpt.get();
                if (!raceRecord.getRequestHash().equals(requestHash)) {
                    throw new IdempotencyConflictException(idempotencyKey);
                }
                if (raceRecord.getStatus() == IdempotencyStatus.COMPLETED) {
                    return deserialize(raceRecord.getResponseBody(), responseType);
                }
                T result = waitForCompletion(idempotencyKey, requestHash, responseType);
                if (result != null) {
                    return result;
                }
            }
            throw new IdempotentConcurrentExecutionException(idempotencyKey);
        }

        // 3. Execute business logic
        T result;
        try {
            result = businessLogic.get();
        } catch (Exception e) {
            record.setStatus(IdempotencyStatus.FAILED);
            idempotencyRecordRepository.saveAndFlush(record);
            throw e;
        }

        // 4. Save cached result and mark COMPLETED
        try {
            record.setStatus(IdempotencyStatus.COMPLETED);
            record.setResponseBody(objectMapper.writeValueAsString(result));

            // Link payment if applicable
            Long paymentId = extractPaymentId(result);
            if (paymentId != null) {
                paymentRepository.findById(paymentId).ifPresent(record::setPayment);
            }

            idempotencyRecordRepository.saveAndFlush(record);
        } catch (Exception e) {
            log.error("Failed to serialize or save idempotency response for key: [{}]", idempotencyKey, e);
        }

        return result;
    }

    private <T> T waitForCompletion(String idempotencyKey, String requestHash, Class<T> responseType) {
        // Poll for up to 3 seconds with backoff
        int maxAttempts = 30;
        for (int i = 0; i < maxAttempts; i++) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return null;
            }
            Optional<IdempotencyRecord> polled = idempotencyRecordRepository.findByIdempotencyKey(idempotencyKey);
            if (polled.isPresent()) {
                IdempotencyRecord rec = polled.get();
                if (!rec.getRequestHash().equals(requestHash)) {
                    throw new IdempotencyConflictException(idempotencyKey);
                }
                if (rec.getStatus() == IdempotencyStatus.COMPLETED) {
                    return deserialize(rec.getResponseBody(), responseType);
                }
                if (rec.getStatus() == IdempotencyStatus.FAILED) {
                    return null;
                }
            }
        }
        return null;
    }

    private <T> T deserialize(String json, Class<T> responseType) {
        if (json == null) {
            return null;
        }
        try {
            return objectMapper.readValue(json, responseType);
        } catch (Exception e) {
            log.error("Failed to deserialize idempotency response body", e);
            throw new RuntimeException("Failed to deserialize cached idempotency response", e);
        }
    }

    private Long extractPaymentId(Object result) {
        if (result instanceof PaymentResponse pr) {
            return pr.getId();
        }
        if (result instanceof Payment p) {
            return p.getId();
        }
        return null;
    }
}
