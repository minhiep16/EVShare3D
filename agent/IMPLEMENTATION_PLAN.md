# EVShare 3D – MASTER IMPLEMENTATION PLAN

## 1. Plan Overview & Governance Principles

* **Application Model**: Pure 3D Interactive Web Application delivered over WebGL (React Three Fiber + Three.js).
* **Execution Boundary Rule**: Strictly sequential phase execution (PHASE 01 $\to$ PHASE 10). Agents must never skip ahead or create features belonging to subsequent phases.
* **Architecture Source of Truth**: All specifications in `/docs` and governance files in `/agent` represent the binding technical standards.

---

## 2. Phase-by-Phase Execution Specifications

### PHASE 01 – Specification & System Architecture
* **Objective**: Establish the complete, unified architectural and technical specifications for the entire EVShare 3D platform before any code implementation begins.
* **Scope**:
  * Functional & non-functional requirements (`docs/REQUIREMENTS.md`).
  * Strict business rules and mathematical invariants (`docs/BUSINESS_RULES.md`).
  * Full-stack architecture specification (`docs/ARCHITECTURE.md`).
  * RBAC matrix and security method guards (`docs/RBAC.md`).
  * REST API catalog and DTO schemas (`docs/API.md`).
  * Complete 27-table relational database design (`docs/DATABASE.md`).
  * 12 pure 3D metaverse environments and camera director (`docs/WORLD_ARCHITECTURE.md`).
  * Pure 3D UI component library, materials, and states (`docs/3D_DESIGN_SYSTEM.md`).
  * AI Mobility Intelligence algorithms and advisory constraints (`docs/AI_SPECIFICATION.md`).
  * Agent governance, status, ADRs, and risk register (`agent/*`).
* **Dependencies**: Master Prompt and project directives.
* **Expected Output**: 9 specification documents in `/docs` and 6 governance files in `/agent`.
* **Verification**: Run `node agent/verify-phase01.js` to assert cross-specification consistency across all 34 automated audit checks.
* **Completion Criteria**: 100% specification files substantive; zero contradictions; quality gate validated and signed off by user.

---

### PHASE 02 – Database & Backend Foundation
* **Objective**: Construct the Java Spring Boot 3 foundation, configure MySQL 8.0, and deploy deterministic Flyway database migrations.
* **Scope**:
  * Maven build descriptor (`backend/pom.xml`) with Java 17, Spring Boot 3.2+, JPA, Flyway, MySQL, Validation, Actuator, OpenAPI, Lombok.
  * Application configuration (`application.yml`) with HikariCP pool and `ddl-auto: validate`.
  * Flyway migrations (`V1` to `V7`) mapping all 27 tables defined in `docs/DATABASE.md`.
  * JPA Entity classes in `com.example.evshare.entity.*` with exact `BigDecimal` typing.
  * Standard API envelopes (`ApiResponse<T>`, `ApiErrorResponse`) and `GlobalExceptionHandler`.
  * Spring Data JPA repositories in `com.example.evshare.repository.*`.
  * Diagnostic endpoint (`/api/v1/health`) and OpenAPI 3 UI.
  * *Constraint*: Zero complex business services implemented.
* **Dependencies**: Approved `docs/DATABASE.md`, `docs/ARCHITECTURE.md`, `PHASE 01`.
* **Expected Output**: Operational backend project structure, Flyway SQL migrations, entity models, and health controller.
* **Verification**: `mvn clean test` succeeds against test profile; H2/MySQL migrations apply cleanly; health endpoint returns `status: UP`.
* **Completion Criteria**: Build passes; all 27 entities verified against database schema; Actuator and OpenAPI endpoints operational.

---

### PHASE 03 – Authentication & Authorization
* **Objective**: Implement secure authentication, JWT token lifecycle management, and role-based access control.
* **Scope**:
  * Spring Security 6 filter chain with stateless JWT authentication.
  * Dual-token model: 15-minute Access Token and 7-day Refresh Token with revocation tracking.
  * BCrypt password hashing (work factor 12).
  * Public endpoints: `/api/v1/auth/register`, `/api/v1/auth/login`, `/api/v1/auth/refresh`, `/api/v1/auth/logout`.
  * Authenticated user endpoint: `/api/v1/users/me`.
  * Method-level authorization guards (`@PreAuthorize`) enforcing `ROLE_CO_OWNER`, `ROLE_STAFF`, `ROLE_ADMIN`.
* **Dependencies**: `PHASE 02`, `docs/RBAC.md`, `docs/API.md`.
* **Expected Output**: Authentication controller, JWT provider, UserDetailsService, and security unit/integration tests.
* **Verification**: Integration tests verify: valid registration, duplicate email rejection, login token generation, refresh rotation, and 401/403 unauthorized access blocks.
* **Completion Criteria**: 100% auth tests pass; security context correctly populated; credentials never logged or exposed.

---

