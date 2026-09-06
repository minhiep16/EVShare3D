# EVShare 3D – RELATIONAL DATABASE DESIGN SPECIFICATION

## 1. Database Standards & Conventions

* **RDBMS Engine**: MySQL 8.0+ with InnoDB storage engine.
* **Charset & Collation**: `utf8mb4` / `utf8mb4_unicode_ci`.
* **Naming Conventions**:
  * Tables: `snake_case` plural (e.g., `ownership_groups`, `vehicle_inspections`).
  * Columns: `snake_case` singular (e.g., `start_time`, `battery_level`).
  * Primary Keys: `id BIGINT AUTO_INCREMENT PRIMARY KEY`.
  * Foreign Keys: `<singular_table>_id BIGINT NOT NULL` referencing `<plural_table>(id)` with explicit referential integrity (`ON DELETE RESTRICT` or `CASCADE`).
  * Monetary values: `DECIMAL(15, 2)` (VND currency, strictly no floating-point).
  * Fractions & Percentages: `DECIMAL(5, 2)` (supports 0.00% to 100.00%).
  * Standard Audit Columns: `created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP`, `updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP`.

---

## 2. Enumeration Types (Enums)

| Enum Name | Target Table(s) / Column(s) | Allowed String Values |
| :--- | :--- | :--- |
| `RoleName` | `roles.name` | `'ROLE_CO_OWNER'`, `'ROLE_STAFF'`, `'ROLE_ADMIN'` |
| `VerificationStatus` | `identity_verifications.verification_status` | `'PENDING'`, `'VERIFIED'`, `'REJECTED'` |
| `DriverLicenseClass` | `driver_licenses.license_class` | `'B1'`, `'B2'`, `'C'`, `'D'`, `'E'` |
| `VehicleStatus` | `vehicles.status` | `'AVAILABLE'`, `'BOOKED'`, `'IN_USE'`, `'CHARGING'`, `'MAINTENANCE'`, `'DAMAGED'`, `'UNAVAILABLE'` |
| `ContractStatus` | `co_ownership_contracts.status` | `'DRAFT'`, `'PENDING_SIGNATURE'`, `'SIGNED'`, `'ACTIVE'`, `'EXPIRED'`, `'TERMINATED'`, `'REJECTED'` |
| `BookingStatus` | `bookings.status` | `'PENDING'`, `'APPROVED'`, `'CONFIRMED'`, `'IN_USE'`, `'COMPLETED'`, `'CANCELLED'`, `'REJECTED'`, `'NO_SHOW'` |
| `UsageSessionStatus` | `usage_sessions.status` | `'ACTIVE'`, `'COMPLETED'`, `'DISPUTED'` |
| `InspectionType` | `vehicle_inspections.inspection_type` | `'CHECK_IN'`, `'CHECK_OUT'`, `'ROUTINE'` |
| `ServiceType` | `vehicle_services.service_type` | `'MAINTENANCE'`, `'REPAIR'`, `'CHARGING'`, `'CLEANING'`, `'INSPECTION'` |
| `ServiceStatus` | `vehicle_services.service_status` | `'PENDING'`, `'IN_PROGRESS'`, `'COMPLETED'`, `'CANCELLED'` |
| `ExpenseCategory` | `expenses.category` | `'CHARGING'`, `'PREVENTIVE_MAINTENANCE'`, `'EMERGENCY_REPAIR'`, `'INSURANCE'`, `'INSPECTION'`, `'CLEANING'` |
| `AllocationStrategy` | `expenses.allocation_strategy` | `'OWNERSHIP_BASED'`, `'USAGE_BASED'`, `'HYBRID'` |
| `TransactionType` | `fund_transactions.transaction_type` | `'DEPOSIT'`, `'EXPENSE_PAYOUT'`, `'CAPITAL_CALL'`, `'INTEREST'`, `'REFUND'` |
| `PaymentMethod` | `payments.payment_method` | `'BANK_TRANSFER'`, `'E_WALLET'`, `'CREDIT_CARD'` |
| `PaymentStatus` | `payments.status` | `'PENDING'`, `'COMPLETED'`, `'FAILED'`, `'REFUNDED'` |
| `ProposalType` | `proposals.proposal_type` | `'ROUTINE_EXPENSE'`, `'MAJOR_EXPENSE'`, `'OPERATIONAL_RULE_CHANGE'`, `'OWNER_ADMISSION_OR_EXIT'` |
| `ProposalStatus` | `proposals.status` | `'ACTIVE'`, `'PASSED'`, `'REJECTED'`, `'EXPIRED'` |
| `VoteOptionKey` | `vote_options.option_key` | `'APPROVE'`, `'REJECT'`, `'ABSTAIN'` |
| `DisputeStatus` | `disputes.status` | `'OPEN'`, `'UNDER_REVIEW'`, `'RESOLVED'`, `'ESCALATED'` |
| `NotificationCategory` | `notifications.category` | `'BOOKING'`, `'PAYMENT_DUE'`, `'VOTE_CALL'`, `'DISPUTE'`, `'MAINTENANCE'` |

