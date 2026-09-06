# EVShare 3D – COMPREHENSIVE BUSINESS RULES SPECIFICATION

## 1. Ownership & Group Equity Rules

### BR-OWN-01: Absolute 100% Equity Invariant
* Every vehicle is bound to exactly one `OwnershipGroup`.
* The mathematical sum of all active `OwnershipShare.percentage` records within any group must equal exactly **100.00%** at all times:
  $$\sum_{i=1}^{N} \text{percentage}_i = 100.00\%$$
* Any transaction modifying share allocations (e.g., admitting a new owner, transferring equity, or buying out a member) must be executed within an ACID transaction that verifies this sum to 2 decimal places. If the sum $\neq 100.00\%$, the database transaction must automatically roll back with an `InvalidOwnershipDistributionException`.

### BR-OWN-02: Equity Bounds per Co-Owner
* Minimum permissible ownership stake: **5.00%**.
* Maximum permissible ownership stake: **80.00%** (to prevent monopolization in a co-ownership model).
* An ownership group must have a minimum of **2** and a maximum of **10** co-owners per vehicle.

### BR-OWN-03: Equity Transfer & Rebalancing Protocol
* An owner wishing to sell or reduce their share must first offer right-of-first-refusal to existing group members via a 3D Decision Proposal.
* If existing members decline within 14 calendar days, the share may be offered to external vetted buyers.
* All transfers require 100% digital signature compliance in the Digital Contract Room.

---

## 2. Vehicle Reservation & Booking Rules

### BR-BKG-01: Time Granularity & Booking Constraints
* Minimum booking duration: **30 minutes**.
* Maximum continuous booking duration: **72 hours** (unless approved by special group vote).
* Maximum advance reservation window: **30 days** in advance.
* Buffer period between consecutive bookings: A mandatory **30-minute turnaround buffer** is automatically enforced between consecutive bookings to accommodate cleaning, parking verification, and initial charging.

### BR-BKG-02: Concurrency & Overlap Prevention
* Two bookings for the same vehicle can **never** overlap:
  $$\text{Interval}_A = [start_A, end_A + buffer]$$
  $$\text{Interval}_B = [start_B, end_B + buffer]$$
  $$\text{Interval}_A \cap \text{Interval}_B = \emptyset$$
* Booking requests must be validated using pessimistic locking (`SELECT ... FOR UPDATE`) or serializable transaction isolation on the vehicle availability schedule to guarantee zero double-booking under race conditions.

### BR-BKG-03: Cancellation & No-Show Penalties
* **Free Cancellation**: Up to **12 hours** prior to reservation start time.
* **Late Cancellation** (within 12 hours of start): Incurs a 20% reservation fee deduction credited to the Shared Fund and logs a penalty mark against the co-owner's fair usage profile.
* **No-Show Rule**: If check-in is not executed within **30 minutes** of the scheduled start time, the reservation status transitions to `NO_SHOW`, the vehicle becomes available for emergency bookings, and the user is assessed a 100% time slot penalty.

---

## 3. Fair Usage Engine & Allocation Formula

### BR-FAIR-01: Fair Usage Quota & Metrics
The platform ensures that vehicle access is strictly equitable according to each member's equity share.
* **Equity Quota ($Q_i$)**: The target allocation of vehicle resources for Co-Owner $i$ over an evaluation window (e.g., monthly 720 hours):
  $$Q_i = \text{percentage}_i \times \text{AvailableOperatingHours}$$
* **Weighted Usage Units ($W_i$)**: Actual consumption weighted by time-of-day demand:
  $$W_i = \sum_{session} \left( \text{DurationHours} \times \text{DemandMultiplier} \right)$$
  * **Peak Demand (Fri 16:00 – Sun 22:00, Public Holidays)**: Multiplier = **1.5x**
  * **Standard Demand (Mon–Fri 07:00 – 16:00)**: Multiplier = **1.0x**
  * **Off-Peak Demand (Daily 22:00 – 07:00)**: Multiplier = **0.7x**

### BR-FAIR-02: Fairness Ratio ($FR_i$) & Imbalance Classification
* For each co-owner, the normalized fairness ratio is computed:
  $$FR_i = \frac{W_i / \sum_{k=1}^N W_k}{\text{percentage}_i / 100}$$
* **Classification Tiers**:
  * **$0.90 \le FR_i \le 1.10$**: `FAIR` (Green 3D Aura)
  * **$0.75 \le FR_i < 0.90$** or **$1.10 < FR_i \le 1.25$**: `SLIGHTLY_IMBALANCED` (Yellow 3D Aura)
  * **$0.50 \le FR_i < 0.75$** or **$1.25 < FR_i \le 1.50$**: `IMBALANCED` (Orange 3D Aura)
  * **$FR_i < 0.50$** or **$FR_i > 1.50$**: `SEVERELY_IMBALANCED` (Red 3D Aura)

### BR-FAIR-03: Dynamic Booking Priority Resolution
* During peak hours or competing booking submissions, co-owners with $FR_i < 1.0$ (under-users) receive priority scheduling rights and an early-booking window advantage (up to 48 hours prior to standard release).
* Co-owners with $FR_i > 1.30$ (over-users) are restricted to off-peak slots unless all other co-owners have explicitly forfeited interest.

---

## 4. Operational Check-In & Check-Out Rules

