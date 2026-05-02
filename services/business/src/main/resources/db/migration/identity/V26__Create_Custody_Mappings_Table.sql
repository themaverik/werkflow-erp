-- V26__Create_Custody_Mappings_Table.sql
-- Creates custody_mappings table in identity_service schema (ADR-004)
-- Moves custody group management from werkflow-enterprise admin-service to ERP as source of truth.

CREATE SCHEMA IF NOT EXISTS identity_service;

CREATE TABLE IF NOT EXISTS identity_service.custody_mappings (
    id               BIGSERIAL PRIMARY KEY,
    tenant_id        VARCHAR(100)  NOT NULL,
    custody_owner    VARCHAR(255)  NOT NULL,
    candidate_groups TEXT[]        NOT NULL,
    created_at       TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at       TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_custody_owner_tenant UNIQUE (tenant_id, custody_owner)
);

CREATE INDEX IF NOT EXISTS idx_custody_mappings_tenant_id ON identity_service.custody_mappings (tenant_id);
