package com.werkflow.business.common.identity;

import com.werkflow.business.common.context.TenantContext;
import com.werkflow.business.common.filter.TenantContextFilter;
import com.werkflow.business.common.filter.UserContextFilter;
import com.werkflow.business.common.idempotency.filter.IdempotencyFilter;
import com.werkflow.business.common.idempotency.service.IdempotencyService;
import com.werkflow.business.common.identity.dto.UserProfileResponse;
import com.werkflow.business.config.SecurityConfig;
import com.werkflow.business.config.TestSecurityConfig;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.equalTo;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(
    controllers = UserProfileController.class,
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
class UserProfileControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserProfileService userProfileService;

    @MockBean
    private TenantContext tenantContext;

    @MockBean
    private IdempotencyService idempotencyService;

    @Test
    @WithMockUser
    void getProfile_returns200WithProfile_whenUserExists() throws Exception {
        UserProfileResponse profile = UserProfileResponse.builder()
                .keycloakId("user-123")
                .displayName("Jane Smith")
                .email("jane@example.com")
                .departmentCode("FIN")
                .employeeId("EMP-001")
                .costCenter("CC-100")
                .isPoc(true)
                .build();

        when(userProfileService.getProfile("user-123")).thenReturn(profile);

        mockMvc.perform(get("/api/v1/users/user-123/profile")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.keycloakId", equalTo("user-123")))
                .andExpect(jsonPath("$.displayName", equalTo("Jane Smith")))
                .andExpect(jsonPath("$.email", equalTo("jane@example.com")))
                .andExpect(jsonPath("$.departmentCode", equalTo("FIN")))
                .andExpect(jsonPath("$.employeeId", equalTo("EMP-001")))
                .andExpect(jsonPath("$.costCenter", equalTo("CC-100")))
                .andExpect(jsonPath("$.poc", equalTo(true)));
    }

    @Test
    @WithMockUser
    void getProfile_returns404_whenUserNotFound() throws Exception {
        when(userProfileService.getProfile("missing"))
                .thenThrow(new EntityNotFoundException("User profile not found for keycloakId: missing"));

        mockMvc.perform(get("/api/v1/users/missing/profile"))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser
    void getProfile_returnsNullableEnterpriseFields_whenNotSet() throws Exception {
        UserProfileResponse profile = UserProfileResponse.builder()
                .keycloakId("user-456")
                .displayName("John Doe")
                .build();

        when(userProfileService.getProfile("user-456")).thenReturn(profile);

        mockMvc.perform(get("/api/v1/users/user-456/profile"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.departmentCode").doesNotExist())
                .andExpect(jsonPath("$.employeeId").doesNotExist())
                .andExpect(jsonPath("$.costCenter").doesNotExist())
                .andExpect(jsonPath("$.poc", equalTo(false)));
    }
}
