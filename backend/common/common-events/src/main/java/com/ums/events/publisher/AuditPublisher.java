package com.ums.events.publisher;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import com.ums.events.constants.RabbitMQConstants;
import com.ums.events.event.AuditEvent;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class AuditPublisher {

	private final RabbitTemplate rabbitTemplate;

	public void publish(AuditEvent event) {

		rabbitTemplate.convertAndSend(RabbitMQConstants.AUDIT_EXCHANGE, RabbitMQConstants.AUDIT_ROUTING_KEY, event);
	}
}