### BR-OPS-01: Cryptographic QR Check-In Protocol
* 15 minutes before booking start, the backend generates a signed, time-bounded QR token (valid for 5 minutes).
* To check-in:
  1. Co-owner approaches the 3D Check-In Station.
  2. Token is verified against server public key and booking state.
  3. Co-owner enters starting odometer and battery State of Charge (SoC).
  4. Co-owner inspects the 3D vehicle avatar to confirm pre-existing body conditions.
  5. Upon confirmation, vehicle state transitions: `BOOKED` $\to$ `IN_USE`.

### BR-OPS-02: Check-Out & Return Requirements
* Upon trip completion:
  1. Co-owner parks vehicle in the designated garage stall.
  2. Telemetry logged: final odometer, final battery SoC, return timestamp.
  3. Minimum Battery Rule: Vehicle must be returned with $\ge \mathbf{20\%}$ SoC unless plugged into an active charging station. Returning with $<20\%$ SoC without plugging in assesses an automatic charging service surcharge of 150,000 VND.
  4. Physical Inspection: Any new scratch, dent, or interior flaw must be marked on the 3D vehicle model and photographed.
  5. System computes actual usage hours and mileage against the booking schedule.
  6. Vehicle state transitions: `IN_USE` $\to$ `AVAILABLE` (or `CHARGING` / `MAINTENANCE` if flagged).

---

## 5. Cost Management & Expense Allocation Rules

### BR-FIN-01: Cost Classification
Every vehicle expenditure belongs to one of two categories:
1. **Fixed Overhead Expenses**:
   * Annual insurance, road tax, scheduled regulatory inspection, garage parking rent, connectivity subscriptions.
2. **Variable Operating Expenses**:
   * DC fast charging / AC charging power bills, tires, brake pads, wiper fluids, detailing / car wash, unexpected minor repairs.

### BR-FIN-02: Cost Allocation Strategies
The backend `CostAllocationService` implements three mathematical allocation models:
1. **`OWNERSHIP_BASED`**:
   $$\text{OwedAmount}_i = \text{TotalExpense} \times \left( \frac{\text{percentage}_i}{100} \right)$$
   *Mandatory for all Fixed Overhead Expenses.*
2. **`USAGE_BASED`**:
   $$\text{OwedAmount}_i = \text{TotalExpense} \times \left( \frac{\text{DistanceLogged}_i}{\sum \text{DistanceLogged}} \right) \quad \text{or} \quad \left( \frac{\text{HoursUsed}_i}{\sum \text{HoursUsed}} \right)$$
   *Mandatory for variable charging and tire/brake consumables.*
3. **`HYBRID`**:
   * Fixed base (30%) split by Ownership %, variable remainder (70%) split by proportional monthly mileage.

### BR-FIN-03: Shared Fund Vault & Minimum Liquidity Threshold
* Each Ownership Group maintains a dedicated `SharedFund` account in the vault.
* **Minimum Reserve Threshold**: 10,000,000 VND per vehicle.
* If the fund balance falls below this threshold:
  * The 3D Vault enters `LOW_LIQUIDITY` warning mode (amber pulsing illumination).
  * Automated capital call notices are generated and dispatched to all co-owners pro-rata to their equity stake.
  * Booking privileges are suspended if a co-owner's required contribution remains unpaid after 7 calendar days.

---

## 6. Group Governance & 3D Voting Rules

### BR-VOT-01: Proposal Lifecycle & Categories
Proposals may be initiated by any co-owner holding $\ge 10.00\%$ equity for the following actions:
* `ROUTINE_EXPENSE`: Repairs/upgrades between 2,000,000 VND and 10,000,000 VND.
* `MAJOR_EXPENSE`: Upgrades/repairs exceeding 10,000,000 VND (e.g., traction battery replacement).
* `OPERATIONAL_RULE_CHANGE`: Modifying peak hour definitions or buffer times.
* `OWNER_ADMISSION_OR_EXIT`: Approving new co-owner onboarding or share transfers.

### BR-VOT-02: Quorum & Passing Thresholds
* **Quorum Requirement**: Valid vote sessions require a minimum of **60.00%** total ownership equity participation.
* **Passing Thresholds**:
  * **Ordinary Business (Routine expenses)**: $> 50.00\%$ of participating equity weight votes `APPROVE`.
  * **Supermajority Business (Major upgrades, contract amendments)**: $\ge 75.00\%$ of total group equity votes `APPROVE`.
  * **Unanimous Business (Vehicle disposal, group dissolution)**: $100.00\%$ of total group equity votes `APPROVE`.
* Voting period default duration: **72 hours**.

---

## 7. Digital Contracts & Legal Validity Rules

### BR-CNT-01: Multi-Party Signature Integrity
* A Co-Ownership Agreement requires digital signatures from **100% of enrolled group members**.
* Status progression:
  $$\text{DRAFT} \to \text{PENDING\_SIGNATURE} \to \text{SIGNED} \to \text{ACTIVE}$$
* If any co-owner rejects the draft, the contract transitions to `REJECTED` and reverts to negotiation.
* Active contracts are immutable. Amendments must be spawned as child `ContractAmendment` entities.

---

## 8. Dispute Arbitration Rules

### BR-DIS-01: Dispute Escalation Path
* A dispute can be filed by any co-owner or staff within **24 hours** of check-out regarding damages, hygiene, late returns, or billing errors.
* Status progression:
  $$\text{OPEN} \to \text{UNDER\_REVIEW} \to \text{RESOLVED} \quad \text{or} \quad \text{ESCALATED}$$
* If peer co-owners fail to reach consensus within 5 calendar days, the dispute auto-escalates to `ADMIN_ARBITRATION`.
* Admin decision is final and automatically triggers ledger balance debits/credits or fairness score adjustments.
