# EVShare 3D – TECHNICAL RISKS & MITIGATION STRATEGIES

## 1. WebGL Performance & Draw Call Bottlenecks
* **Risk**: Rendering 12 interconnected 3D metaverse sectors, detailed digital twin EV models, and numerous 3D spatial UI terminals could easily exceed the 60 FPS budget on standard hardware due to excessive draw calls and shadow calculations.
* **Mitigation Strategy**:
  1. Implement spatial sector culling: Rooms out of camera frustum or beyond threshold distance have their meshes completely unmounted or set to `visible={false}`.
  2. Geometry Instancing & Batching: Repetitive structural elements (beams, light fixtures, floor panels) rendered via `InstancedMesh`.
  3. Dynamic Adaptive Quality Manager: Monitors frame time and automatically scales post-processing (SSAO, bloom, shadow map resolution) across `HIGH`, `MEDIUM`, and `LOW` presets.

---

## 2. 3D Spatial Input Ergonomics & Virtual Keyboard Latency
* **Risk**: Typing extensive strings (e.g. passwords, contract notes) purely by clicking individual 3D keys with a mouse pointer can cause user fatigue and high input latency.
* **Mitigation Strategy**:
  1. Hybrid Input Listener: When a `ThreeDInput` object is focused in the 3D world, the platform simultaneously listens to standard physical computer keyboard events (`keydown`) in addition to pointer clicks on the 3D virtual keyboard.
  2. Visual & Audio Tactility: Immediate local mesh translation (Z-axis depression) and instantaneous audio click give the user immediate tactile confidence before network operations fire.

---

## 3. High-Concurrency Booking Race Conditions (Double-Booking)
* **Risk**: Multiple co-owners in a syndicate simultaneously attempting to reserve the same high-demand weekend slot (e.g. Saturday 09:00–17:00) could lead to double-booking if checked only at the application level.
* **Mitigation Strategy**:
  1. Enforce transactional row-locking using `SELECT ... FOR UPDATE` on the vehicle availability ledger within `@Transactional(isolation = Isolation.READ_COMMITTED)`.
  2. Implement an overlapping interval query that rejects any booking where:
     `existing.start_time < new.end_time AND existing.end_time > new.start_time`.

---

