package com.werkflow.business.inventory.repository;

import com.werkflow.business.inventory.entity.AssetCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for AssetCategory entity.
 * Tenant-scoped query methods follow the pattern established in Tasks 4-6.
 */
@Repository
public interface AssetCategoryRepository extends JpaRepository<AssetCategory, Long> {

    // Tenant-scoped methods
    List<AssetCategory> findByTenantId(String tenantId);

    List<AssetCategory> findByTenantIdAndActiveTrue(String tenantId);

    List<AssetCategory> findByTenantIdAndActiveFalse(String tenantId);

    @Query("SELECT c FROM AssetCategory c WHERE c.tenantId = :tenantId AND c.parentCategory IS NULL AND c.active = true")
    List<AssetCategory> findRootCategoriesForTenant(@Param("tenantId") String tenantId);

    List<AssetCategory> findByTenantIdAndParentCategoryIsNull(String tenantId);

    List<AssetCategory> findByTenantIdAndParentCategoryIsNotNullAndActiveTrue(String tenantId);

    List<AssetCategory> findByTenantIdAndParentCategoryCode(String tenantId, String parentCode);

    List<AssetCategory> findByTenantIdAndParentCategoryIdAndActiveTrue(String tenantId, Long parentCategoryId);

    List<AssetCategory> findByTenantIdAndCustodianDeptCode(String tenantId, String custodianDeptCode);

    List<AssetCategory> findByTenantIdAndRequiresApprovalTrueAndActiveTrue(String tenantId);

    @Query("SELECT c FROM AssetCategory c WHERE c.tenantId = :tenantId AND " +
           "(LOWER(c.name) LIKE LOWER(CONCAT('%', :searchTerm, '%')) " +
           "OR LOWER(c.code) LIKE LOWER(CONCAT('%', :searchTerm, '%'))) " +
           "AND c.active = true")
    List<AssetCategory> searchCategoriesForTenant(@Param("tenantId") String tenantId,
                                                  @Param("searchTerm") String searchTerm);

    Optional<AssetCategory> findByTenantIdAndCode(String tenantId, String code);

    Optional<AssetCategory> findByTenantIdAndName(String tenantId, String name);

    // Legacy unscoped methods
    @Deprecated(forRemoval = false, since = "1.0.0")
    Optional<AssetCategory> findByCode(String code);

    @Deprecated(forRemoval = false, since = "1.0.0")
    Optional<AssetCategory> findByName(String name);

    @Deprecated(forRemoval = false, since = "1.0.0")
    List<AssetCategory> findByActiveTrue();

    @Deprecated(forRemoval = false, since = "1.0.0")
    List<AssetCategory> findByActiveFalse();

    @Deprecated(forRemoval = false, since = "1.0.0")
    @Query("SELECT c FROM AssetCategory c WHERE c.parentCategory IS NULL AND c.active = true")
    List<AssetCategory> findRootCategories();

    @Deprecated(forRemoval = false, since = "1.0.0")
    List<AssetCategory> findByParentCategoryIsNull();

    @Deprecated(forRemoval = false, since = "1.0.0")
    List<AssetCategory> findByParentCategoryIsNotNullAndActiveTrue();

    @Deprecated(forRemoval = false, since = "1.0.0")
    List<AssetCategory> findByParentCategoryCode(String parentCode);

    @Deprecated(forRemoval = false, since = "1.0.0")
    List<AssetCategory> findByParentCategoryIdAndActiveTrue(Long parentCategoryId);

    @Deprecated(forRemoval = false, since = "1.0.0")
    List<AssetCategory> findByCustodianDeptCode(String custodianDeptCode);

    @Deprecated(forRemoval = false, since = "1.0.0")
    List<AssetCategory> findByRequiresApprovalTrueAndActiveTrue();

    @Deprecated(forRemoval = false, since = "1.0.0")
    @Query("SELECT c FROM AssetCategory c WHERE " +
           "(LOWER(c.name) LIKE LOWER(CONCAT('%', :searchTerm, '%')) " +
           "OR LOWER(c.code) LIKE LOWER(CONCAT('%', :searchTerm, '%'))) " +
           "AND c.active = true")
    List<AssetCategory> searchCategories(@Param("searchTerm") String searchTerm);
}
