-- V22: Add Logistics department and seed 12 demo employees across 5 departments

-- -------------------------------------------------------------------------
-- Logistics department (depts 1-6 already seeded by V5; sequence at 7)
-- -------------------------------------------------------------------------
INSERT INTO hr_service.departments (name, code, organization_id, department_type, office_location, department_email, is_active, tenant_id)
VALUES ('Logistics', 'LOGISTICS', 1, 'OPS', 'SHILLONG_IN', 'logistics@werkflow.com', true, 'default-tenant');

-- -------------------------------------------------------------------------
-- Employees  (sequence reset to 1 by V5; no prior rows)
-- IT (dept_id=1)
-- -------------------------------------------------------------------------
INSERT INTO hr_service.employees
    (employee_code, first_name, last_name, email, phone_number, date_of_birth, join_date,
     job_title, employment_status, salary, department_id, department_code,
     organization_id, doa_level, office_location, gender, is_active, tenant_id)
VALUES
    ('EMP-001', 'James',   'Chen',      'james.chen@werkflow.com',      '+1-206-555-0101', '1985-03-15', '2020-01-10',
     'IT Manager',          'ACTIVE', 8500.00, 1, 'IT',          1, 2, 'SEATTLE_US',   'MALE',             true, 'default-tenant'),
    ('EMP-002', 'Sarah',   'Kim',       'sarah.kim@werkflow.com',       '+1-206-555-0102', '1990-07-22', '2021-03-01',
     'Senior Developer',    'ACTIVE', 7200.00, 1, 'IT',          1, 1, 'SEATTLE_US',   'FEMALE',           true, 'default-tenant'),
    ('EMP-003', 'Mike',    'Torres',    'mike.torres@werkflow.com',     '+1-206-555-0103', '1993-11-08', '2022-06-15',
     'DevOps Engineer',     'ACTIVE', 6800.00, 1, 'IT',          1, 0, 'SEATTLE_US',   'MALE',             true, 'default-tenant'),

-- HR (dept_id=2)
    ('EMP-004', 'Priya',   'Sharma',    'priya.sharma@werkflow.com',    '+91-80-5550-0201', '1988-05-12', '2019-07-01',
     'HR Manager',          'ACTIVE', 7500.00, 2, 'HR',          1, 2, 'BANGALORE_IN', 'FEMALE',           true, 'default-tenant'),
    ('EMP-005', 'Aditya',  'Nair',      'aditya.nair@werkflow.com',     '+91-80-5550-0202', '1995-02-28', '2023-01-16',
     'HR Specialist',       'ACTIVE', 5500.00, 2, 'HR',          1, 0, 'BANGALORE_IN', 'MALE',             true, 'default-tenant'),

-- Finance (dept_id=3)
    ('EMP-006', 'Laura',   'Bennett',   'laura.bennett@werkflow.com',   '+1-206-555-0301', '1980-09-04', '2017-04-03',
     'Finance Director',    'ACTIVE',12000.00, 3, 'FINANCE',     1, 3, 'SEATTLE_US',   'FEMALE',           true, 'default-tenant'),
    ('EMP-007', 'Chris',   'Anderson',  'chris.anderson@werkflow.com',  '+1-206-555-0302', '1984-12-19', '2018-09-10',
     'Finance Manager',     'ACTIVE', 9500.00, 3, 'FINANCE',     1, 2, 'SEATTLE_US',   'MALE',             true, 'default-tenant'),
    ('EMP-008', 'Emma',    'Wilson',    'emma.wilson@werkflow.com',     '+1-206-555-0303', '1992-04-30', '2022-02-14',
     'Financial Analyst',   'ACTIVE', 6500.00, 3, 'FINANCE',     1, 0, 'SEATTLE_US',   'FEMALE',           true, 'default-tenant'),

