package com.example.evshare.exception;

import org.springframework.http.HttpStatus;

import java.math.BigDecimal;

/**
 * Domain exception thrown when an ownership share operation violates
 * the absolute 100.00% equity invariant defined in docs/BUSINESS_RULES.md (BR-OWN-01).
 */
public class InvalidOwnershipDistributionException extends BusinessException {

    private final Long groupId;
    private final BigDecimal currentTotal;
    private final BigDecimal targetTotal;

    public InvalidOwnershipDistributionException(String message) {
        super(message, HttpStatus.BAD_REQUEST);
        this.groupId = null;
        this.currentTotal = null;
        this.targetTotal = new BigDecimal("100.00");
    }

    public InvalidOwnershipDistributionException(Long groupId, BigDecimal currentTotal) {
        super(buildMessage(groupId, currentTotal), HttpStatus.BAD_REQUEST);
        this.groupId = groupId;
        this.currentTotal = currentTotal;
        this.targetTotal = new BigDecimal("100.00");
    }

    public InvalidOwnershipDistributionException(String message, Long groupId, BigDecimal currentTotal) {
        super(message, HttpStatus.BAD_REQUEST);
        this.groupId = groupId;
        this.currentTotal = currentTotal;
        this.targetTotal = new BigDecimal("100.00");
    }

    private static String buildMessage(Long groupId, BigDecimal currentTotal) {
        BigDecimal target = new BigDecimal("100.00");
        String comparison = currentTotal.compareTo(target) < 0 ? "less than" : "greater than";
        return String.format(
                "Invalid ownership distribution: Total active ownership percentage for group %d must equal exactly 100.00%%, but found %s%% (%s 100.00%%)",
                groupId, currentTotal, comparison
        );
    }

    public Long getGroupId() {
        return groupId;
    }

    public BigDecimal getCurrentTotal() {
        return currentTotal;
    }

    public BigDecimal getTargetTotal() {
        return targetTotal;
    }
}
