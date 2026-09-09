package com.example.evshare.service.payment;

import com.example.evshare.entity.enums.PaymentMethod;
import com.example.evshare.entity.enums.PaymentProviderType;
import com.example.evshare.entity.enums.PaymentStatus;
import com.example.evshare.service.payment.model.*;

/**
 * Service Provider Interface (SPI) abstraction for payment gateways and channels.
 * Decouples the platform financial domain and syndicate Shared Fund from external provider implementations.
 */
public interface PaymentProvider {

    /**
     * Identifies the provider type (e.g. MOCK, BANK_TRANSFER, E_WALLET, GATEWAY).
     */
    PaymentProviderType getProviderType();

    /**
     * Determines whether this provider supports the given customer payment method.
     */
    boolean supports(PaymentMethod paymentMethod);

    /**
     * Initiates a payment session, generating provider references, payment URL, QR code, or transfer instructions.
     */
    PaymentInitiationResult initiate(PaymentInitiationCommand command);

    /**
     * Verifies the authenticity and completion status of a transaction or callback payload.
     */
    PaymentVerificationResult verify(PaymentVerificationCommand command);

    /**
     * Processes a full or partial refund against a previously verified transaction.
     */
    PaymentRefundResult refund(PaymentRefundCommand command);

    /**
     * Normalizes a provider-specific external status code/string into platform PaymentStatus.
     */
    PaymentStatus mapStatus(String externalOrProviderStatus);
}
