package com.ums.notification.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.ums.events.constants.RabbitMQConstants;
import com.ums.events.constants.RoutingKeyConstants;

@Configuration
public class RabbitConsumerConfig {

	@Bean
	public MessageConverter messageConverter() {
		return new Jackson2JsonMessageConverter();
	}

	@Bean
	public Queue notificationUserRegisteredQueue() {
		return new Queue(RabbitMQConstants.NOTIFICATION_USER_REGISTERED_QUEUE);
	}

	@Bean
	public TopicExchange userExchange() {
		return new TopicExchange(RabbitMQConstants.USER_EXCHANGE);
	}

	@Bean
	public Binding notificationUserRegisteredBinding(Queue notificationUserRegisteredQueue,
			TopicExchange userExchange) {
		return BindingBuilder.bind(notificationUserRegisteredQueue).to(userExchange)
				.with(RabbitMQConstants.USER_REGISTERED_ROUTING_KEY);
	}

	@Bean
	public TopicExchange organizationExchange() {
		return new TopicExchange(RabbitMQConstants.ORGANIZATION_EXCHANGE);
	}

	@Bean
	public Queue notificationOrganizationCreatedQueue() {
		return new Queue(RabbitMQConstants.NOTIFICATION_ORGANIZATION_CREATED_QUEUE, true);
	}

	@Bean
	public Binding organizationCreatedBinding(Queue notificationOrganizationCreatedQueue,
			TopicExchange organizationExchange) {
		return BindingBuilder.bind(notificationOrganizationCreatedQueue).to(organizationExchange)
				.with(RabbitMQConstants.ORGANIZATION_CREATED_ROUTING_KEY);
	}

	@Bean
	public Queue roleAssignedQueue() {
		return new Queue(RabbitMQConstants.ROLE_ASSIGNED_QUEUE, true);
	}

	@Bean
	public Queue notificationRoleRevokedQueue() {
		return new Queue(RabbitMQConstants.NOTIFICATION_ROLE_REVOKED_QUEUE, true);
	}

	@Bean
	public TopicExchange authExchange() {
		return new TopicExchange(RabbitMQConstants.AUTH_EXCHANGE);
	}

	@Bean
	public Binding roleAssignedBinding(Queue roleAssignedQueue, TopicExchange authExchange) {
		return BindingBuilder.bind(roleAssignedQueue).to(authExchange).with(RoutingKeyConstants.ROLE_ASSIGNED);
	}

	@Bean
	public Binding notificationRoleRevokedBinding(Queue notificationRoleRevokedQueue, TopicExchange authExchange) {
		return BindingBuilder.bind(notificationRoleRevokedQueue).to(authExchange)
				.with(RoutingKeyConstants.ROLE_REVOKED);
	}

	@Bean
	public Queue emailVerificationQueue() {
		return new Queue(RabbitMQConstants.NOTIFICATION_EMAIL_VERIFICATION_QUEUE, true);
	}

	@Bean
	public Queue passwordResetQueue() {
		return new Queue(RabbitMQConstants.NOTIFICATION_PASSWORD_RESET_QUEUE, true);
	}

	@Bean
	public Queue mfaOtpQueue() {
		return new Queue(RabbitMQConstants.NOTIFICATION_MFA_OTP_QUEUE, true);
	}

	@Bean
	public Binding emailVerificationBinding(Queue emailVerificationQueue, TopicExchange authExchange) {
		return BindingBuilder.bind(emailVerificationQueue).to(authExchange)
				.with(RabbitMQConstants.EMAIL_VERIFICATION_ROUTING_KEY);
	}

	@Bean
	public Binding passwordResetBinding(Queue passwordResetQueue, TopicExchange authExchange) {
		return BindingBuilder.bind(passwordResetQueue).to(authExchange)
				.with(RabbitMQConstants.PASSWORD_RESET_ROUTING_KEY);
	}

	@Bean
	public Binding mfaOtpBinding(Queue mfaOtpQueue, TopicExchange authExchange) {
		return BindingBuilder.bind(mfaOtpQueue).to(authExchange)
				.with(RabbitMQConstants.MFA_OTP_ROUTING_KEY);
	}
}
