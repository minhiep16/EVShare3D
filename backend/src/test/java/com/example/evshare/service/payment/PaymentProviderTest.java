package com.example.evshare.service.payment;

import com.example.evshare.entity.enums.PaymentMethod;
import com.example.evshare.entity.enums.PaymentProviderType;
import com.example.evshare.entity.enums.PaymentStatus;
import com.example.evshare.service.payment.model.*;
import com.example.evshare.service.payment.provider.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("06-J — Payment Provider Unit Tests")
class PaymentProviderTest {

    // =========================================================================
    // 1. MOCK PAYMENT PROVIDER TESTS
    // =========================================================================
    @Nested
    @DisplayName("Mock Payment Provider Tests")
    class MockProviderTests {

        private final MockPaymentProvider provider = new MockPaymentProvider();

        @Test
        @DisplayName("Mock: Identity and supported method verification")
        void testProviderIdentityAndSupports() {
            assertEquals(PaymentProviderType.MOCK, provider.getProviderType());
            assertTrue(provider.supports(PaymentMethod.MOCK));
            assertFalse(provider.supports(PaymentMethod.BANK_TRANSFER));
            assertFalse(provider.supports(PaymentMethod.E_WALLET));
            assertFalse(provider.supports(PaymentMethod.CREDIT_CARD));
        }

        @Test
        @DisplayName("Mock: Initiate success scenario includes prominent development/test disclaimers")
        void testInitiate_SuccessWithDisclaimers() {
            PaymentInitiationCommand command = PaymentInitiationCommand.builder()
                    .transactionReference("TX-MOCK-001")
                    .amount(new BigDecimal("500000.00"))
                    .currency("VND")
                    .paymentMethod(PaymentMethod.MOCK)
                    .description("Syndicate seed deposit")
                    .payerId(101L)
                    .payerName("Nguyen Van A")
                    .payerEmail("a@evshare.io")
                    .build();

            PaymentInitiationResult result = provider.initiate(command);

            assertNotNull(result);
            assertEquals("TX-MOCK-001", result.getTransactionReference());
            assertEquals(PaymentProviderType.MOCK, result.getProviderType());
            assertEquals(PaymentStatus.PENDING, result.getStatus());
            assertEquals(new BigDecimal("500000.00"), result.getAmount());
            assertNotNull(result.getExternalTransactionId());
            assertTrue(result.getPaymentUrl().contains("TX-MOCK-001"));
            assertTrue(result.getPaymentUrl().contains("sandbox.evshare.io"));
            assertTrue(result.getQrCodeData().contains("TX-MOCK-001"));
            assertNotNull(result.getExpiresAt());

            // Validate explicit development/test markers
            assertTrue(result.getInstructions().contains("[TEST/SANDBOX ONLY - NOT A REAL PAYMENT]"));
            assertEquals(true, result.getProviderData().get("isDevelopmentOrTest"));
            assertEquals("SANDBOX_TEST", result.getProviderData().get("simulationEnvironment"));
            assertTrue(String.valueOf(result.getProviderData().get("disclaimer")).contains("DEVELOPMENT/TEST PAYMENT SIMULATION ONLY"));
        }

        @Test
        @DisplayName("Mock: Initiate supports autoConfirm flag returning COMPLETED immediately")
        void testInitiate_AutoConfirm() {
            PaymentInitiationCommand command = PaymentInitiationCommand.builder()
                    .transactionReference("TX-MOCK-AUTOCONF")
                    .amount(new BigDecimal("200000.00"))
                    .paymentMethod(PaymentMethod.MOCK)
                    .metadata(Map.of("autoConfirm", true))
                    .build();

            PaymentInitiationResult result = provider.initiate(command);

            assertEquals(PaymentStatus.COMPLETED, result.getStatus());
            assertTrue(result.getInstructions().contains("auto-confirmed successfully"));
            assertTrue(result.getInstructions().contains("[TEST/SANDBOX ONLY - NOT A REAL PAYMENT]"));
        }