### PHASE 04 – Vehicle, Co-Ownership & Contract
* **Objective**: Implement the core co-ownership domain, digital twin vehicle state management, and multi-party contract signing.
* **Scope**:
  * `VehicleService`: Vehicle CRUD, real-time telemetry, state machine (`AVAILABLE`, `BOOKED`, `IN_USE`, `CHARGING`, `MAINTENANCE`, `DAMAGED`, `UNAVAILABLE`).
  * `OwnershipService`: Group formation, co-owner share assignments, right-of-first-refusal share transfers.
  * *Critical Invariant*: Atomic transaction validation guaranteeing $\sum \text{percentage} = 100.00\%$ with rollback on discrepancy.
  * `ContractService`: Multi-party agreement lifecycle (`DRAFT` $\to$ `PENDING_SIGNATURE` $\to$ `SIGNED` $\to$ `ACTIVE`), cryptographic SHA-256 signature digests.
  * REST Controllers under `/api/v1/vehicles`, `/api/v1/ownership-groups`, `/api/v1/contracts`.
* **Dependencies**: `PHASE 03`, `docs/BUSINESS_RULES.md`.
* **Expected Output**: Domain entities, services, controllers, DTOs, and transactional unit tests.
* **Verification**: Unit tests verify 100% equity validation (passes on 100%, fails on 99.99% or 100.01%), vehicle state transitions, and contract activation once all members sign.
* **Completion Criteria**: All equity calculation tests pass; contract state transitions behave deterministically; APIs documented in OpenAPI.

---

### PHASE 05 – Booking, Fair Usage & Vehicle Operation
* **Objective**: Build the vehicle reservation lifecycle, concurrency-safe conflict prevention, fair usage engine, and operational check-in/out.
* **Scope**:
  * `BookingService`: Reservation creation, modification, cancellation, and fee calculations.
  * Concurrency Protection: Pessimistic locking (`SELECT ... FOR UPDATE`) preventing overlapping booking intervals.
  * `FairUsageService`: Equity quota $Q_i$, weighted usage units $W_i$ (peak 1.5x, standard 1.0x, off-peak 0.7x), and imbalance classification (`FAIR`, `SLIGHTLY_IMBALANCED`, `IMBALANCED`, `SEVERELY_IMBALANCED`).
  * `UsageSessionService`: Check-in/check-out lifecycle, signed 5-minute QR token generation and validation.
  * Vehicle inspection logging: Odometers, battery SoC %, and 3D defect coordinate flags.
* **Dependencies**: `PHASE 04`, `docs/BUSINESS_RULES.md`.
* **Expected Output**: Booking and session services, QR token utility, fair usage scoring engine, and REST endpoints.
* **Verification**: Concurrent thread tests simulate simultaneous booking of same time slot; fair usage unit tests assert correct tier categorization; QR verification validates HMAC and expiry.
* **Completion Criteria**: Zero double-booking possible under race conditions; fair usage formula computes exact scores; check-in transitions vehicle state to `IN_USE`.

---

### PHASE 06 – Energy, Finance, Cost Allocation & Shared Fund
* **Objective**: Build the financial ledger, 3-strategy cost allocation engine, shared fund vault balance management, and payment gateway abstraction.
* **Scope**:
  * `ExpenseService`: Logging operating expenditures across categories (Charging, Maintenance, Insurance, Inspection, Cleaning).
  * `CostAllocationService`: Mathematical allocation models:
    1. `OWNERSHIP_BASED`: Fixed costs divided strictly by equity share.
    2. `USAGE_BASED`: Variable wear divided strictly by actual km / hours.
    3. `HYBRID`: 30% fixed by equity, 70% variable by distance.
  * `FundService`: Vault balance tracking, 10,000,000 VND reserve threshold monitoring, automatic capital call generation.
  * `PaymentService`: Payment transaction processing via provider abstraction (`BankTransfer`, `EWallet`, `MockGateway`).
* **Dependencies**: `PHASE 05`, `docs/BUSINESS_RULES.md`.
* **Expected Output**: Finance domain services, allocation calculators, vault ledger, payment adapters, and REST controllers.
* **Verification**: Tests assert that allocated shares sum up exactly to total invoice down to integer cents; fund balance updates atomically; low-liquidity warnings trigger correctly.
* **Completion Criteria**: All financial calculations execute with `BigDecimal`; payments update fund balances atomically; audit logs record transactions.

---

### PHASE 07 – Voting, Decision Chamber & Dispute Resolution
* **Objective**: Implement group governance, equity-weighted proposal voting, and dispute arbitration.
* **Scope**:
  * `VotingService`: Proposal lifecycle (`ACTIVE` $\to$ `PASSED` / `REJECTED` / `EXPIRED`).
  * Equity-weighted tallying based on active share percentage at vote time.
  * Quorum validation ($\ge 60.00\%$ participating equity required).
  * Passing thresholds: $>50\%$ for routine proposals, $\ge 75\%$ for major upgrades/amendments.
  * `DisputeService`: Dispute filing linked to usage sessions, evidence attachments, staff mediation, and admin arbitration with automated ledger settlement.
