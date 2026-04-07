# ADR-001: Service Boundary Architecture — werkflow-erp as Pure CRUD Service

**Status**: Accepted
**Date**: 2026-04-06
**Authors**: Tech Lead, Architecture Review

---

## Context

The werkflow-erp project is a **completely independent business ERP system** that:

1. **Can exist standalone**: Full ERP functionality without any dependency on werkflow
2. **Can be used as a testing data source**: When testing the main werkflow platform, use werkflow-erp as the business data provider
3. **Has no coupling to werkflow**: No imports from werkflow, no hardcoded assumptions about orchestration
4. **Exposes clean REST APIs**: Any system (werkflow or otherwise) can call these APIs

The key architectural principle: **werkflow-erp is ignorant of how its data is used.**

### Two Deployment Models

**Model 1: Standalone ERP**
```
Standalone Client Application
    ↓
werkflow-erp REST API (8084)
    ↓
PostgreSQL (5433)
```

**Model 2: Integrated with werkflow (Testing)**
```
werkflow Platform (8081, 8083, 4000, 8090)
    ↓ (calls REST APIs)
    ↓
werkflow-erp (8084)
    ↓
Shared PostgreSQL (5433)
```

**werkflow-erp is the same in both models** — it doesn't know or care which is running.

---

## Decision: werkflow-erp is an Independent ERP System

### What werkflow-erp Provides

**Complete business domain operations:**
- **HR**: Manage employees, departments, leave, attendance, payroll, performance reviews
- **Finance**: Manage budgets, expenses, approval thresholds, budget checks
- **Procurement**: Manage vendors, purchase requests, orders, receipts
- **Inventory**: Manage assets, categories, definitions, custody, transfers, maintenance

**Standard operations on domain data:**
- CREATE: Add new records (employees, asset requests, purchase orders, etc.)
- READ: Query records with pagination and filtering
- UPDATE: Modify existing records (approve leave, mark receipt complete, etc.)
- DELETE: Remove records (where business rules allow)
- VALIDATE: Foreign key constraints, enum validation, required fields, idempotency
- STATUS UPDATES: Transition records through their lifecycle (e.g., AssetRequest: PENDING → APPROVED)

### What werkflow-erp Does NOT Provide

werkflow-erp is **pure business data management**. It does NOT provide:

- **Orchestration**: No BPMN, no workflow logic, no conditional branching
- **Approval workflows**: No "route to manager for approval" logic
- **Business rules enforcement**: No "check budget before PO" or "validate stock before shipment"
- **Notifications**: No email, SMS, or task notifications
- **Task assignment**: No task routing or delegation
- **Platform user management**: No Keycloak integration or user provisioning

These are **orchestration concerns** — handled by whatever system is calling werkflow-erp (could be werkflow, could be a custom scheduler, could be manual).

### How werkflow-erp is Used

**By werkflow (for testing workflows):**
```
werkflow Portal
    ↓ User submits form
    ↓
werkflow Engine executes BPMN
    ↓ Calls REST API
    ↓
werkflow-erp stores data
    ↓ Returns response
    ↓
werkflow Engine evaluates response and continues BPMN
    (makes business decisions, routes tasks, sends notifications)
```

**By standalone client (direct ERP usage):**
```
Custom Application (e.g., HR onboarding tool)
    ↓
werkflow-erp REST API
    ↓
Stores/retrieves business data
    ↓
Custom Application applies business logic
```

**By another system:**
```
External ERP / HRIS / Finance system
    ↓
werkflow-erp REST API
    ↓
Data layer
```

**werkflow-erp is identical in all cases** — it doesn't know which system is calling it.

---

## Decision: Platform User ↔ HR Employee Linking (WITHOUT Coupling)

### The Problem

A **platform user** (person who logs into the system) may also be an **HR employee** (person tracked in payroll, assigned assets, requesting leave).

Examples:
- Alice is a platform admin AND an HR employee
- Bob is a contractor (HR employee) but NOT a platform user
- Charlie is a platform user but NOT tracked in HR

### The Solution: Decoupled Linking

werkflow-erp does NOT create or manage platform users. Instead:

