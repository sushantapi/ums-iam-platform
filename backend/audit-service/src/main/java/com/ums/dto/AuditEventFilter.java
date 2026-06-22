package com.ums.dto;

import java.time.LocalDate;

public record AuditEventFilter(
		String actor,
		String target,
		String organizationId,
		String eventType,
		String serviceName,
		String outcome,
		LocalDate from,
		LocalDate to) {
}
