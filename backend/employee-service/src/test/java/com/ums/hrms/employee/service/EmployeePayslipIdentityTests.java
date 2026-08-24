package com.ums.hrms.employee.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.ums.hrms.employee.dto.CreateEmployeeRequest;
import com.ums.hrms.employee.dto.UpdateEmployeeRequest;
import com.ums.hrms.employee.entity.Employee;
import com.ums.hrms.employee.entity.EmployeeStatus;
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

    @Test
    void legacyUpdatePreservesPayslipIdentityWhenNewFieldsAreOmitted() {
        UUID employeeId = UUID.randomUUID();
        UUID organizationId = UUID.randomUUID();
        UUID actorUserId = UUID.randomUUID();
        LocalDate joiningDate = LocalDate.of(2025, 9, 24);
        Employee existing = Employee.builder()
                .id(employeeId)
                .organizationId(organizationId)
                .umsUserId(UUID.randomUUID())
                .employeeCode("EMP-001")
                .displayName("Sushant Kumar")
                .dateOfJoining(joiningDate)
                .panDisplay("******234F")
                .uanDisplay("********0400")
                .esiDisplay("******7890")
                .bankAccountDisplay("********7890")
                .status(EmployeeStatus.ACTIVE)
                .build();

        when(employeeRepository.findByIdAndOrganizationId(employeeId, organizationId))
                .thenReturn(Optional.of(existing));
        when(employeeRepository.save(any(Employee.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = employeeService.update(
                employeeId,
                new UpdateEmployeeRequest(
                        organizationId,
                        "EMP-001",
                        null,
                        null,
                        EmployeeStatus.ACTIVE),
                actorUserId,
                false);

        assertEquals("Sushant Kumar", response.displayName());
        assertEquals(joiningDate, response.dateOfJoining());
        assertEquals("******234F", response.panDisplay());
        assertEquals("********0400", response.uanDisplay());
        assertEquals("******7890", response.esiDisplay());
        assertEquals("********7890", response.bankAccountDisplay());
    }

    @Test
    void explicitIdentityUpdatesReplaceOnlySuppliedValues() {
        UUID employeeId = UUID.randomUUID();
        UUID organizationId = UUID.randomUUID();
        UUID actorUserId = UUID.randomUUID();
        Employee existing = Employee.builder()
                .id(employeeId)
                .organizationId(organizationId)
                .umsUserId(UUID.randomUUID())
                .employeeCode("EMP-001")
                .displayName("Old Name")
                .dateOfJoining(LocalDate.of(2025, 1, 1))
                .panDisplay("******1111")
                .uanDisplay("********2222")
                .esiDisplay("******3333")
                .bankAccountDisplay("********4444")
                .status(EmployeeStatus.ACTIVE)
                .build();

        when(employeeRepository.findByIdAndOrganizationId(employeeId, organizationId))
                .thenReturn(Optional.of(existing));
        when(employeeRepository.save(any(Employee.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = employeeService.update(
                employeeId,
                new UpdateEmployeeRequest(
                        organizationId,
                        "EMP-001",
                        null,
                        null,
                        EmployeeStatus.ACTIVE,
                        "New Name",
                        LocalDate.of(2025, 2, 2),
                        "ABCDE9999Z",
                        null,
                        null,
                        "123456789012"),
                actorUserId,
                false);

        assertEquals("New Name", response.displayName());
        assertEquals(LocalDate.of(2025, 2, 2), response.dateOfJoining());
        assertEquals("******999Z", response.panDisplay());
        assertEquals("********2222", response.uanDisplay());
        assertEquals("******3333", response.esiDisplay());
        assertEquals("********9012", response.bankAccountDisplay());
    }
}
