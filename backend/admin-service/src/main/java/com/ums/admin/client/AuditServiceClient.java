package com.ums.admin.client;

import java.util.List;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

import com.ums.admin.dto.response.AuditLogResponse;

@FeignClient(name = "audit-service", contextId = "auditClient")
public interface AuditServiceClient {

	@GetMapping("/internal/audit/logs")
	List<AuditLogResponse> getAuditLogs();
}