package com.werkflow.business.common.meta;

import org.springframework.stereotype.Service;
import java.util.*;

/**
 * Service for providing metadata about all enums across the application.
 * Exposes information about enum values, labels, and descriptions for API consumers.
 */
@Service
public class EnumMetadataService {

    /**
     * Returns metadata for all enums in the application.
     *
     * @return EnumMetadataResponseDTO containing all enum metadata
     */
    public EnumMetadataResponseDTO getAllEnums() {
        List<EnumMetadataDTO> enums = new ArrayList<>();

        // HR Domain Enums
        enums.add(buildEmployeeStatusEnum());
        enums.add(buildLeaveTypeEnum());
        enums.add(buildAttendanceStatusEnum());
        enums.add(buildPerformanceRatingEnum());

        // Finance Domain Enums
        enums.add(buildBudgetStatusEnum());
        enums.add(buildExpenseStatusEnum());

        // Procurement Domain Enums
        enums.add(buildPrStatusEnum());
        enums.add(buildPoStatusEnum());
        enums.add(buildReceiptStatusEnum());
        enums.add(buildVendorStatusEnum());

        // Inventory Domain Enums
        enums.add(buildAssetRequestStatusEnum());
        enums.add(buildAssetConditionEnum());
        enums.add(buildAssetStatusEnum());
        enums.add(buildTransferStatusEnum());
        enums.add(buildMaintenanceTypeEnum());

        return EnumMetadataResponseDTO.builder()
                .enums(enums)
                .build();
    }

    // HR Domain Enums
    private EnumMetadataDTO buildEmployeeStatusEnum() {
        return EnumMetadataDTO.builder()
                .name("EmployeeStatus")
                .description("Represents the employment status of an employee")
                .values(Arrays.asList(
                        EnumValueDTO.builder().value("ACTIVE").label("Active").description("Employee is actively employed").build(),
                        EnumValueDTO.builder().value("ON_LEAVE").label("On Leave").description("Employee is currently on leave").build(),
                        EnumValueDTO.builder().value("SUSPENDED").label("Suspended").description("Employee employment is suspended").build(),
                        EnumValueDTO.builder().value("TERMINATED").label("Terminated").description("Employee has been terminated").build(),
                        EnumValueDTO.builder().value("RESIGNED").label("Resigned").description("Employee has resigned").build()
                ))
                .build();
    }

    private EnumMetadataDTO buildLeaveTypeEnum() {
        return EnumMetadataDTO.builder()
                .name("LeaveType")
                .description("Represents different types of leave available to employees")
                .values(Arrays.asList(
                        EnumValueDTO.builder().value("ANNUAL").label("Annual Leave").description("Annual paid leave").build(),
                        EnumValueDTO.builder().value("SICK").label("Sick Leave").description("Leave taken when employee is sick").build(),
                        EnumValueDTO.builder().value("MATERNITY").label("Maternity Leave").description("Maternity leave for female employees").build(),
                        EnumValueDTO.builder().value("PATERNITY").label("Paternity Leave").description("Paternity leave for male employees").build(),
                        EnumValueDTO.builder().value("UNPAID").label("Unpaid Leave").description("Unpaid leave of absence").build(),
                        EnumValueDTO.builder().value("COMPENSATORY").label("Compensatory Leave").description("Leave compensated for extra work hours").build(),
                        EnumValueDTO.builder().value("BEREAVEMENT").label("Bereavement Leave").description("Leave due to death in family").build(),
                        EnumValueDTO.builder().value("STUDY").label("Study Leave").description("Leave for educational pursuits").build()
                ))
                .build();
    }

