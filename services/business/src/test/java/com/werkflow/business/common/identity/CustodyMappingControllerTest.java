package com.werkflow.business.common.identity;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.werkflow.business.common.context.TenantContext;
import com.werkflow.business.common.filter.TenantContextFilter;
import com.werkflow.business.common.filter.UserContextFilter;
import com.werkflow.business.common.idempotency.filter.IdempotencyFilter;
import com.werkflow.business.common.idempotency.service.IdempotencyService;
import com.werkflow.business.common.identity.dto.CustodyMappingRequest;
import com.werkflow.business.common.identity.dto.CustodyMappingResponse;
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
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(
    controllers = CustodyMappingController.class,
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
class CustodyMappingControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private CustodyMappingService custodyMappingService;

    @MockBean
    private TenantContext tenantContext;

    @MockBean
    private IdempotencyService idempotencyService;

    private CustodyMappingResponse buildResponse() {
        return CustodyMappingResponse.builder()
                .id(1L)
                .tenantId("tenant-abc")
                .custodyOwner("IT_EQUIPMENT")
                .candidateGroups(List.of("it_team", "procurement_team"))
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    @Test
    @WithMockUser
    void list_returns200WithPage() throws Exception {
        when(custodyMappingService.list(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(buildResponse())));

        mockMvc.perform(get("/api/v1/custody-mappings"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].custodyOwner", equalTo("IT_EQUIPMENT")))
                .andExpect(jsonPath("$.content[0].candidateGroups", containsInAnyOrder("it_team", "procurement_team")));
    }

    @Test
    @WithMockUser
    void getById_returns200_whenFound() throws Exception {
        when(custodyMappingService.getById(1L)).thenReturn(buildResponse());

        mockMvc.perform(get("/api/v1/custody-mappings/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", equalTo(1)))
                .andExpect(jsonPath("$.custodyOwner", equalTo("IT_EQUIPMENT")));
    }

    @Test
    @WithMockUser
    void getById_returns404_whenNotFound() throws Exception {
        when(custodyMappingService.getById(99L))
                .thenThrow(new EntityNotFoundException("Custody mapping not found: 99"));

        mockMvc.perform(get("/api/v1/custody-mappings/99"))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser
    void upsert_returns201_withValidRequest() throws Exception {
        CustodyMappingRequest request = new CustodyMappingRequest("IT_EQUIPMENT", List.of("it_team"));
        when(custodyMappingService.upsert(any(CustodyMappingRequest.class))).thenReturn(buildResponse());

        mockMvc.perform(post("/api/v1/custody-mappings")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.custodyOwner", equalTo("IT_EQUIPMENT")));
    }

    @Test
    @WithMockUser
    void upsert_returns400_whenCustodyOwnerBlank() throws Exception {
        CustodyMappingRequest request = new CustodyMappingRequest("", List.of("it_team"));

        mockMvc.perform(post("/api/v1/custody-mappings")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser
    void upsert_returns400_whenCandidateGroupsEmpty() throws Exception {
        CustodyMappingRequest request = new CustodyMappingRequest("IT_EQUIPMENT", List.of());

        mockMvc.perform(post("/api/v1/custody-mappings")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser
    void update_returns200_whenValid() throws Exception {
        CustodyMappingRequest request = new CustodyMappingRequest("IT_EQUIPMENT_V2", List.of("new_team"));
        when(custodyMappingService.update(any(Long.class), any(CustodyMappingRequest.class)))
                .thenReturn(buildResponse());

        mockMvc.perform(put("/api/v1/custody-mappings/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser
    void delete_returns204_whenFound() throws Exception {
        mockMvc.perform(delete("/api/v1/custody-mappings/1"))
                .andExpect(status().isNoContent());

        verify(custodyMappingService).delete(1L);
    }

    @Test
    @WithMockUser
    void delete_returns404_whenNotFound() throws Exception {
        org.mockito.Mockito.doThrow(new EntityNotFoundException("Custody mapping not found: 99"))
                .when(custodyMappingService).delete(99L);

        mockMvc.perform(delete("/api/v1/custody-mappings/99"))
                .andExpect(status().isNotFound());
    }
}
