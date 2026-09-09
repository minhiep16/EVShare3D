package com.example.evshare.service.payment;

import com.example.evshare.dto.request.InitiatePaymentRequest;
import com.example.evshare.dto.response.PaymentResponse;
import com.example.evshare.entity.ExpenseAllocation;
import com.example.evshare.entity.Payment;
import com.example.evshare.entity.SharedFund;
import com.example.evshare.entity.User;
import com.example.evshare.entity.enums.PaymentStatus;
import com.example.evshare.exception.ResourceNotFoundException;
import com.example.evshare.repository.ExpenseAllocationRepository;
import com.example.evshare.repository.PaymentRepository;
import com.example.evshare.repository.SharedFundRepository;
import com.example.evshare.repository.UserRepository;
import com.example.evshare.service.payment.model.PaymentInitiationCommand;
import com.example.evshare.service.payment.model.PaymentInitiationResult;
import com.example.evshare.service.payment.model.PaymentRefundCommand;
import com.example.evshare.service.payment.model.PaymentVerificationCommand;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
@Transactional(rollbackFor = Exception.class)
public class PaymentServiceImpl implements PaymentService {

    private static final Logger log = LoggerFactory.getLogger(PaymentServiceImpl.class);

    private final PaymentRepository paymentRepository;
    private final UserRepository userRepository;
    private final SharedFundRepository sharedFundRepository;
    private final ExpenseAllocationRepository expenseAllocationRepository;
    private final PaymentProviderRegistry paymentProviderRegistry;
    private final PaymentLifecycleService paymentLifecycleService;
    private final IdempotencyService idempotencyService;

