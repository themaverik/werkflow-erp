package com.werkflow.business.common.validator;

import com.werkflow.business.hr.entity.Department;
import com.werkflow.business.hr.repository.DepartmentRepository;
import com.werkflow.business.finance.entity.BudgetCategory;
import com.werkflow.business.finance.repository.BudgetCategoryRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CrossDomainValidatorTest {

    @Mock
    private DepartmentRepository departmentRepository;

    @Mock
    private BudgetCategoryRepository budgetCategoryRepository;

    @InjectMocks
    private CrossDomainValidator validator;

    private static final String TENANT_ID = "ACME";
    private static final Long DEPT_ID = 1L;
    private static final Long CATEGORY_ID = 10L;

    @Test
    void validateDepartmentExists_withValidDepartment_succeeds() {
        Department dept = new Department();
        when(departmentRepository.findByIdAndTenantId(DEPT_ID, TENANT_ID))
                .thenReturn(Optional.of(dept));

        assertDoesNotThrow(() -> validator.validateDepartmentExists(DEPT_ID, TENANT_ID));
        verify(departmentRepository).findByIdAndTenantId(DEPT_ID, TENANT_ID);
    }

    @Test
    void validateDepartmentExists_withNullId_succeeds() {
        assertDoesNotThrow(() -> validator.validateDepartmentExists(null, TENANT_ID));
        verifyNoInteractions(departmentRepository);
    }

    @Test
    void validateDepartmentExists_withNonExistentDepartment_throwsException() {
        when(departmentRepository.findByIdAndTenantId(DEPT_ID, TENANT_ID))
                .thenReturn(Optional.empty());

        EntityNotFoundException ex = assertThrows(EntityNotFoundException.class,
                () -> validator.validateDepartmentExists(DEPT_ID, TENANT_ID));

        assertTrue(ex.getMessage().contains("Department not found"));
        assertTrue(ex.getMessage().contains(String.valueOf(DEPT_ID)));
        assertTrue(ex.getMessage().contains(TENANT_ID));
    }

    @Test
    void validateDepartmentExists_withWrongTenant_throwsException() {
        String wrongTenant = "OTHER";
        when(departmentRepository.findByIdAndTenantId(DEPT_ID, wrongTenant))
                .thenReturn(Optional.empty());

        EntityNotFoundException ex = assertThrows(EntityNotFoundException.class,
                () -> validator.validateDepartmentExists(DEPT_ID, wrongTenant));

        assertTrue(ex.getMessage().contains("Department not found"));
        assertTrue(ex.getMessage().contains(wrongTenant));
    }

    @Test
    void validateBudgetCategoryExists_withValidCategory_succeeds() {
        BudgetCategory category = new BudgetCategory();
        when(budgetCategoryRepository.findByIdAndTenantId(CATEGORY_ID, TENANT_ID))
                .thenReturn(Optional.of(category));

        assertDoesNotThrow(() -> validator.validateBudgetCategoryExists(CATEGORY_ID, TENANT_ID));
        verify(budgetCategoryRepository).findByIdAndTenantId(CATEGORY_ID, TENANT_ID);
    }

    @Test
    void validateBudgetCategoryExists_withNullId_succeeds() {
        assertDoesNotThrow(() -> validator.validateBudgetCategoryExists(null, TENANT_ID));
        verifyNoInteractions(budgetCategoryRepository);
    }

    @Test
    void validateBudgetCategoryExists_withNonExistentCategory_throwsException() {
        when(budgetCategoryRepository.findByIdAndTenantId(CATEGORY_ID, TENANT_ID))
                .thenReturn(Optional.empty());

        EntityNotFoundException ex = assertThrows(EntityNotFoundException.class,
                () -> validator.validateBudgetCategoryExists(CATEGORY_ID, TENANT_ID));

        assertTrue(ex.getMessage().contains("Budget category not found"));
        assertTrue(ex.getMessage().contains(String.valueOf(CATEGORY_ID)));
        assertTrue(ex.getMessage().contains(TENANT_ID));
    }
}
