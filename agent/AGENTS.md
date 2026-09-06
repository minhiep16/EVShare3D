# EVShare 3D — AGENTS.md

## 1. PROJECT IDENTITY

Project:

EVShare 3D — PURE 3D INTERACTIVE EV CO-OWNERSHIP PLATFORM

The application is a true 3D interactive web application.

The 3D world is the primary application environment.

Three.js / React Three Fiber is not decorative.

---

# 2. PHASE CONTROL — ABSOLUTE RULE

The project contains:

PHASE 01 → PHASE 10

Each Phase is an independent implementation boundary.

NEVER automatically continue to another Phase.

If the current Phase is PHASE 04:

You may implement PHASE 04 only.

Do NOT implement PHASE 05.

Do NOT prepare PHASE 06 implementation.

Do NOT silently create features belonging to later phases.

After completing the current Phase:

STOP.

Wait for explicit user instruction.

---

# 3. SPECIFICATION IS THE SOURCE OF TRUTH

Before modifying code:

Read the relevant Phase specification.

Read the relevant documents under:

`docs/`

Read relevant files under:

`agent/`

Never invent domain rules when the specification already defines them.

If two specifications conflict:

DO NOT GUESS.

Report the contradiction.

If the contradiction affects implementation correctness:

STOP.

---

# 4. IMPLEMENTATION WORKFLOW

Every Phase MUST follow:

READ
↓
AUDIT
↓
PLAN
↓
IMPLEMENT
↓
TEST
↓
VERIFY
↓
DOCUMENT
↓
STOP

Never skip the verification stage.

---

# 5. CURRENT PHASE ONLY

At the beginning of every task identify:

CURRENT PHASE = X

Before changing a file determine:

Does this file belong to the current Phase?

If NO:

Do not modify it unless the change is strictly required for compatibility.

If a compatibility change is necessary:

Report it explicitly.

---

# 6. DO NOT BREAK EXISTING WORK

Before editing:

Inspect existing code.

Do not overwrite working implementation blindly.

Prefer incremental modification.

Do not delete working functionality unless the current specification explicitly requires it.

---

# 7. PURE 3D RULE

The frontend must remain a PURE 3D INTERACTIVE application.

The following must NOT become the primary application UI:

* traditional navbar
* traditional sidebar
* traditional dashboard
* 2D CRUD pages
* conventional HTML forms
* HTML modal
* HTML overlay
* iframe-based UI

Primary workflows must exist inside the WebGL / Three.js environment.

Drei `<Html>` must not be used as the primary application UI.

HTML may only be used where technically necessary for accessibility, browser integration, debugging, fallback messaging, or non-primary infrastructure.

---

# 8. BACKEND AUTHORITY

Frontend permissions are never trusted.

Backend must enforce:

* authentication
* authorization
* ownership rules
* booking rules
* financial rules
* voting rules
* dispute rules
* state transitions

Never rely on frontend validation for security.

---

# 9. DATABASE RULE

`docs/DATABASE.md` is the authoritative database specification.

Do not invent:

* tables
* columns
* relationships
* constraints
* indexes
* enum values

unless the specification is explicitly updated.

Database changes must be deterministic.

Flyway is the schema migration authority.

JPA must not automatically modify the production schema.

---

# 10. FINANCIAL DATA

Financial operations must be transactional.

Money must not use floating-point types.

Use appropriate decimal representation.

Historical financial records must never be silently overwritten.

Payment operations must consider idempotency.

---

# 11. OWNERSHIP

Ownership percentages must satisfy:

TOTAL OWNERSHIP = 100%

Backend validation is mandatory.

Never trust frontend ownership calculations.

Historical ownership information must be preserved where required.

---

# 12. BOOKING

The backend must prevent conflicting bookings for the same vehicle.

Booking state transitions must be validated.

Never allow frontend-only conflict detection.

---

# 13. AI

AI recommendations must not silently modify:

* ownership
* contracts
* bookings
* payments
* financial records
* voting results

AI is recommendation/decision-support unless a specification explicitly defines an authorized automated action.

---

# 14. SECURITY

Never hard-code:

* production passwords
* JWT secrets
* API keys
* payment credentials

Use environment variables or secure configuration.

Never expose sensitive credentials in logs.

---

# 15. TESTING

Never claim a test passed unless it was actually executed.

If an external dependency is unavailable:

report:

`NOT VERIFIED`

Do not fabricate successful results.

---

# 16. ERROR HANDLING

When a build/test/runtime error occurs:

1. inspect the actual error
2. identify root cause
3. fix only what is necessary
4. rerun verification

Do not hide errors.

---

# 17. DOCUMENTATION

Important architectural or implementation decisions must be documented.

Use:

`agent/CURRENT_STATUS.md`

`agent/DECISIONS.md`

`agent/KNOWN_ISSUES.md`

Do not overwrite historical decisions without recording the change.

---

# 18. FINAL RESPONSE AFTER EACH PHASE

Report:

1. Current Phase
2. Files created
3. Files modified
4. Tests executed
5. Test results
6. Verification status
7. Known issues
8. Scope violations if any

Then:

STOP.

Never continue automatically.
