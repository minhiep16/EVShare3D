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
