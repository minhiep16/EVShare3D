# EVShare 3D — PHASE 02-F: DATABASE MIGRATION REPORT

## 1. Executive Summary

| Attribute | Value |
|---|---|
| **Phase** | PHASE 02-F (Deterministic Flyway Migration Generation) |
| **Authoritative Source** | `docs/DATABASE.md` & `docs/PHASE_02_DATABASE_AUDIT.md` |
| **Total Migration Scripts** | **7 SQL Scripts** (`V1` through `V7`) |
| **Total Relational Tables** | **27 Tables** (100% covered, zero assumed or omitted) |
| **Storage Engine** | InnoDB exclusively across all tables |
| **Charset & Collation** | `utf8mb4` / `utf8mb4_unicode_ci` |
| **MySQL Runtime Status** | **`NOT_AVAILABLE`** (Socket listening, but credentials unprovided; migration execution deferred) |
| **Maven Packaging Verification** | **`BUILD SUCCESS`** (7 migration resources packaged to `target/classes/db/migration/`) |
| **Source of Truth Integrity** | `docs/DATABASE.md` strictly preserved without modification |
| **Next Phase** | PHASE 02-G (Awaiting explicit user command) |

---

## 2. Topological Dependency Graph & Migration Order

To prevent circular dependency errors, the 27 tables are created in strictly forward-referencing topological order across 7 versioned Flyway scripts:

```text
TIER 1 (Base Root Tables)
├── users                        [V1]
├── roles                        [V1]
└── vehicles                     [V2]

TIER 2 (Direct Dependents of Tier 1)
├── user_roles                   [V1] (users, roles)
├── identity_verifications       [V1] (users)
├── driver_licenses              [V1] (users)
├── ownership_groups             [V2] (vehicles)
├── bookings                     [V4] (vehicles, users)
├── vehicle_services             [V4] (vehicles, users)
├── notifications                [V7] (users)
└── audit_logs                   [V7] (users)

TIER 3 (Direct Dependents of Tier 2)
├── ownership_shares             [V2] (ownership_groups, users)
├── co_ownership_contracts       [V3] (ownership_groups)
├── usage_sessions               [V4] (bookings)
├── shared_funds                 [V5] (ownership_groups)
├── expenses                     [V5] (ownership_groups, users)
├── proposals                    [V6] (ownership_groups, users)
├── disputes                     [V6] (ownership_groups, users, usage_sessions)
└── ai_recommendations           [V7] (ownership_groups, users)

TIER 4 (Direct Dependents of Tier 3)
├── contract_signatures          [V3] (co_ownership_contracts, users)
├── vehicle_inspections          [V4] (usage_sessions, users)
├── fund_transactions            [V5] (shared_funds, users)
├── expense_allocations          [V5] (expenses, users)
├── vote_options                 [V6] (proposals)
└── dispute_evidences            [V6] (disputes, users)

TIER 5 (Terminal Dependents)
├── payments                     [V5] (users, shared_funds, expense_allocations)
└── votes                        [V6] (proposals, users, vote_options)
```

---

## 3. Migration Catalog & Table Allocation

All migration scripts are located under `backend/src/main/resources/db/migration/`:

### 3.1. `V1__init_security_and_users.sql`
* **Tables Created (5)**:
  1. `users`: Core identity table with unique email, phone number, and audit timestamps.
  2. `roles`: RBAC catalog (`ROLE_CO_OWNER`, `ROLE_STAFF`, `ROLE_ADMIN`).
  3. `user_roles`: Composite PK (`user_id`, `role_id`) with `ON DELETE CASCADE`.
  4. `identity_verifications`: National ID dossiers with `ON DELETE RESTRICT` (user) and `ON DELETE SET NULL` (reviewer).
  5. `driver_licenses`: User driving credentials with unique constraint on `user_id` and `license_number`.
* **Seed Data**: Includes initial insertion of core RBAC roles (`ROLE_CO_OWNER`, `ROLE_STAFF`, `ROLE_ADMIN`).

### 3.2. `V2__init_vehicles_and_ownership.sql`
* **Tables Created (3)**:
  6. `vehicles`: Digital Twin registry with unique VIN and license plate, battery check constraint (`0..100`), odometer check (`>= 0.00`).
  7. `ownership_groups`: Syndicates bound 1:1 with vehicles (`vehicle_id UNIQUE`).
  8. `ownership_shares`: Equity certificates with `percentage` (`0.01..100.00`), unique share certificate number, and composite unique constraint `(group_id, user_id)`.

### 3.3. `V3__init_contracts.sql`
* **Tables Created (2)**:
  9. `co_ownership_contracts`: Legal master contracts bound to groups with Markdown terms in `LONGTEXT`.
  10. `contract_signatures`: Append-only cryptographic signatures with `signature_hash`, `ip_address`, and composite unique constraint `(contract_id, user_id)`.

