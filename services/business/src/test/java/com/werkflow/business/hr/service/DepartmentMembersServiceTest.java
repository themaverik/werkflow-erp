package com.werkflow.business.hr.service;

import com.werkflow.business.common.context.TenantContext;
import com.werkflow.business.hr.dto.EmployeeResponse;
import com.werkflow.business.hr.entity.Employee;
import com.werkflow.business.hr.repository.DepartmentRepository;
import com.werkflow.business.hr.repository.EmployeeRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DepartmentMembersServiceTest {

    @Mock private DepartmentRepository departmentRepository;
    @Mock private EmployeeRepository employeeRepository;
    @Mock private TenantContext tenantContext;
    @Mock private com.werkflow.business.common.context.UserContext userContext;

    @InjectMocks
    private DepartmentService departmentService;

    @Test
    void getMembersByDeptCode_returnsTenantScopedPage() {
        Employee emp = new Employee();
        emp.setId(1L);
        emp.setKeycloakUserId("kc-user-1");
        emp.setFirstName("Jane");
        emp.setLastName("Smith");
        emp.setDepartmentCode("FIN");

        when(tenantContext.getTenantId()).thenReturn("tenant-abc");
        when(employeeRepository.findByTenantIdAndDepartmentCode("tenant-abc", "FIN", PageRequest.of(0, 10)))
                .thenReturn(new PageImpl<>(List.of(emp)));

        Page<EmployeeResponse> result = departmentService.getMembersByDeptCode("FIN", PageRequest.of(0, 10));

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getKeycloakUserId()).isEqualTo("kc-user-1");
        assertThat(result.getContent().get(0).getDepartmentCode()).isEqualTo("FIN");
    }

    @Test
    void getMembersByDeptCode_returnsEmptyPage_whenNoneFound() {
        when(tenantContext.getTenantId()).thenReturn("tenant-abc");
        when(employeeRepository.findByTenantIdAndDepartmentCode("tenant-abc", "UNKNOWN", PageRequest.of(0, 10)))
                .thenReturn(Page.empty());

        Page<EmployeeResponse> result = departmentService.getMembersByDeptCode("UNKNOWN", PageRequest.of(0, 10));

        assertThat(result.getContent()).isEmpty();
    }
}
