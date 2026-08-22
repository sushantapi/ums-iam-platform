package com.ums.notification.consumer;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import com.ums.events.constants.RabbitMQConstants;
import com.ums.events.event.organization.OrganizationInviteEvent;
import com.ums.notification.service.EmailService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class OrganizationInvitationEventConsumer {

	private final EmailService emailService;

	@RabbitListener(queues = RabbitMQConstants.NOTIFICATION_ORGANIZATION_INVITATION_QUEUE)
	public void consume(OrganizationInviteEvent event) {
		try {
			emailService.processOrganizationInvitation(event);
		} catch (RuntimeException ex) {
			log.warn("Organization invitation email delivery failed failureType={}", ex.getClass().getName());
		}
	}
}
