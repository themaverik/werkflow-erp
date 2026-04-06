package com.werkflow.business.procurement.repository;

import com.werkflow.business.procurement.entity.Vendor;
import com.werkflow.business.procurement.entity.Vendor.VendorStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

@Repository
public interface VendorRepository extends JpaRepository<Vendor, Long> {

    // Tenant-scoped methods
    List<Vendor> findByTenantId(String tenantId);

    List<Vendor> findByTenantIdAndStatus(String tenantId, VendorStatus status);

    List<Vendor> findByTenantIdAndStatusIn(String tenantId, List<VendorStatus> statuses);

    @Query("SELECT v FROM Vendor v WHERE v.tenantId = :tenantId AND v.status = 'ACTIVE' ORDER BY v.rating DESC")
    List<Vendor> findActiveVendorsOrderByRatingForTenant(@Param("tenantId") String tenantId);

    @Query("SELECT v FROM Vendor v WHERE v.tenantId = :tenantId AND v.status = 'ACTIVE' " +
           "AND (v.rating IS NULL OR v.rating >= :minRating)")
    List<Vendor> findActiveVendorsByMinRatingForTenant(@Param("tenantId") String tenantId,
                                                       @Param("minRating") BigDecimal minRating);

    @Query("SELECT v FROM Vendor v WHERE v.tenantId = :tenantId AND " +
           "(LOWER(v.name) LIKE LOWER(CONCAT('%', :searchTerm, '%')) " +
           "OR LOWER(v.contactPerson) LIKE LOWER(CONCAT('%', :searchTerm, '%')))")
    List<Vendor> searchVendorsForTenant(@Param("tenantId") String tenantId,
                                        @Param("searchTerm") String searchTerm);

    boolean existsByTenantIdAndNameIgnoreCase(String tenantId, String name);

    // Legacy unscoped methods
    @Deprecated(forRemoval = false, since = "1.0.0")
    List<Vendor> findByStatus(VendorStatus status);

    @Deprecated(forRemoval = false, since = "1.0.0")
    List<Vendor> findByStatusIn(List<VendorStatus> statuses);

    @Deprecated(forRemoval = false, since = "1.0.0")
    @Query("SELECT v FROM Vendor v WHERE v.status = 'ACTIVE' ORDER BY v.rating DESC")
    List<Vendor> findActiveVendorsOrderByRating();

    @Deprecated(forRemoval = false, since = "1.0.0")
    @Query("SELECT v FROM Vendor v WHERE v.status = 'ACTIVE' " +
           "AND (v.rating IS NULL OR v.rating >= :minRating)")
    List<Vendor> findActiveVendorsByMinRating(@Param("minRating") BigDecimal minRating);

    @Deprecated(forRemoval = false, since = "1.0.0")
    @Query("SELECT v FROM Vendor v WHERE LOWER(v.name) LIKE LOWER(CONCAT('%', :searchTerm, '%')) " +
           "OR LOWER(v.contactPerson) LIKE LOWER(CONCAT('%', :searchTerm, '%'))")
    List<Vendor> searchVendors(@Param("searchTerm") String searchTerm);

    @Deprecated(forRemoval = false, since = "1.0.0")
    boolean existsByNameIgnoreCase(String name);
}