    public PaymentServiceImpl(
            PaymentRepository paymentRepository,
            UserRepository userRepository,
            SharedFundRepository sharedFundRepository,
            ExpenseAllocationRepository expenseAllocationRepository,
            PaymentProviderRegistry paymentProviderRegistry,
            PaymentLifecycleService paymentLifecycleService,
            IdempotencyService idempotencyService) {
        this.paymentRepository = paymentRepository;
        this.userRepository = userRepository;
        this.sharedFundRepository = sharedFundRepository;
        this.expenseAllocationRepository = expenseAllocationRepository;
        this.paymentProviderRegistry = paymentProviderRegistry;
        this.paymentLifecycleService = paymentLifecycleService;
        this.idempotencyService = idempotencyService;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PaymentResponse initiatePayment(InitiatePaymentRequest request, String idempotencyKey) {
        String key = idempotencyKey != null && !idempotencyKey.isBlank()
                ? idempotencyKey
                : (request != null ? request.getIdempotencyKey() : null);

        return idempotencyService.executeIdempotent(key, "PAYMENT_INITIATION", request, PaymentResponse.class, () ->
                executeInitiatePayment(request)
        );
    }

    @Transactional(rollbackFor = Exception.class)
    public PaymentResponse executeInitiatePayment(InitiatePaymentRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Payment request cannot be null");
        }

        User payer = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + request.getUserId()));

        SharedFund fund = sharedFundRepository.findById(request.getFundId())
                .orElseThrow(() -> new ResourceNotFoundException("SharedFund not found with id: " + request.getFundId()));

        ExpenseAllocation allocation = null;
        if (request.getExpenseAllocationId() != null) {
            allocation = expenseAllocationRepository.findById(request.getExpenseAllocationId())
                    .orElseThrow(() -> new ResourceNotFoundException("ExpenseAllocation not found with id: " + request.getExpenseAllocationId()));
        }

        // Generate unique platform transaction reference
        String transactionReference = "TX-PAY-" + System.currentTimeMillis() + "-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        // 1. Create Payment in PENDING state
        Payment payment = new Payment();
        payment.setUser(payer);
        payment.setFund(fund);
        payment.setExpenseAllocation(allocation);
        payment.setAmount(request.getAmount());
        payment.setPaymentMethod(request.getPaymentMethod());
        payment.setTransactionReference(transactionReference);
        payment.setStatus(PaymentStatus.PENDING);
        payment.setCreatedAt(Instant.now());
        Payment savedPayment = paymentRepository.saveAndFlush(payment);

        // 2. Delegate to appropriate PaymentProvider
        PaymentProvider provider = paymentProviderRegistry.getProviderForMethod(request.getPaymentMethod());
        PaymentInitiationCommand command = PaymentInitiationCommand.builder()
                .transactionReference(transactionReference)
                .amount(savedPayment.getAmount())
                .paymentMethod(request.getPaymentMethod())
                .payerId(payer.getId())
                .payerEmail(payer.getEmail())
                .payerName(payer.getFullName())
                .payerPhone(payer.getPhoneNumber())
                .description(request.getDescription())
                .metadata(request.getMetadata())
                .build();

        PaymentInitiationResult result = provider.initiate(command);

        // 3. Handle immediate auto-confirm/success if returned by provider
        PaymentStatus finalStatus = savedPayment.getStatus();
        if (result.getStatus() == PaymentStatus.SUCCESS || result.getStatus() == PaymentStatus.COMPLETED) {
            Payment settled = paymentLifecycleService.transitionStatus(
                    savedPayment.getId(),
                    PaymentStatus.SUCCESS,
                    "Provider immediate authorization & settlement",
                    payer.getId()
            );
            finalStatus = settled.getStatus();
        } else if (result.getStatus() == PaymentStatus.FAILED) {
            Payment failed = paymentLifecycleService.transitionStatus(
                    savedPayment.getId(),
                    PaymentStatus.FAILED,
                    "Provider initiation failed: " + result.getInstructions(),
                    payer.getId()
            );
            finalStatus = failed.getStatus();
        }

        return new PaymentResponse(
                savedPayment.getId(),
                savedPayment.getTransactionReference(),
                fund.getId(),
                payer.getId(),
                savedPayment.getAmount(),
                savedPayment.getPaymentMethod(),
                finalStatus,
                result.getPaymentUrl(),
                result.getQrCodeData(),
                result.getInstructions(),
                result.getProviderData(),
                savedPayment.getCreatedAt()
        );
    }

    @Override
    @Transactional(readOnly = true)
    public PaymentResponse getPaymentById(Long paymentId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found with id: " + paymentId));
        return PaymentResponse.fromEntity(payment);
    }

    @Override
    @Transactional(readOnly = true)
    public PaymentResponse getPaymentByReference(String transactionReference) {
        Payment payment = paymentRepository.findByTransactionReference(transactionReference)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found with reference: " + transactionReference));
        return PaymentResponse.fromEntity(payment);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PaymentResponse verifyPayment(PaymentVerificationCommand command, String idempotencyKey) {
        return idempotencyService.executeIdempotent(idempotencyKey, "PAYMENT_VERIFICATION", command, PaymentResponse.class, () -> {
            Payment payment = paymentRepository.findByTransactionReference(command.getTransactionReference())
                    .orElseThrow(() -> new ResourceNotFoundException("Payment not found with reference: " + command.getTransactionReference()));

            PaymentProvider provider = paymentProviderRegistry.getProviderForMethod(payment.getPaymentMethod());
            var result = provider.verify(command);

            if (result.isVerified()) {
                payment = paymentLifecycleService.transitionStatus(payment.getId(), PaymentStatus.SUCCESS, "Verified settlement", null);
            } else if (result.getStatus() == PaymentStatus.FAILED) {
                payment = paymentLifecycleService.transitionStatus(payment.getId(), PaymentStatus.FAILED, result.getProviderMessage(), null);
            }

            PaymentResponse response = PaymentResponse.fromEntity(payment);
            response.setMetadata(result.getRawResponse());
            return response;
        });
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PaymentResponse refundPayment(PaymentRefundCommand command, String idempotencyKey) {
        return idempotencyService.executeIdempotent(idempotencyKey, "PAYMENT_REFUND", command, PaymentResponse.class, () -> {
            Payment payment = paymentRepository.findByTransactionReference(command.getTransactionReference())
                    .orElseThrow(() -> new ResourceNotFoundException("Payment not found with reference: " + command.getTransactionReference()));

            PaymentProvider provider = paymentProviderRegistry.getProviderForMethod(payment.getPaymentMethod());
            var result = provider.refund(command);

            if (result.isSuccessful()) {
                payment = paymentLifecycleService.transitionStatus(payment.getId(), PaymentStatus.REFUNDED, command.getReason(), null);
            }

            PaymentResponse response = PaymentResponse.fromEntity(payment);
            response.setMetadata(result.getRawResponse());
            return response;
        });
    }
}
