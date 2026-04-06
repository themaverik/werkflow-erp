package com.werkflow.business.hr.service;

import com.werkflow.business.common.context.TenantContext;
import com.werkflow.business.hr.dto.DepartmentRequest;
import com.werkflow.business.hr.dto.DepartmentResponse;
import com.werkflow.business.hr.entity.Department;
import com.werkflow.business.hr.repository.DepartmentRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Service for Department operations
 * All queries are tenant-scoped via TenantContext
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class DepartmentService {

    private final DepartmentRepository departmentRepository;
    private final TenantContext tenantContext;

    private String getTenantId() {
        return tenantContext.getTenantId();
    }

    public List<DepartmentResponse> getAllDepartments() {
        String tenantId = getTenantId();
        log.debug("Fetching all departments for tenant: {}", tenantId);
        return departmentRepository.findByTenantId(tenantId).stream()
            .map(this::convertToResponse)
            .collect(Collectors.toList());
    }

    public DepartmentResponse getDepartmentById(Long id) {
        String tenantId = getTenantId();
        log.debug("Fetching department by id: {} for tenant: {}", id, tenantId);
        Department department = departmentRepository.findById(id)
            .filter(d -> d.getTenantId().equals(tenantId))
            .orElseThrow(() -> new EntityNotFoundException("Department not found with id: " + id));
        return convertToResponse(department);
    }

    public DepartmentResponse getDepartmentByCode(String code) {
        String tenantId = getTenantId();
        log.debug("Fetching department by code: {} for tenant: {}", code, tenantId);
        Department department = departmentRepository.findByTenantIdAndCode(tenantId, code)
            .orElseThrow(() -> new EntityNotFoundException("Department not found with code: " + code));
        return convertToResponse(department);
    }

    public List<DepartmentResponse> getActiveDepartments() {
        String tenantId = getTenantId();
        log.debug("Fetching active departments for tenant: {}", tenantId);
        return departmentRepository.findByTenantIdAndIsActive(tenantId, true).stream()
            .map(this::convertToResponse)
            .collect(Collectors.toList());
    }

    public List<DepartmentResponse> getDepartmentsByOrganization(Long organizationId) {
        String tenantId = getTenantId();
        log.debug("Fetching departments for organization: {} in tenant: {}", organizationId, tenantId);
        return departmentRepository.findByTenantIdAndOrganizationId(tenantId, organizationId).stream()
            .map(this::convertToResponse)
            .collect(Collectors.toList());
    }

    @Transactional
    public DepartmentResponse createDepartment(DepartmentRequest request) {
        String tenantId = getTenantId();
        log.info("Creating new department: {} in tenant: {}", request.getName(), tenantId);

        if (departmentRepository.existsByTenantIdAndCodeAndOrganizationId(tenantId, request.getCode(), request.getOrganizationId())) {
            throw new IllegalArgumentException("Department code already exists in this organization: " + request.getCode());
        }
        if (departmentRepository.existsByTenantIdAndNameAndOrganizationId(tenantId, request.getName(), request.getOrganizationId())) {
            throw new IllegalArgumentException("Department name already exists in this organization: " + request.getName());
        }

        Department department = convertToEntity(request, tenantId);
        Department savedDepartment = departmentRepository.save(department);
        log.info("Department created successfully with id: {}", savedDepartment.getId());
        return convertToResponse(savedDepartment);
    }

    @Transactional
    public DepartmentResponse updateDepartment(Long id, DepartmentRequest request) {
        String tenantId = getTenantId();
        log.info("Updating department {} in tenant: {}", id, tenantId);

        Department department = departmentRepository.findById(id)
            .filter(d -> d.getTenantId().equals(tenantId))
            .orElseThrow(() -> new EntityNotFoundException("Department not found with id: " + id));

        if (!request.getCode().equals(department.getCode())
            && departmentRepository.existsByTenantIdAndCodeAndOrganizationId(tenantId, request.getCode(), request.getOrganizationId())) {
            throw new IllegalArgumentException("Department code already exists in this organization: " + request.getCode());
        }
        if (!request.getName().equals(department.getName())
            && departmentRepository.existsByTenantIdAndNameAndOrganizationId(tenantId, request.getName(), request.getOrganizationId())) {
            throw new IllegalArgumentException("Department name already exists in this organization: " + request.getName());
        }

        updateEntityFromRequest(department, request, tenantId);
        Department updatedDepartment = departmentRepository.save(department);
        log.info("Department updated successfully: {}", id);
        return convertToResponse(updatedDepartment);
    }

    @Transactional
    public void deleteDepartment(Long id) {
        String tenantId = getTenantId();
        log.info("Deactivating department {} in tenant: {}", id, tenantId);
        Department department = departmentRepository.findById(id)
            .filter(d -> d.getTenantId().equals(tenantId))
            .orElseThrow(() -> new EntityNotFoundException("Department not found with id: " + id));
        department.setIsActive(false);
        departmentRepository.save(department);
        log.info("Department deactivated successfully: {}", id);
    }

    private Department convertToEntity(DepartmentRequest request, String tenantId) {
        return Department.builder()
            .tenantId(tenantId)
            .name(request.getName())
            .code(request.getCode())
            .organizationId(request.getOrganizationId())
            .departmentType(request.getDepartmentType())
            .leadUserId(request.getLeadUserId())
            .officeLocation(request.getOfficeLocation())
            .departmentEmail(request.getDepartmentEmail())
            .isActive(request.getIsActive() != null ? request.getIsActive() : true)
            .build();
    }

    private void updateEntityFromRequest(Department department, DepartmentRequest request, String tenantId) {
        department.setName(request.getName());
        department.setCode(request.getCode());
        department.setOrganizationId(request.getOrganizationId());
        department.setDepartmentType(request.getDepartmentType());
        department.setLeadUserId(request.getLeadUserId());
        department.setOfficeLocation(request.getOfficeLocation());
        department.setDepartmentEmail(request.getDepartmentEmail());
        department.setIsActive(request.getIsActive());
    }

    private DepartmentResponse convertToResponse(Department department) {
        return DepartmentResponse.builder()
            .id(department.getId())
            .name(department.getName())
            .code(department.getCode())
            .organizationId(department.getOrganizationId())
            .departmentType(department.getDepartmentType())
            .leadUserId(department.getLeadUserId())
            .officeLocation(department.getOfficeLocation())
            .departmentEmail(department.getDepartmentEmail())
            .isActive(department.getIsActive())
            .createdAt(department.getCreatedAt())
            .updatedAt(department.getUpdatedAt())
            .build();
    }
}
