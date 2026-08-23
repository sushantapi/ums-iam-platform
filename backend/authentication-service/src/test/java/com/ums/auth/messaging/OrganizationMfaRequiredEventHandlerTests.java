package com.ums.auth.messaging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.ums.auth.entity.ProcessedSecurityEvent;
import com.ums.auth.entity.Session;
import com.ums.auth.repository.ProcessedSecurityEventRepository;
import com.ums.auth.repository.SessionRepository;
import com.ums.auth.service.TokenBlacklistService;
import com.ums.events.event.organization.OrganizationMfaRequiredEvent;

@ExtendWith(MockitoExtension.class)
class OrganizationMfaRequiredEventHandlerTests {

	@Mock
	private ProcessedSecurityEventRepository processedSecurityEventRepository;

	@Mock
	private SessionRepository sessionRepository;

	@Mock
	private TokenBlacklistService blacklistService;

	@InjectMocks
	private OrganizationMfaRequiredEventHandler handler;

	@Test
	void newEventRevokesActiveNonMfaOrganizationSessionsAndStoresMarker() {
		UUID eventId = UUID.randomUUID();
		UUID organizationId = UUID.randomUUID();
		UUID updatedBy = UUID.randomUUID();
		Session first = session(organizationId);
		Session second = session(organizationId);
		OrganizationMfaRequiredEvent event = event(eventId, organizationId, updatedBy);

		when(processedSecurityEventRepository.existsById(eventId)).thenReturn(false);
		when(sessionRepository.findByOrganizationIdAndRevokedFalseAndMfaVerifiedFalseAndExpiresAtAfter(
				eq(organizationId), any(Instant.class)))
				.thenReturn(List.of(first, second));

		boolean processed = handler.process(event);

		assertThat(processed).isTrue();
		assertThat(first.isRevoked()).isTrue();
		assertThat(second.isRevoked()).isTrue();
		assertThat(first.getRevokedAt()).isNotNull();
		assertThat(second.getRevokedAt()).isNotNull();
		verify(blacklistService).revokeSession(eq(first.getId()), anyLong());
		verify(blacklistService).revokeSession(eq(second.getId()), anyLong());
		verify(sessionRepository).saveAll(List.of(first, second));

		ArgumentCaptor<ProcessedSecurityEvent> marker =
				ArgumentCaptor.forClass(ProcessedSecurityEvent.class);
		verify(processedSecurityEventRepository).save(marker.capture());
		assertThat(marker.getValue().getEventId()).isEqualTo(eventId);
		assertThat(marker.getValue().getEventType()).isEqualTo("organization.security.mfa.required");
		assertThat(marker.getValue().getUserId()).isEqualTo(updatedBy);
	}

	@Test
	void duplicateEventDoesNotRevokeSessionsAgain() {
		UUID eventId = UUID.randomUUID();
		OrganizationMfaRequiredEvent event =
				event(eventId, UUID.randomUUID(), UUID.randomUUID());
		when(processedSecurityEventRepository.existsById(eventId)).thenReturn(true);

		boolean processed = handler.process(event);

		assertThat(processed).isFalse();
		verify(sessionRepository, never())
				.findByOrganizationIdAndRevokedFalseAndMfaVerifiedFalseAndExpiresAtAfter(any(), any());
		verify(sessionRepository, never()).saveAll(any());
		verify(processedSecurityEventRepository, never()).save(any());
		verifyNoInteractions(blacklistService);
	}

	private OrganizationMfaRequiredEvent event(UUID eventId, UUID organizationId, UUID updatedBy) {
		return OrganizationMfaRequiredEvent.builder()
				.eventId(eventId)
				.organizationId(organizationId)
				.updatedBy(updatedBy)
				.occurredAt(java.time.LocalDateTime.now())
				.build();
	}

	private Session session(UUID organizationId) {
		return Session.builder()
				.id(UUID.randomUUID())
				.organizationId(organizationId)
				.mfaVerified(false)
				.revoked(false)
				.expiresAt(Instant.now().plusSeconds(3600))
				.build();
	}
}
