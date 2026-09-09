package com.example.evshare.entity.enums;

/**
 * Originating operational source trigger for SharedFund ledger transactions.
 */
public enum FundTransactionSource {
    MEMBER_CONTRIBUTION,
    EXPENSE_PAYOUT,
    CAPITAL_CALL_REPLENISHMENT,
    LATE_CANCELLATION_PENALTY,
    BATTERY_SURCHARGE,
    PAYMENT_SETTLEMENT,
    VAULT_INITIALIZATION,
    MANUAL_ADJUSTMENT
}
