package com.werkflow.business.finance.service;

import com.werkflow.business.common.context.TenantContext;
import com.werkflow.business.common.context.UserContext;
import com.werkflow.business.finance.dto.BudgetPlanRequest;
import com.werkflow.business.finance.dto.BudgetPlanResponse;
import com.werkflow.business.finance.entity.BudgetPlan;
import com.werkflow.business.finance.repository.BudgetPlanRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class BudgetPlanService {
    private final BudgetPlanRepository budgetPlanRepository;
    private final TenantContext tenantContext;

    private String getTenantId() {
        return tenantContext.getTenantId();
    }

    @Transactional(readOnly = true)
    public Page<BudgetPlanResponse> getAllBudgetPlans(Pageable pageable) {
        String tenantId = getTenantId();
        log.debug("Fetching all budget plans for tenant: {}", tenantId);
        return budgetPlanRepository.findByTenantId(tenantId, pageable).map(this::toResponse);
    }

    @Deprecated
    @Transactional(readOnly = true)
    public List<BudgetPlanResponse> getAllBudgetPlansUnscoped() {
        return budgetPlanRepository.findAll().stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public BudgetPlanResponse getBudgetPlanById(Long id) {
        String tenantId = getTenantId();
        BudgetPlan plan = budgetPlanRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("BudgetPlan not found with id: " + id));
        if (!plan.getTenantId().equals(tenantId)) {
            throw new AccessDeniedException("Not authorized to access this BudgetPlan");
        }
        return toResponse(plan);
    }

    @Transactional
    public BudgetPlanResponse createBudgetPlan(BudgetPlanRequest request) {
        String tenantId = getTenantId();
        BudgetPlan plan = BudgetPlan.builder()
            .tenantId(tenantId)
            .departmentId(request.getDepartmentId())
            .fiscalYear(request.getFiscalYear())
            .periodStart(request.getPeriodStart())
            .periodEnd(request.getPeriodEnd())
            .totalAmount(request.getTotalAmount())
            .allocatedAmount(BigDecimal.ZERO)
            .spentAmount(BigDecimal.ZERO)
            .status(BudgetPlan.BudgetStatus.DRAFT)
            .createdByUserId(request.getCreatedByUserId())
            .notes(request.getNotes())
            .build();
        return toResponse(budgetPlanRepository.save(plan));
    }

    @Transactional
    public BudgetPlanResponse updateBudgetPlan(Long id, BudgetPlanRequest request) {
        String tenantId = getTenantId();
        log.debug("Updating budget plan with id: {} for tenant: {}", id, tenantId);
        BudgetPlan plan = budgetPlanRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("BudgetPlan not found with id: " + id));
        if (!plan.getTenantId().equals(tenantId)) {
            throw new AccessDeniedException("Not authorized to update this BudgetPlan");
        }

        plan.setDepartmentId(request.getDepartmentId());
        plan.setFiscalYear(request.getFiscalYear());
        plan.setPeriodStart(request.getPeriodStart());
        plan.setPeriodEnd(request.getPeriodEnd());
        plan.setTotalAmount(request.getTotalAmount());
        if (request.getStatus() != null) {
            plan.setStatus(request.getStatus());
        }
        plan.setNotes(request.getNotes());

        BudgetPlan updated = budgetPlanRepository.save(plan);
        log.info("Updated budget plan: {}", id);
        return toResponse(updated);
    }

    @Transactional
    public void deleteBudgetPlan(Long id) {
        String tenantId = getTenantId();
        log.debug("Deleting budget plan with id: {} for tenant: {}", id, tenantId);
        BudgetPlan plan = budgetPlanRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("BudgetPlan not found with id: " + id));
        if (!plan.getTenantId().equals(tenantId)) {
            throw new AccessDeniedException("Not authorized to delete this BudgetPlan");
        }
        budgetPlanRepository.deleteById(id);
        log.info("Deleted budget plan: {}", id);
    }

    private BudgetPlanResponse toResponse(BudgetPlan plan) {
        BudgetPlanResponse response = BudgetPlanResponse.builder()
            .id(plan.getId())
            .departmentId(plan.getDepartmentId())
            .fiscalYear(plan.getFiscalYear())
            .periodStart(plan.getPeriodStart())
            .periodEnd(plan.getPeriodEnd())
            .totalAmount(plan.getTotalAmount())
            .allocatedAmount(plan.getAllocatedAmount())
            .spentAmount(plan.getSpentAmount())
            .status(plan.getStatus())
            .createdByUserId(plan.getCreatedByUserId())
            .approvedByUserId(plan.getApprovedByUserId())
            .approvedDate(plan.getApprovedDate())
            .notes(plan.getNotes())
            .createdAt(plan.getCreatedAt())
            .updatedAt(plan.getUpdatedAt())
            .build();

        try {
            String displayName = UserContext.getDisplayName();
            response.setCreatedByDisplayName(displayName);
            response.setUpdatedByDisplayName(displayName);
        } catch (IllegalStateException e) {
            // UserContext not available — leave as null
        }

        return response;
    }
}
