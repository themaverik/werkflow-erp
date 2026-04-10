package com.werkflow.business.hr.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.werkflow.business.common.context.TenantContext;
import com.werkflow.business.common.filter.TenantContextFilter;
import com.werkflow.business.common.filter.UserContextFilter;
import com.werkflow.business.common.idempotency.filter.IdempotencyFilter;
import com.werkflow.business.common.idempotency.service.IdempotencyService;
import com.werkflow.business.config.SecurityConfig;
import com.werkflow.business.config.TestSecurityConfig;
import com.werkflow.business.hr.dto.EmployeeResponse;
import com.werkflow.business.hr.dto.KeycloakLinkRequest;
import com.werkflow.business.hr.service.EmployeeService;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(
    controllers = EmployeeController.class,
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
public class EmployeeControllerKeycloakTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private EmployeeService employeeService;

    @MockBean
    private TenantContext tenantContext;

    @MockBean
    private IdempotencyService idempotencyService;

    @Test
    @WithMockUser
    void testLinkKeycloakUserSuccess() throws Exception {
        EmployeeResponse response = EmployeeResponse.builder()
            .id(1L)
            .firstName("John")
            .lastName("Doe")
            .email("john@example.com")
            .keycloakUserId("keycloak-uuid-123")
            .build();

        when(employeeService.linkKeycloakUser(1L, "keycloak-uuid-123"))
            .thenReturn(response);

        KeycloakLinkRequest request = new KeycloakLinkRequest("keycloak-uuid-123");

        mockMvc.perform(patch("/employees/1/keycloak-link")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(1))
            .andExpect(jsonPath("$.keycloakUserId").value("keycloak-uuid-123"));

        verify(employeeService).linkKeycloakUser(1L, "keycloak-uuid-123");
    }

    @Test
    @WithMockUser
    void testLinkKeycloakUserNotFound() throws Exception {
        when(employeeService.linkKeycloakUser(999L, "keycloak-uuid-123"))
            .thenThrow(new EntityNotFoundException("Employee not found"));

        KeycloakLinkRequest request = new KeycloakLinkRequest("keycloak-uuid-123");

        mockMvc.perform(patch("/employees/999/keycloak-link")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser
    void testLinkKeycloakUserConflict() throws Exception {
        when(employeeService.linkKeycloakUser(1L, "keycloak-uuid-new"))
            .thenThrow(new DataIntegrityViolationException(
                "Employee already linked to a different Keycloak user"
            ));

        KeycloakLinkRequest request = new KeycloakLinkRequest("keycloak-uuid-new");

        mockMvc.perform(patch("/employees/1/keycloak-link")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isConflict());
    }

    @Test
    @WithMockUser
    void testLinkKeycloakUserValidationFails() throws Exception {
        KeycloakLinkRequest request = new KeycloakLinkRequest("");

        mockMvc.perform(patch("/employees/1/keycloak-link")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest());

        verify(employeeService, never()).linkKeycloakUser(anyLong(), anyString());
    }
}
