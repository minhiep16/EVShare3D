# PHASE 01 – SPECIFICATION & SYSTEM ARCHITECTURE

You are now executing PHASE 01.

Do NOT implement the complete application yet.

Focus on system analysis and architecture.

## OBJECTIVE

Analyze the EVShare 3D platform and create the complete technical specification required for implementation.

The system is a PURE 3D INTERACTIVE application.

Frontend:

React + TypeScript + Three.js + React Three Fiber

Backend:

Java + Spring Boot

Database:

MySQL

Authentication:

JWT

Communication:

REST API

Deployment:

Docker

---

# TASK 1 – PROJECT INSPECTION

Inspect the entire repository.

Determine:

* existing files
* existing source code
* existing configuration
* existing dependencies
* existing database configuration
* existing frontend structure
* existing backend structure
* existing tests

Do not overwrite existing working code.

---

# TASK 2 – REQUIREMENTS

Create:

```text
docs/REQUIREMENTS.md
```

Document:

* system objectives
* actors
* functional requirements
* non-functional requirements
* business rules
* major workflows
* system constraints

Actors:

CO_OWNER

STAFF

ADMIN

---

# TASK 3 – BUSINESS RULES

Create:

```text
docs/BUSINESS_RULES.md
```

Define rules for:

* ownership
* ownership percentage
* booking
* booking conflicts
* fair usage
* check-in
* check-out
* expenses
* cost allocation
* shared funds
* voting
* disputes
* contracts
* payments

Do not leave critical business rules ambiguous.

---

# TASK 4 – SYSTEM ARCHITECTURE

Create:

```text
docs/ARCHITECTURE.md
```

Define:

Frontend architecture

Backend architecture

Database architecture

Authentication architecture

API architecture

3D architecture

Security architecture

Deployment architecture

---

# TASK 5 – RBAC

Create:

```text
docs/RBAC.md
```

Define permissions for:

CO_OWNER

STAFF

ADMIN

Create a detailed permission matrix.

---

# TASK 6 – API ARCHITECTURE

Create:

```text
docs/API.md
```

Define:

* API version
* endpoint groups
* request/response strategy
* validation
* error format
* pagination
* filtering
* authentication
* authorization

Do not implement all endpoints yet.

---

# TASK 7 – DATABASE ARCHITECTURE

Create:

```text
docs/DATABASE.md
```

Define:

* entities
* relationships
* primary keys
* foreign keys
* indexes
* constraints
* enums
* audit strategy

Do not generate all migrations yet.

---

# TASK 8 – 3D WORLD ARCHITECTURE

Create:

```text
docs/WORLD_ARCHITECTURE.md
```

Define:

* world structure
* environments
* rooms
* portals
* interactive objects
* player movement
* camera
* interaction system
* scene transitions

Required environments:

* EV Central Garage
* Co-Ownership Hall
* Booking Chamber
* Finance Center
* Shared Fund Vault
* Contract Room
* Decision Chamber
* AI Intelligence Center
* Operations Center
* Service Workshop
* Dispute Room
* Admin Command Center

---

# TASK 9 – 3D DESIGN SYSTEM

Create:

```text
docs/3D_DESIGN_SYSTEM.md
```

Define:

* materials
* lighting
* typography
* 3D buttons
* 3D inputs
* 3D panels
* 3D terminals
* hover states
* active states
* selected states
* loading states
* error states
* animations
* sound
* camera transitions

---

# TASK 10 – IMPLEMENTATION PLAN

Create:

```text
agent/IMPLEMENTATION_PLAN.md
agent/CURRENT_STATUS.md
agent/DECISIONS.md
agent/KNOWN_ISSUES.md
```

The implementation plan must be divided into:

PHASE 01 → PHASE 10.

---

# IMPORTANT

Do not start building the full frontend.

Do not start building the full backend.

Do not create random demo components.

First create the architecture and specification.

Then run a consistency review across:

Requirements

Business Rules

Database

API

RBAC

3D World

3D Design

## PHASE 01 QUALITY GATE

Phase 01 is complete only when:

[ ] Requirements documented

[ ] Business rules documented

[ ] Architecture documented

[ ] RBAC documented

[ ] API specification documented

[ ] Database design documented

[ ] 3D world documented

[ ] 3D design system documented

[ ] Implementation plan documented

[ ] No major contradiction between documents

At the end:

STOP.

Do not continue to Phase 02.
