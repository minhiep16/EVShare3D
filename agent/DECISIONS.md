# EVShare 3D – ARCHITECTURE DECISION RECORDS (ADR)

## ADR-01: Pure 3D Canvas Application Architecture vs. Hybrid HTML Overlay
* **Status**: ACCEPTED
* **Context**: The master specification explicitly forbids traditional Web2 HTML dashboards, navbars, sidebars, modal popups, and `<Html>` overlays from `@react-three/drei` as the primary user interface.
* **Decision**: All primary user interaction (login, inputs, keyboards, buttons, timelines, charts, modals, terminals) will be implemented as authentic 3D entities inside the WebGL canvas using Three.js meshes, SDF text rendering, and R3F pointer events.
* **Consequences**: Significantly increases visual immersion and fulfills the "Virtual Metaverse" experience. Requires building custom reusable spatial UI primitives (`ThreeDInput`, `ThreeDKeyboard`, `ThreeDButton`, `ThreeDTerminal`).

---

## ADR-02: Text Rendering Strategy in 3D Space
* **Status**: ACCEPTED
* **Context**: Traditional 2D DOM text cannot be placed in pure WebGL without HTML overlays. Text in 3D space must be crisp at all angles and distances without prohibitive draw call overhead.
* **Decision**: Adopt `@react-three/drei` `Text` (powered by `Troika-three-text`) utilizing Signed Distance Fields (SDF). For high-density dynamic visual charts, render to offscreen HTML5 Canvases and project via dynamic `CanvasTexture`.
* **Consequences**: Delivers sharp typography, high performance, and zero dependency on HTML overlay panels.

---

## ADR-03: Database Engine & Transactional Isolation
* **Status**: ACCEPTED
* **Context**: Co-ownership calculations, equity invariants ($\sum = 100.00\%$), booking conflict prevention, and fund ledgers require strict ACID guarantees.
* **Decision**: Use MySQL 8.0 with the InnoDB storage engine. Financial and booking transactions will run under `READ_COMMITTED` with explicit pessimistic write locking (`SELECT ... FOR UPDATE`) on vehicle schedule intervals during reservation creation.
* **Consequences**: Eliminates race conditions and double-booking vulnerabilities.

---

## ADR-04: Stateless JWT Authentication with Refresh Rotation
* **Status**: ACCEPTED
* **Context**: The application requires robust authentication across browser sessions without maintaining sticky server-side sessions.
* **Decision**: Implement stateless JWT with dual tokens: short-lived Access Token (15 minutes) stored in memory and longer-lived Refresh Token (7 days) stored securely with server-side revocation tracking.
* **Consequences**: Minimizes attack windows while delivering a seamless login experience at the 3D Security Checkpoint.

---

## ADR-05: AI System Scope & Advisory Boundaries
* **Status**: ACCEPTED
* **Context**: Predictive intelligence (battery health, fair usage balance, maintenance schedules) can easily produce unintended side effects if permitted to execute autonomous writes.
* **Decision**: Enforce a strict non-negotiable boundary: the AI service is strictly an **advisory engine**. It may generate recommendations and structured proposal drafts, but can never directly modify bookings, contracts, financial ledgers, or ownership shares without explicit human confirmation.
* **Consequences**: Protects legal and financial integrity while providing deep contextual assistance.

---

## ADR-06: Phased Development Roadmap (Backend-First Foundation)
* **Status**: ACCEPTED
* **Context**: Pure 3D world development requires rich, deterministic backend APIs to bind against. Developing 3D interactions against fake temporary mocks causes architectural drift.
* **Decision**: Construct the complete backend business foundation, database migrations, security layers, and domain services in Phases 02–08, followed by full 3D world synthesis and real API integration in Phase 09.
* **Consequences**: Ensures that every 3D interactive terminal, button, and digital twin in Phase 09 integrates immediately with genuine, validated backend REST APIs.

---

