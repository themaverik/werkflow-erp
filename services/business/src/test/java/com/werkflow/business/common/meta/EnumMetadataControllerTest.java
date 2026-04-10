package com.werkflow.business.common.meta;

import com.werkflow.business.common.context.TenantContext;
import com.werkflow.business.common.filter.TenantContextFilter;
import com.werkflow.business.common.filter.UserContextFilter;
import com.werkflow.business.common.idempotency.filter.IdempotencyFilter;
import com.werkflow.business.common.idempotency.service.IdempotencyService;
import com.werkflow.business.config.SecurityConfig;
import com.werkflow.business.config.TestSecurityConfig;
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

import java.util.Arrays;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(
    controllers = EnumMetadataController.class,
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
public class EnumMetadataControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private EnumMetadataService enumMetadataService;

    @MockBean
    private TenantContext tenantContext;

    @MockBean
    private IdempotencyService idempotencyService;

    @Test
    @WithMockUser
    void testGetAllEnumsEndpointExists() throws Exception {
        EnumMetadataResponseDTO response = EnumMetadataResponseDTO.builder()
                .enums(Arrays.asList())
                .build();

        when(enumMetadataService.getAllEnums()).thenReturn(response);

        mockMvc.perform(get("/api/v1/meta/enums")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        verify(enumMetadataService, times(1)).getAllEnums();
    }

    @Test
    @WithMockUser
    void testGetAllEnumsReturnsOkStatus() throws Exception {
        EnumMetadataDTO enumDto = EnumMetadataDTO.builder()
                .name("TestEnum")
                .description("Test enum")
                .values(Arrays.asList(
                        EnumValueDTO.builder()
                                .value("VALUE1")
                                .label("Value 1")
                                .description("First value")
                                .build()
                ))
                .build();

        EnumMetadataResponseDTO response = EnumMetadataResponseDTO.builder()
                .enums(Arrays.asList(enumDto))
                .build();

        when(enumMetadataService.getAllEnums()).thenReturn(response);

        mockMvc.perform(get("/api/v1/meta/enums")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser
    void testGetAllEnumsReturnsJsonContentType() throws Exception {
        EnumMetadataResponseDTO response = EnumMetadataResponseDTO.builder()
                .enums(Arrays.asList())
                .build();

        when(enumMetadataService.getAllEnums()).thenReturn(response);

        mockMvc.perform(get("/api/v1/meta/enums"))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON));
    }

    @Test
    @WithMockUser
    void testGetAllEnumsReturnsEnumsArray() throws Exception {
        EnumMetadataDTO enumDto = EnumMetadataDTO.builder()
                .name("TestEnum")
                .description("Test enum")
                .values(Arrays.asList(
                        EnumValueDTO.builder()
                                .value("VALUE1")
                                .label("Value 1")
                                .description("First value")
                                .build()
                ))
                .build();

        EnumMetadataResponseDTO response = EnumMetadataResponseDTO.builder()
                .enums(Arrays.asList(enumDto))
                .build();

        when(enumMetadataService.getAllEnums()).thenReturn(response);

        mockMvc.perform(get("/api/v1/meta/enums"))
                .andExpect(jsonPath("$.enums", notNullValue()));
    }

    @Test
    @WithMockUser
    void testGetAllEnumsReturnsEnumMetadata() throws Exception {
        EnumValueDTO value = EnumValueDTO.builder()
                .value("DRAFT")
                .label("Draft")
                .description("Initial state")
                .build();

        EnumMetadataDTO enumDto = EnumMetadataDTO.builder()
                .name("PrStatus")
                .description("Purchase request status")
                .values(Arrays.asList(value))
                .build();

        EnumMetadataResponseDTO response = EnumMetadataResponseDTO.builder()
                .enums(Arrays.asList(enumDto))
                .build();

        when(enumMetadataService.getAllEnums()).thenReturn(response);

        mockMvc.perform(get("/api/v1/meta/enums"))
                .andExpect(jsonPath("$.enums[0].name", equalTo("PrStatus")))
                .andExpect(jsonPath("$.enums[0].description", equalTo("Purchase request status")))
                .andExpect(jsonPath("$.enums[0].values", notNullValue()));
    }

    @Test
    @WithMockUser
    void testGetAllEnumsReturnsEnumValues() throws Exception {
        EnumValueDTO value = EnumValueDTO.builder()
                .value("DRAFT")
                .label("Draft")
                .description("Initial state")
                .build();

        EnumMetadataDTO enumDto = EnumMetadataDTO.builder()
                .name("PrStatus")
                .description("Purchase request status")
                .values(Arrays.asList(value))
                .build();

        EnumMetadataResponseDTO response = EnumMetadataResponseDTO.builder()
                .enums(Arrays.asList(enumDto))
                .build();

        when(enumMetadataService.getAllEnums()).thenReturn(response);

        mockMvc.perform(get("/api/v1/meta/enums"))
                .andExpect(jsonPath("$.enums[0].values[0].value", equalTo("DRAFT")))
                .andExpect(jsonPath("$.enums[0].values[0].label", equalTo("Draft")))
                .andExpect(jsonPath("$.enums[0].values[0].description", equalTo("Initial state")));
    }

    @Test
    @WithMockUser
    void testGetAllEnumsCallsService() throws Exception {
        EnumMetadataResponseDTO response = EnumMetadataResponseDTO.builder()
                .enums(Arrays.asList())
                .build();

        when(enumMetadataService.getAllEnums()).thenReturn(response);

        mockMvc.perform(get("/api/v1/meta/enums"));

        verify(enumMetadataService, times(1)).getAllEnums();
    }

    @Test
    @WithMockUser
    void testGetAllEnumsMultipleEnums() throws Exception {
        EnumValueDTO value1 = EnumValueDTO.builder()
                .value("DRAFT")
                .label("Draft")
                .description("Initial state")
                .build();

        EnumValueDTO value2 = EnumValueDTO.builder()
                .value("ACTIVE")
                .label("Active")
                .description("Active state")
                .build();

        EnumMetadataDTO enum1 = EnumMetadataDTO.builder()
                .name("PrStatus")
                .description("Purchase request status")
                .values(Arrays.asList(value1))
                .build();

        EnumMetadataDTO enum2 = EnumMetadataDTO.builder()
                .name("EmployeeStatus")
                .description("Employee status")
                .values(Arrays.asList(value2))
                .build();

        EnumMetadataResponseDTO response = EnumMetadataResponseDTO.builder()
                .enums(Arrays.asList(enum1, enum2))
                .build();

        when(enumMetadataService.getAllEnums()).thenReturn(response);

        mockMvc.perform(get("/api/v1/meta/enums"))
                .andExpect(jsonPath("$.enums", hasSize(2)))
                .andExpect(jsonPath("$.enums[0].name", equalTo("PrStatus")))
                .andExpect(jsonPath("$.enums[1].name", equalTo("EmployeeStatus")));
    }
}
