package com.werkflow.business.inventory.service;

import com.werkflow.business.common.context.TenantContext;
import com.werkflow.business.inventory.entity.AssetDefinition;
import com.werkflow.business.inventory.entity.AssetInstance;
import com.werkflow.business.inventory.repository.AssetDefinitionRepository;
import com.werkflow.business.inventory.repository.AssetInstanceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityNotFoundException;
import java.time.LocalDate;
import java.util.List;

/**
 * Service for AssetInstance operations.
 * All queries are tenant-scoped via TenantContext.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class AssetInstanceService {

    private final AssetInstanceRepository instanceRepository;
    private final AssetDefinitionRepository definitionRepository;
    private final TenantContext tenantContext;

    private String getTenantId() {
        return tenantContext.getTenantId();
    }

    /**
     * Create a new asset instance
     */
    @Transactional
    public AssetInstance createInstance(AssetInstance instance) {
        String tenantId = getTenantId();
        log.info("Creating new asset instance with tag: {} for tenant: {}", instance.getAssetTag(), tenantId);

        if (instanceRepository.findByTenantIdAndAssetTag(tenantId, instance.getAssetTag()).isPresent()) {
            throw new IllegalArgumentException("Asset instance with tag already exists: " + instance.getAssetTag());
        }

        // Validate that the asset definition belongs to the same tenant
        if (instance.getAssetDefinition() != null && instance.getAssetDefinition().getId() != null) {
            definitionRepository.findById(instance.getAssetDefinition().getId())
                .filter(def -> def.getTenantId().equals(tenantId))
                .orElseThrow(() -> new AccessDeniedException("AssetDefinition does not belong to the current tenant"));
        }

        instance.setTenantId(tenantId);
        AssetInstance saved = instanceRepository.save(instance);
        log.info("Asset instance created with id: {} for tenant: {}", saved.getId(), tenantId);
        return saved;
    }

    /**
     * Get asset instance by ID
     */
    public AssetInstance getInstanceById(Long id) {
        String tenantId = getTenantId();
        AssetInstance instance = instanceRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Asset instance not found with id: " + id));
        if (!instance.getTenantId().equals(tenantId)) {
            throw new AccessDeniedException("Not authorized to access this AssetInstance");
        }
        return instance;
    }

    /**
     * Get asset instance by asset tag
     */
    public AssetInstance getInstanceByAssetTag(String assetTag) {
        String tenantId = getTenantId();
        return instanceRepository.findByTenantIdAndAssetTag(tenantId, assetTag)
            .orElseThrow(() -> new EntityNotFoundException("Asset instance not found with tag: " + assetTag));
    }

    /**
     * Get asset instances by asset definition
     */
    public List<AssetInstance> getInstancesByDefinition(Long definitionId) {
        String tenantId = getTenantId();
        // Validate definition belongs to this tenant before querying instances
        definitionRepository.findById(definitionId)
            .filter(def -> def.getTenantId().equals(tenantId))
            .orElseThrow(() -> new EntityNotFoundException("Asset definition not found with id: " + definitionId));
        return instanceRepository.findByAssetDefinitionIdAndTenantId(definitionId, tenantId);
    }

    /**
     * Get asset instances by status
     */
    public List<AssetInstance> getInstancesByStatus(String status) {
        String tenantId = getTenantId();
        return instanceRepository.findByTenantIdAndStatus(tenantId, status);
    }

    /**
     * Get available assets
     */
    public List<AssetInstance> getAvailableAssets() {
        String tenantId = getTenantId();
        return instanceRepository.findAvailableAssetsForTenant(tenantId);
    }

    /**
     * Get assets in use
     */
    public List<AssetInstance> getAssetsInUse() {
        String tenantId = getTenantId();
        return instanceRepository.findAssetsInUseForTenant(tenantId);
    }

    /**
     * Get assets requiring maintenance
     */
    public List<AssetInstance> getAssetsRequiringMaintenance() {
        String tenantId = getTenantId();
        return instanceRepository.findAssetsRequiringMaintenanceForTenant(tenantId);
    }

    /**
     * Get assets with warranty expiring soon
     */
    public List<AssetInstance> getAssetsWithExpiringWarranty(LocalDate expiryDate) {
        String tenantId = getTenantId();
        return instanceRepository.findAssetsWithExpiringWarrantyForTenant(tenantId, expiryDate);
    }

    /**
     * Get all asset instances
     */
    public List<AssetInstance> getAllInstances() {
        String tenantId = getTenantId();
        return instanceRepository.findByTenantId(tenantId);
    }

    /**
     * Update asset instance
     */
    @Transactional
    public AssetInstance updateInstance(Long id, AssetInstance instanceDetails) {
        String tenantId = getTenantId();
        log.info("Updating asset instance with id: {} for tenant: {}", id, tenantId);

        AssetInstance instance = instanceRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Asset instance not found with id: " + id));
        if (!instance.getTenantId().equals(tenantId)) {
            throw new AccessDeniedException("Not authorized to update this AssetInstance");
        }

        // Validate asset tag uniqueness if changed
        if (!instance.getAssetTag().equals(instanceDetails.getAssetTag())) {
            if (instanceRepository.findByTenantIdAndAssetTag(tenantId, instanceDetails.getAssetTag()).isPresent()) {
                throw new IllegalArgumentException("Asset instance with tag already exists: " + instanceDetails.getAssetTag());
            }
        }

        instance.setAssetTag(instanceDetails.getAssetTag());
        instance.setSerialNumber(instanceDetails.getSerialNumber());
        instance.setPurchaseDate(instanceDetails.getPurchaseDate());
        instance.setPurchaseCost(instanceDetails.getPurchaseCost());
        instance.setWarrantyExpiryDate(instanceDetails.getWarrantyExpiryDate());
        instance.setCondition(instanceDetails.getCondition());
        instance.setStatus(instanceDetails.getStatus());
        instance.setCurrentLocation(instanceDetails.getCurrentLocation());
        instance.setNotes(instanceDetails.getNotes());
        instance.setMetadata(instanceDetails.getMetadata());

        if (instanceDetails.getAssetDefinition() != null) {
            // Validate that the new definition belongs to the same tenant
            definitionRepository.findById(instanceDetails.getAssetDefinition().getId())
                .filter(def -> def.getTenantId().equals(tenantId))
                .orElseThrow(() -> new AccessDeniedException("AssetDefinition does not belong to the current tenant"));
            instance.setAssetDefinition(instanceDetails.getAssetDefinition());
        }

        AssetInstance updated = instanceRepository.save(instance);
        log.info("Asset instance updated with id: {} for tenant: {}", id, tenantId);
        return updated;
    }

    /**
     * Update asset status
     */
    @Transactional
    public AssetInstance updateStatus(Long id, String status) {
        log.info("Updating asset instance status to: {}", status);
        AssetInstance instance = getInstanceById(id);
        instance.setStatus(AssetInstance.AssetStatus.valueOf(status));
        return instanceRepository.save(instance);
    }

    /**
     * Search asset instances
     */
    public List<AssetInstance> searchInstances(String searchTerm) {
        String tenantId = getTenantId();
        return instanceRepository.searchAssetsForTenant(tenantId, searchTerm);
    }

    /**
     * Get assets needing attention
     */
    public List<AssetInstance> getAssetsNeedingAttention() {
        String tenantId = getTenantId();
        return instanceRepository.findAssetsNeedingAttentionForTenant(tenantId);
    }
}