1. **HR Employee is the source of truth** for organizational data (name, department, manager, salary)
2. **Platform User is the source of truth** for login credentials and platform roles (managed externally, e.g., Keycloak)
3. **Link is optional and read-only**: `Employee.platformUserId` field stores the external user ID if this employee has platform access

### Implementation: Link Without Coupling

The `Employee` entity tracks an optional platform user:

```java
@Entity
public class Employee {
    @Id
    private Long id;

    private String firstName;
    private String lastName;
    private String email;

    @Nullable
    private String platformUserId;  // ← Optional link to external user system
                                     // Could be Keycloak ID, LDAP DN, or any system

    private String departmentCode;
    private Long managerId;         // ← FK to another Employee
    private String costCentre;
    private String jobTitle;
    private LocalDate hireDate;
}
```

### Three Linking Patterns

**Pattern 1: External system links to werkflow-erp**

```
External Platform (e.g., Keycloak Admin)
    ↓ "User keycloak-uuid-123 is Alice, who is employee 42"
    ↓
PATCH /api/v1/hr/employees/42/platform-link
Body: { platformUserId: "keycloak-uuid-123" }
    ↓
werkflow-erp stores the link
    ↓
Later queries can find employee by platformUserId
GET /api/v1/hr/employees/platform/keycloak-uuid-123
```

**Pattern 2: werkflow-erp links to external system**

```
werkflow-erp Admin:
    ↓ "I want to link employee 42 to platform user"
    ↓
Call external system REST API to verify user exists
    ↓
PATCH /api/v1/hr/employees/42/platform-link
Body: { platformUserId: "verified-user-id" }
    ↓
werkflow-erp stores link WITHOUT further coupling
```

**Pattern 3: Batch reconciliation (webhook)**

```
External system (e.g., every night at 9 PM):
    ↓ "Sync all user IDs to werkflow-erp"
    ↓
POST /api/v1/hr/employees/platform-link-batch
Body: [
    { employeeId: 1, platformUserId: "user-123" },
    { employeeId: 2, platformUserId: "user-456" }
]
    ↓
werkflow-erp stores all links atomically
```

### Endpoints Required

```
# Link an employee to a platform user
PATCH /api/v1/hr/employees/{employeeId}/platform-link
Body: { platformUserId: string, platformSystem?: string }
Response: { id, firstName, lastName, platformUserId }

# Unlink (remove platform user association)
DELETE /api/v1/hr/employees/{employeeId}/platform-link
Response: { id, firstName, lastName, platformUserId: null }

# Query employee by platform user ID
GET /api/v1/hr/employees/platform/{platformUserId}
Response: { id, firstName, lastName, departmentCode, managerId, ... }

# Batch link
POST /api/v1/hr/employees/platform-link-batch
Body: [{ employeeId, platformUserId }, ...]
Response: { successCount, failureCount, errors: [...] }
```

### Key Principles (NO COUPLING)

1. **werkflow-erp never calls external systems** to create/manage users
2. **werkflow-erp never validates** that a platformUserId exists (caller is responsible)
3. **platformUserId is opaque**: Could be Keycloak ID, Azure AD OID, or LDAP DN
4. **No hardcoded assumptions**: No imports of Keycloak libraries, no OIDC configuration
5. **Works standalone**: Delete platformUserId field and werkflow-erp still functions perfectly

### Usage Scenarios

**Scenario 1: Standalone werkflow-erp (no platform user tracking)**
```
HR Manager uses werkflow-erp directly
    ↓
Never calls platform-link endpoints
    ↓
platformUserId is always null
    ↓
Everything works normally
```

**Scenario 2: werkflow integration (workflow tasks to employees)**
```
werkflow Portal: User logs in (Keycloak provides user ID = "abc123")
    ↓
werkflow Engine queries: GET /api/v1/hr/employees/platform/abc123
    ↓
werkflow-erp returns: { id: 42, firstName: "Alice", departmentCode: "ENG", ... }
    ↓
werkflow Engine uses employee info for task routing, variable enrichment
```

**Scenario 3: Multiple platform systems (hybrid setup)**
```
Some users from Keycloak (platformUserId = "keycloak-...")
Some users from Azure AD (platformUserId = "oid-...")
Some users not tracked in any platform (platformUserId = null)
    ↓
werkflow-erp stores all relationships
    ↓
Each calling system resolves: "This user ID came from {Keycloak|AzureAD|...}"
    ↓
werkflow-erp doesn't care — it just returns matching employees
```

