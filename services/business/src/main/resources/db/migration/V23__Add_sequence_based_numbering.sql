-- V23__Add_sequence_based_numbering.sql
-- Adds sequence-based numbering for PR/PO/GRN documents
-- Format: {PREFIX}-{TENANT_ID}-{TIMESTAMP_MS}-{SEQ:05d}
-- Example: PR-ACME-1712345678901-00042

-- 1. Create helper function for sequence creation
-- Used by Java application code via executeQuery
CREATE OR REPLACE FUNCTION create_tenant_sequence(p_tenant_id VARCHAR, p_doc_type VARCHAR)
RETURNS VOID AS $$
DECLARE
  v_seq_name VARCHAR;
  v_tenant_upper VARCHAR;
BEGIN
  v_tenant_upper := UPPER(p_tenant_id);
  v_seq_name := p_doc_type || '_seq_' || v_tenant_upper;
  EXECUTE format('CREATE SEQUENCE IF NOT EXISTS %I START WITH 1', v_seq_name);
END;
$$ LANGUAGE plpgsql;

-- 2. Backfill existing PurchaseRequests with new format
-- Only updates records not already in new format (PR-*-*-*-*)
WITH numbered_prs AS (
  SELECT id, tenant_id,
         ROW_NUMBER() OVER (PARTITION BY tenant_id ORDER BY id) AS seq_num
  FROM procurement_service.purchase_requests
  WHERE pr_number NOT LIKE 'PR-%-%-%--%'
)
UPDATE procurement_service.purchase_requests pr
SET pr_number = 'PR-' || UPPER(np.tenant_id) || '-' || (EXTRACT(EPOCH FROM NOW()) * 1000)::BIGINT || '-' || LPAD(np.seq_num::TEXT, 5, '0')
FROM numbered_prs np
WHERE pr.id = np.id;

-- 3. Backfill existing PurchaseOrders with new format
WITH numbered_pos AS (
  SELECT id, tenant_id,
         ROW_NUMBER() OVER (PARTITION BY tenant_id ORDER BY id) AS seq_num
  FROM procurement_service.purchase_orders
  WHERE po_number NOT LIKE 'PO-%-%-%--%'
)
UPDATE procurement_service.purchase_orders po
SET po_number = 'PO-' || UPPER(np.tenant_id) || '-' || (EXTRACT(EPOCH FROM NOW()) * 1000)::BIGINT || '-' || LPAD(np.seq_num::TEXT, 5, '0')
FROM numbered_pos np
WHERE po.id = np.id;

-- 4. Backfill existing Receipts with new format
WITH numbered_receipts AS (
  SELECT id, tenant_id,
         ROW_NUMBER() OVER (PARTITION BY tenant_id ORDER BY id) AS seq_num
  FROM procurement_service.receipts
  WHERE receipt_number NOT LIKE 'GRN-%-%-%--%'
)
UPDATE procurement_service.receipts r
SET receipt_number = 'GRN-' || UPPER(np.tenant_id) || '-' || (EXTRACT(EPOCH FROM NOW()) * 1000)::BIGINT || '-' || LPAD(np.seq_num::TEXT, 5, '0')
FROM numbered_receipts np
WHERE r.id = np.id;
