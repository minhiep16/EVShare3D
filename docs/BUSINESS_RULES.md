# EVShare 3D – COMPREHENSIVE BUSINESS RULES SPECIFICATION

## 1. Ownership & Group Equity Rules

### BR-OWN-01: Absolute 100% Equity Invariant
* Every vehicle is bound to exactly one `OwnershipGroup`.
* The mathematical sum of all active `OwnershipShare.percentage` records within any group must equal exactly **100.00%** at all times:
  $$\sum_{i=1}^{N} \text{percentage}_i = 100.00\%$$
* Any transaction modifying share allocations (e.g., admitting a new owner, transferring equity, or buying out a member) must be executed within an ACID transaction that verifies this sum to 2 decimal places. If the sum $\neq 100.00\%$, the database transaction must automatically roll back with an `InvalidOwnershipDistributionException`.

### BR-OWN-02: Equity Bounds per Co-Owner
* Minimum permissible ownership stake: **5.00%**.
* Maximum permissible ownership stake: **80.00%** (to prevent monopolization in a co-ownership model).
* An ownership group must have a minimum of **2** and a maximum of **10** co-owners per vehicle.

### BR-OWN-03: Equity Transfer & Rebalancing Protocol
* An owner wishing to sell or reduce their share must first offer right-of-first-refusal to existing group members via a 3D Decision Proposal.
* If existing members decline within 14 calendar days, the share may be offered to external vetted buyers.
* All transfers require 100% digital signature compliance in the Digital Contract Room.

---

## 2. Vehicle Reservation & Booking Rules

### BR-BKG-01: Time Granularity & Booking Constraints
* Minimum booking duration: **30 minutes**.
* Maximum continuous booking duration: **72 hours** (unless approved by special group vote).
* Maximum advance reservation window: **30 days** in advance.
* Buffer period between consecutive bookings: A mandatory **30-minute turnaround buffer** is automatically enforced between consecutive bookings to accommodate cleaning, parking verification, and initial charging.

### BR-BKG-02: Concurrency & Overlap Prevention
* Two bookings for the same vehicle can **never** overlap:
  $$\text{Interval}_A = [start_A, end_A + buffer]$$
  $$\text{Interval}_B = [start_B, end_B + buffer]$$
  $$\text{Interval}_A \cap \text{Interval}_B = \emptyset$$
* Booking requests must be validated using pessimistic locking (`SELECT ... FOR UPDATE`) or serializable transaction isolation on the vehicle availability schedule to guarantee zero double-booking under race conditions.

### BR-BKG-03: Cancellation & No-Show Penalties
* **Free Cancellation**: Up to **12 hours** prior to reservation start time.
* **Late Cancellation** (within 12 hours of start): Incurs a 20% reservation fee deduction credited to the Shared Fund and logs a penalty mark against the co-owner's fair usage profile.
* **No-Show Rule**: If check-in is not executed within **30 minutes** of the scheduled start time, the reservation status transitions to `NO_SHOW`, the vehicle becomes available for emergency bookings, and the user is assessed a 100% time slot penalty.

---

## 3. Fair Usage Engine & Allocation Formula

### BR-FAIR-01: Fair Usage Quota & Metrics
The platform ensures that vehicle access is strictly equitable according to each member's equity share.
* **Equity Quota ($Q_i$)**: The target allocation of vehicle resources for Co-Owner $i$ over an evaluation window (e.g., monthly 720 hours):
  $$Q_i = \text{percentage}_i \times \text{AvailableOperatingHours}$$
* **Weighted Usage Units ($W_i$)**: Actual consumption weighted by time-of-day demand:
  $$W_i = \sum_{session} \left( \text{DurationHours} \times \text{DemandMultiplier} \right)$$
  * **Peak Demand (Fri 16:00 – Sun 22:00, Public Holidays)**: Multiplier = **1.5x**
  * **Standard Demand (Mon–Fri 07:00 – 16:00)**: Multiplier = **1.0x**
  * **Off-Peak Demand (Daily 22:00 – 07:00)**: Multiplier = **0.7x**

### BR-FAIR-02: Fairness Ratio ($FR_i$) & Imbalance Classification
* For each co-owner, the normalized fairness ratio is computed:
  $$FR_i = \frac{W_i / \sum_{k=1}^N W_k}{\text{percentage}_i / 100}$$
