package com.werkflow.business.finance.controller;

import com.werkflow.business.common.context.TenantContext;
import com.werkflow.business.common.filter.TenantContextFilter;
import com.werkflow.business.common.filter.UserContextFilter;
import com.werkflow.business.common.idempotency.filter.IdempotencyFilter;
import com.werkflow.business.common.idempotency.service.IdempotencyService;
import com.werkflow.business.config.SecurityConfig;
import com.werkflow.business.config.TestSecurityConfig;
import com.werkflow.business.finance.dto.BudgetPlanResponse;
import com.werkflow.business.finance.entity.BudgetPlan.BudgetStatus;
import com.werkflow.business.finance.service.BudgetPlanService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Pagination integration tests for BudgetPlanController (Finance domain).
 */
@WebMvcTest(
    controllers = BudgetPlanController.class,
    excludeAutoConfiguration = {
        org.springframework.boot.autoconfigure.security.oauth2.resource.servlet.OAuth2ResourceServerAutoConfiguration.class
    },
    excludeFilters = @ComponentScan.Filter(
        type = FilterType.ASSIGNABLE_TYPE,
        classes = { SecurityConfig.class, TenantContextFilter.class, UserContextFilter.class, IdempotencyFilter.class }
    )
)
@MockBean(JpaMetamodelMappingContext.class)
@Import(TestSecurityConfig.class)
class BudgetPlanControllerPaginationTest {

    @Autowired
    private MockMvc mvc;

    @MockBean
    private BudgetPlanService budgetPlanService;

    @MockBean
    private TenantContext tenantContext;

    @MockBean
    private IdempotencyService idempotencyService;

    private List<BudgetPlanResponse> buildBudgetPlans(int count, int pageStart) {
        return IntStream.rangeClosed(pageStart, pageStart + count - 1)
            .mapToObj(i -> BudgetPlanResponse.builder()
                .id((long) i)
                .departmentId(1L)
                .fiscalYear(2026)
                .periodStart(LocalDate.of(2026, 1, 1))
                .periodEnd(LocalDate.of(2026, 12, 31))
                .totalAmount(BigDecimal.valueOf(100000))
                .allocatedAmount(BigDecimal.ZERO)
                .spentAmount(BigDecimal.ZERO)
                .status(BudgetStatus.DRAFT)
                .createdByUserId(1L)
                .build())
            .collect(Collectors.toList());
    }

    // -----------------------------------------------------------------------
    // 1. Default pagination: no params → size=20, page=0
    // -----------------------------------------------------------------------
    @Test
    @WithMockUser
    void testDefaultPagination() throws Exception {
        List<BudgetPlanResponse> items = buildBudgetPlans(20, 1);
        Page<BudgetPlanResponse> page = new PageImpl<>(items,
            PageRequest.of(0, 20), 25);

        when(budgetPlanService.getAllBudgetPlans(any(Pageable.class))).thenReturn(page);

        mvc.perform(get("/budgets"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content", hasSize(20)))
            .andExpect(jsonPath("$.totalElements", is(25)))
            .andExpect(jsonPath("$.totalPages", is(2)))
            .andExpect(jsonPath("$.number", is(0)))
            .andExpect(jsonPath("$.size", is(20)))
            .andExpect(jsonPath("$.first", is(true)))
            .andExpect(jsonPath("$.last", is(false)));
    }

    // -----------------------------------------------------------------------
    // 2. Custom page size: size=10
    // -----------------------------------------------------------------------
    @Test
    @WithMockUser
    void testCustomPageSize() throws Exception {
        List<BudgetPlanResponse> items = buildBudgetPlans(10, 1);
        Page<BudgetPlanResponse> page = new PageImpl<>(items,
            PageRequest.of(0, 10), 25);

        when(budgetPlanService.getAllBudgetPlans(any(Pageable.class))).thenReturn(page);

        mvc.perform(get("/budgets").param("size", "10"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content", hasSize(10)))
            .andExpect(jsonPath("$.size", is(10)))
            .andExpect(jsonPath("$.totalPages", is(3)));
    }

    // -----------------------------------------------------------------------
    // 3. Second page: page=1, size=20 (last page with 5 items)
    // -----------------------------------------------------------------------
    @Test
    @WithMockUser
    void testSecondPage() throws Exception {
        List<BudgetPlanResponse> items = buildBudgetPlans(5, 21);
        Page<BudgetPlanResponse> page = new PageImpl<>(items,
            PageRequest.of(1, 20), 25);

        when(budgetPlanService.getAllBudgetPlans(any(Pageable.class))).thenReturn(page);

        mvc.perform(get("/budgets").param("page", "1").param("size", "20"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content", hasSize(5)))
            .andExpect(jsonPath("$.number", is(1)))
            .andExpect(jsonPath("$.first", is(false)))
            .andExpect(jsonPath("$.last", is(true)));
    }

    // -----------------------------------------------------------------------
    // 4. Size capping: size=5000 capped to 1000 by Spring Data
    // -----------------------------------------------------------------------
    @Test
    @WithMockUser
    void testSizeCapping() throws Exception {
        List<BudgetPlanResponse> items = buildBudgetPlans(25, 1);
        Page<BudgetPlanResponse> page = new PageImpl<>(items,
            PageRequest.of(0, 1000), 25);

        when(budgetPlanService.getAllBudgetPlans(any(Pageable.class))).thenReturn(page);

        mvc.perform(get("/budgets").param("size", "5000"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalElements", is(25)));
    }

    // -----------------------------------------------------------------------
    // 5. Custom sort: sort=fiscalYear,asc
    // -----------------------------------------------------------------------
    @Test
    @WithMockUser
    void getBudgetPlanById_populatesDisplayNamesInResponse() throws Exception {
        BudgetPlanResponse response = BudgetPlanResponse.builder()
            .id(1L)
            .departmentId(1L)
            .fiscalYear(2026)
            .periodStart(LocalDate.of(2026, 1, 1))
            .periodEnd(LocalDate.of(2026, 12, 31))
            .totalAmount(BigDecimal.valueOf(100000))
            .allocatedAmount(BigDecimal.ZERO)
            .spentAmount(BigDecimal.ZERO)
            .status(BudgetStatus.DRAFT)
            .createdByUserId(1L)
            .createdByDisplayName("Jane Smith")
            .updatedByDisplayName("Jane Smith")
            .build();

        when(budgetPlanService.getBudgetPlanById(anyLong())).thenReturn(response);

        mvc.perform(get("/budgets/1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.createdByDisplayName", is("Jane Smith")))
            .andExpect(jsonPath("$.updatedByDisplayName", is("Jane Smith")));
    }

    @Test
    @WithMockUser
    void testCustomSort() throws Exception {
        List<BudgetPlanResponse> items = buildBudgetPlans(20, 1);
        Page<BudgetPlanResponse> page = new PageImpl<>(items,
            PageRequest.of(0, 20, Sort.by(Sort.Direction.ASC, "fiscalYear")), 25);

        when(budgetPlanService.getAllBudgetPlans(any(Pageable.class))).thenReturn(page);

        mvc.perform(get("/budgets").param("sort", "fiscalYear,asc"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content", hasSize(20)))
            .andExpect(jsonPath("$.content[0].fiscalYear", is(2026)));
    }
}
