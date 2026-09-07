# EVShare 3D â€” PHASE 04 DOMAIN AUDIT REPORT
**Module**: Digital Twin Vehicles, Co-Ownership Syndicates & Digital Contracts
**Phase**: `PHASE 04 â€” VEHICLE, CO-OWNERSHIP & CONTRACT`
**Checkpoint**: `04-A â€” DOMAIN AUDIT`
**Date**: September 2026
**Status**: **AUDIT COMPLETE / READY FOR IMPLEMENTATION**

---

## 1. Executive Summary

This domain audit establishes the baseline architectural, domain relational, and business rule analysis for **Phase 04: Vehicle, Co-Ownership & Contract**.

Phase 04 builds directly upon the secure foundation created in Phase 02 (Database & JPA Foundations) and Phase 03 (Authentication, Authorization & RBAC). The objective of Phase 04 is to implement:
1. **Vehicle Domain & Digital Twin State Machine**: Vehicle registry, real-time telemetry, location stalls, and lifecycle transitions (`AVAILABLE`, `BOOKED`, `IN_USE`, `CHARGING`, `MAINTENANCE`, `DAMAGED`, `UNAVAILABLE`).
2. **Co-Ownership Syndicate & Equity Invariants**: Syndicate formation, share allocation, equity bounds (5.00% to 80.00%, 2 to 10 owners per group), right-of-first-refusal share rebalancing, and atomic validation guaranteeing $\sum \text{percentage} = 100.00\%$.
3. **Digital Contract Room & Cryptographic Signing**: Contract lifecycle (`DRAFT` $\to$ `PENDING_SIGNATURE` $\to$ `SIGNED` $\to$ `ACTIVE`), multi-party signature consensus (100% enrolled members), and SHA-256 cryptographic signature hashing.

