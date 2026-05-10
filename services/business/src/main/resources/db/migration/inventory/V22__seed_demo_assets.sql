-- V22: Seed demo asset instances, custody records, and 1 transfer request

-- -------------------------------------------------------------------------
-- Asset Instances
-- Definition IDs from V5 seed (sequence reset to 1):
--   1 = MacBook Pro 16" (IT-LAP-001)
--   2 = Dell XPS 15     (IT-LAP-002)
--   4 = iPhone 15 Pro   (IT-MOB-001)
--   9 = Ergonomic Chair (OFF-CHAIR-001)
-- -------------------------------------------------------------------------
INSERT INTO inventory_service.asset_instances
    (asset_definition_id, asset_tag, serial_number, purchase_date, purchase_cost,
     warranty_expiry_date, condition, status, current_location, tenant_id)
VALUES
    (1, 'ASSET-IT-0001', 'C02ZK1ABMD6N', '2025-03-08', 2999.00, '2027-03-08', 'NEW',  'IN_USE',    'Seattle HQ — IT Floor',    'default'),
    (1, 'ASSET-IT-0002', 'C02ZK1ACMD6N', '2025-03-08', 2999.00, '2027-03-08', 'NEW',  'IN_USE',    'Seattle HQ — IT Floor',    'default'),
    (2, 'ASSET-IT-0003', 'DXPS15SN00312','2024-08-15', 1899.00, '2026-08-15', 'GOOD', 'AVAILABLE', 'Seattle HQ — IT Stockroom','default'),
    (4, 'ASSET-IT-0004', 'G7TXR4K5PHN8M','2024-11-01',  999.00, '2025-11-01', 'GOOD', 'IN_USE',    'Seattle HQ — IT Floor',    'default'),
    (9, 'ASSET-OFF-0001','HM-AERON-00045','2023-06-20', 1395.00, '2033-06-20', 'GOOD', 'IN_USE',    'Bangalore HQ — HR Floor',  'default');

-- -------------------------------------------------------------------------
-- Custody Records
-- dept_id references hr_service.departments: 1=IT, 2=HR, 7=LOGISTICS
-- custodian_user_id is the hr_service.employees.id
-- -------------------------------------------------------------------------
INSERT INTO inventory_service.custody_records
    (asset_instance_id, custodian_dept_id, custodian_user_id, physical_location,
     custody_type, start_date, end_date, tenant_id)
VALUES
    -- MacBook #1 → Sarah Kim (EMP-002, IT dept)
    (1, 1, 2, 'Seattle HQ — IT Floor, Desk S-104', 'PERMANENT', '2025-03-10 09:00:00', NULL, 'default'),
    -- MacBook #2 → Mike Torres (EMP-003, IT dept)
    (2, 1, 3, 'Seattle HQ — IT Floor, Desk S-106', 'PERMANENT', '2025-03-10 09:00:00', NULL, 'default'),
    -- iPhone → James Chen (EMP-001, IT Manager)
    (4, 1, 1, 'Seattle HQ — IT Floor, Desk S-101', 'PERMANENT', '2024-11-05 10:00:00', NULL, 'default'),
    -- Ergonomic Chair → Priya Sharma (EMP-004, HR)
    (5, 2, 4, 'Bangalore HQ — HR Floor, Desk B-201', 'PERMANENT', '2023-06-25 08:30:00', NULL, 'default');

-- -------------------------------------------------------------------------
-- Transfer Request — Dell XPS 15 from IT to Procurement (pending approval)
-- -------------------------------------------------------------------------
INSERT INTO inventory_service.transfer_requests
    (asset_instance_id, from_dept_id, from_user_id, to_dept_id, to_user_id,
     transfer_type, transfer_reason, initiated_by_user_id, initiated_date, status, tenant_id)
VALUES
    (3, 1, 1, 4, 9,
     'INTER_DEPARTMENT',
     'Dell XPS 15 no longer required in IT team after MacBook refresh. Procurement team needs a laptop for vendor evaluation sessions in Bangalore.',
     1, '2025-04-20 11:00:00', 'PENDING', 'default');