        @Test
        @DisplayName("Mock: Initiate supports simulated failure mode")
        void testInitiate_FailureMode() {
            PaymentInitiationCommand command = PaymentInitiationCommand.builder()
                    .transactionReference("TX-MOCK-INIT-FAIL")
                    .amount(new BigDecimal("100000.00"))
                    .paymentMethod(PaymentMethod.MOCK)
                    .metadata(Map.of("simulationMode", "FAILURE", "failureReason", "INSUFFICIENT_FUNDS"))
                    .build();

            PaymentInitiationResult result = provider.initiate(command);

            assertEquals(PaymentStatus.FAILED, result.getStatus());
            assertTrue(result.getInstructions().contains("INSUFFICIENT_FUNDS"));
            assertTrue(result.getInstructions().contains("[TEST/SANDBOX ONLY - NOT A REAL PAYMENT]"));
        }

        @Test
        @DisplayName("Mock: Initiate supports processing state")
        void testInitiate_ProcessingMode() {
            PaymentInitiationCommand command = PaymentInitiationCommand.builder()
                    .transactionReference("TX-MOCK-INIT-PROC")
                    .amount(new BigDecimal("150000.00"))
                    .paymentMethod(PaymentMethod.MOCK)
                    .metadata(Map.of("simulationMode", "PROCESSING"))
                    .build();

            PaymentInitiationResult result = provider.initiate(command);

            assertEquals(PaymentStatus.PENDING, result.getStatus());
            assertTrue(result.getInstructions().contains("processing in progress"));
        }

        @Test
        @DisplayName("Mock: Verify returns COMPLETED on SUCCESS scenario with test disclaimers")
        void testVerify_Success() {
            PaymentVerificationCommand successCmd = PaymentVerificationCommand.builder()
                    .transactionReference("TX-MOCK-002")
                    .expectedAmount(new BigDecimal("1000000.00"))
                    .payload(Map.of("simulationMode", "SUCCESS"))
                    .build();

            PaymentVerificationResult successRes = provider.verify(successCmd);

            assertTrue(successRes.isVerified());
            assertEquals(PaymentStatus.COMPLETED, successRes.getStatus());
            assertEquals(new BigDecimal("1000000.00"), successRes.getAmount());
            assertNotNull(successRes.getCompletedAt());
            assertTrue(successRes.getProviderMessage().contains("[TEST/SANDBOX ONLY - NOT A REAL PAYMENT]"));
            assertEquals(true, successRes.getRawResponse().get("isDevelopmentOrTest"));
            assertEquals("SANDBOX_TEST", successRes.getRawResponse().get("simulationEnvironment"));
            assertTrue(String.valueOf(successRes.getRawResponse().get("disclaimer")).contains("DEVELOPMENT/TEST PAYMENT SIMULATION ONLY"));
        }

        @Test
        @DisplayName("Mock: Verify returns FAILED on FAILURE scenario with specific decline reason")
        void testVerify_FailureWithReason() {
            PaymentVerificationCommand failCmd = PaymentVerificationCommand.builder()
                    .transactionReference("TX-MOCK-003")
                    .expectedAmount(new BigDecimal("1000000.00"))
                    .payload(Map.of("simulationMode", "FAILURE", "failureReason", "CARD_EXPIRED_OR_STOLEN"))
                    .build();

            PaymentVerificationResult failRes = provider.verify(failCmd);

            assertFalse(failRes.isVerified());
            assertEquals(PaymentStatus.FAILED, failRes.getStatus());
            assertTrue(failRes.getProviderMessage().contains("CARD_EXPIRED_OR_STOLEN"));
            assertTrue(failRes.getProviderMessage().contains("[TEST/SANDBOX ONLY - NOT A REAL PAYMENT]"));
            assertNull(failRes.getCompletedAt());
        }

