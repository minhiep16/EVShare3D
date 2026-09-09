package com.example.evshare.entity.enums;

/**
 * Fundamental financial accounting direction for SharedFund ledger movements.
 */
public enum TransactionEntryType {
    /** Inflow: increases fund balance (e.g. contribution, deposit, interest, refund) */
    CREDIT,

    /** Outflow: decreases fund balance (e.g. withdrawal, expense payout, penalty charge) */
    DEBIT
}
