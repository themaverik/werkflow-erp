package com.werkflow.business.finance.dto;

import com.werkflow.business.finance.entity.Expense.ExpenseStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(
    description = "Expense response with complete expense details and status",
    example = "{\"id\": 201, \"budgetLineItemId\": 100, \"departmentId\": 5, \"expenseDate\": \"2026-04-01\", \"amount\": 150.00, \"categoryId\": 10, \"categoryName\": \"Travel\", \"vendorName\": \"Acme Corp\", \"description\": \"Client meeting expenses\", \"status\": \"APPROVED\", \"submittedByUserId\": 20, \"approvedByUserId\": 15, \"approvedDate\": \"2026-04-03T15:30:00Z\", \"createdAt\": \"2026-04-01T10:00:00Z\", \"updatedAt\": \"2026-04-03T15:30:00Z\"}"
)
public class ExpenseResponse {
    private Long id;
    private Long budgetLineItemId;
    private Long departmentId;
    private LocalDate expenseDate;
    private BigDecimal amount;
    private Long categoryId;
    private String categoryName;
    private String vendorName;
    private String description;
    private String receiptUrl;
    private ExpenseStatus status;
    private Long submittedByUserId;
    private Long approvedByUserId;
    private LocalDateTime approvedDate;
    private String processInstanceId;
    private String rejectionReason;
    private Map<String, Object> metadata;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @Schema(example = "Jane Smith")
    private String createdByDisplayName;

    @Schema(example = "John Doe")
    private String updatedByDisplayName;
}