## ADR-07: Decoupled Token Storage & Password Reset Foundation
* **Status**: ACCEPTED
* **Context**: Phase 03 requires robust server-side token management (refresh token revocation/rotation and secure password reset tokens) without introducing unneeded third-party runtime dependencies (e.g. external SMTP or Redis) before required.
* **Decision**: Define SPI interfaces (`RefreshTokenStore`, `PasswordResetTokenStore`, and `PasswordResetNotifier`). Implement thread-safe in-memory providers (`InMemoryRefreshTokenStore`, `InMemoryPasswordResetTokenStore`, and `DevPasswordResetNotifier`) for the development, test, and standalone runtime profiles, while preserving simple swap-in capability for distributed caching (Redis) and production notification providers in deployment phases.
* **Consequences**: Enables 100% deterministic, zero-external-dependency automated testing in Phase 03 while fully satisfying security requirements (RFC 6819 token rotation, anti-enumeration, single-use token invalidation, and session revocation).

---

## ADR-08: Canonical 7-State Vehicle Finite State Machine & Pessimistic Concurrency
* **Status**: ACCEPTED
* **Context**: Digital twin vehicles in EVShare 3D transition across 7 distinct operational states (`AVAILABLE`, `BOOKED`, `IN_USE`, `CHARGING`, `MAINTENANCE`, `DAMAGED`, `UNAVAILABLE`). Uncontrolled concurrent state transitions (e.g., reserving a vehicle while staff is dispatching it to maintenance) could corrupt vehicle telemetry, safety compliance, and booking operations.
* **Decision**: Formalize an immutable state transition matrix in `VehicleStateMachine` that strictly rejects invalid transitions. Concurrency during status updates is guarded by transactional pessimistic write locks (`findByIdForUpdate` / `SELECT ... FOR UPDATE`) in MySQL InnoDB.
* **Consequences**: Guarantees zero race conditions or invalid lifecycle jumps across vehicle fleet management.

---

## ADR-09: Fractional Co-Ownership 100.00% Absolute Distribution Invariant (BR-OWN-01)
* **Status**: ACCEPTED
* **Context**: In fractional co-ownership of high-value electric vehicles, financial risk, voting rights, and cost allocations strictly depend on exact share mathematics. Floating-point numbers introduce rounding drift, and concurrent share creation/transfers could oversell or undersell vehicle equity beyond 100.00%.
* **Decision**: Enforce `BR-OWN-01` requiring the sum of all active member equity shares in an ownership group to equal exactly `100.00%` (`BigDecimal` scaled to 2 decimal places with `RoundingMode.HALF_EVEN`). All equity mutations (create share, update percentage, deactivate, reactivate, transfer, rebalance) execute under `@Transactional` with explicit pessimistic write locking (`findGroupByIdForUpdate`) on the syndicate group row. If the resultant active share sum is not 100.00%, mutations either throw `InvalidOwnershipDistributionException` (reverting the transaction) or, in batch rebalancing, calculate exact residual adjustments.
* **Consequences**: Mathematically guarantees zero overselling or underselling of vehicle equity and maintains strict referential integrity across the syndicate lifecycle.

---

## ADR-10: Co-Ownership Contract Lifecycle, Terms Immutability, and Cryptographic SHA-256 Signatures
* **Status**: ACCEPTED
* **Context**: Legal co-ownership contracts require strict tamper-evidence, clear progression from drafting to activation, and multi-party signature collection. Once submitted for signature or activated, contract terms text must not be altered, and existing historical contracts must never be deleted.
* **Decision**: Implement `ContractStateMachine` enforcing the canonical lifecycle: `DRAFT -> PENDING_SIGNATURE -> SIGNED -> ACTIVE -> EXPIRED / TERMINATED`. Update of terms or title is strictly rejected once a contract leaves `DRAFT`. Contract signatures compute and store a cryptographic SHA-256 digest (`SHA-256(contractId + ":" + version + ":" + termsText + ":" + userId + ":" + timestamp)`) binding the signer to the exact immutable text. Direct `DELETE` operations on contracts are permanently blocked with HTTP 405 Method Not Allowed / 400 Bad Request to guarantee historical contract data preservation.
* **Consequences**: Ensures legal and regulatory compliance, tamper-evident auditability, and complete contract historical lineage.

---