        @Test
        @DisplayName("Mock: Verify returns PENDING on PROCESSING scenario (asynchronous in-progress)")
        void testVerify_Processing() {
            PaymentVerificationCommand procCmd = PaymentVerificationCommand.builder()
                    .transactionReference("TX-MOCK-004")
                    .expectedAmount(new BigDecimal("1000000.00"))
                    .payload(Map.of("simulationMode", "PROCESSING"))
                    .build();

            PaymentVerificationResult procRes = provider.verify(procCmd);

            assertFalse(procRes.isVerified());
            assertEquals(PaymentStatus.PENDING, procRes.getStatus());
            assertTrue(procRes.getProviderMessage().contains("processing in progress"));
            assertTrue(procRes.getProviderMessage().contains("[TEST/SANDBOX ONLY - NOT A REAL PAYMENT]"));
        }

        @Test
        @DisplayName("Mock: Reference conventions automatically trigger scenario without metadata")
        void testVerify_ReferenceConventions() {
            PaymentVerificationCommand failRefCmd = PaymentVerificationCommand.builder()
                    .transactionReference("TX-MOCK-DECLINE-FAIL")
                    .expectedAmount(new BigDecimal("50000.00"))
                    .build();
            PaymentVerificationResult failResult = provider.verify(failRefCmd);
            assertEquals(PaymentStatus.FAILED, failResult.getStatus());

            PaymentVerificationCommand procRefCmd = PaymentVerificationCommand.builder()
                    .transactionReference("TX-MOCK-ORDER-PROCESSING")
                    .expectedAmount(new BigDecimal("50000.00"))
                    .build();
            PaymentVerificationResult procResult = provider.verify(procRefCmd);
            assertEquals(PaymentStatus.PENDING, procResult.getStatus());
        }

        @Test
        @DisplayName("Mock: Refund returns successful REFUNDED response with test disclaimers")
        void testRefund_Success() {
            PaymentRefundCommand command = PaymentRefundCommand.builder()
                    .transactionReference("TX-MOCK-005")
                    .externalTransactionId("EXT-MOCK-005")
                    .amount(new BigDecimal("300000.00"))
                    .reason("Overpayment adjustment")
                    .build();

            PaymentRefundResult result = provider.refund(command);

            assertNotNull(result);
            assertTrue(result.isSuccessful());
            assertEquals("TX-MOCK-005", result.getTransactionReference());
            assertEquals(PaymentStatus.REFUNDED, result.getStatus());
            assertEquals(new BigDecimal("300000.00"), result.getAmountRefunded());
            assertNotNull(result.getRefundReference());
            assertTrue(result.getRefundReference().startsWith("MOCK-REF-"));
            assertNotNull(result.getRefundedAt());
            assertTrue(result.getProviderMessage().contains("[TEST/SANDBOX ONLY - NOT A REAL PAYMENT]"));
            assertEquals(true, result.getRawResponse().get("isDevelopmentOrTest"));
            assertTrue(String.valueOf(result.getRawResponse().get("disclaimer")).contains("DEVELOPMENT/TEST PAYMENT SIMULATION ONLY"));
        }

        @Test
        @DisplayName("Mock: Refund supports simulated rejection")
        void testRefund_SimulatedFailure() {
            PaymentRefundCommand command = PaymentRefundCommand.builder()
                    .transactionReference("TX-MOCK-006")
                    .amount(new BigDecimal("500000.00"))
                    .reason("Disputed charge")
                    .metadata(Map.of("simulateRefundFailure", true, "failureReason", "REFUND_WINDOW_EXPIRED"))
                    .build();

            PaymentRefundResult result = provider.refund(command);

            assertNotNull(result);
            assertFalse(result.isSuccessful());
            assertEquals(PaymentStatus.FAILED, result.getStatus());
            assertEquals(BigDecimal.ZERO, result.getAmountRefunded());
            assertNull(result.getRefundReference());
            assertTrue(result.getProviderMessage().contains("REFUND_WINDOW_EXPIRED"));
            assertTrue(result.getProviderMessage().contains("[TEST/SANDBOX ONLY - NOT A REAL PAYMENT]"));
        }

