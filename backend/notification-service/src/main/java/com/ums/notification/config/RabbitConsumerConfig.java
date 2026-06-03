/*
 * package com.ums.notification.config;
 * 
 * import org.springframework.amqp.core.Binding; import
 * org.springframework.amqp.core.BindingBuilder; import
 * org.springframework.amqp.core.Queue; import
 * org.springframework.amqp.core.QueueBuilder; import
 * org.springframework.amqp.core.TopicExchange; import
 * org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
 * import org.springframework.amqp.support.converter.MessageConverter; import
 * org.springframework.context.annotation.Bean; import
 * org.springframework.context.annotation.Configuration;
 * 
 * import com.ums.events.constants.RabbitMQConstants; import
 * com.ums.notification.constant.QueueConstants;
 * 
 * @Configuration public class RabbitConsumerConfig {
 * 
 * public static final String QUEUE = "user.created.queue"; public static final
 * String EXCHANGE = "user.exchange"; public static final String ROUTING_KEY =
 * "user.created";
 * 
 * @Bean public Queue queue() { return new Queue(QUEUE); }
 * 
 * @Bean public MessageConverter jsonMessageConverter() { return new
 * Jackson2JsonMessageConverter(); }
 * 
 * @Bean public TopicExchange exchange() { return new
 * TopicExchange(QueueConstants.EXCHANGE); }
 * 
 * 
 * ========================== DLQ Configuration ==========================
 * 
 * 
 * @Bean public Queue userDlq() { return
 * QueueBuilder.durable(QueueConstants.USER_DLQ).build(); }
 * 
 * @Bean public Binding userDlqBinding(Queue userDlq, TopicExchange exchange) {
 * 
 * return
 * BindingBuilder.bind(userDlq).to(exchange).with(QueueConstants.USER_DLQ_KEY);
 * }
 * 
 * 
 * ========================== Main Queues ==========================
 * 
 * 
 * @Bean public Queue userQueue() {
 * 
 * return QueueBuilder.durable(QueueConstants.USER_QUEUE).deadLetterExchange(
 * QueueConstants.EXCHANGE)
 * .deadLetterRoutingKey(QueueConstants.USER_DLQ_KEY).build(); }
 * 
 * @Bean public Queue authQueue() { return
 * QueueBuilder.durable(QueueConstants.AUTH_QUEUE).build(); }
 * 
 * @Bean public Queue orgQueue() { return
 * QueueBuilder.durable(QueueConstants.ORG_QUEUE).build(); }
 * 
 * @Bean public Queue emailVerificationQueue() { return
 * QueueBuilder.durable(QueueConstants.EMAIL_VERIFICATION_QUEUE).build(); }
 * 
 * @Bean public Queue passwordResetQueue() { return
 * QueueBuilder.durable(QueueConstants.PASSWORD_RESET_QUEUE).build(); }
 * 
 * @Bean public Queue mfaOtpQueue() { return
 * QueueBuilder.durable(QueueConstants.MFA_OTP_QUEUE).build(); }
 * 
 * 
 * ========================== Bindings ==========================
 * 
 * 
 * @Bean public Binding userRegisteredBinding(Queue userQueue, TopicExchange
 * exchange) {
 * 
 * return BindingBuilder.bind(userQueue).to(exchange).with("user.*"); }
 * 
 * @Bean public Binding authBinding(Queue authQueue, TopicExchange exchange) {
 * 
 * return BindingBuilder.bind(authQueue).to(exchange).with("auth.*"); }
 * 
 * @Bean public Binding orgBinding(Queue orgQueue, TopicExchange exchange) {
 * 
 * return BindingBuilder.bind(orgQueue).to(exchange).with("organization.*"); }
 * 
 * @Bean public Binding emailVerificationBinding(Queue emailVerificationQueue,
 * TopicExchange exchange) {
 * 
 * return
 * BindingBuilder.bind(emailVerificationQueue).to(exchange).with(QueueConstants.
 * EMAIL_VERIFIED_KEY); }
 * 
 * @Bean public Binding passwordResetBinding(Queue passwordResetQueue,
 * TopicExchange exchange) {
 * 
 * return
 * BindingBuilder.bind(passwordResetQueue).to(exchange).with(QueueConstants.
 * PASSWORD_RESET_KEY); }
 * 
 * @Bean public Binding mfaOtpBinding(Queue mfaOtpQueue, TopicExchange exchange)
 * {
 * 
 * return
 * BindingBuilder.bind(mfaOtpQueue).to(exchange).with(QueueConstants.MFA_OTP_KEY
 * ); }
 * 
 * @Bean public Queue userRegisteredQueue() { return new
 * Queue(RabbitMQConstants.USER_REGISTERED_QUEUE); }
 * 
 * @Bean public TopicExchange userExchange() { return new
 * TopicExchange(RabbitMQConstants.USER_EXCHANGE); }
 * 
 * @Bean public Binding binding(Queue userRegisteredQueue, TopicExchange
 * userExchange) {
 * 
 * return BindingBuilder.bind(userRegisteredQueue).to(userExchange)
 * .with(RabbitMQConstants.USER_REGISTERED_ROUTING_KEY); }
 * 
 * }
 */

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
	public Binding notificationBinding(Queue notificationUserRegisteredQueue, TopicExchange userExchange) {

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

}