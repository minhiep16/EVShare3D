package com.example.evshare.service;

import com.example.evshare.dto.request.CreateProposalRequest;
import com.example.evshare.dto.response.ProposalResponse;
import com.example.evshare.entity.enums.ProposalStatus;

import java.util.List;

public interface VotingService {

    /**
     * Creates a new decision chamber proposal with automated options seeding (APPROVE, REJECT, ABSTAIN).
     * Enforces ownership group existence, active proposer membership, category validation,
     * required title/description, voting deadline constraints, and minimum 10.00% active equity eligibility threshold per BR-VOT-01.
     *
     * @param request the proposal creation payload
     * @param proposerUserId the authenticated user ID initiating the proposal
     * @return the created proposal response with seeded voting options
     */
    ProposalResponse createProposal(CreateProposalRequest request, Long proposerUserId);

    /**
     * Retrieves a proposal by ID including its available voting options.
     *
     * @param proposalId the proposal ID
     * @return the proposal response
     */
    ProposalResponse getProposalById(Long proposalId);

    /**
     * Retrieves all proposals for an ownership group, optionally filtered by status.
     *
     * @param groupId the ownership group ID
     * @param status optional status filter (null to retrieve all)
     * @return list of proposal responses
     */
    List<ProposalResponse> getProposalsByGroupId(Long groupId, ProposalStatus status);

    /**
     * Authoritatively transitions a proposal lifecycle status in accordance with ProposalStateMachine.
     * Validates transition rules, enforces terminal state immutability, updates database state,
     * and immutably records an audit log entry.
     *
     * @param proposalId the proposal ID to transition
     * @param targetStatus the desired target status (PASSED, REJECTED, EXPIRED)
     * @param reason context or explanation for the transition
     * @param actorUserId the user ID executing the transition
     * @return the updated proposal response
     */
    ProposalResponse transitionProposalStatus(Long proposalId, ProposalStatus targetStatus, String reason, Long actorUserId);

    /**
     * Retrieves chronological audit transition history for the specified proposal.
     *
     * @param proposalId the proposal ID
     * @return list of audit log entries for the proposal
     */
    List<com.example.evshare.dto.response.ProposalAuditLogResponse> getProposalHistory(Long proposalId);

    /**
     * Evaluates whether an ACTIVE proposal has exceeded its voting deadline and automatically
     * transitions it to EXPIRED if quorum has not been satisfied.
     *
     * @param proposalId the proposal ID
     * @return the evaluated proposal response
     */
    ProposalResponse checkAndExpireIfDeadlinePassed(Long proposalId);

    /**
     * Validates whether a user meets all eligibility criteria to sponsor a proposal in a group.
     * Enforces active membership and minimum 10.00% equity stake per BR-VOT-01.
     * Throws BusinessException (HTTP 403) or InsufficientEquityException (HTTP 403) if ineligible.
     *
     * @param groupId the ownership group ID
     * @param proposerUserId the proposer's user ID
     * @return the verified active OwnershipShare of the proposer
     */
    com.example.evshare.entity.OwnershipShare validateProposerEligibility(Long groupId, Long proposerUserId);

    /**
     * Checks and evaluates proposer eligibility details for a user in an ownership group.
     *
     * @param groupId the ownership group ID
     * @param proposerUserId the user ID to evaluate
     * @return the eligibility evaluation details
     */
    com.example.evshare.dto.response.ProposerEligibilityResponse checkProposerEligibility(Long groupId, Long proposerUserId);

    /**
     * Casts a single equity-weighted ballot on an ACTIVE proposal.
     * Enforces voter authentication, active ownership in proposal's group,
     * proposal ACTIVE status, unexpired voting window, valid option choice (APPROVE, REJECT, ABSTAIN),
     * and strictly one vote per voter per proposal.
     *
     * @param proposalId the proposal ID
     * @param request the vote choice payload
     * @param voterUserId the authenticated user ID casting the ballot
     * @return the persisted vote response
     */
    com.example.evshare.dto.response.VoteResponse castVote(Long proposalId, com.example.evshare.dto.request.CastVoteRequest request, Long voterUserId);

    /**
     * Retrieves all ballots cast on the specified proposal.
     *
     * @param proposalId the proposal ID
     * @return list of cast vote responses
     */
    java.util.List<com.example.evshare.dto.response.VoteResponse> getVotesByProposalId(Long proposalId);

    /**
     * Retrieves the ballot cast by a specific user on a proposal, if any.
     *
     * @param proposalId the proposal ID
     * @param userId the voter user ID
     * @return optional vote response
     */
    java.util.Optional<com.example.evshare.dto.response.VoteResponse> getVoteByProposalAndUser(Long proposalId, Long userId);

    /**
     * Deterministically aggregates equity-weighted voting results for a proposal,
     * calculating participating equity weight, quorum satisfaction (>=60%),
     * and passing thresholds based on category (routine vs supermajority)
     * while strictly preserving individual vote history.
     *
     * @param proposalId the target proposal ID
     * @return the deterministic proposal tally response
     */
    com.example.evshare.dto.response.ProposalTallyResponse getProposalTally(Long proposalId);

    /**
     * Computes the official voting results and final decision summary for a proposal,
     * returning total eligible equity, participating equity, vote weights, quorum status,
     * threshold requirement, and final decision without exposing unauthorized individual ballot data.
     *
     * @param proposalId the target proposal ID
     * @return official proposal results response
     */
    com.example.evshare.dto.response.ProposalResultsResponse getProposalResults(Long proposalId);
}
