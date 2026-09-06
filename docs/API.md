# EVShare 3D – REST API SPECIFICATION

## 1. Global API Standards

* **Base URL**: `/api/v1`
* **Protocol**: HTTPS / RESTful JSON
* **Authentication**: Bearer Token via standard HTTP Header: `Authorization: Bearer <jwt>`
* **Date-Time Format**: ISO-8601 UTC string (`YYYY-MM-DDTHH:mm:ssZ`)
* **Pagination Standards**: Standard query parameters:
  * `page`: 0-indexed integer (default: 0)
  * `size`: integer (default: 20, max: 100)
  * `sort`: field name and direction (e.g., `createdAt,desc`)
* **Standard Response Envelope**:
```json
{
  "success": true,
  "message": "Operation completed successfully",
  "data": { ... },
  "timestamp": "2026-09-06T11:00:00Z"
}
```

---

## 2. API Endpoint Catalog

### 2.1. Authentication & Identity (`/api/v1/auth`)

| HTTP Verb | Path | Roles Allowed | Request Body | Description |
| :--- | :--- | :--- | :--- | :--- |
| `POST` | `/auth/register` | Public | `RegisterRequest` | Register new user account |
| `POST` | `/auth/login` | Public | `LoginRequest` | Authenticate & obtain Access + Refresh JWTs |
| `POST` | `/auth/refresh` | Public | `RefreshTokenRequest` | Exchange refresh token for fresh access token |
| `POST` | `/auth/logout` | Authenticated | `RefreshTokenRequest` | Invalidate active refresh token session |

### 2.2. Users & Profiles (`/api/v1/users`)

| HTTP Verb | Path | Roles Allowed | Request Body | Description |
| :--- | :--- | :--- | :--- | :--- |
| `GET` | `/users/me` | Authenticated | None | Retrieve authenticated user profile, roles, and groups |
| `PUT` | `/users/me` | Authenticated | `UpdateProfileRequest` | Update contact details, full name, 3D avatar preference |
| `GET` | `/users/{id}` | Staff, Admin | None | Retrieve user details by ID |

### 2.3. Digital Twin Vehicles (`/api/v1/vehicles`)

| HTTP Verb | Path | Roles Allowed | Request Body | Description |
| :--- | :--- | :--- | :--- | :--- |
| `GET` | `/vehicles` | Authenticated | Query params | List all accessible vehicles (paged) |
| `GET` | `/vehicles/{id}` | Authenticated | None | Get full digital twin vehicle details & state |
| `POST` | `/vehicles` | Admin | `CreateVehicleRequest` | Register new EV and assign 3D asset model |
| `PATCH` | `/vehicles/{id}/status` | Staff, Admin | `UpdateVehicleStatusRequest` | Transition status (`MAINTENANCE`, `CHARGING`, etc.) |
| `GET` | `/vehicles/{id}/telemetry` | Authenticated | None | Real-time battery SoC, odometer, and bay location |

### 2.4. Ownership Groups & Equity (`/api/v1/ownership-groups`)

| HTTP Verb | Path | Roles Allowed | Request Body | Description |
| :--- | :--- | :--- | :--- | :--- |
| `GET` | `/ownership-groups/{id}` | Authenticated | None | Retrieve group info, vehicle ID, and member share list |
| `GET` | `/ownership-groups/my-groups` | Co-Owner | None | List groups where user holds active equity |
| `POST` | `/ownership-groups` | Admin | `CreateGroupRequest` | Form a new co-ownership group |
| `POST` | `/ownership-groups/{id}/transfer-share` | Admin | `TransferShareRequest` | Execute validated equity transfer (validating 100%) |

### 2.5. Digital Contracts (`/api/v1/contracts`)

| HTTP Verb | Path | Roles Allowed | Request Body | Description |
| :--- | :--- | :--- | :--- | :--- |
| `GET` | `/contracts/group/{groupId}` | Co-Owner, Admin | None | Fetch current active contract and terms |
| `POST` | `/contracts/{id}/sign` | Co-Owner | `SignContractRequest` | Submit digital cryptographic signature |
| `GET` | `/contracts/{id}/signatures` | Co-Owner, Admin | None | View list of signed members and pending signers |

### 2.6. Reservations & 3D Booking Timeline (`/api/v1/bookings`)

