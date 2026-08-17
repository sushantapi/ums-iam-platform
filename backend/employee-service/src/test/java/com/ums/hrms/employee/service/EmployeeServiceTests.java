package com.ums.hrms.employee.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import com.ums.hrms.employee.dto.CreateEmployeeRequest;
import com.ums.hrms.employee.dto.UpdateEmployeeRequest;
import com.ums.hrms.employee.entity.Employee;
import com.ums.hrms.employee.entity.EmployeeStatus;
import com.ums.hrms.employee.repository.EmployeeRepository;

@ExtendWith(MockitoExtension.class)
class EmployeeServiceTests {

    @Mock
    private EmployeeRepository employeeRepository;

    @Mock
    private OrganizationAccessService organizationAccessService;

    @Mock
    private EmployeeAuditPublisher employeeAuditPublisher;

    @InjectMocks
    private EmployeeService employeeService;

    @Test
    void createPersistsTenantScopedEmployee() {
        UUID organizationId = UUID.randomUUID();
        UUID umsUserId = UUID.randomUUID();
        UUID actorUserId = UUID.randomUUID();
        UUID employeeId = UUID.randomUUID();

        when(employeeRepository.existsByOrganizationIdAndEmployeeCodeIgnoreCase(organizationId, "EMP-001"))
                .thenReturn(false);
        when(employeeRepository.existsByOrganizationIdAndUmsUserId(organizationId, umsUserId)).thenReturn(false);
        when(employeeRepository.save(any(Employee.class))).thenAnswer(invocation -> {
            Employee employee = invocation.getArgument(0);
            employee.setId(employeeId);
            return employee;
        });

        var response = employeeService.create(
                new CreateEmployeeRequest(organizationId, umsUserId, " EMP-001 ", null, null),
                actorUserId,
                false);

        verify(organizationAccessService).assertCanAccess(organizationId, actorUserId, false);
        verify(organizationAccessService).assertUserBelongsToOrganization(organizationId, umsUserId);
        verify(employeeAuditPublisher).publishCreated(any(Employee.class), org.mockito.ArgumentMatchers.eq(actorUserId));
        assertEquals(employeeId, response.id());
        assertEquals(organizationId, response.organizationId());
        assertEquals("EMP-001", response.employeeCode());
        assertEquals(EmployeeStatus.ACTIVE, response.status());
    }

    @Test
    void createRejectsUmsUserOutsideOrganizationBeforePersistence() {
        UUID organizationId = UUID.randomUUID();
        UUID umsUserId = UUID.randomUUID();
        UUID actorUserId = UUID.randomUUID();

        doThrow(new ResponseStatusException(HttpStatus.BAD_REQUEST, "UMS user does not belong to organization"))
                .when(organizationAccessService)
                .assertUserBelongsToOrganization(organizationId, umsUserId);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> employeeService.create(
                        new CreateEmployeeRequest(organizationId, umsUserId, "EMP-001", null, null),
                        actorUserId,
                        false));

        assertEquals(400, exception.getStatusCode().value());
        verify(employeeRepository, never()).existsByOrganizationIdAndEmployeeCodeIgnoreCase(any(), any());
        verify(employeeRepository, never()).save(any(Employee.class));
    }

    @Test
    void getNeverReadsEmployeeOutsideRequestedOrganization() {
        UUID employeeId = UUID.randomUUID();
        UUID organizationId = UUID.randomUUID();
        UUID actorUserId = UUID.randomUUID();

        when(employeeRepository.findByIdAndOrganizationId(employeeId, organizationId)).thenReturn(Optional.empty());

        assertThrows(ResponseStatusException.class,
                () -> employeeService.get(employeeId, organizationId, actorUserId, false));

        verify(organizationAccessService).assertCanAccess(organizationId, actorUserId, false);
        verify(employeeRepository).findByIdAndOrganizationId(employeeId, organizationId);
    }

    @Test
    void createRejectsDuplicateEmployeeCodeWithinOrganization() {
        UUID organizationId = UUID.randomUUID();
        UUID actorUserId = UUID.randomUUID();

        when(employeeRepository.existsByOrganizationIdAndEmployeeCodeIgnoreCase(organizationId, "EMP-001"))
                .thenReturn(true);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> employeeService.create(
                        new CreateEmployeeRequest(
                                organizationId,
                                UUID.randomUUID(),
                                "EMP-001",
                                null,
                                null),
                        actorUserId,
                        false));

        assertEquals(409, exception.getStatusCode().value());
    }

    @Test
    void updatePublishesAuditForTenantScopedEmployee() {
        UUID employeeId = UUID.randomUUID();
        UUID organizationId = UUID.randomUUID();
        UUID actorUserId = UUID.randomUUID();

        Employee employee = Employee.builder()
                .id(employeeId)
                .organizationId(organizationId)
                .umsUserId(UUID.randomUUID())
                .employeeCode("EMP-001")
                .status(EmployeeStatus.ACTIVE)
                .build();

        when(employeeRepository.findByIdAndOrganizationId(employeeId, organizationId))
                .thenReturn(Optional.of(employee));
        when(employeeRepository.existsByOrganizationIdAndEmployeeCodeIgnoreCase(organizationId, "EMP-002"))
                .thenReturn(false);
        when(employeeRepository.save(employee)).thenReturn(employee);

        var response = employeeService.update(
                employeeId,
                new UpdateEmployeeRequest(organizationId, " EMP-002 ", null, null, EmployeeStatus.INACTIVE),
                actorUserId,
                false);

        verify(organizationAccessService).assertCanAccess(organizationId, actorUserId, false);
        verify(employeeAuditPublisher).publishUpdated(employee, actorUserId);
        assertEquals("EMP-002", response.employeeCode());
        assertEquals(EmployeeStatus.INACTIVE, response.status());
    }
}
