# EVShare 3D – ROLE-BASED ACCESS CONTROL (RBAC) SPECIFICATION

## 1. Security Principles

1. **Server-Side Enforcement**: Permissions in the 3D client are strictly visual hints (e.g. disabling a 3D button or hiding an interactive pedestal). The backend Spring Security engine enforces all access rules; client assertions are untrusted.
2. **Principle of Least Privilege**: Users are granted only the permissions essential for their operational role.
3. **Data Scoping (Ownership ACL)**: Even with permission to book or view expenses, a `CO_OWNER` can only access data belonging to the specific `OwnershipGroup` in which they hold active equity.

---

## 2. Roles Definition

| Role Identifier | Title | Description |
| :--- | :--- | :--- |
| `ROLE_CO_OWNER` | Co-Owner / Member | Fractional vehicle owner participating in vehicle usage, cost sharing, and group voting. |
| `ROLE_STAFF` | Operations & Field Staff | Garage technician and fleet attendant handling physical vehicle prep, inspections, check-ins, and service. |
| `ROLE_ADMIN` | Platform Administrator | Global platform operator possessing unrestricted oversight, user management, and dispute arbitration privileges. |

---

## 3. Comprehensive Permission Matrix

| Functional Module | Specific Permission | Co-Owner | Staff | Admin | Scope & Constraint Notes |
| :--- | :--- | :---: | :---: | :---: | :--- |
| **Authentication & Profile** | `AUTH_REGISTER` | ✅ | ❌ | ✅ | Public registration (default `CO_OWNER`) or Admin invite; self-assignment of Staff/Admin forbidden |
| | `AUTH_LOGIN` | ✅ | ✅ | ✅ | Standard JWT issuance; inactive accounts rejected with 403 |
| | `AUTH_REFRESH` | ✅ | ✅ | ✅ | Dual-token refresh with RFC 6819 rotation and server-side revocation tracking |
| | `AUTH_PASSWORD_RESET`| ✅ | ✅ | ✅ | Public flow with anti-enumeration protection; invalidates all sessions on success |
| | `USER_VIEW_SELF` | ✅ | ✅ | ✅ | `GET /users/me`: View own profile & credentials (anti-spoofing) |
| | `USER_UPDATE_SELF` | ✅ | ✅ | ✅ | Update password, phone, avatar |
| | `USER_VIEW_BY_ID` | ❌ | ✅ | ✅ | `GET /users/{id}`: Inspect user details restricted to Staff and Admin |
| | `USER_MANAGE_ALL` | ❌ | ❌ | ✅ | Lock user, change roles, inspect audit logs |
| **System & Monitoring** | `ACTUATOR_HEALTH` | ✅ | ✅ | ✅ | Public `/actuator/health/**` & `/actuator/info` for container orchestration |
| | `ACTUATOR_ADMIN` | ❌ | ❌ | ✅ | `/actuator/**` metrics and management endpoints restricted to Admin |
| **Vehicle Management** | `VEHICLE_VIEW_OWNED` | ✅ | ✅ | ✅ | Co-owner views vehicles in their group |
| | `VEHICLE_VIEW_ALL` | ❌ | ✅ | ✅ | Staff & Admin inspect total fleet |
| | `VEHICLE_CREATE` | ❌ | ❌ | ✅ | Register new EV into platform |
| | `VEHICLE_UPDATE_SPEC` | ❌ | ❌ | ✅ | Update model, VIN, hardware specs |
| | `VEHICLE_UPDATE_STATE` | ❌ | ✅ | ✅ | Staff marks `CHARGING` / `MAINTENANCE` |
| **Ownership & Equity** | `OWNERSHIP_VIEW_GROUP` | ✅ | ✅ | ✅ | View equity share distribution of group |
| | `OWNERSHIP_PROPOSE_TRANSFER` | ✅ | ❌ | ✅ | Propose selling or rebalancing shares |
| | `OWNERSHIP_MODIFY_SHARES` | ❌ | ❌ | ✅ | Admin executes final approved share adjustment |
| **Digital Contracts** | `CONTRACT_VIEW_GROUP` | ✅ | ✅ | ✅ | View co-ownership agreement text & versions |
| | `CONTRACT_SIGN` | ✅ | ❌ | ❌ | Sign legally binding digital contract |
| | `CONTRACT_CREATE_TEMPLATE` | ❌ | ❌ | ✅ | Create new master contract templates |
| **Booking & Reservations** | `BOOKING_VIEW_CALENDAR` | ✅ | ✅ | ✅ | Inspect 3D timeline availability |
| | `BOOKING_CREATE` | ✅ | ❌ | ✅ | Book vehicle within group & quota |
| | `BOOKING_CANCEL_OWN` | ✅ | ❌ | ✅ | Cancel reservation subject to BR-BKG-03 |
| | `BOOKING_CANCEL_ANY` | ❌ | ✅ | ✅ | Staff/Admin cancel for maintenance emergencies |
| **Vehicle Operations** | `SESSION_CHECK_IN_OWN` | ✅ | ❌ | ❌ | Co-owner initiates trip check-in |
| | `SESSION_CHECK_OUT_OWN` | ✅ | ❌ | ❌ | Co-owner finalizes return session |
| | `SESSION_VERIFY_STAFF` | ❌ | ✅ | ✅ | Staff scans QR to validate check-in/out |
| | `SESSION_LOG_INSPECTION` | ❌ | ✅ | ✅ | Staff records physical damages & battery |
| **Energy & Finance** | `EXPENSE_VIEW_GROUP` | ✅ | ✅ | ✅ | View group expenses & allocation splits |
| | `EXPENSE_CREATE` | ❌ | ✅ | ✅ | Staff/Admin log invoices (charging/repairs) |
| | `FUND_VIEW_BALANCE` | ✅ | ✅ | ✅ | View 3D Vault balance & transaction ledger |
| | `FUND_DEPOSIT_OWN` | ✅ | ❌ | ✅ | Contribute money to shared fund pool |
| | `PAYMENT_EXECUTE_OWN` | ✅ | ❌ | ❌ | Pay allocated expense or fund call |
| **Governance & Voting** | `VOTE_PROPOSAL_CREATE` | ✅ | ❌ | ✅ | Create repair/rule proposal ($\ge 10\%$ equity) |
| | `VOTE_CAST_OWN` | ✅ | ❌ | ❌ | Cast APPROVE / REJECT / ABSTAIN |
| | `VOTE_ADMIN_SUPERVISE` | ❌ | ❌ | ✅ | Close invalid votes, audit participation |
| **Dispute Resolution** | `DISPUTE_RAISE` | ✅ | ✅ | ✅ | File damage or late-return dispute |
| | `DISPUTE_RESOLVE_STAFF` | ❌ | ✅ | ✅ | Staff mediation & factual evidence upload |
| | `DISPUTE_ARBITRATE_ADMIN` | ❌ | ❌ | ✅ | Final binding financial arbitration |
| **AI Intelligence** | `AI_VIEW_INSIGHTS` | ✅ | ✅ | ✅ | View fair usage trends & battery health |
| | `AI_TRIGGER_ANALYSIS` | ❌ | ❌ | ✅ | Trigger batch fairness & allocation recompute |

---

## 4. Backend Spring Security Implementation Mapping

```java
// Example method security annotations:
@PreAuthorize("hasRole('ADMIN')")
public VehicleDto createVehicle(CreateVehicleRequest request) { ... }

@PreAuthorize("hasAnyRole('CO_OWNER', 'ADMIN') and @ownershipSecurity.isGroupMember(#groupId, principal.id)")
public List<BookingDto> getGroupBookings(Long groupId) { ... }

@PreAuthorize("hasRole('CO_OWNER') and @bookingSecurity.isBookingOwner(#bookingId, principal.id)")
public BookingDto cancelBooking(Long bookingId) { ... }

@PreAuthorize("hasAnyRole('STAFF', 'ADMIN')")
public UsageSessionDto verifyCheckIn(StaffCheckInRequest request) { ... }
```