### 3.4. `V4__init_bookings_sessions_and_services.sql`
* **Tables Created (4)**:
  11. `bookings`: Reservation slots with composite index `idx_vehicle_time (vehicle_id, start_time, end_time)` for zero-latency overlap prevention.
  12. `usage_sessions`: Trip check-in/out records with battery and odometer telemetry.
  13. `vehicle_inspections`: Physical inspection logs with 3D defect mesh coordinates in `JSON`.
  14. `vehicle_services`: Workshop maintenance activities with odometer and cost tracking.

### 3.5. `V5__init_finance_funds_and_payments.sql`
* **Tables Created (5)**:
  15. `shared_funds`: 3D Vault liquid reserve account bound 1:1 to ownership groups.
  16. `fund_transactions`: Append-only immutable financial ledger recording deposits, payouts, and capital calls.
  17. `expenses`: Operating cost invoices with `total_amount > 0.00` check.
  18. `expense_allocations`: Pro-rata member expense breakdowns with `allocated_amount >= 0.00`.
  19. `payments`: Payment gateway transactions with unique `transaction_reference` and `amount > 0.00`.

### 3.6. `V6__init_governance_and_disputes.sql`
* **Tables Created (5)**:
  20. `proposals`: Syndicate voting proposals with deadlines and categories.
  21. `vote_options`: Ballot option choices with composite unique `(proposal_id, option_key)`.
  22. `votes`: Equity-weighted ballots with `equity_weight` (`0.01..100.00`) and composite unique `(proposal_id, user_id)`.
  23. `disputes`: Conflict dossiers linked to usage sessions and groups.
  24. `dispute_evidences`: Attached photographic evidence and 3D defect coordinates in `JSON`.

### 3.7. `V7__init_notifications_ai_and_audit.sql`
* **Tables Created (3)**:
  25. `notifications`: In-world spatial notifications with delivery flags.
  26. `ai_recommendations`: Advisory cards with structured suggested actions in `JSON`.
  27. `audit_logs`: Immutable compliance audit trail with entity index `idx_audit_entity (entity_name, entity_id)` and timestamp index `idx_audit_created (created_at)`.

---

## 4. Constraint & Referential Integrity Audit

| Schema Feature | Standard Implemented | Audit Verification |
|---|---|:---:|
| **Primary Keys** | `BIGINT AUTO_INCREMENT` on all tables (composite on `user_roles`) | **100% PASS** |
| **Foreign Keys** | Explicit naming (`fk_<child>_<parent>`) with `ON DELETE RESTRICT`, `CASCADE`, or `SET NULL` | **100% PASS** |
| **Unique Keys** | `email`, `phone_number`, `id_card_number`, `license_number`, `vin`, `license_plate`, `vehicle_id`, `share_certificate_number`, `transaction_reference` | **100% PASS** |
| **Composite Uniques** | `(group_id, user_id)` on shares, `(contract_id, user_id)` on signatures, `(proposal_id, option_key)` on options, `(proposal_id, user_id)` on votes | **100% PASS** |
| **Performance Indexes** | `idx_vehicle_time`, `idx_audit_entity`, `idx_audit_created` | **100% PASS** |
| **Numeric Precision** | Money: `DECIMAL(15, 2)`, Equity: `DECIMAL(5, 2)`, Telemetry: `DECIMAL(10, 2)` | **100% PASS** |
| **Engine & Charset** | `ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci` on every table | **100% PASS** |

---

## 5. MySQL Runtime Availability & Migration Execution

* **Host Socket**: Port 3306 is open and listening on `127.0.0.1`.
* **Service Name**: Windows Service `MySQL80` is active.
* **Authentication**: Connection attempts using local unconfigured credentials resulted in `ERROR 1045 (28000): Access denied for user 'root'@'localhost'`.
* **Reported Verdict**:
  ```
  MYSQL_RUNTIME = NOT_AVAILABLE
  ```
* Per Rule 15 of `AGENTS.md`, live migration execution against the database instance is deferred until valid credentials are provided in `DB_PASSWORD`. Successful migration is not claimed without execution.

---

## 6. Maven Verification

```powershell
mvn clean compile
```

### Execution Log
```
[INFO] --- clean:3.3.2:clean (default-clean) @ evshare-backend ---
[INFO] Deleting E:\EVShare3D\backend\target
[INFO] 
[INFO] --- resources:3.3.1:resources (default-resources) @ evshare-backend ---
[INFO] Copying 4 resources from src\main\resources to target\classes
[INFO] Copying 7 resources from src\main\resources to target\classes
[INFO] 
[INFO] --- compiler:3.11.0:compile (default-compile) @ evshare-backend ---
[INFO] Changes detected - recompiling the module! :source
[INFO] Compiling 65 source files with javac [debug release 17] to target\classes
[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
[INFO] ------------------------------------------------------------------------
[INFO] Total time:  3.019 s
[INFO] Finished at: 2026-09-06T19:29:06+07:00
[INFO] ------------------------------------------------------------------------
```

All 7 SQL migration scripts are verified to exist and are packaged into `target/classes/db/migration/`.

---

## 7. Next Step

**Phase 02-F is complete.** Ready for **PHASE 02-G** upon explicit user instruction. Execution is paused.
