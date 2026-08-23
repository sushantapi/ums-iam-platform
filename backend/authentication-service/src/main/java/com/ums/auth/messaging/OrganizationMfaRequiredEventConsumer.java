package com.ums.auth.messaging;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import com.ums.events.constants.RabbitMQConstants;
import com.ums.events.event.organization.OrganizationMfaRequiredEvent;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class OrganizationMfaRequiredEventConsumer {

	private final OrganizationMfaRequiredEventHandler handler;

	@RabbitListener(queues = RabbitMQConstants.AUTH_ORGANIZATION_MFA_REQUIRED_QUEUE)
	public void consume(OrganizationMfaRequiredEvent event) {
		boolean processed = handler.process(event);
		if (processed) {
			log.info("Processed organization MFA-required event eventId={} organizationId={}",
					event.getEventId(), event.getOrganizationId());
		} else {
			log.info("Ignoring duplicate organization MFA-required event eventId={} organizationId={}",
					event.getEventId(), event.getOrganizationId());
		}
	}
}
