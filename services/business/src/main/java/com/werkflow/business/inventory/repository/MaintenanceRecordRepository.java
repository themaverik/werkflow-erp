package com.werkflow.business.inventory.repository;

import com.werkflow.business.inventory.entity.AssetInstance;
import com.werkflow.business.inventory.entity.MaintenanceRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Repository for MaintenanceRecord entity.
 * Tenant-scoped query methods follow the pattern established in Tasks 4-6.
 */
@Repository
public interface MaintenanceRecordRepository extends JpaRepository<MaintenanceRecord, Long> {

    // Tenant-scoped methods
    List<MaintenanceRecord> findByTenantId(String tenantId);

    List<MaintenanceRecord> findByTenantIdAndMaintenanceType(String tenantId, String maintenanceType);

    @Query("SELECT m FROM MaintenanceRecord m WHERE m.tenantId = :tenantId " +
           "AND m.assetInstance = :assetInstance ORDER BY m.scheduledDate DESC")
    List<MaintenanceRecord> findByAssetInstanceForTenantOrderByScheduledDateDesc(@Param("tenantId") String tenantId,
                                                                                 @Param("assetInstance") AssetInstance assetInstance);

    @Query("SELECT m FROM MaintenanceRecord m WHERE m.tenantId = :tenantId " +
           "AND m.scheduledDate BETWEEN :startDate AND :endDate")
    List<MaintenanceRecord> findByScheduledDateRangeForTenant(@Param("tenantId") String tenantId,
                                                              @Param("startDate") LocalDate startDate,
                                                              @Param("endDate") LocalDate endDate);

    @Query("SELECT m FROM MaintenanceRecord m WHERE m.tenantId = :tenantId AND m.completedDate IS NULL")
    List<MaintenanceRecord> findIncompleteMaintenanceRecordsForTenant(@Param("tenantId") String tenantId);

    @Query("SELECT m FROM MaintenanceRecord m WHERE m.tenantId = :tenantId " +
           "AND m.scheduledDate <= :currentDate AND m.completedDate IS NULL")
    List<MaintenanceRecord> findOverdueMaintenanceRecordsForTenant(@Param("tenantId") String tenantId,
                                                                   @Param("currentDate") LocalDate currentDate);

    @Query("SELECT m FROM MaintenanceRecord m WHERE m.tenantId = :tenantId AND m.completedDate IS NOT NULL")
    List<MaintenanceRecord> findCompletedMaintenanceRecordsForTenant(@Param("tenantId") String tenantId);

    @Query("SELECT m FROM MaintenanceRecord m WHERE m.tenantId = :tenantId " +
           "AND m.nextMaintenanceDate <= :dueDate AND m.completedDate IS NOT NULL")
    List<MaintenanceRecord> findScheduledMaintenanceDueForTenant(@Param("tenantId") String tenantId,
                                                                 @Param("dueDate") LocalDate dueDate);

    @Query("SELECT m FROM MaintenanceRecord m WHERE m.tenantId = :tenantId AND m.cost >= :minCost")
    List<MaintenanceRecord> findExpensiveMaintenanceRecordsForTenant(@Param("tenantId") String tenantId,
                                                                     @Param("minCost") BigDecimal minCost);

    // Legacy unscoped methods
    @Deprecated(forRemoval = false, since = "1.0.0")
    List<MaintenanceRecord> findByAssetInstanceOrderByScheduledDateDesc(AssetInstance assetInstance);

    @Deprecated(forRemoval = false, since = "1.0.0")
    List<MaintenanceRecord> findByMaintenanceType(String maintenanceType);

    @Deprecated(forRemoval = false, since = "1.0.0")
    @Query("SELECT m FROM MaintenanceRecord m WHERE m.scheduledDate BETWEEN :startDate AND :endDate")
    List<MaintenanceRecord> findByScheduledDateRange(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    @Deprecated(forRemoval = false, since = "1.0.0")
    @Query("SELECT m FROM MaintenanceRecord m WHERE m.completedDate IS NULL")
    List<MaintenanceRecord> findIncompleteMaintenanceRecords();

    @Deprecated(forRemoval = false, since = "1.0.0")
    @Query("SELECT m FROM MaintenanceRecord m WHERE m.scheduledDate <= :currentDate AND m.completedDate IS NULL")
    List<MaintenanceRecord> findOverdueMaintenanceRecords(@Param("currentDate") LocalDate currentDate);

    @Deprecated(forRemoval = false, since = "1.0.0")
    @Query("SELECT m FROM MaintenanceRecord m WHERE m.completedDate IS NOT NULL")
    List<MaintenanceRecord> findCompletedMaintenanceRecords();

    @Deprecated(forRemoval = false, since = "1.0.0")
    List<MaintenanceRecord> findByAssetInstanceAndMaintenanceType(AssetInstance assetInstance, String maintenanceType);

    @Deprecated(forRemoval = false, since = "1.0.0")
    List<MaintenanceRecord> findByPerformedBy(String performedBy);

    @Deprecated(forRemoval = false, since = "1.0.0")
    @Query("SELECT m FROM MaintenanceRecord m WHERE m.nextMaintenanceDate <= :dueDate AND m.completedDate IS NOT NULL")
    List<MaintenanceRecord> findScheduledMaintenanceDue(@Param("dueDate") LocalDate dueDate);

    @Deprecated(forRemoval = false, since = "1.0.0")
    @Query("SELECT m FROM MaintenanceRecord m WHERE m.cost >= :minCost")
    List<MaintenanceRecord> findExpensiveMaintenanceRecords(@Param("minCost") BigDecimal minCost);
}
