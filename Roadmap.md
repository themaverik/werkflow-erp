# werkflow-erp Implementation Roadmap

**Project**: Standalone ERP Data Service for HR, Finance, Procurement, Inventory
**Status**: Pre-MVP — Extracted from main werkflow platform
**Last Updated**: 2026-04-10
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

**Status**: P1.2.5 COMPLETE ✓ — User Identity Architecture implemented
**Active Phase**: P1 — Quality & Integration
**Next Task**: P1.5 — Test Suite (contract + integration tests)
**Last Commit**: 6c95edc test(P1.2.5): add security tests for JWT claims minimization and GDPR compliance
**Branch**: feature/p1-quality-integration
**Test Status**: 231 tests passing, 0 failures

---

## Phase Summary

| Phase | Status | Completion |
|-------|--------|-----------|
| **P0 — Critical Path** | ✅ COMPLETE | 6/6 items (Multi-tenant, Idempotency, processInstanceId, FK validation, API versioning, Pagination) |
| **P1.1 — API Standardization** | ✅ COMPLETE | Error responses, Enum metadata, DTO examples (118 tests passing) |
| **P1.2 — HR/Keycloak Linking** | ✅ COMPLETE | PATCH endpoint, tenant isolation, conflict detection (13 tests passing) |
| **P1.2.5 — User Identity (OIDC)** | ✅ COMPLETE | 6 phases, 231 tests passing (18 hours) |
| **P1.3 — User Name Enrichment** | ⏸️ DEFERRED | Superseded by P1.2.5 (better architecture) |
| **P1.4 — Number Generation** | ✅ COMPLETE | PR/PO/GRN sequence fix (database sequences) |
| **P1.5 — Test Suite** | ⏳ PENDING | Contract + Integration tests (12 hours) |
| **P2 — Documentation & Release** | ⏳ PENDING | README, API guide, integration guide (6 hours) |
| **P3 — Future Enhancements** | 🔮 FUTURE | Audit logging, CapEx, filtering, webhooks, bulk ops, custom fields |

---

## Execution Plan

### ✅ P0 — Critical Path to Production (COMPLETE)

All foundation work complete. Summary:
- [x] **P0.1** Multi-Tenant Isolation (TenantContext, TenantContextFilter, all 23 entities scoped)
- [x] **P0.2** Idempotency (IdempotencyRecord entity, IdempotencyFilter)
- [x] **P0.3** processInstanceId Pattern (asset/purchase request workflows)
- [x] **P0.4** Cross-Domain FK Validation (CrossDomainValidator service)
- [x] **P0.5** API Versioning (/api/v1 context path, all docs updated)
- [x] **P0.6** Pagination (all 18-20 list endpoints with Page<T>, Pageable, sorting)

**Test Status**: 40/40 tests passing (integration tests verified)

---

### ✅ P1.1 — API Contract Standardization (COMPLETE)

- [x] **P1.1.1** Expose enum metadata endpoint (GET /api/v1/meta/enums, 15 enums, 4 domains)
- [x] **P1.1.2** Add request/response examples to all DTOs (@Schema annotations, realistic JSON)
- [x] **P1.1.3** Standardize error responses (ErrorResponse DTO, GlobalExceptionHandler, 7 tests)

**Test Status**: 118 tests passing (P0 + P1.1 combined)

---

### ✅ P1.2 — HR Module: Keycloak Linking (COMPLETE)

- [x] **P1.2.1** Create keycloak-link endpoint (PATCH /api/v1/hr/employees/{id}/keycloak-link, idempotent, tenant-scoped, conflict detection)
- [x] **P1.2.2** Document HR integration flow (Design spec, Implementation plan)

**Test Status**: 13 new tests (4 DTO + 5 service + 4 controller), all passing

---

### ✅ P1.2.5 — User Identity Architecture (OIDC-Compliant) (COMPLETE)

**Design**: `docs/ADR-002-User-Identity-And-JWT-Claims.md` ✓
**Plan**: `docs/superpowers/plans/2026-04-08-p1.2-keycloak-linking.md` ✓

#### Phase 1: Core Infrastructure (3-4 hours)
- [x] **1.1** User Entity and Database Migrations (users table, audit columns, V24 Flyway)
- [x] **1.2** UserInfoResolver Service with Caching (Caffeine, issuer discovery, upsert, 8+ tests)
- [x] **1.3** UserContext Component (extract JWT sub, ThreadLocal, UserContextFilter, 5+ tests)

#### Phase 2: Security Updates (1-2 hours)
- [x] **2.1** Update SecurityConfig for OIDC Compliance (generic OidcRoleConverter, configurable roles claim, 4+ tests)

#### Phase 3: Response DTO Updates (2-3 hours)
- [x] **3.1** Update All Audit-Relevant Response DTOs (add createdByDisplayName, updatedByDisplayName to 13 DTOs, service layer populates names)

#### Phase 4: Testing (4-5 hours)
- [x] **4.1** Unit Tests: UserInfoResolver (cache hit/miss, issuer discovery, timeout, error handling, concurrent requests, 8+ tests)
- [x] **4.2** Unit Tests: UserContext (extract sub, resolve profile, ThreadLocal, clear on exit, 5+ tests)
- [x] **4.3** Integration Tests: werkflow → werkflow-erp with Display Names (multi-user, cache behavior, cross-tenant, 6+ tests)
- [x] **4.4** Security Tests: JWT Claims and Logs (minimal JWT, no PII in logs, configurable roles, GDPR compliance, 5+ tests)

