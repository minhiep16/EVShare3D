# EVShare 3D — Production Docker Deployment Guide

## 1. Overview

EVShare 3D is architected for containerized production deployment using Docker and Docker Compose. The topology isolates services within an internal bridge network (`evshare-network`), mounts persistent storage for transactional durability, and exposes security-hardened HTTP/HTTPS ports.

```
                  +----------------------------------------------+
                  |               Internet / Client              |
                  +----------------------------------------------+
                                         |
                                         | HTTP (:3001) / HTTPS (:443)
                                         v
                  +----------------------------------------------+
                  |      evshare-frontend (Nginx 1.31 Alpine)    |
                  |  - Pure 3D Three.js SPA Assets (dist)        |
                  |  - Gzip Compression & Security Headers       |
                  |  - Reverse Proxy /api/ & /actuator/          |
                  +----------------------------------------------+
                                         |
                                         | Proxy Pass (http://backend:8080)
                                         v
                  +----------------------------------------------+
                  |    evshare-backend (Eclipse Temurin JRE 17)  |
                  |  - Spring Boot 3.3 Production Executable     |
                  |  - Non-root user 'evshare' (UID 10001)       |
                  |  - G1GC Memory Tuning (256m - 1024m)         |
                  +----------------------------------------------+
                                         |
                                         | JDBC (:3306)
                                         v
                  +----------------------------------------------+
                  |         evshare-mysql (MySQL 8.0)            |
                  |  - Character Set: utf8mb4_unicode_ci         |
                  |  - Volume: evshare_mysql_data (Persistent)   |
                  +----------------------------------------------+
```

---

## 2. Container Specifications & Images

| Service | Base Image | Size | Security & Optimization | Health Check |
| :--- | :--- | :--- | :--- | :--- |
| **`frontend`** | `node:20-alpine` $\to$ `nginx:alpine` (Multi-stage) | ~96 MB | Unprivileged Nginx, custom `nginx.conf`, SPA fallback, IPv4/IPv6 dual-stack, security headers | `wget -qO- http://127.0.0.1:80/health \|\| exit 1` |
| **`backend`** | `eclipse-temurin:17-jre-jammy` | ~493 MB | Unprivileged user `evshare` (UID 10001), G1GC flags, `/actuator/health` probe, secrets via env vars | `wget -qO- http://localhost:8080/actuator/health \|\| exit 1` |
| **`mysql`** | `mysql:8.0` | ~580 MB | `utf8mb4` collation, persistent named volume, non-root initialization | `mysqladmin ping -h localhost -u root -p$$MYSQL_ROOT_PASSWORD` |

---

## 3. Environment Configuration

All configurable properties are driven by environment variables (`.env`). No passwords, tokens, or sensitive API keys are baked into images.

Copy `.env.example` to `.env` before production launch:
```bash
cp .env.example .env
```

Key environment variables:
```properties
# MySQL
MYSQL_ROOT_PASSWORD=evshare_root_secure_password_2026
MYSQL_DATABASE=evshare_db
MYSQL_USER=evshare_app
MYSQL_PASSWORD=evshare_secure_db_pass_2026

# Spring Boot Backend
JWT_SECRET=your_secure_random_base64_or_hex_jwt_secret_min_32_bytes
JWT_EXPIRATION_MS=900000
JWT_REFRESH_EXPIRATION_MS=604800000
SPRING_PROFILES_ACTIVE=prod
JAVA_OPTS=-Xms256m -Xmx1024m -XX:+UseG1GC

# AI Integration
GEMINI_API_KEY=
```

---

## 4. Compose Service Topology (`docker-compose.yml`)

### Network
- **Name**: `evshare-network`
- **Driver**: `bridge`
- **Isolation**: Containers communicate by service DNS name (`mysql`, `backend`, `frontend`).

### Storage
- **Volume**: `evshare_mysql_data`
- **Type**: Named local persistent Docker volume mounted to `/var/lib/mysql`.
- **Durability**: Survives container restarts, upgrades, and teardowns (`docker compose down`).

### Service Dependencies & Ordering
- `backend` specifies:
  ```yaml
  depends_on:
    mysql:
      condition: service_healthy
  ```
  Ensures Spring Boot and Flyway do not start until MySQL has initialized and passed ping checks.
- `frontend` specifies:
  ```yaml
  depends_on:
    backend:
      condition: service_healthy
  ```
  Ensures Nginx reverse proxy starts only after Spring Boot `/actuator/health` returns `UP`.

---

## 5. Deployment Commands

### Build Images
```bash
docker compose build
```

### Start Stack (Detached)
```bash
docker compose up -d
```

### Check Stack Status
```bash
docker compose ps
```
Expected output:
```
NAME               IMAGE                     STATUS                    PORTS
evshare-backend    evshare-backend:latest    Up (healthy)              0.0.0.0:8081->8080/tcp
evshare-frontend   evshare-frontend:latest   Up (healthy)              0.0.0.0:3001->80/tcp
evshare-mysql      mysql:8.0                 Up (healthy)              0.0.0.0:3307->3306/tcp
```

### Smoke Test Endpoints
```bash
# Frontend health
curl -s -i http://localhost:3001/health

# Backend Actuator health (direct)
curl -s http://localhost:8081/actuator/health

# Backend Actuator health (via Nginx reverse proxy)
curl -s http://localhost:3001/actuator/health

# Protected API endpoint (returns 401 Unauthorized as expected)
curl -s http://localhost:3001/api/vehicles
```

### Teardown Stack
```bash
# Stop containers (preserves database volume)
docker compose down

# Stop containers and remove volume (destructive)
docker compose down -v
```