        @Test
        @DisplayName("Mock: Status mapping normalizes provider strings for all scenarios")
        void testMapStatus() {
            assertEquals(PaymentStatus.COMPLETED, provider.mapStatus("SUCCESS"));
            assertEquals(PaymentStatus.COMPLETED, provider.mapStatus("PAID"));
            assertEquals(PaymentStatus.COMPLETED, provider.mapStatus("COMPLETED"));
            assertEquals(PaymentStatus.COMPLETED, provider.mapStatus("SETTLED"));

            assertEquals(PaymentStatus.PENDING, provider.mapStatus("PROCESSING"));
            assertEquals(PaymentStatus.PENDING, provider.mapStatus("PENDING"));
            assertEquals(PaymentStatus.PENDING, provider.mapStatus("IN_PROGRESS"));
            assertEquals(PaymentStatus.PENDING, provider.mapStatus("AWAITING_CLEARANCE"));

            assertEquals(PaymentStatus.FAILED, provider.mapStatus("FAILURE"));
            assertEquals(PaymentStatus.FAILED, provider.mapStatus("FAILED"));
            assertEquals(PaymentStatus.FAILED, provider.mapStatus("DECLINED"));
            assertEquals(PaymentStatus.FAILED, provider.mapStatus("ERROR"));
            assertEquals(PaymentStatus.FAILED, provider.mapStatus("REJECTED"));
            assertEquals(PaymentStatus.FAILED, provider.mapStatus("CANCELLED"));
            assertEquals(PaymentStatus.FAILED, provider.mapStatus("USER_CANCELLED"));

            assertEquals(PaymentStatus.REFUNDED, provider.mapStatus("REFUND"));
            assertEquals(PaymentStatus.REFUNDED, provider.mapStatus("REFUNDED"));
            assertEquals(PaymentStatus.REFUNDED, provider.mapStatus("REVERSED"));

            assertEquals(PaymentStatus.PENDING, provider.mapStatus("UNKNOWN"));
            assertEquals(PaymentStatus.PENDING, provider.mapStatus(null));
        }
    }

    // =========================================================================
    // 2. BANK TRANSFER (VIETQR) PROVIDER TESTS
    // =========================================================================
    @Nested
    @DisplayName("Bank Transfer (VietQR) Provider Tests")
    class BankTransferProviderTests {

        private final BankTransferPaymentProvider provider = new BankTransferPaymentProvider();

        @Test
        @DisplayName("Bank Transfer: Identity and supported method verification")
        void testProviderIdentityAndSupports() {
            assertEquals(PaymentProviderType.BANK_TRANSFER, provider.getProviderType());
            assertTrue(provider.supports(PaymentMethod.BANK_TRANSFER));
            assertFalse(provider.supports(PaymentMethod.E_WALLET));
            assertFalse(provider.supports(PaymentMethod.CREDIT_CARD));
        }

        @Test
        @DisplayName("Bank Transfer: Initiate formats VietQR payload, account details, and transfer memo")
        void testInitiate() {
            PaymentInitiationCommand command = PaymentInitiationCommand.builder()
                    .transactionReference("TX-BNK-9901")
                    .amount(new BigDecimal("2500000.00"))
                    .currency("VND")
                    .paymentMethod(PaymentMethod.BANK_TRANSFER)
                    .description("Syndicate Q3 reserve contribution")
                    .build();

            PaymentInitiationResult result = provider.initiate(command);

            assertNotNull(result);
            assertEquals("TX-BNK-9901", result.getTransactionReference());
            assertEquals(PaymentProviderType.BANK_TRANSFER, result.getProviderType());
            assertEquals(PaymentStatus.PENDING, result.getStatus());
            assertEquals(new BigDecimal("2500000.00"), result.getAmount());

            // Check VietQR payload contains BIN, amount, and exact transfer memo syntax
            assertNotNull(result.getQrCodeData());
            assertTrue(result.getQrCodeData().startsWith("vietqr://pay?"));
            assertTrue(result.getQrCodeData().contains("memo=EVSHARE TX-BNK-9901"));
            assertTrue(result.getInstructions().contains("EVSHARE TX-BNK-9901"));
            assertTrue(result.getInstructions().contains("Vietcombank"));
        }

