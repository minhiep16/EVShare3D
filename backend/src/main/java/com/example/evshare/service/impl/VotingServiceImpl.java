package com.example.evshare.service.impl;

import com.example.evshare.dto.request.CastVoteRequest;
import com.example.evshare.dto.request.CreateProposalRequest;
import com.example.evshare.dto.response.ProposalResponse;
import com.example.evshare.dto.response.ProposalResultsResponse;
import com.example.evshare.dto.response.ProposalTallyResponse;
import com.example.evshare.dto.response.ProposerEligibilityResponse;
import com.example.evshare.dto.response.VoteResponse;
import com.example.evshare.entity.AuditLog;
import com.example.evshare.entity.OwnershipGroup;
import com.example.evshare.entity.OwnershipShare;
import com.example.evshare.entity.Proposal;
import com.example.evshare.entity.User;
import com.example.evshare.entity.Vote;
import com.example.evshare.entity.VoteOption;
import com.example.evshare.entity.enums.ProposalStatus;
import com.example.evshare.entity.enums.ProposalType;
import com.example.evshare.entity.enums.VoteOptionKey;
import com.example.evshare.exception.BusinessException;
import com.example.evshare.exception.InsufficientEquityException;
import com.example.evshare.exception.ResourceNotFoundException;
import com.example.evshare.repository.AuditLogRepository;
import com.example.evshare.repository.OwnershipGroupRepository;
import com.example.evshare.repository.OwnershipShareRepository;
import com.example.evshare.repository.ProposalRepository;
import com.example.evshare.repository.UserRepository;
import com.example.evshare.repository.VoteOptionRepository;
import com.example.evshare.repository.VoteRepository;
import com.example.evshare.service.VotingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class VotingServiceImpl implements VotingService {

    private static final Logger log = LoggerFactory.getLogger(VotingServiceImpl.class);
    private static final BigDecimal MIN_SPONSOR_EQUITY_PERCENTAGE = new BigDecimal("10.00");
    private static final long DEFAULT_VOTING_WINDOW_HOURS = 72;

    private final ProposalRepository proposalRepository;
    private final VoteOptionRepository voteOptionRepository;
    private final OwnershipGroupRepository ownershipGroupRepository;
    private final OwnershipShareRepository ownershipShareRepository;
    private final UserRepository userRepository;
    private final AuditLogRepository auditLogRepository;
    private final com.example.evshare.service.ProposalStateMachine proposalStateMachine;
    private final VoteRepository voteRepository;

    public VotingServiceImpl(ProposalRepository proposalRepository,
                             VoteOptionRepository voteOptionRepository,
                             OwnershipGroupRepository ownershipGroupRepository,
                             OwnershipShareRepository ownershipShareRepository,
                             UserRepository userRepository,
                             AuditLogRepository auditLogRepository,
                             com.example.evshare.service.ProposalStateMachine proposalStateMachine,
                             VoteRepository voteRepository) {
        this.proposalRepository = proposalRepository;
        this.voteOptionRepository = voteOptionRepository;
        this.ownershipGroupRepository = ownershipGroupRepository;
        this.ownershipShareRepository = ownershipShareRepository;
        this.userRepository = userRepository;
        this.auditLogRepository = auditLogRepository;
        this.proposalStateMachine = proposalStateMachine;
        this.voteRepository = voteRepository;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ProposalResponse createProposal(CreateProposalRequest request, Long proposerUserId) {
        if (request == null) {
            throw new BusinessException("CreateProposalRequest payload cannot be null");
        }
        if (proposerUserId == null) {
            throw new BusinessException("Proposer user ID is mandatory", HttpStatus.UNAUTHORIZED);
        }

        // 1. Validate Ownership Group
        OwnershipGroup group = ownershipGroupRepository.findById(request.getGroupId())
                .orElseThrow(() -> new ResourceNotFoundException("OwnershipGroup", "id", request.getGroupId()));
        if (Boolean.FALSE.equals(group.getIsActive())) {
            throw new BusinessException("Ownership group " + request.getGroupId() + " is inactive and cannot accept proposals");
        }

        // 2. Validate Proposer User
        User proposer = userRepository.findById(proposerUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", proposerUserId));
        if (Boolean.FALSE.equals(proposer.getIsActive())) {
            throw new BusinessException("Proposer user account is inactive");
        }

        // 3. Validate Category (ProposalType)
        if (request.getProposalType() == null) {
            throw new BusinessException("Proposal category (proposalType) is mandatory");
        }

        // 4. Validate Required Data
        if (request.getTitle() == null || request.getTitle().trim().isEmpty()) {
            throw new BusinessException("Proposal title cannot be blank");
        }
        String trimmedTitle = request.getTitle().trim();
        if (trimmedTitle.length() > 150) {
            throw new BusinessException("Proposal title cannot exceed 150 characters");
        }
        if (request.getDescription() == null || request.getDescription().trim().isEmpty()) {
            throw new BusinessException("Proposal description cannot be blank");
        }
        String trimmedDescription = request.getDescription().trim();

        // 5. Validate / Default Voting Deadline (72 hours per BR-VOT-02)
        Instant now = Instant.now();
        Instant deadline = request.getVotingDeadline();
        if (deadline == null) {
            deadline = now.plus(DEFAULT_VOTING_WINDOW_HOURS, ChronoUnit.HOURS);
        } else {
            if (!deadline.isAfter(now)) {
                throw new BusinessException("Voting deadline must be strictly in the future");
            }
        }

        // 6. Validate Proposer Eligibility (BR-VOT-01: active co-owner holding >= 10.00% equity)
        OwnershipShare share = validateProposerEligibility(group.getId(), proposerUserId);
        BigDecimal equityPercentage = share.getPercentage() != null ? share.getPercentage() : BigDecimal.ZERO;

        // 7. Persist Proposal with status ACTIVE
        Proposal proposal = new Proposal();
        proposal.setGroup(group);
        proposal.setProposerUser(proposer);
        proposal.setTitle(trimmedTitle);
        proposal.setDescription(trimmedDescription);
        proposal.setProposalType(request.getProposalType());
        proposal.setStatus(ProposalStatus.ACTIVE);
        proposal.setVotingDeadline(deadline);
        proposal.setCreatedAt(now);

        Proposal savedProposal = proposalRepository.save(proposal);
        log.info("Governance proposal [{}] created in group [{}] by user [{}] with category [{}]",
                savedProposal.getId(), group.getId(), proposerUserId, savedProposal.getProposalType());

        // 8. Automatically Seed Canonical Ballot Options: APPROVE, REJECT, ABSTAIN
        VoteOption approveOption = new VoteOption(null, savedProposal, VoteOptionKey.APPROVE, "Approve Proposal");
        VoteOption rejectOption = new VoteOption(null, savedProposal, VoteOptionKey.REJECT, "Reject Proposal");
        VoteOption abstainOption = new VoteOption(null, savedProposal, VoteOptionKey.ABSTAIN, "Abstain from Vote");

        List<VoteOption> options = voteOptionRepository.saveAll(List.of(approveOption, rejectOption, abstainOption));

        // 9. Write Audit Log
        AuditLog auditLog = new AuditLog();
        auditLog.setUser(proposer);
        auditLog.setAction("PROPOSAL_CREATED");
        auditLog.setEntityName("Proposal");
        auditLog.setEntityId(savedProposal.getId());
        auditLog.setNewStateJson(String.format(
                "{\"status\":\"ACTIVE\",\"proposalType\":\"%s\",\"title\":\"%s\",\"votingDeadline\":\"%s\",\"equityWeight\":\"%s\"}",
                savedProposal.getProposalType(), savedProposal.getTitle(), savedProposal.getVotingDeadline(), equityPercentage
        ));
        auditLog.setCreatedAt(now);
        auditLogRepository.save(auditLog);

        return ProposalResponse.fromEntity(savedProposal, options);
    }

    @Override
    public ProposalResponse getProposalById(Long proposalId) {
        if (proposalId == null) {
            throw new BusinessException("Proposal ID cannot be null");
        }
        Proposal proposal = proposalRepository.findById(proposalId)
                .orElseThrow(() -> new ResourceNotFoundException("Proposal", "id", proposalId));
        List<VoteOption> options = voteOptionRepository.findByProposalId(proposal.getId());
        return ProposalResponse.fromEntity(proposal, options);
    }

    @Override
    public List<ProposalResponse> getProposalsByGroupId(Long groupId, ProposalStatus status) {
        if (groupId == null) {
            throw new BusinessException("Ownership group ID cannot be null");
        }
        if (!ownershipGroupRepository.existsById(groupId)) {
            throw new ResourceNotFoundException("OwnershipGroup", "id", groupId);
        }

        List<Proposal> proposals;
        if (status != null) {
            proposals = proposalRepository.findByGroupIdAndStatus(groupId, status);
        } else {
            proposals = proposalRepository.findByGroupId(groupId);
        }

        return proposals.stream().map(proposal -> {
            List<VoteOption> options = voteOptionRepository.findByProposalId(proposal.getId());
            return ProposalResponse.fromEntity(proposal, options);
        }).collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ProposalResponse transitionProposalStatus(Long proposalId, ProposalStatus targetStatus, String reason, Long actorUserId) {
        if (proposalId == null) {
            throw new BusinessException("Proposal ID cannot be null");
        }
        if (targetStatus == null) {
            throw new BusinessException("Target status cannot be null");
        }

        Proposal proposal = proposalRepository.findById(proposalId)
                .orElseThrow(() -> new ResourceNotFoundException("Proposal", "id", proposalId));

        User actor = actorUserId != null ? userRepository.findById(actorUserId).orElse(null) : null;

        ProposalStatus oldStatus = proposal.getStatus();
        proposalStateMachine.validateTransition(oldStatus, targetStatus, proposalId);

        proposal.setStatus(targetStatus);
        Proposal saved = proposalRepository.save(proposal);
        log.info("Proposal [{}] transitioned: [{}] -> [{}] by user [{}] (reason: {})",
                proposalId, oldStatus, targetStatus, actorUserId, reason);

        // Preserve history in audit log
        AuditLog auditLog = new AuditLog();
        auditLog.setUser(actor);
        auditLog.setAction("PROPOSAL_STATE_TRANSITION");
        auditLog.setEntityName("Proposal");
        auditLog.setEntityId(saved.getId());
        auditLog.setOldStateJson(String.format("{\"status\":\"%s\"}", oldStatus));
        auditLog.setNewStateJson(String.format("{\"status\":\"%s\",\"reason\":\"%s\"}",
                targetStatus, reason != null ? reason.replace("\"", "\\\"") : "Lifecycle transition"));
        auditLog.setCreatedAt(Instant.now());
        auditLogRepository.save(auditLog);

        List<VoteOption> options = voteOptionRepository.findByProposalId(saved.getId());
        return ProposalResponse.fromEntity(saved, options);
    }

    @Override
    public List<com.example.evshare.dto.response.ProposalAuditLogResponse> getProposalHistory(Long proposalId) {
        if (proposalId == null) {
            throw new BusinessException("Proposal ID cannot be null");
        }
        if (!proposalRepository.existsById(proposalId)) {
            throw new ResourceNotFoundException("Proposal", "id", proposalId);
        }

        List<AuditLog> logs = auditLogRepository.findByEntityNameAndEntityIdOrderByCreatedAtDesc("Proposal", proposalId);
        return logs.stream().map(com.example.evshare.dto.response.ProposalAuditLogResponse::fromEntity).collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ProposalResponse checkAndExpireIfDeadlinePassed(Long proposalId) {
        if (proposalId == null) {
            throw new BusinessException("Proposal ID cannot be null");
        }
        Proposal proposal = proposalRepository.findById(proposalId)
                .orElseThrow(() -> new ResourceNotFoundException("Proposal", "id", proposalId));

        if (proposal.getStatus() == ProposalStatus.ACTIVE
                && proposal.getVotingDeadline() != null
                && proposal.getVotingDeadline().isBefore(Instant.now())) {
            return transitionProposalStatus(proposalId, ProposalStatus.EXPIRED,
                    "Voting deadline elapsed without resolution", null);
        }

        List<VoteOption> options = voteOptionRepository.findByProposalId(proposal.getId());
        return ProposalResponse.fromEntity(proposal, options);
    }

    @Override
    public OwnershipShare validateProposerEligibility(Long groupId, Long proposerUserId) {
        if (groupId == null) {
            throw new BusinessException("Ownership group ID cannot be null");
        }
        if (proposerUserId == null) {
            throw new BusinessException("Proposer user ID cannot be null", HttpStatus.FORBIDDEN);
        }

        Optional<OwnershipShare> shareOpt = ownershipShareRepository.findByGroupIdAndUserId(groupId, proposerUserId);
        if (shareOpt.isEmpty()) {
            throw new BusinessException("User " + proposerUserId + " is not an active co-owner in group " + groupId, HttpStatus.FORBIDDEN);
        }

        OwnershipShare share = shareOpt.get();
        if (!Boolean.TRUE.equals(share.getIsActive())) {
            throw new BusinessException("User " + proposerUserId + " is an inactive co-owner in group " + groupId, HttpStatus.FORBIDDEN);
        }

        BigDecimal equityPercentage = share.getPercentage() != null ? share.getPercentage() : BigDecimal.ZERO;
        if (equityPercentage.compareTo(MIN_SPONSOR_EQUITY_PERCENTAGE) < 0) {
            throw new InsufficientEquityException(
                    "Co-owner must hold at least 10.00% active equity in group " + groupId
                            + " to sponsor a proposal. Current active equity: " + equityPercentage + "%"
            );
        }

        return share;
    }

    @Override
    public ProposerEligibilityResponse checkProposerEligibility(Long groupId, Long proposerUserId) {
        if (groupId == null) {
            throw new BusinessException("Ownership group ID cannot be null");
        }
        if (!ownershipGroupRepository.existsById(groupId)) {
            throw new ResourceNotFoundException("OwnershipGroup", "id", groupId);
        }
        if (proposerUserId == null) {
            return new ProposerEligibilityResponse(groupId, null, false, false, BigDecimal.ZERO, MIN_SPONSOR_EQUITY_PERCENTAGE, false, "Proposer user ID is missing");
        }

        Optional<OwnershipShare> shareOpt = ownershipShareRepository.findByGroupIdAndUserId(groupId, proposerUserId);
        if (shareOpt.isEmpty()) {
            return new ProposerEligibilityResponse(groupId, proposerUserId, false, false, BigDecimal.ZERO, MIN_SPONSOR_EQUITY_PERCENTAGE, false,
                    "User " + proposerUserId + " is not an active co-owner in group " + groupId);
        }

        OwnershipShare share = shareOpt.get();
        boolean isActive = Boolean.TRUE.equals(share.getIsActive());
        BigDecimal equityPercentage = share.getPercentage() != null ? share.getPercentage() : BigDecimal.ZERO;

        if (!isActive) {
            return new ProposerEligibilityResponse(groupId, proposerUserId, true, false, equityPercentage, MIN_SPONSOR_EQUITY_PERCENTAGE, false,
                    "User " + proposerUserId + " is an inactive co-owner in group " + groupId);
        }

        if (equityPercentage.compareTo(MIN_SPONSOR_EQUITY_PERCENTAGE) < 0) {
            return new ProposerEligibilityResponse(groupId, proposerUserId, true, true, equityPercentage, MIN_SPONSOR_EQUITY_PERCENTAGE, false,
                    "Co-owner holds " + equityPercentage + "% active equity, which is below the required 10.00% minimum threshold");
        }

        return new ProposerEligibilityResponse(groupId, proposerUserId, true, true, equityPercentage, MIN_SPONSOR_EQUITY_PERCENTAGE, true,
                "User is eligible to sponsor governance proposals with " + equityPercentage + "% active equity");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public VoteResponse castVote(Long proposalId, CastVoteRequest request, Long voterUserId) {
        if (proposalId == null) {
            throw new BusinessException("Proposal ID cannot be null");
        }
        if (voterUserId == null) {
            throw new BusinessException("Voter user ID cannot be null", HttpStatus.UNAUTHORIZED);
        }
        if (request == null || request.getOptionKey() == null) {
            throw new BusinessException("Vote choice must be specified (APPROVE, REJECT, ABSTAIN)");
        }

        // 1. Fetch and validate Proposal
        Proposal proposal = proposalRepository.findById(proposalId)
                .orElseThrow(() -> new ResourceNotFoundException("Proposal", "id", proposalId));

        // 2. Enforce Proposal status must be ACTIVE
        if (proposal.getStatus() != ProposalStatus.ACTIVE) {
            throw new BusinessException(
                    "Votes can only be cast on ACTIVE proposals. Current proposal status: " + proposal.getStatus(),
                    HttpStatus.BAD_REQUEST
            );
        }

        // 3. Enforce Voting Deadline
        if (proposal.getVotingDeadline() != null && proposal.getVotingDeadline().isBefore(Instant.now())) {
            throw new BusinessException(
                    "Voting deadline for proposal " + proposalId + " has expired",
                    HttpStatus.BAD_REQUEST
            );
        }

        // 4. Enforce One Vote Per Eligible Voter (Duplicate ballot rejection)
        if (voteRepository.existsByProposalIdAndUserId(proposal.getId(), voterUserId)) {
            throw new BusinessException(
                    "User " + voterUserId + " has already cast a ballot on proposal " + proposal.getId(),
                    HttpStatus.CONFLICT
            );
        }

        // 5. Enforce Active Ownership in Proposal Syndicate Group
        Long groupId = proposal.getGroup().getId();
        Optional<OwnershipShare> shareOpt = ownershipShareRepository.findByGroupIdAndUserId(groupId, voterUserId);
        if (shareOpt.isEmpty()) {
            throw new BusinessException(
                    "User " + voterUserId + " is not an active co-owner in group " + groupId,
                    HttpStatus.FORBIDDEN
            );
        }

        OwnershipShare share = shareOpt.get();
        if (!Boolean.TRUE.equals(share.getIsActive())) {
            throw new BusinessException(
                    "User " + voterUserId + " is an inactive co-owner in group " + groupId,
                    HttpStatus.FORBIDDEN
            );
        }

        BigDecimal equityWeight = share.getPercentage() != null ? share.getPercentage() : BigDecimal.ZERO;
        if (equityWeight.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException(
                    "Active co-owner holds 0.00% equity stake and cannot cast a weighted vote",
                    HttpStatus.FORBIDDEN
            );
        }

        // 6. Fetch Voter User
        User voter = userRepository.findById(voterUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", voterUserId));
        if (!Boolean.TRUE.equals(voter.getIsActive())) {
            throw new BusinessException("User account is inactive", HttpStatus.FORBIDDEN);
        }

        // 7. Find Matching Seeded Vote Option for Proposal
        VoteOption voteOption = voteOptionRepository.findByProposalIdAndOptionKey(proposal.getId(), request.getOptionKey())
                .orElseThrow(() -> new BusinessException(
                        "Vote option " + request.getOptionKey() + " does not exist on proposal " + proposal.getId()
                ));

        // 8. Persist Ballot with Authoritative Equity Weight & Database Uniqueness Enforcement
        Vote vote = new Vote();
        vote.setProposal(proposal);
        vote.setUser(voter);
        vote.setVoteOption(voteOption);
        vote.setEquityWeight(equityWeight);
        vote.setVotedAt(Instant.now());

        Vote savedVote;
        try {
            savedVote = voteRepository.saveAndFlush(vote);
        } catch (DataIntegrityViolationException ex) {
            log.warn("Database unique constraint uk_proposal_user_vote prevented duplicate ballot for user [{}] on proposal [{}]: {}",
                    voterUserId, proposal.getId(), ex.getMessage());
            throw new BusinessException(
                    "User " + voterUserId + " has already cast a ballot on proposal " + proposal.getId(),
                    HttpStatus.CONFLICT
            );
        }
        log.info("Ballot cast: User [{}] voted [{}] with equity weight [{}%] on proposal [{}]",
                voterUserId, voteOption.getOptionKey(), equityWeight, proposal.getId());

        // 9. Persist Audit Log
        AuditLog auditLog = new AuditLog();
        auditLog.setUser(voter);
        auditLog.setAction("PROPOSAL_VOTE_CAST");
        auditLog.setEntityName("Vote");
        auditLog.setEntityId(savedVote.getId());
        auditLog.setNewStateJson(String.format(
                "{\"proposalId\":%d,\"userId\":%d,\"optionKey\":\"%s\",\"equityWeight\":\"%s\"}",
                proposal.getId(), voterUserId, voteOption.getOptionKey(), equityWeight
        ));
        auditLog.setCreatedAt(Instant.now());
        auditLogRepository.save(auditLog);

        return VoteResponse.fromEntity(savedVote);
    }

    @Override
    public List<VoteResponse> getVotesByProposalId(Long proposalId) {
        if (proposalId == null) {
            throw new BusinessException("Proposal ID cannot be null");
        }
        if (!proposalRepository.existsById(proposalId)) {
            throw new ResourceNotFoundException("Proposal", "id", proposalId);
        }
        return voteRepository.findByProposalId(proposalId).stream()
                .map(VoteResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Override
    public Optional<VoteResponse> getVoteByProposalAndUser(Long proposalId, Long userId) {
        if (proposalId == null || userId == null) {
            return Optional.empty();
        }
        return voteRepository.findByProposalIdAndUserId(proposalId, userId)
                .map(VoteResponse::fromEntity);
    }

    @Override
    public ProposalTallyResponse getProposalTally(Long proposalId) {
        if (proposalId == null) {
            throw new BusinessException("Proposal ID cannot be null");
        }
        Proposal proposal = proposalRepository.findById(proposalId)
                .orElseThrow(() -> new ResourceNotFoundException("Proposal", "id", proposalId));

        Long groupId = proposal.getGroup().getId();
        BigDecimal groupActiveEquity = ownershipShareRepository.sumActivePercentagesByGroupId(groupId);
        if (groupActiveEquity == null || groupActiveEquity.compareTo(BigDecimal.ZERO) <= 0) {
            groupActiveEquity = new BigDecimal("100.00");
        }

        List<Vote> votes = voteRepository.findByProposalId(proposalId);

        BigDecimal approveEquity = new BigDecimal("0.00");
        BigDecimal rejectEquity = new BigDecimal("0.00");
        BigDecimal abstainEquity = new BigDecimal("0.00");

        for (Vote vote : votes) {
            BigDecimal weight = vote.getEquityWeight() != null ? vote.getEquityWeight() : BigDecimal.ZERO;
            if (vote.getVoteOption() != null && vote.getVoteOption().getOptionKey() != null) {
                switch (vote.getVoteOption().getOptionKey()) {
                    case APPROVE -> approveEquity = approveEquity.add(weight);
                    case REJECT -> rejectEquity = rejectEquity.add(weight);
                    case ABSTAIN -> abstainEquity = abstainEquity.add(weight);
                }
            }
        }

        BigDecimal totalParticipatingEquity = approveEquity.add(rejectEquity).add(abstainEquity);
        BigDecimal quorumThreshold = new BigDecimal("60.00");

        // Quorum: participation calculated using active ownership equity
        // participationRate = (totalParticipatingEquity / groupActiveEquity) * 100
        BigDecimal participationRate = new BigDecimal("0.00");
        if (groupActiveEquity.compareTo(BigDecimal.ZERO) > 0) {
            participationRate = totalParticipatingEquity.multiply(BigDecimal.valueOf(100))
                    .divide(groupActiveEquity, 2, java.math.RoundingMode.HALF_UP);
        }

        // Exact comparison to avoid rounding loss:
        // totalParticipatingEquity * 100 >= groupActiveEquity * 60.00
        boolean quorumReached = totalParticipatingEquity.multiply(BigDecimal.valueOf(100))
                .compareTo(groupActiveEquity.multiply(quorumThreshold)) >= 0;

        BigDecimal approvePctOfParticipating = BigDecimal.ZERO;
        BigDecimal rejectPctOfParticipating = BigDecimal.ZERO;
        BigDecimal abstainPctOfParticipating = BigDecimal.ZERO;

        if (totalParticipatingEquity.compareTo(BigDecimal.ZERO) > 0) {
            approvePctOfParticipating = approveEquity.multiply(BigDecimal.valueOf(100))
                    .divide(totalParticipatingEquity, 2, java.math.RoundingMode.HALF_UP);
            rejectPctOfParticipating = rejectEquity.multiply(BigDecimal.valueOf(100))
                    .divide(totalParticipatingEquity, 2, java.math.RoundingMode.HALF_UP);
            abstainPctOfParticipating = abstainEquity.multiply(BigDecimal.valueOf(100))
                    .divide(totalParticipatingEquity, 2, java.math.RoundingMode.HALF_UP);
        }

        BigDecimal approvePctOfTotal = approveEquity.multiply(BigDecimal.valueOf(100))
                .divide(groupActiveEquity, 2, java.math.RoundingMode.HALF_UP);

        boolean passed;
        String outcomeReason;

        BigDecimal requiredThreshold;
        String thresholdType;

        if (proposal.getProposalType() == ProposalType.ROUTINE_EXPENSE) {
            requiredThreshold = new BigDecimal("50.00");
            thresholdType = "RELATIVE_TO_PARTICIPATING";
        } else {
            requiredThreshold = new BigDecimal("75.00");
            thresholdType = "RELATIVE_TO_TOTAL";
        }

        if (!quorumReached) {
            passed = false;
            outcomeReason = String.format("Quorum of 60.00%% not reached. Current participation: %s%%", totalParticipatingEquity);
        } else {
            if (proposal.getProposalType() == ProposalType.ROUTINE_EXPENSE) {
                // Routine business: strictly > 50.00% of participating equity voting APPROVE
                boolean passes = approveEquity.multiply(BigDecimal.valueOf(2)).compareTo(totalParticipatingEquity) > 0;
                passed = passes;
                outcomeReason = passes
                        ? String.format("Approved with %s%% of participating equity (>50.00%%)", approvePctOfParticipating)
                        : String.format("Rejected: Did not achieve >50.00%% approval among participating equity (%s%%)", approvePctOfParticipating);
            } else {
                // Major business: >= 75.00% of total group active equity voting APPROVE
                // Exact arithmetic: approveEquity * 100 >= groupActiveEquity * 75.00
                boolean passes = approveEquity.multiply(BigDecimal.valueOf(100))
                        .compareTo(groupActiveEquity.multiply(requiredThreshold)) >= 0;
                passed = passes;
                outcomeReason = passes
                        ? String.format("Supermajority satisfied with %s%% of total syndicate equity (>=75.00%%)", approveEquity)
                        : String.format("Rejected: Failed 75.00%% supermajority threshold (achieved %s%%)", approveEquity);
            }
        }

        List<VoteResponse> ballots = votes.stream()
                .map(VoteResponse::fromEntity)
                .collect(Collectors.toList());

        ProposalTallyResponse tally = new ProposalTallyResponse();
        tally.setProposalId(proposal.getId());
        tally.setProposalTitle(proposal.getTitle());
        tally.setProposalType(proposal.getProposalType());
        tally.setStatus(proposal.getStatus());
        tally.setTotalGroupActiveEquity(groupActiveEquity);
        tally.setTotalParticipatingEquity(totalParticipatingEquity);
        tally.setParticipationRatePercentage(participationRate);
        tally.setTotalVotersCount(votes.size());
        tally.setQuorumPercentage(quorumThreshold);
        tally.setQuorumReached(quorumReached);
        tally.setApproveEquity(approveEquity);
        tally.setRejectEquity(rejectEquity);
        tally.setAbstainEquity(abstainEquity);
        tally.setApprovePercentageOfParticipating(approvePctOfParticipating);
        tally.setRejectPercentageOfParticipating(rejectPctOfParticipating);
        tally.setAbstainPercentageOfParticipating(abstainPctOfParticipating);
        tally.setApprovePercentageOfTotal(approvePctOfTotal);
        tally.setRequiredThresholdPercentage(requiredThreshold);
        tally.setThresholdType(thresholdType);
        tally.setPassed(passed);
        tally.setOutcomeReason(outcomeReason);
        tally.setBallots(ballots);

        return tally;
    }

    @Override
    public ProposalResultsResponse getProposalResults(Long proposalId) {
        ProposalTallyResponse tally = getProposalTally(proposalId);
        Proposal proposal = proposalRepository.findById(proposalId)
                .orElseThrow(() -> new ResourceNotFoundException("Proposal", "id", proposalId));

        ProposalResultsResponse response = new ProposalResultsResponse();
        response.setProposalId(tally.getProposalId());
        response.setProposalTitle(tally.getProposalTitle());
        response.setProposalType(tally.getProposalType());
        response.setStatus(tally.getStatus());
        response.setTotalEligibleEquity(tally.getTotalGroupActiveEquity());
        response.setParticipatingEquity(tally.getTotalParticipatingEquity());
        response.setApproveWeight(tally.getApproveEquity());
        response.setRejectWeight(tally.getRejectEquity());
        response.setAbstainWeight(tally.getAbstainEquity());

        response.setQuorumStatus(tally.isQuorumReached() ? "REACHED" : "NOT_REACHED");
        response.setQuorumReached(tally.isQuorumReached());
        response.setQuorumPercentage(tally.getQuorumPercentage());
        response.setParticipationRatePercentage(tally.getParticipationRatePercentage());

        response.setThreshold(tally.getRequiredThresholdPercentage());
        response.setThresholdType(tally.getThresholdType());
        if (proposal.getProposalType() == ProposalType.ROUTINE_EXPENSE) {
            response.setThresholdDescription("> 50.00% of participating equity");
        } else {
            response.setThresholdDescription(">= 75.00% of total eligible equity");
        }

        // Determine final decision
        String finalDecision;
        if (proposal.getStatus() == ProposalStatus.PASSED) {
            finalDecision = "PASSED";
        } else if (proposal.getStatus() == ProposalStatus.REJECTED) {
            finalDecision = "REJECTED";
        } else if (proposal.getStatus() == ProposalStatus.EXPIRED) {
            finalDecision = "EXPIRED";
        } else {
            // Active proposal: evaluation based on current tally
            if (!tally.isQuorumReached()) {
                finalDecision = "QUORUM_NOT_MET";
            } else if (tally.isPassed()) {
                finalDecision = "PASSED";
            } else {
                finalDecision = "REJECTED";
            }
        }

        response.setFinalDecision(finalDecision);
        response.setPassed(tally.isPassed());
        response.setDecisionReason(tally.getOutcomeReason());

        return response;
    }
}
