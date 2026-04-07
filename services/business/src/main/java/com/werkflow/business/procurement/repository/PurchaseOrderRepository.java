package com.werkflow.business.procurement.repository;

import com.werkflow.business.procurement.entity.PurchaseOrder;
import com.werkflow.business.procurement.entity.PurchaseOrder.PoStatus;
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
public interface PurchaseOrderRepository extends JpaRepository<PurchaseOrder, Long> {

    // Tenant-scoped methods
    Page<PurchaseOrder> findByTenantId(String tenantId, Pageable pageable);

    Optional<PurchaseOrder> findByPoNumberAndTenantId(String poNumber, String tenantId);

    Page<PurchaseOrder> findByTenantIdAndVendorId(String tenantId, Long vendorId, Pageable pageable);

    Page<PurchaseOrder> findByTenantIdAndStatus(String tenantId, PoStatus status, Pageable pageable);

    Page<PurchaseOrder> findByTenantIdAndStatusIn(String tenantId, List<PoStatus> statuses, Pageable pageable);

    Optional<PurchaseOrder> findByPurchaseRequestIdAndTenantId(Long purchaseRequestId, String tenantId);

    @Query("SELECT po FROM PurchaseOrder po WHERE po.tenantId = :tenantId " +
           "AND po.vendor.id = :vendorId AND po.status = :status ORDER BY po.orderDate DESC")
    Page<PurchaseOrder> findByTenantIdAndVendorAndStatus(
        @Param("tenantId") String tenantId,
        @Param("vendorId") Long vendorId,
        @Param("status") PoStatus status,
        Pageable pageable
    );

    @Query("SELECT po FROM PurchaseOrder po WHERE po.tenantId = :tenantId " +
           "AND po.status IN ('SENT', 'ACKNOWLEDGED', 'PARTIALLY_RECEIVED') ORDER BY po.orderDate ASC")
    Page<PurchaseOrder> findActivePurchaseOrdersForTenant(@Param("tenantId") String tenantId, Pageable pageable);

    @Query("SELECT po FROM PurchaseOrder po WHERE po.tenantId = :tenantId " +
           "AND po.status = 'PARTIALLY_RECEIVED'")
    Page<PurchaseOrder> findPartiallyReceivedOrdersForTenant(@Param("tenantId") String tenantId, Pageable pageable);

    @Query("SELECT po FROM PurchaseOrder po WHERE po.tenantId = :tenantId " +
           "AND po.orderDate BETWEEN :startDate AND :endDate ORDER BY po.orderDate DESC")
    Page<PurchaseOrder> findByTenantIdAndOrderDateRange(
        @Param("tenantId") String tenantId,
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate,
        Pageable pageable
    );

    @Query("SELECT po FROM PurchaseOrder po WHERE po.tenantId = :tenantId " +
           "AND po.expectedDeliveryDate < :date " +
           "AND po.status NOT IN ('FULLY_RECEIVED', 'CLOSED', 'CANCELLED')")
    Page<PurchaseOrder> findOverdueDeliveriesForTenant(@Param("tenantId") String tenantId,
                                                        @Param("date") LocalDate date,
                                                        Pageable pageable);

    boolean existsByPoNumberAndTenantId(String poNumber, String tenantId);

    // Legacy unscoped methods
    @Deprecated(forRemoval = false, since = "1.0.0")
    Optional<PurchaseOrder> findByPoNumber(String poNumber);

    @Deprecated(forRemoval = false, since = "1.0.0")
    List<PurchaseOrder> findByVendorId(Long vendorId);

    @Deprecated(forRemoval = false, since = "1.0.0")
    List<PurchaseOrder> findByStatus(PoStatus status);

    @Deprecated(forRemoval = false, since = "1.0.0")
    List<PurchaseOrder> findByStatusIn(List<PoStatus> statuses);

    @Deprecated(forRemoval = false, since = "1.0.0")
    Optional<PurchaseOrder> findByPurchaseRequestId(Long purchaseRequestId);

    @Deprecated(forRemoval = false, since = "1.0.0")
    @Query("SELECT po FROM PurchaseOrder po WHERE po.vendor.id = :vendorId " +
           "AND po.status = :status ORDER BY po.orderDate DESC")
    List<PurchaseOrder> findByVendorAndStatus(
        @Param("vendorId") Long vendorId,
        @Param("status") PoStatus status
    );

    @Deprecated(forRemoval = false, since = "1.0.0")
    @Query("SELECT po FROM PurchaseOrder po WHERE po.status IN ('SENT', 'ACKNOWLEDGED', 'PARTIALLY_RECEIVED') " +
           "ORDER BY po.orderDate ASC")
    List<PurchaseOrder> findActivePurchaseOrders();

    @Deprecated(forRemoval = false, since = "1.0.0")
    @Query("SELECT po FROM PurchaseOrder po WHERE po.status = 'PARTIALLY_RECEIVED'")
    List<PurchaseOrder> findPartiallyReceivedOrders();

    @Deprecated(forRemoval = false, since = "1.0.0")
    @Query("SELECT po FROM PurchaseOrder po WHERE po.orderDate BETWEEN :startDate AND :endDate " +
           "ORDER BY po.orderDate DESC")
    List<PurchaseOrder> findByOrderDateRange(
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate
    );

    @Deprecated(forRemoval = false, since = "1.0.0")
    @Query("SELECT po FROM PurchaseOrder po WHERE po.expectedDeliveryDate < :date " +
           "AND po.status NOT IN ('FULLY_RECEIVED', 'CLOSED', 'CANCELLED')")
    List<PurchaseOrder> findOverdueDeliveries(@Param("date") LocalDate date);

    @Deprecated(forRemoval = false, since = "1.0.0")
    boolean existsByPoNumber(String poNumber);
}
