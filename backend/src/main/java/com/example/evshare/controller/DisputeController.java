package com.example.evshare.controller;

import com.example.evshare.dto.request.AddMediationNotesRequest;
import com.example.evshare.dto.request.AdminArbitrateDisputeRequest;
import com.example.evshare.dto.request.CreateDisputeEvidenceRequest;
import com.example.evshare.dto.request.CreateDisputeRequest;
import com.example.evshare.dto.request.DisputeFundAdjustmentRequest;
import com.example.evshare.dto.request.ProposeResolutionRequest;
import com.example.evshare.dto.request.TransitionDisputeStatusRequest;
import com.example.evshare.dto.response.ApiResponse;
import com.example.evshare.dto.response.DisputeArbitrationDossierResponse;
import com.example.evshare.dto.response.DisputeAuditLogResponse;
import com.example.evshare.dto.response.DisputeEvidenceResponse;
import com.example.evshare.dto.response.DisputeResponse;
import com.example.evshare.entity.enums.DisputeStatus;
import com.example.evshare.exception.BusinessException;
import com.example.evshare.security.UserPrincipal;
import com.example.evshare.service.DisputeService;
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
@RequestMapping("/api/v1/disputes")
@Tag(name = "Dispute Resolution & Arbitration", description = "Endpoints for filing grievances, uploading evidence, and arbitrating co-owner disputes per BR-DIS-01")
@SecurityRequirement(name = "bearerAuth")
public class DisputeController {

    private final DisputeService disputeService;

