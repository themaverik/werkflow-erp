package com.werkflow.business.procurement.repository;

import com.werkflow.business.procurement.entity.Vendor;
import com.werkflow.business.procurement.entity.Vendor.VendorStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

@Repository
public interface VendorRepository extends JpaRepository<Vendor, Long> {

    // Tenant-scoped methods
    Page<Vendor> findByTenantId(String tenantId, Pageable pageable);

    Page<Vendor> findByTenantIdAndStatus(String tenantId, VendorStatus status, Pageable pageable);

    Page<Vendor> findByTenantIdAndStatusIn(String tenantId, List<VendorStatus> statuses, Pageable pageable);

    @Query("SELECT v FROM Vendor v WHERE v.tenantId = :tenantId AND v.status = 'ACTIVE' ORDER BY v.rating DESC")
    Page<Vendor> findActiveVendorsOrderByRatingForTenant(@Param("tenantId") String tenantId, Pageable pageable);

    @Query("SELECT v FROM Vendor v WHERE v.tenantId = :tenantId AND v.status = 'ACTIVE' " +
           "AND (v.rating IS NULL OR v.rating >= :minRating)")
    Page<Vendor> findActiveVendorsByMinRatingForTenant(@Param("tenantId") String tenantId,
                                                       @Param("minRating") BigDecimal minRating,
                                                       Pageable pageable);

    @Query("SELECT v FROM Vendor v WHERE v.tenantId = :tenantId AND " +
           "(LOWER(v.name) LIKE LOWER(CONCAT('%', :searchTerm, '%')) " +
           "OR LOWER(v.contactPerson) LIKE LOWER(CONCAT('%', :searchTerm, '%')))")
    Page<Vendor> searchVendorsForTenant(@Param("tenantId") String tenantId,
                                        @Param("searchTerm") String searchTerm,
                                        Pageable pageable);

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
