package com.example.evshare.service;

import com.example.evshare.dto.response.UserResponse;

public interface UserService {

    /**
     * Retrieves the profile of the currently authenticated user.
     *
     * @param userId the authenticated user ID obtained from security context
     * @param includeRoles whether to include role assignments in the response
     * @return safe UserResponse without sensitive fields
     */
    UserResponse getCurrentUserProfile(Long userId, boolean includeRoles);

    /**
     * Retrieves user details by ID. Restricted to STAFF and ADMIN roles per RBAC.
     *
     * @param targetUserId the ID of the user to look up
     * @return safe UserResponse
     */
    UserResponse getUserById(Long targetUserId);
}
