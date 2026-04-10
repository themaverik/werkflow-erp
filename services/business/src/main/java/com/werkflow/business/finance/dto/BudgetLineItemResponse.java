package com.werkflow.business.finance.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BudgetLineItemResponse {
    private Long id;
    private Long budgetPlanId;
    private Long categoryId;
    private String categoryName;
    private String description;
    private BigDecimal allocatedAmount;
    private BigDecimal spentAmount;
    private String notes;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @Schema(example = "Jane Smith")
    private String createdByDisplayName;

    @Schema(example = "John Doe")
    private String updatedByDisplayName;
}
