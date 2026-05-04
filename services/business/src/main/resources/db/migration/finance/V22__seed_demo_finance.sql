-- V22: Seed demo budget plans, line items, expenses; update approval thresholds to L1-L4

-- -------------------------------------------------------------------------
-- Update approval thresholds to L1-L4 amounts
-- (4 rows already exist from V3 seed with old role names)
-- -------------------------------------------------------------------------
UPDATE finance_service.approval_thresholds SET min_amount = 0.01,      max_amount =  10000.00 WHERE approval_order = 1 AND approver_role = 'DEPARTMENT_MANAGER';
UPDATE finance_service.approval_thresholds SET min_amount = 10000.01,  max_amount =  50000.00 WHERE approval_order = 1 AND approver_role = 'FINANCE_MANAGER';
UPDATE finance_service.approval_thresholds SET min_amount = 50000.01,  max_amount = 200000.00 WHERE approval_order = 1 AND approver_role = 'FINANCE_DIRECTOR';
UPDATE finance_service.approval_thresholds SET min_amount = 200000.01, max_amount = NULL       WHERE approval_order = 1 AND approver_role = 'CFO';

-- -------------------------------------------------------------------------
-- Budget Plans FY2025 — one per department
-- dept IDs: 1=IT, 2=HR, 3=Finance, 4=Procurement, 7=Logistics
-- created_by_user_id references employees: 1=James(IT),4=Priya(HR),6=Laura(Fin),9=Raj(Proc),11=David(Log)
-- -------------------------------------------------------------------------
INSERT INTO finance_service.budget_plans
    (department_id, fiscal_year, period_start, period_end,
     total_amount, allocated_amount, spent_amount, status,
     created_by_user_id, approved_by_user_id, approved_date, notes, tenant_id)
VALUES
    (1, 2025, '2025-01-01', '2025-12-31', 150000.00, 120000.00,  8500.00, 'ACTIVE',  1, 6, '2025-01-15 09:00:00', 'IT FY2025 — covers hardware refresh, software licenses, cloud infra.',         'default-tenant'),
    (2, 2025, '2025-01-01', '2025-12-31',  80000.00,  60000.00,  1500.00, 'ACTIVE',  4, 6, '2025-01-15 09:00:00', 'HR FY2025 — recruitment, training programs, employee engagement events.',     'default-tenant'),
    (3, 2025, '2025-01-01', '2025-12-31', 120000.00,  95000.00,  4200.00, 'ACTIVE',  6, 6, '2025-01-15 09:00:00', 'Finance FY2025 — audit fees, software, compliance training.',                'default-tenant'),
    (4, 2025, '2025-01-01', '2025-12-31', 100000.00,  75000.00,  3600.00, 'ACTIVE',  9, 6, '2025-01-15 09:00:00', 'Procurement FY2025 — vendor onboarding, travel, system subscriptions.',     'default-tenant'),
    (7, 2025, '2025-01-01', '2025-12-31',  90000.00,  70000.00,  2800.00, 'ACTIVE', 11, 6, '2025-01-15 09:00:00', 'Logistics FY2025 — fleet maintenance, warehouse ops, freight expenses.',    'default-tenant');

-- -------------------------------------------------------------------------
-- Budget Line Items
-- budget_categories IDs from V3 seed: 1=OPS,2=CAP,3=HR,4=TECH,5=OPS-SUP,6=OPS-TRV,7=CAP-EQP,8=HR-TRN,9=TECH-LIC
-- -------------------------------------------------------------------------
INSERT INTO finance_service.budget_line_items
    (budget_plan_id, category_id, description, allocated_amount, spent_amount, tenant_id)
VALUES
    -- IT plan (id=1)
    (1, 7, 'Hardware refresh — laptops and peripherals', 60000.00, 5998.00, 'default-tenant'),
    (1, 9, 'Software licenses — JetBrains, Atlassian, GitHub', 30000.00, 2400.00, 'default-tenant'),
    (1, 4, 'Cloud infrastructure — AWS, monitoring tools',    30000.00,  102.00, 'default-tenant'),

    -- HR plan (id=2)
    (2, 3, 'Recruitment — job boards, assessments',           20000.00,    0.00, 'default-tenant'),
    (2, 8, 'Employee training and development programs',      25000.00, 1500.00, 'default-tenant'),
    (2, 5, 'Office supplies and onboarding kits',             15000.00,    0.00, 'default-tenant'),

    -- Finance plan (id=3)
    (3, 9, 'Accounting software — SAP, QuickBooks',          35000.00, 3200.00, 'default-tenant'),
    (3, 6, 'Finance team travel and audit visits',            20000.00, 1000.00, 'default-tenant'),
    (3, 8, 'Compliance and CFA training',                     40000.00,    0.00, 'default-tenant'),

    -- Procurement plan (id=4)
    (4, 9, 'Procurement platform — Ariba subscription',      40000.00, 3600.00, 'default-tenant'),
    (4, 6, 'Vendor visits and supplier audits travel',        20000.00,    0.00, 'default-tenant'),
    (4, 5, 'Office supplies and documentation',               15000.00,    0.00, 'default-tenant'),

    -- Logistics plan (id=5)
    (5, 2, 'Fleet maintenance and vehicle servicing',         40000.00, 2800.00, 'default-tenant'),
    (5, 1, 'Warehouse operations and freight costs',          20000.00,    0.00, 'default-tenant'),
    (5, 5, 'Packing materials and labelling supplies',        10000.00,    0.00, 'default-tenant');

-- -------------------------------------------------------------------------
-- Expenses (2)
-- submitted_by_user_id: 2=Sarah Kim(IT), 5=Aditya Nair(HR)
-- -------------------------------------------------------------------------
INSERT INTO finance_service.expenses
    (budget_line_item_id, department_id, expense_date, amount, category_id,
     vendor_name, description, status, submitted_by_user_id, tenant_id)
VALUES
    (2, 1, '2025-03-01', 2400.00, 9,
     'Atlassian', 'Annual Jira Software + Confluence license renewal — 10 seats.',
     'APPROVED', 2, 'default-tenant'),

    (5, 2, '2025-03-17', 1500.00, 8,
     'Apex Consulting Group', 'Leadership & Communication Skills workshop for HR team (5 attendees).',
     'APPROVED', 5, 'default-tenant');
