package com.ums.notification.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.ums.events.constants.RabbitMQConstants;

@Configuration
public class PasswordResetFailureTopologyConfig {

	@Bean
	public TopicExchange notificationFailureExchange() {
		return new TopicExchange(RabbitMQConstants.NOTIFICATION_FAILURE_EXCHANGE, true, false);
	}

	@Bean
	public Queue passwordResetDlq() {
		return new Queue(RabbitMQConstants.NOTIFICATION_PASSWORD_RESET_DLQ, true);
	}

	@Bean
	public Binding passwordResetDlqBinding(Queue passwordResetDlq,
			TopicExchange notificationFailureExchange) {
		return BindingBuilder.bind(passwordResetDlq)
				.to(notificationFailureExchange)
				.with(RabbitMQConstants.NOTIFICATION_PASSWORD_RESET_FAILURE_ROUTING_KEY);
	}
}
