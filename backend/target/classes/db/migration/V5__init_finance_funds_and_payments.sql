-- ===================================================================
-- EVShare 3D Platform - Flyway Migration V5
-- Module: Finance, 3D Vault Shared Funds, Expenses & Payments
-- Tables: shared_funds, fund_transactions, expenses, expense_allocations, payments
-- ===================================================================

CREATE TABLE shared_funds (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    group_id BIGINT NOT NULL UNIQUE,
    current_balance DECIMAL(15, 2) NOT NULL DEFAULT 0.00,
    minimum_reserve_threshold DECIMAL(15, 2) NOT NULL DEFAULT 10000000.00,
    currency VARCHAR(10) NOT NULL DEFAULT 'VND',
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_shared_fund_group FOREIGN KEY (group_id) REFERENCES ownership_groups(id) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE fund_transactions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    fund_id BIGINT NOT NULL,
    user_id BIGINT NULL,
    transaction_type VARCHAR(30) NOT NULL,
    amount DECIMAL(15, 2) NOT NULL,
    balance_after DECIMAL(15, 2) NOT NULL,
    description VARCHAR(255) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_fund_tx_fund FOREIGN KEY (fund_id) REFERENCES shared_funds(id) ON DELETE RESTRICT,
    CONSTRAINT fk_fund_tx_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE expenses (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    group_id BIGINT NOT NULL,
    title VARCHAR(150) NOT NULL,
    category VARCHAR(40) NOT NULL,
    total_amount DECIMAL(15, 2) NOT NULL,
    allocation_strategy VARCHAR(30) NOT NULL DEFAULT 'OWNERSHIP_BASED',
    invoice_reference VARCHAR(100) NULL,
    logged_by_user_id BIGINT NOT NULL,
    incurred_date DATE NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_expense_total CHECK (total_amount > 0.00),
    CONSTRAINT fk_expense_group FOREIGN KEY (group_id) REFERENCES ownership_groups(id) ON DELETE RESTRICT,
    CONSTRAINT fk_expense_logger FOREIGN KEY (logged_by_user_id) REFERENCES users(id) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE expense_allocations (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    expense_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    allocated_amount DECIMAL(15, 2) NOT NULL,
    is_settled BOOLEAN NOT NULL DEFAULT FALSE,
    settled_at TIMESTAMP NULL,
    CONSTRAINT chk_allocated_amount CHECK (allocated_amount >= 0.00),
    CONSTRAINT fk_allocation_expense FOREIGN KEY (expense_id) REFERENCES expenses(id) ON DELETE CASCADE,
    CONSTRAINT fk_allocation_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE payments (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    fund_id BIGINT NOT NULL,
    expense_allocation_id BIGINT NULL,
    amount DECIMAL(15, 2) NOT NULL,
    payment_method VARCHAR(30) NOT NULL,
    transaction_reference VARCHAR(100) NOT NULL UNIQUE,
    status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_payment_amount CHECK (amount > 0.00),
    CONSTRAINT fk_payment_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE RESTRICT,
    CONSTRAINT fk_payment_fund FOREIGN KEY (fund_id) REFERENCES shared_funds(id) ON DELETE RESTRICT,
    CONSTRAINT fk_payment_allocation FOREIGN KEY (expense_allocation_id) REFERENCES expense_allocations(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
