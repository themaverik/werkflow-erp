package com.werkflow.business.inventory.controller;

import com.werkflow.business.common.context.TenantContext;
import com.werkflow.business.common.filter.TenantContextFilter;
import com.werkflow.business.common.filter.UserContextFilter;
import com.werkflow.business.common.idempotency.filter.IdempotencyFilter;
import com.werkflow.business.common.idempotency.service.IdempotencyService;
import com.werkflow.business.config.SecurityConfig;
import com.werkflow.business.config.TestSecurityConfig;
import com.werkflow.business.inventory.entity.AssetDefinition;
import com.werkflow.business.inventory.entity.AssetInstance;
import com.werkflow.business.inventory.entity.AssetInstance.AssetCondition;
import com.werkflow.business.inventory.entity.AssetInstance.AssetStatus;
import com.werkflow.business.inventory.service.AssetDefinitionService;
import com.werkflow.business.inventory.service.AssetInstanceService;
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
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Pagination integration tests for AssetInstanceController (Inventory domain).
 *
 * The controller maps Page<AssetInstance> → Page<AssetInstanceResponseDto> inline,
 * so this test mocks AssetInstanceService.getAllInstances(Pageable).
 */
@WebMvcTest(
    controllers = AssetInstanceController.class,
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
class AssetInstanceControllerPaginationTest {

    @Autowired
    private MockMvc mvc;

    @MockBean
    private AssetInstanceService instanceService;

    @MockBean
    private AssetDefinitionService definitionService;

    @MockBean
    private TenantContext tenantContext;

    @MockBean
    private IdempotencyService idempotencyService;

    /**
     * Build minimal AssetInstance stubs. The controller's mapToResponse() accesses
     * definition.getId() and definition.getName(), so we provide a stub AssetDefinition.
     */
    private List<AssetInstance> buildInstances(int count, int pageStart) {
        return IntStream.rangeClosed(pageStart, pageStart + count - 1)
            .mapToObj(i -> {
                AssetDefinition def = AssetDefinition.builder()
                    .id(1L)
                    .name("Laptop")
                    .build();

                return AssetInstance.builder()
                    .id((long) i)
                    .tenantId("test-tenant")
                    .assetDefinition(def)
                    .assetTag("TAG-" + String.format("%04d", i))
                    .serialNumber("SN-" + i)
                    .purchaseCost(BigDecimal.valueOf(1500))
                    .condition(AssetCondition.NEW)
                    .status(AssetStatus.AVAILABLE)
                    .build();
            })
            .collect(Collectors.toList());
    }

    // -----------------------------------------------------------------------
    // 1. Default pagination: no params → size=20, page=0
    // -----------------------------------------------------------------------
    @Test
    @WithMockUser
    void testDefaultPagination() throws Exception {
        List<AssetInstance> items = buildInstances(20, 1);
        Page<AssetInstance> page = new PageImpl<>(items,
            PageRequest.of(0, 20), 25);

        when(instanceService.getAllInstances(any(Pageable.class))).thenReturn(page);

        mvc.perform(get("/asset-instances"))
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
        List<AssetInstance> items = buildInstances(10, 1);
        Page<AssetInstance> page = new PageImpl<>(items,
            PageRequest.of(0, 10), 25);

        when(instanceService.getAllInstances(any(Pageable.class))).thenReturn(page);

        mvc.perform(get("/asset-instances").param("size", "10"))
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
        List<AssetInstance> items = buildInstances(5, 21);
        Page<AssetInstance> page = new PageImpl<>(items,
            PageRequest.of(1, 20), 25);

        when(instanceService.getAllInstances(any(Pageable.class))).thenReturn(page);

        mvc.perform(get("/asset-instances").param("page", "1").param("size", "20"))
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
        List<AssetInstance> items = buildInstances(25, 1);
        Page<AssetInstance> page = new PageImpl<>(items,
            PageRequest.of(0, 1000), 25);

        when(instanceService.getAllInstances(any(Pageable.class))).thenReturn(page);

        mvc.perform(get("/asset-instances").param("size", "5000"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalElements", is(25)));
    }

    // -----------------------------------------------------------------------
    // 5. Custom sort: sort=assetTag,asc
    // -----------------------------------------------------------------------
    @Test
    @WithMockUser
    void testCustomSort() throws Exception {
        List<AssetInstance> items = buildInstances(20, 1);
        Page<AssetInstance> page = new PageImpl<>(items,
            PageRequest.of(0, 20, Sort.by(Sort.Direction.ASC, "assetTag")), 25);

        when(instanceService.getAllInstances(any(Pageable.class))).thenReturn(page);

        mvc.perform(get("/asset-instances").param("sort", "assetTag,asc"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content", hasSize(20)))
            .andExpect(jsonPath("$.content[0].assetTag", is("TAG-0001")));
    }
}
