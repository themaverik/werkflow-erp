package com.werkflow.business.procurement.repository;

import com.werkflow.business.procurement.entity.ReceiptLineItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReceiptLineItemRepository extends JpaRepository<ReceiptLineItem, Long> {

    // Tenant-scoped methods
    List<ReceiptLineItem> findByTenantId(String tenantId);

    List<ReceiptLineItem> findByReceiptIdAndTenantId(Long receiptId, String tenantId);

    List<ReceiptLineItem> findByPoLineItemIdAndTenantId(Long poLineItemId, String tenantId);

    @Query("SELECT rli FROM ReceiptLineItem rli " +
           "WHERE rli.receipt.id = :receiptId AND rli.tenantId = :tenantId " +
           "AND rli.condition != 'GOOD'")
    List<ReceiptLineItem> findItemsWithIssuesForTenant(@Param("receiptId") Long receiptId,
                                                        @Param("tenantId") String tenantId);

    @Query("SELECT rli FROM ReceiptLineItem rli " +
           "WHERE rli.receipt.id = :receiptId AND rli.tenantId = :tenantId " +
           "AND rli.rejectedQuantity > 0")
    List<ReceiptLineItem> findRejectedItemsForTenant(@Param("receiptId") Long receiptId,
                                                      @Param("tenantId") String tenantId);

    @Query("SELECT SUM(rli.acceptedQuantity) FROM ReceiptLineItem rli " +
           "WHERE rli.poLineItem.id = :poLineItemId AND rli.tenantId = :tenantId")
    Integer sumAcceptedQuantityByPoLineItemAndTenant(@Param("poLineItemId") Long poLineItemId,
                                                      @Param("tenantId") String tenantId);

    // Legacy unscoped methods
    @Deprecated(forRemoval = false, since = "1.0.0")
    List<ReceiptLineItem> findByReceiptId(Long receiptId);

    @Deprecated(forRemoval = false, since = "1.0.0")
    List<ReceiptLineItem> findByPoLineItemId(Long poLineItemId);

    @Deprecated(forRemoval = false, since = "1.0.0")
    @Query("SELECT rli FROM ReceiptLineItem rli " +
           "WHERE rli.receipt.id = :receiptId " +
           "AND rli.condition != 'GOOD'")
    List<ReceiptLineItem> findItemsWithIssues(@Param("receiptId") Long receiptId);

    @Deprecated(forRemoval = false, since = "1.0.0")
    @Query("SELECT rli FROM ReceiptLineItem rli " +
           "WHERE rli.receipt.id = :receiptId " +
           "AND rli.rejectedQuantity > 0")
    List<ReceiptLineItem> findRejectedItems(@Param("receiptId") Long receiptId);

    @Deprecated(forRemoval = false, since = "1.0.0")
    @Query("SELECT SUM(rli.acceptedQuantity) FROM ReceiptLineItem rli " +
           "WHERE rli.poLineItem.id = :poLineItemId")
    Integer sumAcceptedQuantityByPoLineItem(@Param("poLineItemId") Long poLineItemId);
}
