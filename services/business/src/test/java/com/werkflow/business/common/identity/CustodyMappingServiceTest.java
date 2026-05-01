package com.werkflow.business.common.identity;

import com.werkflow.business.common.context.TenantContext;
import com.werkflow.business.common.identity.dto.CustodyMappingRequest;
import com.werkflow.business.common.identity.dto.CustodyMappingResponse;
import com.werkflow.business.common.identity.entity.CustodyMapping;
import com.werkflow.business.common.identity.repository.CustodyMappingRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class CustodyMappingServiceTest {

    @Mock
    private CustodyMappingRepository repository;

    @Mock
    private TenantContext tenantContext;

    @InjectMocks
    private CustodyMappingService service;

    private static final String TENANT = "tenant-abc";
    private CustodyMapping mapping;

    @BeforeEach
    void setUp() {
        mapping = CustodyMapping.builder()
                .id(1L)
                .tenantId(TENANT)
                .custodyOwner("IT_EQUIPMENT")
                .candidateGroups(new String[]{"it_team", "procurement_team"})
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        when(tenantContext.getTenantId()).thenReturn(TENANT);
    }

    @Test
    void list_returnsTenantScopedPage() {
        Pageable pageable = PageRequest.of(0, 10);
        when(repository.findByTenantIdOrderByCustodyOwner(TENANT, pageable))
                .thenReturn(new PageImpl<>(List.of(mapping)));

        Page<CustodyMappingResponse> result = service.list(pageable);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getCustodyOwner()).isEqualTo("IT_EQUIPMENT");
        assertThat(result.getContent().get(0).getCandidateGroups()).containsExactly("it_team", "procurement_team");
    }

    @Test
    void getById_returnsMapping_whenOwnedByTenant() {
        when(repository.findById(1L)).thenReturn(Optional.of(mapping));

        CustodyMappingResponse result = service.getById(1L);

        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getTenantId()).isEqualTo(TENANT);
    }

    @Test
    void getById_throwsEntityNotFound_whenIdMissing() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getById(99L))
                .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    void getById_throwsEntityNotFound_whenTenantMismatch() {
        CustodyMapping otherTenant = CustodyMapping.builder()
                .id(2L).tenantId("other-tenant").custodyOwner("X")
                .candidateGroups(new String[]{"g1"})
                .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now())
                .build();
        when(repository.findById(2L)).thenReturn(Optional.of(otherTenant));

        assertThatThrownBy(() -> service.getById(2L))
                .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    void upsert_callsRepositoryUpsertAndReturnsRecord() {
        CustodyMappingRequest request = new CustodyMappingRequest("IT_EQUIPMENT", List.of("it_team"));
        when(repository.findByTenantIdAndCustodyOwner(TENANT, "IT_EQUIPMENT"))
                .thenReturn(Optional.of(mapping));

        CustodyMappingResponse result = service.upsert(request);

        verify(repository).upsert(eq(TENANT), eq("IT_EQUIPMENT"), any(String[].class), any(LocalDateTime.class));
        assertThat(result.getCustodyOwner()).isEqualTo("IT_EQUIPMENT");
    }

    @Test
    void update_updatesFieldsAndSaves() {
        when(repository.findById(1L)).thenReturn(Optional.of(mapping));
        when(repository.save(mapping)).thenReturn(mapping);

        CustodyMappingRequest request = new CustodyMappingRequest("IT_EQUIPMENT_V2", List.of("new_team"));
        CustodyMappingResponse result = service.update(1L, request);

        assertThat(mapping.getCustodyOwner()).isEqualTo("IT_EQUIPMENT_V2");
        verify(repository).save(mapping);
    }

    @Test
    void delete_removesMapping_whenOwnedByTenant() {
        when(repository.findById(1L)).thenReturn(Optional.of(mapping));

        service.delete(1L);

        verify(repository).deleteById(1L);
    }

    @Test
    void delete_throwsEntityNotFound_whenMissing() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.delete(99L))
                .isInstanceOf(EntityNotFoundException.class);
    }
}
