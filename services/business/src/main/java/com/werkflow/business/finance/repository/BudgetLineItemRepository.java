package com.werkflow.business.finance.repository;

import com.werkflow.business.finance.entity.BudgetLineItem;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BudgetLineItemRepository extends JpaRepository<BudgetLineItem, Long> {

    Page<BudgetLineItem> findByTenantId(String tenantId, Pageable pageable);

    Page<BudgetLineItem> findByBudgetPlanId(Long budgetPlanId, Pageable pageable);

    Page<BudgetLineItem> findByBudgetPlanIdAndTenantId(Long budgetPlanId, String tenantId, Pageable pageable);

    List<BudgetLineItem> findByCategoryId(Long categoryId);

    @Query("SELECT bli FROM BudgetLineItem bli " +
           "WHERE bli.budgetPlan.id = :planId AND bli.category.id = :categoryId")
    List<BudgetLineItem> findByBudgetPlanAndCategory(
        @Param("planId") Long budgetPlanId,
        @Param("categoryId") Long categoryId
    );

    @Query("SELECT bli FROM BudgetLineItem bli " +
           "WHERE bli.budgetPlan.id = :planId " +
           "AND (bli.spentAmount / bli.allocatedAmount) > :threshold")
    List<BudgetLineItem> findLineItemsExceedingThreshold(
        @Param("planId") Long budgetPlanId,
        @Param("threshold") Double threshold
    );

    @Query("SELECT SUM(bli.allocatedAmount) FROM BudgetLineItem bli " +
           "WHERE bli.budgetPlan.id = :planId")
    java.math.BigDecimal sumAllocatedAmountByBudgetPlan(@Param("planId") Long budgetPlanId);

    @Query("SELECT SUM(bli.spentAmount) FROM BudgetLineItem bli " +
           "WHERE bli.budgetPlan.id = :planId")
    java.math.BigDecimal sumSpentAmountByBudgetPlan(@Param("planId") Long budgetPlanId);
}
