package com.werkflow.business.common.identity;

import com.werkflow.business.common.context.TenantContext;
import com.werkflow.business.common.identity.dto.CustodyMappingRequest;
import com.werkflow.business.common.identity.dto.CustodyMappingResponse;
import com.werkflow.business.common.identity.entity.CustodyMapping;
import com.werkflow.business.common.identity.repository.CustodyMappingRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

/**
 * Service for tenant-scoped custody mapping CRUD.
 *
 * Tenant ID is always sourced from {@link TenantContext} — callers must not pass it
 * in the request body, preventing cross-tenant data leakage.
 */
@Service
@RequiredArgsConstructor
public class CustodyMappingService {

    private final CustodyMappingRepository repository;
    private final TenantContext tenantContext;

    /**
     * Lists all custody mappings for the current tenant, paginated and ordered by custodyOwner.
     */
    @Transactional(readOnly = true)
    public Page<CustodyMappingResponse> list(Pageable pageable) {
        String tenantId = tenantContext.getTenantId();
        return repository.findByTenantIdOrderByCustodyOwner(tenantId, pageable)
                .map(this::toResponse);
    }

    /**
     * Returns a single custody mapping by ID, scoped to the current tenant.
     *
     * @throws EntityNotFoundException if not found or belongs to a different tenant
     */
    @Transactional(readOnly = true)
    public CustodyMappingResponse getById(Long id) {
        return toResponse(findOwnedById(id));
    }

    /**
     * Creates a new custody mapping. Idempotent: if (tenantId, custodyOwner) already exists,
     * the existing record is updated instead (upsert semantics).
     */
    @Transactional
    public CustodyMappingResponse upsert(CustodyMappingRequest request) {
        String tenantId = tenantContext.getTenantId();
        String[] groups = request.candidateGroups().toArray(String[]::new);
        repository.upsert(tenantId, request.custodyOwner(), groups, LocalDateTime.now());
        CustodyMapping saved = repository
                .findByTenantIdAndCustodyOwner(tenantId, request.custodyOwner())
                .orElseThrow(() -> new IllegalStateException(
                        "Upsert succeeded but record not found for custodyOwner: " + request.custodyOwner()));
        return toResponse(saved);
    }

    /**
     * Updates an existing custody mapping by ID, scoped to the current tenant.
     *
     * @throws EntityNotFoundException if not found or belongs to a different tenant
     */
    @Transactional
    public CustodyMappingResponse update(Long id, CustodyMappingRequest request) {
        CustodyMapping mapping = findOwnedById(id);
        mapping.setCustodyOwner(request.custodyOwner());
        mapping.setCandidateGroups(request.candidateGroups().toArray(String[]::new));
        return toResponse(repository.save(mapping));
    }

    /**
     * Deletes a custody mapping by ID, scoped to the current tenant.
     *
     * @throws EntityNotFoundException if not found or belongs to a different tenant
     */
    @Transactional
    public void delete(Long id) {
        findOwnedById(id);
        repository.deleteById(id);
    }

    private CustodyMapping findOwnedById(Long id) {
        CustodyMapping mapping = repository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Custody mapping not found: " + id));
        if (!mapping.getTenantId().equals(tenantContext.getTenantId())) {
            throw new EntityNotFoundException("Custody mapping not found: " + id);
        }
        return mapping;
    }

    private CustodyMappingResponse toResponse(CustodyMapping mapping) {
        List<String> groups = mapping.getCandidateGroups() != null
                ? Arrays.asList(mapping.getCandidateGroups())
                : List.of();
        return CustodyMappingResponse.builder()
                .id(mapping.getId())
                .tenantId(mapping.getTenantId())
                .custodyOwner(mapping.getCustodyOwner())
                .candidateGroups(groups)
                .createdAt(mapping.getCreatedAt())
                .updatedAt(mapping.getUpdatedAt())
                .build();
    }
}
