package com.werkflow.business.finance.service;

import com.werkflow.business.common.context.TenantContext;
import com.werkflow.business.finance.dto.BudgetCheckRequest;
import com.werkflow.business.finance.dto.BudgetCheckResponse;
import com.werkflow.business.finance.entity.BudgetPlan;
import com.werkflow.business.finance.repository.BudgetPlanRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@DisplayName("BudgetCheckService Contract Tests")
class BudgetCheckServiceContractTest {

    @Mock
    private BudgetPlanRepository budgetPlanRepository;

    @Mock
    private TenantContext tenantContext;

    private BudgetCheckService budgetCheckService;
    private static final String TENANT_ID = "ACME";
    private static final Long DEPARTMENT_ID = 1L;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        budgetCheckService = new BudgetCheckService(budgetPlanRepository, tenantContext);
        when(tenantContext.getTenantId()).thenReturn(TENANT_ID);
    }

    @Test
    @DisplayName("Contract: No budget plan found → unavailable with zero amounts")
    void testCheckBudgetNoPlanFound() {
        // Arrange: no budget plan exists
        BudgetCheckRequest request = BudgetCheckRequest.builder()
            .departmentId(DEPARTMENT_ID)
            .amount(new BigDecimal("5000.00"))
            .costCenter("CC-001")
            .fiscalYear(2026)
            .build();

        when(budgetPlanRepository.findByDepartmentIdAndFiscalYearAndTenantId(
            eq(DEPARTMENT_ID), eq(2026), eq(TENANT_ID)))
            .thenReturn(Optional.empty());

        // Act
        BudgetCheckResponse response = budgetCheckService.checkBudgetAvailability(request);

        // Assert: contract is "no plan = not available"
        assertFalse(response.isAvailable());
        assertTrue(response.getReason().contains("No budget plan found"));
        assertEquals(BigDecimal.ZERO, response.getAvailableAmount());
        assertEquals(BigDecimal.ZERO, response.getAllocatedAmount());
        assertEquals(new BigDecimal("5000.00"), response.getRequestedAmount());
    }

    @Test
    @DisplayName("Contract: Sufficient budget → available")
    void testCheckBudgetSufficientFunds() {
        // Arrange: allocated 100k, spent 20k, request 50k → available 80k
        BudgetPlan budgetPlan = BudgetPlan.builder()
            .allocatedAmount(new BigDecimal("100000.00"))
            .spentAmount(new BigDecimal("20000.00"))
            .build();

        BudgetCheckRequest request = BudgetCheckRequest.builder()
            .departmentId(DEPARTMENT_ID)
            .amount(new BigDecimal("50000.00"))
            .costCenter("CC-001")
            .fiscalYear(2026)
            .build();

        when(budgetPlanRepository.findByDepartmentIdAndFiscalYearAndTenantId(
            eq(DEPARTMENT_ID), eq(2026), eq(TENANT_ID)))
            .thenReturn(Optional.of(budgetPlan));

        // Act
        BudgetCheckResponse response = budgetCheckService.checkBudgetAvailability(request);

        // Assert: contract is "available >= requested"
        assertTrue(response.isAvailable());
        assertEquals(new BigDecimal("80000.00"), response.getAvailableAmount());
        assertEquals(new BigDecimal("100000.00"), response.getAllocatedAmount());
        assertEquals(new BigDecimal("20000.00"), response.getUtilizedAmount());
    }

    @Test
    @DisplayName("Contract: Insufficient budget → unavailable with reason")
    void testCheckBudgetInsufficientFunds() {
        // Arrange: allocated 50k, spent 40k, available 10k, request 15k
        BudgetPlan budgetPlan = BudgetPlan.builder()
            .allocatedAmount(new BigDecimal("50000.00"))
            .spentAmount(new BigDecimal("40000.00"))
            .build();

        BudgetCheckRequest request = BudgetCheckRequest.builder()
            .departmentId(DEPARTMENT_ID)
            .amount(new BigDecimal("15000.00"))
            .costCenter("CC-002")
            .fiscalYear(2026)
            .build();

        when(budgetPlanRepository.findByDepartmentIdAndFiscalYearAndTenantId(
            eq(DEPARTMENT_ID), eq(2026), eq(TENANT_ID)))
            .thenReturn(Optional.of(budgetPlan));

        // Act
        BudgetCheckResponse response = budgetCheckService.checkBudgetAvailability(request);

        // Assert: contract is "insufficient budget fails with details"
        assertFalse(response.isAvailable());
        assertEquals(new BigDecimal("10000.00"), response.getAvailableAmount());
        assertTrue(response.getReason().contains("Insufficient budget"));
        assertTrue(response.getReason().contains("10000"));
        assertTrue(response.getReason().contains("15000"));
    }

    @Test
    @DisplayName("Contract: Fiscal year defaults to current year")
    void testCheckBudgetDefaultsCurrentYear() {
        // Arrange: no fiscal year provided
        int currentYear = LocalDate.now().getYear();
        BudgetPlan budgetPlan = BudgetPlan.builder()
            .fiscalYear(currentYear)
            .allocatedAmount(new BigDecimal("75000.00"))
            .spentAmount(new BigDecimal("25000.00"))
            .build();

        BudgetCheckRequest request = BudgetCheckRequest.builder()
            .departmentId(DEPARTMENT_ID)
            .amount(new BigDecimal("30000.00"))
            .costCenter("CC-003")
            .fiscalYear(null) // no fiscal year
            .build();

        when(budgetPlanRepository.findByDepartmentIdAndFiscalYearAndTenantId(
            eq(DEPARTMENT_ID), eq(currentYear), eq(TENANT_ID)))
            .thenReturn(Optional.of(budgetPlan));

        // Act
        BudgetCheckResponse response = budgetCheckService.checkBudgetAvailability(request);

        // Assert: contract is "fiscal year defaults"
        assertTrue(response.isAvailable());
        assertEquals(currentYear, response.getFiscalYear());
        assertEquals(new BigDecimal("50000.00"), response.getAvailableAmount());
    }

    @Test
    @DisplayName("Contract: Exact match (available == requested) → available")
    void testCheckBudgetExactMatch() {
        // Arrange: allocated 100k, spent 70k, available 30k, request 30k
        BudgetPlan budgetPlan = BudgetPlan.builder()
            .allocatedAmount(new BigDecimal("100000.00"))
            .spentAmount(new BigDecimal("70000.00"))
            .build();

        BudgetCheckRequest request = BudgetCheckRequest.builder()
            .departmentId(DEPARTMENT_ID)
            .amount(new BigDecimal("30000.00"))
            .costCenter("CC-005")
            .fiscalYear(2026)
            .build();

        when(budgetPlanRepository.findByDepartmentIdAndFiscalYearAndTenantId(
            eq(DEPARTMENT_ID), eq(2026), eq(TENANT_ID)))
            .thenReturn(Optional.of(budgetPlan));

        // Act
        BudgetCheckResponse response = budgetCheckService.checkBudgetAvailability(request);

        // Assert: contract is ">=" not just ">"
        assertTrue(response.isAvailable());
        assertEquals(new BigDecimal("30000.00"), response.getAvailableAmount());
    }

    @Test
    @DisplayName("Contract: Zero allocated → unavailable")
    void testCheckBudgetZeroAllocated() {
        // Arrange: no budget allocated
        BudgetPlan budgetPlan = BudgetPlan.builder()
            .allocatedAmount(BigDecimal.ZERO)
            .spentAmount(BigDecimal.ZERO)
            .build();

        BudgetCheckRequest request = BudgetCheckRequest.builder()
            .departmentId(DEPARTMENT_ID)
            .amount(new BigDecimal("1000.00"))
            .costCenter("CC-004")
            .fiscalYear(2026)
            .build();

        when(budgetPlanRepository.findByDepartmentIdAndFiscalYearAndTenantId(
            eq(DEPARTMENT_ID), eq(2026), eq(TENANT_ID)))
            .thenReturn(Optional.of(budgetPlan));

        // Act
        BudgetCheckResponse response = budgetCheckService.checkBudgetAvailability(request);

        // Assert: contract is "zero budget = unavailable"
        assertFalse(response.isAvailable());
        assertEquals(BigDecimal.ZERO, response.getAvailableAmount());
    }
}
