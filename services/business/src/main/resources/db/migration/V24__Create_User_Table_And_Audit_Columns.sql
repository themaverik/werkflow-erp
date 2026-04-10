-- V24__Create_User_Table_And_Audit_Columns.sql
-- Creates users table for caching OIDC user profiles (ADR-002)
-- Adds created_by_display_name and updated_by_display_name to audit-relevant entities

-- =============================================================================
-- 1. Create users table (identity domain — not scoped to HR/Finance/Procurement/Inventory)
-- =============================================================================
-- users table lives in identity_service schema to align with domain-per-schema
-- convention used throughout this codebase (hr_service, finance_service, etc.)
CREATE SCHEMA IF NOT EXISTS identity_service;

CREATE TABLE IF NOT EXISTS identity_service.users (
    keycloak_id  VARCHAR(255) NOT NULL,
    display_name VARCHAR(255) NOT NULL,
    email        VARCHAR(255),
    created_at   TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at   TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_users PRIMARY KEY (keycloak_id)
);

-- =============================================================================
-- 2. HR Service — audit display name columns
-- =============================================================================
ALTER TABLE hr_service.employees
    ADD COLUMN IF NOT EXISTS created_by_display_name VARCHAR(255),
    ADD COLUMN IF NOT EXISTS updated_by_display_name VARCHAR(255);

ALTER TABLE hr_service.departments
    ADD COLUMN IF NOT EXISTS created_by_display_name VARCHAR(255),
    ADD COLUMN IF NOT EXISTS updated_by_display_name VARCHAR(255);

ALTER TABLE hr_service.leaves
    ADD COLUMN IF NOT EXISTS created_by_display_name VARCHAR(255),
    ADD COLUMN IF NOT EXISTS updated_by_display_name VARCHAR(255);

-- =============================================================================
-- 3. Finance Service — audit display name columns
-- =============================================================================
ALTER TABLE finance_service.budget_plans
    ADD COLUMN IF NOT EXISTS created_by_display_name VARCHAR(255),
    ADD COLUMN IF NOT EXISTS updated_by_display_name VARCHAR(255);

ALTER TABLE finance_service.budget_line_items
    ADD COLUMN IF NOT EXISTS created_by_display_name VARCHAR(255),
    ADD COLUMN IF NOT EXISTS updated_by_display_name VARCHAR(255);

ALTER TABLE finance_service.expenses
    ADD COLUMN IF NOT EXISTS created_by_display_name VARCHAR(255),
    ADD COLUMN IF NOT EXISTS updated_by_display_name VARCHAR(255);

-- =============================================================================
-- 4. Procurement Service — audit display name columns
-- =============================================================================
ALTER TABLE procurement_service.purchase_requests
    ADD COLUMN IF NOT EXISTS created_by_display_name VARCHAR(255),
    ADD COLUMN IF NOT EXISTS updated_by_display_name VARCHAR(255);

ALTER TABLE procurement_service.purchase_orders
    ADD COLUMN IF NOT EXISTS created_by_display_name VARCHAR(255),
    ADD COLUMN IF NOT EXISTS updated_by_display_name VARCHAR(255);

ALTER TABLE procurement_service.receipts
    ADD COLUMN IF NOT EXISTS created_by_display_name VARCHAR(255),
    ADD COLUMN IF NOT EXISTS updated_by_display_name VARCHAR(255);

-- =============================================================================
-- 5. Inventory Service — audit display name columns
-- =============================================================================
ALTER TABLE inventory_service.custody_records
    ADD COLUMN IF NOT EXISTS created_by_display_name VARCHAR(255),
    ADD COLUMN IF NOT EXISTS updated_by_display_name VARCHAR(255);

ALTER TABLE inventory_service.asset_requests
    ADD COLUMN IF NOT EXISTS created_by_display_name VARCHAR(255),
    ADD COLUMN IF NOT EXISTS updated_by_display_name VARCHAR(255);

ALTER TABLE inventory_service.transfer_requests
    ADD COLUMN IF NOT EXISTS created_by_display_name VARCHAR(255),
    ADD COLUMN IF NOT EXISTS updated_by_display_name VARCHAR(255);

ALTER TABLE inventory_service.maintenance_records
    ADD COLUMN IF NOT EXISTS created_by_display_name VARCHAR(255),
    ADD COLUMN IF NOT EXISTS updated_by_display_name VARCHAR(255);
