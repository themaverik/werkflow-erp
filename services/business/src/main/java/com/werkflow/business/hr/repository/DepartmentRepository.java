package com.werkflow.business.hr.repository;

import com.werkflow.business.hr.entity.Department;
import com.werkflow.business.hr.entity.DepartmentType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for Department entity
 */
@Repository
public interface DepartmentRepository extends JpaRepository<Department, Long> {

    // Tenant-scoped methods (NEW)
    List<Department> findByTenantId(@Param("tenantId") String tenantId);

    Optional<Department> findByTenantIdAndCode(@Param("tenantId") String tenantId,
                                               @Param("code") String code);

    Optional<Department> findByTenantIdAndName(@Param("tenantId") String tenantId,
                                               @Param("name") String name);

    List<Department> findByTenantIdAndIsActive(@Param("tenantId") String tenantId,
                                               @Param("isActive") Boolean isActive);

    List<Department> findByTenantIdAndOrganizationId(@Param("tenantId") String tenantId,
                                                     @Param("organizationId") Long organizationId);

    List<Department> findByTenantIdAndOrganizationIdAndDepartmentType(@Param("tenantId") String tenantId,
                                                                      @Param("organizationId") Long organizationId,
                                                                      @Param("departmentType") DepartmentType departmentType);

    boolean existsByTenantIdAndCode(@Param("tenantId") String tenantId,
                                    @Param("code") String code);

    boolean existsByTenantIdAndName(@Param("tenantId") String tenantId,
                                    @Param("name") String name);

    boolean existsByTenantIdAndCodeAndOrganizationId(@Param("tenantId") String tenantId,
                                                     @Param("code") String code,
                                                     @Param("organizationId") Long organizationId);

    boolean existsByTenantIdAndNameAndOrganizationId(@Param("tenantId") String tenantId,
                                                     @Param("name") String name,
                                                     @Param("organizationId") Long organizationId);

    // Legacy methods (kept for backward compatibility, but deprecated)
    @Deprecated(forRemoval = false, since = "1.0.0")
    Optional<Department> findByCode(String code);

    @Deprecated(forRemoval = false, since = "1.0.0")
    Optional<Department> findByName(String name);

    @Deprecated(forRemoval = false, since = "1.0.0")
    List<Department> findByIsActive(Boolean isActive);

    @Deprecated(forRemoval = false, since = "1.0.0")
    List<Department> findByOrganizationId(Long organizationId);

    @Deprecated(forRemoval = false, since = "1.0.0")
    List<Department> findByOrganizationIdAndDepartmentType(Long organizationId, DepartmentType departmentType);

    @Deprecated(forRemoval = false, since = "1.0.0")
    boolean existsByCode(String code);

    @Deprecated(forRemoval = false, since = "1.0.0")
    boolean existsByName(String name);

    @Deprecated(forRemoval = false, since = "1.0.0")
    boolean existsByCodeAndOrganizationId(String code, Long organizationId);

    @Deprecated(forRemoval = false, since = "1.0.0")
    boolean existsByNameAndOrganizationId(String name, Long organizationId);
}