---

## 3. Comprehensive Table Schemas (27 Tables)

### 3.1. Identity, Users & Roles

#### Table: `users`
| Column | Data Type | Nullable | Default | Constraints | Description / Relationships |
| :--- | :--- | :---: | :--- | :--- | :--- |
| `id` | `BIGINT` | NO | AUTO_INCREMENT | `PK` | Unique user identity |
| `email` | `VARCHAR(150)` | NO | None | `UNIQUE` | User login email |
| `password_hash` | `VARCHAR(255)` | NO | None | None | BCrypt/Argon2 password hash |
| `full_name` | `VARCHAR(100)` | NO | None | None | Legal full name |
| `phone_number` | `VARCHAR(20)` | YES | NULL | `UNIQUE` | Contact mobile number |
| `avatar_3d_url` | `VARCHAR(255)` | YES | NULL | None | URI to personal 3D avatar GLB |
| `is_active` | `BOOLEAN` | NO | `TRUE` | None | Account state toggle |
| `created_at` | `TIMESTAMP` | NO | `CURRENT_TIMESTAMP` | None | Registration timestamp |
| `updated_at` | `TIMESTAMP` | NO | `CURRENT_TIMESTAMP` | `ON UPDATE` | Last modification timestamp |

#### Table: `roles`
| Column | Data Type | Nullable | Default | Constraints | Description / Relationships |
| :--- | :--- | :---: | :--- | :--- | :--- |
| `id` | `BIGINT` | NO | AUTO_INCREMENT | `PK` | Unique role ID |
| `name` | `VARCHAR(50)` | NO | None | `UNIQUE` | Role name (`ROLE_CO_OWNER`, `ROLE_STAFF`, `ROLE_ADMIN`) |

#### Table: `user_roles`
| Column | Data Type | Nullable | Default | Constraints | Description / Relationships |
| :--- | :--- | :---: | :--- | :--- | :--- |
| `user_id` | `BIGINT` | NO | None | `PK, FK -> users(id)` | User reference (`ON DELETE CASCADE`) |
| `role_id` | `BIGINT` | NO | None | `PK, FK -> roles(id)` | Role reference (`ON DELETE CASCADE`) |

#### Table: `identity_verifications`
| Column | Data Type | Nullable | Default | Constraints | Description / Relationships |
| :--- | :--- | :---: | :--- | :--- | :--- |
| `id` | `BIGINT` | NO | AUTO_INCREMENT | `PK` | Verification record ID |
| `user_id` | `BIGINT` | NO | None | `FK -> users(id)` | User reference (`ON DELETE RESTRICT`) |
| `id_card_number` | `VARCHAR(50)` | NO | None | `UNIQUE` | National ID / Citizen card number |
| `verification_status` | `VARCHAR(30)` | NO | `'PENDING'` | None | Status: `PENDING`, `VERIFIED`, `REJECTED` |
| `document_front_url` | `VARCHAR(255)` | NO | None | None | Cloud/local URI to front image |
| `document_back_url` | `VARCHAR(255)` | NO | None | None | Cloud/local URI to back image |
| `verified_by_user_id`| `BIGINT` | YES | NULL | `FK -> users(id)` | Staff/Admin reviewer (`ON DELETE SET NULL`) |
| `verified_at` | `TIMESTAMP` | YES | NULL | None | Timestamp of verification decision |

#### Table: `driver_licenses`
| Column | Data Type | Nullable | Default | Constraints | Description / Relationships |
| :--- | :--- | :---: | :--- | :--- | :--- |
| `id` | `BIGINT` | NO | AUTO_INCREMENT | `PK` | License record ID |
| `user_id` | `BIGINT` | NO | None | `UNIQUE, FK -> users(id)` | Single active license per user (`ON DELETE RESTRICT`) |
| `license_number` | `VARCHAR(50)` | NO | None | `UNIQUE` | Official driver license number |
| `license_class` | `VARCHAR(20)` | NO | `'B2'` | None | License class tier |
| `issue_date` | `DATE` | NO | None | None | Official date of issuance |
| `expiry_date` | `DATE` | NO | None | None | Expiration date |
| `is_verified` | `BOOLEAN` | NO | `FALSE` | None | Staff verification flag |
| `verified_at` | `TIMESTAMP` | YES | NULL | None | Timestamp of staff verification |

