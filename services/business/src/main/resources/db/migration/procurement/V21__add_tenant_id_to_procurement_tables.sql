-- Flyway Migration V21: Add tenant_id column to Procurement domain tables
-- Adds multi-tenant isolation support by adding tenant_id (NOT NULL) to all Procurement entities

-- Add tenant_id to vendors table
ALTER TABLE procurement_service.vendors
ADD COLUMN IF NOT EXISTS tenant_id VARCHAR(255) NOT NULL DEFAULT 'default-tenant';

-- Add tenant_id to purchase_requests table
ALTER TABLE procurement_service.purchase_requests
ADD COLUMN IF NOT EXISTS tenant_id VARCHAR(255) NOT NULL DEFAULT 'default-tenant';

-- Add tenant_id to pr_line_items table
ALTER TABLE procurement_service.pr_line_items
ADD COLUMN IF NOT EXISTS tenant_id VARCHAR(255) NOT NULL DEFAULT 'default-tenant';

-- Add tenant_id to purchase_orders table
ALTER TABLE procurement_service.purchase_orders
ADD COLUMN IF NOT EXISTS tenant_id VARCHAR(255) NOT NULL DEFAULT 'default-tenant';

-- Add tenant_id to po_line_items table
ALTER TABLE procurement_service.po_line_items
ADD COLUMN IF NOT EXISTS tenant_id VARCHAR(255) NOT NULL DEFAULT 'default-tenant';

-- Add tenant_id to receipts table
ALTER TABLE procurement_service.receipts
ADD COLUMN IF NOT EXISTS tenant_id VARCHAR(255) NOT NULL DEFAULT 'default-tenant';

-- Add tenant_id to receipt_line_items table
ALTER TABLE procurement_service.receipt_line_items
ADD COLUMN IF NOT EXISTS tenant_id VARCHAR(255) NOT NULL DEFAULT 'default-tenant';

-- Create indexes on tenant_id for better query performance
CREATE INDEX IF NOT EXISTS idx_vendors_tenant_id ON procurement_service.vendors(tenant_id);
CREATE INDEX IF NOT EXISTS idx_purchase_requests_tenant_id ON procurement_service.purchase_requests(tenant_id);
CREATE INDEX IF NOT EXISTS idx_pr_line_items_tenant_id ON procurement_service.pr_line_items(tenant_id);
CREATE INDEX IF NOT EXISTS idx_purchase_orders_tenant_id ON procurement_service.purchase_orders(tenant_id);
CREATE INDEX IF NOT EXISTS idx_po_line_items_tenant_id ON procurement_service.po_line_items(tenant_id);
CREATE INDEX IF NOT EXISTS idx_receipts_tenant_id ON procurement_service.receipts(tenant_id);
CREATE INDEX IF NOT EXISTS idx_receipt_line_items_tenant_id ON procurement_service.receipt_line_items(tenant_id);
