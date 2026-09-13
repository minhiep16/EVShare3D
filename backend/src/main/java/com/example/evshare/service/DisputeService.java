package com.example.evshare.service;

import com.example.evshare.dto.request.AddMediationNotesRequest;
import com.example.evshare.dto.request.AdminArbitrateDisputeRequest;
import com.example.evshare.dto.request.CreateDisputeEvidenceRequest;
import com.example.evshare.dto.request.CreateDisputeRequest;
import com.example.evshare.dto.request.DisputeFundAdjustmentRequest;
import com.example.evshare.dto.request.ProposeResolutionRequest;
import com.example.evshare.dto.response.DisputeArbitrationDossierResponse;
import com.example.evshare.dto.response.DisputeEvidenceResponse;
import com.example.evshare.dto.response.DisputeResponse;
import com.example.evshare.entity.enums.DisputeStatus;

import java.util.List;

public interface DisputeService {

    /**
     * Files a new dispute grievance within an ownership syndicate group.
     * Enforces:
     * - Creator is an active co-owner in the group or platform staff/admin
     * - Ownership group exists and is active
     * - Related usage session (if provided) exists and belongs to group vehicle
     * - Respondent user (if provided) exists and is not the complainant
     * - Mandatory non-blank reason (title and description)
     * - Evidence requirement: at least one valid evidence record must be attached
     *
     * @param request the dispute creation payload
     * @param complainantUserId the authenticated complainant user ID
     * @return the created dispute response with attached evidence
     */
    DisputeResponse createDispute(CreateDisputeRequest request, Long complainantUserId);

    /**
     * Retrieves a dispute by its unique ID, including all associated evidence items.
     *
     * @param disputeId the dispute ID
     * @return the dispute details response
     */
    DisputeResponse getDisputeById(Long disputeId);

    /**
     * Retrieves all disputes belonging to an ownership syndicate group,
     * optionally filtered by lifecycle status.
     *
     * @param groupId the ownership group ID
     * @param status optional status filter (null for all)
     * @return list of dispute responses
     */
    List<DisputeResponse> getDisputesByGroupId(Long groupId, DisputeStatus status);

    /**
     * Authoritatively transitions the dispute lifecycle status in accordance with DisputeStateMachine.
     * Transitions: OPEN -> UNDER_REVIEW, RESOLVED, ESCALATED; UNDER_REVIEW -> RESOLVED, ESCALATED; ESCALATED -> RESOLVED.
     * Enforces terminal immutability for RESOLVED and persists an immutable audit log entry.
     *
     * @param disputeId the dispute ID
     * @param targetStatus the target lifecycle status
     * @param reason the context or rationale for the status change
     * @param resolutionSummary the agreed resolution terms (required if target is RESOLVED)
     * @param actorUserId the user ID executing the transition
     * @return the updated dispute response
     */
    DisputeResponse transitionDisputeStatus(Long disputeId, DisputeStatus targetStatus, String reason, String resolutionSummary, Long actorUserId);

    /**
     * Attaches supplementary evidence to an ongoing dispute.
     *
     * @param disputeId the dispute ID
     * @param request the evidence upload payload
     * @param uploaderUserId the user ID uploading the evidence
     * @return the persisted dispute evidence response
     */
    DisputeEvidenceResponse addEvidence(Long disputeId, CreateDisputeEvidenceRequest request, Long uploaderUserId);

    /**
     * Retrieves all evidence records associated with the specified dispute.
     *
     * @param disputeId the dispute ID
     * @return list of evidence responses
     */
    List<DisputeEvidenceResponse> getDisputeEvidences(Long disputeId);

    /**
     * Retrieves a specific evidence record belonging to a dispute.
     *
     * @param disputeId the dispute ID
     * @param evidenceId the evidence ID
     * @return the evidence response
     */
    DisputeEvidenceResponse getDisputeEvidenceById(Long disputeId, Long evidenceId);

