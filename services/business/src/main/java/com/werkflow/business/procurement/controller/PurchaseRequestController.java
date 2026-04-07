package com.werkflow.business.procurement.controller;

import com.werkflow.business.procurement.dto.PurchaseRequestRequest;
import com.werkflow.business.procurement.dto.PurchaseRequestResponse;
import com.werkflow.business.procurement.service.PurchaseRequestService;
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

@RestController
@RequestMapping("/purchase-requests")
@RequiredArgsConstructor
@Tag(name = "Purchase Requests", description = "Purchase request management endpoints")
public class PurchaseRequestController {

    private final PurchaseRequestService prService;

    @GetMapping
    @Operation(summary = "Get all purchase requests", parameters = {
        @Parameter(name = "page", description = "0-indexed page number"),
        @Parameter(name = "size", description = "Page size (max 1000)"),
        @Parameter(name = "sort", description = "Sort criteria (e.g., createdAt,desc)")
    })
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Page<PurchaseRequestResponse>> getAllPurchaseRequests(
            @ParameterObject Pageable pageable) {
        return ResponseEntity.ok(prService.getAllPurchaseRequests(pageable));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get purchase request by ID")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<PurchaseRequestResponse> getPurchaseRequestById(@PathVariable Long id) {
        return ResponseEntity.ok(prService.getPurchaseRequestById(id));
    }

    @PostMapping
    @Operation(
        summary = "Create new purchase request",
        description = "Supports idempotent creation via Idempotency-Key header. " +
            "Provide a unique idempotency key to safely retry failed requests without duplicating the resource. " +
            "If the key is omitted, each request is processed independently. " +
            "If the same key is used with different payloads, a 409 Conflict is returned."
    )
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<PurchaseRequestResponse> createPurchaseRequest(
        @Valid @RequestBody PurchaseRequestRequest request,
        @RequestHeader(name = "Idempotency-Key", required = false) String idempotencyKey) {
        return ResponseEntity.status(HttpStatus.CREATED).body(prService.createPurchaseRequest(request));
    }
}
