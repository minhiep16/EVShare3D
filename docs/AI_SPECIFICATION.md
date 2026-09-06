# EVShare 3D – AI MOBILITY INTELLIGENCE SPECIFICATION

## 1. System Vision & Core Tenets

The **AI Mobility Intelligence Center** serves as an autonomous analytical advisor within the EVShare 3D metaverse. It continuously ingests vehicle telemetry, co-owner booking patterns, charging cycle logs, and financial expenditures to generate actionable recommendations.

### ⚠️ Non-Negotiable Boundary Rule: Zero Silent Mutations
The AI is strictly an **advisory engine**. It **must never** silently mutate, override, or finalize:
* Booking schedules or existing reservations
* Legal co-ownership contracts or addenda
* Financial ledger balances or automated debit deductions
* Ownership percentage allocations

All AI-generated proposals appear as interactive 3D holographic cards requiring explicit human acceptance, co-owner consensus, or vote passage in the Decision Chamber.

---

## 2. Intelligence Engines & Algorithmic Models

### 2.1. Fair Usage Equilibrium Advisor (`FairUsageOptimizer`)
* **Input Features**:
  * Co-owner equity shares ($E_1, \dots, E_N$)
  * 30-day trailing weighted hours ($W_1, \dots, W_N$)
  * Peak vs. off-peak consumption ratios
  * Cancellation & no-show counts
* **Objective Function**:
  Minimize variance in normalized fairness ratios: $\min \sum_{i=1}^N (FR_i - 1.0)^2$
* **Recommendation Outputs**:
  * Suggests specific upcoming booking windows (e.g. "Recommend reserving Saturday 08:00–12:00 for Owner C to restore equilibrium").
  * Proposes off-peak incentive credits for over-utilized members ($FR_i > 1.25$).

### 2.2. Battery Health & Degradation Forecaster (`BatteryHealthPredictor`)
* **Input Features**:
  * Total DC fast charging cycles vs. AC slow charging cycles
  * Average depth of discharge (DoD) per trip
  * High-temperature charging occurrences
  * Current State of Health (SoH %) telemetry
* **Algorithmic Model**:
  Linear degradation trend with penalty weights for frequent discharge below 10% SoC and fast-charging above 80% SoC.
* **Recommendation Outputs**:
  * Alerts co-owners if rapid degradation is detected: "Frequent charging to 100% on DC Fast Charger detected. Recommend capping daily charge to 80% to preserve pack longevity."
  * Automatically proposes a battery cooling service inspection when thermal variance spikes.

### 2.3. Predictive Maintenance & Consumables Model (`MaintenancePredictor`)
* **Input Features**:
  * Cumulative odometer kilometers
  * Severe braking and high-torque acceleration events logged by vehicle IMU
  * Elapsed calendar days since last tire rotation, brake fluid flush, and cabin air filter replacement
* **Recommendation Outputs**:
  * Spawns a 3D service notification drone when tire tread or brake pad thresholds are approached.
  * Formulates a pre-filled proposal in the Decision Chamber: "Routine 20,000 km Maintenance & Inspection – Estimated Cost: 3,500,000 VND".

### 2.4. Operating Cost & Energy Budget Forecaster (`EnergyCostForecaster`)
* **Input Features**:
  * Historical electricity tariffs (Peak, Standard, Off-Peak)
  * Projected monthly fleet distance based on advance reservations
  * Fixed recurring overheads (insurance, parking bay rent)
* **Recommendation Outputs**:
  * Projects total group expenditure for the following month.
  * Proposes the optimal Shared Fund replenishment contribution for each member.

---

## 3. 3D Spatial Representation in the AI Intelligence Center

1. **The AI Neural Sphere**:
   * A pulsating holographic sphere surrounded by orbiting data rings. Particle speed accelerates during real-time telemetry processing.
2. **Floating Recommendation Cards**:
   * Curved glass panels hovering around the center.
   * Priority Badging:
     * **Information (Cyan)**: Usage balance tips.
     * **Warning (Amber)**: Battery charge habit alerts.
     * **Critical (Red)**: Imminent service requirement or severe usage imbalance.
3. **Interactive Actions**:
   * Selecting a card allows the user to:
     * `APPLY RECOMMENDATION`: Pre-fills a booking or proposal draft.
     * `DISMISS / ACKNOWLEDGE`: Archives the recommendation with user feedback.
