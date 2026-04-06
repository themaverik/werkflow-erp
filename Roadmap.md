# werkflow-erp Implementation Roadmap

**Project**: Standalone ERP Data Service for HR, Finance, Procurement, Inventory
**Status**: Pre-MVP — Extracted from main werkflow platform
**Last Updated**: 2026-04-06
**Architecture**: See `docs/ADR-001-Service-Boundary-Architecture.md`

---

## How To Use This File

Single source of truth for task tracking and session continuity.

### Task Status Markers

| Marker | Status |
|--------|--------|
| `[ ]` | Pending |
| `[~]` | In Progress — note what remains |
| `[x]` | Completed — note commit hash if applicable |
| `[!]` | Blocked — note reason |

### Resume Instructions

1. Find first `[~]` task in active phase
2. If none, start first `[ ]` in lowest active phase
3. Branch check: confirm you're on the right branch before writing code

---

## Current Session State

**Status**: P0.1.1 Complete — Moving to P0.1.2
**Active Phase**: P0 — Critical Path to Production (Weeks 1-2)
**Current Task**: P0.1.2 — Update repositories to filter by tenantId
**Last Commit**: 8fdaf5b feat(P0.1.1): add tenantId to all domain entities
**Branch**: feature/p0-multi-tenancy

**Session Notes**:
- P0.1.1 COMPLETE: tenantId columns added to 23 entities via V21 migrations, contract test in place
- Architecture decisions finalized (see docs/ADR-001-Service-Boundary-Architecture.md)
- Ready to implement repository query filtering
- Estimated P0 completion: 1.5 weeks

---

## Project Health

**Current State**: werkflow-erp extracted as standalone service, all CRUD APIs implemented but missing:
- Multi-tenant scoping (CRITICAL)
- Idempotency for safe retries
- Cross-domain FK validation
- API versioning
- Pagination on list endpoints

**Architecture**: Pure CRUD service layer. Orchestration, approvals, and workflow logic stay in main werkflow platform.

**Optional Deployment**: werkflow-erp can be deployed independently or replaced by client's own ERP system via Connector Registry.

---

## Execution Order

### P0 — Critical Path to Production (Weeks 1-2)

Must complete before any production deployment.

#### P0.1 — Multi-Tenant Isolation
- [x] **P0.1.1** Add `tenantId` column to all entities (hr, finance, procurement, inventory) *(commit: 8fdaf5b)*
  - [x] V21 Flyway migration: add `tenant_id` NOT NULL to all domain tables
  - [x] Add `@Column(nullable = false)` to all entity `tenantId` fields
  - [x] Entities: Employee, Department, Leave, Attendance, Payroll, PerformanceReview, BudgetPlan, BudgetCategory, BudgetLineItem, Expense, ApprovalThreshold, Vendor, PurchaseRequest, PurchaseOrder, Receipt, AssetCategory, AssetDefinition, AssetInstance, CustodyRecord, TransferRequest, MaintenanceRecord
  - [x] Contract test: TenantIdMigrationTest verifies schema

- [~] **P0.1.2** Update all repository queries to filter by `tenantId`
  - [ ] Create `TenantContext` utility to extract tenantId from JWT claims or header
  - [ ] Update `*Repository.findBy*` methods to require tenantId parameter
  - [ ] Update `*Service` layer to inject `TenantContext` and call `getTenantId()`
  - [ ] Test: 3 services (HR, Finance, Procurement) before Inventory
  - [ ] Estimated: 6 hours

- [ ] **P0.1.3** Add `TenantContext` middleware to SecurityFilterChain
  - [ ] Extract tenantId from JWT claim `organization_id` (or header fallback)
  - [ ] Store in `ThreadLocal<String>` for request scope
  - [ ] Clear on request exit
  - [ ] Estimated: 1 hour

- [ ] **P0.1.4** CRITICAL: Fix BudgetCheckService to scope by tenantId
  - [ ] `BudgetCheckService.checkBudgetAvailability()` must filter by tenantId + departmentId
  - [ ] Update `BudgetCheckController` to extract tenantId
  - [ ] Test: prevent cross-tenant budget queries
  - [ ] Estimated: 2 hours

#### P0.2 — Idempotency for Safe Retries
- [ ] **P0.2.1** Create `IdempotencyRecord` entity and repository
  - [ ] Fields: `idempotencyKey (UUID, unique)`, `responseBody (JSON)`, `statusCode (int)`, `createdAt (timestamp)`, `tenantId (FK)`
  - [ ] Repository: `findByIdempotencyKeyAndTenantId(String key, String tenantId)`
  - [ ] TTL cleanup: Scheduled task to delete records older than 24 hours
  - [ ] V22 Flyway migration
  - [ ] Estimated: 2 hours

- [ ] **P0.2.2** Create `IdempotencyInterceptor` and wire to SecurityFilterChain
  - [ ] Intercepts POST/PUT requests
  - [ ] Checks `Idempotency-Key` header
  - [ ] On duplicate key: return cached response with 200 OK
  - [ ] On first call: stores response after controller returns
  - [ ] Estimated: 3 hours

