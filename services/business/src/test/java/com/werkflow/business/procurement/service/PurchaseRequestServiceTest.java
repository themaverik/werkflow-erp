package com.werkflow.business.procurement.service;

import com.werkflow.business.common.context.TenantContext;
import com.werkflow.business.common.validator.CrossDomainValidator;
import com.werkflow.business.procurement.dto.PurchaseRequestRequest;
import com.werkflow.business.procurement.dto.PurchaseRequestResponse;
import com.werkflow.business.procurement.entity.PurchaseRequest;
import com.werkflow.business.procurement.repository.PrLineItemRepository;
import com.werkflow.business.procurement.repository.PurchaseRequestRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PurchaseRequestServiceTest {

    @Mock
    private PurchaseRequestRepository prRepository;

    @Mock
    private PrLineItemRepository lineItemRepository;

    @Mock
    private TenantContext tenantContext;

    @Mock
    private CrossDomainValidator validator;

    @InjectMocks
    private PurchaseRequestService prService;

    private static final String TENANT_ID = "ACME";

    @Test
    void createPurchaseRequest_withInvalidDepartmentId_throwsException() {
        // Given: request with non-existent department
        PurchaseRequestRequest request = PurchaseRequestRequest.builder()
            .requestingDeptId(99999L)
            .requesterUserId(1L)
            .requestDate(LocalDate.of(2026, 1, 15))
            .justification("Test PR with invalid dept")
            .build();

        when(tenantContext.getTenantId()).thenReturn(TENANT_ID);
        doThrow(new EntityNotFoundException("Department not found: id=99999 for tenant=" + TENANT_ID))
            .when(validator).validateDepartmentExists(99999L, TENANT_ID);

        // When/Then: should throw EntityNotFoundException
        EntityNotFoundException exception = assertThrows(
            EntityNotFoundException.class,
            () -> prService.createPurchaseRequest(request)
        );

        assertTrue(exception.getMessage().contains("Department not found"));
        assertTrue(exception.getMessage().contains("99999"));
    }

    @Test
    void createPurchaseRequest_withValidDepartmentId_succeeds() {
        // Given: request with valid department (mocked)
        Long validDeptId = 1L;
        PurchaseRequestRequest request = PurchaseRequestRequest.builder()
            .requestingDeptId(validDeptId)
            .requesterUserId(2L)
            .requestDate(LocalDate.of(2026, 1, 15))
            .justification("Test PR with valid dept")
            .build();

        when(tenantContext.getTenantId()).thenReturn(TENANT_ID);
        // validator does nothing by default (valid dept)

        PurchaseRequest savedEntity = PurchaseRequest.builder()
            .id(1L)
            .tenantId(TENANT_ID)
            .prNumber("PR-12345")
            .requestingDeptId(validDeptId)
            .requesterUserId(2L)
            .requestDate(LocalDate.of(2026, 1, 15))
            .justification("Test PR with valid dept")
            .totalAmount(BigDecimal.ZERO)
            .status(PurchaseRequest.PrStatus.DRAFT)
            .priority(PurchaseRequest.Priority.MEDIUM)
            .build();

        when(prRepository.save(any(PurchaseRequest.class))).thenReturn(savedEntity);
        when(lineItemRepository.findByPurchaseRequestIdAndTenantId(anyLong(), anyString()))
            .thenReturn(Collections.emptyList());

        // When: create is called
        PurchaseRequestResponse response = prService.createPurchaseRequest(request);

        // Then: should succeed
        assertNotNull(response.getId());
        assertEquals(validDeptId, response.getRequestingDeptId());

        // Verify validator was invoked with correct arguments
        verify(validator).validateDepartmentExists(validDeptId, TENANT_ID);

        // Verify response fields
        assertEquals(PurchaseRequest.PrStatus.DRAFT, response.getStatus());
        assertNotNull(response.getPrNumber());
        assertEquals(2L, response.getRequesterUserId());
    }
}
