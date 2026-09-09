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
 * Conceptual E-Wallet Payment Provider (MoMo, ZaloPay, VNPay simulation).
 * Simulates mobile in-app deep linking, QR checkout, and HMAC callback verification.
 * Operates without real e-wallet merchant secrets.
 */
@Component
public class EWalletPaymentProvider implements PaymentProvider {

    private static final Logger log = LoggerFactory.getLogger(EWalletPaymentProvider.class);

    @Override
    public PaymentProviderType getProviderType() {
        return PaymentProviderType.E_WALLET;
    }

    @Override
    public boolean supports(PaymentMethod paymentMethod) {
        return PaymentMethod.E_WALLET.equals(paymentMethod);
    }

    @Override
    public PaymentInitiationResult initiate(PaymentInitiationCommand command) {
        log.info("[E-WALLET] Generating deep-link and QR payload for reference: {}, amount: {}",
                command.getTransactionReference(), command.getAmount());

        String orderId = "EWL-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        String deepLink = String.format("evshare://ewallet/pay?orderId=%s&ref=%s&amount=%s",
                orderId, command.getTransactionReference(), command.getAmount().toPlainString());
        String webPayUrl = String.format("https://pay.ewallet.vn/v2/gateway/pay?orderId=%s&ref=%s",
                orderId, command.getTransactionReference());
        String qrPayload = String.format("momo://payment?action=payWithApp&orderId=%s&amount=%s",
                orderId, command.getAmount().toPlainString());

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("walletPartner", "MOMO_ZALOPAY_HUB");
        data.put("orderId", orderId);
        data.put("deepLink", deepLink);
        data.put("simulatedSignature", "HMAC_SIM_" + UUID.randomUUID().toString().substring(0, 12));

        return PaymentInitiationResult.builder()
                .transactionReference(command.getTransactionReference())
                .externalTransactionId(orderId)
                .providerType(PaymentProviderType.E_WALLET)
                .status(PaymentStatus.PENDING)
                .amount(command.getAmount())
                .paymentUrl(webPayUrl)
                .qrCodeData(qrPayload)
                .instructions("Open your mobile e-wallet application to scan QR or click the payment link.")
                .expiresAt(Instant.now().plus(15, ChronoUnit.MINUTES))
                .providerData(data)
                .build();
    }

    @Override
    public PaymentVerificationResult verify(PaymentVerificationCommand command) {
        log.info("[E-WALLET] Verifying IPN / callback signature for reference: {}", command.getTransactionReference());

        String resultCode = String.valueOf(command.getPayload().getOrDefault("resultCode", "0"));
        String signature = (String) command.getPayload().getOrDefault("signature", "VALID_SIGNATURE");

        // Simulate HMAC signature check: reject if explicitly marked INVALID
        boolean signatureValid = !"INVALID_SIGNATURE".equalsIgnoreCase(signature);
        PaymentStatus status = mapStatus(resultCode);
        boolean isVerified = signatureValid && PaymentStatus.COMPLETED.equals(status);

        return PaymentVerificationResult.builder()
                .transactionReference(command.getTransactionReference())
                .externalTransactionId(command.getExternalTransactionId() != null
                        ? command.getExternalTransactionId()
                        : "EWL-TRANS-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase())
                .providerType(PaymentProviderType.E_WALLET)
                .status(isVerified ? PaymentStatus.COMPLETED : (!signatureValid ? PaymentStatus.FAILED : status))
                .amount(command.getExpectedAmount())
                .verified(isVerified)
                .providerMessage(isVerified
                        ? "E-wallet transaction verified and IPN signature validated"
                        : (signatureValid ? "E-wallet payment rejected with code: " + resultCode : "Invalid HMAC signature on webhook payload"))
                .completedAt(isVerified ? Instant.now() : null)
                .rawResponse(Map.of("resultCode", resultCode, "signatureValid", signatureValid, "verified", isVerified))
                .build();
    }

    @Override
    public PaymentRefundResult refund(PaymentRefundCommand command) {
        log.info("[E-WALLET] Processing e-wallet reversal refund for reference: {}, amount: {}",
                command.getTransactionReference(), command.getAmount());

        String refundRef = "EWL-REF-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        return PaymentRefundResult.builder()
                .transactionReference(command.getTransactionReference())
                .refundReference(refundRef)
                .externalRefundId("EWL-REV-" + UUID.randomUUID().toString().substring(0, 6).toUpperCase())
                .providerType(PaymentProviderType.E_WALLET)
                .status(PaymentStatus.REFUNDED)
                .amountRefunded(command.getAmount())
                .successful(true)
                .providerMessage("E-wallet funds credited back to user wallet account")
                .refundedAt(Instant.now())
                .rawResponse(Map.of("refundId", refundRef, "reason", command.getReason() != null ? command.getReason() : ""))
                .build();
    }

    @Override
    public PaymentStatus mapStatus(String externalOrProviderStatus) {
        if (externalOrProviderStatus == null) {
            return PaymentStatus.PENDING;
        }
        return switch (externalOrProviderStatus.toUpperCase()) {
            case "0", "SUCCESS", "PAID", "COMPLETED" -> PaymentStatus.COMPLETED;
            case "1006", "USER_CANCELLED", "FAILED", "49", "EXPIRED", "DENIED" -> PaymentStatus.FAILED;
            case "REFUNDED" -> PaymentStatus.REFUNDED;
            default -> PaymentStatus.PENDING;
        };
    }
}
