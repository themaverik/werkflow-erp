-- V28: Seed custody mappings for all 5 active departments
-- custody_owner follows the DEPT:<code> convention used by asset_categories responsible_group
-- candidate_groups are Keycloak group names that receive task assignments for each department

INSERT INTO identity_service.custody_mappings (tenant_id, custody_owner, candidate_groups)
VALUES
    ('default', 'DEPT:IT',          ARRAY['it-team', 'it-leads']),
    ('default', 'DEPT:HR',          ARRAY['hr-team', 'hr-leads']),
    ('default', 'DEPT:FINANCE',     ARRAY['finance-team', 'finance-leads']),
    ('default', 'DEPT:PROCUREMENT', ARRAY['procurement-team', 'procurement-leads']),
    ('default', 'DEPT:LOGISTICS',   ARRAY['logistics-team', 'logistics-leads'])
ON CONFLICT (tenant_id, custody_owner) DO UPDATE
    SET candidate_groups = EXCLUDED.candidate_groups;
