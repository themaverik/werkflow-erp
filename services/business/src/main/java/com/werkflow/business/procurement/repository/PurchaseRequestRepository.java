package com.werkflow.business.procurement.repository;

import com.werkflow.business.procurement.entity.PurchaseRequest;
import com.werkflow.business.procurement.entity.PurchaseRequest.Priority;
import com.werkflow.business.procurement.entity.PurchaseRequest.PrStatus;
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
public interface PurchaseRequestRepository extends JpaRepository<PurchaseRequest, Long> {

    // Tenant-scoped methods
    Page<PurchaseRequest> findByTenantId(String tenantId, Pageable pageable);

    Optional<PurchaseRequest> findByPrNumberAndTenantId(String prNumber, String tenantId);

    Page<PurchaseRequest> findByTenantIdAndRequestingDeptId(String tenantId, Long deptId, Pageable pageable);

    Page<PurchaseRequest> findByTenantIdAndRequesterUserId(String tenantId, Long userId, Pageable pageable);

    Page<PurchaseRequest> findByTenantIdAndStatus(String tenantId, PrStatus status, Pageable pageable);

    Page<PurchaseRequest> findByTenantIdAndStatusIn(String tenantId, List<PrStatus> statuses, Pageable pageable);

    @Query("SELECT pr FROM PurchaseRequest pr WHERE pr.tenantId = :tenantId " +
           "AND pr.requestingDeptId = :deptId AND pr.status = :status ORDER BY pr.requestDate DESC")
    Page<PurchaseRequest> findByTenantIdAndDepartmentAndStatus(
        @Param("tenantId") String tenantId,
        @Param("deptId") Long departmentId,
        @Param("status") PrStatus status,
        Pageable pageable
    );

    @Query("SELECT pr FROM PurchaseRequest pr WHERE pr.tenantId = :tenantId " +
           "AND pr.status IN ('SUBMITTED', 'PENDING_APPROVAL') " +
           "ORDER BY pr.priority DESC, pr.requestDate ASC")
    Page<PurchaseRequest> findPendingRequestsForTenant(@Param("tenantId") String tenantId, Pageable pageable);

    @Query("SELECT pr FROM PurchaseRequest pr WHERE pr.tenantId = :tenantId " +
           "AND pr.status = 'PENDING_APPROVAL' AND pr.priority = :priority")
    Page<PurchaseRequest> findPendingByPriorityForTenant(@Param("tenantId") String tenantId,
                                                          @Param("priority") Priority priority,
                                                          Pageable pageable);

    @Query("SELECT pr FROM PurchaseRequest pr WHERE pr.tenantId = :tenantId " +
           "AND pr.requestDate BETWEEN :startDate AND :endDate ORDER BY pr.requestDate DESC")
    Page<PurchaseRequest> findByTenantIdAndDateRange(
        @Param("tenantId") String tenantId,
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate,
        Pageable pageable
    );

    boolean existsByPrNumberAndTenantId(String prNumber, String tenantId);

    // Legacy unscoped methods
    @Deprecated(forRemoval = false, since = "1.0.0")
    Optional<PurchaseRequest> findByPrNumber(String prNumber);

    @Deprecated(forRemoval = false, since = "1.0.0")
    List<PurchaseRequest> findByRequestingDeptId(Long deptId);

    @Deprecated(forRemoval = false, since = "1.0.0")
    List<PurchaseRequest> findByRequesterUserId(Long userId);

    @Deprecated(forRemoval = false, since = "1.0.0")
    List<PurchaseRequest> findByStatus(PrStatus status);

    @Deprecated(forRemoval = false, since = "1.0.0")
    List<PurchaseRequest> findByStatusIn(List<PrStatus> statuses);

    @Deprecated(forRemoval = false, since = "1.0.0")
    @Query("SELECT pr FROM PurchaseRequest pr WHERE pr.requestingDeptId = :deptId " +
           "AND pr.status = :status ORDER BY pr.requestDate DESC")
    List<PurchaseRequest> findByDepartmentAndStatus(
        @Param("deptId") Long departmentId,
        @Param("status") PrStatus status
    );

    @Deprecated(forRemoval = false, since = "1.0.0")
    @Query("SELECT pr FROM PurchaseRequest pr WHERE pr.status IN ('SUBMITTED', 'PENDING_APPROVAL') " +
           "ORDER BY pr.priority DESC, pr.requestDate ASC")
    List<PurchaseRequest> findPendingRequests();

    @Deprecated(forRemoval = false, since = "1.0.0")
    @Query("SELECT pr FROM PurchaseRequest pr WHERE pr.status = 'PENDING_APPROVAL' " +
           "AND pr.priority = :priority")
    List<PurchaseRequest> findPendingByPriority(@Param("priority") Priority priority);

    @Deprecated(forRemoval = false, since = "1.0.0")
    @Query("SELECT pr FROM PurchaseRequest pr WHERE pr.requestDate BETWEEN :startDate AND :endDate " +
           "ORDER BY pr.requestDate DESC")
    List<PurchaseRequest> findByDateRange(
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate
    );

    @Deprecated(forRemoval = false, since = "1.0.0")
    boolean existsByPrNumber(String prNumber);
}