## ADR-11: Append-Only Provenance Audit Logging for Equity Ownership & Legal Contracts
* **Status**: ACCEPTED
* **Context**: Disputes, equity audits, and regulatory inspections require complete chronological traceability of all vehicle ownership and contract lifecycle mutations, including exact before-and-after states and acting principals.
* **Decision**: Store all ownership group creations, equity share certificate issuances, transfers, rebalances, and contract lifecycle actions in an append-only `audit_logs` table. Every event records acting `userId`, `action`, `entityName`, `entityId`, `old_state_json`, `new_state_json`, `ipAddress`, and immutable `created_at` timestamp.
* **Consequences**: Provides full non-repudiation and audit provenance accessible via `/history` REST endpoints without impacting primary read performance.

---

## ADR-12: Pessimistic Turnaround-Buffer Booking Concurrency & Conflict Engine
* **Status**: ACCEPTED
* **Context**: In co-ownership syndicates, high demand for prime weekend and holiday driving slots creates severe concurrency risks where multiple members attempt to book overlapping intervals. Moreover, operational readiness requires cleaning, inspection, and charging turnaround between consecutive trips.
* **Decision**: Implement a database-level pessimistic write lock on the target vehicle (`SELECT ... FOR UPDATE` via `findByIdForUpdate`) during booking creation, rescheduling, and status transitions within `@Transactional`. Enforce an automated 30-minute turnaround buffer between consecutive bookings:
  $$\text{Interval}_A \cap \text{Interval}_B \neq \emptyset \iff \max(start_A, start_B) < \min(end_A + 30\text{m}, end_B + 30\text{m})$$
  Any overlapping attempt throws `BookingConflictException` (mapped to HTTP 409 Conflict).
* **Consequences**: Mathematically prevents double-booking and buffer race conditions under extreme concurrent load while ensuring vehicles have adequate buffer time for turnaround.

---

## ADR-13: Syndicate Fair Usage Allocation Formula & Weighted Demand Gini Imbalance Classification
* **Status**: ACCEPTED
* **Context**: Equity co-owners expect vehicle access proportional to their ownership percentage (`BR-FAIR-01..03`). Pure duration tracking ignores peak demand premiums (Friday evening vs. Tuesday night) and encourages resource hoarding.
* **Decision**: Implement a multi-factor fair usage engine. Historical trip sessions are weighted by time-of-day demand multipliers (Peak = 1.5x, Standard = 1.0x, Off-Peak = 0.7x). Compute individual fairness ratios ($FR_i$) normalized against active equity shares and calculate the syndicate-wide Gini inequality coefficient ($G \in [0.0, 1.0]$). Co-owners are dynamically mapped into 4 visual aura tiers: `BALANCED` ($0.90 \le FR \le 1.10$), `SLIGHTLY_IMBALANCED`, `IMBALANCED`, and `SEVERELY_IMBALANCED`. Co-owners with $FR_i < 1.0$ receive automated priority in booking contention.
* **Consequences**: Produces mathematically transparent, un-gameable fairness metrics exposed via `/api/v1/analytics/fair-usage` for 3D UI visualization and automated priority arbitration.

---

## ADR-14: Usage Session Historical Immutability, Physical 3D Inspection Coordination, and Controlled Surcharge Computation
* **Status**: ACCEPTED
* **Context**: Vehicle operations require auditing actual mileage and battery consumption, assessing penalties (such as returning under 20% SoC unplugged per BR-OPS-02), logging 3D defect coordinate flags, and guaranteeing that concluded trips cannot be rewritten or manipulated.
* **Decision**: Implement `UsageSession` with transactional closure. Check-in records starting odometer, battery, and optional initial physical inspection. Check-out validates non-negative ending mileage ($\ge \text{startOdometer}$), computes battery consumption delta, automatically calculates deterministic surcharges (150,000 VND for $<20\%$ battery unplugged; 50,000 VND / 30 min for late return $>15$ min), captures 3D mesh defect coordinates in `vehicle_inspections`, and permanently seals the session. Concluded sessions reject rewrite attempts with `HistoricalUsageImmutableException` (HTTP 409).
* **Consequences**: Protects financial and operational audit trails against fraudulent retroactive alterations and provides clear evidence for syndicate dispute resolution.

