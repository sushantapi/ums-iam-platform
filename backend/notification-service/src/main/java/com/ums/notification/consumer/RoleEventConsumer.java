package com.ums.notification.consumer;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import com.ums.events.constants.RabbitMQConstants;
import com.ums.events.event.role.RoleAssignedEvent;
import com.ums.notification.service.NotificationService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class RoleEventConsumer {

	private final NotificationService notificationService;

	@RabbitListener(queues = RabbitMQConstants.ROLE_ASSIGNED_QUEUE)
	public void consume(RoleAssignedEvent event) {

		log.info("Received RoleAssignedEvent for userId={}", event.getUserId());
		notificationService.processRoleAssigned(event);
	}
}
