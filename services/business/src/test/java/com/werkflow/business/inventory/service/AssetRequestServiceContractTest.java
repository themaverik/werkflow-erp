package com.werkflow.business.inventory.service;

import com.werkflow.business.common.context.UserContext;
import com.werkflow.business.hr.entity.OfficeLocation;
import com.werkflow.business.inventory.dto.AssetRequestDto;
import com.werkflow.business.inventory.dto.AssetRequestResponse;
import com.werkflow.business.inventory.entity.AssetRequest;
import com.werkflow.business.inventory.entity.AssetRequestStatus;
import com.werkflow.business.inventory.repository.AssetCategoryRepository;
import com.werkflow.business.inventory.repository.AssetDefinitionRepository;
import com.werkflow.business.inventory.repository.AssetRequestRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@DisplayName("AssetRequestService Contract Tests")
class AssetRequestServiceContractTest {

    @Mock
    private AssetRequestRepository assetRequestRepository;

    @Mock
    private AssetDefinitionRepository assetDefinitionRepository;

    @Mock
    private AssetCategoryRepository assetCategoryRepository;

    private AssetRequestService service;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        service = new AssetRequestService(
            assetRequestRepository,
            assetDefinitionRepository,
            assetCategoryRepository
        );
    }

    @Test
    @DisplayName("Contract: Create request defaults to PENDING status")
    void testCreateRequestDefaultPending() {
        // Arrange: create request without explicit status
        AssetRequestDto dto = AssetRequestDto.builder()
            .requesterUserId("user-123")
            .requesterName("John Doe")
            .requesterEmail("john@example.com")
            .requesterPhone("555-1234")
            .departmentCode("IT")
            .officeLocation(OfficeLocation.SEATTLE_US)
            .assetDefinitionId(1L)
            .assetCategoryId(1L)
            .quantity(1)
            .procurementRequired(false)
            .approxPrice(new BigDecimal("500.00"))
            .deliveryDate(LocalDate.now().plusDays(7))
            .justification("New hire requirement")
            .build();

        AssetRequest savedRequest = AssetRequest.builder()
            .requesterUserId("user-123")
            .requesterName("John Doe")
            .requesterEmail("john@example.com")
            .departmentCode("IT")
            .officeLocation(OfficeLocation.SEATTLE_US)
            .assetDefinitionId(1L)
            .assetCategoryId(1L)
            .quantity(1)
            .status(AssetRequestStatus.PENDING) // default
            .build();

        when(assetRequestRepository.save(any(AssetRequest.class))).thenReturn(savedRequest);

        // Act
        AssetRequestResponse response = service.createRequest(dto);

        // Assert: contract is "default status is PENDING"
        assertNotNull(response);
        assertEquals(AssetRequestStatus.PENDING, response.getStatus());
    }

    @Test
    @DisplayName("Contract: Approve request sets status and approver")
    void testApproveRequestSuccess() {
        // Arrange: approve by processInstanceId
        String processInstanceId = "proc-789";
        String approverUserId = "approver-456";

        AssetRequest existingRequest = AssetRequest.builder()
            .processInstanceId(processInstanceId)
            .requesterUserId("user-234")
            .requesterName("Jane Smith")
            .status(AssetRequestStatus.PENDING)
            .quantity(2)
            .officeLocation(OfficeLocation.BANGALORE_IN)
            .build();

        AssetRequest approvedRequest = AssetRequest.builder()
            .processInstanceId(processInstanceId)
            .requesterUserId("user-234")
            .requesterName("Jane Smith")
            .status(AssetRequestStatus.APPROVED) // changed
            .approvedByUserId(approverUserId) // set
            .quantity(2)
            .officeLocation(OfficeLocation.BANGALORE_IN)
            .build();

        when(assetRequestRepository.findByProcessInstanceId(processInstanceId))
            .thenReturn(Optional.of(existingRequest));
        when(assetRequestRepository.save(any(AssetRequest.class))).thenReturn(approvedRequest);

        // Act
        AssetRequestResponse response = service.approveRequest(processInstanceId, approverUserId);

        // Assert: contract is "approve sets status and approver"
        assertEquals(AssetRequestStatus.APPROVED, response.getStatus());
        assertEquals(approverUserId, response.getApprovedByUserId());
    }

    @Test
    @DisplayName("Contract: Reject request sets status and reason")
    void testRejectRequestSuccess() {
        // Arrange: reject with reason
        String processInstanceId = "proc-999";
        String approverUserId = "approver-789";
        String rejectionReason = "Insufficient budget";

        AssetRequest existingRequest = AssetRequest.builder()
            .processInstanceId(processInstanceId)
            .requesterUserId("user-345")
            .requesterName("Bob Johnson")
            .status(AssetRequestStatus.PENDING)
            .officeLocation(OfficeLocation.SEATTLE_US)
            .build();

        AssetRequest rejectedRequest = AssetRequest.builder()
            .processInstanceId(processInstanceId)
            .requesterUserId("user-345")
            .requesterName("Bob Johnson")
            .status(AssetRequestStatus.REJECTED) // changed
            .approvedByUserId(approverUserId)
            .rejectionReason(rejectionReason) // set
            .officeLocation(OfficeLocation.SEATTLE_US)
            .build();

        when(assetRequestRepository.findByProcessInstanceId(processInstanceId))
            .thenReturn(Optional.of(existingRequest));
        when(assetRequestRepository.save(any(AssetRequest.class))).thenReturn(rejectedRequest);

        // Act
        AssetRequestResponse response = service.rejectRequest(processInstanceId, approverUserId, rejectionReason);

        // Assert: contract is "reject sets status, approver, and reason"
        assertEquals(AssetRequestStatus.REJECTED, response.getStatus());
        assertEquals(rejectionReason, response.getRejectionReason());
        assertEquals(approverUserId, response.getApprovedByUserId());
    }

    @Test
    @DisplayName("Contract: Get non-existent request throws EntityNotFoundException")
    void testGetNonExistentRequestThrows() {
        // Arrange: request doesn't exist
        when(assetRequestRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert: contract is "non-existent throws error"
        EntityNotFoundException exception = assertThrows(
            EntityNotFoundException.class,
            () -> service.getRequestById(999L)
        );
        assertTrue(exception.getMessage().contains("AssetRequest not found"));
    }

    @Test
    @DisplayName("Contract: Update processInstanceId succeeds")
    void testUpdateProcessInstanceIdSuccess() {
        // Arrange: update processInstanceId
        Long requestId = 4L;
        String newProcessInstanceId = "proc-new-123";

        AssetRequest existingRequest = AssetRequest.builder()
            .requesterUserId("user-456")
            .requesterName("Alice Brown")
            .status(AssetRequestStatus.PENDING)
            .processInstanceId(null)
            .officeLocation(OfficeLocation.SEATTLE_US)
            .build();

        AssetRequest updatedRequest = AssetRequest.builder()
            .requesterUserId("user-456")
            .requesterName("Alice Brown")
            .status(AssetRequestStatus.PENDING)
            .processInstanceId(newProcessInstanceId) // updated
            .officeLocation(OfficeLocation.SEATTLE_US)
            .build();

        when(assetRequestRepository.findById(requestId)).thenReturn(Optional.of(existingRequest));
        when(assetRequestRepository.save(any(AssetRequest.class))).thenReturn(updatedRequest);

        // Act
        AssetRequestResponse response = service.updateProcessInstanceId(requestId, newProcessInstanceId);

        // Assert: contract is "processInstanceId updated"
        assertEquals(newProcessInstanceId, response.getProcessInstanceId());
    }

    @Test
    @DisplayName("Contract: Initiate procurement changes status")
    void testInitiateProcurementSuccess() {
        // Arrange: initiate procurement
        String processInstanceId = "proc-procurement-456";

        AssetRequest approvedRequest = AssetRequest.builder()
            .processInstanceId(processInstanceId)
            .requesterUserId("user-567")
            .requesterName("Charlie Davis")
            .status(AssetRequestStatus.APPROVED)
            .officeLocation(OfficeLocation.BANGALORE_IN)
            .build();

        AssetRequest procurementRequest = AssetRequest.builder()
            .processInstanceId(processInstanceId)
            .requesterUserId("user-567")
            .requesterName("Charlie Davis")
            .status(AssetRequestStatus.PROCUREMENT_INITIATED) // changed
            .officeLocation(OfficeLocation.BANGALORE_IN)
            .build();

        when(assetRequestRepository.findByProcessInstanceId(processInstanceId))
            .thenReturn(Optional.of(approvedRequest));
        when(assetRequestRepository.save(any(AssetRequest.class))).thenReturn(procurementRequest);

        // Act
        AssetRequestResponse response = service.initiateProcurement(processInstanceId);

        // Assert: contract is "status changes to PROCUREMENT_INITIATED"
        assertEquals(AssetRequestStatus.PROCUREMENT_INITIATED, response.getStatus());
    }
}