* **Classification Tiers**:
  * **$0.90 \le FR_i \le 1.10$**: `FAIR` (Green 3D Aura)
  * **$0.75 \le FR_i < 0.90$** or **$1.10 < FR_i \le 1.25$**: `SLIGHTLY_IMBALANCED` (Yellow 3D Aura)
  * **$0.50 \le FR_i < 0.75$** or **$1.25 < FR_i \le 1.50$**: `IMBALANCED` (Orange 3D Aura)
  * **$FR_i < 0.50$** or **$FR_i > 1.50$**: `SEVERELY_IMBALANCED` (Red 3D Aura)

### BR-FAIR-03: Dynamic Booking Priority Resolution
* During peak hours or competing booking submissions, co-owners with $FR_i < 1.0$ (under-users) receive priority scheduling rights and an early-booking window advantage (up to 48 hours prior to standard release).
* Co-owners with $FR_i > 1.30$ (over-users) are restricted to off-peak slots unless all other co-owners have explicitly forfeited interest.

---

## 4. Operational Check-In & Check-Out Rules

### BR-OPS-01: Cryptographic QR Check-In Protocol
* 15 minutes before booking start, the backend generates a signed, time-bounded QR token (valid for 5 minutes).
* **Check-In Time Window**: Check-in is eligible strictly within $[startTime - 15\text{m}, startTime + 30\text{m}]$.
  * If check-in is attempted earlier than 15 minutes before scheduled start, access is rejected with `InvalidQrException`.
  * If check-in is attempted $>30$ minutes after scheduled start, the reservation is automatically marked `NO_SHOW` in the database and access is rejected.
* To check-in:
  1. Co-owner approaches the 3D Check-In Station or scans via mobile app.
  2. Token is cryptographically verified (HMAC-SHA256) and cross-validated against live database records.
  3. Co-owner enters starting odometer and battery State of Charge (SoC).
  4. Co-owner inspects the 3D vehicle avatar to confirm pre-existing body conditions.
  5. Upon confirmation, vehicle state transitions: `BOOKED` $\to$ `IN_USE` (or `AVAILABLE` $\to$ `BOOKED` $\to$ `IN_USE`).

### BR-OPS-02: Check-Out & Return Requirements
* Upon trip completion:
  1. Co-owner parks vehicle in the designated garage stall.
  2. Telemetry logged: final odometer, final battery SoC, return timestamp.
  3. **Minimum Battery Rule**: Vehicle must be returned with $\ge \mathbf{20\%}$ SoC unless plugged into an active charging station. Returning with $<20\%$ SoC without plugging in assesses an automatic charging service surcharge of 150,000 VND credited to the Shared Fund.
  4. **Late Return Rule**: If returned $>15$ minutes past scheduled end time, a late fee of 50,000 VND per 30-minute block is assessed.
  5. **Physical Inspection & Damage Rule**: Any new scratch, dent, or interior flaw must be marked on the 3D vehicle model and photographed.
     * If damage is flagged (`hasDamage == true`), the vehicle transitions `IN_USE` $\to$ `DAMAGED`, immediately blocking future reservations until inspected and serviced via `DAMAGED` $\to$ `MAINTENANCE` $\to$ `AVAILABLE`.
     * If plugged into a charging stall, vehicle transitions `IN_USE` $\to$ `CHARGING`.
     * Otherwise, vehicle safely transitions `IN_USE` $\to$ `AVAILABLE`.
  6. Historical usage session is sealed transactionally and becomes permanently immutable (`HistoricalUsageImmutableException` thrown on any rewrite attempt).

---

## 5. Cost Management & Expense Allocation Rules

### BR-FIN-01: Cost Classification
Every vehicle expenditure belongs to one of two categories:
1. **Fixed Overhead Expenses**:
   * Annual insurance, road tax, scheduled regulatory inspection, garage parking rent, connectivity subscriptions.
2. **Variable Operating Expenses**:
   * DC fast charging / AC charging power bills, tires, brake pads, wiper fluids, detailing / car wash, unexpected minor repairs.

