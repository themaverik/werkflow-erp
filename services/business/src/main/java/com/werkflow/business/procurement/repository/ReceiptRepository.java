package com.werkflow.business.procurement.repository;

import com.werkflow.business.procurement.entity.Receipt;
import com.werkflow.business.procurement.entity.Receipt.ReceiptStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface ReceiptRepository extends JpaRepository<Receipt, Long> {

    // Tenant-scoped methods
    Page<Receipt> findByTenantId(String tenantId, Pageable pageable);

    Optional<Receipt> findByReceiptNumberAndTenantId(String receiptNumber, String tenantId);

    Page<Receipt> findByPurchaseOrderIdAndTenantId(Long purchaseOrderId, String tenantId, Pageable pageable);

    Page<Receipt> findByTenantIdAndStatus(String tenantId, ReceiptStatus status, Pageable pageable);

    Page<Receipt> findByTenantIdAndReceivedByUserId(String tenantId, Long userId, Pageable pageable);

    @Query("SELECT r FROM Receipt r WHERE r.tenantId = :tenantId AND r.status = 'DISCREPANCY'")
    Page<Receipt> findReceiptsWithDiscrepanciesForTenant(@Param("tenantId") String tenantId, Pageable pageable);

    @Query("SELECT r FROM Receipt r WHERE r.tenantId = :tenantId " +
           "AND r.receiptDate BETWEEN :startDate AND :endDate ORDER BY r.receiptDate DESC")
    Page<Receipt> findByTenantIdAndReceiptDateRange(
        @Param("tenantId") String tenantId,
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate,
        Pageable pageable
    );

    @Query("SELECT r FROM Receipt r WHERE r.tenantId = :tenantId " +
           "AND r.purchaseOrder.vendor.id = :vendorId ORDER BY r.receiptDate DESC")
    Page<Receipt> findByTenantIdAndVendorId(@Param("tenantId") String tenantId,
                                             @Param("vendorId") Long vendorId,
                                             Pageable pageable);

    boolean existsByReceiptNumberAndTenantId(String receiptNumber, String tenantId);

    // Legacy unscoped methods
    @Deprecated(forRemoval = false, since = "1.0.0")
    Optional<Receipt> findByReceiptNumber(String receiptNumber);

    @Deprecated(forRemoval = false, since = "1.0.0")
    List<Receipt> findByPurchaseOrderId(Long purchaseOrderId);

    @Deprecated(forRemoval = false, since = "1.0.0")
    List<Receipt> findByStatus(ReceiptStatus status);

    @Deprecated(forRemoval = false, since = "1.0.0")
    List<Receipt> findByReceivedByUserId(Long userId);

    @Deprecated(forRemoval = false, since = "1.0.0")
    @Query("SELECT r FROM Receipt r WHERE r.status = 'DISCREPANCY'")
    List<Receipt> findReceiptsWithDiscrepancies();

    @Deprecated(forRemoval = false, since = "1.0.0")
    @Query("SELECT r FROM Receipt r WHERE r.receiptDate BETWEEN :startDate AND :endDate " +
           "ORDER BY r.receiptDate DESC")
    List<Receipt> findByReceiptDateRange(
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate
    );

    @Deprecated(forRemoval = false, since = "1.0.0")
    @Query("SELECT r FROM Receipt r " +
           "WHERE r.purchaseOrder.vendor.id = :vendorId " +
           "ORDER BY r.receiptDate DESC")
    List<Receipt> findByVendorId(@Param("vendorId") Long vendorId);

    @Deprecated(forRemoval = false, since = "1.0.0")
    boolean existsByReceiptNumber(String receiptNumber);
}
