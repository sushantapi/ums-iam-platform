package com.ums.notification.consumer;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.ums.events.event.organization.OrganizationInviteEvent;
import com.ums.notification.service.EmailService;

@ExtendWith(MockitoExtension.class)
class OrganizationInvitationEventConsumerTests {

	@Mock
	private EmailService emailService;

	@Test
	void routesInvitationToSensitiveEmailPath() {
		OrganizationInviteEvent event = new OrganizationInviteEvent(
				"invitee@example.test", "Example Org", "https://example.test/accept?token=secret");
		OrganizationInvitationEventConsumer consumer = new OrganizationInvitationEventConsumer(emailService);

		consumer.consume(event);

		verify(emailService).processOrganizationInvitation(event);
	}

	@Test
	void containsDeliveryFailureSoSensitiveRabbitMessageIsAcknowledged() {
		OrganizationInviteEvent event = new OrganizationInviteEvent(
				"invitee@example.test", "Example Org", "https://example.test/accept?token=secret");
		doThrow(new IllegalStateException("delivery failed"))
				.when(emailService).processOrganizationInvitation(event);
		OrganizationInvitationEventConsumer consumer = new OrganizationInvitationEventConsumer(emailService);

		assertThatCode(() -> consumer.consume(event)).doesNotThrowAnyException();

		verify(emailService).processOrganizationInvitation(event);
	}
}
