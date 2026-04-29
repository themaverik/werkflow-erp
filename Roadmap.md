# Werkflow ERP — Roadmap

**Project**: Standalone ERP Data Service — HR, Finance, Procurement, Inventory
**Master Roadmap**: `~/Projects/werkflow-platform/docs/Roadmap.md` (authoritative for all future tasks)
**Last Updated**: 2026-04-29
**Target**: Internal Enterprise Demo — June 2026

> Future tasks in this file are synced from the master Roadmap (M1 + M7 ERP share).

---

## Current Session State

**Active Phase**: M1 — ERP Enterprise APIs
**Current Task**: P1.5.2 (integration tests — deferred, solution documented) → P1.6.1 (next)
**Branch**: feature/p1.5-test-suite
**Test Status**: 255 tests passing, 0 failures
**Note**: P1.5.2 architectural issue documented in `docs/P1.5.2-INTEGRATION-TESTS-IMPLEMENTATION-NOTES.md` — use `@WebMvcTest + MockMvc` approach

---

## Phase Summary

| Phase | Status |
|-------|--------|
| P0 — Critical Path | ✅ COMPLETE |
| P1.1 — API Standardisation | ✅ COMPLETE |
| P1.2 — HR/Keycloak Linking | ✅ COMPLETE |
| P1.2.5 — User Identity (OIDC) | ✅ COMPLETE — 231 tests |
| P1.3 — User Name Enrichment | ⏸️ DEFERRED — superseded by P1.2.5 |
| P1.4 — Number Generation | ✅ COMPLETE |
| P1.5.1 — Contract Tests | ✅ COMPLETE — 255 total tests |
| P1.5.2 — Integration Tests | ⏳ PENDING — blocked on TenantContext; solution documented |
| M1 — Enterprise Integration APIs | ⏳ PENDING — P1.6.1–P1.6.3 |
| M7 — CI/CD (ERP share) | ⏳ PENDING |
| P2 — Documentation | ⏳ PENDING — deferred until after June demo |
| P3 — Future Enhancements | 🔮 POST-MVP |

---

## Active: M1 — ERP Enterprise Integration APIs

**Deps**: none
**Estimate**: 8–10 hours
**Required by**: werkflow-enterprise M3 (Groups 2–3 cannot wire ERP data without these)

- [ ] **P1.5.2** Integration tests — `@WebMvcTest + MockMvc` approach; spec in `docs/P1.5.2-INTEGRATION-TESTS-SPEC.md` (4h)

- [ ] **P1.6.1** Extend `users` table + profile endpoint
  - Add columns: `department_code`, `employee_id`, `cost_center`, `is_poc` to `users` table
  - Flyway V25 migration
  - New endpoint: `GET /api/v1/users/{keycloakId}/profile`
  - Required by ADR-003 (Keycloak semantic roles) + ADR-005 (department-scoped routing)
  - Estimate: 3h

- [ ] **P1.6.2** CustodyMapping entity + API
  - Move `CustodyMapping` from werkflow-enterprise admin-service to werkflow-erp (ADR-004)
  - Entity: `custody_owner (VARCHAR), candidate_groups (TEXT[]), tenant_id`
  - Endpoints: `GET/POST/PUT/DELETE /api/v1/custody-mappings`
  - Tenant-scoped, paginated, idempotent upsert
  - Required by ADR-004
  - Estimate: 3h

- [ ] **P1.6.3** Department API verification + user resolution endpoint
  - Verify `GET /api/v1/departments` returns `deptCode` correctly
  - Add `GET /api/v1/departments/{deptCode}/members` (for engine candidateGroup resolution)
  - Required by ADR-005
  - Estimate: 2–3h

---

## M7 — CI/CD (ERP Share)

**Deps**: none hard; slot alongside M4–M6 in enterprise
**Estimate**: 2–3 hours

- [ ] CI (`ci.yml`): trigger on PR + push to main; job: Maven verify (all tests must pass)
- [ ] Release (`release.yml`): trigger on tag `v*`; build + push to `ghcr.io`

---

## Deferred — P2 Documentation

**Status**: Deferred until after June enterprise demo
**Reason**: No external consumers yet; internal demo does not require public docs

- [ ] **P2.1.1** Update README.md (quick start, multi-tenancy setup, JWT auth)
- [ ] **P2.1.2** API usage guide (step-by-step workflow examples)
- [ ] **P2.1.3** Integration guide for werkflow (connector registration, config)
- [ ] **P2.2.1** Load test: 1000 concurrent requests (optional pre-demo)
- [ ] **P2.2.2** Security audit: SQL injection, JWT, rate limiting (optional pre-demo)

---

## Deferred — P3 Future Enhancements

Not tracked for MVP.

- [ ] **P3.1** Audit logging: all mutations logged with user + timestamp
- [ ] **P3.2** CapEx workflow implementation (currently stubbed)
- [ ] **P3.3** Advanced filtering: complex queries (`assetCondition=GOOD AND status=IN_USE`)
- [ ] **P3.4** Webhook support: notify werkflow on critical data changes
- [ ] **P3.5** Bulk operations: `POST /api/v1/bulk/asset-instances`
- [ ] **P3.6** Custom fields: tenant-specific metadata on core entities

---

## Historical Summary — Completed (P0–P1.5.1)

| Phase | Highlights | Tests |
|-------|-----------|-------|
| P0 | Multi-tenant isolation (23 entities), idempotency, processInstanceId, FK validation, API versioning, pagination | 40 |
| P1.1 | Error responses (GlobalExceptionHandler), enum metadata endpoint (15 enums), DTO examples | 118 |
| P1.2 | Keycloak link endpoint (PATCH idempotent, tenant-scoped, conflict detection) | 131 |
| P1.2.5 | OIDC user identity — UserInfoResolver (Caffeine cache), UserContext/Filter, OidcRoleConverter, 13 audit DTOs with display names, GDPR compliant | 231 |
| P1.4 | PR/PO/GRN number sequences (DB-level, `{type}-{tenant}-{year}-{seq:05d}`) | 231 |
| P1.5.1 | 24 contract tests across HR, Finance, Procurement, Inventory domain services | 255 |

---

## Related Documents

- `docs/ADR-001-Service-Boundary-Architecture.md`
- `docs/ADR-002-User-Identity-And-JWT-Claims.md`
- `docs/P1.5.2-INTEGRATION-TESTS-SPEC.md`
- `docs/P1.5.2-INTEGRATION-TESTS-IMPLEMENTATION-NOTES.md`
