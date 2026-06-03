package com.ums.auth.config;

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
}