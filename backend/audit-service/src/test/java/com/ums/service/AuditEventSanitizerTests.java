package com.ums.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

import com.ums.entity.AuditLog;
import com.ums.events.event.AuditEvent;

class AuditEventSanitizerTests {

	private final AuditEventSanitizer sanitizer = new AuditEventSanitizer();

	@Test
	void redactsCommonSecretsAndNormalizesMultilineValues() {
		AuditEvent event = AuditEvent.builder()
				.eventType("auth.login.failed")
				.serviceName("authentication-service")
				.details("{\"password\":\"hunter2\",\"token\":\"abc\"} otp=123456")
				.action("LOGIN\nFAILED")
				.build();

		AuditLog sanitized = sanitizer.sanitize(event);

		assertThat(sanitized.getDetails())
				.doesNotContain("hunter2", "abc", "123456")
				.contains("[REDACTED]");
		assertThat(sanitized.getAction()).isEqualTo("LOGIN FAILED");
		assertThat(sanitized.getCreatedAt()).isNotNull();
	}

	@Test
	void rejectsEventsWithoutRequiredProvenance() {
		assertThatThrownBy(() -> sanitizer.sanitize(AuditEvent.builder().eventType("auth.login").build()))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("serviceName");
	}
}
