package com.ums.auth.service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.ums.auth.entity.PasswordRecoveryAuditOutboxEvent;
import com.ums.auth.entity.PasswordRecoveryAuditOutboxEvent.Status;
import com.ums.auth.repository.PasswordRecoveryAuditOutboxRepository;
import com.ums.events.event.AuditEvent;
import com.ums.events.publisher.AuditPublisher;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class PasswordRecoveryAuditOutboxService {

	private static final int MAX_BATCH_SIZE = 100;
	private static final long RETRY_DELAY_SECONDS = 30L;

	private final PasswordRecoveryAuditOutboxRepository repository;
	private final AuditPublisher auditPublisher;

	@Transactional(propagation = Propagation.MANDATORY)
	public void record(AuditEvent event) {
		repository.save(toOutboxEvent(event));
	}

	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public void recordInNewTransaction(AuditEvent event) {
		repository.save(toOutboxEvent(event));
	}

	@Transactional(readOnly = true)
	public List<UUID> findPendingIds(int limit) {
		int safeLimit = Math.max(1, Math.min(limit, MAX_BATCH_SIZE));
		return repository.findByStatusAndNextAttemptAtLessThanEqualOrderByCreatedAtAsc(
				Status.PENDING, Instant.now(), PageRequest.of(0, safeLimit))
				.stream()
				.map(PasswordRecoveryAuditOutboxEvent::getId)
				.toList();
	}

	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public void publishPending(UUID id) {
		Instant now = Instant.now();
		PasswordRecoveryAuditOutboxEvent outboxEvent = repository.findByIdForUpdate(id).orElse(null);
		if (outboxEvent == null || outboxEvent.getStatus() != Status.PENDING
				|| outboxEvent.getNextAttemptAt() == null || outboxEvent.getNextAttemptAt().isAfter(now)) {
			return;
		}

		try {
			auditPublisher.publish(toAuditEvent(outboxEvent));
			outboxEvent.setStatus(Status.PUBLISHED);
			outboxEvent.setPublishedAt(now);
			outboxEvent.setLastError(null);
		} catch (Exception ex) {
			outboxEvent.setAttempts(outboxEvent.getAttempts() + 1);
			outboxEvent.setNextAttemptAt(now.plusSeconds(RETRY_DELAY_SECONDS));
			outboxEvent.setLastError(ex.getClass().getSimpleName());
			log.warn("Password recovery audit outbox delivery failed for id={}, attempt={}, errorType={}",
					outboxEvent.getId(), outboxEvent.getAttempts(), ex.getClass().getSimpleName());
		}

		repository.save(outboxEvent);
	}

	private PasswordRecoveryAuditOutboxEvent toOutboxEvent(AuditEvent event) {
		return PasswordRecoveryAuditOutboxEvent.builder()
				.eventType(event.getEventType())
				.serviceName(event.getServiceName())
				.userId(event.getUserId())
				.userEmail(event.getUserEmail())
				.action(event.getAction())
				.entityType(event.getEntityType())
				.entityId(event.getEntityId())
				.details(event.getDetails())
				.ipAddress(event.getIpAddress())
				.eventTimestamp(event.getTimestamp() == null ? LocalDateTime.now() : event.getTimestamp())
				.status(Status.PENDING)
				.attempts(0)
				.nextAttemptAt(Instant.now())
				.build();
	}

	private AuditEvent toAuditEvent(PasswordRecoveryAuditOutboxEvent outboxEvent) {
		return AuditEvent.builder()
				.eventType(outboxEvent.getEventType())
				.serviceName(outboxEvent.getServiceName())
				.userId(outboxEvent.getUserId())
				.userEmail(outboxEvent.getUserEmail())
				.action(outboxEvent.getAction())
				.entityType(outboxEvent.getEntityType())
				.entityId(outboxEvent.getEntityId())
				.details(outboxEvent.getDetails())
				.ipAddress(outboxEvent.getIpAddress())
				.timestamp(outboxEvent.getEventTimestamp())
				.build();
	}
}
