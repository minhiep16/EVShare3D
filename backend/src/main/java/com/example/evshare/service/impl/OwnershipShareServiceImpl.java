package com.example.evshare.service.impl;

import com.example.evshare.dto.request.IssueOwnershipShareRequest;
import com.example.evshare.dto.request.RebalanceSharesRequest;
import com.example.evshare.dto.request.TransferShareRequest;
import com.example.evshare.dto.request.UpdateOwnershipShareRequest;
import com.example.evshare.dto.response.OwnershipGroupResponse;
import com.example.evshare.dto.response.OwnershipHistoryResponse;
import com.example.evshare.dto.response.OwnershipShareResponse;
import com.example.evshare.entity.AuditLog;
import com.example.evshare.entity.OwnershipGroup;
import com.example.evshare.entity.OwnershipShare;
import com.example.evshare.entity.User;
import com.example.evshare.exception.BusinessException;
import com.example.evshare.exception.InvalidOwnershipDistributionException;
import com.example.evshare.exception.ResourceNotFoundException;
import com.example.evshare.repository.AuditLogRepository;
import com.example.evshare.repository.OwnershipGroupRepository;
import com.example.evshare.repository.OwnershipShareRepository;
import com.example.evshare.repository.UserRepository;
import com.example.evshare.security.UserPrincipal;
import com.example.evshare.service.OwnershipShareService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class OwnershipShareServiceImpl implements OwnershipShareService {

    private static final Logger log = LoggerFactory.getLogger(OwnershipShareServiceImpl.class);
    private static final BigDecimal TARGET_HUNDRED = new BigDecimal("100.00");

    private final OwnershipShareRepository ownershipShareRepository;
    private final OwnershipGroupRepository ownershipGroupRepository;
    private final UserRepository userRepository;
    private final AuditLogRepository auditLogRepository;
    private final ObjectMapper objectMapper;

    public OwnershipShareServiceImpl(OwnershipShareRepository ownershipShareRepository,
                                     OwnershipGroupRepository ownershipGroupRepository,
                                     UserRepository userRepository,
                                     AuditLogRepository auditLogRepository,
                                     ObjectMapper objectMapper) {
        this.ownershipShareRepository = ownershipShareRepository;
        this.ownershipGroupRepository = ownershipGroupRepository;
        this.userRepository = userRepository;
        this.auditLogRepository = auditLogRepository;
        this.objectMapper = objectMapper;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public OwnershipShareResponse issueShare(Long groupId, IssueOwnershipShareRequest request) {
        log.info("Issuing equity share in groupId={} for userId={} (percentage={}%)",
                groupId, request.getUserId(), request.getPercentage());

        // 1. Strict percentage validation (never allow negative, zero, or > 100%)
        validatePercentage(request.getPercentage());

        // 2. Acquire transactional pessimistic lock on OwnershipGroup to serialize equity modifications
        OwnershipGroup group = ownershipGroupRepository.findByIdForUpdate(groupId)
                .orElseThrow(() -> new ResourceNotFoundException(String.format("Ownership group not found with ID: %d", groupId)));

        if (Boolean.FALSE.equals(group.getIsActive())) {
            throw new BusinessException("Cannot issue equity shares for an inactive ownership group", HttpStatus.BAD_REQUEST);
        }

        // 3. Validate user existence
        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException(String.format("User not found with ID: %d", request.getUserId())));

        // 4. Enforce composite unique constraint (group_id, user_id)
        Optional<OwnershipShare> existingShareOpt = ownershipShareRepository.findByGroupIdAndUserId(groupId, request.getUserId());
        if (existingShareOpt.isPresent() && Boolean.TRUE.equals(existingShareOpt.get().getIsActive())) {
            throw new BusinessException(
                    String.format("User %d already holds an active equity share in ownership group %d", request.getUserId(), groupId),
                    HttpStatus.CONFLICT
            );
        }

        // 5. Enforce 100.00% equity invariant (BR-OWN-01): verify resulting total
        BigDecimal currentSum = ownershipShareRepository.sumActivePercentagesByGroupId(groupId);
        if (currentSum == null) {
            currentSum = BigDecimal.ZERO;
        }
        BigDecimal newTotal = currentSum.add(request.getPercentage());
        if (newTotal.compareTo(TARGET_HUNDRED) != 0) {
            String comparison = newTotal.compareTo(TARGET_HUNDRED) < 0 ? "less than" : "greater than";
            throw new InvalidOwnershipDistributionException(
                    String.format("Cannot issue ownership share: Total active ownership percentage for group %d must equal exactly 100.00%%, but would be %s%% (%s 100.00%%)",
                            groupId, newTotal, comparison),
                    groupId, newTotal
            );
        }

        // 6. Snapshot pre-mutation state for audit trail
        String oldStateJson = existingShareOpt.map(this::serializeShareSnapshot).orElse(null);
        String action = existingShareOpt.isPresent() ? "REACTIVATE_SHARE" : "ISSUE_SHARE";

        // 7. Persist new or reactivated OwnershipShare record
        OwnershipShare saved;
        if (existingShareOpt.isPresent()) {
            OwnershipShare existing = existingShareOpt.get();
            existing.setIsActive(true);
            existing.setPercentage(request.getPercentage());
            existing.setShareCertificateNumber(generateCertificateNumber(groupId, user.getId()));
            existing.setAcquiredAt(Instant.now());
            saved = ownershipShareRepository.save(existing);
        } else {
            OwnershipShare share = new OwnershipShare();
            share.setGroup(group);
            share.setUser(user);
            share.setPercentage(request.getPercentage());
            share.setShareCertificateNumber(generateCertificateNumber(groupId, user.getId()));
            share.setAcquiredAt(Instant.now());
            share.setIsActive(true);
            saved = ownershipShareRepository.save(share);
        }

        // 8. Immutable audit trail recording
        recordAudit(action, saved.getId(), oldStateJson, serializeShareSnapshot(saved));

        log.info("Equity share issued successfully: certNumber={}, percentage={}%",
                saved.getShareCertificateNumber(), saved.getPercentage());

        return OwnershipShareResponse.fromEntity(saved);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public OwnershipShareResponse updateShare(Long groupId, Long shareId, UpdateOwnershipShareRequest request) {
        log.info("Updating shareId={} in groupId={}", shareId, groupId);

        // 1. Transactional pessimistic lock on group and share
        ownershipGroupRepository.findByIdForUpdate(groupId)
                .orElseThrow(() -> new ResourceNotFoundException(String.format("Ownership group not found with ID: %d", groupId)));

        OwnershipShare share = ownershipShareRepository.findByIdForUpdate(shareId)
                .orElseThrow(() -> new ResourceNotFoundException(String.format("Ownership share not found with ID: %d", shareId)));

        if (!share.getGroup().getId().equals(groupId)) {
            throw new BusinessException("Share does not belong to the specified ownership group", HttpStatus.BAD_REQUEST);
        }

        // 2. Validate percentage bounds if provided
        if (request.getPercentage() != null) {
            validatePercentage(request.getPercentage());
        }

        // 3. Enforce 100.00% equity invariant (BR-OWN-01) on update
        BigDecimal currentSum = ownershipShareRepository.sumActivePercentagesByGroupId(groupId);
        if (currentSum == null) {
            currentSum = BigDecimal.ZERO;
        }

        BigDecimal targetPercentage = request.getPercentage() != null ? request.getPercentage() : share.getPercentage();
        boolean targetActive = request.getIsActive() != null ? request.getIsActive() : Boolean.TRUE.equals(share.getIsActive());

        BigDecimal newTotal;
        if (Boolean.TRUE.equals(share.getIsActive())) {
            if (targetActive) {
                newTotal = currentSum.subtract(share.getPercentage()).add(targetPercentage);
            } else {
                newTotal = currentSum.subtract(share.getPercentage());
            }
        } else {
            if (targetActive) {
                newTotal = currentSum.add(targetPercentage);
            } else {
                newTotal = currentSum;
            }
        }

        if (newTotal.compareTo(TARGET_HUNDRED) != 0) {
            String comparison = newTotal.compareTo(TARGET_HUNDRED) < 0 ? "less than" : "greater than";
            throw new InvalidOwnershipDistributionException(
                    String.format("Cannot update ownership share %d: Total active ownership percentage for group %d must equal exactly 100.00%%, but would be %s%% (%s 100.00%%)",
                            shareId, groupId, newTotal, comparison),
                    groupId, newTotal
            );
        }

        // 4. Capture pre-mutation snapshot to ensure historical records remain auditable without silent overwriting
        String oldStateJson = serializeShareSnapshot(share);

        if (request.getPercentage() != null) {
            share.setPercentage(request.getPercentage());
            share.setShareCertificateNumber(generateCertificateNumber(groupId, share.getUser().getId()));
            share.setAcquiredAt(Instant.now());
        }
        if (request.getIsActive() != null) {
            share.setIsActive(request.getIsActive());
        }

        OwnershipShare saved = ownershipShareRepository.save(share);

        // 5. Immutable audit trail logging
        recordAudit("UPDATE_SHARE", saved.getId(), oldStateJson, serializeShareSnapshot(saved));

        log.info("Share updated successfully: shareId={}, percentage={}%, isActive={}",
                saved.getId(), saved.getPercentage(), saved.getIsActive());

        return OwnershipShareResponse.fromEntity(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public OwnershipShareResponse getShareById(Long shareId) {
        OwnershipShare share = ownershipShareRepository.findById(shareId)
                .orElseThrow(() -> new ResourceNotFoundException(String.format("Ownership share not found with ID: %d", shareId)));
        return OwnershipShareResponse.fromEntity(share);
    }

    @Override
    @Transactional(readOnly = true)
    public List<OwnershipShareResponse> getSharesByGroup(Long groupId, Boolean activeOnly) {
        if (!ownershipGroupRepository.existsById(groupId)) {
            throw new ResourceNotFoundException(String.format("Ownership group not found with ID: %d", groupId));
        }

        List<OwnershipShare> shares = Boolean.TRUE.equals(activeOnly)
                ? ownershipShareRepository.findByGroupIdAndIsActiveTrue(groupId)
                : ownershipShareRepository.findByGroupId(groupId);

        return shares.stream()
                .map(OwnershipShareResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deactivateShare(Long groupId, Long shareId) {
        log.info("Deactivating shareId={} in groupId={}", shareId, groupId);
        ownershipGroupRepository.findByIdForUpdate(groupId)
                .orElseThrow(() -> new ResourceNotFoundException(String.format("Ownership group not found with ID: %d", groupId)));

        OwnershipShare share = ownershipShareRepository.findByIdForUpdate(shareId)
                .orElseThrow(() -> new ResourceNotFoundException(String.format("Ownership share not found with ID: %d", shareId)));

        if (!share.getGroup().getId().equals(groupId)) {
            throw new BusinessException("Share does not belong to the specified ownership group", HttpStatus.BAD_REQUEST);
        }

        // Enforce 100.00% equity invariant (BR-OWN-01): prevent invalid final state
        if (Boolean.TRUE.equals(share.getIsActive())) {
            BigDecimal currentSum = ownershipShareRepository.sumActivePercentagesByGroupId(groupId);
            if (currentSum == null) {
                currentSum = BigDecimal.ZERO;
            }
            BigDecimal newTotal = currentSum.subtract(share.getPercentage());
            if (newTotal.compareTo(TARGET_HUNDRED) != 0) {
                throw new InvalidOwnershipDistributionException(
                        String.format("Cannot remove ownership share %d: Total active ownership percentage for group %d must equal exactly 100.00%%, but would be %s%% (less than 100.00%%). Removing active share violates 100%% equity invariant.",
                                shareId, groupId, newTotal),
                        groupId, newTotal
                );
            }
        }

        // Capture immutable snapshot prior to deactivation
        String oldStateJson = serializeShareSnapshot(share);

        share.setIsActive(false);
        OwnershipShare saved = ownershipShareRepository.save(share);

        recordAudit("DEACTIVATE_SHARE", saved.getId(), oldStateJson, serializeShareSnapshot(saved));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void reactivateShare(Long groupId, Long shareId) {
        log.info("Reactivating shareId={} in groupId={}", shareId, groupId);
        ownershipGroupRepository.findByIdForUpdate(groupId)
                .orElseThrow(() -> new ResourceNotFoundException(String.format("Ownership group not found with ID: %d", groupId)));

        OwnershipShare share = ownershipShareRepository.findByIdForUpdate(shareId)
                .orElseThrow(() -> new ResourceNotFoundException(String.format("Ownership share not found with ID: %d", shareId)));

        if (!share.getGroup().getId().equals(groupId)) {
            throw new BusinessException("Share does not belong to the specified ownership group", HttpStatus.BAD_REQUEST);
        }

        if (Boolean.FALSE.equals(share.getIsActive())) {
            BigDecimal currentSum = ownershipShareRepository.sumActivePercentagesByGroupId(groupId);
            if (currentSum == null) {
                currentSum = BigDecimal.ZERO;
            }
            BigDecimal newTotal = currentSum.add(share.getPercentage());
            if (newTotal.compareTo(TARGET_HUNDRED) != 0) {
                String comparison = newTotal.compareTo(TARGET_HUNDRED) < 0 ? "less than" : "greater than";
                throw new InvalidOwnershipDistributionException(
                        String.format("Cannot reactivate ownership share %d: Total active ownership percentage for group %d must equal exactly 100.00%%, but would be %s%% (%s 100.00%%)",
                                shareId, groupId, newTotal, comparison),
                        groupId, newTotal
                );
            }
        }

        // Capture immutable snapshot prior to reactivation
        String oldStateJson = serializeShareSnapshot(share);

        share.setIsActive(true);
        share.setAcquiredAt(Instant.now());
        OwnershipShare saved = ownershipShareRepository.save(share);

        recordAudit("REACTIVATE_SHARE", saved.getId(), oldStateJson, serializeShareSnapshot(saved));
    }

    @Override
    @Transactional(readOnly = true)
    public BigDecimal validateOwnershipDistribution(Long groupId) {
        if (!ownershipGroupRepository.existsById(groupId)) {
            throw new ResourceNotFoundException(String.format("Ownership group not found with ID: %d", groupId));
        }

        BigDecimal total = ownershipShareRepository.sumActivePercentagesByGroupId(groupId);
        if (total == null) {
            total = BigDecimal.ZERO;
        }

        if (total.compareTo(TARGET_HUNDRED) != 0) {
            throw new InvalidOwnershipDistributionException(groupId, total);
        }

        return total;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public OwnershipGroupResponse transferShare(Long groupId, TransferShareRequest request) {
        log.info("Transferring {}% equity in groupId={} from userId={} to userId={}",
                request.getPercentage(), groupId, request.getFromUserId(), request.getToUserId());

        validatePercentage(request.getPercentage());

        if (request.getFromUserId().equals(request.getToUserId())) {
            throw new BusinessException("Seller and buyer cannot be the same user", HttpStatus.BAD_REQUEST);
        }

        OwnershipGroup group = ownershipGroupRepository.findByIdForUpdate(groupId)
                .orElseThrow(() -> new ResourceNotFoundException(String.format("Ownership group not found with ID: %d", groupId)));

        User seller = userRepository.findById(request.getFromUserId())
                .orElseThrow(() -> new ResourceNotFoundException(String.format("Seller user not found with ID: %d", request.getFromUserId())));

        User buyer = userRepository.findById(request.getToUserId())
                .orElseThrow(() -> new ResourceNotFoundException(String.format("Buyer user not found with ID: %d", request.getToUserId())));

        OwnershipShare sellerShare = ownershipShareRepository.findByGroupIdAndUserId(groupId, seller.getId())
                .orElseThrow(() -> new BusinessException(String.format("User %d is not a member of group %d", seller.getId(), groupId), HttpStatus.BAD_REQUEST));

        if (!Boolean.TRUE.equals(sellerShare.getIsActive())) {
            throw new BusinessException(String.format("Seller user %d does not hold an active equity share", seller.getId()), HttpStatus.BAD_REQUEST);
        }

        if (sellerShare.getPercentage().compareTo(request.getPercentage()) < 0) {
            throw new BusinessException(
                    String.format("Seller user %d holds only %s%%, which is insufficient for transfer of %s%%",
                            seller.getId(), sellerShare.getPercentage(), request.getPercentage()),
                    HttpStatus.BAD_REQUEST
            );
        }

        // Adjust seller share with audit logging
        String sellerOldJson = serializeShareSnapshot(sellerShare);
        BigDecimal newSellerPercentage = sellerShare.getPercentage().subtract(request.getPercentage());
        if (newSellerPercentage.compareTo(BigDecimal.ZERO) == 0) {
            // Full divestment: deactivate seller share
            sellerShare.setIsActive(false);
        } else {
            sellerShare.setPercentage(newSellerPercentage);
            sellerShare.setShareCertificateNumber(generateCertificateNumber(groupId, seller.getId()));
            sellerShare.setAcquiredAt(Instant.now());
        }
        OwnershipShare savedSeller = ownershipShareRepository.save(sellerShare);
        recordAudit("TRANSFER_EQUITY_OUT", savedSeller.getId(), sellerOldJson, serializeShareSnapshot(savedSeller));

        // Adjust buyer share with audit logging
        Optional<OwnershipShare> buyerShareOpt = ownershipShareRepository.findByGroupIdAndUserId(groupId, buyer.getId());
        if (buyerShareOpt.isPresent()) {
            OwnershipShare buyerShare = buyerShareOpt.get();
            String buyerOldJson = serializeShareSnapshot(buyerShare);
            if (Boolean.TRUE.equals(buyerShare.getIsActive())) {
                buyerShare.setPercentage(buyerShare.getPercentage().add(request.getPercentage()));
            } else {
                buyerShare.setIsActive(true);
                buyerShare.setPercentage(request.getPercentage());
            }
            buyerShare.setShareCertificateNumber(generateCertificateNumber(groupId, buyer.getId()));
            buyerShare.setAcquiredAt(Instant.now());
            OwnershipShare savedBuyer = ownershipShareRepository.save(buyerShare);
            recordAudit("TRANSFER_EQUITY_IN", savedBuyer.getId(), buyerOldJson, serializeShareSnapshot(savedBuyer));
        } else {
            OwnershipShare newBuyerShare = new OwnershipShare();
            newBuyerShare.setGroup(group);
            newBuyerShare.setUser(buyer);
            newBuyerShare.setPercentage(request.getPercentage());
            newBuyerShare.setShareCertificateNumber(generateCertificateNumber(groupId, buyer.getId()));
            newBuyerShare.setAcquiredAt(Instant.now());
            newBuyerShare.setIsActive(true);
            OwnershipShare savedBuyer = ownershipShareRepository.save(newBuyerShare);
            recordAudit("TRANSFER_EQUITY_IN", savedBuyer.getId(), null, serializeShareSnapshot(savedBuyer));
        }

        // Verify mathematical invariant
        validateOwnershipDistribution(groupId);

        List<OwnershipShare> activeShares = ownershipShareRepository.findByGroupIdAndIsActiveTrue(groupId);
        log.info("Equity transfer completed successfully in groupId={}. Active members: {}", groupId, activeShares.size());

        return OwnershipGroupResponse.fromEntity(group, activeShares);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public OwnershipGroupResponse rebalanceShares(Long groupId, RebalanceSharesRequest request) {
        log.info("Rebalancing equity shares for groupId={} (allocations count={})", groupId, request.getAllocations().size());

        OwnershipGroup group = ownershipGroupRepository.findByIdForUpdate(groupId)
                .orElseThrow(() -> new ResourceNotFoundException(String.format("Ownership group not found with ID: %d", groupId)));

        // 1. Verify mathematical sum of allocations equals exactly 100.00%
        BigDecimal totalAllocation = BigDecimal.ZERO;
        Set<Long> userIds = new HashSet<>();
        for (RebalanceSharesRequest.ShareAllocation alloc : request.getAllocations()) {
            validatePercentage(alloc.getPercentage());
            if (!userIds.add(alloc.getUserId())) {
                throw new BusinessException(String.format("Duplicate user ID %d in rebalance allocations", alloc.getUserId()), HttpStatus.BAD_REQUEST);
            }
            totalAllocation = totalAllocation.add(alloc.getPercentage());
        }

        if (totalAllocation.compareTo(TARGET_HUNDRED) != 0) {
            String comparison = totalAllocation.compareTo(TARGET_HUNDRED) < 0 ? "less than" : "greater than";
            throw new InvalidOwnershipDistributionException(
                    String.format("Rebalance failed: Total allocations must sum to exactly 100.00%%, but found %s%% (%s 100.00%%)",
                            totalAllocation, comparison),
                    groupId, totalAllocation
            );
        }

        // 2. Fetch existing group shares
        List<OwnershipShare> existingShares = ownershipShareRepository.findByGroupId(groupId);
        Map<Long, OwnershipShare> existingShareMap = existingShares.stream()
                .collect(Collectors.toMap(s -> s.getUser().getId(), s -> s));

        // 3. Apply allocations with audit tracking
        for (RebalanceSharesRequest.ShareAllocation alloc : request.getAllocations()) {
            User user = userRepository.findById(alloc.getUserId())
                    .orElseThrow(() -> new ResourceNotFoundException(String.format("User not found with ID: %d", alloc.getUserId())));

            if (existingShareMap.containsKey(alloc.getUserId())) {
                OwnershipShare share = existingShareMap.get(alloc.getUserId());
                String oldJson = serializeShareSnapshot(share);
                share.setPercentage(alloc.getPercentage());
                share.setIsActive(true);
                share.setShareCertificateNumber(generateCertificateNumber(groupId, user.getId()));
                share.setAcquiredAt(Instant.now());
                OwnershipShare saved = ownershipShareRepository.save(share);
                recordAudit("REBALANCE_SHARE", saved.getId(), oldJson, serializeShareSnapshot(saved));
            } else {
                OwnershipShare share = new OwnershipShare();
                share.setGroup(group);
                share.setUser(user);
                share.setPercentage(alloc.getPercentage());
                share.setShareCertificateNumber(generateCertificateNumber(groupId, user.getId()));
                share.setAcquiredAt(Instant.now());
                share.setIsActive(true);
                OwnershipShare saved = ownershipShareRepository.save(share);
                recordAudit("REBALANCE_SHARE", saved.getId(), null, serializeShareSnapshot(saved));
            }
        }

        // 4. Deactivate existing active shares not present in new allocation
        for (OwnershipShare share : existingShares) {
            if (!userIds.contains(share.getUser().getId()) && Boolean.TRUE.equals(share.getIsActive())) {
                String oldJson = serializeShareSnapshot(share);
                share.setIsActive(false);
                OwnershipShare saved = ownershipShareRepository.save(share);
                recordAudit("REBALANCE_DEACTIVATE", saved.getId(), oldJson, serializeShareSnapshot(saved));
            }
        }

        // 5. Final invariant verification
        validateOwnershipDistribution(groupId);

        List<OwnershipShare> activeShares = ownershipShareRepository.findByGroupIdAndIsActiveTrue(groupId);
        return OwnershipGroupResponse.fromEntity(group, activeShares);
    }

    @Override
    @Transactional(readOnly = true)
    public List<OwnershipHistoryResponse> getShareHistory(Long groupId, Long shareId) {
        OwnershipShare share = ownershipShareRepository.findById(shareId)
                .orElseThrow(() -> new ResourceNotFoundException(String.format("Ownership share not found with ID: %d", shareId)));

        if (!share.getGroup().getId().equals(groupId)) {
            throw new BusinessException("Share does not belong to the specified ownership group", HttpStatus.BAD_REQUEST);
        }

        List<AuditLog> auditLogs = auditLogRepository.findByEntityNameAndEntityIdOrderByIdDesc("OwnershipShare", shareId);
        return auditLogs.stream()
                .map(this::mapToHistoryResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<OwnershipHistoryResponse> getGroupOwnershipHistory(Long groupId) {
        if (!ownershipGroupRepository.existsById(groupId)) {
            throw new ResourceNotFoundException(String.format("Ownership group not found with ID: %d", groupId));
        }

        List<OwnershipShare> shares = ownershipShareRepository.findByGroupId(groupId);
        if (shares.isEmpty()) {
            return Collections.emptyList();
        }

        List<Long> shareIds = shares.stream().map(OwnershipShare::getId).collect(Collectors.toList());
        List<AuditLog> auditLogs = auditLogRepository.findByEntityNameAndEntityIdInOrderByIdDesc("OwnershipShare", shareIds);
        return auditLogs.stream()
                .map(this::mapToHistoryResponse)
                .collect(Collectors.toList());
    }

    private String serializeShareSnapshot(OwnershipShare share) {
        if (share == null) {
            return null;
        }
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", share.getId());
        map.put("groupId", share.getGroup() != null ? share.getGroup().getId() : null);
        map.put("userId", share.getUser() != null ? share.getUser().getId() : null);
        map.put("percentage", share.getPercentage());
        map.put("shareCertificateNumber", share.getShareCertificateNumber());
        map.put("acquiredAt", share.getAcquiredAt() != null ? share.getAcquiredAt().toString() : null);
        map.put("isActive", share.getIsActive());
        try {
            return objectMapper.writeValueAsString(map);
        } catch (Exception e) {
            log.error("Failed to serialize share snapshot for shareId={}", share.getId(), e);
            return "{}";
        }
    }

    private void recordAudit(String action, Long shareId, String oldStateJson, String newStateJson) {
        User currentUser = getCurrentUserOrNull();
        AuditLog logEntry = new AuditLog();
        logEntry.setUser(currentUser);
        logEntry.setAction(action);
        logEntry.setEntityName("OwnershipShare");
        logEntry.setEntityId(shareId);
        logEntry.setOldStateJson(oldStateJson);
        logEntry.setNewStateJson(newStateJson);
        logEntry.setCreatedAt(Instant.now());
        auditLogRepository.save(logEntry);
    }

    private User getCurrentUserOrNull() {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.getPrincipal() instanceof UserPrincipal principal) {
                return userRepository.findById(principal.getId()).orElse(null);
            }
        } catch (Exception e) {
            log.debug("No authenticated user in context for audit log: {}", e.getMessage());
        }
        return null;
    }

    private OwnershipHistoryResponse mapToHistoryResponse(AuditLog logEntry) {
        OwnershipHistoryResponse resp = new OwnershipHistoryResponse();
        resp.setId(logEntry.getId());
        resp.setShareId(logEntry.getEntityId());
        resp.setAction(logEntry.getAction());
        resp.setActingUserId(logEntry.getUser() != null ? logEntry.getUser().getId() : null);
        resp.setEffectiveDate(logEntry.getCreatedAt());
        resp.setOldStateJson(logEntry.getOldStateJson());
        resp.setNewStateJson(logEntry.getNewStateJson());

        if (logEntry.getOldStateJson() != null) {
            try {
                JsonNode oldNode = objectMapper.readTree(logEntry.getOldStateJson());
                if (oldNode.has("percentage") && !oldNode.get("percentage").isNull()) {
                    resp.setPreviousPercentage(new BigDecimal(oldNode.get("percentage").asText()));
                }
                if (oldNode.has("shareCertificateNumber") && !oldNode.get("shareCertificateNumber").isNull()) {
                    resp.setPreviousCertificateNumber(oldNode.get("shareCertificateNumber").asText());
                }
                if (oldNode.has("isActive") && !oldNode.get("isActive").isNull()) {
                    resp.setPreviousIsActive(oldNode.get("isActive").asBoolean());
                }
            } catch (Exception e) {
                log.warn("Failed to parse oldStateJson for auditLogId={}: {}", logEntry.getId(), e.getMessage());
            }
        }

        if (logEntry.getNewStateJson() != null) {
            try {
                JsonNode newNode = objectMapper.readTree(logEntry.getNewStateJson());
                if (newNode.has("percentage") && !newNode.get("percentage").isNull()) {
                    resp.setNewPercentage(new BigDecimal(newNode.get("percentage").asText()));
                }
                if (newNode.has("shareCertificateNumber") && !newNode.get("shareCertificateNumber").isNull()) {
                    resp.setNewCertificateNumber(newNode.get("shareCertificateNumber").asText());
                }
                if (newNode.has("isActive") && !newNode.get("isActive").isNull()) {
                    resp.setNewIsActive(newNode.get("isActive").asBoolean());
                }
            } catch (Exception e) {
                log.warn("Failed to parse newStateJson for auditLogId={}: {}", logEntry.getId(), e.getMessage());
            }
        }

        return resp;
    }

    private void validatePercentage(BigDecimal percentage) {
        if (percentage == null) {
            throw new BusinessException("Ownership percentage cannot be null", HttpStatus.BAD_REQUEST);
        }
        if (percentage.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException(
                    String.format("Ownership percentage must be strictly greater than 0.00%% (received %s)", percentage),
                    HttpStatus.BAD_REQUEST
            );
        }
        if (percentage.compareTo(TARGET_HUNDRED) > 0) {
            throw new BusinessException(
                    String.format("Ownership percentage cannot exceed 100.00%% (received %s)", percentage),
                    HttpStatus.BAD_REQUEST
            );
        }
        if (percentage.scale() > 2) {
            throw new BusinessException(
                    String.format("Ownership percentage cannot exceed 2 decimal places (received %s)", percentage),
                    HttpStatus.BAD_REQUEST
            );
        }
    }

    private String generateCertificateNumber(Long groupId, Long userId) {
        return String.format("CERT-G%d-U%d-%s", groupId, userId, UUID.randomUUID().toString().substring(0, 8).toUpperCase());
    }
}
