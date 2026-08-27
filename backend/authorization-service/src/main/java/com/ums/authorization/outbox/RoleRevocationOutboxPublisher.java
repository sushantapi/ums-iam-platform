package com.ums.authorization.outbox;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.springframework.amqp.core.ReturnedMessage;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.ums.authorization.entity.RoleRevocationOutbox;
import com.ums.authorization.repository.RoleRevocationOutboxRepository;
import com.ums.events.constants.ExchangeConstants;
import com.ums.events.constants.RoutingKeyConstants;
import com.ums.events.event.role.RoleRevokedEvent;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class RoleRevocationOutboxPublisher {

	private static final int BATCH_SIZE = 25;
	private static final int MAX_ERROR_LENGTH = 1000;
	private static final long CONFIRM_TIMEOUT_SECONDS = 5L;

	private final RoleRevocationOutboxRepository outboxRepository;
	private final RabbitTemplate rabbitTemplate;

	@PostConstruct
	void configureMandatoryReturns() {
		rabbitTemplate.setMandatory(true);
	}

	@Scheduled(fixedDelayString = "${authorization.role-revocation-outbox.publish-delay-ms:2000}")
	@Transactional
	public void publishPending() {
		List<RoleRevocationOutbox> events = outboxRepository.findPublishable(
				List.of(RoleRevocationOutbox.STATUS_PENDING, RoleRevocationOutbox.STATUS_FAILED),
				PageRequest.of(0, BATCH_SIZE));

		for (RoleRevocationOutbox outbox : events) {
			try {
				publish(outbox);
				outbox.markPublished(LocalDateTime.now());
				log.info("Published role revocation event eventId={} userId={} assignmentId={}",
						outbox.getEventId(), outbox.getUserId(), outbox.getAssignmentId());
			} catch (Exception ex) {
				String message = ex.getMessage() == null ? ex.getClass().getSimpleName() : ex.getMessage();
				outbox.markFailed(truncate(message));
				log.warn("Role revocation event publish failed eventId={} attempt={}: {}",
						outbox.getEventId(), outbox.getAttempts(), message);
			}
		}

		if (!events.isEmpty()) {
			outboxRepository.saveAll(events);
		}
	}

	private void publish(RoleRevocationOutbox outbox) throws Exception {
		RoleRevokedEvent event = RoleRevokedEvent.builder()
				.eventId(outbox.getEventId())
				.assignmentId(outbox.getAssignmentId())
				.userId(outbox.getUserId())
				.roleId(outbox.getRoleId())
				.roleName(outbox.getRoleName())
				.scopeType(outbox.getScopeType())
				.scopeId(outbox.getScopeId())
				.revokedBy(outbox.getRevokedBy())
				.revokedAt(outbox.getRevokedAt())
				.build();

		CorrelationData correlationData = new CorrelationData(outbox.getEventId().toString());
		rabbitTemplate.convertAndSend(
				ExchangeConstants.AUTH_EXCHANGE,
				RoutingKeyConstants.ROLE_REVOKED,
				event,
				correlationData);

		CorrelationData.Confirm confirm = correlationData.getFuture()
				.get(CONFIRM_TIMEOUT_SECONDS, TimeUnit.SECONDS);

		if (!confirm.isAck()) {
			throw new IllegalStateException("RabbitMQ did not ACK role revocation event: " + confirm.getReason());
		}

		ReturnedMessage returned = correlationData.getReturned();
		if (returned != null) {
			throw new IllegalStateException("Role revocation event was unroutable: " + returned.getReplyText());
		}
	}

	private String truncate(String value) {
		return value.length() <= MAX_ERROR_LENGTH ? value : value.substring(0, MAX_ERROR_LENGTH);
	}
}