---

### 3.2. Vehicles, Ownership Groups & Equity Shares

#### Table: `vehicles`
| Column | Data Type | Nullable | Default | Constraints | Description / Relationships |
| :--- | :--- | :---: | :--- | :--- | :--- |
| `id` | `BIGINT` | NO | AUTO_INCREMENT | `PK` | Vehicle digital twin ID |
| `vin` | `VARCHAR(50)` | NO | None | `UNIQUE` | 17-character VIN identifier |
| `license_plate` | `VARCHAR(20)` | NO | None | `UNIQUE` | Registered vehicle license plate |
| `model_name` | `VARCHAR(100)` | NO | None | None | Model title (e.g. Tesla Model 3, VF 8) |
| `manufacturer` | `VARCHAR(50)` | NO | None | None | Manufacturer name |
| `model_3d_asset_path`| `VARCHAR(255)` | NO | None | None | Relative path to GLB digital twin model |
| `status` | `VARCHAR(30)` | NO | `'AVAILABLE'` | None | `AVAILABLE`, `BOOKED`, `IN_USE`, `CHARGING`, `MAINTENANCE`, `DAMAGED`, `UNAVAILABLE` |
| `battery_level` | `INT` | NO | `100` | `CHECK (0..100)` | Battery State of Charge (SoC %) |
| `odometer_km` | `DECIMAL(10, 2)`| NO | `0.00` | `CHECK (>= 0.00)`| Total vehicle distance logged |
| `stall_location_code`| `VARCHAR(30)` | NO | `'BAY-01'` | None | Parking bay / charging stall code in 3D Garage |
| `created_at` | `TIMESTAMP` | NO | `CURRENT_TIMESTAMP` | None | Creation timestamp |
| `updated_at` | `TIMESTAMP` | NO | `CURRENT_TIMESTAMP` | `ON UPDATE` | Last telemetry update timestamp |

#### Table: `ownership_groups`
| Column | Data Type | Nullable | Default | Constraints | Description / Relationships |
| :--- | :--- | :---: | :--- | :--- | :--- |
| `id` | `BIGINT` | NO | AUTO_INCREMENT | `PK` | Group identifier |
| `group_name` | `VARCHAR(100)` | NO | None | None | Group / Syndicate title |
| `vehicle_id` | `BIGINT` | NO | None | `UNIQUE, FK -> vehicles(id)`| One vehicle per ownership group (`ON DELETE RESTRICT`) |
| `formation_date` | `DATE` | NO | None | None | Group legal establishment date |
| `is_active` | `BOOLEAN` | NO | `TRUE` | None | Active group flag |

#### Table: `ownership_shares`
| Column | Data Type | Nullable | Default | Constraints | Description / Relationships |
| :--- | :--- | :---: | :--- | :--- | :--- |
| `id` | `BIGINT` | NO | AUTO_INCREMENT | `PK` | Share certificate record ID |
| `group_id` | `BIGINT` | NO | None | `FK -> ownership_groups(id)`| Syndicate group reference (`ON DELETE RESTRICT`) |
| `user_id` | `BIGINT` | NO | None | `FK -> users(id)` | Co-owner reference (`ON DELETE RESTRICT`) |
| `percentage` | `DECIMAL(5, 2)` | NO | None | `CHECK (0.01..100.00)`| Equity share percentage (e.g. 40.00) |
| `share_certificate_number`| `VARCHAR(100)`| NO | None | `UNIQUE` | Legal share certificate reference ID |
| `acquired_at` | `TIMESTAMP` | NO | `CURRENT_TIMESTAMP`| None | Acquisition / issuance timestamp |
| `is_active` | `BOOLEAN` | NO | `TRUE` | None | Active equity flag |
| *Composite Unique* | - | - | - | `UNIQUE (group_id, user_id)`| One active share per user per group |

---

### 3.3. Digital Co-Ownership Contracts & Signatures

#### Table: `co_ownership_contracts`
| Column | Data Type | Nullable | Default | Constraints | Description / Relationships |
| :--- | :--- | :---: | :--- | :--- | :--- |
| `id` | `BIGINT` | NO | AUTO_INCREMENT | `PK` | Contract record ID |
| `group_id` | `BIGINT` | NO | None | `FK -> ownership_groups(id)`| Bound syndicate (`ON DELETE RESTRICT`) |
| `contract_title` | `VARCHAR(150)` | NO | None | None | Agreement title |
| `contract_terms_text`| `LONGTEXT` | NO | None | None | Full legal text in Markdown format |
| `version` | `INT` | NO | `1` | None | Contract version integer |
| `status` | `VARCHAR(30)` | NO | `'DRAFT'` | None | `DRAFT`, `PENDING_SIGNATURE`, `SIGNED`, `ACTIVE`, `EXPIRED`, `TERMINATED` |
| `effective_date` | `DATE` | YES | NULL | None | Legal activation date |
| `expiry_date` | `DATE` | YES | NULL | None | Expiration date |
| `created_at` | `TIMESTAMP` | NO | `CURRENT_TIMESTAMP` | None | Draft creation timestamp |

