package com.ums.org.publisher;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.ums.events.event.organization.OrganizationMfaRequiredEvent;
import com.ums.org.entity.OrganizationSecurityEventOutbox;
import com.ums.org.entity.OrganizationSecurityEventOutbox.Status;
import com.ums.org.repositoty.OrganizationSecurityEventOutboxRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class OrganizationSecurityEventOutboxDispatcher {

	private static final int BATCH_SIZE = 50;

	private final OrganizationSecurityEventOutboxRepository outboxRepository;
	private final OrganizationEventPublisher organizationEventPublisher;

	@Scheduled(
			initialDelayString = "${organization.security-event-outbox.initial-delay-ms:2000}",
			fixedDelayString = "${organization.security-event-outbox.dispatch-delay-ms:2000}")
	@Transactional
	public void dispatchPending() {
		LocalDateTime now = LocalDateTime.now();
		List<OrganizationSecurityEventOutbox> rows = outboxRepository.findReadyForDispatch(
				Status.PENDING,
				now,
				PageRequest.of(0, BATCH_SIZE));

		if (rows.isEmpty()) {
			return;
		}

		for (OrganizationSecurityEventOutbox row : rows) {
			try {
				organizationEventPublisher.publishOrganizationMfaRequired(toEvent(row));
				row.setStatus(Status.PUBLISHED);
				row.setPublishedAt(now);
				row.setNextAttemptAt(null);
				row.setLastErrorType(null);
			} catch (RuntimeException ex) {
				int attempts = row.getAttempts() + 1;
				row.setAttempts(attempts);
				row.setLastErrorType(ex.getClass().getName());
				row.setNextAttemptAt(now.plusSeconds(backoffSeconds(attempts)));
				log.warn(
						"Organization security-event outbox dispatch failed eventId={} organizationId={} attempts={} failureType={}",
						row.getEventId(),
						row.getOrganizationId(),
						attempts,
						ex.getClass().getName());
			}
		}

		outboxRepository.saveAll(rows);
	}

	private OrganizationMfaRequiredEvent toEvent(OrganizationSecurityEventOutbox row) {
		return OrganizationMfaRequiredEvent.builder()
				.eventId(row.getEventId())
				.organizationId(row.getOrganizationId())
				.updatedBy(row.getUpdatedBy())
				.occurredAt(row.getOccurredAt())
				.build();
	}

	private long backoffSeconds(int attempts) {
		return Math.min(300L, Math.max(5L, attempts * 5L));
	}
}
