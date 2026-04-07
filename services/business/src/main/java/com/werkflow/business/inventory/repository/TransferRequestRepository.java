package com.werkflow.business.inventory.repository;

import com.werkflow.business.inventory.entity.AssetInstance;
import com.werkflow.business.inventory.entity.TransferRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository for TransferRequest entity.
 * Tenant-scoped query methods follow the pattern established in Tasks 4-6.
 */
@Repository
public interface TransferRequestRepository extends JpaRepository<TransferRequest, Long> {

    // Tenant-scoped methods
    Page<TransferRequest> findByTenantId(String tenantId, Pageable pageable);

    List<TransferRequest> findByTenantIdAndStatus(String tenantId, String status);

    @Query("SELECT tr FROM TransferRequest tr WHERE tr.tenantId = :tenantId AND tr.status = 'PENDING'")
    List<TransferRequest> findPendingRequestsForTenant(@Param("tenantId") String tenantId);

    List<TransferRequest> findByTenantIdAndFromDeptId(String tenantId, Long fromDeptId);

    List<TransferRequest> findByTenantIdAndToDeptId(String tenantId, Long toDeptId);

    List<TransferRequest> findByTenantIdAndFromDeptIdAndStatus(String tenantId, Long fromDeptId, String status);

    List<TransferRequest> findByTenantIdAndToDeptIdAndStatus(String tenantId, Long toDeptId, String status);

    List<TransferRequest> findByTenantIdAndInitiatedByUserId(String tenantId, Long userId);

    List<TransferRequest> findByTenantIdAndTransferType(String tenantId, String transferType);

    @Query("SELECT tr FROM TransferRequest tr WHERE tr.tenantId = :tenantId " +
           "AND tr.transferType = 'INTER_DEPARTMENT' AND tr.status != 'CANCELLED'")
    List<TransferRequest> findActiveInterDepartmentTransfersForTenant(@Param("tenantId") String tenantId);

    @Query("SELECT tr FROM TransferRequest tr WHERE tr.tenantId = :tenantId " +
           "AND tr.transferType = 'LOAN' AND tr.status = 'COMPLETED'")
    List<TransferRequest> findActiveLoanRequestsForTenant(@Param("tenantId") String tenantId);

    @Query("SELECT tr FROM TransferRequest tr WHERE tr.tenantId = :tenantId " +
           "AND tr.transferType = 'LOAN' AND tr.expectedReturnDate <= :currentDate AND tr.status = 'APPROVED'")
    List<TransferRequest> findOverdueLoansForTenant(@Param("tenantId") String tenantId,
                                                    @Param("currentDate") LocalDateTime currentDate);

    @Query("SELECT tr FROM TransferRequest tr WHERE tr.tenantId = :tenantId AND " +
           "(LOWER(tr.transferReason) LIKE LOWER(CONCAT('%', :searchTerm, '%')) " +
           "OR tr.assetInstance.assetTag LIKE CONCAT('%', :searchTerm, '%'))")
    List<TransferRequest> searchTransfersForTenant(@Param("tenantId") String tenantId,
                                                   @Param("searchTerm") String searchTerm);

    List<TransferRequest> findByTenantIdAndAssetInstance(String tenantId, AssetInstance assetInstance);

    // Legacy unscoped methods
    @Deprecated(forRemoval = false, since = "1.0.0")
    List<TransferRequest> findByAssetInstance(AssetInstance assetInstance);

    @Deprecated(forRemoval = false, since = "1.0.0")
    List<TransferRequest> findByStatus(String status);

    @Deprecated(forRemoval = false, since = "1.0.0")
    @Query("SELECT tr FROM TransferRequest tr WHERE tr.status = 'PENDING'")
    List<TransferRequest> findPendingRequests();

    @Deprecated(forRemoval = false, since = "1.0.0")
    List<TransferRequest> findByFromDeptId(Long fromDeptId);

    @Deprecated(forRemoval = false, since = "1.0.0")
    List<TransferRequest> findByToDeptId(Long toDeptId);

    @Deprecated(forRemoval = false, since = "1.0.0")
    List<TransferRequest> findByFromDeptIdAndStatus(Long fromDeptId, String status);

    @Deprecated(forRemoval = false, since = "1.0.0")
    List<TransferRequest> findByToDeptIdAndStatus(Long toDeptId, String status);

    @Deprecated(forRemoval = false, since = "1.0.0")
    List<TransferRequest> findByInitiatedByUserId(Long userId);

    @Deprecated(forRemoval = false, since = "1.0.0")
    List<TransferRequest> findByTransferType(String transferType);

    @Deprecated(forRemoval = false, since = "1.0.0")
    @Query("SELECT tr FROM TransferRequest tr WHERE tr.transferType = 'INTER_DEPARTMENT' AND tr.status != 'CANCELLED'")
    List<TransferRequest> findActiveInterDepartmentTransfers();

    @Deprecated(forRemoval = false, since = "1.0.0")
    @Query("SELECT tr FROM TransferRequest tr WHERE tr.transferType = 'LOAN' AND tr.status = 'COMPLETED'")
    List<TransferRequest> findActiveLoanRequests();

    @Deprecated(forRemoval = false, since = "1.0.0")
    @Query("SELECT tr FROM TransferRequest tr WHERE tr.transferType = 'LOAN' AND tr.expectedReturnDate <= :currentDate AND tr.status = 'APPROVED'")
    List<TransferRequest> findOverdueLoans(@Param("currentDate") LocalDateTime currentDate);

    @Deprecated(forRemoval = false, since = "1.0.0")
    Optional<TransferRequest> findByProcessInstanceId(String processInstanceId);

    @Deprecated(forRemoval = false, since = "1.0.0")
    @Query("SELECT tr FROM TransferRequest tr WHERE " +
           "LOWER(tr.transferReason) LIKE LOWER(CONCAT('%', :searchTerm, '%')) " +
           "OR tr.assetInstance.assetTag LIKE CONCAT('%', :searchTerm, '%')")
    List<TransferRequest> searchTransfers(@Param("searchTerm") String searchTerm);
}