---

## Decision: Zero External Dependencies (Complete Independence)

### Principle: werkflow-erp must not import werkflow code

werkflow-erp is a **completely standalone service**. It MUST NOT:

- Import werkflow Engine code
- Import werkflow Admin code
- Depend on werkflow being running
- Have hardcoded assumptions about Keycloak
- Call external systems for core functionality

**Examples of FORBIDDEN imports:**
```java
❌ import com.werkflow.engine.*;
❌ import com.werkflow.admin.*;
❌ import org.keycloak.admin.client.*;  // (except for test/demo configs)
❌ new KeycloakClient(...);
```

**Examples of ALLOWED dependencies:**
```java
✅ import org.springframework.boot.*;
✅ import org.springframework.security.oauth2.*;  // JWT parsing only
✅ import org.postgresql.*;
✅ import java.util.*;
✅ import lombok.*;
```

### Why This Matters

If werkflow-erp imports werkflow code:
- ❌ Deploying werkflow-erp requires building/deploying werkflow first
- ❌ Upgrading werkflow breaks werkflow-erp
- ❌ Can't use werkflow-erp without werkflow
- ❌ Hard to test werkflow-erp in isolation

If werkflow-erp is independent:
- ✅ werkflow-erp deployment is completely independent
- ✅ werkflow-erp works with ANY orchestration system (werkflow, Zapier, cron, manual)
- ✅ Can test werkflow against werkflow-erp without needing werkflow code
- ✅ Can replace werkflow with another orchestrator, still use werkflow-erp

### Implementation Check

Add this to CI/CD pipeline:

```bash
#!/bin/bash
# Fail if werkflow-erp imports werkflow code
if grep -r "import com.werkflow.engine" services/business/ || \
   grep -r "import com.werkflow.admin" services/business/ || \
   grep -r "import com.werkflow.portal" services/business/; then
  echo "ERROR: werkflow-erp has forbidden imports"
  exit 1
fi
echo "OK: werkflow-erp is independent"
```

---

## Decision: Cross-Domain Foreign Key Validation

### Pattern: CrossDomainValidator (Same Service)

**Problem**: `PrLineItem.budgetCategoryId` references `finance_service.budget_categories` but has no validation. Invalid IDs are silently accepted.

**Solution**: Create a `CrossDomainValidator` component for each validation boundary.

```java
@Service
public class CrossDomainValidator {

    @Autowired
    private BudgetCategoryRepository budgetCategoryRepository;

    @Autowired
    private DepartmentRepository departmentRepository;

    public void validateBudgetCategoryExists(Long budgetCategoryId) {
        if (!budgetCategoryRepository.existsById(budgetCategoryId)) {
            throw new EntityNotFoundException(
                "BudgetCategory not found: " + budgetCategoryId
            );
        }
    }

    public void validateDepartmentExists(Long departmentId) {
        if (!departmentRepository.existsById(departmentId)) {
            throw new EntityNotFoundException(
                "Department not found: " + departmentId
            );
        }
    }
}
```

**Where to call**:

```java
@Service
public class PurchaseRequestService {

    @Autowired
    private CrossDomainValidator validator;

    public PurchaseRequest createRequest(PurchaseRequestRequest request) {
        // Validate cross-domain references BEFORE persist
        validator.validateBudgetCategoryExists(request.getBudgetCategoryId());

        PurchaseRequest pr = new PurchaseRequest();
        pr.setBudgetCategoryId(request.getBudgetCategoryId());
        // ...
        return purchaseRequestRepository.save(pr);
    }
}
```

**Why direct repository call (not REST)?**

Both schemas are in the same PostgreSQL database and same deployed service. A REST call would:
- Add 50-100ms latency per request
- Create a fake network boundary
- Introduce failure modes (timeout, 5xx)
- Require caching to be practical

Direct repository call is correct here.

---

## Decision: Cross-Domain FK Validation Pattern

**Status**: Accepted (P0.4)

**Context**: Services have foreign key references to entities in other domains (e.g.,
PurchaseRequest.requestingDeptId → HR.Department). Without validation, callers can
provide invalid or non-existent IDs, causing silent failures and downstream errors
in the workflow orchestration layer.

