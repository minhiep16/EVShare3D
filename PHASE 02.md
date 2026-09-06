# PHASE 02 – DATABASE & BACKEND FOUNDATION

Implement PHASE 02 only.

Read all documents under:

```text
/docs
/agent
```

before coding.

## OBJECTIVE

Build the backend foundation and MySQL database.

Technology:

Java

Spring Boot

Maven

Spring Data JPA

MySQL

Flyway

Bean Validation

OpenAPI

Actuator

---

# TASK 1

Initialize or verify:

```text
backend/
```

---

# TASK 2

Configure:

* application profiles
* environment variables
* MySQL connection
* JPA
* Flyway
* validation
* logging
* Actuator
* OpenAPI

---

# TASK 3

Create the initial domain entities according to DATABASE.md.

At minimum prepare the structure for:

User

Role

Vehicle

OwnershipGroup

OwnershipShare

CoOwnershipContract

Booking

UsageSession

Expense

SharedFund

Payment

Voting

Dispute

Notification

AuditLog

Do not invent fields that contradict DATABASE.md.

---

# TASK 4

Create Flyway migrations.

Use:

```text
V1__create_users.sql
V2__create_vehicles.sql
...
```

Keep migration order deterministic.

---

# TASK 5

Create:

* repositories
* base exception structure
* response model
* validation structure
* configuration

Do not implement complex business services yet.

---

# TASK 6

Create health endpoint.

Verify:

```text
GET /actuator/health
```

---

# TASK 7

Create OpenAPI foundation.

---

# TASK 8

Write backend unit/integration tests for the foundation.

---

# QUALITY GATE

Run:

```text
mvn clean test
```

The build must pass.

Verify:

* MySQL connection
* Flyway migration
* Spring Boot startup
* JPA initialization
* health endpoint
* OpenAPI startup

Update:

```text
agent/CURRENT_STATUS.md
```

Then STOP.