-- Procurement (dept_id=4)
    ('EMP-009', 'Raj',     'Patel',     'raj.patel@werkflow.com',       '+91-80-5550-0401', '1983-06-17', '2018-11-05',
     'Procurement Manager', 'ACTIVE', 8000.00, 4, 'PROCUREMENT', 1, 2, 'BANGALORE_IN', 'MALE',             true, 'default-tenant'),
    ('EMP-010', 'Neha',    'Gupta',     'neha.gupta@werkflow.com',      '+91-80-5550-0402', '1994-08-23', '2022-08-22',
     'Procurement Specialist','ACTIVE',5800.00, 4, 'PROCUREMENT', 1, 0, 'BANGALORE_IN', 'FEMALE',          true, 'default-tenant'),

-- Logistics (dept_id=7 — inserted above)
    ('EMP-011', 'David',   'Lyngdoh',   'david.lyngdoh@werkflow.com',   '+91-364-555-0501', '1986-01-25', '2020-05-18',
     'Logistics Head',      'ACTIVE', 7800.00, 7, 'LOGISTICS',   1, 2, 'SHILLONG_IN',  'MALE',             true, 'default-tenant'),
    ('EMP-012', 'Mary',    'Kharkongor','mary.kharkongor@werkflow.com', '+91-364-555-0502', '1996-10-14', '2023-03-06',
     'Logistics Coordinator','ACTIVE',5200.00, 7, 'LOGISTICS',   1, 0, 'SHILLONG_IN',  'FEMALE',           true, 'default-tenant');

-- -------------------------------------------------------------------------
-- Leaves (2 records)
-- -------------------------------------------------------------------------
INSERT INTO hr_service.leaves
    (employee_id, leave_type, start_date, end_date, number_of_days, reason, status, approved_by, approved_at, tenant_id)
VALUES
    (2, 'ANNUAL',  '2025-04-07', '2025-04-11', 5, 'Family vacation', 'APPROVED', 1, '2025-03-28', 'default-tenant'),
    (5, 'SICK',    '2025-03-17', '2025-03-18', 2, 'Fever and cold',  'APPROVED', 4, '2025-03-17', 'default-tenant');

-- -------------------------------------------------------------------------
-- Payrolls — April 2025 for 3 employees
-- -------------------------------------------------------------------------
INSERT INTO hr_service.payrolls
    (employee_id, payment_month, payment_year, payment_date,
     basic_salary, allowances, bonuses, tax_deduction, insurance_deduction,
     gross_salary, net_salary, is_paid, tenant_id)
VALUES
    (1, 4, 2025, '2025-04-30', 8500.00,  500.00,  0.00, 1500.00, 200.00,  9000.00,  7300.00, true,  'default-tenant'),
    (4, 4, 2025, '2025-04-30', 7500.00,  400.00,  0.00, 1200.00, 150.00,  7900.00,  6550.00, true,  'default-tenant'),
    (6, 4, 2025, '2025-04-30',12000.00, 1000.00, 500.00,2500.00, 300.00, 13500.00, 10700.00, true,  'default-tenant');

-- -------------------------------------------------------------------------
-- Performance review — EMP-002 reviewed by EMP-001
-- -------------------------------------------------------------------------
INSERT INTO hr_service.performance_reviews
    (employee_id, review_date, review_period_start, review_period_end,
     rating, score, strengths, areas_for_improvement, goals, comments,
     reviewer_id, employee_acknowledged, acknowledged_at, tenant_id)
VALUES
    (2, '2025-01-15', '2024-01-01', '2024-12-31',
     'EXCEEDS_EXPECTATIONS', 88.5,
     'Strong backend skills, proactive code reviews, excellent mentoring of junior devs.',
     'Could improve documentation habits; presentation skills need polish.',
     'Lead the API gateway migration in Q2; earn AWS Solutions Architect cert.',
     'Consistently delivers high-quality work. Recommended for senior-lead track.',
     1, true, '2025-01-20', 'default-tenant');