    /**
     * Retrieves the chronological audit history for a dispute, including lifecycle
     * transitions and evidence attachment events.
     *
     * @param disputeId the dispute ID
     * @return list of audit log responses
     */
    List<com.example.evshare.dto.response.DisputeAuditLogResponse> getDisputeHistory(Long disputeId);

    /**
     * Records staff mediation review notes and factual observations for an ongoing dispute.
     * Restricted to users with ROLE_STAFF or ROLE_ADMIN.
     *
     * @param disputeId the dispute ID
     * @param request the mediation notes payload
     * @param staffUserId the ID of the authenticated staff member
     * @return the updated dispute response
     */
    DisputeResponse addMediationNotes(Long disputeId, AddMediationNotesRequest request, Long staffUserId);

    /**
     * Formulates and records a staff proposed resolution recommendation for an ongoing dispute.
     * Restricted to users with ROLE_STAFF or ROLE_ADMIN.
     *
     * @param disputeId the dispute ID
     * @param request the proposed resolution payload
     * @param staffUserId the ID of the authenticated staff member
     * @return the updated dispute response
     */
    DisputeResponse proposeResolution(Long disputeId, ProposeResolutionRequest request, Long staffUserId);

    /**
     * Retrieves all disputes pending staff review or filtered by lifecycle status.
     * Restricted to users with ROLE_STAFF or ROLE_ADMIN.
     *
     * @param status optional lifecycle status filter (null for all pending: OPEN, UNDER_REVIEW, ESCALATED)
     * @return list of dispute responses
     */
    List<DisputeResponse> getDisputesForStaffReview(DisputeStatus status);

    /**
     * Executes final binding dispute arbitration by a platform administrator per BR-DIS-05.
     * Enforces:
     * - Administrator authorization (ROLE_ADMIN strictly required; staff and co-owners rejected with 403)
     * - Mandatory reason justifying the arbitration decision
     * - Mandatory resolution terms and record
     * - Verification of attached evidence review
     * - Lifecycle transition validation to terminal RESOLVED state
     * - Audit log persistence (DISPUTE_ARBITRATED)
     *
     * @param disputeId the dispute ID
     * @param request the arbitration ruling payload
     * @param adminUserId the authenticated administrator user ID
     * @return the arbitrated and resolved dispute response
     */
    DisputeResponse arbitrateDispute(Long disputeId, AdminArbitrateDisputeRequest request, Long adminUserId);

    /**
     * Retrieves the comprehensive arbitration dossier for evidence, mediation notes,
     * and audit history review by platform administrators prior to issuing a final ruling.
     *
     * @param disputeId the dispute ID
     * @param adminUserId the authenticated administrator user ID
     * @return the full arbitration dossier
     */
    DisputeArbitrationDossierResponse getArbitrationDossier(Long disputeId, Long adminUserId);

    /**
     * Executes final binding dispute arbitration integrated with an atomic SharedFund financial adjustment per BR-DIS-06.
     * Enforces:
     * - Administrator authorization (ROLE_ADMIN strictly required)
     * - Prevention of duplicate resolution (throws 409 Conflict if dispute is already RESOLVED)
     * - Pessimistic write lock on the syndicate SharedFund
     * - Balance validation (for DEBIT adjustments, throws InsufficientFundBalanceException if balance < amount)
     * - Creation of an immutable FundTransaction with bidirectional dispute reference
     * - Atomic rollback of dispute resolution if fund adjustment fails
     * - Comprehensive dual audit trail (Dispute and SharedFund)
     *
     * @param disputeId the dispute ID
     * @param request the dispute fund adjustment payload
     * @param adminUserId the authenticated administrator user ID
     * @return the arbitrated and financially adjusted dispute response
     */
    DisputeResponse arbitrateDisputeWithFundAdjustment(Long disputeId, DisputeFundAdjustmentRequest request, Long adminUserId);
}
