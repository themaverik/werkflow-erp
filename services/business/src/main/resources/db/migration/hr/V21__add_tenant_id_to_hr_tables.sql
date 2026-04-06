-- Flyway Migration V21: Add tenant_id column to HR domain tables
-- Adds multi-tenant isolation support by adding tenant_id (NOT NULL) to all HR entities

-- Add tenant_id to employees table
ALTER TABLE hr_service.employees
ADD COLUMN IF NOT EXISTS tenant_id VARCHAR(255) NOT NULL DEFAULT 'default-tenant';

-- Add tenant_id to departments table
ALTER TABLE hr_service.departments
ADD COLUMN IF NOT EXISTS tenant_id VARCHAR(255) NOT NULL DEFAULT 'default-tenant';

-- Add tenant_id to leaves table
ALTER TABLE hr_service.leaves
ADD COLUMN IF NOT EXISTS tenant_id VARCHAR(255) NOT NULL DEFAULT 'default-tenant';

-- Add tenant_id to attendances table
ALTER TABLE hr_service.attendances
ADD COLUMN IF NOT EXISTS tenant_id VARCHAR(255) NOT NULL DEFAULT 'default-tenant';

-- Add tenant_id to performance_reviews table
ALTER TABLE hr_service.performance_reviews
ADD COLUMN IF NOT EXISTS tenant_id VARCHAR(255) NOT NULL DEFAULT 'default-tenant';

-- Add tenant_id to payrolls table
ALTER TABLE hr_service.payrolls
ADD COLUMN IF NOT EXISTS tenant_id VARCHAR(255) NOT NULL DEFAULT 'default-tenant';

-- Create indexes on tenant_id for better query performance
CREATE INDEX IF NOT EXISTS idx_employees_tenant_id ON hr_service.employees(tenant_id);
CREATE INDEX IF NOT EXISTS idx_departments_tenant_id ON hr_service.departments(tenant_id);
CREATE INDEX IF NOT EXISTS idx_leaves_tenant_id ON hr_service.leaves(tenant_id);
CREATE INDEX IF NOT EXISTS idx_attendances_tenant_id ON hr_service.attendances(tenant_id);
CREATE INDEX IF NOT EXISTS idx_performance_reviews_tenant_id ON hr_service.performance_reviews(tenant_id);
CREATE INDEX IF NOT EXISTS idx_payrolls_tenant_id ON hr_service.payrolls(tenant_id);
