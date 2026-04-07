package com.werkflow.business.procurement.controller;

import com.werkflow.business.procurement.dto.PurchaseOrderRequest;
import com.werkflow.business.procurement.dto.PurchaseOrderResponse;
import com.werkflow.business.procurement.service.PurchaseOrderService;
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
@RequestMapping("/purchase-orders")
@RequiredArgsConstructor
@Tag(name = "Purchase Orders", description = "Purchase order management endpoints")
public class PurchaseOrderController {

    private final PurchaseOrderService poService;

    @GetMapping
    @Operation(summary = "Get all purchase orders")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Page<PurchaseOrderResponse>> getAllPurchaseOrders(
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(poService.getAllPurchaseOrders(pageable));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get purchase order by ID")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<PurchaseOrderResponse> getPurchaseOrderById(@PathVariable Long id) {
        return ResponseEntity.ok(poService.getPurchaseOrderById(id));
    }

    @PostMapping
    @Operation(
        summary = "Create new purchase order",
        description = "Supports idempotent creation via Idempotency-Key header. " +
            "Provide a unique idempotency key to safely retry failed requests without duplicating the resource. " +
            "If the key is omitted, each request is processed independently. " +
            "If the same key is used with different payloads, a 409 Conflict is returned."
    )
    @PreAuthorize("hasAnyRole('PROCUREMENT_MANAGER', 'ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<PurchaseOrderResponse> createPurchaseOrder(
        @Valid @RequestBody PurchaseOrderRequest request,
        @RequestHeader(name = "Idempotency-Key", required = false) String idempotencyKey) {
        return ResponseEntity.status(HttpStatus.CREATED).body(poService.createPurchaseOrder(request));
    }
}
