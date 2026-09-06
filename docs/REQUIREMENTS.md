# EVShare 3D – SYSTEM REQUIREMENTS SPECIFICATION (SRS)

## 1. Executive Summary & Vision

**EVShare 3D** is a groundbreaking, next-generation Electric Vehicle (EV) Co-Ownership and Cost-Sharing Platform delivered as a **PURE 3D INTERACTIVE WEB APPLICATION**. 

Unlike conventional Web2 dashboards that merely decorate HTML cards with an optional 3D canvas or background widget, EVShare 3D places the **entire user experience inside a real-time WebGL virtual metaverse**. Every business capability—from user authentication and vehicle reservation to fractional ownership tracking, cost allocation, multi-party voting, and dispute arbitration—is embodied physically as spatial objects, interactive terminals, holographic displays, and digital twin vehicles.

If the 3D Canvas is removed, the primary application ceases to exist.

---

## 2. Actors & Stakeholder Profiles

### 2.1. CO_OWNER (Co-Owner / Member)
* **Description**: An individual or legal entity holding a fractional ownership stake (equity share) in one or more electric vehicles.
* **Core Responsibilities & Capabilities**:
  * Authenticate at the 3D Security Checkpoint.
  * Inspect owned vehicles via the 3D Digital Twin (battery state of charge, odometer, physical condition, active location).
  * Reserve vehicle time slots using the 3D Booking Chamber & Spatial Timeline.
  * Check-in and check-out of vehicles using spatial QR code terminals and telemetry loggers.
  * Inspect real-time cost breakdowns (charging, preventive maintenance, insurance, cleaning) in the 3D Energy & Finance Center.
  * Settle monthly cost shares and contribute capital to the Shared Fund Vault via 3D Payment Terminals.
  * Participate in group governance by casting ownership-weighted votes in the Decision Chamber.
  * Review and sign co-ownership agreements and addenda in the Digital Contract Room.
  * Receive personalized mobility recommendations and fair-usage insights from the AI Intelligence Core.
  * Raise condition or scheduling disputes in the 3D Dispute Room.

### 2.2. STAFF (Operations & Field Technician)
* **Description**: Garage managers, technicians, and inspection staff responsible for physical vehicle maintenance, turnaround, check-in validation, and charging logistics.
* **Core Responsibilities & Capabilities**:
  * Access the 3D Operations Center and Service Workshop.
  * Scan physical QR codes or verify co-owner check-in / check-out operations.
  * Perform and log vehicle condition inspections (battery health, tire wear, physical damage mapping on 3D vehicle geometry).
  * Transition vehicles into `MAINTENANCE`, `CHARGING`, or `DAMAGED` states.
  * Log actual service costs, invoices, and service notes into the finance subsystem.
  * Provide initial staff review for escalated disputes.

### 2.3. ADMIN (Platform Super Administrator)
* **Description**: Platform operators possessing global governance and supervisory privileges.
* **Core Responsibilities & Capabilities**:
  * Access the 3D Admin Command Center.
  * Manage global user directories (User Core), vehicle inventories (Vehicle Core), and co-ownership syndicates (Ownership Core).
  * Supervise contract templates, voting protocols, and financial audits.
  * Arbitrate unresolved disputes and issue final binding resolutions.
  * Monitor real-time platform metrics, system logs, and fairness distribution graphs across all vehicles.

---

## 3. Functional Requirements (FR)

### FR-01: Pure 3D Spatial Interface & Navigation
* **FR-01.1**: The application must run exclusively in a WebGL Canvas utilizing Three.js and React Three Fiber.
* **FR-01.2**: No HTML overlay panels, traditional navbars, sidebars, modal dialogs, or 2D CRUD tables shall serve as primary UI.
* **FR-01.3**: Navigation between functional areas (Garage, Finance, AI, Booking, Admin, Operations) must occur via spatial movement (WASD/click-to-move), 3D teleportation portals, or smooth camera transitions.
* **FR-01.4**: All text must be rendered in 3D space using SDF/3D text engines (`@react-three/drei` `Text` / Troika-three-text) or canvas-projected textures.

