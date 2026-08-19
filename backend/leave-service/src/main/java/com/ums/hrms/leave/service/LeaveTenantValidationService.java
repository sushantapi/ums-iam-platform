package com.ums.hrms.leave.service;

import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.ums.hrms.leave.client.EmployeeClient;
import com.ums.hrms.leave.client.EmployeeInternalResponse;

import feign.FeignException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class LeaveTenantValidationService {

    private final EmployeeClient employeeClient;

    public void validateEmployeeBelongsToOrganization(UUID employeeId, UUID organizationId) {
        EmployeeInternalResponse employee;
        try {
            employee = employeeClient.getEmployee(employeeId, organizationId);
        } catch (FeignException.NotFound ex) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Employee not found", ex);
        } catch (FeignException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Employee service unavailable", ex);
        }

        if (employee == null
                || !organizationId.equals(employee.organizationId())
                || !employeeId.equals(employee.id())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Employee not found");
        }

        if (!"ACTIVE".equalsIgnoreCase(employee.status())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Employee is not active");
        }
    }
}
