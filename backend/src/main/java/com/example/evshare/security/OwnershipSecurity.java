package com.example.evshare.security;

import com.example.evshare.entity.OwnershipShare;
import com.example.evshare.repository.OwnershipShareRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Optional;

/**
 * Custom SpEL security evaluation bean for Ownership ACL and data-scoping checks.
 * Referenced in method security expressions via {@code @ownershipSecurity.isGroupMember(...)}
 * in compliance with docs/RBAC.md and docs/ARCHITECTURE.md.
 */
@Component("ownershipSecurity")
public class OwnershipSecurity {

    private static final Logger log = LoggerFactory.getLogger(OwnershipSecurity.class);

    private final OwnershipShareRepository ownershipShareRepository;
    private final com.example.evshare.repository.CoOwnershipContractRepository coOwnershipContractRepository;

    public OwnershipSecurity(OwnershipShareRepository ownershipShareRepository,
                             com.example.evshare.repository.CoOwnershipContractRepository coOwnershipContractRepository) {
        this.ownershipShareRepository = ownershipShareRepository;
        this.coOwnershipContractRepository = coOwnershipContractRepository;
    }

    /**
     * Checks if the specified user holds an active equity share in the ownership group.
     *
     * @param groupId the ownership group ID
     * @param userId the user ID from the authenticated security principal
     * @return true if user holds an active equity share in the group; false otherwise
     */
    public boolean isGroupMember(Long groupId, Long userId) {
        if (groupId == null || userId == null) {
            return false;
        }

        Optional<OwnershipShare> shareOpt = ownershipShareRepository.findByGroupIdAndUserId(groupId, userId);
        boolean isMember = shareOpt.isPresent() && Boolean.TRUE.equals(shareOpt.get().getIsActive());

        if (!isMember) {
            log.warn("Ownership ACL denied: User [{}] is not an active equity holder in group [{}]", userId, groupId);
        } else {
            log.debug("Ownership ACL granted: User [{}] verified as active equity holder in group [{}]", userId, groupId);
        }

        return isMember;
    }

    /**
     * Checks if the specified user holds an active equity share in the ownership group
     * to which the given contract belongs.
     *
     * @param contractId the contract ID
     * @param userId the user ID
     * @return true if user is an active equity holder in the contract's group; false otherwise
     */
    public boolean isContractGroupMember(Long contractId, Long userId) {
        if (contractId == null || userId == null) {
            return false;
        }

        return coOwnershipContractRepository.findById(contractId)
                .map(contract -> isGroupMember(contract.getGroup().getId(), userId))
                .orElse(false);
    }

    /**
     * Checks if the user holds at least the minimum specified equity percentage in the group
     * (e.g. 10.00% required to sponsor governance proposals per BR-GOV-01).
     *
     * @param groupId the ownership group ID
     * @param userId the user ID
     * @param minPercentage the minimum required equity threshold
     * @return true if active equity meets or exceeds the threshold; false otherwise
     */
    public boolean hasMinimumEquity(Long groupId, Long userId, BigDecimal minPercentage) {
        if (groupId == null || userId == null || minPercentage == null) {
            return false;
        }

        return ownershipShareRepository.findByGroupIdAndUserId(groupId, userId)
                .filter(share -> Boolean.TRUE.equals(share.getIsActive()))
                .map(share -> share.getPercentage() != null && share.getPercentage().compareTo(minPercentage) >= 0)
                .orElse(false);
    }
}
