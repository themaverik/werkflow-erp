package com.werkflow.business.hr.service;

import com.werkflow.business.common.context.TenantContext;
import com.werkflow.business.hr.dto.PayrollRequest;
import com.werkflow.business.hr.dto.PayrollResponse;
import com.werkflow.business.hr.entity.Employee;
import com.werkflow.business.hr.entity.Payroll;
import com.werkflow.business.hr.repository.EmployeeRepository;
import com.werkflow.business.hr.repository.PayrollRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class PayrollService {

    private final PayrollRepository payrollRepository;
    private final EmployeeRepository employeeRepository;
    private final TenantContext tenantContext;

    private String getTenantId() {
        return tenantContext.getTenantId();
    }

    public List<PayrollResponse> getAllPayrolls() {
        String tenantId = getTenantId();
        log.debug("Fetching all payrolls for tenant: {}", tenantId);
        return payrollRepository.findByTenantId(tenantId).stream()
            .map(this::convertToResponse)
            .collect(Collectors.toList());
    }

    public PayrollResponse getPayrollById(Long id) {
        String tenantId = getTenantId();
        log.debug("Fetching payroll by id: {} for tenant: {}", id, tenantId);
        Payroll payroll = payrollRepository.findById(id)
            .filter(p -> p.getTenantId().equals(tenantId))
            .orElseThrow(() -> new EntityNotFoundException("Payroll not found"));
        return convertToResponse(payroll);
    }

    public List<PayrollResponse> getPayrollsByEmployee(Long employeeId) {
        String tenantId = getTenantId();
        log.debug("Fetching payrolls for employee: {} in tenant: {}", employeeId, tenantId);
        return payrollRepository.findByTenantIdAndEmployeeIdOrderByPaymentDateDesc(tenantId, employeeId)
            .stream()
            .map(this::convertToResponse)
            .collect(Collectors.toList());
    }

    public List<PayrollResponse> getPayrollsByMonthYear(Integer month, Integer year) {
        String tenantId = getTenantId();
        log.debug("Fetching payrolls for month: {} year: {} in tenant: {}", month, year, tenantId);
        return payrollRepository.findByTenantIdAndMonthAndYear(tenantId, month, year)
            .stream()
            .map(this::convertToResponse)
            .collect(Collectors.toList());
    }

    @Transactional
    public PayrollResponse createPayroll(PayrollRequest request) {
        String tenantId = getTenantId();
        log.info("Creating payroll for employee: {} in tenant: {}", request.getEmployeeId(), tenantId);

        Employee employee = employeeRepository.findById(request.getEmployeeId())
            .filter(e -> e.getTenantId().equals(tenantId))
            .orElseThrow(() -> new EntityNotFoundException("Employee not found"));

        // Check if payroll already exists for this month/year
        if (payrollRepository.existsByTenantIdAndEmployeeIdAndPaymentMonthAndPaymentYear(
            tenantId, request.getEmployeeId(), request.getPaymentMonth(), request.getPaymentYear())) {
            throw new IllegalStateException("Payroll already exists for this month/year");
        }

        Payroll payroll = Payroll.builder()
            .tenantId(tenantId)
            .employee(employee)
            .paymentMonth(request.getPaymentMonth())
            .paymentYear(request.getPaymentYear())
            .paymentDate(request.getPaymentDate())
            .basicSalary(request.getBasicSalary())
            .allowances(request.getAllowances() != null ? request.getAllowances() : BigDecimal.ZERO)
            .bonuses(request.getBonuses() != null ? request.getBonuses() : BigDecimal.ZERO)
            .overtimePay(request.getOvertimePay() != null ? request.getOvertimePay() : BigDecimal.ZERO)
            .taxDeduction(request.getTaxDeduction() != null ? request.getTaxDeduction() : BigDecimal.ZERO)
            .insuranceDeduction(request.getInsuranceDeduction() != null ? request.getInsuranceDeduction() : BigDecimal.ZERO)
            .otherDeductions(request.getOtherDeductions() != null ? request.getOtherDeductions() : BigDecimal.ZERO)
            .remarks(request.getRemarks())
            .isPaid(false)
            .build();

        Payroll saved = payrollRepository.save(payroll);
        log.info("Payroll created with id: {}", saved.getId());
        return convertToResponse(saved);
    }

    @Transactional
    public PayrollResponse updatePayroll(Long id, PayrollRequest request) {
        String tenantId = getTenantId();
        log.info("Updating payroll {} in tenant: {}", id, tenantId);

        Payroll payroll = payrollRepository.findById(id)
            .filter(p -> p.getTenantId().equals(tenantId))
            .orElseThrow(() -> new EntityNotFoundException("Payroll not found"));

        payroll.setPaymentDate(request.getPaymentDate());
        payroll.setBasicSalary(request.getBasicSalary());
        payroll.setAllowances(request.getAllowances() != null ? request.getAllowances() : BigDecimal.ZERO);
        payroll.setBonuses(request.getBonuses() != null ? request.getBonuses() : BigDecimal.ZERO);
        payroll.setOvertimePay(request.getOvertimePay() != null ? request.getOvertimePay() : BigDecimal.ZERO);
        payroll.setTaxDeduction(request.getTaxDeduction() != null ? request.getTaxDeduction() : BigDecimal.ZERO);
        payroll.setInsuranceDeduction(request.getInsuranceDeduction() != null ? request.getInsuranceDeduction() : BigDecimal.ZERO);
        payroll.setOtherDeductions(request.getOtherDeductions() != null ? request.getOtherDeductions() : BigDecimal.ZERO);
        payroll.setRemarks(request.getRemarks());

        return convertToResponse(payrollRepository.save(payroll));
    }

    @Transactional
    public PayrollResponse markAsPaid(Long id) {
        String tenantId = getTenantId();
        log.info("Marking payroll {} as paid for tenant: {}", id, tenantId);

        Payroll payroll = payrollRepository.findById(id)
            .filter(p -> p.getTenantId().equals(tenantId))
            .orElseThrow(() -> new EntityNotFoundException("Payroll not found with id: " + id));

        payroll.setIsPaid(true);
        return convertToResponse(payrollRepository.save(payroll));
    }

    @Transactional
    public void deletePayroll(Long id) {
        String tenantId = getTenantId();
        log.info("Deleting payroll {} in tenant: {}", id, tenantId);

        Payroll payroll = payrollRepository.findById(id)
            .filter(p -> p.getTenantId().equals(tenantId))
            .orElseThrow(() -> new EntityNotFoundException("Payroll not found with id: " + id));

        payrollRepository.delete(payroll);
    }

    private PayrollResponse convertToResponse(Payroll payroll) {
        return PayrollResponse.builder()
            .id(payroll.getId())
            .employeeId(payroll.getEmployee().getId())
            .employeeName(payroll.getEmployee().getFullName())
            .paymentMonth(payroll.getPaymentMonth())
            .paymentYear(payroll.getPaymentYear())
            .paymentDate(payroll.getPaymentDate())
            .basicSalary(payroll.getBasicSalary())
            .allowances(payroll.getAllowances())
            .bonuses(payroll.getBonuses())
            .overtimePay(payroll.getOvertimePay())
            .taxDeduction(payroll.getTaxDeduction())
            .insuranceDeduction(payroll.getInsuranceDeduction())
            .otherDeductions(payroll.getOtherDeductions())
            .grossSalary(payroll.getGrossSalary())
            .netSalary(payroll.getNetSalary())
            .remarks(payroll.getRemarks())
            .isPaid(payroll.getIsPaid())
            .createdAt(payroll.getCreatedAt())
            .updatedAt(payroll.getUpdatedAt())
            .build();
    }
}
