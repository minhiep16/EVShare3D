# EVShare 3D – CURRENT PROJECT STATUS

## 1. Active Phase & Checkpoint
* **CURRENT_PHASE**: `PHASE 10 — FINAL TESTING, OPTIMIZATION & DEPLOYMENT`
* **PHASE 10 STATUS**: **`COMPLETE`**
* **CURRENT_CHECKPOINT**: `10-R — FINAL AUDIT REPORT`
* **CHECKPOINT 10-R STATUS**: **`COMPLETE`** (`FINAL_AUDIT.md` created with all 24 required sections; all verifications marked PASS; final system status evaluated as `PASS_WITH_MINOR_ISSUES`; implementation workflow successfully concluded)
* **CHECKPOINT 10-Q STATUS**: **`COMPLETE`** (agent/KNOWN_ISSUES.md created and updated with transparent, rigorous classification across BLOCKER [0], HIGH [2], MEDIUM [3], and LOW [3] with full reproduction, impact, status, workaround, and recommended future fix specifications; zero hidden failures)
* **CHECKPOINT 10-O STATUS**: **`COMPLETE`** (Pristine clean-slate run verified via `docker compose down -v` and `up -d`; clean MySQL volume initialized, all 13 Flyway migrations V1..V13 executed cleanly with success=1, backend started in prod mode, frontend Nginx started, reverse proxy communication confirmed, user registration and JWT login verified, 3D world canvas rendered with 0 console errors and 0 404 broken assets, critical APIs verified with 100% success; 0 failures recorded; recorded in clean_env_verify_1789901178664.webp)
* **CHECKPOINT 10-N STATUS**: **`COMPLETE`** (Production Docker configuration verified; frontend [evshare-frontend:latest, 96MB Nginx alpine, multi-stage], backend [evshare-backend:latest, 493MB JRE 17 jammy, unprivileged user], MySQL 8.0, and docker-compose.yml with healthchecks, persistent volume evshare_mysql_data, and evshare-network bridge built and validated; zero hard-coded secrets)
* **CHECKPOINT 10-M STATUS**: **`COMPLETE`** (Asset audit completed across 3D models, procedural geometries, textures, materials, compression, lazy loading, AssetCache memoization, LODMesh distance thresholds, InstancedProps batching, and 0 unused assets; visual fidelity 100% preserved)
* **CHECKPOINT 10-L STATUS**: **`COMPLETE`** (Full performance audit completed; measured FPS 58-60 across HIGH/MEDIUM/LOW tiers, frame time ~16.6-17.2ms, API latency <30ms steady state, bundle size 1.83MB JS / 59.3MB JAR; documented in docs/PERFORMANCE_AUDIT.md and recorded in perf_audit_run_1789895003582.webp)
* **CHECKPOINT 10-K STATUS**: **`COMPLETE`** (Mobile/tablet 3D touch verified at 820x1180; 100% WebGL canvas preserved with zero 2D downgrade; virtual touch joystick, 3-axis camera orbit/1st/3rd person, raycast tap selection, adaptive FOV & DPR, and sector portals verified; hardware WebXR HMD advisory documented; recorded in mobile_touch_audit_1789893451056.webp)
* **CHECKPOINT 10-J STATUS**: **`COMPLETE`** (Critical user journey executed end-to-end: Open → 3D Boot → Login [Failure/Success] → Garage → Select EV → Ownership → Booking → Usage → Finance → AI → Return; verified network requests, Spring Boot responses, MySQL persistence, 3D visual updates, 0 broken interactions; recorded in browser_e2e_10j_1789892026952.webp)
* **CHECKPOINT 10-I STATUS**: **`COMPLETE`** (Browser session recording pure_3d_audit_1789889703525.webp verifies 100% WebGL Three.js spatial environments; 0 navbars, 0 sidebars, 0 CRUD pages, 0 HTML modals as primary interaction; all 16 primary workflows verified in 3D)
* **CHECKPOINT 10-H STATUS**: **`COMPLETE`** (All 17 critical business rules audited, mathematically verified, cross-referenced with backend FSMs/services, and validated across 86 automated business rule tests with 100% PASS)
* **CHECKPOINT 10-G STATUS**: **`COMPLETE`** (All 104 OpenAPI paths audited across 12 modules; response envelopes, error schemas, pagination, idempotency, and RBAC verified against docs/API.md)
* **CHECKPOINT 10-F STATUS**: **`COMPLETE`** (All 14 security dimensions verified; 45 security test cases 100% PASS; IDOR defense, RBAC, JWT rotation, anti-enumeration, and zero frontend bypass confirmed)
* **CHECKPOINT 10-E STATUS**: **`COMPLETE`** (Clean DB migration V1..V13 tested & verified; 0 checksum issues; FK ordering valid; repeatability confirmed)
* **CHECKPOINT 10-D STATUS**: **`COMPLETE`** (MySQL 8.0 schema verified; all 28 tables, PKs, FKs, unique/check constraints, audit fields inspected; 1 doc discrepancy recorded)
* **CHECKPOINT 10-C STATUS**: **`COMPLETE`** (`mvn clean test` 1,145/1,145 PASS; `mvn package` generated 59.3MB executable JAR; 0 compilation errors; 0 dependency conflicts)
* **CHECKPOINT 10-B STATUS**: **`COMPLETE`** (`npm install`, `typecheck`, `test`, `build` all 100% PASS)
* **CHECKPOINT 10-A STATUS**: **`COMPLETE`** (`docs/FINAL_REPOSITORY_AUDIT.md` created)
* **PREVIOUS_PHASE**: `PHASE 09 — PURE 3D WORLD & FULL INTEGRATION` (COMMITTED & PUSHED at `435692b`)
* **CHECKPOINT 09-AB STATUS**: **`COMPLETE`**
* **CHECKPOINT 09-AA STATUS**: **`COMPLETE`** (Real Browser E2E: 14-step journey verified with WebP recording)
* **CHECKPOINT 09-Z STATUS**: **`COMPLETE`** (Role-Based World Access & Perimeter Defense)
* **CHECKPOINT 09-Y STATUS**: **`COMPLETE`** (AI API Integration & Honest NOT_AVAILABLE Disclosure)
* **CHECKPOINT 09-X STATUS**: **`COMPLETE`** (Dispute Room & RBAC Arbitration)
* **CHECKPOINT 09-W STATUS**: **`COMPLETE`** (Voting & Authoritative Decision Chamber)
* **CHECKPOINT 09-V STATUS**: **`COMPLETE`** (Digital Contract Room & Holographic Signatures)
* **CHECKPOINT 09-U STATUS**: **`COMPLETE`** (Finance Center, Vault & Payment Terminal)
* **CHECKPOINT 09-T STATUS**: **`COMPLETE`** (Booking Chamber & Chrono-Spatial Timeline)
* **CHECKPOINT 09-S STATUS**: **`COMPLETE`** (Vehicle Digital Twin Full Integration)
* **CHECKPOINT 09-R STATUS**: **`COMPLETE`** (Central Garage Integration)
* **CHECKPOINT 09-Q STATUS**: **`COMPLETE`** (Security Checkpoint Auth Integration)
* **CHECKPOINT 09-P STATUS**: **`COMPLETE`** (Digital Twin Store & Synchronization)
* **CHECKPOINT 09-O STATUS**: **`COMPLETE`**
* **CHECKPOINT 09-N STATUS**: **`COMPLETE`**
* **CHECKPOINT 09-M STATUS**: **`COMPLETE`**
* **CHECKPOINT 09-L STATUS**: **`COMPLETE`**
* **CHECKPOINT 09-K STATUS**: **`COMPLETE`**
* **CHECKPOINT 09-J STATUS**: **`COMPLETE`**
* **CHECKPOINT 09-I STATUS**: **`COMPLETE`**
* **CHECKPOINT 09-H STATUS**: **`COMPLETE`**
* **CHECKPOINT 09-G STATUS**: **`COMPLETE`**
* **CHECKPOINT 09-F STATUS**: **`COMPLETE`**
* **CHECKPOINT 09-E STATUS**: **`COMPLETE`**
* **CHECKPOINT 09-D STATUS**: **`COMPLETE`**
* **CHECKPOINT 09-C STATUS**: **`COMPLETE`**
* **CHECKPOINT 09-B STATUS**: **`COMPLETE`**
* **CHECKPOINT 09-A STATUS**: **`COMPLETE`**
* **PREVIOUS_PHASE**: `PHASE 08 — PURE 3D ENGINE & 3D DESIGN SYSTEM` (VERIFIED / COMMITTED / PUSHED at `1e299b3`)
* **Frontend Verification (`npm test -- --run && npx tsc --noEmit && npm run build`)**: **`BUILD & TEST SUCCESS`** (43/43 unit test suites, 442/442 unit tests PASS; 0 errors across TypeScript typecheck and Vite production build in 7.04s)
* **Summary of Checkpoint 09-AB (Final Verification & Comprehensive 20-Point System Audit)**:
  - Conducted complete, rigorous final verification of Phase 09 across all 20 requirement dimensions:
    1. **All 12 Metaverse Environments (+ Security Gate = 13 Sectors)**: `SECURITY_CHECKPOINT`, `CENTRAL_GARAGE`, `CO_OWNERSHIP_HALL`, `BOOKING_CHAMBER`, `ENERGY_FINANCE_CENTER`, `SHARED_FUND_VAULT`, `DIGITAL_CONTRACT_ROOM`, `DECISION_CHAMBER`, `AI_INTELLIGENCE_CENTER`, `OPERATIONS_CENTER`, `SERVICE_WORKSHOP`, `DISPUTE_ROOM`, and `ADMIN_COMMAND_CENTER`. All 13 registered in `SceneRegistry` and rendered within `WorldRoot.tsx`.
    2. **Pure 3D Requirement (Strict Negative Invariant)**: Verified zero traditional navbars, sidebars, dashboard grids, CRUD pages, HTML modals as primary UI, or HTML overlays replacing 3D interaction. 100% of primary interactions execute as Three.js WebGL objects using raycasting, SDF text, and tactile 3D mesh depression.
    3. **Digital Twins**: Verified 4-stage unidirectional pipeline ($\text{Backend} \rightarrow \text{Store} \rightarrow \text{Facets} \rightarrow \text{Mesh}$) across 7 facets (Battery, Status, Ownership, Booking, Usage, Maintenance, Finance) with zero fake truth.
    4. **API Integration**: Complete `apiClient.ts` integration across all 12 REST API modules with RFC 6819 refresh queue, token renewal, and `ApiResponse<T>` unwrapping.
    5. **Authentication**: Stateless dual-token JWT (15-minute access, 7-day refresh) integrated into 3D holographic security gate console and backend `authApi`.
    6. **RBAC**: Dual-layer defense model ("Frontend visibility is NOT security"): Spring Security `@PreAuthorize` backend gating, `canAccessSector` in `useNavigationStore.ts`, in-world red laser portal rejection, and boundary collision bounce.
    7. **Vehicle Subsystem**: 7-state authoritative FSM, pessimistic locking, Superchargers, and in-world 3D inspection terminal.
    8. **Ownership Subsystem**: Absolute 100.00% equity invariant (`BR-OWN-01`), `BigDecimal` Banker's rounding, and cryptographic cap table audit.
    9. **Booking Subsystem**: 30-minute automated turnaround buffer (`BR-BKG-02`), interval overlap rejection, and 3D Chrono-Timeline helix.
    10. **Usage Subsystem**: 15-minute TTL QR verification, check-in/out, odometer/battery validation, and late return penalty logging.
    11. **Finance Subsystem**: 3 expense allocation strategies (Ownership, Usage, Hybrid), residual penny distribution, and safety reserve floor (`BR-FIN-03`).
    12. **Payment Subsystem**: Multi-channel 3D payment kiosk (VietQR, E-Wallet, NFC), idempotency key fingerprinting, and zero fake financial success.
    13. **Contract Subsystem**: Multi-page 3D lectern, version tree (`v1.0`, `v1.1`, `v2.0`), SHA-256 cryptographic signatures, and permanent rejection of `DELETE`.
    14. **Voting Subsystem**: 60.00% quorum threshold, weighted equity ballots, authoritative backend calculations, and duplicate vote rejection.
    15. **Dispute Subsystem**: 3D defect coordinate holotank, immutable evidence attachments, staff mediation desk, and admin binding arbitration with atomic `SharedFund` deductions.
    16. **AI Subsystem**: Honest and transparent `NOT_AVAILABLE` disclosure when model is unreachable, advisory-only representation, and hard safety interlock (`BR-AI-SAFE-01`).
    17. **Staff Operations**: Dedicated hangar, maintenance bay with hydraulic lift, OBD-II diagnostic scanner, spare parts rack, and dispute mediation console.
    18. **Admin Command Center**: Orbital panopticon deck ($Y=25\text{m}$) housing the 7 conceptual cores with sovereign lockdown and cap table audits.
    19. **Browser E2E**: Live real-browser session verified the complete 14-step user journey (`Open → 3D Boot → Login → Garage → Vehicle → Ownership → Booking → Usage → Finance → Contract → Voting → Dispute → AI → Return`), recorded as `browser_e2e_09aa_1789651372549.webp` with status PASS.
    20. **Console Errors**: 0 JavaScript runtime errors, 0 WebGL crashes, 0 404 broken assets. 442/442 unit tests PASS; production build completes in 7.04s.
  - Zero new features introduced (strict Phase 09 boundary).
  - Directive: STOP. Do not start Phase 10. Awaiting explicit user command.
* **Summary of Checkpoint 09-O (Vehicle Digital Twin Synchronization Layer)**:
  - Designed and implemented the authoritative synchronization layer between backend Vehicle domain models and 3D vehicle representations in `frontend/src/digitalTwin/`:
    - Strict Unidirectional Pipeline: $\text{Backend Response} \longrightarrow \text{Application State} \longrightarrow \text{Digital Twin State} \longrightarrow \text{Visual Update}$.
    - Prohibition of Independent Fake Truth: The 3D vehicle does not maintain independent fake truth; it derives geometry, lighting, status indicators, and holographic HUDs directly from the authoritative store (`useDigitalTwinStore`).
    - 7 Conceptual Facets Verified:
      1. **Battery Facet**: Live state of charge (SoC %), charging power (kW), charging state (`IDLE`, `CHARGING`, `DISCHARGING`), remaining range (km), battery temperature ($^\circ\text{C}$), and state of health (SoH %). Dynamically illuminates the 3D undercarriage battery tray.
      2. **Status Facet**: Operational status (`AVAILABLE`, `RESERVED`, `IN_USE`, `MAINTENANCE`, `CHARGING`, `OFFLINE`), remote door lock indicator, ignition readiness, physical bay/stall location, and audit reason trail. Dynamically tints the 3D ground ring beacon.
      3. **Ownership Facet**: Co-ownership syndicate name, total members count, user equity share %, calculated voting power %, and cryptographic cap table hash.
      4. **Booking Facet**: Active reservation ID, reserved member name, booking window start/end timestamps, and turnaround conflict status.
      5. **Usage Facet**: Active telemetry session ID, driver name, odometer (km), real-time velocity, speed limit compliance, and structural damage flags.
      6. **Maintenance Facet**: Overall vehicle condition grade, open work order ID, inspection timestamp, OBD-II DTC diagnostic trouble codes, and subsystem status matrix (Brakes, LiDAR ADAS, HV Battery, Suspension, Thermal Inverter).
      7. **Finance Facet**: Co-ownership SharedFund vault balance (VND), outstanding expense liabilities, operating cost per km, deposit status, and safety reserve ratio.
    - Components & Modules:
      - `digitalTwinTypes.ts`: Domain models for all 7 facets, camera presets, and digital twin vehicles.
      - `useDigitalTwinStore.ts`: Authoritative Zustand store maintaining baseline vehicles, facet mutation actions, and backend re-synchronization (`syncFromBackend`, `syncAllFromBackend`).
      - `DigitalTwinVehicle3D.tsx`: Procedural 3D WebGL Vehicle that visually responds to all 7 facets (paint finish, emissive ground aura, undercarriage battery glow, lock indicator, and an interactive 3D Holographic Telemetry Inspector with 7 selectable 3D tabs and a live `[ 🔄 RE-SYNC DIGITAL TWIN FROM BACKEND ]` button).
      - `frontend/src/garage/DigitalTwinVehicle3D.tsx`: Refactored to bind directly to `useDigitalTwinStore`, eliminating disconnected mock properties.
  - Automated Unit Tests:
    - Created `frontend/src/digitalTwin/digitalTwin.test.ts` (12/12 tests PASS) verifying the 4-stage pipeline, all 7 individual facets, live backend sync, and rejection of fake truth.
    - Vitest test suite: **31 test files, 315/315 unit tests PASS**.
    - TypeScript typecheck: **0 errors**.
    - Vite build: Production bundle generated in 5.81s.
  - Visual Browser Verification:
    - Executed automated browser session navigating to `http://localhost:4173/?sector=CENTRAL_GARAGE`, verifying 3D digital twin vehicles, telemetry hologram with 7 facet tabs, and live re-synchronization. Captured recording `digital_twin_verify_1789529715214.webp` and screenshots.
