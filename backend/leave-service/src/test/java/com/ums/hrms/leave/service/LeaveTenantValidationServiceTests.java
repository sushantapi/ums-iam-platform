package com.ums.hrms.leave.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.ums.hrms.leave.client.EmployeeClient;
import com.ums.hrms.leave.client.EmployeeInternalResponse;

class LeaveTenantValidationServiceTests {

    @Test
    void acceptsActiveEmployeeFromSameOrganization() {
        UUID organizationId = UUID.randomUUID();
        UUID employeeId = UUID.randomUUID();
        EmployeeClient client = mock(EmployeeClient.class);
        when(client.getEmployee(employeeId, organizationId))
                .thenReturn(new EmployeeInternalResponse(employeeId, organizationId, "ACTIVE"));

        new LeaveTenantValidationService(client)
                .validateEmployeeBelongsToOrganization(employeeId, organizationId);
    }

    @Test
    void rejectsMissingEmployeeReference() {
        UUID organizationId = UUID.randomUUID();
        UUID employeeId = UUID.randomUUID();
        EmployeeClient client = mock(EmployeeClient.class);
        when(client.getEmployee(employeeId, organizationId)).thenReturn(null);

        assertThatThrownBy(() -> new LeaveTenantValidationService(client)
                .validateEmployeeBelongsToOrganization(employeeId, organizationId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Employee does not belong to organization");
    }

    @Test
    void rejectsWrongTenantEmployee() {
        UUID organizationId = UUID.randomUUID();
        UUID otherOrganizationId = UUID.randomUUID();
        UUID employeeId = UUID.randomUUID();
        EmployeeClient client = mock(EmployeeClient.class);
        when(client.getEmployee(employeeId, organizationId))
                .thenReturn(new EmployeeInternalResponse(employeeId, otherOrganizationId, "ACTIVE"));

        assertThatThrownBy(() -> new LeaveTenantValidationService(client)
                .validateEmployeeBelongsToOrganization(employeeId, organizationId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Employee does not belong to organization");
    }

    @Test
    void rejectsInactiveEmployee() {
        UUID organizationId = UUID.randomUUID();
        UUID employeeId = UUID.randomUUID();
        EmployeeClient client = mock(EmployeeClient.class);
        when(client.getEmployee(employeeId, organizationId))
                .thenReturn(new EmployeeInternalResponse(employeeId, organizationId, "INACTIVE"));

        assertThatThrownBy(() -> new LeaveTenantValidationService(client)
                .validateEmployeeBelongsToOrganization(employeeId, organizationId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Employee is not active");
    }
}
