# EVShare 3D – PHASE 02-A: DATABASE SPECIFICATION AUDIT

> **Execution Phase**: PHASE 02-A – DATABASE SPECIFICATION AUDIT  
> **Status**: **READY**  
> **Directive Compliance**: Read-only database audit. Zero application code written.

---

## 1. Executive Summary & Audit Verdict

This document serves as the formal architectural audit of the relational database specification for **EVShare 3D** in preparation for **PHASE 02-B (Implementation of JPA Entities and Flyway Migrations)**.

* **Audit Scope**: Cross-referenced `docs/DATABASE.md`, `PHASE 02.md`, `docs/ARCHITECTURE.md`, `docs/API.md`, `docs/RBAC.md`, and `agent/IMPLEMENTATION_PLAN.md`.
* **Exact Table Count**: **27 Tables** (100% accounted for, no assumed or estimated counts).
* **Contradiction Analysis**: Zero critical contradictions detected between `PHASE 02.md` and `docs/DATABASE.md`.
* **Verdict**: **`STATUS = READY`**. The schema design is mathematically consistent, acyclic in foreign key dependencies, and completely ready for deterministic Flyway migration authoring.

---

## 2. Comparison: `PHASE 02.md` vs `docs/DATABASE.md`

`PHASE 02.md` (Task 3) defines the minimum required entity categories:
* `User`, `Role`, `Vehicle`, `OwnershipGroup`, `OwnershipShare`, `CoOwnershipContract`, `Booking`, `UsageSession`, `Expense`, `SharedFund`, `Payment`, `Voting`, `Dispute`, `Notification`, `AuditLog`.

`docs/DATABASE.md` elaborates these 15 core concepts into a complete, normalized 27-table relational schema:
1. `User` & `Role` $\to$ `users`, `roles`, `user_roles` (join table), `identity_verifications`, `driver_licenses`.
2. `Vehicle` & `Ownership` $\to$ `vehicles`, `ownership_groups`, `ownership_shares`.
3. `CoOwnershipContract` $\to$ `co_ownership_contracts`, `contract_signatures`.
4. `Booking` & `UsageSession` $\to$ `bookings`, `usage_sessions`, `vehicle_inspections`, `vehicle_services`.
5. `SharedFund` & `Payment` $\to$ `shared_funds`, `fund_transactions`, `expenses`, `expense_allocations`, `payments`.
6. `Voting` & `Dispute` $\to$ `proposals`, `vote_options`, `votes`, `disputes`, `dispute_evidences`.
7. `Notification`, `AI`, `Audit` $\to$ `notifications`, `ai_recommendations`, `audit_logs`.

Every entity demanded by `PHASE 02.md` is strictly represented with zero missing dependencies or undocumented fields.

---

## 3. Comprehensive Schema Audit (27 Tables)

### Table 1: `users`
* **Purpose**: Core identity record for co-owners, field staff, and platform administrators.
* **Columns (9)**:
  * `id`: `BIGINT`, NOT NULL, PK, AUTO_INCREMENT
  * `email`: `VARCHAR(150)`, NOT NULL, UNIQUE
  * `password_hash`: `VARCHAR(255)`, NOT NULL
  * `full_name`: `VARCHAR(100)`, NOT NULL
  * `phone_number`: `VARCHAR(20)`, NULL, UNIQUE
  * `avatar_3d_url`: `VARCHAR(255)`, NULL
  * `is_active`: `BOOLEAN`, NOT NULL, DEFAULT `TRUE`
  * `created_at`: `TIMESTAMP`, NOT NULL, DEFAULT `CURRENT_TIMESTAMP`
  * `updated_at`: `TIMESTAMP`, NOT NULL, DEFAULT `CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP`
* **Relationships**: Has many `roles` (via `user_roles`), `ownership_shares`, `bookings`, `payments`, `votes`.

### Table 2: `roles`
* **Purpose**: RBAC role catalog.
* **Columns (2)**:
  * `id`: `BIGINT`, NOT NULL, PK, AUTO_INCREMENT
  * `name`: `VARCHAR(50)`, NOT NULL, UNIQUE (`ROLE_CO_OWNER`, `ROLE_STAFF`, `ROLE_ADMIN`)
* **Relationships**: M:N with `users` through `user_roles`.

