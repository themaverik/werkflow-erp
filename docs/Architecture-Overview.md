# werkflow-erp Architecture Overview

## Core Principle: Complete Independence

werkflow-erp is a **completely standalone business data service**. It has **zero dependencies** on any workflow orchestration platform, including werkflow.

```
┌─────────────────────────────────────────────────────────────────┐
│                                                                 │
│                   werkflow-erp (This Service)                  │
│                                                                 │
│  ┌────────────────────────────────────────────────────────┐   │
│  │  REST API Layer (Stateless, Idempotent)              │   │
│  │  /api/v1/hr/* /api/v1/finance/* /api/v1/...        │   │
│  └────────────────────────────────────────────────────────┘   │
│           ↑                                                     │
│           │ JWT validation (Spring Security)                   │
│           │ Multi-tenant scoping (TenantContext)              │
│           │ Idempotency tracking (IdempotencyRecord)          │
│           │                                                     │
│  ┌────────────────────────────────────────────────────────┐   │
│  │  Business Domain Services                             │   │
│  │  (HR, Finance, Procurement, Inventory)                │   │
│  │  • Validation logic                                   │   │
│  │  • FK constraints                                     │   │
│  │  • Status state machines                              │   │
│  │  • No business rules (those are caller's concern)     │   │
│  └────────────────────────────────────────────────────────┘   │
│           ↓                                                     │
│  ┌────────────────────────────────────────────────────────┐   │
│  │  Data Layer (PostgreSQL)                             │   │
│  │  4 schemas: hr_service, finance_service,             │   │
│  │             procurement_service, inventory_service   │   │
│  └────────────────────────────────────────────────────────┘   │
│                                                                 │
│  NO EXTERNAL CALLS                                             │
│  NO WORKFLOW REFERENCES                                        │
│  NO KEYCLOAK CLIENT CODE                                       │
│  NO werkflow IMPORTS                                           │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

---

## Three Deployment Scenarios (All Use Same Code)

### Scenario 1: Standalone ERP
```
Company A (No workflow platform)
    ↓
Custom HR App / Finance Portal
    ↓
werkflow-erp REST API
    ↓
PostgreSQL
```

Company A gets a complete ERP system without needing werkflow.

### Scenario 2: Integrated with werkflow (Testing)
```
werkflow Platform
├─ Engine (BPMN orchestration)
├─ Admin (Users, departments)
├─ Portal (Workflow designer)
└─ Keycloak (Authentication)
    ↓ (via REST API)
    ↓
werkflow-erp (Business data provider)
    ↓
PostgreSQL (shared)
```

werkflow uses werkflow-erp for testing and running business workflows.

### Scenario 3: Hybrid with Multiple Orchestrators
```
werkflow Platform     +     Zapier     +     Custom Scheduler
    ↓                      ↓                   ↓
    └──────────────────────┴───────────────────┘
                          ↓
                werkflow-erp REST API
                          ↓
                       PostgreSQL
```

Multiple systems share the same business data.

---

## Key Architectural Decisions

### 1. **Platform User ↔ HR Employee Linking (WITHOUT Coupling)**

**The Problem:**
- A person may be both a platform user (logs in) AND an HR employee (in payroll)
- But they're tracked in separate systems

**The Solution:**
- `Employee.platformUserId` is an optional field
- External systems (werkflow, SAP, etc.) manage the linking
- werkflow-erp just stores the reference, doesn't validate it

```
External Platform creates user
    ↓
Calls: PATCH /api/v1/hr/employees/{id}/platform-link
    Body: { platformUserId: "uuid-123" }
    ↓
werkflow-erp stores the link
    ↓
Later queries can find: GET /api/v1/hr/employees/platform/uuid-123
```

**Why no coupling?**
- werkflow-erp never calls Keycloak or any platform system
- Works with Keycloak, Azure AD, LDAP, or custom systems
- Works without any platform system (platformUserId = null is valid)

---

### 2. **Zero External Dependencies**

werkflow-erp MUST NOT:
```
❌ import com.werkflow.*;
❌ import org.keycloak.admin.client.*;
❌ Call Admin Service for user validation
❌ Validate user IDs against external systems
❌ Have hardcoded Keycloak configuration
❌ Assume BPMN orchestration
```

werkflow-erp DOES:
```
✅ Validate JWT signature (Spring Security, no client code)
✅ Store user IDs as opaque strings
✅ Provide status update endpoints
✅ Work standalone or integrated with any caller
```

**CI/CD Check:**
```bash
if grep -r "import com.werkflow" services/business/ ; then
  echo "FAIL: werkflow-erp has forbidden imports"
  exit 1