    private EnumMetadataDTO buildAttendanceStatusEnum() {
        return EnumMetadataDTO.builder()
                .name("AttendanceStatus")
                .description("Represents the attendance status of an employee for a day")
                .values(Arrays.asList(
                        EnumValueDTO.builder().value("PRESENT").label("Present").description("Employee was present").build(),
                        EnumValueDTO.builder().value("ABSENT").label("Absent").description("Employee was absent").build(),
                        EnumValueDTO.builder().value("HALF_DAY").label("Half Day").description("Employee worked half day").build(),
                        EnumValueDTO.builder().value("ON_LEAVE").label("On Leave").description("Employee was on leave").build(),
                        EnumValueDTO.builder().value("HOLIDAY").label("Holiday").description("Day is a public holiday").build(),
                        EnumValueDTO.builder().value("WEEKEND").label("Weekend").description("Day is a weekend").build()
                ))
                .build();
    }

    private EnumMetadataDTO buildPerformanceRatingEnum() {
        return EnumMetadataDTO.builder()
                .name("PerformanceRating")
                .description("Represents the performance rating level for employees")
                .values(Arrays.asList(
                        EnumValueDTO.builder().value("OUTSTANDING").label("Outstanding").description("Exceeds all expectations significantly").build(),
                        EnumValueDTO.builder().value("EXCEEDS_EXPECTATIONS").label("Exceeds Expectations").description("Performance exceeds expectations").build(),
                        EnumValueDTO.builder().value("MEETS_EXPECTATIONS").label("Meets Expectations").description("Performance meets expected standards").build(),
                        EnumValueDTO.builder().value("NEEDS_IMPROVEMENT").label("Needs Improvement").description("Performance needs improvement").build(),
                        EnumValueDTO.builder().value("UNSATISFACTORY").label("Unsatisfactory").description("Performance is unsatisfactory").build()
                ))
                .build();
    }

    // Finance Domain Enums
    private EnumMetadataDTO buildBudgetStatusEnum() {
        return EnumMetadataDTO.builder()
                .name("BudgetStatus")
                .description("Represents the status of a budget plan")
                .values(Arrays.asList(
                        EnumValueDTO.builder().value("DRAFT").label("Draft").description("Budget is in draft state").build(),
                        EnumValueDTO.builder().value("PENDING_APPROVAL").label("Pending Approval").description("Budget awaiting approval").build(),
                        EnumValueDTO.builder().value("APPROVED").label("Approved").description("Budget has been approved").build(),
                        EnumValueDTO.builder().value("REJECTED").label("Rejected").description("Budget has been rejected").build(),
                        EnumValueDTO.builder().value("ACTIVE").label("Active").description("Budget is currently active").build(),
                        EnumValueDTO.builder().value("CLOSED").label("Closed").description("Budget period has closed").build()
                ))
                .build();
    }

    private EnumMetadataDTO buildExpenseStatusEnum() {
        return EnumMetadataDTO.builder()
                .name("ExpenseStatus")
                .description("Represents the status of an expense claim")
                .values(Arrays.asList(
                        EnumValueDTO.builder().value("PENDING").label("Pending").description("Expense claim is pending").build(),
                        EnumValueDTO.builder().value("SUBMITTED").label("Submitted").description("Expense claim has been submitted").build(),
                        EnumValueDTO.builder().value("APPROVED").label("Approved").description("Expense claim has been approved").build(),
                        EnumValueDTO.builder().value("REJECTED").label("Rejected").description("Expense claim has been rejected").build(),
                        EnumValueDTO.builder().value("PAID").label("Paid").description("Expense has been paid").build(),
                        EnumValueDTO.builder().value("CANCELLED").label("Cancelled").description("Expense claim has been cancelled").build()
                ))
                .build();
    }

