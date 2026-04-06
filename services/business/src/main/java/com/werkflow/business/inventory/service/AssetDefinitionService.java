package com.werkflow.business.inventory.service;

import com.werkflow.business.common.context.TenantContext;
import com.werkflow.business.inventory.entity.AssetDefinition;
import com.werkflow.business.inventory.repository.AssetCategoryRepository;
import com.werkflow.business.inventory.repository.AssetDefinitionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityNotFoundException;
import java.math.BigDecimal;
import java.util.List;

/**
 * Service for AssetDefinition operations.
 * All queries are tenant-scoped via TenantContext.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class AssetDefinitionService {

    private final AssetDefinitionRepository definitionRepository;
    private final AssetCategoryRepository categoryRepository;
    private final TenantContext tenantContext;

    private String getTenantId() {
        return tenantContext.getTenantId();
    }

    /**
     * Create a new asset definition
     */
    @Transactional
    public AssetDefinition createDefinition(AssetDefinition definition) {
        String tenantId = getTenantId();
        log.info("Creating new asset definition: {} for tenant: {}", definition.getName(), tenantId);

        if (definitionRepository.findByTenantIdAndSku(tenantId, definition.getSku()).isPresent()) {
            throw new IllegalArgumentException("Asset definition with SKU already exists: " + definition.getSku());
        }

        // Validate that the category belongs to the same tenant
        if (definition.getCategory() != null && definition.getCategory().getId() != null) {
            categoryRepository.findById(definition.getCategory().getId())
                .filter(cat -> cat.getTenantId().equals(tenantId))
                .orElseThrow(() -> new AccessDeniedException("Category does not belong to the current tenant"));
        }

        definition.setTenantId(tenantId);
        AssetDefinition saved = definitionRepository.save(definition);
        log.info("Asset definition created with id: {} for tenant: {}", saved.getId(), tenantId);
        return saved;
    }

    /**
     * Get asset definition by ID
     */
    public AssetDefinition getDefinitionById(Long id) {
        String tenantId = getTenantId();
        AssetDefinition definition = definitionRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Asset definition not found with id: " + id));
        if (!definition.getTenantId().equals(tenantId)) {
            throw new AccessDeniedException("Not authorized to access this AssetDefinition");
        }
        return definition;
    }

    /**
     * Get asset definition by SKU
     */
    public AssetDefinition getDefinitionBySku(String sku) {
        String tenantId = getTenantId();
        return definitionRepository.findByTenantIdAndSku(tenantId, sku)
            .orElseThrow(() -> new EntityNotFoundException("Asset definition not found with SKU: " + sku));
    }

    /**
     * Get asset definitions by category
     */
    public List<AssetDefinition> getDefinitionsByCategory(Long categoryId) {
        String tenantId = getTenantId();
        return definitionRepository.findByTenantIdAndCategoryIdAndActiveTrue(tenantId, categoryId);
    }

    /**
     * Get all active asset definitions
     */
    public List<AssetDefinition> getActiveDefinitions() {
        String tenantId = getTenantId();
        return definitionRepository.findByTenantIdAndActiveTrue(tenantId);
    }

    /**
     * Get all asset definitions
     */
    public List<AssetDefinition> getAllDefinitions() {
        String tenantId = getTenantId();
        return definitionRepository.findByTenantId(tenantId);
    }

    /**
     * Get asset definitions requiring maintenance
     */
    public List<AssetDefinition> getDefinitionsRequiringMaintenance() {
        String tenantId = getTenantId();
        return definitionRepository.findByTenantIdAndRequiresMaintenanceTrueAndActiveTrue(tenantId);
    }

    /**
     * Get asset definitions by manufacturer
     */
    public List<AssetDefinition> getDefinitionsByManufacturer(String manufacturer) {
        String tenantId = getTenantId();
        return definitionRepository.findByTenantIdAndManufacturerAndActiveTrue(tenantId, manufacturer);
    }

    /**
     * Get asset definitions by price range
     */
    public List<AssetDefinition> getDefinitionsByPriceRange(BigDecimal minPrice, BigDecimal maxPrice) {
        String tenantId = getTenantId();
        return definitionRepository.findByPriceRangeForTenant(tenantId, minPrice, maxPrice);
    }

    /**
     * Update asset definition
     */
    @Transactional
    public AssetDefinition updateDefinition(Long id, AssetDefinition definitionDetails) {
        String tenantId = getTenantId();
        log.info("Updating asset definition with id: {} for tenant: {}", id, tenantId);

        AssetDefinition definition = definitionRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Asset definition not found with id: " + id));
        if (!definition.getTenantId().equals(tenantId)) {
            throw new AccessDeniedException("Not authorized to update this AssetDefinition");
        }

        // Validate SKU uniqueness if changed
        if (!definition.getSku().equals(definitionDetails.getSku())) {
            if (definitionRepository.findByTenantIdAndSku(tenantId, definitionDetails.getSku()).isPresent()) {
                throw new IllegalArgumentException("Asset definition with SKU already exists: " + definitionDetails.getSku());
            }
        }

        definition.setSku(definitionDetails.getSku());
        definition.setName(definitionDetails.getName());
        definition.setManufacturer(definitionDetails.getManufacturer());
        definition.setModel(definitionDetails.getModel());
        if (definitionDetails.getItemType() != null) {
            definition.setItemType(definitionDetails.getItemType());
        }
        definition.setSpecifications(definitionDetails.getSpecifications());
        definition.setUnitCost(definitionDetails.getUnitCost());
        definition.setExpectedLifespanMonths(definitionDetails.getExpectedLifespanMonths());
        definition.setRequiresMaintenance(definitionDetails.getRequiresMaintenance());
        definition.setMaintenanceIntervalMonths(definitionDetails.getMaintenanceIntervalMonths());
        definition.setActive(definitionDetails.getActive());

        if (definitionDetails.getCategory() != null) {
            // Validate the new category belongs to the same tenant
            categoryRepository.findById(definitionDetails.getCategory().getId())
                .filter(cat -> cat.getTenantId().equals(tenantId))
                .orElseThrow(() -> new AccessDeniedException("Category does not belong to the current tenant"));
            definition.setCategory(definitionDetails.getCategory());
        }

        AssetDefinition updated = definitionRepository.save(definition);
        log.info("Asset definition updated with id: {} for tenant: {}", id, tenantId);
        return updated;
    }

    /**
     * Deactivate asset definition
     */
    @Transactional
    public AssetDefinition deactivateDefinition(Long id) {
        log.info("Deactivating asset definition with id: {}", id);
        AssetDefinition definition = getDefinitionById(id);
        definition.setActive(false);
        return definitionRepository.save(definition);
    }

    /**
     * Activate asset definition
     */
    @Transactional
    public AssetDefinition activateDefinition(Long id) {
        log.info("Activating asset definition with id: {}", id);
        AssetDefinition definition = getDefinitionById(id);
        definition.setActive(true);
        return definitionRepository.save(definition);
    }

    /**
     * Search asset definitions
     */
    public List<AssetDefinition> searchDefinitions(String searchTerm) {
        String tenantId = getTenantId();
        return definitionRepository.searchDefinitionsForTenant(tenantId, searchTerm);
    }
}
