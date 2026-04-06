package com.werkflow.business.inventory.service;

import com.werkflow.business.common.context.TenantContext;
import com.werkflow.business.inventory.dto.AssetCategoryResponseDto;
import com.werkflow.business.inventory.entity.AssetCategory;
import com.werkflow.business.inventory.repository.AssetCategoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityNotFoundException;
import java.util.List;

/**
 * Service for AssetCategory operations.
 * All queries are tenant-scoped via TenantContext.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class AssetCategoryService {

    private final AssetCategoryRepository categoryRepository;
    private final TenantContext tenantContext;

    private String getTenantId() {
        return tenantContext.getTenantId();
    }

    /**
     * Create a new asset category
     */
    @Transactional
    public AssetCategory createCategory(AssetCategory category) {
        String tenantId = getTenantId();
        log.info("Creating new asset category: {} for tenant: {}", category.getName(), tenantId);

        if (category.getCode() != null) {
            if (categoryRepository.findByTenantIdAndCode(tenantId, category.getCode()).isPresent()) {
                throw new IllegalArgumentException("Category code already exists: " + category.getCode());
            }
        }

        category.setTenantId(tenantId);
        AssetCategory saved = categoryRepository.save(category);
        log.info("Asset category created with id: {} for tenant: {}", saved.getId(), tenantId);
        return saved;
    }

    /**
     * Get asset category by ID
     */
    public AssetCategory getCategoryById(Long id) {
        String tenantId = getTenantId();
        AssetCategory category = categoryRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Asset category not found with id: " + id));
        if (!category.getTenantId().equals(tenantId)) {
            throw new AccessDeniedException("Not authorized to access this AssetCategory");
        }
        return category;
    }

    /**
     * Get asset category by code
     */
    public AssetCategory getCategoryByCode(String code) {
        String tenantId = getTenantId();
        return categoryRepository.findByTenantIdAndCode(tenantId, code)
            .orElseThrow(() -> new EntityNotFoundException("Asset category not found with code: " + code));
    }

    /**
     * Get all active categories
     */
    public List<AssetCategory> getActiveCategories() {
        String tenantId = getTenantId();
        return categoryRepository.findByTenantIdAndActiveTrue(tenantId);
    }

    /**
     * Get active subcategories (leaf categories that have a parent)
     */
    public List<AssetCategory> getActiveSubcategories() {
        String tenantId = getTenantId();
        return categoryRepository.findByTenantIdAndParentCategoryIsNotNullAndActiveTrue(tenantId);
    }

    /**
     * Get all categories
     */
    public List<AssetCategory> getAllCategories() {
        String tenantId = getTenantId();
        return categoryRepository.findByTenantId(tenantId);
    }

    /**
     * Get root categories
     */
    public List<AssetCategory> getRootCategories() {
        String tenantId = getTenantId();
        return categoryRepository.findRootCategoriesForTenant(tenantId);
    }

    /**
     * Get child categories
     */
    public List<AssetCategory> getChildCategories(Long parentId) {
        String tenantId = getTenantId();
        return categoryRepository.findByTenantIdAndParentCategoryIdAndActiveTrue(tenantId, parentId);
    }

    /**
     * Update asset category
     */
    @Transactional
    public AssetCategory updateCategory(Long id, AssetCategory categoryDetails) {
        String tenantId = getTenantId();
        log.info("Updating asset category with id: {} for tenant: {}", id, tenantId);

        AssetCategory category = categoryRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Asset category not found with id: " + id));
        if (!category.getTenantId().equals(tenantId)) {
            throw new AccessDeniedException("Not authorized to update this AssetCategory");
        }

        category.setName(categoryDetails.getName());
        category.setCode(categoryDetails.getCode());
        category.setDescription(categoryDetails.getDescription());
        category.setRequiresApproval(categoryDetails.getRequiresApproval());
        category.setActive(categoryDetails.getActive());
        category.setCustodianDeptCode(categoryDetails.getCustodianDeptCode());
        category.setCustodianUserId(categoryDetails.getCustodianUserId());

        AssetCategory updated = categoryRepository.save(category);
        log.info("Asset category updated with id: {} for tenant: {}", id, tenantId);
        return updated;
    }

    /**
     * Deactivate category
     */
    @Transactional
    public AssetCategory deactivateCategory(Long id) {
        log.info("Deactivating asset category with id: {}", id);
        AssetCategory category = getCategoryById(id);
        category.setActive(false);
        return categoryRepository.save(category);
    }

    /**
     * Activate category
     */
    @Transactional
    public AssetCategory activateCategory(Long id) {
        log.info("Activating asset category with id: {}", id);
        AssetCategory category = getCategoryById(id);
        category.setActive(true);
        return categoryRepository.save(category);
    }

    /**
     * Delete category (soft-delete via deactivation if it has children).
     */
    @Transactional
    public void deleteCategory(Long id) {
        String tenantId = getTenantId();
        log.info("Deleting asset category with id: {} for tenant: {}", id, tenantId);

        AssetCategory category = categoryRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Asset category not found with id: " + id));
        if (!category.getTenantId().equals(tenantId)) {
            throw new AccessDeniedException("Not authorized to delete this AssetCategory");
        }

        if (category.getChildCategories() != null && !category.getChildCategories().isEmpty()) {
            throw new IllegalStateException("Cannot delete category with children. Deactivate it instead.");
        }

        categoryRepository.deleteById(id);
        log.info("Asset category deleted with id: {} for tenant: {}", id, tenantId);
    }

    /**
     * Search categories
     */
    public List<AssetCategory> searchCategories(String searchTerm) {
        String tenantId = getTenantId();
        return categoryRepository.searchCategoriesForTenant(tenantId, searchTerm);
    }

    /**
     * Get full category tree starting from root categories
     */
    public List<AssetCategoryResponseDto> getCategoryTree() {
        String tenantId = getTenantId();
        List<AssetCategory> roots = categoryRepository.findByTenantIdAndParentCategoryIsNull(tenantId);
        return roots.stream().map(this::toTreeDto).toList();
    }

    private AssetCategoryResponseDto toTreeDto(AssetCategory cat) {
        AssetCategoryResponseDto dto = convertToResponseDto(cat);
        if (cat.getChildCategories() != null && !cat.getChildCategories().isEmpty()) {
            dto.setChildren(cat.getChildCategories().stream().map(this::toTreeDto).toList());
        }
        return dto;
    }

    private AssetCategoryResponseDto convertToResponseDto(AssetCategory category) {
        return AssetCategoryResponseDto.builder()
            .id(category.getId())
            .name(category.getName())
            .code(category.getCode())
            .description(category.getDescription())
            .parentCategoryId(category.getParentCategory() != null ? category.getParentCategory().getId() : null)
            .custodianDeptCode(category.getCustodianDeptCode())
            .custodianUserId(category.getCustodianUserId())
            .requiresApproval(category.getRequiresApproval())
            .active(category.getActive())
            .createdAt(category.getCreatedAt())
            .updatedAt(category.getUpdatedAt())
            .build();
    }
}