**Decision**: Implement a centralized `CrossDomainValidator` service that validates
FK existence and tenant isolation at the service layer, before entity creation/update.
Validators throw `EntityNotFoundException` which bubble to controllers and are mapped
to HTTP 404 (or 400 in future with standardized error responses in P1.1).

**Rationale**:
- Service layer is responsible for business rules validation
- Centralizes validators for reuse across multiple services
- Tenant isolation enforced at validation time (not after the fact)
- Follows existing error handling patterns (`EntityNotFoundException`)
- Extensible: new validators added to one service as features need them

**Consequences**:
- Service layer adds validator injection dependency
- Each create/update method must call relevant validators
- Improved data integrity: invalid FKs caught at write time
- Controller error responses may improve in P1.1 (currently 404, future 400 + error code)

**Implementation**:
- `CrossDomainValidator` in `common/validator/` package
- Injected into services that have cross-domain FKs
- Two initial validators: `validateDepartmentExists()`, `validateBudgetCategoryExists()`
- Additional validators added as TODO for future phases (Vendor, Employee, Asset*, Budget*)

**Related Components**: TenantContext (P0.1), all domain services (PurchaseRequest, CustodyRecord, etc.)

---

## Decision: User ID Handling (Opaque, External)

### Pattern: Trust the Caller, Never Validate Externally

**Problem**: Entities reference user IDs (e.g., `CustodyRecord.custodianUserId`, `PurchaseRequest.requesterUserId`). Where do these come from? How much should werkflow-erp validate them?

**Solution: Store them as-is, trust the caller.**

werkflow-erp does NOT validate, enrich, or call external systems to verify user IDs. Reasons:

1. **JWT already validated**: The caller authenticated via JWT (validated by `SecurityConfig`). If the JWT is valid, the user exists in whatever system issued the token.

2. **Zero external dependencies**: werkflow-erp doesn't know or care if user IDs come from Keycloak, Azure AD, LDAP, or a custom system.

3. **No coupling**: User ID is just a string identifier. Store it, return it, don't interpret it.

4. **Works standalone**: Users can supply user IDs manually (no auth system needed) for testing/integration.

**Implementation:**

```java
@Entity
public class CustodyRecord {
    @Id
    private Long id;

    @Column(nullable = false)
    private String custodianUserId;  // ← Just a string. Could be UUID, email, LDAP DN, etc.
                                      // Caller provides it, werkflow-erp stores it.

    private String assignedByUserId;  // ← Same pattern
    // ...
}

@Service
public class CustodyRecordService {

    public CustodyRecord create(CustodyRecordRequest request) {
        CustodyRecord record = new CustodyRecord();
        record.setCustodianUserId(request.getCustodianUserId());  // ← No validation
        record.setAssignedByUserId(request.getAssignedByUserId());  // ← No validation
        record.setCustodianDeptId(request.getCustodianDeptId());
        // ...
        return custodyRecordRepository.save(record);
    }
}
```

### User ID Scenarios (All Work the Same)

**Scenario 1: werkflow (Keycloak)**
```
werkflow Portal: User logs in → Keycloak issues JWT with sub = "keycloak-uuid-123"
    ↓
werkflow Engine extracts sub from JWT
    ↓
werkflow Engine calls: POST /api/v1/inventory/custody-records
    Body: { custodianUserId: "keycloak-uuid-123", ... }
    ↓
werkflow-erp stores: custodianUserId = "keycloak-uuid-123" (no validation)
```

**Scenario 2: SAP system**
```
SAP ERP integration: Employee management system
    ↓
Calls: POST /api/v1/inventory/custody-records
    Body: { custodianUserId: "SAP-EMP-42", ... }
    ↓
werkflow-erp stores: custodianUserId = "SAP-EMP-42" (no validation)
```

**Scenario 3: Manual REST client / testing**
```
Test script or API client
    ↓
Calls: POST /api/v1/inventory/custody-records
    Body: { custodianUserId: "alice@example.com", ... }
    ↓
werkflow-erp stores: custodianUserId = "alice@example.com" (no validation)
```

**All three work identically.** werkflow-erp doesn't care what format the user ID is.

### Optional Enrichment (Caller's Responsibility)

If a calling system (werkflow, Zapier, custom app) needs to enrich responses with user display names, that system should:

