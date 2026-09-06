# EVShare 3D – Pure 3D Interactive EV Co-Ownership Platform

> **A Pure 3D Web Application for Electric Vehicle Co-Ownership, Real-time Digital Twins & Spatial Resource Sharing.**

---

## 1. Project Philosophy: The Pure 3D Paradigm

EVShare 3D eliminates traditional 2D dashboards, HTML sidebars, and Web2 admin cards. The **entire application experience physically exists inside a real-time WebGL 3D metaverse**.

* **No HTML Overlay Panels**: Modals, keyboards, buttons, charts, and forms are genuine 3D meshes rendered inside the Three.js scene graph.
* **Digital Twin at Center**: Electric Vehicles are interactive 3D digital twins reflecting real-time backend telemetry (battery SoC, odometer, active status, ownership equity rings).
* **The Removal Test**: If the 3D Canvas is removed from the DOM, the primary user experience ceases to exist.

---

## 2. Technology Stack

* **Frontend**:
  * React 18+ & TypeScript (Strict Mode)
  * Three.js & React Three Fiber (`@react-three/fiber`)
  * `@react-three/drei` (SDF 3D Typography, Environment, Shaders)
  * Zustand (Spatial 3D UI states, camera controls, audio triggers)
  * TanStack Query (Server state caching & background synchronization)
* **Backend**:
  * Java 17+ (LTS) & Spring Boot 3.2+
  * Spring Security 6 & Stateless JWT (Access + Refresh tokens)
  * Spring Data JPA & Hibernate 6
  * Bean Validation (Jakarta) & OpenAPI 3.0
* **Database**:
  * MySQL 8.0 (InnoDB Engine, strict decimal precision)
  * Flyway deterministic versioned migrations
* **Deployment**:
  * Docker & Docker Compose (Multi-stage builds)

---

## 3. Metaverse World Sectors (12 Spatial Environments)

```text
                                [ADMIN COMMAND CENTER]
                                          ▲
                                          │ Teleport Portal
                                          ▼
[OPERATIONS CENTER] ◄──► [EV CENTRAL GARAGE] ◄──► [AI INTELLIGENCE CENTER]
   │ (Staff Gate)             ▲         ▲                │
   ▼                          │         │                ▼
[SERVICE WORKSHOP]            │         │         [DECISION CHAMBER]
   │                          ▼         ▼
[DISPUTE ROOM]         [BOOKING CHAMBER] [CO-OWNERSHIP HALL]
                              │                 ▲
                              ▼                 │
                       [FINANCE CENTER] ◄───────┤
                              │                 │
                              ▼                 ▼
                     [SHARED FUND VAULT]  [DIGITAL CONTRACT ROOM]
```

1. **Security Checkpoint**: 3D Biometric Login Terminal with Spatial Virtual Keyboard.
2. **EV Central Garage**: Main showroom housing digital twin EVs and dynamic 3D equity rings.
3. **Co-Ownership Hall**: Co-owner identity pedestals and legal share certificates.
4. **Booking Chamber**: 3D cylindrical availability timeline and interactive reservation pegs.
5. **Energy & Finance Center**: Volumetric expense cubes and 3D payment terminal.
6. **Shared Fund Vault**: Cybernetic vault with glowing liquid balance column.
7. **Digital Contract Room**: Floating 3D holographic contracts and biometric signing pad.
8. **Decision Chamber**: Parliamentary debate arena with live quorum fluid cylinder.
9. **AI Mobility Intelligence Center**: Central neural sphere generating advisory insights.
10. **Operations Center**: Staff check-in/out station with 3D laser QR scanners.
11. **Service Workshop**: Automotive workshop featuring hydraulic 3D vehicle lift.
12. **Dispute Room**: Deliberation chamber with 3D evidence holotank.
13. **Admin Command Center**: Orbital control deck with 7 interactive Command Cores.

---

## 4. Documentation Index

Detailed engineering specifications are maintained in the [`/docs`](file:///e:/EVShare3D/docs) and [`/agent`](file:///e:/EVShare3D/agent) directories:

* [`docs/REQUIREMENTS.md`](file:///e:/EVShare3D/docs/REQUIREMENTS.md) – Functional (FR) & Non-Functional (NFR) Requirements.
* [`docs/BUSINESS_RULES.md`](file:///e:/EVShare3D/docs/BUSINESS_RULES.md) – 100% Equity invariant, Fair Usage formula, Booking overlap prevention.
* [`docs/ARCHITECTURE.md`](file:///e:/EVShare3D/docs/ARCHITECTURE.md) – Full-stack system architecture, pure 3D design, security layers.
* [`docs/RBAC.md`](file:///e:/EVShare3D/docs/RBAC.md) – Role-Based Access Control matrix for `CO_OWNER`, `STAFF`, `ADMIN`.
* [`docs/API.md`](file:///e:/EVShare3D/docs/API.md) – REST API specification with DTO schemas and RFC 7807 error envelopes.
* [`docs/DATABASE.md`](file:///e:/EVShare3D/docs/DATABASE.md) – Relational database schema with 25+ tables, foreign keys, and indexes.
* [`docs/WORLD_ARCHITECTURE.md`](file:///e:/EVShare3D/docs/WORLD_ARCHITECTURE.md) – 12 pure 3D metaverse environments and camera director.
* [`docs/3D_DESIGN_SYSTEM.md`](file:///e:/EVShare3D/docs/3D_DESIGN_SYSTEM.md) – Spatial UI primitives, PBR material palettes, 8-state interaction model.
* [`docs/AI_SPECIFICATION.md`](file:///e:/EVShare3D/docs/AI_SPECIFICATION.md) – AI Mobility Intelligence algorithms and advisory constraints.
* [`agent/IMPLEMENTATION_PLAN.md`](file:///e:/EVShare3D/agent/IMPLEMENTATION_PLAN.md) – Master 10-Phase execution roadmap and quality gates.
* [`agent/CURRENT_STATUS.md`](file:///e:/EVShare3D/agent/CURRENT_STATUS.md) – Project status and phase progression tracking.
* [`agent/DECISIONS.md`](file:///e:/EVShare3D/agent/DECISIONS.md) – Architecture Decision Records (ADRs).
* [`agent/KNOWN_ISSUES.md`](file:///e:/EVShare3D/agent/KNOWN_ISSUES.md) – Technical risks and mitigation strategies.
* [`agent/AGENTS.md`](file:///e:/EVShare3D/agent/AGENTS.md) – Autonomous agent guidelines and execution protocol.

---

## 5. Phased Roadmap

* **Phase 01**: Specification & System Architecture *(Completed)*
* **Phase 02**: Database & Backend Foundation
* **Phase 03**: Authentication & Authorization
* **Phase 04**: Vehicle, Co-Ownership & Contract
* **Phase 05**: Booking, Fair Usage & Vehicle Operation
* **Phase 06**: Energy, Finance, Cost Allocation & Shared Fund
* **Phase 07**: Voting, Decision Chamber & Dispute Resolution
* **Phase 08**: AI Recommendations, Analytics & Audit Logging
* **Phase 09**: 3D Engine, 3D World & Full Backend Integration
* **Phase 10**: Final Testing, Optimization & Deployment Audit
