-- ===================================================================
-- EVShare 3D Platform - Flyway Migration V11
-- Module: Dispute Resolution & Governance Subsystem
-- Enhancement: Add dispute mediation notes, proposed resolution, and mediator user fields
-- ===================================================================

ALTER TABLE disputes ADD COLUMN mediation_notes TEXT NULL;
ALTER TABLE disputes ADD COLUMN proposed_resolution TEXT NULL;
ALTER TABLE disputes ADD COLUMN mediator_user_id BIGINT NULL;

ALTER TABLE disputes ADD CONSTRAINT fk_dispute_mediator FOREIGN KEY (mediator_user_id) REFERENCES users(id) ON DELETE SET NULL;
