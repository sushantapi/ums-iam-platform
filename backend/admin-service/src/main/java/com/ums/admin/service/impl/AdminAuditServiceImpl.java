package com.ums.admin.service.impl;

import org.springframework.stereotype.Service;

import com.ums.admin.client.AuditServiceClient;
import com.ums.admin.dto.response.AuditLogPageResponse;
import com.ums.admin.service.AdminAuditService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AdminAuditServiceImpl implements AdminAuditService {

	private final AuditServiceClient auditServiceClient;

	@Override
	public AuditLogPageResponse getAuditLogs(int page, int size, String actor, String target, String eventType,
			String serviceName) {
		return auditServiceClient.getAuditLogs(page, size, actor, target, eventType, serviceName);
	}
}
