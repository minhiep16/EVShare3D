package com.example.evshare.service;

import com.example.evshare.dto.request.CreateContractRequest;
import com.example.evshare.dto.request.TransitionContractStatusRequest;
import com.example.evshare.dto.request.UpdateContractRequest;
import com.example.evshare.dto.response.ContractResponse;

import java.util.List;

public interface ContractService {

    /**
     * Creates a new co-ownership contract draft associated with an ownership group.
     * Automatically computes incremental version number.
     *
     * @param request Contract creation payload
     * @return Created contract details
     */
    ContractResponse createContract(CreateContractRequest request);

    /**
     * Updates an existing contract while in DRAFT status.
     * Non-draft contracts are legally frozen and immutable.
     *
     * @param contractId Contract ID
     * @param request Update payload
     * @return Updated contract details
     */
    ContractResponse updateDraftContract(Long contractId, UpdateContractRequest request);

    /**
     * Transitions a contract across its defined lifecycle states:
     * DRAFT -> PENDING_SIGNATURE -> SIGNED -> ACTIVE -> EXPIRED / TERMINATED.
     *
     * @param contractId Contract ID
     * @param request Status transition payload
     * @return Transitioned contract details
     */
    ContractResponse transitionStatus(Long contractId, TransitionContractStatusRequest request);

    /**
     * Retrieves contract details by ID.
     *
     * @param contractId Contract ID
     * @return Contract details
     */
    ContractResponse getContractById(Long contractId);

    /**
     * Retrieves all historical and current contract versions for an ownership group
     * ordered by version descending.
     *
     * @param groupId Group ID
     * @return List of contract responses
     */
    List<ContractResponse> getContractsByGroupId(Long groupId);

    /**
     * Retrieves the current ACTIVE contract governing an ownership group.
     *
     * @param groupId Group ID
     * @return Active contract details
     */
    ContractResponse getActiveContractByGroupId(Long groupId);

    /**
     * Strictly rejects contract deletion to preserve historical legal co-ownership records.
     *
     * @param contractId Contract ID
     */
    void deleteContract(Long contractId);

    /**
     * Submits a digital signature for a contract in PENDING_SIGNATURE status.
     * Generates a cryptographic SHA-256 digest binding terms, version, signer, timestamp, and IP.
     * When all active co-owners have signed, transitions contract status to SIGNED.
     *
     * @param contractId Contract ID
     * @param userId Signatory user ID
     * @param request Sign contract payload
     * @param ipAddress Client IP address
     * @return Created signature details
     */
    com.example.evshare.dto.response.ContractSignatureResponse signContract(
            Long contractId, Long userId, com.example.evshare.dto.request.SignContractRequest request, String ipAddress);

    /**
     * Retrieves an overview of signatures and pending signers for a contract.
     *
     * @param contractId Contract ID
     * @param requesterUserId Requester user ID
     * @return Signatures overview with list of signatures and pending co-owners
     */
    com.example.evshare.dto.response.ContractSignaturesOverviewResponse getSignaturesOverview(
            Long contractId, Long requesterUserId);
}
