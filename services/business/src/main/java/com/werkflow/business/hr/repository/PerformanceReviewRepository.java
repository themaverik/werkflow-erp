package com.werkflow.business.hr.repository;

import com.werkflow.business.hr.entity.PerformanceRating;
import com.werkflow.business.hr.entity.PerformanceReview;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

/**
 * Repository for PerformanceReview entity
 */
@Repository
public interface PerformanceReviewRepository extends JpaRepository<PerformanceReview, Long> {

    // Tenant-scoped methods (NEW)
    List<PerformanceReview> findByTenantId(@Param("tenantId") String tenantId);

    List<PerformanceReview> findByTenantIdAndEmployeeId(@Param("tenantId") String tenantId,
                                                        @Param("employeeId") Long employeeId);

    List<PerformanceReview> findByTenantIdAndReviewerId(@Param("tenantId") String tenantId,
                                                        @Param("reviewerId") Long reviewerId);

    List<PerformanceReview> findByTenantIdAndRating(@Param("tenantId") String tenantId,
                                                    @Param("rating") PerformanceRating rating);

    @Query("SELECT pr FROM PerformanceReview pr WHERE pr.tenantId = :tenantId AND pr.employee.id = :employeeId " +
           "ORDER BY pr.reviewDate DESC")
    List<PerformanceReview> findByTenantIdAndEmployeeIdOrderByReviewDateDesc(@Param("tenantId") String tenantId,
                                                                              @Param("employeeId") Long employeeId);

    @Query("SELECT pr FROM PerformanceReview pr WHERE pr.tenantId = :tenantId AND pr.reviewDate BETWEEN :startDate AND :endDate")
    List<PerformanceReview> findByTenantIdAndReviewDateBetween(@Param("tenantId") String tenantId,
                                                               @Param("startDate") LocalDate startDate,
                                                               @Param("endDate") LocalDate endDate);

    @Query("SELECT pr FROM PerformanceReview pr WHERE pr.tenantId = :tenantId AND pr.employee.department.id = :departmentId " +
           "AND pr.reviewDate BETWEEN :startDate AND :endDate")
    List<PerformanceReview> findByTenantIdAndDepartmentAndDateRange(@Param("tenantId") String tenantId,
                                                                    @Param("departmentId") Long departmentId,
                                                                    @Param("startDate") LocalDate startDate,
                                                                    @Param("endDate") LocalDate endDate);

    @Query("SELECT AVG(pr.score) FROM PerformanceReview pr WHERE pr.tenantId = :tenantId AND pr.employee.id = :employeeId")
    Double getAverageScoreByEmployeeTenant(@Param("tenantId") String tenantId,
                                           @Param("employeeId") Long employeeId);

    @Query("SELECT pr FROM PerformanceReview pr WHERE pr.tenantId = :tenantId AND pr.employeeAcknowledged = false")
    List<PerformanceReview> findByTenantIdAndPendingAcknowledgement(@Param("tenantId") String tenantId);

    long countByTenantIdAndEmployeeIdAndRating(@Param("tenantId") String tenantId,
                                               @Param("employeeId") Long employeeId,
                                               @Param("rating") PerformanceRating rating);

    // Legacy methods (kept for backward compatibility, but deprecated)
    @Deprecated(forRemoval = false, since = "1.0.0")
    List<PerformanceReview> findByEmployeeId(Long employeeId);

    @Deprecated(forRemoval = false, since = "1.0.0")
    List<PerformanceReview> findByReviewerId(Long reviewerId);

    @Deprecated(forRemoval = false, since = "1.0.0")
    List<PerformanceReview> findByRating(PerformanceRating rating);

    @Deprecated(forRemoval = false, since = "1.0.0")
    @Query("SELECT pr FROM PerformanceReview pr WHERE pr.employee.id = :employeeId " +
           "ORDER BY pr.reviewDate DESC")
    List<PerformanceReview> findByEmployeeIdOrderByReviewDateDesc(@Param("employeeId") Long employeeId);

    @Deprecated(forRemoval = false, since = "1.0.0")
    @Query("SELECT pr FROM PerformanceReview pr WHERE pr.reviewDate BETWEEN :startDate AND :endDate")
    List<PerformanceReview> findByReviewDateBetween(@Param("startDate") LocalDate startDate,
                                                    @Param("endDate") LocalDate endDate);

    @Deprecated(forRemoval = false, since = "1.0.0")
    @Query("SELECT pr FROM PerformanceReview pr WHERE pr.employee.department.id = :departmentId " +
           "AND pr.reviewDate BETWEEN :startDate AND :endDate")
    List<PerformanceReview> findByDepartmentAndDateRange(@Param("departmentId") Long departmentId,
                                                         @Param("startDate") LocalDate startDate,
                                                         @Param("endDate") LocalDate endDate);

    @Deprecated(forRemoval = false, since = "1.0.0")
    @Query("SELECT AVG(pr.score) FROM PerformanceReview pr WHERE pr.employee.id = :employeeId")
    Double getAverageScoreByEmployee(@Param("employeeId") Long employeeId);

    @Deprecated(forRemoval = false, since = "1.0.0")
    @Query("SELECT pr FROM PerformanceReview pr WHERE pr.employeeAcknowledged = false")
    List<PerformanceReview> findPendingAcknowledgement();

    @Deprecated(forRemoval = false, since = "1.0.0")
    long countByEmployeeIdAndRating(Long employeeId, PerformanceRating rating);
}
