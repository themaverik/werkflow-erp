package com.werkflow.business.hr.service;

import com.werkflow.business.common.context.TenantContext;
import com.werkflow.business.hr.dto.EmployeeRequest;
import com.werkflow.business.hr.dto.EmployeeResponse;
import com.werkflow.business.hr.entity.Department;
import com.werkflow.business.hr.entity.Employee;
import com.werkflow.business.hr.entity.EmploymentStatus;
import com.werkflow.business.hr.repository.DepartmentRepository;
import com.werkflow.business.hr.repository.EmployeeRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Service for Employee operations
 * All queries are tenant-scoped via TenantContext
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class EmployeeService {

    private final EmployeeRepository employeeRepository;
    private final DepartmentRepository departmentRepository;
    private final RoleDisplayService roleDisplayService;
    private final TenantContext tenantContext;

    private String getTenantId() {
        return tenantContext.getTenantId();
    }

    public Page<EmployeeResponse> getAllEmployees(Pageable pageable) {
        String tenantId = getTenantId();
        log.debug("Fetching all employees for tenant: {}", tenantId);
        return employeeRepository.findByTenantId(tenantId, pageable)
            .map(this::convertToResponse);
    }

    public EmployeeResponse getEmployeeById(Long id) {
        String tenantId = getTenantId();
        log.debug("Fetching employee by id: {} for tenant: {}", id, tenantId);
        Employee employee = employeeRepository.findById(id)
            .filter(e -> e.getTenantId().equals(tenantId))
            .orElseThrow(() -> new EntityNotFoundException("Employee not found with id: " + id));
        return convertToResponse(employee);
    }

    public EmployeeResponse getEmployeeByEmail(String email) {
        String tenantId = getTenantId();
        log.debug("Fetching employee by email: {} for tenant: {}", email, tenantId);
        Employee employee = employeeRepository.findByTenantIdAndEmail(tenantId, email)
            .orElseThrow(() -> new EntityNotFoundException("Employee not found with email: " + email));
        return convertToResponse(employee);
    }

    public EmployeeResponse getEmployeeByKeycloakUserId(String keycloakUserId) {
        String tenantId = getTenantId();
        log.debug("Fetching employee by keycloakUserId: {} for tenant: {}", keycloakUserId, tenantId);
        Employee employee = employeeRepository.findByTenantIdAndKeycloakUserId(tenantId, keycloakUserId)
            .orElseThrow(() -> new EntityNotFoundException(
                "Employee not found with keycloakUserId: " + keycloakUserId));
        return convertToResponse(employee);
    }

    public Page<EmployeeResponse> getEmployeesByOrganization(Long orgId, Pageable pageable) {
        String tenantId = getTenantId();
        log.debug("Fetching employees for org: {} in tenant: {}", orgId, tenantId);
        return employeeRepository.findByTenantIdAndOrganizationId(tenantId, orgId, pageable)
            .map(this::convertToResponse);
    }

    public Page<EmployeeResponse> getEmployeesByDepartment(Long departmentId, Pageable pageable) {
        String tenantId = getTenantId();
        log.debug("Fetching employees for department: {} in tenant: {}", departmentId, tenantId);
        return employeeRepository.findByTenantIdAndDepartmentId(tenantId, departmentId, pageable)
            .map(this::convertToResponse);
    }

    public Page<EmployeeResponse> getEmployeesByDepartmentCode(String code, Pageable pageable) {
        String tenantId = getTenantId();
        log.debug("Fetching employees for department code: {} in tenant: {}", code, tenantId);
        return employeeRepository.findByTenantIdAndDepartmentCode(tenantId, code, pageable)
            .map(this::convertToResponse);
    }

    public Page<EmployeeResponse> getEmployeesByStatus(EmploymentStatus status, Pageable pageable) {
        String tenantId = getTenantId();
        log.debug("Fetching employees by status: {} in tenant: {}", status, tenantId);
        return employeeRepository.findByTenantIdAndEmploymentStatus(tenantId, status, pageable)
            .map(this::convertToResponse);
    }

    public Page<EmployeeResponse> searchEmployees(String searchTerm, Pageable pageable) {
        String tenantId = getTenantId();
        log.debug("Searching employees for term: {} in tenant: {}", searchTerm, tenantId);
        return employeeRepository.searchEmployeesByTenant(tenantId, searchTerm, pageable)
            .map(this::convertToResponse);
    }

    @Transactional
    public EmployeeResponse createEmployee(EmployeeRequest request) {
        String tenantId = getTenantId();
        log.info("Creating new employee: {} in tenant: {}", request.getEmail(), tenantId);

        if (employeeRepository.existsByTenantIdAndEmail(tenantId, request.getEmail())) {
            throw new IllegalArgumentException("Email already exists: " + request.getEmail());
        }
        if (request.getKeycloakUserId() != null
                && employeeRepository.existsByTenantIdAndKeycloakUserId(tenantId, request.getKeycloakUserId())) {
            throw new IllegalArgumentException(
                "Keycloak user already linked: " + request.getKeycloakUserId());
        }

        Employee employee = convertToEntity(request, tenantId);
        Employee savedEmployee = employeeRepository.save(employee);
        log.info("Employee created successfully with id: {}", savedEmployee.getId());
        return convertToResponse(savedEmployee);
    }

    @Transactional
    public EmployeeResponse updateEmployee(Long id, EmployeeRequest request) {
        String tenantId = getTenantId();
        log.info("Updating employee {} in tenant: {}", id, tenantId);

        Employee employee = employeeRepository.findById(id)
            .filter(e -> e.getTenantId().equals(tenantId))
            .orElseThrow(() -> new EntityNotFoundException("Employee not found with id: " + id));

        if (!request.getEmail().equals(employee.getEmail())
                && employeeRepository.existsByTenantIdAndEmail(tenantId, request.getEmail())) {
            throw new IllegalArgumentException("Email already exists: " + request.getEmail());
        }

        updateEntityFromRequest(employee, request, tenantId);
        Employee updatedEmployee = employeeRepository.save(employee);
        log.info("Employee updated successfully: {}", id);
        return convertToResponse(updatedEmployee);
    }

    @Transactional
    public void deleteEmployee(Long id) {
        String tenantId = getTenantId();
        log.info("Deleting employee {} in tenant: {}", id, tenantId);
        Employee employee = employeeRepository.findById(id)
            .filter(e -> e.getTenantId().equals(tenantId))
            .orElseThrow(() -> new EntityNotFoundException("Employee not found with id: " + id));
        employeeRepository.delete(employee);
        log.info("Employee deleted successfully: {}", id);
    }

    private Employee convertToEntity(EmployeeRequest request, String tenantId) {
        Department department = null;
        if (request.getDepartmentId() != null) {
            department = departmentRepository.findById(request.getDepartmentId())
                .filter(d -> d.getTenantId().equals(tenantId))
                .orElseThrow(() -> new EntityNotFoundException(
                    "Department not found with id: " + request.getDepartmentId()));
        }

        validateDeptHeadUniqueness(tenantId, request.getDepartmentCode(), request.getDoaLevel(), null);

        return Employee.builder()
            .tenantId(tenantId)
            .organizationId(request.getOrganizationId())
            .keycloakUserId(request.getKeycloakUserId())
            .firstName(request.getFirstName())
            .lastName(request.getLastName())
            .email(request.getEmail())
            .phone(request.getPhone())
            .gender(request.getGender())
            .profilePhotoUrl(request.getProfilePhotoUrl())
            .department(department)
            .departmentCode(request.getDepartmentCode())
            .doaLevel(request.getDoaLevel() != null ? request.getDoaLevel() : 0)
            .officeLocation(request.getOfficeLocation())
            .position(request.getPosition())
            .dateOfJoining(request.getDateOfJoining())
            .employmentStatus(request.getEmploymentStatus())
            .salary(request.getSalary())
            .build();
    }

    private void updateEntityFromRequest(Employee employee, EmployeeRequest request, String tenantId) {
        // Department head uniqueness: only check if doaLevel is changing to 2
        if (request.getDoaLevel() != null && request.getDoaLevel() == 2) {
            validateDeptHeadUniqueness(tenantId, request.getDepartmentCode(), request.getDoaLevel(), employee.getId());
        }

        employee.setOrganizationId(request.getOrganizationId());
        employee.setKeycloakUserId(request.getKeycloakUserId());
        employee.setFirstName(request.getFirstName());
        employee.setLastName(request.getLastName());
        employee.setEmail(request.getEmail());
        employee.setPhone(request.getPhone());
        employee.setGender(request.getGender());
        employee.setProfilePhotoUrl(request.getProfilePhotoUrl());
        employee.setDepartmentCode(request.getDepartmentCode());
        employee.setDoaLevel(request.getDoaLevel() != null ? request.getDoaLevel() : 0);
        employee.setOfficeLocation(request.getOfficeLocation());
        employee.setPosition(request.getPosition());
        employee.setDateOfJoining(request.getDateOfJoining());
        employee.setEmploymentStatus(request.getEmploymentStatus());
        employee.setSalary(request.getSalary());

        if (request.getDepartmentId() != null) {
            Department department = departmentRepository.findById(request.getDepartmentId())
                .filter(d -> d.getTenantId().equals(tenantId))
                .orElseThrow(() -> new EntityNotFoundException(
                    "Department not found with id: " + request.getDepartmentId()));
            employee.setDepartment(department);
        } else {
            employee.setDepartment(null);
        }
    }

    /**
     * Enforces at most one department_head (doaLevel=2) per department code within a tenant.
     * Pass currentEmployeeId to exclude the employee being updated from the check.
     */
    private void validateDeptHeadUniqueness(String tenantId, String departmentCode, Integer doaLevel, Long currentEmployeeId) {
        if (doaLevel != null && doaLevel == 2 && departmentCode != null) {
            boolean headExists = currentEmployeeId == null
                ? employeeRepository.existsByTenantIdAndDepartmentCodeAndDoaLevel(tenantId, departmentCode, 2)
                : employeeRepository.existsByTenantIdAndDepartmentCodeAndDoaLevelAndIdNot(tenantId, departmentCode, 2, currentEmployeeId);
            if (headExists) {
                throw new IllegalStateException(
                    "A department head already exists for department: " + departmentCode);
            }
        }
    }

    public Map<String, String> getRoleDisplay(Long id, List<String> keycloakRoles) {
        String tenantId = getTenantId();
        log.debug("Getting role display for employee {} in tenant: {} with roles: {}", id, tenantId, keycloakRoles);
        Employee employee = employeeRepository.findById(id)
            .filter(e -> e.getTenantId().equals(tenantId))
            .orElseThrow(() -> new EntityNotFoundException("Employee not found: " + id));

        String displayRole = roleDisplayService.getDisplayRole(keycloakRoles, employee.getDepartmentCode());
        return Map.of(
            "displayRole", displayRole,
            "doaLevel", String.valueOf(employee.getDoaLevel())
        );
    }

    private EmployeeResponse convertToResponse(Employee employee) {
        String departmentName = employee.getDepartment() != null
            ? employee.getDepartment().getName()
            : null;

        return EmployeeResponse.builder()
            .id(employee.getId())
            .organizationId(employee.getOrganizationId())
            .keycloakUserId(employee.getKeycloakUserId())
            .firstName(employee.getFirstName())
            .lastName(employee.getLastName())
            .fullName(employee.getFullName())
            .email(employee.getEmail())
            .phone(employee.getPhone())
            .gender(employee.getGender())
            .profilePhotoUrl(employee.getProfilePhotoUrl())
            .departmentId(employee.getDepartmentId())
            .departmentName(departmentName)
            .departmentCode(employee.getDepartmentCode())
            .doaLevel(employee.getDoaLevel())
            .officeLocation(employee.getOfficeLocation())
            .position(employee.getPosition())
            .dateOfJoining(employee.getDateOfJoining())
            .employmentStatus(employee.getEmploymentStatus())
            .salary(employee.getSalary())
            .isActive(employee.getIsActive())
            .createdAt(employee.getCreatedAt())
            .updatedAt(employee.getUpdatedAt())
            .build();
    }
}
