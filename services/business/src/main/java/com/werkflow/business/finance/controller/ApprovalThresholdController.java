package com.werkflow.business.finance.controller;

import com.werkflow.business.finance.dto.ApprovalThresholdRequest;
import com.werkflow.business.finance.dto.ApprovalThresholdResponse;
import com.werkflow.business.finance.service.ApprovalThresholdService;
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
@RequestMapping("/approval-thresholds")
@RequiredArgsConstructor
@Tag(name = "Approval Thresholds", description = "Approval threshold management endpoints")
public class ApprovalThresholdController {

    private final ApprovalThresholdService thresholdService;

    @GetMapping
    @Operation(summary = "Get all approval thresholds")
    @PreAuthorize("hasAnyRole('FINANCE_MANAGER', 'SUPER_ADMIN')")
    public ResponseEntity<List<ApprovalThresholdResponse>> getAllThresholds() {
        return ResponseEntity.ok(thresholdService.getAllThresholds());
    }

    @PostMapping
    @Operation(summary = "Create new approval threshold",
        description = "Supports idempotent creation via Idempotency-Key header. " +
            "Provide a unique idempotency key to safely retry failed requests without duplicating the resource. " +
            "If the key is omitted, each request is processed independently. " +
            "If the same key is used with different payloads, a 409 Conflict is returned.")
    @PreAuthorize("hasAnyRole('FINANCE_MANAGER', 'SUPER_ADMIN')")
    public ResponseEntity<ApprovalThresholdResponse> createThreshold(
            @Valid @RequestBody ApprovalThresholdRequest request,
            @RequestHeader(name = "Idempotency-Key", required = false) String idempotencyKey) {
        return ResponseEntity.status(HttpStatus.CREATED).body(thresholdService.createThreshold(request));
    }
}