* **Dependencies**: `PHASE 06`, `docs/BUSINESS_RULES.md`.
* **Expected Output**: Proposal and dispute services, ballot calculators, arbitration workflows, and REST endpoints.
* **Verification**: Tests assert duplicate voting rejection; quorum failure forces `REJECTED` status; equity-weighted calculations match mathematical formulas.
* **Completion Criteria**: Quorum logic validated; voting results immutably recorded; dispute arbitration successfully executes financial balance adjustments.

---

### PHASE 08 – AI Recommendations, Analytics & Audit Logging
* **Objective**: Build the AI Mobility Intelligence Center algorithms and platform-wide immutable audit logging.
* **Scope**:
  * `AIRecommendationService`:
    * `FairUsageOptimizer`: Recommending schedule adjustments to restore equilibrium.
    * `BatteryHealthPredictor`: Fast-charge degradation trends and battery health alerts.
    * `MaintenancePredictor`: Mileage and brake/tire consumable service triggers.
    * `EnergyCostForecaster`: Seasonal energy tariff and monthly budget projections.
  * *Strict Boundary*: AI is strictly advisory. Zero silent mutations on bookings, contracts, finances, or shares.
  * `AuditService`: JPA entity mutation interceptor recording `old_state_json` and `new_state_json` to `audit_logs`.
* **Dependencies**: `PHASE 07`, `docs/AI_SPECIFICATION.md`.
* **Expected Output**: Predictive intelligence services, advisory REST endpoints, and JPA audit listeners.
* **Verification**: Tests verify advisory generation, user acknowledgment flow, and automated creation of audit log records on entity mutations.
* **Completion Criteria**: Advisory models produce structured JSON output; audit trails reflect before/after states; zero unauthorized database writes from AI.

---

### PHASE 09 – 3D Engine, 3D World & Full Backend Integration
* **Objective**: Build the entire Pure 3D Web Application in Vite + React Three Fiber, construct all 12 metaverse rooms, and bind every spatial interaction to genuine backend REST APIs.
* **Scope**:
  * Frontend project setup (`frontend/`): React 18, TypeScript, Three.js, R3F, Drei, Zustand, TanStack Query.
  * Spatial 3D UI component library: `ThreeDButton`, `ThreeDInput`, `ThreeDKeyboard`, `ThreeDPanel`, `ThreeDWindow`, `ThreeDTerminal`, `ThreeDChart`, `ThreeDSlider`, `ThreeDNotification`, `ThreeDPortal`.
  * 12 Metaverse Sectors: Security Checkpoint, EV Central Garage, Co-Ownership Hall, Booking Chamber, Finance Center, Shared Fund Vault, Digital Contract Room, Decision Chamber, AI Intelligence Center, Operations Center, Service Workshop, Dispute Room, Admin Command Center.
  * Camera Director: Smooth transitions across First-Person, Orbit, Inspection Focus, and Teleport modes.
  * Digital Twin EVs: Real-time telemetry reflection (battery SoC %, status, concentric equity rings).
  * API Integration: TanStack Query hooks connecting all 3D terminals to Spring Boot backend.
  * *Strict Constraint*: Zero 2D HTML dashboards, traditional navbars, or `<Html>` overlay main interfaces.
* **Dependencies**: `PHASE 08` (Complete backend operational), `docs/WORLD_ARCHITECTURE.md`, `docs/3D_DESIGN_SYSTEM.md`.
* **Expected Output**: Complete interactive 3D frontend communicating with real backend REST APIs.
* **Verification**: Browser E2E validation of complete user journey in pure 3D: Login $\to$ Garage $\to$ Vehicle Selection $\to$ 3D Timeline Booking $\to$ Check-in $\to$ Finance $\to$ Voting $\to$ AI.
* **Completion Criteria**: The removal test is satisfied (removing 3D canvas leaves no secondary 2D UI); 60 FPS sustained on desktop; all 12 rooms accessible; backend integration complete.

---

### PHASE 10 – Final Testing, Optimization & Deployment Audit
* **Objective**: Conduct comprehensive performance profiling, adaptive quality optimization, end-to-end testing, and Docker production packaging.
* **Scope**:
  * Performance Optimization: Draw call batching (`InstancedMesh`), frustum culling, LOD mesh switching, texture compression.
  * Adaptive Quality Manager: Real-time FPS monitoring scaling between `HIGH`, `MEDIUM`, and `LOW` presets.
  * End-to-end testing: Automated Vitest/Playwright browser interaction tests and backend JUnit integration tests.
  * Security Audit: JWT expiration, CORS policy, SQL injection prevention, rate limiting.
  * Dockerization: Multi-stage `Dockerfile` (frontend NGINX, backend JRE 17) and `docker-compose.yml` orchestrating MySQL, backend, and frontend.
  * Final audit report: `FINAL_AUDIT.md`.
* **Dependencies**: `PHASE 09`.
* **Expected Output**: Production-ready Docker Compose environment, test suite reports, and `FINAL_AUDIT.md`.
* **Verification**: `docker compose up --build` launches clean; all automated test suites pass with 0 failures; 60 FPS sustained on target hardware.
* **Completion Criteria**: Clean build across frontend and backend; Docker stack boots and functions end-to-end; final audit report signed off as `PASS`.
