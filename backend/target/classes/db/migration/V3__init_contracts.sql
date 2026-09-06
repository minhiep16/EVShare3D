-- ===================================================================
-- EVShare 3D Platform - Flyway Migration V3
-- Module: Digital Co-Ownership Contracts & Cryptographic Signatures
-- Tables: co_ownership_contracts, contract_signatures
-- ===================================================================

CREATE TABLE co_ownership_contracts (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    group_id BIGINT NOT NULL,
    contract_title VARCHAR(150) NOT NULL,
    contract_terms_text LONGTEXT NOT NULL,
    version INT NOT NULL DEFAULT 1,
    status VARCHAR(30) NOT NULL DEFAULT 'DRAFT',
    effective_date DATE NULL,
    expiry_date DATE NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_contract_group FOREIGN KEY (group_id) REFERENCES ownership_groups(id) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE contract_signatures (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    contract_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    signature_hash VARCHAR(255) NOT NULL,
    signed_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    ip_address VARCHAR(45) NOT NULL,
    CONSTRAINT uk_contract_user_signature UNIQUE (contract_id, user_id),
    CONSTRAINT fk_signature_contract FOREIGN KEY (contract_id) REFERENCES co_ownership_contracts(id) ON DELETE CASCADE,
    CONSTRAINT fk_signature_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
