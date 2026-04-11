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

---

## Business Flow Diagrams

The following Mermaid diagrams illustrate the critical business processes that werkflow-erp supports. These are informational — they show how callers (werkflow, SAP, custom apps) orchestrate werkflow-erp API calls. werkflow-erp itself has no awareness of these flows.

### 1. Asset Lifecycle Flow

```mermaid
graph TD
    A["EMPLOYEE<br/>Submits Asset Request"] -->|POST /inventory/asset-requests<br/>procurementRequired=true| B["INVENTORY<br/>AssetRequest Created<br/>status: PENDING"]

    B -->|POST /asset-requests/{id}/<br/>process-instance| C["ENGINE SERVICE<br/>BPMN Process Started<br/>Variables: assetRequestId,<br/>departmentCode,<br/>custodianGroupName"]

    C -->|User Task:<br/>Manager Reviews| D{"Manager<br/>Decision"}

    D -->|APPROVE| E["INVENTORY<br/>AssetRequest.status<br/>= APPROVED"]
    D -->|REJECT| F["INVENTORY<br/>AssetRequest.status<br/>= REJECTED<br/>END FLOW"]

    E -->|Procurement Flag<br/>procurementRequired=true<br/>Callback| G["INVENTORY<br/>AssetRequest.status<br/>= PROCUREMENT_INITIATED"]

    G -->|POST /procurement/<br/>purchase-requests<br/>include budgetCategoryId| H["PROCUREMENT<br/>Purchase Request Created<br/>status: DRAFT"]

    H -->|POST /finance/budget-check<br/>Request: dept, amount,<br/>fiscalYear| I{"Budget<br/>Available?"}

    I -->|NO| J["PR REJECTED<br/>Notify Requester<br/>END FLOW"]
    I -->|YES| K["PROCUREMENT<br/>PR.status =<br/>PENDING_APPROVAL"]

    K -->|Manager Approval<br/>via BPMN| L["PROCUREMENT<br/>PR.status = APPROVED"]

    L -->|POST /procurement/<br/>purchase-orders<br/>vendorId, prId| M["PROCUREMENT<br/>Purchase Order Created<br/>PO.status: CONFIRMED"]

    M -->|Vendor Delivers| N["LOGISTICS<br/>Goods in Transit"]

    N -->|POST /procurement/<br/>receipts<br/>poId, acceptedQty| O["PROCUREMENT<br/>Receipt Created (GRN)<br/>status: COMPLETE"]

    O -->|Manual Step<br/>Future: Webhook| P["INVENTORY<br/>AssetInstance Created<br/>status: AVAILABLE<br/>assetTag assigned"]

    P -->|POST /custody-records<br/>assetId, custodianDeptId,<br/>custodianUserId| Q["INVENTORY<br/>CustodyRecord Created<br/>status: ACTIVE<br/>AssetInstance.status: IN_USE"]

    Q -->|Asset needs<br/>relocation| R["INVENTORY<br/>TransferRequest Created<br/>fromDeptId to toDeptId"]

    R -->|Manager Approves| S["INVENTORY<br/>Transfer.status =<br/>COMPLETED<br/>Old Custody ended<br/>New Custody created"]

    Q -->|Maintenance<br/>Scheduled| T["INVENTORY<br/>MaintenanceRecord<br/>Created<br/>status: SCHEDULED"]

    T -->|Service completed| U["INVENTORY<br/>MaintenanceRecord<br/>status: COMPLETED<br/>nextMaintenanceDate set"]
```

---

### 2. Budget Approval Flow