### FR-02: 3D Authentication & Identity
* **FR-02.1**: User login and registration must occur at a physical 3D Security Checkpoint.
* **FR-02.2**: The terminal must feature a proximity-triggered 3D holographic interface and a 3D interactive virtual keyboard.
* **FR-02.3**: Successful authentication via JWT must physically trigger gate/portal opening animations and camera transition into the EV Central Garage.

### FR-03: Digital Twin Vehicle Management
* **FR-03.1**: Each vehicle in the system must be rendered as an interactive 3D model reflecting real-time backend state.
* **FR-03.2**: Supported vehicle states: `AVAILABLE`, `BOOKED`, `IN_USE`, `CHARGING`, `MAINTENANCE`, `DAMAGED`, `UNAVAILABLE`.
* **FR-03.3**: Approaching a vehicle must trigger proximity highlights, glowing outlines, and floating 3D holographic specification plates (battery %, range, status, ownership distribution).
* **FR-03.4**: Clicking a vehicle must initiate camera focus, door opening animations, and contextual action menus (Inspect, Book, Service, Ownership).

### FR-04: Co-Ownership & Dynamic Equity Rings
* **FR-04.1**: Each vehicle must be linked to an Ownership Group where total equity shares equal exactly 100.00%.
* **FR-04.2**: Equity distribution must be visually rendered in 3D around the vehicle using concentric energy rings, spatial segments, or orbiting identity nodes.
* **FR-04.3**: The Co-Ownership Hall must allow co-owners to inspect peer profiles, historical usage ratios, and legal share certificates.

### FR-05: 3D Booking Chamber & Spatial Timeline
* **FR-05.1**: Reservations must be booked inside the 3D Booking Chamber.
* **FR-05.2**: Time slots must be rendered as a 3D cylindrical or horizontal spatial timeline with color-coded interactive time blocks.
* **FR-05.3**: The system must enforce strict concurrency control preventing overlapping bookings for the same vehicle.
* **FR-05.4**: Booking requests must calculate dynamic fairness impact prior to confirmation.

### FR-06: Fair Usage Engine & Chamber
* **FR-06.1**: A dedicated `FairUsageService` must calculate an objective fairness score and imbalance level for every co-owner based on ownership %, booked hours, peak vs. off-peak ratio, cancellation history, and mileage.
* **FR-06.2**: The 3D Fairness Chamber must visualize the delta between Ownership % vs. Actual Usage % using 3D comparative energy bars and spatial imbalance vectors.

### FR-07: Energy, Cost Allocation & Shared Fund
* **FR-07.1**: The 3D Energy & Finance Center must represent operating expenses (Charging, Maintenance, Insurance, Cleaning, Parking) as physical 3D data objects.
* **FR-07.2**: Expense allocation must support three configurable strategies:
  1. `OWNERSHIP_BASED`: Pro-rata by equity share.
  2. `USAGE_BASED`: Pro-rata by logged kilometers / operating hours.
  3. `HYBRID`: Fixed costs by ownership %, variable costs by usage %.
* **FR-07.3**: The Shared Fund Vault must physically visualize group pool balances, deposits, and reserve thresholds with 3D transaction ledgers.
* **FR-07.4**: Co-owners must settle dues at the 3D Payment Terminal supporting payment provider abstractions (Bank Transfer, E-Wallet, Card).

### FR-08: Decision Chamber & 3D Voting
* **FR-08.1**: Proposals (e.g., battery replacement, insurance renewal, adding co-owners) must appear as floating 3D holographic proposal pods.
* **FR-08.2**: Co-owners vote `APPROVE`, `REJECT`, or `ABSTAIN` via 3D biometric pedestal buttons.
* **FR-08.3**: Votes are weighted by equity percentage or equal tally depending on proposal policy. Real-time 3D particle streams visually feed the voting outcome cylinder.

