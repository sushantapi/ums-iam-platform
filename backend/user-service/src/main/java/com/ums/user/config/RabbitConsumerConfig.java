/*
 * package com.ums.user.config;
 * 
 * import org.springframework.amqp.core.Binding; import
 * org.springframework.amqp.core.BindingBuilder; import
 * org.springframework.amqp.core.Queue; import
 * org.springframework.amqp.core.TopicExchange; import
 * org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
 * import org.springframework.amqp.rabbit.connection.ConnectionFactory; import
 * org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
 * import org.springframework.context.annotation.Bean; import
 * org.springframework.context.annotation.Configuration;
 * 
 * import com.ums.events.constants.RabbitMQConstants;
 * 
 * @Configuration public class RabbitConsumerConfig {
 * 
 * @Bean public Jackson2JsonMessageConverter messageConverter() { return new
 * Jackson2JsonMessageConverter(); }
 * 
 * @Bean public SimpleRabbitListenerContainerFactory
 * rabbitListenerContainerFactory(ConnectionFactory connectionFactory,
 * Jackson2JsonMessageConverter converter) {
 * 
 * SimpleRabbitListenerContainerFactory factory = new
 * SimpleRabbitListenerContainerFactory();
 * 
 * factory.setConnectionFactory(connectionFactory);
 * factory.setMessageConverter(converter);
 * 
 * return factory; }
 * 
 * @Bean public TopicExchange userExchange() { return new
 * TopicExchange(RabbitMQConstants.USER_EXCHANGE); }
 * 
 * @Bean public Queue profileUserRegisteredQueue() { return new
 * Queue(RabbitMQConstants.PROFILE_USER_REGISTERED_QUEUE, true); }
 * 
 * @Bean public Binding profileUserRegisteredBinding(Queue
 * profileUserRegisteredQueue, TopicExchange userExchange) {
 * 
 * return BindingBuilder.bind(profileUserRegisteredQueue).to(userExchange)
 * .with(RabbitMQConstants.USER_REGISTERED_ROUTING_KEY); } }
 */

package com.ums.user.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.ums.events.constants.RabbitMQConstants;

@Configuration
public class RabbitConsumerConfig {

	@Bean
	public Jackson2JsonMessageConverter messageConverter() {
		return new Jackson2JsonMessageConverter();
	}

	@Bean
	public TopicExchange userExchange() {
		return new TopicExchange(RabbitMQConstants.USER_EXCHANGE);
	}

	@Bean
	public Queue profileUserRegisteredQueue() {
		return new Queue(RabbitMQConstants.PROFILE_USER_REGISTERED_QUEUE, true);
	}

	@Bean
	public Binding profileUserRegisteredBinding(Queue profileUserRegisteredQueue, TopicExchange userExchange) {

		return BindingBuilder.bind(profileUserRegisteredQueue).to(userExchange)
				.with(RabbitMQConstants.USER_REGISTERED_ROUTING_KEY);
	}
}