### BR-FIN-02: Cost Allocation Strategies & Rounding Invariant
The backend `CostAllocationService` implements three mathematical allocation models:
1. **`OWNERSHIP_BASED`**:
   $$\text{OwedAmount}_i = \text{TotalExpense} \times \left( \frac{\text{percentage}_i}{100.00} \right)$$
   *Mandatory for all Fixed Overhead Expenses (insurance, taxes, inspection).*
2. **`USAGE_BASED`**:
   $$\text{OwedAmount}_i = \text{TotalExpense} \times \left( \frac{\text{DistanceLogged}_i}{\sum \text{DistanceLogged}} \right) \quad \text{or} \quad \left( \frac{\text{HoursUsed}_i}{\sum \text{HoursUsed}} \right)$$
   *Mandatory for variable charging and tire/brake consumables. If zero utilization is recorded, gracefully falls back to active ownership equity percentages.*
3. **`HYBRID`**:
   $$\text{FixedPart} = \text{round}(\text{TotalExpense} \times 0.30, 2), \quad \text{VariablePart} = \text{TotalExpense} - \text{FixedPart}$$
   $$\text{OwedAmount}_i = \text{round}\left(\text{FixedPart} \times \frac{\text{percentage}_i}{100.00}, 2\right) + \text{round}\left(\text{VariablePart} \times \frac{\text{DistanceLogged}_i}{\sum \text{DistanceLogged}}, 2\right)$$
   *Fixed base (exactly 30.00%) split by Ownership equity %, variable remainder (exactly 70.00%) split by proportional usage telemetry.*

**Rounding & Penny Absorption Invariant**:
* All intermediate calculations enforce Banker's Rounding (`RoundingMode.HALF_EVEN`, scale 2).
* Strict Invariant: $\sum_{i=1}^N \text{AllocatedAmount}_i \equiv \text{TotalExpense}$ exact to 0.01 VND.
* Residual penny discrepancy ($\Delta = \text{TotalExpense} - \sum \text{AllocatedAmount}_i$) is deterministically absorbed by the co-owner with the highest allocated share (tie-breaker: lowest `userId`). Zero pseudo-randomness.

### BR-FIN-03: Shared Fund Vault & Minimum Liquidity Threshold
* Each Ownership Group maintains a dedicated `SharedFund` 3D Vault account.
* **Minimum Reserve Threshold**: 10,000,000 VND per vehicle.
* If the fund balance falls below this threshold:
  * The 3D Vault enters `LOW_LIQUIDITY` warning mode (amber pulsing illumination).
  * Automated capital call notices are generated and dispatched to all co-owners pro-rata to their equity stake.
  * Booking privileges are suspended if a co-owner's required contribution remains unpaid after 7 calendar days.
* **Immutable Double-Entry Ledger**: Every balance mutation must be accompanied by an immutable row in `fund_transactions` recording `amount`, `balance_after`, `entry_type` (`CREDIT`/`DEBIT`), `transaction_reference`, and `source`.
* **Reconciliation Invariant**: $\sum \text{CREDITS} - \sum \text{DEBITS} \equiv \text{current\_balance}$ down to 0.01 VND.

### BR-FIN-04: Authoritative Payment Lifecycle & Idempotency Rules
* **Canonical States**: `PENDING`, `PROCESSING`, `SUCCESS` (alias `COMPLETED`), `FAILED`, `REFUNDED`, `CANCELLED`.
* **Transition Matrix**: Exactly 8 valid transitions are permitted:
  * `PENDING` $\to$ `PROCESSING`, `SUCCESS`, `FAILED`, `CANCELLED`
  * `PROCESSING` $\to$ `SUCCESS`, `FAILED`, `CANCELLED`
  * `SUCCESS` $\to$ `REFUNDED`
* All other 28 permutations, backward transitions, and modifications of terminal states (`FAILED`, `REFUNDED`, `CANCELLED`) are strictly rejected with `InvalidPaymentStateTransitionException` (HTTP 409 Conflict).
* **Payment Idempotency**:
  * Client operations support optional or mandatory `Idempotency-Key` header.
  * SHA-256 fingerprint digest of payload ensures tamper detection.
  * Replayed requests with the same key and identical payload return the cached response without re-executing payments.
  * Replaying a used key with an altered payload is rejected with `IdempotencyConflictException` (HTTP 409 Conflict).

