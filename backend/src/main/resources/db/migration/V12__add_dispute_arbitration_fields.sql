-- ===================================================================
-- EVShare 3D Platform - Flyway Migration V12
-- Module: Governance & Dispute Resolution
-- Table: disputes (Add final admin arbitration fields)
-- ===================================================================

ALTER TABLE disputes
    ADD COLUMN arbitrator_user_id BIGINT NULL,
    ADD COLUMN resolved_at TIMESTAMP NULL;

ALTER TABLE disputes
    ADD CONSTRAINT fk_dispute_arbitrator
    FOREIGN KEY (arbitrator_user_id) REFERENCES users (id) ON DELETE SET NULL;

CREATE INDEX idx_disputes_arbitrator_user_id ON disputes (arbitrator_user_id);
