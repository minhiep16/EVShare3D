package com.example.evshare.controller;

import com.example.evshare.dto.response.ApiResponse;
import com.example.evshare.dto.response.UserResponse;
import com.example.evshare.exception.BusinessException;
import com.example.evshare.security.SecurityRoles;
import com.example.evshare.security.UserPrincipal;
import com.example.evshare.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/users")
@Tag(name = "Users", description = "User profile and identity management endpoints")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/me")
    @Operation(summary = "Get current authenticated user profile",
            description = "Retrieves the profile details of the currently authenticated user identified by the security context")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "User profile retrieved successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Authentication required or invalid access token"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "User account is disabled or deactivated"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "User not found")
    })
    public ResponseEntity<ApiResponse<UserResponse>> getCurrentUser(
            @AuthenticationPrincipal UserPrincipal principal,
            @Parameter(description = "Whether to include assigned roles in the response")
            @RequestParam(name = "includeRoles", required = false, defaultValue = "true") boolean includeRoles
    ) {
        // Requirement 1 & 2: Obtain identity strictly from backend security context
        if (principal == null) {
            throw new BusinessException("Authentication required", HttpStatus.UNAUTHORIZED);
        }

        // Requirement 3: Never accept userId from query/body to determine identity; use principal.getId()
        UserResponse response = userService.getCurrentUserProfile(principal.getId(), includeRoles);
        return ResponseEntity.ok(ApiResponse.ok("User profile retrieved successfully", response));
    }

    @GetMapping("/{id}")
    @PreAuthorize(SecurityRoles.HAS_STAFF_OR_ADMIN)
    @Operation(summary = "Retrieve user details by ID",
            description = "Restricted to STAFF and ADMIN roles per docs/RBAC.md and docs/API.md")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "User retrieved successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Authentication required"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Access denied: Requires STAFF or ADMIN role"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "User not found")
    })
    public ResponseEntity<ApiResponse<UserResponse>> getUserById(@PathVariable("id") Long id) {
        UserResponse response = userService.getUserById(id);
        return ResponseEntity.ok(ApiResponse.ok("User retrieved successfully", response));
    }
}