### BR-FIN-05: Financial Transaction Safety & Zero Partial State Invariant
* All mutating financial operations enforce `@Transactional(rollbackFor = Exception.class)`.
* Conflicting concurrent financial operations acquire pessimistic row-level write locks (`findByIdWithLock`, `SELECT ... FOR UPDATE`) in MySQL InnoDB.
* **Atomic Cross-Entity Consistency**:
  * On payment `SUCCESS`: Atomically credit `SharedFund.currentBalance`, record `FundTransaction` (`CREDIT`, `PAYMENT_SETTLEMENT`), and mark `ExpenseAllocation.isSettled = true`.
  * On payment `REFUNDED`: Atomically debit `SharedFund.currentBalance`, record `FundTransaction` (`DEBIT`, `MANUAL_ADJUSTMENT`), and revert `ExpenseAllocation.isSettled = false`.
* Downstream failures trigger total database rollback, leaving zero partial financial state or balance discrepancies.

---

## 6. Group Governance & 3D Voting Rules

### BR-VOT-01: Proposal Lifecycle & Categories
Proposals may be initiated by any co-owner holding $\ge 10.00\%$ equity for the following actions:
* `ROUTINE_EXPENSE`: Repairs/upgrades between 2,000,000 VND and 10,000,000 VND.
* `MAJOR_EXPENSE`: Upgrades/repairs exceeding 10,000,000 VND (e.g., traction battery replacement).
* `OPERATIONAL_RULE_CHANGE`: Modifying peak hour definitions or buffer times.
* `OWNER_ADMISSION_OR_EXIT`: Approving new co-owner onboarding or share transfers.

### BR-VOT-02: Quorum & Passing Thresholds
* **Quorum Requirement**: Valid vote sessions require a minimum of **60.00%** total ownership equity participation.
  * **Participation Calculation Base**: Participation is calculated relative to **active ownership equity** in the syndicate group:
    $$\text{Participation Rate} = \frac{\sum_{v \in \text{Cast Votes}} v.\text{equityWeight}}{\sum_{s \in \text{Active Shares}} s.\text{percentage}} \times 100\%$$
  * **Inactive Share Handling**: Shares where `isActive == false` cannot cast ballots and are excluded from the active equity denominator.
  * **Role of `ABSTAIN`**: Cast ballots selecting `ABSTAIN` represent valid voter participation and count toward reaching the 60.00% quorum threshold ($\text{Participating} = \text{APPROVE} + \text{REJECT} + \text{ABSTAIN}$).
  * **Quorum Threshold Boundaries**:
    * Participation $< 60.00\%$ (e.g., $59.99\%$): Quorum is **NOT** reached; proposal cannot pass and transitions to `EXPIRED` if deadline passes without quorum.
    * Participation $= 60.00\%$: Quorum is **REACHED** (boundary inclusion $\ge 60.00\%$).
    * Participation $> 60.00\%$: Quorum is **REACHED**.
    * Zero votes cast ($0.00\%$ participation): Quorum is **NOT** reached.
* **Decision & Passing Thresholds**:
  * **Evaluation Precondition**: Quorum ($\ge 60.00\%$ active participation) **must be verified first**. If quorum is not achieved, the proposal fails immediately (`passed = false`), irrespective of approval percentages.
  * **Ordinary / Routine Business (`ROUTINE_EXPENSE`)**:
    * **Formula**: Requires strictly greater than **50.00%** of **participating equity**:
      $$\text{Approval Condition} \iff \frac{E_{\text{approve}}}{E_{\text{participating}}} > 0.5000 \iff E_{\text{approve}} \times 2 > E_{\text{participating}}$$
    * **Denominator**: $E_{\text{participating}} = E_{\text{approve}} + E_{\text{reject}} + E_{\text{abstain}}$.
    * **Boundaries**:
      * $\le 50.00\%$ (e.g. $49.99\%$, $50.00\%$): **FAILS / REJECTED** (strict inequality required; a 50/50 split does not pass).
      * $> 50.00\%$ (e.g. $50.01\%$): **PASSES**.
  * **Supermajority / Major Business (`MAJOR_EXPENSE`, `OPERATIONAL_RULE_CHANGE`, `OWNER_ADMISSION_OR_EXIT`)**:
    * **Formula**: Requires greater than or equal to **75.00%** of **total active syndicate equity**:
      $$\text{Approval Condition} \iff \frac{E_{\text{approve}}}{E_{\text{activeGroup}}} \ge 0.7500 \iff E_{\text{approve}} \times 100 \ge E_{\text{activeGroup}} \times 75.00$$
    * **Denominator**: $E_{\text{activeGroup}} = \sum_{s \in \text{Active Shares}} s.\text{percentage}$.
    * **Boundaries**:
      * $< 75.00\%$ (e.g. $74.99\%$): **FAILS / REJECTED**.
      * $\ge 75.00\%$ (e.g. $75.00\%$, $75.01\%$): **PASSES** (inclusive inequality $\ge$).
  * **Unanimous Business (Vehicle disposal, group dissolution)**: $100.00\%$ of total active group equity votes `APPROVE`.
