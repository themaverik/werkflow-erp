package com.werkflow.business.hr.dto;

import com.werkflow.business.hr.entity.LeaveType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * DTO for Leave creation and update requests
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(
    description = "Leave request for creating or updating employee leave",
    example = "{\"employeeId\": 1, \"leaveType\": \"ANNUAL\", \"startDate\": \"2026-04-15\", \"endDate\": \"2026-04-20\", \"numberOfDays\": 5, \"reason\": \"Planned vacation\"}"
)
public class LeaveRequest {

    @NotNull(message = "Employee ID is required")
    private Long employeeId;

    @NotNull(message = "Leave type is required")
    private LeaveType leaveType;

    @NotNull(message = "Start date is required")
    private LocalDate startDate;

    @NotNull(message = "End date is required")
    private LocalDate endDate;

    @NotNull(message = "Number of days is required")
    @Min(value = 1, message = "Number of days must be at least 1")
    private Integer numberOfDays;

    @Size(max = 1000, message = "Reason cannot exceed 1000 characters")
    private String reason;
}
