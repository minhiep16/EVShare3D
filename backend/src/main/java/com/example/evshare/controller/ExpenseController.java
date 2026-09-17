package com.example.evshare.controller;

import com.example.evshare.dto.request.CreateExpenseRequest;
import com.example.evshare.dto.response.*;
import com.example.evshare.entity.enums.AllocationStrategy;
import com.example.evshare.entity.enums.ExpenseCategory;
import com.example.evshare.security.UserPrincipal;
import com.example.evshare.service.ExpenseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/expenses")
@Tag(name = "Finance & Expenses", description = "Endpoints for syndicate vehicle expense management, validation, and historical audit ledger")
@SecurityRequirement(name = "bearerAuth")
public class ExpenseController {

    private final ExpenseService expenseService;

    public ExpenseController(ExpenseService expenseService) {
        this.expenseService = expenseService;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('STAFF', 'ADMIN', 'CO_OWNER')")
    @Operation(summary = "Record new syndicate vehicle expense",
            description = "Validates amount, currency, group, vehicle, creator authorization, category, date, and receipt evidence, writing an immutable audit entry")
    public ResponseEntity<ApiResponse<ExpenseResponse>> createExpense(
            @Valid @RequestBody CreateExpenseRequest request,
            @AuthenticationPrincipal UserPrincipal principal,
            HttpServletRequest servletRequest
    ) {
        Long currentUserId = principal != null ? principal.getId() : null;
        String ipAddress = servletRequest != null ? servletRequest.getRemoteAddr() : null;

        ExpenseResponse response = expenseService.createExpense(request, currentUserId, ipAddress);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Expense recorded successfully in ledger", response));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('STAFF', 'ADMIN') or (hasRole('CO_OWNER') and @ownershipSecurity.isExpenseGroupMember(#id, principal.id))")
    @Operation(summary = "Retrieve expense details by ID",
            description = "Fetches complete expense information including cost allocations and receipt evidence references")
    public ResponseEntity<ApiResponse<ExpenseResponse>> getExpenseById(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        Long currentUserId = principal != null ? principal.getId() : null;
        ExpenseResponse response = expenseService.getExpenseById(id, currentUserId);
        return ResponseEntity.ok(ApiResponse.ok("Expense retrieved successfully", response));
    }

    @GetMapping("/group/{groupId}")
    @PreAuthorize("hasAnyRole('STAFF', 'ADMIN') or (hasRole('CO_OWNER') and @ownershipSecurity.isGroupMember(#groupId, principal.id))")
    @Operation(summary = "List syndicate expenses",
            description = "Retrieves paginated expenses for an ownership group, filterable by category and date range")
    public ResponseEntity<ApiResponse<PagedData<ExpenseResponse>>> getGroupExpenses(
            @PathVariable Long groupId,
            @RequestParam(required = false) ExpenseCategory category,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @PageableDefault(size = 20, sort = "incurredDate", direction = Sort.Direction.DESC) Pageable pageable,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        Long currentUserId = principal != null ? principal.getId() : null;
        PagedData<ExpenseResponse> response = expenseService.getGroupExpenses(groupId, category, startDate, endDate, pageable, currentUserId);
        return ResponseEntity.ok(ApiResponse.ok("Group expenses retrieved successfully", response));
    }

    @GetMapping("/{id}/history")
    @PreAuthorize("hasAnyRole('STAFF', 'ADMIN') or (hasRole('CO_OWNER') and @ownershipSecurity.isExpenseGroupMember(#id, principal.id))")
    @Operation(summary = "Retrieve expense audit history",
            description = "Retrieves chronological immutable audit logs for the specified expense")
    public ResponseEntity<ApiResponse<List<ExpenseAuditLogResponse>>> getExpenseHistory(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        Long currentUserId = principal != null ? principal.getId() : null;
        List<ExpenseAuditLogResponse> response = expenseService.getExpenseHistory(id, currentUserId);
        return ResponseEntity.ok(ApiResponse.ok("Expense audit trail retrieved successfully", response));
    }

    @GetMapping("/group/{groupId}/allocation-summary")
    @PreAuthorize("hasAnyRole('STAFF', 'ADMIN') or (hasRole('CO_OWNER') and @ownershipSecurity.isGroupMember(#groupId, principal.id))")
    @Operation(summary = "Authoritative syndicate cost allocation summary",
            description = "Computes group-wide member cost liabilities using authoritative backend allocation strategies")
    public ResponseEntity<ApiResponse<GroupAllocationSummaryResponse>> getGroupAllocationSummary(
            @PathVariable Long groupId,
            @RequestParam(required = false, defaultValue = "HYBRID") AllocationStrategy strategy,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        Long currentUserId = principal != null ? principal.getId() : null;
        GroupAllocationSummaryResponse response = expenseService.getGroupAllocationSummary(groupId, strategy, currentUserId);
        return ResponseEntity.ok(ApiResponse.ok("Authoritative group allocation summary retrieved", response));
    }

    @GetMapping("/{id}/allocations")
    @PreAuthorize("hasAnyRole('STAFF', 'ADMIN') or (hasRole('CO_OWNER') and @ownershipSecurity.isExpenseGroupMember(#id, principal.id))")
    @Operation(summary = "Retrieve itemized cost allocations for an expense",
            description = "Fetches persisted member liability breakdown and settlement state")
    public ResponseEntity<ApiResponse<List<ExpenseAllocationResponse>>> getExpenseAllocations(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        Long currentUserId = principal != null ? principal.getId() : null;
        List<ExpenseAllocationResponse> response = expenseService.getExpenseAllocations(id, currentUserId);
        return ResponseEntity.ok(ApiResponse.ok("Expense allocations retrieved successfully", response));
    }

    @PostMapping("/{id}/allocate")
    @PreAuthorize("hasAnyRole('STAFF', 'ADMIN') or (hasRole('CO_OWNER') and @ownershipSecurity.isExpenseGroupMember(#id, principal.id))")
    @Operation(summary = "Authoritatively calculate and persist cost allocations for an expense")
    public ResponseEntity<ApiResponse<ExpenseResponse>> allocateExpense(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        Long currentUserId = principal != null ? principal.getId() : null;
        ExpenseResponse response = expenseService.allocateExpense(id, currentUserId);
        return ResponseEntity.ok(ApiResponse.ok("Expense allocated and persisted successfully", response));
    }
}