        @Test
        @DisplayName("Bank Transfer: Verify checks statement memo and matched amount")
        void testVerify() {
            PaymentVerificationCommand successCmd = PaymentVerificationCommand.builder()
                    .transactionReference("TX-BNK-9902")
                    .expectedAmount(new BigDecimal("1000000.00"))
                    .payload(Map.of(
                            "transferMemo", "EVSHARE TX-BNK-9902 - NGUYEN VAN A",
                            "receivedAmount", "1000000.00",
                            "status", "PAID"
                    ))
                    .build();

            PaymentVerificationResult successRes = provider.verify(successCmd);
            assertTrue(successRes.isVerified());
            assertEquals(PaymentStatus.COMPLETED, successRes.getStatus());

            PaymentVerificationCommand memoMismatchCmd = PaymentVerificationCommand.builder()
                    .transactionReference("TX-BNK-9902")
                    .expectedAmount(new BigDecimal("1000000.00"))
                    .payload(Map.of(
                            "transferMemo", "WRONG_MEMO_CONTENT",
                            "receivedAmount", "1000000.00",
                            "status", "PAID"
                    ))
                    .build();

            PaymentVerificationResult mismatchRes = provider.verify(memoMismatchCmd);
            assertFalse(mismatchRes.isVerified());
            assertEquals(PaymentStatus.PENDING, mismatchRes.getStatus());

            PaymentVerificationCommand expiredCmd = PaymentVerificationCommand.builder()
                    .transactionReference("TX-BNK-9903")
                    .expectedAmount(new BigDecimal("1000000.00"))
                    .payload(Map.of("status", "EXPIRED"))
                    .build();

            PaymentVerificationResult expiredRes = provider.verify(expiredCmd);
            assertFalse(expiredRes.isVerified());
            assertEquals(PaymentStatus.FAILED, expiredRes.getStatus());
        }

        @Test
        @DisplayName("Bank Transfer: Refund initiates reverse wire transfer")
        void testRefund() {
            PaymentRefundCommand command = PaymentRefundCommand.builder()
                    .transactionReference("TX-BNK-9904")
                    .amount(new BigDecimal("2000000.00"))
                    .reason("Duplicate deposit reversal")
                    .build();

            PaymentRefundResult result = provider.refund(command);

            assertNotNull(result);
            assertTrue(result.isSuccessful());
            assertEquals("TX-BNK-9904", result.getTransactionReference());
            assertEquals(PaymentStatus.REFUNDED, result.getStatus());
            assertTrue(result.getRefundReference().startsWith("BNK-REV-"));
        }

        @Test
        @DisplayName("Bank Transfer: Status mapping")
        void testMapStatus() {
            assertEquals(PaymentStatus.COMPLETED, provider.mapStatus("PAID"));
            assertEquals(PaymentStatus.COMPLETED, provider.mapStatus("CREDITED"));
            assertEquals(PaymentStatus.FAILED, provider.mapStatus("EXPIRED"));
            assertEquals(PaymentStatus.FAILED, provider.mapStatus("CANCELLED"));
            assertEquals(PaymentStatus.REFUNDED, provider.mapStatus("REFUNDED"));
            assertEquals(PaymentStatus.PENDING, provider.mapStatus("WAITING"));
        }
    }

    // =========================================================================
    // 3. E-WALLET (MOMO / ZALOPAY) PROVIDER TESTS
    // =========================================================================
    @Nested
    @DisplayName("E-Wallet Provider Tests")
    class EWalletProviderTests {

        private final EWalletPaymentProvider provider = new EWalletPaymentProvider();

        @Test
        @DisplayName("E-Wallet: Identity and supported method verification")
        void testProviderIdentityAndSupports() {
            assertEquals(PaymentProviderType.E_WALLET, provider.getProviderType());
            assertTrue(provider.supports(PaymentMethod.E_WALLET));
            assertFalse(provider.supports(PaymentMethod.BANK_TRANSFER));
            assertFalse(provider.supports(PaymentMethod.CREDIT_CARD));
        }

