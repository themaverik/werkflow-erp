package com.werkflow.business.hr.repository;

import com.werkflow.business.hr.entity.Payroll;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Repository for Payroll entity
 */
@Repository
public interface PayrollRepository extends JpaRepository<Payroll, Long> {

    // Tenant-scoped methods (NEW)
    Page<Payroll> findByTenantId(@Param("tenantId") String tenantId, Pageable pageable);

    Page<Payroll> findByTenantIdAndEmployeeId(@Param("tenantId") String tenantId,
                                              @Param("employeeId") Long employeeId,
                                              Pageable pageable);

    Optional<Payroll> findByTenantIdAndEmployeeIdAndPaymentMonthAndPaymentYear(@Param("tenantId") String tenantId,
                                                                                @Param("employeeId") Long employeeId,
                                                                                @Param("paymentMonth") Integer month,
                                                                                @Param("paymentYear") Integer year);

    @Query("SELECT p FROM Payroll p WHERE p.tenantId = :tenantId AND p.employee.id = :employeeId " +
           "ORDER BY p.paymentYear DESC, p.paymentMonth DESC")
    Page<Payroll> findByTenantIdAndEmployeeIdOrderByPaymentDateDesc(@Param("tenantId") String tenantId,
                                                                     @Param("employeeId") Long employeeId,
                                                                     Pageable pageable);

    @Query("SELECT p FROM Payroll p WHERE p.tenantId = :tenantId AND p.paymentMonth = :month AND p.paymentYear = :year")
    Page<Payroll> findByTenantIdAndMonthAndYear(@Param("tenantId") String tenantId,
                                                @Param("month") Integer month,
                                                @Param("year") Integer year,
                                                Pageable pageable);

    @Query("SELECT p FROM Payroll p WHERE p.tenantId = :tenantId AND p.paymentDate BETWEEN :startDate AND :endDate")
    Page<Payroll> findByTenantIdAndPaymentDateBetween(@Param("tenantId") String tenantId,
                                                      @Param("startDate") LocalDate startDate,
                                                      @Param("endDate") LocalDate endDate,
                                                      Pageable pageable);

    Page<Payroll> findByTenantIdAndIsPaid(@Param("tenantId") String tenantId,
                                          @Param("isPaid") Boolean isPaid,
                                          Pageable pageable);

    @Query("SELECT p FROM Payroll p WHERE p.tenantId = :tenantId AND p.employee.department.id = :departmentId " +
           "AND p.paymentMonth = :month AND p.paymentYear = :year")
    Page<Payroll> findByTenantIdAndDepartmentAndMonthYear(@Param("tenantId") String tenantId,
                                                          @Param("departmentId") Long departmentId,
                                                          @Param("month") Integer month,
                                                          @Param("year") Integer year,
                                                          Pageable pageable);

    @Query("SELECT SUM(p.netSalary) FROM Payroll p WHERE p.tenantId = :tenantId AND p.paymentMonth = :month " +
           "AND p.paymentYear = :year AND p.isPaid = true")
    Double getTotalPaidSalaryByTenantAndMonthYear(@Param("tenantId") String tenantId,
                                                  @Param("month") Integer month,
                                                  @Param("year") Integer year);

    @Query("SELECT SUM(p.netSalary) FROM Payroll p WHERE p.tenantId = :tenantId AND p.employee.department.id = :departmentId " +
           "AND p.paymentMonth = :month AND p.paymentYear = :year")
    Double getTotalSalaryByTenantAndDepartmentAndMonthYear(@Param("tenantId") String tenantId,
                                                           @Param("departmentId") Long departmentId,
                                                           @Param("month") Integer month,
                                                           @Param("year") Integer year);

    boolean existsByTenantIdAndEmployeeIdAndPaymentMonthAndPaymentYear(@Param("tenantId") String tenantId,
                                                                        @Param("employeeId") Long employeeId,
                                                                        @Param("paymentMonth") Integer month,
                                                                        @Param("paymentYear") Integer year);

    // Legacy methods (kept for backward compatibility, but deprecated)
    @Deprecated(forRemoval = false, since = "1.0.0")
    List<Payroll> findByEmployeeId(Long employeeId);

    @Deprecated(forRemoval = false, since = "1.0.0")
    Optional<Payroll> findByEmployeeIdAndPaymentMonthAndPaymentYear(Long employeeId, Integer month, Integer year);

    @Deprecated(forRemoval = false, since = "1.0.0")
    @Query("SELECT p FROM Payroll p WHERE p.employee.id = :employeeId " +
           "ORDER BY p.paymentYear DESC, p.paymentMonth DESC")
    List<Payroll> findByEmployeeIdOrderByPaymentDateDesc(@Param("employeeId") Long employeeId);

    @Deprecated(forRemoval = false, since = "1.0.0")
    @Query("SELECT p FROM Payroll p WHERE p.paymentMonth = :month AND p.paymentYear = :year")
    List<Payroll> findByMonthAndYear(@Param("month") Integer month, @Param("year") Integer year);

    @Deprecated(forRemoval = false, since = "1.0.0")
    @Query("SELECT p FROM Payroll p WHERE p.paymentDate BETWEEN :startDate AND :endDate")
    List<Payroll> findByPaymentDateBetween(@Param("startDate") LocalDate startDate,
                                           @Param("endDate") LocalDate endDate);

    @Deprecated(forRemoval = false, since = "1.0.0")
    List<Payroll> findByIsPaid(Boolean isPaid);

    @Deprecated(forRemoval = false, since = "1.0.0")
    @Query("SELECT p FROM Payroll p WHERE p.employee.department.id = :departmentId " +
           "AND p.paymentMonth = :month AND p.paymentYear = :year")
    List<Payroll> findByDepartmentAndMonthYear(@Param("departmentId") Long departmentId,
                                               @Param("month") Integer month,
                                               @Param("year") Integer year);

    @Deprecated(forRemoval = false, since = "1.0.0")
    @Query("SELECT SUM(p.netSalary) FROM Payroll p WHERE p.paymentMonth = :month " +
           "AND p.paymentYear = :year AND p.isPaid = true")
    Double getTotalPaidSalaryByMonthYear(@Param("month") Integer month, @Param("year") Integer year);

    @Deprecated(forRemoval = false, since = "1.0.0")
    @Query("SELECT SUM(p.netSalary) FROM Payroll p WHERE p.employee.department.id = :departmentId " +
           "AND p.paymentMonth = :month AND p.paymentYear = :year")
    Double getTotalSalaryByDepartmentAndMonthYear(@Param("departmentId") Long departmentId,
                                                   @Param("month") Integer month,
                                                   @Param("year") Integer year);

    @Deprecated(forRemoval = false, since = "1.0.0")
    boolean existsByEmployeeIdAndPaymentMonthAndPaymentYear(Long employeeId, Integer month, Integer year);
}
