-- V22: Seed demo procurement data — 5 vendors, 2 PRs, 2 POs, 1 receipt

-- -------------------------------------------------------------------------
-- Vendors
-- -------------------------------------------------------------------------
INSERT INTO procurement_service.vendors
    (name, code, contact_person, email, phone, payment_terms, rating, status, notes, tenant_id)
VALUES
    ('TechPro Solutions',       'TECHPRO',     'Alex Morgan',     'sales@techpro.example',      '+1-800-555-1001', 'NET_30', 4.7, 'ACTIVE', 'Primary IT hardware supplier — MacBook, Dell, Apple peripherals.',   'default'),
    ('OfficeWorld Supplies',    'OFFICEWORLD', 'Susan Clarke',    'orders@officeworld.example', '+1-800-555-1002', 'NET_15', 4.2, 'ACTIVE', 'Office furniture, chairs, and stationery.',                          'default'),
    ('FleetMasters India',      'FLEETMASTERS','Ramesh Iyer',     'fleet@fleetmasters.example', '+91-80-5550-2001','NET_30', 4.5, 'ACTIVE', 'Vehicle fleet procurement and maintenance contracts.',               'default'),
    ('Apex Consulting Group',   'APEX',        'Jennifer Walsh',  'info@apex.example',          '+1-206-555-2002', 'NET_45', 4.8, 'ACTIVE', 'Management consulting and training services.',                       'default'),
    ('CloudSys Technologies',   'CLOUDSYS',    'Kevin Park',      'sales@cloudsys.example',     '+1-650-555-2003', 'NET_30', 4.6, 'ACTIVE', 'SaaS subscriptions and cloud infrastructure.',                      'default');

-- -------------------------------------------------------------------------
-- Purchase Requests
-- PR-2025-001: IT dept, 2× MacBook Pro 16" — APPROVED
-- PR-2025-002: Finance dept, 3× Ergonomic Chairs — PENDING_APPROVAL
-- -------------------------------------------------------------------------
INSERT INTO procurement_service.purchase_requests
    (pr_number, requesting_dept_id, requester_user_id, request_date, required_by_date,
     priority, justification, total_amount, status, approved_by_user_id, approved_date, tenant_id)
VALUES
    ('PR-2025-001', 1, 1, '2025-02-10', '2025-03-15',
     'HIGH',
     'Two new MacBook Pro units required for onboarding Senior Developer (EMP-002) and DevOps Engineer (EMP-003). Current equipment is 5 years old and unsupported.',
     5998.00, 'APPROVED', 7, '2025-02-18 10:30:00', 'default'),

    ('PR-2025-002', 3, 7, '2025-04-05', '2025-05-01',
     'MEDIUM',
     'Finance team workspace expansion — 3 ergonomic chairs for new analyst hires. Approved under department budget FY2025.',
     4185.00, 'PENDING_APPROVAL', NULL, NULL, 'default');

-- PR line items
INSERT INTO procurement_service.pr_line_items
    (purchase_request_id, line_number, item_description, description, quantity,
     unit_of_measure, estimated_unit_price, estimated_total_amount, total_price,
     budget_category_id, tenant_id)
VALUES
    (1, 1, 'Apple MacBook Pro 16" M3 Pro — 36GB RAM / 1TB SSD', 'MacBook Pro 16" M3 Pro 2023', 2, 'UNIT', 2999.00, 5998.00, 5998.00, 7, 'default'),
    (2, 1, 'Herman Miller Aeron Ergonomic Office Chair',          'Ergonomic Chair — Aeron Size B', 3, 'UNIT', 1395.00, 4185.00, 4185.00, 7, 'default');

-- -------------------------------------------------------------------------
-- Purchase Orders
-- PO-2025-001: MacBook order — DELIVERED
-- PO-2025-002: Office supplies top-up — CONFIRMED (ad-hoc, no PR)
-- -------------------------------------------------------------------------
INSERT INTO procurement_service.purchase_orders
    (po_number, purchase_request_id, vendor_id, order_date, expected_delivery_date,
     actual_delivery_date, total_amount, grand_total, payment_terms, delivery_address,
     status, created_by_user_id, tenant_id)
VALUES
    ('PO-2025-001', 1, 1, '2025-02-20', '2025-03-10',
     '2025-03-08', 5998.00, 5998.00, 'NET_30',
     '1420 Harbor Ave SW, Seattle, WA 98126, USA',
     'DELIVERED', 9, 'default'),

    ('PO-2025-002', NULL, 2, '2025-03-15', '2025-03-22',
     NULL, 1197.00, 1197.00, 'NET_15',
     'Prestige Tech Park, Outer Ring Rd, Bengaluru 560103, India',
     'CONFIRMED', 9, 'default');

-- PO line items
INSERT INTO procurement_service.po_line_items
    (purchase_order_id, line_number, item_description, description,
     ordered_quantity, quantity, unit_of_measure, unit_price,
     total_amount, total_price, received_quantity, tenant_id)
VALUES
    -- PO-2025-001: 2× MacBook
    (1, 1, 'Apple MacBook Pro 16" M3 Pro', 'MacBook Pro 16" M3 Pro 2023', 2, 2, 'UNIT', 2999.00, 5998.00, 5998.00, 2, 'default'),
    -- PO-2025-002: office supplies
    (2, 1, 'A4 Copy Paper — 500 sheets/ream', 'Navigator A4 80gsm', 10, 10, 'REAM', 8.99,  89.90,  89.90,  0, 'default'),
    (2, 2, 'Whiteboard Markers — Assorted',  'Staedtler Lumocolor Set 12', 30, 30, 'PACK', 3.69, 110.70, 110.70,  0, 'default'),
    (2, 3, 'Stapler — Heavy Duty',            'Swingline 747 Business Stapler', 5, 5, 'UNIT', 24.99, 124.95, 124.95,  0, 'default'),
    (2, 4, 'Sticky Notes 3×3 — 12 pads/pack','Post-it Super Sticky', 20, 20, 'PACK', 4.38,  87.60,  87.60,  0, 'default');

-- -------------------------------------------------------------------------
-- Receipt for PO-2025-001 (MacBook delivery)
-- -------------------------------------------------------------------------
INSERT INTO procurement_service.receipts
    (receipt_number, purchase_order_id, receipt_date, received_by_user_id, status, notes, tenant_id)
VALUES
    ('RCV-2025-001', 1, '2025-03-08', 9, 'RECEIVED',
     'Both MacBook Pro units received in original sealed packaging. Serial numbers verified against PO.', 'default');

INSERT INTO procurement_service.receipt_line_items
    (receipt_id, po_line_item_id, quantity_received, accepted_quantity, rejected_quantity, condition, tenant_id)
VALUES
    (1, 1, 2, 2, 0, 'NEW', 'default');
