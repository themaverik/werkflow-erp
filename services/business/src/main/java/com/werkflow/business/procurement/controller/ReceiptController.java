package com.werkflow.business.procurement.controller;

import com.werkflow.business.procurement.dto.ReceiptRequest;
import com.werkflow.business.procurement.dto.ReceiptResponse;
import com.werkflow.business.procurement.service.ReceiptService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/receipts")
@RequiredArgsConstructor
@Tag(name = "Receipts", description = "Goods receipt management endpoints")
public class ReceiptController {

    private final ReceiptService receiptService;

    @GetMapping
    @Operation(summary = "Get all receipts")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Page<ReceiptResponse>> getAllReceipts(
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(receiptService.getAllReceipts(pageable));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get receipt by ID")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ReceiptResponse> getReceiptById(@PathVariable Long id) {
        return ResponseEntity.ok(receiptService.getReceiptById(id));
    }

    @PostMapping
    @Operation(
        summary = "Create new receipt",
        description = "Supports idempotent creation via Idempotency-Key header. " +
            "Provide a unique idempotency key to safely retry failed requests without duplicating the resource. " +
            "If the key is omitted, each request is processed independently. " +
            "If the same key is used with different payloads, a 409 Conflict is returned."
    )
    @PreAuthorize("hasAnyRole('WAREHOUSE_STAFF', 'PROCUREMENT_MANAGER', 'ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<ReceiptResponse> createReceipt(
        @Valid @RequestBody ReceiptRequest request,
        @RequestHeader(name = "Idempotency-Key", required = false) String idempotencyKey) {
        return ResponseEntity.status(HttpStatus.CREATED).body(receiptService.createReceipt(request));
    }
}