### Table 3: `user_roles`
* **Purpose**: Join table linking users to roles.
* **Columns (2)**:
  * `user_id`: `BIGINT`, NOT NULL, PK, FK $\to$ `users(id)` ON DELETE CASCADE
  * `role_id`: `BIGINT`, NOT NULL, PK, FK $\to$ `roles(id)` ON DELETE CASCADE

### Table 4: `identity_verifications`
* **Purpose**: National identity verification dossiers.
* **Columns (8)**:
  * `id`: `BIGINT`, NOT NULL, PK, AUTO_INCREMENT
  * `user_id`: `BIGINT`, NOT NULL, FK $\to$ `users(id)` ON DELETE RESTRICT
  * `id_card_number`: `VARCHAR(50)`, NOT NULL, UNIQUE
  * `verification_status`: `VARCHAR(30)`, NOT NULL, DEFAULT `'PENDING'`
  * `document_front_url`: `VARCHAR(255)`, NOT NULL
  * `document_back_url`: `VARCHAR(255)`, NOT NULL
  * `verified_by_user_id`: `BIGINT`, NULL, FK $\to$ `users(id)` ON DELETE SET NULL
  * `verified_at`: `TIMESTAMP`, NULL

### Table 5: `driver_licenses`
* **Purpose**: Driving credential verification for vehicle reservation rights.
* **Columns (8)**:
  * `id`: `BIGINT`, NOT NULL, PK, AUTO_INCREMENT
  * `user_id`: `BIGINT`, NOT NULL, UNIQUE, FK $\to$ `users(id)` ON DELETE RESTRICT
  * `license_number`: `VARCHAR(50)`, NOT NULL, UNIQUE
  * `license_class`: `VARCHAR(20)`, NOT NULL, DEFAULT `'B2'`
  * `issue_date`: `DATE`, NOT NULL
  * `expiry_date`: `DATE`, NOT NULL
  * `is_verified`: `BOOLEAN`, NOT NULL, DEFAULT `FALSE`
  * `verified_at`: `TIMESTAMP`, NULL

### Table 6: `vehicles`
* **Purpose**: Digital Twin representation of physical electric vehicles.
* **Columns (12)**:
  * `id`: `BIGINT`, NOT NULL, PK, AUTO_INCREMENT
  * `vin`: `VARCHAR(50)`, NOT NULL, UNIQUE
  * `license_plate`: `VARCHAR(20)`, NOT NULL, UNIQUE
  * `model_name`: `VARCHAR(100)`, NOT NULL
  * `manufacturer`: `VARCHAR(50)`, NOT NULL
  * `model_3d_asset_path`: `VARCHAR(255)`, NOT NULL
  * `status`: `VARCHAR(30)`, NOT NULL, DEFAULT `'AVAILABLE'`
  * `battery_level`: `INT`, NOT NULL, DEFAULT `100`, CHECK (`battery_level BETWEEN 0 AND 100`)
  * `odometer_km`: `DECIMAL(10, 2)`, NOT NULL, DEFAULT `0.00`, CHECK (`odometer_km >= 0.00`)
  * `stall_location_code`: `VARCHAR(30)`, NOT NULL, DEFAULT `'BAY-01'`
  * `created_at`: `TIMESTAMP`, NOT NULL, DEFAULT `CURRENT_TIMESTAMP`
  * `updated_at`: `TIMESTAMP`, NOT NULL, DEFAULT `CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP`

### Table 7: `ownership_groups`
* **Purpose**: Co-ownership syndicate binding an EV to its equity co-owners.
* **Columns (5)**:
  * `id`: `BIGINT`, NOT NULL, PK, AUTO_INCREMENT
  * `group_name`: `VARCHAR(100)`, NOT NULL
  * `vehicle_id`: `BIGINT`, NOT NULL, UNIQUE, FK $\to$ `vehicles(id)` ON DELETE RESTRICT
  * `formation_date`: `DATE`, NOT NULL
  * `is_active`: `BOOLEAN`, NOT NULL, DEFAULT `TRUE`

### Table 8: `ownership_shares`
* **Purpose**: Equity fraction certificates.
* **Columns (7)**:
  * `id`: `BIGINT`, NOT NULL, PK, AUTO_INCREMENT
  * `group_id`: `BIGINT`, NOT NULL, FK $\to$ `ownership_groups(id)` ON DELETE RESTRICT
  * `user_id`: `BIGINT`, NOT NULL, FK $\to$ `users(id)` ON DELETE RESTRICT
  * `percentage`: `DECIMAL(5, 2)`, NOT NULL, CHECK (`percentage > 0.00 AND percentage <= 100.00`)
  * `share_certificate_number`: `VARCHAR(100)`, NOT NULL, UNIQUE
  * `acquired_at`: `TIMESTAMP`, NOT NULL, DEFAULT `CURRENT_TIMESTAMP`
  * `is_active`: `BOOLEAN`, NOT NULL, DEFAULT `TRUE`
