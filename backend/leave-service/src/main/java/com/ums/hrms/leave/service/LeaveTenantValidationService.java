package com.ums.hrms.leave.service;

import java.util.UUID;

import org.springframework.stereotype.Service;

import com.ums.hrms.leave.client.EmployeeClient;
import com.ums.hrms.leave.client.EmployeeInternalResponse;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class LeaveTenantValidationService {

    private final EmployeeClient employeeClient;

    public void validateEmployeeBelongsToOrganization(UUID employeeId, UUID organizationId) {
        EmployeeInternalResponse employee = employeeClient.getEmployee(employeeId, organizationId);
        if (!organizationId.equals(employee.organizationId())) {
            throw new IllegalArgumentException("Employee does not belong to organization");
        }
    }
}
