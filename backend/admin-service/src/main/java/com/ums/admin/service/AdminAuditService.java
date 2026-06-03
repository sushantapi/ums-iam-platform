package com.ums.admin.service;

import java.util.List;

import com.ums.admin.dto.response.AuditLogResponse;

public interface AdminAuditService {

	List<AuditLogResponse> getAuditLogs();
}