---

## ADR-15: Stateless HMAC-SHA256 Cryptographic QR Check-In Protocol with Live DB Cross-Validation
* **Status**: ACCEPTED
* **Context**: Fast, contactless check-in at physical parking bays or charging stalls requires a secure QR code. Exposing user credentials or sensitive booking data in a QR payload introduces security risks, while trusting offline QR claims alone risks unauthorized access to damaged or cancelled vehicles.
* **Decision**: Implement a two-tiered QR validation protocol. The backend issues a short-lived (5-minute TTL) signed JWT with `tokenType="QR_CHECK_IN"` containing strictly non-sensitive public identifiers (`bookingId`, `vehicleId`, `userId`, `jti`, `exp`), signed with server-side HMAC-SHA256. Upon scanning, the server verifies the cryptographic signature and expiration, then performs authoritative live queries against `BookingRepository`, `VehicleRepository`, `OwnershipGroupRepository`, and `UserRepository` to verify active co-owner equity ACLs, vehicle operational status (`AVAILABLE` or `BOOKED`), and the check-in time window ($[startTime - 15\text{m}, startTime + 30\text{m}]$). Scans $>30$ minutes overdue automatically mark the booking as `NO_SHOW` in the database per BR-OPS-01.
* **Consequences**: Guarantees zero sensitive data leakage inside QR codes, prevents replay or forged token attacks, and enforces live physical station verification before vehicles can be operated.

---

## ADR-16: Mathematical Cost Allocation Engine & Deterministic Residual Penny Absorption (BR-FIN-02)
* **Status**: ACCEPTED
* **Context**: Irregular expenses split across multiple fractional co-owners inevitably produce fractional pennies due to division precision limits. Floating-point math introduces unacceptable financial drift, and arbitrary remainder assignment causes dispute risks.
* **Decision**: Define a pluggable strategy pattern (`CostAllocationStrategy`) with three mathematical implementations:
  1. `OwnershipBasedAllocationStrategy`: Allocates purely pro-rata to active ownership equity percentages for fixed overhead.
  2. `UsageBasedAllocationStrategy`: Allocates pro-rata to telemetry odometer distance logged across completed `UsageSession` records, safely falling back to active ownership equity if zero utilization is recorded.
  3. `HybridAllocationStrategy`: Exactly 30.00% fixed base (ownership equity) + 70.00% variable remainder (usage distance).
  All arithmetic uses Java `BigDecimal` with Banker's Rounding (`RoundingMode.HALF_EVEN`, scale 2). Any residual discrepancy ($\Delta = \text{totalExpense} - \sum \text{allocatedShares}$) is deterministically absorbed by the co-owner with the highest allocated share (tie-breaker: lowest `userId`).
* **Consequences**: Enforces mathematical penny equality ($\sum \text{allocatedShares} \equiv \text{totalExpense}$ down to 0.01 VND) and eliminates arbitrary remainder assignment.

---

## ADR-17: Shared Fund Immutable Double-Entry Ledger, Pessimistic Row-Level Locking & Balance Reconciliation
* **Status**: ACCEPTED
* **Context**: Syndicate 3D Vault funds receive concurrent deposits, member contributions, automated surcharge credits, and expense payouts. Uncontrolled concurrency can cause lost updates or balance inconsistencies between the summary balance and the transaction log.
* **Decision**: Implement an immutable append-only ledger in `fund_transactions`. Every balance mutation executes within `@Transactional(isolation = Isolation.READ_COMMITTED)` under a pessimistic write lock (`SELECT ... FOR UPDATE`) on the `shared_funds` row. Each transaction records an immutable entry with `amount`, `balance_after`, `entry_type` (`CREDIT` or `DEBIT`), unique `transaction_reference`, and `source`. Provide an automated reconciliation engine that asserts $\sum \text{CREDITS} - \sum \text{DEBITS} \equiv \text{currentBalance}$. Overdrafts are strictly rejected unless explicitly flagged.
* **Consequences**: Mathematically prevents lost updates, ensures 100% auditability, and guarantees ledger reconciliation parity.

