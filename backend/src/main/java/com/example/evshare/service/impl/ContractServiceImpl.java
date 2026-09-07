package com.example.evshare.service.impl;

import com.example.evshare.dto.request.CreateContractRequest;
import com.example.evshare.dto.request.SignContractRequest;
import com.example.evshare.dto.request.TransitionContractStatusRequest;
import com.example.evshare.dto.request.UpdateContractRequest;
import com.example.evshare.dto.response.ContractResponse;
import com.example.evshare.dto.response.ContractSignatureResponse;
import com.example.evshare.dto.response.ContractSignaturesOverviewResponse;
import com.example.evshare.entity.AuditLog;
import com.example.evshare.entity.CoOwnershipContract;
import com.example.evshare.entity.ContractSignature;
import com.example.evshare.entity.OwnershipGroup;
import com.example.evshare.entity.OwnershipShare;
import com.example.evshare.entity.User;
import com.example.evshare.entity.enums.ContractStatus;
import com.example.evshare.exception.BusinessException;
import com.example.evshare.exception.InvalidContractTransitionException;
import com.example.evshare.exception.ResourceNotFoundException;
import com.example.evshare.repository.AuditLogRepository;
import com.example.evshare.repository.CoOwnershipContractRepository;
import com.example.evshare.repository.ContractSignatureRepository;
import com.example.evshare.repository.OwnershipGroupRepository;
import com.example.evshare.repository.OwnershipShareRepository;
import com.example.evshare.repository.UserRepository;
import com.example.evshare.security.UserPrincipal;
import com.example.evshare.service.ContractService;
import com.example.evshare.service.ContractStateMachine;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ContractServiceImpl implements ContractService {

    private static final Logger log = LoggerFactory.getLogger(ContractServiceImpl.class);

    private final CoOwnershipContractRepository coOwnershipContractRepository;
    private final OwnershipGroupRepository ownershipGroupRepository;
    private final ContractSignatureRepository contractSignatureRepository;
    private final OwnershipShareRepository ownershipShareRepository;
    private final ContractStateMachine contractStateMachine;
    private final AuditLogRepository auditLogRepository;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;

    public ContractServiceImpl(CoOwnershipContractRepository coOwnershipContractRepository,
                               OwnershipGroupRepository ownershipGroupRepository,
                               ContractSignatureRepository contractSignatureRepository,
                               OwnershipShareRepository ownershipShareRepository,
                               ContractStateMachine contractStateMachine,
                               AuditLogRepository auditLogRepository,
                               UserRepository userRepository,
                               ObjectMapper objectMapper) {
        this.coOwnershipContractRepository = coOwnershipContractRepository;
        this.ownershipGroupRepository = ownershipGroupRepository;
        this.contractSignatureRepository = contractSignatureRepository;
        this.ownershipShareRepository = ownershipShareRepository;
        this.contractStateMachine = contractStateMachine;
        this.auditLogRepository = auditLogRepository;
        this.userRepository = userRepository;
        this.objectMapper = objectMapper;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ContractResponse createContract(CreateContractRequest request) {
        log.info("Creating co-ownership contract for groupId='{}', title='{}'", request.getGroupId(), request.getContractTitle());

        OwnershipGroup group = ownershipGroupRepository.findById(request.getGroupId())
                .orElseThrow(() -> new ResourceNotFoundException(String.format("Ownership group not found with ID: %d", request.getGroupId())));

        if (Boolean.FALSE.equals(group.getIsActive())) {
            throw new BusinessException("Cannot create contract for an inactive ownership group", HttpStatus.BAD_REQUEST);
        }

        validateDates(request.getEffectiveDate(), request.getExpiryDate());

        int nextVersion = coOwnershipContractRepository.findTopByGroupIdOrderByVersionDesc(request.getGroupId())
                .map(latest -> latest.getVersion() + 1)
                .orElse(1);

        CoOwnershipContract contract = new CoOwnershipContract();
        contract.setGroup(group);
        contract.setContractTitle(request.getContractTitle().trim());
        contract.setContractTermsText(request.getContractTermsText().trim());
        contract.setVersion(nextVersion);
        contract.setStatus(ContractStatus.DRAFT);
        contract.setEffectiveDate(request.getEffectiveDate());
        contract.setExpiryDate(request.getExpiryDate());
        contract.setCreatedAt(Instant.now());

        CoOwnershipContract saved = coOwnershipContractRepository.save(contract);
        recordAudit("CREATE_CONTRACT", saved.getId(), null, serializeContractSnapshot(saved));

        log.info("Successfully created contract ID={} version={} for groupId={}", saved.getId(), saved.getVersion(), group.getId());
        return ContractResponse.fromEntity(saved);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ContractResponse updateDraftContract(Long contractId, UpdateContractRequest request) {
        log.info("Updating draft contract ID={}", contractId);

        CoOwnershipContract contract = coOwnershipContractRepository.findByIdForUpdate(contractId)
                .orElseThrow(() -> new ResourceNotFoundException(String.format("Contract not found with ID: %d", contractId)));

        if (contract.getStatus() != ContractStatus.DRAFT) {
            throw new BusinessException(
                    String.format("Contract version %d cannot be edited because it is in status '%s'. Only DRAFT contracts can be modified. Non-draft contracts are legally immutable; create a new contract version for amendments.",
                            contract.getVersion(), contract.getStatus()),
                    HttpStatus.CONFLICT
            );
        }

        validateDates(request.getEffectiveDate(), request.getExpiryDate());

        String oldJson = serializeContractSnapshot(contract);

        contract.setContractTitle(request.getContractTitle().trim());
        contract.setContractTermsText(request.getContractTermsText().trim());
        contract.setEffectiveDate(request.getEffectiveDate());
        contract.setExpiryDate(request.getExpiryDate());

        CoOwnershipContract saved = coOwnershipContractRepository.save(contract);
        recordAudit("UPDATE_DRAFT_CONTRACT", saved.getId(), oldJson, serializeContractSnapshot(saved));

        log.info("Successfully updated draft contract ID={}", saved.getId());
        return ContractResponse.fromEntity(saved);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ContractResponse transitionStatus(Long contractId, TransitionContractStatusRequest request) {
        log.info("Transitioning contract ID={} to target status '{}' (reason='{}')", contractId, request.getTargetStatus(), request.getReason());

        CoOwnershipContract contract = coOwnershipContractRepository.findByIdForUpdate(contractId)
                .orElseThrow(() -> new ResourceNotFoundException(String.format("Contract not found with ID: %d", contractId)));

        ContractStatus currentStatus = contract.getStatus();
        ContractStatus targetStatus = request.getTargetStatus();

        validateStateTransition(currentStatus, targetStatus);

        // If activating this version, supersede any currently active contract for this group
        if (targetStatus == ContractStatus.ACTIVE) {
            if (contract.getEffectiveDate() == null) {
                contract.setEffectiveDate(LocalDate.now());
            }

            Optional<CoOwnershipContract> existingActiveOpt = coOwnershipContractRepository.findByGroupIdAndStatus(contract.getGroup().getId(), ContractStatus.ACTIVE);
            if (existingActiveOpt.isPresent() && !existingActiveOpt.get().getId().equals(contract.getId())) {
                CoOwnershipContract existingActive = existingActiveOpt.get();
                String activeOldJson = serializeContractSnapshot(existingActive);
                existingActive.setStatus(ContractStatus.TERMINATED);
                CoOwnershipContract superseded = coOwnershipContractRepository.save(existingActive);
                recordAudit("TERMINATE_CONTRACT", superseded.getId(), activeOldJson, serializeContractSnapshot(superseded));
                log.info("Previous active contract ID={} version={} terminated (superseded by version={})",
                        superseded.getId(), superseded.getVersion(), contract.getVersion());
            }
        }

        String oldJson = serializeContractSnapshot(contract);
        contract.setStatus(targetStatus);

        CoOwnershipContract saved = coOwnershipContractRepository.save(contract);
        recordAudit("TRANSITION_CONTRACT_STATUS", saved.getId(), oldJson, serializeContractSnapshot(saved));

        log.info("Contract ID={} transitioned from '{}' to '{}'", saved.getId(), currentStatus, targetStatus);
        return ContractResponse.fromEntity(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public ContractResponse getContractById(Long contractId) {
        CoOwnershipContract contract = coOwnershipContractRepository.findById(contractId)
                .orElseThrow(() -> new ResourceNotFoundException(String.format("Contract not found with ID: %d", contractId)));
        return ContractResponse.fromEntity(contract);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ContractResponse> getContractsByGroupId(Long groupId) {
        if (!ownershipGroupRepository.existsById(groupId)) {
            throw new ResourceNotFoundException(String.format("Ownership group not found with ID: %d", groupId));
        }

        return coOwnershipContractRepository.findByGroupIdOrderByVersionDesc(groupId)
                .stream()
                .map(ContractResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public ContractResponse getActiveContractByGroupId(Long groupId) {
        if (!ownershipGroupRepository.existsById(groupId)) {
            throw new ResourceNotFoundException(String.format("Ownership group not found with ID: %d", groupId));
        }

        return coOwnershipContractRepository.findByGroupIdAndStatus(groupId, ContractStatus.ACTIVE)
                .map(ContractResponse::fromEntity)
                .orElse(null);
    }

    @Override
    public void deleteContract(Long contractId) {
        log.warn("Attempt to delete historical contract record ID={}", contractId);
        throw new BusinessException(
                "Historical contract data cannot be deleted. Historical co-ownership records must remain immutable and auditable. You may transition the contract to TERMINATED instead.",
                HttpStatus.BAD_REQUEST
        );
    }

    private void validateStateTransition(ContractStatus current, ContractStatus target) {
        contractStateMachine.validateTransition(current, target);
    }

    private void validateDates(LocalDate effectiveDate, LocalDate expiryDate) {
        if (effectiveDate != null && expiryDate != null && expiryDate.isBefore(effectiveDate)) {
            throw new BusinessException("Contract expiry date cannot be before effective date", HttpStatus.BAD_REQUEST);
        }
    }

    private String serializeContractSnapshot(CoOwnershipContract contract) {
        if (contract == null) {
            return null;
        }
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", contract.getId());
        map.put("groupId", contract.getGroup() != null ? contract.getGroup().getId() : null);
        map.put("contractTitle", contract.getContractTitle());
        map.put("version", contract.getVersion());
        map.put("status", contract.getStatus() != null ? contract.getStatus().name() : null);
        map.put("effectiveDate", contract.getEffectiveDate() != null ? contract.getEffectiveDate().toString() : null);
        map.put("expiryDate", contract.getExpiryDate() != null ? contract.getExpiryDate().toString() : null);
        map.put("createdAt", contract.getCreatedAt() != null ? contract.getCreatedAt().toString() : null);
        try {
            return objectMapper.writeValueAsString(map);
        } catch (Exception e) {
            log.error("Failed to serialize contract snapshot for ID={}", contract.getId(), e);
            return "{}";
        }
    }

    private void recordAudit(String action, Long contractId, String oldStateJson, String newStateJson) {
        User currentUser = getCurrentUserOrNull();
        AuditLog logEntry = new AuditLog();
        logEntry.setUser(currentUser);
        logEntry.setAction(action);
        logEntry.setEntityName("CoOwnershipContract");
        logEntry.setEntityId(contractId);
        logEntry.setOldStateJson(oldStateJson);
        logEntry.setNewStateJson(newStateJson);
        logEntry.setCreatedAt(Instant.now());
        auditLogRepository.save(logEntry);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ContractSignatureResponse signContract(Long contractId, Long userId, SignContractRequest request, String ipAddress) {
        if (request == null || !Boolean.TRUE.equals(request.getAcceptTerms())) {
            throw new BusinessException("Contract terms must be explicitly acknowledged and accepted", HttpStatus.BAD_REQUEST);
        }

        CoOwnershipContract contract = coOwnershipContractRepository.findByIdForUpdate(contractId)
                .orElseThrow(() -> new ResourceNotFoundException(String.format("Contract not found with id: %d", contractId)));

        // 1. Contract Status validation
        if (contract.getStatus() == ContractStatus.DRAFT) {
            throw new BusinessException("Contract is in DRAFT status and is not yet open for signatures. It must be transitioned to PENDING_SIGNATURE first.",
                    HttpStatus.BAD_REQUEST);
        }
        if (contract.getStatus() != ContractStatus.PENDING_SIGNATURE) {
            throw new BusinessException(String.format("Contract is in %s status and cannot be signed.", contract.getStatus()),
                    HttpStatus.CONFLICT);
        }

        // 2. Signer authorization: must hold an active equity share in the contract's ownership group
        Long groupId = contract.getGroup().getId();
        Optional<OwnershipShare> shareOpt = ownershipShareRepository.findByGroupIdAndUserId(groupId, userId);
        if (shareOpt.isEmpty() || !Boolean.TRUE.equals(shareOpt.get().getIsActive())) {
            throw new BusinessException(String.format("User [%d] is not an active co-owner in ownership group [%d] for this contract", userId, groupId),
                    HttpStatus.FORBIDDEN);
        }

        // 3. Duplicate signature prevention
        if (contractSignatureRepository.existsByContractIdAndUserId(contractId, userId)) {
            throw new BusinessException(String.format("User [%d] has already signed contract version %d (Contract ID: %d)",
                    userId, contract.getVersion(), contractId), HttpStatus.CONFLICT);
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException(String.format("User not found with id: %d", userId)));

        // 4. Timestamp & Cryptographic Hash computation
        Instant signedAt = Instant.now();
        String safeIp = (ipAddress != null && !ipAddress.isBlank()) ? ipAddress : "127.0.0.1";
        String termsHash = sha256Hex(contract.getContractTermsText() != null ? contract.getContractTermsText() : "");
        String dataToHash = contractId + ":" + contract.getVersion() + ":" + userId + ":" + safeIp + ":" + signedAt.toEpochMilli() + ":" + termsHash;
        String signatureHash = sha256Hex(dataToHash);

        ContractSignature signature = new ContractSignature(
                null,
                contract,
                user,
                signatureHash,
                signedAt,
                safeIp
        );
        ContractSignature savedSignature = contractSignatureRepository.save(signature);

        // 5. Audit log for SIGN_CONTRACT
        Map<String, Object> signatureLogMap = new LinkedHashMap<>();
        signatureLogMap.put("signatureId", savedSignature.getId());
        signatureLogMap.put("contractId", contractId);
        signatureLogMap.put("contractVersion", contract.getVersion());
        signatureLogMap.put("userId", userId);
        signatureLogMap.put("userEmail", user.getEmail());
        signatureLogMap.put("signatureHash", signatureHash);
        signatureLogMap.put("signedAt", signedAt.toString());
        signatureLogMap.put("ipAddress", safeIp);

        try {
            String logJson = objectMapper.writeValueAsString(signatureLogMap);
            recordAudit("SIGN_CONTRACT", contractId, null, logJson);
        } catch (Exception e) {
            log.warn("Failed to serialize SIGN_CONTRACT audit log for contract ID={}: {}", contractId, e.getMessage());
        }

        // 6. Check if all active co-owners have signed -> automatically transition to SIGNED
        List<OwnershipShare> activeShares = ownershipShareRepository.findByGroupIdAndIsActiveTrue(groupId);
        List<ContractSignature> allSignatures = contractSignatureRepository.findByContractId(contractId);

        if (!activeShares.isEmpty() && allSignatures.size() >= activeShares.size()) {
            String oldSnapshot = serializeContractSnapshot(contract);
            contract.setStatus(ContractStatus.SIGNED);
            coOwnershipContractRepository.save(contract);
            String newSnapshot = serializeContractSnapshot(contract);
            recordAudit("TRANSITION_CONTRACT_STATUS", contractId, oldSnapshot, newSnapshot);
            log.info("All {} active co-owners have signed contract ID={} (v{}). Automatically transitioned status to SIGNED.",
                    activeShares.size(), contractId, contract.getVersion());
        }

        return mapSignatureToResponse(savedSignature);
    }

    @Override
    @Transactional(readOnly = true)
    public ContractSignaturesOverviewResponse getSignaturesOverview(Long contractId, Long requesterUserId) {
        CoOwnershipContract contract = coOwnershipContractRepository.findById(contractId)
                .orElseThrow(() -> new ResourceNotFoundException(String.format("Contract not found with id: %d", contractId)));

        Long groupId = contract.getGroup().getId();

        List<OwnershipShare> activeShares = ownershipShareRepository.findByGroupIdAndIsActiveTrue(groupId);
        List<ContractSignature> signatures = contractSignatureRepository.findByContractId(contractId);

        Set<Long> signedUserIds = signatures.stream()
                .map(sig -> sig.getUser().getId())
                .collect(Collectors.toSet());

        List<ContractSignatureResponse> signatureResponses = signatures.stream()
                .map(this::mapSignatureToResponse)
                .collect(Collectors.toList());

        List<ContractSignaturesOverviewResponse.PendingSignerDto> pendingSigners = activeShares.stream()
                .filter(share -> !signedUserIds.contains(share.getUser().getId()))
                .map(share -> new ContractSignaturesOverviewResponse.PendingSignerDto(
                        share.getUser().getId(),
                        share.getUser().getFullName(),
                        share.getUser().getEmail(),
                        share.getPercentage(),
                        share.getShareCertificateNumber()
                ))
                .collect(Collectors.toList());

        boolean allSigned = !activeShares.isEmpty() && signatures.size() >= activeShares.size();

        return new ContractSignaturesOverviewResponse(
                contract.getId(),
                contract.getVersion(),
                contract.getStatus().name(),
                activeShares.size(),
                signatures.size(),
                allSigned,
                signatureResponses,
                pendingSigners
        );
    }

    private ContractSignatureResponse mapSignatureToResponse(ContractSignature signature) {
        return new ContractSignatureResponse(
                signature.getId(),
                signature.getContract().getId(),
                signature.getContract().getVersion(),
                signature.getUser().getId(),
                signature.getUser().getFullName(),
                signature.getUser().getEmail(),
                signature.getSignatureHash(),
                signature.getSignedAt(),
                signature.getIpAddress()
        );
    }

    private String sha256Hex(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (Exception e) {
            throw new RuntimeException("SHA-256 algorithm unavailable in runtime", e);
        }
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
}
