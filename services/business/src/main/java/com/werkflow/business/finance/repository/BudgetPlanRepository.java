package com.werkflow.business.finance.repository;

import com.werkflow.business.finance.entity.BudgetPlan;
import com.werkflow.business.finance.entity.BudgetPlan.BudgetStatus;
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
public interface BudgetPlanRepository extends JpaRepository<BudgetPlan, Long> {

    Page<BudgetPlan> findByTenantId(String tenantId, Pageable pageable);

    Optional<BudgetPlan> findByDepartmentIdAndFiscalYearAndTenantId(Long departmentId, Integer fiscalYear, String tenantId);

    // NOTE: findActiveBudgetForDepartment and findBudgetsExceedingThreshold below are
    // unscoped custom queries (no tenantId filter). They must not be called from
    // tenant-facing code paths. Retain only for potential future admin tooling;
    // scope them before any production use.
    @Query("SELECT bp FROM BudgetPlan bp WHERE bp.departmentId = :deptId " +
           "AND bp.periodStart <= :date AND bp.periodEnd >= :date")
    Optional<BudgetPlan> findActiveBudgetForDepartment(
        @Param("deptId") Long departmentId,
        @Param("date") LocalDate date
    );

    @Query("SELECT bp FROM BudgetPlan bp WHERE bp.status = 'ACTIVE' " +
           "AND (bp.spentAmount / bp.totalAmount) > :threshold")
    List<BudgetPlan> findBudgetsExceedingThreshold(@Param("threshold") Double threshold);
}
