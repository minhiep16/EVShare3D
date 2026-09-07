package com.example.evshare.controller;

import com.example.evshare.dto.request.CreateContractRequest;
import com.example.evshare.dto.request.SignContractRequest;
import com.example.evshare.dto.request.TransitionContractStatusRequest;
import com.example.evshare.dto.request.UpdateContractRequest;
import com.example.evshare.dto.response.ApiResponse;
import com.example.evshare.dto.response.ContractResponse;
import com.example.evshare.dto.response.ContractSignatureResponse;
import com.example.evshare.dto.response.ContractSignaturesOverviewResponse;
import com.example.evshare.security.SecurityRoles;
import com.example.evshare.security.UserPrincipal;
import com.example.evshare.service.ContractService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/contracts")
@Tag(name = "Co-Ownership Contracts", description = "Digital co-ownership legal agreement lifecycle and versioning endpoints")
public class ContractController {

    private final ContractService contractService;

    public ContractController(ContractService contractService) {
        this.contractService = contractService;
    }

    @PostMapping
    @PreAuthorize(SecurityRoles.HAS_STAFF_OR_ADMIN)
    @Operation(summary = "Draft a new co-ownership contract",
            description = "Creates a new contract draft with incremental version number for the ownership group. Restricted to STAFF or ADMIN.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Contract draft created successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Validation error or invalid group"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Authentication required"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden: Insufficient role permissions")
    })
    public ResponseEntity<ApiResponse<ContractResponse>> createContract(
            @Valid @RequestBody CreateContractRequest request
    ) {
        ContractResponse response = contractService.createContract(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Co-ownership contract draft created successfully", response));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('STAFF', 'ADMIN') or (hasRole('CO_OWNER') and @ownershipSecurity.isContractGroupMember(#id, principal.id))")
    @Operation(summary = "Get co-ownership contract details",
            description = "Accessible by Staff, Admin, or enrolled Co-owners of the bound group.")
    public ResponseEntity<ApiResponse<ContractResponse>> getContractById(
            @PathVariable Long id
    ) {
        ContractResponse response = contractService.getContractById(id);
        return ResponseEntity.ok(ApiResponse.ok("Contract details retrieved successfully", response));
    }

    @PutMapping("/{id}")
    @PreAuthorize(SecurityRoles.HAS_STAFF_OR_ADMIN)
    @Operation(summary = "Update a draft co-ownership contract",
            description = "Modifies terms while in DRAFT status. Non-draft contracts are legally frozen and reject updates with 409 Conflict.")
    public ResponseEntity<ApiResponse<ContractResponse>> updateDraftContract(
            @PathVariable Long id,
            @Valid @RequestBody UpdateContractRequest request
    ) {
        ContractResponse response = contractService.updateDraftContract(id, request);
        return ResponseEntity.ok(ApiResponse.ok("Draft contract updated successfully", response));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize(SecurityRoles.HAS_STAFF_OR_ADMIN)
    @Operation(summary = "Transition contract lifecycle status",
            description = "Controls transition between DRAFT, PENDING_SIGNATURE, SIGNED, ACTIVE, EXPIRED, TERMINATED.")
    public ResponseEntity<ApiResponse<ContractResponse>> transitionStatus(
            @PathVariable Long id,
            @Valid @RequestBody TransitionContractStatusRequest request
    ) {
        ContractResponse response = contractService.transitionStatus(id, request);
        return ResponseEntity.ok(ApiResponse.ok("Contract status transitioned successfully", response));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize(SecurityRoles.HAS_STAFF_OR_ADMIN)
    @Operation(summary = "Reject deletion of historical contract",
            description = "Historical contract records must remain auditable and cannot be deleted.")
    public ResponseEntity<ApiResponse<Void>> deleteContract(
            @PathVariable Long id
    ) {
        contractService.deleteContract(id);
        return ResponseEntity.ok(ApiResponse.ok("Deleted", null));
    }

    @GetMapping("/group/{groupId}")
    @PreAuthorize("hasAnyRole('STAFF', 'ADMIN') or (hasRole('CO_OWNER') and @ownershipSecurity.isGroupMember(#groupId, principal.id))")
    @Operation(summary = "List all contract versions for an ownership group",
            description = "Returns complete chronological version history for the syndicate.")
    public ResponseEntity<ApiResponse<List<ContractResponse>>> getContractsByGroupId(
            @PathVariable Long groupId
    ) {
        List<ContractResponse> response = contractService.getContractsByGroupId(groupId);
        return ResponseEntity.ok(ApiResponse.ok("Group contracts retrieved successfully", response));
    }

    @GetMapping("/group/{groupId}/active")
    @PreAuthorize("hasAnyRole('STAFF', 'ADMIN') or (hasRole('CO_OWNER') and @ownershipSecurity.isGroupMember(#groupId, principal.id))")
    @Operation(summary = "Get active contract for an ownership group",
            description = "Returns currently active legal agreement governing the syndicate.")
    public ResponseEntity<ApiResponse<ContractResponse>> getActiveContractByGroupId(
            @PathVariable Long groupId
    ) {
        ContractResponse response = contractService.getActiveContractByGroupId(groupId);
        return ResponseEntity.ok(ApiResponse.ok("Active contract retrieved successfully", response));
    }

    @PostMapping("/{id}/sign")
    @PreAuthorize("hasRole('CO_OWNER') and @ownershipSecurity.isContractGroupMember(#id, principal.id)")
    @Operation(summary = "Digitally sign a co-ownership contract",
            description = "Submits a cryptographic SHA-256 digital signature binding the terms, version, signer, timestamp, and IP address.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Contract successfully signed"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Contract is in DRAFT or terms not accepted"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Authentication required"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden: Signer not in syndicate"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Conflict: Duplicate signature or invalid status")
    })
    public ResponseEntity<ApiResponse<ContractSignatureResponse>> signContract(
            @PathVariable Long id,
            @Valid @RequestBody SignContractRequest request,
            @AuthenticationPrincipal UserPrincipal principal,
            HttpServletRequest servletRequest
    ) {
        String ip = servletRequest.getHeader("X-Forwarded-For");
        if (ip == null || ip.isBlank() || "unknown".equalsIgnoreCase(ip)) {
            ip = servletRequest.getRemoteAddr();
        }
        if (ip == null || ip.isBlank()) {
            ip = "127.0.0.1";
        }
        if (ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }

        ContractSignatureResponse response = contractService.signContract(id, principal.getId(), request, ip);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Contract signed successfully", response));
    }

    @GetMapping("/{id}/signatures")
    @PreAuthorize("hasAnyRole('STAFF', 'ADMIN') or (hasRole('CO_OWNER') and @ownershipSecurity.isContractGroupMember(#id, principal.id))")
    @Operation(summary = "View contract signatures and pending signers",
            description = "Accessible by Staff, Admin, or enrolled Co-owners of the bound syndicate.")
    public ResponseEntity<ApiResponse<ContractSignaturesOverviewResponse>> getSignaturesOverview(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        ContractSignaturesOverviewResponse response = contractService.getSignaturesOverview(id, principal != null ? principal.getId() : null);
        return ResponseEntity.ok(ApiResponse.ok("Contract signatures retrieved successfully", response));
    }
}