1. Cache user data locally (5-minute TTL)
2. Call its own user lookup service (Keycloak API, LDAP query, database join)
3. Merge the display name into the werkflow-erp response

werkflow-erp never calls external user services.

---

## Decision: Optional Workflow Tracking Fields (NOT Coupling)

### Problem

werkflow-erp entities have optional fields like `processInstanceId` that are used by orchestration systems (e.g., werkflow Engine) to track workflow state. These MUST NOT create coupling.

### Solution: Optional Tracking Fields

Add optional tracking fields to key entities that external systems MAY use:

**AssetRequest:**
```java
@Entity
public class AssetRequest {
    @Id
    private Long id;

    private String requesterUserId;
    private Long assetDefinitionId;
    private Long assetCategoryId;
    private Integer quantity;

    @Nullable
    private String externalProcessId;  // ← Optional: for workflow tracking
                                        // e.g., BPMN processInstanceId,
                                        // job scheduler ID, approval flow ID

    @Enumerated(EnumType.STRING)
    private AssetRequestStatus status;  // PENDING, APPROVED, REJECTED, etc.
}
```

**PurchaseRequest:**
```java
@Entity
public class PurchaseRequest {
    @Id
    private Long id;

    private String prNumber;
    private Long requestingDeptId;
    private String requesterUserId;

    @Nullable
    private String externalProcessId;  // ← Optional workflow tracking

    @Enumerated(EnumType.STRING)
    private PrStatus status;
}
```

### How This Works (WITHOUT Coupling)

**Standalone usage (no external system):**
```
Client creates asset request:
    POST /api/v1/inventory/asset-requests
    Body: { requesterUserId: "...", assetDefinitionId: 123, quantity: 1 }

werkflow-erp response:
    { id: 42, status: "PENDING", externalProcessId: null }

Client updates status directly:
    PATCH /api/v1/inventory/asset-requests/42/status
    Body: { status: "APPROVED" }
```

**With external orchestration system (werkflow or other):**
```
Orchestration system wants to track workflow:
    POST /api/v1/inventory/asset-requests
    Body: {
        requesterUserId: "...",
        assetDefinitionId: 123,
        externalProcessId: "bpmn-proc-uuid-456"  // ← Optional
    }

werkflow-erp response:
    { id: 42, status: "PENDING", externalProcessId: "bpmn-proc-uuid-456" }

Orchestration system updates status AND calls its own callbacks:
    PATCH /api/v1/inventory/asset-requests/42/status
    Body: { status: "APPROVED" }

werkflow-erp stores status change, orchestration system proceeds with its logic
```

### Endpoints (Status Updates Only)

werkflow-erp provides **pure status update endpoints**, not workflow callbacks:

```
# Update asset request status
PATCH /api/v1/inventory/asset-requests/{id}/status
Body: { status: "APPROVED" | "REJECTED" }
Response: { id, status, updatedAt, ... }

# Update purchase request status
PATCH /api/v1/procurement/purchase-requests/{id}/status
Body: { status: "PENDING_APPROVAL" | "APPROVED" | "ORDERED" }
Response: { id, status, updatedAt, ... }

# Update custody record status
PATCH /api/v1/inventory/custody-records/{id}/status
Body: { status: "ACTIVE" | "ENDED" }
Response: { id, status, updatedAt, ... }
```

### Key Points (NO COUPLING)

1. **externalProcessId is purely informational**: werkflow-erp doesn't validate it, doesn't call it, doesn't depend on it
2. **Status updates are simple operations**: No conditional logic, no decision making
3. **Any system can update status**: werkflow, Zapier, cron job, REST client — all the same
4. **Works without external system**: `externalProcessId = null` is perfectly valid
5. **No hardcoded workflow references**: "callback" terminology is avoided; these are just status updates

### Examples

**werkflow using werkflow-erp (for testing)**
```
User submits asset request form in werkflow Portal
    ↓
werkflow Engine:
  a. Calls POST /api/v1/inventory/asset-requests
     (stores externalProcessId = BPMN process UUID)
  b. Executes approval task
  c. On approval: calls PATCH /asset-requests/{id}/status
  d. Engine continues with its own logic (notifications, task routing)
```

