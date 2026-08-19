package com.ums.hrms.leave.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import com.ums.hrms.leave.client.EmployeeClient;
import com.ums.hrms.leave.client.EmployeeInternalResponse;

import feign.FeignException;
import feign.Request;
import feign.RequestTemplate;

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
    void mapsEmployeeNotFoundToNotFound() {
        UUID organizationId = UUID.randomUUID();
        UUID employeeId = UUID.randomUUID();
        EmployeeClient client = mock(EmployeeClient.class);
        when(client.getEmployee(employeeId, organizationId))
                .thenThrow(new FeignException.NotFound("not found", request(employeeId), null, Map.of()));

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> new LeaveTenantValidationService(client)
                        .validateEmployeeBelongsToOrganization(employeeId, organizationId));

        assertEquals(404, ex.getStatusCode().value());
        assertEquals("Employee not found", ex.getReason());
    }

    @Test
    void mapsEmployeeServiceFailureToBadGateway() {
        UUID organizationId = UUID.randomUUID();
        UUID employeeId = UUID.randomUUID();
        EmployeeClient client = mock(EmployeeClient.class);
        when(client.getEmployee(employeeId, organizationId))
                .thenThrow(new FeignException.ServiceUnavailable("unavailable", request(employeeId), null, Map.of()));

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> new LeaveTenantValidationService(client)
                        .validateEmployeeBelongsToOrganization(employeeId, organizationId));

        assertEquals(502, ex.getStatusCode().value());
        assertEquals("Employee service unavailable", ex.getReason());
    }

    @Test
    void rejectsMissingEmployeeReferenceAsNotFound() {
        UUID organizationId = UUID.randomUUID();
        UUID employeeId = UUID.randomUUID();
        EmployeeClient client = mock(EmployeeClient.class);
        when(client.getEmployee(employeeId, organizationId)).thenReturn(null);

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> new LeaveTenantValidationService(client)
                        .validateEmployeeBelongsToOrganization(employeeId, organizationId));

        assertEquals(404, ex.getStatusCode().value());
        assertEquals("Employee not found", ex.getReason());
    }

    @Test
    void rejectsWrongTenantEmployeeAsNotFound() {
        UUID organizationId = UUID.randomUUID();
        UUID otherOrganizationId = UUID.randomUUID();
        UUID employeeId = UUID.randomUUID();
        EmployeeClient client = mock(EmployeeClient.class);
        when(client.getEmployee(employeeId, organizationId))
                .thenReturn(new EmployeeInternalResponse(employeeId, otherOrganizationId, "ACTIVE"));

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> new LeaveTenantValidationService(client)
                        .validateEmployeeBelongsToOrganization(employeeId, organizationId));

        assertEquals(404, ex.getStatusCode().value());
        assertEquals("Employee not found", ex.getReason());
    }

    @Test
    void rejectsInactiveEmployeeAsConflict() {
        UUID organizationId = UUID.randomUUID();
        UUID employeeId = UUID.randomUUID();
        EmployeeClient client = mock(EmployeeClient.class);
        when(client.getEmployee(employeeId, organizationId))
                .thenReturn(new EmployeeInternalResponse(employeeId, organizationId, "INACTIVE"));

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> new LeaveTenantValidationService(client)
                        .validateEmployeeBelongsToOrganization(employeeId, organizationId));

        assertEquals(409, ex.getStatusCode().value());
        assertEquals("Employee is not active", ex.getReason());
    }

    private Request request(UUID employeeId) {
        return Request.create(
                Request.HttpMethod.GET,
                "/api/v1/internal/hrms/employees/" + employeeId,
                Map.of(),
                null,
                StandardCharsets.UTF_8,
                new RequestTemplate());
    }
}
