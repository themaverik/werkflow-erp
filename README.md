<div align="center">
  <img src="public/logo.png" alt="WERP Logo" width="300" />
</div>

# Werkflow-ERP

A standalone CRUD data service for HR, Finance, Procurement, and Inventory domains. Designed for independent deployment or integration with the [Werkflow](https://github.com/themaverik/werkflow) workflow orchestration platform.

| Property | Value |
|---|---|
| Type | Spring Boot microservice (Java 21) |
| Port | 8084 |
| Context Path | `/api/v1` |
| Database | PostgreSQL 5433 |
| Authentication | OIDC JWT (Keycloak, Auth0, Azure AD, AWS Cognito) |
| Multi-Tenancy | Yes |

---

## What This Service Does

Provides CRUD APIs for four business domains:

- **HR**: Employees, departments, leave, attendance, payroll, performance reviews
- **Finance**: Budget plans, expenses, approval thresholds
- **Procurement**: Vendors, purchase requests, orders, receipts
- **Inventory**: Assets, categories, custody, transfers, maintenance

Validates data (FK constraints, enum values, required fields) and enforces idempotency for safe retries. Does not implement business approval logic, notifications, or workflow routing — those belong to the caller.

---

## Prerequisites

- Docker and Docker Compose
- Java 21+
- Maven 3.8+

Shared services (must be running):

```bash
cd ../werkflow/infrastructure/docker
docker compose up -d postgres keycloak mailpit
```

---

## Quick Start

```bash
# Build
mvn clean install -DskipTests

# Run (Docker Compose)
docker compose up -d

# Verify
curl -s http://localhost:8084/api/v1/actuator/health | jq .

# Swagger UI
open http://localhost:8084/api/v1/swagger-ui.html
```

---

## Configuration

Environment variables in `config/env/`:

| File | Purpose |
|---|---|
| `.env.shared` | Database, Keycloak URLs |
| `.env.business` | Service port, log level |

Key variables: `POSTGRES_HOST`, `POSTGRES_PORT`, `KEYCLOAK_URL`, `KEYCLOAK_REALM`, `SERVER_PORT`.

---

## Documentation

| Document | Description |
|---|---|
| [docs/Architecture-Overview.md](./docs/Architecture-Overview.md) | Architecture, design principles, and business flow diagrams |
| [docs/API-Usage-Guide.md](./docs/API-Usage-Guide.md) | Step-by-step API examples for all four domains |
| [docs/Werkflow-Integration-Guide.md](./docs/Werkflow-Integration-Guide.md) | Connector setup, BPMN workflow examples, ProcessInstanceId linking |
| [docs/Independence-Checklist.md](./docs/Independence-Checklist.md) | PR review checklist and anti-pattern guide |
| [docs/adr/ADR-001-Service-Boundary-Architecture.md](./docs/adr/ADR-001-Service-Boundary-Architecture.md) | Service boundary and independence decisions |
| [docs/adr/ADR-002-User-Identity-And-JWT-Claims.md](./docs/adr/ADR-002-User-Identity-And-JWT-Claims.md) | User identity and JWT claims design |
| [Roadmap.md](./Roadmap.md) | Implementation roadmap and task tracking |

---

## License

Proprietary — All rights reserved