```mermaid
graph TD
    A["FINANCE MANAGER<br/>Creates Budget Plan"] -->|POST /finance/budgets<br/>departmentId, fiscalYear,<br/>totalAmount| B["FINANCE<br/>BudgetPlan Created<br/>status: DRAFT<br/>allocatedAmount: 0"]

    B -->|POST /finance/budget-<br/>line-items<br/>categoryId, lineAmount| C["FINANCE<br/>BudgetLineItems Created<br/>linked to BudgetPlan"]

    C -->|Manager approves plan| D["FINANCE<br/>BudgetPlan.status<br/>= APPROVED<br/>allocatedAmount updated"]

    D -->|Plan becomes ACTIVE| E["FINANCE<br/>BudgetPlan.status<br/>= ACTIVE"]

    E -->|Employee submits<br/>PurchaseRequest| F["PROCUREMENT<br/>PR created with<br/>budgetCategoryId"]

    F -->|BEFORE PR APPROVAL:<br/>POST /finance/budget-check<br/>dept, amount, fiscalYear| G{"Check:<br/>allocated - spent<br/>= available?"}

    G -->|Insufficient| H["PR REJECTED<br/>Notify Requester<br/>Suggest defer to<br/>next fiscal period"]
    G -->|Sufficient| I["PR can proceed<br/>to approval flow"]

    I -->|Manager approves PR<br/>via BPMN| J["PROCUREMENT<br/>PR.status =<br/>APPROVED to ORDERED"]

    J -->|Expense submitted| K["FINANCE<br/>Expense Created<br/>budgetLineItemId,<br/>amount, status: SUBMITTED"]

    K -->|Approval threshold check<br/>GET /approval-thresholds<br/>match by amount| L{"Approval<br/>Threshold<br/>Match?"}

    L -->|No match| M["Amount outside<br/>approval bands<br/>Escalate to SUPER_ADMIN"]
    L -->|Match| N["BPMN Process<br/>Routes to requiredRole<br/>approvalOrder sequence"]

    N -->|Multi-level approval| O{"All Approvers<br/>Signed?"}

    O -->|Rejected| P["Expense Rejected<br/>status: REJECTED"]
    O -->|Approved| Q["Expense Approved<br/>status: APPROVED"]

    Q -->|Manual Update<br/>Future: Auto-increment| R["FINANCE<br/>BudgetPlan.spentAmount<br/>+= expense.amount<br/>POST-MVP FIX"]

    R -->|Recompute| S["FINANCE<br/>availableAmount<br/>= allocated - spent"]

    S -->|Next PR checks<br/>remaining budget| F
```

---

### 3. Procurement Flow (PR to PO to Receipt to Inventory)

```mermaid
graph TD
    A["PROCUREMENT<br/>Team Creates<br/>Purchase Request"] -->|POST /purchase-requests<br/>requestingDeptId,<br/>lineItems[budgetCategoryId]| B["PROCUREMENT<br/>PR Created<br/>prNumber: PR-{ts}<br/>status: DRAFT"]

    B -->|BEFORE APPROVAL:<br/>POST /finance/budget-check| C{"Budget<br/>Gate"}

    C -->|FAILED| D["PR REJECTED<br/>Can't proceed"]
    C -->|PASSED| E["PR APPROVED<br/>status: PENDING_APPROVAL"]

    E -->|Manager Approval| F["PROCUREMENT<br/>PR.status =<br/>APPROVED"]

    F -->|POST /purchase-orders<br/>vendorId,<br/>purchaseRequestId| G["PROCUREMENT<br/>PO Created<br/>poNumber: PO-{ts}<br/>status: DRAFT"]

    G -->|Line items added<br/>from PR| H["PROCUREMENT<br/>PoLineItems Created<br/>prLineItemId ref"]

    H -->|Manager confirms| I["PROCUREMENT<br/>PO.status =<br/>CONFIRMED"]

    I -->|PO sent to vendor| J["VENDOR<br/>Processes Order"]

    J -->|Goods shipped| K["LOGISTICS<br/>In Transit"]

    K -->|POST /receipts<br/>poId, lineItems<br/>receiveQty, condition| L["PROCUREMENT<br/>Receipt Created (GRN)<br/>receiptNumber: GR-{ts}"]

    L -->|Goods inspected<br/>Quality Check| M{"QC PROCESS<br/>acceptedQty vs<br/>rejectedQty"}

    M -->|Issues found| N["DISCREPANCY<br/>discrepancyNotes<br/>recorded"]
    M -->|All good| O["RECEIPT COMPLETE<br/>Receipt.status =<br/>COMPLETE"]

    O -->|Manual Step<br/>Future: Webhook| P["INVENTORY<br/>AssetInstance Created<br/>assetTag assigned<br/>status: AVAILABLE"]

    P -->|POST /inventory/stock<br/>assetDefinitionId,<br/>quantityTotal += accepted| Q["INVENTORY<br/>Stock Updated<br/>quantityAvailable<br/>+= acceptedQty"]

    Q -->|Asset ready<br/>for assignment| R["INVENTORY<br/>CustodyRecord Created<br/>Asset assigned to user/dept"]

    N -->|Return flow| S["RETURN TO VENDOR<br/>or PR revision"]

    D -->|Fix budget<br/>or defer| B
```

