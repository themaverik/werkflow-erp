package com.werkflow.business.hr.repository;

import com.werkflow.business.hr.entity.Leave;
import com.werkflow.business.hr.entity.LeaveStatus;
import com.werkflow.business.hr.entity.LeaveType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

/**
 * Repository for Leave entity
 */
@Repository
public interface LeaveRepository extends JpaRepository<Leave, Long> {

    // Tenant-scoped methods (NEW)
    List<Leave> findByTenantIdAndEmployeeId(@Param("tenantId") String tenantId,
                                            @Param("employeeId") Long employeeId);

    List<Leave> findByTenantIdAndStatus(@Param("tenantId") String tenantId,
                                        @Param("status") LeaveStatus status);

    List<Leave> findByTenantIdAndLeaveType(@Param("tenantId") String tenantId,
                                           @Param("leaveType") LeaveType leaveType);

    @Query("SELECT l FROM Leave l WHERE l.tenantId = :tenantId AND l.employee.id = :employeeId AND l.status = :status")
    List<Leave> findByTenantIdAndEmployeeIdAndStatus(@Param("tenantId") String tenantId,
                                                     @Param("employeeId") Long employeeId,
                                                     @Param("status") LeaveStatus status);

    @Query("SELECT l FROM Leave l WHERE l.tenantId = :tenantId AND l.employee.id = :employeeId " +
           "AND l.startDate <= :endDate AND l.endDate >= :startDate")
    List<Leave> findByTenantIdAndEmployeeIdAndOverlappingDates(@Param("tenantId") String tenantId,
                                                               @Param("employeeId") Long employeeId,
                                                               @Param("startDate") LocalDate startDate,
                                                               @Param("endDate") LocalDate endDate);

    @Query("SELECT l FROM Leave l WHERE l.tenantId = :tenantId AND l.startDate BETWEEN :startDate AND :endDate")
    List<Leave> findByTenantIdAndDateRange(@Param("tenantId") String tenantId,
                                           @Param("startDate") LocalDate startDate,
                                           @Param("endDate") LocalDate endDate);

    @Query("SELECT SUM(l.numberOfDays) FROM Leave l WHERE l.tenantId = :tenantId AND l.employee.id = :employeeId " +
           "AND l.leaveType = :leaveType AND l.status = 'APPROVED' " +
           "AND EXTRACT(YEAR FROM l.startDate) = :year")
    Integer getTotalApprovedLeavesByTypeTenant(@Param("tenantId") String tenantId,
                                               @Param("employeeId") Long employeeId,
                                               @Param("leaveType") LeaveType leaveType,
                                               @Param("year") int year);

    List<Leave> findByTenantIdAndApprovedById(@Param("tenantId") String tenantId,
                                              @Param("approverId") Long approverId);

    long countByTenantIdAndEmployeeIdAndStatus(@Param("tenantId") String tenantId,
                                               @Param("employeeId") Long employeeId,
                                               @Param("status") LeaveStatus status);

    // Legacy methods (kept for backward compatibility, but deprecated)
    @Deprecated(forRemoval = false, since = "1.0.0")
    List<Leave> findByEmployeeId(Long employeeId);

    @Deprecated(forRemoval = false, since = "1.0.0")
    List<Leave> findByStatus(LeaveStatus status);

    @Deprecated(forRemoval = false, since = "1.0.0")
    List<Leave> findByLeaveType(LeaveType leaveType);

    @Deprecated(forRemoval = false, since = "1.0.0")
    @Query("SELECT l FROM Leave l WHERE l.employee.id = :employeeId AND l.status = :status")
    List<Leave> findByEmployeeIdAndStatus(@Param("employeeId") Long employeeId,
                                          @Param("status") LeaveStatus status);

    @Deprecated(forRemoval = false, since = "1.0.0")
    @Query("SELECT l FROM Leave l WHERE l.employee.id = :employeeId " +
           "AND l.startDate <= :endDate AND l.endDate >= :startDate")
    List<Leave> findOverlappingLeaves(@Param("employeeId") Long employeeId,
                                      @Param("startDate") LocalDate startDate,
                                      @Param("endDate") LocalDate endDate);

    @Deprecated(forRemoval = false, since = "1.0.0")
    @Query("SELECT l FROM Leave l WHERE l.startDate BETWEEN :startDate AND :endDate")
    List<Leave> findByDateRange(@Param("startDate") LocalDate startDate,
                                @Param("endDate") LocalDate endDate);

    @Deprecated(forRemoval = false, since = "1.0.0")
    @Query("SELECT SUM(l.numberOfDays) FROM Leave l WHERE l.employee.id = :employeeId " +
           "AND l.leaveType = :leaveType AND l.status = 'APPROVED' " +
           "AND EXTRACT(YEAR FROM l.startDate) = :year")
    Integer getTotalApprovedLeavesByType(@Param("employeeId") Long employeeId,
                                         @Param("leaveType") LeaveType leaveType,
                                         @Param("year") int year);

    @Deprecated(forRemoval = false, since = "1.0.0")
    List<Leave> findByApprovedById(Long approverId);

    @Deprecated(forRemoval = false, since = "1.0.0")
    long countByEmployeeIdAndStatus(Long employeeId, LeaveStatus status);
}
