package com.werkflow.business.hr.service;

import com.werkflow.business.common.context.TenantContext;
import com.werkflow.business.hr.dto.LeaveRequest;
import com.werkflow.business.hr.dto.LeaveResponse;
import com.werkflow.business.hr.entity.Employee;
import com.werkflow.business.hr.entity.Leave;
import com.werkflow.business.hr.entity.LeaveStatus;
import com.werkflow.business.hr.repository.EmployeeRepository;
import com.werkflow.business.hr.repository.LeaveRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class LeaveService {

    private final LeaveRepository leaveRepository;
    private final EmployeeRepository employeeRepository;
    private final TenantContext tenantContext;

    private String getTenantId() {
        return tenantContext.getTenantId();
    }

    public List<LeaveResponse> getAllLeaves() {
        String tenantId = getTenantId();
        log.debug("Fetching all leaves for tenant: {}", tenantId);
        return leaveRepository.findAll().stream()
            .filter(l -> l.getTenantId().equals(tenantId))
            .map(this::convertToResponse)
            .collect(Collectors.toList());
    }

    public LeaveResponse getLeaveById(Long id) {
        String tenantId = getTenantId();
        log.debug("Fetching leave by id: {} for tenant: {}", id, tenantId);
        Leave leave = leaveRepository.findById(id)
            .filter(l -> l.getTenantId().equals(tenantId))
            .orElseThrow(() -> new EntityNotFoundException("Leave not found with id: " + id));
        return convertToResponse(leave);
    }

    public List<LeaveResponse> getLeavesByEmployee(Long employeeId) {
        String tenantId = getTenantId();
        log.debug("Fetching leaves for employee: {} in tenant: {}", employeeId, tenantId);
        return leaveRepository.findByTenantIdAndEmployeeId(tenantId, employeeId).stream()
            .map(this::convertToResponse)
            .collect(Collectors.toList());
    }

    public List<LeaveResponse> getLeavesByStatus(LeaveStatus status) {
        String tenantId = getTenantId();
        log.debug("Fetching leaves by status: {} in tenant: {}", status, tenantId);
        return leaveRepository.findByTenantIdAndStatus(tenantId, status).stream()
            .map(this::convertToResponse)
            .collect(Collectors.toList());
    }

    @Transactional
    public LeaveResponse createLeave(LeaveRequest request) {
        String tenantId = getTenantId();
        log.info("Creating leave request for employee: {} in tenant: {}", request.getEmployeeId(), tenantId);

        Employee employee = employeeRepository.findById(request.getEmployeeId())
            .filter(e -> e.getTenantId().equals(tenantId))
            .orElseThrow(() -> new EntityNotFoundException("Employee not found"));

        // Check for overlapping leaves
        List<Leave> overlapping = leaveRepository.findByTenantIdAndEmployeeIdAndOverlappingDates(
            tenantId, request.getEmployeeId(), request.getStartDate(), request.getEndDate());
        if (!overlapping.isEmpty()) {
            throw new IllegalStateException("Leave request overlaps with existing leave");
        }

        Leave leave = Leave.builder()
            .tenantId(tenantId)
            .employee(employee)
            .leaveType(request.getLeaveType())
            .startDate(request.getStartDate())
            .endDate(request.getEndDate())
            .numberOfDays(request.getNumberOfDays())
            .reason(request.getReason())
            .status(LeaveStatus.PENDING)
            .build();

        Leave savedLeave = leaveRepository.save(leave);
        log.info("Leave request created with id: {}", savedLeave.getId());
        return convertToResponse(savedLeave);
    }

    @Transactional
    public LeaveResponse updateLeave(Long id, LeaveRequest request) {
        String tenantId = getTenantId();
        log.info("Updating leave {} in tenant: {}", id, tenantId);

        Leave leave = leaveRepository.findById(id)
            .filter(l -> l.getTenantId().equals(tenantId))
            .orElseThrow(() -> new EntityNotFoundException("Leave not found"));

        leave.setLeaveType(request.getLeaveType());
        leave.setStartDate(request.getStartDate());
        leave.setEndDate(request.getEndDate());
        leave.setNumberOfDays(request.getNumberOfDays());
        leave.setReason(request.getReason());

        return convertToResponse(leaveRepository.save(leave));
    }

    @Transactional
    public LeaveResponse approveLeave(Long id, Long approverId, String remarks) {
        String tenantId = getTenantId();
        log.info("Approving leave {} in tenant: {}", id, tenantId);

        Leave leave = leaveRepository.findById(id)
            .filter(l -> l.getTenantId().equals(tenantId))
            .orElseThrow(() -> new EntityNotFoundException("Leave not found"));

        Employee approver = employeeRepository.findById(approverId)
            .filter(e -> e.getTenantId().equals(tenantId))
            .orElseThrow(() -> new EntityNotFoundException("Approver not found"));

        leave.setStatus(LeaveStatus.APPROVED);
        leave.setApprovedBy(approver);
        leave.setApprovedAt(LocalDate.now());
        leave.setApprovalRemarks(remarks);

        return convertToResponse(leaveRepository.save(leave));
    }

    @Transactional
    public LeaveResponse rejectLeave(Long id, Long approverId, String remarks) {
        String tenantId = getTenantId();
        log.info("Rejecting leave {} in tenant: {}", id, tenantId);

        Leave leave = leaveRepository.findById(id)
            .filter(l -> l.getTenantId().equals(tenantId))
            .orElseThrow(() -> new EntityNotFoundException("Leave not found"));

        Employee approver = employeeRepository.findById(approverId)
            .filter(e -> e.getTenantId().equals(tenantId))
            .orElseThrow(() -> new EntityNotFoundException("Approver not found"));

        leave.setStatus(LeaveStatus.REJECTED);
        leave.setApprovedBy(approver);
        leave.setApprovedAt(LocalDate.now());
        leave.setApprovalRemarks(remarks);

        return convertToResponse(leaveRepository.save(leave));
    }

    @Transactional
    public void deleteLeave(Long id) {
        String tenantId = getTenantId();
        log.info("Deleting leave {} in tenant: {}", id, tenantId);

        Leave leave = leaveRepository.findById(id)
            .filter(l -> l.getTenantId().equals(tenantId))
            .orElseThrow(() -> new EntityNotFoundException("Leave not found"));

        leaveRepository.delete(leave);
    }

    private LeaveResponse convertToResponse(Leave leave) {
        return LeaveResponse.builder()
            .id(leave.getId())
            .employeeId(leave.getEmployee().getId())
            .employeeName(leave.getEmployee().getFullName())
            .leaveType(leave.getLeaveType())
            .startDate(leave.getStartDate())
            .endDate(leave.getEndDate())
            .numberOfDays(leave.getNumberOfDays())
            .reason(leave.getReason())
            .status(leave.getStatus())
            .approvedById(leave.getApprovedBy() != null ? leave.getApprovedBy().getId() : null)
            .approvedByName(leave.getApprovedBy() != null ? leave.getApprovedBy().getFullName() : null)
            .approvedAt(leave.getApprovedAt())
            .approvalRemarks(leave.getApprovalRemarks())
            .createdAt(leave.getCreatedAt())
            .updatedAt(leave.getUpdatedAt())
            .build();
    }
}
