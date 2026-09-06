-- ===================================================================
-- EVShare 3D Platform - Flyway Migration V6
-- Module: Governance, Decision Chamber Voting & Dispute Resolution
-- Tables: proposals, vote_options, votes, disputes, dispute_evidences
-- ===================================================================

CREATE TABLE proposals (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    group_id BIGINT NOT NULL,
    proposer_user_id BIGINT NOT NULL,
    title VARCHAR(150) NOT NULL,
    description TEXT NOT NULL,
    proposal_type VARCHAR(40) NOT NULL,
    voting_deadline TIMESTAMP NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_proposal_group FOREIGN KEY (group_id) REFERENCES ownership_groups(id) ON DELETE RESTRICT,
    CONSTRAINT fk_proposal_proposer FOREIGN KEY (proposer_user_id) REFERENCES users(id) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE vote_options (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    proposal_id BIGINT NOT NULL,
    option_key VARCHAR(30) NOT NULL,
    label VARCHAR(100) NOT NULL,
    CONSTRAINT uk_proposal_option_key UNIQUE (proposal_id, option_key),
    CONSTRAINT fk_option_proposal FOREIGN KEY (proposal_id) REFERENCES proposals(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE votes (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    proposal_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    vote_option_id BIGINT NOT NULL,
    equity_weight DECIMAL(5, 2) NOT NULL,
    voted_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_vote_weight CHECK (equity_weight > 0.00 AND equity_weight <= 100.00),
    CONSTRAINT uk_proposal_user_vote UNIQUE (proposal_id, user_id),
    CONSTRAINT fk_vote_proposal FOREIGN KEY (proposal_id) REFERENCES proposals(id) ON DELETE CASCADE,
    CONSTRAINT fk_vote_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE RESTRICT,
    CONSTRAINT fk_vote_option FOREIGN KEY (vote_option_id) REFERENCES vote_options(id) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE disputes (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    group_id BIGINT NOT NULL,
    usage_session_id BIGINT NULL,
    complainant_user_id BIGINT NOT NULL,
    respondent_user_id BIGINT NULL,
    title VARCHAR(150) NOT NULL,
    description TEXT NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'OPEN',
    resolution_summary TEXT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_dispute_group FOREIGN KEY (group_id) REFERENCES ownership_groups(id) ON DELETE RESTRICT,
    CONSTRAINT fk_dispute_session FOREIGN KEY (usage_session_id) REFERENCES usage_sessions(id) ON DELETE SET NULL,
    CONSTRAINT fk_dispute_complainant FOREIGN KEY (complainant_user_id) REFERENCES users(id) ON DELETE RESTRICT,
    CONSTRAINT fk_dispute_respondent FOREIGN KEY (respondent_user_id) REFERENCES users(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE dispute_evidences (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    dispute_id BIGINT NOT NULL,
    uploaded_by_user_id BIGINT NOT NULL,
    file_url VARCHAR(255) NOT NULL,
    mesh_3d_defect_coordinates JSON NULL,
    description VARCHAR(255) NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_evidence_dispute FOREIGN KEY (dispute_id) REFERENCES disputes(id) ON DELETE CASCADE,
    CONSTRAINT fk_evidence_uploader FOREIGN KEY (uploaded_by_user_id) REFERENCES users(id) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
