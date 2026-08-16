package com.ums.auth.messaging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.ums.auth.entity.ProcessedSecurityEvent;
import com.ums.auth.repository.ProcessedSecurityEventRepository;
import com.ums.auth.service.AdminSessionService;
import com.ums.events.event.role.RoleRevokedEvent;

@ExtendWith(MockitoExtension.class)
class RoleRevocationEventHandlerTests {

	@Mock
	private ProcessedSecurityEventRepository processedSecurityEventRepository;

	@Mock
	private AdminSessionService adminSessionService;

	@InjectMocks
	private RoleRevocationEventHandler handler;

	@Test
	void newEventRevokesAllSessionsAndStoresIdempotencyMarker() {
		UUID eventId = UUID.randomUUID();
		UUID userId = UUID.randomUUID();
		UUID revokedBy = UUID.randomUUID();
		RoleRevokedEvent event = RoleRevokedEvent.builder()
				.eventId(eventId)
				.userId(userId)
				.revokedBy(revokedBy)
				.build();

		when(processedSecurityEventRepository.existsById(eventId)).thenReturn(false);

		boolean processed = handler.process(event);

		assertThat(processed).isTrue();
		verify(adminSessionService).revokeAllUserSessions(userId, revokedBy);

		ArgumentCaptor<ProcessedSecurityEvent> marker = ArgumentCaptor.forClass(ProcessedSecurityEvent.class);
		verify(processedSecurityEventRepository).save(marker.capture());
		assertThat(marker.getValue().getEventId()).isEqualTo(eventId);
		assertThat(marker.getValue().getUserId()).isEqualTo(userId);
		assertThat(marker.getValue().getEventType()).isEqualTo("role.revoked");
	}

	@Test
	void duplicateEventDoesNotRevokeSessionsAgain() {
		UUID eventId = UUID.randomUUID();
		RoleRevokedEvent event = RoleRevokedEvent.builder()
				.eventId(eventId)
				.userId(UUID.randomUUID())
				.build();

		when(processedSecurityEventRepository.existsById(eventId)).thenReturn(true);

		boolean processed = handler.process(event);

		assertThat(processed).isFalse();
		verify(adminSessionService, never()).revokeAllUserSessions(any(), any());
		verify(processedSecurityEventRepository, never()).save(any());
	}
}
