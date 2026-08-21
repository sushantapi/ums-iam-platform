package com.ums.org.publisher;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import java.time.LocalDateTime;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import com.ums.events.constants.RabbitMQConstants;
import com.ums.events.event.organization.OrganizationInviteEvent;
import com.ums.org.config.OrganizationInvitationProperties;
import com.ums.org.service.OrganizationInvitationDeliveryStateService;

@ExtendWith(MockitoExtension.class)
class OrganizationEventPublisherTests {

	private static final UUID INVITATION_ID = UUID.fromString("00000000-0000-0000-0000-000000000020");
	private static final String EMAIL = "invitee@example.test";
	private static final String RAW_TOKEN = "raw-invitation-token";

	@Mock
	private RabbitTemplate rabbitTemplate;
	@Mock
	private OrganizationInvitationProperties invitationProperties;
	@Mock
	private OrganizationInvitationDeliveryStateService deliveryStateService;

	private OrganizationEventPublisher publisher;

	@BeforeEach
	void setUp() {
		publisher = new OrganizationEventPublisher(rabbitTemplate, invitationProperties, deliveryStateService);
		lenient().when(invitationProperties.getAcceptPageUrl())
				.thenReturn("http://localhost:5174/accept-invitation");
		TransactionSynchronizationManager.initSynchronization();
		TransactionSynchronizationManager.setActualTransactionActive(true);
	}

	@AfterEach
	void tearDown() {
		if (TransactionSynchronizationManager.isSynchronizationActive()) {
			TransactionSynchronizationManager.clearSynchronization();
		}
		TransactionSynchronizationManager.setActualTransactionActive(false);
	}

	@Test
	void publishesOnlyAfterCommitAndRedactsEventStringRepresentation() {
		publisher.publishOrganizationInvitationAfterCommit(INVITATION_ID, EMAIL, "Example Org", RAW_TOKEN);

		verifyNoInteractions(rabbitTemplate);
		assertThat(TransactionSynchronizationManager.getSynchronizations()).hasSize(1);

		TransactionSynchronizationManager.getSynchronizations().get(0).afterCommit();

		ArgumentCaptor<OrganizationInviteEvent> eventCaptor = ArgumentCaptor.forClass(OrganizationInviteEvent.class);
		verify(rabbitTemplate).convertAndSend(
				eq(RabbitMQConstants.ORGANIZATION_EXCHANGE),
				eq(RabbitMQConstants.ORGANIZATION_INVITATION_ROUTING_KEY),
				eventCaptor.capture());
		OrganizationInviteEvent event = eventCaptor.getValue();
		assertThat(event.getEmail()).isEqualTo(EMAIL);
		assertThat(event.getOrganizationName()).isEqualTo("Example Org");
		assertThat(event.getInviteLink()).contains("accept-invitation").contains("token=" + RAW_TOKEN);
		assertThat(event.toString()).doesNotContain(EMAIL, RAW_TOKEN, event.getInviteLink());
		verify(deliveryStateService).markNotificationSent(eq(INVITATION_ID), any(LocalDateTime.class));
	}

	@Test
	void brokerFailureIsContainedAfterCommitAndDoesNotMarkSent() {
		doThrow(new IllegalStateException("broker unavailable"))
				.when(rabbitTemplate)
				.convertAndSend(eq(RabbitMQConstants.ORGANIZATION_EXCHANGE),
						eq(RabbitMQConstants.ORGANIZATION_INVITATION_ROUTING_KEY),
						any(OrganizationInviteEvent.class));
		publisher.publishOrganizationInvitationAfterCommit(INVITATION_ID, EMAIL, "Example Org", RAW_TOKEN);

		TransactionSynchronizationManager.getSynchronizations().get(0).afterCommit();

		verify(deliveryStateService, never()).markNotificationSent(any(), any());
	}

	@Test
	void refusesToScheduleSensitiveNotificationWithoutActiveTransaction() {
		TransactionSynchronizationManager.clearSynchronization();
		TransactionSynchronizationManager.setActualTransactionActive(false);

		assertThatThrownBy(() -> publisher.publishOrganizationInvitationAfterCommit(
				INVITATION_ID, EMAIL, "Example Org", RAW_TOKEN))
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("active transaction");

		verifyNoInteractions(rabbitTemplate);
	}
}
