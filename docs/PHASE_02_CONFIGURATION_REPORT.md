# EVShare 3D — PHASE 02-C: APPLICATION CONFIGURATION REPORT

## 1. Executive Summary

| Attribute | Value |
|---|---|
| **Phase** | PHASE 02-C (Application Configuration & Profile Architecture) |
| **Audit Precondition** | `docs/PHASE_02_DATABASE_AUDIT.md` (STATUS: **READY**) |
| **Root Package** | `com.example.evshare` |
| **Active Profiles** | `dev` (default), `prod`, `test` |
| **Runtime & Framework** | Java 17+ (JDK 21 LTS / JDK 25 verified), Spring Boot 3.2.5 |
| **Compilation Result** | `BUILD SUCCESS` (14 source files, 4 configuration profiles processed) |
| **MySQL Runtime Status** | **`MYSQL_RUNTIME = NOT_AVAILABLE`** (Authenticated connection unverified without credentials) |
| **Phase Status** | **COMPLETED & VERIFIED** |
| **Next Phase** | PHASE 02-D (JPA Entities & Flyway Migrations — On Explicit Instruction) |

---

## 2. Configuration Architecture

The configuration subsystem has been architected into a modular multi-profile structure under `backend/src/main/resources/`:

```
backend/src/main/resources/
├── application.yml          # Master baseline configuration & environment parameter mapping
├── application-dev.yml      # Local development profile (verbose SQL, expanded actuator details)
├── application-prod.yml     # Production hardened profile (minimized logging, strict pool, no stack traces)
├── application-test.yml     # Integration test profile (isolated JPA validation & Flyway)
└── db/
    └── migration/           # Flyway SQL migration repository (Phase 02-D)
```

---

## 3. Implemented Configuration Features

### 3.1. Server Port & Lifecycle
* Configured port: `${PORT:8080}`
* Context path: `/`
* Shutdown policy: `graceful` (ensures inflight HTTP requests finish before termination)

### 3.2. Application Profiles
* Active profile selector: `${SPRING_PROFILES_ACTIVE:dev}`
* Supports runtime profile switching via `-Dspring.profiles.active=prod` or container environment variable `SPRING_PROFILES_ACTIVE`.

### 3.3. MySQL Datasource & Secure Credentials
Production credentials are **strictly NEVER hard-coded**. All database parameters are injected via standardized environment variables with resilient fallback keys:

| Environment Variable | Property Key | Default / Fallback | Purpose |
|---|---|---|---|
| `DB_HOST` | `spring.datasource.url` | `localhost` | MySQL host address |
| `DB_PORT` | `spring.datasource.url` | `3306` | MySQL port |
| `DB_NAME` | `spring.datasource.url` | `evshare_db` | Database schema name |
| `DB_USERNAME` | `spring.datasource.username` | `root` | Database user account |
| `DB_PASSWORD` | `spring.datasource.password` | `""` (Empty) | Database password (no plaintext secret) |
| `DB_POOL_MAX_SIZE` | `hikari.maximum-pool-size` | `10` (dev) / `25` (prod) | Connection pool ceiling |
| `DB_POOL_MIN_IDLE` | `hikari.minimum-idle` | `5` (dev) / `10` (prod) | Minimum warm connections |

* JDBC URL: `jdbc:mysql://${DB_HOST:localhost}:${DB_PORT:3306}/${DB_NAME:evshare_db}?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC&characterEncoding=UTF-8`
* Driver: `com.mysql.cj.jdbc.Driver`
* Connection pool: HikariCP (`EvShareHikariPool`) with 20s connection timeout and 30s idle timeout.

### 3.4. JPA / Hibernate Schema Validation
* **`ddl-auto: validate`**: Enforced across all profiles. Hibernate will strictly validate entities against the schema created by Flyway and **will never alter or drop production tables**.
* `open-in-view: false`: Avoids holding database connections open during view rendering.
* Dialect: `org.hibernate.dialect.MySQLDialect`.

### 3.5. Flyway Migration Engine
* Enabled: `true`
* Locations: `classpath:db/migration`
* Baseline on migrate: `true` (baseline version `0`)
* Validate on migrate: `true`
* Out of order: `false` (deterministic forward-only migrations)

### 3.6. Jakarta Bean Validation
* Validation properties configured (`validation.fail-fast: false`).
* Supported by `spring-boot-starter-validation` and centralized in `GlobalExceptionHandler`.

### 3.7. Logging Subsystem
* Console Pattern: `%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n`
* `dev` profile: `DEBUG` for `com.example.evshare`, `org.hibernate.SQL`, and `org.flywaydb`.
* `prod` profile: `INFO` for application, `WARN` for root/SQL to prevent log pollution and credential leakage.

### 3.8. Spring Boot Actuator
* Exposed web endpoints: `health`, `info`, `metrics`.
* Health endpoint details: `always` in dev, `when_authorized` in base, `never` in prod.
* Health probes (`livenessState`, `readinessState`): `enabled: true`.

### 3.9. OpenAPI 3 / Swagger
* Spec path: `/v3/api-docs`
* UI path: `/swagger-ui.html`
* Configuration: Method sorting enabled, tag alpha sorting enabled, actuator endpoints integrated.

---

## 4. Build & Verification Results

### Build Command
```powershell
mvn clean compile
```

### Compiler Log
```
[INFO] --- clean:3.3.2:clean (default-clean) @ evshare-backend ---
[INFO] Deleting E:\EVShare3D\backend\target
[INFO] 
[INFO] --- resources:3.3.1:resources (default-resources) @ evshare-backend ---
[INFO] Copying 4 resources from src\main\resources to target\classes
[INFO] Copying 0 resource from src\main\resources to target\classes
[INFO] 
[INFO] --- compiler:3.11.0:compile (default-compile) @ evshare-backend ---
[INFO] Changes detected - recompiling the module! :source
[INFO] Compiling 14 source files with javac [debug release 17] to target\classes
[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
[INFO] ------------------------------------------------------------------------
[INFO] Total time:  2.374 s
[INFO] Finished at: 2026-09-06T19:13:28+07:00
[INFO] ------------------------------------------------------------------------
```

---

## 5. MySQL Runtime Status Audit

* **Port 3306 Check**: Socket active on `127.0.0.1:3306`.
* **Authentication Check**: Local root account requires password credentials which are not configured in system environment variables (`ERROR 1045 (28000): Access denied`).
* **Reported Verdict**:
  ```
  MYSQL_RUNTIME = NOT_AVAILABLE
  ```
* Per Rule 15 of `AGENTS.md`, database connection is **NOT VERIFIED** at runtime until valid credentials are provided or database instance is initialized. Successful connection is never claimed without explicit end-to-end execution.

---

## 6. Scope Boundary Confirmation

* **No JWT Implementation**: Security package remains decoupled.
* **No Business Services**: Services remain package markers only.
* **No Frontend Code**: Pure 3D frontend boundaries fully maintained.

---

## 7. Next Step

**Phase 02-C is complete.** Ready for **PHASE 02-D** (JPA Entity Models, Flyway Migration Scripts V1–V7, and Database Schema Setup). Execution is paused pending explicit user approval.
