package com.werkflow.business.inventory.service;

import com.werkflow.business.common.context.TenantContext;
import com.werkflow.business.common.validator.CrossDomainValidator;
import com.werkflow.business.inventory.entity.AssetInstance;
import com.werkflow.business.inventory.entity.CustodyRecord;
import com.werkflow.business.inventory.repository.AssetInstanceRepository;
import com.werkflow.business.inventory.repository.CustodyRecordRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityNotFoundException;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Service for CustodyRecord operations (Inter-department asset assignments).
 * All queries are tenant-scoped via TenantContext.
 *
 * <p>Cross-domain FK validation is performed for {@code custodianDeptId} via
 * {@link com.werkflow.business.common.validator.CrossDomainValidator}.
 * Validation is tenant-scoped: the department must exist and belong to the current tenant.
 * {@code custodianUserId} and {@code assignedByUserId} are not yet validated.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class CustodyRecordService {

    private final CustodyRecordRepository custodyRepository;
    private final AssetInstanceRepository assetRepository;
    private final TenantContext tenantContext;
    private final CrossDomainValidator validator;

    private String getTenantId() {
        return tenantContext.getTenantId();
    }

    /**
     * Create a new custody record
     */
    @Transactional
    public CustodyRecord createCustodyRecord(CustodyRecord record) {
        String tenantId = getTenantId();
        log.info("Creating new custody record for asset: {} for tenant: {}",
            record.getAssetInstance().getAssetTag(), tenantId);

        validator.validateDepartmentExists(record.getCustodianDeptId(), tenantId);

        // Validate that the asset instance belongs to the same tenant
        AssetInstance asset = assetRepository.findById(record.getAssetInstance().getId())
            .orElseThrow(() -> new EntityNotFoundException("Asset instance not found"));
        if (!asset.getTenantId().equals(tenantId)) {
            throw new AccessDeniedException("AssetInstance does not belong to the current tenant");
        }

        record.setTenantId(tenantId);
        CustodyRecord saved = custodyRepository.save(record);
        log.info("Custody record created with id: {} for tenant: {}", saved.getId(), tenantId);
        return saved;
    }

    /**
     * Get custody record by ID
     */
    public CustodyRecord getCustodyRecordById(Long id) {
        String tenantId = getTenantId();
        CustodyRecord record = custodyRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Custody record not found with id: " + id));
        if (!record.getTenantId().equals(tenantId)) {
            throw new AccessDeniedException("Not authorized to access this CustodyRecord");
        }
        return record;
    }

    /**
     * Get current custody for an asset
     */
    public CustodyRecord getCurrentCustody(Long assetId) {
        String tenantId = getTenantId();
        AssetInstance asset = assetRepository.findById(assetId)
            .orElseThrow(() -> new EntityNotFoundException("Asset instance not found with id: " + assetId));
        if (!asset.getTenantId().equals(tenantId)) {
            throw new AccessDeniedException("AssetInstance does not belong to the current tenant");
        }

        return custodyRepository.findCurrentCustodyForTenant(tenantId, asset)
            .orElseThrow(() -> new EntityNotFoundException("No active custody record found for asset: " + assetId));
    }

    /**
     * Get custody history for an asset
     */
    public List<CustodyRecord> getCustodyHistory(Long assetId) {
        String tenantId = getTenantId();
        AssetInstance asset = assetRepository.findById(assetId)
            .orElseThrow(() -> new EntityNotFoundException("Asset instance not found with id: " + assetId));
        if (!asset.getTenantId().equals(tenantId)) {
            throw new AccessDeniedException("AssetInstance does not belong to the current tenant");
        }

        return custodyRepository.findByAssetInstanceForTenantOrderByStartDateDesc(tenantId, asset);
    }

    /**
     * Get custody records by department
     */
    public List<CustodyRecord> getCustodyByDepartment(Long deptId) {
        String tenantId = getTenantId();
        return custodyRepository.findActiveCustodyByDepartmentForTenant(tenantId, deptId);
    }

    /**
     * Get custody records by user
     */
    public List<CustodyRecord> getCustodyByUser(Long userId) {
        String tenantId = getTenantId();
        return custodyRepository.findByTenantIdAndCustodianUserId(tenantId, userId);
    }

    /**
     * Get custody records by custody type
     */
    public List<CustodyRecord> getCustodyByType(String custodyType) {
        String tenantId = getTenantId();
        return custodyRepository.findByTenantIdAndCustodyType(tenantId, custodyType);
    }

    /**
     * Get all active custody records
     */
    public List<CustodyRecord> getActiveCustodyRecords() {
        String tenantId = getTenantId();
        return custodyRepository.findActiveCustodyRecordsForTenant(tenantId);
    }

    /**
     * Get overdue temporary custody records
     */
    public List<CustodyRecord> getOverdueTemporaryCustody() {
        String tenantId = getTenantId();
        return custodyRepository.findOverdueTemporaryCustodyForTenant(tenantId, LocalDateTime.now());
    }

    /**
     * End custody (return asset)
     */
    @Transactional
    public CustodyRecord endCustody(Long id, String returnCondition) {
        log.info("Ending custody record with id: {}", id);
        CustodyRecord record = getCustodyRecordById(id);
        record.setEndDate(LocalDateTime.now());
        record.setReturnCondition(AssetInstance.AssetCondition.valueOf(returnCondition));
        return custodyRepository.save(record);
    }

    /**
     * Update custody record
     */
    @Transactional
    public CustodyRecord updateCustodyRecord(Long id, CustodyRecord recordDetails) {
        String tenantId = getTenantId();
        log.info("Updating custody record with id: {} for tenant: {}", id, tenantId);

        CustodyRecord record = custodyRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Custody record not found with id: " + id));
        if (!record.getTenantId().equals(tenantId)) {
            throw new AccessDeniedException("Not authorized to update this CustodyRecord");
        }

        if (recordDetails.getCustodianDeptId() != null && !recordDetails.getCustodianDeptId().equals(record.getCustodianDeptId())) {
            validator.validateDepartmentExists(recordDetails.getCustodianDeptId(), tenantId);
            record.setCustodianDeptId(recordDetails.getCustodianDeptId());
        }

        record.setPhysicalLocation(recordDetails.getPhysicalLocation());
        record.setCustodyType(recordDetails.getCustodyType());
        record.setNotes(recordDetails.getNotes());

        return custodyRepository.save(record);
    }

    /**
     * Get all custody records
     */
    public List<CustodyRecord> getAllCustodyRecords() {
        String tenantId = getTenantId();
        return custodyRepository.findByTenantId(tenantId);
    }
}
