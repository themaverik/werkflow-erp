package com.werkflow.business.procurement.repository;

import com.werkflow.business.procurement.entity.Receipt;
import com.werkflow.business.procurement.entity.Receipt.ReceiptStatus;
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
    List<Receipt> findByTenantId(String tenantId);

    Optional<Receipt> findByReceiptNumberAndTenantId(String receiptNumber, String tenantId);

    List<Receipt> findByPurchaseOrderIdAndTenantId(Long purchaseOrderId, String tenantId);

    List<Receipt> findByTenantIdAndStatus(String tenantId, ReceiptStatus status);

    List<Receipt> findByTenantIdAndReceivedByUserId(String tenantId, Long userId);

    @Query("SELECT r FROM Receipt r WHERE r.tenantId = :tenantId AND r.status = 'DISCREPANCY'")
    List<Receipt> findReceiptsWithDiscrepanciesForTenant(@Param("tenantId") String tenantId);

    @Query("SELECT r FROM Receipt r WHERE r.tenantId = :tenantId " +
           "AND r.receiptDate BETWEEN :startDate AND :endDate ORDER BY r.receiptDate DESC")
    List<Receipt> findByTenantIdAndReceiptDateRange(
        @Param("tenantId") String tenantId,
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate
    );

    @Query("SELECT r FROM Receipt r WHERE r.tenantId = :tenantId " +
           "AND r.purchaseOrder.vendor.id = :vendorId ORDER BY r.receiptDate DESC")
    List<Receipt> findByTenantIdAndVendorId(@Param("tenantId") String tenantId,
                                             @Param("vendorId") Long vendorId);

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
