package com.werkflow.business.finance.service;

import com.werkflow.business.common.context.TenantContext;
import com.werkflow.business.finance.dto.BudgetLineItemRequest;
import com.werkflow.business.finance.dto.BudgetLineItemResponse;
import com.werkflow.business.finance.entity.BudgetLineItem;
import com.werkflow.business.finance.entity.BudgetPlan;
import com.werkflow.business.finance.repository.BudgetLineItemRepository;
import com.werkflow.business.finance.repository.BudgetPlanRepository;
import com.werkflow.business.finance.repository.BudgetCategoryRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class BudgetLineItemService {
    private final BudgetLineItemRepository lineItemRepository;
    private final BudgetPlanRepository budgetPlanRepository;
    private final BudgetCategoryRepository categoryRepository;
    private final TenantContext tenantContext;

    private String getTenantId() {
        return tenantContext.getTenantId();
    }

    @Transactional(readOnly = true)
    public List<BudgetLineItemResponse> getAllLineItems() {
        String tenantId = getTenantId();
        log.debug("Fetching all budget line items for tenant: {}", tenantId);
        return lineItemRepository.findByTenantId(tenantId).stream()
            .map(this::toResponse).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<BudgetLineItemResponse> getLineItemsByBudgetPlan(Long budgetPlanId) {
        String tenantId = getTenantId();
        BudgetPlan plan = budgetPlanRepository.findById(budgetPlanId)
            .orElseThrow(() -> new EntityNotFoundException("BudgetPlan not found with id: " + budgetPlanId));
        if (!plan.getTenantId().equals(tenantId)) {
            throw new AccessDeniedException("Not authorized to access this BudgetPlan");
        }
        return lineItemRepository.findByBudgetPlanIdAndTenantId(budgetPlanId, tenantId).stream()
            .map(this::toResponse).collect(Collectors.toList());
    }

    @Transactional
    public BudgetLineItemResponse createLineItem(BudgetLineItemRequest request) {
        String tenantId = getTenantId();
        var budgetPlan = budgetPlanRepository.findById(request.getBudgetPlanId())
            .orElseThrow(() -> new EntityNotFoundException("BudgetPlan not found with id: " + request.getBudgetPlanId()));
        if (!budgetPlan.getTenantId().equals(tenantId)) {
            throw new AccessDeniedException("Not authorized to access this BudgetPlan");
        }
        var category = categoryRepository.findById(request.getCategoryId())
            .orElseThrow(() -> new EntityNotFoundException("BudgetCategory not found with id: " + request.getCategoryId()));
        if (!category.getTenantId().equals(tenantId)) {
            throw new AccessDeniedException("Not authorized to access this BudgetCategory");
        }

        BudgetLineItem lineItem = BudgetLineItem.builder()
            .tenantId(tenantId)
            .budgetPlan(budgetPlan)
            .category(category)
            .description(request.getDescription())
            .allocatedAmount(request.getAllocatedAmount())
            .spentAmount(BigDecimal.ZERO)
            .notes(request.getNotes())
            .build();
        return toResponse(lineItemRepository.save(lineItem));
    }

    private BudgetLineItemResponse toResponse(BudgetLineItem item) {
        return BudgetLineItemResponse.builder()
            .id(item.getId())
            .budgetPlanId(item.getBudgetPlan().getId())
            .categoryId(item.getCategory().getId())
            .categoryName(item.getCategory().getName())
            .description(item.getDescription())
            .allocatedAmount(item.getAllocatedAmount())
            .spentAmount(item.getSpentAmount())
            .notes(item.getNotes())
            .createdAt(item.getCreatedAt())
            .updatedAt(item.getUpdatedAt())
            .build();
    }
}
