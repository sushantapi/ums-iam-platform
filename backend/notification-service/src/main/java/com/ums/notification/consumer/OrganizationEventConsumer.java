
package com.ums.notification.consumer;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import com.ums.events.constants.RabbitMQConstants;
import com.ums.events.event.organization.OrganizationCreatedEvent;
import com.ums.notification.service.NotificationService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class OrganizationEventConsumer {

	private final NotificationService notificationService;

	@RabbitListener(queues = RabbitMQConstants.NOTIFICATION_ORGANIZATION_CREATED_QUEUE)
	public void consume(OrganizationCreatedEvent event) {

		log.info("Received OrganizationCreatedEvent for organizationId={}", event.getOrganizationId());

		notificationService.sendOrganizationCreatedEmail(event);
	}
}
