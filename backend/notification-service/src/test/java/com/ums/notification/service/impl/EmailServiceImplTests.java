package com.ums.notification.service.impl;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ums.notification.entity.NotificationEvent;
import com.ums.notification.service.NotificationAuditService;
import com.ums.notification.service.NotificationEventService;
import com.ums.notification.service.TemplateService;

@ExtendWith(MockitoExtension.class)
class EmailServiceImplTests {

	@Mock
	private JavaMailSender mailSender;

	@Mock
	private TemplateService templateService;

	@Mock
	private NotificationAuditService auditService;

	@Mock
	private NotificationEventService eventService;

	private EmailServiceImpl emailService;

	@BeforeEach
	void setUp() {
		emailService = new EmailServiceImpl(
				mailSender,
				templateService,
				auditService,
				eventService,
				new ObjectMapper());
		when(eventService.save(any(NotificationEvent.class))).thenAnswer(invocation -> {
			NotificationEvent event = invocation.getArgument(0);
			event.setId(42L);
			return event;
		});
	}

	@Test
	void marksSuccessfulDeliveryAsProcessed() {
		stubWelcomeTemplate();

		emailService.sendWelcomeEmail("ada@example.com", "Ada");

		verify(eventService).markProcessed(42L);
		verify(auditService).logSuccess("WELCOME_EMAIL", "ada@example.com", "Welcome");
	}

	@Test
	void persistsFailureWithoutRethrowingToRabbit() {
		stubWelcomeTemplate();
		doThrow(new IllegalStateException("SMTP unavailable"))
				.when(mailSender)
				.send(any(SimpleMailMessage.class));

		emailService.sendWelcomeEmail("ada@example.com", "Ada");

		verify(eventService).markFailed(42L, "SMTP unavailable");
		verify(auditService).logFailure(
				"WELCOME_EMAIL",
				"ada@example.com",
				"Welcome",
				"SMTP unavailable");
	}

	@Test
	void sendsRoleAssignedEmailWithRoleTemplate() {
		when(templateService.getSubject("ROLE_ASSIGNED")).thenReturn("Role assigned");
		when(templateService.buildTemplate(eq("ROLE_ASSIGNED"), any())).thenReturn("Hello Ada");

		emailService.sendRoleAssignedEmail("ada@example.com", "Ada", "EMPLOYEE");

		verify(eventService).markProcessed(42L);
		verify(auditService).logSuccess("ROLE_ASSIGNED", "ada@example.com", "Role assigned");
	}

	private void stubWelcomeTemplate() {
		when(templateService.getSubject("WELCOME_EMAIL")).thenReturn("Welcome");
		when(templateService.buildTemplate(eq("WELCOME_EMAIL"), any())).thenReturn("Hello Ada");
	}
}
