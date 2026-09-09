-- ===================================================================
-- EVShare 3D Platform - Flyway Migration V8
-- Module: Finance & Cost Allocation Subsystem
-- Enhancement: Add vehicle linkage, currency, and evidence URL to expenses
-- ===================================================================

ALTER TABLE expenses
    ADD COLUMN vehicle_id BIGINT NULL AFTER group_id,
    ADD COLUMN currency VARCHAR(10) NOT NULL DEFAULT 'VND' AFTER total_amount,
    ADD COLUMN evidence_url VARCHAR(255) NULL AFTER invoice_reference;

-- Populate vehicle_id from ownership_groups for existing rows
UPDATE expenses e
    INNER JOIN ownership_groups og ON e.group_id = og.id
    SET e.vehicle_id = og.vehicle_id
    WHERE e.vehicle_id IS NULL;

-- Enforce foreign key on vehicle_id
ALTER TABLE expenses
    ADD CONSTRAINT fk_expense_vehicle FOREIGN KEY (vehicle_id) REFERENCES vehicles(id) ON DELETE RESTRICT;

-- Indexes for performance and historical queries
CREATE INDEX idx_expenses_group_date ON expenses (group_id, incurred_date);
CREATE INDEX idx_expenses_vehicle ON expenses (vehicle_id);
