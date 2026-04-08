package com.werkflow.business.common.meta;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class EnumMetadataServiceTest {

    private EnumMetadataService service;

    @BeforeEach
    void setUp() {
        service = new EnumMetadataService();
    }

    @Test
    void testGetAllEnumsReturnsNonEmpty() {
        EnumMetadataResponseDTO response = service.getAllEnums();
        assertNotNull(response);
        assertNotNull(response.getEnums());
        assertFalse(response.getEnums().isEmpty());
    }

    @Test
    void testGetAllEnumsReturns15Enums() {
        EnumMetadataResponseDTO response = service.getAllEnums();
        assertEquals(15, response.getEnums().size(), "Should return exactly 15 enums");
    }

    @Test
    void testAllEnumsHaveRequiredFields() {
        EnumMetadataResponseDTO response = service.getAllEnums();

        for (EnumMetadataDTO enumMetadata : response.getEnums()) {
            assertNotNull(enumMetadata.getName(), "Enum name should not be null");
            assertFalse(enumMetadata.getName().trim().isEmpty(), "Enum name should not be empty");
            assertNotNull(enumMetadata.getDescription(), "Enum description should not be null");
            assertFalse(enumMetadata.getDescription().trim().isEmpty(), "Enum description should not be empty");
            assertNotNull(enumMetadata.getValues(), "Enum values should not be null");
            assertFalse(enumMetadata.getValues().isEmpty(), "Enum should have at least one value");
        }
    }

    @Test
    void testAllEnumValuesHaveRequiredFields() {
        EnumMetadataResponseDTO response = service.getAllEnums();

        for (EnumMetadataDTO enumMetadata : response.getEnums()) {
            for (EnumValueDTO value : enumMetadata.getValues()) {
                assertNotNull(value.getValue(), "Value should not be null in " + enumMetadata.getName());
                assertFalse(value.getValue().trim().isEmpty(), "Value should not be empty in " + enumMetadata.getName());
                assertNotNull(value.getLabel(), "Label should not be null in " + enumMetadata.getName());
                assertFalse(value.getLabel().trim().isEmpty(), "Label should not be empty in " + enumMetadata.getName());
                assertNotNull(value.getDescription(), "Description should not be null in " + enumMetadata.getName());
            }
        }
    }

    @Test
    void testHasAllHREnums() {
        EnumMetadataResponseDTO response = service.getAllEnums();
        Map<String, EnumMetadataDTO> enumMap = response.getEnums().stream()
                .collect(Collectors.toMap(EnumMetadataDTO::getName, e -> e));

        assertTrue(enumMap.containsKey("EmployeeStatus"), "Should contain EmployeeStatus");
        assertTrue(enumMap.containsKey("LeaveType"), "Should contain LeaveType");
        assertTrue(enumMap.containsKey("AttendanceStatus"), "Should contain AttendanceStatus");
        assertTrue(enumMap.containsKey("PerformanceRating"), "Should contain PerformanceRating");
    }

    @Test
    void testHasAllFinanceEnums() {
        EnumMetadataResponseDTO response = service.getAllEnums();
        Map<String, EnumMetadataDTO> enumMap = response.getEnums().stream()
                .collect(Collectors.toMap(EnumMetadataDTO::getName, e -> e));

        assertTrue(enumMap.containsKey("BudgetStatus"), "Should contain BudgetStatus");
        assertTrue(enumMap.containsKey("ExpenseStatus"), "Should contain ExpenseStatus");
    }

    @Test
    void testHasAllProcurementEnums() {
        EnumMetadataResponseDTO response = service.getAllEnums();
        Map<String, EnumMetadataDTO> enumMap = response.getEnums().stream()
                .collect(Collectors.toMap(EnumMetadataDTO::getName, e -> e));

        assertTrue(enumMap.containsKey("PrStatus"), "Should contain PrStatus");
        assertTrue(enumMap.containsKey("PoStatus"), "Should contain PoStatus");
        assertTrue(enumMap.containsKey("ReceiptStatus"), "Should contain ReceiptStatus");
        assertTrue(enumMap.containsKey("VendorStatus"), "Should contain VendorStatus");
    }

    @Test
    void testHasAllInventoryEnums() {
        EnumMetadataResponseDTO response = service.getAllEnums();
        Map<String, EnumMetadataDTO> enumMap = response.getEnums().stream()
                .collect(Collectors.toMap(EnumMetadataDTO::getName, e -> e));

        assertTrue(enumMap.containsKey("AssetRequestStatus"), "Should contain AssetRequestStatus");
        assertTrue(enumMap.containsKey("AssetCondition"), "Should contain AssetCondition");
        assertTrue(enumMap.containsKey("AssetStatus"), "Should contain AssetStatus");
        assertTrue(enumMap.containsKey("TransferStatus"), "Should contain TransferStatus");
        assertTrue(enumMap.containsKey("MaintenanceType"), "Should contain MaintenanceType");
    }

    @Test
    void testEmployeeStatusEnumValues() {
        EnumMetadataResponseDTO response = service.getAllEnums();
        EnumMetadataDTO enumMetadata = response.getEnums().stream()
                .filter(e -> e.getName().equals("EmployeeStatus"))
                .findFirst()
                .orElseThrow();

        List<String> values = enumMetadata.getValues().stream()
                .map(EnumValueDTO::getValue)
                .collect(Collectors.toList());

        assertEquals(5, values.size());
        assertTrue(values.containsAll(List.of("ACTIVE", "ON_LEAVE", "SUSPENDED", "TERMINATED", "RESIGNED")));
    }

    @Test
    void testLeaveTypeEnumValues() {
        EnumMetadataResponseDTO response = service.getAllEnums();
        EnumMetadataDTO enumMetadata = response.getEnums().stream()
                .filter(e -> e.getName().equals("LeaveType"))
                .findFirst()
                .orElseThrow();

        List<String> values = enumMetadata.getValues().stream()
                .map(EnumValueDTO::getValue)
                .collect(Collectors.toList());

        assertEquals(8, values.size());
        assertTrue(values.containsAll(List.of("ANNUAL", "SICK", "MATERNITY", "PATERNITY", "UNPAID", "COMPENSATORY", "BEREAVEMENT", "STUDY")));
    }

    @Test
    void testAttendanceStatusEnumValues() {
        EnumMetadataResponseDTO response = service.getAllEnums();
        EnumMetadataDTO enumMetadata = response.getEnums().stream()
                .filter(e -> e.getName().equals("AttendanceStatus"))
                .findFirst()
                .orElseThrow();

        List<String> values = enumMetadata.getValues().stream()
                .map(EnumValueDTO::getValue)
                .collect(Collectors.toList());

        assertEquals(6, values.size());
        assertTrue(values.containsAll(List.of("PRESENT", "ABSENT", "HALF_DAY", "ON_LEAVE", "HOLIDAY", "WEEKEND")));
    }

    @Test
    void testPerformanceRatingEnumValues() {
        EnumMetadataResponseDTO response = service.getAllEnums();
        EnumMetadataDTO enumMetadata = response.getEnums().stream()
                .filter(e -> e.getName().equals("PerformanceRating"))
                .findFirst()
                .orElseThrow();

        List<String> values = enumMetadata.getValues().stream()
                .map(EnumValueDTO::getValue)
                .collect(Collectors.toList());

        assertEquals(5, values.size());
        assertTrue(values.containsAll(List.of("OUTSTANDING", "EXCEEDS_EXPECTATIONS", "MEETS_EXPECTATIONS", "NEEDS_IMPROVEMENT", "UNSATISFACTORY")));
    }

    @Test
    void testBudgetStatusEnumValues() {
        EnumMetadataResponseDTO response = service.getAllEnums();
        EnumMetadataDTO enumMetadata = response.getEnums().stream()
                .filter(e -> e.getName().equals("BudgetStatus"))
                .findFirst()
                .orElseThrow();

        List<String> values = enumMetadata.getValues().stream()
                .map(EnumValueDTO::getValue)
                .collect(Collectors.toList());

        assertEquals(6, values.size());
        assertTrue(values.containsAll(List.of("DRAFT", "PENDING_APPROVAL", "APPROVED", "REJECTED", "ACTIVE", "CLOSED")));
    }

    @Test
    void testExpenseStatusEnumValues() {
        EnumMetadataResponseDTO response = service.getAllEnums();
        EnumMetadataDTO enumMetadata = response.getEnums().stream()
                .filter(e -> e.getName().equals("ExpenseStatus"))
                .findFirst()
                .orElseThrow();

        List<String> values = enumMetadata.getValues().stream()
                .map(EnumValueDTO::getValue)
                .collect(Collectors.toList());

        assertEquals(6, values.size());
        assertTrue(values.containsAll(List.of("PENDING", "SUBMITTED", "APPROVED", "REJECTED", "PAID", "CANCELLED")));
    }

    @Test
    void testPrStatusEnumValues() {
        EnumMetadataResponseDTO response = service.getAllEnums();
        EnumMetadataDTO enumMetadata = response.getEnums().stream()
                .filter(e -> e.getName().equals("PrStatus"))
                .findFirst()
                .orElseThrow();

        List<String> values = enumMetadata.getValues().stream()
                .map(EnumValueDTO::getValue)
                .collect(Collectors.toList());

        assertEquals(9, values.size());
        assertTrue(values.containsAll(List.of("DRAFT", "PENDING", "SUBMITTED", "PENDING_APPROVAL", "APPROVED", "REJECTED", "ORDERED", "RECEIVED", "CANCELLED")));
    }

    @Test
    void testPoStatusEnumValues() {
        EnumMetadataResponseDTO response = service.getAllEnums();
        EnumMetadataDTO enumMetadata = response.getEnums().stream()
                .filter(e -> e.getName().equals("PoStatus"))
                .findFirst()
                .orElseThrow();

        List<String> values = enumMetadata.getValues().stream()
                .map(EnumValueDTO::getValue)
                .collect(Collectors.toList());

        assertEquals(6, values.size());
        assertTrue(values.containsAll(List.of("DRAFT", "PENDING", "CONFIRMED", "SHIPPED", "DELIVERED", "CANCELLED")));
    }

    @Test
    void testReceiptStatusEnumValues() {
        EnumMetadataResponseDTO response = service.getAllEnums();
        EnumMetadataDTO enumMetadata = response.getEnums().stream()
                .filter(e -> e.getName().equals("ReceiptStatus"))
                .findFirst()
                .orElseThrow();

        List<String> values = enumMetadata.getValues().stream()
                .map(EnumValueDTO::getValue)
                .collect(Collectors.toList());

        assertEquals(7, values.size());
        assertTrue(values.containsAll(List.of("DRAFT", "SUBMITTED", "APPROVED", "RECEIVED", "DISCREPANCY", "REJECTED", "CANCELLED")));
    }

    @Test
    void testVendorStatusEnumValues() {
        EnumMetadataResponseDTO response = service.getAllEnums();
        EnumMetadataDTO enumMetadata = response.getEnums().stream()
                .filter(e -> e.getName().equals("VendorStatus"))
                .findFirst()
                .orElseThrow();

        List<String> values = enumMetadata.getValues().stream()
                .map(EnumValueDTO::getValue)
                .collect(Collectors.toList());

        assertEquals(3, values.size());
        assertTrue(values.containsAll(List.of("ACTIVE", "INACTIVE", "BLACKLISTED")));
    }

    @Test
    void testAssetRequestStatusEnumValues() {
        EnumMetadataResponseDTO response = service.getAllEnums();
        EnumMetadataDTO enumMetadata = response.getEnums().stream()
                .filter(e -> e.getName().equals("AssetRequestStatus"))
                .findFirst()
                .orElseThrow();

        List<String> values = enumMetadata.getValues().stream()
                .map(EnumValueDTO::getValue)
                .collect(Collectors.toList());

        assertEquals(6, values.size());
        assertTrue(values.containsAll(List.of("PENDING", "APPROVED", "REJECTED", "FULFILLED", "PROCUREMENT_INITIATED", "CANCELLED")));
    }

    @Test
    void testAssetConditionEnumValues() {
        EnumMetadataResponseDTO response = service.getAllEnums();
        EnumMetadataDTO enumMetadata = response.getEnums().stream()
                .filter(e -> e.getName().equals("AssetCondition"))
                .findFirst()
                .orElseThrow();

        List<String> values = enumMetadata.getValues().stream()
                .map(EnumValueDTO::getValue)
                .collect(Collectors.toList());

        assertEquals(6, values.size());
        assertTrue(values.containsAll(List.of("NEW", "GOOD", "FAIR", "POOR", "DAMAGED", "NEEDS_REPAIR")));
    }

    @Test
    void testAssetStatusEnumValues() {
        EnumMetadataResponseDTO response = service.getAllEnums();
        EnumMetadataDTO enumMetadata = response.getEnums().stream()
                .filter(e -> e.getName().equals("AssetStatus"))
                .findFirst()
                .orElseThrow();

        List<String> values = enumMetadata.getValues().stream()
                .map(EnumValueDTO::getValue)
                .collect(Collectors.toList());

        assertEquals(6, values.size());
        assertTrue(values.containsAll(List.of("AVAILABLE", "IN_USE", "MAINTENANCE", "RETIRED", "DISPOSED", "LOST")));
    }

    @Test
    void testTransferStatusEnumValues() {
        EnumMetadataResponseDTO response = service.getAllEnums();
        EnumMetadataDTO enumMetadata = response.getEnums().stream()
                .filter(e -> e.getName().equals("TransferStatus"))
                .findFirst()
                .orElseThrow();

        List<String> values = enumMetadata.getValues().stream()
                .map(EnumValueDTO::getValue)
                .collect(Collectors.toList());

        assertEquals(5, values.size());
        assertTrue(values.containsAll(List.of("PENDING", "APPROVED", "REJECTED", "COMPLETED", "CANCELLED")));
    }

    @Test
    void testMaintenanceTypeEnumValues() {
        EnumMetadataResponseDTO response = service.getAllEnums();
        EnumMetadataDTO enumMetadata = response.getEnums().stream()
                .filter(e -> e.getName().equals("MaintenanceType"))
                .findFirst()
                .orElseThrow();

        List<String> values = enumMetadata.getValues().stream()
                .map(EnumValueDTO::getValue)
                .collect(Collectors.toList());

        assertEquals(5, values.size());
        assertTrue(values.containsAll(List.of("SCHEDULED", "REPAIR", "INSPECTION", "CALIBRATION", "UPGRADE")));
    }
}
