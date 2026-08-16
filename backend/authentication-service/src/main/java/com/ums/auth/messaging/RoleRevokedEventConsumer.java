package com.ums.auth.messaging;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import com.ums.events.constants.RabbitMQConstants;
import com.ums.events.event.role.RoleRevokedEvent;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class RoleRevokedEventConsumer {

	private final RoleRevocationEventHandler roleRevocationEventHandler;

	@RabbitListener(queues = RabbitMQConstants.AUTH_ROLE_REVOKED_QUEUE)
	public void consume(RoleRevokedEvent event) {
		boolean processed = roleRevocationEventHandler.process(event);
		if (processed) {
			log.info("Processed role revocation event eventId={} userId={} assignmentId={}",
					event.getEventId(), event.getUserId(), event.getAssignmentId());
		} else {
			log.info("Ignoring duplicate role revocation event eventId={} userId={}",
					event.getEventId(), event.getUserId());
		}
	}
}
