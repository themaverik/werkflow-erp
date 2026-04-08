# ADR-002: API Contract Standardization

**Status**: Accepted
**Date**: 2026-04-08
**Authors**: Architecture Review
**Phase**: P1.1 — Quality & Integration

---

## Context

werkflow-erp exposes REST APIs across 4 business domains (HR, Finance, Procurement, Inventory) with 20+ entities and 50+ endpoints. As werkflow integrates with werkflow-erp, BPMN form builders need:

1. **Enum metadata** (what values are valid for dropdowns, radio buttons, etc.)
2. **DTO examples** (what does a successful request/response look like?)
3. **Standardized error responses** (how to handle failures consistently)

Currently:
- ❌ No enum metadata endpoint (BPMN designers must hardcode enum values)
- ❌ No `@Schema` examples on DTOs (developers guess field types, formats)
- ❌ No standardized error format (some endpoints return 404, others 400)

This ADR establishes the contract patterns for API clarity and BPMN integration.

---

## Decision: Centralized Enum Metadata Endpoint

### Pattern: Single Source of Enum Truth

**Endpoint**: `GET /api/v1/meta/enums`

**Response**: Flat list of all enums with labels and descriptions
```json
{
  "enums": [
    {
      "name": "PrStatus",
      "description": "Purchase request lifecycle status",
      "values": [
        {
          "value": "DRAFT",
          "label": "Draft",
          "description": "Initial state, not yet submitted"
        },
        {
          "value": "PENDING_APPROVAL",
          "label": "Pending Approval",
          "description": "Awaiting manager approval"
        }
        // ... more values
      ]
    },
    // ... more enums
  ]
}
```

**Enums to Expose** (by domain):
- **HR**: EmployeeStatus, LeaveType, AttendanceStatus, PerformanceRating
- **Finance**: BudgetStatus, ExpenseStatus, ApprovalThresholdType
- **Procurement**: PrStatus, PoStatus, ReceiptStatus, VendorStatus
- **Inventory**: AssetRequestStatus, AssetCondition, AssetStatus, TransferStatus, MaintenanceType

### Why Centralized?

1. **BPMN Discovery**: werkflow designers call once at design-time, cache the response
2. **Single Source of Truth**: All enum definitions in one place (Java enums + metadata endpoint)
3. **Extensible**: Easy to add new enums or properties (labels in future languages, icons, etc.)
4. **REST Convention**: Metadata is a resource like any other (discoverable, cacheable)

### Implementation Details

- **Lazy Loading**: Build enum metadata at startup, cache in memory
- **No Authentication**: Metadata endpoint is public (design-time discovery)
- **Versioned**: Lives under `/api/v1/meta/` for future `/api/v2/meta/` support

---

## Decision: Complete DTO Examples via @Schema

### Pattern: Embedded JSON Examples

Add `@Schema(example="...")` to every Request and Response DTO with **complete, realistic data**.

**Example for PurchaseRequestRequest**:
```java
@Data
@Builder
@Schema(example = """
{
  "requestingDeptId": 1,
  "requesterUserId": "user@example.com",
  "requestDate": "2026-04-08",
  "requiredByDate": "2026-04-15",
  "priority": "HIGH",
  "justification": "Office supplies for Q2",
  "notes": "Urgent",
  "processInstanceId": "bpmn-proc-123",
  "lineItems": [
    {
      "lineNumber": 1,
      "description": "Ergonomic chair",
      "quantity": 5,
      "unitOfMeasure": "UNIT",
      "estimatedUnitPrice": 250.00,
      "budgetCategoryId": 10
    }
  ]
}
""")
public class PurchaseRequestRequest {
  // fields...
}
```

### Coverage

- **All Request DTOs**: Full nested structure (e.g., PR request with line items)
- **All Response DTOs**: All fields populated with realistic values
- **Across 4 domains**: HR, Finance, Procurement, Inventory

### Why Complete Examples?

1. **BPMN Form Builders**: See full payload structure for mapping
2. **API Clients**: Copy-paste ready for testing
3. **Documentation**: Swagger/OpenAPI renders live examples
4. **Field Types**: Developers see date formats, number ranges, nested objects

---

## Decision: Standardized Error Response Format