fi
```

---

### 3. **Status Updates (Not Workflows)**

werkflow-erp doesn't have "workflow callbacks". It has **simple status update endpoints**:

```
PATCH /api/v1/inventory/asset-requests/{id}/status
Body: { status: "APPROVED" }
```

This endpoint:
- ✅ Works when called by werkflow Engine
- ✅ Works when called by Zapier
- ✅ Works when called by cron job
- ✅ Works when called by manual REST client
- ✅ Doesn't care WHO updated the status

---

### 4. **User ID is Opaque and External**

Entities store user IDs (e.g., `custodianUserId: string`), but:

- werkflow-erp **never validates** them against external systems
- werkflow-erp **never enriches** them with user names (caller's responsibility)
- werkflow-erp **trusts the caller** (JWT signature is validation)

```
Scenario 1: werkflow (Keycloak user ID)
    custodianUserId = "keycloak-uuid-123"

Scenario 2: SAP integration (Employee ID)
    custodianUserId = "SAP-EMP-42"

Scenario 3: Manual testing (Email)
    custodianUserId = "alice@example.com"

All three work identically. werkflow-erp doesn't care.
```

---

## API Design Principles

### Stateless & Idempotent
```
POST /api/v1/procurement/purchase-requests
Headers:
  X-Idempotency-Key: "uuid-123"
  X-Tenant-ID: "acme-corp"

Response 201 (first call)
Response 200 (retry with same key)
```

### Multi-Tenant Aware
```
Every entity has: tenantId (NOT NULL)
Every query filters: WHERE tenantId = ?
No cross-tenant data leaks
```

### Pagination
```
GET /api/v1/hr/employees?page=0&size=20&sort=createdAt,desc
Response: { content: [...], pageable: { pageNumber: 0, totalElements: 1250 } }
```

### Versioned
```
✅ /api/v1/...
❌ /api/... (no version)

Allows v2 in future without breaking v1 clients
```

---

## Data Flow Examples

### Example 1: werkflow Testing an Asset Request Workflow

```
1. werkflow Portal
   User: "I need a laptop"
   ↓
2. werkflow Engine
   Calls: POST /api/v1/inventory/asset-requests
   Body: {
     requesterUserId: "keycloak-uuid",
     assetDefinitionId: 123,
     externalProcessId: "bpmn-proc-456"
   }
   ↓
3. werkflow-erp
   ✅ Validates assetDefinitionId exists
   ✅ Stores externalProcessId for tracking
   ✅ Returns: { id: 42, status: "PENDING" }
   ↓
4. werkflow Engine
   ✅ Executes approval task (no werkflow-erp involved)
   ✅ On approval: PATCH /asset-requests/42/status
   Body: { status: "APPROVED" }
   ✓ werkflow-erp updates and returns confirmation
   ✅ Routes to procurement (no werkflow-erp involved)
   ✅ Creates PO (no werkflow-erp involved)
```

werkflow-erp is **completely ignorant** of the approval logic, routing, or notifications.

---

### Example 2: Standalone HR System Using werkflow-erp

```
1. HR Portal (custom built, no workflow)
   HR Manager: Approve leave request
   ↓
2. Direct REST call (no workflow involved)
   PATCH /api/v1/hr/leaves/99/status
   Body: { status: "APPROVED" }
   ↓
3. werkflow-erp
   ✅ Updates leave record
   ✅ Returns confirmation
   ↓
4. HR Portal
   ✅ Notifies employee (portal's responsibility)
   ✅ Updates calendar (portal's responsibility)
```

werkflow-erp has **no idea** that this is being used standalone.

---

## Testing werkflow with werkflow-erp

werkflow can test its workflows using werkflow-erp as the **data provider**:

```
Integration Test: Asset Request Workflow
├─ Start Docker: werkflow-erp + PostgreSQL
├─ Create test data: POST /api/v1/inventory/asset-definitions
├─ Run workflow: Deploy BPMN, start process instance
├─ Verify calls: werkflow calls werkflow-erp REST APIs
├─ Check database: Confirm data stored correctly
└─ Teardown: Stop containers, reset database
```

werkflow tests its **orchestration logic** against werkflow-erp's **data APIs**.

---

## What werkflow-erp Does NOT Know

- Whether it's being used by werkflow, SAP, or a custom app
- What business process is using its data
- Whether approvals happened (that's the caller's problem)
- Whether notifications were sent (that's the caller's problem)
- Any workflow concepts (BPMN, tasks, gateways)
- Any platform concepts (users, roles, authentication)

werkflow-erp just stores and retrieves business domain data. That's it.

---

## Related Documents

- [ADR-001: Service Boundary Architecture](./ADR-001-Service-Boundary-Architecture.md) — Detailed decisions
- [ROADMAP.md](../ROADMAP.md) — Implementation plan
- [README.md](../README.md) — Quick start guide

