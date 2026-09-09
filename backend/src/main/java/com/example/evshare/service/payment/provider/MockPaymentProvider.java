package com.example.evshare.service.payment.provider;

import com.example.evshare.entity.enums.PaymentMethod;
import com.example.evshare.entity.enums.PaymentProviderType;
import com.example.evshare.entity.enums.PaymentStatus;
import com.example.evshare.service.payment.PaymentProvider;
import com.example.evshare.service.payment.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Conceptual Mock Payment Provider strictly designed for development, testing, and sandbox simulation.
 * <p>
 * GUARANTEES:
 * - Operates purely offline without any external network calls or real banking credentials.
 * - Prominently labeled as DEVELOPMENT/TEST ONLY on every request, response, and audit log.
 * - Explicitly rejects any fake production claims.
 * - Deterministically simulates:
 *   1. Success: Instant or verified payment completion (COMPLETED).
 *   2. Failure: Configurable decline scenarios (FAILED, e.g. INSUFFICIENT_FUNDS, CARD_DECLINED, CANCELLED).
 *   3. Processing: Asynchronous clearing in-progress state (PENDING).
 *   4. Refund: Full or partial refund reversals as well as refund rejection scenarios.
 */
@Component
public class MockPaymentProvider implements PaymentProvider {

    private static final Logger log = LoggerFactory.getLogger(MockPaymentProvider.class);

    public static final String DEVELOPMENT_DISCLAIMER =
            "DEVELOPMENT/TEST PAYMENT SIMULATION ONLY - NO REAL FINANCIAL TRANSACTION TOOK PLACE - NOT A REAL PAYMENT";
    public static final String SIMULATION_ENVIRONMENT = "SANDBOX_TEST";
    public static final boolean IS_DEVELOPMENT_OR_TEST = true;

    @Override
    public PaymentProviderType getProviderType() {
        return PaymentProviderType.MOCK;
    }

    @Override
    public boolean supports(PaymentMethod paymentMethod) {
        return PaymentMethod.MOCK.equals(paymentMethod);
    }

    @Override
    public PaymentInitiationResult initiate(PaymentInitiationCommand command) {
        log.info("[TEST-ONLY-SIMULATION] Initiating test payment for reference: {}, amount: {}",
                command.getTransactionReference(), command.getAmount());

        MockSimulationMode mode = resolveSimulationMode(
                command.getMetadata() != null ? command.getMetadata().get("simulationMode") : null,
                command.getTransactionReference()
        );

        String externalTxId = "MOCK-TX-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        String paymentUrl = "https://sandbox.evshare.io/mock-pay?ref=" + command.getTransactionReference() + "&env=test";
        String qrData = "mock://pay?ref=" + command.getTransactionReference() + "&amt=" + command.getAmount() + "&env=test";

        boolean autoConfirm = Boolean.TRUE.equals(
                command.getMetadata() != null ? command.getMetadata().get("autoConfirm") : null
        );

        PaymentStatus initialStatus;
        String instructions;

        switch (mode) {
            case FAILURE -> {
                initialStatus = PaymentStatus.FAILED;
                String failureReason = getFailureReason(command.getMetadata());
                instructions = String.format("[TEST/SANDBOX ONLY - NOT A REAL PAYMENT] Payment initiation simulated failure: %s", failureReason);
            }
            case PROCESSING -> {
                initialStatus = PaymentStatus.PENDING;
                instructions = "[TEST/SANDBOX ONLY - NOT A REAL PAYMENT] Payment processing in progress (simulated asynchronous clearing).";
            }
            case SUCCESS -> {
                initialStatus = autoConfirm ? PaymentStatus.COMPLETED : PaymentStatus.PENDING;
                instructions = autoConfirm
                        ? "[TEST/SANDBOX ONLY - NOT A REAL PAYMENT] Mock payment auto-confirmed successfully."
                        : "[TEST/SANDBOX ONLY - NOT A REAL PAYMENT] Mock sandbox checkout: visit paymentUrl or scan QR to simulate settlement.";
            }
            default -> {
                initialStatus = PaymentStatus.PENDING;
                instructions = "[TEST/SANDBOX ONLY - NOT A REAL PAYMENT] Mock sandbox checkout ready.";
            }
        }

        Map<String, Object> data = new LinkedHashMap<>();
        if (command.getMetadata() != null) {
            data.putAll(command.getMetadata());
        }
        data.put("isDevelopmentOrTest", IS_DEVELOPMENT_OR_TEST);
        data.put("simulationEnvironment", SIMULATION_ENVIRONMENT);
        data.put("disclaimer", DEVELOPMENT_DISCLAIMER);
        data.put("simulationMode", mode.name());
        data.put("authCode", "AUTH-TEST-" + UUID.randomUUID().toString().substring(0, 6).toUpperCase());

        return PaymentInitiationResult.builder()
                .transactionReference(command.getTransactionReference())
                .externalTransactionId(externalTxId)
                .providerType(PaymentProviderType.MOCK)
                .status(initialStatus)
                .amount(command.getAmount())
                .paymentUrl(paymentUrl)
                .qrCodeData(qrData)
                .instructions(instructions)
                .expiresAt(Instant.now().plus(15, ChronoUnit.MINUTES))
                .providerData(data)
                .build();
    }

