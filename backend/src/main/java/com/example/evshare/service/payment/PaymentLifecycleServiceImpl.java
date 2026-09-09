package com.example.evshare.service.payment;

import com.example.evshare.dto.response.PaymentAuditLogResponse;
import com.example.evshare.entity.AuditLog;
import com.example.evshare.entity.ExpenseAllocation;
import com.example.evshare.entity.FundTransaction;
import com.example.evshare.entity.Payment;
import com.example.evshare.entity.SharedFund;
import com.example.evshare.entity.User;
import com.example.evshare.entity.enums.FundTransactionSource;
import com.example.evshare.entity.enums.PaymentStatus;
import com.example.evshare.entity.enums.TransactionEntryType;
import com.example.evshare.entity.enums.TransactionType;
import com.example.evshare.exception.ResourceNotFoundException;
import com.example.evshare.repository.AuditLogRepository;
import com.example.evshare.repository.ExpenseAllocationRepository;
import com.example.evshare.repository.FundTransactionRepository;
import com.example.evshare.repository.PaymentRepository;
import com.example.evshare.repository.SharedFundRepository;
import com.example.evshare.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Implementation of PaymentLifecycleService enforcing state machine rules,
 * acquiring pessimistic row locks, coordinating multi-entity financial consistency,
 * and persisting immutable audit trails on each transition.
 */
@Service
@Transactional(rollbackFor = Exception.class)
public class PaymentLifecycleServiceImpl implements PaymentLifecycleService {

    private static final Logger log = LoggerFactory.getLogger(PaymentLifecycleServiceImpl.class);

    private final PaymentRepository paymentRepository;
    private final PaymentStateMachine paymentStateMachine;
    private final AuditLogRepository auditLogRepository;
    private final UserRepository userRepository;
    private final SharedFundRepository sharedFundRepository;
    private final FundTransactionRepository fundTransactionRepository;
    private final ExpenseAllocationRepository expenseAllocationRepository;
    private final ObjectMapper objectMapper;

