package com.example.evshare.controller;

import com.example.evshare.dto.request.IssueOwnershipShareRequest;
import com.example.evshare.dto.request.UpdateOwnershipShareRequest;
import com.example.evshare.dto.response.ApiResponse;
import com.example.evshare.dto.response.OwnershipHistoryResponse;
import com.example.evshare.dto.response.OwnershipShareResponse;
import com.example.evshare.security.SecurityRoles;
import com.example.evshare.service.OwnershipShareService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/ownership-groups/{groupId}/shares")
@Tag(name = "Ownership Shares", description = "Fractional equity certificates and member share management endpoints")
public class OwnershipShareController {

    private final OwnershipShareService ownershipShareService;

    public OwnershipShareController(OwnershipShareService ownershipShareService) {
        this.ownershipShareService = ownershipShareService;
    }

    @PostMapping
    @PreAuthorize(SecurityRoles.HAS_ROLE_ADMIN)
    @Operation(summary = "Issue an equity share certificate to a user",
            description = "Issues share certificate with percentage validation. Restricted to ADMIN.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Share issued successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid percentage or request payload"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Authentication required"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden: Insufficient role permissions"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Group or user not found"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "User already holds active share in group")
    })
    public ResponseEntity<ApiResponse<OwnershipShareResponse>> issueShare(
            @PathVariable Long groupId,
            @Valid @RequestBody IssueOwnershipShareRequest request
    ) {
        OwnershipShareResponse response = ownershipShareService.issueShare(groupId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Equity share certificate issued successfully", response));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('STAFF', 'ADMIN') or (hasRole('CO_OWNER') and @ownershipSecurity.isGroupMember(#groupId, principal.id))")
    @Operation(summary = "List ownership shares for a group",
            description = "Accessible by Staff, Admin, or enrolled Co-owners of this group via Ownership ACL.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Shares retrieved successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Authentication required"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden: User is not an active co-owner in this group"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Group not found")
    })
    public ResponseEntity<ApiResponse<List<OwnershipShareResponse>>> getSharesByGroup(
            @PathVariable Long groupId,
            @RequestParam(name = "activeOnly", required = false, defaultValue = "true") Boolean activeOnly
    ) {
        List<OwnershipShareResponse> response = ownershipShareService.getSharesByGroup(groupId, activeOnly);
        return ResponseEntity.ok(ApiResponse.ok("Group ownership shares retrieved successfully", response));
    }

    @GetMapping("/{shareId}")
    @PreAuthorize("hasAnyRole('STAFF', 'ADMIN') or (hasRole('CO_OWNER') and @ownershipSecurity.isGroupMember(#groupId, principal.id))")
    @Operation(summary = "Get single ownership share details",
            description = "Accessible by Staff, Admin, or enrolled Co-owners of this group.")
    public ResponseEntity<ApiResponse<OwnershipShareResponse>> getShareById(
            @PathVariable Long groupId,
            @PathVariable Long shareId
    ) {
        OwnershipShareResponse response = ownershipShareService.getShareById(shareId);
        return ResponseEntity.ok(ApiResponse.ok("Ownership share details retrieved successfully", response));
    }

    @PatchMapping("/{shareId}")
    @PreAuthorize(SecurityRoles.HAS_ROLE_ADMIN)
    @Operation(summary = "Update share percentage or active status",
            description = "Transactionally updates percentage or active flag. Restricted to ADMIN.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Share updated successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid percentage or parameters"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Share not found")
    })
    public ResponseEntity<ApiResponse<OwnershipShareResponse>> updateShare(
            @PathVariable Long groupId,
            @PathVariable Long shareId,
            @Valid @RequestBody UpdateOwnershipShareRequest request
    ) {
        OwnershipShareResponse response = ownershipShareService.updateShare(groupId, shareId, request);
        return ResponseEntity.ok(ApiResponse.ok("Ownership share updated successfully", response));
    }

    @DeleteMapping("/{shareId}")
    @PreAuthorize(SecurityRoles.HAS_ROLE_ADMIN)
    @Operation(summary = "Deactivate ownership share",
            description = "Deactivates active equity share. Restricted to ADMIN.")
    public ResponseEntity<ApiResponse<Void>> deactivateShare(
            @PathVariable Long groupId,
            @PathVariable Long shareId
    ) {
        ownershipShareService.deactivateShare(groupId, shareId);
        return ResponseEntity.ok(ApiResponse.ok("Ownership share deactivated successfully", null));
    }

    @PostMapping("/{shareId}/reactivate")
    @PreAuthorize(SecurityRoles.HAS_ROLE_ADMIN)
    @Operation(summary = "Reactivate deactivated ownership share",
            description = "Reactivates equity share. Restricted to ADMIN.")
    public ResponseEntity<ApiResponse<Void>> reactivateShare(
            @PathVariable Long groupId,
            @PathVariable Long shareId
    ) {
        ownershipShareService.reactivateShare(groupId, shareId);
        return ResponseEntity.ok(ApiResponse.ok("Ownership share reactivated successfully", null));
    }

    @PostMapping("/validate")
    @PreAuthorize("hasAnyRole('STAFF', 'ADMIN') or (hasRole('CO_OWNER') and @ownershipSecurity.isGroupMember(#groupId, principal.id))")
    @Operation(summary = "Validate 100% equity invariant for group",
            description = "Verifies active shares sum to exactly 100.00%.")
    public ResponseEntity<ApiResponse<java.math.BigDecimal>> validateShares(
            @PathVariable Long groupId
    ) {
        java.math.BigDecimal total = ownershipShareService.validateOwnershipDistribution(groupId);
        return ResponseEntity.ok(ApiResponse.ok("Ownership distribution is valid (100.00%)", total));
    }

    @PostMapping("/rebalance")
    @PreAuthorize(SecurityRoles.HAS_ROLE_ADMIN)
    @Operation(summary = "Atomically rebalance all group shares",
            description = "Rebalances equity shares ensuring exact 100.00% invariant. Restricted to ADMIN.")
    public ResponseEntity<ApiResponse<com.example.evshare.dto.response.OwnershipGroupResponse>> rebalanceShares(
            @PathVariable Long groupId,
            @Valid @RequestBody com.example.evshare.dto.request.RebalanceSharesRequest request
    ) {
        com.example.evshare.dto.response.OwnershipGroupResponse response = ownershipShareService.rebalanceShares(groupId, request);
        return ResponseEntity.ok(ApiResponse.ok("Group equity shares rebalanced successfully", response));
    }

    @GetMapping("/{shareId}/history")
    @PreAuthorize("hasAnyRole('STAFF', 'ADMIN') or (hasRole('CO_OWNER') and @ownershipSecurity.isGroupMember(#groupId, principal.id))")
    @Operation(summary = "Get share ownership audit history",
            description = "Retrieves chronological audit trail of ownership changes for a specific share.")
    public ResponseEntity<ApiResponse<List<OwnershipHistoryResponse>>> getShareHistory(
            @PathVariable Long groupId,
            @PathVariable Long shareId
    ) {
        List<OwnershipHistoryResponse> response = ownershipShareService.getShareHistory(groupId, shareId);
        return ResponseEntity.ok(ApiResponse.ok("Share ownership history retrieved successfully", response));
    }

    @GetMapping("/history")
    @PreAuthorize("hasAnyRole('STAFF', 'ADMIN') or (hasRole('CO_OWNER') and @ownershipSecurity.isGroupMember(#groupId, principal.id))")
    @Operation(summary = "Get group ownership audit history",
            description = "Retrieves complete chronological audit trail of all ownership changes across the group.")
    public ResponseEntity<ApiResponse<List<OwnershipHistoryResponse>>> getGroupOwnershipHistory(
            @PathVariable Long groupId
    ) {
        List<OwnershipHistoryResponse> response = ownershipShareService.getGroupOwnershipHistory(groupId);
        return ResponseEntity.ok(ApiResponse.ok("Group ownership history retrieved successfully", response));
    }
}
