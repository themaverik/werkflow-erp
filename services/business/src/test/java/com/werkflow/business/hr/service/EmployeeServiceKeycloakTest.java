package com.werkflow.business.hr.service;

import com.werkflow.business.common.context.TenantContext;
import com.werkflow.business.hr.dto.EmployeeResponse;
import com.werkflow.business.hr.entity.Employee;
import com.werkflow.business.hr.repository.DepartmentRepository;
import com.werkflow.business.hr.repository.EmployeeRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class EmployeeServiceKeycloakTest {

    @Mock
    private EmployeeRepository employeeRepository;

    @Mock
    private DepartmentRepository departmentRepository;

    @Mock
    private RoleDisplayService roleDisplayService;

    @Mock
    private TenantContext tenantContext;

    @InjectMocks
    private EmployeeService employeeService;

    private Employee testEmployee;

    @BeforeEach
    void setUp() {
        testEmployee = Employee.builder()
            .tenantId("ACME")
            .organizationId(100L)
            .firstName("John")
            .lastName("Doe")
            .email("john@example.com")
            .keycloakUserId(null)
            .build();
        testEmployee.setId(1L);
    }

    @Test
    void testLinkKeycloakUserHappyPath() {
        when(tenantContext.getTenantId()).thenReturn("ACME");
        when(employeeRepository.findById(1L)).thenReturn(Optional.of(testEmployee));
        when(employeeRepository.save(any(Employee.class))).thenAnswer(inv -> inv.getArgument(0));

        EmployeeResponse result = employeeService.linkKeycloakUser(1L, "keycloak-uuid-123");

        assertNotNull(result);
        assertEquals("John", result.getFirstName());
        verify(employeeRepository).save(argThat(emp ->
            "keycloak-uuid-123".equals(emp.getKeycloakUserId())
        ));
    }

    @Test
    void testLinkKeycloakUserIdempotent() {
        testEmployee.setKeycloakUserId("keycloak-uuid-123");
        when(tenantContext.getTenantId()).thenReturn("ACME");
        when(employeeRepository.findById(1L)).thenReturn(Optional.of(testEmployee));
        when(employeeRepository.save(any(Employee.class))).thenAnswer(inv -> inv.getArgument(0));

        EmployeeResponse result = employeeService.linkKeycloakUser(1L, "keycloak-uuid-123");

        assertNotNull(result);
        assertEquals("keycloak-uuid-123", result.getKeycloakUserId());
    }

    @Test
    void testLinkKeycloakUserConflict() {
        testEmployee.setKeycloakUserId("keycloak-uuid-old");
        when(tenantContext.getTenantId()).thenReturn("ACME");
        when(employeeRepository.findById(1L)).thenReturn(Optional.of(testEmployee));

        // Execute & Verify: throws DataIntegrityViolationException with generic message (no UUID leak)
        DataIntegrityViolationException ex = assertThrows(DataIntegrityViolationException.class, () ->
            employeeService.linkKeycloakUser(1L, "keycloak-uuid-new")
        );

        // Verify message does not leak UUID values
        assertTrue(ex.getMessage().contains("different Keycloak user"));
        assertFalse(ex.getMessage().contains("keycloak-uuid-old"));
        assertFalse(ex.getMessage().contains("keycloak-uuid-new"));
    }

    @Test
    void testLinkKeycloakUserNotFound() {
        when(employeeRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () ->
            employeeService.linkKeycloakUser(1L, "keycloak-uuid-123")
        );
    }

    @Test
    void testLinkKeycloakUserTenantIsolation() {
        when(tenantContext.getTenantId()).thenReturn("TECH");
        when(employeeRepository.findById(1L)).thenReturn(Optional.of(testEmployee));

        assertThrows(EntityNotFoundException.class, () ->
            employeeService.linkKeycloakUser(1L, "keycloak-uuid-123")
        );
    }
}
