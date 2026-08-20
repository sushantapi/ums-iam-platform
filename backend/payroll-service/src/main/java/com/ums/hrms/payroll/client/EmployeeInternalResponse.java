package com.ums.hrms.payroll.client;

import java.util.UUID;

public record EmployeeInternalResponse(
        UUID id,
        UUID organizationId,
        String status) {
}
