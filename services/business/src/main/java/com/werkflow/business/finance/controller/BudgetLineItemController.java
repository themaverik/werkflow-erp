package com.werkflow.business.finance.controller;

import com.werkflow.business.finance.dto.BudgetLineItemRequest;
import com.werkflow.business.finance.dto.BudgetLineItemResponse;
import com.werkflow.business.finance.service.BudgetLineItemService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/budget-line-items")
@RequiredArgsConstructor
@Tag(name = "Budget Line Items", description = "Budget line item management endpoints")
public class BudgetLineItemController {

    private final BudgetLineItemService lineItemService;

    @GetMapping
    @Operation(summary = "Get line items by budget plan", parameters = {
        @Parameter(name = "page", description = "0-indexed page number"),
        @Parameter(name = "size", description = "Page size (max 1000)"),
        @Parameter(name = "sort", description = "Sort criteria (e.g., createdAt,desc)")
    })
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Page<BudgetLineItemResponse>> getLineItemsByBudgetPlan(
            @RequestParam Long budgetPlanId,
            @ParameterObject Pageable pageable) {
        return ResponseEntity.ok(lineItemService.getLineItemsByBudgetPlan(budgetPlanId, pageable));
    }

    @PostMapping
    @Operation(summary = "Create new budget line item",
        description = "Supports idempotent creation via Idempotency-Key header. " +
            "Provide a unique idempotency key to safely retry failed requests without duplicating the resource. " +
            "If the key is omitted, each request is processed independently. " +
            "If the same key is used with different payloads, a 409 Conflict is returned.")
    @PreAuthorize("hasAnyRole('FINANCE_MANAGER', 'ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<BudgetLineItemResponse> createLineItem(
            @Valid @RequestBody BudgetLineItemRequest request,
            @RequestHeader(name = "Idempotency-Key", required = false) String idempotencyKey) {
        return ResponseEntity.status(HttpStatus.CREATED).body(lineItemService.createLineItem(request));
    }
}