---

## ADR-18: Pluggable Payment Provider SPI, Authoritative 6-State Lifecycle Machine & Idempotency Key Fingerprinting
* **Status**: ACCEPTED
* **Context**: Modern EV co-ownership requires flexible payment settlement (banking transfer QR, e-wallets, third-party payment gateways, sandbox mock) with zero vendor lock-in. Furthermore, network retries and duplicate user clicks must not create duplicate payments, and state transitions must follow a strict finite state machine.
* **Decision**:
  1. Define a pluggable `PaymentProvider` Service Provider Interface (SPI) managed by `PaymentProviderRegistry`, supporting `MOCK`, `BANK_TRANSFER`, `E_WALLET`, and `GATEWAY` implementations.
  2. Implement `PaymentStateMachine` governing 6 canonical states (`PENDING`, `PROCESSING`, `SUCCESS`, `FAILED`, `REFUNDED`, `CANCELLED`). Enforce exactly 8 valid state transitions while strictly rejecting the other 28 permutations with HTTP 409 Conflict. State transitions acquire a pessimistic write lock on the `Payment` row and synchronize related entities (`SharedFund` balance credit/debit and `ExpenseAllocation` settlement status) in a single atomic transaction.
  3. Implement `IdempotencyService` storing request records in `idempotency_records`. Incoming requests evaluate the client `Idempotency-Key` header with a SHA-256 payload fingerprint. Identical replays return cached responses; tampered payloads with a used key are rejected with HTTP 409 Conflict.
* **Consequences**: Eliminates duplicate charges, guarantees clean transaction rollback under downstream failures, preserves full transition audit lineage, and allows effortless addition of future payment processors.

---

## ADR-19: Syndicate Democratic Governance & Equity-Weighted Voting Protocol (BR-VOT-01..04)
* **Status**: ACCEPTED
* **Context**: Fractional EV co-ownership requires democratic self-governance for syndicate operational decisions, routine vs. major expenses, and co-owner admissions. Tokenized governance often suffers from voter apathy, Sybil attacks, duplicate voting, and opaque voting tallies. Co-owners need clear eligibility criteria, deterministic mathematical equity weighting, robust quorum checks, and voter privacy.
* **Decision**: Implement the `VotingService` and `ProposalStateMachine` architecture:
  1. **Proposal Eligibility**: Enforces $\ge 10.00\%$ active equity ownership stake in the syndicate group to sponsor proposals (`BR-VOT-01`).
  2. **Automated Ballot Seeding**: Proposals automatically seed canonical ballot options (`APPROVE`, `REJECT`, `ABSTAIN`).
  3. **Equity-Weighted Ballots**: Ballots cast are weighted strictly by the voter's active equity share percentage (`BigDecimal` precision). Duplicate votes are blocked both at the service layer and by database constraint `uk_proposal_user_vote (proposal_id, user_id)`.
  4. **Quorum Enforcement**: Quorum requires $\ge 60.00\%$ active equity participation (`BR-VOT-02`). Ballots selecting `ABSTAIN` count toward reaching quorum.
  5. **Tiered Passing Thresholds**: Routine expenses (`ROUTINE_EXPENSE`) require simple majority of participating equity ($> 50.00\%$), while major actions (`MAJOR_EXPENSE`, `OPERATIONAL_RULE_CHANGE`, `OWNER_ADMISSION_OR_EXIT`) require supermajority ($\ge 75.00\%$) of total active syndicate equity.
  6. **Ballot Immutability & Privacy**: Ballots remain immutable and append-only. The official voting results endpoint (`/results`) provides transparent mathematical tallies while concealing individual ballots to preserve voter privacy (`BR-VOT-03`, `BR-VOT-04`).
* **Consequences**: Guarantees mathematically unalterable democratic decision-making, prevents minority takeover or rogue proposals, and preserves voter confidentiality.

---

