# Integration Guide — Werkflow & Werkflow-ERP

How to integrate Werkflow-ERP with the Werkflow orchestration platform.

---

## Overview

**Werkflow-ERP** is designed to be called from Werkflow BPMN workflows. This guide covers:
- Registering Werkflow-ERP as a connector in Werkflow
- Configuring service tasks to call Werkflow-ERP APIs
- Error handling and retry patterns
- Multi-tenancy and authentication

---

## Prerequisites

- werkflow platform running (Engine 8081, Admin 8083, Portal 4000)
- werkflow-erp service running (8084)
- Admin access to werkflow Admin Portal
- JWT token generation (handled automatically by werkflow)

---

## Step 1: Register Connector in werkflow Admin

### Navigate to Connectors

1. Open werkflow Admin Portal: `http://localhost:4000/admin`
2. Go to **Admin → Connectors**
3. Click **Add Connector**

### Fill in Details

| Field | Value |
|-------|-------|
| **Key** | `business-service` |
| **Display Name** | Werkflow ERP Business Service |
| **Endpoint URL** | `http://localhost:8084/api/v1` |
| **Authentication Type** | Bearer Token (JWT) |
| **Timeout (seconds)** | 30 |
| **Retry On Failure** | Yes (3 attempts) |

Click **Save**.

---

## Step 2: Configure BPMN Service Tasks

### Example 1: Create Purchase Request

```xml
<bpmn:process id="procurement_workflow" name="Procurement Workflow">
  <bpmn:startEvent id="StartEvent" name="PR Submitted" />
  <bpmn:serviceTask
    id="Task_CreatePR"
    name="Create Purchase Request"
    camunda:type="http"
    camunda:method="POST"
    camunda:url="http://localhost:8084/api/v1/procurement/purchase-requests">

    <bpmn:extensionElements>
      <camunda:inputOutput>
        <!-- Request Headers -->
        <camunda:inputParameter name="headers">
          <camunda:map>
            <camunda:entry key="Authorization">Bearer ${auth.token}</camunda:entry>
            <camunda:entry key="X-Tenant-ID">${tenantId}</camunda:entry>
            <camunda:entry key="X-Idempotency-Key">${processInstanceId}</camunda:entry>
            <camunda:entry key="Content-Type">application/json</camunda:entry>
          </camunda:map>
        </camunda:inputParameter>

        <!-- Request Payload -->
        <camunda:inputParameter name="payload">
          <camunda:script scriptFormat="JavaScript">
            ({
              requestingDeptId: parseInt(requestingDeptId),
              priority: priority,
              lineItems: [
                {
                  description: itemDescription,
                  quantity: parseInt(quantity),
                  estimatedUnitCost: parseFloat(estimatedCost)
                }
              ],
              justification: justification
            })
          </camunda:script>
        </camunda:inputParameter>

        <!-- Response Handling -->
        <camunda:outputParameter name="prId">
          ${response.id}
        </camunda:outputParameter>
        <camunda:outputParameter name="prNumber">
          ${response.requestNumber}
        </camunda:outputParameter>
        <camunda:outputParameter name="prStatus">
          ${response.status}
        </camunda:outputParameter>
      </camunda:inputOutput>
    </bpmn:extensionElements>
  </bpmn:serviceTask>

  <!-- Continue workflow based on response -->
  <bpmn:sequenceFlow id="flow1" sourceRef="StartEvent" targetRef="Task_CreatePR" />
</bpmn:process>
```

### Example 2: Check Budget & Create Expense