#### Table: `contract_signatures`
| Column | Data Type | Nullable | Default | Constraints | Description / Relationships |
| :--- | :--- | :---: | :--- | :--- | :--- |
| `id` | `BIGINT` | NO | AUTO_INCREMENT | `PK` | Signature record ID |
| `contract_id` | `BIGINT` | NO | None | `FK -> co_ownership_contracts(id)`| Contract reference (`ON DELETE CASCADE`) |
| `user_id` | `BIGINT` | NO | None | `FK -> users(id)` | Signatory reference (`ON DELETE RESTRICT`) |
| `signature_hash` | `VARCHAR(255)` | NO | None | None | SHA-256 cryptographic digest of terms & signature |
| `signed_at` | `TIMESTAMP` | NO | `CURRENT_TIMESTAMP`| None | Timestamp of digital signing |
| `ip_address` | `VARCHAR(45)` | NO | None | None | Client IP address at signing |
| *Composite Unique* | - | - | - | `UNIQUE (contract_id, user_id)`| One signature per user per contract version |

---

### 3.4. Bookings, Usage Sessions, Inspections & Workshop Services

#### Table: `bookings`
| Column | Data Type | Nullable | Default | Constraints | Description / Relationships |
| :--- | :--- | :---: | :--- | :--- | :--- |
| `id` | `BIGINT` | NO | AUTO_INCREMENT | `PK` | Booking reservation ID |
| `vehicle_id` | `BIGINT` | NO | None | `FK -> vehicles(id)` | Target electric vehicle (`ON DELETE RESTRICT`) |
| `user_id` | `BIGINT` | NO | None | `FK -> users(id)` | Co-owner reserver (`ON DELETE RESTRICT`) |
| `start_time` | `TIMESTAMP` | NO | None | None | Reservation commencement timestamp |
| `end_time` | `TIMESTAMP` | NO | None | None | Reservation conclusion timestamp |
| `status` | `VARCHAR(30)` | NO | `'CONFIRMED'` | None | `PENDING`, `APPROVED`, `CONFIRMED`, `IN_USE`, `COMPLETED`, `CANCELLED`, `REJECTED`, `NO_SHOW` |
| `estimated_cost` | `DECIMAL(15, 2)`| NO | `0.00` | None | Estimated booking allocation cost in VND |
| `created_at` | `TIMESTAMP` | NO | `CURRENT_TIMESTAMP` | None | Booking request timestamp |
| *Composite Index* | - | - | - | `INDEX (vehicle_id, start_time, end_time)`| Critical for zero-latency conflict queries |

#### Table: `usage_sessions`
| Column | Data Type | Nullable | Default | Constraints | Description / Relationships |
| :--- | :--- | :---: | :--- | :--- | :--- |
| `id` | `BIGINT` | NO | AUTO_INCREMENT | `PK` | Usage trip session ID |
| `booking_id` | `BIGINT` | NO | None | `UNIQUE, FK -> bookings(id)`| One active session per booking (`ON DELETE RESTRICT`) |
| `start_odometer` | `DECIMAL(10, 2)`| NO | None | None | Starting odometer reading at check-in |
| `end_odometer` | `DECIMAL(10, 2)`| YES | NULL | None | Concluding odometer reading at check-out |
| `start_battery` | `INT` | NO | None | `CHECK (0..100)` | Starting battery SoC % at check-in |
| `end_battery` | `INT` | YES | NULL | `CHECK (0..100)` | Final battery SoC % at check-out |
| `check_in_time` | `TIMESTAMP` | NO | `CURRENT_TIMESTAMP`| None | Actual check-in timestamp |
| `check_out_time`| `TIMESTAMP` | YES | NULL | None | Actual check-out timestamp |
| `status` | `VARCHAR(30)` | NO | `'ACTIVE'` | None | `ACTIVE`, `COMPLETED`, `DISPUTED` |

