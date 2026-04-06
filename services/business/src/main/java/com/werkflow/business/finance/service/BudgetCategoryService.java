package com.werkflow.business.finance.service;

import com.werkflow.business.common.context.TenantContext;
import com.werkflow.business.finance.dto.BudgetCategoryRequest;
import com.werkflow.business.finance.dto.BudgetCategoryResponse;
import com.werkflow.business.finance.entity.BudgetCategory;
import com.werkflow.business.finance.repository.BudgetCategoryRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class BudgetCategoryService {

    private final BudgetCategoryRepository categoryRepository;
    private final TenantContext tenantContext;

    private String getTenantId() {
        return tenantContext.getTenantId();
    }

    @Transactional(readOnly = true)
    public List<BudgetCategoryResponse> getAllCategories() {
        String tenantId = getTenantId();
        log.debug("Fetching all budget categories for tenant: {}", tenantId);
        return categoryRepository.findByTenantId(tenantId).stream()
            .map(this::toResponse)
            .collect(Collectors.toList());
    }

    @Deprecated
    @Transactional(readOnly = true)
    public List<BudgetCategoryResponse> getAllCategoriesUnscoped() {
        return categoryRepository.findAll().stream()
            .map(this::toResponse)
            .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public BudgetCategoryResponse getCategoryById(Long id) {
        String tenantId = getTenantId();
        BudgetCategory category = categoryRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("BudgetCategory not found with id: " + id));
        if (!category.getTenantId().equals(tenantId)) {
            throw new AccessDeniedException("Not authorized to access this BudgetCategory");
        }
        return toResponse(category);
    }

    @Transactional
    public BudgetCategoryResponse createCategory(BudgetCategoryRequest request) {
        String tenantId = getTenantId();
        if (request.getCode() != null && categoryRepository.existsByCode(request.getCode())) {
            throw new RuntimeException("Category with code already exists: " + request.getCode());
        }

        BudgetCategory category = BudgetCategory.builder()
            .tenantId(tenantId)
            .name(request.getName())
            .code(request.getCode())
            .description(request.getDescription())
            .active(request.getActive() != null ? request.getActive() : true)
            .build();

        if (request.getParentCategoryId() != null) {
            BudgetCategory parent = categoryRepository.findById(request.getParentCategoryId())
                .orElseThrow(() -> new EntityNotFoundException("Parent category not found"));
            category.setParentCategory(parent);
        }

        BudgetCategory saved = categoryRepository.save(category);
        log.info("Created budget category: {}", saved.getId());
        return toResponse(saved);
    }

    @Transactional
    public BudgetCategoryResponse updateCategory(Long id, BudgetCategoryRequest request) {
        String tenantId = getTenantId();
        BudgetCategory category = categoryRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("BudgetCategory not found with id: " + id));
        if (!category.getTenantId().equals(tenantId)) {
            throw new AccessDeniedException("Not authorized to access this BudgetCategory");
        }

        category.setName(request.getName());
        category.setDescription(request.getDescription());
        if (request.getActive() != null) {
            category.setActive(request.getActive());
        }

        BudgetCategory updated = categoryRepository.save(category);
        log.info("Updated budget category: {}", id);
        return toResponse(updated);
    }

    @Transactional
    public void deleteCategory(Long id) {
        String tenantId = getTenantId();
        BudgetCategory category = categoryRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("BudgetCategory not found with id: " + id));
        if (!category.getTenantId().equals(tenantId)) {
            throw new AccessDeniedException("Not authorized to access this BudgetCategory");
        }
        categoryRepository.deleteById(id);
        log.info("Deleted budget category: {}", id);
    }

    private BudgetCategoryResponse toResponse(BudgetCategory category) {
        return BudgetCategoryResponse.builder()
            .id(category.getId())
            .parentCategoryId(category.getParentCategory() != null ? category.getParentCategory().getId() : null)
            .name(category.getName())
            .code(category.getCode())
            .description(category.getDescription())
            .active(category.getActive())
            .createdAt(category.getCreatedAt())
            .updatedAt(category.getUpdatedAt())
            .build();
    }
}