    // Procurement Domain Enums
    private EnumMetadataDTO buildPrStatusEnum() {
        return EnumMetadataDTO.builder()
                .name("PrStatus")
                .description("Represents the status of a purchase request")
                .values(Arrays.asList(
                        EnumValueDTO.builder().value("DRAFT").label("Draft").description("Purchase request is in draft state").build(),
                        EnumValueDTO.builder().value("PENDING").label("Pending").description("Purchase request is pending").build(),
                        EnumValueDTO.builder().value("SUBMITTED").label("Submitted").description("Purchase request has been submitted").build(),
                        EnumValueDTO.builder().value("PENDING_APPROVAL").label("Pending Approval").description("Purchase request awaiting approval").build(),
                        EnumValueDTO.builder().value("APPROVED").label("Approved").description("Purchase request has been approved").build(),
                        EnumValueDTO.builder().value("REJECTED").label("Rejected").description("Purchase request has been rejected").build(),
                        EnumValueDTO.builder().value("ORDERED").label("Ordered").description("Purchase order has been created").build(),
                        EnumValueDTO.builder().value("RECEIVED").label("Received").description("Items have been received").build(),
                        EnumValueDTO.builder().value("CANCELLED").label("Cancelled").description("Purchase request has been cancelled").build()
                ))
                .build();
    }

    private EnumMetadataDTO buildPoStatusEnum() {
        return EnumMetadataDTO.builder()
                .name("PoStatus")
                .description("Represents the status of a purchase order")
                .values(Arrays.asList(
                        EnumValueDTO.builder().value("DRAFT").label("Draft").description("Purchase order is in draft state").build(),
                        EnumValueDTO.builder().value("PENDING").label("Pending").description("Purchase order is pending").build(),
                        EnumValueDTO.builder().value("CONFIRMED").label("Confirmed").description("Purchase order has been confirmed with vendor").build(),
                        EnumValueDTO.builder().value("SHIPPED").label("Shipped").description("Items have been shipped").build(),
                        EnumValueDTO.builder().value("DELIVERED").label("Delivered").description("Items have been delivered").build(),
                        EnumValueDTO.builder().value("CANCELLED").label("Cancelled").description("Purchase order has been cancelled").build()
                ))
                .build();
    }

    private EnumMetadataDTO buildReceiptStatusEnum() {
        return EnumMetadataDTO.builder()
                .name("ReceiptStatus")
                .description("Represents the status of a goods receipt")
                .values(Arrays.asList(
                        EnumValueDTO.builder().value("DRAFT").label("Draft").description("Receipt is in draft state").build(),
                        EnumValueDTO.builder().value("SUBMITTED").label("Submitted").description("Receipt has been submitted").build(),
                        EnumValueDTO.builder().value("APPROVED").label("Approved").description("Receipt has been approved").build(),
                        EnumValueDTO.builder().value("RECEIVED").label("Received").description("Goods have been received").build(),
                        EnumValueDTO.builder().value("DISCREPANCY").label("Discrepancy").description("Discrepancy found in receipt").build(),
                        EnumValueDTO.builder().value("REJECTED").label("Rejected").description("Receipt has been rejected").build(),
                        EnumValueDTO.builder().value("CANCELLED").label("Cancelled").description("Receipt has been cancelled").build()
                ))
                .build();
    }

    private EnumMetadataDTO buildVendorStatusEnum() {
        return EnumMetadataDTO.builder()
                .name("VendorStatus")
                .description("Represents the status of a vendor")
                .values(Arrays.asList(
                        EnumValueDTO.builder().value("ACTIVE").label("Active").description("Vendor is active").build(),
                        EnumValueDTO.builder().value("INACTIVE").label("Inactive").description("Vendor is inactive").build(),
                        EnumValueDTO.builder().value("BLACKLISTED").label("Blacklisted").description("Vendor is blacklisted").build()
                ))
                .build();
    }

    // Inventory Domain Enums
    private EnumMetadataDTO buildAssetRequestStatusEnum() {
        return EnumMetadataDTO.builder()
                .name("AssetRequestStatus")
                .description("Represents the status of an asset request")
                .values(Arrays.asList(
                        EnumValueDTO.builder().value("PENDING").label("Pending").description("Asset request is pending approval").build(),
                        EnumValueDTO.builder().value("APPROVED").label("Approved").description("Asset request has been approved").build(),
                        EnumValueDTO.builder().value("REJECTED").label("Rejected").description("Asset request has been rejected").build(),
                        EnumValueDTO.builder().value("FULFILLED").label("Fulfilled").description("Asset request has been fulfilled").build(),
                        EnumValueDTO.builder().value("PROCUREMENT_INITIATED").label("Procurement Initiated").description("Procurement process has started").build(),
                        EnumValueDTO.builder().value("CANCELLED").label("Cancelled").description("Asset request has been cancelled").build()
                ))
                .build();
    }