#### Table: `vehicle_inspections`
| Column | Data Type | Nullable | Default | Constraints | Description / Relationships |
| :--- | :--- | :---: | :--- | :--- | :--- |
| `id` | `BIGINT` | NO | AUTO_INCREMENT | `PK` | Physical inspection ID |
| `usage_session_id` | `BIGINT` | NO | None | `FK -> usage_sessions(id)`| Linked trip session (`ON DELETE CASCADE`) |
| `inspector_user_id`| `BIGINT` | NO | None | `FK -> users(id)` | Technician/Owner inspector (`ON DELETE RESTRICT`) |
| `inspection_type` | `VARCHAR(20)` | NO | None | None | `CHECK_IN`, `CHECK_OUT`, `ROUTINE` |
| `condition_mesh_flags`| `JSON` | YES | NULL | None | 3D vertex / zone IDs with physical defects |
| `notes` | `TEXT` | YES | NULL | None | Textual condition report |
| `created_at` | `TIMESTAMP` | NO | `CURRENT_TIMESTAMP`| None | Inspection timestamp |

#### Table: `vehicle_services`
| Column | Data Type | Nullable | Default | Constraints | Description / Relationships |
| :--- | :--- | :---: | :--- | :--- | :--- |
| `id` | `BIGINT` | NO | AUTO_INCREMENT | `PK` | Service task ID |
| `vehicle_id` | `BIGINT` | NO | None | `FK -> vehicles(id)` | Vehicle serviced (`ON DELETE RESTRICT`) |
| `service_type` | `VARCHAR(40)` | NO | None | None | `MAINTENANCE`, `REPAIR`, `CHARGING`, `CLEANING`, `INSPECTION` |
| `description` | `TEXT` | NO | None | None | Work scope / technician notes |
| `technician_user_id`| `BIGINT` | NO | None | `FK -> users(id)` | Performing staff member (`ON DELETE RESTRICT`) |
| `cost_amount` | `DECIMAL(15, 2)`| NO | `0.00` | None | Total service cost in VND |
| `odometer_at_service`| `DECIMAL(10, 2)`| NO | None | None | Odometer reading when service commenced |
| `service_status` | `VARCHAR(30)` | NO | `'PENDING'` | None | `PENDING`, `IN_PROGRESS`, `COMPLETED`, `CANCELLED` |
| `started_at` | `TIMESTAMP` | YES | NULL | None | Service start timestamp |
| `completed_at` | `TIMESTAMP` | YES | NULL | None | Service completion timestamp |

---

### 3.5. Finance, Shared Funds & Payments

#### Table: `shared_funds`
| Column | Data Type | Nullable | Default | Constraints | Description / Relationships |
| :--- | :--- | :---: | :--- | :--- | :--- |
| `id` | `BIGINT` | NO | AUTO_INCREMENT | `PK` | 3D Vault shared fund ID |
| `group_id` | `BIGINT` | NO | None | `UNIQUE, FK -> ownership_groups(id)`| One vault per group (`ON DELETE RESTRICT`) |
| `current_balance` | `DECIMAL(15, 2)`| NO | `0.00` | None | Current liquid balance in VND |
| `minimum_reserve_threshold`| `DECIMAL(15, 2)`| NO | `10000000.00` | None | Reserve safety limit (triggers capital calls) |
| `currency` | `VARCHAR(10)` | NO | `'VND'` | None | Currency identifier |
| `updated_at` | `TIMESTAMP` | NO | `CURRENT_TIMESTAMP`| `ON UPDATE` | Last balance modification timestamp |

#### Table: `fund_transactions`
| Column | Data Type | Nullable | Default | Constraints | Description / Relationships |
| :--- | :--- | :---: | :--- | :--- | :--- |
| `id` | `BIGINT` | NO | AUTO_INCREMENT | `PK` | Ledger transaction ID |
| `fund_id` | `BIGINT` | NO | None | `FK -> shared_funds(id)`| Target vault (`ON DELETE RESTRICT`) |
| `user_id` | `BIGINT` | YES | NULL | `FK -> users(id)` | Initiating/contributing user (`ON DELETE SET NULL`) |
| `transaction_type`| `VARCHAR(30)` | NO | None | None | `DEPOSIT`, `EXPENSE_PAYOUT`, `CAPITAL_CALL`, `INTEREST`, `REFUND` |
| `amount` | `DECIMAL(15, 2)`| NO | None | None | Transaction amount (+ or -) |
| `balance_after` | `DECIMAL(15, 2)`| NO | None | None | Resulting vault balance after mutation |
| `description` | `VARCHAR(255)` | NO | None | None | Ledger narrative / expense reference |
| `created_at` | `TIMESTAMP` | NO | `CURRENT_TIMESTAMP`| None | Transaction execution timestamp |