## ADR-20: Dispute Resolution Lifecycle, Multi-Role Mediation, and Administrative Arbitration Dossier (BR-DIS-01..05)
* **Status**: ACCEPTED
* **Context**: Fractional co-ownership syndicates experience interpersonal conflicts over vehicle damage, telemetry discrepancies, late returns, hygiene, and expense allocations. Unstructured disputes lead to deadlock or offline legal exposure. The system requires structured dispute progression, mandatory evidence verification, staff mediation capabilities, and authoritative administrative arbitration.
* **Decision**: Implement `DisputeService` and `DisputeStateMachine`:
  1. **Canonical Lifecycle**: Formalize canonical dispute lifecycle: `OPEN -> UNDER_REVIEW -> RESOLVED` or `OPEN / UNDER_REVIEW -> ESCALATED -> RESOLVED`.
  2. **Mandatory Evidence & 3D Spatial Defects**: Enforce mandatory evidence attachment (`CreateDisputeEvidenceRequest`) with high-precision timestamping and optional 3D mesh defect coordinates (`mesh3dDefectCoordinates`) for physical vehicle inspections (`BR-DIS-02`, `BR-DIS-03`).
  3. **Evidence Immutability**: Dispute evidence is strictly append-only and immutable; `PUT` and `DELETE` on evidence return `HTTP 405 Method Not Allowed`.
  4. **Staff Mediation vs. Admin Authority**: Platform Staff (`ROLE_STAFF`) can record mediation notes and propose non-binding resolution terms (`BR-DIS-04`), but are strictly forbidden from executing final binding arbitration (`HTTP 403 Forbidden`).
  5. **Administrative Arbitration Dossier**: Platform Administrators (`ROLE_ADMIN`) hold exclusive authority to execute final binding arbitration (`BR-DIS-05`) backed by comprehensive evidence review via the unified arbitration dossier (`GET /api/v1/disputes/{id}/arbitration-dossier`).
* **Consequences**: Eliminates informal or unprovable accusations, ensures complete due process and evidence transparency, and enforces strict RBAC separation between mediation and final legal rulings.

---

## ADR-21: Dispute-Treasury Settlement Integration with Atomic Balance Adjustment and Immutable Financial History (BR-DIS-06)
* **Status**: ACCEPTED
* **Context**: Resolving disputes often mandates financial compensation—either reimbursing an aggrieved co-owner from the syndicate SharedFund or assessing damages/penalties deposited into the fund. If dispute resolution and treasury fund modification execute in separate transactions, network failures or overdrafts could result in resolved disputes without payment or deducted balances without dispute closure.
* **Decision**: Integrate `DisputeService` with `SharedFundRepository` and `FundTransactionRepository` under a single atomic `@Transactional(isolation = Isolation.READ_COMMITTED, rollbackFor = Exception.class)` boundary (`POST /api/v1/disputes/{id}/fund-adjustment`):
  1. **Pessimistic Row-Level Locking**: Acquire pessimistic write lock (`SELECT ... FOR UPDATE` via `findByGroupIdWithLock`) on the syndicate's `SharedFund` vault row.
  2. **Overdraft Protection**: For `DEBIT` adjustments, validate that `currentBalance >= adjustmentAmount`; throw `InsufficientFundBalanceException` (`HTTP 400 Bad Request`) on overdraft.
  3. **Atomic Multi-Entity Sync**: Atomically mutate fund balance, create an immutable `FundTransaction` (`DISPUTE_ADJUSTMENT`, `DISPUTE_RESOLUTION`, unique reference `DISP-<disputeId>-<UUID8>`), transition dispute status to `RESOLVED`, and bind `fund_transaction_id` and `fund_adjustment_amount` on the `Dispute` entity.
  4. **Strict Rollback on Failure**: Enforce strict rollback on any exception: zero balance drift, zero partial transactions.
  5. **Double-Adjustment Prevention**: Prevent duplicate resolution or double fund adjustments: terminal `RESOLVED` status and existing `fund_transaction_id` strictly reject subsequent attempts with `HTTP 409 Conflict`.
  6. **Dual Audit Trails**: Record dual immutable audit logs (`DISPUTE_ARBITRATED` on Dispute, `SHARED_FUND_DISPUTE_ADJUSTMENT` on SharedFund).
* **Consequences**: Eliminates all risk of ledger discrepancy, double payouts, or partial settlement state between the governance dispute subsystem and the treasury banking ledger.