## 4. Financial Calculation Precision & Rounding Discrepancies
* **Risk**: Dividing an irregular expense (e.g. 1,000,000 VND) across 3 co-owners with shares of 33.33%, 33.33%, 33.34% can produce fractional penny rounding errors if floating-point math is used.
* **Mitigation Strategy**:
  1. Mandatory usage of `BigDecimal` in Java with explicit `RoundingMode.HALF_EVEN` (Banker's rounding) and `DECIMAL(15, 2)` in MySQL.
  2. Residual penny allocation algorithm: Any leftover remainder after integer cents division is deterministically allocated to the co-owner with the largest fractional remainder or highest equity stake.

---

## 5. WebGL Unsupported Fallback Environment
* **Risk**: Users on legacy browsers or systems with disabled WebGL might see a blank white screen or error cascade.
* **Mitigation Strategy**:
  1. Implement a WebGL detection probe at application boot.
  2. If WebGL is unavailable, display a styled 3D-aesthetic error notification explaining that hardware acceleration is required to access the EV Metaverse. In adherence to Master Prompt rules, it will not degrade into a substitute 2D website.

---

## 6. Multi-Instance Token Store & Clustering Scalability
* **Risk**: The current `InMemoryRefreshTokenStore` and `InMemoryPasswordResetTokenStore` use JVM-local `ConcurrentHashMap` structures. Deploying multiple backend instances behind a load balancer without sticky sessions would cause cross-instance token lookup misses.
* **Mitigation Strategy**:
  1. Token stores are decoupled behind SPI interfaces (`RefreshTokenStore` and `PasswordResetTokenStore`).
  2. For multi-replica production deployments in Phase 10, provide a Redis-backed or database-backed implementation via Spring profiles (`@Profile("cluster")` or `@Profile("prod")`), allowing seamless distributed token verification with zero changes to business services or security filters.

---

## 7. Concurrent Equity Mutations & 100.00% Share Invariant Race Conditions
* **Risk**: Concurrent equity transfer or share modification requests targeting the same ownership group could induce race conditions, resulting in an active equity sum exceeding or dropping below 100.00% (violating `BR-OWN-01`).
* **Mitigation Strategy**:
  1. Mandatory transactional write locking: Every share mutation method locks the parent `ownership_groups` record using `findGroupByIdForUpdate` (`SELECT ... FOR UPDATE`) under `@Transactional`.
  2. Strict transaction boundary: If post-mutation validation detects `sum != 100.00%`, `InvalidOwnershipDistributionException` is thrown, triggering a complete transaction rollback before any invalid equity state can commit.

---

## 8. Contract Tampering & State Transition Concurrency
* **Risk**: Modifying contract terms text while signatures are being collected, or race conditions where concurrent administrative calls attempt contradictory status transitions.
* **Mitigation Strategy**:
  1. Strict status guards: Contract terms and title updates are permitted only when `status == ContractStatus.DRAFT`. Any attempt to modify a contract in `PENDING_SIGNATURE`, `SIGNED`, or `ACTIVE` state throws `IllegalStateException` (HTTP 400).
  2. Transactional locking: All status transitions and digital signing operations use `findContractByIdForUpdate` with pessimistic write locking.
  3. Automatic transition to `SIGNED` only executes once the count of distinct valid cryptographic signatures equals the number of active group members.
  4. Deletion permanently disallowed: HTTP `DELETE /contracts/{id}` is permanently blocked with HTTP 405 Method Not Allowed / 400 Bad Request to guarantee historical contract preservation.

---

## 9. Duplicate Signatures & Signer Impersonation
* **Risk**: A malicious user or duplicate network request submitting duplicate digital signatures, or a user signing a contract for an ownership group in which they hold no equity.
* **Mitigation Strategy**:
  1. Authorization & Membership check: Signer identity is resolved strictly from `UserPrincipal` (authenticated user). The system verifies that the user holds an active equity share in the contract's ownership group.
  2. Composite uniqueness: `contract_signatures` table enforces a composite unique constraint `UNIQUE (contract_id, user_id)`. The signature service also checks if the user has already signed and rejects duplicate submissions.
  3. Cryptographic digest verification: Each signature computes a SHA-256 hash incorporating contract ID, version, terms text, user ID, and timestamp, guaranteeing complete tamper-evidence.

---

## 10. Turnaround Buffer Scheduling Contention & Buffer Bleed-Over
* **Risk**: A 30-minute turnaround buffer between consecutive bookings could be violated if co-owners submit back-to-back reservations or if a prior user returns the vehicle late, bleeding over into the next reservation's turnaround buffer.
* **Mitigation Strategy**:
  1. Automated buffer expansion: The availability and conflict engines enforce an automated 30-minute post-booking buffer (`[startTime, endTime + 30m]`) in all overlap queries.
  2. Late return detection: When a trip is returned $>15$ minutes late, the check-out engine automatically logs a late return fee (50,000 VND / 30 min block) and notifies station dispatch to expedite cleaning/charging before the next booked trip.

---

## 11. Late QR Check-In & Automatic No-Show Race Condition
* **Risk**: A co-owner arriving at the station exactly at the 30-minute mark might experience a race condition where an automated background job or concurrent check-in attempt marks the booking as `NO_SHOW` while the user is physically scanning.
* **Mitigation Strategy**:
  1. Atomicity in QR validation: QR validation checks the time window $[startTime - 15\text{m}, startTime + 30\text{m}]$ under database transaction. If $>30$ minutes overdue, it transitions the booking to `NO_SHOW` within the same transaction, safely releasing the vehicle back to `AVAILABLE`.
  2. Time sync: Server time (`Instant.now()`) is authoritative; mobile/client timestamps are ignored in validation.

---

## 12. Telemetry Discrepancy & Offline Return Resolution
* **Risk**: Physical vehicle odometers or battery sensors reporting values inconsistent with user input (e.g., lower ending odometer than starting odometer, or SoC out of 0–100% range).
* **Mitigation Strategy**:
  1. Strict input validation: Check-out requests strictly reject ending odometers less than starting odometers (`endOdometer >= startOdometer`) and battery SoC outside $[0, 100]$.
  2. Telemetry warning logging: If start odometer is less than vehicle's registered odometer in the DB, a security warning is logged for station inspection without blocking legitimate user trips.

---

## 13. Gini Coefficient Drift with Infrequent Usage in Newly Formed Syndicates
* **Risk**: In newly formed ownership groups with few or zero historical trips, calculating fairness ratios and Gini coefficients could encounter division-by-zero errors or artificially high inequality metrics.
* **Mitigation Strategy**:
  1. Zero-safe mathematical guards: If total group usage is zero, each member's fairness ratio defaults safely to `1.0000` (`BALANCED` tier), and the group Gini coefficient defaults to `0.0000` (perfect equality).
  2. Time window scaling: The default evaluation window is 30 days, smoothing out short-term fluctuations.

---

## 14. Financial Transaction Rollback & Partial State Commits
* **Risk**: Complex operations touching multiple tables (`payments`, `shared_funds`, `fund_transactions`, `expense_allocations`) could leave orphan records or partially updated balances if a downstream service or network exception occurs mid-execution.
* **Mitigation Strategy**:
  1. Class-level and method-level `@Transactional(rollbackFor = Exception.class)` applied across all financial services (`PaymentServiceImpl`, `PaymentLifecycleServiceImpl`, `SharedFundServiceImpl`).
  2. Spring AOP proxy self-invocation bypass eliminated by keeping sub-methods public and calling through bean references where transactional boundaries are required.
  3. Integration tests explicitly verify that intentional runtime exceptions trigger complete rollbacks, leaving zero balance drift or uncommitted ledger records.

---

## 15. Concurrent Payment State Transitions & Webhook / Cancellation Race Conditions
* **Risk**: A user clicking "Cancel" simultaneously with an asynchronous payment gateway webhook delivering a "Success/Capture" notification could result in an inconsistent state or duplicate refund/credit actions.
* **Mitigation Strategy**:
  1. Pessimistic row locking on `Payment`: Status transitions acquire `@Lock(LockModeType.PESSIMISTIC_WRITE)` (`findByIdWithLock`) before reading the current state.
  2. Authoritative state machine guard: `PaymentStateMachine` enforces that only valid transitions are accepted. The first transaction to acquire the lock transitions the state (e.g. `PROCESSING -> SUCCESS`), and the competing transaction is rejected with `InvalidPaymentStateTransitionException` (HTTP 409 Conflict) upon attempting an illegal transition (e.g. `SUCCESS -> CANCELLED`).

---

## 16. Payment Gateway Network Retries & Idempotency Key Replay Attacks
* **Risk**: Network interruptions or repeated client checkout submissions can cause duplicate payments, while reusing an idempotency key with modified payment details could trick the system into fulfilling an altered order.
* **Mitigation Strategy**:
  1. Mandatory SHA-256 request payload fingerprinting: `IdempotencyService` computes `SHA-256(requestBody)` and persists it in `idempotency_records` alongside the key.
  2. Identical request replay: Requests with an identical key and identical hash return the cached HTTP response immediately without re-executing business logic.
  3. Tampered request rejection: Reusing a key with a different payload hash throws `IdempotencyConflictException` (HTTP 409 Conflict), preventing parameter manipulation.

---

## 17. Concurrent Voting Ballot Races & Duplicate Ballot Injection
* **Risk**: Multiple rapid clicks or concurrent client requests attempting to cast votes for the same co-owner on a single proposal could lead to double-counting equity weights or skewing quorum calculations.
* **Mitigation Strategy**:
  1. Service-level validation checks `voteRepository.findByProposalIdAndUserId(proposalId, userId)` prior to ballot insertion.
  2. Database-level composite uniqueness: The `votes` table enforces a composite unique constraint `uk_proposal_user_vote (proposal_id, user_id)`.
  3. Concurrent race conditions that bypass the application check trigger a `DataIntegrityViolationException`, which is caught and mapped to `DuplicateVoteException` (HTTP 409 Conflict), guaranteeing that exactly one ballot per co-owner is recorded.

---

## 18. Unauthorized Dispute Arbitration & State Transition Race Conditions
* **Risk**: Co-owners or unauthorized staff attempting to unilaterally settle disputes, or concurrent administrative requests attempting contradictory lifecycle transitions (e.g. concurrent escalation and resolution).
* **Mitigation Strategy**:
  1. Strict RBAC enforcement: Arbitration endpoints (`/arbitrate`, `/fund-adjustment`, `/arbitration-dossier`) enforce `@PreAuthorize("hasRole('ADMIN')")`. Staff attempts to transition disputes to `RESOLVED` are programmatically rejected with HTTP 403 Forbidden ("Staff members cannot perform final binding dispute arbitration. Final resolution is restricted to administrators.").
  2. Transactional locking: Dispute lifecycle mutations acquire pessimistic write locks (`findDisputeByIdForUpdate`) under `@Transactional`.
  3. Authoritative state machine: `DisputeStateMachine` rejects invalid transitions, reverse jumps, and mutations on terminal `RESOLVED` records with `InvalidDisputeStateTransitionException` (HTTP 409 Conflict).

---

## 19. Dispute Fund Adjustment Overdraft & Partial Settlement Inconsistency
* **Risk**: An administrative dispute resolution awarding a treasury reimbursement (`DEBIT`) could overdraft the syndicate's `SharedFund` vault if concurrent withdrawals reduce the balance, or an exception during ledger entry creation could leave a dispute marked `RESOLVED` without actual payment.
* **Mitigation Strategy**:
  1. Single atomic `@Transactional(isolation = Isolation.READ_COMMITTED, rollbackFor = Exception.class)` boundary spanning dispute status update (`RESOLVED`), `SharedFund` balance mutation, and `FundTransaction` ledger creation.
  2. Pessimistic row-level lock: Acquires `SELECT ... FOR UPDATE` via `sharedFundRepository.findByGroupIdWithLock(groupId)` before reading balance.
  3. Overdraft prevention: Validates `currentBalance >= adjustmentAmount` for `DEBIT` operations; throws `InsufficientFundBalanceException` (HTTP 400 Bad Request) on insufficient funds.
  4. Automatic rollback: Downstream errors trigger complete transaction rollback, preserving unchanged dispute status, untouched fund balance, and zero partial ledger records.
  5. Duplicate prevention: Validates that the dispute is not already `RESOLVED` and has no existing `fund_transaction_id`, rejecting duplicate adjustment requests with HTTP 409 Conflict.

---

## 20. Mobile GPU Thermal Throttling & DPR Scaling
* **Risk**: Rendering high-polygon 3D meshes with real-time PBR lighting and soft shadows on mobile devices with high-density Retina displays (e.g. 3x DPR on modern iPhones) causes rapid thermal throttling, severe frame drops, and heavy battery drain.
* **Mitigation Strategy**:
  1. `ResponsiveViewportController` and `usePerformanceStore` enforce a strict DPR clamp on mobile devices ($\le 1.25$ on mobile phones, $\le 1.5$ on tablets), reducing fragment shader load by up to $60\%$.
  2. Mobile screens automatically boot with `LOW` tier profile (shadows disabled, unneeded post-processing removed, aggressive geometry LOD reduction).

---

## 21. WebGL Context Loss on Mobile Browser Backgrounding
* **Risk**: When mobile users switch apps, receive phone calls, or lock their screens, mobile operating systems (iOS Safari, Android Chrome) aggressively terminate or suspend the WebGL context to conserve VRAM, causing unhandled renderer crashes when returning to the tab.
* **Mitigation Strategy**:
  1. `Canvas3DFoundation` binds directly to `webglcontextlost` and `webglcontextrestored` events on the HTML Canvas.
  2. The recovery store halts active render loops during context loss, frees pending texture uploads, and automatically reinitializes scene pipelines upon receiving `webglcontextrestored`.
  3. If context restoration fails, the cybernetic `RecoveryScreen` is presented with retry and Safe Mode recovery options.

---

## 22. Touch Gesture Conflicts with Native Browser Scrolling & Navigation
* **Risk**: Touch interactions in the 3D canvas (such as virtual joystick movement, pinch-to-zoom, or look drag) could inadvertently trigger native browser gestures, such as pull-to-refresh, pinch-zoom on the webpage DOM, or edge-swipe back navigation.
* **Mitigation Strategy**:
  1. The `#root` and `canvas` container enforce `touch-action: none; overflow: hidden; overscroll-behavior: none; user-select: none;` in `index.css`.
  2. `VirtualTouchJoystick` and `TouchGestureController` call `e.preventDefault()` and `e.stopPropagation()` on active touch events, locking input exclusively into the 3D spatial engine.

---

## 23. Direct Teleportation & Spatial Coordinate Tampering vs. Backend Security Boundaries
* **Risk**: Malicious client scripts modifying the local Zustand `useNavigationStore` or player coordinates `[X, Y, Z]` could bypass the physical laser barrier in the 3D world to enter restricted sectors (e.g. `ADMIN_COMMAND_CENTER` or `OPERATIONS_CENTER`) without proper credentials.
* **Mitigation Strategy**:
  1. Frontend navigation checks: `canAccessSector(sector, userRole)` strictly guards teleportation and camera transitions. Unauthorized teleport attempts are intercepted, resetting position to `SECURITY_CHECKPOINT` with visual access-denied pulses.
  2. Dual-Layer Security Invariant ("Frontend visibility is NOT security"): Regardless of client-side coordinate tampering, every API request (`apiClient.ts`) carries standard JWT Bearer headers validated by Spring Security. Endpoints enforce `@PreAuthorize("hasRole('ADMIN')")`, `@PreAuthorize("hasAnyRole('STAFF', 'ADMIN')")`, and ownership ACL expressions. Attempted unauthorized API calls are permanently rejected with HTTP 401/403.

---

## 24. 3D Raycasting Occlusion & Depth Buffer Z-Fighting in Densely Clustered Spatial Terminals
* **Risk**: Overlapping 3D meshes (such as holographic telemetry cards, 3D buttons, and digital twin undercarriages) sharing identical or near-identical Z-depth values can cause visual flickering (Z-fighting) and pointer raycasting mis-clicks.
* **Mitigation Strategy**:
  1. Layered Z-offsets: Micro-offsets ($\Delta Z \ge 0.005\text{m}$) are enforced across all SDF text elements, button faceplates, and backplates.
  2. Spatial UI Raycasting Priority: `UI3DManager` registers interactive buttons and terminals with explicit bounding volumes, filtering raycast intersections from front to back with hover debouncing to prevent flickering state oscillations.
  3. `polygonOffset` in Three.js materials applied to holographic decals and ground rings to ensure rendering precedence without depth contention.

---

## 25. Phase 09 Final Verification Resolution Audit Summary
* **Audit Execution**: Completed under Checkpoint `09-AB — FINAL VERIFICATION`.
* **Dimension Coverage**: All 20 audit dimensions rated **PASS** (100% compliance):
  - 13 sectors verified (12 metaverse environments + Security Checkpoint gateway).
  - Pure 3D requirement verified: Zero traditional navbars, sidebars, dashboard grids, CRUD pages, HTML modals as primary UI, or HTML overlays replacing 3D interaction.
  - 7 digital twin facets synchronized without independent fake truth.
  - Live real-browser E2E journey executed and recorded: `browser_e2e_09aa_1789651372549.webp`.
  - Zero runtime console errors, zero WebGL context faults, zero broken asset links.
  - 43 frontend test suites, 442 unit tests PASS (100%). Production bundle generated in 7.04s.
* **Resolution State**: All identified Phase 09 technical risks have been successfully mitigated. No blocking issues remain.