### Pattern: Consistent Error Structure

All 4xx/5xx errors return:
```json
{
  "code": "ERROR_CODE",
  "message": "Human-readable error message",
  "timestamp": "2026-04-08T10:30:00Z",
  "details": {
    "fieldErrors": [
      {
        "field": "requestingDeptId",
        "message": "Department not found"
      }
    ],
    "path": "/api/v1/purchase-requests"
  }
}
```

**HTTP Status Codes** (unchanged):
- `400 Bad Request` — Validation failure (required field missing, invalid format)
- `404 Not Found` — Entity not found (PR with ID 999 doesn't exist)
- `409 Conflict` — Idempotency conflict (same key, different payload)
- `500 Internal Server Error` — Unexpected error (database down, sequence creation failed)

### Error Code Convention

Error codes follow industry standard: `{ENTITY}_{PROBLEM}`

**Examples**:
- `DEPARTMENT_NOT_FOUND` — Department referenced in FK doesn't exist
- `BUDGET_INSUFFICIENT` — Budget check failed
- `SEQUENCE_CREATION_FAILED` — Database sequence error
- `VALIDATION_FAILED` — Generic validation error

**Extensibility**: Error codes can be added anytime (they're just strings in the response). No breaking changes.

### Implementation

- **Global `@RestControllerAdvice`** intercepts all exceptions
- Maps standard exceptions → `ErrorResponse`
- All controllers return consistent format automatically

---

## Decision: Provision for Future Error Codes

### Pattern: Extensible Without Breaking Changes

Today (P1.1):
```json
{
  "code": "DEPARTMENT_NOT_FOUND",
  "message": "Department with ID 1 not found",
  "timestamp": "...",
  "details": {}
}
```

Tomorrow (P1.X, when needed):
```json
{
  "code": "DEPARTMENT_NOT_FOUND",
  "message": "Department with ID 1 not found",
  "timestamp": "...",
  "details": {
    "entityType": "Department",
    "entityId": 1,
    "reason": "DELETED"
  }
}
```

**Why Extensible?**

1. **No API Breaking**: Clients ignore unknown fields
2. **Gradual Rollout**: Add error codes incrementally
3. **Industry Standards**: Aligns with OAuth 2.0, OpenAPI patterns

---

## Consequences

### Positive

- **Clarity**: BPMN designers see all valid enum values immediately
- **Integration**: werkflow can build forms dynamically from metadata
- **Documentation**: Examples render in Swagger UI (developers see realistic data)
- **Consistency**: All endpoints return errors the same way
- **Extensibility**: Error codes added later without breaking clients

### Tradeoffs

- **Metadata Endpoint**: Another endpoint to maintain (but it's read-only, immutable)
- **Examples Maintenance**: Need to keep DTO examples in sync with schema (tooling can help)
- **Error Code Strategy**: Needs naming convention, team discipline (but flexible)

### Risks and Mitigations

| Risk | Mitigation |
|------|-----------|
| Enum metadata gets out of sync with code | Document generation from Java enums (can be automated) |
| DTO examples become stale | Code review checklist (change example when DTO changes) |
| Error codes inconsistent across endpoints | Naming convention + central ErrorResponse class |
| BPMN caches old enum metadata | HTTP Cache-Control headers (clients can set TTL) |

---

## Related Documents

- **ROADMAP.md**: P1.1 phase execution tracking
- **ADR-001**: Service Boundary Architecture (parent decision)
- **ADR-003 (TBD)**: Error handling and retry policies (complements this decision)

---

## Implementation Checklist (P1.1)

- [ ] Create `GET /api/v1/meta/enums` endpoint returning all enums
- [ ] Add `@Schema(example="...")` to all Request/Response DTOs
- [ ] Create global `@RestControllerAdvice` for standardized error handling
- [ ] Create `ErrorResponse` DTO with code, message, timestamp, details
- [ ] Map common exceptions (EntityNotFoundException, ValidationException, etc.) to ErrorResponse
- [ ] Document enum metadata structure in README
- [ ] Add error response examples to Swagger/OpenAPI
- [ ] Test: Verify enum endpoint returns all 15+ enums with labels
- [ ] Test: Verify all 404/400 errors use standardized format
