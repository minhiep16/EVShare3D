# EVShare 3D – SYSTEM ARCHITECTURE SPECIFICATION

## 1. Architectural Philosophy: The Pure 3D Paradigm

EVShare 3D discards the antiquated pattern of a "2D website embedding a WebGL widget". Instead, it is architected as an **interactive 3D software application delivered over the web**. 

```text
┌─────────────────────────────────────────────────────────────────────────┐
│                           BROWSER CLIENT                                │
│  ┌───────────────────────────────────────────────────────────────────┐  │
│  │                    WebGL 2.0 Canvas (R3F)                         │  │
│  │  ┌───────────────────┐ ┌───────────────────┐ ┌─────────────────┐  │  │
│  │  │  3D Environments  │ │  Digital Twins    │ │   Spatial UI    │  │  │
│  │  │ (Garage, Rooms)   │ │  (Interactive EV) │ │ (3D Terminals)  │  │  │
│  │  └─────────┬─────────┘ └─────────┬─────────┘ └────────┬────────┘  │  │
│  │            └─────────────────────┼────────────────────┘           │  │
│  │                         Scene Graph                               │  │
│  └──────────────────────────────────┬────────────────────────────────┘  │
│                                     │                                   │
│  ┌──────────────────────────────────┴────────────────────────────────┐  │
│  │                  Client State & Interaction Layer                 │  │
│  │   • Camera Manager       • Raycast / Pointer Manager              │  │
│  │   • Zustand (3D UI State) • TanStack Query (Server Cache)         │  │
│  └──────────────────────────────────┬────────────────────────────────┘  │
└─────────────────────────────────────┼───────────────────────────────────┘
                                      │ REST API (JSON / JWT)
                                      ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                    SPRING BOOT BACKEND (JAVA 17+)                       │
│  ┌───────────────────────────────────────────────────────────────────┐  │
│  │               Security & API Layer (/api/v1)                      │  │
│  │   • Spring Security Filter Chain  • JWT Auth • RBAC Method Guards │  │
│  │   • Global Exception Handler     • Bean Validation                │  │
│  └──────────────────────────────────┬────────────────────────────────┘  │
│                                     │                                   │
│  ┌──────────────────────────────────┴────────────────────────────────┐  │
│  │                       Domain Services                             │  │
│  │   • VehicleService   • FairUsageService      • CostAllocationSvc  │  │
│  │   • BookingService   • VotingService         • AIRecommendSvc     │  │
│  └──────────────────────────────────┬────────────────────────────────┘  │
│                                     │                                   │
│  ┌──────────────────────────────────┴────────────────────────────────┐  │
│  │                  Persistence Layer (Spring Data JPA)              │  │
│  │   • Repositories     • ACID Transactions     • Audit Interceptors │  │
│  └──────────────────────────────────┬────────────────────────────────┘  │
└─────────────────────────────────────┼───────────────────────────────────┘
                                      │ JDBC / Connection Pool (HikariCP)
                                      ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                        MYSQL 8.0 DATABASE                               │
│  • InnoDB Engine  • Flyway Versioned Migrations  • Strict Constraints   │
└─────────────────────────────────────────────────────────────────────────┘
```

---

## 2. Frontend Architecture (React + TypeScript + Three.js + R3F)

### 2.1. Core Tech Stack
* **Framework**: React 18+ with TypeScript (Strict Mode).
* **Build Tooling**: Vite 5+ with HMR and multi-stage bundling.
* **3D Runtime**: Three.js (r160+) coupled with `@react-three/fiber` (R3F) v8+.
* **3D Helpers & Text**: `@react-three/drei` for loaders, SDF `Text`, environment lighting, and post-processing shaders (`@react-three/postprocessing`).
* **State Management**:
  * `Zustand`: Client-only real-time states (camera position, focus mode, 3D hover/active states, active room, virtual keyboard buffer, spatial audio triggers).
  * `TanStack Query (React Query v5)`: Server state caching, optimistic updates, background polling, and cache invalidation.