#### Phase 5: Documentation Updates (1-2 hours)
- [x] **5.1** Update ROADMAP.md (mark complete, document actual hours)
- [x] **5.2** Update README.md (User Identity Architecture section, OIDC compliance, config examples)
- [x] **5.3** Update Integration Docs (docs/WERKFLOW_INTEGRATION.md: display names, no extra calls)

#### Phase 6: Final Validation (1 hour)
- [x] **6.1** Full Test Suite Run (mvn clean test, 231 tests passing, 0 failures)
- [x] **6.2** Manual Verification (Keycloak-compliant OIDC role extraction, display names in all audit responses)

**Total Estimated**: 12-17 hours across 1-2 sessions
**Total Actual**: 18 hours across 1 session (estimated 12-17 hours)

**P1.2.5 Completion Summary:**
- User entity and UserRepository with upsert capability
- UserInfoResolver service with Caffeine caching (TTL 10 min)
- UserContext and UserContextFilter for request-scoped identity
- OidcRoleConverter for OIDC-compliant role extraction (configurable)
- All 13 audit-relevant Response DTOs with display names
- 56+ comprehensive tests (unit, integration, security)
- GDPR/CCPA compliance verified
- 231 total tests passing

---

### ⏸️ P1.3 — User Name Enrichment (DEFERRED — Superseded by P1.2.5)

**Status**: DEFERRED (not tracked for MVP, better architecture in P1.2.5)

**Reason**: P1.3 was designed to call Admin Service for names, but this violates werkflow-erp decoupling principle. P1.2.5 provides a better solution: OIDC UserInfo endpoint pattern works for standalone and integrated deployments without external dependencies.

**Post-MVP**: After P1.2.5 complete, consider adding name change sync strategy (event-driven updates when Keycloak user modified).

---

### ✅ P1.4 — Number Generation & Collision Prevention (COMPLETE)

- [x] **P1.4.1** Fix PR number generation (database sequence: `pr_seq_{TENANT_ID}`, format: `PR-{tenantId}-{year}-{seq:05d}`)
- [x] **P1.4.2** Apply same pattern to PO and Receipt numbers (PO: `PO-{tenantId}-{year}-{seq:05d}`, GRN: `GRN-{tenantId}-{year}-{seq:05d}`)

**Test Status**: Pattern verified, V23 Flyway migration applied

---

### ⏳ P1.5 — Test Suite (PENDING — After P1.2.5)

**Estimated**: 12 hours (can run in parallel with P1.2.5 or after completion)

- [ ] **P1.5.1** Contract tests for domain services (HR: Employee, Department; Finance: BudgetCheck, Expense; Procurement: PurchaseRequest; Inventory: AssetRequest; 3-5 tests each, 8 hours)
- [ ] **P1.5.2** Integration tests: werkflow → werkflow-erp API calls (Asset request lifecycle, Budget check, Cross-tenant isolation, 4 hours)

**Target**: 160+ total tests after P1.2.5 + P1.5 complete

---

### ⏳ P2 — Documentation & Release (PENDING — After P1.5)

**Estimated**: 6 hours

- [ ] **P2.1.1** Update README.md (quick start, multi-tenancy setup, JWT auth, 2 hours)
- [ ] **P2.1.2** Create API usage guide (step-by-step workflow examples, 2 hours)
- [ ] **P2.1.3** Create integration guide for werkflow (connector registration, config, 2 hours)

**Optional** (can defer to post-MVP):
- [ ] **P2.2.1** Load test: 1000 concurrent requests
- [ ] **P2.2.2** Security audit: SQL injection, JWT, rate limiting

---

### 🔮 P3 — Future Enhancements (POST-MVP)

Not tracked for MVP, but documented for reference:

- [ ] **P3.1** Audit logging: All mutations logged with user + timestamp
- [ ] **P3.2** CapEx workflow implementation (currently stubbed)
- [ ] **P3.3** Advanced filtering: Complex queries (e.g., `assetCondition=GOOD AND status=IN_USE`)
- [ ] **P3.4** Webhook support: Notify werkflow when critical data changes
- [ ] **P3.5** Bulk operations: `POST /api/v1/bulk/asset-instances` for large imports
- [ ] **P3.6** Custom fields: Tenant-specific metadata on core entities

---

## Critical Path to MVP

**Current**: P0 ✓ + P1.1 ✓ + P1.2 ✓ + P1.2.5 ✓ + P1.4 ✓ (231 tests)

**Next 2-3 Sessions**:
1. **P1.2.5** User Identity Architecture (12-17 hours)
2. **P1.5** Test Suite (12 hours, can overlap with P1.2.5)
3. **P2** Documentation & Release (6 hours)

**Target**: 160+ tests passing, all docs updated, ready for MVP release

---

## Related Documents

- `docs/ADR-001-Service-Boundary-Architecture.md` — Architecture decisions
- `docs/ADR-002-User-Identity-And-JWT-Claims.md` — User identity design
- `docs/superpowers/plans/2026-04-08-p1.2-keycloak-linking.md` — Implementation plan
- `docs/superpowers/specs/2026-04-08-p1.2-keycloak-linking-design.md` — Design spec
- `README.md` — Project overview and quick start
- `FLOW_DIAGRAMS.md` — Business process flows (informational)
