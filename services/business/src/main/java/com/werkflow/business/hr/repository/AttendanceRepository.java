package com.werkflow.business.hr.repository;

import com.werkflow.business.hr.entity.Attendance;
import com.werkflow.business.hr.entity.AttendanceStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Repository for Attendance entity
 */
@Repository
public interface AttendanceRepository extends JpaRepository<Attendance, Long> {

    // Tenant-scoped methods (NEW)
    Optional<Attendance> findByTenantIdAndEmployeeIdAndAttendanceDate(@Param("tenantId") String tenantId,
                                                                      @Param("employeeId") Long employeeId,
                                                                      @Param("attendanceDate") LocalDate attendanceDate);

    List<Attendance> findByTenantIdAndEmployeeId(@Param("tenantId") String tenantId,
                                                 @Param("employeeId") Long employeeId);

    List<Attendance> findByTenantIdAndAttendanceDate(@Param("tenantId") String tenantId,
                                                     @Param("attendanceDate") LocalDate attendanceDate);

    @Query("SELECT a FROM Attendance a WHERE a.tenantId = :tenantId AND a.employee.id = :employeeId " +
           "AND a.attendanceDate BETWEEN :startDate AND :endDate " +
           "ORDER BY a.attendanceDate DESC")
    List<Attendance> findByTenantIdAndEmployeeIdAndDateRange(@Param("tenantId") String tenantId,
                                                             @Param("employeeId") Long employeeId,
                                                             @Param("startDate") LocalDate startDate,
                                                             @Param("endDate") LocalDate endDate);

    @Query("SELECT a FROM Attendance a WHERE a.tenantId = :tenantId AND a.employee.id = :employeeId " +
           "AND a.status = :status " +
           "AND a.attendanceDate BETWEEN :startDate AND :endDate")
    List<Attendance> findByTenantIdAndEmployeeIdAndStatusAndDateRange(@Param("tenantId") String tenantId,
                                                                      @Param("employeeId") Long employeeId,
                                                                      @Param("status") AttendanceStatus status,
                                                                      @Param("startDate") LocalDate startDate,
                                                                      @Param("endDate") LocalDate endDate);

    @Query("SELECT SUM(a.workedHours) FROM Attendance a WHERE a.tenantId = :tenantId AND a.employee.id = :employeeId " +
           "AND a.attendanceDate BETWEEN :startDate AND :endDate")
    Double getTotalWorkedHoursTenant(@Param("tenantId") String tenantId,
                                     @Param("employeeId") Long employeeId,
                                     @Param("startDate") LocalDate startDate,
                                     @Param("endDate") LocalDate endDate);

    @Query("SELECT COUNT(a) FROM Attendance a WHERE a.tenantId = :tenantId AND a.employee.id = :employeeId " +
           "AND a.status = :status " +
           "AND EXTRACT(MONTH FROM a.attendanceDate) = :month " +
           "AND EXTRACT(YEAR FROM a.attendanceDate) = :year")
    long countByTenantIdAndEmployeeIdAndStatusAndMonthYear(@Param("tenantId") String tenantId,
                                                           @Param("employeeId") Long employeeId,
                                                           @Param("status") AttendanceStatus status,
                                                           @Param("month") int month,
                                                           @Param("year") int year);

    boolean existsByTenantIdAndEmployeeIdAndAttendanceDate(@Param("tenantId") String tenantId,
                                                           @Param("employeeId") Long employeeId,
                                                           @Param("attendanceDate") LocalDate attendanceDate);

    // Legacy methods (kept for backward compatibility, but deprecated)
    @Deprecated(forRemoval = false, since = "1.0.0")
    Optional<Attendance> findByEmployeeIdAndAttendanceDate(Long employeeId, LocalDate attendanceDate);

    @Deprecated(forRemoval = false, since = "1.0.0")
    List<Attendance> findByEmployeeId(Long employeeId);

    @Deprecated(forRemoval = false, since = "1.0.0")
    List<Attendance> findByAttendanceDate(LocalDate attendanceDate);

    @Deprecated(forRemoval = false, since = "1.0.0")
    @Query("SELECT a FROM Attendance a WHERE a.employee.id = :employeeId " +
           "AND a.attendanceDate BETWEEN :startDate AND :endDate " +
           "ORDER BY a.attendanceDate DESC")
    List<Attendance> findByEmployeeIdAndDateRange(@Param("employeeId") Long employeeId,
                                                  @Param("startDate") LocalDate startDate,
                                                  @Param("endDate") LocalDate endDate);

    @Deprecated(forRemoval = false, since = "1.0.0")
    @Query("SELECT a FROM Attendance a WHERE a.employee.id = :employeeId " +
           "AND a.status = :status " +
           "AND a.attendanceDate BETWEEN :startDate AND :endDate")
    List<Attendance> findByEmployeeIdAndStatusAndDateRange(@Param("employeeId") Long employeeId,
                                                           @Param("status") AttendanceStatus status,
                                                           @Param("startDate") LocalDate startDate,
                                                           @Param("endDate") LocalDate endDate);

    @Deprecated(forRemoval = false, since = "1.0.0")
    @Query("SELECT SUM(a.workedHours) FROM Attendance a WHERE a.employee.id = :employeeId " +
           "AND a.attendanceDate BETWEEN :startDate AND :endDate")
    Double getTotalWorkedHours(@Param("employeeId") Long employeeId,
                               @Param("startDate") LocalDate startDate,
                               @Param("endDate") LocalDate endDate);

    @Deprecated(forRemoval = false, since = "1.0.0")
    @Query("SELECT COUNT(a) FROM Attendance a WHERE a.employee.id = :employeeId " +
           "AND a.status = :status " +
           "AND EXTRACT(MONTH FROM a.attendanceDate) = :month " +
           "AND EXTRACT(YEAR FROM a.attendanceDate) = :year")
    long countByEmployeeIdAndStatusAndMonthYear(@Param("employeeId") Long employeeId,
                                                @Param("status") AttendanceStatus status,
                                                @Param("month") int month,
                                                @Param("year") int year);

    @Deprecated(forRemoval = false, since = "1.0.0")
    boolean existsByEmployeeIdAndAttendanceDate(Long employeeId, LocalDate attendanceDate);
}
