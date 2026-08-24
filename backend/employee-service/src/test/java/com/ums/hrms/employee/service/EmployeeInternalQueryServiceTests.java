package com.ums.hrms.employee.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import com.ums.hrms.employee.entity.Department;
import com.ums.hrms.employee.entity.Designation;
import com.ums.hrms.employee.entity.Employee;
import com.ums.hrms.employee.entity.EmployeeStatus;
import com.ums.hrms.employee.repository.DepartmentRepository;
import com.ums.hrms.employee.repository.DesignationRepository;
import com.ums.hrms.employee.repository.EmployeeRepository;

@ExtendWith(MockitoExtension.class)
class EmployeeInternalQueryServiceTests {

    @Mock
    private EmployeeRepository employeeRepository;

    @Mock
    private DepartmentRepository departmentRepository;

    @Mock
    private DesignationRepository designationRepository;

    @InjectMocks
    private EmployeeInternalQueryService service;

    @Test
    void returnsTenantScopedPayslipIdentityWithResolvedStructureNames() {
        UUID organizationId = UUID.randomUUID();
        UUID employeeId = UUID.randomUUID();
        UUID departmentId = UUID.randomUUID();
        UUID designationId = UUID.randomUUID();

        Employee employee = Employee.builder()
                .id(employeeId)
                .organizationId(organizationId)
                .umsUserId(UUID.randomUUID())
                .employeeCode("EMP-001")
                .displayName("Sushant Kumar")
                .dateOfJoining(LocalDate.of(2025, 9, 24))
                .departmentId(departmentId)
                .designationId(designationId)
                .panDisplay("******234F")
                .uanDisplay("********0400")
                .esiDisplay("******7890")
                .bankAccountDisplay("********7890")
                .status(EmployeeStatus.ACTIVE)
                .build();

        when(employeeRepository.findByIdAndOrganizationId(employeeId, organizationId))
                .thenReturn(Optional.of(employee));
        when(departmentRepository.findByIdAndOrganizationId(departmentId, organizationId))
                .thenReturn(Optional.of(Department.builder()
                        .id(departmentId)
                        .organizationId(organizationId)
                        .name("Engineering")
                        .build()));
        when(designationRepository.findByIdAndOrganizationId(designationId, organizationId))
                .thenReturn(Optional.of(Designation.builder()
                        .id(designationId)
                        .organizationId(organizationId)
                        .name("Backend Engineer")
                        .build()));

        var response = service.get(employeeId, organizationId);

        assertEquals("Sushant Kumar", response.displayName());
        assertEquals("Engineering", response.departmentName());
        assertEquals("Backend Engineer", response.designationName());
        assertEquals("******234F", response.panDisplay());
        assertEquals("********7890", response.bankAccountDisplay());
    }

    @Test
    void neverReturnsEmployeeFromAnotherOrganization() {
        UUID employeeId = UUID.randomUUID();
        UUID organizationId = UUID.randomUUID();
        when(employeeRepository.findByIdAndOrganizationId(employeeId, organizationId))
                .thenReturn(Optional.empty());

        assertThrows(ResponseStatusException.class, () -> service.get(employeeId, organizationId));
    }
}
