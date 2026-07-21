-- V23: Promote insurance tracking to first-class columns on asset_instances.
-- Previously insurance data (if any) rode in the metadata JSONB blob, which meant
-- there was no queryable expiry and no /expiring-insurance endpoint. These columns
-- mirror warranty_expiry_date so warranty and insurance watch behave symmetrically.
ALTER TABLE inventory_service.asset_instances
    ADD COLUMN insurance_provider VARCHAR(200),
    ADD COLUMN insurance_expiry_date DATE;
