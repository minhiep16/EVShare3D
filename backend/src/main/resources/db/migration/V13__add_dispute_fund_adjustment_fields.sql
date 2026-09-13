-- ===================================================================
-- EVShare 3D Platform - Flyway Migration V13
-- Module: Governance & Finance Integration
-- Table: disputes (Add fund adjustment link and amount)
-- ===================================================================

ALTER TABLE disputes
    ADD COLUMN fund_adjustment_amount DECIMAL(15, 2) NULL,
    ADD COLUMN fund_transaction_id BIGINT NULL;

ALTER TABLE disputes
    ADD CONSTRAINT fk_dispute_fund_transaction
    FOREIGN KEY (fund_transaction_id) REFERENCES fund_transactions (id) ON DELETE SET NULL;

CREATE INDEX idx_disputes_fund_transaction_id ON disputes (fund_transaction_id);