---

## ADR-22: Multi-Tier Dynamic Performance Engine & Adaptive DPR Scaling
* **Status**: ACCEPTED
* **Context**: The master specification requires EVShare 3D to maintain a smooth 60 FPS visual experience across high-end discrete GPUs, laptops with integrated GPUs, and mobile devices without suffering thermal throttling or frame stutter.
* **Decision**: Implement `usePerformanceStore` and calibrated `PerformanceProfile` tiers (`HIGH`, `MEDIUM`, `LOW`):
  1. `HIGH`: Native Retina DPR `[1.0, 2.0]`, 2048 PCF soft shadows, $16\times$ anisotropy, $1.0\times$ LOD bias, full bloom/SSAO.
  2. `MEDIUM`: Balanced DPR `[0.85, 1.5]`, 1024 PCF shadows, $4\times$ anisotropy, $1.2\times$ LOD bias, subtle bloom.
  3. `LOW`: Clamped DPR `[0.65, 1.0]`, shadows disabled, $1\times$ filtering, $1.5\times$ aggressive polygon reduction, post-processing off.
  4. Dynamic degradation engine: Measures frame render time via moving average buffer ($N=60$). If FPS drops below $28\text{fps}$ for $\ge 3\text{s}$, automatically steps down tier without page reloads.
* **Consequences**: Ensures high-end machines display hyper-realistic cyber-industrial visuals while lower-spec machines automatically maintain stable framerates.

---

## ADR-23: Cybernetic WebGL Diagnostics & Safe Mode Recovery (Anti-2D Fallback Mandate)
* **Status**: ACCEPTED
* **Context**: When WebGL experiences context loss, memory exhaustion, or hardware capability failure, traditional Web3/3D apps often silently degrade into an ordinary HTML 2D dashboard or display a blank white page. Both violate the master Pure 3D mandate.
* **Decision**: Implement a dedicated WebGL recovery architecture in `frontend/src/engine/recovery/`:
  1. `webglDetector`: Queries WebGL2, GPU vendor, renderer, and unmasked hardware telemetry, flagging software CPU emulators.
  2. `useWebGLRecoveryStore`: Tracks context loss/restore events, catches initialization faults, and persists diagnostic reports.
  3. `RecoveryScreen`: Cybernetic diagnostic HUD offering detailed GPU readout, copyable JSON telemetry, retry loop (up to 3 attempts), and a lightweight 3D "Safe Mode" (booting with `LOW` tier profile).
  4. `ErrorBoundary3D`: Catches R3F rendering and buffer overflow faults.
* **Consequences**: Strictly prevents blank screens and eliminates silent 2D dashboard fallback while empowering users and engineers with deep GPU diagnostic visibility.

---

## ADR-24: Unified Touch Control & Responsive Viewport Adaptation
* **Status**: ACCEPTED
* **Context**: Metaverse EV co-ownership must be accessible on smartphones and tablets. Standard mouse/pointer controls fail on touchscreens, and narrow portrait aspect ratios can clip 3D vehicle showrooms or terminals.
* **Decision**: Implement the mobile/tablet touch subsystem in `frontend/src/engine/touch/`:
  1. `VirtualTouchJoystick`: Bottom-left HUD thumbstick computing 360° normalized analog vectors `[strafe, forward]`, mapping displacement magnitude to `WALKING` vs `SPRINTING` with spring-recentering.
  2. `TouchGestureController`: Filters taps ($\le 250\text{ms}$, $\le 10\text{px}$) for raycasting selection via normalized NDC coordinates, while touch drags on the right viewport orbit and pitch the spatial camera. Two-finger pinch controls camera zoom/FOV.
  3. `ResponsiveViewportController`: Dynamically adapts vertical FOV on portrait viewports ($aspect < 1.0$, scaling up to $1.4\times$) and automatically clamps DPR ($\le 1.25$ mobile, $\le 1.5$ tablet) to prevent thermal degradation.
* **Consequences**: Provides smooth, intuitive tactile navigation on mobile devices within the exact same pure 3D canvas experience.
