package com.werkflow.business.finance.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(
    description = "Expense request for submitting a new expense claim",
    example = "{\"budgetLineItemId\": 100, \"departmentId\": 5, \"expenseDate\": \"2026-04-01\", \"amount\": 150.00, \"categoryId\": 10, \"vendorName\": \"Acme Corp\", \"description\": \"Client meeting expenses\", \"submittedByUserId\": 20}"
)
public class ExpenseRequest {
    private Long budgetLineItemId;

    @NotNull(message = "Department ID is required")
    private Long departmentId;

    @NotNull(message = "Expense date is required")
    private LocalDate expenseDate;

    @NotNull(message = "Amount is required")
    @Positive(message = "Amount must be positive")
    private BigDecimal amount;

    @NotNull(message = "Category ID is required")
    private Long categoryId;

    @Size(max = 200)
    private String vendorName;

    @NotBlank(message = "Description is required")
    @Size(max = 1000)
    private String description;

    @Size(max = 500)
    private String receiptUrl;

    @NotNull(message = "Submitted by user ID is required")
    private Long submittedByUserId;

    private Map<String, Object> metadata;
}
