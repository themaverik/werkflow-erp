package com.werkflow.business.finance.controller;

import com.werkflow.business.finance.dto.BudgetPlanRequest;
import com.werkflow.business.finance.dto.BudgetPlanResponse;
import com.werkflow.business.finance.service.BudgetPlanService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/budgets")
@RequiredArgsConstructor
@Tag(name = "Budget Plans", description = "Budget plan management endpoints")
public class BudgetPlanController {

    private final BudgetPlanService budgetPlanService;

    @GetMapping
    @Operation(summary = "Get all budget plans")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<BudgetPlanResponse>> getAllBudgetPlans() {
        return ResponseEntity.ok(budgetPlanService.getAllBudgetPlans());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get budget plan by ID")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<BudgetPlanResponse> getBudgetPlanById(@PathVariable Long id) {
        return ResponseEntity.ok(budgetPlanService.getBudgetPlanById(id));
    }

    @PostMapping
    @Operation(summary = "Create new budget plan",
        description = "Supports idempotent creation via Idempotency-Key header. " +
            "Provide a unique idempotency key to safely retry failed requests without duplicating the resource. " +
            "If the key is omitted, each request is processed independently. " +
            "If the same key is used with different payloads, a 409 Conflict is returned.")
    @PreAuthorize("hasAnyRole('FINANCE_MANAGER', 'ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<BudgetPlanResponse> createBudgetPlan(
            @Valid @RequestBody BudgetPlanRequest request,
            @RequestHeader(name = "Idempotency-Key", required = false) String idempotencyKey) {
        return ResponseEntity.status(HttpStatus.CREATED).body(budgetPlanService.createBudgetPlan(request));
    }
}
