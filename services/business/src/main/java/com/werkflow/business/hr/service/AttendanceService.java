package com.werkflow.business.hr.service;

import com.werkflow.business.common.context.TenantContext;
import com.werkflow.business.hr.dto.AttendanceRequest;
import com.werkflow.business.hr.dto.AttendanceResponse;
import com.werkflow.business.hr.entity.Attendance;
import com.werkflow.business.hr.entity.Employee;
import com.werkflow.business.hr.repository.AttendanceRepository;
import com.werkflow.business.hr.repository.EmployeeRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class AttendanceService {

    private final AttendanceRepository attendanceRepository;
    private final EmployeeRepository employeeRepository;
    private final TenantContext tenantContext;

    private String getTenantId() {
        return tenantContext.getTenantId();
    }

    public Page<AttendanceResponse> getAllAttendances(Pageable pageable) {
        String tenantId = getTenantId();
        log.debug("Fetching all attendances for tenant: {}", tenantId);
        return attendanceRepository.findByTenantId(tenantId, pageable)
            .map(this::convertToResponse);
    }

    public AttendanceResponse getAttendanceById(Long id) {
        String tenantId = getTenantId();
        log.debug("Fetching attendance by id: {} for tenant: {}", id, tenantId);
        Attendance attendance = attendanceRepository.findById(id)
            .filter(a -> a.getTenantId().equals(tenantId))
            .orElseThrow(() -> new EntityNotFoundException("Attendance not found"));
        return convertToResponse(attendance);
    }

    public Page<AttendanceResponse> getAttendancesByEmployee(Long employeeId, Pageable pageable) {
        String tenantId = getTenantId();
        log.debug("Fetching attendances for employee: {} in tenant: {}", employeeId, tenantId);
        return attendanceRepository.findByTenantIdAndEmployeeId(tenantId, employeeId, pageable)
            .map(this::convertToResponse);
    }

    public Page<AttendanceResponse> getAttendancesByDateRange(Long employeeId,
                                                              LocalDate startDate,
                                                              LocalDate endDate,
                                                              Pageable pageable) {
        String tenantId = getTenantId();
        log.debug("Fetching attendances for employee: {} between {} and {} in tenant: {}",
            employeeId, startDate, endDate, tenantId);
        return attendanceRepository.findByTenantIdAndEmployeeIdAndDateRange(tenantId, employeeId, startDate, endDate, pageable)
            .map(this::convertToResponse);
    }

    @Transactional
    public AttendanceResponse createAttendance(AttendanceRequest request) {
        String tenantId = getTenantId();
        log.info("Creating attendance for employee: {} in tenant: {}", request.getEmployeeId(), tenantId);

        Employee employee = employeeRepository.findById(request.getEmployeeId())
            .filter(e -> e.getTenantId().equals(tenantId))
            .orElseThrow(() -> new EntityNotFoundException("Employee not found"));

        // Check if attendance already exists
        if (attendanceRepository.existsByTenantIdAndEmployeeIdAndAttendanceDate(
            tenantId, request.getEmployeeId(), request.getAttendanceDate())) {
            throw new IllegalStateException("Attendance already exists for this date");
        }

        Attendance attendance = Attendance.builder()
            .tenantId(tenantId)
            .employee(employee)
            .attendanceDate(request.getAttendanceDate())
            .checkInTime(request.getCheckInTime())
            .checkOutTime(request.getCheckOutTime())
            .status(request.getStatus())
            .remarks(request.getRemarks())
            .build();

        Attendance saved = attendanceRepository.save(attendance);
        log.info("Attendance created with id: {}", saved.getId());
        return convertToResponse(saved);
    }

    @Transactional
    public AttendanceResponse updateAttendance(Long id, AttendanceRequest request) {
        String tenantId = getTenantId();
        log.info("Updating attendance {} in tenant: {}", id, tenantId);

        Attendance attendance = attendanceRepository.findById(id)
            .filter(a -> a.getTenantId().equals(tenantId))
            .orElseThrow(() -> new EntityNotFoundException("Attendance not found"));

        attendance.setCheckInTime(request.getCheckInTime());
        attendance.setCheckOutTime(request.getCheckOutTime());
        attendance.setStatus(request.getStatus());
        attendance.setRemarks(request.getRemarks());

        return convertToResponse(attendanceRepository.save(attendance));
    }

    @Transactional
    public void deleteAttendance(Long id) {
        String tenantId = getTenantId();
        log.info("Deleting attendance {} in tenant: {}", id, tenantId);

        Attendance attendance = attendanceRepository.findById(id)
            .filter(a -> a.getTenantId().equals(tenantId))
            .orElseThrow(() -> new EntityNotFoundException("Attendance not found"));

        attendanceRepository.delete(attendance);
    }

    private AttendanceResponse convertToResponse(Attendance attendance) {
        return AttendanceResponse.builder()
            .id(attendance.getId())
            .employeeId(attendance.getEmployee().getId())
            .employeeName(attendance.getEmployee().getFullName())
            .attendanceDate(attendance.getAttendanceDate())
            .checkInTime(attendance.getCheckInTime())
            .checkOutTime(attendance.getCheckOutTime())
            .status(attendance.getStatus())
            .remarks(attendance.getRemarks())
            .workedHours(attendance.getWorkedHours())
            .createdAt(attendance.getCreatedAt())
            .updatedAt(attendance.getUpdatedAt())
            .build();
    }
}
