package com.ums.org.config;

import org.springframework.amqp.core.TopicExchange;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.ums.events.constants.RabbitMQConstants;

@Configuration
public class RabbitProducerConfig {

	public static final String EXCHANGE = "ums.exchange";

	@Bean
	public TopicExchange organizationExchange() {
		return new TopicExchange(RabbitMQConstants.ORGANIZATION_EXCHANGE);
	}

	@Bean
	TopicExchange topicExchange() {
		return new TopicExchange(EXCHANGE);
	}



}