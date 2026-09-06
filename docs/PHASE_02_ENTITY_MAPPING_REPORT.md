# EVShare 3D — PHASE 02-E: JPA ENTITY MAPPING AUDIT REPORT

## 1. Executive Summary

| Attribute | Value |
|---|---|
| **Phase** | PHASE 02-E (JPA Entity Implementation & Schema Mapping) |
| **Authoritative Specification** | `docs/DATABASE.md` & `docs/PHASE_02_DATABASE_AUDIT.md` |
| **Total Relational Tables** | **27 Tables** |
| **Total JPA Entities** | **27 Entities** (Including join table `UserRole` & `UserRoleId`) |
| **Total Enumerations** | **20 Enums** (Package `com.example.evshare.entity.enums`) |
| **Compilation Result** | **`PASS`** (`mvn clean compile` -> `BUILD SUCCESS`, 65 source files compiled in 3.528s) |
| **Unit Test Result** | **`PASS`** (7/7 tests passed in 3.973s) |
| **Mapping Compliance** | **100% Exact Match** (Zero invented fields, zero missing tables/columns) |
| **Next Phase** | PHASE 02-F (Flyway Migrations V1–V7 — On Explicit Instruction) |

---

## 2. Exhaustive Comparison Matrix: `DATABASE.md` ↔ JPA Entities

Below is the complete 27-table comparison proving full compliance across all architectural criteria.

| # | Table Name (`DATABASE.md`) | JPA Entity Class | Primary Key | Column Count | Foreign Keys & Relationships | Enum Mappings | BigDecimals (Scale/Precision) | Status |
|:---|:---|:---|:---|:---:|:---|:---|:---|:---:|
| 1 | `users` | `User.java` | `id` (BIGINT AUTO) | 9 | `@ManyToMany` $\to$ `roles` | - | - | **MATCH** |
| 2 | `roles` | `Role.java` | `id` (BIGINT AUTO) | 2 | Inverse join $\to$ `users` | `RoleName` | - | **MATCH** |
| 3 | `user_roles` | `UserRole.java` (`UserRoleId`) | `(user_id, role_id)` | 2 | `@ManyToOne` $\to$ `users`, `roles` | - | - | **MATCH** |
| 4 | `identity_verifications` | `IdentityVerification.java` | `id` (BIGINT AUTO) | 8 | `@ManyToOne` $\to$ `users` (applicant & verifier) | `VerificationStatus` | - | **MATCH** |
| 5 | `driver_licenses` | `DriverLicense.java` | `id` (BIGINT AUTO) | 8 | `@OneToOne` $\to$ `users` (unique) | `DriverLicenseClass` | - | **MATCH** |
| 6 | `vehicles` | `Vehicle.java` | `id` (BIGINT AUTO) | 12 | Root EV digital twin | `VehicleStatus` | `odometer_km` (10, 2) | **MATCH** |
| 7 | `ownership_groups` | `OwnershipGroup.java` | `id` (BIGINT AUTO) | 5 | `@OneToOne` $\to$ `vehicles` | - | - | **MATCH** |
| 8 | `ownership_shares` | `OwnershipShare.java` | `id` (BIGINT AUTO) | 7 | `@ManyToOne` $\to$ `ownership_groups`, `users` | - | `percentage` (5, 2) | **MATCH** |
| 9 | `co_ownership_contracts` | `CoOwnershipContract.java`| `id` (BIGINT AUTO) | 9 | `@ManyToOne` $\to$ `ownership_groups` | `ContractStatus` | - | **MATCH** |
| 10| `contract_signatures` | `ContractSignature.java` | `id` (BIGINT AUTO) | 6 | `@ManyToOne` $\to$ `co_ownership_contracts`, `users` | - | - | **MATCH** |
| 11| `bookings` | `Booking.java` | `id` (BIGINT AUTO) | 8 | `@ManyToOne` $\to$ `vehicles`, `users` | `BookingStatus` | `estimated_cost` (15, 2) | **MATCH** |
| 12| `usage_sessions` | `UsageSession.java` | `id` (BIGINT AUTO) | 9 | `@OneToOne` $\to$ `bookings` | `UsageSessionStatus` | `start_odometer` (10, 2), `end_odometer` (10, 2) | **MATCH** |
| 13| `vehicle_inspections` | `VehicleInspection.java` | `id` (BIGINT AUTO) | 7 | `@ManyToOne` $\to$ `usage_sessions`, `users` | `InspectionType` | - | **MATCH** |
| 14| `vehicle_services` | `VehicleService.java` | `id` (BIGINT AUTO) | 10 | `@ManyToOne` $\to$ `vehicles`, `users` | `ServiceType`, `ServiceStatus` | `cost_amount` (15, 2), `odometer_at_service` (10, 2) | **MATCH** |
| 15| `shared_funds` | `SharedFund.java` | `id` (BIGINT AUTO) | 6 | `@OneToOne` $\to$ `ownership_groups` | - | `current_balance` (15, 2), `minimum_reserve_threshold` (15, 2) | **MATCH** |
| 16| `fund_transactions` | `FundTransaction.java` | `id` (BIGINT AUTO) | 7 | `@ManyToOne` $\to$ `shared_funds`, `users` | `TransactionType` | `amount` (15, 2), `balance_after` (15, 2) | **MATCH** |
| 17| `expenses` | `Expense.java` | `id` (BIGINT AUTO) | 10 | `@ManyToOne` $\to$ `ownership_groups`, `users` | `ExpenseCategory`, `AllocationStrategy` | `total_amount` (15, 2) | **MATCH** |
| 18| `expense_allocations` | `ExpenseAllocation.java` | `id` (BIGINT AUTO) | 6 | `@ManyToOne` $\to$ `expenses`, `users` | - | `allocated_amount` (15, 2) | **MATCH** |
| 19| `payments` | `Payment.java` | `id` (BIGINT AUTO) | 8 | `@ManyToOne` $\to$ `users`, `shared_funds`, `expense_allocations` | `PaymentMethod`, `PaymentStatus` | `amount` (15, 2) | **MATCH** |
| 20| `proposals` | `Proposal.java` | `id` (BIGINT AUTO) | 8 | `@ManyToOne` $\to$ `ownership_groups`, `users` | `ProposalType`, `ProposalStatus` | - | **MATCH** |
| 21| `vote_options` | `VoteOption.java` | `id` (BIGINT AUTO) | 4 | `@ManyToOne` $\to$ `proposals` | `VoteOptionKey` | - | **MATCH** |
| 22| `votes` | `Vote.java` | `id` (BIGINT AUTO) | 6 | `@ManyToOne` $\to$ `proposals`, `users`, `vote_options` | - | `equity_weight` (5, 2) | **MATCH** |
| 23| `disputes` | `Dispute.java` | `id` (BIGINT AUTO) | 9 | `@ManyToOne` $\to$ `ownership_groups`, `usage_sessions`, `users` | `DisputeStatus` | - | **MATCH** |
| 24| `dispute_evidences` | `DisputeEvidence.java` | `id` (BIGINT AUTO) | 6 | `@ManyToOne` $\to$ `disputes`, `users` | - | - | **MATCH** |
| 25| `notifications` | `Notification.java` | `id` (BIGINT AUTO) | 7 | `@ManyToOne` $\to$ `users` | `NotificationCategory` | - | **MATCH** |
| 26| `ai_recommendations` | `AiRecommendation.java` | `id` (BIGINT AUTO) | 7 | `@ManyToOne` $\to$ `ownership_groups`, `users` | - | - | **MATCH** |
| 27| `audit_logs` | `AuditLog.java` | `id` (BIGINT AUTO) | 9 | `@ManyToOne` $\to$ `users` | - | - | **MATCH** |

