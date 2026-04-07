-- Flyway Migration V21: Add tenant_id column to Inventory domain tables
-- Adds multi-tenant isolation support by adding tenant_id (NOT NULL) to all Inventory entities

-- Add tenant_id to asset_categories table
ALTER TABLE inventory_service.asset_categories
ADD COLUMN IF NOT EXISTS tenant_id VARCHAR(255) NOT NULL DEFAULT 'default-tenant';

-- Add tenant_id to asset_definitions table
ALTER TABLE inventory_service.asset_definitions
ADD COLUMN IF NOT EXISTS tenant_id VARCHAR(255) NOT NULL DEFAULT 'default-tenant';

-- Add tenant_id to asset_instances table
ALTER TABLE inventory_service.asset_instances
ADD COLUMN IF NOT EXISTS tenant_id VARCHAR(255) NOT NULL DEFAULT 'default-tenant';

-- Add tenant_id to custody_records table
ALTER TABLE inventory_service.custody_records
ADD COLUMN IF NOT EXISTS tenant_id VARCHAR(255) NOT NULL DEFAULT 'default-tenant';

-- Add tenant_id to transfer_requests table
ALTER TABLE inventory_service.transfer_requests
ADD COLUMN IF NOT EXISTS tenant_id VARCHAR(255) NOT NULL DEFAULT 'default-tenant';

-- Add tenant_id to maintenance_records table
ALTER TABLE inventory_service.maintenance_records
ADD COLUMN IF NOT EXISTS tenant_id VARCHAR(255) NOT NULL DEFAULT 'default-tenant';

-- Create indexes on tenant_id for better query performance
CREATE INDEX IF NOT EXISTS idx_asset_categories_tenant_id ON inventory_service.asset_categories(tenant_id);
CREATE INDEX IF NOT EXISTS idx_asset_definitions_tenant_id ON inventory_service.asset_definitions(tenant_id);
CREATE INDEX IF NOT EXISTS idx_asset_instances_tenant_id ON inventory_service.asset_instances(tenant_id);
CREATE INDEX IF NOT EXISTS idx_custody_records_tenant_id ON inventory_service.custody_records(tenant_id);
CREATE INDEX IF NOT EXISTS idx_transfer_requests_tenant_id ON inventory_service.transfer_requests(tenant_id);
CREATE INDEX IF NOT EXISTS idx_maintenance_records_tenant_id ON inventory_service.maintenance_records(tenant_id);
