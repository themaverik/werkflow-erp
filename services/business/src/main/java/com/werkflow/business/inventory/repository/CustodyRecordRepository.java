package com.werkflow.business.inventory.repository;

import com.werkflow.business.inventory.entity.AssetInstance;
import com.werkflow.business.inventory.entity.CustodyRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository for CustodyRecord entity.
 * Tenant-scoped query methods follow the pattern established in Tasks 4-6.
 */
@Repository
public interface CustodyRecordRepository extends JpaRepository<CustodyRecord, Long> {

    // Tenant-scoped methods
    List<CustodyRecord> findByTenantId(String tenantId);

    @Query("SELECT c FROM CustodyRecord c WHERE c.tenantId = :tenantId AND c.assetInstance = :assetInstance AND c.endDate IS NULL")
    Optional<CustodyRecord> findCurrentCustodyForTenant(@Param("tenantId") String tenantId,
                                                        @Param("assetInstance") AssetInstance assetInstance);

    @Query("SELECT c FROM CustodyRecord c WHERE c.tenantId = :tenantId AND c.assetInstance = :assetInstance ORDER BY c.startDate DESC")
    List<CustodyRecord> findByAssetInstanceForTenantOrderByStartDateDesc(@Param("tenantId") String tenantId,
                                                                         @Param("assetInstance") AssetInstance assetInstance);

    List<CustodyRecord> findByTenantIdAndCustodianDeptId(String tenantId, Long deptId);

    List<CustodyRecord> findByTenantIdAndCustodianUserId(String tenantId, Long userId);

    List<CustodyRecord> findByTenantIdAndCustodyType(String tenantId, String custodyType);

    @Query("SELECT c FROM CustodyRecord c WHERE c.tenantId = :tenantId AND c.endDate IS NULL")
    List<CustodyRecord> findActiveCustodyRecordsForTenant(@Param("tenantId") String tenantId);

    @Query("SELECT c FROM CustodyRecord c WHERE c.tenantId = :tenantId " +
           "AND c.custodyType = 'TEMPORARY' AND c.endDate <= :currentDate AND c.endDate IS NOT NULL")
    List<CustodyRecord> findOverdueTemporaryCustodyForTenant(@Param("tenantId") String tenantId,
                                                             @Param("currentDate") LocalDateTime currentDate);

    @Query("SELECT c FROM CustodyRecord c WHERE c.tenantId = :tenantId " +
           "AND c.custodianDeptId = :deptId AND c.endDate IS NULL")
    List<CustodyRecord> findActiveCustodyByDepartmentForTenant(@Param("tenantId") String tenantId,
                                                               @Param("deptId") Long deptId);

    // Legacy unscoped methods
    @Deprecated(forRemoval = false, since = "1.0.0")
    @Query("SELECT c FROM CustodyRecord c WHERE c.assetInstance = :assetInstance AND c.endDate IS NULL")
    Optional<CustodyRecord> findCurrentCustody(@Param("assetInstance") AssetInstance assetInstance);

    @Deprecated(forRemoval = false, since = "1.0.0")
    List<CustodyRecord> findByAssetInstanceOrderByStartDateDesc(AssetInstance assetInstance);

    @Deprecated(forRemoval = false, since = "1.0.0")
    List<CustodyRecord> findByCustodianDeptId(Long deptId);

    @Deprecated(forRemoval = false, since = "1.0.0")
    List<CustodyRecord> findByCustodianUserId(Long userId);

    @Deprecated(forRemoval = false, since = "1.0.0")
    List<CustodyRecord> findByCustodyType(String custodyType);

    @Deprecated(forRemoval = false, since = "1.0.0")
    @Query("SELECT c FROM CustodyRecord c WHERE c.endDate IS NULL")
    List<CustodyRecord> findActiveCustodyRecords();

    @Deprecated(forRemoval = false, since = "1.0.0")
    @Query("SELECT c FROM CustodyRecord c WHERE c.custodyType = 'TEMPORARY' AND c.endDate <= :currentDate AND c.endDate IS NOT NULL")
    List<CustodyRecord> findOverdueTemporaryCustody(@Param("currentDate") LocalDateTime currentDate);

    @Deprecated(forRemoval = false, since = "1.0.0")
    List<CustodyRecord> findByAssetInstanceAndCustodyType(AssetInstance assetInstance, String custodyType);

    @Deprecated(forRemoval = false, since = "1.0.0")
    List<CustodyRecord> findByCustodianDeptIdAndCustodianUserId(Long deptId, Long userId);

    @Deprecated(forRemoval = false, since = "1.0.0")
    @Query("SELECT c FROM CustodyRecord c WHERE c.custodianDeptId = :deptId AND c.endDate IS NULL")
    List<CustodyRecord> findActiveCustodyByDepartment(@Param("deptId") Long deptId);
}
