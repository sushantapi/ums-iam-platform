package com.ums.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.ums.auth.dto.admin.AdminSessionResponse;
import com.ums.auth.entity.Session;
import com.ums.auth.entity.User;
import com.ums.auth.repository.SessionRepository;
import com.ums.events.publisher.AuditPublisher;

@ExtendWith(MockitoExtension.class)
class AdminSessionServiceImplTests {

	@Mock
	private SessionRepository sessionRepository;

	@Mock
	private AuditPublisher auditPublisher;

	@Mock
	private TokenBlacklistService tokenBlacklistService;

	@InjectMocks
	private AdminSessionServiceImpl adminSessionService;

	@Test
	void mapsUserSessionsToTheAdminContract() {
		UUID userId = UUID.randomUUID();
		UUID organizationId = UUID.randomUUID();
		Session session = activeSession(userId);
		session.setOrganizationId(organizationId);
		session.setClient("admin-portal");

		when(sessionRepository.findByUserId(userId)).thenReturn(List.of(session));

		List<AdminSessionResponse> response = adminSessionService.listUserSessions(userId);

		assertThat(response).singleElement().satisfies(item -> {
			assertThat(item.userId()).isEqualTo(userId);
			assertThat(item.userName()).isEqualTo("Ada Lovelace");
			assertThat(item.organizationId()).isEqualTo(organizationId);
			assertThat(item.client()).isEqualTo("admin-portal");
			assertThat(item.status()).isEqualTo("ACTIVE");
		});
	}

	@Test
	void revokesOneSession() {
		Session session = activeSession(UUID.randomUUID());
		when(sessionRepository.findById(session.getId())).thenReturn(Optional.of(session));

		adminSessionService.revokeSession(session.getId(), UUID.randomUUID());

		assertThat(session.isRevoked()).isTrue();
		assertThat(session.getRevokedAt()).isNotNull();
		verify(sessionRepository).save(session);
		verify(tokenBlacklistService).revokeSession(eq(session.getId()), anyLong());
	}

	@Test
	void revokesAllSessionsForAUser() {
		UUID userId = UUID.randomUUID();
		Session first = activeSession(userId);
		Session second = activeSession(userId);
		when(sessionRepository.findByUserId(userId)).thenReturn(List.of(first, second));

		adminSessionService.revokeAllUserSessions(userId, UUID.randomUUID());

		assertThat(first.isRevoked()).isTrue();
		assertThat(second.isRevoked()).isTrue();
		verify(sessionRepository).saveAll(List.of(first, second));
		verify(tokenBlacklistService).revokeSession(eq(first.getId()), anyLong());
		verify(tokenBlacklistService).revokeSession(eq(second.getId()), anyLong());
	}

	private Session activeSession(UUID userId) {
		User user = User.builder().id(userId).firstName("Ada").lastName("Lovelace").email("ada@example.com")
				.passwordHash("hash").build();
		return Session.builder().id(UUID.randomUUID()).user(user).refreshTokenHash("hash")
				.deviceInfo("Chrome on Windows").ipAddress("127.0.0.1").createdAt(Instant.now().minusSeconds(60))
				.lastSeenAt(Instant.now()).expiresAt(Instant.now().plusSeconds(3600)).build();
	}
}
