package com.example.evshare.controller;

import com.example.evshare.dto.response.ApiResponse;
import com.example.evshare.dto.response.FairUsageMetricsResponse;
import com.example.evshare.dto.response.GroupFairUsageResponse;
import com.example.evshare.security.UserPrincipal;
import com.example.evshare.service.FairUsageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/analytics")
@Tag(name = "Analytics & Fair Usage", description = "Endpoints for AI mobility analytics, fair usage calculation, and quota monitoring")
@SecurityRequirement(name = "bearerAuth")
public class AnalyticsController {

    private final FairUsageService fairUsageService;

    public AnalyticsController(FairUsageService fairUsageService) {
        this.fairUsageService = fairUsageService;
    }

    @GetMapping("/fair-usage/{groupId}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Retrieve syndicate fair usage analytics",
            description = "Calculates equity quotas, weighted consumption, and fairness scores across syndicate members per BR-FAIR-01..03")
    public ResponseEntity<ApiResponse<GroupFairUsageResponse>> getGroupFairUsage(
            @PathVariable Long groupId,
            @RequestParam(defaultValue = "30") int windowDays,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        boolean isAdmin = principal != null && principal.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN") || a.getAuthority().equals("ROLE_STAFF"));
        Long currentUserId = principal != null ? principal.getId() : null;
        GroupFairUsageResponse response = fairUsageService.getGroupFairUsage(groupId, windowDays, currentUserId, isAdmin);
        return ResponseEntity.ok(ApiResponse.ok("Syndicate fair usage analytics retrieved successfully", response));
    }

    @GetMapping("/fair-usage/{groupId}/my-score")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Retrieve current user's fair usage score and recommendation",
            description = "Returns caller's personalized fairness ratio, score, and priority status")
    public ResponseEntity<ApiResponse<FairUsageMetricsResponse>> getMyFairUsage(
            @PathVariable Long groupId,
            @RequestParam(defaultValue = "30") int windowDays,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        boolean isAdmin = principal != null && principal.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN") || a.getAuthority().equals("ROLE_STAFF"));
        Long currentUserId = principal != null ? principal.getId() : null;
        FairUsageMetricsResponse response = fairUsageService.getUserFairUsage(groupId, currentUserId, windowDays, currentUserId, isAdmin);
        return ResponseEntity.ok(ApiResponse.ok("Personal fair usage metrics retrieved successfully", response));
    }
}
