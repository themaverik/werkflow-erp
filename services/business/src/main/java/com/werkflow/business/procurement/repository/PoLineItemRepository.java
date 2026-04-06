package com.werkflow.business.procurement.repository;

import com.werkflow.business.procurement.entity.PoLineItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

@Repository
public interface PoLineItemRepository extends JpaRepository<PoLineItem, Long> {

    // Tenant-scoped methods
    List<PoLineItem> findByTenantId(String tenantId);

    List<PoLineItem> findByPurchaseOrderIdAndTenantId(Long purchaseOrderId, String tenantId);

    @Query("SELECT poli FROM PoLineItem poli " +
           "WHERE poli.purchaseOrder.id = :poId AND poli.tenantId = :tenantId " +
           "ORDER BY poli.lineNumber ASC")
    List<PoLineItem> findByPurchaseOrderAndTenantOrderByLineNumber(@Param("poId") Long purchaseOrderId,
                                                                    @Param("tenantId") String tenantId);

    @Query("SELECT poli FROM PoLineItem poli " +
           "WHERE poli.purchaseOrder.id = :poId AND poli.tenantId = :tenantId " +
           "AND poli.receivedQuantity < poli.orderedQuantity")
    List<PoLineItem> findPendingReceiptItemsForTenant(@Param("poId") Long purchaseOrderId,
                                                       @Param("tenantId") String tenantId);

    @Query("SELECT SUM(poli.totalPrice) FROM PoLineItem poli " +
           "WHERE poli.purchaseOrder.id = :poId AND poli.tenantId = :tenantId")
    BigDecimal sumTotalPriceByPurchaseOrderAndTenant(@Param("poId") Long purchaseOrderId,
                                                      @Param("tenantId") String tenantId);

    // Legacy unscoped methods
    @Deprecated(forRemoval = false, since = "1.0.0")
    List<PoLineItem> findByPurchaseOrderId(Long purchaseOrderId);

    @Deprecated(forRemoval = false, since = "1.0.0")
    @Query("SELECT poli FROM PoLineItem poli " +
           "WHERE poli.purchaseOrder.id = :poId " +
           "ORDER BY poli.lineNumber ASC")
    List<PoLineItem> findByPurchaseOrderOrderByLineNumber(@Param("poId") Long purchaseOrderId);

    @Deprecated(forRemoval = false, since = "1.0.0")
    @Query("SELECT poli FROM PoLineItem poli " +
           "WHERE poli.purchaseOrder.id = :poId " +
           "AND poli.receivedQuantity < poli.orderedQuantity")
    List<PoLineItem> findPendingReceiptItems(@Param("poId") Long purchaseOrderId);

    @Deprecated(forRemoval = false, since = "1.0.0")
    @Query("SELECT SUM(poli.totalPrice) FROM PoLineItem poli " +
           "WHERE poli.purchaseOrder.id = :poId")
    BigDecimal sumTotalPriceByPurchaseOrder(@Param("poId") Long purchaseOrderId);

    @Deprecated(forRemoval = false, since = "1.0.0")
    @Query("SELECT COUNT(poli) FROM PoLineItem poli " +
           "WHERE poli.purchaseOrder.id = :poId " +
           "AND poli.receivedQuantity >= poli.orderedQuantity")
    Long countFullyReceivedItems(@Param("poId") Long purchaseOrderId);

    @Deprecated(forRemoval = false, since = "1.0.0")
    @Query("SELECT COUNT(poli) FROM PoLineItem poli " +
           "WHERE poli.purchaseOrder.id = :poId")
    Long countTotalItems(@Param("poId") Long purchaseOrderId);
}
