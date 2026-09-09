package com.example.evshare.service.payment;

import com.example.evshare.entity.enums.PaymentMethod;
import com.example.evshare.entity.enums.PaymentProviderType;
import com.example.evshare.exception.BusinessException;
import com.example.evshare.service.payment.provider.BankTransferPaymentProvider;
import com.example.evshare.service.payment.provider.EWalletPaymentProvider;
import com.example.evshare.service.payment.provider.GatewayPaymentProvider;
import com.example.evshare.service.payment.provider.MockPaymentProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("06-J — Payment Provider Registry Tests")
class PaymentProviderRegistryTest {

    private PaymentProviderRegistry registry;
    private MockPaymentProvider mockProvider;
    private BankTransferPaymentProvider bankProvider;
    private EWalletPaymentProvider eWalletProvider;
    private GatewayPaymentProvider gatewayProvider;

    @BeforeEach
    void setUp() {
        mockProvider = new MockPaymentProvider();
        bankProvider = new BankTransferPaymentProvider();
        eWalletProvider = new EWalletPaymentProvider();
        gatewayProvider = new GatewayPaymentProvider();

        registry = new PaymentProviderRegistry(List.of(
                mockProvider,
                bankProvider,
                eWalletProvider,
                gatewayProvider
        ));
    }

    @Test
    @DisplayName("Registry: Correctly registers and discovers all 4 conceptual providers")
    void testDiscoversAllProviders() {
        List<PaymentProvider> all = registry.getAllProviders();
        assertEquals(4, all.size());
        assertTrue(registry.isSupported(PaymentProviderType.MOCK));
        assertTrue(registry.isSupported(PaymentProviderType.BANK_TRANSFER));
        assertTrue(registry.isSupported(PaymentProviderType.E_WALLET));
        assertTrue(registry.isSupported(PaymentProviderType.GATEWAY));
    }

    @Test
    @DisplayName("Registry: Resolves provider by PaymentProviderType")
    void testGetProviderByType() {
        assertSame(mockProvider, registry.getProvider(PaymentProviderType.MOCK));
        assertSame(bankProvider, registry.getProvider(PaymentProviderType.BANK_TRANSFER));
        assertSame(eWalletProvider, registry.getProvider(PaymentProviderType.E_WALLET));
        assertSame(gatewayProvider, registry.getProvider(PaymentProviderType.GATEWAY));
    }

    @Test
    @DisplayName("Registry: Resolves provider by customer PaymentMethod")
    void testGetProviderByMethod() {
        assertSame(mockProvider, registry.getProviderForMethod(PaymentMethod.MOCK));
        assertSame(bankProvider, registry.getProviderForMethod(PaymentMethod.BANK_TRANSFER));
        assertSame(eWalletProvider, registry.getProviderForMethod(PaymentMethod.E_WALLET));
        assertSame(gatewayProvider, registry.getProviderForMethod(PaymentMethod.CREDIT_CARD));
        assertSame(gatewayProvider, registry.getProviderForMethod(PaymentMethod.GATEWAY));
    }

    @Test
    @DisplayName("Registry: Throws BusinessException when null or unsupported provider requested")
    void testUnsupportedProviderHandling() {
        assertThrows(BusinessException.class, () -> registry.getProvider(null));
        assertThrows(BusinessException.class, () -> registry.getProviderForMethod(null));

        PaymentProviderRegistry emptyRegistry = new PaymentProviderRegistry(List.of());
        assertThrows(BusinessException.class, () -> emptyRegistry.getProvider(PaymentProviderType.MOCK));
        assertThrows(BusinessException.class, () -> emptyRegistry.getProviderForMethod(PaymentMethod.BANK_TRANSFER));
    }

    @Test
    @DisplayName("Registry: Method support checks")
    void testIsMethodSupported() {
        assertTrue(registry.isMethodSupported(PaymentMethod.BANK_TRANSFER));
        assertTrue(registry.isMethodSupported(PaymentMethod.E_WALLET));
        assertTrue(registry.isMethodSupported(PaymentMethod.CREDIT_CARD));
        assertTrue(registry.isMethodSupported(PaymentMethod.MOCK));
        assertTrue(registry.isMethodSupported(PaymentMethod.GATEWAY));
        assertFalse(registry.isMethodSupported(null));
    }
}