- [ ] **P0.2.3** Update POST/PUT endpoints to include `X-Idempotency-Key` documentation
  - [ ] Add `@RequestHeader(name = "X-Idempotency-Key")` to all creation methods
  - [ ] Update Swagger/OpenAPI docs
  - [ ] Estimated: 1 hour

#### P0.3 — processInstanceId Race Condition Fix
- [ ] **P0.3.1** Allow `processInstanceId` in asset request create payload
  - [ ] Update `AssetRequestCreateRequest` DTO to include optional `processInstanceId`
  - [ ] Update `AssetRequestController.create()` to accept it
  - [ ] Update `AssetRequestService.create()` to store it
  - [ ] Estimated: 1 hour

- [ ] **P0.3.2** Update werkflow integration docs
  - [ ] Document: werkflow should generate processInstanceId first, then call POST
  - [ ] Document fallback: if unavailable, use existing `PATCH /api/v1/inventory/asset-requests/{id}` endpoint
  - [ ] Estimated: 1 hour

- [ ] **P0.3.3** Apply same pattern to PurchaseRequest and PurchaseOrder
  - [ ] Update create DTOs and service
  - [ ] Estimated: 2 hours

#### P0.4 — Cross-Domain FK Validation
- [ ] **P0.4.1** Create `CrossDomainValidator` service
  - [ ] Methods: `validateBudgetCategoryExists(id)`, `validateDepartmentExists(id)`
  - [ ] Inject `BudgetCategoryRepository`, `DepartmentRepository`
  - [ ] Throw `EntityNotFoundException` on missing FK
  - [ ] Estimated: 1 hour

- [ ] **P0.4.2** Wire validation into service layer
  - [ ] `PurchaseRequestService.create()` calls `validator.validateBudgetCategoryExists()`
  - [ ] `CustodyRecordService.create()` calls `validator.validateDepartmentExists()`
  - [ ] Test: unit tests for each validation
  - [ ] Estimated: 2 hours

#### P0.5 — API Versioning (/api/v1)
- [ ] **P0.5.1** Update application.yml context-path to include version
  - [ ] Change: `server.servlet.context-path: /api` → `/api/v1`
  - [ ] Verify all controller `@RequestMapping` paths are compatible
  - [ ] Update Swagger `api-docs.path: /api-docs` → `/api/v1/api-docs`
  - [ ] Estimated: 1 hour

- [ ] **P0.5.2** Update all integration points to use /api/v1
  - [ ] Update `docker-compose.yml` service URLs
  - [ ] Update application.yml service endpoint references
  - [ ] Update werkflow integration docs
  - [ ] Estimated: 1 hour

#### P0.6 — Pagination on List Endpoints
- [ ] **P0.6.1** Add pagination to all GET list endpoints
  - [ ] Add `Pageable` parameter: `?page=0&size=20&sort=createdAt,desc`
  - [ ] Return `Page<Dto>` instead of `List<Dto>`
  - [ ] Controllers: EmployeeController, DepartmentController, etc. (all domains)
  - [ ] Estimated: 3 hours

- [ ] **P0.6.2** Set sensible defaults
  - [ ] Default size: 20
  - [ ] Max size: 1000
  - [ ] Default sort: createdAt DESC
  - [ ] Estimated: 1 hour

### P1 — Quality & Integration (Weeks 3)

#### P1.1 — API Contract Standardization
- [ ] **P1.1.1** Expose enum metadata endpoint
  - [ ] `GET /api/v1/meta/enums`
  - [ ] Returns: all enum types (AssetRequestStatus, PrStatus, etc.) with values and labels
  - [ ] Used by werkflow at BPMN design time
  - [ ] Estimated: 2 hours

- [ ] **P1.1.2** Add request/response examples to all DTOs
  - [ ] Use `@Schema(example = "...")` annotations
  - [ ] Add to: EmployeeDto, AssetRequestDto, PurchaseRequestDto, etc.
  - [ ] Estimated: 2 hours

- [ ] **P1.1.3** Standardize error responses
  - [ ] All 4xx/5xx errors use consistent `ErrorResponse` format
  - [ ] Include: `code`, `message`, `timestamp`, `details`
  - [ ] Document in README
  - [ ] Estimated: 2 hours

#### P1.2 — HR Module: Keycloak Linking
- [ ] **P1.2.1** Create keycloak-link endpoint
  - [ ] `PATCH /api/v1/hr/employees/{employeeId}/keycloak-link`
  - [ ] Body: `{ keycloakUserId: string }`
  - [ ] Called by Admin Service after user provisioning
  - [ ] Estimated: 1 hour

- [ ] **P1.2.2** Document HR integration flow
  - [ ] HR system onboarding guide
  - [ ] API contract: Admin Service → werkflow-erp
  - [ ] Estimated: 1 hour