### FR-09: Digital Contract Room
* **FR-09.1**: Legal co-ownership contracts, maintenance pacts, and amendments must be rendered as 3D holographic manuscripts.
* **FR-09.2**: Users can zoom, flip pages, review clauses, and digitally sign via 3D stylus/biometric pad, advancing contract state (`DRAFT` -> `PENDING_SIGNATURE` -> `SIGNED` -> `ACTIVE`).

### FR-10: Vehicle Check-In & Check-Out Operations
* **FR-10.1**: Operations Center must provide 3D Check-in and Check-out terminals.
* **FR-10.2**: Users or Staff scan spatial 3D QR codes verified cryptographically against the backend session.
* **FR-10.3**: Check-in/out captures mileage, battery %, and pinpoints physical condition/scratches directly on the 3D vehicle mesh.

### FR-11: Dispute Resolution Room
* **FR-11.1**: Discrepancies (unreported damage, late returns, dirty interior) appear as red glowing 3D dispute crystals.
* **FR-11.2**: Parties inspect 3D evidence, review check-out telemetry, and submit statements or counter-offers through 3D dispute terminals.

### FR-12: AI Mobility Intelligence Center
* **FR-12.1**: A central 3D AI Core surrounded by floating data nodes analyzes co-ownership trends, predicts battery degradation, and detects fairness anomalies.
* **FR-12.2**: Generates proactive recommendations displayed as 3D holographic cards (e.g., "Owner C should receive 4 priority hours this weekend").

### FR-13: Admin Command Center
* **FR-13.1**: Super-admins control the platform from a sci-fi command deck featuring 7 interactive 3D Cores: User Core, Vehicle Core, Ownership Core, Booking Core, Finance Core, Dispute Core, and System Core.

---

## 4. Non-Functional Requirements (NFR)

### NFR-01: Performance & Frame Rate
* The 3D scene must target a sustained **60 FPS** on standard modern desktop GPUs and at least **30 FPS** on integrated GPUs / mobile devices.
* Total draw calls per scene must not exceed 150 through instancing, texture atlasing, and geometry merging.
* Initial bundle download must utilize code-splitting and asset lazy-loading.

### NFR-02: Adaptive Quality & Scalability
* Automatic performance detection must dynamically scale render quality between `HIGH`, `MEDIUM`, and `LOW`:
  * `HIGH`: Full post-processing (Bloom, SSAO, Vignette), dynamic point shadows, high-res textures.
  * `MEDIUM`: Simplified lighting, disabled ambient occlusion, lowered shadow map resolution.
  * `LOW`: Unshadowed lighting, disabled post-processing, simplified geometry, reduced particle density.

### NFR-03: Security & Data Integrity
* All API communication must be guarded with JWT (access + refresh tokens) and role-based access control (RBAC).
* Backend authorization is authoritative; client-side visual states are never trusted for security enforcement.
* Passwords must be hashed using BCrypt (work factor 12) or Argon2id.
* Database operations affecting equity percentages (100.00%) and fund balances must execute inside ACID transactions with pessimistic or optimistic locking.

### NFR-04: Responsive Spatial Design
* Mobile and touch devices must receive a fully 3D experience with adapted controls (virtual touch joysticks, enlarged touch targets, simplified camera navigation), NOT a fallback 2D website.

### NFR-05: WebGL Compatibility & Fallback
* If WebGL 2.0 is completely unsupported or hardware-accelerated graphics are disabled, the system displays a dedicated 3D-styled error environment informing the user of required browser/GPU features. It will not silently degrade into a 2D CRUD website.

### NFR-06: Auditability
* Every financial transaction, vote cast, contract signature, vehicle state transition, and dispute resolution must be immutably recorded in the `audit_logs` table.
