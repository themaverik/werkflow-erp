package com.werkflow.business.common.validator;

import com.werkflow.business.hr.repository.DepartmentRepository;
import com.werkflow.business.finance.repository.BudgetCategoryRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Validates cross-domain foreign key references.
 * All validations are tenant-scoped: FK must exist and belong to the current tenant.
 *
 * <p>Validators are added incrementally as services need them. Each validator
 * checks existence and tenant isolation, throwing EntityNotFoundException if invalid.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CrossDomainValidator {

    private final DepartmentRepository departmentRepository;
    private final BudgetCategoryRepository budgetCategoryRepository;

    /**
     * Validates that a department exists and belongs to the specified tenant.
     *
     * @param departmentId Department ID from HR domain (can be null for optional FKs)
     * @param tenantId Tenant ID from TenantContext
     * @throws EntityNotFoundException if department not found or wrong tenant
     */
    public void validateDepartmentExists(Long departmentId, String tenantId) {
        if (departmentId == null) {
            return; // Optional FK
        }

        departmentRepository
            .findByIdAndTenantId(departmentId, tenantId)
            .orElseThrow(() -> new EntityNotFoundException(
                String.format("Department not found: id=%d for tenant=%s", departmentId, tenantId)));

        log.debug("Department validation passed: id={}, tenant={}", departmentId, tenantId);
    }

    /**
     * Validates that a budget category exists and belongs to the specified tenant.
     *
     * @param categoryId Budget category ID from Finance domain (can be null for optional FKs)
     * @param tenantId Tenant ID from TenantContext
     * @throws EntityNotFoundException if category not found or wrong tenant
     */
    public void validateBudgetCategoryExists(Long categoryId, String tenantId) {
        if (categoryId == null) {
            return; // Optional FK
        }

        budgetCategoryRepository
            .findByIdAndTenantId(categoryId, tenantId)
            .orElseThrow(() -> new EntityNotFoundException(
                String.format("Budget category not found: id=%d for tenant=%s", categoryId, tenantId)));

        log.debug("Budget category validation passed: id={}, tenant={}", categoryId, tenantId);
    }

    // TODO: Add validators as needed during P0.4 and beyond
    // public void validateVendorExists(Long vendorId, String tenantId) { ... }
    // public void validateEmployeeExists(Long employeeId, String tenantId) { ... }
    // public void validateAssetCategoryExists(Long categoryId, String tenantId) { ... }
    // public void validateAssetDefinitionExists(Long defId, String tenantId) { ... }
    // public void validateBudgetPlanExists(Long planId, String tenantId) { ... }
}