* Voting period default duration: **72 hours**.

### BR-VOT-03: Proposal Voting Results & Data Privacy Rules
* **Official Results Endpoint**: `GET /api/v1/proposals/{id}/results`.
* **Authorized Governance Metrics Returned**:
  * `totalEligibleEquity`: Total active equity in the syndicate group ($\sum_{s \in \text{Active Shares}} s.\text{percentage}$).
  * `participatingEquity`: Sum of equity weights from all cast ballots ($E_{\text{approve}} + E_{\text{reject}} + E_{\text{abstain}}$).
  * `approveWeight`: Cumulative active equity weight voting `APPROVE`.
  * `rejectWeight`: Cumulative active equity weight voting `REJECT`.
  * `abstainWeight`: Cumulative active equity weight voting `ABSTAIN`.
  * `quorumStatus`: `"REACHED"` ($\ge 60.00\%$) or `"NOT_REACHED"` ($< 60.00\%$).
  * `threshold`: Decision passing threshold percentage ($50.00\%$ for routine, $75.00\%$ for supermajority).
  * `finalDecision`: Authoritative governance determination (`PASSED`, `REJECTED`, `EXPIRED`, `QUORUM_NOT_MET`).
* **Data Privacy & Unauthorized Data Protection**:
  * **Syndicate Authorization**: Protected by `@ownershipSecurity.isProposalGroupMember(#id, principal.id)` and `hasAnyRole('STAFF', 'ADMIN')`.
  * **Access Rejection**: Unauthenticated callers receive `401 Unauthorized`. Outsiders (not in target syndicate) and inactive co-owners (`isActive == false`) receive `403 Forbidden`.
  * **Ballot Anonymity**: The `/results` payload deliberately excludes individual ballots and voter identities (`ballots` list is completely omitted), safeguarding voter privacy.

### BR-VOT-04: Ballot Casting, Equity Weighting & Duplicate Ballot Invariant
* **Active Equity Weighting**:
  * Every ballot cast by an authenticated co-owner is dynamically and immutably weighted by their active equity share percentage in the target syndicate group:
    $$\text{ballot.equityWeight} = \text{ownershipShare.percentage}$$
  * Co-owners without active shares (`isActive == false`) or non-group members are strictly forbidden from voting (`HTTP 403 Forbidden`).
* **One-Voter-One-Ballot Invariant & Duplicate Prevention**:
  * Each co-owner may cast exactly one ballot per proposal (`APPROVE`, `REJECT`, or `ABSTAIN`).
  * Attempting to vote multiple times on the same proposal throws `DuplicateVoteException` (`HTTP 409 Conflict`).
  * Concurrency safety is guaranteed at the persistence tier by the composite database unique constraint `uk_proposal_user_vote (proposal_id, user_id)` on the `votes` table.
* **Proposal Lifecycle & Deadline Preconditions**:
  * Ballots may only be submitted while the proposal is strictly in the `ACTIVE` status (`proposal.status == ProposalStatus.ACTIVE`).
  * Ballots submitted after the proposal deadline (`Instant.now().isAfter(proposal.expiresAt)`) or on non-active proposals (`PASSED`, `REJECTED`, `EXPIRED`) are rejected with `HTTP 409 Conflict`.
* **Ballot Immutability & Audit Trail**:
  * Cast ballots are append-only and immutable; once submitted, a vote cannot be modified, replaced, retracted, or deleted.
  * Every vote cast writes an immutable audit record (`VOTE_CAST`) capturing voter user ID, proposal ID, ballot choice, equity weight, and timestamp.