    @Override
    public PaymentVerificationResult verify(PaymentVerificationCommand command) {
        log.info("[TEST-ONLY-SIMULATION] Verifying test transaction reference: {}", command.getTransactionReference());

        Object modeObj = command.getPayload() != null ? command.getPayload().get("simulationMode") : null;
        Object statusObj = command.getPayload() != null ? command.getPayload().get("simulatedStatus") : null;
        MockSimulationMode mode = resolveSimulationMode(modeObj != null ? modeObj : statusObj, command.getTransactionReference());

        PaymentStatus status;
        boolean isVerified;
        String message;

        switch (mode) {
            case SUCCESS -> {
                status = PaymentStatus.COMPLETED;
                isVerified = true;
                message = "[TEST/SANDBOX ONLY - NOT A REAL PAYMENT] Mock payment verified and settled successfully.";
            }
            case FAILURE -> {
                status = PaymentStatus.FAILED;
                isVerified = false;
                String failureReason = getFailureReason(command.getPayload());
                message = String.format("[TEST/SANDBOX ONLY - NOT A REAL PAYMENT] Mock payment verification failed: %s", failureReason);
            }
            case PROCESSING -> {
                status = PaymentStatus.PENDING;
                isVerified = false;
                message = "[TEST/SANDBOX ONLY - NOT A REAL PAYMENT] Payment processing in progress (simulated asynchronous clearance).";
            }
            default -> {
                status = mapStatus(String.valueOf(statusObj));
                isVerified = PaymentStatus.COMPLETED.equals(status);
                message = isVerified
                        ? "[TEST/SANDBOX ONLY - NOT A REAL PAYMENT] Mock payment verified."
                        : "[TEST/SANDBOX ONLY - NOT A REAL PAYMENT] Mock payment status: " + status;
            }
        }

        Map<String, Object> rawResponse = new LinkedHashMap<>();
        if (command.getPayload() != null) {
            rawResponse.putAll(command.getPayload());
        }
        rawResponse.put("isDevelopmentOrTest", IS_DEVELOPMENT_OR_TEST);
        rawResponse.put("simulationEnvironment", SIMULATION_ENVIRONMENT);
        rawResponse.put("disclaimer", DEVELOPMENT_DISCLAIMER);
        rawResponse.put("verified", isVerified);
        rawResponse.put("simulationMode", mode.name());

        return PaymentVerificationResult.builder()
                .transactionReference(command.getTransactionReference())
                .externalTransactionId(command.getExternalTransactionId() != null
                        ? command.getExternalTransactionId()
                        : "MOCK-VERIFIED-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase())
                .providerType(PaymentProviderType.MOCK)
                .status(status)
                .amount(command.getExpectedAmount())
                .verified(isVerified)
                .providerMessage(message)
                .completedAt(isVerified ? Instant.now() : null)
                .rawResponse(rawResponse)
                .build();
    }

    @Override
    public PaymentRefundResult refund(PaymentRefundCommand command) {
        log.info("[TEST-ONLY-SIMULATION] Processing test refund for reference: {}, amount: {}",
                command.getTransactionReference(), command.getAmount());

        // Support simulated refund rejection
        boolean simulateFailure = command.getMetadata() != null &&
                Boolean.TRUE.equals(command.getMetadata().get("simulateRefundFailure"));

        if (simulateFailure || (command.getAmount() != null && command.getAmount().compareTo(BigDecimal.ZERO) <= 0)) {
            String failReason = command.getMetadata() != null && command.getMetadata().get("failureReason") != null
                    ? String.valueOf(command.getMetadata().get("failureReason"))
                    : "Simulated refund rejection (invalid refund amount or dispute window elapsed)";

            return PaymentRefundResult.builder()
                    .transactionReference(command.getTransactionReference())
                    .refundReference(null)
                    .externalRefundId(null)
                    .providerType(PaymentProviderType.MOCK)
                    .status(PaymentStatus.FAILED)
                    .amountRefunded(BigDecimal.ZERO)
                    .successful(false)
                    .providerMessage("[TEST/SANDBOX ONLY - NOT A REAL PAYMENT] " + failReason)
                    .refundedAt(Instant.now())
                    .rawResponse(Map.of(
                            "isDevelopmentOrTest", IS_DEVELOPMENT_OR_TEST,
                            "disclaimer", DEVELOPMENT_DISCLAIMER,
                            "successful", false,
                            "error", failReason
                    ))
                    .build();
        }

        String refundRef = "MOCK-REF-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        return PaymentRefundResult.builder()
                .transactionReference(command.getTransactionReference())
                .refundReference(refundRef)
                .externalRefundId("MOCK-EXT-REF-" + UUID.randomUUID().toString().substring(0, 6).toUpperCase())
                .providerType(PaymentProviderType.MOCK)
                .status(PaymentStatus.REFUNDED)
                .amountRefunded(command.getAmount())
                .successful(true)
                .providerMessage("[TEST/SANDBOX ONLY - NOT A REAL PAYMENT] Mock refund executed successfully: " + command.getReason())
                .refundedAt(Instant.now())
                .rawResponse(Map.of(
                        "isDevelopmentOrTest", IS_DEVELOPMENT_OR_TEST,
                        "disclaimer", DEVELOPMENT_DISCLAIMER,
                        "refundReference", refundRef,
                        "reason", command.getReason() != null ? command.getReason() : "",
                        "successful", true
                ))
                .build();
    }

    @Override
    public PaymentStatus mapStatus(String externalOrProviderStatus) {
        if (externalOrProviderStatus == null) {
            return PaymentStatus.PENDING;
        }
        return switch (externalOrProviderStatus.toUpperCase()) {
            case "SUCCESS", "PAID", "COMPLETED", "SETTLED" -> PaymentStatus.COMPLETED;
            case "PROCESSING", "PENDING", "IN_PROGRESS", "AWAITING_CLEARANCE" -> PaymentStatus.PENDING;
            case "FAILURE", "FAILED", "DECLINED", "ERROR", "REJECTED", "CANCELLED", "USER_CANCELLED" -> PaymentStatus.FAILED;
            case "REFUND", "REFUNDED", "REVERSED" -> PaymentStatus.REFUNDED;
            default -> PaymentStatus.PENDING;
        };
    }

    private MockSimulationMode resolveSimulationMode(Object modeObj, String reference) {
        if (modeObj != null) {
            String modeStr = String.valueOf(modeObj).toUpperCase();
            try {
                return MockSimulationMode.valueOf(modeStr);
            } catch (IllegalArgumentException ignored) {
                if (modeStr.contains("FAIL") || modeStr.contains("DECLINE") || modeStr.contains("ERROR")) {
                    return MockSimulationMode.FAILURE;
                }
                if (modeStr.contains("PROCESS") || modeStr.contains("PENDING")) {
                    return MockSimulationMode.PROCESSING;
                }
                if (modeStr.contains("REFUND")) {
                    return MockSimulationMode.REFUND;
                }
            }
        }

        // Fallback: reference naming conventions (e.g. TX-...-FAIL, TX-...-PROCESSING)
        if (reference != null) {
            String upper = reference.toUpperCase();
            if (upper.endsWith("-FAIL") || upper.endsWith("-DECLINE") || upper.contains("-FAIL-")) {
                return MockSimulationMode.FAILURE;
            }
            if (upper.endsWith("-PROCESSING") || upper.endsWith("-PENDING") || upper.contains("-PROCESSING-")) {
                return MockSimulationMode.PROCESSING;
            }
            if (upper.endsWith("-REFUND") || upper.contains("-REFUND-")) {
                return MockSimulationMode.REFUND;
            }
        }

        return MockSimulationMode.SUCCESS;
    }

    private String getFailureReason(Map<String, Object> map) {
        if (map != null && map.get("failureReason") != null) {
            return String.valueOf(map.get("failureReason"));
        }
        return "SIMULATED_CARD_DECLINED_INSUFFICIENT_FUNDS";
    }
}
