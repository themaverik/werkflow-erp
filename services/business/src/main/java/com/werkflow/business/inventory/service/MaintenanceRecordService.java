package com.werkflow.business.inventory.service;

import com.werkflow.business.common.context.TenantContext;
import com.werkflow.business.inventory.entity.AssetInstance;
import com.werkflow.business.inventory.entity.MaintenanceRecord;
import com.werkflow.business.inventory.repository.AssetInstanceRepository;
import com.werkflow.business.inventory.repository.MaintenanceRecordRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityNotFoundException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Service for MaintenanceRecord operations.
 * All queries are tenant-scoped via TenantContext.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class MaintenanceRecordService {

    private final MaintenanceRecordRepository maintenanceRepository;
    private final AssetInstanceRepository assetRepository;
    private final TenantContext tenantContext;

    private String getTenantId() {
        return tenantContext.getTenantId();
    }

    /**
     * Create a new maintenance record
     */
    @Transactional
    public MaintenanceRecord createMaintenanceRecord(MaintenanceRecord record) {
        String tenantId = getTenantId();
        log.info("Creating new maintenance record for asset: {} for tenant: {}",
            record.getAssetInstance().getAssetTag(), tenantId);

        // Validate that the asset instance belongs to the same tenant
        AssetInstance asset = assetRepository.findById(record.getAssetInstance().getId())
            .orElseThrow(() -> new EntityNotFoundException("Asset instance not found"));
        if (!asset.getTenantId().equals(tenantId)) {
            throw new AccessDeniedException("AssetInstance does not belong to the current tenant");
        }

        record.setTenantId(tenantId);
        MaintenanceRecord saved = maintenanceRepository.save(record);
        log.info("Maintenance record created with id: {} for tenant: {}", saved.getId(), tenantId);
        return saved;
    }

    /**
     * Get maintenance record by ID
     */
    public MaintenanceRecord getMaintenanceRecordById(Long id) {
        String tenantId = getTenantId();
        MaintenanceRecord record = maintenanceRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Maintenance record not found with id: " + id));
        if (!record.getTenantId().equals(tenantId)) {
            throw new AccessDeniedException("Not authorized to access this MaintenanceRecord");
        }
        return record;
    }

    /**
     * Get maintenance history for an asset
     */
    public List<MaintenanceRecord> getMaintenanceHistory(Long assetId) {
        String tenantId = getTenantId();
        AssetInstance asset = assetRepository.findById(assetId)
            .orElseThrow(() -> new EntityNotFoundException("Asset instance not found with id: " + assetId));
        if (!asset.getTenantId().equals(tenantId)) {
            throw new AccessDeniedException("AssetInstance does not belong to the current tenant");
        }

        return maintenanceRepository.findByAssetInstanceForTenantOrderByScheduledDateDesc(tenantId, asset);
    }

    /**
     * Get maintenance records by type
     */
    public List<MaintenanceRecord> getMaintenanceByType(String maintenanceType) {
        String tenantId = getTenantId();
        return maintenanceRepository.findByTenantIdAndMaintenanceType(tenantId, maintenanceType);
    }

    /**
     * Get incomplete maintenance records
     */
    public List<MaintenanceRecord> getIncompleteMaintenanceRecords() {
        String tenantId = getTenantId();
        return maintenanceRepository.findIncompleteMaintenanceRecordsForTenant(tenantId);
    }

    /**
     * Get overdue maintenance records
     */
    public List<MaintenanceRecord> getOverdueMaintenanceRecords() {
        String tenantId = getTenantId();
        return maintenanceRepository.findOverdueMaintenanceRecordsForTenant(tenantId, LocalDate.now());
    }

    /**
     * Get completed maintenance records
     */
    public List<MaintenanceRecord> getCompletedMaintenanceRecords() {
        String tenantId = getTenantId();
        return maintenanceRepository.findCompletedMaintenanceRecordsForTenant(tenantId);
    }

    /**
     * Get scheduled maintenance coming due
     */
    public List<MaintenanceRecord> getScheduledMaintenanceDue(LocalDate dueDate) {
        String tenantId = getTenantId();
        return maintenanceRepository.findScheduledMaintenanceDueForTenant(tenantId, dueDate);
    }

    /**
     * Get expensive maintenance records
     */
    public List<MaintenanceRecord> getExpensiveMaintenanceRecords(BigDecimal minCost) {
        String tenantId = getTenantId();
        return maintenanceRepository.findExpensiveMaintenanceRecordsForTenant(tenantId, minCost);
    }

    /**
     * Update maintenance record
     */
    @Transactional
    public MaintenanceRecord updateMaintenanceRecord(Long id, MaintenanceRecord recordDetails) {
        String tenantId = getTenantId();
        log.info("Updating maintenance record with id: {} for tenant: {}", id, tenantId);

        MaintenanceRecord record = maintenanceRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Maintenance record not found with id: " + id));
        if (!record.getTenantId().equals(tenantId)) {
            throw new AccessDeniedException("Not authorized to update this MaintenanceRecord");
        }

        record.setScheduledDate(recordDetails.getScheduledDate());
        record.setCompletedDate(recordDetails.getCompletedDate());
        record.setPerformedBy(recordDetails.getPerformedBy());
        record.setCost(recordDetails.getCost());
        record.setDescription(recordDetails.getDescription());
        record.setNextMaintenanceDate(recordDetails.getNextMaintenanceDate());

        return maintenanceRepository.save(record);
    }

    /**
     * Complete maintenance record
     */
    @Transactional
    public MaintenanceRecord completeMaintenanceRecord(Long id, LocalDate completedDate, LocalDate nextMaintenanceDate) {
        log.info("Completing maintenance record with id: {}", id);
        MaintenanceRecord record = getMaintenanceRecordById(id);
        record.setCompletedDate(completedDate);
        record.setNextMaintenanceDate(nextMaintenanceDate);
        return maintenanceRepository.save(record);
    }

    /**
     * Get all maintenance records
     */
    public List<MaintenanceRecord> getAllMaintenanceRecords() {
        String tenantId = getTenantId();
        return maintenanceRepository.findByTenantId(tenantId);
    }
}
