package com.werkflow.business.hr.repository;

import com.werkflow.business.hr.entity.Employee;
import com.werkflow.business.hr.entity.EmploymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Repository for Employee entity
 */
@Repository
public interface EmployeeRepository extends JpaRepository<Employee, Long> {

    // Tenant-scoped methods (NEW)
    Optional<Employee> findByTenantIdAndEmail(@Param("tenantId") String tenantId,
                                              @Param("email") String email);

    Optional<Employee> findByTenantIdAndKeycloakUserId(@Param("tenantId") String tenantId,
                                                       @Param("keycloakUserId") String keycloakUserId);

    List<Employee> findByTenantIdAndOrganizationId(@Param("tenantId") String tenantId,
                                                   @Param("organizationId") Long orgId);

    List<Employee> findByTenantIdAndDepartmentCode(@Param("tenantId") String tenantId,
                                                   @Param("code") String code);

    List<Employee> findByTenantIdAndOrganizationIdAndDepartmentCode(@Param("tenantId") String tenantId,
                                                                    @Param("organizationId") Long orgId,
                                                                    @Param("code") String code);

    List<Employee> findByTenantIdAndDoaLevelGreaterThanEqual(@Param("tenantId") String tenantId,
                                                             @Param("level") Integer level);

    List<Employee> findByTenantIdAndEmploymentStatus(@Param("tenantId") String tenantId,
                                                     @Param("status") EmploymentStatus status);

    @Query("SELECT e FROM Employee e WHERE e.tenantId = :tenantId AND e.department.id = :departmentId")
    List<Employee> findByTenantIdAndDepartmentId(@Param("tenantId") String tenantId,
                                                 @Param("departmentId") Long departmentId);

    @Query("SELECT e FROM Employee e WHERE e.tenantId = :tenantId AND e.department.id = :departmentId " +
           "AND e.employmentStatus = :status")
    List<Employee> findByTenantIdAndDepartmentIdAndStatus(@Param("tenantId") String tenantId,
                                                          @Param("departmentId") Long departmentId,
                                                          @Param("status") EmploymentStatus status);

    @Query("SELECT e FROM Employee e WHERE e.tenantId = :tenantId AND " +
           "(LOWER(e.firstName) LIKE LOWER(CONCAT('%', :searchTerm, '%')) " +
           "OR LOWER(e.lastName) LIKE LOWER(CONCAT('%', :searchTerm, '%')) " +
           "OR LOWER(e.email) LIKE LOWER(CONCAT('%', :searchTerm, '%')))")
    List<Employee> searchEmployeesByTenant(@Param("tenantId") String tenantId,
                                          @Param("searchTerm") String searchTerm);

    @Query("SELECT e FROM Employee e WHERE e.tenantId = :tenantId " +
           "AND e.dateOfJoining BETWEEN :startDate AND :endDate")
    List<Employee> findByTenantIdAndJoinDateBetween(@Param("tenantId") String tenantId,
                                                    @Param("startDate") LocalDate startDate,
                                                    @Param("endDate") LocalDate endDate);

    boolean existsByTenantIdAndEmail(@Param("tenantId") String tenantId,
                                     @Param("email") String email);

    boolean existsByTenantIdAndKeycloakUserId(@Param("tenantId") String tenantId,
                                              @Param("keycloakUserId") String keycloakUserId);

    boolean existsByTenantIdAndDepartmentCodeAndDoaLevel(@Param("tenantId") String tenantId,
                                                         @Param("code") String code,
                                                         @Param("doaLevel") Integer doaLevel);

    boolean existsByTenantIdAndDepartmentCodeAndDoaLevelAndIdNot(@Param("tenantId") String tenantId,
                                                                  @Param("departmentCode") String departmentCode,
                                                                  @Param("doaLevel") Integer doaLevel,
                                                                  @Param("id") Long id);

    long countByTenantIdAndDepartmentCode(@Param("tenantId") String tenantId,
                                          @Param("code") String code);

    long countByTenantIdAndOrganizationId(@Param("tenantId") String tenantId,
                                          @Param("organizationId") Long orgId);

    long countByTenantIdAndEmploymentStatus(@Param("tenantId") String tenantId,
                                            @Param("status") EmploymentStatus status);

    // Legacy methods (kept for backward compatibility, but deprecated)
    @Deprecated(forRemoval = false, since = "1.0.0")
    Optional<Employee> findByEmail(String email);

    @Deprecated(forRemoval = false, since = "1.0.0")
    Optional<Employee> findByKeycloakUserId(String keycloakUserId);

    @Deprecated(forRemoval = false, since = "1.0.0")
    List<Employee> findByOrganizationId(Long orgId);

    @Deprecated(forRemoval = false, since = "1.0.0")
    List<Employee> findByDepartmentCode(String code);

    @Deprecated(forRemoval = false, since = "1.0.0")
    List<Employee> findByOrganizationIdAndDepartmentCode(Long orgId, String code);

    @Deprecated(forRemoval = false, since = "1.0.0")
    List<Employee> findByDoaLevelGreaterThanEqual(Integer level);

    @Deprecated(forRemoval = false, since = "1.0.0")
    List<Employee> findByEmploymentStatus(EmploymentStatus status);

    @Deprecated(forRemoval = false, since = "1.0.0")
    @Query("SELECT e FROM Employee e WHERE e.department.id = :departmentId")
    List<Employee> findByDepartmentId(@Param("departmentId") Long departmentId);

    @Deprecated(forRemoval = false, since = "1.0.0")
    @Query("SELECT e FROM Employee e WHERE e.department.id = :departmentId AND e.employmentStatus = :status")
    List<Employee> findByDepartmentIdAndStatus(@Param("departmentId") Long departmentId,
                                               @Param("status") EmploymentStatus status);

    @Deprecated(forRemoval = false, since = "1.0.0")
    @Query("SELECT e FROM Employee e WHERE LOWER(e.firstName) LIKE LOWER(CONCAT('%', :searchTerm, '%')) " +
           "OR LOWER(e.lastName) LIKE LOWER(CONCAT('%', :searchTerm, '%')) " +
           "OR LOWER(e.email) LIKE LOWER(CONCAT('%', :searchTerm, '%'))")
    List<Employee> searchEmployees(@Param("searchTerm") String searchTerm);

    @Deprecated(forRemoval = false, since = "1.0.0")
    @Query("SELECT e FROM Employee e WHERE e.dateOfJoining BETWEEN :startDate AND :endDate")
    List<Employee> findByJoinDateBetween(@Param("startDate") LocalDate startDate,
                                         @Param("endDate") LocalDate endDate);

    @Deprecated(forRemoval = false, since = "1.0.0")
    boolean existsByEmail(String email);

    @Deprecated(forRemoval = false, since = "1.0.0")
    boolean existsByKeycloakUserId(String keycloakUserId);

    @Deprecated(forRemoval = false, since = "1.0.0")
    // Used for department head uniqueness validation (doaLevel=2 means dept_head)
    boolean existsByDepartmentCodeAndDoaLevel(String code, Integer doaLevel);

    @Deprecated(forRemoval = false, since = "1.0.0")
    // Used on update path to exclude the employee being updated from the uniqueness check
    boolean existsByDepartmentCodeAndDoaLevelAndIdNot(String departmentCode, Integer doaLevel, Long id);

    @Deprecated(forRemoval = false, since = "1.0.0")
    long countByDepartmentCode(String code);

    @Deprecated(forRemoval = false, since = "1.0.0")
    long countByOrganizationId(Long orgId);

    @Deprecated(forRemoval = false, since = "1.0.0")
    long countByEmploymentStatus(EmploymentStatus status);
}
