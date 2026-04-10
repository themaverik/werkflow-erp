package com.werkflow.business.hr.service;

import com.werkflow.business.common.context.TenantContext;
import com.werkflow.business.hr.dto.EmployeeRequest;
import com.werkflow.business.hr.dto.EmployeeResponse;
import com.werkflow.business.hr.entity.Employee;
import com.werkflow.business.hr.entity.EmploymentStatus;
import com.werkflow.business.hr.repository.DepartmentRepository;
import com.werkflow.business.hr.repository.EmployeeRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@DisplayName("EmployeeService Contract Tests")
class EmployeeServiceContractTest {

    @Mock
    private EmployeeRepository employeeRepository;

    @Mock
    private DepartmentRepository departmentRepository;

    @Mock
    private RoleDisplayService roleDisplayService;

    @Mock
    private TenantContext tenantContext;

    private EmployeeService employeeService;
    private static final String TENANT_ID = "ACME";

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        employeeService = new EmployeeService(
            employeeRepository,
            departmentRepository,
            roleDisplayService,
            tenantContext
        );
        when(tenantContext.getTenantId()).thenReturn(TENANT_ID);
    }

    @Test
    @DisplayName("Contract: Create employee with duplicate email should reject")
    void testCreateEmployeeWithDuplicateEmailThrows() {
        // Arrange: duplicate email exists
        EmployeeRequest request = EmployeeRequest.builder()
            .firstName("John")
            .lastName("Doe")
            .email("john@acme.com")
            .phone("123-456-7890")
            .organizationId(1L)
            .employmentStatus(EmploymentStatus.ACTIVE)
            .build();

        when(employeeRepository.existsByTenantIdAndEmail(TENANT_ID, "john@acme.com"))
            .thenReturn(true);

        // Act & Assert: contract is "no duplicate emails"
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> employeeService.createEmployee(request)
        );
        assertTrue(exception.getMessage().contains("Email already exists"));
    }

    @Test
    @DisplayName("Contract: Create employee with duplicate keycloakUserId should reject")
    void testCreateEmployeeWithDuplicateKeycloakUserIdThrows() {
        // Arrange: keycloak user already linked
        String keycloakUserId = "kc-123";
        EmployeeRequest request = EmployeeRequest.builder()
            .firstName("Jane")
            .lastName("Smith")
            .email("jane@acme.com")
            .keycloakUserId(keycloakUserId)
            .organizationId(1L)
            .employmentStatus(EmploymentStatus.ACTIVE)
            .build();

        when(employeeRepository.existsByTenantIdAndEmail(TENANT_ID, "jane@acme.com"))
            .thenReturn(false);
        when(employeeRepository.existsByTenantIdAndKeycloakUserId(TENANT_ID, keycloakUserId))
            .thenReturn(true);

        // Act & Assert: contract is "no duplicate keycloak links"
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> employeeService.createEmployee(request)
        );
        assertTrue(exception.getMessage().contains("Keycloak user already linked"));
    }

    @Test
    @DisplayName("Contract: Link keycloak user twice (idempotent)")
    void testLinkKeycloakUserIdempotent() {
        // Arrange: employee with existing keycloak link
        Long employeeId = 1L;
        String keycloakUserId = "kc-456";

        Employee employee = Employee.builder()
            .firstName("Alice")
            .lastName("Brown")
            .email("alice@acme.com")
            .tenantId(TENANT_ID)
            .keycloakUserId(keycloakUserId)
            .build();

        when(employeeRepository.findById(employeeId)).thenReturn(Optional.of(employee));
        when(employeeRepository.save(any(Employee.class))).thenReturn(employee);

        // Act: link same keycloak user twice
        EmployeeResponse response1 = employeeService.linkKeycloakUser(employeeId, keycloakUserId);
        EmployeeResponse response2 = employeeService.linkKeycloakUser(employeeId, keycloakUserId);

        // Assert: contract is "idempotent" — both succeed
        assertNotNull(response1);
        assertNotNull(response2);
        assertEquals(keycloakUserId, response2.getKeycloakUserId());
    }

    @Test
    @DisplayName("Contract: Link different keycloak user to already-linked employee should fail")
    void testLinkDifferentKeycloakUserThrows() {
        // Arrange: employee already linked to different user
        Long employeeId = 2L;
        String existingKeycloakUserId = "kc-111";
        String newKeycloakUserId = "kc-222";

        Employee employee = Employee.builder()
            .firstName("Diana")
            .lastName("Evans")
            .email("diana@acme.com")
            .tenantId(TENANT_ID)
            .keycloakUserId(existingKeycloakUserId)
            .build();

        when(employeeRepository.findById(employeeId)).thenReturn(Optional.of(employee));

        // Act & Assert: contract is "no re-linking to different user"
        DataIntegrityViolationException exception = assertThrows(
            DataIntegrityViolationException.class,
            () -> employeeService.linkKeycloakUser(employeeId, newKeycloakUserId)
        );
        assertTrue(exception.getMessage().contains("already linked to a different Keycloak user"));
    }

    @Test
    @DisplayName("Contract: Department head uniqueness — cannot create duplicate dept heads")
    void testDepartmentHeadUniquenessFails() {
        // Arrange: dept head already exists for department
        EmployeeRequest request = EmployeeRequest.builder()
            .firstName("Eve")
            .lastName("Frank")
            .email("eve@acme.com")
            .organizationId(1L)
            .departmentCode("SALES")
            .doaLevel(2) // dept head level
            .employmentStatus(EmploymentStatus.ACTIVE)
            .build();

        when(employeeRepository.existsByTenantIdAndEmail(TENANT_ID, "eve@acme.com"))
            .thenReturn(false);
        when(employeeRepository.existsByTenantIdAndDepartmentCodeAndDoaLevel(TENANT_ID, "SALES", 2))
            .thenReturn(true); // dept head already exists

        // Act & Assert: contract is "at most one dept head per department"
        IllegalStateException exception = assertThrows(
            IllegalStateException.class,
            () -> employeeService.createEmployee(request)
        );
        assertTrue(exception.getMessage().contains("department head already exists"));
    }

    @Test
    @DisplayName("Contract: Tenant isolation — cross-tenant access blocked")
    void testTenantIsolationEnforced() {
        // Arrange: employee from different tenant
        Long employeeId = 3L;
        Employee employee = Employee.builder()
            .firstName("Frank")
            .lastName("Green")
            .email("frank@other.com")
            .tenantId("OTHER_TENANT")
            .build();

        when(employeeRepository.findById(employeeId)).thenReturn(Optional.of(employee));

        // Act & Assert: contract is "no cross-tenant leakage"
        EntityNotFoundException exception = assertThrows(
            EntityNotFoundException.class,
            () -> employeeService.getEmployeeById(employeeId)
        );
        assertTrue(exception.getMessage().contains("Employee not found"));
    }
}
