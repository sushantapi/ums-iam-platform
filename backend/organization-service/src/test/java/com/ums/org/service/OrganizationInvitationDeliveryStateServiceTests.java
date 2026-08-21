package com.ums.org.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.ums.org.entity.OrganizationInvitation;
import com.ums.org.enums.OrganizationRole;
import com.ums.org.repositoty.OrganizationInvitationRepository;

@ExtendWith(MockitoExtension.class)
class OrganizationInvitationDeliveryStateServiceTests {

	@Mock
	private OrganizationInvitationRepository invitationRepository;

	@Test
	void marksPendingInvitationAsSentWithoutTouchingTokenMaterial() {
		UUID invitationId = UUID.randomUUID();
		LocalDateTime issuedAt = LocalDateTime.of(2026, 8, 21, 22, 0);
		LocalDateTime sentAt = issuedAt.plusMinutes(1);
		OrganizationInvitation invitation = OrganizationInvitation.createPending(
				UUID.randomUUID(), "invitee@example.test", OrganizationRole.MEMBER, UUID.randomUUID(),
				"a".repeat(64), issuedAt.plusDays(3), issuedAt);
		when(invitationRepository.findById(invitationId)).thenReturn(Optional.of(invitation));
		OrganizationInvitationDeliveryStateService service =
				new OrganizationInvitationDeliveryStateService(invitationRepository);

		service.markNotificationSent(invitationId, sentAt);

		assertThat(invitation.getLastSentAt()).isEqualTo(sentAt);
		assertThat(invitation.getTokenHash()).isEqualTo("a".repeat(64));
		verify(invitationRepository).saveAndFlush(invitation);
	}

	@Test
	void doesNotMarkTerminalInvitationAsSent() {
		UUID invitationId = UUID.randomUUID();
		LocalDateTime issuedAt = LocalDateTime.of(2026, 8, 21, 22, 0);
		OrganizationInvitation invitation = OrganizationInvitation.createPending(
				UUID.randomUUID(), "invitee@example.test", OrganizationRole.MEMBER, UUID.randomUUID(),
				"a".repeat(64), issuedAt.plusDays(3), issuedAt);
		invitation.markRevoked(issuedAt.plusMinutes(1));
		when(invitationRepository.findById(invitationId)).thenReturn(Optional.of(invitation));
		OrganizationInvitationDeliveryStateService service =
				new OrganizationInvitationDeliveryStateService(invitationRepository);

		service.markNotificationSent(invitationId, issuedAt.plusMinutes(2));

		assertThat(invitation.getLastSentAt()).isNull();
		verify(invitationRepository, never()).saveAndFlush(invitation);
	}
}
