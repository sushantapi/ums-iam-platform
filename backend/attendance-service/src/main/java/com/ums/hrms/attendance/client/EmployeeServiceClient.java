package com.ums.hrms.attendance.client;

import java.util.UUID;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import com.ums.hrms.attendance.config.InternalServiceFeignConfig;

@FeignClient(
        name = "employee-service",
        contextId = "attendanceEmployeeReferenceClient",
        configuration = InternalServiceFeignConfig.class)
public interface EmployeeServiceClient {

    @GetMapping("/api/v1/internal/hrms/employees/{employeeId}")
    EmployeeSummary getEmployee(
            @PathVariable UUID employeeId,
            @RequestParam UUID organizationId);

    record EmployeeSummary(UUID id, UUID organizationId, String status) {
    }
}