    private EnumMetadataDTO buildAssetConditionEnum() {
        return EnumMetadataDTO.builder()
                .name("AssetCondition")
                .description("Represents the condition of an asset")
                .values(Arrays.asList(
                        EnumValueDTO.builder().value("NEW").label("New").description("Asset is new").build(),
                        EnumValueDTO.builder().value("GOOD").label("Good").description("Asset is in good condition").build(),
                        EnumValueDTO.builder().value("FAIR").label("Fair").description("Asset is in fair condition").build(),
                        EnumValueDTO.builder().value("POOR").label("Poor").description("Asset is in poor condition").build(),
                        EnumValueDTO.builder().value("DAMAGED").label("Damaged").description("Asset is damaged").build(),
                        EnumValueDTO.builder().value("NEEDS_REPAIR").label("Needs Repair").description("Asset needs repair").build()
                ))
                .build();
    }

    private EnumMetadataDTO buildAssetStatusEnum() {
        return EnumMetadataDTO.builder()
                .name("AssetStatus")
                .description("Represents the status of an asset")
                .values(Arrays.asList(
                        EnumValueDTO.builder().value("AVAILABLE").label("Available").description("Asset is available for use").build(),
                        EnumValueDTO.builder().value("IN_USE").label("In Use").description("Asset is currently in use").build(),
                        EnumValueDTO.builder().value("MAINTENANCE").label("In Maintenance").description("Asset is undergoing maintenance").build(),
                        EnumValueDTO.builder().value("RETIRED").label("Retired").description("Asset has been retired").build(),
                        EnumValueDTO.builder().value("DISPOSED").label("Disposed").description("Asset has been disposed").build(),
                        EnumValueDTO.builder().value("LOST").label("Lost").description("Asset has been lost").build()
                ))
                .build();
    }

    private EnumMetadataDTO buildTransferStatusEnum() {
        return EnumMetadataDTO.builder()
                .name("TransferStatus")
                .description("Represents the status of an asset transfer request")
                .values(Arrays.asList(
                        EnumValueDTO.builder().value("PENDING").label("Pending").description("Transfer request is pending approval").build(),
                        EnumValueDTO.builder().value("APPROVED").label("Approved").description("Transfer request has been approved").build(),
                        EnumValueDTO.builder().value("REJECTED").label("Rejected").description("Transfer request has been rejected").build(),
                        EnumValueDTO.builder().value("COMPLETED").label("Completed").description("Transfer has been completed").build(),
                        EnumValueDTO.builder().value("CANCELLED").label("Cancelled").description("Transfer request has been cancelled").build()
                ))
                .build();
    }

    private EnumMetadataDTO buildMaintenanceTypeEnum() {
        return EnumMetadataDTO.builder()
                .name("MaintenanceType")
                .description("Represents the type of maintenance performed on an asset")
                .values(Arrays.asList(
                        EnumValueDTO.builder().value("SCHEDULED").label("Scheduled").description("Planned maintenance").build(),
                        EnumValueDTO.builder().value("REPAIR").label("Repair").description("Repair maintenance").build(),
                        EnumValueDTO.builder().value("INSPECTION").label("Inspection").description("Asset inspection").build(),
                        EnumValueDTO.builder().value("CALIBRATION").label("Calibration").description("Equipment calibration").build(),
                        EnumValueDTO.builder().value("UPGRADE").label("Upgrade").description("Asset upgrade").build()
                ))
                .build();
    }
}
