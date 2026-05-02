-- V25__Add_Profile_Columns_To_Users.sql
-- Extends identity_service.users with enterprise profile fields (ADR-003, ADR-005)

ALTER TABLE identity_service.users
    ADD COLUMN IF NOT EXISTS department_code VARCHAR(50),
    ADD COLUMN IF NOT EXISTS employee_id     VARCHAR(100),
    ADD COLUMN IF NOT EXISTS cost_center     VARCHAR(50),
    ADD COLUMN IF NOT EXISTS is_poc          BOOLEAN NOT NULL DEFAULT FALSE;
