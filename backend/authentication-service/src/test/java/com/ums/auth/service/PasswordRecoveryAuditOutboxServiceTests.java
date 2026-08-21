package com.ums.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.ums.auth.entity.PasswordRecoveryAuditOutboxEvent;
import com.ums.auth.entity.PasswordRecoveryAuditOutboxEvent.Status;
import com.ums.auth.repository.PasswordRecoveryAuditOutboxRepository;
import com.ums.events.event.AuditEvent;
import com.ums.events.publisher.AuditPublisher;

@ExtendWith(MockitoExtension.class)
class PasswordRecoveryAuditOutboxServiceTests {

	@Mock private PasswordRecoveryAuditOutboxRepository repository;
	@Mock private AuditPublisher auditPublisher;

	@InjectMocks
	private PasswordRecoveryAuditOutboxService service;

	@Test
	void recordStoresPendingAuditEvidenceBeforeBrokerDelivery() {
		service.record(sampleAuditEvent());

		ArgumentCaptor<PasswordRecoveryAuditOutboxEvent> captor =
				ArgumentCaptor.forClass(PasswordRecoveryAuditOutboxEvent.class);
		verify(repository).save(captor.capture());
		PasswordRecoveryAuditOutboxEvent stored = captor.getValue();
		assertThat(stored.getStatus()).isEqualTo(Status.PENDING);
		assertThat(stored.getAttempts()).isZero();
		assertThat(stored.getNextAttemptAt()).isNotNull();
		assertThat(stored.getEventType()).isEqualTo("auth.password_reset.completed");
		assertThat(stored.getDetails()).doesNotContain("token=");
	}

	@Test
	void publishSuccessMarksOutboxEventPublished() {
		UUID id = UUID.randomUUID();
		PasswordRecoveryAuditOutboxEvent pending = pendingEvent(id);
		when(repository.findByIdForUpdate(id)).thenReturn(Optional.of(pending));

		service.publishPending(id);

		ArgumentCaptor<AuditEvent> auditCaptor = ArgumentCaptor.forClass(AuditEvent.class);
		verify(auditPublisher).publish(auditCaptor.capture());
		assertThat(auditCaptor.getValue().getEventType()).isEqualTo("auth.password_reset.completed");
		assertThat(pending.getStatus()).isEqualTo(Status.PUBLISHED);
		assertThat(pending.getPublishedAt()).isNotNull();
		assertThat(pending.getLastError()).isNull();
		verify(repository).save(pending);
	}

	@Test
	void publishFailureKeepsDurablePendingEventForRetry() {
		UUID id = UUID.randomUUID();
		PasswordRecoveryAuditOutboxEvent pending = pendingEvent(id);
		when(repository.findByIdForUpdate(id)).thenReturn(Optional.of(pending));
		doThrow(new IllegalStateException("broker unavailable")).when(auditPublisher).publish(any(AuditEvent.class));
		Instant before = Instant.now();

		service.publishPending(id);

		assertThat(pending.getStatus()).isEqualTo(Status.PENDING);
		assertThat(pending.getAttempts()).isEqualTo(1);
		assertThat(pending.getNextAttemptAt()).isAfter(before);
		assertThat(pending.getLastError()).isEqualTo("IllegalStateException");
		assertThat(pending.getLastError()).doesNotContain("broker unavailable");
		verify(repository).save(pending);
	}

	private AuditEvent sampleAuditEvent() {
		return AuditEvent.builder()
				.eventType("auth.password_reset.completed")
				.serviceName("authentication-service")
				.userId(UUID.randomUUID().toString())
				.userEmail("user@example.com")
				.action("PASSWORD_RESET")
				.entityType("USER")
				.entityId(UUID.randomUUID().toString())
				.details("Password reset completed and active sessions revoked")
				.ipAddress("127.0.0.1")
				.timestamp(LocalDateTime.now())
				.build();
	}

	private PasswordRecoveryAuditOutboxEvent pendingEvent(UUID id) {
		return PasswordRecoveryAuditOutboxEvent.builder()
				.id(id)
				.eventType("auth.password_reset.completed")
				.serviceName("authentication-service")
				.userEmail("user@example.com")
				.action("PASSWORD_RESET")
				.entityType("USER")
				.entityId(UUID.randomUUID().toString())
				.details("Password reset completed and active sessions revoked")
				.ipAddress("127.0.0.1")
				.eventTimestamp(LocalDateTime.now())
				.status(Status.PENDING)
				.attempts(0)
				.nextAttemptAt(Instant.now().minusSeconds(1))
				.build();
	}
}
