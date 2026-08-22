package com.ums.notification.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.ums.events.constants.RabbitMQConstants;

@Configuration
public class OrganizationInvitationTopologyConfig {

	@Bean
	public Queue notificationOrganizationInvitationQueue() {
		return new Queue(RabbitMQConstants.NOTIFICATION_ORGANIZATION_INVITATION_QUEUE, true);
	}

	@Bean
	public Binding organizationInvitationBinding(
			@Qualifier("notificationOrganizationInvitationQueue") Queue notificationOrganizationInvitationQueue,
			@Qualifier("organizationExchange") TopicExchange organizationExchange) {
		return BindingBuilder.bind(notificationOrganizationInvitationQueue)
				.to(organizationExchange)
				.with(RabbitMQConstants.ORGANIZATION_INVITATION_ROUTING_KEY);
	}
}
