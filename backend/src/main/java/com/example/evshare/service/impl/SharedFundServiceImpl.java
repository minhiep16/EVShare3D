package com.example.evshare.service.impl;

import com.example.evshare.dto.request.FundContributionRequest;
import com.example.evshare.dto.request.FundWithdrawalRequest;
import com.example.evshare.dto.response.FundAuditLogResponse;
import com.example.evshare.dto.response.FundReconciliationResponse;
import com.example.evshare.dto.response.FundTransactionResponse;
import com.example.evshare.dto.response.SharedFundResponse;
import com.example.evshare.entity.AuditLog;
import com.example.evshare.entity.FundTransaction;
import com.example.evshare.entity.OwnershipGroup;
import com.example.evshare.entity.SharedFund;
import com.example.evshare.entity.User;
import com.example.evshare.entity.enums.FundTransactionSource;
import com.example.evshare.entity.enums.RoleName;
import com.example.evshare.entity.enums.TransactionEntryType;
import com.example.evshare.entity.enums.TransactionType;
import com.example.evshare.exception.BusinessException;
import com.example.evshare.exception.InsufficientFundBalanceException;
import com.example.evshare.exception.ResourceNotFoundException;
import com.example.evshare.repository.AuditLogRepository;
import com.example.evshare.repository.FundTransactionRepository;
import com.example.evshare.repository.OwnershipGroupRepository;
import com.example.evshare.repository.OwnershipShareRepository;
import com.example.evshare.repository.SharedFundRepository;
import com.example.evshare.repository.UserRepository;
import com.example.evshare.service.SharedFundService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class SharedFundServiceImpl implements SharedFundService {

    private static final Logger log = LoggerFactory.getLogger(SharedFundServiceImpl.class);
    private static final BigDecimal DEFAULT_MIN_RESERVE = new BigDecimal("10000000.00");

    private final SharedFundRepository sharedFundRepository;
    private final FundTransactionRepository fundTransactionRepository;
    private final OwnershipGroupRepository ownershipGroupRepository;
    private final OwnershipShareRepository ownershipShareRepository;
    private final UserRepository userRepository;
    private final AuditLogRepository auditLogRepository;
    private final ObjectMapper objectMapper;

    public SharedFundServiceImpl(SharedFundRepository sharedFundRepository,
                                 FundTransactionRepository fundTransactionRepository,
                                 OwnershipGroupRepository ownershipGroupRepository,
                                 OwnershipShareRepository ownershipShareRepository,
                                 UserRepository userRepository,
                                 AuditLogRepository auditLogRepository,
                                 ObjectMapper objectMapper) {
        this.sharedFundRepository = sharedFundRepository;
        this.fundTransactionRepository = fundTransactionRepository;
        this.ownershipGroupRepository = ownershipGroupRepository;
        this.ownershipShareRepository = ownershipShareRepository;
        this.userRepository = userRepository;
        this.auditLogRepository = auditLogRepository;
        this.objectMapper = objectMapper;
    }

    @Override
    @Transactional(readOnly = true)
    public SharedFundResponse getFundByGroupId(Long groupId, Long currentUserId) {
        OwnershipGroup group = findGroupOrThrow(groupId);
        User actor = findUserOrThrow(currentUserId);
        validateGroupMembershipOrAdmin(group, actor);

        SharedFund fund = sharedFundRepository.findByGroupId(groupId)
                .orElseGet(() -> getOrCreateFundForGroup(group));

        return SharedFundResponse.fromEntity(fund);
    }

    @Override
    @Transactional(readOnly = true)
    public SharedFundResponse getFundById(Long fundId, Long currentUserId) {
        SharedFund fund = sharedFundRepository.findById(fundId)
                .orElseThrow(() -> new ResourceNotFoundException("SharedFund not found with id: " + fundId));
        User actor = findUserOrThrow(currentUserId);
        validateGroupMembershipOrAdmin(fund.getGroup(), actor);

        return SharedFundResponse.fromEntity(fund);
    }

    @Override
    @Transactional(readOnly = true)
    public SharedFundResponse getFundBalance(Long groupId, Long currentUserId) {
        return getFundByGroupId(groupId, currentUserId);
    }

    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED, rollbackFor = Exception.class)
    public FundTransactionResponse contribute(Long groupId, FundContributionRequest request, Long currentUserId, String ipAddress) {
        if (request == null || request.getAmount() == null) {
            throw new BusinessException("Contribution request and amount cannot be null", HttpStatus.BAD_REQUEST);
        }

        BigDecimal amount = request.getAmount().setScale(2, RoundingMode.HALF_EVEN);
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("Contribution amount must be strictly greater than zero", HttpStatus.BAD_REQUEST);
        }

        OwnershipGroup group = findGroupOrThrow(groupId);
        User actor = findUserOrThrow(currentUserId);
        validateGroupMembershipOrAdmin(group, actor);

        // Pessimistic write lock to serialize concurrent operations on this syndicate fund
        SharedFund fund = sharedFundRepository.findByGroupIdWithLock(groupId)
                .orElseGet(() -> getOrCreateFundForGroup(group));

        BigDecimal oldBalance = fund.getCurrentBalance() != null ? fund.getCurrentBalance() : BigDecimal.ZERO;
        BigDecimal newBalance = oldBalance.add(amount).setScale(2, RoundingMode.HALF_EVEN);

        fund.setCurrentBalance(newBalance);
        fund.setUpdatedAt(Instant.now());
        SharedFund savedFund = sharedFundRepository.save(fund);

        String desc = request.getDescription() != null && !request.getDescription().isBlank()
                ? request.getDescription().trim()
                : "Member contribution by " + actor.getFullName();

        FundTransactionSource source = request.getSource() != null
                ? request.getSource()
                : FundTransactionSource.MEMBER_CONTRIBUTION;
        String reference = resolveTransactionReference(request.getTransactionReference(), "CRD");

        FundTransaction tx = new FundTransaction();
        tx.setFund(savedFund);
        tx.setUser(actor);
        tx.setTransactionType(TransactionType.CONTRIBUTION);
        tx.setEntryType(TransactionEntryType.CREDIT);
        tx.setAmount(amount);
        tx.setBalanceAfter(newBalance);
        tx.setTransactionReference(reference);
        tx.setDescription(desc);
        tx.setSource(source);
        tx.setCreatedAt(Instant.now());
        FundTransaction savedTx = fundTransactionRepository.save(tx);

        recordAuditLog(savedFund, actor, "SHARED_FUND_CONTRIBUTION", oldBalance, newBalance, amount,
                savedTx.getId(), reference, "CREDIT", "CONTRIBUTION", ipAddress);

        log.info("Recorded contribution of {} {} for fund {} by user {}. Ref: {}, New balance: {}",
                amount, fund.getCurrency(), fund.getId(), actor.getId(), reference, newBalance);

        return FundTransactionResponse.fromEntity(savedTx);
    }

    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED, rollbackFor = Exception.class)
    public FundTransactionResponse withdraw(Long groupId, FundWithdrawalRequest request, Long currentUserId, String ipAddress) {
        if (request == null || request.getAmount() == null) {
            throw new BusinessException("Withdrawal request and amount cannot be null", HttpStatus.BAD_REQUEST);
        }

        BigDecimal amount = request.getAmount().setScale(2, RoundingMode.HALF_EVEN);
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("Withdrawal amount must be strictly greater than zero", HttpStatus.BAD_REQUEST);
        }

        OwnershipGroup group = findGroupOrThrow(groupId);
        User actor = findUserOrThrow(currentUserId);
        validateGroupMembershipOrAdmin(group, actor);

        // Pessimistic write lock to serialize concurrent operations on this syndicate fund
        SharedFund fund = sharedFundRepository.findByGroupIdWithLock(groupId)
                .orElseGet(() -> getOrCreateFundForGroup(group));

        BigDecimal oldBalance = fund.getCurrentBalance() != null ? fund.getCurrentBalance() : BigDecimal.ZERO;

        // Requirement: no negative balance unless explicitly permitted
        boolean allowOverdraft = Boolean.TRUE.equals(request.getAllowOverdraft());
        if (oldBalance.compareTo(amount) < 0 && !allowOverdraft) {
            log.warn("Withdrawal rejected: fund {} has balance {} but {} was requested (overdraft not permitted)",
                    fund.getId(), oldBalance, amount);
            throw new InsufficientFundBalanceException(fund.getId(), oldBalance, amount);
        }

        BigDecimal newBalance = oldBalance.subtract(amount).setScale(2, RoundingMode.HALF_EVEN);
        fund.setCurrentBalance(newBalance);
        fund.setUpdatedAt(Instant.now());
        SharedFund savedFund = sharedFundRepository.save(fund);

        String desc = request.getDescription() != null && !request.getDescription().isBlank()
                ? request.getDescription().trim()
                : "Fund withdrawal by " + actor.getFullName();

        FundTransactionSource source = request.getSource() != null
                ? request.getSource()
                : FundTransactionSource.EXPENSE_PAYOUT;
        String reference = resolveTransactionReference(request.getTransactionReference(), "DBT");

        FundTransaction tx = new FundTransaction();
        tx.setFund(savedFund);
        tx.setUser(actor);
        tx.setTransactionType(TransactionType.WITHDRAWAL);
        tx.setEntryType(TransactionEntryType.DEBIT);
        tx.setAmount(amount);
        tx.setBalanceAfter(newBalance);
        tx.setTransactionReference(reference);
        tx.setDescription(desc);
        tx.setSource(source);
        tx.setCreatedAt(Instant.now());
        FundTransaction savedTx = fundTransactionRepository.save(tx);

        recordAuditLog(savedFund, actor, "SHARED_FUND_WITHDRAWAL", oldBalance, newBalance, amount,
                savedTx.getId(), reference, "DEBIT", allowOverdraft ? "WITHDRAWAL_OVERDRAFT" : "WITHDRAWAL", ipAddress);

        log.info("Recorded withdrawal of {} {} from fund {} by user {}. Ref: {}, New balance: {} (overdraft: {})",
                amount, fund.getCurrency(), fund.getId(), actor.getId(), reference, newBalance, allowOverdraft);

        return FundTransactionResponse.fromEntity(savedTx);
    }

    @Override
    @Transactional(readOnly = true)
    public List<FundTransactionResponse> getTransactionHistory(Long groupId, Long currentUserId) {
        OwnershipGroup group = findGroupOrThrow(groupId);
        User actor = findUserOrThrow(currentUserId);
        validateGroupMembershipOrAdmin(group, actor);

        SharedFund fund = sharedFundRepository.findByGroupId(groupId)
                .orElseGet(() -> getOrCreateFundForGroup(group));

        return fundTransactionRepository.findByFundIdOrderByCreatedAtDesc(fund.getId()).stream()
                .map(FundTransactionResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<FundAuditLogResponse> getFundAuditHistory(Long groupId, Long currentUserId) {
        OwnershipGroup group = findGroupOrThrow(groupId);
        User actor = findUserOrThrow(currentUserId);
        validateGroupMembershipOrAdmin(group, actor);

        SharedFund fund = sharedFundRepository.findByGroupId(groupId)
                .orElseGet(() -> getOrCreateFundForGroup(group));

        List<AuditLog> auditLogs = auditLogRepository.findByEntityNameAndEntityIdOrderByIdDesc("SharedFund", fund.getId());
        return auditLogs.stream()
                .map(FundAuditLogResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public SharedFund getOrCreateFundForGroup(OwnershipGroup group) {
        return sharedFundRepository.findByGroupId(group.getId())
                .orElseGet(() -> {
                    SharedFund newFund = new SharedFund();
                    newFund.setGroup(group);
                    newFund.setCurrentBalance(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_EVEN));
                    newFund.setMinimumReserveThreshold(DEFAULT_MIN_RESERVE);
                    newFund.setCurrency("VND");
                    newFund.setUpdatedAt(Instant.now());
                    return sharedFundRepository.save(newFund);
                });
    }

    @Override
    @Transactional(readOnly = true)
    public FundReconciliationResponse reconcileFundBalance(Long groupId, Long currentUserId) {
        OwnershipGroup group = findGroupOrThrow(groupId);
        User actor = findUserOrThrow(currentUserId);
        validateGroupMembershipOrAdmin(group, actor);

        SharedFund fund = sharedFundRepository.findByGroupId(groupId)
                .orElseGet(() -> getOrCreateFundForGroup(group));

        BigDecimal currentBalance = fund.getCurrentBalance() != null
                ? fund.getCurrentBalance().setScale(2, RoundingMode.HALF_EVEN)
                : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_EVEN);

        List<FundTransaction> transactions = fundTransactionRepository.findByFundIdOrderByCreatedAtAscIdAsc(fund.getId());

        BigDecimal runningBalance = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_EVEN);
        BigDecimal totalCredits = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_EVEN);
        BigDecimal totalDebits = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_EVEN);
        int creditCount = 0;
        int debitCount = 0;

        for (FundTransaction tx : transactions) {
            BigDecimal txAmount = tx.getAmount() != null
                    ? tx.getAmount().setScale(2, RoundingMode.HALF_EVEN)
                    : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_EVEN);

            if (tx.isCredit() || TransactionEntryType.CREDIT.equals(tx.getEntryType())) {
                runningBalance = runningBalance.add(txAmount).setScale(2, RoundingMode.HALF_EVEN);
                totalCredits = totalCredits.add(txAmount).setScale(2, RoundingMode.HALF_EVEN);
                creditCount++;
            } else if (tx.isDebit() || TransactionEntryType.DEBIT.equals(tx.getEntryType())) {
                runningBalance = runningBalance.subtract(txAmount).setScale(2, RoundingMode.HALF_EVEN);
                totalDebits = totalDebits.add(txAmount).setScale(2, RoundingMode.HALF_EVEN);
                debitCount++;
            }
        }

        BigDecimal delta = currentBalance.subtract(runningBalance).setScale(2, RoundingMode.HALF_EVEN);
        boolean isReconciled = delta.compareTo(BigDecimal.ZERO) == 0;

        String summary = isReconciled
                ? String.format("Ledger reconciled successfully for fund %d. Total credits: %s (%d), Total debits: %s (%d), Balance: %s",
                fund.getId(), totalCredits.toPlainString(), creditCount, totalDebits.toPlainString(), debitCount, currentBalance.toPlainString())
                : String.format("Ledger discrepancy detected for fund %d! Current balance: %s, Calculated ledger: %s, Discrepancy delta: %s",
                fund.getId(), currentBalance.toPlainString(), runningBalance.toPlainString(), delta.toPlainString());

        if (!isReconciled) {
            log.error("FUND AUDIT ALERT: Fund {} reconciliation failed! Current: {}, Calculated: {}, Delta: {}",
                    fund.getId(), currentBalance, runningBalance, delta);
        } else {
            log.info("Fund {} reconciliation verified: {} txs, balance {}", fund.getId(), transactions.size(), currentBalance);
        }

        return new FundReconciliationResponse(
                fund.getId(),
                group.getId(),
                currentBalance,
                runningBalance,
                totalCredits,
                totalDebits,
                creditCount,
                debitCount,
                transactions.size(),
                isReconciled,
                delta,
                Instant.now(),
                summary
        );
    }

    @Override
    @Transactional(readOnly = true)
    public FundTransactionResponse getTransactionByReference(Long groupId, String reference, Long currentUserId) {
        if (reference == null || reference.isBlank()) {
            throw new BusinessException("Transaction reference cannot be null or blank", HttpStatus.BAD_REQUEST);
        }

        OwnershipGroup group = findGroupOrThrow(groupId);
        User actor = findUserOrThrow(currentUserId);
        validateGroupMembershipOrAdmin(group, actor);

        SharedFund fund = sharedFundRepository.findByGroupId(groupId)
                .orElseGet(() -> getOrCreateFundForGroup(group));

        FundTransaction tx = fundTransactionRepository.findByTransactionReference(reference.trim())
                .orElseThrow(() -> new ResourceNotFoundException("Transaction not found with reference: " + reference));

        if (!fund.getId().equals(tx.getFund().getId())) {
            throw new BusinessException("Transaction does not belong to this ownership group's fund", HttpStatus.BAD_REQUEST);
        }

        return FundTransactionResponse.fromEntity(tx);
    }

    private String resolveTransactionReference(String requestedRef, String prefix) {
        if (requestedRef != null && !requestedRef.trim().isEmpty()) {
            String trimmed = requestedRef.trim();
            if (fundTransactionRepository.existsByTransactionReference(trimmed)) {
                throw new BusinessException("Transaction reference already exists: " + trimmed, HttpStatus.CONFLICT);
            }
            return trimmed;
        }
        return "TX-" + prefix + "-" + LocalDate.now().toString().replace("-", "") + "-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    private void recordAuditLog(SharedFund fund, User actor, String action,
                                BigDecimal oldBalance, BigDecimal newBalance, BigDecimal amount,
                                Long transactionId, String reference, String entryType, String subtype, String ipAddress) {
        try {
            AuditLog auditLog = new AuditLog();
            auditLog.setUser(actor);
            auditLog.setAction(action);
            auditLog.setEntityName("SharedFund");
            auditLog.setEntityId(fund.getId());
            auditLog.setIpAddress(ipAddress);
            auditLog.setCreatedAt(Instant.now());

            Map<String, Object> oldState = Map.of(
                    "balance", oldBalance != null ? oldBalance.toPlainString() : "0.00",
                    "minimumReserveThreshold", fund.getMinimumReserveThreshold().toPlainString()
            );

            Map<String, Object> newState = new LinkedHashMap<>();
            newState.put("balance", newBalance.toPlainString());
            newState.put("amount", amount.toPlainString());
            newState.put("transactionId", transactionId);
            newState.put("reference", reference);
            newState.put("entryType", entryType);
            newState.put("subtype", subtype);
            newState.put("isLowLiquidity", fund.isLowLiquidity());
            newState.put("minimumReserveThreshold", fund.getMinimumReserveThreshold().toPlainString());

            auditLog.setOldStateJson(objectMapper.writeValueAsString(oldState));
            auditLog.setNewStateJson(objectMapper.writeValueAsString(newState));

            auditLogRepository.save(auditLog);
        } catch (Exception e) {
            log.error("Failed to write audit log for fund {} action {}: {}", fund.getId(), action, e.getMessage(), e);
        }
    }

    private void validateGroupMembershipOrAdmin(OwnershipGroup group, User user) {
        if (user == null) {
            throw new BusinessException("User authentication required", HttpStatus.UNAUTHORIZED);
        }

        boolean isAdminOrStaff = user.getRoles().stream()
                .anyMatch(r -> RoleName.ROLE_ADMIN.equals(r.getName()) || RoleName.ROLE_STAFF.equals(r.getName()));
        if (isAdminOrStaff) {
            return;
        }

        boolean isMember = ownershipShareRepository.findByGroupIdAndUserId(group.getId(), user.getId())
                .map(share -> Boolean.TRUE.equals(share.getIsActive()))
                .orElse(false);

        if (!isMember) {
            log.warn("Access denied: User {} is not an active member of group {}", user.getId(), group.getId());
            throw new BusinessException("User is not an active co-owner of this syndicate group", HttpStatus.FORBIDDEN);
        }
    }

    private OwnershipGroup findGroupOrThrow(Long groupId) {
        if (groupId == null) {
            throw new BusinessException("Group ID cannot be null", HttpStatus.BAD_REQUEST);
        }
        return ownershipGroupRepository.findById(groupId)
                .orElseThrow(() -> new ResourceNotFoundException("Ownership group not found with id: " + groupId));
    }

    private User findUserOrThrow(Long userId) {
        if (userId == null) {
            throw new BusinessException("User ID cannot be null", HttpStatus.UNAUTHORIZED);
        }
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));
    }
}
