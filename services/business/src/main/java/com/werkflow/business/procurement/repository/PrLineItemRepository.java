package com.werkflow.business.procurement.repository;

import com.werkflow.business.procurement.entity.PrLineItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

@Repository
public interface PrLineItemRepository extends JpaRepository<PrLineItem, Long> {

    // Tenant-scoped methods
    List<PrLineItem> findByTenantId(String tenantId);

    List<PrLineItem> findByPurchaseRequestIdAndTenantId(Long purchaseRequestId, String tenantId);

    @Query("SELECT pli FROM PrLineItem pli WHERE pli.tenantId = :tenantId " +
           "AND pli.budgetCategoryId = :categoryId")
    List<PrLineItem> findByTenantIdAndBudgetCategoryId(@Param("tenantId") String tenantId,
                                                        @Param("categoryId") Long budgetCategoryId);

    @Query("SELECT SUM(pli.totalPrice) FROM PrLineItem pli " +
           "WHERE pli.purchaseRequest.id = :prId AND pli.tenantId = :tenantId")
    BigDecimal sumTotalPriceByPurchaseRequestAndTenant(@Param("prId") Long purchaseRequestId,
                                                        @Param("tenantId") String tenantId);

    @Query("SELECT pli FROM PrLineItem pli " +
           "WHERE pli.purchaseRequest.id = :prId AND pli.tenantId = :tenantId " +
           "ORDER BY pli.lineNumber ASC")
    List<PrLineItem> findByPurchaseRequestAndTenantOrderByLineNumber(@Param("prId") Long purchaseRequestId,
                                                                      @Param("tenantId") String tenantId);

    // Legacy unscoped methods
    @Deprecated(forRemoval = false, since = "1.0.0")
    List<PrLineItem> findByPurchaseRequestId(Long purchaseRequestId);

    @Deprecated(forRemoval = false, since = "1.0.0")
    @Query("SELECT pli FROM PrLineItem pli WHERE pli.budgetCategoryId = :categoryId")
    List<PrLineItem> findByBudgetCategoryId(@Param("categoryId") Long budgetCategoryId);

    @Deprecated(forRemoval = false, since = "1.0.0")
    @Query("SELECT SUM(pli.totalPrice) FROM PrLineItem pli " +
           "WHERE pli.purchaseRequest.id = :prId")
    BigDecimal sumTotalPriceByPurchaseRequest(@Param("prId") Long purchaseRequestId);

    @Deprecated(forRemoval = false, since = "1.0.0")
    @Query("SELECT pli FROM PrLineItem pli " +
           "WHERE pli.purchaseRequest.id = :prId " +
           "ORDER BY pli.lineNumber ASC")
    List<PrLineItem> findByPurchaseRequestOrderByLineNumber(@Param("prId") Long purchaseRequestId);
}
