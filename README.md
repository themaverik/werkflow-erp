# Werkflow ERP — Business Service

Standalone deployment of the Werkflow Business Service for external integration testing.

Provides HR, Finance, Procurement, and Inventory capabilities as an optional microservice that integrates with the main Werkflow platform via the Connector Registry.

## Quick Start

### Prerequisites

- Docker and Docker Compose
- Werkflow platform running on `werkflow-network` (localhost:8081 engine, 8083 admin, 8090 keycloak)

### Run Business Service

```bash
# Build and start
docker compose build --no-cache
docker compose up -d

# Check status
docker compose ps
docker compose logs -f business-service
```

Service will be available at: `http://localhost:8084/api`

### Stop

```bash
docker compose down
```

## Integration with Main Platform

The business service shares:
- PostgreSQL database with the main platform (werkflow schema)
- Keycloak authentication (werkflow realm)
- Engine and Admin service connectivity

To register the business service as a connector in the main Werkflow platform:

1. Navigate to Admin Portal → Connectors
2. Add new connector with:
   - **Key**: `business-service`
   - **Display Name**: Werkflow ERP Business Service
   - **Endpoint URL**: `http://localhost:8084/api`

## Service Endpoints

- Health: `GET http://localhost:8084/api/actuator/health`
- HR Module: `GET|POST http://localhost:8084/api/hr/*`
- Finance Module: `GET|POST http://localhost:8084/api/finance/*`
- Procurement Module: `GET|POST http://localhost:8084/api/procurement/*`
- Inventory Module: `GET|POST http://localhost:8084/api/inventory/*`

## Configuration

Environment variables in `config/env/`:
- `.env.shared` — Common settings
- `.env.business` — Business service specifics (DB schema, ports, etc.)

## Architecture

```
Werkflow Platform                   Werkflow ERP (This Repo)
├─ Engine (8081)           ←→      ├─ Business Service (8084)
├─ Admin (8083)            ←→      └─ Shared DB (werkflow.business_service schema)
├─ Portal (4000)
└─ Keycloak (8090) ←shared auth→
```

The business service is optional — clients can bring their own HR/Finance/Procurement systems and register them as connectors instead.
