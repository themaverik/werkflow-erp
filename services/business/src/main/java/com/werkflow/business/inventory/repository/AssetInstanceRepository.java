package com.werkflow.business.inventory.repository;

import com.werkflow.business.inventory.entity.AssetDefinition;
import com.werkflow.business.inventory.entity.AssetInstance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Repository for AssetInstance entity.
 * Tenant-scoped query methods follow the pattern established in Tasks 4-6.
 */
@Repository
public interface AssetInstanceRepository extends JpaRepository<AssetInstance, Long> {

    // Tenant-scoped methods
    List<AssetInstance> findByTenantId(String tenantId);

    List<AssetInstance> findByTenantIdAndStatus(String tenantId, String status);

    List<AssetInstance> findByTenantIdAndCondition(String tenantId, String condition);

    List<AssetInstance> findByTenantIdAndCurrentLocation(String tenantId, String location);

    List<AssetInstance> findByAssetDefinitionIdAndTenantId(Long assetDefinitionId, String tenantId);

    Optional<AssetInstance> findByTenantIdAndAssetTag(String tenantId, String assetTag);

    @Query("SELECT a FROM AssetInstance a WHERE a.tenantId = :tenantId AND a.status = 'AVAILABLE'")
    List<AssetInstance> findAvailableAssetsForTenant(@Param("tenantId") String tenantId);

    @Query("SELECT a FROM AssetInstance a WHERE a.tenantId = :tenantId AND a.status = 'IN_USE'")
    List<AssetInstance> findAssetsInUseForTenant(@Param("tenantId") String tenantId);

    @Query("SELECT a FROM AssetInstance a WHERE a.tenantId = :tenantId AND a.status = 'MAINTENANCE'")
    List<AssetInstance> findAssetsRequiringMaintenanceForTenant(@Param("tenantId") String tenantId);

    @Query("SELECT a FROM AssetInstance a WHERE a.tenantId = :tenantId " +
           "AND a.warrantyExpiryDate <= :expiryDate AND a.status != 'DISPOSED' AND a.status != 'RETIRED'")
    List<AssetInstance> findAssetsWithExpiringWarrantyForTenant(@Param("tenantId") String tenantId,
                                                                @Param("expiryDate") LocalDate expiryDate);

    @Query("SELECT a FROM AssetInstance a WHERE a.tenantId = :tenantId " +
           "AND a.condition IN ('POOR', 'DAMAGED', 'NEEDS_REPAIR')")
    List<AssetInstance> findAssetsNeedingAttentionForTenant(@Param("tenantId") String tenantId);

    @Query("SELECT a FROM AssetInstance a WHERE a.tenantId = :tenantId AND " +
           "(LOWER(a.assetTag) LIKE LOWER(CONCAT('%', :searchTerm, '%')) " +
           "OR LOWER(a.serialNumber) LIKE LOWER(CONCAT('%', :searchTerm, '%')))")
    List<AssetInstance> searchAssetsForTenant(@Param("tenantId") String tenantId,
                                              @Param("searchTerm") String searchTerm);

    // Legacy unscoped methods
    @Deprecated(forRemoval = false, since = "1.0.0")
    Optional<AssetInstance> findByAssetTag(String assetTag);

    @Deprecated(forRemoval = false, since = "1.0.0")
    List<AssetInstance> findByAssetDefinition(AssetDefinition assetDefinition);

    @Deprecated(forRemoval = false, since = "1.0.0")
    List<AssetInstance> findByStatus(String status);

    @Deprecated(forRemoval = false, since = "1.0.0")
    List<AssetInstance> findByCondition(String condition);

    @Deprecated(forRemoval = false, since = "1.0.0")
    List<AssetInstance> findByCurrentLocation(String location);

    @Deprecated(forRemoval = false, since = "1.0.0")
    @Query("SELECT a FROM AssetInstance a WHERE a.status = 'AVAILABLE'")
    List<AssetInstance> findAvailableAssets();

    @Deprecated(forRemoval = false, since = "1.0.0")
    @Query("SELECT a FROM AssetInstance a WHERE a.status = 'IN_USE'")
    List<AssetInstance> findAssetsInUse();

    @Deprecated(forRemoval = false, since = "1.0.0")
    @Query("SELECT a FROM AssetInstance a WHERE a.status = 'MAINTENANCE'")
    List<AssetInstance> findAssetsRequiringMaintenance();

    @Deprecated(forRemoval = false, since = "1.0.0")
    @Query("SELECT a FROM AssetInstance a WHERE a.warrantyExpiryDate <= :expiryDate AND a.status != 'DISPOSED' AND a.status != 'RETIRED'")
    List<AssetInstance> findAssetsWithExpiringWarranty(@Param("expiryDate") LocalDate expiryDate);

    @Deprecated(forRemoval = false, since = "1.0.0")
    List<AssetInstance> findByAssetDefinitionAndStatus(AssetDefinition assetDefinition, String status);

    @Deprecated(forRemoval = false, since = "1.0.0")
    @Query("SELECT a FROM AssetInstance a WHERE " +
           "LOWER(a.assetTag) LIKE LOWER(CONCAT('%', :searchTerm, '%')) " +
           "OR LOWER(a.serialNumber) LIKE LOWER(CONCAT('%', :searchTerm, '%'))")
    List<AssetInstance> searchAssets(@Param("searchTerm") String searchTerm);

    @Deprecated(forRemoval = false, since = "1.0.0")
    long countByStatus(String status);

    @Deprecated(forRemoval = false, since = "1.0.0")
    long countByAssetDefinitionIdAndStatus(Long assetDefinitionId, String status);

    @Deprecated(forRemoval = false, since = "1.0.0")
    @Query("SELECT a FROM AssetInstance a WHERE a.condition IN ('POOR', 'DAMAGED', 'NEEDS_REPAIR')")
    List<AssetInstance> findAssetsNeedingAttention();
}