---

## 7. Digital Contracts & Legal Validity Rules

### BR-CNT-01: Multi-Party Signature Integrity
* A Co-Ownership Agreement requires digital signatures from **100% of enrolled group members**.
* Status progression:
  $$\text{DRAFT} \to \text{PENDING\_SIGNATURE} \to \text{SIGNED} \to \text{ACTIVE}$$
* If any co-owner rejects the draft, the contract transitions to `REJECTED` and reverts to negotiation.
* Active contracts are immutable. Amendments must be spawned as child `ContractAmendment` entities.

---

## 8. Dispute Arbitration Rules

### BR-DIS-01: Dispute Escalation Path
* A dispute can be filed by any co-owner or staff within **24 hours** of check-out regarding damages, hygiene, late returns, or billing errors.
* Status progression:
  $$\text{OPEN} \to \text{UNDER\_REVIEW} \to \text{RESOLVED} \quad \text{or} \quad \text{ESCALATED}$$
* If peer co-owners fail to reach consensus within 5 calendar days, the dispute auto-escalates to `ADMIN_ARBITRATION`.
* Admin decision is final and automatically triggers ledger balance debits/credits or fairness score adjustments.

### BR-DIS-02: Dispute Creation & Evidence Validation Invariants
* **Creator Validation**:
  * The complainant must be an active co-owner in the target ownership group (`isActive == true`) or platform staff/admin.
  * Inactive co-owners and external users are rejected with `HTTP 403 Forbidden`.
* **Ownership Group Validation**:
  * The ownership group must exist and be active (`isActive == true`).
* **Related Entity Validation**:
  * If a `usageSessionId` is linked, the session must exist and belong to the group's vehicle.
  * If a `respondentUserId` is named, the user must exist and cannot be the complainant (self-dispute is prohibited; `HTTP 400 Bad Request`).
* **Reason Validation**:
  * Both `title` (max 150 chars) and `description` (max 5000 chars) are mandatory and cannot be blank.
* **Evidence Requirement**:
  * A dispute submission must include at least one valid evidence record (`fileUrl` mandatory, optional 3D mesh defect annotations, optional description). Submissions lacking evidence are rejected with `HTTP 400 Bad Request`.
* **Dispute State Machine Invariants**:
  * Permitted transitions: `OPEN` $\to$ `UNDER_REVIEW`, `RESOLVED`, `ESCALATED`; `UNDER_REVIEW` $\to$ `RESOLVED`, `ESCALATED`; `ESCALATED` $\to$ `RESOLVED`.
  * `RESOLVED` is an immutable terminal state. Reverse transitions and redundant transitions are rejected with `HTTP 409 Conflict`.

### BR-DIS-03: Evidence Attachment, Immutability & Audit Rules
* **Owner & Access Control**:
  * Authorized parties to upload supplementary evidence:
    1. Complainant user (`complainantUserId`)
    2. Respondent user (`respondentUserId`)
    3. Active syndicate co-owners in the dispute's group (`isActive == true`)
    4. Platform staff and administrators (`ROLE_STAFF`, `ROLE_ADMIN`)
  * Unauthorized parties (non-group outsiders, inactive co-owners who are not complainant/respondent) are rejected with `HTTP 403 Forbidden`.
  * Unauthenticated callers receive `HTTP 401 Unauthorized`.
* **High-Precision Timestamps**:
  * Every evidence record captures an immutable high-precision timestamp (`Instant createdAt = Instant.now()`) with `updatable = false`.
* **Immutable Storage & 3D Spatial References**:
  * Evidence stores references without requiring external blob storage SDK integration (`fileUrl` string reference, `mesh3dDefectCoordinates` JSON string for 3D model spatial defects).
  * Database fields (`dispute_id`, `uploaded_by_user_id`, `file_url`, `mesh_3d_defect_coordinates`, `description`, `created_at`) have `updatable = false`.
* **Tamper & Mutation Prevention**:
  * Evidence cannot be attached to a closed dispute; if dispute is `RESOLVED`, upload attempts are rejected with `HTTP 409 Conflict`.
  * Modification (`PUT`) and deletion (`DELETE`) of evidence records are strictly prohibited; attempts return `HTTP 405 Method Not Allowed` ("Dispute evidence is immutable and cannot be modified or deleted").
