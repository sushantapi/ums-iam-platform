package com.ums.notification.service.impl;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.ums.events.event.role.RoleAssignedEvent;
import com.ums.notification.service.EmailService;

@ExtendWith(MockitoExtension.class)
class NotificationServiceImplTests {

	@Mock
	private EmailService emailService;

	@InjectMocks
	private NotificationServiceImpl notificationService;

	@Test
	void sendsRoleAssignedEmailWhenRecipientEmailIsAvailable() {
		RoleAssignedEvent event = RoleAssignedEvent.builder()
				.userId(UUID.randomUUID())
				.email("ada@example.com")
				.firstName("Ada")
				.roleName("EMPLOYEE")
				.build();

		notificationService.processRoleAssigned(event);

		verify(emailService).sendRoleAssignedEmail("ada@example.com", "Ada", "EMPLOYEE");
	}

	@Test
	void skipsRoleAssignedEmailWhenRecipientEmailIsMissing() {
		RoleAssignedEvent event = RoleAssignedEvent.builder()
				.userId(UUID.randomUUID())
				.roleName("EMPLOYEE")
				.build();

		notificationService.processRoleAssigned(event);

		verify(emailService, never()).sendRoleAssignedEmail(null, null, "EMPLOYEE");
	}
}