### 2.2. Directory Structure (`frontend/src/`)
```text
frontend/src/
├── api/                  # Axios / Fetch HTTP client with JWT interceptors
├── audio/                # 3D spatial audio buffers and sound managers
├── camera/               # Camera controller, transitions, lerp dampeners
├── config/               # App configuration, quality presets, environment vars
├── environments/         # High-level 3D scenes (Garage, Booking, Finance, etc.)
├── hooks/                # Custom React/Three hooks (useRaycast, useKeyboard3D)
├── interactions/         # Centralized Pointer, Drag, Focus, and Raycast managers
├── objects3d/            # Reusable 3D assets (Vehicles, Charging Stalls, Vaults)
├── scenes/               # Scene orchestrator and room level-of-detail (LOD)
├── services/             # Frontend business adapters and calculations
├── shaders/              # Custom GLSL shaders (Hologram, Energy rings, Scanlines)
├── stores/               # Zustand stores (useWorldStore, useAuthStore, useUI3DStore)
├── types/                # TypeScript type definitions and API contracts
├── ui3d/                 # Pure 3D UI components (ThreeDButton, ThreeDInput, etc.)
└── world/                # Main canvas container, scene graph, lights, postprocessing
```

### 2.3. Pure 3D UI System
The visual UI is composed entirely of 3D entities:
* **Geometry**: `BoxGeometry`, `CylinderGeometry`, `PlaneGeometry`, rounded spatial cards via custom procedural geometries.
* **Materials**: Physical Glass (`MeshPhysicalMaterial` with transmission, roughness, and metalness), Holographic Glow (custom vertex/fragment GLSL shaders), Carbon Fiber, Emissive Neon Indicators.
* **Text Rendering**: High-performance SDF text rendering via `@react-three/drei` `Text` and dynamic CanvasTextures for complex data plots.
* **Zero HTML Overlays**: No DOM elements float above the canvas. Modals, tooltips, keyboards, and buttons are spatial meshes positioned in 3D coordinate space.

---

## 3. Backend Architecture (Java + Spring Boot 3)

### 3.1. Core Tech Stack
* **Language & Runtime**: Java 17 (or Java 21 LTS).
* **Framework**: Spring Boot 3.2+.
* **Persistence**: Spring Data JPA with Hibernate 6.
* **Database Driver**: MySQL Connector/J with HikariCP high-performance connection pooling.
* **Migration**: Flyway for deterministic database version control.
* **Security**: Spring Security 6 with stateless JWT authentication filter.
* **Validation**: Jakarta Bean Validation (`hibernate-validator`).
* **Documentation**: OpenAPI 3.0 via `springdoc-openapi-starter-webmvc-ui`.
* **Testing**: JUnit 5, Mockito, AssertJ, Spring Boot Test, Testcontainers.

### 3.2. Package Organization (`com.example.evshare.*`)
```text
com.example.evshare
├── config/             # SecurityConfig, WebConfig, OpenApiConfig, JpaAuditConfig
├── controller/         # REST Controllers grouped by domain (/api/v1/...)
├── service/            # Domain service interfaces and @Service implementations
│   ├── impl/           # Concrete business logic, transaction boundaries
├── repository/         # Spring Data JPA interfaces with custom JPQL queries
├── entity/             # JPA entity models with relationship mappings
├── dto/                # Request / Response Data Transfer Objects (Records / Classes)
│   ├── request/
│   └── response/
├── security/           # JwtTokenProvider, JwtFilter, CustomUserDetailsService
├── exception/          # GlobalExceptionHandler, BusinessException, ResourceNotFound
├── validation/         # Custom annotations (e.g. @ValidOwnershipSum)
└── audit/              # EntityAuditListener, AuditLogService
```

### 3.3. Layered Design & Transaction Boundaries
* **Controllers** are thin adapters: validate incoming DTOs, extract `Authentication`, delegate to service, return unified `ApiResponse<T>`.
* **Services** enforce all business rules, execute fairness calculations, manage state transitions, and delimit `@Transactional` boundaries.
* **Repositories** interact with MySQL via strongly-typed Spring Data queries and native queries where strict row-locking (`SELECT ... FOR UPDATE`) is mandated.

---

## 4. Database Architecture (MySQL 8.0)