In strict accordance with project governance ([`agent/AGENTS.md`](file:///e:/EVShare3D/agent/AGENTS.md)), **no feature implementation or business logic code was written during this checkpoint**.

---

## 2. Domain Entities & Relational Architecture

### 2.1. Entity Inspection

| Entity Class | Table Name | JPA Mapping & Identifier | Key Domain Fields | Status & Integrity |
|:---|:---|:---|:---|:---:|
| [`Vehicle.java`](file:///e:/EVShare3D/backend/src/main/java/com/example/evshare/entity/Vehicle.java) | `vehicles` | `@Entity`, `Long id` (`IDENTITY`) | `vin` (unique), `licensePlate` (unique), `modelName`, `manufacturer`, `model3dAssetPath`, `status` (`VehicleStatus`), `batteryLevel` (0â€“100), `odometerKm` (`BigDecimal(10,2)`), `stallLocationCode` | **`EXISTS / VERIFIED`** |
| [`OwnershipGroup.java`](file:///e:/EVShare3D/backend/src/main/java/com/example/evshare/entity/OwnershipGroup.java) | `ownership_groups` | `@Entity`, `Long id` (`IDENTITY`) | `groupName`, `vehicle` (`@OneToOne` via `vehicle_id` unique), `formationDate` (`LocalDate`), `isActive` (`Boolean`) | **`EXISTS / VERIFIED`** |
| [`OwnershipShare.java`](file:///e:/EVShare3D/backend/src/main/java/com/example/evshare/entity/OwnershipShare.java) | `ownership_shares` | `@Entity`, `Long id` (`IDENTITY`) | `group` (`@ManyToOne`), `user` (`@ManyToOne`), `percentage` (`BigDecimal(5,2)`), `shareCertificateNumber` (unique), `acquiredAt`, `isActive` | **`EXISTS / VERIFIED`** |
| [`CoOwnershipContract.java`](file:///e:/EVShare3D/backend/src/main/java/com/example/evshare/entity/CoOwnershipContract.java) | `co_ownership_contracts` | `@Entity`, `Long id` (`IDENTITY`) | `group` (`@ManyToOne`), `contractTitle`, `contractTermsText` (`LONGTEXT`), `version` (`Integer`), `status` (`ContractStatus`), `effectiveDate`, `expiryDate`, `createdAt` | **`EXISTS / VERIFIED`** |
| [`ContractSignature.java`](file:///e:/EVShare3D/backend/src/main/java/com/example/evshare/entity/ContractSignature.java) | `contract_signatures` | `@Entity`, `Long id` (`IDENTITY`) | `contract` (`@ManyToOne`), `user` (`@ManyToOne`), `signatureHash` (`VARCHAR(255)`), `signedAt` (`Instant`), `ipAddress` (`VARCHAR(45)`) | **`EXISTS / VERIFIED`** |
| [`User.java`](file:///e:/EVShare3D/backend/src/main/java/com/example/evshare/entity/User.java) | `users` | `@Entity`, `Long id` (`IDENTITY`) | `email`, `fullName`, `phoneNumber`, `passwordHash`, `isActive`, `roles` (`@ManyToMany`) | **`EXISTS / VERIFIED`** |
| [`Role.java`](file:///e:/EVShare3D/backend/src/main/java/com/example/evshare/entity/Role.java) | `roles` | `@Entity`, `Long id` (`IDENTITY`) | `name` (`RoleName`: `ROLE_CO_OWNER`, `ROLE_STAFF`, `ROLE_ADMIN`) | **`EXISTS / VERIFIED`** |

### 2.2. Enumeration Types

* **`VehicleStatus`**: `AVAILABLE`, `BOOKED`, `IN_USE`, `CHARGING`, `MAINTENANCE`, `DAMAGED`, `UNAVAILABLE`.
* **`ContractStatus`**: `DRAFT`, `PENDING_SIGNATURE`, `SIGNED`, `ACTIVE`, `EXPIRED`, `TERMINATED`, `REJECTED`.
* **`RoleName`**: `ROLE_CO_OWNER`, `ROLE_STAFF`, `ROLE_ADMIN`.

### 2.3. Flyway Migration Alignment

* **`V1__init_security_and_users.sql`**: Configures `users`, `roles`, and `user_roles`.
* **`V2__init_vehicles_and_ownership.sql`**:
  - `vehicles`: Configures unique VIN, unique license plate, check constraints on battery level `[0..100]` and odometer `[>= 0.00]`.
  - `ownership_groups`: One-to-one relationship with `vehicles(id)` through unique foreign key `vehicle_id`.
  - `ownership_shares`: Foreign keys to `ownership_groups(id)` and `users(id)`; check constraint on percentage `(0.00 < percentage <= 100.00)`; composite unique key `uk_share_group_user` on `(group_id, user_id)`.
* **`V3__init_contracts.sql`**:
  - `co_ownership_contracts`: Foreign key to `ownership_groups(id)`; long text storage for contract terms.
  - `contract_signatures`: Foreign keys to `co_ownership_contracts(id)` (`ON DELETE CASCADE`) and `users(id)` (`ON DELETE RESTRICT`); composite unique key `uk_contract_user_signature` on `(contract_id, user_id)`.

---

## 3. Business Rules & Mathematical Invariant Analysis

| Rule Code | Domain Rule | Specification Requirement | Implementation Mechanism in Phase 04 |
|:---|:---|:---|:---|
| **BR-OWN-01** | **Absolute 100% Equity Invariant** | Total equity shares across all active members of an `OwnershipGroup` must sum to exactly **100.00%**: $\sum_{i=1}^N \text{percentage}_i = 100.00\%$. Discrepancies (e.g. 99.99% or 100.01%) must trigger transaction rollback with a custom domain exception (`InvalidOwnershipDistributionException`). | Strict validation using Java `BigDecimal` with 2 decimal places and `compareTo(new BigDecimal("100.00")) == 0` within `@Transactional(rollbackFor = Exception.class)`. |
| **BR-OWN-02** | **Equity Bounds per Co-Owner** | Minimum stake: **5.00%**; Maximum stake: **80.00%**. Minimum co-owners: **2**; Maximum co-owners: **10** per vehicle syndicate. | Enforce in `OwnershipService` during group creation, member admission, and share transfer. |
| **BR-OWN-03** | **Equity Transfer & Rebalancing** | Transferring equity between users or reallocating shares must atomically debit the seller, credit the buyer, verify member count and share bounds, and assert the 100.00% sum before persisting. | Transactional `transferShare(...)` method with pessimistic or serializable checks. |
| **BR-CNT-01** | **Multi-Party Signature Consensus** | A co-ownership agreement requires digital signatures from **100% of enrolled group members** before transitioning from `PENDING_SIGNATURE` to `SIGNED` / `ACTIVE`. | Count active group members vs. verified signatures in `contract_signatures`. When `signatures.count == members.count`, auto-promote to `SIGNED`/`ACTIVE` with `effectiveDate = LocalDate.now()`. |
| **BR-CNT-02** | **Cryptographic Signature Hash** | Signatures must record a deterministic SHA-256 digest of contract version terms text + signatory user ID + signing timestamp. | Standard Java `MessageDigest.getInstance("SHA-256")` generating hex digest. |
| **State Machine** | **Vehicle State Machine** | Strict transitions: `AVAILABLE` $\leftrightarrow$ `BOOKED`, `BOOKED` $\to$ `IN_USE`, `IN_USE` $\to$ `AVAILABLE` / `CHARGING` / `MAINTENANCE` / `DAMAGED`. Direct arbitrary jumps are prohibited. | State transition validator within `VehicleService`. |

---

## 4. Current Codebase Gap Analysis

### 4.1. Repositories

| Repository | Current State | Required Custom Queries for Phase 04 |
|:---|:---|:---|
| [`VehicleRepository.java`](file:///e:/EVShare3D/backend/src/main/java/com/example/evshare/repository/VehicleRepository.java) | Basic lookup (`findByVin`, `findByLicensePlate`, `findByStatus`) | Need `findAll(Pageable)` for paged listing, `existsByVin`, `existsByLicensePlate`. |
| [`OwnershipGroupRepository.java`](file:///e:/EVShare3D/backend/src/main/java/com/example/evshare/repository/OwnershipGroupRepository.java) | `findByVehicleId`, `existsByVehicleId` | Need query to find groups by co-owner user ID via join with `ownership_shares`: `@Query("SELECT s.group FROM OwnershipShare s WHERE s.user.id = :userId AND s.isActive = true")`. |
| [`OwnershipShareRepository.java`](file:///e:/EVShare3D/backend/src/main/java/com/example/evshare/repository/OwnershipShareRepository.java) | `findByGroupId`, `findByUserId`, `findByGroupIdAndUserId`, `findByShareCertificateNumber` | Need query to sum active percentages for a group: `@Query("SELECT COALESCE(SUM(s.percentage), 0) FROM OwnershipShare s WHERE s.group.id = :groupId AND s.isActive = true")`. |
| [`CoOwnershipContractRepository.java`](file:///e:/EVShare3D/backend/src/main/java/com/example/evshare/repository/CoOwnershipContractRepository.java) | `findByGroupId`, `findByGroupIdAndStatus`, `findByGroupIdOrderByVersionDesc` | Sufficient for Phase 04 contract operations. |
| [`ContractSignatureRepository.java`](file:///e:/EVShare3D/backend/src/main/java/com/example/evshare/repository/ContractSignatureRepository.java) | `findByContractId`, `findByContractIdAndUserId`, `existsByContractIdAndUserId` | Need `long countByContractId(Long contractId)`. |

### 4.2. DTO Layer (Missing)

* **Vehicle DTOs**:
  - `CreateVehicleRequest` (`vin`, `licensePlate`, `modelName`, `manufacturer`, `model3dAssetPath`, `stallLocationCode`).
  - `UpdateVehicleStatusRequest` (`status`).
  - `VehicleResponse` (full digital twin details, battery SoC, odometer, status).
  - `VehicleTelemetryResponse` (lightweight real-time telemetry: battery level, odometer, stall location, updated timestamp).
* **Ownership Group DTOs**:
  - `CreateOwnershipGroupRequest` (`groupName`, `vehicleId`, `formationDate`, `List<InitialShareRequest>`).
  - `InitialShareRequest` (`userId`, `percentage`).
  - `TransferShareRequest` (`fromUserId`, `toUserId`, `percentage`).
  - `OwnershipGroupResponse` (`id`, `groupName`, `vehicleId`, `vehicleModel`, `formationDate`, `isActive`, `memberCount`, `List<OwnershipShareResponse>`).
  - `OwnershipShareResponse` (`id`, `userId`, `userFullName`, `percentage`, `shareCertificateNumber`, `acquiredAt`, `isActive`).
* **Contract DTOs**:
  - `CreateContractRequest` (`groupId`, `contractTitle`, `contractTermsText`, `expiryDate`).
  - `SignContractRequest` (empty body or acknowledgment flag; user identity taken from JWT principal).
  - `ContractResponse` (`id`, `groupId`, `contractTitle`, `contractTermsText`, `version`, `status`, `effectiveDate`, `expiryDate`, `createdAt`, `totalSignaturesRequired`, `signaturesSubmitted`).
  - `ContractSignatureResponse` (`id`, `contractId`, `userId`, `userName`, `signedAt`, `signatureHash`).

### 4.3. Service Layer (Missing)

* **`VehicleService` / `VehicleServiceImpl`**:
  - `VehicleResponse createVehicle(CreateVehicleRequest request)` [ADMIN]
  - `VehicleResponse getVehicleById(Long id)` [AUTHENTICATED]
  - `Page<VehicleResponse> listVehicles(Pageable pageable)` [AUTHENTICATED]
  - `VehicleResponse updateVehicleStatus(Long id, UpdateVehicleStatusRequest request)` [STAFF, ADMIN]
  - `VehicleTelemetryResponse getVehicleTelemetry(Long id)` [AUTHENTICATED]
* **`OwnershipService` / `OwnershipServiceImpl`**:
  - `OwnershipGroupResponse createGroup(CreateOwnershipGroupRequest request)` [ADMIN] (atomically asserts $\sum = 100.00\%$)
  - `OwnershipGroupResponse getGroupById(Long groupId)` [AUTHENTICATED & ACL]
  - `List<OwnershipGroupResponse> getMyGroups(Long userId)` [CO_OWNER]
  - `OwnershipGroupResponse transferShare(Long groupId, TransferShareRequest request)` [ADMIN] (atomically validates and rebalances)
  - `List<OwnershipShareResponse> getGroupShares(Long groupId)` [AUTHENTICATED & ACL]
* **`ContractService` / `ContractServiceImpl`**:
  - `ContractResponse createContract(CreateContractRequest request)` [ADMIN]
  - `ContractResponse getActiveContract(Long groupId)` [CO_OWNER & ACL, ADMIN]
  - `ContractSignatureResponse signContract(Long contractId, Long userId, String ipAddress)` [CO_OWNER] (SHA-256 digest calculation, auto-promotion to `SIGNED`/`ACTIVE` when 100% members sign)
  - `List<ContractSignatureResponse> getContractSignatures(Long contractId)` [CO_OWNER & ACL, ADMIN]

### 4.4. Controller Layer (Missing)

* **`VehicleController`**: `/api/v1/vehicles`
* **`OwnershipGroupController`**: `/api/v1/ownership-groups`
* **`ContractController`**: `/api/v1/contracts`

---

## 5. Security & RBAC Integration

Phase 04 controllers and services will bind directly against Phase 03 authorization architecture:
* **Central Constants**: `SecurityRoles.HAS_ROLE_ADMIN`, `SecurityRoles.HAS_ROLE_STAFF`, `SecurityRoles.HAS_ROLE_CO_OWNER`, `SecurityRoles.HAS_STAFF_OR_ADMIN`.
* **Ownership ACL**: Component [`OwnershipSecurity.java`](file:///e:/EVShare3D/backend/src/main/java/com/example/evshare/security/OwnershipSecurity.java) will be used in SpEL expressions:
  - `@ownershipSecurity.isGroupMember(#groupId, principal.id)`
  - `@ownershipSecurity.hasMinimumEquity(#groupId, principal.id, 5.00)`
* **Audit Metadata**: `@CreatedDate` and `@LastModifiedDate` JPA listeners automatically manage timestamps. Signatures capture client IP address and principal ID from `SecurityContextHolder`.

---

## 6. Recommended Phase 04 Execution Roadmap

1. **04-B â€” DTO Layer & API Contract**: Create all Request and Response DTOs with Bean Validation constraints.
2. **04-C â€” Vehicle Management Service & API**: Implement `VehicleService`, `VehicleServiceImpl`, `VehicleController`, and state machine validation.
3. **04-D â€” Ownership Group & 100% Invariant Service**: Implement `OwnershipService`, `OwnershipServiceImpl`, `OwnershipGroupController`, and 100.00% mathematical invariant verification.
4. **04-E â€” Equity Transfer Protocol**: Implement transactional share transfer, bounds validation (5%â€“80%, 2â€“10 owners), and rollback on discrepancy.
5. **04-F â€” Digital Contract Service & Multi-Party Consensus**: Implement `ContractService`, `ContractServiceImpl`, `ContractController`, SHA-256 signature hashing, and auto-activation when 100% of co-owners sign.
6. **04-G â€” Domain Integration & Security Verification**: Comprehensive integration tests covering happy paths, edge cases (99.99% vs 100.01%), illegal state transitions, and RBAC data scoping.

---
*Domain audit certified. System ready for next Phase 04 instructions.*
