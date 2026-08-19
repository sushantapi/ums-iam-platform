package com.ums.hrms.leave.client;
import java.util.UUID;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;
import com.ums.hrms.leave.config.InternalServiceFeignConfig;
@FeignClient(name="employee-service",configuration=InternalServiceFeignConfig.class)
public interface EmployeeClient {
 @GetMapping("/api/v1/internal/hrms/employees/{employeeId}") EmployeeInternalResponse getEmployee(@PathVariable UUID employeeId,@RequestParam UUID organizationId);
}
