# EVShare 3D — PHASE 06 FINANCE DOMAIN AUDIT REPORT

**Module**: Finance, Cost Allocation, Shared Fund Vault & Payment Subsystem
**Phase**: `PHASE 06 — ENERGY, FINANCE, COST ALLOCATION & SHARED FUND`
**Checkpoint**: `06-A — FINANCE AUDIT`
**Date**: September 2026
**Status**: **AUDIT COMPLETE / READY FOR IMPLEMENTATION**
**Auditor**: Antigravity Agent

---

## 1. Executive Summary & Objective

The primary objective of **Phase 06** is to engineer the financial core of the EVShare 3D platform. In a fractional co-ownership ecosystem for premium electric vehicles, equitable cost management, transparent treasury reserves, and automated financial settlements are critical to trust and long-term sustainability.

Phase 06 establishes:
1. **Expense Classification & Ledger**: Incurred vehicle bills classified into Fixed Overhead vs. Variable Operating expenses (`BR-FIN-01`).
2. **Deterministic Cost Allocation Engine**: Three mathematical allocation strategies (`OWNERSHIP_BASED`, `USAGE_BASED`, `HYBRID`) calculating exact individual liability with guaranteed mathematical equality:
   $$\sum_{i=1}^N \text{AllocatedAmount}_i = \text{TotalExpense}$$
   enforcing Banker's rounding (`RoundingMode.HALF_EVEN`) and deterministic residual penny absorption (`BR-FIN-02`).
3. **Shared Fund 3D Vault**: Group-level liquidity management, real-time balance tracking, minimum reserve threshold monitoring (10,000,000 VND / vehicle), and automated capital call triggers (`BR-FIN-03`).
4. **Multi-Provider Payment Gateway Abstraction**: Extensible payment processing supporting Bank Transfer (VietQR), E-Wallets (MoMo/ZaloPay), and Mock Sandbox Gateways with two-phase commit lifecycle (`PENDING` -> `COMPLETED` / `FAILED`).
5. **Cross-Domain Operational Settlement Integration**: Seamless offset linking between usage session penalties (BR-OPS-02 low battery & late return surcharges), booking cancellation fees (BR-BKG-03), and the shared fund ledger.

In accordance with Checkpoint `06-A` governance, **no feature code has been implemented** in this checkpoint. This document presents the exhaustive audit of existing foundational structures, identifies all technical gaps, and defines the Phase 06 execution blueprint.

---

## 2. Cross-Domain Entity Inspection & Analysis