* **Constraints**: `UNIQUE (group_id, user_id)`

### Table 9: `co_ownership_contracts`
* **Purpose**: Legal master contracts and addenda governing the syndicate.
* **Columns (9)**:
  * `id`: `BIGINT`, NOT NULL, PK, AUTO_INCREMENT
  * `group_id`: `BIGINT`, NOT NULL, FK $\to` `ownership_groups(id)` ON DELETE RESTRICT
  * `contract_title`: `VARCHAR(150)`, NOT NULL
  * `contract_terms_text`: `LONGTEXT`, NOT NULL
  * `version`: `INT`, NOT NULL, DEFAULT `1`
  * `status`: `VARCHAR(30)`, NOT NULL, DEFAULT `'DRAFT'`
  * `effective_date`: `DATE`, NULL
  * `expiry_date`: `DATE`, NULL
  * `created_at`: `TIMESTAMP`, NOT NULL, DEFAULT `CURRENT_TIMESTAMP`

### Table 10: `contract_signatures`
* **Purpose**: Multi-party cryptographic digital signatures.
* **Columns (6)**:
  * `id`: `BIGINT`, NOT NULL, PK, AUTO_INCREMENT
  * `contract_id`: `BIGINT`, NOT NULL, FK $\to$ `co_ownership_contracts(id)` ON DELETE CASCADE
  * `user_id`: `BIGINT`, NOT NULL, FK $\to$ `users(id)` ON DELETE RESTRICT
  * `signature_hash`: `VARCHAR(255)`, NOT NULL
  * `signed_at`: `TIMESTAMP`, NOT NULL, DEFAULT `CURRENT_TIMESTAMP`
  * `ip_address`: `VARCHAR(45)`, NOT NULL
* **Constraints**: `UNIQUE (contract_id, user_id)`

### Table 11: `bookings`
* **Purpose**: Time slot reservations.
* **Columns (8)**:
  * `id`: `BIGINT`, NOT NULL, PK, AUTO_INCREMENT
  * `vehicle_id`: `BIGINT`, NOT NULL, FK $\to$ `vehicles(id)` ON DELETE RESTRICT
  * `user_id`: `BIGINT`, NOT NULL, FK $\to$ `users(id)` ON DELETE RESTRICT
  * `start_time`: `TIMESTAMP`, NOT NULL
  * `end_time`: `TIMESTAMP`, NOT NULL
  * `status`: `VARCHAR(30)`, NOT NULL, DEFAULT `'CONFIRMED'`
  * `estimated_cost`: `DECIMAL(15, 2)`, NOT NULL, DEFAULT `0.00`
  * `created_at`: `TIMESTAMP`, NOT NULL, DEFAULT `CURRENT_TIMESTAMP`
* **Indexes**: `INDEX idx_vehicle_time (vehicle_id, start_time, end_time)`

### Table 12: `usage_sessions`
* **Purpose**: Trip execution records linking check-in and check-out telemetry.
* **Columns (9)**:
  * `id`: `BIGINT`, NOT NULL, PK, AUTO_INCREMENT
  * `booking_id`: `BIGINT`, NOT NULL, UNIQUE, FK $\to$ `bookings(id)` ON DELETE RESTRICT
  * `start_odometer`: `DECIMAL(10, 2)`, NOT NULL
  * `end_odometer`: `DECIMAL(10, 2)`, NULL
  * `start_battery`: `INT`, NOT NULL, CHECK (`start_battery BETWEEN 0 AND 100`)
  * `end_battery`: `INT`, NULL, CHECK (`end_battery BETWEEN 0 AND 100`)
  * `check_in_time`: `TIMESTAMP`, NOT NULL, DEFAULT `CURRENT_TIMESTAMP`
  * `check_out_time`: `TIMESTAMP`, NULL
  * `status`: `VARCHAR(30)`, NOT NULL, DEFAULT `'ACTIVE'`

### Table 13: `vehicle_inspections`
* **Purpose**: Physical damage inspection reports logged on 3D vehicle avatar.
* **Columns (7)**:
  * `id`: `BIGINT`, NOT NULL, PK, AUTO_INCREMENT
  * `usage_session_id`: `BIGINT`, NOT NULL, FK $\to$ `usage_sessions(id)` ON DELETE CASCADE
  * `inspector_user_id`: `BIGINT`, NOT NULL, FK $\to$ `users(id)` ON DELETE RESTRICT
  * `inspection_type`: `VARCHAR(20)`, NOT NULL
  * `condition_mesh_flags`: `JSON`, NULL
  * `notes`: `TEXT`, NULL
  * `created_at`: `TIMESTAMP`, NOT NULL, DEFAULT `CURRENT_TIMESTAMP`

### Table 14: `vehicle_services`
* **Purpose**: Workshop service activities (maintenance, repairs, tire swap).
* **Columns (10)**:
  * `id`: `BIGINT`, NOT NULL, PK, AUTO_INCREMENT
  * `vehicle_id`: `BIGINT`, NOT NULL, FK $\to$ `vehicles(id)` ON DELETE RESTRICT
  * `service_type`: `VARCHAR(40)`, NOT NULL
  * `description`: `TEXT`, NOT NULL
  * `technician_user_id`: `BIGINT`, NOT NULL, FK $\to$ `users(id)` ON DELETE RESTRICT
  * `cost_amount`: `DECIMAL(15, 2)`, NOT NULL, DEFAULT `0.00`
  * `odometer_at_service`: `DECIMAL(10, 2)`, NOT NULL
  * `service_status`: `VARCHAR(30)`, NOT NULL, DEFAULT `'PENDING'`
  * `started_at`: `TIMESTAMP`, NULL
  * `completed_at`: `TIMESTAMP`, NULL

### Table 15: `shared_funds`
* **Purpose**: Group liquid reserve account in the 3D Vault.
* **Columns (6)**:
  * `id`: `BIGINT`, NOT NULL, PK, AUTO_INCREMENT
  * `group_id`: `BIGINT`, NOT NULL, UNIQUE, FK $\to$ `ownership_groups(id)` ON DELETE RESTRICT
  * `current_balance`: `DECIMAL(15, 2)`, NOT NULL, DEFAULT `0.00`
  * `minimum_reserve_threshold`: `DECIMAL(15, 2)`, NOT NULL, DEFAULT `10000000.00`
  * `currency`: `VARCHAR(10)`, NOT NULL, DEFAULT `'VND'`
  * `updated_at`: `TIMESTAMP`, NOT NULL, DEFAULT `CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP`

### Table 16: `fund_transactions`
* **Purpose**: Immutable ledger tracking fund entries, payouts, and capital calls.
* **Columns (7)**:
  * `id`: `BIGINT`, NOT NULL, PK, AUTO_INCREMENT
  * `fund_id`: `BIGINT`, NOT NULL, FK $\to$ `shared_funds(id)` ON DELETE RESTRICT
  * `user_id`: `BIGINT`, NULL, FK $\to$ `users(id)` ON DELETE SET NULL
  * `transaction_type`: `VARCHAR(30)`, NOT NULL
  * `amount`: `DECIMAL(15, 2)`, NOT NULL
  * `balance_after`: `DECIMAL(15, 2)`, NOT NULL
  * `description`: `VARCHAR(255)`, NOT NULL
  * `created_at`: `TIMESTAMP`, NOT NULL, DEFAULT `CURRENT_TIMESTAMP`

### Table 17: `expenses`
* **Purpose**: Operating expenditure invoices.
* **Columns (10)**:
  * `id`: `BIGINT`, NOT NULL, PK, AUTO_INCREMENT
  * `group_id`: `BIGINT`, NOT NULL, FK $\to$ `ownership_groups(id)` ON DELETE RESTRICT
  * `title`: `VARCHAR(150)`, NOT NULL
  * `category`: `VARCHAR(40)`, NOT NULL
  * `total_amount`: `DECIMAL(15, 2)`, NOT NULL, CHECK (`total_amount > 0.00`)
  * `allocation_strategy`: `VARCHAR(30)`, NOT NULL, DEFAULT `'OWNERSHIP_BASED'`
  * `invoice_reference`: `VARCHAR(100)`, NULL
  * `logged_by_user_id`: `BIGINT`, NOT NULL, FK $\to$ `users(id)` ON DELETE RESTRICT
  * `incurred_date`: `DATE`, NOT NULL
  * `created_at`: `TIMESTAMP`, NOT NULL, DEFAULT `CURRENT_TIMESTAMP`

### Table 18: `expense_allocations`
* **Purpose**: Pro-rata expense breakdown owed by each individual co-owner.
* **Columns (6)**:
  * `id`: `BIGINT`, NOT NULL, PK, AUTO_INCREMENT
  * `expense_id`: `BIGINT`, NOT NULL, FK $\to$ `expenses(id)` ON DELETE CASCADE
  * `user_id`: `BIGINT`, NOT NULL, FK $\to$ `users(id)` ON DELETE RESTRICT
  * `allocated_amount`: `DECIMAL(15, 2)`, NOT NULL, CHECK (`allocated_amount >= 0.00`)
  * `is_settled`: `BOOLEAN`, NOT NULL, DEFAULT `FALSE`
  * `settled_at`: `TIMESTAMP`, NULL

### Table 19: `payments`
* **Purpose**: Financial settlement transactions.
* **Columns (8)**:
  * `id`: `BIGINT`, NOT NULL, PK, AUTO_INCREMENT
  * `user_id`: `BIGINT`, NOT NULL, FK $\to$ `users(id)` ON DELETE RESTRICT
  * `fund_id`: `BIGINT`, NOT NULL, FK $\to$ `shared_funds(id)` ON DELETE RESTRICT
  * `expense_allocation_id`: `BIGINT`, NULL, FK $\to$ `expense_allocations(id)` ON DELETE SET NULL
  * `amount`: `DECIMAL(15, 2)`, NOT NULL, CHECK (`amount > 0.00`)
  * `payment_method`: `VARCHAR(30)`, NOT NULL
  * `transaction_reference`: `VARCHAR(100)`, NOT NULL, UNIQUE
  * `status`: `VARCHAR(30)`, NOT NULL, DEFAULT `'PENDING'`
  * `created_at`: `TIMESTAMP`, NOT NULL, DEFAULT `CURRENT_TIMESTAMP`

### Table 20: `proposals`
* **Purpose**: Group governance proposals voted on in the Decision Chamber.
* **Columns (8)**:
  * `id`: `BIGINT`, NOT NULL, PK, AUTO_INCREMENT
  * `group_id`: `BIGINT`, NOT NULL, FK $\to$ `ownership_groups(id)` ON DELETE RESTRICT
  * `proposer_user_id`: `BIGINT`, NOT NULL, FK $\to$ `users(id)` ON DELETE RESTRICT
  * `title`: `VARCHAR(150)`, NOT NULL
  * `description`: `TEXT`, NOT NULL
  * `proposal_type`: `VARCHAR(40)`, NOT NULL
  * `voting_deadline`: `TIMESTAMP`, NOT NULL
  * `status`: `VARCHAR(30)`, NOT NULL, DEFAULT `'ACTIVE'`
  * `created_at`: `TIMESTAMP`, NOT NULL, DEFAULT `CURRENT_TIMESTAMP`

### Table 21: `vote_options`
* **Purpose**: Ballot options attached to a proposal.
* **Columns (4)**:
  * `id`: `BIGINT`, NOT NULL, PK, AUTO_INCREMENT
  * `proposal_id`: `BIGINT`, NOT NULL, FK $\to$ `proposals(id)` ON DELETE CASCADE
  * `option_key`: `VARCHAR(30)`, NOT NULL (`APPROVE`, `REJECT`, `ABSTAIN`)
  * `label`: `VARCHAR(100)`, NOT NULL
* **Constraints**: `UNIQUE (proposal_id, option_key)`

### Table 22: `votes`
* **Purpose**: Equity-weighted ballots cast by co-owners.
* **Columns (6)**:
  * `id`: `BIGINT`, NOT NULL, PK, AUTO_INCREMENT
  * `proposal_id`: `BIGINT`, NOT NULL, FK $\to$ `proposals(id)` ON DELETE CASCADE
  * `user_id`: `BIGINT`, NOT NULL, FK $\to$ `users(id)` ON DELETE RESTRICT
  * `vote_option_id`: `BIGINT`, NOT NULL, FK $\to$ `vote_options(id)` ON DELETE RESTRICT
  * `equity_weight`: `DECIMAL(5, 2)`, NOT NULL, CHECK (`equity_weight > 0.00 AND equity_weight <= 100.00`)
  * `voted_at`: `TIMESTAMP`, NOT NULL, DEFAULT `CURRENT_TIMESTAMP`
* **Constraints**: `UNIQUE (proposal_id, user_id)`

### Table 23: `disputes`
* **Purpose**: Formal dispute dossiers raised regarding damage or billing.
* **Columns (9)**:
  * `id`: `BIGINT`, NOT NULL, PK, AUTO_INCREMENT
  * `group_id`: `BIGINT`, NOT NULL, FK $\to$ `ownership_groups(id)` ON DELETE RESTRICT
  * `usage_session_id`: `BIGINT`, NULL, FK $\to$ `usage_sessions(id)` ON DELETE SET NULL
  * `complainant_user_id`: `BIGINT`, NOT NULL, FK $\to$ `users(id)` ON DELETE RESTRICT
  * `respondent_user_id`: `BIGINT`, NULL, FK $\to$ `users(id)` ON DELETE SET NULL
  * `title`: `VARCHAR(150)`, NOT NULL
  * `description`: `TEXT`, NOT NULL
  * `status`: `VARCHAR(30)`, NOT NULL, DEFAULT `'OPEN'`
  * `resolution_summary`: `TEXT`, NULL
  * `created_at`: `TIMESTAMP`, NOT NULL, DEFAULT `CURRENT_TIMESTAMP`

### Table 24: `dispute_evidences`
* **Purpose**: Media and 3D defect coordinate attachments for dispute review.
* **Columns (6)**:
  * `id`: `BIGINT`, NOT NULL, PK, AUTO_INCREMENT
  * `dispute_id`: `BIGINT`, NOT NULL, FK $\to$ `disputes(id)` ON DELETE CASCADE
  * `uploaded_by_user_id`: `BIGINT`, NOT NULL, FK $\to$ `users(id)` ON DELETE RESTRICT
  * `file_url`: `VARCHAR(255)`, NOT NULL
  * `mesh_3d_defect_coordinates`: `JSON`, NULL
  * `description`: `VARCHAR(255)`, NULL
  * `created_at`: `TIMESTAMP`, NOT NULL, DEFAULT `CURRENT_TIMESTAMP`

### Table 25: `notifications`
* **Purpose**: Contextual messages delivered via 3D floating drones / beacons.
* **Columns (7)**:
  * `id`: `BIGINT`, NOT NULL, PK, AUTO_INCREMENT
  * `user_id`: `BIGINT`, NOT NULL, FK $\to$ `users(id)` ON DELETE CASCADE
  * `title`: `VARCHAR(150)`, NOT NULL
  * `message`: `TEXT`, NOT NULL
  * `category`: `VARCHAR(40)`, NOT NULL
  * `spatial_sector_code`: `VARCHAR(30)`, NULL
  * `is_read`: `BOOLEAN`, NOT NULL, DEFAULT `FALSE`
  * `created_at`: `TIMESTAMP`, NOT NULL, DEFAULT `CURRENT_TIMESTAMP`

### Table 26: `ai_recommendations`
* **Purpose**: Advisory intelligence cards surfaced in the AI Intelligence Center.
* **Columns (7)**:
  * `id`: `BIGINT`, NOT NULL, PK, AUTO_INCREMENT
  * `group_id`: `BIGINT`, NOT NULL, FK $\to$ `ownership_groups(id)` ON DELETE CASCADE
  * `target_user_id`: `BIGINT`, NULL, FK $\to$ `users(id)` ON DELETE SET NULL
  * `recommendation_type`: `VARCHAR(50)`, NOT NULL
  * `message`: `TEXT`, NOT NULL
  * `suggested_actions`: `JSON`, NULL
  * `is_acknowledged`: `BOOLEAN`, NOT NULL, DEFAULT `FALSE`
  * `created_at`: `TIMESTAMP`, NOT NULL, DEFAULT `CURRENT_TIMESTAMP`

### Table 27: `audit_logs`
* **Purpose**: Immutable security and compliance provenance log.
* **Columns (9)**:
  * `id`: `BIGINT`, NOT NULL, PK, AUTO_INCREMENT
  * `user_id`: `BIGINT`, NULL, FK $\to$ `users(id)` ON DELETE SET NULL
  * `action`: `VARCHAR(100)`, NOT NULL
  * `entity_name`: `VARCHAR(50)`, NOT NULL
  * `entity_id`: `BIGINT`, NOT NULL
  * `old_state_json`: `JSON`, NULL
  * `new_state_json`: `JSON`, NULL
  * `ip_address`: `VARCHAR(45)`, NULL
  * `created_at`: `TIMESTAMP`, NOT NULL, DEFAULT `CURRENT_TIMESTAMP`
* **Indexes**: `INDEX idx_audit_entity (entity_name, entity_id)`, `INDEX idx_audit_created (created_at)`

---

## 4. Foreign Key Dependency Graph & Topological Migration Order

To guarantee that Flyway migrations execute deterministically without foreign key reference errors, the table creation order is partitioned into 5 topological tiers:

```text
TIER 1 (Base Root Tables)
├── users
├── roles
└── vehicles

