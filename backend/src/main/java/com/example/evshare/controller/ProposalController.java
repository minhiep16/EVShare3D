package com.example.evshare.controller;

import com.example.evshare.dto.request.CreateProposalRequest;
import com.example.evshare.dto.response.ApiResponse;
import com.example.evshare.dto.response.ProposalResponse;
import com.example.evshare.dto.response.ProposalResultsResponse;
import com.example.evshare.entity.enums.ProposalStatus;
import com.example.evshare.security.UserPrincipal;
import com.example.evshare.service.VotingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/proposals")
@Tag(name = "Decision Chamber & Governance", description = "Endpoints for syndicate proposal creation, governance deliberation, and voting")
@SecurityRequirement(name = "bearerAuth")
public class ProposalController {

    private final VotingService votingService;

    public ProposalController(VotingService votingService) {
        this.votingService = votingService;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('CO_OWNER', 'ADMIN')")
    @Operation(
            summary = "Create new governance decision proposal",
            description = "Initiates a new proposal with automated ballot options seeding (APPROVE, REJECT, ABSTAIN). Enforces minimum 10.00% active equity eligibility per BR-VOT-01."
    )
    public ResponseEntity<ApiResponse<ProposalResponse>> createProposal(
            @Valid @RequestBody CreateProposalRequest request,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        Long currentUserId = principal != null ? principal.getId() : null;
        ProposalResponse response = votingService.createProposal(request, currentUserId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Proposal created successfully in Decision Chamber", response));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('STAFF', 'ADMIN') or (hasRole('CO_OWNER') and @ownershipSecurity.isProposalGroupMember(#id, principal.id))")
    @Operation(
            summary = "Retrieve proposal details by ID",
            description = "Retrieves proposal metadata and available voting options. Scoped to syndicate group co-owners, staff, and platform administrators."
    )
    public ResponseEntity<ApiResponse<ProposalResponse>> getProposalById(
            @PathVariable Long id
    ) {
        ProposalResponse response = votingService.getProposalById(id);
        return ResponseEntity.ok(ApiResponse.ok("Proposal retrieved successfully", response));
    }

    @GetMapping("/group/{groupId}")
    @PreAuthorize("hasAnyRole('STAFF', 'ADMIN') or (hasRole('CO_OWNER') and @ownershipSecurity.isGroupMember(#groupId, principal.id))")
    @Operation(
            summary = "List proposals for an ownership group",
            description = "Retrieves all proposals for the specified syndicate group, with optional lifecycle status filtering. Scoped to group co-owners and platform operators."
    )
    public ResponseEntity<ApiResponse<List<ProposalResponse>>> getProposalsByGroupId(
            @PathVariable Long groupId,
            @RequestParam(required = false) ProposalStatus status
    ) {
        List<ProposalResponse> responses = votingService.getProposalsByGroupId(groupId, status);
        return ResponseEntity.ok(ApiResponse.ok("Group proposals retrieved successfully", responses));
    }

    @PostMapping("/{id}/transition")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
            summary = "Transition proposal lifecycle status",
            description = "Transitions a proposal between states (ACTIVE -> PASSED, REJECTED, EXPIRED) in accordance with ProposalStateMachine. Rejects invalid transitions with HTTP 409 Conflict."
    )
    public ResponseEntity<ApiResponse<ProposalResponse>> transitionProposalStatus(
            @PathVariable Long id,
            @Valid @RequestBody com.example.evshare.dto.request.TransitionProposalStatusRequest request,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        Long currentUserId = principal != null ? principal.getId() : null;
        ProposalResponse response = votingService.transitionProposalStatus(id, request.getTargetStatus(), request.getReason(), currentUserId);
        return ResponseEntity.ok(ApiResponse.ok("Proposal status transitioned successfully", response));
    }

    @GetMapping("/{id}/history")
    @PreAuthorize("hasAnyRole('STAFF', 'ADMIN') or (hasRole('CO_OWNER') and @ownershipSecurity.isProposalGroupMember(#id, principal.id))")
    @Operation(
            summary = "Get proposal transition history",
            description = "Retrieves chronological audit trail of all lifecycle events and state transitions for the proposal."
    )
    public ResponseEntity<ApiResponse<List<com.example.evshare.dto.response.ProposalAuditLogResponse>>> getProposalHistory(
            @PathVariable Long id
    ) {
        List<com.example.evshare.dto.response.ProposalAuditLogResponse> history = votingService.getProposalHistory(id);
        return ResponseEntity.ok(ApiResponse.ok("Proposal history retrieved successfully", history));
    }

    @GetMapping("/group/{groupId}/eligibility")
    @PreAuthorize("hasAnyRole('CO_OWNER', 'ADMIN')")
    @Operation(
            summary = "Check proposer eligibility for governance",
            description = "Evaluates whether the authenticated co-owner holds the required minimum 10.00% active equity in the group to sponsor proposals per BR-VOT-01."
    )
    public ResponseEntity<ApiResponse<com.example.evshare.dto.response.ProposerEligibilityResponse>> checkEligibility(
            @PathVariable Long groupId,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        Long currentUserId = principal != null ? principal.getId() : null;
        com.example.evshare.dto.response.ProposerEligibilityResponse response = votingService.checkProposerEligibility(groupId, currentUserId);
        return ResponseEntity.ok(ApiResponse.ok("Proposer eligibility evaluated successfully", response));
    }

    @PostMapping("/{id}/votes")
    @PreAuthorize("hasAnyRole('CO_OWNER', 'ADMIN')")
    @Operation(
            summary = "Cast vote on active proposal",
            description = "Casts an equity-weighted ballot (APPROVE, REJECT, ABSTAIN) on an ACTIVE proposal. Enforces active group membership and one vote per voter."
    )
    public ResponseEntity<ApiResponse<com.example.evshare.dto.response.VoteResponse>> castVote(
            @PathVariable Long id,
            @Valid @RequestBody com.example.evshare.dto.request.CastVoteRequest request,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        Long currentUserId = principal != null ? principal.getId() : null;
        com.example.evshare.dto.response.VoteResponse response = votingService.castVote(id, request, currentUserId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Vote cast successfully in Decision Chamber", response));
    }

    @GetMapping("/{id}/votes")
    @PreAuthorize("hasAnyRole('STAFF', 'ADMIN') or (hasRole('CO_OWNER') and @ownershipSecurity.isProposalGroupMember(#id, principal.id))")
    @Operation(
            summary = "List all votes for a proposal",
            description = "Retrieves all ballots cast on the specified proposal."
    )
    public ResponseEntity<ApiResponse<List<com.example.evshare.dto.response.VoteResponse>>> getVotes(
            @PathVariable Long id
    ) {
        List<com.example.evshare.dto.response.VoteResponse> responses = votingService.getVotesByProposalId(id);
        return ResponseEntity.ok(ApiResponse.ok("Proposal votes retrieved successfully", responses));
    }

    @GetMapping("/{id}/votes/my-vote")
    @PreAuthorize("hasAnyRole('CO_OWNER', 'ADMIN')")
    @Operation(
            summary = "Get current voter's ballot on proposal",
            description = "Retrieves the ballot cast by the authenticated user on the specified proposal, if any."
    )
    public ResponseEntity<ApiResponse<com.example.evshare.dto.response.VoteResponse>> getMyVote(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        Long currentUserId = principal != null ? principal.getId() : null;
        com.example.evshare.dto.response.VoteResponse response = votingService.getVoteByProposalAndUser(id, currentUserId).orElse(null);
        return ResponseEntity.ok(ApiResponse.ok("User vote status retrieved", response));
    }

    @GetMapping("/{id}/tally")
    @PreAuthorize("hasAnyRole('STAFF', 'ADMIN') or (hasRole('CO_OWNER') and @ownershipSecurity.isProposalGroupMember(#id, principal.id))")
    @Operation(
            summary = "Get deterministic equity-weighted vote tally",
            description = "Computes deterministic equity-weighted aggregation, quorum, and passing threshold results while preserving ballot history."
    )
    public ResponseEntity<ApiResponse<com.example.evshare.dto.response.ProposalTallyResponse>> getProposalTally(
            @PathVariable Long id
    ) {
        com.example.evshare.dto.response.ProposalTallyResponse response = votingService.getProposalTally(id);
        return ResponseEntity.ok(ApiResponse.ok("Proposal vote tally retrieved successfully", response));
    }

    @GetMapping("/{id}/results")
    @PreAuthorize("hasAnyRole('STAFF', 'ADMIN') or (hasRole('CO_OWNER') and @ownershipSecurity.isProposalGroupMember(#id, principal.id))")
    @Operation(
            summary = "Get official proposal voting results",
            description = "Retrieves aggregated voting metrics, quorum status, threshold evaluation, and final decision without exposing unauthorized individual ballot data."
    )
    public ResponseEntity<ApiResponse<ProposalResultsResponse>> getProposalResults(
            @PathVariable Long id
    ) {
        ProposalResultsResponse response = votingService.getProposalResults(id);
        return ResponseEntity.ok(ApiResponse.ok("Proposal results retrieved successfully", response));
    }
}
