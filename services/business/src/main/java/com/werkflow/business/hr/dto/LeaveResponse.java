package com.werkflow.business.hr.dto;

import com.werkflow.business.hr.entity.LeaveStatus;
import com.werkflow.business.hr.entity.LeaveType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(
    description = "Leave response with full details",
    example = "{\"id\": 101, \"employeeId\": 1, \"employeeName\": \"John Doe\", \"leaveType\": \"ANNUAL\", \"startDate\": \"2026-04-15\", \"endDate\": \"2026-04-20\", \"numberOfDays\": 5, \"reason\": \"Planned vacation\", \"status\": \"PENDING\", \"createdAt\": \"2026-04-01T10:00:00Z\", \"updatedAt\": \"2026-04-01T10:00:00Z\"}"
)
public class LeaveResponse {
    private Long id;
    private Long employeeId;
    private String employeeName;
    private LeaveType leaveType;
    private LocalDate startDate;
    private LocalDate endDate;
    private Integer numberOfDays;
    private String reason;
    private LeaveStatus status;
    private Long approvedById;
    private String approvedByName;
    private LocalDate approvedAt;
    private String approvalRemarks;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
