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
 * Conceptual Payment Gateway Provider (Credit Card / 3D-Secure / International payment gateway simulation).
 * Simulates hosted checkout redirect sessions, card tokenization, webhook verification, and card reversals.
 * Operates without real gateway secret keys.
 */
@Component
public class GatewayPaymentProvider implements PaymentProvider {

    private static final Logger log = LoggerFactory.getLogger(GatewayPaymentProvider.class);

    @Override
    public PaymentProviderType getProviderType() {
        return PaymentProviderType.GATEWAY;
    }

    @Override
    public boolean supports(PaymentMethod paymentMethod) {
        return PaymentMethod.CREDIT_CARD.equals(paymentMethod) || PaymentMethod.GATEWAY.equals(paymentMethod);
    }

    @Override
    public PaymentInitiationResult initiate(PaymentInitiationCommand command) {
        log.info("[GATEWAY] Creating hosted card checkout session for reference: {}, amount: {}",
                command.getTransactionReference(), command.getAmount());

        String sessionId = "cs_test_" + UUID.randomUUID().toString().replace("-", "");
        String checkoutUrl = "https://checkout.gateway.evshare.io/pay/" + sessionId;

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("sessionId", sessionId);
        data.put("gatewayEnvironment", "SANDBOX_SIMULATION");
        data.put("threeDSecureRequired", true);

        return PaymentInitiationResult.builder()
                .transactionReference(command.getTransactionReference())
                .externalTransactionId(sessionId)
                .providerType(PaymentProviderType.GATEWAY)
                .status(PaymentStatus.PENDING)
                .amount(command.getAmount())
                .paymentUrl(checkoutUrl)
                .instructions("Redirecting user to PCI-DSS Level 1 compliant card payment gateway.")
                .expiresAt(Instant.now().plus(30, ChronoUnit.MINUTES))
                .providerData(data)
                .build();
    }

    @Override
    public PaymentVerificationResult verify(PaymentVerificationCommand command) {
        log.info("[GATEWAY] Verifying gateway payment intent for reference: {}", command.getTransactionReference());

        String intentStatus = (String) command.getPayload().getOrDefault("status", "succeeded");
        String chargeId = (String) command.getPayload().getOrDefault("chargeId", "ch_" + UUID.randomUUID().toString().substring(0, 12));

        PaymentStatus status = mapStatus(intentStatus);
        boolean isVerified = PaymentStatus.COMPLETED.equals(status);

        return PaymentVerificationResult.builder()
                .transactionReference(command.getTransactionReference())
                .externalTransactionId(chargeId)
                .providerType(PaymentProviderType.GATEWAY)
                .status(status)
                .amount(command.getExpectedAmount())
                .verified(isVerified)
                .providerMessage(isVerified
                        ? "Card payment intent authorized and captured"
                        : "Card payment intent failed: " + intentStatus)
                .completedAt(isVerified ? Instant.now() : null)
                .rawResponse(Map.of("intentStatus", intentStatus, "chargeId", chargeId, "verified", isVerified))
                .build();
    }

    @Override
    public PaymentRefundResult refund(PaymentRefundCommand command) {
        log.info("[GATEWAY] Processing card charge refund for reference: {}, amount: {}",
                command.getTransactionReference(), command.getAmount());

        String refundId = "re_test_" + UUID.randomUUID().toString().replace("-", "");
        return PaymentRefundResult.builder()
                .transactionReference(command.getTransactionReference())
                .refundReference(refundId)
                .externalRefundId(refundId)
                .providerType(PaymentProviderType.GATEWAY)
                .status(PaymentStatus.REFUNDED)
                .amountRefunded(command.getAmount())
                .successful(true)
                .providerMessage("Card charge refunded via payment gateway processor")
                .refundedAt(Instant.now())
                .rawResponse(Map.of("refundId", refundId, "reason", command.getReason() != null ? command.getReason() : ""))
                .build();
    }

    @Override
    public PaymentStatus mapStatus(String externalOrProviderStatus) {
        if (externalOrProviderStatus == null) {
            return PaymentStatus.PENDING;
        }
        return switch (externalOrProviderStatus.toLowerCase()) {
            case "succeeded", "paid", "captured", "success" -> PaymentStatus.COMPLETED;
            case "failed", "card_declined", "canceled", "expired" -> PaymentStatus.FAILED;
            case "refunded", "partially_refunded" -> PaymentStatus.REFUNDED;
            default -> PaymentStatus.PENDING;
        };
    }
}
