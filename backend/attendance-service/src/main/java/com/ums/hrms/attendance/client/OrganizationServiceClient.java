package com.ums.hrms.attendance.client;

import java.util.UUID;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;

import com.ums.hrms.attendance.config.InternalServiceFeignConfig;

@FeignClient(
        name = "organization-service",
        contextId = "attendanceOrganizationAccessClient",
        configuration = InternalServiceFeignConfig.class)
public interface OrganizationServiceClient {

    @GetMapping("/api/v1/internal/organizations/{organizationId}")
    void assertAccessible(
            @PathVariable UUID organizationId,
            @RequestHeader("X-Actor-User-Id") UUID actorUserId,
            @RequestHeader("X-Actor-Super-Admin") boolean superAdmin);
}
