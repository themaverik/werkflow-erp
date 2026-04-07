package com.werkflow.business.procurement.controller;

import com.werkflow.business.procurement.dto.VendorRequest;
import com.werkflow.business.procurement.dto.VendorResponse;
import com.werkflow.business.procurement.service.VendorService;
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
@RequestMapping("/vendors")
@RequiredArgsConstructor
@Tag(name = "Vendors", description = "Vendor management endpoints")
public class VendorController {

    private final VendorService vendorService;

    @GetMapping
    @Operation(summary = "Get all vendors")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Page<VendorResponse>> getAllVendors(
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(vendorService.getAllVendors(pageable));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get vendor by ID")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<VendorResponse> getVendorById(@PathVariable Long id) {
        return ResponseEntity.ok(vendorService.getVendorById(id));
    }

    @PostMapping
    @Operation(
        summary = "Create new vendor",
        description = "Supports idempotent creation via Idempotency-Key header. " +
            "Provide a unique idempotency key to safely retry failed requests without duplicating the resource. " +
            "If the key is omitted, each request is processed independently. " +
            "If the same key is used with different payloads, a 409 Conflict is returned."
    )
    @PreAuthorize("hasAnyRole('PROCUREMENT_MANAGER', 'ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<VendorResponse> createVendor(
        @Valid @RequestBody VendorRequest request,
        @RequestHeader(name = "Idempotency-Key", required = false) String idempotencyKey) {
        return ResponseEntity.status(HttpStatus.CREATED).body(vendorService.createVendor(request));
    }
}
