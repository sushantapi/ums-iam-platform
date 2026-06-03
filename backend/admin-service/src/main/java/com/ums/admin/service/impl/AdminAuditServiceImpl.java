package com.ums.admin.service.impl;

import java.util.List;

import org.springframework.stereotype.Service;

import com.ums.admin.client.AuditServiceClient;
import com.ums.admin.dto.response.AuditLogResponse;
import com.ums.admin.service.AdminAuditService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AdminAuditServiceImpl implements AdminAuditService {

	private final AuditServiceClient auditServiceClient;

	@Override
	public List<AuditLogResponse> getAuditLogs() {

		return auditServiceClient.getAuditLogs();
	}
}