---

## 3. Enumeration Catalog (20 Enums in `com.example.evshare.entity.enums`)

All 20 domain enums match Section 2 of `docs/DATABASE.md` without exception:
1. `RoleName`: `ROLE_CO_OWNER`, `ROLE_STAFF`, `ROLE_ADMIN`
2. `VerificationStatus`: `PENDING`, `VERIFIED`, `REJECTED`
3. `DriverLicenseClass`: `B1`, `B2`, `C`, `D`, `E`
4. `VehicleStatus`: `AVAILABLE`, `BOOKED`, `IN_USE`, `CHARGING`, `MAINTENANCE`, `DAMAGED`, `UNAVAILABLE`
5. `ContractStatus`: `DRAFT`, `PENDING_SIGNATURE`, `SIGNED`, `ACTIVE`, `EXPIRED`, `TERMINATED`, `REJECTED`
6. `BookingStatus`: `PENDING`, `APPROVED`, `CONFIRMED`, `IN_USE`, `COMPLETED`, `CANCELLED`, `REJECTED`, `NO_SHOW`
7. `UsageSessionStatus`: `ACTIVE`, `COMPLETED`, `DISPUTED`
8. `InspectionType`: `CHECK_IN`, `CHECK_OUT`, `ROUTINE`
9. `ServiceType`: `MAINTENANCE`, `REPAIR`, `CHARGING`, `CLEANING`, `INSPECTION`
10. `ServiceStatus`: `PENDING`, `IN_PROGRESS`, `COMPLETED`, `CANCELLED`
11. `ExpenseCategory`: `CHARGING`, `PREVENTIVE_MAINTENANCE`, `EMERGENCY_REPAIR`, `INSURANCE`, `INSPECTION`, `CLEANING`
12. `AllocationStrategy`: `OWNERSHIP_BASED`, `USAGE_BASED`, `HYBRID`
13. `TransactionType`: `DEPOSIT`, `EXPENSE_PAYOUT`, `CAPITAL_CALL`, `INTEREST`, `REFUND`
14. `PaymentMethod`: `BANK_TRANSFER`, `E_WALLET`, `CREDIT_CARD`
15. `PaymentStatus`: `PENDING`, `COMPLETED`, `FAILED`, `REFUNDED`
16. `ProposalType`: `ROUTINE_EXPENSE`, `MAJOR_EXPENSE`, `OPERATIONAL_RULE_CHANGE`, `OWNER_ADMISSION_OR_EXIT`
17. `ProposalStatus`: `ACTIVE`, `PASSED`, `REJECTED`, `EXPIRED`
18. `VoteOptionKey`: `APPROVE`, `REJECT`, `ABSTAIN`
19. `DisputeStatus`: `OPEN`, `UNDER_REVIEW`, `RESOLVED`, `ESCALATED`
20. `NotificationCategory`: `BOOKING`, `PAYMENT_DUE`, `VOTE_CALL`, `DISPUTE`, `MAINTENANCE`

