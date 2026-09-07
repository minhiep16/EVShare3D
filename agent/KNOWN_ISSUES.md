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
