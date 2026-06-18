package com.ums.authorization.publisher;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import com.ums.events.constants.ExchangeConstants;
import com.ums.events.constants.RoutingKeyConstants;
import com.ums.events.event.role.RoleAssignedEvent;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class RoleEventPublisher {

	private final RabbitTemplate rabbitTemplate;

	public void publishRoleAssigned(RoleAssignedEvent event) {

		rabbitTemplate.convertAndSend(ExchangeConstants.AUTH_EXCHANGE, RoutingKeyConstants.ROLE_ASSIGNED, event);
	}
}