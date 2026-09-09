package com.example.evshare.controller;

import com.example.evshare.dto.request.FundContributionRequest;
import com.example.evshare.dto.request.FundWithdrawalRequest;
import com.example.evshare.dto.response.ApiResponse;
import com.example.evshare.dto.response.FundAuditLogResponse;
import com.example.evshare.dto.response.FundReconciliationResponse;
import com.example.evshare.dto.response.FundTransactionResponse;
import com.example.evshare.dto.response.SharedFundResponse;
import com.example.evshare.security.UserPrincipal;
import com.example.evshare.service.SharedFundService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
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
@RequestMapping("/api/v1/ownership-groups/{groupId}/fund")
@Tag(name = "Finance & Shared Fund", description = "Endpoints for syndicate 3D Vault shared funds, balances, contributions, withdrawals, and immutable transaction history")
@SecurityRequirement(name = "bearerAuth")
public class SharedFundController {

    private final SharedFundService sharedFundService;

    public SharedFundController(SharedFundService sharedFundService) {
        this.sharedFundService = sharedFundService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('STAFF', 'ADMIN') or (hasRole('CO_OWNER') and @ownershipSecurity.isGroupMember(#groupId, principal.id))")
    @Operation(summary = "Retrieve SharedFund details and reserve status for syndicate group",
            description = "Fetches current balance, minimum reserve threshold, currency, and low-liquidity status per BR-FIN-03")
    public ResponseEntity<ApiResponse<SharedFundResponse>> getFund(
            @PathVariable Long groupId,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        Long currentUserId = principal != null ? principal.getId() : null;
        SharedFundResponse response = sharedFundService.getFundByGroupId(groupId, currentUserId);
        return ResponseEntity.ok(ApiResponse.ok("Shared fund retrieved successfully", response));
    }

    @GetMapping("/balance")
    @PreAuthorize("hasAnyRole('STAFF', 'ADMIN') or (hasRole('CO_OWNER') and @ownershipSecurity.isGroupMember(#groupId, principal.id))")
    @Operation(summary = "Quick balance check for SharedFund",
            description = "Returns current balance and liquidity state for group vault")
    public ResponseEntity<ApiResponse<SharedFundResponse>> getBalance(
            @PathVariable Long groupId,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        Long currentUserId = principal != null ? principal.getId() : null;
        SharedFundResponse response = sharedFundService.getFundBalance(groupId, currentUserId);
        return ResponseEntity.ok(ApiResponse.ok("Fund balance retrieved successfully", response));
    }

    @PostMapping("/contributions")
    @PreAuthorize("hasAnyRole('STAFF', 'ADMIN') or (hasRole('CO_OWNER') and @ownershipSecurity.isGroupMember(#groupId, principal.id))")
    @Operation(summary = "Make a capital contribution or deposit into syndicate SharedFund",
            description = "Increases fund balance atomically under pessimistic write lock, records an immutable transaction, and creates an audit trail")
    public ResponseEntity<ApiResponse<FundTransactionResponse>> contribute(
            @PathVariable Long groupId,
            @Valid @RequestBody FundContributionRequest request,
            @AuthenticationPrincipal UserPrincipal principal,
            HttpServletRequest servletRequest
    ) {
        Long currentUserId = principal != null ? principal.getId() : null;
        String ipAddress = servletRequest != null ? servletRequest.getRemoteAddr() : null;

        FundTransactionResponse response = sharedFundService.contribute(groupId, request, currentUserId, ipAddress);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Contribution recorded successfully", response));
    }

    @PostMapping("/withdrawals")
    @PreAuthorize("hasAnyRole('STAFF', 'ADMIN') or (hasRole('CO_OWNER') and @ownershipSecurity.isGroupMember(#groupId, principal.id))")
    @Operation(summary = "Execute an authorized withdrawal or expense payout from syndicate SharedFund",
            description = "Decreases fund balance atomically under pessimistic write lock. Rejects negative balance unless allowOverdraft is explicitly set")
    public ResponseEntity<ApiResponse<FundTransactionResponse>> withdraw(
            @PathVariable Long groupId,
            @Valid @RequestBody FundWithdrawalRequest request,
            @AuthenticationPrincipal UserPrincipal principal,
            HttpServletRequest servletRequest
    ) {
        Long currentUserId = principal != null ? principal.getId() : null;
        String ipAddress = servletRequest != null ? servletRequest.getRemoteAddr() : null;

        FundTransactionResponse response = sharedFundService.withdraw(groupId, request, currentUserId, ipAddress);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Withdrawal executed successfully", response));
    }

    @GetMapping("/transactions")
    @PreAuthorize("hasAnyRole('STAFF', 'ADMIN') or (hasRole('CO_OWNER') and @ownershipSecurity.isGroupMember(#groupId, principal.id))")
    @Operation(summary = "Retrieve chronological transaction ledger for SharedFund",
            description = "Returns immutable list of historical deposits, contributions, payouts, and withdrawals")
    public ResponseEntity<ApiResponse<List<FundTransactionResponse>>> getTransactionHistory(
            @PathVariable Long groupId,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        Long currentUserId = principal != null ? principal.getId() : null;
        List<FundTransactionResponse> response = sharedFundService.getTransactionHistory(groupId, currentUserId);
        return ResponseEntity.ok(ApiResponse.ok("Transaction history retrieved successfully", response));
    }

    @GetMapping("/audit")
    @PreAuthorize("hasAnyRole('STAFF', 'ADMIN') or (hasRole('CO_OWNER') and @ownershipSecurity.isGroupMember(#groupId, principal.id))")
    @Operation(summary = "Retrieve complete audit history for SharedFund mutations",
            description = "Returns state-change snapshots, actor user IDs, and timestamps for all balance alterations")
    public ResponseEntity<ApiResponse<List<FundAuditLogResponse>>> getAuditHistory(
            @PathVariable Long groupId,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        Long currentUserId = principal != null ? principal.getId() : null;
        List<FundAuditLogResponse> response = sharedFundService.getFundAuditHistory(groupId, currentUserId);
        return ResponseEntity.ok(ApiResponse.ok("Audit history retrieved successfully", response));
    }

    @GetMapping("/reconciliation")
    @PreAuthorize("hasAnyRole('STAFF', 'ADMIN') or (hasRole('CO_OWNER') and @ownershipSecurity.isGroupMember(#groupId, principal.id))")
    @Operation(summary = "Audit and reconcile SharedFund ledger against current account balance",
            description = "Computes sum(CREDITS) - sum(DEBITS) across historical ledger entries and verifies mathematical equality with current balance")
    public ResponseEntity<ApiResponse<FundReconciliationResponse>> reconcileFund(
            @PathVariable Long groupId,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        Long currentUserId = principal != null ? principal.getId() : null;
        FundReconciliationResponse response = sharedFundService.reconcileFundBalance(groupId, currentUserId);
        return ResponseEntity.ok(ApiResponse.ok("Fund ledger reconciliation completed", response));
    }

    @GetMapping("/transactions/{reference}")
    @PreAuthorize("hasAnyRole('STAFF', 'ADMIN') or (hasRole('CO_OWNER') and @ownershipSecurity.isGroupMember(#groupId, principal.id))")
    @Operation(summary = "Lookup a specific transaction by its unique reference code",
            description = "Fetches immutable ledger transaction entry details by unique reference")
    public ResponseEntity<ApiResponse<FundTransactionResponse>> getTransactionByReference(
            @PathVariable Long groupId,
            @PathVariable String reference,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        Long currentUserId = principal != null ? principal.getId() : null;
        FundTransactionResponse response = sharedFundService.getTransactionByReference(groupId, reference, currentUserId);
        return ResponseEntity.ok(ApiResponse.ok("Transaction retrieved successfully", response));
    }
}
