package com.ums.dto;

import java.time.LocalDateTime;

public record AuditEventResponse(
		Long id,
		String eventId,
		String eventType,
		String action,
		String actor,
		String target,
		String username,
		String userEmail,
		String serviceName,
		String entityType,
		String entityId,
		String ipAddress,
		String outcome,
		String details,
		LocalDateTime timestamp,
		LocalDateTime createdAt) {
}