#### Table: `expenses`
| Column | Data Type | Nullable | Default | Constraints | Description / Relationships |
| :--- | :--- | :---: | :--- | :--- | :--- |
| `id` | `BIGINT` | NO | AUTO_INCREMENT | `PK` | Operating expense ID |
| `group_id` | `BIGINT` | NO | None | `FK -> ownership_groups(id)`| Target syndicate (`ON DELETE RESTRICT`) |
| `title` | `VARCHAR(150)` | NO | None | None | Expense description / invoice title |
| `category` | `VARCHAR(40)` | NO | None | None | `CHARGING`, `PREVENTIVE_MAINTENANCE`, `EMERGENCY_REPAIR`, `INSURANCE`, `INSPECTION`, `CLEANING` |
| `total_amount` | `DECIMAL(15, 2)`| NO | None | `CHECK (> 0.00)` | Total invoice cost |
| `allocation_strategy`| `VARCHAR(30)` | NO | `'OWNERSHIP_BASED'`| None | `OWNERSHIP_BASED`, `USAGE_BASED`, `HYBRID` |
| `invoice_reference` | `VARCHAR(100)`| YES | NULL | None | External vendor receipt/invoice reference |
| `logged_by_user_id`| `BIGINT` | NO | None | `FK -> users(id)` | User who recorded the bill (`ON DELETE RESTRICT`) |
| `incurred_date` | `DATE` | NO | None | None | Date expense was incurred |
| `created_at` | `TIMESTAMP` | NO | `CURRENT_TIMESTAMP`| None | System entry timestamp |

#### Table: `expense_allocations`
| Column | Data Type | Nullable | Default | Constraints | Description / Relationships |
| :--- | :--- | :---: | :--- | :--- | :--- |
| `id` | `BIGINT` | NO | AUTO_INCREMENT | `PK` | Share allocation ID |
| `expense_id` | `BIGINT` | NO | None | `FK -> expenses(id)` | Parent expense bill (`ON DELETE CASCADE`) |
| `user_id` | `BIGINT` | NO | None | `FK -> users(id)` | Debited co-owner (`ON DELETE RESTRICT`) |
| `allocated_amount` | `DECIMAL(15, 2)`| NO | None | `CHECK (>= 0.00)`| Individual amount owed |
| `is_settled` | `BOOLEAN` | NO | `FALSE` | None | Settlement status |
| `settled_at` | `TIMESTAMP` | YES | NULL | None | Settlement timestamp |

#### Table: `payments`
| Column | Data Type | Nullable | Default | Constraints | Description / Relationships |
| :--- | :--- | :---: | :--- | :--- | :--- |
| `id` | `BIGINT` | NO | AUTO_INCREMENT | `PK` | Payment record ID |
| `user_id` | `BIGINT` | NO | None | `FK -> users(id)` | Payer reference (`ON DELETE RESTRICT`) |
| `fund_id` | `BIGINT` | NO | None | `FK -> shared_funds(id)`| Credited vault (`ON DELETE RESTRICT`) |
| `expense_allocation_id`| `BIGINT` | YES | NULL | `FK -> expense_allocations(id)`| Linked expense offset (`ON DELETE SET NULL`) |
| `amount` | `DECIMAL(15, 2)`| NO | None | `CHECK (> 0.00)` | Settled amount |
| `payment_method` | `VARCHAR(30)` | NO | None | None | `BANK_TRANSFER`, `E_WALLET`, `CREDIT_CARD` |
| `transaction_reference`| `VARCHAR(100)`| NO | None | `UNIQUE` | Unique external banking/gateway reference |
| `status` | `VARCHAR(30)` | NO | `'PENDING'` | None | `PENDING`, `COMPLETED`, `FAILED`, `REFUNDED` |
| `created_at` | `TIMESTAMP` | NO | `CURRENT_TIMESTAMP`| None | Payment initiation timestamp |

---

### 3.6. Governance, Voting & Disputes

#### Table: `proposals`
| Column | Data Type | Nullable | Default | Constraints | Description / Relationships |
| :--- | :--- | :---: | :--- | :--- | :--- |
| `id` | `BIGINT` | NO | AUTO_INCREMENT | `PK` | Decision proposal ID |
| `group_id` | `BIGINT` | NO | None | `FK -> ownership_groups(id)`| Syndicate reference (`ON DELETE RESTRICT`) |
| `proposer_user_id` | `BIGINT` | NO | None | `FK -> users(id)` | Proposing co-owner (`ON DELETE RESTRICT`) |
| `title` | `VARCHAR(150)` | NO | None | None | Proposal title |
| `description` | `TEXT` | NO | None | None | Detailed proposal narrative |
| `proposal_type` | `VARCHAR(40)` | NO | None | None | `ROUTINE_EXPENSE`, `MAJOR_EXPENSE`, `OPERATIONAL_RULE_CHANGE`, `OWNER_ADMISSION_OR_EXIT` |
| `voting_deadline` | `TIMESTAMP` | NO | None | None | Voting closure timestamp |
| `status` | `VARCHAR(30)` | NO | `'ACTIVE'` | None | `ACTIVE`, `PASSED`, `REJECTED`, `EXPIRED` |
| `created_at` | `TIMESTAMP` | NO | `CURRENT_TIMESTAMP`| None | Proposal creation timestamp |

