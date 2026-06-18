package com.ums.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.ums.events.constants.RabbitMQConstants;

@Configuration
public class AuditRabbitConfig {

	@Bean
	public Queue auditQueue() {
		return new Queue(RabbitMQConstants.AUDIT_QUEUE, true);
	}

	@Bean
	public Binding auditBinding(Queue auditQueue, TopicExchange auditExchange) {

		return BindingBuilder.bind(auditQueue).to(auditExchange).with(RabbitMQConstants.AUDIT_ROUTING_KEY);
	}
}