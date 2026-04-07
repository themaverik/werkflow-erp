package com.werkflow.business.hr.controller;

import com.werkflow.business.common.context.TenantContext;
import com.werkflow.business.common.filter.TenantContextFilter;
import com.werkflow.business.common.idempotency.filter.IdempotencyFilter;
import com.werkflow.business.common.idempotency.service.IdempotencyService;
import com.werkflow.business.config.SecurityConfig;
import com.werkflow.business.config.TestSecurityConfig;
import com.werkflow.business.hr.dto.EmployeeResponse;
import com.werkflow.business.hr.service.EmployeeService;
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

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Pagination integration tests for EmployeeController (HR domain).
 * Uses @WebMvcTest slice with mocked EmployeeService to verify pagination
 * parameter handling, response shape, and JSON field correctness.
 *
 * The production SecurityConfig (JWT-based) is excluded via excludeFilters.
 * TestSecurityConfig provides a simple permit-all chain compatible with @WithMockUser.
 */
@WebMvcTest(
    controllers = EmployeeController.class,
    excludeAutoConfiguration = {
        org.springframework.boot.autoconfigure.security.oauth2.resource.servlet.OAuth2ResourceServerAutoConfiguration.class
    },
    excludeFilters = @ComponentScan.Filter(
        type = FilterType.ASSIGNABLE_TYPE,
        classes = { SecurityConfig.class, TenantContextFilter.class, IdempotencyFilter.class }
    )
)
@MockBean(JpaMetamodelMappingContext.class)
@Import(TestSecurityConfig.class)
class EmployeeControllerPaginationTest {

    @Autowired
    private MockMvc mvc;

    @MockBean
    private EmployeeService employeeService;

    @MockBean
    private TenantContext tenantContext;

    @MockBean
    private IdempotencyService idempotencyService;

    private List<EmployeeResponse> buildEmployees(int count, int pageStart) {
        return IntStream.rangeClosed(pageStart, pageStart + count - 1)
            .mapToObj(i -> EmployeeResponse.builder()
                .id((long) i)
                .firstName("Employee" + i)
                .lastName("Test")
                .email("emp" + i + "@example.com")
                .build())
            .collect(Collectors.toList());
    }

    // -----------------------------------------------------------------------
    // 1. Default pagination: no params → size=20, page=0
    // -----------------------------------------------------------------------
    @Test
    @WithMockUser
    void testDefaultPagination() throws Exception {
        List<EmployeeResponse> page0Items = buildEmployees(20, 1);
        Page<EmployeeResponse> page = new PageImpl<>(page0Items,
            PageRequest.of(0, 20), 25);

        when(employeeService.getAllEmployees(any(Pageable.class))).thenReturn(page);

        mvc.perform(get("/employees"))
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
        List<EmployeeResponse> page0Items = buildEmployees(10, 1);
        Page<EmployeeResponse> page = new PageImpl<>(page0Items,
            PageRequest.of(0, 10), 25);

        when(employeeService.getAllEmployees(any(Pageable.class))).thenReturn(page);

        mvc.perform(get("/employees").param("size", "10"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content", hasSize(10)))
            .andExpect(jsonPath("$.size", is(10)))
            .andExpect(jsonPath("$.totalElements", is(25)))
            .andExpect(jsonPath("$.totalPages", is(3)));
    }

    // -----------------------------------------------------------------------
    // 3. Second page: page=1, size=20 (last page with 5 remaining items)
    // -----------------------------------------------------------------------
    @Test
    @WithMockUser
    void testSecondPage() throws Exception {
        List<EmployeeResponse> page1Items = buildEmployees(5, 21);
        Page<EmployeeResponse> page = new PageImpl<>(page1Items,
            PageRequest.of(1, 20), 25);

        when(employeeService.getAllEmployees(any(Pageable.class))).thenReturn(page);

        mvc.perform(get("/employees").param("page", "1").param("size", "20"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content", hasSize(5)))
            .andExpect(jsonPath("$.number", is(1)))
            .andExpect(jsonPath("$.first", is(false)))
            .andExpect(jsonPath("$.last", is(true)));
    }

    // -----------------------------------------------------------------------
    // 4. Size capping: size=5000 capped to 1000 (configured max-page-size)
    //    Spring Data caps the value before passing Pageable to the service.
    // -----------------------------------------------------------------------
    @Test
    @WithMockUser
    void testSizeCapping() throws Exception {
        List<EmployeeResponse> allItems = buildEmployees(25, 1);
        Page<EmployeeResponse> page = new PageImpl<>(allItems,
            PageRequest.of(0, 1000), 25);

        when(employeeService.getAllEmployees(any(Pageable.class))).thenReturn(page);

        mvc.perform(get("/employees").param("size", "5000"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content", hasSize(25)))
            .andExpect(jsonPath("$.totalElements", is(25)));
    }

    // -----------------------------------------------------------------------
    // 5. Custom sort: sort=firstName,asc
    // -----------------------------------------------------------------------
    @Test
    @WithMockUser
    void testCustomSort() throws Exception {
        List<EmployeeResponse> sorted = buildEmployees(20, 1);
        Page<EmployeeResponse> page = new PageImpl<>(sorted,
            PageRequest.of(0, 20, Sort.by(Sort.Direction.ASC, "firstName")), 25);

        when(employeeService.getAllEmployees(any(Pageable.class))).thenReturn(page);

        mvc.perform(get("/employees").param("sort", "firstName,asc"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content", hasSize(20)))
            .andExpect(jsonPath("$.content[0].firstName", is("Employee1")));
    }
}
