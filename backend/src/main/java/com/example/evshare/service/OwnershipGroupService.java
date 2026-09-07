package com.example.evshare.service;

import com.example.evshare.dto.request.AddGroupMemberRequest;
import com.example.evshare.dto.request.CreateOwnershipGroupRequest;
import com.example.evshare.dto.request.UpdateOwnershipGroupRequest;
import com.example.evshare.dto.response.OwnershipGroupResponse;

import java.util.List;

public interface OwnershipGroupService {

    /**
     * Creates a new ownership group and binds it to a vehicle.
     * Optionally assigns initial members.
     *
     * @param request Creation payload
     * @return Created ownership group response
     */
    OwnershipGroupResponse createGroup(CreateOwnershipGroupRequest request);

    /**
     * Retrieves ownership group details by ID including bound vehicle and active member shares.
     *
     * @param id Group ID
     * @return Ownership group response
     */
    OwnershipGroupResponse getGroupById(Long id);

    /**
     * Retrieves all active ownership groups where the specified user is an active member.
     *
     * @param userId User ID
     * @return List of ownership group responses
     */
    List<OwnershipGroupResponse> getMyGroups(Long userId);

    /**
     * Updates permitted attributes of an ownership group (e.g. group name, active state).
     * Vehicle association remains immutable.
     *
     * @param id      Group ID
     * @param request Update payload
     * @return Updated ownership group response
     */
    OwnershipGroupResponse updateGroup(Long id, UpdateOwnershipGroupRequest request);

    /**
     * Enrolls a new member into an existing ownership group.
     *
     * @param groupId Group ID
     * @param request Member enrollment payload
     * @return Updated ownership group response
     */
    OwnershipGroupResponse addMember(Long groupId, AddGroupMemberRequest request);

    /**
     * Removes/deactivates a member from an ownership group.
     *
     * @param groupId Group ID
     * @param userId  User ID to remove
     */
    void removeMember(Long groupId, Long userId);
}