    public DisputeController(DisputeService disputeService) {
        this.disputeService = disputeService;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('CO_OWNER', 'STAFF', 'ADMIN')")
    @Operation(
            summary = "File new dispute grievance with evidence",
            description = "Files a dispute within an ownership group. Enforces active group membership, valid related usage session or respondent, and mandatory evidence."
    )
    public ResponseEntity<ApiResponse<DisputeResponse>> createDispute(
            @Valid @RequestBody CreateDisputeRequest request,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        Long currentUserId = principal != null ? principal.getId() : null;
        DisputeResponse response = disputeService.createDispute(request, currentUserId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Dispute filed successfully", response));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('STAFF', 'ADMIN') or (hasRole('CO_OWNER') and @ownershipSecurity.isDisputeGroupMember(#id, principal.id))")
    @Operation(
            summary = "Retrieve dispute details by ID",
            description = "Retrieves dispute details and attached evidence. Restricted to syndicate co-owners, platform staff, and administrators."
    )
    public ResponseEntity<ApiResponse<DisputeResponse>> getDisputeById(
            @PathVariable Long id
    ) {
        DisputeResponse response = disputeService.getDisputeById(id);
        return ResponseEntity.ok(ApiResponse.ok("Dispute retrieved successfully", response));
    }

    @GetMapping("/group/{groupId}")
    @PreAuthorize("hasAnyRole('STAFF', 'ADMIN') or (hasRole('CO_OWNER') and @ownershipSecurity.isGroupMember(#groupId, principal.id))")
    @Operation(
            summary = "List disputes for an ownership group",
            description = "Retrieves all disputes for the specified syndicate group, with optional lifecycle status filter."
    )
    public ResponseEntity<ApiResponse<List<DisputeResponse>>> getDisputesByGroupId(
            @PathVariable Long groupId,
            @RequestParam(required = false) DisputeStatus status
    ) {
        List<DisputeResponse> responses = disputeService.getDisputesByGroupId(groupId, status);
        return ResponseEntity.ok(ApiResponse.ok("Group disputes retrieved successfully", responses));
    }

    @PostMapping("/{id}/transition")
    @PreAuthorize("hasAnyRole('STAFF', 'ADMIN')")
    @Operation(
            summary = "Transition dispute lifecycle status",
            description = "Transitions a dispute status (OPEN -> UNDER_REVIEW, RESOLVED, ESCALATED; UNDER_REVIEW -> RESOLVED, ESCALATED; ESCALATED -> RESOLVED) per DisputeStateMachine."
    )
    public ResponseEntity<ApiResponse<DisputeResponse>> transitionDisputeStatus(
            @PathVariable Long id,
            @Valid @RequestBody TransitionDisputeStatusRequest request,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        Long currentUserId = principal != null ? principal.getId() : null;
        DisputeResponse response = disputeService.transitionDisputeStatus(
                id, request.getTargetStatus(), request.getReason(), request.getResolutionSummary(), currentUserId
        );
        return ResponseEntity.ok(ApiResponse.ok("Dispute status transitioned successfully", response));
    }

    @PostMapping("/{id}/evidence")
    @PreAuthorize("hasAnyRole('STAFF', 'ADMIN') or (hasRole('CO_OWNER') and @ownershipSecurity.isDisputeGroupMember(#id, principal.id))")
    @Operation(
            summary = "Attach supplementary evidence to dispute",
            description = "Uploads additional evidence (media URL, 3D mesh defect annotations, description) to an active dispute."
    )
    public ResponseEntity<ApiResponse<DisputeEvidenceResponse>> addEvidence(
            @PathVariable Long id,
            @Valid @RequestBody CreateDisputeEvidenceRequest request,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        Long currentUserId = principal != null ? principal.getId() : null;
        DisputeEvidenceResponse response = disputeService.addEvidence(id, request, currentUserId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Evidence attached successfully", response));
    }

    @GetMapping("/{id}/evidence")
    @PreAuthorize("hasAnyRole('STAFF', 'ADMIN') or (hasRole('CO_OWNER') and @ownershipSecurity.isDisputeGroupMember(#id, principal.id))")
    @Operation(
            summary = "List all evidence for a dispute",
            description = "Retrieves all evidence attachments associated with the dispute."
    )
    public ResponseEntity<ApiResponse<List<DisputeEvidenceResponse>>> getDisputeEvidences(
            @PathVariable Long id
    ) {
        List<DisputeEvidenceResponse> responses = disputeService.getDisputeEvidences(id);
        return ResponseEntity.ok(ApiResponse.ok("Dispute evidence list retrieved successfully", responses));
    }

    @GetMapping("/{id}/evidence/{evidenceId}")
    @PreAuthorize("hasAnyRole('STAFF', 'ADMIN') or (hasRole('CO_OWNER') and @ownershipSecurity.isDisputeGroupMember(#id, principal.id))")
    @Operation(
            summary = "Retrieve specific dispute evidence by ID",
            description = "Retrieves an immutable dispute evidence record by ID, including uploader, URL, and 3D defect coordinate mesh annotations."
    )
    public ResponseEntity<ApiResponse<DisputeEvidenceResponse>> getDisputeEvidenceById(
            @PathVariable Long id,
            @PathVariable Long evidenceId
    ) {
        DisputeEvidenceResponse response = disputeService.getDisputeEvidenceById(id, evidenceId);
        return ResponseEntity.ok(ApiResponse.ok("Dispute evidence retrieved successfully", response));
    }

    @GetMapping("/{id}/history")
    @PreAuthorize("hasAnyRole('STAFF', 'ADMIN') or (hasRole('CO_OWNER') and @ownershipSecurity.isDisputeGroupMember(#id, principal.id))")
    @Operation(
            summary = "Retrieve dispute audit trail history",
            description = "Retrieves chronological audit events for the dispute, including creation, status transitions, and evidence attachments."
    )
    public ResponseEntity<ApiResponse<List<DisputeAuditLogResponse>>> getDisputeHistory(
            @PathVariable Long id
    ) {
        List<DisputeAuditLogResponse> history = disputeService.getDisputeHistory(id);
        return ResponseEntity.ok(ApiResponse.ok("Dispute history retrieved successfully", history));
    }

    @PutMapping("/{id}/evidence/{evidenceId}")
    @Operation(
            summary = "Reject evidence mutation (immutable)",
            description = "Dispute evidence items represent legal arbitration records and cannot be mutated or replaced."
    )
    public ResponseEntity<Void> updateEvidence(@PathVariable Long id, @PathVariable Long evidenceId) {
        throw new BusinessException("Dispute evidence is immutable and cannot be modified or replaced", HttpStatus.METHOD_NOT_ALLOWED);
    }

    @DeleteMapping("/{id}/evidence/{evidenceId}")
    @Operation(
            summary = "Reject evidence deletion (immutable)",
            description = "Dispute evidence items represent legal arbitration records and cannot be deleted."
    )
    public ResponseEntity<Void> deleteEvidence(@PathVariable Long id, @PathVariable Long evidenceId) {
        throw new BusinessException("Dispute evidence is immutable and cannot be deleted", HttpStatus.METHOD_NOT_ALLOWED);
    }

    @GetMapping("/staff/review")
    @PreAuthorize("hasAnyRole('STAFF', 'ADMIN')")
    @Operation(
            summary = "List disputes pending staff review or by status",
            description = "Retrieves disputes requiring staff review or mediation. Restricted to staff and administrators per BR-DIS-01 and RBAC rules."
    )
    public ResponseEntity<ApiResponse<List<DisputeResponse>>> getDisputesForStaffReview(
            @RequestParam(required = false) DisputeStatus status
    ) {
        List<DisputeResponse> responses = disputeService.getDisputesForStaffReview(status);
        return ResponseEntity.ok(ApiResponse.ok("Disputes for staff review retrieved successfully", responses));
    }

    @PostMapping("/{id}/mediation-notes")
    @PreAuthorize("hasAnyRole('STAFF', 'ADMIN')")
    @Operation(
            summary = "Add staff mediation review notes",
            description = "Records mediation review notes and factual observations. Restricted to staff and administrators."
    )
    public ResponseEntity<ApiResponse<DisputeResponse>> addMediationNotes(
            @PathVariable Long id,
            @Valid @RequestBody AddMediationNotesRequest request,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        Long currentUserId = principal != null ? principal.getId() : null;
        DisputeResponse response = disputeService.addMediationNotes(id, request, currentUserId);
        return ResponseEntity.ok(ApiResponse.ok("Mediation notes recorded successfully", response));
    }

    @PostMapping("/{id}/propose-resolution")
    @PreAuthorize("hasAnyRole('STAFF', 'ADMIN')")
    @Operation(
            summary = "Propose staff resolution recommendation",
            description = "Formulates a proposed resolution recommendation. Restricted to staff and administrators."
    )
    public ResponseEntity<ApiResponse<DisputeResponse>> proposeResolution(
            @PathVariable Long id,
            @Valid @RequestBody ProposeResolutionRequest request,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        Long currentUserId = principal != null ? principal.getId() : null;
        DisputeResponse response = disputeService.proposeResolution(id, request, currentUserId);
        return ResponseEntity.ok(ApiResponse.ok("Proposed resolution recorded successfully", response));
    }

    @PostMapping("/{id}/arbitrate")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
            summary = "Execute administrator final binding dispute arbitration",
            description = "Executes final binding arbitration for a dispute per BR-DIS-05. Strictly restricted to administrators (ROLE_ADMIN). Ordinary co-owners and staff are forbidden."
    )
    public ResponseEntity<ApiResponse<DisputeResponse>> arbitrateDispute(
            @PathVariable Long id,
            @Valid @RequestBody AdminArbitrateDisputeRequest request,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        Long currentUserId = principal != null ? principal.getId() : null;
        DisputeResponse response = disputeService.arbitrateDispute(id, request, currentUserId);
        return ResponseEntity.ok(ApiResponse.ok("Dispute arbitrated and resolved successfully", response));
    }

    @GetMapping("/{id}/arbitration-dossier")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
            summary = "Retrieve dispute arbitration dossier for evidence review",
            description = "Retrieves complete arbitration dossier including all evidence attachments, mediation notes, and full audit history. Strictly restricted to administrators (ROLE_ADMIN)."
    )
    public ResponseEntity<ApiResponse<DisputeArbitrationDossierResponse>> getArbitrationDossier(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        Long currentUserId = principal != null ? principal.getId() : null;
        DisputeArbitrationDossierResponse response = disputeService.getArbitrationDossier(id, currentUserId);
        return ResponseEntity.ok(ApiResponse.ok("Dispute arbitration dossier retrieved successfully", response));
    }

    @PostMapping("/{id}/fund-adjustment")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
            summary = "Arbitrate dispute with SharedFund transactional adjustment",
            description = "Executes final binding arbitration with an atomic SharedFund balance adjustment (DEBIT/CREDIT) per BR-DIS-06. Strictly restricted to administrators (ROLE_ADMIN)."
    )
    public ResponseEntity<ApiResponse<DisputeResponse>> arbitrateDisputeWithFundAdjustment(
            @PathVariable Long id,
            @Valid @RequestBody DisputeFundAdjustmentRequest request,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        Long currentUserId = principal != null ? principal.getId() : null;
        DisputeResponse response = disputeService.arbitrateDisputeWithFundAdjustment(id, request, currentUserId);
        return ResponseEntity.ok(ApiResponse.ok("Dispute arbitrated and fund adjusted successfully", response));
    }
}
