package com.ums.notification.listener;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

import com.ums.events.constants.RabbitMQConstants;
import com.ums.events.event.organization.OrganizationCreatedEvent;
import com.ums.notification.service.NotificationService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrganizationEventListener {

	private final NotificationService notificationService;

	@RabbitListener(queues = RabbitMQConstants.NOTIFICATION_ORGANIZATION_CREATED_QUEUE)
	public void consume(OrganizationCreatedEvent event) {

		log.info("Received OrganizationCreatedEvent: {}", event.getOrganizationName());

		notificationService.sendOrganizationCreatedEmail(event);
	}
}