# PHASE 05 – BOOKING, FAIR USAGE & VEHICLE OPERATION

Implement only PHASE 05.

## OBJECTIVE

Build the complete vehicle usage lifecycle.

---

# BOOKING

Implement:

* availability
* create booking
* update booking
* cancel booking
* booking history
* conflict detection

Statuses:

PENDING

APPROVED

CONFIRMED

IN_USE

COMPLETED

CANCELLED

REJECTED

---

# BOOKING CONFLICT

Backend must prevent overlapping bookings for the same vehicle.

Use transaction-safe logic.

---

# FAIR USAGE

Implement:

```text
FairUsageService
```

Consider:

* ownership percentage
* booking frequency
* usage duration
* distance
* cancellations
* recent usage

Return:

* fairness score
* imbalance level
* recommendation

---

# USAGE SESSION

Implement:

* check-in
* check-out
* mileage
* battery
* condition
* photos/evidence
* additional cost

---

# QR

Implement QR validation flow.

Backend must verify:

* user
* vehicle
* booking
* time
* permission

---

# VEHICLE STATE

Connect booking and usage state with vehicle state.

Example:

AVAILABLE
→ BOOKED
→ IN_USE
→ AVAILABLE

Invalid transitions must be rejected.

---

# TESTS

Test:

* booking conflict
* booking cancellation
* fairness calculation
* check-in
* check-out
* invalid state transitions
* QR authorization

Run all backend tests.

Update documentation.

Then STOP.
