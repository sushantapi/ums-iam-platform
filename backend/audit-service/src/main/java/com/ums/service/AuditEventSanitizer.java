package com.ums.service;

import java.time.LocalDateTime;
import java.util.regex.Pattern;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.ums.entity.AuditLog;
import com.ums.events.event.AuditEvent;

@Component
public class AuditEventSanitizer {

	private static final int MAX_STANDARD_FIELD_LENGTH = 255;
	private static final int MAX_DETAILS_LENGTH = 5000;
	private static final Pattern SENSITIVE_JSON_VALUE = Pattern.compile(
			"(?i)(\"?(?:password|passwd|pwd|token|access_token|refresh_token|secret|client_secret|authorization|otp|mfa_code)\"?\\s*[:=]\\s*\")([^\"]*)(\")");
	private static final Pattern SENSITIVE_TEXT_VALUE = Pattern.compile(
			"(?i)\\b(password|passwd|pwd|token|access_token|refresh_token|secret|client_secret|authorization|otp|mfa_code)\\b\\s*[:=]\\s*([^,;\\s}]+)");

	public AuditLog sanitize(AuditEvent event) {
		if (event == null || !StringUtils.hasText(event.getEventType())
				|| !StringUtils.hasText(event.getServiceName())) {
			throw new IllegalArgumentException("Audit events require eventType and serviceName");
		}

		return AuditLog.builder().eventType(clean(event.getEventType(), MAX_STANDARD_FIELD_LENGTH))
				.serviceName(clean(event.getServiceName(), MAX_STANDARD_FIELD_LENGTH))
				.userId(clean(event.getUserId(), MAX_STANDARD_FIELD_LENGTH))
				.userEmail(clean(event.getUserEmail(), MAX_STANDARD_FIELD_LENGTH))
				.action(clean(event.getAction(), MAX_STANDARD_FIELD_LENGTH))
				.entityType(clean(event.getEntityType(), MAX_STANDARD_FIELD_LENGTH))
				.entityId(clean(event.getEntityId(), MAX_STANDARD_FIELD_LENGTH))
				.details(redactDetails(event.getDetails()))
				.ipAddress(clean(event.getIpAddress(), MAX_STANDARD_FIELD_LENGTH))
				.createdAt(event.getTimestamp() != null ? event.getTimestamp() : LocalDateTime.now()).build();
	}

	private String redactDetails(String details) {
		String value = clean(details, MAX_DETAILS_LENGTH);
		if (value == null) {
			return null;
		}

		value = SENSITIVE_JSON_VALUE.matcher(value).replaceAll("$1[REDACTED]$3");
		return SENSITIVE_TEXT_VALUE.matcher(value).replaceAll("$1=[REDACTED]");
	}

	private String clean(String value, int maxLength) {
		if (!StringUtils.hasText(value)) {
			return null;
		}

		String cleaned = value.trim().replaceAll("[\\r\\n\\t]+", " ");
		return cleaned.length() <= maxLength ? cleaned : cleaned.substring(0, maxLength);
	}
}