#### P1.3 — Admin Service User Enrichment (Optional)
- [ ] **P1.3.1** Create `UserEnrichmentService` with Caffeine cache
  - [ ] Optional call to Admin Service for user display names
  - [ ] Cache TTL: 5 minutes
  - [ ] Graceful degradation if Admin Service unavailable
  - [ ] Estimated: 2 hours

- [ ] **P1.3.2** Update response DTOs to include enriched user names
  - [ ] CustodyRecord responses: include `custodianUserName`
  - [ ] PurchaseRequest responses: include `requesterUserName`
  - [ ] Estimated: 1 hour

#### P1.4 — Number Generation & Collision Prevention
- [ ] **P1.4.1** Fix PR number generation (currently uses System.currentTimeMillis)
  - [ ] Change: `PR-{tenantId}-{year}-{seq:05d}` (e.g., `PR-ACME-2026-00042`)
  - [ ] Use database sequence: `pr_number_seq`
  - [ ] V23 Flyway migration
  - [ ] Estimated: 2 hours

- [ ] **P1.4.2** Apply same pattern to PO numbers and Receipt numbers
  - [ ] PO: `PO-{tenantId}-{year}-{seq:05d}`
  - [ ] GRN: `GRN-{tenantId}-{year}-{seq:05d}`
  - [ ] Estimated: 1 hour

#### P1.5 — Test Suite
- [ ] **P1.5.1** Write contract tests for all domain services
  - [ ] HR: EmployeeService, DepartmentService (at least 3 tests each)
  - [ ] Finance: BudgetCheckService, ExpenseService (at least 3 tests each)
  - [ ] Procurement: PurchaseRequestService (at least 5 tests)
  - [ ] Inventory: AssetRequestService (at least 5 tests, including tenant isolation)
  - [ ] Estimated: 8 hours

- [ ] **P1.5.2** Integration tests: werkflow → werkflow-erp API calls
  - [ ] Test: Asset request lifecycle (create → process-instance → approve callback)
  - [ ] Test: Budget check before PR creation
  - [ ] Test: Cross-tenant isolation (verify one tenant can't query another's data)
  - [ ] Estimated: 4 hours

### P2 — Documentation & Release (Week 4)

#### P2.1 — Update Documentation
- [ ] **P2.1.1** Update README.md
  - [ ] Quick start with /api/v1 URLs
  - [ ] Multi-tenancy setup
  - [ ] Authentication with JWT + tenantId
  - [ ] Estimated: 2 hours

- [ ] **P2.1.2** Create API usage guide
  - [ ] Step-by-step: Create employee → Create asset request → Create PO
  - [ ] Include: JWT token, tenant header, idempotency key
  - [ ] Estimated: 2 hours

- [ ] **P2.1.3** Create integration guide for werkflow
  - [ ] How to register werkflow-erp as a connector
  - [ ] ExternalApiCallDelegate configuration
  - [ ] Estimated: 2 hours

#### P2.2 — Performance & Security
- [ ] **P2.2.1** Load test: 1000 concurrent requests
  - [ ] Tool: JMeter or Apache Bench
  - [ ] Verify: no deadlocks, no N+1 queries
  - [ ] Estimated: 2 hours

- [ ] **P2.2.2** Security audit
  - [ ] SQL injection: verify prepared statements
  - [ ] JWT validation: confirm tenant isolation can't be bypassed
  - [ ] Rate limiting: add if needed
  - [ ] Estimated: 2 hours

### P3 — Future Enhancements (Post-MVP)

- [ ] **P3.1** Audit logging: All mutations logged with user + timestamp
- [ ] **P3.2** CapEx workflow implementation (currently stubbed)
- [ ] **P3.3** Advanced filtering: Complex queries (e.g., `assetCondition=GOOD AND status=IN_USE`)
- [ ] **P3.4** Webhook support: Notify werkflow when critical data changes
- [ ] **P3.5** Bulk operations: `POST /api/v1/bulk/asset-instances` for large imports
- [ ] **P3.6** Custom fields: Tenant-specific metadata on core entities

---

## Critical Path to MVP (Estimated 3 Weeks)

**Week 1 (P0.1 – P0.6)**:
- Multi-tenant isolation ✓
- Idempotency ✓
- API versioning ✓
- Pagination ✓
- FK validation ✓

**Week 2 (P1.1 – P1.5)**:
- API contract clarity ✓
- Test suite ✓
- Integration with werkflow ✓

**Week 3 (P2)**:
- Documentation ✓
- Performance & security audit ✓
- Release preparation ✓

**Blockers**: None identified

---

## Notes for Next Session

When resuming:
1. Check git log for last commit hash
2. Run `git status` to see uncommitted work
3. Find first `[~]` task in this roadmap
4. Confirm branch matches task context
5. Pull any latest changes from main

---

## Related Documents

- `docs/ADR-001-Service-Boundary-Architecture.md` — Architecture decisions
- `README.md` — Project overview and quick start
- `FLOW_DIAGRAMS.md` — Business process flows (informational)

