# PHASE 06 – ENERGY, FINANCE, COST ALLOCATION & SHARED FUND

Implement only PHASE 06.

Read:

```text
docs/REQUIREMENTS.md
docs/BUSINESS_RULES.md
docs/ARCHITECTURE.md
docs/DATABASE.md
docs/API.md
```

## OBJECTIVE

Build the complete financial, expense allocation, shared fund, and payment settlement subsystem.

---

# EXPENSES & COST ALLOCATION

Implement:

* `ExpenseService`
* `CostAllocationService`

Expense categories:

* `CHARGING`
* `PREVENTIVE_MAINTENANCE`
* `EMERGENCY_REPAIR`
* `INSURANCE`
* `INSPECTION`
* `CLEANING`

Allocation strategies:

* `OWNERSHIP_BASED`
* `USAGE_BASED`
* `HYBRID`

Critical rule:

Allocations must sum up exactly to the total expense amount. Use `DECIMAL(15, 2)` precision and handle rounding pennies deterministically.

---

# SHARED FUND VAULT

Implement:

* `FundService`
* Group shared fund tracking
* Liquidity threshold monitoring (e.g. 10,000,000 VND)
* Transaction ledger (deposits, payouts, expense offsets)
* Capital call generation when balance falls below threshold

---

# PAYMENT INTEGRATION ABSTRACTION

Implement:

* `PaymentService`
* Provider abstraction:
  * `BankTransferPaymentProvider`
  * `EWalletPaymentProvider`
  * `MockGatewayPaymentProvider`
* Payment transaction lifecycle:
  * `PENDING` -> `COMPLETED` / `FAILED`
* Audit logging for every financial movement

---

# API

Implement:

```text
POST /api/v1/expenses
GET  /api/v1/expenses/group/{groupId}
GET  /api/v1/expenses/my-dues
GET  /api/v1/funds/group/{groupId}
GET  /api/v1/funds/{fundId}/transactions
POST /api/v1/payments/initiate
POST /api/v1/payments/confirm
```

---

# TESTS

Test:

* Expense creation and strategy calculation (Ownership, Usage, Hybrid)
* Mathematical equality of allocations to total expense
* Fund deposit and balance updates
* Safety threshold alerts
* Payment completion and ledger crediting
* Concurrent transactions

Run:

```text
mvn clean test
```

Update:

```text
agent/CURRENT_STATUS.md
```

Then STOP.
