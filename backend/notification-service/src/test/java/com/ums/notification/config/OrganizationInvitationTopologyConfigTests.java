package com.ums.notification.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;

import com.ums.events.constants.RabbitMQConstants;

class OrganizationInvitationTopologyConfigTests {

	@Test
	void declaresDedicatedDurableQueueWithoutDeadLetteringSensitivePayload() {
		OrganizationInvitationTopologyConfig config = new OrganizationInvitationTopologyConfig();
		Queue queue = config.notificationOrganizationInvitationQueue();
		TopicExchange exchange = new TopicExchange(RabbitMQConstants.ORGANIZATION_EXCHANGE);
		Binding binding = config.organizationInvitationBinding(queue, exchange);

		assertThat(queue.getName()).isEqualTo(RabbitMQConstants.NOTIFICATION_ORGANIZATION_INVITATION_QUEUE);
		assertThat(queue.isDurable()).isTrue();
		assertThat(queue.getArguments() == null || queue.getArguments().isEmpty()).isTrue();
		assertThat(binding.getExchange()).isEqualTo(RabbitMQConstants.ORGANIZATION_EXCHANGE);
		assertThat(binding.getRoutingKey()).isEqualTo(RabbitMQConstants.ORGANIZATION_INVITATION_ROUTING_KEY);
	}
}