```xml
<bpmn:serviceTask
  id="Task_CheckBudget"
  name="Check Budget Availability"
  camunda:type="http"
  camunda:method="POST"
  camunda:url="http://localhost:8084/api/v1/finance/budget-check">

  <bpmn:extensionElements>
    <camunda:inputOutput>
      <camunda:inputParameter name="headers">
        <camunda:map>
          <camunda:entry key="Authorization">Bearer ${auth.token}</camunda:entry>
          <camunda:entry key="X-Tenant-ID">${tenantId}</camunda:entry>
          <camunda:entry key="Content-Type">application/json</camunda:entry>
        </camunda:map>
      </camunda:inputParameter>

      <camunda:inputParameter name="payload">
        <camunda:script scriptFormat="JavaScript">
          ({
            departmentId: parseInt(departmentId),
            amount: parseFloat(expenseAmount),
            fiscalYear: parseInt(fiscalYear)
          })
        </camunda:script>
      </camunda:inputParameter>

      <camunda:outputParameter name="budgetAvailable">
        ${response.available}
      </camunda:outputParameter>
      <camunda:outputParameter name="availableAmount">
        ${response.availableAmount}
      </camunda:outputParameter>
    </camunda:inputOutput>
  </bpmn:extensionElements>
</bpmn:serviceTask>

<!-- If budget available, create expense -->
<bpmn:exclusiveGateway id="Gateway_BudgetCheck" name="Budget Available?" />

<bpmn:serviceTask
  id="Task_CreateExpense"
  name="Create Expense"
  camunda:type="http"
  camunda:method="POST"
  camunda:url="http://localhost:8084/api/v1/finance/expenses">

  <bpmn:extensionElements>
    <camunda:inputOutput>
      <camunda:inputParameter name="headers">
        <camunda:map>
          <camunda:entry key="Authorization">Bearer ${auth.token}</camunda:entry>
          <camunda:entry key="X-Tenant-ID">${tenantId}</camunda:entry>
          <camunda:entry key="X-Idempotency-Key">${processInstanceId}</camunda:entry>
          <camunda:entry key="Content-Type">application/json</camunda:entry>
        </camunda:map>
      </camunda:inputParameter>

      <camunda:inputParameter name="payload">
        <camunda:script scriptFormat="JavaScript">
          ({
            departmentId: parseInt(departmentId),
            budgetCategoryId: parseInt(budgetCategoryId),
            description: expenseDescription,
            amount: parseFloat(expenseAmount),
            invoiceDate: invoiceDate,
            paymentDueDate: paymentDueDate
          })
        </camunda:script>
      </camunda:inputParameter>

      <camunda:outputParameter name="expenseId">
        ${response.id}
      </camunda:outputParameter>
    </camunda:inputOutput>
  </bpmn:extensionElements>
</bpmn:serviceTask>

<bpmn:sequenceFlow
  id="flow_yes"
  sourceRef="Gateway_BudgetCheck"
  targetRef="Task_CreateExpense"
  conditionExpression="${budgetAvailable == true}" />

<bpmn:sequenceFlow
  id="flow_no"
  sourceRef="Gateway_BudgetCheck"
  targetRef="Task_RejectExpense"
  conditionExpression="${budgetAvailable == false}" />
```

### Example 3: Process Asset Request Callbacks

```xml
<!-- After Manager Approval -->
<bpmn:serviceTask
  id="Task_ApproveAssetRequest"
  name="Approve Asset Request"
  camunda:type="http"
  camunda:method="POST"
  camunda:url="http://localhost:8084/api/v1/inventory/asset-requests/callback/approve">

  <bpmn:extensionElements>
    <camunda:inputOutput>
      <camunda:inputParameter name="headers">
        <camunda:map>
          <camunda:entry key="Authorization">Bearer ${auth.token}</camunda:entry>
          <camunda:entry key="X-Tenant-ID">${tenantId}</camunda:entry>
          <camunda:entry key="Content-Type">application/json</camunda:entry>
        </camunda:map>
      </camunda:inputParameter>

      <camunda:inputParameter name="payload">
        <camunda:script scriptFormat="JavaScript">
          ({
            assetRequestId: parseInt(assetRequestId),
            approverUserId: currentUser.email
          })
        </camunda:script>
      </camunda:inputParameter>

      <camunda:outputParameter name="assetStatus">
        ${response.status}
      </camunda:outputParameter>
    </camunda:inputOutput>
  </bpmn:extensionElements>
</bpmn:serviceTask>

<!-- If Procurement Needed -->
<bpmn:serviceTask
  id="Task_TriggerProcurement"
  name="Trigger Procurement"
  camunda:type="http"
  camunda:method="POST"
  camunda:url="http://localhost:8084/api/v1/inventory/asset-requests/callback/procurement">

  <bpmn:extensionElements>
    <camunda:inputOutput>
      <camunda:inputParameter name="headers">
        <camunda:map>
          <camunda:entry key="Authorization">Bearer ${auth.token}</camunda:entry>
          <camunda:entry key="X-Tenant-ID">${tenantId}</camunda:entry>
          <camunda:entry key="Content-Type">application/json</camunda:entry>
        </camunda:map>
      </camunda:inputParameter>

      <camunda:inputParameter name="payload">
        <camunda:script scriptFormat="JavaScript">
          ({
            assetRequestId: parseInt(assetRequestId),
            vendorId: parseInt(vendorId),
            expectedDeliveryDays: 14
          })
        </camunda:script>
      </camunda:inputParameter>
    </camunda:inputOutput>
  </bpmn:extensionElements>
</bpmn:serviceTask>
```