        @Test
        @DisplayName("E-Wallet: Initiate generates app deep link and gateway URL")
        void testInitiate() {
            PaymentInitiationCommand command = PaymentInitiationCommand.builder()
                    .transactionReference("TX-EWL-8801")
                    .amount(new BigDecimal("150000.00"))
                    .currency("VND")
                    .paymentMethod(PaymentMethod.E_WALLET)
                    .description("Charging fee top-up")
                    .build();

            PaymentInitiationResult result = provider.initiate(command);

            assertNotNull(result);
            assertEquals("TX-EWL-8801", result.getTransactionReference());
            assertEquals(PaymentProviderType.E_WALLET, result.getProviderType());
            assertEquals(PaymentStatus.PENDING, result.getStatus());
            assertTrue(result.getPaymentUrl().contains("pay.ewallet.vn"));
            assertTrue(result.getQrCodeData().contains("momo://payment"));
            assertNotNull(result.getProviderData().get("deepLink"));
        }

        @Test
        @DisplayName("E-Wallet: Verify validates IPN resultCode and signature")
        void testVerify() {
            PaymentVerificationCommand successCmd = PaymentVerificationCommand.builder()
                    .transactionReference("TX-EWL-8802")
                    .expectedAmount(new BigDecimal("150000.00"))
                    .payload(Map.of("resultCode", "0", "signature", "VALID_SIGNATURE"))
                    .build();

            PaymentVerificationResult successRes = provider.verify(successCmd);
            assertTrue(successRes.isVerified());
            assertEquals(PaymentStatus.COMPLETED, successRes.getStatus());

            // Invalid HMAC signature rejects verification
            PaymentVerificationCommand badSigCmd = PaymentVerificationCommand.builder()
                    .transactionReference("TX-EWL-8803")
                    .expectedAmount(new BigDecimal("150000.00"))
                    .payload(Map.of("resultCode", "0", "signature", "INVALID_SIGNATURE"))
                    .build();

            PaymentVerificationResult badSigRes = provider.verify(badSigCmd);
            assertFalse(badSigRes.isVerified());
            assertEquals(PaymentStatus.FAILED, badSigRes.getStatus());
            assertTrue(badSigRes.getProviderMessage().contains("Invalid HMAC signature"));

            // User cancelled
            PaymentVerificationCommand cancelCmd = PaymentVerificationCommand.builder()
                    .transactionReference("TX-EWL-8804")
                    .expectedAmount(new BigDecimal("150000.00"))
                    .payload(Map.of("resultCode", "1006", "signature", "VALID_SIGNATURE"))
                    .build();

            PaymentVerificationResult cancelRes = provider.verify(cancelCmd);
            assertFalse(cancelRes.isVerified());
            assertEquals(PaymentStatus.FAILED, cancelRes.getStatus());
        }

        @Test
        @DisplayName("E-Wallet: Refund returns REFUNDED")
        void testRefund() {
            PaymentRefundCommand command = PaymentRefundCommand.builder()
                    .transactionReference("TX-EWL-8805")
                    .amount(new BigDecimal("150000.00"))
                    .reason("Cancelled booking refund")
                    .build();

            PaymentRefundResult result = provider.refund(command);

            assertNotNull(result);
            assertTrue(result.isSuccessful());
            assertEquals("TX-EWL-8805", result.getTransactionReference());
            assertEquals(PaymentStatus.REFUNDED, result.getStatus());
            assertTrue(result.getRefundReference().startsWith("EWL-REF-"));
        }

        @Test
        @DisplayName("E-Wallet: Status mapping handles vendor result codes")
        void testMapStatus() {
            assertEquals(PaymentStatus.COMPLETED, provider.mapStatus("0"));
            assertEquals(PaymentStatus.COMPLETED, provider.mapStatus("SUCCESS"));
            assertEquals(PaymentStatus.FAILED, provider.mapStatus("1006"));
            assertEquals(PaymentStatus.FAILED, provider.mapStatus("49"));
            assertEquals(PaymentStatus.REFUNDED, provider.mapStatus("REFUNDED"));
            assertEquals(PaymentStatus.PENDING, provider.mapStatus("WAITING"));
        }
    }

    // =========================================================================
    // 4. GATEWAY (CREDIT CARD / PAYOS / STRIPE) PROVIDER TESTS
    // =========================================================================
    @Nested
    @DisplayName("Gateway Provider Tests")
    class GatewayProviderTests {