#### Table: `vote_options`
| Column | Data Type | Nullable | Default | Constraints | Description / Relationships |
| :--- | :--- | :---: | :--- | :--- | :--- |
| `id` | `BIGINT` | NO | AUTO_INCREMENT | `PK` | Option ID |
| `proposal_id` | `BIGINT` | NO | None | `FK -> proposals(id)` | Proposal reference (`ON DELETE CASCADE`) |
| `option_key` | `VARCHAR(30)` | NO | None | None | `APPROVE`, `REJECT`, `ABSTAIN` |
| `label` | `VARCHAR(100)` | NO | None | None | Visual button label |
| *Composite Unique* | - | - | - | `UNIQUE (proposal_id, option_key)`| No duplicate options per proposal |

#### Table: `votes`
| Column | Data Type | Nullable | Default | Constraints | Description / Relationships |
| :--- | :--- | :---: | :--- | :--- | :--- |
| `id` | `BIGINT` | NO | AUTO_INCREMENT | `PK` | Vote ballot ID |
| `proposal_id` | `BIGINT` | NO | None | `FK -> proposals(id)` | Proposal reference (`ON DELETE CASCADE`) |
| `user_id` | `BIGINT` | NO | None | `FK -> users(id)` | Voter reference (`ON DELETE RESTRICT`) |
| `vote_option_id` | `BIGINT` | NO | None | `FK -> vote_options(id)`| Selected choice (`ON DELETE RESTRICT`) |
| `equity_weight` | `DECIMAL(5, 2)` | NO | None | `CHECK (0.01..100.00)`| Voter's equity % at vote time |
| `voted_at` | `TIMESTAMP` | NO | `CURRENT_TIMESTAMP`| None | Ballot casting timestamp |
| *Composite Unique* | - | - | - | `UNIQUE (proposal_id, user_id)`| Strictly one vote per member per proposal |

#### Table: `disputes`
| Column | Data Type | Nullable | Default | Constraints | Description / Relationships |
| :--- | :--- | :---: | :--- | :--- | :--- |
| `id` | `BIGINT` | NO | AUTO_INCREMENT | `PK` | Dispute dossier ID |
| `group_id` | `BIGINT` | NO | None | `FK -> ownership_groups(id)`| Group reference (`ON DELETE RESTRICT`) |
| `usage_session_id` | `BIGINT` | YES | NULL | `FK -> usage_sessions(id)`| Contested trip session (`ON DELETE SET NULL`) |
| `complainant_user_id`| `BIGINT` | NO | None | `FK -> users(id)` | Initiating party (`ON DELETE RESTRICT`) |
| `respondent_user_id` | `BIGINT` | YES | NULL | `FK -> users(id)` | Responding party (`ON DELETE SET NULL`) |
| `title` | `VARCHAR(150)` | NO | None | None | Dispute claim headline |
| `description` | `TEXT` | NO | None | None | Full statement of facts |
| `status` | `VARCHAR(30)` | NO | `'OPEN'` | None | `OPEN`, `UNDER_REVIEW`, `RESOLVED`, `ESCALATED` |
| `resolution_summary` | `TEXT` | YES | NULL | None | Final arbitration verdict & adjustments |
| `created_at` | `TIMESTAMP` | NO | `CURRENT_TIMESTAMP`| None | Filing timestamp |

#### Table: `dispute_evidences`
| Column | Data Type | Nullable | Default | Constraints | Description / Relationships |
| :--- | :--- | :---: | :--- | :--- | :--- |
| `id` | `BIGINT` | NO | AUTO_INCREMENT | `PK` | Evidence item ID |
| `dispute_id` | `BIGINT` | NO | None | `FK -> disputes(id)` | Linked dispute case (`ON DELETE CASCADE`) |
| `uploaded_by_user_id`| `BIGINT` | NO | None | `FK -> users(id)` | Uploader reference (`ON DELETE RESTRICT`) |
| `file_url` | `VARCHAR(255)` | NO | None | None | Document or image URI |
| `mesh_3d_defect_coordinates`| `JSON` | YES | NULL | None | 3D vertex coordinates of vehicle damage |
| `description` | `VARCHAR(255)` | YES | NULL | None | Evidence description |
| `created_at` | `TIMESTAMP` | NO | `CURRENT_TIMESTAMP`| None | Upload timestamp |

