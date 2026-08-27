package com.ums.notification.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.ums.notification.client.UserDirectoryClient;
import com.ums.notification.dto.UserDirectoryResponse;

@ExtendWith(MockitoExtension.class)
class NotificationRecipientResolverTests {

	@Mock
	private UserDirectoryClient userDirectoryClient;

	@InjectMocks
	private NotificationRecipientResolver resolver;

	@BeforeEach
	void setInternalSecret() {
		ReflectionTestUtils.setField(resolver, "internalServiceSecret", "test-internal-secret");
	}

	@Test
	void eventRecipientIsPreferredWithoutDirectoryLookup() {
		UUID userId = UUID.randomUUID();

		var recipient = resolver.resolve(userId, "user@example.com", "Sushant");

		assertThat(recipient).isPresent();
		assertThat(recipient.orElseThrow().email()).isEqualTo("user@example.com");
		assertThat(recipient.orElseThrow().firstName()).isEqualTo("Sushant");
		verifyNoInteractions(userDirectoryClient);
	}

	@Test
	void directoryRecipientIsUsedWhenEventHasNoEmail() {
		UUID userId = UUID.randomUUID();
		when(userDirectoryClient.getUser(userId, "test-internal-secret"))
				.thenReturn(new UserDirectoryResponse(userId, "resolved@example.com", "Resolved", "User"));

		var recipient = resolver.resolve(userId, null, null);

		assertThat(recipient).isPresent();
		assertThat(recipient.orElseThrow().email()).isEqualTo("resolved@example.com");
		assertThat(recipient.orElseThrow().firstName()).isEqualTo("Resolved");
	}

	@Test
	void missingDirectoryEmailIsHandledWithoutDeliveryTarget() {
		UUID userId = UUID.randomUUID();
		when(userDirectoryClient.getUser(userId, "test-internal-secret"))
				.thenReturn(new UserDirectoryResponse(userId, " ", "NoEmail", "User"));

		assertThat(resolver.resolve(userId, null, null)).isEmpty();
	}
}