**Standalone HR app using werkflow-erp**
```
HR Manager approves asset request via direct API call
    ↓
PATCH /api/v1/inventory/asset-requests/42/status
Body: { status: "APPROVED" }
    ↓
werkflow-erp stores status change
    ↓
HR app queries the updated record
    ↓
No external workflow system involved
```

**Third-party system using werkflow-erp**
```
Zapier workflow (or IFTTT, Make.com, etc.):
    1. Receives webhook: "Purchase order approved in SAP"
    2. Calls PATCH /api/v1/procurement/purchase-orders/99/status
    3. Body: { status: "APPROVED" }
    4. werkflow-erp stores it
    5. Zapier continues to next step (send email, create Slack message, etc.)

werkflow-erp doesn't know or care about Zapier
```

---

## Decision: List Endpoint Pagination

**Status**: Accepted (P0.6)

**Pattern**: Spring Data `Page<T>` with `Pageable` query parameters.

**All GET list endpoints return `Page<Dto>` instead of `List<Dto>`.**

```java
// Before
@GetMapping
public ResponseEntity<List<EmployeeResponse>> getAllEmployees() {
    return ResponseEntity.ok(employeeService.getAllEmployees());
}

// After
@GetMapping
public ResponseEntity<Page<EmployeeResponse>> getAllEmployees(
    @ParameterObject Pageable pageable) {
    return ResponseEntity.ok(employeeService.getAllEmployees(pageable));
}
```

**Query parameters** (handled by Spring Data):
```
GET /api/v1/hr/employees?page=0&size=20&sort=createdAt,desc
```

**Response metadata**:
```json
{
  "content": [...],
  "totalElements": 245,
  "totalPages": 13,
  "number": 0,
  "size": 20
}
```

**Configuration** (`application.yml`):
```yaml
spring:
  data:
    web:
      pageable:
        default-page-size: 20
        max-page-size: 1000
        one-indexed-parameters: false
```

**Rationale**:
- Standard Spring Data pattern, widely understood
- Springdoc auto-documents `Page` and `Pageable` in Swagger
- Supports sorting, filtering large datasets efficiently
- Zero custom serialization needed
- Matches industry conventions

**Scope**: ~18-20 list endpoints across all 4 domains (HR, Finance, Procurement, Inventory)

**Breaking change**: Clients must unpack `response.content` instead of treating response as array. This is acceptable within `/api/v1`.

---

## Decision: API Versioning

### Pattern: /api/v1/resource Paths

**All endpoints must use `/api/v1/` prefix.**

```
✅ Correct:   GET /api/v1/hr/employees
❌ Wrong:     GET /api/hr/employees
```

**Why version now?**

1. **Multi-tenancy**: v2 may add `X-Tenant-ID` header validation
2. **API evolution**: Binary protocol changes can happen (breaking changes)
3. **Connector Registry**: Registered connectors are versioned
4. **Pagination**: `/api/v1` allows breaking changes like List→Page return types within same major version

---

## Decision: Idempotency Keys

### Pattern: Idempotency-Key Header

**All POST/PUT endpoints that create or mutate state must support idempotency.**

```
POST /api/v1/inventory/asset-requests
Headers:
  Idempotency-Key: "550e8400-e29b-41d4-a716-446655440000"
Body: { ... }
```

**Response (first call):**
```
201 Created
Headers:
  Idempotency-Key: "550e8400-e29b-41d4-a716-446655440000"
Body: { id: 123, ... }
```

**Response (retry with same key):**
```
200 OK
Headers:
  Idempotency-Key: "550e8400-e29b-41d4-a716-446655440000"
Body: { id: 123, ... }  ← same response, not 201
```

**Implementation**: `IdempotencyFilter` intercepts POST/PUT, stores `(idempotencyKey, responseBody, timestamp)` in a `IdempotencyRecord` table, TTL 24 hours.

### Client Discovery: OpenAPI Documentation

**All single-object creation endpoints document idempotency support via OpenAPI annotations:**

