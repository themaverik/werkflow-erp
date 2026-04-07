package com.werkflow.business.migration;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Contract test: Verify all HR, Finance, Procurement, and Inventory entities
 * have tenant_id column with NOT NULL constraint after migration V21+
 *
 * This test directly queries the database to verify schema contracts.
 * Requires a live PostgreSQL instance; excluded from unit test phase via @Tag("integration").
 */
@Tag("integration")
class TenantIdMigrationTest {

    private static final String DB_URL = System.getenv("POSTGRES_URL") != null
        ? System.getenv("POSTGRES_URL")
        : "jdbc:postgresql://localhost:5433/werkflow";
    private static final String DB_USER = System.getenv("POSTGRES_USER") != null
        ? System.getenv("POSTGRES_USER")
        : "werkflow_admin";
    private static final String DB_PASS = System.getenv("POSTGRES_PASSWORD") != null
        ? System.getenv("POSTGRES_PASSWORD")
        : "secure_password_change_me";

    private static final List<String> TABLES_TO_VERIFY = Arrays.asList(
        // HR tables
        "hr_service.employees",
        "hr_service.departments",
        "hr_service.leaves",
        "hr_service.attendances",
        "hr_service.performance_reviews",
        "hr_service.payrolls",
        // Finance tables
        "finance_service.budget_plans",
        "finance_service.budget_categories",
        "finance_service.budget_line_items",
        "finance_service.expenses",
        "finance_service.approval_thresholds",
        // Procurement tables
        "procurement_service.vendors",
        "procurement_service.purchase_requests",
        "procurement_service.pr_line_items",
        "procurement_service.purchase_orders",
        "procurement_service.po_line_items",
        "procurement_service.receipts",
        "procurement_service.receipt_line_items",
        // Inventory tables
        "inventory_service.asset_categories",
        "inventory_service.asset_definitions",
        "inventory_service.asset_instances",
        "inventory_service.custody_records",
        "inventory_service.transfer_requests",
        "inventory_service.maintenance_records"
    );

    @Test
    void testAllTablesHaveTenantIdColumn() {
        for (String table : TABLES_TO_VERIFY) {
            assertTrue(
                tableHasColumn(extractSchema(table), extractTable(table), "tenant_id"),
                "Table " + table + " should have tenant_id column"
            );
        }
    }

    @Test
    void testTenantIdColumnIsNotNull() {
        for (String table : TABLES_TO_VERIFY) {
            assertTrue(
                columnIsNotNull(extractSchema(table), extractTable(table), "tenant_id"),
                "Column tenant_id in " + table + " should be NOT NULL"
            );
        }
    }

    @Test
    void testTenantIdColumnHasCorrectType() {
        for (String table : TABLES_TO_VERIFY) {
            String dataType = getColumnDataType(extractSchema(table), extractTable(table), "tenant_id");
            assertNotNull(dataType, "Could not determine data type for tenant_id in " + table);
            assertTrue(
                dataType.toLowerCase().contains("varchar") ||
                    dataType.toLowerCase().contains("character") ||
                    dataType.toLowerCase().contains("text"),
                "tenant_id in " + table + " should be VARCHAR/CHARACTER/TEXT, got: " + dataType
            );
        }
    }

    private boolean tableHasColumn(String schema, String table, String column) {
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS)) {
            DatabaseMetaData metaData = conn.getMetaData();
            ResultSet rs = metaData.getColumns(null, schema, table, column);
            return rs.next();
        } catch (Exception e) {
            fail("Error checking column existence for " + schema + "." + table + "." + column + ": " + e.getMessage());
            return false;
        }
    }

    private boolean columnIsNotNull(String schema, String table, String column) {
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS)) {
            DatabaseMetaData metaData = conn.getMetaData();
            ResultSet rs = metaData.getColumns(null, schema, table, column);
            if (rs.next()) {
                int nullable = rs.getInt("NULLABLE");
                // DatabaseMetaData.columnNoNulls = 0 means NOT NULL
                return nullable == DatabaseMetaData.columnNoNulls;
            }
            return false;
        } catch (Exception e) {
            fail("Error checking NOT NULL constraint for " + schema + "." + table + "." + column + ": " + e.getMessage());
            return false;
        }
    }

    private String getColumnDataType(String schema, String table, String column) {
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS)) {
            DatabaseMetaData metaData = conn.getMetaData();
            ResultSet rs = metaData.getColumns(null, schema, table, column);
            if (rs.next()) {
                return rs.getString("TYPE_NAME");
            }
            return null;
        } catch (Exception e) {
            fail("Error getting data type for " + schema + "." + table + "." + column + ": " + e.getMessage());
            return null;
        }
    }

    private String extractSchema(String qualifiedTable) {
        return qualifiedTable.split("\\.")[0];
    }

    private String extractTable(String qualifiedTable) {
        return qualifiedTable.split("\\.")[1];
    }
}
