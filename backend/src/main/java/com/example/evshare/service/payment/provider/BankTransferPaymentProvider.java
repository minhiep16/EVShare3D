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
 * Conceptual Bank Transfer Payment Provider.
 * Simulates domestic bank wire transfer via VietQR standard with automated memo parsing and statement reconciliation.
 * Operates without real banking API credentials.
 */
@Component
public class BankTransferPaymentProvider implements PaymentProvider {

    private static final Logger log = LoggerFactory.getLogger(BankTransferPaymentProvider.class);

    private static final String DEFAULT_BANK_CODE = "970436"; // Vietcombank BIN
    private static final String DEFAULT_BANK_NAME = "Vietcombank";
    private static final String DEFAULT_ACCOUNT_NUMBER = "109887766554";
    private static final String DEFAULT_ACCOUNT_NAME = "EVSHARE SYNDICATE ESCROW VAULT";

    @Override
    public PaymentProviderType getProviderType() {
        return PaymentProviderType.BANK_TRANSFER;
    }

    @Override
    public boolean supports(PaymentMethod paymentMethod) {
        return PaymentMethod.BANK_TRANSFER.equals(paymentMethod);
    }

    @Override
    public PaymentInitiationResult initiate(PaymentInitiationCommand command) {
        log.info("[BANK TRANSFER] Generating VietQR payload for reference: {}, amount: {}",
                command.getTransactionReference(), command.getAmount());

        String memo = "EVSHARE " + command.getTransactionReference();
        String vietQrData = String.format("vietqr://pay?bin=%s&acc=%s&amount=%s&memo=%s",
                DEFAULT_BANK_CODE, DEFAULT_ACCOUNT_NUMBER, command.getAmount().toPlainString(), memo);

        String instructions = String.format(
                "Please transfer exactly %s %s to %s (Acc: %s, %s) with the exact transfer memo: '%s'. Transfer will be verified automatically.",
                command.getAmount().toPlainString(), command.getCurrency(), DEFAULT_BANK_NAME,
                DEFAULT_ACCOUNT_NUMBER, DEFAULT_ACCOUNT_NAME, memo
        );

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("bankBin", DEFAULT_BANK_CODE);
        data.put("bankName", DEFAULT_BANK_NAME);
        data.put("accountNumber", DEFAULT_ACCOUNT_NUMBER);
        data.put("accountHolder", DEFAULT_ACCOUNT_NAME);
        data.put("transferMemo", memo);

        return PaymentInitiationResult.builder()
                .transactionReference(command.getTransactionReference())
                .externalTransactionId("BNK-TX-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase())
                .providerType(PaymentProviderType.BANK_TRANSFER)
                .status(PaymentStatus.PENDING)
                .amount(command.getAmount())
                .paymentUrl("https://vietqr.net/api/generate?bin=" + DEFAULT_BANK_CODE + "&acc=" + DEFAULT_ACCOUNT_NUMBER)
                .qrCodeData(vietQrData)
                .instructions(instructions)
                .expiresAt(Instant.now().plus(24, ChronoUnit.HOURS))
                .providerData(data)
                .build();
    }

    @Override
    public PaymentVerificationResult verify(PaymentVerificationCommand command) {
        log.info("[BANK TRANSFER] Verifying bank transfer statement for reference: {}", command.getTransactionReference());

        String memo = (String) command.getPayload().getOrDefault("transferMemo", "");
        String statusStr = (String) command.getPayload().getOrDefault("status", "PAID");
        Object receivedAmtObj = command.getPayload().get("receivedAmount");

        BigDecimal receivedAmount = command.getExpectedAmount();
        if (receivedAmtObj instanceof Number) {
            receivedAmount = new BigDecimal(receivedAmtObj.toString());
        }

        PaymentStatus status = mapStatus(statusStr);
        boolean memoMatches = memo.contains(command.getTransactionReference());
        boolean amountMatches = receivedAmount.compareTo(command.getExpectedAmount()) >= 0;
        boolean isVerified = PaymentStatus.COMPLETED.equals(status) && (memo.isBlank() || memoMatches) && amountMatches;

        return PaymentVerificationResult.builder()
                .transactionReference(command.getTransactionReference())
                .externalTransactionId(command.getExternalTransactionId() != null
                        ? command.getExternalTransactionId()
                        : "BNK-REC-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase())
                .providerType(PaymentProviderType.BANK_TRANSFER)
                .status(isVerified ? PaymentStatus.COMPLETED : (statusStr.equalsIgnoreCase("EXPIRED") ? PaymentStatus.FAILED : PaymentStatus.PENDING))
                .amount(receivedAmount)
                .verified(isVerified)
                .providerMessage(isVerified
                        ? "Bank transfer successfully reconciled with treasury account"
                        : "Bank transfer verification pending or memo mismatch: " + memo)
                .completedAt(isVerified ? Instant.now() : null)
                .rawResponse(Map.of("memo", memo, "receivedAmount", receivedAmount.toPlainString(), "verified", isVerified))
                .build();
    }

    @Override
    public PaymentRefundResult refund(PaymentRefundCommand command) {
        log.info("[BANK TRANSFER] Initiating reverse bank payout refund for reference: {}, amount: {}",
                command.getTransactionReference(), command.getAmount());

        String refundRef = "BNK-REV-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        return PaymentRefundResult.builder()
                .transactionReference(command.getTransactionReference())
                .refundReference(refundRef)
                .externalRefundId("BNK-PAYOUT-" + UUID.randomUUID().toString().substring(0, 6).toUpperCase())
                .providerType(PaymentProviderType.BANK_TRANSFER)
                .status(PaymentStatus.REFUNDED)
                .amountRefunded(command.getAmount())
                .successful(true)
                .providerMessage("Reverse bank transfer initiated to co-owner source account")
                .refundedAt(Instant.now())
                .rawResponse(Map.of("payoutBatchId", refundRef, "reason", command.getReason() != null ? command.getReason() : ""))
                .build();
    }

    @Override
    public PaymentStatus mapStatus(String externalOrProviderStatus) {
        if (externalOrProviderStatus == null) {
            return PaymentStatus.PENDING;
        }
        return switch (externalOrProviderStatus.toUpperCase()) {
            case "PAID", "CREDITED", "TRANSFERRED", "SUCCESS" -> PaymentStatus.COMPLETED;
            case "EXPIRED", "CANCELLED", "INVALID_SYNTAX", "REJECTED" -> PaymentStatus.FAILED;
            case "REFUNDED", "RETURNED" -> PaymentStatus.REFUNDED;
            default -> PaymentStatus.PENDING;
        };
    }
}