    public PaymentLifecycleServiceImpl(
            PaymentRepository paymentRepository,
            PaymentStateMachine paymentStateMachine,
            AuditLogRepository auditLogRepository,
            UserRepository userRepository,
            SharedFundRepository sharedFundRepository,
            FundTransactionRepository fundTransactionRepository,
            ExpenseAllocationRepository expenseAllocationRepository,
            ObjectMapper objectMapper) {
        this.paymentRepository = paymentRepository;
        this.paymentStateMachine = paymentStateMachine;
        this.auditLogRepository = auditLogRepository;
        this.userRepository = userRepository;
        this.sharedFundRepository = sharedFundRepository;
        this.fundTransactionRepository = fundTransactionRepository;
        this.expenseAllocationRepository = expenseAllocationRepository;
        this.objectMapper = objectMapper;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Payment transitionStatus(Long paymentId, PaymentStatus targetStatus, String reason, Long actorUserId) {
        Payment payment = paymentRepository.findByIdWithLock(paymentId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found with id: " + paymentId));
        return executeTransition(payment, targetStatus, reason, actorUserId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Payment transitionStatusByReference(String transactionReference, PaymentStatus targetStatus, String reason, Long actorUserId) {
        Payment payment = paymentRepository.findByTransactionReferenceWithLock(transactionReference)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found with reference: " + transactionReference));
        return executeTransition(payment, targetStatus, reason, actorUserId);
    }

    private Payment executeTransition(Payment payment, PaymentStatus targetStatus, String reason, Long actorUserId) {
        PaymentStatus currentStatus = payment.getStatus();

        // 1. Validate transition legality according to authoritative state machine rules
        paymentStateMachine.validateTransition(currentStatus, targetStatus, payment.getTransactionReference());

        // 2. Snapshot old state for audit trail
        String oldStateJson = buildStateJson(payment, currentStatus, null);

        // 3. Mutate payment status and persist
        payment.setStatus(targetStatus);
        Payment savedPayment = paymentRepository.saveAndFlush(payment);

        // 4. Coordinate Cross-Entity Financial Consistency
        coordinateFinancialConsistency(savedPayment, currentStatus, targetStatus);

        // 5. Snapshot new state with reason
        String newStateJson = buildStateJson(savedPayment, targetStatus, reason);

        // 6. Look up actor if provided
        User actor = null;
        if (actorUserId != null) {
            actor = userRepository.findById(actorUserId).orElse(null);
        }

        // 7. Record immutable AuditLog entry
        AuditLog auditLog = new AuditLog();
        auditLog.setEntityName("Payment");
        auditLog.setEntityId(savedPayment.getId());
        auditLog.setAction("PAYMENT_TRANSITION_" + targetStatus.name());
        auditLog.setUser(actor);
        auditLog.setOldStateJson(oldStateJson);
        auditLog.setNewStateJson(newStateJson);
        auditLog.setCreatedAt(Instant.now());
        auditLogRepository.save(auditLog);

        log.info("Payment [{}] transitioned from {} to {} by actorId={}, reason='{}'",
                savedPayment.getTransactionReference(), currentStatus, targetStatus, actorUserId, reason);

        return savedPayment;
    }

    private void coordinateFinancialConsistency(Payment payment, PaymentStatus currentStatus, PaymentStatus targetStatus) {
        boolean isEnteringSettled = (targetStatus == PaymentStatus.SUCCESS || targetStatus == PaymentStatus.COMPLETED);
        boolean wasSettled = (currentStatus == PaymentStatus.SUCCESS || currentStatus == PaymentStatus.COMPLETED);

        if (isEnteringSettled) {
            // Settle ExpenseAllocation if attached
            if (payment.getExpenseAllocation() != null) {
                ExpenseAllocation allocation = payment.getExpenseAllocation();
                allocation.setIsSettled(true);
                allocation.setSettledAt(Instant.now());
                expenseAllocationRepository.save(allocation);
                log.info("Marked expense allocation {} as settled via payment {}", allocation.getId(), payment.getId());
            }

            // Credit SharedFund if attached
            if (payment.getFund() != null) {
                SharedFund fund = sharedFundRepository.findByIdWithLock(payment.getFund().getId())
                        .orElse(payment.getFund());
                BigDecimal oldBalance = fund.getCurrentBalance() != null ? fund.getCurrentBalance() : BigDecimal.ZERO;
                BigDecimal newBalance = oldBalance.add(payment.getAmount()).setScale(2, RoundingMode.HALF_EVEN);
                fund.setCurrentBalance(newBalance);
                fund.setUpdatedAt(Instant.now());
                sharedFundRepository.save(fund);

                FundTransaction tx = new FundTransaction();
                tx.setFund(fund);
                tx.setUser(payment.getUser());
                tx.setTransactionType(TransactionType.CONTRIBUTION);
                tx.setEntryType(TransactionEntryType.CREDIT);
                tx.setAmount(payment.getAmount());
                tx.setBalanceAfter(newBalance);
                tx.setTransactionReference(payment.getTransactionReference());
                tx.setDescription("Settlement for payment " + payment.getTransactionReference());
                tx.setSource(FundTransactionSource.PAYMENT_SETTLEMENT);
                tx.setCreatedAt(Instant.now());
                fundTransactionRepository.save(tx);

                log.info("Credited fund {} with {} {} via payment settlement {}",
                        fund.getId(), payment.getAmount(), fund.getCurrency(), payment.getTransactionReference());
            }
        } else if (targetStatus == PaymentStatus.REFUNDED && wasSettled) {
            // Revert ExpenseAllocation settlement if attached
            if (payment.getExpenseAllocation() != null) {
                ExpenseAllocation allocation = payment.getExpenseAllocation();
                allocation.setIsSettled(false);
                allocation.setSettledAt(null);
                expenseAllocationRepository.save(allocation);
                log.info("Reverted settlement of expense allocation {} due to refund of payment {}", allocation.getId(), payment.getId());
            }

            // Revert / Debit SharedFund if attached
            if (payment.getFund() != null) {
                SharedFund fund = sharedFundRepository.findByIdWithLock(payment.getFund().getId())
                        .orElse(payment.getFund());
                BigDecimal oldBalance = fund.getCurrentBalance() != null ? fund.getCurrentBalance() : BigDecimal.ZERO;
                BigDecimal newBalance = oldBalance.subtract(payment.getAmount()).setScale(2, RoundingMode.HALF_EVEN);
                fund.setCurrentBalance(newBalance);
                fund.setUpdatedAt(Instant.now());
                sharedFundRepository.save(fund);

                FundTransaction tx = new FundTransaction();
                tx.setFund(fund);
                tx.setUser(payment.getUser());
                tx.setTransactionType(TransactionType.WITHDRAWAL);
                tx.setEntryType(TransactionEntryType.DEBIT);
                tx.setAmount(payment.getAmount());
                tx.setBalanceAfter(newBalance);
                tx.setTransactionReference(payment.getTransactionReference() + "-REFUND");
                tx.setDescription("Refund reversal for payment " + payment.getTransactionReference());
                tx.setSource(FundTransactionSource.MANUAL_ADJUSTMENT);
                tx.setCreatedAt(Instant.now());
                fundTransactionRepository.save(tx);

                log.info("Debited fund {} with {} {} due to refund of payment {}",
                        fund.getId(), payment.getAmount(), fund.getCurrency(), payment.getTransactionReference());
            }
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<PaymentAuditLogResponse> getPaymentHistory(Long paymentId, Long currentUserId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found with id: " + paymentId));

        return auditLogRepository.findByEntityNameAndEntityIdOrderByIdDesc("Payment", payment.getId())
                .stream()
                .map(PaymentAuditLogResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<PaymentAuditLogResponse> getPaymentHistoryByReference(String transactionReference, Long currentUserId) {
        Payment payment = paymentRepository.findByTransactionReference(transactionReference)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found with reference: " + transactionReference));

        return auditLogRepository.findByEntityNameAndEntityIdOrderByIdDesc("Payment", payment.getId())
                .stream()
                .map(PaymentAuditLogResponse::fromEntity)
                .collect(Collectors.toList());
    }

    private String buildStateJson(Payment payment, PaymentStatus status, String reason) {
        try {
            ObjectNode node = objectMapper.createObjectNode();
            node.put("paymentId", payment.getId());
            node.put("transactionReference", payment.getTransactionReference());
            node.put("status", status != null ? status.name() : null);
            node.put("amount", payment.getAmount() != null ? payment.getAmount().toPlainString() : "0.00");
            node.put("paymentMethod", payment.getPaymentMethod() != null ? payment.getPaymentMethod().name() : null);
            if (payment.getUser() != null) {
                node.put("userId", payment.getUser().getId());
            }
            if (payment.getFund() != null) {
                node.put("fundId", payment.getFund().getId());
            }
            if (payment.getExpenseAllocation() != null) {
                node.put("expenseAllocationId", payment.getExpenseAllocation().getId());
            }
            if (reason != null && !reason.isBlank()) {
                node.put("reason", reason);
            }
            return objectMapper.writeValueAsString(node);
        } catch (Exception e) {
            log.warn("Failed to serialize state JSON for payment {}", payment.getId(), e);
            return String.format("{\"status\":\"%s\",\"reason\":\"%s\"}", status, reason != null ? reason : "");
        }
    }
}
