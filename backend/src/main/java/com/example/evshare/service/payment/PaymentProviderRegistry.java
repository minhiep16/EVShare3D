package com.example.evshare.service.payment;

import com.example.evshare.entity.enums.PaymentMethod;
import com.example.evshare.entity.enums.PaymentProviderType;
import com.example.evshare.exception.BusinessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Central registry for payment providers.
 * Manages provider discovery, dependency injection, and channel routing.
 */
@Component
public class PaymentProviderRegistry {

    private static final Logger log = LoggerFactory.getLogger(PaymentProviderRegistry.class);

    private final Map<PaymentProviderType, PaymentProvider> providersByType = new EnumMap<>(PaymentProviderType.class);
    private final List<PaymentProvider> allProviders;

    public PaymentProviderRegistry(List<PaymentProvider> providers) {
        this.allProviders = providers != null ? List.copyOf(providers) : Collections.emptyList();
        for (PaymentProvider provider : this.allProviders) {
            providersByType.put(provider.getProviderType(), provider);
            log.info("Registered payment provider: {} ({})",
                    provider.getProviderType(), provider.getClass().getSimpleName());
        }
    }

    /**
     * Retrieves the provider registered for a specific PaymentProviderType.
     */
    public PaymentProvider getProvider(PaymentProviderType type) {
        if (type == null) {
            throw new BusinessException("Payment provider type cannot be null", HttpStatus.BAD_REQUEST);
        }
        PaymentProvider provider = providersByType.get(type);
        if (provider == null) {
            log.warn("Payment provider not found for type: {}", type);
            throw new BusinessException("Unsupported payment provider type: " + type, HttpStatus.BAD_REQUEST);
        }
        return provider;
    }

    /**
     * Resolves the appropriate payment provider capable of processing the given customer PaymentMethod.
     */
    public PaymentProvider getProviderForMethod(PaymentMethod method) {
        if (method == null) {
            throw new BusinessException("Payment method cannot be null", HttpStatus.BAD_REQUEST);
        }
        return allProviders.stream()
                .filter(p -> p.supports(method))
                .findFirst()
                .orElseThrow(() -> {
                    log.warn("No payment provider found supporting method: {}", method);
                    return new BusinessException("No payment provider configured for method: " + method, HttpStatus.BAD_REQUEST);
                });
    }

    /**
     * Returns an unmodifiable list of all registered providers.
     */
    public List<PaymentProvider> getAllProviders() {
        return allProviders;
    }

    /**
     * Checks if a provider type is registered.
     */
    public boolean isSupported(PaymentProviderType type) {
        return type != null && providersByType.containsKey(type);
    }

    /**
     * Checks if any registered provider supports the given payment method.
     */
    public boolean isMethodSupported(PaymentMethod method) {
        return method != null && allProviders.stream().anyMatch(p -> p.supports(method));
    }
}
