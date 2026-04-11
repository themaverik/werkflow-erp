# Werkflow-ERP Implementation Roadmap

**Project**: Standalone ERP Data Service for HR, Finance, Procurement, Inventory
**Status**: Pre-MVP — Extracted from main Werkflow platform
**Last Updated**: 2026-04-11 (P2.1 complete, PR #6 submitted)
**Architecture**: See `docs/adr/ADR-001-Service-Boundary-Architecture.md`

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

**Status**: P1 COMPLETE | P2.1 COMPLETE
**Active Phase**: P2 — Documentation & Release (P2.1 done, awaiting PR review/merge)
**Completed Tasks**: P2.1.1, P2.1.2, P2.1.3 (all documentation)
**PR**: #6 created (docs(P2.1): Complete documentation suite with clean structure)
**Branch**: feature/p2-documentation (submitted for review)
**Test Status**: 255 tests passing (P1.5.1 complete, P1.5.2 deferred)
**Documentation Delivered**:
  - README: Logo, essentials-only (quick start, config, doc index)
  - API-Usage-Guide.md: 12 workflow examples across all 4 domains
  - Werkflow-Integration-Guide.md: Connector setup, BPMN examples, ProcessInstanceId patterns
  - Architecture-Overview.md: 5 business flow diagrams (mermaid), consolidated
  - docs/adr/: 3 Architecture Decision Records
  - docs/specs/: 6 specification files (P1.5, Conventions, Implementation)
  - docs/superpowers/: 12 implementation plans + 7 design specs (organized by phase)
**Documentation Changes**:
  - Removed all emojis/special characters (CLAUDE.md compliance)
  - Reorganized from flat to hierarchical structure
  - Fixed all Title-Case naming (API-Usage-Guide.md, Werkflow-Integration-Guide.md)
  - Fixed all 5 mermaid diagrams (Asset, Budget, Procurement, Callback, Security flows)
  - Consolidated Flow-Diagrams into Architecture-Overview
**Next Steps**:
  1. Merge PR #6 (documentation complete)
  2. P2.2 Load testing (optional, 2 hours)
  3. P1.5.2 Integration tests (4 hours, @WebMvcTest approach documented)
  4. Release prep for MVP

---

## Status Summary

**COMPLETED (7 phases)**
- P0: Critical Path (6/6) - 255 tests baseline
- P1.1: API Standardization (3/3)
- P1.2: HR/Keycloak Linking (2/2)
- P1.2.5: User Identity Architecture (6/6)
- P1.4: Number Generation (2/2)
- P1.5.1: Contract Tests (24 tests)
- P2.1: Documentation Suite (3/3) - PR #6 submitted

**IN REVIEW**
- P2 (PR #6: Complete documentation suite with clean structure)

**DEFERRED**
- P1.3: User Name Enrichment (superseded by P1.2.5)
- P1.5.2: Integration Tests (architectural solution documented, 4 hours)

**PENDING**
- P2.2: Load Testing (optional, 2 hours)
- P3: Future Enhancements (post-MVP)

**Test Status**: 255 passing (up from 231 baseline)

---

## Execution Plan

### P0 — Critical Path to Production (COMPLETE)

All foundation work complete. Summary:
- [x] **P0.1** Multi-Tenant Isolation (TenantContext, TenantContextFilter, all 23 entities scoped)
- [x] **P0.2** Idempotency (IdempotencyRecord entity, IdempotencyFilter)
- [x] **P0.3** processInstanceId Pattern (asset/purchase request workflows)
- [x] **P0.4** Cross-Domain FK Validation (CrossDomainValidator service)
- [x] **P0.5** API Versioning (/api/v1 context path, all docs updated)
- [x] **P0.6** Pagination (all 18-20 list endpoints with Page<T>, Pageable, sorting)

**Test Status**: 40/40 tests passing (integration tests verified)

---

### P1.1 — API Contract Standardization (COMPLETE)

- [x] **P1.1.1** Expose enum metadata endpoint (GET /api/v1/meta/enums, 15 enums, 4 domains)
- [x] **P1.1.2** Add request/response examples to all DTOs (@Schema annotations, realistic JSON)
- [x] **P1.1.3** Standardize error responses (ErrorResponse DTO, GlobalExceptionHandler, 7 tests)

**Test Status**: 118 tests passing (P0 + P1.1 combined)

---

### P1.2 — HR Module: Keycloak Linking (COMPLETE)

- [x] **P1.2.1** Create keycloak-link endpoint (PATCH /api/v1/hr/employees/{id}/keycloak-link, idempotent, tenant-scoped, conflict detection)
- [x] **P1.2.2** Document HR integration flow (Design spec, Implementation plan)

**Test Status**: 13 new tests (4 DTO + 5 service + 4 controller), all passing

---

### ✅ P1.2.5 — User Identity Architecture (OIDC-Compliant) (COMPLETE)

**Design**: `docs/adr/ADR-002-User-Identity-And-JWT-Claims.md`
**Plan**: `docs/superpowers/plans/2026-04-08-p1.2-keycloak-linking.md`

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

### P1.3 — User Name Enrichment (DEFERRED — Superseded by P1.2.5)

**Status**: DEFERRED (not tracked for MVP, better architecture in P1.2.5)

**Reason**: P1.3 was designed to call Admin Service for names, but this violates werkflow-erp decoupling principle. P1.2.5 provides a better solution: OIDC UserInfo endpoint pattern works for standalone and integrated deployments without external dependencies.

**Post-MVP**: After P1.2.5 complete, consider adding name change sync strategy (event-driven updates when Keycloak user modified).

---

### P1.4 — Number Generation & Collision Prevention (COMPLETE)

- [x] **P1.4.1** Fix PR number generation (database sequence: `pr_seq_{TENANT_ID}`, format: `PR-{tenantId}-{year}-{seq:05d}`)
- [x] **P1.4.2** Apply same pattern to PO and Receipt numbers (PO: `PO-{tenantId}-{year}-{seq:05d}`, GRN: `GRN-{tenantId}-{year}-{seq:05d}`)

**Test Status**: Pattern verified, V23 Flyway migration applied

---

### P1.5 — Test Suite (IN PROGRESS — After P1.2.5)

**Estimated**: 12 hours total | **Actual (P1.5.1)**: 4 hours | **Actual (P1.5.2 attempt)**: 1 hour (deferred)

**[x] P1.5.1** Contract tests for domain services *(commit: 4f1c2ee)*
- Goal: Unit tests for core domain services (non-HTTP)
- Tests implemented (24 total, all passing):
  - HR: EmployeeServiceContractTest (6 tests: duplicate email, keycloak linking idempotent, dept head uniqueness, tenant isolation)
  - Finance: BudgetCheckServiceContractTest (6 tests: no plan, sufficient/insufficient, default fiscal year, zero allocated, exact match)
  - Procurement: PurchaseRequestServiceContractTest (6 tests: invalid dept, tenant isolation, defaults, processInstanceId)
  - Inventory: AssetRequestServiceContractTest (6 tests: default status, approve/reject, processInstanceId, non-existent)
- Implementation: Mock-based unit tests with no entity construction, focus on service contracts
- Test count: 231 → 255 tests (+24)
- Status: COMPLETE

- [!] **P1.5.2** Integration tests specification documented *(see docs/P1.5.2-INTEGRATION-TESTS-SPEC.md)*
  - Specification complete: 3 workflows, 12-18 tests planned
  - **Blocked**: Architectural issue with TenantContext mocking in @SpringBootTest contexts
  - Attempted: BudgetCheckWorkflowIntegrationTest, CrossTenantIsolationIntegrationTest
  - Issue: TenantContext is request-scoped and requires TenantContextFilter initialization, which conflicts with test setup
  - **Solution for next session**: Use @WebMvcTest + MockMvc approach (HTTP-based integration tests instead of @SpringBootTest)
  - Detailed notes: See `docs/P1.5.2-INTEGRATION-TESTS-IMPLEMENTATION-NOTES.md`
  - Effort remaining: 4 hours (next session)

**Current Status**: 255 tests (P1.5.1 complete, P1.5.2 blocked on architecture, solution documented)

---

### P2 — Documentation & Release (IN REVIEW — PR #6 submitted)

**Estimated**: 6 hours | **Actual (P2.1)**: 2 hours (including reorganization, cleanup, mermaid fixes)

**[x] P2.1** Documentation Suite (COMPLETE - PR #6 submitted for review)
- [x] **P2.1.1** Update README.md with logo and focus *(multiple commits, finalized: e709fb7)*
- [x] **P2.1.2** Create API-Usage-Guide.md (12 workflow examples, all 4 domains) *(docs/API-Usage-Guide.md)*
- [x] **P2.1.3** Create Werkflow-Integration-Guide.md (BPMN, connector setup, ProcessInstanceId) *(docs/Werkflow-Integration-Guide.md)*
- [x] Reorganized docs structure (adr/, specs/, superpowers/) *(commit: 90b34e5)*
- [x] Removed all emojis/special characters *(commits: 9572390, dde129a)*
- [x] Fixed all mermaid diagrams (Asset, Budget, Procurement, Callback, Security) *(commits: e2a0999, e709fb7)*
- [x] Title-Case naming throughout *(commit: c576ca4)*
- [x] PR #6: Complete documentation suite with clean structure (ready for review/merge)

**Optional** (can defer to post-MVP):
- [ ] **P2.2.1** Load test: 1000 concurrent requests
- [ ] **P2.2.2** Security audit: SQL injection, JWT, rate limiting

---

### P3 — Future Enhancements (POST-MVP)

Not tracked for MVP, but documented for reference:

- [ ] **P3.1** Audit logging: All mutations logged with user + timestamp
- [ ] **P3.2** CapEx workflow implementation (currently stubbed)
- [ ] **P3.3** Advanced filtering: Complex queries (e.g., `assetCondition=GOOD AND status=IN_USE`)
- [ ] **P3.4** Webhook support: Notify werkflow when critical data changes
- [ ] **P3.5** Bulk operations: `POST /api/v1/bulk/asset-instances` for large imports
- [ ] **P3.6** Custom fields: Tenant-specific metadata on core entities

---

## Critical Path to MVP

**Current Progress**: P0 + P1.1 + P1.2 + P1.2.5 + P1.4 + P1.5.1 COMPLETE (255 tests)
                     P2.1 COMPLETE (PR #6 submitted)

**Completed**:
- P0 (Multi-tenant, Idempotency, FK validation, API versioning, Pagination) - 6/6
- P1.1 (Error responses, Enum metadata, DTO examples) - 3/3
- P1.2 (Keycloak linking) - 2/2
- P1.2.5 (User Identity Architecture with OIDC) - 6/6
- P1.4 (Number generation & collision prevention) - 2/2
- P1.5.1 (Contract tests for domain services) - 24 tests, 255 total
- P2.1 (Documentation suite with clean structure) - PR #6

**In Progress**:
- P2 Review/Merge (awaiting PR #6 approval)

**Next Sessions**:
1. **PR #6 Merge** (documentation)
2. **P1.5.2** Integration tests (4 hours, @WebMvcTest approach documented)
3. **P2.2** Load testing (optional, 2 hours)
4. **MVP Release Prep**

**Target**: Merge P2.1, implement P1.5.2, complete load testing, ready for MVP release

---

## Related Documents

- `docs/adr/ADR-001-Service-Boundary-Architecture.md` — Architecture decisions
- `docs/adr/ADR-002-User-Identity-And-JWT-Claims.md` — User identity design
- `docs/superpowers/plans/2026-04-08-p1.2-keycloak-linking.md` — Implementation plan
- `docs/superpowers/specs/2026-04-08-p1.2-keycloak-linking-design.md` — Design spec
- `README.md` — Project overview and quick start
- `docs/Architecture-Overview.md` — Architecture overview and business flow diagrams