---

## Step 3: Handle Errors and Retries

### Automatic Retries

werkflow automatically retries failed calls (3 attempts, exponential backoff). These fail:
- Network timeouts (> 30 seconds)
- HTTP 5xx errors (server errors)
- Connection refused

These do NOT retry:
- HTTP 400 Bad Request (validation error, won't be fixed by retrying)
- HTTP 401 Unauthorized (auth issue)
- HTTP 403 Forbidden (permission issue)

### Error Handling in BPMN

```xml
<bpmn:serviceTask id="Task_CreateAsset">
  <!-- ... task config ... -->
</bpmn:serviceTask>

<!-- Catch validation errors -->
<bpmn:boundaryEvent id="BoundaryEvent_ValidationError"
  attachedToRef="Task_CreateAsset"
  camunda:errorCodeVariable="errorCode">
  <bpmn:errorEventDefinition />
</bpmn:boundaryEvent>

<bpmn:sequenceFlow
  sourceRef="BoundaryEvent_ValidationError"
  targetRef="Task_LogError" />

<bpmn:serviceTask id="Task_LogError" name="Log Validation Error">
  <bpmn:extensionElements>
    <camunda:inputOutput>
      <camunda:inputParameter name="logMessage">
        Asset creation failed: ${errorCode}
      </camunda:inputParameter>
    </camunda:inputOutput>
  </bpmn:extensionElements>
</bpmn:serviceTask>
```

---

## Step 4: Multi-Tenancy & Authentication

### Tenant ID Resolution

werkflow-erp requires `X-Tenant-ID` in every request. Set it from:

1. **Process Variable** (recommended):
```xml
<camunda:entry key="X-Tenant-ID">${tenantId}</camunda:entry>
```

2. **User Context**:
```xml
<camunda:entry key="X-Tenant-ID">${currentUser.organization}</camunda:entry>
```

### JWT Token

werkflow automatically includes a JWT token with:
- `sub` — User ID
- `roles` — User's roles
- `organization_id` — Tenant ID
- `exp` — Expiration time

**Do NOT hardcode tokens.** Use `${auth.token}` variable:

```xml
<camunda:entry key="Authorization">Bearer ${auth.token}</camunda:entry>
```

### Idempotency Keys

Always use `X-Idempotency-Key` to prevent duplicate resource creation:

```xml
<camunda:entry key="X-Idempotency-Key">${processInstanceId}</camunda:entry>
```

This ensures:
- First request → 201 Created, resource created
- Retry with same key → 200 OK, cached response (no duplicate)

---

## Step 5: Common Workflows

### Workflow 1: Simple Purchase Order

```
1. User submits PR form
2. Manager approves (human task)
3. System creates Purchase Request in werkflow-erp
4. Finance reviews budget
5. If approved: Create Purchase Order
6. If rejected: Reject PR
```

### Workflow 2: Asset Request with Procurement

```
1. Employee requests asset
2. Manager approves
3. System checks asset availability
   - If in stock: Create custody record
   - If not: Create purchase request for procurement
4. Procurement creates PO and receives goods
5. System creates asset instance
6. System assigns to employee
```

### Workflow 3: Expense Approval with Budget Check

```
1. Employee submits expense
2. System checks budget availability
   - If available: Create expense, route to manager
   - If not available: Auto-reject
3. Manager approves/rejects
4. Finance records payment
```

---

## Configuration Best Practices

### 1. Error Handling

Always plan for failures:

```xml
<bpmn:serviceTask id="Task_Create" ... />

<bpmn:boundaryEvent id="Event_Timeout"
  attachedToRef="Task_Create"
  timerEventDefinition>
  <bpmn:timerEventDefinition>
    <bpmn:timeDuration>PT30S</bpmn:timeDuration>
  </bpmn:timerEventDefinition>
</bpmn:boundaryEvent>

<bpmn:sequenceFlow
  sourceRef="Event_Timeout"
  targetRef="Task_NotifyAdmin" />
```

### 2. Data Validation

Validate before calling werkflow-erp:

```xml
<bpmn:exclusiveGateway id="Gateway_ValidateInput">
  <bpmn:conditionExpression>
    ${quantity > 0 AND departmentId > 0}
  </bpmn:conditionExpression>
</bpmn:exclusiveGateway>
```

### 3. Logging

Use logging to debug integrations:

```xml
<camunda:executionListener event="start">
  <camunda:script scriptFormat="JavaScript">
    console.log('Creating PR with amount: ' + amount);
  </camunda:script>
</camunda:executionListener>
```

### 4. Timeouts

Set reasonable timeouts based on operation:

```xml
<camunda:property name="camunda:asyncAfter" value="true" />
<camunda:property name="camunda:timeout" value="PT1M" />
```

---

## Testing Integration

### Manual Test

```bash
# 1. Start werkflow-erp
cd werkflow-erp
mvn spring-boot:run

# 2. Get JWT token
TOKEN=$(curl -s http://localhost:8090/realms/werkflow/protocol/openid-connect/token \
  -d "grant_type=client_credentials&client_id=werkflow-api&client_secret=secret" \
  | jq -r '.access_token')

# 3. Test API call
curl -X POST http://localhost:8084/api/v1/hr/employees \
  -H "Authorization: Bearer $TOKEN" \
  -H "X-Tenant-ID: test-tenant" \
  -H "Content-Type: application/json" \
  -d '{
    "firstName": "Test",
    "lastName": "User",
    "email": "test@example.com",
    "departmentId": 1,
    "status": "ACTIVE",
    "joinDate": "2026-04-10"
  }'
```

### BPMN Test

Deploy a test workflow:

1. Open werkflow Designer: `http://localhost:4000/designer`
2. Create simple test workflow
3. Use Test API Connector task
4. Deploy and execute
5. Check werkflow-erp logs for requests/responses

---

## Troubleshooting

### 401 Unauthorized

**Cause**: JWT token missing or expired

**Solution**: Ensure `${auth.token}` is used, not hardcoded token. Token is automatically refreshed by werkflow.

### 403 Forbidden - Cross-tenant Access

**Cause**: Tenant ID in header doesn't match JWT claim

**Solution**: Verify `X-Tenant-ID` matches the user's organization from JWT.

### 409 Conflict

**Cause**: Foreign key constraint violated

**Solution**: Ensure all referenced IDs (departmentId, vendorId, etc.) exist in the target tenant.

### 400 Bad Request

**Cause**: Validation error (invalid enum, required field missing, etc.)

**Solution**: Check error details in response. Validate data before calling werkflow-erp.

### Connection Refused

**Cause**: werkflow-erp not running or wrong URL

**Solution**:
```bash
# Check if running
curl http://localhost:8084/api/v1/actuator/health

# Or verify connector URL in Admin Portal
```

---

## Related Documents

- **[API Usage Guide](./API-USAGE-GUIDE.md)** — Complete API examples for all domains
- **[README](../README.md)** — Project overview
- **[Architecture Decision](./ADR-001-Service-Boundary-Architecture.md)** — Design rationale
