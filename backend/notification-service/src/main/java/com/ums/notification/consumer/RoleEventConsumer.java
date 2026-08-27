package com.ums.notification.consumer;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import com.ums.events.constants.RabbitMQConstants;
import com.ums.events.event.role.RoleAssignedEvent;
import com.ums.events.event.role.RoleRevokedEvent;
import com.ums.notification.service.NotificationService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class RoleEventConsumer {

	private final NotificationService notificationService;

	@RabbitListener(queues = RabbitMQConstants.ROLE_ASSIGNED_QUEUE)
	public void consumeAssigned(RoleAssignedEvent event) {
		log.info("Received RoleAssignedEvent eventId={} userId={}", event.getEventId(), event.getUserId());
		notificationService.processRoleAssigned(event);
	}

	@RabbitListener(queues = RabbitMQConstants.NOTIFICATION_ROLE_REVOKED_QUEUE)
	public void consumeRevoked(RoleRevokedEvent event) {
		log.info("Received RoleRevokedEvent eventId={} userId={}", event.getEventId(), event.getUserId());
		notificationService.processRoleRevoked(event);
	}
}
