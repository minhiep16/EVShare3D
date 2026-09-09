-- ===================================================================
-- EVShare 3D Platform - Flyway Migration V9
-- Module: Finance, Shared Fund & Immutable Transaction Ledger
-- Enhancement: Add entry_type (CREDIT/DEBIT), transaction_reference, and source
-- ===================================================================

ALTER TABLE fund_transactions
    ADD COLUMN entry_type VARCHAR(10) NOT NULL DEFAULT 'CREDIT' AFTER transaction_type,
    ADD COLUMN transaction_reference VARCHAR(64) NULL AFTER balance_after,
    ADD COLUMN source VARCHAR(50) NOT NULL DEFAULT 'MANUAL' AFTER description;

-- Backfill transaction_reference and entry_type for existing records if any
UPDATE fund_transactions
    SET transaction_reference = CONCAT('TX-LEGACY-', id)
    WHERE transaction_reference IS NULL;

UPDATE fund_transactions
    SET entry_type = 'DEBIT'
    WHERE transaction_type IN ('WITHDRAWAL', 'EXPENSE_PAYOUT');

UPDATE fund_transactions
    SET entry_type = 'CREDIT'
    WHERE transaction_type IN ('CONTRIBUTION', 'DEPOSIT', 'CAPITAL_CALL', 'INTEREST', 'REFUND');

-- Enforce NOT NULL and UNIQUE on transaction_reference
ALTER TABLE fund_transactions
    MODIFY transaction_reference VARCHAR(64) NOT NULL,
    ADD CONSTRAINT uq_fund_tx_reference UNIQUE (transaction_reference);

-- Indexes for performance and chronological ledger reconciliation
CREATE INDEX idx_fund_tx_fund_created ON fund_transactions (fund_id, created_at);
CREATE INDEX idx_fund_tx_entry_type ON fund_transactions (entry_type);
