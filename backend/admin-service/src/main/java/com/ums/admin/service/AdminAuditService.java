package com.ums.admin.service;

import com.ums.admin.dto.response.AuditLogPageResponse;

public interface AdminAuditService {

	AuditLogPageResponse getAuditLogs(
			int page,
			int size,
			String actor,
			String target,
			String eventType,
			String serviceName);
}
