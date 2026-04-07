package com.werkflow.business.finance.controller;

import com.werkflow.business.finance.dto.ExpenseRequest;
import com.werkflow.business.finance.dto.ExpenseResponse;
import com.werkflow.business.finance.service.ExpenseService;
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
@RequestMapping("/expenses")
@RequiredArgsConstructor
@Tag(name = "Expenses", description = "Expense tracking and approval endpoints")
public class ExpenseController {

    private final ExpenseService expenseService;

    @GetMapping
    @Operation(summary = "Get all expenses")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<ExpenseResponse>> getAllExpenses() {
        return ResponseEntity.ok(expenseService.getAllExpenses());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get expense by ID")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ExpenseResponse> getExpenseById(@PathVariable Long id) {
        return ResponseEntity.ok(expenseService.getExpenseById(id));
    }

    @PostMapping
    @Operation(summary = "Create new expense",
        description = "Supports idempotent creation via Idempotency-Key header. " +
            "Provide a unique idempotency key to safely retry failed requests without duplicating the resource. " +
            "If the key is omitted, each request is processed independently. " +
            "If the same key is used with different payloads, a 409 Conflict is returned.")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ExpenseResponse> createExpense(
            @Valid @RequestBody ExpenseRequest request,
            @RequestHeader(name = "Idempotency-Key", required = false) String idempotencyKey) {
        return ResponseEntity.status(HttpStatus.CREATED).body(expenseService.createExpense(request));
    }
}
