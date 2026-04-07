package com.werkflow.business.hr.controller;

import com.werkflow.business.hr.dto.LeaveRequest;
import com.werkflow.business.hr.dto.LeaveResponse;
import com.werkflow.business.hr.entity.LeaveStatus;
import com.werkflow.business.hr.service.LeaveService;
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
@RequestMapping("/leaves")
@RequiredArgsConstructor
@Tag(name = "Leaves", description = "Leave management APIs")
public class LeaveController {

    private final LeaveService leaveService;

    @GetMapping
    @Operation(summary = "Get all leaves", description = "Retrieve all leave requests", parameters = {
        @Parameter(name = "page", description = "0-indexed page number"),
        @Parameter(name = "size", description = "Page size (max 1000)"),
        @Parameter(name = "sort", description = "Sort criteria (e.g., createdAt,desc)")
    })
    public ResponseEntity<Page<LeaveResponse>> getAllLeaves(
            @ParameterObject Pageable pageable) {
        return ResponseEntity.ok(leaveService.getAllLeaves(pageable));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get leave by ID", description = "Retrieve a leave request by ID")
    public ResponseEntity<LeaveResponse> getLeaveById(@PathVariable Long id) {
        return ResponseEntity.ok(leaveService.getLeaveById(id));
    }

    @GetMapping("/employee/{employeeId}")
    @Operation(summary = "Get leaves by employee", description = "Retrieve all leaves for an employee")
    public ResponseEntity<Page<LeaveResponse>> getLeavesByEmployee(
            @PathVariable Long employeeId,
            @ParameterObject Pageable pageable) {
        return ResponseEntity.ok(leaveService.getLeavesByEmployee(employeeId, pageable));
    }

    @GetMapping("/status/{status}")
    @Operation(summary = "Get leaves by status", description = "Retrieve leaves by status")
    public ResponseEntity<Page<LeaveResponse>> getLeavesByStatus(
            @PathVariable LeaveStatus status,
            @ParameterObject Pageable pageable) {
        return ResponseEntity.ok(leaveService.getLeavesByStatus(status, pageable));
    }

    @PostMapping
    @Operation(summary = "Create leave request", description = "Supports idempotent creation via Idempotency-Key header. " +
        "Provide a unique idempotency key to safely retry failed requests without duplicating the resource. " +
        "If the key is omitted, each request is processed independently. " +
        "If the same key is used with different payloads, a 409 Conflict is returned.")
    public ResponseEntity<LeaveResponse> createLeave(
            @Valid @RequestBody LeaveRequest request,
            @RequestHeader(name = "Idempotency-Key", required = false) String idempotencyKey) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(leaveService.createLeave(request));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update leave", description = "Update a leave request")
    public ResponseEntity<LeaveResponse> updateLeave(
            @PathVariable Long id,
            @Valid @RequestBody LeaveRequest request) {
        return ResponseEntity.ok(leaveService.updateLeave(id, request));
    }

    @PutMapping("/{id}/approve")
    @Operation(summary = "Approve leave", description = "Approve a leave request")
    public ResponseEntity<LeaveResponse> approveLeave(
            @PathVariable Long id,
            @RequestParam Long approverId,
            @RequestParam(required = false) String remarks) {
        return ResponseEntity.ok(leaveService.approveLeave(id, approverId, remarks));
    }

    @PutMapping("/{id}/reject")
    @Operation(summary = "Reject leave", description = "Reject a leave request")
    public ResponseEntity<LeaveResponse> rejectLeave(
            @PathVariable Long id,
            @RequestParam Long approverId,
            @RequestParam(required = false) String remarks) {
        return ResponseEntity.ok(leaveService.rejectLeave(id, approverId, remarks));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete leave", description = "Delete a leave request")
    public ResponseEntity<Void> deleteLeave(@PathVariable Long id) {
        leaveService.deleteLeave(id);
        return ResponseEntity.noContent().build();
    }
}
