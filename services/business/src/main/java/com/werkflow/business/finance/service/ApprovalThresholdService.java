package com.werkflow.business.finance.service;

import com.werkflow.business.common.context.TenantContext;
import com.werkflow.business.finance.dto.ApprovalThresholdRequest;
import com.werkflow.business.finance.dto.ApprovalThresholdResponse;
import com.werkflow.business.finance.entity.ApprovalThreshold;
import com.werkflow.business.finance.repository.ApprovalThresholdRepository;
import com.werkflow.business.finance.repository.BudgetCategoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ApprovalThresholdService {
    private final ApprovalThresholdRepository thresholdRepository;
    private final BudgetCategoryRepository categoryRepository;
    private final TenantContext tenantContext;

    private String getTenantId() {
        return tenantContext.getTenantId();
    }

    @Transactional(readOnly = true)
    public List<ApprovalThresholdResponse> getAllThresholds() {
        String tenantId = getTenantId();
        log.debug("Fetching all approval thresholds for tenant: {}", tenantId);
        return thresholdRepository.findByTenantId(tenantId).stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Deprecated
    @Transactional(readOnly = true)
    public List<ApprovalThresholdResponse> getAllThresholdsUnscoped() {
        return thresholdRepository.findAll().stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Transactional
    public ApprovalThresholdResponse createThreshold(ApprovalThresholdRequest request) {
        ApprovalThreshold threshold = ApprovalThreshold.builder()
            .departmentId(request.getDepartmentId())
            .minAmount(request.getMinAmount())
            .maxAmount(request.getMaxAmount())
            .requiredRole(request.getRequiredRole())
            .description(request.getDescription())
            .active(request.getActive() != null ? request.getActive() : true)
            .build();

        if (request.getCategoryId() != null) {
            var category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new RuntimeException("Category not found"));
            threshold.setCategory(category);
        }

        return toResponse(thresholdRepository.save(threshold));
    }

    private ApprovalThresholdResponse toResponse(ApprovalThreshold threshold) {
        return ApprovalThresholdResponse.builder()
            .id(threshold.getId())
            .departmentId(threshold.getDepartmentId())
            .categoryId(threshold.getCategory() != null ? threshold.getCategory().getId() : null)
            .categoryName(threshold.getCategory() != null ? threshold.getCategory().getName() : null)
            .minAmount(threshold.getMinAmount())
            .maxAmount(threshold.getMaxAmount())
            .requiredRole(threshold.getRequiredRole())
            .description(threshold.getDescription())
            .active(threshold.getActive())
            .createdAt(threshold.getCreatedAt())
            .updatedAt(threshold.getUpdatedAt())
            .build();
    }
}
