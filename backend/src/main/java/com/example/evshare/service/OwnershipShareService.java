package com.example.evshare.service;

import com.example.evshare.dto.request.IssueOwnershipShareRequest;
import com.example.evshare.dto.request.RebalanceSharesRequest;
import com.example.evshare.dto.request.TransferShareRequest;
import com.example.evshare.dto.request.UpdateOwnershipShareRequest;
import com.example.evshare.dto.response.OwnershipGroupResponse;
import com.example.evshare.dto.response.OwnershipShareResponse;

import java.math.BigDecimal;
import java.util.List;

public interface OwnershipShareService {

    /**
     * Issues an equity share certificate to a user within an ownership group.
     * Enforces strict percentage validation and the 100.00% invariant.
     *
     * @param groupId Group ID
     * @param request Share issuance payload
     * @return Issued share certificate response
     */
    OwnershipShareResponse issueShare(Long groupId, IssueOwnershipShareRequest request);

    /**
     * Transactionally updates an existing ownership share percentage or status.
     *
     * @param groupId Group ID
     * @param shareId Share ID
     * @param request Update payload
     * @return Updated share certificate response
     */
    OwnershipShareResponse updateShare(Long groupId, Long shareId, UpdateOwnershipShareRequest request);

    /**
     * Retrieves an ownership share by ID.
     *
     * @param shareId Share ID
     * @return Share response
     */
    OwnershipShareResponse getShareById(Long shareId);

    /**
     * Lists all ownership shares within a group, optionally filtered by active status.
     *
     * @param groupId    Group ID
     * @param activeOnly If true, only active shares are returned
     * @return List of share responses
     */
    List<OwnershipShareResponse> getSharesByGroup(Long groupId, Boolean activeOnly);

    /**
     * Deactivates an active ownership share certificate.
     * Enforces the 100.00% invariant (prevents invalid final state).
     *
     * @param groupId Group ID
     * @param shareId Share ID
     */
    void deactivateShare(Long groupId, Long shareId);

    /**
     * Reactivates a deactivated ownership share certificate.
     *
     * @param groupId Group ID
     * @param shareId Share ID
     */
    void reactivateShare(Long groupId, Long shareId);

    /**
     * Validates that the sum of active ownership shares for the group equals exactly 100.00%.
     *
     * @param groupId Group ID
     * @return The active sum (exactly 100.00)
     */
    BigDecimal validateOwnershipDistribution(Long groupId);

    /**
     * Atomically transfers equity percentage between two co-owners within an ownership group,
     * maintaining the absolute 100.00% equity invariant.
     *
     * @param groupId Group ID
     * @param request Transfer request
     * @return Updated ownership group response
     */
    OwnershipGroupResponse transferShare(Long groupId, TransferShareRequest request);

    /**
     * Atomically rebalances all active shares in an ownership group in an ACID transaction.
     *
     * @param groupId Group ID
     * @param request Rebalance allocations payload
     * @return Updated ownership group response
     */
    OwnershipGroupResponse rebalanceShares(Long groupId, RebalanceSharesRequest request);

    /**
     * Retrieves the chronological ownership history for a specific share.
     *
     * @param groupId Group ID
     * @param shareId Share ID
     * @return List of ownership history records
     */
    List<com.example.evshare.dto.response.OwnershipHistoryResponse> getShareHistory(Long groupId, Long shareId);

    /**
     * Retrieves the complete consolidated ownership history across all shares in a group.
     *
     * @param groupId Group ID
     * @return List of ownership history records in reverse chronological order
     */
    List<com.example.evshare.dto.response.OwnershipHistoryResponse> getGroupOwnershipHistory(Long groupId);
}
