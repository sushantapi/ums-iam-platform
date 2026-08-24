package com.ums.hrms.employee.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.ums.hrms.employee.dto.CreateEmployeeRequest;
import com.ums.hrms.employee.entity.Employee;
import com.ums.hrms.employee.repository.EmployeeRepository;

@ExtendWith(MockitoExtension.class)
class EmployeePayslipIdentityTests {

    @Mock
    private EmployeeRepository employeeRepository;

    @Mock
    private OrganizationAccessService organizationAccessService;

    @Mock
    private OrganizationStructureReferenceService organizationStructureReferenceService;

    @Mock
    private EmployeeAuditPublisher employeeAuditPublisher;

    @InjectMocks
    private EmployeeService employeeService;

    @Test
    void createStoresOnlyMaskedPayslipIdentifiers() {
        UUID organizationId = UUID.randomUUID();
        UUID umsUserId = UUID.randomUUID();
        UUID actorUserId = UUID.randomUUID();
        LocalDate joiningDate = LocalDate.of(2025, 9, 24);

        when(employeeRepository.existsByOrganizationIdAndEmployeeCodeIgnoreCase(organizationId, "EMP-001"))
                .thenReturn(false);
        when(employeeRepository.existsByOrganizationIdAndUmsUserId(organizationId, umsUserId))
                .thenReturn(false);
        when(employeeRepository.save(any(Employee.class))).thenAnswer(invocation -> {
            Employee employee = invocation.getArgument(0);
            employee.setId(UUID.randomUUID());
            return employee;
        });

        var response = employeeService.create(
                new CreateEmployeeRequest(
                        organizationId,
                        umsUserId,
                        "EMP-001",
                        null,
                        null,
                        "  Sushant Kumar  ",
                        joiningDate,
                        "ABCDE1234F",
                        "100200300400",
                        "1234567890",
                        "001234567890"),
                actorUserId,
                false);

        assertEquals("Sushant Kumar", response.displayName());
        assertEquals(joiningDate, response.dateOfJoining());
        assertEquals("******234F", response.panDisplay());
        assertEquals("********0400", response.uanDisplay());
        assertEquals("******7890", response.esiDisplay());
        assertEquals("********7890", response.bankAccountDisplay());
    }
}
