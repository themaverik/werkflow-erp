# Werkflow-ERP Architecture Overview

## Core Principle: Complete Independence

werkflow-erp is a **completely standalone business data service**. It has **zero dependencies** on any workflow orchestration platform, including werkflow.

REST API Layer (Stateless, Idempotent)
  Endpoints: /api/v1/hr/*, /api/v1/finance/*, /api/v1/*

  Processing:
    - JWT validation (Spring Security)
    - Multi-tenant scoping (TenantContext)
    - Idempotency tracking (IdempotencyRecord)

Business Domain Services
  (HR, Finance, Procurement, Inventory)
  - Validation logic
  - FK constraints
  - Status state machines
  - No business rules (those are caller's concern)

Data Layer (PostgreSQL)
  4 schemas: hr_service, finance_service,
             procurement_service, inventory_service

Constraints:
  - NO EXTERNAL CALLS
  - NO WORKFLOW REFERENCES
  - NO KEYCLOAK CLIENT CODE
  - NO werkflow IMPORTS

---

## Three Deployment Scenarios (All Use Same Code)

### Scenario 1: Standalone ERP
```
Company A (No workflow platform)
    |
    v
Custom HR App / Finance Portal
    |
    v
werkflow-erp REST API
    |
    v
PostgreSQL
```

Company A gets a complete ERP system without needing werkflow.

### Scenario 2: Integrated with werkflow (Testing)
```
werkflow Platform
  - Engine (BPMN orchestration)
  - Admin (Users, departments)
  - Portal (Workflow designer)
  - Keycloak (Authentication)
    |
    | (via REST API)
    |
werkflow-erp (Business data provider)
    |
    v
PostgreSQL (shared)
```

werkflow uses werkflow-erp for testing and running business workflows.

### Scenario 3: Hybrid with Multiple Orchestrators
```
werkflow Platform     +     Zapier     +     Custom Scheduler
    |                      |                   |
    +------+-------+-------+-------+-------+---+
           |
           v
      werkflow-erp REST API
           |
           v
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
    |
    v
Calls: PATCH /api/v1/hr/employees/{id}/platform-link
    Body: { platformUserId: "uuid-123" }
    |
    v
werkflow-erp stores the link
    |
    v
Later queries can find: GET /api/v1/hr/employees/platform/uuid-123
```

**Why no coupling?**
- werkflow-erp never calls Keycloak or any platform system
- Works with Keycloak, Azure AD, LDAP, or custom systems
- Works without any platform system (platformUserId = null is valid)

---

### 2. **Zero External Dependencies**

werkflow-erp MUST NOT:
  - import com.werkflow.*;
  - import org.keycloak.admin.client.*;
  - Call Admin Service for user validation
  - Validate user IDs against external systems
  - Have hardcoded Keycloak configuration
  - Assume BPMN orchestration

werkflow-erp DOES:
  - Validate JWT signature (Spring Security, no client code)
  - Store user IDs as opaque strings
  - Provide status update endpoints
  - Work standalone or integrated with any caller

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

---

## Business Flow Diagrams

The following Mermaid diagrams illustrate the critical business processes that werkflow-erp supports. These are informational — they show how callers (werkflow, SAP, custom apps) orchestrate werkflow-erp API calls. werkflow-erp itself has no awareness of these flows.

### 1. Asset Lifecycle Flow

```mermaid
graph TD
    A["EMPLOYEE
Submits Asset Request"] -->|POST /inventory/asset-requests
procurementRequired=true| B["INVENTORY
AssetRequest Created
status: PENDING"]

    B -->|POST /asset-requests/{id}/
process-instance| C["ENGINE SERVICE
BPMN Process Started
Variables: assetRequestId,
departmentCode,
custodianGroupName"]

    C -->|User Task:
Manager Reviews| D{"Manager
Decision"}

    D -->|APPROVE| E["INVENTORY
AssetRequest.status
= APPROVED"]
    D -->|REJECT| F["INVENTORY
AssetRequest.status
= REJECTED
END FLOW"]

    E -->|Procurement Flag
procurementRequired=true
Callback| G["INVENTORY
AssetRequest.status
= PROCUREMENT_INITIATED"]

    G -->|POST /procurement/
purchase-requests
include budgetCategoryId| H["PROCUREMENT
Purchase Request Created
status: DRAFT"]

    H -->|POST /finance/budget-check
Request: dept, amount,
fiscalYear| I{"Budget
Available?"}

    I -->|NO| J["PR REJECTED
Notify Requester
END FLOW"]
    I -->|YES| K["PROCUREMENT
PR.status =
PENDING_APPROVAL"]

    K -->|Manager Approval
via BPMN| L["PROCUREMENT
PR.status = APPROVED"]

    L -->|POST /procurement/
purchase-orders
vendorId, prId| M["PROCUREMENT
Purchase Order Created
PO.status: CONFIRMED"]

    M -->|Vendor Delivers| N["LOGISTICS
Goods in Transit"]

    N -->|POST /procurement/
receipts
poId, acceptedQty| O["PROCUREMENT
Receipt Created (GRN)
status: COMPLETE"]

    O -->|Manual Step
Future: Webhook| P["INVENTORY
AssetInstance Created
status: AVAILABLE
assetTag assigned"]

    P -->|POST /custody-records
assetId, custodianDeptId,
custodianUserId| Q["INVENTORY
CustodyRecord Created
status: ACTIVE
AssetInstance.status: IN_USE"]

    Q -->|Asset needs
relocation| R["INVENTORY
TransferRequest Created
fromDeptId to toDeptId"]

    R -->|Manager Approves| S["INVENTORY
Transfer.status =
COMPLETED
Old Custody ended
New Custody created"]

    Q -->|Maintenance
Scheduled| T["INVENTORY
MaintenanceRecord
Created
status: SCHEDULED"]

    T -->|Service completed| U["INVENTORY
MaintenanceRecord
status: COMPLETED
nextMaintenanceDate set"]
```

---

### 2. Budget Approval Flow

```mermaid
graph TD
    A["FINANCE MANAGER
Creates Budget Plan"] -->|POST /finance/budgets
departmentId, fiscalYear,
totalAmount| B["FINANCE
BudgetPlan Created
status: DRAFT
allocatedAmount: 0"]

    B -->|POST /finance/budget-
line-items
categoryId, lineAmount| C["FINANCE
BudgetLineItems Created
linked to BudgetPlan"]

    C -->|Manager approves plan| D["FINANCE
BudgetPlan.status
= APPROVED
allocatedAmount updated"]

    D -->|Plan becomes ACTIVE| E["FINANCE
BudgetPlan.status
= ACTIVE"]

    E -->|Employee submits
PurchaseRequest| F["PROCUREMENT
PR created with
budgetCategoryId"]

    F -->|BEFORE PR APPROVAL:
POST /finance/budget-check
dept, amount, fiscalYear| G{"Check:
allocated - spent
= available?"}

    G -->|Insufficient| H["PR REJECTED
Notify Requester
Suggest defer to
next fiscal period"]
    G -->|Sufficient| I["PR can proceed
to approval flow"]

    I -->|Manager approves PR
via BPMN| J["PROCUREMENT
PR.status =
APPROVED to ORDERED"]

    J -->|Expense submitted| K["FINANCE
Expense Created
budgetLineItemId,
amount, status: SUBMITTED"]

    K -->|Approval threshold check
GET /approval-thresholds
match by amount| L{"Approval
Threshold
Match?"}

    L -->|No match| M["Amount outside
approval bands
Escalate to SUPER_ADMIN"]
    L -->|Match| N["BPMN Process
Routes to requiredRole
approvalOrder sequence"]

    N -->|Multi-level approval| O{"All Approvers
Signed?"}

    O -->|Rejected| P["Expense Rejected
status: REJECTED"]
    O -->|Approved| Q["Expense Approved
status: APPROVED"]

    Q -->|Manual Update
Future: Auto-increment| R["FINANCE
BudgetPlan.spentAmount
+= expense.amount
POST-MVP FIX"]

    R -->|Recompute| S["FINANCE
availableAmount
= allocated - spent"]

    S -->|Next PR checks
remaining budget| F
```

---

### 3. Procurement Flow (PR to PO to Receipt to Inventory)

```mermaid
graph TD
    A["PROCUREMENT
Team Creates
Purchase Request"] -->|POST /purchase-requests
requestingDeptId,
lineItems[budgetCategoryId]| B["PROCUREMENT
PR Created
prNumber: PR-{ts}
status: DRAFT"]

    B -->|BEFORE APPROVAL:
POST /finance/budget-check| C{"Budget
Gate"}

    C -->|FAILED| D["PR REJECTED
Can't proceed"]
    C -->|PASSED| E["PR APPROVED
status: PENDING_APPROVAL"]

    E -->|Manager Approval| F["PROCUREMENT
PR.status =
APPROVED"]

    F -->|POST /purchase-orders
vendorId,
purchaseRequestId| G["PROCUREMENT
PO Created
poNumber: PO-{ts}
status: DRAFT"]

    G -->|Line items added
from PR| H["PROCUREMENT
PoLineItems Created
prLineItemId ref"]

    H -->|Manager confirms| I["PROCUREMENT
PO.status =
CONFIRMED"]

    I -->|PO sent to vendor| J["VENDOR
Processes Order"]

    J -->|Goods shipped| K["LOGISTICS
In Transit"]

    K -->|POST /receipts
poId, lineItems
receiveQty, condition| L["PROCUREMENT
Receipt Created (GRN)
receiptNumber: GR-{ts}"]

    L -->|Goods inspected
Quality Check| M{"QC PROCESS
acceptedQty vs
rejectedQty"}

    M -->|Issues found| N["DISCREPANCY
discrepancyNotes
recorded"]
    M -->|All good| O["RECEIPT COMPLETE
Receipt.status =
COMPLETE"]

    O -->|Manual Step
Future: Webhook| P["INVENTORY
AssetInstance Created
assetTag assigned
status: AVAILABLE"]

    P -->|POST /inventory/stock
assetDefinitionId,
quantityTotal += accepted| Q["INVENTORY
Stock Updated
quantityAvailable
+= acceptedQty"]

    Q -->|Asset ready
for assignment| R["INVENTORY
CustodyRecord Created
Asset assigned to user/dept"]

    N -->|Return flow| S["RETURN TO VENDOR
or PR revision"]

    D -->|Fix budget
or defer| B
```

---

### 4. Engine Service Callback Flow

```mermaid
graph TD
    A["EXTERNAL EVENT
e.g., Asset Request
submitted by Employee"] -->|Business Service calls
Engine REST to start
BPMN process| B["ENGINE SERVICE
BPMN Process
Instance Created"]

    B -->|Retrieve start variables
GET /process-variables| C["BUSINESS SERVICE
AssetRequest supplies:
- assetRequestId
- requesterUserId
- departmentCode
- custodianGroupName
- procurementGroupName"]

    C -->|Engine injects variables
into BPMN process| D["ENGINE SERVICE
Variables available
in User Tasks and
Service Tasks"]

    D -->|User Task:
Assign to Manager| E["KEYCLOAK USER
Keycloak user logs in
sees approval task"]

    E -->|Manager
Submits Decision| F["ENGINE SERVICE
Task Complete
Decision variable set
Process continues"]

    F -->|Engine Service
Calls Callback| G["CALLBACK:
POST /callback/{action}?
processInstanceId&approverUserId&reason"]

    G -->|Router selects
callback handler| H{"Action
Type"}

    H -->|APPROVE| I["BUSINESS SERVICE
AssetRequest.status =
APPROVED
approvedByUserId set"]
    H -->|REJECT| J["BUSINESS SERVICE
AssetRequest.status =
REJECTED
rejectionReason set"]
    H -->|PROCUREMENT| K["BUSINESS SERVICE
AssetRequest.status =
PROCUREMENT_INITIATED
Auto-creates PR (future)"]

    I -->|Continue Workflow| L["ENGINE SERVICE
Next User Task
or Service Task"]
    J -->|Exit Workflow| M["ENGINE SERVICE
Process Terminated
Notify requester"]
    K -->|Procurement Task| N["ENGINE SERVICE
Route to procurement
manager task queue"]
```

---

### 5. Security and Auth Flow

```mermaid
graph TD
    A["KEYCLOAK USER
Logs in with
username/password"] -->|Keycloak
OIDC flow| B["KEYCLOAK
realm: werkflow
Verifies credentials
Issues JWT"]

    B -->|JWT contains:
realm_access.roles:
- admin, hr_manager,
  finance_manager,
  procurement_manager| C["CLIENT APP
Stores JWT
in local storage"]

    C -->|Every API request
Authorization header
Bearer {JWT}| D["BUSINESS SERVICE
SecurityFilterChain"]

    D -->|JwtDecoder
validates signature| E{"Signature
Valid?"}

    E -->|NO| F["401 Unauthorized
Token rejected"]
    E -->|YES| G["JWT Decoded
Claims extracted"]

    G -->|KeycloakRoleConverter
reads realm_access.roles| H["CONVERT TO
GrantedAuthority
ROLE_ADMIN
ROLE_HR_MANAGER
ROLE_FINANCE_MANAGER"]

    H -->|Routing by
@PreAuthorize| I{"Required
Role?"}

    I -->|Missing role| J["403 Forbidden
Insufficient permissions"]
    I -->|Has role| K["ALLOWED
Request proceeds
to handler"]

    K -->|Handler executes| L["BUSINESS SERVICE
API Endpoint
Database operation"]

    D -->|CORS check| M{"Origin
Allowed?"}

    M -->|localhost:4000
localhost:4001
localhost:3000| N["CORS OK
Response headers
set"]
    M -->|Other origin| O["CORS Blocked
No response headers"]
```

---

### Diagram Legend

| Color Meaning | State |
|---|---|
| Light Blue | User or client action |
| Light Yellow | Decision point or check |
| Light Green | Success or approved state |
| Light Red | Error or rejected state |
| Light Orange | Business service action |
| Light Purple | Engine service action |
| Gray | External or vendor action |

---

## Related Documents

- [ADR-001: Service Boundary Architecture](./adr/ADR-001-Service-Boundary-Architecture.md) — Detailed decisions
- [ADR-002: API Contract Standardization](./adr/ADR-002-API-Contract-Standardization.md) — API design decisions
- [ADR-002: User Identity and JWT Claims](./adr/ADR-002-User-Identity-And-JWT-Claims.md) — Identity architecture
- [Independence-Checklist.md](./Independence-Checklist.md) — PR review checklist
- [ROADMAP.md](../ROADMAP.md) — Implementation plan
- [README.md](../README.md) — Quick start guide

