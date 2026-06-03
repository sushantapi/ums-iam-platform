package com.ums.org.publisher;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import com.ums.events.constants.RabbitMQConstants;
import com.ums.events.event.organization.OrganizationCreatedEvent;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
@RequiredArgsConstructor
public class OrganizationEventPublisher {

	private final RabbitTemplate rabbitTemplate;

	public void publishOrganizationCreated(OrganizationCreatedEvent event) {

		log.info("Publishing OrganizationCreatedEvent: {}", event);

		rabbitTemplate.convertAndSend(RabbitMQConstants.ORGANIZATION_EXCHANGE,
				RabbitMQConstants.ORGANIZATION_CREATED_ROUTING_KEY, event);
	}
}