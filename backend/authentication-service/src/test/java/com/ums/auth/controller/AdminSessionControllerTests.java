package com.ums.auth.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;

import com.ums.auth.dto.admin.AdminSessionResponse;
import com.ums.auth.service.AdminSessionService;
import com.ums.security.dto.JwtUser;

@ExtendWith(MockitoExtension.class)
class AdminSessionControllerTests {

	@Mock
	private AdminSessionService adminSessionService;

	@InjectMocks
	private AdminSessionController controller;

	@Test
	void returnsTheStandardPageContract() {
		when(adminSessionService.listSessions(any(), any()))
				.thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 20), 0));

		var response = controller.getSessions(0, 20, null, null, null, null, null);

		assertThat(response.content()).isEmpty();
		assertThat(response.page()).isZero();
		assertThat(response.size()).isEqualTo(20);
		assertThat(response.totalElements()).isZero();
	}

	@Test
	void revokesOneSessionAsTheCurrentAdmin() {
		UUID sessionId = UUID.randomUUID();
		UUID adminId = UUID.randomUUID();
		JwtUser admin = JwtUser.builder().userId(adminId).build();

		var response = controller.revokeSession(sessionId, admin);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
		verify(adminSessionService).revokeSession(sessionId, adminId);
	}

	@Test
	void returnsUserSessions() {
		UUID userId = UUID.randomUUID();
		when(adminSessionService.listUserSessions(userId)).thenReturn(List.<AdminSessionResponse>of());

		assertThat(controller.getUserSessions(userId)).isEmpty();
		verify(adminSessionService).listUserSessions(userId);
	}
}
