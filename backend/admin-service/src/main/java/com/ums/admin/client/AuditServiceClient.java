package com.ums.admin.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.ums.admin.dto.response.AuditLogPageResponse;

@FeignClient(name = "audit-service", contextId = "auditClient")
public interface AuditServiceClient {

	@GetMapping("/api/v1/internal/audit/events")
	AuditLogPageResponse getAuditLogs(
			@RequestParam int page,
			@RequestParam int size,
			@RequestParam(required = false) String actor,
			@RequestParam(required = false) String target,
			@RequestParam(required = false) String eventType,
			@RequestParam(required = false) String serviceName);
}
