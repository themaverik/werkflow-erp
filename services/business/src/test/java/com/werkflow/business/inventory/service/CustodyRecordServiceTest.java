package com.werkflow.business.inventory.service;

import com.werkflow.business.common.context.TenantContext;
import com.werkflow.business.common.validator.CrossDomainValidator;
import com.werkflow.business.inventory.entity.AssetInstance;
import com.werkflow.business.inventory.entity.CustodyRecord;
import com.werkflow.business.inventory.repository.AssetInstanceRepository;
import com.werkflow.business.inventory.repository.CustodyRecordRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CustodyRecordServiceTest {

    @Mock
    private CustodyRecordRepository custodyRepository;

    @Mock
    private AssetInstanceRepository assetRepository;

    @Mock
    private TenantContext tenantContext;

    @Mock
    private CrossDomainValidator validator;

    @InjectMocks
    private CustodyRecordService custodyRecordService;

    private static final String TENANT_ID = "ACME";
    private static final LocalDateTime FIXED_DATE = LocalDateTime.of(2026, 1, 15, 10, 30, 0);

    @Test
    void createCustodyRecord_withInvalidDepartmentId_throwsException() {
        // Given: record with non-existent department
        AssetInstance asset = AssetInstance.builder()
            .id(1L)
            .tenantId(TENANT_ID)
            .assetTag("ASSET-001")
            .build();

        CustodyRecord record = CustodyRecord.builder()
            .assetInstance(asset)
            .custodianDeptId(99999L)
            .custodyType(CustodyRecord.CustodyType.PERMANENT)
            .startDate(FIXED_DATE)
            .build();

        when(tenantContext.getTenantId()).thenReturn(TENANT_ID);
        doThrow(new EntityNotFoundException("Department not found: id=99999 for tenant=" + TENANT_ID))
            .when(validator).validateDepartmentExists(99999L, TENANT_ID);

        // When/Then: should throw EntityNotFoundException
        EntityNotFoundException exception = assertThrows(
            EntityNotFoundException.class,
            () -> custodyRecordService.createCustodyRecord(record)
        );

        assertTrue(exception.getMessage().contains("Department not found"));
        assertTrue(exception.getMessage().contains("99999"));
    }

    @Test
    void createCustodyRecord_withValidDepartmentId_succeeds() {
        // Given: record with valid department
        Long validDeptId = 1L;

        AssetInstance asset = AssetInstance.builder()
            .id(1L)
            .tenantId(TENANT_ID)
            .assetTag("ASSET-001")
            .build();

        CustodyRecord record = CustodyRecord.builder()
            .assetInstance(asset)
            .custodianDeptId(validDeptId)
            .custodyType(CustodyRecord.CustodyType.PERMANENT)
            .startDate(FIXED_DATE)
            .build();

        CustodyRecord savedRecord = CustodyRecord.builder()
            .id(1L)
            .tenantId(TENANT_ID)
            .assetInstance(asset)
            .custodianDeptId(validDeptId)
            .custodyType(CustodyRecord.CustodyType.PERMANENT)
            .startDate(FIXED_DATE)
            .build();

        when(tenantContext.getTenantId()).thenReturn(TENANT_ID);
        when(assetRepository.findById(1L)).thenReturn(Optional.of(asset));
        when(custodyRepository.save(any(CustodyRecord.class))).thenReturn(savedRecord);

        // When: create is called
        CustodyRecord response = custodyRecordService.createCustodyRecord(record);

        // Then: should succeed
        assertNotNull(response.getId());
        assertEquals(validDeptId, response.getCustodianDeptId());

        // Verify validator was invoked with correct arguments
        verify(validator).validateDepartmentExists(validDeptId, TENANT_ID);

        // Verify additional response fields
        assertEquals(TENANT_ID, response.getTenantId());
        assertEquals(CustodyRecord.CustodyType.PERMANENT, response.getCustodyType());
        assertEquals(FIXED_DATE, response.getStartDate());
        assertEquals("ASSET-001", response.getAssetInstance().getAssetTag());
    }
}
