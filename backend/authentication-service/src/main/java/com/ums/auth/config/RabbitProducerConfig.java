package com.ums.auth.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.ums.events.constants.RabbitMQConstants;

@Configuration
public class RabbitProducerConfig {

	@Bean
	public TopicExchange userExchange() {
		return new TopicExchange(RabbitMQConstants.USER_EXCHANGE);
	}

	@Bean
	public TopicExchange authExchange() {
		return new TopicExchange(RabbitMQConstants.AUTH_EXCHANGE);
	}

	@Bean
	public TopicExchange organizationExchange() {
		return new TopicExchange(RabbitMQConstants.ORGANIZATION_EXCHANGE);
	}

	@Bean
	public Queue authRoleRevokedQueue() {
		return new Queue(RabbitMQConstants.AUTH_ROLE_REVOKED_QUEUE, true);
	}

	@Bean
	public Queue authOrganizationMfaRequiredQueue() {
		return new Queue(RabbitMQConstants.AUTH_ORGANIZATION_MFA_REQUIRED_QUEUE, true);
	}

	@Bean
	public Binding authRoleRevokedBinding(Queue authRoleRevokedQueue, TopicExchange authExchange) {
		return BindingBuilder.bind(authRoleRevokedQueue)
				.to(authExchange)
				.with(RabbitMQConstants.ROLE_REVOKED_ROUTING_KEY);
	}

	@Bean
	public Binding authOrganizationMfaRequiredBinding(
			Queue authOrganizationMfaRequiredQueue,
			TopicExchange organizationExchange) {
		return BindingBuilder.bind(authOrganizationMfaRequiredQueue)
				.to(organizationExchange)
				.with(RabbitMQConstants.ORGANIZATION_MFA_REQUIRED_ROUTING_KEY);
	}
}