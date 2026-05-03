package com.werkflow.business.common.apikey;

import com.werkflow.business.common.apikey.dto.ApiKeyRegisterRequest;
import com.werkflow.business.common.apikey.dto.ApiKeyRegisterResponse;
import com.werkflow.business.common.apikey.entity.ApiKey;
import com.werkflow.business.common.apikey.repository.ApiKeyRepository;
import com.werkflow.business.common.context.TenantContext;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/v1/api-keys")
@RequiredArgsConstructor
public class ApiKeyController {

    private final ApiKeyRepository apiKeyRepository;
    private final TenantContext tenantContext;

    /**
     * Registers a pre-hashed API key for the current tenant.
     * Called by admin service during connector registration — the raw key never leaves the caller.
     * JWT auth required; tenantId extracted from TenantContext (set by TenantContextFilter).
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN', 'ENGINE_SERVICE')")
    public ResponseEntity<ApiKeyRegisterResponse> register(@Valid @RequestBody ApiKeyRegisterRequest request) {
        String tenantId = tenantContext.getTenantId();
        if (tenantId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tenant context not available");
        }

        if (apiKeyRepository.findByKeyHashAndActiveTrue(request.getKeyHash()).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "API key with this hash already exists");
        }

        ApiKey saved = apiKeyRepository.save(ApiKey.of(request.getKeyHash(), tenantId, request.getName()));

        return ResponseEntity.status(HttpStatus.CREATED).body(
            ApiKeyRegisterResponse.builder()
                .id(saved.getId())
                .name(saved.getName())
                .tenantId(tenantId)
                .createdAt(saved.getCreatedAt())
                .build()
        );
    }
}