* **Engine**: InnoDB exclusively for foreign key enforcement, row-level locking, and crash recovery.
* **Character Set**: `utf8mb4` with collation `utf8mb4_unicode_ci`.
* **Data Precision**:
  * Monetary amounts: `DECIMAL(15, 2)` (never float or double).
  * Percentages (Equity share, battery SoC): `DECIMAL(5, 2)` with check constraints ($0.00 \le x \le 100.00$).
  * Temporal timestamps: `TIMESTAMP` with timezone normalized to UTC.
* **Migration Strategy**: Sequential Flyway scripts:
  * `V1__init_security_and_users.sql`
  * `V2__init_vehicles_and_ownership.sql`
  * `V3__init_bookings_and_usage.sql`
  * `V4__init_finance_funds_payments.sql`
  * `V5__init_governance_voting_disputes.sql`
  * `V6__init_ai_and_audit_logs.sql`

---

## 5. Security & Authentication Architecture

### 5.1. Authentication Flow
1. Co-Owner enters email and password at the 3D Virtual Terminal.
2. Client issues `POST /api/v1/auth/login`.
3. Spring Security validates credentials via `DaoAuthenticationProvider` and `BCryptPasswordEncoder`.
4. System issues a short-lived JWT Access Token (15 minutes) and a persistent, securely hashed Refresh Token (7 days).
5. All subsequent requests include `Authorization: Bearer <accessToken>`.
6. Client silently refreshes tokens via `POST /api/v1/auth/refresh` prior to expiration.

### 5.2. Role-Based Access Control (RBAC) & Ownership Scoping
* Standard roles: `ROLE_CO_OWNER`, `ROLE_STAFF`, `ROLE_ADMIN`.
* Controllers protected with `@PreAuthorize("hasRole('ADMIN')")` or custom SpEL expressions checking vehicle group membership:
  `@PreAuthorize("@ownershipSecurity.isGroupMember(#vehicleId, principal.id)")`.

---

## 6. API Architecture & Envelope Pattern

### 6.1. Unified Response Envelope (`ApiResponse<T>`)
```json
{
  "success": true,
  "message": "Operation completed successfully",
  "data": { ... },
  "timestamp": "2026-09-06T11:00:00Z"
}
```

### 6.2. RFC 7807 Error Envelope (`ApiErrorResponse`)
```json
{
  "success": false,
  "status": 409,
  "error": "CONFLICT",
  "message": "Booking slot overlaps with an existing confirmed reservation",
  "path": "/api/v1/bookings",
  "timestamp": "2026-09-06T11:00:00Z",
  "validationErrors": []
}
```

---

## 7. 3D Engine & Interaction Pipeline

1. **Raycasting & Interaction**: An efficient `RaycastManager` listens to pointer moves/clicks, selectively querying only objects registered on the interactive layer.
2. **Camera Director**: A tween-based camera manager interpolates the camera between navigation modes:
   * **Exploration Mode**: Smooth WASD / Orbit controls for walking the garage.
   * **Inspection Mode**: Smooth lerp to vehicle focus node when a car is selected.
   * **Terminal Mode**: Orthogonal/slanted lock-on when interacting with 3D input panels.
3. **Adaptive Level of Detail (LOD)**: Distant rooms are culled from rendering; complex vehicle meshes swap to simplified hulls when the camera moves away.
4. **Post-Processing Pipeline**: Selective bloom on emissive materials, screen space ambient occlusion (SSAO), subtle film grain, and chromatic aberration to generate a cyberpunk / futuristic automotive aesthetic.

---

## 8. Deployment Architecture (Docker & Microservices Readiness)

* Containerized via multi-stage `Dockerfile` configurations:
  * **Frontend**: Node 20 alpine build stage $\to$ NGINX alpine serving static bundles with gzip/brotli compression and SPA routing.
  * **Backend**: Eclipse Temurin JDK 17 build stage $\to$ lightweight JRE 17 runtime container with non-root user execution.
  * **Database**: Official MySQL 8.0 image with persistent volume mounts.
* Orchestrated with `docker-compose.yml` defining networks, secrets, healthchecks, and resource constraints.
