package com.werkflow.business.hr.controller;

import com.werkflow.business.hr.dto.AttendanceRequest;
import com.werkflow.business.hr.dto.AttendanceResponse;
import com.werkflow.business.hr.service.AttendanceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/attendances")
@RequiredArgsConstructor
@Tag(name = "Attendances", description = "Attendance management APIs")
public class AttendanceController {

    private final AttendanceService attendanceService;

    @GetMapping
    @Operation(summary = "Get all attendances", description = "Retrieve all attendance records", parameters = {
        @Parameter(name = "page", description = "0-indexed page number"),
        @Parameter(name = "size", description = "Page size (max 1000)"),
        @Parameter(name = "sort", description = "Sort criteria (e.g., createdAt,desc)")
    })
    public ResponseEntity<Page<AttendanceResponse>> getAllAttendances(
            @ParameterObject Pageable pageable) {
        return ResponseEntity.ok(attendanceService.getAllAttendances(pageable));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get attendance by ID", description = "Retrieve an attendance record by ID")
    public ResponseEntity<AttendanceResponse> getAttendanceById(@PathVariable Long id) {
        return ResponseEntity.ok(attendanceService.getAttendanceById(id));
    }

    @GetMapping("/employee/{employeeId}")
    @Operation(summary = "Get attendances by employee", description = "Retrieve all attendances for an employee")
    public ResponseEntity<Page<AttendanceResponse>> getAttendancesByEmployee(
            @PathVariable Long employeeId,
            @ParameterObject Pageable pageable) {
        return ResponseEntity.ok(attendanceService.getAttendancesByEmployee(employeeId, pageable));
    }

    @GetMapping("/employee/{employeeId}/range")
    @Operation(summary = "Get attendances by date range", description = "Retrieve attendances for an employee within a date range")
    public ResponseEntity<Page<AttendanceResponse>> getAttendancesByDateRange(
            @PathVariable Long employeeId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @ParameterObject Pageable pageable) {
        return ResponseEntity.ok(attendanceService.getAttendancesByDateRange(employeeId, startDate, endDate, pageable));
    }

    @PostMapping
    @Operation(summary = "Create attendance", description = "Supports idempotent creation via Idempotency-Key header. " +
        "Provide a unique idempotency key to safely retry failed requests without duplicating the resource. " +
        "If the key is omitted, each request is processed independently. " +
        "If the same key is used with different payloads, a 409 Conflict is returned.")
    public ResponseEntity<AttendanceResponse> createAttendance(
            @Valid @RequestBody AttendanceRequest request,
            @RequestHeader(name = "Idempotency-Key", required = false) String idempotencyKey) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(attendanceService.createAttendance(request));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update attendance", description = "Update an attendance record")
    public ResponseEntity<AttendanceResponse> updateAttendance(
            @PathVariable Long id,
            @Valid @RequestBody AttendanceRequest request) {
        return ResponseEntity.ok(attendanceService.updateAttendance(id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete attendance", description = "Delete an attendance record")
    public ResponseEntity<Void> deleteAttendance(@PathVariable Long id) {
        attendanceService.deleteAttendance(id);
        return ResponseEntity.noContent().build();
    }
}
