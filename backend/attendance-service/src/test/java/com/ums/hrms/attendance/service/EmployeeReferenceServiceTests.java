package com.ums.hrms.attendance.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import com.ums.hrms.attendance.client.EmployeeServiceClient;

@ExtendWith(MockitoExtension.class)
class EmployeeReferenceServiceTests {

    @Mock EmployeeServiceClient employeeServiceClient;
    @InjectMocks EmployeeReferenceService employeeReferenceService;

    @Test
    void acceptsActiveEmployeeInSameOrganization() {
        UUID organizationId = UUID.randomUUID();
        UUID employeeId = UUID.randomUUID();
        when(employeeServiceClient.getEmployee(employeeId, organizationId))
                .thenReturn(new EmployeeServiceClient.EmployeeSummary(employeeId, organizationId, "ACTIVE"));

        assertDoesNotThrow(() -> employeeReferenceService.assertActiveEmployee(organizationId, employeeId));
    }

    @Test
    void rejectsInactiveEmployee() {
        UUID organizationId = UUID.randomUUID();
        UUID employeeId = UUID.randomUUID();
        when(employeeServiceClient.getEmployee(employeeId, organizationId))
                .thenReturn(new EmployeeServiceClient.EmployeeSummary(employeeId, organizationId, "TERMINATED"));

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> employeeReferenceService.assertActiveEmployee(organizationId, employeeId));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
    }
}