### 2.1. `Expense` & `ExpenseAllocation`
* **JPA Entity Location**:
  - [`Expense.java`](file:///e:/EVShare3D/backend/src/main/java/com/example/evshare/entity/Expense.java)
  - [`ExpenseAllocation.java`](file:///e:/EVShare3D/backend/src/main/java/com/example/evshare/entity/ExpenseAllocation.java)
* **Current State**:
  - `Expense` entity models `id`, `group` (ManyToOne `OwnershipGroup`), `title` (VARCHAR 150), `category` (Enum `ExpenseCategory`), `totalAmount` (`DECIMAL(15, 2)`), `allocationStrategy` (Enum `AllocationStrategy`), `invoiceReference` (VARCHAR 100), `loggedByUser` (ManyToOne `User`), `incurredDate` (`LocalDate`), and `createdAt` (`Instant`).
  - `ExpenseAllocation` entity models `id`, `expense` (ManyToOne `Expense`), `user` (ManyToOne `User`), `allocatedAmount` (`DECIMAL(15, 2)`), `isSettled` (`Boolean`), and `settledAt` (`Instant`).
* **Existing Repositories**:
  - [`ExpenseRepository.java`](file:///e:/EVShare3D/backend/src/main/java/com/example/evshare/repository/ExpenseRepository.java): defines `findByGroupId`, `findByGroupIdAndCategory`, `findByLoggedByUserId`.
  - [`ExpenseAllocationRepository.java`](file:///e:/EVShare3D/backend/src/main/java/com/example/evshare/repository/ExpenseAllocationRepository.java): defines `findByExpenseId`, `findByUserId`, `findByUserIdAndIsSettled`.
* **Gaps**:
  - Missing bidirectional `@OneToMany` or helper in `Expense` to manage cascade allocations if needed.
  - Missing aggregate query to compute total unsettled dues per co-owner across syndicates.
  - Missing pagination and date-range filtering for group expenses in `ExpenseRepository`.
  - Zero business service logic (`ExpenseService`, `CostAllocationService`).

### 2.2. `SharedFund` & `FundTransaction`
* **JPA Entity Location**:
  - [`SharedFund.java`](file:///e:/EVShare3D/backend/src/main/java/com/example/evshare/entity/SharedFund.java)
  - [`FundTransaction.java`](file:///e:/EVShare3D/backend/src/main/java/com/example/evshare/entity/FundTransaction.java)
* **Current State**:
  - `SharedFund` models `id`, `group` (OneToOne `OwnershipGroup`, unique), `currentBalance` (`DECIMAL(15, 2)`, default `0.00`), `minimumReserveThreshold` (`DECIMAL(15, 2)`, default `10000000.00`), `currency` (`VARCHAR(10)`, default `'VND'`), and `updatedAt`.
  - `FundTransaction` models `id`, `fund` (ManyToOne `SharedFund`), `user` (ManyToOne `User`, nullable), `transactionType` (Enum `TransactionType`), `amount` (`DECIMAL(15, 2)`), `balanceAfter` (`DECIMAL(15, 2)`), `description` (`VARCHAR(255)`), and `createdAt`.
* **Existing Repositories**:
  - [`SharedFundRepository.java`](file:///e:/EVShare3D/backend/src/main/java/com/example/evshare/repository/SharedFundRepository.java): defines `findByGroupId`, `existsByGroupId`.
  - [`FundTransactionRepository.java`](file:///e:/EVShare3D/backend/src/main/java/com/example/evshare/repository/FundTransactionRepository.java): defines `findByFundId`, `findByFundIdOrderByCreatedAtDesc`, `findByFundIdAndTransactionType`.
* **Gaps**:
  - Missing pessimistic write locking query in `SharedFundRepository` (`SELECT ... FOR UPDATE` via `findWithLockingByGroupId` or `findByIdForUpdate`) to prevent concurrent deposit/withdrawal race conditions on the vault balance.
  - Missing automated liquidity status evaluation (`HEALTHY`, `LOW_LIQUIDITY`, `CRITICAL`).
  - Missing capital call tracking and pro-rata generation engine.
  - Zero business service logic (`FundService`).

### 2.3. `Payment`
* **JPA Entity Location**:
  - [`Payment.java`](file:///e:/EVShare3D/backend/src/main/java/com/example/evshare/entity/Payment.java)
* **Current State**:
  - Models `id`, `user` (ManyToOne `User`), `fund` (ManyToOne `SharedFund`), `expenseAllocation` (ManyToOne `ExpenseAllocation`, nullable), `amount` (`DECIMAL(15, 2)`), `paymentMethod` (Enum `PaymentMethod`), `transactionReference` (`VARCHAR(100)`, unique), `status` (Enum `PaymentStatus`, default `PENDING`), and `createdAt`.
* **Existing Repositories**:
  - [`PaymentRepository.java`](file:///e:/EVShare3D/backend/src/main/java/com/example/evshare/repository/PaymentRepository.java): defines `findByTransactionReference`, `findByUserId`, `findByFundId`, `findByUserIdAndStatus`, `existsByTransactionReference`.
* **Gaps**:
  - Missing payment gateway provider abstraction (`PaymentProvider` SPI) to decouple banking and e-wallet protocols.
  - Missing transactional confirmation and settlement pipeline that updates `Payment`, credits `SharedFund.currentBalance`, creates `FundTransaction`, and marks linked `ExpenseAllocation.isSettled = true`.
  - Zero business service logic (`PaymentService`).

### 2.4. Integration with `OwnershipShare`
* **Current State**:
  - `OwnershipShareRepository.findByGroupIdAndIsActiveTrue(groupId)` returns active equity certificates with validated percentages summing to exactly 100.00% (`BR-OWN-01`).
* **Role in Phase 06**:
  - `OWNERSHIP_BASED` allocation directly uses `share.getPercentage()` / 100.00.
  - Capital calls distribute required top-up funds to co-owners strictly pro-rata to their equity stake.
  - `HYBRID` allocation applies `share.getPercentage()` to the 30% fixed component.

### 2.5. Integration with `UsageSession` & `Booking`
* **Current State from Phase 05**:
  - `UsageSession` records actual driving metrics: `startOdometer`, `endOdometer`, `mileage` (`endOdometer - startOdometer`), `checkInTime`, `checkOutTime`, and `additionalCost`.
  - `UsageSession` captures itemized surcharges: BR-OPS-02 low battery penalty (150,000 VND), late return fee (50,000 VND / 30 min), cleaning/damage fees.
  - `Booking` enforces cancellation penalty (BR-BKG-03: 20% deduction within 12h) and estimated cost.
* **Role in Phase 06**:
  - `USAGE_BASED` allocation requires calculating distance logged by each co-owner across all completed sessions for the vehicle in the target billing window:
    $$\text{Ratio}_i = \frac{\text{DistanceLogged}_i}{\sum_{k=1}^N \text{DistanceLogged}_k}$$
  - Surcharges assessed during check-out must be billable to the co-owner and payable into the Shared Fund Vault.

---

## 3. Business Rule Mapping & Mathematical Precision

### 3.1. Cost Classification (`BR-FIN-01`)
* **Fixed Overhead Expenses**:
  - Mandatory Strategy: `OWNERSHIP_BASED`.
  - Categories: `INSURANCE`, `INSPECTION`, fixed parking, connectivity.
* **Variable Operating Expenses**:
  - Strategy: `USAGE_BASED` or `HYBRID`.
  - Categories: `CHARGING`, `PREVENTIVE_MAINTENANCE`, `EMERGENCY_REPAIR`, `CLEANING`.

### 3.2. Mathematical Allocation Strategies (`BR-FIN-02`)

#### Strategy 1: `OWNERSHIP_BASED`
$$\text{Allocated}_i = \text{round}\left( \text{TotalExpense} \times \frac{\text{percentage}_i}{100.00}, 2 \right)$$

#### Strategy 2: `USAGE_BASED`
* Over the specified billing period (e.g. preceding 30 days or billing month):
  $$\text{TotalDistance} = \sum_{k=1}^N \text{DistanceLogged}_k$$
* If $\text{TotalDistance} > 0$:
  $$\text{Allocated}_i = \text{round}\left( \text{TotalExpense} \times \frac{\text{DistanceLogged}_i}{\text{TotalDistance}}, 2 \right)$$
* **Zero-Usage Fallback**: If the vehicle logged 0 km during the billing window, the strategy safely degrades to `OWNERSHIP_BASED` to prevent division-by-zero errors.

#### Strategy 3: `HYBRID`
$$\text{FixedPart} = \text{TotalExpense} \times 0.30$$
$$\text{VariablePart} = \text{TotalExpense} \times 0.70$$
$$\text{Allocated}_i = \text{round}\left(\text{FixedPart} \times \frac{\text{percentage}_i}{100.00}, 2\right) + \text{round}\left(\text{VariablePart} \times \frac{\text{DistanceLogged}_i}{\text{TotalDistance}}, 2\right)$$
*(With zero-usage fallback on the variable portion).*

### 3.3. Deterministic Residual Penny Absorption Algorithm
Because each co-owner's allocated share is rounded to 2 decimal places (`HALF_EVEN`), the sum of individual allocations may diverge from `TotalExpense` by residual cents (e.g. $\pm 0.01$ or $\pm 0.02$ VND):
$$\text{Residual} = \text{TotalExpense} - \sum_{i=1}^N \text{Allocated}_i$$
* **Invariant**: The residual $\Delta$ is deterministically allocated to the active co-owner with the highest equity percentage (or largest fractional remainder) to guarantee:
  $$\sum_{i=1}^N \text{Allocated}_i \equiv \text{TotalExpense}$$

### 3.4. Shared Fund Minimum Reserve & Capital Calls (`BR-FIN-03`)
* Minimum Reserve Threshold: **10,000,000.00 VND** per vehicle.
* When $\text{currentBalance} < \text{minimumReserveThreshold}$:
  $$\text{Deficit} = \text{minimumReserveThreshold} - \text{currentBalance}$$
  $$\text{CapitalCallDue}_i = \text{round}\left( \text{Deficit} \times \frac{\text{percentage}_i}{100.00}, 2 \right)$$
  *(Residual penny rule applied to capital calls).*

---

## 4. API Catalog & Specification Alignment

The endpoints specified in [`docs/API.md`](file:///e:/EVShare3D/docs/API.md) and [`PHASE 06.md`](file:///e:/EVShare3D/PHASE%2006.md):

| HTTP Verb | Path | Controller | Authorized Roles | Description |
| :--- | :--- | :--- | :--- | :--- |
| `POST` | `/api/v1/expenses` | `ExpenseController` | Staff, Admin | Record new vehicle expense bill & compute allocations |
| `GET` | `/api/v1/expenses/group/{groupId}` | `ExpenseController` | Co-Owner (Member), Staff, Admin | List group expenses with allocation breakdowns |
| `GET` | `/api/v1/expenses/my-dues` | `ExpenseController` | Authenticated (Co-Owner) | List all unsettled expense allocations for caller |
| `GET` | `/api/v1/funds/group/{groupId}` | `SharedFundController` | Co-Owner (Member), Staff, Admin | View 3D Vault balance, threshold, and status |
| `GET` | `/api/v1/funds/{fundId}/transactions`| `SharedFundController` | Co-Owner (Member), Staff, Admin | List chronological fund ledger transactions |
| `POST` | `/api/v1/payments/initiate` | `PaymentController` | Co-Owner (Member) | Generate payment transaction reference & gateway payload |
| `POST` | `/api/v1/payments/confirm` | `PaymentController` | Co-Owner, Staff, Admin | Verify gateway signature & execute atomic ledger settlement |

---

## 5. Security & RBAC Enforcement Mapping

In compliance with [`docs/RBAC.md`](file:///e:/EVShare3D/docs/RBAC.md):

1. **Expense Logging**:
   - Only `ROLE_STAFF` and `ROLE_ADMIN` can record expenses (`POST /api/v1/expenses`).
   - Co-owners cannot self-issue expense bills against syndicate accounts.
2. **Data-Scoping (Ownership ACL)**:
   - Co-owners can only view expenses, vaults, and fund ledgers for syndicates where they hold active equity (`@ownershipSecurity.isGroupMember(#groupId, principal.id)`).
   - Co-owners cannot view dues or payments belonging to other members.
3. **Payment Initiation**:
   - Only authenticated co-owners can initiate payments against their own allocations or vault contributions (`principal.id == request.userId`).
4. **Audit Logging**:
   - Every financial mutation (`EXPENSE_LOGGED`, `FUND_DEPOSIT`, `CAPITAL_CALL`, `PAYMENT_SETTLED`) must be recorded in the append-only `audit_logs` table.

---

## 6. Technical Gap Analysis & Implementation Requirements

| Subsystem | Existing Foundation | Identified Technical Gap | Required Implementation |
|---|---|---|---|
| **Expense Domain** | `Expense`, `ExpenseAllocation` entities & repositories | Zero business logic, zero DTOs, zero controllers | `ExpenseService`, `CostAllocationService`, `ExpenseController`, DTOs (`CreateExpenseRequest`, `ExpenseResponse`, `ExpenseAllocationResponse`) |
| **Allocation Engine** | Enums `AllocationStrategy`, `ExpenseCategory` | No calculation algorithms or rounding absorption | Strategy implementation (`OWNERSHIP_BASED`, `USAGE_BASED`, `HYBRID`) with residual penny absorption |
| **Shared Fund Vault** | `SharedFund`, `FundTransaction` entities | No balance management, no concurrency locking, no threshold checks | `FundService`, `SharedFundController`, pessimistic locking `findWithLockingByGroupId`, threshold alerts |
| **Capital Calls** | DB column `minimum_reserve_threshold` | No deficit detection or pro-rata assessment generation | Automated capital call generator in `FundService` pro-rata to equity shares |
| **Payment Gateway** | `Payment` entity & repository | No provider abstraction, no gateway simulation, no settlement transaction | `PaymentService`, `PaymentProvider` interface (`BankTransferPaymentProvider`, `EWalletPaymentProvider`, `MockGatewayPaymentProvider`), `PaymentController` |
| **Transactional Closure** | JPA mappings | Unsettled allocations remain open; fund balance unlinked | Atomic settlement: update `Payment` to `COMPLETED`, credit `SharedFund`, log `FundTransaction`, seal `ExpenseAllocation` |

---

## 7. Phase 06 Sub-Checkpoint Roadmap

```text
PHASE 06 — ENERGY, FINANCE, COST ALLOCATION & SHARED FUND
├── 06-A: Finance Audit (Current Checkpoint — COMPLETE)
├── 06-B: Cost Allocation Strategy Engine (Ownership, Usage, Hybrid algorithms)
├── 06-C: Expense Service & Creation Flow (Expense logging, allocation persistence)
├── 06-D: Expense Query & User Dues Endpoints (Group expenses, personal outstanding dues)
├── 06-E: Shared Fund Vault Management (Balance tracking, safety threshold monitoring)
├── 06-F: Fund Transaction Ledger (Chronological audit history, deposits, withdrawals)
├── 06-G: Capital Call Mechanics (Deficit triggers, pro-rata assessment notices)
├── 06-H: Payment Provider Abstraction (BankTransfer, EWallet, MockGateway providers)
├── 06-I: Payment Initiation Protocol (Payment creation, reference generation, idempotency)
├── 06-J: Payment Confirmation & Settlement (Atomic balance crediting, allocation clearing)
├── 06-K: Usage Session Surcharge Integration (Check-out surcharges to expense/fund linkage)
├── 06-L: Concurrency & Pessimistic Lock Protection (SELECT ... FOR UPDATE on SharedFund)
├── 06-M: Finance RBAC & Data-Scoping Verification (Method security and ownership ACL)
├── 06-N: Phase 06 Automated Test Suite (Comprehensive unit & integration test coverage)
└── 06-O: Final Verification & Sign-Off (Audits, reports, quality gate approval)
```

---

## 8. Audit Conclusion & Sign-Off

The financial domain audit for **Phase 06: Energy, Finance, Cost Allocation & Shared Fund** is complete.
- Foundational schemas (`expenses`, `expense_allocations`, `shared_funds`, `fund_transactions`, `payments`) and enums exist in the database from Phase 02 migrations.
- Business rules (`BR-FIN-01..03`, `BR-BKG-03`, `BR-OPS-02`), mathematical allocation formulas, and security boundaries have been verified.
- All implementation gaps have been cataloged and partitioned into sequential checkpoints.

**STATUS**: **AUDIT COMPLETE / READY FOR IMPLEMENTATION**
**STOP**: In accordance with instructions, no feature code has been implemented. Awaiting user instruction for Checkpoint `06-B`.
