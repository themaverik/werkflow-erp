-- Flyway Migration V21: Add tenant_id column to Finance domain tables
-- Adds multi-tenant isolation support by adding tenant_id (NOT NULL) to all Finance entities

-- Add tenant_id to budget_plans table
ALTER TABLE finance_service.budget_plans
ADD COLUMN IF NOT EXISTS tenant_id VARCHAR(255) NOT NULL DEFAULT 'default-tenant';

-- Add tenant_id to budget_categories table
ALTER TABLE finance_service.budget_categories
ADD COLUMN IF NOT EXISTS tenant_id VARCHAR(255) NOT NULL DEFAULT 'default-tenant';

-- Add tenant_id to budget_line_items table
ALTER TABLE finance_service.budget_line_items
ADD COLUMN IF NOT EXISTS tenant_id VARCHAR(255) NOT NULL DEFAULT 'default-tenant';

-- Add tenant_id to expenses table
ALTER TABLE finance_service.expenses
ADD COLUMN IF NOT EXISTS tenant_id VARCHAR(255) NOT NULL DEFAULT 'default-tenant';

-- Add tenant_id to approval_thresholds table
ALTER TABLE finance_service.approval_thresholds
ADD COLUMN IF NOT EXISTS tenant_id VARCHAR(255) NOT NULL DEFAULT 'default-tenant';

-- Create indexes on tenant_id for better query performance
CREATE INDEX IF NOT EXISTS idx_budget_plans_tenant_id ON finance_service.budget_plans(tenant_id);
CREATE INDEX IF NOT EXISTS idx_budget_categories_tenant_id ON finance_service.budget_categories(tenant_id);
CREATE INDEX IF NOT EXISTS idx_budget_line_items_tenant_id ON finance_service.budget_line_items(tenant_id);
CREATE INDEX IF NOT EXISTS idx_expenses_tenant_id ON finance_service.expenses(tenant_id);
CREATE INDEX IF NOT EXISTS idx_approval_thresholds_tenant_id ON finance_service.approval_thresholds(tenant_id);
