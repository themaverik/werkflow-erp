package com.werkflow.business.finance.repository;

import com.werkflow.business.finance.entity.BudgetCategory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BudgetCategoryRepository extends JpaRepository<BudgetCategory, Long> {

    Page<BudgetCategory> findByTenantId(String tenantId, Pageable pageable);

    Optional<BudgetCategory> findByIdAndTenantId(Long id, String tenantId);

    Optional<BudgetCategory> findByCode(String code);

    List<BudgetCategory> findByActiveTrue();

    List<BudgetCategory> findByParentCategoryIsNull();

    List<BudgetCategory> findByParentCategoryId(Long parentId);

    @Query("SELECT c FROM BudgetCategory c WHERE c.parentCategory IS NULL AND c.active = true")
    List<BudgetCategory> findRootCategoriesActive();

    @Query("SELECT c FROM BudgetCategory c LEFT JOIN FETCH c.childCategories WHERE c.id = :id")
    Optional<BudgetCategory> findByIdWithChildren(@Param("id") Long id);

    boolean existsByCode(String code);
}
