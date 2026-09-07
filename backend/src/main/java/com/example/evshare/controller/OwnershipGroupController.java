package com.example.evshare.controller;

import com.example.evshare.dto.request.AddGroupMemberRequest;
import com.example.evshare.dto.request.CreateOwnershipGroupRequest;
import com.example.evshare.dto.request.UpdateOwnershipGroupRequest;
import com.example.evshare.dto.response.ApiResponse;
import com.example.evshare.dto.response.OwnershipGroupResponse;
import com.example.evshare.security.SecurityRoles;
import com.example.evshare.security.UserPrincipal;
import com.example.evshare.service.OwnershipGroupService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/ownership-groups")
@Tag(name = "Ownership Groups", description = "Co-ownership syndicate and equity management endpoints")
public class OwnershipGroupController {

    private final OwnershipGroupService ownershipGroupService;
    private final com.example.evshare.service.OwnershipShareService ownershipShareService;

    public OwnershipGroupController(OwnershipGroupService ownershipGroupService,
                                    com.example.evshare.service.OwnershipShareService ownershipShareService) {
        this.ownershipGroupService = ownershipGroupService;
        this.ownershipShareService = ownershipShareService;
    }

    @PostMapping
    @PreAuthorize(SecurityRoles.HAS_ROLE_ADMIN)
    @Operation(summary = "Form a new co-ownership group",
            description = "Creates syndicate and binds it to a designated EV. Restricted to ADMIN.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Group created successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid payload"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Authentication required"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden: Insufficient role permissions"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Vehicle not found"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Vehicle already bound to an ownership group")
    })
    public ResponseEntity<ApiResponse<OwnershipGroupResponse>> createGroup(
            @Valid @RequestBody CreateOwnershipGroupRequest request
    ) {
        OwnershipGroupResponse response = ownershipGroupService.createGroup(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Ownership group created successfully", response));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('STAFF', 'ADMIN') or (hasRole('CO_OWNER') and @ownershipSecurity.isGroupMember(#id, principal.id))")
    @Operation(summary = "Retrieve ownership group details by ID",
            description = "Accessible by Staff, Admin, or enrolled Co-owners of this group via Ownership ACL.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Ownership group retrieved successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Authentication required"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden: User is not an active co-owner in this syndicate"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Ownership group not found")
    })
    public ResponseEntity<ApiResponse<OwnershipGroupResponse>> getGroupById(@PathVariable Long id) {
        OwnershipGroupResponse response = ownershipGroupService.getGroupById(id);
        return ResponseEntity.ok(ApiResponse.ok("Ownership group retrieved successfully", response));
    }

    @GetMapping("/my-groups")
    @PreAuthorize(SecurityRoles.HAS_ROLE_CO_OWNER)
    @Operation(summary = "List ownership groups of the current co-owner",
            description = "Returns all active groups where the authenticated user holds equity.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Groups retrieved successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Authentication required")
    })
    public ResponseEntity<ApiResponse<List<OwnershipGroupResponse>>> getMyGroups(
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        List<OwnershipGroupResponse> response = ownershipGroupService.getMyGroups(principal.getId());
        return ResponseEntity.ok(ApiResponse.ok("User ownership groups retrieved successfully", response));
    }

    @PatchMapping("/{id}")
    @PreAuthorize(SecurityRoles.HAS_ROLE_ADMIN)
    @Operation(summary = "Update permitted group metadata",
            description = "Updates group name or active toggle. Vehicle binding is immutable. Restricted to ADMIN.")
    public ResponseEntity<ApiResponse<OwnershipGroupResponse>> updateGroup(
            @PathVariable Long id,
            @Valid @RequestBody UpdateOwnershipGroupRequest request
    ) {
        OwnershipGroupResponse response = ownershipGroupService.updateGroup(id, request);
        return ResponseEntity.ok(ApiResponse.ok("Ownership group updated successfully", response));
    }

    @PostMapping("/{id}/members")
    @PreAuthorize(SecurityRoles.HAS_ROLE_ADMIN)
    @Operation(summary = "Enroll a new member into group",
            description = "Enrolls co-owner user and generates share certificate. Restricted to ADMIN.")
    public ResponseEntity<ApiResponse<OwnershipGroupResponse>> addMember(
            @PathVariable Long id,
            @Valid @RequestBody AddGroupMemberRequest request
    ) {
        OwnershipGroupResponse response = ownershipGroupService.addMember(id, request);
        return ResponseEntity.ok(ApiResponse.ok("Member enrolled into ownership group successfully", response));
    }

    @DeleteMapping("/{id}/members/{userId}")
    @PreAuthorize(SecurityRoles.HAS_ROLE_ADMIN)
    @Operation(summary = "Remove or deactivate member from group",
            description = "Deactivates active membership share. Restricted to ADMIN.")
    public ResponseEntity<ApiResponse<Void>> removeMember(
            @PathVariable Long id,
            @PathVariable Long userId
    ) {
        ownershipGroupService.removeMember(id, userId);
        return ResponseEntity.ok(ApiResponse.ok("Member deactivated from ownership group successfully", null));
    }

    @PostMapping("/{id}/transfer-share")
    @PreAuthorize(SecurityRoles.HAS_ROLE_ADMIN)
    @Operation(summary = "Transfer equity share between co-owners",
            description = "Atomically transfers equity percentage between members ensuring 100% invariant. Restricted to ADMIN.")
    public ResponseEntity<ApiResponse<OwnershipGroupResponse>> transferShare(
            @PathVariable Long id,
            @Valid @RequestBody com.example.evshare.dto.request.TransferShareRequest request
    ) {
        OwnershipGroupResponse response = ownershipShareService.transferShare(id, request);
        return ResponseEntity.ok(ApiResponse.ok("Equity share transferred successfully", response));
    }

    @PostMapping("/{id}/validate")
    @PreAuthorize("hasAnyRole('STAFF', 'ADMIN') or (hasRole('CO_OWNER') and @ownershipSecurity.isGroupMember(#id, principal.id))")
    @Operation(summary = "Validate 100% equity distribution invariant",
            description = "Verifies group active shares sum to exactly 100.00%.")
    public ResponseEntity<ApiResponse<java.math.BigDecimal>> validateGroupShares(
            @PathVariable Long id
    ) {
        java.math.BigDecimal total = ownershipShareService.validateOwnershipDistribution(id);
        return ResponseEntity.ok(ApiResponse.ok("Ownership distribution is valid (100.00%)", total));
    }
}