---

### 3.7. Notifications, AI Recommendations & System Audit

#### Table: `notifications`
| Column | Data Type | Nullable | Default | Constraints | Description / Relationships |
| :--- | :--- | :---: | :--- | :--- | :--- |
| `id` | `BIGINT` | NO | AUTO_INCREMENT | `PK` | Notification message ID |
| `user_id` | `BIGINT` | NO | None | `FK -> users(id)` | Recipient reference (`ON DELETE CASCADE`) |
| `title` | `VARCHAR(150)` | NO | None | None | Notification title |
| `message` | `TEXT` | NO | None | None | Notification body text |
| `category` | `VARCHAR(40)` | NO | None | None | `BOOKING`, `PAYMENT_DUE`, `VOTE_CALL`, `DISPUTE`, `MAINTENANCE` |
| `spatial_sector_code`| `VARCHAR(30)` | YES | NULL | None | Target 3D sector code (e.g. `FINANCE`, `GARAGE`) |
| `is_read` | `BOOLEAN` | NO | `FALSE` | None | Read flag |
| `created_at` | `TIMESTAMP` | NO | `CURRENT_TIMESTAMP`| None | Delivery timestamp |

#### Table: `ai_recommendations`
| Column | Data Type | Nullable | Default | Constraints | Description / Relationships |
| :--- | :--- | :---: | :--- | :--- | :--- |
| `id` | `BIGINT` | NO | AUTO_INCREMENT | `PK` | AI advisory card ID |
| `group_id` | `BIGINT` | NO | None | `FK -> ownership_groups(id)`| Syndicate reference (`ON DELETE CASCADE`) |
| `target_user_id` | `BIGINT` | YES | NULL | `FK -> users(id)` | Target co-owner (`ON DELETE SET NULL`) |
| `recommendation_type`| `VARCHAR(50)` | NO | None | None | `FAIR_USAGE_IMBALANCE`, `BATTERY_HEALTH_WARNING`, `COST_OPTIMIZATION` |
| `message` | `TEXT` | NO | None | None | Advisory text |
| `suggested_actions` | `JSON` | YES | NULL | None | Structured actionable proposals (JSON format) |
| `is_acknowledged` | `BOOLEAN` | NO | `FALSE` | None | Acknowledged/dismissed flag |
| `created_at` | `TIMESTAMP` | NO | `CURRENT_TIMESTAMP`| None | Advisory timestamp |

#### Table: `audit_logs`
| Column | Data Type | Nullable | Default | Constraints | Description / Relationships |
| :--- | :--- | :---: | :--- | :--- | :--- |
| `id` | `BIGINT` | NO | AUTO_INCREMENT | `PK` | Immutable audit log record ID |
| `user_id` | `BIGINT` | YES | NULL | `FK -> users(id)` | Acting principal (`ON DELETE SET NULL`) |
| `action` | `VARCHAR(100)` | NO | None | None | Business action performed |
| `entity_name` | `VARCHAR(50)` | NO | None | None | Target entity class name |
| `entity_id` | `BIGINT` | NO | None | None | Target record ID |
| `old_state_json` | `JSON` | YES | NULL | None | Pre-mutation entity snapshot |
| `new_state_json` | `JSON` | YES | NULL | None | Post-mutation entity snapshot |
| `ip_address` | `VARCHAR(45)` | YES | NULL | None | Client IP address |
| `created_at` | `TIMESTAMP` | NO | `CURRENT_TIMESTAMP`| None | Log timestamp |
| *Indexes* | - | - | - | `INDEX (entity_name, entity_id)`<br>`INDEX (created_at)` | Fast audit trail retrieval |

---

## 4. Audit Requirements & Strategy

* **JPA Entity Auditing**: Spring Data JPA `@EntityListeners(AuditingEntityListener.class)` automatically sets `createdAt` (`@CreatedDate`) and `updatedAt` (`@LastModifiedDate`).
* **Financial & Governance Immutability**:
  * `fund_transactions`, `contract_signatures`, `votes`, and `audit_logs` are **append-only tables**. No `UPDATE` or `DELETE` operations are permitted.
  * In the event of an erroneous transaction or refund, a compensating transaction row must be inserted.
* **Audit Interceptor**: Key mutation events in Phase 08 will serialize old and new entity states into `old_state_json` and `new_state_json` for comprehensive change provenance.