* **Summary of Checkpoint 09-N (Admin Command Center & 7 Conceptual Cores)**:
  - Designed and built the complete ADMIN-only 3D environment in `frontend/src/admin/` strictly adhering to the Pure 3D mandate (zero 2D HTML forms, overlays, or traditional admin dashboards) and mandatory backend authorization (`ROLE_ADMIN` exclusivity):
    - `adminTypes.ts`: Domain models for the 7 conceptual cores (`AdminCoreId`), telemetry records, camera presets, and administrative state.
    - `adminLayout.ts`: Geometric layout at sector coordinates `[0, 25, 0]` (`SECTOR_CENTERS.ADMIN_COMMAND_CENTER`), environment profile `COMMAND_HORIZON` (`#38bdf8`, `#fbbf24`, dark obsidian `#020617`), and 8 camera presets (`ORBITAL_OVERVIEW`, `USER_CORE_FOCUS`, `VEHICLE_CORE_FOCUS`, `OWNERSHIP_CORE_FOCUS`, `BOOKING_CORE_FOCUS`, `FINANCE_CORE_FOCUS`, `DISPUTE_CORE_FOCUS`, `SYSTEM_CORE_FOCUS`).
    - `useAdminStore.ts`: Reactive Zustand store tracking live states for all 7 cores, simulated user role (`ROLE_CO_OWNER`, `ROLE_STAFF`, `ROLE_ADMIN`), strict RBAC lockout enforcement, and mutation actions with offline resilience.
    - `AdminOrbitalFloor3D.tsx`: Translucent obsidian glass deck ($13.5\text{m}$ radius) elevated at altitude $Y=25\text{m}$ overlooking the central garage and plaza below, with 7 radial energy buslines, peripheral guardrail balustrades, and sky-bridge entrance.
    - `UserCore3D.tsx`: Core 1 (North `[0, 0, -5.4]`) with levitating biometric polyhedral nucleus, live account readouts, and 3D interactions: `[ 🛡 VERIFY & SIGN KYC CREDENTIALS ]`, `[ ⛔ SUSPEND USER / ⚡ ACTIVATE USER ]`, and `[ 🔑 PROMOTE TO STAFF ]`.
    - `VehicleCore3D.tsx`: Core 2 (North-East `[4.8, 0, -2.6]`) with floating wireframe fleet prism, telemetry diagnostics, and 3D interactions: `[ 🚨 REMOTE EMERGENCY VEHICLE LOCKDOWN ]`, `[ 🔄 SYNC TELEMATICS ]`, and `[ 🛠 DISPATCH WORKSHOP ]`.
    - `OwnershipCore3D.tsx`: Core 3 (South-East `[4.8, 0, 2.6]`) with dual-torus equity balance rings, syndicate cap table stats, and 3D interactions: `[ 🔒 FREEZE SYNDICATE TRANSFERS ]` and `[ 📜 CRYPTOGRAPHIC CAP TABLE AUDIT ]`.
    - `BookingCore3D.tsx`: Core 4 (South `[0, 0, 5.4]`) with chrono-spatial calendar helix, scheduling dispute readouts, and 3D interactions: `[ ⏱ ARBITRATE SCHEDULE CONFLICT ]`, `[ ⛔ PREEMPT SLOT ]`, and `[ ⚡ PURGE HOLDS ]`.
    - `FinanceCore3D.tsx`: Core 5 (South-West `[-4.8, 0, 2.6]`) with levitating golden treasury octahedron, vault reserve statistics, and 3D interactions: `[ 💰 INJECT LIQUIDITY (+2,000,000 VND) ]`, `[ 🛑 FREEZE OUTFLOW ]`, and `[ 🔍 AUDIT VAULT ]`.
    - `DisputeCore3D.tsx`: Core 6 (North-West `[-4.8, 0, -2.6]`) with floating crimson arbitration crystal, docket details, and 3D interactions: `[ ⚖ ENFORCE SUMMARY ARBITRATION VERDICT ]` and `[ 💸 GRANT COMPENSATORY CREDIT ]`.
    - `SystemCore3D.tsx`: Core 7 (Center Zenith `[0, 1.8, 0]`) with floating crystalline master sphere, rotating orbital rings, node health telemetry, WebGL throughput readouts, and 3D interactions: `[ 🚨 TRIGGER GLOBAL PLATFORM EMERGENCY LOCKDOWN ]` and `[ 🔄 FLUSH SYSTEM CACHES & SHADER PIPELINES ]`.
    - `AdminCommandCenter3D.tsx`: Composed sector master module mounted in `WorldRoot.tsx` and registered as `DEFAULT_ADMIN_COMMAND_CENTER_SCENE` in `defaultScenes.ts`, featuring a floating overhead sovereign notice ribbon, 8 camera quick-teleport buttons, and an in-scene 3D RBAC role switcher.
  - Backend API Integration (`adminApi.ts`):
    - REST client connecting to Spring Boot backend (`UserController`, `VehicleController`, `OwnershipGroupController`, `BookingController`, `SharedFundController`, `DisputeController`, `HealthController`, `AnalyticsController`).
  - Mandatory Backend Authorization & RBAC Enforcement:
    - Exclusively accessible to `ROLE_ADMIN`. Co-Owners and Staff attempting any administrative operation encounter sovereign lockout rejections (`SOVEREIGN LOCKOUT (403)`).
  - Automated Unit Tests:
    - Created `frontend/src/admin/admin.test.ts` (24/24 tests PASS) validating panopticon coordinates, all 7 conceptual cores, RBAC security lockouts, and live core mutations.
    - Updated `scene.test.ts` to expect 13 registered scenes (13/13 scenes verified).
    - Vitest test suite: **30 test files, 303/303 unit tests PASS**.
    - TypeScript typecheck: **0 errors**.
    - Vite build: Production bundle generated in 7.96s.
  - Visual Browser Verification:
    - Executed automated browser session navigating to `http://localhost:4173/?sector=ADMIN_COMMAND_CENTER`, verifying 3D panopticon deck, 7 conceptual cores, environment lighting, and 0 console errors. Captured screenshots `admin_overview_1789528888745.png`, `admin_cores_interactive_1789528913173.png`, and recording `admin_command_verify_1789528815347.webp`.
  - Designed and built the complete 3D Dispute Room and Arbitration Chamber in `frontend/src/dispute/` strictly adhering to the Pure 3D mandate (zero 2D HTML forms, overlays, or modal dialogs) and strictly enforcing backend RBAC permissions:
    - `disputeTypes.ts`: Domain models for `DisputeStation`, `DisputeTab`, `DefectMarker3D`, `ArbitrationVerdictChoice`, `DisputeCameraPreset`, and `DisputeState`.
    - `disputeLayout.ts`: Sector coordinates `[-40, 0, 80]` (`SECTOR_CENTERS.DISPUTE_ROOM`), environment profile `DISPUTE_CRIMSON` (`#ff1744`, `#fbbf24`, dark obsidian `#1a0303`), and 6 camera presets (`CHAMBER_OVERVIEW`, `CRYSTAL_FOCUS`, `HOLOTANK_FOCUS`, `EVIDENCE_FOCUS`, `STAFF_CONSOLE_FOCUS`, `ADMIN_DAIS_FOCUS`).
    - `useDisputeStore.ts`: Reactive Zustand store tracking active dispute #10 (Alice Owner vs Bob Driver), 2 evidence records with 3D defect coordinates, user simulated role with RBAC enforcement (`ROLE_CO_OWNER`, `ROLE_STAFF`, `ROLE_ADMIN`), staff review inputs, and admin arbitration with atomic SharedFund balance adjustment.
    - `DisputeChamberFloor3D.tsx`: Heavy obsidian radial arena floor ($11.5\text{m}$ radius) with concentric circuitry rings, station conduit bus lines, and 8 perimeter monoliths with levitating crimson shards.
    - `FloatingDisputeCrystal3D.tsx`: Central dual-polyhedral levitating crystal with dual rotating energy rings, breathing core, and status-driven aura (`OPEN`, `UNDER_REVIEW`, `ESCALATED`, `RESOLVED`).
    - `DefectHolotank3D.tsx`: Cylindrical holographic table at `[0, 0, -1.8]` projecting a wireframe digital twin EV with clickable spatial 3D defect pins (`mesh3dDefectCoordinates`), defect details card, and photo preview.
    - `EvidenceDisplay3D.tsx`: Holographic evidence carousel at `[-3.6, 0, 1.8]` displaying uploaded evidence attachments, descriptions, uploader signatures, SHA-256 validation badges, and `[ ➕ ATTACH DEFECT EVIDENCE ]` interaction.
    - `StaffMediationConsole3D.tsx`: Ergonomic desk at `[3.6, 0, 1.8]` with staff review queue, mediation notes input, proposed resolution button, and `[ ⚖ ESCALATE TO ADMIN ARBITRATION ]`. Gated on `ROLE_STAFF` or `ROLE_ADMIN`.
    - `AdminArbitrationDais3D.tsx`: Sovereign dais at `[0, 0, 4.2]` strictly restricted to `ROLE_ADMIN` with arbitration verdict selector, deductible amount input, and `[ ⚖ EXECUTE BINDING ARBITRATION ]` with atomic SharedFund adjustment.
    - `DisputeStatusStela3D.tsx`: Chronological immutable audit history stela at `[0, 0, -5.2]` tracking state transitions and final settlement summary.
    - `DisputeRoom3D.tsx`: Composed sector container mounted in `WorldRoot.tsx` and registered as `DEFAULT_DISPUTE_ROOM_SCENE` in `defaultScenes.ts`, featuring an in-scene 3D camera quick-teleport bar and 3D RBAC role simulation switcher.
  - Backend API Integration (`disputesApi.ts`):
    - REST client mapping to Spring Boot `DisputeController.java` (`getDisputesByGroupId`, `getDisputeById`, `createDispute`, `transitionDisputeStatus`, `addEvidence`, `getDisputeEvidences`, `getDisputeHistory`, `getDisputesForStaffReview`, `addMediationNotes`, `proposeResolution`, `arbitrateDispute`, `getArbitrationDossier`, `arbitrateDisputeWithFundAdjustment`).
  - Strict RBAC & Legal Immutability Enforcement:
    - Evidence Immutability per BR-DIS-01: All uploaded evidence records and 3D defect coordinate pins are permanent legal records.
    - Mediation Access per BR-DIS-03: Co-Owners cannot record staff mediation notes or propose resolutions.
    - Sovereign Arbitration Exclusivity per BR-DIS-05 / BR-DIS-06: Only `ROLE_ADMIN` can execute binding arbitration verdicts and trigger atomic SharedFund deductions.
  - Automated Unit Tests:
    - Created `frontend/src/dispute/dispute.test.ts` (17/17 tests PASS) validating layout coordinates, 3D defect coordinates, immutable evidence attachment, staff mediation gating, and admin binding arbitration.
    - Updated `scene.test.ts` to expect 12 registered scenes (12/12 scenes verified).
    - Vitest test suite: **29 test files, 279/279 unit tests PASS**.
    - TypeScript typecheck: **0 errors**.
    - Vite build: Production bundle generated in 10.23s.
  - Visual Browser Verification:
    - Executed automated browser session navigating to `http://localhost:4173/?sector=DISPUTE_ROOM`, verifying 3D dispute room overview, stations, HUD camera controls, and 0 console errors. Captured screenshots `dispute_room_overview_1789527872548.png`, `dispute_room_cinematic_1789527918124.png`, and recording `dispute_room_verify_1789527717078.webp`.
  - Designed and built the complete 3D Staff Service Workshop in `frontend/src/workshop/` strictly adhering to the Pure 3D mandate (zero 2D HTML forms, overlays, or modal dialogs) and the strict "No fake completed maintenance" interlock:
    - `workshopTypes.ts`: Domain models for `SubsystemId`, `SubsystemStatus`, `VehicleConditionSubsystem`, `WorkOrderStatus`, `MaintenanceWorkOrder`, `WorkshopStation`, `WorkshopTab`, `WorkshopCameraPreset`, and `WorkshopState`.
    - `workshopLayout.ts`: Geometric layout at sector coordinates `[-40, 0, 40]` (`SECTOR_CENTERS.SERVICE_WORKSHOP`) with heavy industrial steel floor ($11.5\text{m}$ radius), caution hazard border, oil drainage grates, and 6 camera presets (`WORKSHOP_OVERVIEW`, `HYDRAULIC_LIFT_FOCUS`, `DIAGNOSTIC_CART_FOCUS`, `PARTS_RACK_FOCUS`, `WORK_ORDER_STELA_FOCUS`, `UNDERCARRIAGE_INSPECTION`).
    - `useWorkshopStore.ts`: Reactive Zustand store tracking active vehicle (VinFast VF6 Eco #3, plate: `30A-666.66`, status: `IN_SERVICE`), hydraulic lift elevation (0.2m to 1.8m), 5 vehicle subsystems, OBD-II scanning, replacement part installation, backend expense commitment, and strict safety certification interlock.
    - `WorkshopFloor3D.tsx`: Industrial steel floor slab with diamond plate texturing, drainage grates, conduit lines, and 8 heavy perimeter steel trusses with fluorescent cyan work lights.
    - `HydraulicLift3D.tsx`: Dual heavy hydraulic steel columns at $x = \pm 1.8\text{m}$, telescoping arms, smooth lerp elevation of the staged VinFast VF6 digital twin, and 3D control pedestal (`[ ▲ ELEVATE (1.80M) ]` / `[ ▼ LOWER (0.20M) ]`).
    - `VehicleConditionDisplay3D.tsx`: 5 physical diagnostic nodes on the vehicle (Front Brakes, Front LiDAR ADAS, HV Battery Tray, Adaptive Suspension, Inverter Thermal Loop) displaying status spheres, health percentages, and fault badges.
    - `DiagnosticCart3D.tsx`: Mobile tool trolley with OBD-II CAN bus monitor, fault code readout (`DTC-P1A24`, `DTC-U0100`), and interactive 3D buttons (`[ 🔍 OBD SCAN ]`, `[ 🛠 OVERHAUL ]`).
    - `PartsRack3D.tsx`: Heavy parts shelving unit with 3D OEM replacement modules (Ceramic Rotor/Pad set, LiDAR unit, Inverter Coolant Circulator) with click-to-install interactions.
    - `WorkOrderStela3D.tsx`: Panoramic digital dispatch stela with 3 panels: Active Work Order Details, Safety Interlock Gateway (`[ 💳 COMMIT EXPENSE ]`, `[ 🏁 CERTIFY & RELEASE TO FLEET ]`), and Historical Maintenance Audit Ledger connected to backend expenses.
    - `ServiceWorkshop3D.tsx`: Composed master sector module mounted in `WorldRoot.tsx` and registered as `DEFAULT_SERVICE_WORKSHOP_SCENE` in `defaultScenes.ts`.
  - Backend API Integration (`financeApi.ts` & `vehiclesApi.ts`):
    - Connected to Spring Boot `ExpenseController.java` (`createExpense`) to log real 6,000,000 VND maintenance liability with audit reference.
    - Connected to Spring Boot `VehicleController.java` (`updateStatus`) to transition vehicle status to `AVAILABLE` upon certified release.
  - Strict "No Fake Completed Maintenance" Interlock:
    - Attempting to release a vehicle while active DTC faults remain triggers an immediate safety violation rejection.
    - Attempting to release a vehicle without committing the expense ledger record triggers an audit hold.
    - Certification requires: (1) OBD diagnostic verification, (2) all subsystems repaired to 100%, and (3) ledger expense committed.
  - Automated Unit Tests:
    - Created `frontend/src/workshop/workshop.test.ts` (14/14 tests PASS) validating layout coordinates, hydraulic lift, OBD scan, subsystem repairs, strict safety interlock, and ledger commitment.
    - Updated `scene.test.ts` to expect 11 registered scenes (11/11 scenes verified).
    - Vitest test suite: **28 test files, 262/262 unit tests PASS**.
    - TypeScript typecheck: **0 errors**.
    - Vite build: Production bundle generated in 6.73s.
  - Visual Browser Verification:
    - Executed automated browser session navigating to `http://localhost:4173/?sector=WORKSHOP`, verifying 3D workshop, hydraulic lift elevation, parts replacement, ledger commitment, and safety certification. Captured screenshots `workshop_overview_clean_1789525126501.png` and `workshop_complete_1789525901231.png`.
* **Summary of Checkpoint 09-K (Operations Center & Staff Fleet Logistics)**:
  - Designed and built the complete 3D Staff Operations & Logistics Center in `frontend/src/operations/` strictly adhering to the Pure 3D mandate (zero 2D HTML forms, overlays, or modal dialogs) and connecting directly to real Spring Boot backend endpoints:
    - `operationsTypes.ts`: Domain models for `OperationsTab`, `OperationsStation`, `CheckInOutMode`, `OperationalAlertItem`, `VehicleOperationStatus`, `OperationsCameraPreset`, and `OperationsState`.
    - `operationsLayout.ts`: Geometric layout at sector coordinates `[-40, 0, 0]` with heavy industrial hangar floor ($11.5\text{m}$ radius), perimeter hazard caution stripes (`#eab308`/`#111827`), 3 designated service bays (`BAY 01`, `BAY 02`, `BAY 03`), and 6 camera presets (`HANGAR_OVERVIEW`, `QR_STATION_FOCUS`, `DISPATCH_CONSOLE_FOCUS`, `FLEET_STATUS_FOCUS`, `NOTIFICATION_BOARD_FOCUS`, `INSPECTION_BAY_FOCUS`).
    - `useOperationsStore.ts`: Reactive Zustand store tracking fleet vehicles (VinFast VF8 Plus, VF9 Executive, VF6 Eco), active session, check-in and check-out execution, optical QR generation/validation, odometer & battery inputs, inspection damage flag, and operational alert dismissals.
    - `OperationsFloor3D.tsx`: Heavy industrial hangar floor slab with caution hazard border, 3 service bay demarcation zones, conduit buslines, and 8 perimeter structural trusses with pulsing amber warning beacon caps.
    - `QRScannerStation3D.tsx`: Optical scanner kiosk with animated sweeping laser line, 3D procedural holographic QR matrix, token validity readout, and interactive 3D buttons (`[ ⚡ GENERATE QR ]`, `[ 🔍 VALIDATE QR ]`).
    - `DispatchConsole3D.tsx`: Master vehicle operations console angled at $-22^\circ$ with check-in/out tabs, vehicle selector, odometer & battery SoC inputs with +/- 3D buttons, chassis damage toggle, and big master dispatch button (`[ 🟢 DISPATCH VEHICLE ]` / `[ 🏁 RECONCILE RETURN ]`).
    - `FleetStatusStela3D.tsx`: Curved 3-bay digital twin fleet matrix showing statuses, live battery SoC bars, odometer readings, assigned bays, active drivers, and interactive service hold toggles.
    - `OperationalNotifications3D.tsx`: Floating holographic bulletin board displaying real-time alerts with interactive acknowledge buttons and global dismiss.
    - `VehicleInspectionBay3D.tsx`: Staging pad with sweeping laser scan plane, ghost EV wireframe chassis, and multi-sensor checkpoints (LiDAR, Tire Hubs, Battery Tray, Charge Port).
    - `OperationsCenter3D.tsx`: Composed sector module mounted in `WorldRoot.tsx` and registered as `DEFAULT_OPERATIONS_CENTER_SCENE` in `defaultScenes.ts`.
  - Backend API Integration (`frontend/src/api/usageSessionsApi.ts`):
    - Matched Spring Boot `UsageSessionController.java` and `VehicleController.java`: `generateQr`, `validateQr`, `checkIn`, `checkOut`, `getSessionById`, `updateVehicleStatus`, and `getVehicleTelemetry`.
  - Automated Unit Tests:
    - Created `frontend/src/operations/operations.test.ts` (16/16 tests PASS) covering spatial layout, station coordinates, store state, alert management, QR workflow, check-in, check-out, and status updates.
    - Updated `scene.test.ts` to expect 10 registered scenes (10/10 scenes verified).
    - Vitest test suite: **27 test files, 248/248 unit tests PASS**.
    - TypeScript typecheck: **0 errors**.
    - Vite build: Production bundle generated in 7.66s.
  - Visual Browser Verification:
    - Executed automated browser session navigating to `http://localhost:4173/`, activating `OPERATIONS` sector, verifying 3D hangar, stations, and capturing screenshots `operations_center_overview_1789524015986.png` and `operations_center_active_interaction_1789524091810.png`.
  - Designed and built the complete 3D AI Intelligence Center in `frontend/src/ai/` strictly adhering to the Pure 3D mandate (zero 2D HTML forms, overlays, or modals), the Honest Status Disclosure directive (*"Never pretend mock AI is production AI"*), and the AI Safety Boundary Protocol (BR-AI-SAFE-01):
    - `aiTypes.ts`: Domain models for `AIModelStatus`, `AIIntelligenceCategory` (`RECOMMENDATION`, `INSIGHT`, `ANOMALY_INDICATOR`, `FAIRNESS_SUGGESTION`), `AnomalySeverity`, `AISafetyActionType`, `AISafetyAttempt`, `AIRecommendationItem`, `AIDataNodeModel`, `AICameraPreset`, and `AICoreState`.
    - `aiLayout.ts`: Geometric layout at sector coordinates `[40, 0, 0]`: neural arena floor disc ($11.5\text{m}$ radius), central floating neural holosphere, interaction console, 4 satellite data nodes, curved holographic recommendation screen, result visualization stela, and 6 camera presets (`NEXUS_OVERVIEW`, `CORE_FOCUS`, `TERMINAL_FOCUS`, `HOLOGRAPHIC_RECOMMENDATIONS`, `DATA_NODES_FOCUS`, `SAFETY_AUDIT_FOCUS`).
    - `aiSafety.ts`: Strict enforcement of BR-AI-SAFE-01. AI is hardcoded as advisory-only and cannot independently authorize payments, alter equity, bypass RBAC, approve contracts, or execute irreversible financial commitments. Generates immutable audit violation records.
    - `useAIStore.ts`: Reactive Zustand store connecting to backend `AnalyticsController.java` (`analyticsApi.getGroupFairUsage`). Explicitly badges development mock status (`isProductionAI = false`, `modelStatus = 'DEVELOPMENT_MOCK'`, `disclosureNotice = 'STATUS: DEVELOPMENT MOCK (AI API UNAVAILABLE IN PROD ENV)'`), manages multi-category recommendations, interactive query prompt chips, and safety interlocks.
    - `AIChamberFloor3D.tsx`: Radial cybernetic floor with concentric neon purple and cyan bus lines, illuminated central dais, and 8 perimeter neural pylons with glowing crystal crowns.
    - `AICoreHoloSphere3D.tsx`: Central neural sphere with 3 concentric rotating gyroscopic wireframe rings, pulsing inner synaptic core, and hovering honest disclosure and advisory disclaimer badge.
    - `AIDataNodes3D.tsx`: 4 floating satellite nodes (`NODE_MOBILITY`, `NODE_FAIRNESS`, `NODE_FINANCIAL`, `NODE_GOVERNANCE`) with live laser conduits connecting to the AI core and floating telemetry cards.
    - `AIRecommendationHolograms3D.tsx`: Curved 3D holographic display with category filter tabs projecting advisory cards (off-peak tariff optimization, weekend peak demand imbalance, battery cell thermal drift, fairness parity allocation) with confidence scores and impact badges.
    - `AIResultVisualization3D.tsx`: 3D visual data stela displaying member fairness ratios vs 1.0 parity baseline, battery pack thermal variance matrix, and autonomy compliance seal.
    - `AITerminal3D.tsx`: Ergonomic console angled at $-22^\circ$ with query chips (`[ ANALYZE FAIRNESS ]`, `[ BATTERY TELEMETRY ]`, etc.), honest mock disclosure banner, and safety boundary test buttons with an interactive 3D Hard Safety Interlock modal.
    - `AIIntelligenceCenter3D.tsx`: Composed sector module mounted in `WorldRoot.tsx` and registered as `DEFAULT_AI_INTELLIGENCE_CENTER_SCENE` in `defaultScenes.ts`.
  - Backend API Integration (`frontend/src/api/analyticsApi.ts`):
    - Connected algorithmic data to Spring Boot `AnalyticsController.java`: `getGroupFairUsage` and `getMyFairUsageScore`.
  - Automated Unit Tests:
    - Created `frontend/src/ai/ai.test.ts` (16/16 tests PASS) validating layout coordinates, honest mock disclosure, multi-category coverage, BR-AI-SAFE-01 safety enforcement on all prohibited actions, and interactive query synthesis.
    - Updated `scene.test.ts` to expect 9 registered scenes (9/9 scenes verified).
    - Vitest test suite: **26 test files, 232/232 unit tests PASS**.
    - TypeScript typecheck: **0 errors**.
    - Vite build: Production bundle generated in 7.55s.
* **Summary of Checkpoint 09-I (Decision Chamber & Authoritative Governance Deliberation)**:
  - Designed and built the complete 3D Decision Chamber in `frontend/src/decision/` strictly adhering to the Pure 3D mandate (zero 2D HTML forms, overlays, or modals) and Backend Authority mandate ("Do not calculate final voting authority only on frontend. Backend remains authoritative."):
    - `decisionTypes.ts`: Domain models for `DecisionTab`, `DecisionChamberStation`, `SyndicateMemberEquity`, `DecisionCameraPreset`, and `DecisionState`.
    - `decisionLayout.ts`: Geometric layout at sector coordinates `[40, 0, -40]`: parliamentary arena disc ($11.5\text{m}$ radius), floating proposal pod cluster, angled voting console ($-25^\circ$ tilt), member equity allocation pillars, volumetric liquid quorum column with 60% laser mark, authoritative governance verdict stela, and 6 camera presets (`ARENA_OVERVIEW`, `TERMINAL_FOCUS`, `PROPOSAL_PODS_FOCUS`, `EQUITY_FOCUS`, `QUORUM_RESULTS_FOCUS`, `STELA_FOCUS`).
    - `useDecisionStore.ts`: Reactive Zustand store tracking active proposals, co-owner equity distribution (Minh Hiep 35.0%, Tran Duc 25.0%, Le Hoang 20.0%, Pham Mai 20.0%), live ballot casting, and backend synchronization. Strictly queries backend `/proposals/{id}/tally` and `/proposals/{id}/results` for official quorum and passing decisions.
    - `DecisionChamberFloor3D.tsx`: Semicircular arena floor with concentric parliament indigo and neon violet rings, central amphitheater dais, and 8 perimeter architectural pylons topped with glowing violet crystal crowns.
    - `ProposalHoloPod3D.tsx`: Semicircular arc of 3D floating polyhedrons (Octahedron for routine expense, Dodecahedron for asset upgrade, Icosahedron for amendment) with interactive selection, wireframe outer shells, and 3D holographic title cards.
    - `VotingTerminal3D.tsx`: Parliamentary console tilted at $-25^\circ$ toward user, voter equity readout (35.00%), 3 tactile 3D vote button pedestals (`[ APPROVE ]`, `[ REJECT ]`, `[ ABSTAIN ]`), glowing status feedback, and calling backend `POST /api/v1/proposals/{id}/votes`.
    - `EquityAllocationPillars3D.tsx`: 4 vertical glass/neon pillars proportional in height to each member's equity percentage, with hovering member badges and live ballot status indicators (`✓ APPROVE`, `✓ REJECT`, `PENDING`).
    - `QuorumLiquidColumn3D.tsx`: Transparent cylinder with volumetric liquid core tracking participation rate, horizontal laser ring marking mandatory 60.00% quorum threshold, and live quorum satisfaction indicator.
    - `DecisionResultStela3D.tsx`: Monolithic obsidian stela displaying official backend verdict (`PASSED`, `REJECTED`, `QUORUM_NOT_MET`), mathematical breakdown, and decision reason string from the Spring Boot backend engine.
    - `DecisionChamber3D.tsx`: Composed sector module mounted in `WorldRoot.tsx` and registered as `DEFAULT_DECISION_CHAMBER_SCENE` in `defaultScenes.ts`.
  - Backend API Integration (`frontend/src/api/proposalsApi.ts`):
    - Matched Spring Boot `ProposalController.java`: `getProposalsByGroupId`, `getProposalById`, `castVote`, `getMyVote`, `getProposalTally`, and `getProposalResults`.
  - Automated Unit Tests:
    - Created `frontend/src/decision/decision.test.ts` (10/10 tests PASS) validating layout coordinates, 100.00% member equity distribution, proposal pod selection, quorum pending (<60%) before ballot, quorum satisfied (80%) after ballot, and backend authority.
    - Updated `scene.test.ts` to expect 8 registered scenes (8/8 scenes verified).
    - Vitest test suite: **25 test files, 216/216 unit tests PASS**.
    - TypeScript typecheck: **0 errors**.
    - Vite build: Production bundle generated in 5.78s.
* **Summary of Checkpoint 09-H (Digital Contract Room & Holographic Document Signing)**:
  - Designed and built the complete 3D Digital Contract Room workflow in `frontend/src/contracts/` strictly adhering to the Pure 3D mandate (zero 2D HTML document pages, tables, or overlays):
    - `contractTypes.ts`: Domain models for `ContractDocumentSection`, `ContractVersionCard`, `ContractRoomTab` (`DOCUMENT`, `VERSIONS`, `SIGNATURES`, `STATUS_OVERVIEW`), `ContractSigningState`, and `ContractOperationResult`.
    - `contractLayout.ts`: Geometric layout at sector coordinates `[40, 0, -80]`: executive slate disc floor ($16\text{m}$ radius), central document lectern, biometric signature dais, version tree monolith, floating holographic status seal, and 5 specialized camera presets (`ROOM_OVERVIEW`, `DOCUMENT_FOCUS`, `SIGNATURE_FOCUS`, `VERSION_FOCUS`, `STATUS_FOCUS`).
    - `useContractStore.ts`: Reactive Zustand store managing active legal contract, 5-section legal agreement text, version history, signatures overview, interactive page turning, terms acknowledgment checkbox, and real backend digital signing with SHA-256 hash provenance.
    - `ContractRoomFloor3D.tsx`: Executive slate floor with concentric sapphire circuit lines, rotating outer legal gears, 8 perimeter slate pylons with blue beacons, and inscribed statutory compliance text.
    - `ContractDocument3D.tsx`: Ergonomic lectern pedestal with an angled sapphire glass document slab ($30^\circ$ tilt), multi-page legal text rendered via `@react-three/drei` `<Text>`, 5 quick-jump section chips, background cryptographic watermarks, and 3D pagination buttons (`[ < PREV SECTION ]`, `PAGE X OF 5`, `[ NEXT SECTION > ]`).
    - `SignaturePedestal3D.tsx`: Biometric hand/thumbprint scanner glass pad with laser sweep micro-animation, interactive 3D terms acknowledgment checkbox, and tactile 3D `[ EXECUTE DIGITAL SIGNATURE ]` button calling backend `POST /api/v1/contracts/{id}/sign`.
    - `ContractVersionStela3D.tsx`: Chrome-slate monolith displaying chronological version tree (`v1.0`, `v1.1`, `v2.0`) with interactive version cards that load corresponding agreements into the 3D document lectern.
    - `ContractStatusSeal3D.tsx`: Floating 3D holographic status emblem above the lectern with rotating halo ring, live quorum progress gauge (`QUORUM: 4/4 SIGNED`), signatory status breakdown, and status activation trigger.
    - `DigitalContractRoom3D.tsx`: Composed sector module mounted in `WorldRoot.tsx` and registered as `DEFAULT_DIGITAL_CONTRACT_ROOM_SCENE` in `defaultScenes.ts`.
  - Backend API Integration (`frontend/src/api/contractsApi.ts`):
    - Aligned REST endpoints with Spring Boot `ContractController.java`: `getContractById`, `getContractsByGroupId`, `getActiveContractByGroupId`, `getSignaturesOverview`, `signContract`, `transitionStatus`, `createContract`, and `updateDraftContract`.
  - Automated Unit Tests:
    - Created `frontend/src/contracts/contract.test.ts` (9/9 tests PASS) validating layout coordinates, camera presets, 5-section multi-page navigation, version loading, mandatory terms acceptance, SHA-256 digital signature creation, and status transitions.
    - Updated `scene.test.ts` to expect 7 registered scenes (7/7 scenes verified).
    - Vitest test suite: **24 test files, 206/206 unit tests PASS**.
    - TypeScript typecheck: **0 errors**.
    - Vite build: Production bundle generated in 5.51s.
* **Summary of Checkpoint 09-F (Energy & Finance Center & Cost Allocation)**:
  - Designed and implemented the complete 3D Finance Center workflow in `frontend/src/finance/` strictly adhering to the Pure 3D mandate (zero 2D HTML pages, tables, or modals) and strict "No Fake Financial Success" enforcement:
    - `financeTypes.ts`: Full domain models for `ExpenseCategory`, `AllocationStrategy` (`OWNERSHIP_BASED`, `USAGE_BASED`, `HYBRID`), `CategoryVisualConfig`, `MemberAllocationShare`, `ExpenseItemModel`, `MemberCostSummary`, and `PaymentTransactionSession`.
    - `financeLayout.ts`: Geometric positioning within the `[0, 0, -80]` sector: cleanroom floor dial ($16\text{m}$ radius), expense crystal dais, cost allocation bar graph platform, liquid reserve vault column, contactless payment kiosk, master finance console, and 6 camera framing presets (`FINANCE_OVERVIEW`, `TERMINAL_FOCUS`, `EXPENSE_CLUSTER`, `ALLOCATION_FOCUS`, `PAYMENT_KIOSK_FOCUS`, `VAULT_COLUMN_FOCUS`).
    - `useFinanceStore.ts`: Reactive Zustand store tracking active group finance, dynamic allocation recomputation across the 3 strategies, liquid balance ratio vs minimum safety reserve floor (`BR-FIN-03`), and real backend payment actions with strict error propagation.
    - `FinanceCenterFloor3D.tsx`: High-tech surgical cleanroom floor with concentric emerald and cyan guideline rings, outer neon curb ring, radial data conduits, 10 perimeter atmospheric pylons, and suspended overhead cyber luminance halo.
    - `FloatingExpenseCrystal3D.tsx`: Interactive procedural polyhedrons with category-specific geometry (Octahedron for charging, Dodecahedron for maintenance, Icosahedron for insurance, Cylinder for cleaning, Box for parking). Features gentle hover bobbing, 3D SDF info badges with amount and settlement status, and volumetric slice expansion when selected into 4 member allocation shards.
    - `ExpenseCluster3D.tsx`: Semicircular dais staging the floating expense crystals with monthly pool total readout.
    - `CostAllocationBarGraph3D.tsx`: 3D vertical columns rising from the floor for each co-owner with live height tweening, dual emerald (paid) and amber (due) segments, member equity/usage stats, and interactive 3D strategy switcher buttons (`EQUITY %`, `USAGE %`, `HYBRID`).
    - `LiquidReserveColumn3D.tsx`: Transparent 3D cylindrical glass chamber with glowing fluid fill level correlating to liquid VND balance, amber laser guideline for safety reserve (`BR-FIN-03`), flashing warning beacon for low liquidity, and floating financial health telemetry stela.
    - `PaymentKiosk3D.tsx`: Physical contactless payment station with NFC tap pad, VietQR 3D display, amount selector, channel switchers (`VIETQR / BANK`, `E-WALLET`, `NFC CARD`, `DEV SANDBOX`), and strict "No Fake Financial Success" execution (displays real processing, exact backend error diagnostics upon failure, and verified receipt codes upon genuine settlement).
    - `FinanceTerminal3D.tsx`: Master console kiosk with ergonomic $-18^\circ$ viewing tilt, 5 breadcrumb tabs (`OVERVIEW`, `EXPENSES`, `ALLOCATION`, `PAYMENT`, `AUDIT`), monthly pool metrics, and in-world pure 3D expense logger connected to backend POST `/api/v1/expenses`.
    - `FinanceCenter3D.tsx`: Composed sector module mounted in `WorldRoot.tsx` and registered in `SceneRegistry`.
  - Backend REST Integration (`backend/src/main/java/com/example/evshare/controller/PaymentController.java`):
    - Added `PaymentController.java` exposing `PaymentService` over REST (`POST /api/v1/payments/initiate`, `GET /api/v1/payments/{id}`, `GET /api/v1/payments/reference/{reference}`, `POST /api/v1/payments/{id}/verify`).
    - Backend compiles with `mvn.cmd test-compile` (BUILD SUCCESS, 293 source files).
  - Automated Unit Tests:
    - Created `frontend/src/finance/finance.test.ts` (13/13 tests PASS) validating initial state, category fixed/variable classifications, allocation strategy recalculations, liquid reserve deficit detection, real payment initiation error handling, and verified payment completion.
    - Updated `scene.test.ts` (5/5 tests PASS) asserting 5 default scenes.
    - Total unit test count: **22 test files, 187/187 tests PASS**.
    - TypeScript typecheck: **0 errors**.
    - Vite build: Production bundle generated in 5.75s.
* **PREVIOUS_PHASE**: `PHASE 08 — PURE 3D ENGINE & 3D DESIGN SYSTEM` (VERIFIED / COMMITTED / PUSHED at `1e299b3`)
* **Frontend Verification (`npm run test && npm run typecheck && npm run build`)**: **`BUILD & TEST SUCCESS`** (21/21 unit test suites, 174/174 unit tests PASS; 0 errors, 0 warnings across TypeScript typecheck and Vite production build)
* **Summary of Checkpoint 09-E (Chrono-Spatial 3D Booking Chamber)**:
  - Designed and implemented the complete 3D Booking Chamber workflow in `frontend/src/booking/` strictly adhering to the Pure 3D mandate (zero 2D HTML booking forms or popups):
    - `bookingTypes.ts`: Domain definitions for `BookingStep` (`CALENDAR`, `TIMELINE`, `TIME_SELECTION`, `CONFIRMATION`, `RESULT`), `TimelineHourSlot`, `CalendarDayCell`, `FairUsageScoreModel`, and `BookingSubmissionResult`.
    - `bookingLayout.ts`: Geometric specification of the chronometer dial floor ($16\text{m}$ radius), vehicle showcase turntable ($3.5\text{m}$ radius), master holographic booking kiosk, floating fair usage quota stela, and camera presets (`CHAMBER_OVERVIEW`, `TERMINAL_FOCUS`, `CALENDAR_FOCUS`, `VEHICLE_INSPECTION`).
    - `useBookingStore.ts`: Reactive Zustand store tracking active booking vehicle, 14-day calendar cells with occupancy rates, 24-hour timeline slots, mandatory 30-minute turnaround buffer (`BR-BKG-02`), optimistic reservation submission, fair usage quotient, and camera framing.
    - `BookingChamberFloor3D.tsx`: Chronometer dial floor with radial tick marks, inner glowing guidance rings, and 12 atmospheric perimeter pylons with beacon lighting.
    - `VehicleShowcasePedestal3D.tsx`: Central rotating turntable with procedural digital twin EV model, spinning halo ring, and floating holographic vehicle identity badge.
    - `ChronoCalendar3D.tsx`: Interactive 14-day 3D calendar cards arranged along a viewing curve, featuring occupancy indicators, week pagination, and raycast selection.
    - `ChronoTimeline3D.tsx`: 24-hour interactive 3D timeline ribbon with hour blocks and distinct purple 30-minute turnaround buffer visualization (`BR-BKG-02`).
    - `TimeSelector3D.tsx`: In-world 3D time stepper controls, duration presets (2h, 4h, 8h, full day), live turnaround buffer conflict detection, and real-time total estimate calculator.
    - `FairUsageHoloPillar3D.tsx`: Floating holographic quota stela displaying member fairness ratio, peak-hour usage balance, and AI scheduling recommendations.
    - `BookingConfirmation3D.tsx`: 3D spatial confirmation card with trip schedule, vehicle specs, turnaround buffer compliance notice, and action buttons.
    - `BookingResult3D.tsx`: Celebratory confirmation certificate hologram with confirmation code, unlock PIN, calendar sync, and garage return action.
    - `BookingTerminal3D.tsx`: Master console kiosk orchestrating stages with 3D breadcrumbs and contextual stage rendering.
    - `BookingChamber3D.tsx`: Composed sector module mounted in `WorldRoot.tsx` at coordinates `[0, 0, 48]`.
  - Backend API Integration (`frontend/src/api/`):
    - `bookingsApi.ts`: REST endpoints for `/bookings/availability`, `/bookings/timeline`, `/bookings`, `/bookings/{id}`, `/bookings/my-bookings`, and `/bookings/{id}/cancel` with resilient fallback.
    - `analyticsApi.ts`: REST endpoints for `/analytics/fair-usage/my-score` and `/analytics/fair-usage/group/{groupId}`.
  - Inter-Sector Linking:
    - Wired `VehicleInspectionTerminal3D.tsx` in Central Garage with a `📅 BOOK` button that teleports the user directly to the Booking Chamber and focuses the Booking Terminal.
    - Registered `DEFAULT_BOOKING_CHAMBER_SCENE` in `defaultScenes.ts` and `App.tsx`.
  - Automated Unit Tests:
    - Created `booking.test.ts` (14/14 tests PASS) testing initial state, vehicle switching, 14-day calendar, 24-hour timeline duration, turnaround buffer conflict enforcement, workflow steps, submission, and fair usage.
    - Updated `scene.test.ts` (5/5 tests PASS).
    - Total unit test count: **21 test files, 174/174 tests PASS**.
    - TypeScript typecheck: **0 errors**.
    - Vite build: Production bundle generated in 6.23s.
  - Browser Verification:
    - Recording artifact: `booking_chamber_done_1789443966823.webp`.
    - Verified pure 3D interaction across 3D calendar day selection, 3D timeline ribbon, turnaround buffers, booking confirmation, and Central Garage cross-sector linking.
* **PREVIOUS_PHASE**: `PHASE 08 — PURE 3D ENGINE & 3D DESIGN SYSTEM` (VERIFIED / COMMITTED / PUSHED at `1e299b3`)
* **Frontend Verification (`npm run test && npm run typecheck && npm run build`)**: **`BUILD & TEST SUCCESS`** (20/20 unit test suites, 160/160 unit tests PASS; 0 errors, 0 warnings across TypeScript typecheck and Vite production build)
* **Summary of Checkpoint 09-C (Central Garage & Digital Twin Vehicles)**:
  - Designed and implemented the complete 3D Central Garage & Digital Twin Vehicle system in `frontend/src/garage/`:
    - `garageTypes.ts`: Full domain models for `GarageVehicle`, `ParkingBay`, `ChargingStation`, and `VehicleOwnershipInfo`.
    - `garageLayout.ts`: Geometric specification of 6 designated parking & charging bays within the 24m garage radius (`BAY-01` to `BAY-06`) and 2 Hypercharger stations (`CHG-01`, `CHG-02`).
    - `useGarageStore.ts`: Reactive Zustand store tracking fleet telemetry, parking bay occupancy, charging power, camera framing coordination, and optimistic status updates.
    - `GarageFloor3D.tsx`: Central showroom floor disc ($24\text{m}$ radius), perimeter neon curb ring, guidance rings, central circular pedestal, and suspended overhead neon luminance halo ring.
    - `ParkingBay3D.tsx`: Individual stall markings, neon striping (cyan/emerald), rear wheel stop bumper bars, and 3D floor labels.
    - `ChargingStation3D.tsx`: High-power 250 kW Supercharger kiosk with vertical status lightstrips, front holographic display, charging cable nozzle, and pulsing aura.
    - `DigitalTwinVehicle3D.tsx`: Aerodynamic procedural EV models with metallic paint, glass canopy, Cyberpunk LED lightbars, alloy wheels with brake calipers, status underglow, and floating 3D holographic telemetry cards (Model, License plate, Status badge, Battery SoC progress bar, and Syndicate Ownership info).
    - `VehicleInspectionTerminal3D.tsx`: In-world holographic terminal positioned beside selected vehicles with live telemetry readouts, lock/unlock button, charging control, and camera coordination.
    - `CentralGarage3D.tsx`: Composed sector module mounted directly into `WorldRoot.tsx` at `CENTRAL_GARAGE` coordinates `[0, 0, 0]`.
  - Backend Integration (`frontend/src/api/`):
    - `apiClient.ts`: Configured Axios client with `/api` baseURL and auth header interceptor.
    - `vehiclesApi.ts`: REST endpoints for `/vehicles`, `/vehicles/{id}`, `/vehicles/{id}/telemetry`, and `/vehicles/{id}/status`.
    - `ownershipGroupsApi.ts`: REST endpoints for `/ownership-groups` and `/ownership-groups/{id}`.
    - Seamless live backend data hydration with graceful bootstrap fallback when offline.
  - Automated Unit Tests:
    - Created `garage.test.ts` (8/8 tests PASS) covering garage layout geometry, charging bay correlations, vehicle selection & camera framing, lock/charging controls, live backend hydration, and offline fallback.
  - Overall Frontend Quality:
    - `npm run test`: **20 test files, 160 tests PASS** in 4.22s.
    - `npm run typecheck`: **0 errors**.
    - `npm run build`: Vite production bundle generated cleanly in 5.53s.
* **Frontend Stack**:
  - React `18.3.1` + React DOM `18.3.1`
  - Three.js `^0.160.0`
  - `@react-three/fiber` `^8.18.0`
  - `@react-three/drei` `^9.121.4` (SDF 3D `<Text>` rendering in WebGL)
  - `zustand` `^4.5.2` (12 modular decoupled stores including `useTouchStore`, `usePerformanceStore`, and `useWebGLRecoveryStore`)
  - `vitest` `^1.6.1` (Automated frontend unit testing, 17 test suites)
  - `axios` `^1.7.9`
  - TypeScript `~5.3.3` (Strict mode, ES2022, bundler module resolution, `@/*` alias)
  - Vite `^5.4.14` (Proxy `/api` -> `http://localhost:8080`, port 3000, vendor chunking)
* **Summary of Checkpoint 08-AG (3D Engine Tests & Verification)**:
  - Validated and wrote comprehensive automated unit test suites covering all 14 mandated engine subsystems:
    - `scene initialization`: `scene.test.ts` (5 tests) covering SceneRegistry, default scene configs, lighting/fog environments, and lifecycle states.
    - `camera`: `CameraManager.test.ts` (7 tests) covering camera modes (FIRST_PERSON, THIRD_PERSON, FREE_ORBIT), cinematic keyframes, and transitions.
    - `movement`: `PlayerMovement.test.ts` (7 tests) covering velocity integration, friction damping, directional heading, and sprint speed.
    - `raycasting`: `RaycastManager.test.ts` (6 tests) covering NDC normalization, priority sorting, distance thresholds, and interactable registration.
    - `interaction`: `InteractionManager.test.ts` (7 tests) covering 6-stage pipeline, hover states, selection, and activation handlers.
    - `focus`: `FocusManager.test.ts` (5 tests) covering focus targets, camera framing presets, and navigation history stacks.
    - `animation`: `AnimationManager.test.ts` (8 tests) covering SpringSolver, LerpSolver, spatial transforms, and hover scales.
    - `state`: `visualState.test.ts` (20 tests) and `UI3DManager.test.ts` (6 tests) covering modal, card, panel, and 8 visual interaction states.
    - `3D button`: `ThreeDButton.test.ts` (9 tests) covering elevations, variants, active/disabled states, and spatial text labels.
    - `input`: `InputManager.test.ts` (12 tests) covering keyboard action mapping, pointer drag thresholds, click dispatches, and typing modes.
    - `terminal`: `terminal.test.ts` (4 tests) covering status LED variants, ergonomic $-18^\circ$ viewing tilt, and FocusRegistry integration.
    - `portal`: `portal.test.ts` (3 tests) covering waypoint teleportation, camera framing relocation, and destination collision avoidance.
    - `WebGL failure`: `recovery.test.ts` (7 tests) covering WebGL context loss/restoration, initialization failure diagnostics, retries, and Safe Mode downgrade to LOW tier.
    - `responsive behavior`: `touch.test.ts` (7 tests) covering virtual joystick normalization, tap selection criteria, portrait FOV expansion, and mobile/tablet performance profiles.
    - Also validated `performance.test.ts` (11 tests) and `AudioManager.test.ts` (7 tests).
  - Executed `npm install` and verified all 224 packages are up-to-date.
  - Executed `npm run test`: **17 test files, 131 tests passed** in 1.77s.
  - Executed `npm run typecheck`: **0 errors**.
  - Executed `npm run build`: Vite production bundle generated cleanly in 6.20s.
  - Verified live in-browser execution with `browser_subagent`: Loaded at `http://127.0.0.1:4173/`, verified canvas rendering, HUD telemetry, camera mode switching (`1ST PERSON` / `3RD PERSON`), and confirmed **0 console errors / 0 warnings**.
* **Summary of Checkpoint 08-AF (Mobile & Tablet 3D Input Subsystem)**:
  - Designed and implemented the responsive touch and viewport control architecture in `frontend/src/engine/touch/`:
    - `touchTypes.ts`: Type definitions for `TouchDeviceProfile`, `VirtualJoystickState`, and `TouchTapEvent`.
    - `useTouchStore.ts`: Reactive Zustand store tracking touch device flags, virtual joystick coordinates, and tap events. Maps joystick vectors directly to `useInputStore.setMovement` and `usePlayerStore.setMovementMode` (`WALKING` vs `SPRINTING`).
    - `VirtualTouchJoystick.tsx`: Glassmorphic cybernetic touch thumbstick component mounted at the bottom-left of the viewport. Provides fluid 360-degree normalized analog vector control with spring-recentering.
    - `TouchGestureController.tsx`: Full-viewport touch event manager supporting single-finger look/orbit dragging, rapid tap selection raycasting (`<250ms`, `<10px`) emitting `POINTER_CLICK` with NDC coordinates, and two-finger pinch-to-zoom adjusting camera FOV.
    - `ResponsiveViewportController.tsx`: R3F viewport controller adjusting camera vertical FOV for narrow portrait screens (`aspect < 1.0`) and automatically profiling performance (`LOW`/`MEDIUM` tier, DPR clamping $\le 1.5$) on mobile/tablet devices.
    - Integrated with `Canvas3DFoundation.tsx` and `App.tsx` while ensuring no separate 2D dashboard application is ever created.
  - Automated Unit Tests: Created `touch.test.ts` (7/7 tests PASS) covering joystick vector normalization, walking/sprinting thresholds, tap validation thresholds, portrait FOV expansion, and mobile performance tier assignment. Total frontend unit tests: 119/119 PASS.
  - Verified `npm run test`, `npm run typecheck`, and `npm run build` with exit code 0.
* **Summary of Checkpoint 08-AE (WebGL Failure & Recovery Subsystem)**:
  - Designed and implemented the WebGL error boundary and recovery architecture in `frontend/src/engine/recovery/`:
    - `webglDetector.ts`: Hardware and capability probe querying WebGL2/WebGL1 availability, unmasked GPU vendor and renderer strings via `WEBGL_debug_renderer_info`, max texture and renderbuffer dimensions, and software CPU rasterizer detection (SwiftShader, llvmpipe, softpipe, VirtualBox).
    - `useWebGLRecoveryStore.ts`: Central recovery store tracking failure states (`CONTEXT_LOST`, `INIT_FAILED`, `RENDER_ERROR`, `UNSUPPORTED`), retry counts, diagnostic report caching, and safe mode switching.
    - `RecoveryScreen.tsx`: Cybernetic diagnostic cockpit providing crystal-clear user feedback, hardware capability readouts, copyable diagnostic JSON reports, and recovery controls (`RETRY INITIALIZATION`, `LAUNCH SAFE MODE (LOW)`, `RELOAD`). Strictly prevents blank pages while adhering to the mandate to never silently convert the application into a traditional 2D dashboard.
    - `ErrorBoundary3D.tsx`: React error boundary catching 3D render pipeline exceptions and rendering `RecoveryScreen`.
    - `Canvas3DFoundation.tsx`: Directly hooked `webglcontextlost` and `webglcontextrestored` events to the recovery store and mounted `RecoveryScreen` when context is lost.
  - Automated Unit Tests: Created `recovery.test.ts` (7/7 tests PASS) validating detection probes, context loss handling, render error capture, retry resets, and Safe Mode downgrade to `LOW` tier. Total frontend unit tests: 112/112 PASS.
  - Verified `npm run test`, `npm run typecheck`, and `npm run build` with exit code 0.
* **Summary of Checkpoint 08-AD (Performance Foundation & Quality Control Engine)**:
  - Designed and implemented the performance subsystem in `frontend/src/engine/performance/`:
    - `performanceTypes.ts`: Type definitions for `PerformanceTier` (`HIGH`, `MEDIUM`, `LOW`), `PerformanceProfile`, and `FrameRateSample`.
    - `performanceProfiles.ts`: Calibrated profiles for `HIGH` (DPR `[1.0, 2.0]`, 2048 PCF soft shadows, $16\times$ anisotropy, $1.0\times$ LOD bias), `MEDIUM` (DPR `[0.85, 1.5]`, 1024 PCF shadows, $4\times$ anisotropy, $1.2\times$ LOD bias), and `LOW` (DPR `[0.65, 1.0]`, shadows disabled, $1\times$ anisotropy, $1.5\times$ aggressive LOD reduction).
    - `usePerformanceStore.ts`: Reactive Zustand store tracking active tier, dynamic adaptive DPR, smoothed framerate, and automatic degradation logic (auto-downgrades tier if FPS sustained $<28\text{fps}$ for $\ge 3\text{s}$).
    - `AdaptivePerformanceController.tsx`: In-canvas controller continuously sampling render deltas, adjusting DPR dynamically, and syncing with Three.js `gl.setPixelRatio`.
    - `LODMesh.tsx`: Distance-based Level of Detail mesh selector with dynamic distance thresholds scaled by the active performance tier's `lodBias`.
    - `InstancedProps.tsx`: Instanced mesh batch renderer collapsing repeated 3D props into a single draw call.
    - `AssetCache.ts`: Central memory manager with in-flight request deduplication, texture anisotropy profiling, geometry/material reuse, and GPU disposal.
    - Integrated `AdaptivePerformanceController` into `Canvas3DFoundation` and wired tier buttons & adaptive DPR telemetry into `App.tsx`.
  - Automated Unit Tests: Created `performance.test.ts` (11/11 tests PASS) asserting profile parameters, adaptive DPR clamping, auto-degradation timing, texture quality limits, and asset disposal. Total frontend unit tests: 105/105 PASS.
  - Verified `npm run test`, `npm run typecheck`, and `npm run build` with exit code 0.
* **Summary of Checkpoint 08-AC (Visual States – Unified 8-State 3D Engine)**:
  - Designed and implemented the shared 8-state 3D visual state system in `frontend/src/engine/ui3d/visualStates/`:
    - `visualStateTypes.ts`: Defines `VisualState` (`IDLE`, `HOVER`, `ACTIVE`, `SELECTED`, `DISABLED`, `LOADING`, `SUCCESS`, `ERROR`), PBR parameter contracts (`primaryColor`, `backgroundColor`, `borderColor`, `textColor`, `emissiveColor`, `emissiveIntensity`, `scale`, `elevationZ`, `opacity`, `cursor`, `isInteractive`).
    - `visualStateEngine.ts`: Central mathematical resolver enforcing strict priority hierarchy:
      $\text{controlledState} \succ \text{DISABLED} \succ \text{LOADING} \succ \text{ACTIVE} \succ \text{SELECTED} \succ \text{HOVER} \succ \text{IDLE}$.
      Maps states to physical spring elevations ($-0.018\text{m}$ depression, $+0.025\text{m}$ hover, $+0.035\text{m}$ selection), scale punches ($1.04\times \to 1.08\times$), raycast interactivity guards, and procedural audio IDs.
    - `useVisualState.ts`: Reactive React hook connecting component props (`disabled`, `loading`, `isPressed`, `isHovered`, etc.) with state derivation, smooth transition interpolation, and automatic audio triggers.
    - Integrated with `ThreeDButton` and exported to `ui3d`.
    - Documented the shared 8-state specification table in `docs/3D_DESIGN_SYSTEM.md`.
  - Automated Unit Tests: Created `visualState.test.ts` (20/20 tests PASS) verifying precedence resolution, physical elevation targets, non-interactive guards, and sound mappings. Total frontend unit tests: 94/94 PASS.
  - Verified `npm run test`, `npm run typecheck`, and `npm run build` with exit code 0.
* **Summary of Checkpoint 08-O (ThreeDButton – WebGL Interactive Button Component)**:
  - Designed and implemented `ThreeDButton` in `frontend/src/engine/ui3d/ThreeDButton.tsx` strictly adhering to `docs/3D_DESIGN_SYSTEM.md`:
    - **WebGL-Native Geometry & Materials**: CyberMetal base chassis + beveled pressable button cap with physically based material (`metalness: 0.88`, `roughness: 0.18`), glowing wireframe rim stroke, and razor-sharp SDF 3D typography (`font="Orbitron"`).
    - **Physical Z-Depression & Levitation**: Smooth spring-damped frame interpolation for $-0.018\text{m}$ click depression, $+0.025\text{m}$ hover levitation, and $1.04\times$ scale expansion.
    - **Multi-State Lifecycle**: Full support for `IDLE`, `HOVER`, `ACTIVE`, `DISABLED` (desaturated matte gray `#12161f`, zero emission, disabled collider), `LOADING` (revolving holographic 3D spinner ring), `SUCCESS` (emerald green pulse `#00e676`), and `ERROR` (crimson red jitter `#ff1744`).
    - **Keyboard & Accessibility Support**: Keyboard activation on `Enter` / `Space` when `isFocused=true` or via customizable `shortcutKey`; high-contrast pulsing focus indicator ring in WebGL; accessible `ariaLabel` rendered as a 3D hover tooltip; spatial audio chimes.
    - **Raycasting**: High-priority UI interaction (`priority: 120`) with dynamic pointer/wait/not-allowed cursors.
  - Automated Unit Tests: Created `ThreeDButton.test.ts` (9/9 tests PASS) asserting state resolution, design system palette mapping, interactivity rules, and keyboard trigger logic. Total frontend unit tests: 74/74 PASS.
  - Backward compatibility: Preserved `Button3D` as a clean delegator to `ThreeDButton`.
  - Verified `npm run test`, `npm run typecheck`, and `npm run build` with exit code 0.
* **Summary of Checkpoint 08-N (UI3D Manager & Pure WebGL Interface Architecture)**:
  - Designed and implemented 100% WebGL-native 3D UI architecture in `frontend/src/engine/ui3d/` without any traditional HTML dashboards or overlays:
    - `ui3dTypes.ts`: Color palette tokens (`cyan`, `emerald`, `amber`, `crimson`, `neutral`) and interface contracts for panels, buttons, inputs, terminals, and modals.
    - `Panel3D.tsx`: Glassmorphic translucent dark slabs (`#0a1622`, 0.88 opacity) with cyber wireframe rim borders and top header banners.
    - `Button3D.tsx`: Interactive 3D button mesh with raycasting (`priority: 120`), hover extrusion ($+0.025\text{m}$ in Z), press depression ($-0.015\text{m}$ in Z), procedural audio feedback (`useSound().playClick()`), and Drei `<Text>`.
    - `Input3D.tsx`: WebGL-native text input capturing keyboard typing directly into the 3D text mesh without an HTML `<input>`. Synchronizes `isTypingMode` with `useInputStore` to prevent player movement during typing; includes 2.5Hz blinking 3D cursor mesh (`|`), keystroke blips, and password masking.
    - `Terminal3D.tsx`: Cyberpunk console kiosk with pedestal stand, base anchor ring, and display head tilted $-18^\circ$ for ergonomic viewing; integrated with `useFocusTarget` (`category: 'TERMINAL'`).
    - `Modal3D.tsx`: Spatial 3D modal space featuring a dimming backdrop plane ($40\text{m} \times 40\text{m}$) blocking raycast clicks to background scene geometry (`priority: 150`), opening scale pop ($0.8 \to 1.0$), close button (`X`), and `Escape` key dismissal.
  - Automated Unit Tests: Created `UI3DManager.test.ts` (6/6 tests PASS) asserting 3D modal stack operations (`openModal`, `closeModal`, `closeActiveModal`), terminal state, typing mode synchronization, and palette tokens. Total frontend unit tests: 65/65 PASS.
  - Integrated `Terminal3D` and `Modal3D` with passcodes and confirmation buttons directly into `DemoSecurityGateScene.tsx`.
  - Added 3D spatial interface controls and active modal telemetry in `App.tsx`.
  - Verified `npm run test`, `npm run typecheck`, and `npm run build` with exit code 0.
* **Summary of Checkpoint 08-M (Audio Manager & Procedural Synthesis Foundation)**:
  - Designed and implemented rights-free Web Audio API foundation in `frontend/src/engine/audio/`:
    - `audioTypes.ts`: Type definitions for `SoundCategory` (`UI`, `AMBIENT`, `TRANSITION`, `NOTIFICATION`), `ProceduralSoundId`, `SoundOptions`, and `AudioSettings`.
    - `proceduralSynth.ts`: 100% rights-free procedural audio generator avoiding external copyrighted assets:
      - `playHoverBlip`: High-frequency sine micro-chirp ($850\text{Hz} \to 1250\text{Hz}$, $35\text{ms}$).
      - `playClickSnap`: Crisp percussive pop ($540\text{Hz} \to 180\text{Hz}$, $45\text{ms}$).
      - `playWarpTransition`: Resonant frequency and low-pass filter upward sweep ($140\text{Hz} \to 780\text{Hz}$, $450\text{ms}$).
      - `playSuccessChime`: Ascending 3-note major triad chime ($C_5 \to E_5 \to G_5$, harmonic bells).
      - `playWarningBeep`: Dual alert pulse ($520\text{Hz} / 420\text{Hz}$).
      - `playErrorBuzz`: Descending sawtooth drop ($220\text{Hz} \to 75\text{Hz}$).
      - `createAmbientDrone`: Continuous sub-bass drone ($55\text{Hz} + 110.5\text{Hz}$ with low-pass sweep) with graceful fadeout.
    - `useAudioStore.ts`: Zustand store managing master volume, per-category volumes (`UI`, `AMBIENT`, `TRANSITION`, `NOTIFICATION`), and mute toggle.
    - `AudioEngine.ts`: Core singleton managing `AudioContext`, master GainNode, category GainNodes, buffer cache for lazy-loaded audio files, and user gesture auto-unlock.
    - `useSound.ts`: Clean React hook providing component-level sound triggers.
    - `AudioManager.tsx`: Canvas director subscribing to store changes and auto-dispatching notification sounds.
  - Automated Unit Tests: Created `AudioManager.test.ts` (7/7 tests PASS) asserting volume scaling, clamping, mute effective gain calculation, procedural dispatch, and disposal. Total frontend unit tests: 59/59 PASS.
  - Added interactive audio controls and live sound test triggers to `App.tsx`.
  - Verified `npm run test`, `npm run typecheck`, and `npm run build` with exit code 0.
* **Summary of Checkpoint 08-L (Animation Manager & Spatial State Engine)**:
  - Designed and implemented spatial animation subsystem in `frontend/src/engine/animation/`:
    - `animationTypes.ts`: Type definitions for `AnimationState` (`IDLE`, `HOVER`, `SELECTED`, `ACTIVE`, `LOADING`, `SUCCESS`, `ERROR`), `SpatialTransform3D`, `AnimationConfig`, and `ActiveAnimationTrack`.
    - `easing.ts`: Mathematical interpolation utilities including `springDamp` (frame-rate-independent spring integration), `computeShakeOffset` (decaying error wobble), `computeActivationScale` (punch compress-and-pop curve), and `sinePulse`.
    - `AnimationRegistry.ts`: Central track lifecycle manager classifying looping states (`HOVER`, `SELECTED`, `LOADING`) vs. finite transient states (`ACTIVE`: $350\text{ms}$, `SUCCESS`: $750\text{ms}$, `ERROR`: $500\text{ms}$). Automatically terminates finished transient tracks and auto-settles to `IDLE`, strictly preventing uncontrolled animation loops.
    - `useSpatialAnimation.ts`: Reusable React hook for any 3D mesh to receive live animated position offsets, scales, and emissive intensities with zero timer allocations.
    - `AnimationManager.tsx`: Global canvas director component.
  - Automated Unit Tests: Created `AnimationManager.test.ts` (8/8 tests PASS) asserting track classification, finite transient durations, auto-settle loop prevention, shake decay to exact zero, and activation curves. Total frontend unit tests: 52/52 PASS.
  - Integrated with `DemoSecurityGateScene` (biometric beacon levitates on hover, flashes emerald pulse on success) and updated HUD badge.
  - Verified `npm run test`, `npm run typecheck`, and `npm run build` with exit code 0.
* **Summary of Checkpoint 08-K (Focus Manager & Framing Director)**:
  - Designed and implemented spatial focus management subsystem in `frontend/src/engine/focus/`:
    - `focusTypes.ts`: Type definitions for `FocusCategory` (`VEHICLE`, `TERMINAL`, `PORTAL`, `OBJECT`), `FocusPreset`, `FocusTargetConfig`, and `SavedCameraState`.
    - `FocusRegistry.ts`: Pre-calibrated ergonomic framing presets:
      - `VEHICLE`: $5.8\text{m}$ distance, $+1.5\text{m}$ elevation, $45^\circ$ azimuth, $42^\circ$ FOV for full automotive digital twin inspection.
      - `TERMINAL`: $1.8\text{m}$ distance, $+0.2\text{m}$ elevation, $0^\circ$ azimuth, $34^\circ$ FOV for crisp readable 3D UI text.
      - `PORTAL`: $4.5\text{m}$ distance, $+1.2\text{m}$ elevation, $45^\circ$ FOV.
      - `OBJECT`: $3.2\text{m}$ distance, $+0.8\text{m}$ elevation, $40^\circ$ FOV.
    - `useFocusStore.ts`: Zustand store managing focal target resolution, bounding radius scaling, previous camera transform stack, and smooth restoration.
    - `useFocusTarget.ts`: Single-line React hook enabling future vehicles, terminals, portals, and objects to register their focal parameters.
    - `FocusManager.tsx`: Canvas director capturing `Escape` key / cancel actions to return smoothly to the previous camera position.
  - Automated Unit Tests: Created `FocusManager.test.ts` (5/5 tests PASS) asserting focus calculations, bounding radius adaptations, category presets, and previous camera restoration. Total frontend unit tests: 44/44 PASS.
  - Connected `FocusManager` to `App.tsx` and validated live focus target/category telemetry and Return Cam button.
  - Verified `npm run test`, `npm run typecheck`, and `npm run build` with exit code 0.
* **Summary of Checkpoint 08-J (Interaction Manager & Pipeline)**:
  - Designed and implemented 6-stage interaction pipeline in `frontend/src/engine/interaction/`:
    - `interactionTypes.ts`: Type definitions for `VisualInteractionState` (`IDLE`, `HOVER`, `ACTIVE`, `SELECTED`, `DISABLED`, `LOADING`, `SUCCESS`, `ERROR`), `InteractionContext`, `ValidationResult`, and `InteractionDefinition`.
    - `InteractionPipeline.ts`: Pipeline orchestrating Stage 1 (user input) $\to$ Stage 2 (raycast) $\to$ Stage 3 (target detection) $\to$ Stage 4 (permission & proximity validation) $\to$ Stage 5 (decoupled action) $\to$ Stage 6 (state update). Strictly enforces the invariant: zero backend business logic inside 3D mesh components.
    - `useInteractionState.ts`: React hook allowing 3D meshes to consume visual interaction states purely for rendering without embedding authorization logic.
    - `InteractionManager.tsx`: Core bridge linking raycast hover/click events to pipeline execution with automatic distance calculation.
  - Automated Unit Tests: Created `InteractionManager.test.ts` (7/7 tests PASS) verifying proximity range checks, RBAC role permission checks, disabled state blocks, decoupled action execution, and state transitions. Total frontend unit tests: 39/39 PASS.
  - Connected `InteractionPipeline` to `DemoSecurityGateScene` (biometric beacon registered with proximity checking and visual feedback) and updated HUD badge.
  - Verified `npm run test`, `npm run typecheck`, and `npm run build` with exit code 0.
* **Summary of Checkpoint 08-I (Raycast Manager & Spatial Interaction Priority)**:
  - Designed and implemented unified raycasting engine in `frontend/src/engine/raycast/`:
    - `raycastTypes.ts`: Type definitions for `RaycastHit`, `InteractableConfig`, and `InteractableCallbacks`.
    - `interactableRegistry.ts`: Spatial registry of interactive 3D meshes with hierarchy mapping, ensuring the raycaster only tests interactive colliders rather than the entire decorative scene graph (~95% cost reduction).
    - `raycastEngine.ts`: Interaction priority sorting engine (ranking: $\text{priority} \times 1000 - \text{distance}$) ensuring modal buttons/HUD elements always take precedence over background geometry. Includes dirty-checking algorithm (`shouldPerformRaycast`) that skips expensive intersection tests when pointer and camera are stationary.
    - `useInteractable.ts`: Single-line React hook registering any 3D mesh with the spatial raycasting subsystem.
    - `RaycastManager.tsx`: Core R3F component managing continuous hover enter/leave transitions, cursor mode switching (`POINTER`, `GRAB`, `TEXT`, `DEFAULT`), pointer down/up, and click/touch selection dispatch.
  - Automated Unit Tests: Created `RaycastManager.test.ts` (6/6 tests PASS) verifying hierarchy mapping, priority resolution over distance, distance resolution at equal priority, and stationary raycast skipping. Total frontend unit tests: 32/32 PASS.
  - Connected `RaycastManager` to active scenes (`DemoSecurityGateScene` biometric beacon with visual activation) and displayed live hover/selection target telemetry in HUD.
  - Verified `npm run test`, `npm run typecheck`, and `npm run build` with exit code 0.
* **Summary of Checkpoint 08-H (Player Movement & Collision Foundation)**:
  - Designed and implemented spatial 3D movement and collision subsystem in `frontend/src/engine/player/`:
    - `movementTypes.ts`: Type definitions for `RoomBounds`, `BoxCollider`, `CylinderCollider`, and `PlayerPhysicsConfig`.
    - `collisionEngine.ts`: Static obstacle collision engine featuring room perimeter clamping, cylinder obstacle penetration resolution (pushing along normal for radial sliding), and box obstacle penetration resolution (tangential wall sliding).
    - `PlayerAvatar.tsx`: 3D cybernetic capsule avatar mesh with visor heading indicator, chest energy core, ground shadow aura ring, and walking bobbing animation (automatically hidden in `FIRST_PERSON` mode).
    - `PlayerController.tsx`: Core R3F movement processor translating camera-relative WASD axes and touch pan gestures into velocity vectors with smooth acceleration, friction damping, smooth yaw rotation, and collision resolution (no HTML navigation controls).
    - `TeleportPad.tsx`: Interactive 3D spatial floor portal pad with rotating holographic ring, hover beam, and click-to-teleport activation updating player coordinates and camera framing.
  - Automated Unit Tests: Created `PlayerMovement.test.ts` (7/7 tests PASS) asserting room boundary clamping, cylinder obstacle sliding, box wall sliding, and teleportation. Total unit tests across frontend: 26/26 PASS.
  - Connected `PlayerController` and `TeleportPad` into active scenes and displayed live avatar coordinates in HUD.
  - Verified `npm run test`, `npm run typecheck`, and `npm run build` with exit code 0.
* **Summary of Checkpoint 08-G (Input Manager & Normalized Event System)**:
  - Designed and implemented unified input architecture in `frontend/src/engine/input/`:
    - `inputTypes.ts`: Type definitions for `InputAction`, `NormalizedPointer` (screen coords, NDC coordinates `[-1, 1]`, drag state, delta), `NormalizedMovement` (forward, strafe, sprint, jump), and `NormalizedTouch` (touch count, pinch distance, gestures).
    - `inputNormalizer.ts`: Pure normalization mathematics for pixel-to-NDC transformation with bounding client rect clamping, Pythagorean 2-point touch pinch calculations, and keyboard code-to-action mapping.
    - `inputDispatcher.ts`: Centralized spatial event broadcaster allowing 3D meshes to register interaction callbacks without attaching DOM event listeners to every object (strictly enforcing the non-duplication rule).
    - `useInputStore.ts`: Zustand store managing normalized pointer, movement, touch, and typing mode states.
    - `InputManager.tsx`: Central viewport listener handling `keydown`, `keyup`, `pointerdown`, `pointermove` (rAF throttled), `pointerup`, `touchstart`, `touchmove` (pinch/pan), and `touchend`. Automatically routes hardware keystrokes to active 3D inputs when typing mode is active without moving avatar.
  - Automated Unit Tests: Created `InputManager.test.ts` (12/12 tests PASS) verifying NDC coordinate transforms, WASD vector computation, touch pinch calculations, and `InputDispatcher` spatial hit routing. Total unit tests across frontend: 19/19 PASS.
  - Connected `InputManager` to `App.tsx` and validated live input telemetry (Pointer NDC, state, movement axes, touch gesture, and typing mode).
  - Verified `npm run test`, `npm run typecheck`, and `npm run build` with exit code 0.
* **Summary of Checkpoint 08-F (Camera Manager & Spatial Transitions)**:
  - Designed and implemented unified `CameraManager` in `frontend/src/engine/camera/`:
    - `cameraTypes.ts`: Configuration types for camera transforms, focus options, move options, and cinematic keyframe sequences.
    - `useCameraStore.ts`: Enhanced camera state with multi-mode targeting (`ORBIT`, `FIRST_PERSON`, `THIRD_PERSON`, `INSPECT`, `CINEMATIC`), current vs. desired transform vectors, and smooth interpolation speed tuning.
    - `CameraManager.tsx`: Frame-rate-independent smooth exponential dampening (`1 - exp(-speed * delta)`):
      - `FIRST_PERSON`: Eye-level camera (+1.7 units above avatar) looking along avatar yaw.
      - `THIRD_PERSON`: Over-the-shoulder chase camera (+2.2 units height, 4.5 units trailing) tracking player upper torso (+1.3 units).
      - `ORBIT`: Smoothly damped OrbitControls (damping 0.05, angle constraints) with butter-smooth glide to new vantage points.
      - `FOCUS_TARGET`: Dynamic inspection framing around any 3D entity with custom elevation, distance, and FOV.
      - `CINEMATIC_TRANSITION`: Multi-waypoint cinematic arc sequencer.
      - `RESET_CAMERA`: Smoothly glides back to default scene vantage point.
      - **Strict Invariant Enforced**: Zero hard camera teleports unless `immediate: true` is explicitly requested.
  - Automated Unit Tests: Created `CameraManager.test.ts` (7/7 tests PASS) verifying mode switches, interpolation invariants, focus targets, cinematics, and reset.
  - Wired interactive test controls into `App.tsx` (mode switches, Focus Target, Cinematic Sweep, Reset Camera).
  - Verified `npm run test`, `npm run typecheck`, and `npm run build` with exit code 0.
* **Summary of Checkpoint 08-E (SceneManager & Environment Switching Foundation)**:
  - Implemented reusable `SceneManager` architecture in `frontend/src/engine/scene/`:
    - `sceneTypes.ts`: Type definitions for `SceneDefinition`, `SceneEnvironmentConfig`, `SceneCameraConfig`, and `SceneLifecycleState` (`UNLOADED`, `INITIALIZING`, `ACTIVE`, `EXITING`, `DISPOSED`).
    - `SceneRegistry.ts`: Singleton registry managing registered scenes and dynamic sector mounting.
    - `resourceDisposal.ts`: Memory management utility traversing Three.js object graphs on scene exit to dispose geometries, materials, and uniform textures, preventing WebGL leaks.
    - `SceneManager.tsx`: Central orchestrator handling lifecycle transitions (`EXITING` -> GPU disposal -> `INITIALIZING` -> camera gliding & lighting reconfiguration -> `ENTERING` -> `ACTIVE`).
    - `SceneTransitionVeil.tsx`: Cyber-industrial loading overlay rendering progress bar (0-100%) and streaming status messages during environment switches.
    - `defaultScenes.ts` & Demo Scenes: Implemented 2 minimal sample scenes (`DemoSecurityGateScene` with `CYBER_NEON` profile and `DemoShowroomScene` with `CLEAN_DAYLIGHT` profile) to verify switching, camera adjustments, and lifecycle hooks without building full EVShare environments yet.
  - Wired `SceneManager` into `App.tsx` with interactive sector switching buttons in the foundation HUD.
  - Verified clean TypeScript typecheck (`tsc --noEmit`) and Vite production bundle (`tsc && vite build`) with exit code 0.
* **Summary of Checkpoint 08-D (State Foundation / Zustand)**:
  - Designed and implemented a modular, decoupled Zustand store architecture in `frontend/src/stores/` without monolithic state bloat:
    - `useAppStore.ts`: Application lifecycle, sector navigation (`currentSector`, `isTeleporting`), entity ID references (`selectedVehicleId`, `selectedGroupId`, etc.), and auth session identity (zero backend duplication).
    - `useWorldStore.ts`: 3D environment lighting profiles (`CYBER_NEON`, `CLEAN_DAYLIGHT`, etc.), room boundaries, ambient audio settings, and debug visualizer toggles.
    - `usePlayerStore.ts`: Avatar spatial coordinates `[x, y, z]`, Euler rotation, velocity vectors, movement modes (`IDLE`, `WALKING`, `SPRINTING`, `DRIVING`, `SEATED`), and vehicle seat occupancy.
    - `useCameraStore.ts`: Spatial camera modes (`ORBIT`, `FIRST_PERSON`, `THIRD_PERSON`, `INSPECT`, `CINEMATIC`), target look-at vectors, FOV, and smooth camera transition orchestration.
    - `useInteractionStore.ts`: 3D raycast targets (`hoveredObjectId`, `selectedObjectId`), focused input slot, 3D cursor modes (`DEFAULT`, `POINTER`, `GRAB`, `TEXT`), and spatial dragging states.
    - `useUI3DStore.ts`: Spatial 3D window/modal stack (strictly 3D meshes, not HTML popups), active terminal sessions, spatial virtual keyboard triggers, and 3D notification beacon queue.
    - `useLoadingStore.ts`: Fine-grained scoped loading key-value map (`startLoading(key)` / `stopLoading(key)`), asset progress percentage (0-100%), and streaming status messages.
    - `useErrorStore.ts`: Standardized error registry, fatal crash isolation, and retry callback execution.
  - Connected stores to `App.tsx` and validated live state telemetry in foundation HUD.
  - Verified clean TypeScript compilation (`tsc --noEmit`) and Vite production bundle (`tsc && vite build`) with exit code 0.
* **Summary of Checkpoint 08-C (Three.js Foundation)**:
  - Implemented reusable 3D engine foundation in `frontend/src/engine/`:
    - `Canvas3DFoundation.tsx`: Core R3F `<Canvas>` wrapper with ACESFilmicToneMapping, SRGBColorSpace, dynamic DPR scaling, shadows, and WebGL context loss/restoration handling.
    - `CameraRig.tsx`: PerspectiveCamera with OrbitControls, smooth damping (0.05), angle clamps, and responsive portrait/landscape FOV adaptation.
    - `LightingRig.tsx`: Multi-point cyber-industrial PBR lighting (Directional key with PCF shadow mapping, cyber-cyan fill, warm rim, and hemisphere ambient).
    - `SceneEnvironment.tsx`: Dark atmospheric fog (`#06070a`), clear color, and foundational ground shadow receiver plane.
    - `ErrorBoundary3D.tsx` & `WebGLFallback.tsx`: Resilient error boundary catching WebGL 2.0 / shader / context crashes with retry mechanism and diagnostic telemetry.
    - `engineStore.ts`: Zustand store managing 4 graphics quality presets (`LOW`, `MEDIUM`, `HIGH`, `ULTRA`), dynamic DPR, shadow maps, and resize metrics.
    - `FoundationProbe.tsx`: Minimal geometric diagnostic probe (rotating cyber prism & wireframe ring) verifying frame loop, shadows, and controls without implementing any EVShare world yet.
  - Optimized Rollup manual chunks (`vendor-react`, `vendor-three`, `vendor-r3f`, `index`) in `vite.config.ts`.
  - Validated typecheck (`tsc --noEmit`) and Vite production bundle (`tsc && vite build`) with exit code 0.
* **Summary of Checkpoint 08-B (Frontend Foundation)**:
  - Initialized and configured modern React 18 + TypeScript + Vite foundation in `frontend/`.
  - Configured `@` import alias mapping in both `vite.config.ts` and `tsconfig.app.json`.
  - Created strongly typed environment variable definitions in `src/vite-env.d.ts` and provided `.env` / `.env.example`.
  - Injected typography CDN preconnects (Orbitron, Space Grotesk, Inter, JetBrains Mono) into `index.html`.
  - Implemented full-viewport dark canvas foundation styles in `src/index.css`.
  - Validated clean TypeScript typecheck (`tsc --noEmit`) and Vite production bundle (`tsc && vite build`) with exit code 0.
* **Summary of Checkpoint 08-A (Frontend Audit)**:
  - Completed architectural audit and published [`docs/PHASE_08_FRONTEND_AUDIT.md`](file:///e:/EVShare3D/docs/PHASE_08_FRONTEND_AUDIT.md).
* **Backend Build Status (`mvn clean test`)**: **`BUILD SUCCESS`** (0 errors, 0 failures across all 1,145 backend tests)

* **Comprehensive Phase 07 Governance Test Suite**: [`com.example.evshare.controller.ComprehensivePhase07GovernanceTestSuiteTest`](file:///e:/EVShare3D/backend/src/test/java/com/example/evshare/controller/ComprehensivePhase07GovernanceTestSuiteTest.java) (15/15 PASS)
* **Dispute Fund Adjustment Integration Tests**: [`com.example.evshare.controller.DisputeFundAdjustmentIntegrationTest`](file:///e:/EVShare3D/backend/src/test/java/com/example/evshare/controller/DisputeFundAdjustmentIntegrationTest.java) (9/9 PASS)
* **Dispute Controller Integration Tests**: [`com.example.evshare.controller.DisputeIntegrationTest`](file:///e:/EVShare3D/backend/src/test/java/com/example/evshare/controller/DisputeIntegrationTest.java) (23/23 PASS)
* **Proposal Controller Integration Tests**: [`com.example.evshare.controller.ProposalIntegrationTest`](file:///e:/EVShare3D/backend/src/test/java/com/example/evshare/controller/ProposalIntegrationTest.java) (15/15 PASS)
* **Dispute Service Unit & Domain Tests**: [`com.example.evshare.service.DisputeServiceTest`](file:///e:/EVShare3D/backend/src/test/java/com/example/evshare/service/DisputeServiceTest.java) (70/70 PASS)
* **Dispute State Machine Unit Tests**: [`com.example.evshare.service.DisputeStateMachineTest`](file:///e:/EVShare3D/backend/src/test/java/com/example/evshare/service/DisputeStateMachineTest.java) (16/16 PASS)
* **Proposal State Machine Unit Tests**: [`com.example.evshare.service.ProposalStateMachineTest`](file:///e:/EVShare3D/backend/src/test/java/com/example/evshare/service/ProposalStateMachineTest.java) (15/15 PASS)
* **Voting Service Unit & Domain Tests**: [`com.example.evshare.service.VotingServiceTest`](file:///e:/EVShare3D/backend/src/test/java/com/example/evshare/service/VotingServiceTest.java) (93/93 PASS)
* **Total Phase 07 Tests**: **256 / 256 PASS** (15 master suite + 241 specialized domain tests). Total full repository test suite: **1,145 / 1,145 PASS**.
* **Summary of Checkpoint 07-Q (Final Verification)**:
  - Verified 100% test pass rate across the full repository (`mvn clean test` executed 1,145 tests with 0 failures, 0 errors, 0 skipped).
  - Validated all Phase 07 governance, voting, dispute mediation, admin arbitration, and fund adjustment requirements against code, tests, and documentation.
  - Updated project documentation:
    - `docs/API.md`: Updated Section 2.10 (Proposals & Voting) and Section 2.11 (Dispute Resolution, Evidence, Mediation, Arbitration, Fund Adjustment) with complete REST API catalogs.
    - `docs/BUSINESS_RULES.md`: Synchronized BR-VOT-01 through BR-VOT-04 and BR-DIS-01 through BR-DIS-06.
    - `agent/CURRENT_STATUS.md`: Recorded Phase 07 completion, test breakdown, and current checkpoint status.
    - `agent/DECISIONS.md`: Recorded ADR-19 (Democratic Governance & Voting), ADR-20 (Dispute Lifecycle & Mediation/Arbitration), and ADR-21 (Dispute Fund Adjustment & Treasury Integration).
    - `agent/KNOWN_ISSUES.md`: Documented concurrency mitigations for voting ballot races, dispute state transition locking, and fund adjustment overdraft rollbacks.
    - `agent/PHASE_07_REPORT.md`: Published formal Phase 07 verification report.
  - Zero new features introduced in Checkpoint 07-Q; truthful statuses only.
  - STOPPED. Phase 08 has not been started.
* **Summary of Checkpoint 07-P (Governance & Dispute Tests)**:
  - Executed full test suite verifying proposal eligibility, proposal lifecycle, vote casting, duplicate vote prevention, equity weighting, quorum calculation, decision thresholds, voting results privacy, dispute lifecycle, evidence immutability, staff mediation, admin arbitration, SharedFund transactional adjustments, RBAC enforcement, and transactional safety.
* **Summary of Checkpoint 07-O (Dispute Fund Adjustment)**:
  - Integrated dispute resolution with `SharedFund` treasury and ledger subsystem in compliance with `BR-DIS-06`.
  - Implemented atomic `@Transactional` boundary for dispute resolution and fund balance modification with pessimistic write locking.
  - Persisted immutable `FundTransaction` ledger records (`DISPUTE_ADJUSTMENT`), bidirectional entity linking, overdraft validation, and dual audit logging.
* **Summary of Checkpoint 06-O**:
  - Executed full suite verification (`mvn clean test`) across all 12 Phase 06 domains:
    1. **Expense**: Creation, validation, duplicate rejection (409 Conflict), audit trail logging.
    2. **Cost Allocation**: Allocation engine calculation, penny parity invariant ($\sum \text{shares} \equiv \text{total}$ down to 0.01 VND).
    3. **Ownership Allocation**: Proportional equity distribution (60/40) with deterministic penny absorption.
    4. **Usage Allocation**: Proportional telemetry usage allocation (25% / 75%) based on odometer distance logged.
    5. **Hybrid Allocation**: Combined dual-factor model adhering to BR-FIN-02 (30% fixed ownership + 70% variable usage).
    6. **Shared Fund**: Vault balance inquiries, minimum reserve threshold enforcement, and currency verification.
    7. **Fund Transactions**: Deposit/withdrawal operations, immutable transaction ledger, and mathematical balance reconciliation.
    8. **Payment Provider**: SPI registry dispatch across MOCK, BANK_TRANSFER, E_WALLET, GATEWAY with sandbox disclaimers.
    9. **Payment Lifecycle**: 6 canonical states, valid transitions, invalid transition rejections (409 Conflict), terminal state immutability.
    10. **Idempotency**: SHA-256 fingerprinting, duplicate request deduplication, and payload tampering rejection.
    11. **Rollback**: Full transactional rollback under downstream exception, ensuring zero balance drift or partial persistence.
    12. **Authorization**: RBAC & syndicate ACL data scoping (401 unauthenticated, 403 outsider forbidden, 200 co-owner permitted, staff/admin override).
  - Audited and hardened all financial transaction operations across the platform:
    1. **@Transactional Boundaries & Proxies**: Added class-level `@Transactional(rollbackFor = Exception.class)` and made execution methods public in `PaymentServiceImpl` and `PaymentLifecycleServiceImpl`, preventing Spring AOP proxy self-invocation bypass.
    2. **Pessimistic Concurrency**: Added `@Lock(LockModeType.PESSIMISTIC_WRITE)` methods to `PaymentRepository` (`findByIdWithLock`, `findByTransactionReferenceWithLock`) and enforced row-level locking during state transitions to serialize conflicting concurrent updates.
    3. **Cross-Entity Financial Consistency**: Coordinated atomic updates across `Payment`, `SharedFund`, and `ExpenseAllocation`:
       - On `SUCCESS`: Atomically marks `ExpenseAllocation.isSettled = true`, credits `SharedFund.currentBalance`, and inserts an immutable `FundTransaction` (`CREDIT`, `PAYMENT_SETTLEMENT`).
       - On `REFUNDED`: Atomically reverts `ExpenseAllocation.isSettled = false`, debits `SharedFund.currentBalance`, and inserts an immutable `FundTransaction` (`DEBIT`, `MANUAL_ADJUSTMENT`).
    4. **Zero Partial Financial State**: Verified full rollback semantics under downstream exceptions and overdraft rejections; zero dirty state or partial updates committed to MySQL.
    5. **Automated Test Suite**: Created [`FinancialTransactionSafetyIntegrationTest.java`](file:///e:/EVShare3D/backend/src/test/java/com/example/evshare/service/FinancialTransactionSafetyIntegrationTest.java) asserting 7 comprehensive scenarios across rollbacks, 10-thread concurrent updates, payment transition race conditions, and end-to-end multi-entity consistency.
* **Directive**: Checkpoint 06-N completed. STOPPED. Awaiting explicit user command for next checkpoint.
* **Summary of Checkpoint 06-L**:
  - Implemented the authoritative Payment Lifecycle State Machine and history preservation service:
    1. **Canonical States**:
       - `PENDING`: Initial checkout / initiation state.
       - `PROCESSING`: Intermediate asynchronous clearing / 3DS challenge.
       - `SUCCESS`: Settled funds captured (retains `COMPLETED` alias for backward compatibility).
       - `FAILED`: Terminal failure / card decline / insufficient funds.
       - `REFUNDED`: Terminal state for refunded / reversed transactions.
       - `CANCELLED`: Terminal state for user-cancelled / timed-out checkouts.
    2. **Transition Rules & Enforcement**:
       - Authoritative `PaymentStateMachine`: allows exactly 8 canonical valid transitions (`PENDING -> PROCESSING`, `PENDING -> SUCCESS`, `PENDING -> FAILED`, `PENDING -> CANCELLED`, `PROCESSING -> SUCCESS`, `PROCESSING -> FAILED`, `PROCESSING -> CANCELLED`, `SUCCESS -> REFUNDED`).
       - Strictly rejects all other 28 permutations, backwards transitions, self/redundant transitions, and terminal state mutations with `InvalidPaymentStateTransitionException` (HTTP 409 Conflict).
    3. **Payment History Preservation**:
       - `PaymentLifecycleServiceImpl` executes transactional state transitions by payment ID or transaction reference.
       - Every valid transition generates an immutable `AuditLog` entry storing `oldStateJson`, `newStateJson`, actor user ID, reference, amount, reason, and timestamp.
       - Provenance query endpoints: `getPaymentHistory` and `getPaymentHistoryByReference`.
    4. **Comprehensive Test Suite**:
       - Exhaustive 36-permutation test matrix asserting every valid and invalid state transition pair.
       - Full integration tests verifying multi-hop trajectories, database persistence, invalid transition state preservation, and chronological audit trail querying.
* **Directive**: Checkpoint 06-L completed. STOPPED. Awaiting explicit user command for next checkpoint.
* **Summary of Checkpoint 06-K**:
  - Implemented dedicated development/test mock payment provider simulation:
    1. **Core Scenarios**:
       - `SUCCESS`: Instant settlement or verification to `COMPLETED` with simulated authorization code.
       - `FAILURE`: Simulated decline to `FAILED` with customizable decline codes (`INSUFFICIENT_FUNDS`, `CARD_EXPIRED_OR_STOLEN`, etc.).
       - `PROCESSING`: Asynchronous in-progress state (`PENDING`) representing simulated clearing.
       - `REFUND`: Full or partial refund reversals to `REFUNDED` with reversal references, as well as simulated refund rejections.
    2. **Prominent Development/Test Marking (No Fake Production Claims)**:
       - Universal constant `DEVELOPMENT_DISCLAIMER`: `"DEVELOPMENT/TEST PAYMENT SIMULATION ONLY - NO REAL FINANCIAL TRANSACTION TOOK PLACE - NOT A REAL PAYMENT"`.
       - All initiation instructions, verification messages, and refund responses tagged with `"[TEST/SANDBOX ONLY - NOT A REAL PAYMENT]"`.
       - Structured metadata on every result: `isDevelopmentOrTest = true`, `simulationEnvironment = "SANDBOX_TEST"`.
       - Zero real credentials, real banking APIs, or claims of live fund movement.
    3. **Scenario Triggers**:
       - Metadata control (`simulationMode`, `simulatedStatus`, `failureReason`, `autoConfirm`, `simulateRefundFailure`).
       - Reference naming convention fallback (`-FAIL`, `-PROCESSING`, etc.).
* **Directive**: Checkpoint 06-K completed. STOPPED. Awaiting explicit user command for next checkpoint.
  - Implemented `PaymentProvider` abstraction SPI and dynamic registry decoupled from specific payment channels:
    1. **PaymentProvider SPI**:
       - Standardized methods: `initiate`, `verify`, `refund`, and `mapStatus`.
       - Standard command and result DTOs: `PaymentInitiationCommand`, `PaymentInitiationResult`, `PaymentVerificationCommand`, `PaymentVerificationResult`, `PaymentRefundCommand`, `PaymentRefundResult`.
    2. **Isolated Conceptual Providers (No Real External Credentials)**:
       - `MockPaymentProvider`: Sandbox simulation with deterministic auto-confirm/failure and simulated authorization codes.
       - `BankTransferPaymentProvider`: Domestic wire transfer simulation with VietQR format (`vietqr://pay?...`), transfer memo syntax (`EVSHARE TX-...`), account details, and statement reconciliation.
       - `EWalletPaymentProvider`: Mobile e-wallet simulation (MoMo/ZaloPay) with app deep-links (`evshare://ewallet/pay?...`), QR code payloads, and HMAC signature check simulation.
       - `GatewayPaymentProvider`: Hosted card checkout simulation (Credit Card / PayOS / Stripe) with checkout session redirect URLs (`https://checkout.gateway.evshare.io/pay/cs_test_...`) and card refund processing.
    3. **Provider Registry & Dynamic Routing**:
       - `PaymentProviderRegistry` Spring component discovering all registered providers.
       - Resolves providers by `PaymentProviderType` (`MOCK`, `BANK_TRANSFER`, `E_WALLET`, `GATEWAY`) and by customer `PaymentMethod` (`BANK_TRANSFER`, `E_WALLET`, `CREDIT_CARD`, `MOCK`, `GATEWAY`).
       - Graceful handling and rejection of unsupported provider requests with HTTP 400 Bad Request.
    4. **Status Mapping**:
       - Standardized vendor-specific status strings normalized to platform `PaymentStatus` (`PENDING`, `COMPLETED`, `FAILED`, `REFUNDED`).
* **Directive**: Checkpoint 06-J completed. STOPPED. Awaiting explicit user command for next checkpoint.
  - Implemented complete syndicate Shared Fund transaction ledger and balance reconciliation according to requirements:
    1. **CREDIT / DEBIT Semantics**:
       - Explicit `TransactionEntryType` (`CREDIT` for deposits/inflows, `DEBIT` for withdrawals/outflows).
       - Enforced across service, repository, entity, and DTO levels.
    2. **Immutable Transaction History**:
       - Database schema constraints with non-null `entry_type`, `transaction_reference`, and `source`.
       - JPA `@Column(updatable = false)` on ledger fields.
       - Entity lifecycle callbacks (`@PreUpdate`, `@PreRemove`) throwing `IllegalStateException` on any tampering.
    3. **Mathematical Balance Reconciliation**:
       - `reconcileFundBalance` audits historical transactions in strict chronological order.
       - Computes $\sum \text{CREDITS} - \sum \text{DEBITS} \equiv \text{currentBalance}$ down to 0.01 VND.
       - Generates `FundReconciliationResponse` with `isReconciled`, `calculatedLedgerBalance`, `reconciliationDelta`, credit/debit counts, and audit summary.
    4. **Transaction Reference & Uniqueness**:
       - Unique 64-char `transaction_reference` (e.g., `TX-CRD-YYYYMMDD-XXXXXXXX`, `TX-DBT-YYYYMMDD-XXXXXXXX`, or user-supplied reference code).
       - Duplicate reference detection with HTTP 409 Conflict.
       - Reference lookup endpoint `GET /api/v1/ownership-groups/{groupId}/fund/transactions/{reference}`.
    5. **Actor Attribution & Timestamps**:
       - Explicit co-owner actor attribution on every transaction and associated audit log.
       - Immutable `created_at` timestamp.
    6. **Source Classification**:
       - `FundTransactionSource` enum categorizing inflows and outflows (`MEMBER_CONTRIBUTION`, `EXPENSE_PAYOUT`, `CAPITAL_CALL_REPLENISHMENT`, `LATE_CANCELLATION_PENALTY`, `BATTERY_SURCHARGE`, `PAYMENT_SETTLEMENT`, `VAULT_INITIALIZATION`, `MANUAL_ADJUSTMENT`).
    7. **Comprehensive Concurrency & Ledger Testing**:
       - 10 concurrent threads executing simultaneous deposits and withdrawals with zero lost updates under pessimistic write locking.
       - Full verification of insufficient balance rejection and mathematical reconciliation equality.
* **Directive**: Checkpoint 06-I completed. STOPPED. Awaiting explicit user command for next checkpoint.
* **Summary of Checkpoints 06-D & 06-E**:
  - **06-D — Cost Allocation Engine**:
    1. Implemented strategy abstraction `CostAllocationService` & `CostAllocationStrategy` supporting `OWNERSHIP_BASED`, `USAGE_BASED`, and `HYBRID` modes.
    2. Deterministic execution: Verified via 1,000 continuous iterations test yielding identical output without variance.
    3. Documented rounding strategy: Banker's Rounding (`RoundingMode.HALF_EVEN`, scale 2). Residual pennies are absorbed deterministically by the highest active equity/usage holder with deterministic tie-breaking (lowest `userId`).
    4. Exact total reconciliation: Proved mathematically that $\sum_{i=1}^N \text{allocated}_i \equiv \text{totalExpenseAmount}$ down to 0.01 VND across all strategies and amounts.
    5. Explainability: Transparent audit explanation strings generated for every member share showing base calculation, ratios, percentages, and any residual rounding adjustments.
  - **06-E — Ownership-Based Allocation**:
    1. Active ownership only: Inactive shares (`isActive == false`) are strictly excluded from allocation calculation and persistence.
    2. 100.00% equity total invariant: Validated prior to calculation; throws `InvalidOwnershipDistributionException` if sum of active shares $\ne 100.00\%$.
    3. Tested multiple distributions: 50/50, 60/40, 33.33/33.33/33.34 (penny absorption), 70/20/10, 25/25/25/25 (4 equal shares with lowest `userId` tie-break), and odd/micro amounts.
    4. Database integration: Verified end-to-end persistence in `expense_allocations` table with exact ledger reconciliation.
  - Total Phase 06 tests: **57 / 57 PASS** (17 cost allocation unit tests, 4 cost allocation integration tests, 21 expense unit tests, 15 expense integration tests).
  - Total platform test suite: **725 / 725 PASS** (`BUILD SUCCESS` across all 48 test classes).
* **Directive**: Checkpoint 06-E completed. STOPPED. Awaiting explicit user command for next checkpoint.
* **Vehicle State Integration Tests**: [`com.example.evshare.controller.VehicleStateIntegrationTest`](file:///e:/EVShare3D/backend/src/test/java/com/example/evshare/controller/VehicleStateIntegrationTest.java) (5/5 PASS)
* **Usage Session Service Unit Tests**: [`com.example.evshare.service.UsageSessionServiceTest`](file:///e:/EVShare3D/backend/src/test/java/com/example/evshare/service/UsageSessionServiceTest.java) (37/37 PASS)
* **Usage Session Integration Tests**: [`com.example.evshare.controller.UsageSessionIntegrationTest`](file:///e:/EVShare3D/backend/src/test/java/com/example/evshare/controller/UsageSessionIntegrationTest.java) (18/18 PASS)
* **QR Validation Integration Tests**: [`com.example.evshare.controller.QrValidationIntegrationTest`](file:///e:/EVShare3D/backend/src/test/java/com/example/evshare/controller/QrValidationIntegrationTest.java) (10/10 PASS)
* **QR Validation Service Unit Tests**: [`com.example.evshare.service.QrValidationServiceTest`](file:///e:/EVShare3D/backend/src/test/java/com/example/evshare/service/QrValidationServiceTest.java) (22/22 PASS)
* **Fair Usage Service Unit Tests**: [`com.example.evshare.service.FairUsageServiceTest`](file:///e:/EVShare3D/backend/src/test/java/com/example/evshare/service/FairUsageServiceTest.java) (26/26 PASS)
* **Fair Usage Analytics Integration Tests**: [`com.example.evshare.controller.FairUsageIntegrationTest`](file:///e:/EVShare3D/backend/src/test/java/com/example/evshare/controller/FairUsageIntegrationTest.java) (5/5 PASS)
* **Booking State Machine Unit Tests**: [`com.example.evshare.service.BookingStateMachineTest`](file:///e:/EVShare3D/backend/src/test/java/com/example/evshare/service/BookingStateMachineTest.java) (67/67 PASS)
* **Booking State Transition Integration Tests**: [`com.example.evshare.controller.BookingStateTransitionIntegrationTest`](file:///e:/EVShare3D/backend/src/test/java/com/example/evshare/controller/BookingStateTransitionIntegrationTest.java) (18/18 PASS)
* **Conflict Detection Test Suite**: [`com.example.evshare.controller.BookingConflictDetectionIntegrationTest`](file:///e:/EVShare3D/backend/src/test/java/com/example/evshare/controller/BookingConflictDetectionIntegrationTest.java) (13/13 PASS)
* **Booking Update/Cancel Test Suite**: [`com.example.evshare.controller.BookingUpdateAndCancelIntegrationTest`](file:///e:/EVShare3D/backend/src/test/java/com/example/evshare/controller/BookingUpdateAndCancelIntegrationTest.java) (16/16 PASS)
* **Booking Creation Test Suite**: [`com.example.evshare.controller.BookingCreationIntegrationTest`](file:///e:/EVShare3D/backend/src/test/java/com/example/evshare/controller/BookingCreationIntegrationTest.java) (19/19 PASS)
* **Booking Availability Test Suite**: [`com.example.evshare.controller.BookingAvailabilityIntegrationTest`](file:///e:/EVShare3D/backend/src/test/java/com/example/evshare/controller/BookingAvailabilityIntegrationTest.java) (9/9 PASS)
* **Booking Repository Test Suite**: [`com.example.evshare.repository.BookingRepositoryTest`](file:///e:/EVShare3D/backend/src/test/java/com/example/evshare/repository/BookingRepositoryTest.java) (9/9 PASS)
* **Total Phase 05 Booking, Fair Usage, Session, QR & Vehicle State Integration Tests**: **274 / 274 PASS** (0 failures, 0 errors, 0 skipped)
* **Total Platform Tests**: **668 / 668 PASS** (0 failures, 0 errors, 0 skipped across all 45 test classes)
* **Summary of Checkpoint 05-O**:
  - Final verification of all Phase 05 functional, mathematical, and security requirements.
  - Zero new feature implementations; zero scope creep beyond Phase 05 boundaries.
  - Formally updated `docs/API.md` with complete endpoint specifications for bookings, usage sessions, QR, and fair usage analytics.
  - Formally updated `docs/BUSINESS_RULES.md` with operational clarifications for BR-OPS-01 and BR-OPS-02.
  - Audited `docs/DATABASE.md` against Phase 05 JPA entities and Flyway migrations (100% verified alignment).
  - Documented ADR-12 through ADR-15 in `agent/DECISIONS.md`.
  - Added Phase 05 risks and mitigations to `agent/KNOWN_ISSUES.md`.
  - Published definitive verification audit in `agent/PHASE_05_REPORT.md` (all 18 requirement dimensions rated PASS).
* **Directive**: STOP. Phase 05 is 100% verified and complete. Do NOT start Phase 06. Awaiting explicit user command.
* **Checkpoint 05-N Status**: **`COMPLETE`**
* **Checkpoint 05-M Status**: **`COMPLETE`**
* **Checkpoint 05-K Status**: **`COMPLETE`**
* **Checkpoint 05-J Status**: **`COMPLETE`**
* **Checkpoint 05-I Status**: **`COMPLETE`**
* **Checkpoint 05-H Status**: **`COMPLETE`**
* **Checkpoint 05-G Status**: **`COMPLETE`**
* **Checkpoint 05-F Status**: **`COMPLETE`**
* **Checkpoint 05-E Status**: **`COMPLETE`**
* **Checkpoint 05-D Status**: **`COMPLETE`**
* **Checkpoint 05-C Status**: **`COMPLETE`**
* **Checkpoint 05-B Status**: **`COMPLETE`**
* **Checkpoint 05-A Status**: **`COMPLETE`** ([`docs/PHASE_05_DOMAIN_AUDIT.md`](file:///e:/EVShare3D/docs/PHASE_05_DOMAIN_AUDIT.md))
* **Summary of Checkpoint 05-M**:
  - Integrated booking and usage session lifecycles with the authoritative `VehicleStateMachine`:
    1. Conceptual flow: strictly enforces and verifies `AVAILABLE -> BOOKED -> IN_USE -> AVAILABLE`.
    2. Cancellation: when a booking is cancelled, if the vehicle was transitioned to `BOOKED` for the impending trip, it is safely transitioned back `BOOKED -> AVAILABLE` via the controlled state machine, recording an audit log.
    3. Maintenance: vehicles placed in `MAINTENANCE` status reject new booking creation and check-in attempts; when repairs finish, transition `MAINTENANCE -> AVAILABLE` restores operational eligibility.
    4. Damage: when physical check-out inspection flags damage (`hasDamage=true`), vehicle transitions `IN_USE -> DAMAGED`, blocking booking attempts until inspected/serviced via `DAMAGED -> MAINTENANCE -> AVAILABLE`.
    5. Controlled transition guard: neither `BookingServiceImpl` nor `UsageSessionServiceImpl` directly mutates `vehicle.setStatus(...)` without validating against `vehicleStateMachine.validateTransition(current, target)`. Direct or illegal transitions (e.g. `DAMAGED -> AVAILABLE`, `MAINTENANCE -> IN_USE`, `DAMAGED -> IN_USE`, or redundant updates) throw `InvalidStateTransitionException` returning HTTP 409 Conflict.
    6. Complete lifecycle integration testing: implemented [`VehicleStateIntegrationTest`](file:///e:/EVShare3D/backend/src/test/java/com/example/evshare/controller/VehicleStateIntegrationTest.java) validating all 5 lifecycle cases against the live MySQL database.
* **Directive**: Checkpoint 05-M completed. STOPPED. Awaiting explicit user command for next checkpoint.
* **Summary of Checkpoint 05-L**:
  - Implemented backend QR code generation and cryptographic validation for vehicle operations per specification:
    1. Authenticated user: verified via SecurityContext / UserPrincipal; missing principal throws HTTP 401 Unauthorized.
    2. Vehicle: verified via authoritative DB query (status must be `AVAILABLE` or `BOOKED`), cross-matched against physical scanner station vehicle ID.
    3. Booking: verified via authoritative DB query (status must be `CONFIRMED` or `APPROVED`), cross-matched against QR claims and request booking ID.
    4. Time window: strictly verified against $[\text{startTime} - 15\text{m}, \text{startTime} + 30\text{m}]$; if scanned $>30$ minutes late, automatically transitions booking to `NO_SHOW` and saves to DB per BR-OPS-01.
    5. Authorization: caller must be the booking creator, an active co-owner in the vehicle's syndicate group, or a staff operator / admin; unauthorized access throws HTTP 403 Forbidden.
    6. Token/code validity: HMAC-SHA256 signature verified against platform secret key, structure and claims verified, token type must match `QR_CHECK_IN`.
    7. Expiration where applicable: strictly bounded to 5-minute TTL per BR-OPS-01.
    8. Never trust QR data alone: all payload claims are cross-checked against authoritative entities in MySQL database.
    9. Do not put sensitive information unnecessarily inside QR: token contains solely non-sensitive identifiers (`bookingId`, `vehicleId`, `userId`, `tokenType`, `jti`, `iat`, `exp`), with 0 PII, credentials, or financial data.
  - Comprehensive testing:
    - 22 unit tests in `QrValidationServiceTest` covering all guard failures, cryptographic signatures, time window boundaries, and automatic `NO_SHOW` transitions.
    - 10 integration tests in `QrValidationIntegrationTest` covering controller endpoints, DB mutations, security authorization, and negative test cases.
* **Directive**: Checkpoint 05-L completed. STOPPED. Awaiting explicit user command for next checkpoint.
* **Summary of Checkpoint 05-K**:
  - Implemented comprehensive check-out validation, telemetry and evidence capture, deterministic surcharge calculation, and transactional closure per specification:
    1. Active usage session guard: verifies session exists, status is `ACTIVE` (HTTP 400 otherwise), rejects rewriting completed sessions via `HistoricalUsageImmutableException` (HTTP 409), and verifies associated booking is in `IN_USE` status (HTTP 400 otherwise).
    2. Correct user guard: enforces authenticated context (HTTP 401), matches explicit `request.userId` (HTTP 400 on mismatch), and validates caller access via booking ownership or syndicate co-ownership/admin (HTTP 403 otherwise).
    3. Correct vehicle guard: verifies vehicle existence, matches explicit `request.vehicleId` (HTTP 400 on mismatch), and ensures vehicle is currently in `IN_USE` status (HTTP 400 otherwise).
    4. Telemetry & evidence guard: verifies non-negative end odometer, end odometer $\ge$ start odometer, and end battery SoC in $[0, 100]$; captures 3D defect mesh flags, condition notes, and photographic evidence.
    5. Deterministic additional surcharges: BR-OPS-02 low battery penalty (150,000 VND if $< 20\%$ SoC and unplugged), late return fee (50,000 VND / 30 min if $> 15$ min overdue), and explicit cleaning/damage costs.
    6. Transactional closure (`@Transactional`): closes session to `COMPLETED`, transitions booking to `COMPLETED` via `BookingStateMachine`, transitions vehicle to `MAINTENANCE` (if damaged), `CHARGING` (if plugged in), or `AVAILABLE` via `VehicleStateMachine`, updates vehicle odometer and battery, records `VehicleInspection` (`CHECK_OUT`), and writes immutable `AuditLog`.
  - Comprehensive testing:
    - 36 unit tests in `UsageSessionServiceTest` covering all guard failures, telemetry boundary conditions, and check-out flows.
    - 18 integration tests in `UsageSessionIntegrationTest` covering controller endpoints, DB mutations, security authorization, and duplicate/invalid checkouts.
* **Directive**: Checkpoint 05-K completed. STOPPED. Awaiting explicit user command for next checkpoint.
* **Checkpoint 04-F Status**: **`COMPLETE`**
* **Checkpoint 04-E Status**: **`COMPLETE`** ([`OwnershipGroupIntegrationTest`](file:///e:/EVShare3D/backend/src/test/java/com/example/evshare/controller/OwnershipGroupIntegrationTest.java), 14/14 PASS)
* **Checkpoint 04-D Status**: **`COMPLETE`** ([`VehicleStateMachineTest`](file:///e:/EVShare3D/backend/src/test/java/com/example/evshare/service/VehicleStateMachineTest.java) 51/51 PASS, [`VehicleStateTransitionIntegrationTest`](file:///e:/EVShare3D/backend/src/test/java/com/example/evshare/controller/VehicleStateTransitionIntegrationTest.java) 10/10 PASS)
* **Checkpoint 04-B Status**: **`COMPLETE`** ([`com.example.evshare.repository.VehicleRepositoryTest`](file:///e:/EVShare3D/backend/src/test/java/com/example/evshare/repository/VehicleRepositoryTest.java), 10/10 PASS)
* **Checkpoint 04-A Status**: **`COMPLETE`** ([`docs/PHASE_04_DOMAIN_AUDIT.md`](file:///e:/EVShare3D/docs/PHASE_04_DOMAIN_AUDIT.md))
* **Phase 03 Status**: **`COMPLETE`** (All 11 checkpoints 03-A through 03-K verified, 140/140 automated tests pass).
* **Phase 02 Status**: **`COMPLETE`** (All 10 checkpoints 02-A through 02-J verified).
* **Summary of Checkpoint 04-N**:
  - Final verification of all Phase 04 functional, structural, and architectural requirements.
  - Zero new features introduced; zero changes beyond Phase 04 boundaries.
  - Formally updated `docs/API.md` with complete endpoint specifications for vehicles, ownership groups, shares, and contracts.
  - Audited `docs/DATABASE.md` against Phase 04 JPA entities and Flyway migrations (100% verified alignment).
  - Documented ADR-08 through ADR-11 in `agent/DECISIONS.md`.
  - Added Phase 04 risks and mitigations to `agent/KNOWN_ISSUES.md`.
  - Published definitive verification audit in `agent/PHASE_04_REPORT.md` (all 13 requirement dimensions rated PASS).
* **Directive**: STOP. Phase 04 is 100% verified and complete. Do NOT start Phase 05. Awaiting explicit user command.

---

## 2. Phase 02 Final Audit & Verification Summary

* **Build & Verification**: `mvn clean verify` — **`BUILD SUCCESS`** (0 errors, 0 failures, repackaged JAR produced).
* **Automated Tests**: **`35 / 35 PASS`** (100% pass rate across 9 test classes in ~19 seconds).
* **Cross-Layer Schema Alignment**: 100% match across `DATABASE.md` (27 tables) ↔ JPA Entities (27 entities + 20 enums) ↔ Flyway Migrations (`V1`–`V7`) ↔ Spring Data Repositories (27 interfaces) ↔ Architecture layers.
* **Scope Compliance**: Strict adherence to Phase 02 boundaries; zero business logic, zero authentication/JWT code, zero frontend changes.

---

## 3. Deliverable Verification Checklist

### Documentation (`/docs`)
* [x] `docs/REQUIREMENTS.md` – Full System Requirements Specification.
* [x] `docs/BUSINESS_RULES.md` – Complete business rules.
* [x] `docs/ARCHITECTURE.md` – Full-stack system architecture.
* [x] `docs/RBAC.md` – Role-Based Access Control matrix & method security mapping.
* [x] `docs/API.md` – Complete REST API specification.
* [x] `docs/DATABASE.md` – 27 relational tables, constraints, indexes, nullability, defaults.
* [x] `docs/WORLD_ARCHITECTURE.md` – 12 pure 3D environments/sectors.
* [x] `docs/3D_DESIGN_SYSTEM.md` – 3D UI component specifications.
* [x] `docs/AI_SPECIFICATION.md` – AI Mobility Intelligence Center algorithms.
* [x] `docs/PHASE_02_DATABASE_AUDIT.md` – Exhaustive 27-table schema audit (`STATUS = READY`).
* [x] `docs/PHASE_02_FOUNDATION_REPORT.md` – Phase 02-B backend foundation report (`BUILD SUCCESS`).
* [x] `docs/PHASE_02_CONFIGURATION_REPORT.md` – Phase 02-C application configuration report (`BUILD SUCCESS`).
* [x] `docs/PHASE_02_API_FOUNDATION_REPORT.md` – Phase 02-D backend API foundation report (`BUILD SUCCESS`).
* [x] `docs/PHASE_02_ENTITY_MAPPING_REPORT.md` – Phase 02-E JPA entity mapping report (`PASS`).
* [x] `docs/PHASE_02_MIGRATION_REPORT.md` – Phase 02-F Flyway database migration report (`BUILD SUCCESS`).
* [x] `docs/PHASE_02_REPOSITORY_REPORT.md` – Phase 02-G Spring Data JPA repository report (`BUILD SUCCESS`).
* [x] `docs/PHASE_02_RUNTIME_REPORT.md` – Phase 02-H runtime and health verification report (`PASS`).
* [x] `docs/PHASE_02_TEST_REPORT.md` – Phase 02-I comprehensive foundation test report (`35/35 PASS`).
* [x] `docs/PHASE_02_FINAL_REPORT.md` – Phase 02-J final audit & completion report (`PHASE 02 STATUS = COMPLETE`).

### Agent & Project Governance (`/agent` & root)
* [x] `agent/AGENTS.md` – Authoritative agent execution guidelines (18 binding rules).
* [x] `agent/PHASE_01_REPOSITORY_AUDIT.md` – Repository audit report across 13 dimensions.
* [x] `agent/IMPLEMENTATION_PLAN.md` – Master 10-phase execution plan.
* [x] `agent/CURRENT_STATUS.md` – Project status tracker (`CURRENT_PHASE = PHASE 02-J`, `PHASE 02 STATUS = COMPLETE`).
* [x] `agent/DECISIONS.md` – Architecture Decision Records (ADR-01 to ADR-06).
* [x] `agent/KNOWN_ISSUES.md` – Technical risk catalog and mitigation strategies.
* [x] `agent/verify-phase01.js` – Automated Phase 01 Quality Gate test suite (100% pass rate).
* [x] `README.md` – Master project presentation and sitemap.

---

## 4. Implementation Code Status
* **Backend Foundation, Configuration & API Foundation**: Complete.
* **JPA Entity Models & Enums**: Complete (27 entities, 20 enums).
* **Flyway Migration Scripts (V1–V7)**: Complete (7 SQL scripts created, packaged, and applied to MySQL).
* **Spring Data JPA Repositories**: Complete (27 interfaces created, compiled, and verified).
* **Foundation Integration Tests & Health Verification (Phase 02-H)**: Complete (Live MySQL & Actuator verified).
* **Comprehensive Foundation Tests (Phase 02-I)**: Complete (35/35 tests passed across 9 test classes).
* **Final Audit & Verification (Phase 02-J)**: Complete (`mvn clean verify` PASS, repackaged JAR produced).
* **Authentication DTO Layer (Phase 03-B)**: Complete (8 Request DTOs, 2 Response DTOs, 13 validation & security contract tests).
* **User Registration (Phase 03-C)**: Complete (POST /api/v1/auth/register, AuthService, BCrypt work factor 12, default ROLE_CO_OWNER, 8 integration tests).
* **User Authentication & Login (Phase 03-D)**: Complete (POST /api/v1/auth/login, AuthService.login, JwtTokenProvider, anti-enumeration protection, account status checks, 7 integration tests).
* **JWT Implementation & Filter (Phase 03-E)**: Complete (JJWT 0.12.5, TokenService, JwtTokenProvider, UserPrincipal, JwtAuthenticationFilter, 9 integration/security tests).
* **Refresh Token & Logout Lifecycle (Phase 03-F)**: Complete (POST /api/v1/auth/refresh, POST /api/v1/auth/logout, RefreshTokenStore, InMemoryRefreshTokenStore, RFC 6819 token rotation & reuse prevention, 10 integration tests).
* **Current User Profile (Phase 03-G)**: Complete (GET /api/v1/users/me, UserService, UserServiceImpl, UserController, anti-tampering, safe DTO, 8 integration tests).
* **Role-Based Access Control & Method Security (Phase 03-H)**: Complete (@EnableMethodSecurity, SecurityRoles constants, OwnershipSecurity ACL bean, Actuator protection, 20 authorization test scenarios).
* **Password Reset Foundation (Phase 03-I)**: Complete (POST /api/v1/auth/password-reset/request, POST /api/v1/auth/password-reset/confirm, PasswordResetTokenStore, DevPasswordResetNotifier, anti-enumeration, session revocation, BCrypt factor 12, 13 integration tests).
* **Comprehensive Security Test Suite (Phase 03-J)**: Complete (`ComprehensiveSecurityTestSuiteTest` asserting all 16 security dimensions: registration, duplicate registration, password hashing, login, invalid login, JWT validation, expired JWT, malformed JWT, refresh, logout, current user, RBAC, forbidden access, unauthenticated access, password reset foundation, sensitive data exposure; 16 integration tests, 140/140 total backend tests pass).
* **Final Phase 03 Verification (Phase 03-K)**: Complete (Full audit of 14 security dimensions, `agent/PHASE_03_REPORT.md` certified, `mvn clean test` 140/140 PASS).
* **Phase 04-B (Vehicle Model & Repository)**: Complete (`Vehicle` verified against `docs/DATABASE.md`, `VehicleRepository` query methods implemented, `VehicleRepositoryTest` 10/10 PASS, full test suite 150/150 PASS).
* **Phase 04-D (Vehicle State Machine)**: Complete (`VehicleStateMachine` implemented, 7 states, transactional pessimistic row locking, 51 unit tests PASS, 10 integration/concurrency tests PASS, full test suite 211/211 PASS).
* **Phase 04-E (Ownership Group)**: Complete (`OwnershipGroup` entity management, 1:1 vehicle relationship, membership relationship via `OwnershipShare`, `@ownershipSecurity` ACL data-scoping, `OwnershipGroupIntegrationTest` 14/14 PASS, full test suite 225/225 PASS).
* **Phase 04-F (Ownership Share)**: Complete (`OwnershipShare` entity management, percentage validation, transactional updates, active/inactive toggles, `OwnershipShareIntegrationTest` 11/11 PASS, full test suite 236/236 PASS).
* **Phase 04-G (Ownership 100% Validation)**: Complete (`BR-OWN-01` absolute 100.00% equity invariant enforced on create/update/remove, `InvalidOwnershipDistributionException`, pessimistic write locking concurrency control, equity transfer protocol, batch rebalance, `OwnershipValidationIntegrationTest` 18/18 PASS, full test suite 254/254 PASS).
* **Phase 04-H (Ownership History)**: Complete (`audit_logs` append-only provenance, `old_state_json` and `new_state_json` snapshots, deterministic chronological history retrieval, `OwnershipHistoryIntegrationTest` 10/10 PASS, full test suite 264/264 PASS).
* **Phase 04-I (Co-Ownership Contract)**: Complete (`CoOwnershipContract` lifecycle management, versioning, syndicate association, immutability beyond DRAFT, state machine transition validation, superseding activation, prohibition of historical data deletion, `ContractIntegrationTest` 17/17 PASS, full test suite 281/281 PASS).
* **Phase 04-J (Contract Signature)**: Complete (Signer identification and syndicate authorization, SHA-256 cryptographic terms & version hashing, duplicate signature prevention, automatic transition to SIGNED, historical preservation, `ContractSignatureIntegrationTest` 15/15 PASS, full test suite 296/296 PASS).
* **Phase 04-K (Contract Lifecycle)**: Complete (Authoritative `ContractStateMachine`, 7 canonical lifecycle states, 52-test exhaustive transition matrix validation, `ContractLifecycleIntegrationTest` 17/17 PASS, audit trail snapshots, full test suite 365/365 PASS).
* **Phase 04-L (REST API)**: Complete (Exposed and documented all Vehicle, Co-Ownership, and Contract REST APIs according to `docs/API.md`, strict RBAC, DTO validation, consistent `ApiResponse` / `PagedData` envelopes, OpenAPI docs, `VehicleApiControllerIntegrationTest` 16/16 PASS, full test suite 381/381 PASS).
* **Phase 04-M (Test Suite)**: Complete (`ComprehensivePhase04TestSuiteTest` validating all 13 domains: vehicle CRUD, vehicle states, ownership group, ownership share, ownership = 100%, invalid ownership, ownership history, contract creation, signatures, contract lifecycle, RBAC, validation, transactions; `mvn clean test` executed with 394/394 PASS, 0 failures, 0 errors, 0 skipped).
* **Phase 04-N (Final Verification)**: Complete (All Phase 04 requirements verified, zero new features, updated `docs/API.md`, verified `docs/DATABASE.md`, updated `agent/CURRENT_STATUS.md`, `agent/DECISIONS.md`, `agent/KNOWN_ISSUES.md`, and generated `agent/PHASE_04_REPORT.md` with PASS ratings across all 13 dimensions).
* **Frontend Source Code**: Complete. Fully integrated across all 13 metaverse sectors, 7 digital twin facets, and 12 REST API modules.
* **Phase 09 Verification**: Complete (All 28 checkpoints 09-A through 09-AB verified; 442/442 unit tests PASS; live browser E2E verified; 0 console errors; production bundle built).
* **Docker Configurations**: Scheduled for Phase 10.

---

## 5. Next Steps
STOP. PHASE 09 — PURE 3D WORLD & FULL INTEGRATION is officially COMPLETE and verified.
All 28 checkpoints (09-A through 09-AB) completed.
All 442 automated tests passing (100% pass rate, 0 failures, 0 errors, 0 skipped).
Quality Gate PASSED.
Do NOT start PHASE 10. Awaiting explicit user command for Phase 10.