---

## 4. Precision, Scale & Constraint Verification

* **Money & Financial Balances**: Strictly mapped to `BigDecimal` with `@Column(precision = 15, scale = 2)` (`estimated_cost`, `cost_amount`, `current_balance`, `minimum_reserve_threshold`, `amount`, `balance_after`, `total_amount`, `allocated_amount`). Floating-point types (`float`, `double`) are **100% prohibited**.
* **Equity Percentages**: Mapped to `BigDecimal` with `@Column(precision = 5, scale = 2)` (`percentage`, `equity_weight`).
* **Telemetry & Distance**: Mapped to `BigDecimal` with `@Column(precision = 10, scale = 2)` (`odometer_km`, `start_odometer`, `end_odometer`, `odometer_at_service`).
* **JSON Defect Coordinates & Snapshots**: Mapped to `@Column(columnDefinition = "json") String` (`condition_mesh_flags`, `mesh_3d_defect_coordinates`, `suggested_actions`, `old_state_json`, `new_state_json`).
* **Table Indexes**:
  * `bookings`: `idx_vehicle_time (vehicle_id, start_time, end_time)`
  * `audit_logs`: `idx_audit_entity (entity_name, entity_id)`, `idx_audit_created (created_at)`
* **Unique Constraints**:
  * `ownership_shares`: `uk_share_group_user (group_id, user_id)`
  * `contract_signatures`: `uk_contract_user_signature (contract_id, user_id)`
  * `vote_options`: `uk_proposal_option_key (proposal_id, option_key)`
  * `votes`: `uk_proposal_user_vote (proposal_id, user_id)`

---

## 5. Build & Compilation Verification

### Build Command
```powershell
mvn clean compile
```

### Output
```
[INFO] --- clean:3.3.2:clean (default-clean) @ evshare-backend ---
[INFO] Deleting E:\EVShare3D\backend\target
[INFO] 
[INFO] --- resources:3.3.1:resources (default-resources) @ evshare-backend ---
[INFO] Copying 4 resources from src\main\resources to target\classes
[INFO] Copying 0 resource from src\main\resources to target\classes
[INFO] 
[INFO] --- compiler:3.11.0:compile (default-compile) @ evshare-backend ---
[INFO] Changes detected - recompiling the module! :source
[INFO] Compiling 65 source files with javac [debug release 17] to target\classes
[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
[INFO] ------------------------------------------------------------------------
[INFO] Total time:  3.528 s
[INFO] Finished at: 2026-09-06T19:26:08+07:00
[INFO] ------------------------------------------------------------------------
```

### Unit Test Execution
```powershell
mvn test
```
* **Summary**: `Tests run: 7, Failures: 0, Errors: 0, Skipped: 0` (Execution time: 3.973s).

---

## 6. Scope Boundary Confirmation

In strict compliance with `AGENTS.md` and Phase 02-E boundaries:
* **NO Business Methods added to entities**: Entities are pure POJOs with standard accessors.
* **NO Services or business logic implemented**: No booking state transitions, fairness formulas, equity transfer calculations, voting tally, or dispute resolution logic.
* **NO Artificial/Invented Fields**: 100% fidelity to `DATABASE.md`.
* **NO Frontend / Three.js code altered**.

---

## 7. Next Step

**Phase 02-E is complete.** Ready for **PHASE 02-F** (Authoring deterministic Flyway migrations V1–V7). Execution is paused pending explicit user approval.
