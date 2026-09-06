# PHASE 04 – VEHICLE, CO-OWNERSHIP & CONTRACT

Implement only PHASE 04.

## OBJECTIVE

Implement the core business domain:

Vehicle

Ownership Group

Ownership Share

Co-Ownership Contract

---

# VEHICLE

Implement:

* create
* update
* view
* status
* battery
* vehicle metadata

Vehicle states:

AVAILABLE

BOOKED

IN_USE

CHARGING

MAINTENANCE

DAMAGED

UNAVAILABLE

---

# OWNERSHIP

Implement:

* ownership group
* owner membership
* ownership percentages
* ownership history
* ownership validation

Critical rule:

```text
Total ownership percentage = 100%
```

Validate this on the backend.

Use transactions.

---

# CONTRACT

Implement:

DRAFT

PENDING_SIGNATURE

SIGNED

ACTIVE

EXPIRED

TERMINATED

Support:

* contract creation
* version
* signature records
* activation
* history

Do not delete historical contract information.

---

# API

Implement appropriate REST endpoints.

Use DTOs.

Use validation.

---

# TESTS

Test:

* ownership = 100%
* invalid ownership
* ownership updates
* vehicle state
* contract lifecycle
* permissions

Run:

```text
mvn clean test
```

Update:

```text
docs/DATABASE.md
docs/API.md
docs/BUSINESS_RULES.md
agent/CURRENT_STATUS.md
```

Then STOP.