* **Comprehensive Audit Trail**:
  * Every evidence attachment permanently writes an `AuditLog` entry (`DISPUTE_EVIDENCE_ATTACHED`) capturing uploader, fileUrl, 3D coordinate status, and timestamp.
  * `GET /api/v1/disputes/{id}/history` provides an authoritative chronological audit log combining dispute lifecycle changes and evidence attachments.

### BR-DIS-04: Staff Review, Mediation & RBAC Separation Rules
* **Staff Capabilities**:
  * Platform Staff (`ROLE_STAFF`) and Administrators (`ROLE_ADMIN`) have oversight authority to review any dispute and associated 3D/media evidence across all groups (`GET /api/v1/disputes/{id}`, `GET /api/v1/disputes/staff/review`).
  * Staff can record detailed mediation review notes and factual observations (`POST /api/v1/disputes/{id}/mediation-notes`).
  * Staff can formulate non-binding proposed resolution terms (`POST /api/v1/disputes/{id}/propose-resolution`).
  * Staff can initiate mediation transitions (`OPEN` $\to$ `UNDER_REVIEW`) and deadlock escalations (`UNDER_REVIEW` $\to$ `ESCALATED`).
* **ADMIN-Only Final Arbitration Invariant**:
  * Staff **must not** perform final binding arbitration (`RESOLVED`).
  * Transitioning any dispute to `RESOLVED` requires `ROLE_ADMIN`. If a user with only `ROLE_STAFF` attempts to resolve a dispute, the system strictly rejects the request with `HTTP 403 Forbidden` ("Staff members cannot perform final binding dispute arbitration. Final resolution is restricted to administrators.").
* **Co-Owner Restrictions**:
  * Co-owners (`ROLE_CO_OWNER`) cannot review staff dashboards, record mediation notes, propose resolutions, or transition dispute lifecycle statuses (`HTTP 403 Forbidden`).

### BR-DIS-05: Administrator Final Arbitration & Binding Decision Rules
* **Strict Administrator Authority**:
  * Platform Administrators (`ROLE_ADMIN`) hold exclusive authority to execute final, legally and technically binding dispute arbitration (`POST /api/v1/disputes/{id}/arbitrate`, `POST /api/v1/disputes/{id}/transition` to `RESOLVED`).
  * Non-administrators (including platform staff with `ROLE_STAFF` and syndicate co-owners with `ROLE_CO_OWNER`) attempting final arbitration are strictly rejected with `HTTP 403 Forbidden` ("Only platform administrators can perform final binding dispute arbitration" / "Staff members cannot perform final binding dispute arbitration. Final resolution is restricted to administrators.").
  * Unauthenticated callers receive `HTTP 401 Unauthorized`.
* **Evidence Review Prerequisite**:
  * Final arbitration requires evidence review. Administrators can review all associated media, 3D defect mesh annotations, staff mediation notes, and full audit logs via the comprehensive arbitration dossier (`GET /api/v1/disputes/{id}/arbitration-dossier`).
  * A dispute cannot be arbitrated if it has zero evidence records (`HTTP 400 Bad Request`).
* **Mandatory Rationale & Binding Terms**:
  * Arbitration requires a non-blank factual justification/rationale (`reason`, 5 to 5000 chars) explaining findings and rulings. Missing or blank reason is rejected with `HTTP 400 Bad Request`.
  * Arbitration requires a non-blank binding resolution summary (`resolutionSummary`, 5 to 5000 chars) detailing rulings, financial/deductible reconciliations, and penalty awards. Missing or blank resolution summary is rejected with `HTTP 400 Bad Request`.
* **Resolution Record & Immutability**:
  * Executing arbitration transitions status from `OPEN`, `UNDER_REVIEW`, or `ESCALATED` to `RESOLVED`.
  * The dispute record captures:
    - Status: `RESOLVED`
    - Arbitrator: `arbitrator_user_id` (foreign key to `users`)
    - Resolution Timestamp: `resolved_at` (immutable Instant)
    - Ruling Terms: `resolution_summary` (TEXT)
  * Once `RESOLVED`, the dispute enters an immutable terminal state. Any attempt to re-arbitrate, transition, attach new evidence, or edit mediation notes is rejected with `HTTP 409 Conflict`.
