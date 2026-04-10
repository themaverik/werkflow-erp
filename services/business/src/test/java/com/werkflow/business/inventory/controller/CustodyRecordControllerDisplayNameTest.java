package com.werkflow.business.inventory.controller;

import com.werkflow.business.common.context.TenantContext;
import com.werkflow.business.common.context.UserContext;
import com.werkflow.business.common.filter.TenantContextFilter;
import com.werkflow.business.common.filter.UserContextFilter;
import com.werkflow.business.common.idempotency.filter.IdempotencyFilter;
import com.werkflow.business.common.idempotency.service.IdempotencyService;
import com.werkflow.business.common.identity.dto.UserInfo;
import com.werkflow.business.config.SecurityConfig;
import com.werkflow.business.config.TestSecurityConfig;
import com.werkflow.business.inventory.entity.AssetInstance;
import com.werkflow.business.inventory.entity.CustodyRecord;
import com.werkflow.business.inventory.service.AssetInstanceService;
import com.werkflow.business.inventory.service.CustodyRecordService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Verifies that CustodyRecordController.mapToResponse() populates
 * createdByDisplayName and updatedByDisplayName from UserContext.
 */
@WebMvcTest(
    controllers = CustodyRecordController.class,
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
class CustodyRecordControllerDisplayNameTest {

    @Autowired
    private MockMvc mvc;

    @MockBean
    private CustodyRecordService custodyService;

    @MockBean
    private AssetInstanceService assetService;

    @MockBean
    private TenantContext tenantContext;

    @MockBean
    private IdempotencyService idempotencyService;

    @AfterEach
    void tearDown() {
        UserContext.clear();
    }

    @Test
    @WithMockUser
    void getCustodyRecordById_populatesCreatedByDisplayName_andUpdatedByDisplayName() throws Exception {
        UserContext.setUserInfo(UserInfo.builder()
            .keycloakId("user-789")
            .displayName("Alice Admin")
            .email("alice@example.com")
            .build());

        AssetInstance asset = AssetInstance.builder()
            .id(1L)
            .assetTag("ASSET-001")
            .build();

        CustodyRecord record = CustodyRecord.builder()
            .id(1L)
            .assetInstance(asset)
            .custodyType(CustodyRecord.CustodyType.PERMANENT)
            .startDate(LocalDateTime.of(2026, 1, 1, 9, 0))
            .build();

        when(custodyService.getCustodyRecordById(1L)).thenReturn(record);

        mvc.perform(get("/custody-records/1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.createdByDisplayName", is("Alice Admin")))
            .andExpect(jsonPath("$.updatedByDisplayName", is("Alice Admin")));
    }
}
