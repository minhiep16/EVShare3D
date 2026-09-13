package com.example.evshare.service.impl;

import com.example.evshare.dto.request.AddMediationNotesRequest;
import com.example.evshare.dto.request.AdminArbitrateDisputeRequest;
import com.example.evshare.dto.request.CreateDisputeEvidenceRequest;
import com.example.evshare.dto.request.CreateDisputeRequest;
import com.example.evshare.dto.request.DisputeFundAdjustmentRequest;
import com.example.evshare.dto.request.ProposeResolutionRequest;
import com.example.evshare.dto.response.DisputeArbitrationDossierResponse;
import com.example.evshare.dto.response.DisputeAuditLogResponse;
import com.example.evshare.dto.response.DisputeEvidenceResponse;
import com.example.evshare.dto.response.DisputeResponse;
import com.example.evshare.entity.*;
import com.example.evshare.entity.enums.DisputeStatus;
import com.example.evshare.entity.enums.FundTransactionSource;
import com.example.evshare.entity.enums.RoleName;
import com.example.evshare.entity.enums.TransactionEntryType;
import com.example.evshare.entity.enums.TransactionType;
import com.example.evshare.exception.BusinessException;
import com.example.evshare.exception.InsufficientFundBalanceException;
import com.example.evshare.exception.ResourceNotFoundException;
import com.example.evshare.repository.*;
import com.example.evshare.service.DisputeService;
import com.example.evshare.service.DisputeStateMachine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class DisputeServiceImpl implements DisputeService {

    private static final Logger log = LoggerFactory.getLogger(DisputeServiceImpl.class);

    private final DisputeRepository disputeRepository;
    private final DisputeEvidenceRepository disputeEvidenceRepository;
    private final OwnershipGroupRepository ownershipGroupRepository;
    private final OwnershipShareRepository ownershipShareRepository;
    private final UserRepository userRepository;
    private final UsageSessionRepository usageSessionRepository;
    private final AuditLogRepository auditLogRepository;
    private final DisputeStateMachine disputeStateMachine;
    private final SharedFundRepository sharedFundRepository;
    private final FundTransactionRepository fundTransactionRepository;

    public DisputeServiceImpl(DisputeRepository disputeRepository,
                              DisputeEvidenceRepository disputeEvidenceRepository,
                              OwnershipGroupRepository ownershipGroupRepository,
                              OwnershipShareRepository ownershipShareRepository,
                              UserRepository userRepository,
                              UsageSessionRepository usageSessionRepository,
                              AuditLogRepository auditLogRepository,
                              DisputeStateMachine disputeStateMachine) {
        this(disputeRepository, disputeEvidenceRepository, ownershipGroupRepository,
                ownershipShareRepository, userRepository, usageSessionRepository,
                auditLogRepository, disputeStateMachine, null, null);
    }

    @Autowired
    public DisputeServiceImpl(DisputeRepository disputeRepository,
                              DisputeEvidenceRepository disputeEvidenceRepository,
                              OwnershipGroupRepository ownershipGroupRepository,
                              OwnershipShareRepository ownershipShareRepository,
                              UserRepository userRepository,
                              UsageSessionRepository usageSessionRepository,
                              AuditLogRepository auditLogRepository,
                              DisputeStateMachine disputeStateMachine,
                              SharedFundRepository sharedFundRepository,
                              FundTransactionRepository fundTransactionRepository) {
        this.disputeRepository = disputeRepository;
        this.disputeEvidenceRepository = disputeEvidenceRepository;
        this.ownershipGroupRepository = ownershipGroupRepository;
        this.ownershipShareRepository = ownershipShareRepository;
        this.userRepository = userRepository;
        this.usageSessionRepository = usageSessionRepository;
        this.auditLogRepository = auditLogRepository;
        this.disputeStateMachine = disputeStateMachine;
        this.sharedFundRepository = sharedFundRepository;
        this.fundTransactionRepository = fundTransactionRepository;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public DisputeResponse createDispute(CreateDisputeRequest request, Long complainantUserId) {
        if (request == null) {
            throw new BusinessException("Dispute creation request cannot be null");
        }
        if (complainantUserId == null) {
            throw new BusinessException("Complainant user ID cannot be null", HttpStatus.FORBIDDEN);
        }

        // 1. Validate Complainant User
        User complainant = userRepository.findById(complainantUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", complainantUserId));
        if (!Boolean.TRUE.equals(complainant.getIsActive())) {
            throw new BusinessException("Complainant user account is inactive", HttpStatus.FORBIDDEN);
        }

        // 2. Validate Ownership Group
        Long groupId = request.getGroupId();
        if (groupId == null) {
            throw new BusinessException("Ownership group ID is required");
        }
        OwnershipGroup group = ownershipGroupRepository.findById(groupId)
                .orElseThrow(() -> new ResourceNotFoundException("OwnershipGroup", "id", groupId));
        if (!Boolean.TRUE.equals(group.getIsActive())) {
            throw new BusinessException("Ownership group " + groupId + " is not active");
        }

        // 3. Validate Complainant Eligibility in Group
        boolean isStaffOrAdmin = complainant.getRoles().stream()
                .anyMatch(r -> r.getName() == RoleName.ROLE_STAFF || r.getName() == RoleName.ROLE_ADMIN);
        if (!isStaffOrAdmin) {
            Optional<OwnershipShare> shareOpt = ownershipShareRepository.findByGroupIdAndUserId(groupId, complainantUserId);
            if (shareOpt.isEmpty()) {
                throw new BusinessException(
                        "User " + complainantUserId + " is not a co-owner in group " + groupId,
                        HttpStatus.FORBIDDEN
                );
            }
            if (!Boolean.TRUE.equals(shareOpt.get().getIsActive())) {
                throw new BusinessException(
                        "User " + complainantUserId + " is an inactive co-owner in group " + groupId,
                        HttpStatus.FORBIDDEN
                );
            }
        }

        // 4. Validate Reason
        if (request.getTitle() == null || request.getTitle().trim().isEmpty()) {
            throw new BusinessException("Dispute title cannot be blank");
        }
        if (request.getDescription() == null || request.getDescription().trim().isEmpty()) {
            throw new BusinessException("Dispute reason and description cannot be blank");
        }

        // 5. Validate Related Entity: Usage Session (if provided)
        UsageSession session = null;
        if (request.getUsageSessionId() != null) {
            session = usageSessionRepository.findById(request.getUsageSessionId())
                    .orElseThrow(() -> new ResourceNotFoundException("UsageSession", "id", request.getUsageSessionId()));

            // Verify session is related to the group's vehicle
            if (group.getVehicle() != null && session.getBooking() != null && session.getBooking().getVehicle() != null
                    && !group.getVehicle().getId().equals(session.getBooking().getVehicle().getId())) {
                throw new BusinessException("Usage session " + request.getUsageSessionId() + " does not belong to group's vehicle");
            }
        }

        // 6. Validate Related Entity: Respondent User (if provided)
        User respondent = null;
        if (request.getRespondentUserId() != null) {
            if (complainantUserId.equals(request.getRespondentUserId())) {
                throw new BusinessException("Complainant cannot file a dispute against themselves", HttpStatus.BAD_REQUEST);
            }
            respondent = userRepository.findById(request.getRespondentUserId())
                    .orElseThrow(() -> new ResourceNotFoundException("User", "id", request.getRespondentUserId()));
        }

        // 7. Validate Evidence Requirement
        if (request.getEvidences() == null || request.getEvidences().isEmpty()) {
            throw new BusinessException(
                    "At least one evidence item (photo, document, or 3D coordinate annotation) is required to file a dispute",
                    HttpStatus.BAD_REQUEST
            );
        }
        for (CreateDisputeEvidenceRequest ev : request.getEvidences()) {
            if (ev.getFileUrl() == null || ev.getFileUrl().trim().isEmpty()) {
                throw new BusinessException("Evidence file URL cannot be blank", HttpStatus.BAD_REQUEST);
            }
        }

        // 8. Persist Dispute Entity
        Dispute dispute = new Dispute();
        dispute.setGroup(group);
        dispute.setComplainantUser(complainant);
        dispute.setRespondentUser(respondent);
        dispute.setUsageSession(session);
        dispute.setTitle(request.getTitle().trim());
        dispute.setDescription(request.getDescription().trim());
        dispute.setStatus(DisputeStatus.OPEN);
        dispute.setCreatedAt(Instant.now());

        Dispute savedDispute = disputeRepository.saveAndFlush(dispute);
        log.info("Dispute [{}] created in group [{}] by user [{}] with status [{}]",
                savedDispute.getId(), groupId, complainantUserId, savedDispute.getStatus());

        // 9. Persist Evidence Items
        List<DisputeEvidence> evidenceEntities = new ArrayList<>();
        for (CreateDisputeEvidenceRequest ev : request.getEvidences()) {
            DisputeEvidence de = new DisputeEvidence();
            de.setDispute(savedDispute);
            de.setUploadedByUser(complainant);
            de.setFileUrl(ev.getFileUrl().trim());
            de.setMesh3dDefectCoordinates(ev.getMesh3dDefectCoordinates());
            de.setDescription(ev.getDescription());
            de.setCreatedAt(Instant.now());
            evidenceEntities.add(de);
        }
        List<DisputeEvidence> savedEvidences = disputeEvidenceRepository.saveAllAndFlush(evidenceEntities);

        // 10. Persist Dispute Creation Audit Log
        AuditLog auditLog = new AuditLog();
        auditLog.setUser(complainant);
        auditLog.setAction("DISPUTE_CREATED");
        auditLog.setEntityName("Dispute");
        auditLog.setEntityId(savedDispute.getId());
        auditLog.setNewStateJson(String.format(
                "{\"disputeId\":%d,\"groupId\":%d,\"complainantUserId\":%d,\"status\":\"%s\",\"title\":\"%s\"}",
                savedDispute.getId(), groupId, complainantUserId, savedDispute.getStatus(), savedDispute.getTitle()
        ));
        auditLog.setCreatedAt(Instant.now());
        auditLogRepository.save(auditLog);

        // 11. Persist Initial Dispute Evidence Audit Logs
        for (DisputeEvidence savedEv : savedEvidences) {
            AuditLog evAudit = new AuditLog();
            evAudit.setUser(complainant);
            evAudit.setAction("DISPUTE_EVIDENCE_ATTACHED");
            evAudit.setEntityName("DisputeEvidence");
            evAudit.setEntityId(savedEv.getId());
            evAudit.setNewStateJson(String.format(
                    "{\"disputeId\":%d,\"uploaderUserId\":%d,\"fileUrl\":\"%s\",\"has3dCoordinates\":%b,\"createdAt\":\"%s\"}",
                    savedDispute.getId(), complainantUserId, savedEv.getFileUrl(), savedEv.getMesh3dDefectCoordinates() != null, savedEv.getCreatedAt()
            ));
            evAudit.setCreatedAt(Instant.now());
            auditLogRepository.save(evAudit);
        }

        return DisputeResponse.fromEntity(savedDispute, savedEvidences);
    }

    @Override
    public DisputeResponse getDisputeById(Long disputeId) {
        if (disputeId == null) {
            throw new BusinessException("Dispute ID cannot be null");
        }
        Dispute dispute = disputeRepository.findById(disputeId)
                .orElseThrow(() -> new ResourceNotFoundException("Dispute", "id", disputeId));
        List<DisputeEvidence> evidences = disputeEvidenceRepository.findByDisputeId(disputeId);
        return DisputeResponse.fromEntity(dispute, evidences);
    }

    @Override
    public List<DisputeResponse> getDisputesByGroupId(Long groupId, DisputeStatus status) {
        if (groupId == null) {
            throw new BusinessException("Ownership group ID cannot be null");
        }
        if (!ownershipGroupRepository.existsById(groupId)) {
            throw new ResourceNotFoundException("OwnershipGroup", "id", groupId);
        }

        List<Dispute> disputes = (status != null)
                ? disputeRepository.findByGroupIdAndStatus(groupId, status)
                : disputeRepository.findByGroupId(groupId);

        return disputes.stream().map(d -> {
            List<DisputeEvidence> evidences = disputeEvidenceRepository.findByDisputeId(d.getId());
            return DisputeResponse.fromEntity(d, evidences);
        }).collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public DisputeResponse transitionDisputeStatus(Long disputeId, DisputeStatus targetStatus, String reason, String resolutionSummary, Long actorUserId) {
        if (disputeId == null) {
            throw new BusinessException("Dispute ID cannot be null");
        }
        if (targetStatus == null) {
            throw new BusinessException("Target dispute status cannot be null");
        }

        Dispute dispute = disputeRepository.findById(disputeId)
                .orElseThrow(() -> new ResourceNotFoundException("Dispute", "id", disputeId));

        if (actorUserId == null) {
            throw new BusinessException("Actor user ID cannot be null", HttpStatus.FORBIDDEN);
        }
        User actor = userRepository.findById(actorUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", actorUserId));

        boolean isAdmin = actor.getRoles().stream().anyMatch(r -> r.getName() == RoleName.ROLE_ADMIN);
        boolean isStaff = actor.getRoles().stream().anyMatch(r -> r.getName() == RoleName.ROLE_STAFF);

        if (!isAdmin && !isStaff) {
            throw new BusinessException("User " + actorUserId + " is not authorized to transition dispute lifecycle status", HttpStatus.FORBIDDEN);
        }

        // STAFF must not perform ADMIN-only final arbitration
        if (targetStatus == DisputeStatus.RESOLVED && !isAdmin) {
            throw new BusinessException("Staff members cannot perform final binding dispute arbitration. Final resolution is restricted to administrators.", HttpStatus.FORBIDDEN);
        }

        if (targetStatus == DisputeStatus.RESOLVED) {
            if (reason == null || reason.trim().isEmpty()) {
                throw new BusinessException("Arbitration reason is required for dispute resolution", HttpStatus.BAD_REQUEST);
            }
            if (resolutionSummary == null || resolutionSummary.trim().isEmpty()) {
                throw new BusinessException("Resolution summary is required for dispute resolution", HttpStatus.BAD_REQUEST);
            }
        }

        // Validate state machine transition
        disputeStateMachine.validateTransition(dispute.getStatus(), targetStatus, disputeId);

        DisputeStatus oldStatus = dispute.getStatus();
        dispute.setStatus(targetStatus);
        if (targetStatus == DisputeStatus.RESOLVED) {
            dispute.setResolutionSummary(resolutionSummary.trim());
            dispute.setArbitratorUser(actor);
            dispute.setResolvedAt(Instant.now());
        }

        Dispute updatedDispute = disputeRepository.saveAndFlush(dispute);
        log.info("Dispute [{}] transitioned: [{}] -> [{}] by user [{}] (reason: {})",
                disputeId, oldStatus, targetStatus, actorUserId, reason);

        // Record Audit Log
        AuditLog auditLog = new AuditLog();
        auditLog.setUser(actor);
        auditLog.setAction("DISPUTE_STATUS_TRANSITION");
        auditLog.setEntityName("Dispute");
        auditLog.setEntityId(disputeId);
        auditLog.setOldStateJson(String.format("{\"status\":\"%s\"}", oldStatus));
        auditLog.setNewStateJson(String.format(
                "{\"status\":\"%s\",\"reason\":\"%s\",\"resolutionSummary\":\"%s\"}",
                targetStatus,
                reason != null ? reason : "",
                dispute.getResolutionSummary() != null ? dispute.getResolutionSummary() : ""
        ));
        auditLog.setCreatedAt(Instant.now());
        auditLogRepository.save(auditLog);

        List<DisputeEvidence> evidences = disputeEvidenceRepository.findByDisputeId(disputeId);
        return DisputeResponse.fromEntity(updatedDispute, evidences);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public DisputeEvidenceResponse addEvidence(Long disputeId, CreateDisputeEvidenceRequest request, Long uploaderUserId) {
        if (disputeId == null) {
            throw new BusinessException("Dispute ID cannot be null");
        }
        if (request == null) {
            throw new BusinessException("Evidence request cannot be null");
        }
        if (request.getFileUrl() == null || request.getFileUrl().trim().isEmpty()) {
            throw new BusinessException("Evidence file URL cannot be blank", HttpStatus.BAD_REQUEST);
        }
        if (uploaderUserId == null) {
            throw new BusinessException("Uploader user ID cannot be null", HttpStatus.FORBIDDEN);
        }

        Dispute dispute = disputeRepository.findById(disputeId)
                .orElseThrow(() -> new ResourceNotFoundException("Dispute", "id", disputeId));

        if (disputeStateMachine.isTerminal(dispute.getStatus())) {
            throw new BusinessException("Cannot attach evidence to a RESOLVED dispute", HttpStatus.CONFLICT);
        }

        User uploader = userRepository.findById(uploaderUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", uploaderUserId));

        if (!Boolean.TRUE.equals(uploader.getIsActive())) {
            throw new BusinessException("User account is inactive", HttpStatus.FORBIDDEN);
        }

        // Access Control: uploader must be Complainant, Respondent, Active Co-owner in Group, or Staff/Admin
        boolean isStaffOrAdmin = uploader.getRoles().stream()
                .anyMatch(r -> r.getName() == RoleName.ROLE_STAFF || r.getName() == RoleName.ROLE_ADMIN);
        boolean isComplainant = dispute.getComplainantUser() != null && dispute.getComplainantUser().getId().equals(uploaderUserId);
        boolean isRespondent = dispute.getRespondentUser() != null && dispute.getRespondentUser().getId().equals(uploaderUserId);

        boolean isAuthorized = isStaffOrAdmin || isComplainant || isRespondent;
        if (!isAuthorized) {
            Optional<OwnershipShare> shareOpt = ownershipShareRepository.findByGroupIdAndUserId(dispute.getGroup().getId(), uploaderUserId);
            if (shareOpt.isPresent() && Boolean.TRUE.equals(shareOpt.get().getIsActive())) {
                isAuthorized = true;
            }
        }

        if (!isAuthorized) {
            throw new BusinessException(
                    "User " + uploaderUserId + " is not authorized to attach evidence to dispute " + disputeId,
                    HttpStatus.FORBIDDEN
            );
        }

        DisputeEvidence evidence = new DisputeEvidence();
        evidence.setDispute(dispute);
        evidence.setUploadedByUser(uploader);
        evidence.setFileUrl(request.getFileUrl().trim());
        evidence.setMesh3dDefectCoordinates(request.getMesh3dDefectCoordinates());
        evidence.setDescription(request.getDescription());
        evidence.setCreatedAt(Instant.now());

        DisputeEvidence savedEvidence = disputeEvidenceRepository.saveAndFlush(evidence);
        log.info("Evidence [{}] added to dispute [{}] by user [{}]", savedEvidence.getId(), disputeId, uploaderUserId);

        // Record Audit Log for Evidence Attachment
        AuditLog evAudit = new AuditLog();
        evAudit.setUser(uploader);
        evAudit.setAction("DISPUTE_EVIDENCE_ATTACHED");
        evAudit.setEntityName("DisputeEvidence");
        evAudit.setEntityId(savedEvidence.getId());
        evAudit.setNewStateJson(String.format(
                "{\"disputeId\":%d,\"uploaderUserId\":%d,\"fileUrl\":\"%s\",\"has3dCoordinates\":%b,\"createdAt\":\"%s\"}",
                disputeId, uploaderUserId, savedEvidence.getFileUrl(), savedEvidence.getMesh3dDefectCoordinates() != null, savedEvidence.getCreatedAt()
        ));
        evAudit.setCreatedAt(Instant.now());
        auditLogRepository.save(evAudit);

        return DisputeEvidenceResponse.fromEntity(savedEvidence);
    }

    @Override
    public List<DisputeEvidenceResponse> getDisputeEvidences(Long disputeId) {
        if (disputeId == null) {
            throw new BusinessException("Dispute ID cannot be null");
        }
        if (!disputeRepository.existsById(disputeId)) {
            throw new ResourceNotFoundException("Dispute", "id", disputeId);
        }
        return disputeEvidenceRepository.findByDisputeId(disputeId).stream()
                .map(DisputeEvidenceResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Override
    public DisputeEvidenceResponse getDisputeEvidenceById(Long disputeId, Long evidenceId) {
        if (disputeId == null) {
            throw new BusinessException("Dispute ID cannot be null");
        }
        if (evidenceId == null) {
            throw new BusinessException("Evidence ID cannot be null");
        }
        if (!disputeRepository.existsById(disputeId)) {
            throw new ResourceNotFoundException("Dispute", "id", disputeId);
        }

        DisputeEvidence evidence = disputeEvidenceRepository.findById(evidenceId)
                .orElseThrow(() -> new ResourceNotFoundException("DisputeEvidence", "id", evidenceId));

        if (!evidence.getDispute().getId().equals(disputeId)) {
            throw new ResourceNotFoundException("DisputeEvidence", "id", evidenceId);
        }

        return DisputeEvidenceResponse.fromEntity(evidence);
    }

    @Override
    public List<com.example.evshare.dto.response.DisputeAuditLogResponse> getDisputeHistory(Long disputeId) {
        if (disputeId == null) {
            throw new BusinessException("Dispute ID cannot be null");
        }
        if (!disputeRepository.existsById(disputeId)) {
            throw new ResourceNotFoundException("Dispute", "id", disputeId);
        }

        List<AuditLog> disputeLogs = auditLogRepository.findByEntityNameAndEntityIdOrderByCreatedAtDesc("Dispute", disputeId);
        List<DisputeEvidence> evidences = disputeEvidenceRepository.findByDisputeId(disputeId);
        List<Long> evidenceIds = evidences.stream().map(DisputeEvidence::getId).collect(Collectors.toList());

        List<AuditLog> evidenceLogs = evidenceIds.isEmpty()
                ? List.of()
                : auditLogRepository.findByEntityNameAndEntityIdInOrderByCreatedAtDesc("DisputeEvidence", evidenceIds);

        List<AuditLog> combined = new ArrayList<>(disputeLogs);
        combined.addAll(evidenceLogs);
        combined.sort((a, b) -> {
            int cmp = b.getCreatedAt().compareTo(a.getCreatedAt());
            if (cmp != 0) return cmp;
            return Long.compare(b.getId() != null ? b.getId() : 0, a.getId() != null ? a.getId() : 0);
        });

        return combined.stream()
                .map(com.example.evshare.dto.response.DisputeAuditLogResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public DisputeResponse addMediationNotes(Long disputeId, AddMediationNotesRequest request, Long staffUserId) {
        if (disputeId == null) {
            throw new BusinessException("Dispute ID cannot be null");
        }
        if (request == null || request.getNotes() == null || request.getNotes().trim().isEmpty()) {
            throw new BusinessException("Mediation notes cannot be blank", HttpStatus.BAD_REQUEST);
        }
        if (staffUserId == null) {
            throw new BusinessException("Staff user ID cannot be null", HttpStatus.FORBIDDEN);
        }

        User staff = userRepository.findById(staffUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", staffUserId));

        boolean isStaffOrAdmin = staff.getRoles().stream()
                .anyMatch(r -> r.getName() == RoleName.ROLE_STAFF || r.getName() == RoleName.ROLE_ADMIN);
        if (!isStaffOrAdmin) {
            throw new BusinessException("Only staff or administrators can record mediation notes", HttpStatus.FORBIDDEN);
        }

        Dispute dispute = disputeRepository.findById(disputeId)
                .orElseThrow(() -> new ResourceNotFoundException("Dispute", "id", disputeId));

        if (disputeStateMachine.isTerminal(dispute.getStatus())) {
            throw new BusinessException("Cannot add mediation notes to a RESOLVED dispute", HttpStatus.CONFLICT);
        }

        dispute.setMediationNotes(request.getNotes().trim());
        dispute.setMediatorUser(staff);

        Dispute savedDispute = disputeRepository.saveAndFlush(dispute);
        log.info("Mediation notes added to dispute [{}] by staff user [{}]", disputeId, staffUserId);

        AuditLog auditLog = new AuditLog();
        auditLog.setUser(staff);
        auditLog.setAction("DISPUTE_MEDIATION_NOTES_ADDED");
        auditLog.setEntityName("Dispute");
        auditLog.setEntityId(disputeId);
        auditLog.setNewStateJson(String.format("{\"disputeId\":%d,\"mediatorUserId\":%d,\"notesLength\":%d}",
                disputeId, staffUserId, request.getNotes().trim().length()));
        auditLog.setCreatedAt(Instant.now());
        auditLogRepository.save(auditLog);

        List<DisputeEvidence> evidences = disputeEvidenceRepository.findByDisputeId(disputeId);
        return DisputeResponse.fromEntity(savedDispute, evidences);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public DisputeResponse proposeResolution(Long disputeId, ProposeResolutionRequest request, Long staffUserId) {
        if (disputeId == null) {
            throw new BusinessException("Dispute ID cannot be null");
        }
        if (request == null || request.getProposedResolution() == null || request.getProposedResolution().trim().isEmpty()) {
            throw new BusinessException("Proposed resolution cannot be blank", HttpStatus.BAD_REQUEST);
        }
        if (staffUserId == null) {
            throw new BusinessException("Staff user ID cannot be null", HttpStatus.FORBIDDEN);
        }

        User staff = userRepository.findById(staffUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", staffUserId));

        boolean isStaffOrAdmin = staff.getRoles().stream()
                .anyMatch(r -> r.getName() == RoleName.ROLE_STAFF || r.getName() == RoleName.ROLE_ADMIN);
        if (!isStaffOrAdmin) {
            throw new BusinessException("Only staff or administrators can propose dispute resolutions", HttpStatus.FORBIDDEN);
        }

        Dispute dispute = disputeRepository.findById(disputeId)
                .orElseThrow(() -> new ResourceNotFoundException("Dispute", "id", disputeId));

        if (disputeStateMachine.isTerminal(dispute.getStatus())) {
            throw new BusinessException("Cannot propose resolution for a RESOLVED dispute", HttpStatus.CONFLICT);
        }

        dispute.setProposedResolution(request.getProposedResolution().trim());
        dispute.setMediatorUser(staff);

        Dispute savedDispute = disputeRepository.saveAndFlush(dispute);
        log.info("Proposed resolution recorded for dispute [{}] by staff user [{}]", disputeId, staffUserId);

        AuditLog auditLog = new AuditLog();
        auditLog.setUser(staff);
        auditLog.setAction("DISPUTE_RESOLUTION_PROPOSED");
        auditLog.setEntityName("Dispute");
        auditLog.setEntityId(disputeId);
        auditLog.setNewStateJson(String.format("{\"disputeId\":%d,\"mediatorUserId\":%d,\"proposedResolutionLength\":%d}",
                disputeId, staffUserId, request.getProposedResolution().trim().length()));
        auditLog.setCreatedAt(Instant.now());
        auditLogRepository.save(auditLog);

        List<DisputeEvidence> evidences = disputeEvidenceRepository.findByDisputeId(disputeId);
        return DisputeResponse.fromEntity(savedDispute, evidences);
    }

    @Override
    public List<DisputeResponse> getDisputesForStaffReview(DisputeStatus status) {
        List<Dispute> disputes;
        if (status != null) {
            disputes = disputeRepository.findByStatus(status);
        } else {
            disputes = disputeRepository.findByStatusIn(List.of(DisputeStatus.OPEN, DisputeStatus.UNDER_REVIEW, DisputeStatus.ESCALATED));
        }

        return disputes.stream().map(d -> {
            List<DisputeEvidence> evidences = disputeEvidenceRepository.findByDisputeId(d.getId());
            return DisputeResponse.fromEntity(d, evidences);
        }).collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public DisputeResponse arbitrateDispute(Long disputeId, AdminArbitrateDisputeRequest request, Long adminUserId) {
        if (disputeId == null) {
            throw new BusinessException("Dispute ID cannot be null");
        }
        if (request == null) {
            throw new BusinessException("Arbitration request cannot be null");
        }
        if (request.getFundAdjustmentAmount() != null && request.getFundAdjustmentAmount().compareTo(BigDecimal.ZERO) > 0) {
            TransactionEntryType entryType = request.getFundAdjustmentType() != null ? request.getFundAdjustmentType() : TransactionEntryType.DEBIT;
            DisputeFundAdjustmentRequest fundReq = new DisputeFundAdjustmentRequest(
                    request.getReason(), request.getResolutionSummary(), request.getFundAdjustmentAmount(), entryType
            );
            return arbitrateDisputeWithFundAdjustment(disputeId, fundReq, adminUserId);
        }
        if (request.getReason() == null || request.getReason().trim().isEmpty()) {
            throw new BusinessException("Arbitration reason cannot be blank", HttpStatus.BAD_REQUEST);
        }
        if (request.getResolutionSummary() == null || request.getResolutionSummary().trim().isEmpty()) {
            throw new BusinessException("Resolution summary cannot be blank", HttpStatus.BAD_REQUEST);
        }
        if (adminUserId == null) {
            throw new BusinessException("Admin user ID cannot be null", HttpStatus.FORBIDDEN);
        }

        User admin = userRepository.findById(adminUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", adminUserId));

        boolean isAdmin = admin.getRoles().stream().anyMatch(r -> r.getName() == RoleName.ROLE_ADMIN);
        if (!isAdmin) {
            throw new BusinessException("Only platform administrators can perform final binding dispute arbitration", HttpStatus.FORBIDDEN);
        }

        Dispute dispute = disputeRepository.findById(disputeId)
                .orElseThrow(() -> new ResourceNotFoundException("Dispute", "id", disputeId));

        // Validate lifecycle state transition (OPEN, UNDER_REVIEW, ESCALATED -> RESOLVED)
        disputeStateMachine.validateTransition(dispute.getStatus(), DisputeStatus.RESOLVED, disputeId);

        // Evidence review verification
        List<DisputeEvidence> evidences = disputeEvidenceRepository.findByDisputeId(disputeId);
        if (evidences.isEmpty()) {
            throw new BusinessException("Dispute " + disputeId + " cannot be arbitrated without evidence records", HttpStatus.BAD_REQUEST);
        }

        DisputeStatus oldStatus = dispute.getStatus();
        dispute.setStatus(DisputeStatus.RESOLVED);
        dispute.setResolutionSummary(request.getResolutionSummary().trim());
        dispute.setArbitratorUser(admin);
        dispute.setResolvedAt(Instant.now());

        Dispute savedDispute = disputeRepository.saveAndFlush(dispute);
        log.info("Dispute [{}] arbitrated and RESOLVED by admin [{}] (reason: {})",
                disputeId, adminUserId, request.getReason().trim());

        // Record Audit Log: DISPUTE_ARBITRATED
        AuditLog auditLog = new AuditLog();
        auditLog.setUser(admin);
        auditLog.setAction("DISPUTE_ARBITRATED");
        auditLog.setEntityName("Dispute");
        auditLog.setEntityId(disputeId);
        auditLog.setOldStateJson(String.format("{\"status\":\"%s\"}", oldStatus));
        auditLog.setNewStateJson(String.format(
                "{\"status\":\"RESOLVED\",\"arbitratorUserId\":%d,\"reason\":\"%s\",\"resolutionSummary\":\"%s\",\"evidenceCount\":%d}",
                adminUserId,
                request.getReason().trim().replace("\"", "\\\""),
                request.getResolutionSummary().trim().replace("\"", "\\\""),
                evidences.size()
        ));
        auditLog.setCreatedAt(Instant.now());
        auditLogRepository.save(auditLog);

        return DisputeResponse.fromEntity(savedDispute, evidences);
    }

    @Override
    public DisputeArbitrationDossierResponse getArbitrationDossier(Long disputeId, Long adminUserId) {
        if (disputeId == null) {
            throw new BusinessException("Dispute ID cannot be null");
        }
        if (adminUserId == null) {
            throw new BusinessException("Admin user ID cannot be null", HttpStatus.FORBIDDEN);
        }

        User admin = userRepository.findById(adminUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", adminUserId));

        boolean isAdmin = admin.getRoles().stream().anyMatch(r -> r.getName() == RoleName.ROLE_ADMIN);
        if (!isAdmin) {
            throw new BusinessException("Only platform administrators can access the arbitration dossier", HttpStatus.FORBIDDEN);
        }

        Dispute dispute = disputeRepository.findById(disputeId)
                .orElseThrow(() -> new ResourceNotFoundException("Dispute", "id", disputeId));

        List<DisputeEvidence> evidences = disputeEvidenceRepository.findByDisputeId(disputeId);
        DisputeResponse disputeResponse = DisputeResponse.fromEntity(dispute, evidences);
        List<DisputeEvidenceResponse> evidenceResponses = evidences.stream()
                .map(DisputeEvidenceResponse::fromEntity)
                .collect(Collectors.toList());
        List<DisputeAuditLogResponse> history = getDisputeHistory(disputeId);

        return new DisputeArbitrationDossierResponse(disputeResponse, evidenceResponses, history);
    }

    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED, rollbackFor = Exception.class)
    public DisputeResponse arbitrateDisputeWithFundAdjustment(Long disputeId, DisputeFundAdjustmentRequest request, Long adminUserId) {
        if (disputeId == null) {
            throw new BusinessException("Dispute ID cannot be null");
        }
        if (request == null) {
            throw new BusinessException("Dispute fund adjustment request cannot be null");
        }
        if (request.getReason() == null || request.getReason().trim().isEmpty()) {
            throw new BusinessException("Arbitration reason cannot be blank", HttpStatus.BAD_REQUEST);
        }
        if (request.getResolutionSummary() == null || request.getResolutionSummary().trim().isEmpty()) {
            throw new BusinessException("Resolution summary cannot be blank", HttpStatus.BAD_REQUEST);
        }
        if (request.getAmount() == null || request.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("Fund adjustment amount must be strictly greater than 0.00", HttpStatus.BAD_REQUEST);
        }
        if (request.getEntryType() == null) {
            throw new BusinessException("Fund adjustment entry type (DEBIT/CREDIT) is required", HttpStatus.BAD_REQUEST);
        }
        if (adminUserId == null) {
            throw new BusinessException("Admin user ID cannot be null", HttpStatus.FORBIDDEN);
        }

        User admin = userRepository.findById(adminUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", adminUserId));

        boolean isAdmin = admin.getRoles().stream().anyMatch(r -> r.getName() == RoleName.ROLE_ADMIN);
        if (!isAdmin) {
            throw new BusinessException("Only platform administrators can perform dispute fund adjustments", HttpStatus.FORBIDDEN);
        }

        Dispute dispute = disputeRepository.findById(disputeId)
                .orElseThrow(() -> new ResourceNotFoundException("Dispute", "id", disputeId));

        // Duplicate resolution prevention: validate transition (OPEN, UNDER_REVIEW, ESCALATED -> RESOLVED)
        // If already RESOLVED, this throws InvalidDisputeStateTransitionException (HTTP 409 Conflict)
        disputeStateMachine.validateTransition(dispute.getStatus(), DisputeStatus.RESOLVED, disputeId);

        // Check if fund adjustment was already executed
        if (dispute.getFundTransaction() != null) {
            throw new BusinessException("Dispute [" + disputeId + "] already has an associated financial fund adjustment", HttpStatus.CONFLICT);
        }

        // Verify evidence
        List<DisputeEvidence> evidences = disputeEvidenceRepository.findByDisputeId(disputeId);
        if (evidences.isEmpty()) {
            throw new BusinessException("Dispute " + disputeId + " cannot be arbitrated without evidence records", HttpStatus.BAD_REQUEST);
        }

        BigDecimal adjustmentAmount = request.getAmount().setScale(2, RoundingMode.HALF_EVEN);
        OwnershipGroup group = dispute.getGroup();

        if (sharedFundRepository == null || fundTransactionRepository == null) {
            throw new IllegalStateException("SharedFund repositories are not available for financial adjustment");
        }

        // Acquire pessimistic write lock on the syndicate SharedFund
        SharedFund fund = sharedFundRepository.findByGroupIdWithLock(group.getId())
                .orElseThrow(() -> new ResourceNotFoundException("SharedFund not found for ownership group: " + group.getId()));

        BigDecimal oldBalance = fund.getCurrentBalance() != null ? fund.getCurrentBalance() : BigDecimal.ZERO;
        BigDecimal newBalance;

        if (request.getEntryType() == TransactionEntryType.DEBIT) {
            if (oldBalance.compareTo(adjustmentAmount) < 0) {
                log.warn("Dispute fund adjustment rejected: fund {} has balance {} but {} was requested (insufficient balance)",
                        fund.getId(), oldBalance, adjustmentAmount);
                throw new InsufficientFundBalanceException(fund.getId(), oldBalance, adjustmentAmount);
            }
            newBalance = oldBalance.subtract(adjustmentAmount).setScale(2, RoundingMode.HALF_EVEN);
        } else {
            newBalance = oldBalance.add(adjustmentAmount).setScale(2, RoundingMode.HALF_EVEN);
        }

        fund.setCurrentBalance(newBalance);
        fund.setUpdatedAt(Instant.now());
        SharedFund savedFund = sharedFundRepository.save(fund);

        String ref = "DISP-" + disputeId + "-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        String desc = "Dispute #" + disputeId + " resolution: " + request.getResolutionSummary().trim();

        FundTransaction tx = new FundTransaction();
        tx.setFund(savedFund);
        tx.setUser(admin);
        tx.setTransactionType(TransactionType.DISPUTE_ADJUSTMENT);
        tx.setEntryType(request.getEntryType());
        tx.setAmount(adjustmentAmount);
        tx.setBalanceAfter(newBalance);
        tx.setTransactionReference(ref);
        tx.setDescription(desc);
        tx.setSource(FundTransactionSource.DISPUTE_RESOLUTION);
        tx.setCreatedAt(Instant.now());
        FundTransaction savedTx = fundTransactionRepository.save(tx);

        DisputeStatus oldStatus = dispute.getStatus();
        dispute.setStatus(DisputeStatus.RESOLVED);
        dispute.setResolutionSummary(request.getResolutionSummary().trim());
        dispute.setArbitratorUser(admin);
        dispute.setResolvedAt(Instant.now());
        dispute.setFundTransaction(savedTx);
        dispute.setFundAdjustmentAmount(adjustmentAmount);

        Dispute savedDispute = disputeRepository.saveAndFlush(dispute);
        log.info("Dispute [{}] arbitrated with fund adjustment of {} {} by admin [{}]. Ref: {}, New balance: {}",
                disputeId, adjustmentAmount, fund.getCurrency(), adminUserId, ref, newBalance);

        // Record Audit Log on Dispute
        AuditLog auditLog = new AuditLog();
        auditLog.setUser(admin);
        auditLog.setAction("DISPUTE_ARBITRATED");
        auditLog.setEntityName("Dispute");
        auditLog.setEntityId(disputeId);
        auditLog.setOldStateJson(String.format("{\"status\":\"%s\"}", oldStatus));
        auditLog.setNewStateJson(String.format(
                "{\"status\":\"RESOLVED\",\"arbitratorUserId\":%d,\"fundAdjustmentAmount\":%s,\"fundTransactionRef\":\"%s\",\"resolutionSummary\":\"%s\"}",
                adminUserId,
                adjustmentAmount.toPlainString(),
                ref,
                request.getResolutionSummary().trim().replace("\"", "\\\"")
        ));
        auditLog.setCreatedAt(Instant.now());
        auditLogRepository.save(auditLog);

        // Record Audit Log on SharedFund
        AuditLog fundAudit = new AuditLog();
        fundAudit.setUser(admin);
        fundAudit.setAction("SHARED_FUND_DISPUTE_ADJUSTMENT");
        fundAudit.setEntityName("SharedFund");
        fundAudit.setEntityId(savedFund.getId());
        fundAudit.setOldStateJson(String.format("{\"balance\":%s}", oldBalance.toPlainString()));
        fundAudit.setNewStateJson(String.format(
                "{\"balance\":%s,\"delta\":%s,\"entryType\":\"%s\",\"disputeId\":%d,\"transactionReference\":\"%s\"}",
                newBalance.toPlainString(),
                adjustmentAmount.toPlainString(),
                request.getEntryType(),
                disputeId,
                ref
        ));
        fundAudit.setCreatedAt(Instant.now());
        auditLogRepository.save(fundAudit);

        return DisputeResponse.fromEntity(savedDispute, evidences);
    }
}
