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
