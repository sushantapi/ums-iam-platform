package com.ums.notification.service.impl;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.ums.events.event.role.RoleAssignedEvent;
import com.ums.events.event.role.RoleRevokedEvent;
import com.ums.notification.service.EmailService;
import com.ums.notification.service.NotificationRecipientResolver;
import com.ums.notification.service.NotificationRecipientResolver.NotificationRecipient;
import com.ums.notification.service.PasswordResetEmailService;

@ExtendWith(MockitoExtension.class)
class NotificationServiceImplTests {

	@Mock
	private EmailService emailService;

	@Mock
	private PasswordResetEmailService passwordResetEmailService;

	@Mock
	private NotificationRecipientResolver recipientResolver;

	@InjectMocks
	private NotificationServiceImpl notificationService;

	@Test
	void roleAssignedUsesResolvedRecipientAndScope() {
		UUID userId = UUID.randomUUID();
		RoleAssignedEvent event = RoleAssignedEvent.builder()
				.eventId(UUID.randomUUID())
				.userId(userId)
				.roleName("ORG_ADMIN")
				.scopeType("ORG")
				.scopeId("org-123")
				.build();
		when(recipientResolver.resolve(userId, null, null))
				.thenReturn(Optional.of(new NotificationRecipient("user@example.com", "Sushant")));

		notificationService.processRoleAssigned(event);

		verify(emailService).sendRoleAssignedEmail(
				"user@example.com", "Sushant", "ORG_ADMIN", "ORG", "org-123");
	}

	@Test
	void roleAssignedPrefersRecipientIncludedInEvent() {
		UUID userId = UUID.randomUUID();
		RoleAssignedEvent event = RoleAssignedEvent.builder()
				.eventId(UUID.randomUUID())
				.userId(userId)
				.roleName("SUPER_ADMIN")
				.email("event@example.com")
				.firstName("Event User")
				.scopeType("PLATFORM")
				.scopeId("*")
				.build();
		when(recipientResolver.resolve(userId, "event@example.com", "Event User"))
				.thenReturn(Optional.of(new NotificationRecipient("event@example.com", "Event User")));

		notificationService.processRoleAssigned(event);

		verify(emailService).sendRoleAssignedEmail(
				"event@example.com", "Event User", "SUPER_ADMIN", "PLATFORM", "*");
	}

	@Test
	void roleRevokedUsesResolvedRecipientAndScope() {
		UUID userId = UUID.randomUUID();
		RoleRevokedEvent event = RoleRevokedEvent.builder()
				.eventId(UUID.randomUUID())
				.userId(userId)
				.roleName("HR_MANAGER")
				.scopeType("DEPARTMENT")
				.scopeId("dept-7")
				.build();
		when(recipientResolver.resolve(userId, null, null))
				.thenReturn(Optional.of(new NotificationRecipient("user@example.com", "Sushant")));

		notificationService.processRoleRevoked(event);

		verify(emailService).sendRoleRevokedEmail(
				"user@example.com", "Sushant", "HR_MANAGER", "DEPARTMENT", "dept-7");
	}

	@Test
	void unresolvedRecipientDoesNotAttemptRoleEmail() {
		UUID userId = UUID.randomUUID();
		RoleAssignedEvent event = RoleAssignedEvent.builder()
				.eventId(UUID.randomUUID())
				.userId(userId)
				.roleName("EMPLOYEE")
				.scopeType("PLATFORM")
				.scopeId("*")
				.build();
		when(recipientResolver.resolve(userId, null, null)).thenReturn(Optional.empty());

		notificationService.processRoleAssigned(event);

		verify(emailService, never()).sendRoleAssignedEmail(
				org.mockito.ArgumentMatchers.anyString(),
				org.mockito.ArgumentMatchers.anyString(),
				org.mockito.ArgumentMatchers.anyString(),
				org.mockito.ArgumentMatchers.anyString(),
				org.mockito.ArgumentMatchers.anyString());
	}
}
