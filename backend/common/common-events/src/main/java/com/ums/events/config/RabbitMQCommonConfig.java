package com.ums.events.config;

import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQCommonConfig {

	@Bean
	public MessageConverter jsonMessageConverter() {
		return new Jackson2JsonMessageConverter();
	}

	@Bean
	public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory, MessageConverter messageConverter) {

		RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);

		rabbitTemplate.setMessageConverter(messageConverter);

		return rabbitTemplate;
	}

	@Bean
	public TopicExchange auditExchange() {
		return new TopicExchange("audit.exchange");
	}
}