package com.werkflow.business.procurement.service;

import com.werkflow.business.common.context.TenantContext;
import com.werkflow.business.common.sequence.NumberGenerationService;
import com.werkflow.business.common.validator.CrossDomainValidator;
import com.werkflow.business.procurement.dto.PurchaseRequestRequest;
import com.werkflow.business.procurement.dto.PurchaseRequestResponse;
import com.werkflow.business.procurement.entity.PurchaseRequest;
import com.werkflow.business.procurement.repository.PrLineItemRepository;
import com.werkflow.business.procurement.repository.PurchaseRequestRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.security.access.AccessDeniedException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

@DisplayName("PurchaseRequestService Contract Tests")
class PurchaseRequestServiceContractTest {

    @Mock
    private PurchaseRequestRepository prRepository;

    @Mock
    private PrLineItemRepository lineItemRepository;

    @Mock
    private TenantContext tenantContext;

    @Mock
    private CrossDomainValidator validator;

    @Mock
    private NumberGenerationService numberGenerationService;

    private PurchaseRequestService service;
    private static final String TENANT_ID = "ACME";
    private static final Long DEPARTMENT_ID = 1L;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        service = new PurchaseRequestService(
            prRepository,
            lineItemRepository,
            tenantContext,
            validator,
            numberGenerationService
        );
        when(tenantContext.getTenantId()).thenReturn(TENANT_ID);
    }

    @Test
    @DisplayName("Contract: Invalid department → validation error")
    void testCreatePrWithInvalidDepartmentThrows() {
        // Arrange: validator rejects invalid dept
        PurchaseRequestRequest request = PurchaseRequestRequest.builder()
            .requestingDeptId(999L)
            .requestDate(LocalDate.now())
            .build();

        doThrow(new EntityNotFoundException("Department not found"))
            .when(validator).validateDepartmentExists(999L, TENANT_ID);

        // Act & Assert: contract is "validate dept before create"
        EntityNotFoundException exception = assertThrows(
            EntityNotFoundException.class,
            () -> service.createPurchaseRequest(request)
        );
        assertTrue(exception.getMessage().contains("Department not found"));
    }

    @Test
    @DisplayName("Contract: Create PR with valid department → success")
    void testCreatePrSuccess() {
        // Arrange: valid department, PR created
        PurchaseRequestRequest request = PurchaseRequestRequest.builder()
            .requestingDeptId(DEPARTMENT_ID)
            .requesterUserId(123L)
            .requestDate(LocalDate.now())
            .requiredByDate(LocalDate.now().plusDays(30))
            .priority(PurchaseRequest.Priority.HIGH)
            .justification("Budget allocated")
            .build();

        PurchaseRequest savedPr = PurchaseRequest.builder()
            .tenantId(TENANT_ID)
            .prNumber("PR-ACME-2026-00001")
            .requestingDeptId(DEPARTMENT_ID)
            .requesterUserId(123L)
            .requestDate(LocalDate.now())
            .priority(PurchaseRequest.Priority.HIGH)
            .totalAmount(BigDecimal.ZERO)
            .status(PurchaseRequest.PrStatus.DRAFT)
            .build();

        when(numberGenerationService.generatePrNumber(TENANT_ID)).thenReturn("PR-ACME-2026-00001");
        when(prRepository.save(any(PurchaseRequest.class))).thenReturn(savedPr);
        when(lineItemRepository.findByPurchaseRequestIdAndTenantId(anyLong(), eq(TENANT_ID)))
            .thenReturn(Collections.emptyList());

        // Act
        PurchaseRequestResponse response = service.createPurchaseRequest(request);

        // Assert: contract is "returns PR with DRAFT status"
        assertNotNull(response);
        assertEquals("PR-ACME-2026-00001", response.getPrNumber());
        assertEquals(PurchaseRequest.PrStatus.DRAFT, response.getStatus());
    }

    @Test
    @DisplayName("Contract: Default priority is MEDIUM")
    void testCreatePrDefaultPriority() {
        // Arrange: no priority specified
        PurchaseRequestRequest request = PurchaseRequestRequest.builder()
            .requestingDeptId(DEPARTMENT_ID)
            .requesterUserId(456L)
            .requestDate(LocalDate.now())
            .priority(null) // no priority
            .build();

        PurchaseRequest savedPr = PurchaseRequest.builder()
            .tenantId(TENANT_ID)
            .prNumber("PR-ACME-2026-00002")
            .requestingDeptId(DEPARTMENT_ID)
            .priority(PurchaseRequest.Priority.MEDIUM) // defaults to MEDIUM
            .totalAmount(BigDecimal.ZERO)
            .status(PurchaseRequest.PrStatus.DRAFT)
            .build();

        when(numberGenerationService.generatePrNumber(TENANT_ID)).thenReturn("PR-ACME-2026-00002");
        when(prRepository.save(any(PurchaseRequest.class))).thenReturn(savedPr);
        when(lineItemRepository.findByPurchaseRequestIdAndTenantId(anyLong(), eq(TENANT_ID)))
            .thenReturn(Collections.emptyList());

        // Act
        PurchaseRequestResponse response = service.createPurchaseRequest(request);

        // Assert: contract is "priority defaults to MEDIUM"
        assertEquals(PurchaseRequest.Priority.MEDIUM, response.getPriority());
    }

    @Test
    @DisplayName("Contract: Get PR from different tenant → AccessDeniedException")
    void testGetPrCrossTenanTThrows() {
        // Arrange: PR from different tenant
        PurchaseRequest pr = PurchaseRequest.builder()
            .tenantId("OTHER_TENANT")
            .prNumber("PR-OTHER-2026-00001")
            .status(PurchaseRequest.PrStatus.DRAFT)
            .build();

        when(prRepository.findById(1L)).thenReturn(Optional.of(pr));

        // Act & Assert: contract is "tenant isolation enforced"
        AccessDeniedException exception = assertThrows(
            AccessDeniedException.class,
            () -> service.getPurchaseRequestById(1L)
        );
        assertTrue(exception.getMessage().contains("Not authorized"));
    }

    @Test
    @DisplayName("Contract: Delete PR from different tenant → AccessDeniedException")
    void testDeletePrCrossTenantThrows() {
        // Arrange: PR from different tenant
        PurchaseRequest pr = PurchaseRequest.builder()
            .tenantId("OTHER_TENANT")
            .prNumber("PR-OTHER-2026-00005")
            .status(PurchaseRequest.PrStatus.DRAFT)
            .build();

        when(prRepository.findById(5L)).thenReturn(Optional.of(pr));

        // Act & Assert: contract is "tenant isolation on delete"
        AccessDeniedException exception = assertThrows(
            AccessDeniedException.class,
            () -> service.deletePurchaseRequest(5L)
        );
        assertTrue(exception.getMessage().contains("Not authorized"));
    }

    @Test
    @DisplayName("Contract: ProcessInstanceId preserved in PR")
    void testCreatePrWithProcessInstanceId() {
        // Arrange: PR with processInstanceId
        String processInstanceId = "proc-123-456";
        PurchaseRequestRequest request = PurchaseRequestRequest.builder()
            .requestingDeptId(DEPARTMENT_ID)
            .requesterUserId(111L)
            .requestDate(LocalDate.now())
            .processInstanceId(processInstanceId)
            .build();

        PurchaseRequest savedPr = PurchaseRequest.builder()
            .tenantId(TENANT_ID)
            .prNumber("PR-ACME-2026-00006")
            .requestingDeptId(DEPARTMENT_ID)
            .processInstanceId(processInstanceId)
            .totalAmount(BigDecimal.ZERO)
            .status(PurchaseRequest.PrStatus.DRAFT)
            .build();

        when(numberGenerationService.generatePrNumber(TENANT_ID)).thenReturn("PR-ACME-2026-00006");
        when(prRepository.save(any(PurchaseRequest.class))).thenReturn(savedPr);
        when(lineItemRepository.findByPurchaseRequestIdAndTenantId(anyLong(), eq(TENANT_ID)))
            .thenReturn(Collections.emptyList());

        // Act
        PurchaseRequestResponse response = service.createPurchaseRequest(request);

        // Assert: contract is "processInstanceId preserved"
        assertEquals(processInstanceId, response.getProcessInstanceId());
    }
}
