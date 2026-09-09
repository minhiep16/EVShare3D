package com.example.evshare.exception;

import org.springframework.http.HttpStatus;

import java.math.BigDecimal;

/**
 * Exception thrown when a fund withdrawal or payout is attempted exceeding
 * the current balance of a SharedFund without explicit overdraft permission.
 */
public class InsufficientFundBalanceException extends BusinessException {

    private final Long fundId;
    private final BigDecimal currentBalance;
    private final BigDecimal requestedAmount;

    public InsufficientFundBalanceException(Long fundId, BigDecimal currentBalance, BigDecimal requestedAmount) {
        super(String.format("Insufficient fund balance: fund %d has current balance %s, but withdrawal of %s was requested",
                fundId,
                currentBalance != null ? currentBalance.toPlainString() : "0.00",
                requestedAmount != null ? requestedAmount.toPlainString() : "0.00"),
                HttpStatus.BAD_REQUEST);
        this.fundId = fundId;
        this.currentBalance = currentBalance;
        this.requestedAmount = requestedAmount;
    }

    public Long getFundId() {
        return fundId;
    }

    public BigDecimal getCurrentBalance() {
        return currentBalance;
    }

    public BigDecimal getRequestedAmount() {
        return requestedAmount;
    }
}
