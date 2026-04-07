package com.werkflow.business.inventory.repository;

import com.werkflow.business.inventory.entity.AssetCategory;
import com.werkflow.business.inventory.entity.AssetDefinition;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/**
 * Repository for AssetDefinition entity.
 * Tenant-scoped query methods follow the pattern established in Tasks 4-6.
 */
@Repository
public interface AssetDefinitionRepository extends JpaRepository<AssetDefinition, Long> {

    // Tenant-scoped methods
    Page<AssetDefinition> findByTenantId(String tenantId, Pageable pageable);

    List<AssetDefinition> findByTenantIdAndActiveTrue(String tenantId);

    List<AssetDefinition> findByTenantIdAndCategoryIdAndActiveTrue(String tenantId, Long categoryId);

    List<AssetDefinition> findByTenantIdAndRequiresMaintenanceTrueAndActiveTrue(String tenantId);

    List<AssetDefinition> findByTenantIdAndManufacturerAndActiveTrue(String tenantId, String manufacturer);

    @Query("SELECT a FROM AssetDefinition a WHERE a.tenantId = :tenantId " +
           "AND a.unitCost >= :minPrice AND a.unitCost <= :maxPrice AND a.active = true")
    List<AssetDefinition> findByPriceRangeForTenant(@Param("tenantId") String tenantId,
                                                    @Param("minPrice") BigDecimal minPrice,
                                                    @Param("maxPrice") BigDecimal maxPrice);

    @Query("SELECT a FROM AssetDefinition a WHERE a.tenantId = :tenantId AND " +
           "(LOWER(a.name) LIKE LOWER(CONCAT('%', :searchTerm, '%')) " +
           "OR LOWER(a.sku) LIKE LOWER(CONCAT('%', :searchTerm, '%'))) " +
           "AND a.active = true")
    List<AssetDefinition> searchDefinitionsForTenant(@Param("tenantId") String tenantId,
                                                     @Param("searchTerm") String searchTerm);

    Optional<AssetDefinition> findByTenantIdAndSku(String tenantId, String sku);

    // Legacy unscoped methods
    @Deprecated(forRemoval = false, since = "1.0.0")
    Optional<AssetDefinition> findBySku(String sku);

    @Deprecated(forRemoval = false, since = "1.0.0")
    List<AssetDefinition> findByCategory(AssetCategory category);

    @Deprecated(forRemoval = false, since = "1.0.0")
    List<AssetDefinition> findByCategoryIdAndActiveTrue(Long categoryId);

    @Deprecated(forRemoval = false, since = "1.0.0")
    List<AssetDefinition> findByActiveTrue();

    @Deprecated(forRemoval = false, since = "1.0.0")
    List<AssetDefinition> findByRequiresMaintenanceTrueAndActiveTrue();

    @Deprecated(forRemoval = false, since = "1.0.0")
    List<AssetDefinition> findByManufacturerAndActiveTrue(String manufacturer);

    @Deprecated(forRemoval = false, since = "1.0.0")
    @Query("SELECT a FROM AssetDefinition a WHERE a.unitCost >= :minPrice AND a.unitCost <= :maxPrice AND a.active = true")
    List<AssetDefinition> findByPriceRange(@Param("minPrice") BigDecimal minPrice, @Param("maxPrice") BigDecimal maxPrice);

    @Deprecated(forRemoval = false, since = "1.0.0")
    @Query("SELECT a FROM AssetDefinition a WHERE " +
           "(LOWER(a.name) LIKE LOWER(CONCAT('%', :searchTerm, '%')) " +
           "OR LOWER(a.sku) LIKE LOWER(CONCAT('%', :searchTerm, '%'))) " +
           "AND a.active = true")
    List<AssetDefinition> searchDefinitions(@Param("searchTerm") String searchTerm);

    @Deprecated(forRemoval = false, since = "1.0.0")
    long countByCategory(AssetCategory category);
}
