# Werkflow Integration Guide

## ProcessInstanceId Pattern

### Pattern: Generate First, Then Create

The recommended pattern for werkflow is to generate `processInstanceId` in the workflow engine **before** making the API call to create an asset request, purchase request, or purchase order.

**Benefits:**
- Single API call — no need for subsequent updates
- Immediate linking of business entity to workflow instance
- Eliminates race conditions between API creation and workflow callback

#### Asset Request Example

```bash
# 1. Workflow engine generates processInstanceId
PROCESS_ID="arn:aws:bpm:us-east-1:123456789:process/asset-approval/2026-04-07-12345"

# 2. Call POST /api/v1/inventory/asset-requests with processInstanceId
curl -X POST http://localhost:8080/api/v1/inventory/asset-requests \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $JWT_TOKEN" \
  -H "Idempotency-Key: $(uuidgen)" \
  -d '{
    "requesterUserId": "user123",
    "requesterName": "John Doe",
    "requesterEmail": "john@acme.com",
    "officeLocation": "NEW_YORK",
    "assetCategoryId": 42,
    "quantity": 1,
    "processInstanceId": "'$PROCESS_ID'"
  }'

# 3. Asset request created with processInstanceId set
# Response includes id (e.g., 789) and processInstanceId
```

#### Purchase Request Example

```bash
# Workflow engine generates processInstanceId
PROCESS_ID="arn:aws:bpm:us-east-1:123456789:process/pr-approval/2026-04-07-54321"

# Call POST /api/v1/procurement/purchase-requests with processInstanceId
curl -X POST http://localhost:8080/api/v1/procurement/purchase-requests \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $JWT_TOKEN" \
  -H "Idempotency-Key: $(uuidgen)" \
  -d '{
    "requesterUserId": "user123",
    "vendorId": 100,
    "lineItems": [...],
    "processInstanceId": "'$PROCESS_ID'"
  }'

# Asset request created with processInstanceId set
```

### Fallback Pattern: Create First, Then Update

If the workflow engine cannot generate `processInstanceId` before API creation, use the fallback:

1. Create the request without `processInstanceId` (optional field)
2. After workflow instance is created, update using the dedicated endpoint

#### Asset Request Fallback

```bash
# 1. Create asset request without processInstanceId
RESPONSE=$(curl -X POST http://localhost:8080/api/v1/inventory/asset-requests \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $JWT_TOKEN" \
  -H "Idempotency-Key: $(uuidgen)" \
  -d '{...}')

ASSET_REQUEST_ID=$(echo $RESPONSE | jq -r '.id')
echo "Created asset request: $ASSET_REQUEST_ID"

# 2. Workflow engine processes and generates processInstanceId
PROCESS_ID="arn:aws:bpm:us-east-1:123456789:process/asset-approval/2026-04-07-12345"

# 3. Update asset request with processInstanceId
curl -X PATCH http://localhost:8080/api/v1/inventory/asset-requests/$ASSET_REQUEST_ID/process-instance \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -H "Authorization: Bearer $JWT_TOKEN" \
  -d "processInstanceId=$PROCESS_ID"

echo "Linked asset request $ASSET_REQUEST_ID to process $PROCESS_ID"
```

**Fallback applies to:**
- Asset Requests: `PATCH /api/v1/inventory/asset-requests/{id}/process-instance?processInstanceId=...`
- Purchase Requests: Similar endpoint (to be implemented)
- Purchase Orders: Similar endpoint (to be implemented)

## API Endpoints

### Asset Request Lifecycle

```
POST   /api/v1/inventory/asset-requests                 Create (processInstanceId optional)
GET    /api/v1/inventory/asset-requests/{id}            Retrieve
PATCH  /api/v1/inventory/asset-requests/{id}/process-instance   Update processInstanceId
POST   /api/v1/inventory/asset-requests/{id}/approve    Approve (via workflow callback)
POST   /api/v1/inventory/asset-requests/{id}/reject     Reject (via workflow callback)
```

### Callback Endpoints

Workflow engine calls these after approval/rejection decisions:

```
POST   /api/v1/inventory/asset-requests/callback/approve     Body: { processInstanceId, approvedByUserId }
POST   /api/v1/inventory/asset-requests/callback/reject      Body: { processInstanceId, approvedByUserId, reason }
POST   /api/v1/inventory/asset-requests/callback/procurement Initiate procurement after approval
```

## Tenant Isolation

All requests are scoped to the tenant extracted from JWT claims (`organization_id` claim or `X-Tenant-ID` header).

Example JWT claim:
```json
{
  "sub": "user123",
  "organization_id": "acme-corp",
  "iat": 1712506800
}
```

## Idempotency

Use the `Idempotency-Key` header for all POST/PUT requests. If the same key is used twice, the cached response is returned without re-executing the operation.

Example:
```bash
curl -X POST http://localhost:8080/api/v1/inventory/asset-requests \
  -H "Idempotency-Key: 550e8400-e29b-41d4-a716-446655440000" \
  -d '{...}'

# Second call with same key returns the same response (200 OK)
curl -X POST http://localhost:8080/api/v1/inventory/asset-requests \
  -H "Idempotency-Key: 550e8400-e29b-41d4-a716-446655440000" \
  -d '{...}'
```

## Development

### Local Testing

Start the service in Docker:

```bash
docker-compose up --build
```

Test asset request creation with processInstanceId:

```bash
curl -X POST http://localhost:8080/api/v1/inventory/asset-requests \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer eyJhbGc..." \
  -H "Idempotency-Key: test-key-1" \
  -d '{
    "requesterUserId": "test-user",
    "requesterName": "Test User",
    "requesterEmail": "test@example.com",
    "officeLocation": "NEW_YORK",
    "assetCategoryId": 1,
    "quantity": 1,
    "processInstanceId": "workflow-123"
  }'
```

Expected response:
```json
{
  "id": 1,
  "processInstanceId": "workflow-123",
  "requesterUserId": "test-user",
  "status": "PENDING",
  "createdAt": "2026-04-07T16:13:00Z"
}
```