---

### 4. Engine Service Callback Flow

```mermaid
graph TD
    A["EXTERNAL EVENT<br/>e.g., Asset Request<br/>submitted by Employee"] -->|Business Service calls<br/>Engine REST to start<br/>BPMN process| B["ENGINE SERVICE<br/>BPMN Process<br/>Instance Created"]

    B -->|Retrieve start variables<br/>GET /process-variables| C["BUSINESS SERVICE<br/>AssetRequest supplies:<br/>- assetRequestId<br/>- requesterUserId<br/>- departmentCode<br/>- custodianGroupName<br/>- procurementGroupName"]

    C -->|Engine injects variables<br/>into BPMN process| D["ENGINE SERVICE<br/>Variables available<br/>in User Tasks and<br/>Service Tasks"]

    D -->|User Task:<br/>Assign to Manager| E["KEYCLOAK USER<br/>Keycloak user logs in<br/>sees approval task"]

    E -->|Manager<br/>Submits Decision| F["ENGINE SERVICE<br/>Task Complete<br/>Decision variable set<br/>Process continues"]

    F -->|Engine Service<br/>Calls Callback| G["CALLBACK:<br/>POST /callback/{action}?<br/>processInstanceId&approverUserId&reason"]

    G -->|Router selects<br/>callback handler| H{"Action<br/>Type"}

    H -->|APPROVE| I["BUSINESS SERVICE<br/>AssetRequest.status =<br/>APPROVED<br/>approvedByUserId set"]
    H -->|REJECT| J["BUSINESS SERVICE<br/>AssetRequest.status =<br/>REJECTED<br/>rejectionReason set"]
    H -->|PROCUREMENT| K["BUSINESS SERVICE<br/>AssetRequest.status =<br/>PROCUREMENT_INITIATED<br/>Auto-creates PR (future)"]

    I -->|Continue Workflow| L["ENGINE SERVICE<br/>Next User Task<br/>or Service Task"]
    J -->|Exit Workflow| M["ENGINE SERVICE<br/>Process Terminated<br/>Notify requester"]
    K -->|Procurement Task| N["ENGINE SERVICE<br/>Route to procurement<br/>manager task queue"]
```

---

### 5. Security and Auth Flow

```mermaid
graph TD
    A["KEYCLOAK USER<br/>Logs in with<br/>username/password"] -->|Keycloak<br/>OIDC flow| B["KEYCLOAK<br/>realm: werkflow<br/>Verifies credentials<br/>Issues JWT"]

    B -->|JWT contains:<br/>realm_access.roles:<br/>- admin, hr_manager,<br/>  finance_manager,<br/>  procurement_manager| C["CLIENT APP<br/>Stores JWT<br/>in local storage"]

    C -->|Every API request<br/>Authorization header<br/>Bearer {JWT}| D["BUSINESS SERVICE<br/>SecurityFilterChain"]

    D -->|JwtDecoder<br/>validates signature| E{"Signature<br/>Valid?"}

    E -->|NO| F["401 Unauthorized<br/>Token rejected"]
    E -->|YES| G["JWT Decoded<br/>Claims extracted"]

    G -->|KeycloakRoleConverter<br/>reads realm_access.roles| H["CONVERT TO<br/>GrantedAuthority<br/>ROLE_ADMIN<br/>ROLE_HR_MANAGER<br/>ROLE_FINANCE_MANAGER"]

    H -->|Routing by<br/>@PreAuthorize| I{"Required<br/>Role?"}

    I -->|Missing role| J["403 Forbidden<br/>Insufficient permissions"]
    I -->|Has role| K["ALLOWED<br/>Request proceeds<br/>to handler"]

    K -->|Handler executes| L["BUSINESS SERVICE<br/>API Endpoint<br/>Database operation"]

    D -->|CORS check| M{"Origin<br/>Allowed?"}

    M -->|localhost:4000<br/>localhost:4001<br/>localhost:3000| N["CORS OK<br/>Response headers<br/>set"]
    M -->|Other origin| O["CORS Blocked<br/>No response headers"]
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

