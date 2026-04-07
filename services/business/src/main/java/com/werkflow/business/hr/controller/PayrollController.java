package com.werkflow.business.hr.controller;

import com.werkflow.business.hr.dto.PayrollRequest;
import com.werkflow.business.hr.dto.PayrollResponse;
import com.werkflow.business.hr.service.PayrollService;
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
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/payrolls")
@RequiredArgsConstructor
@Tag(name = "Payrolls", description = "Payroll management APIs")
public class PayrollController {

    private final PayrollService payrollService;

    @GetMapping
    @Operation(summary = "Get all payrolls", description = "Retrieve all payroll records", parameters = {
        @Parameter(name = "page", description = "0-indexed page number"),
        @Parameter(name = "size", description = "Page size (max 1000)"),
        @Parameter(name = "sort", description = "Sort criteria (e.g., createdAt,desc)")
    })
    public ResponseEntity<Page<PayrollResponse>> getAllPayrolls(
            @ParameterObject Pageable pageable) {
        return ResponseEntity.ok(payrollService.getAllPayrolls(pageable));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get payroll by ID", description = "Retrieve a payroll record by ID")
    public ResponseEntity<PayrollResponse> getPayrollById(@PathVariable Long id) {
        return ResponseEntity.ok(payrollService.getPayrollById(id));
    }

    @GetMapping("/employee/{employeeId}")
    @Operation(summary = "Get payrolls by employee", description = "Retrieve all payroll records for an employee")
    public ResponseEntity<Page<PayrollResponse>> getPayrollsByEmployee(
            @PathVariable Long employeeId,
            @ParameterObject Pageable pageable) {
        return ResponseEntity.ok(payrollService.getPayrollsByEmployee(employeeId, pageable));
    }

    @GetMapping("/period")
    @Operation(summary = "Get payrolls by period", description = "Retrieve payrolls for a specific month and year")
    public ResponseEntity<Page<PayrollResponse>> getPayrollsByMonthYear(
            @RequestParam Integer month,
            @RequestParam Integer year,
            @ParameterObject Pageable pageable) {
        return ResponseEntity.ok(payrollService.getPayrollsByMonthYear(month, year, pageable));
    }

    @PostMapping
    @Operation(summary = "Create payroll", description = "Supports idempotent creation via Idempotency-Key header. " +
        "Provide a unique idempotency key to safely retry failed requests without duplicating the resource. " +
        "If the key is omitted, each request is processed independently. " +
        "If the same key is used with different payloads, a 409 Conflict is returned.")
    public ResponseEntity<PayrollResponse> createPayroll(
            @Valid @RequestBody PayrollRequest request,
            @RequestHeader(name = "Idempotency-Key", required = false) String idempotencyKey) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(payrollService.createPayroll(request));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update payroll", description = "Update a payroll record")
    public ResponseEntity<PayrollResponse> updatePayroll(
            @PathVariable Long id,
            @Valid @RequestBody PayrollRequest request) {
        return ResponseEntity.ok(payrollService.updatePayroll(id, request));
    }

    @PutMapping("/{id}/mark-paid")
    @Operation(summary = "Mark payroll as paid", description = "Mark a payroll record as paid")
    public ResponseEntity<PayrollResponse> markAsPaid(@PathVariable Long id) {
        return ResponseEntity.ok(payrollService.markAsPaid(id));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete payroll", description = "Delete a payroll record")
    public ResponseEntity<Void> deletePayroll(@PathVariable Long id) {
        payrollService.deletePayroll(id);
        return ResponseEntity.noContent().build();
    }
}