| HTTP Verb | Path | Roles Allowed | Request Body | Description |
| :--- | :--- | :--- | :--- | :--- |
| `GET` | `/bookings/timeline` | Authenticated | `vehicleId`, `from`, `to` | Retrieve 3D timeline intervals & occupancy slots |
| `POST` | `/bookings` | Co-Owner, Admin | `CreateBookingRequest` | Create reservation with concurrency locking |
| `GET` | `/bookings/{id}` | Authenticated | None | View booking details |
| `POST` | `/bookings/{id}/cancel` | Co-Owner, Admin | None | Cancel booking subject to penalty rules |

### 2.7. Operations & Usage Sessions (`/api/v1/usage-sessions`)

| HTTP Verb | Path | Roles Allowed | Request Body | Description |
| :--- | :--- | :--- | :--- | :--- |
| `POST` | `/usage-sessions/generate-qr` | Co-Owner | `bookingId` | Generate signed 5-min QR check-in token |
| `POST` | `/usage-sessions/check-in` | Co-Owner, Staff | `CheckInRequest` | Verify QR token, record start odometer & battery |
| `POST` | `/usage-sessions/check-out` | Co-Owner, Staff | `CheckOutRequest` | Record return odometer, battery, and condition |
| `POST` | `/usage-sessions/{id}/inspections` | Staff | `InspectionRequest` | Log 3D physical defect coordinate flags |

### 2.8. Energy, Expenses & Cost Allocation (`/api/v1/expenses`)

| HTTP Verb | Path | Roles Allowed | Request Body | Description |
| :--- | :--- | :--- | :--- | :--- |
| `GET` | `/expenses/group/{groupId}` | Authenticated | Query params | List group expenses with allocation breakdowns |
| `POST` | `/expenses` | Staff, Admin | `CreateExpenseRequest` | Log new expense and execute allocation formula |
| `GET` | `/expenses/my-dues` | Co-Owner | None | List unpaid expense allocations for authenticated user |

### 2.9. Shared Fund Vault & Payments (`/api/v1/funds`, `/api/v1/payments`)

| HTTP Verb | Path | Roles Allowed | Request Body | Description |
| :--- | :--- | :--- | :--- | :--- |
| `GET` | `/funds/group/{groupId}` | Co-Owner, Admin | None | View 3D Vault balance and reserve status |
| `GET` | `/funds/{fundId}/transactions`| Co-Owner, Admin | Query params | List fund transactions (deposits, payouts) |
| `POST` | `/payments/initiate` | Co-Owner | `InitiatePaymentRequest` | Generate payment transaction reference |
| `POST` | `/payments/confirm` | Co-Owner, Staff | `ConfirmPaymentRequest` | Finalize payment & credit vault balance |

### 2.10. Decision Chamber & Voting (`/api/v1/proposals`)

| HTTP Verb | Path | Roles Allowed | Request Body | Description |
| :--- | :--- | :--- | :--- | :--- |
| `GET` | `/proposals/group/{groupId}` | Co-Owner, Admin | Status filter | List active/past group proposals |
| `POST` | `/proposals` | Co-Owner | `CreateProposalRequest` | Submit new voting proposal ($\ge 10\%$ equity) |
| `POST` | `/proposals/{id}/vote` | Co-Owner | `CastVoteRequest` | Cast APPROVE / REJECT / ABSTAIN vote |
| `GET` | `/proposals/{id}/results` | Co-Owner, Admin | None | View equity-weighted tally and passing state |

### 2.11. Dispute Resolution (`/api/v1/disputes`)

| HTTP Verb | Path | Roles Allowed | Request Body | Description |
| :--- | :--- | :--- | :--- | :--- |
| `GET` | `/disputes/group/{groupId}` | Authenticated | Status filter | List open or resolved group disputes |
| `POST` | `/disputes` | Co-Owner, Staff | `CreateDisputeRequest` | File dispute linked to session/expense |
| `POST` | `/disputes/{id}/resolve` | Staff, Admin | `ResolveDisputeRequest` | Issue formal resolution and ledger settlement |

### 2.12. AI Recommendations & Analytics (`/api/v1/ai`, `/api/v1/analytics`)

| HTTP Verb | Path | Roles Allowed | Request Body | Description |
| :--- | :--- | :--- | :--- | :--- |
| `GET` | `/analytics/fair-usage/{groupId}` | Authenticated | None | Retrieve group fairness scores and ratios |
| `GET` | `/ai/recommendations/{groupId}` | Authenticated | None | Retrieve AI mobility insights and suggestions |
| `POST` | `/ai/recommendations/{id}/ack` | Co-Owner | None | Acknowledge / dismiss AI suggestion |