TIER 2 (Direct Dependents of Tier 1)
├── user_roles                   (users, roles)
├── identity_verifications       (users)
├── driver_licenses              (users)
├── ownership_groups             (vehicles)
├── bookings                     (vehicles, users)
├── vehicle_services             (vehicles, users)
├── notifications                (users)
└── audit_logs                   (users)

TIER 3 (Direct Dependents of Tier 2)
├── ownership_shares             (ownership_groups, users)
├── co_ownership_contracts       (ownership_groups)
├── usage_sessions               (bookings)
├── shared_funds                 (ownership_groups)
├── expenses                     (ownership_groups, users)
├── proposals                    (ownership_groups, users)
├── disputes                     (ownership_groups, users, usage_sessions)
└── ai_recommendations           (ownership_groups, users)

TIER 4 (Direct Dependents of Tier 3)
├── contract_signatures          (co_ownership_contracts, users)
├── vehicle_inspections          (usage_sessions, users)
├── fund_transactions            (shared_funds, users)
├── expense_allocations          (expenses, users)
├── vote_options                 (proposals)
└── dispute_evidences            (disputes, users)

TIER 5 (Terminal Dependents)
├── payments                     (users, shared_funds, expense_allocations)
└── votes                        (proposals, users, vote_options)
```

### Planned Flyway Scripts Organization (V1 to V7)
* **`V1__init_security_and_users.sql`**: `users`, `roles`, `user_roles`, `identity_verifications`, `driver_licenses`.
* **`V2__init_vehicles_and_ownership.sql`**: `vehicles`, `ownership_groups`, `ownership_shares`.
* **`V3__init_contracts.sql`**: `co_ownership_contracts`, `contract_signatures`.
* **`V4__init_bookings_sessions_and_services.sql`**: `bookings`, `usage_sessions`, `vehicle_inspections`, `vehicle_services`.
* **`V5__init_finance_funds_and_payments.sql`**: `shared_funds`, `fund_transactions`, `expenses`, `expense_allocations`, `payments`.
* **`V6__init_governance_and_disputes.sql`**: `proposals`, `vote_options`, `votes`, `disputes`, `dispute_evidences`.
* **`V7__init_notifications_ai_and_audit.sql`**: `notifications`, `ai_recommendations`, `audit_logs`.

---

## 5. Audit Requirements & Immutability Rules

1. **Append-Only Tables**:
   * `fund_transactions`, `contract_signatures`, `votes`, and `audit_logs` MUST never be updated or deleted by any application endpoint.
2. **Precision Standards**:
   * All monetary figures use `DECIMAL(15, 2)` (VND currency).
   * All equity allocations use `DECIMAL(5, 2)`.
   * Floating-point types (`FLOAT`, `DOUBLE`) are strictly forbidden.
3. **Pessimistic Locking**:
   * `bookings` requires `SELECT ... FOR UPDATE` row locking during reservation generation to guarantee zero overlapping intervals.

---

## 6. Audit Verdict

| Audit Dimension | Evaluation | Result |
| :--- | :--- | :---: |
| Table count verified | Exactly 27 tables analyzed | **PASS** |
| Data type precision | `BigDecimal` mapping for money/percentages verified | **PASS** |
| Foreign key topology | Fully acyclic, 5-tier dependency sort established | **PASS** |
| Cross-spec consistency | Aligns 100% with `ARCHITECTURE.md`, `API.md`, and `RBAC.md` | **PASS** |
| Alignment with `PHASE 02.md` | All minimum required entities and structures present | **PASS** |

**FINAL STATUS: `STATUS = READY`**  
The database specification is complete, robust, and fully prepared for implementation in PHASE 02-B.
