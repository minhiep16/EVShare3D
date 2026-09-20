# PHASE 06 – FINANCE, SHARED FUND & PAYMENT

Implement only PHASE 06.

## OBJECTIVE

Build the complete financial subsystem.

---

# EXPENSE

Support:

CHARGING

MAINTENANCE

INSURANCE

INSPECTION

CLEANING

REPAIR

PARKING

TOLL

OTHER

---

# COST ALLOCATION

Support:

OWNERSHIP_BASED

USAGE_BASED

HYBRID

Create:

```text
CostAllocationService
```

All financial calculations must be deterministic and testable.

---

# SHARED FUND

Implement:

* balance
* contribution
* withdrawal
* transaction history
* audit

Never modify financial history silently.

---

# PAYMENT

Create provider abstraction:

```text
PaymentProvider
```

Support:

* mock provider
* bank transfer abstraction
* e-wallet abstraction
* gateway abstraction

Payment statuses:

PENDING

PROCESSING

SUCCESS

FAILED

REFUNDED

CANCELLED

Add idempotency protection where appropriate.

---

# TESTS

Test:

* expense
* allocation
* fund balance
* transactions
* duplicate payment request
* payment failure
* payment success

Run:

```text
mvn clean test
```

Update API and database documentation.

Then STOP.
