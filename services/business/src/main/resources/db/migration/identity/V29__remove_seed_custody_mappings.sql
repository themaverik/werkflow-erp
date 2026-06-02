-- V29: Remove previously seeded DEPT:* custody mappings (tenant 'default').
-- These referenced phantom Keycloak groups (it-team, finance-leads, etc.) that were never
-- created in the realm. Admins configure real mappings via the portal after Keycloak setup.
DELETE FROM identity_service.custody_mappings
WHERE tenant_id = 'default'
  AND custody_owner IN ('DEPT:IT', 'DEPT:HR', 'DEPT:FINANCE', 'DEPT:PROCUREMENT', 'DEPT:LOGISTICS');