* **Permanent Audit Trail**:
  * Arbitration persists an immutable `AuditLog` entry (`DISPUTE_ARBITRATED`) recording the arbitrator user, dispute ID, previous lifecycle state, new state `RESOLVED`, reason, resolution terms, and evidence count.

### BR-DIS-06: Dispute Resolution & SharedFund Financial Adjustment Rules
* **Unified Transactional Adjustment**:
  * Final dispute arbitration and financial fund adjustments execute within a single atomic `@Transactional(isolation = Isolation.READ_COMMITTED, rollbackFor = Exception.class)` boundary (`POST /api/v1/disputes/{id}/fund-adjustment` or `POST /api/v1/disputes/{id}/arbitrate` with fund adjustment parameters).
  * State transition to `RESOLVED`, fund balance mutation, and financial ledger transaction creation succeed or fail together as an indivisible unit of work.
* **Immutable Financial History & Ledger Record**:
  * Every fund adjustment creates an immutable `FundTransaction` ledger record:
    - `transactionType`: `DISPUTE_ADJUSTMENT`
    - `source`: `DISPUTE_RESOLUTION`
    - `entryType`: `DEBIT` (reimbursement / payout from fund to affected party) or `CREDIT` (penalty deduction or recovered damages deposited into fund)
    - `amount`: Adjustment amount (strictly positive, scaled to 2 decimal places)
    - `balanceAfter`: The exact resulting fund balance after adjustment
    - `transactionReference`: Guaranteed unique reference formatted as `DISP-<disputeId>-<UUID8>`
    - `description`: Dispute reference and resolution summary
  * Fund transactions are strictly append-only and cannot be mutated or deleted.
* **Bidirectional Resolution Reference**:
  * The `Dispute` entity maintains a direct foreign key `fund_transaction_id` referencing `fund_transactions(id)` and stores `fund_adjustment_amount`.
  * The `FundTransaction` stores the dispute ID in its reference (`DISP-<id>-<uuid>`) and audit metadata, establishing a bidirectional immutable audit trail.
* **Pessimistic Locking & Balance Validation**:
  * Prior to modifying the syndicate fund balance, the system acquires a database pessimistic write lock (`SELECT ... FOR UPDATE` via `sharedFundRepository.findByGroupIdWithLock(groupId)`).
  * For `DEBIT` adjustments, if the current fund balance is less than the requested adjustment amount (`oldBalance < adjustmentAmount`), the transaction is rejected immediately with an `InsufficientFundBalanceException` (`HTTP 400 Bad Request`).
* **Strict Rollback on Failure**:
  * If the fund adjustment fails for any reason (insufficient funds, concurrency lock timeout, data integrity error):
    1. The entire database transaction is rolled back.
    2. The dispute status remains unchanged (`OPEN`, `UNDER_REVIEW`, or `ESCALATED`).
    3. The `SharedFund` balance remains strictly unaltered.
    4. Zero `FundTransaction` entries are persisted.
    5. Zero audit log entries are persisted.
* **Duplicate Resolution & Double Adjustment Prevention**:
  * If a dispute is already in the terminal `RESOLVED` state, any attempt to resolve, re-arbitrate, or execute a fund adjustment throws `InvalidDisputeStateTransitionException` (`HTTP 409 Conflict`).
  * In addition, if a dispute already has an attached `fund_transaction_id`, duplicate adjustments are rejected with `HTTP 409 Conflict`, mathematically guaranteeing zero double payouts or duplicate charges.
* **Dual Audit Trail**:
  * Every successful fund adjustment persists two separate immutable audit records:
    1. `DISPUTE_ARBITRATED` on entity `Dispute`, capturing arbitrator, old status, `RESOLVED`, fund adjustment amount, and transaction reference.
    2. `SHARED_FUND_DISPUTE_ADJUSTMENT` on entity `SharedFund`, capturing arbitrator, old balance, new balance, delta, entry type, dispute ID, and transaction reference.
* **Role-Based Access Control**:
  * Fund adjustments are strictly restricted to administrators (`ROLE_ADMIN`).
  * Co-owners (`ROLE_CO_OWNER`) and platform staff (`ROLE_STAFF`) are strictly prohibited (`HTTP 403 Forbidden`).
