package com.ums.notification.service;

import java.util.Optional;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.ums.notification.client.UserDirectoryClient;
import com.ums.notification.dto.UserDirectoryResponse;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationRecipientResolver {

	private final UserDirectoryClient userDirectoryClient;

	@Value("${internal.service.secret}")
	private String internalServiceSecret;

	public Optional<NotificationRecipient> resolve(UUID userId, String eventEmail, String eventFirstName) {
		if (StringUtils.hasText(eventEmail)) {
			return Optional.of(new NotificationRecipient(eventEmail.trim(), displayName(eventFirstName, eventEmail)));
		}

		if (userId == null) {
			log.warn("Cannot resolve notification recipient because userId is missing");
			return Optional.empty();
		}

		try {
			UserDirectoryResponse user = userDirectoryClient.getUser(userId, internalServiceSecret);
			if (user == null || !StringUtils.hasText(user.email())) {
				log.warn("Notification recipient has no deliverable email userId={}", userId);
				return Optional.empty();
			}

			return Optional.of(new NotificationRecipient(
					user.email().trim(),
					displayName(user.firstName(), user.email())));
		} catch (RuntimeException ex) {
			log.error("Failed to resolve notification recipient userId={} failureType={}",
					userId, ex.getClass().getSimpleName());
			return Optional.empty();
		}
	}

	private String displayName(String firstName, String email) {
		if (StringUtils.hasText(firstName)) {
			return firstName.trim();
		}
		return StringUtils.hasText(email) ? email.trim() : "there";
	}

	public record NotificationRecipient(String email, String firstName) {
	}
}