```java
@PostMapping
@Operation(
    summary = "Create new purchase request",
    description = "Supports idempotent creation via Idempotency-Key header. " +
        "Provide a unique idempotency key to safely retry failed requests without duplicating the resource. " +
        "If the key is omitted, each request is processed independently. " +
        "If the same key is used with different payloads, a 409 Conflict is returned."
)
@PreAuthorize("isAuthenticated()")
public ResponseEntity<PurchaseRequestResponse> createPurchaseRequest(
    @Valid @RequestBody PurchaseRequestRequest request,
    @RequestHeader(name = "Idempotency-Key", required = false) String idempotencyKey) {
    return ResponseEntity.status(HttpStatus.CREATED).body(prService.createPurchaseRequest(request));
}
```

**Scope**: 22 single-object POST endpoints across 4 domains:
- **Procurement** (4 endpoints): PurchaseRequest, PurchaseOrder, Receipt, Vendor
- **Inventory** (7 endpoints): AssetRequest, AssetInstance, CustodyRecord, TransferRequest, MaintenanceRecord, AssetCategory, AssetDefinition
- **Finance** (5 endpoints): ApprovalThreshold, BudgetCategory, BudgetLineItem, BudgetPlan, Expense
- **HR** (6 endpoints): Attendance, Department, Employee, Leave, Payroll, PerformanceReview

**Clients can discover idempotency support by:**
1. Viewing the endpoint's `@Operation` description in Swagger/OpenAPI docs
2. Checking the `Idempotency-Key` parameter in the request schema
3. Following the documented behavior pattern (retry with same key = same response, different payload = 409 Conflict)

---

## Decision: Multi-Tenancy Scoping

### Pattern: Tenant Isolation at Query Layer

**Every query and mutation must include a tenant filter.**

```java
@Entity
public class AssetRequest {
    @Id
    private Long id;

    @Column(nullable = false)
    private String tenantId;  // ← MUST be on every entity

    // ... other fields
}
```

**Repository queries:**

```java
public interface AssetRequestRepository extends JpaRepository<AssetRequest, Long> {
    List<AssetRequest> findByTenantIdAndRequesterUserId(
        String tenantId,
        String requesterUserId
    );
}
```

**Service layer:**

```java
@Service
public class AssetRequestService {

    public List<AssetRequest> listByUser(String requesterUserId) {
        String tenantId = TenantContext.getTenantId();  // from JWT or header
        return repository.findByTenantIdAndRequesterUserId(tenantId, requesterUserId);
    }
}
```

**Critical**: `BudgetCheckService.checkBudgetAvailability()` currently queries by `departmentId` only. **It must add `.where(tenantId = ?)` to prevent cross-tenant budget leaks.**

---

## Consequences

### Positive

- **Complete independence**: werkflow-erp has zero imports from werkflow, can be used standalone
- **Vendor-agnostic**: Any system can call werkflow-erp APIs (werkflow, Zapier, SAP, custom app, etc.)
- **Testing flexibility**: werkflow platform can test its workflows using werkflow-erp as the test data provider
- **Deployment options**:
  - Standalone ERP for teams that don't use werkflow
  - Integrated with werkflow for workflow-driven businesses
  - Both can use the same data service simultaneously
- **No coupling risks**: Code changes to werkflow don't require changes to werkflow-erp
- **Scalability**: werkflow-erp can be scaled, upgraded, or replaced without affecting other systems

### Tradeoffs

- **Network calls**: Every operation requires an HTTP call (not in-process)
- **Latency**: API call overhead vs direct method calls
- **Eventual consistency**: Multiple systems can write the same data, no guaranteed ordering
- **Deployment complexity**: Two independent services to maintain

### Risks and Mitigations

| Risk | Mitigation |
|------|-----------|
| werkflow-erp data not synced with platform users | `platformUserId` link is manual/event-driven, not automatic. No coupling. |
| Two systems modify same record simultaneously | Idempotency keys + timestamps prevent duplicate writes |
| API version mismatch between systems | Semantic versioning (`/api/v1`, `/api/v2`), deprecation period before breaking changes |
| werkflow-erp down → werkflow can't operate | werkflow should design around graceful degradation or use fallback data source |
| No single source of truth | Each system is authoritative for its domain (werkflow for tasks, werkflow-erp for business data) |

---

## Related Documents

- ROADMAP.md — Implementation priorities for achieving this architecture
- docs/API-Contract.md — OpenAPI schema and endpoint specifications
- ADR-002 (TBD) — Caching strategy
- ADR-003 (TBD) — Error handling and retry policies

