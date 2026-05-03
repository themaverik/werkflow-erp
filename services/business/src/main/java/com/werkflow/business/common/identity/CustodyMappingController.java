package com.werkflow.business.common.identity;

import com.werkflow.business.common.identity.dto.CustodyMappingRequest;
import com.werkflow.business.common.identity.dto.CustodyMappingResponse;
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

/**
 * REST controller for tenant-scoped custody mapping management (ADR-004).
 *
 * Tenant context is resolved automatically from the JWT via {@link com.werkflow.business.common.context.TenantContext}.
 * POST is idempotent (upsert on custodyOwner key); PUT updates by database ID.
 */
@RestController
@RequestMapping("/custody-mappings")
@RequiredArgsConstructor
@Tag(name = "Custody Mappings", description = "Tenant-scoped custody group management (ADR-004)")
public class CustodyMappingController {

    private final CustodyMappingService custodyMappingService;

    @GetMapping
    @Operation(summary = "List custody mappings", description = "Paginated list of custody mappings for the current tenant.", parameters = {
        @Parameter(name = "page", description = "0-indexed page number"),
        @Parameter(name = "size", description = "Page size (max 1000)"),
        @Parameter(name = "sort", description = "Sort criteria (e.g., custodyOwner,asc)")
    })
    public ResponseEntity<Page<CustodyMappingResponse>> list(@ParameterObject Pageable pageable) {
        return ResponseEntity.ok(custodyMappingService.list(pageable));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get custody mapping by ID")
    public ResponseEntity<CustodyMappingResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(custodyMappingService.getById(id));
    }

    @PostMapping
    @Operation(
        summary = "Create or update custody mapping",
        description = "Idempotent upsert keyed on custodyOwner. Safe to call multiple times — creates on first call, updates on subsequent calls with the same custodyOwner."
    )
    public ResponseEntity<CustodyMappingResponse> upsert(@Valid @RequestBody CustodyMappingRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(custodyMappingService.upsert(request));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update custody mapping by ID", description = "Updates custodyOwner and candidateGroups for an existing record.")
    public ResponseEntity<CustodyMappingResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody CustodyMappingRequest request) {
        return ResponseEntity.ok(custodyMappingService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete custody mapping by ID")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        custodyMappingService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
