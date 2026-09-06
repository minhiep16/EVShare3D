# PHASE 03 – AUTHENTICATION & AUTHORIZATION

Implement only PHASE 03.

Read:

```text
docs/REQUIREMENTS.md
docs/BUSINESS_RULES.md
docs/RBAC.md
docs/API.md
```

## OBJECTIVE

Implement secure authentication and authorization.

Use:

Spring Security

JWT

BCrypt or Argon2

---

# FEATURES

Implement:

Registration

Login

Logout

Refresh token

Password reset foundation

Current user

Role-based access control

---

# ROLES

```text
CO_OWNER
STAFF
ADMIN
```

---

# SECURITY RULES

Backend authorization is mandatory.

Frontend permissions are not trusted.

Never expose:

* password
* password hash
* JWT secret
* refresh token unnecessarily

---

# API

Implement:

```text
POST /api/v1/auth/register
POST /api/v1/auth/login
POST /api/v1/auth/refresh
POST /api/v1/auth/logout
GET  /api/v1/users/me
```

Use proper DTOs.

---

# TESTS

Test:

* valid registration
* duplicate email
* invalid password
* login success
* login failure
* token validation
* refresh
* unauthorized API
* role authorization

Run:

```text
mvn clean test
```

Do not continue if tests fail.

Update:

```text
docs/API.md
docs/RBAC.md
agent/CURRENT_STATUS.md
```

Then STOP.