        private final GatewayPaymentProvider provider = new GatewayPaymentProvider();

        @Test
        @DisplayName("Gateway: Identity and supported method verification")
        void testProviderIdentityAndSupports() {
            assertEquals(PaymentProviderType.GATEWAY, provider.getProviderType());
            assertTrue(provider.supports(PaymentMethod.CREDIT_CARD));
            assertTrue(provider.supports(PaymentMethod.GATEWAY));
            assertFalse(provider.supports(PaymentMethod.BANK_TRANSFER));
            assertFalse(provider.supports(PaymentMethod.E_WALLET));
        }

        @Test
        @DisplayName("Gateway: Initiate generates hosted checkout session URL")
        void testInitiate() {
            PaymentInitiationCommand command = PaymentInitiationCommand.builder()
                    .transactionReference("TX-GTW-7701")
                    .amount(new BigDecimal("12000000.00"))
                    .currency("VND")
                    .paymentMethod(PaymentMethod.CREDIT_CARD)
                    .description("Capital call replenishment payment")
                    .build();

            PaymentInitiationResult result = provider.initiate(command);

            assertNotNull(result);
            assertEquals("TX-GTW-7701", result.getTransactionReference());
            assertEquals(PaymentProviderType.GATEWAY, result.getProviderType());
            assertEquals(PaymentStatus.PENDING, result.getStatus());
            assertTrue(result.getPaymentUrl().contains("checkout.gateway.evshare.io"));
            assertTrue(result.getExternalTransactionId().startsWith("cs_test_"));
        }

        @Test
        @DisplayName("Gateway: Verify handles intent succeeded and declined statuses")
        void testVerify() {
            PaymentVerificationCommand successCmd = PaymentVerificationCommand.builder()
                    .transactionReference("TX-GTW-7702")
                    .expectedAmount(new BigDecimal("12000000.00"))
                    .payload(Map.of("status", "succeeded", "chargeId", "ch_123456789"))
                    .build();

            PaymentVerificationResult successRes = provider.verify(successCmd);
            assertTrue(successRes.isVerified());
            assertEquals(PaymentStatus.COMPLETED, successRes.getStatus());
            assertEquals("ch_123456789", successRes.getExternalTransactionId());

            PaymentVerificationCommand declineCmd = PaymentVerificationCommand.builder()
                    .transactionReference("TX-GTW-7703")
                    .expectedAmount(new BigDecimal("12000000.00"))
                    .payload(Map.of("status", "card_declined"))
                    .build();

            PaymentVerificationResult declineRes = provider.verify(declineCmd);
            assertFalse(declineRes.isVerified());
            assertEquals(PaymentStatus.FAILED, declineRes.getStatus());
        }

        @Test
        @DisplayName("Gateway: Refund creates refund record with gateway refund ID")
        void testRefund() {
            PaymentRefundCommand command = PaymentRefundCommand.builder()
                    .transactionReference("TX-GTW-7704")
                    .amount(new BigDecimal("5000000.00"))
                    .reason("Partial dispute resolution")
                    .build();

            PaymentRefundResult result = provider.refund(command);

            assertNotNull(result);
            assertTrue(result.isSuccessful());
            assertEquals("TX-GTW-7704", result.getTransactionReference());
            assertEquals(PaymentStatus.REFUNDED, result.getStatus());
            assertTrue(result.getRefundReference().startsWith("re_test_"));
        }

        @Test
        @DisplayName("Gateway: Status mapping handles payment intent strings")
        void testMapStatus() {
            assertEquals(PaymentStatus.COMPLETED, provider.mapStatus("succeeded"));
            assertEquals(PaymentStatus.COMPLETED, provider.mapStatus("paid"));
            assertEquals(PaymentStatus.FAILED, provider.mapStatus("card_declined"));
            assertEquals(PaymentStatus.FAILED, provider.mapStatus("canceled"));
            assertEquals(PaymentStatus.REFUNDED, provider.mapStatus("refunded"));
            assertEquals(PaymentStatus.PENDING, provider.mapStatus("requires_payment_method"));
        }
    }
}
