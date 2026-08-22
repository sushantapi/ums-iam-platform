package com.ums.notification.service.impl;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
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
import com.ums.events.event.organization.OrganizationInviteEvent;
import com.ums.notification.entity.NotificationEvent;
import com.ums.notification.service.NotificationAuditService;
import com.ums.notification.service.NotificationEventService;
import com.ums.notification.service.TemplateService;

@ExtendWith(MockitoExtension.class)
class EmailServiceImplTests {

	private static final String RAW_LINK = "https://example.test/accept?token=raw-secret-token";

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
		lenient().when(eventService.save(any(NotificationEvent.class))).thenAnswer(invocation -> {
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
	void organizationInvitationBypassesGenericNotificationPersistence() {
		when(templateService.getSubject("ORGANIZATION_INVITATION")).thenReturn("Invitation");
		when(templateService.buildTemplate(eq("ORGANIZATION_INVITATION"), any())).thenReturn("Join Acme");

		emailService.processOrganizationInvitation(
				new OrganizationInviteEvent("ada@example.com", "Acme", RAW_LINK));

		verify(mailSender).send(any(SimpleMailMessage.class));
		verify(auditService).logSuccess("ORGANIZATION_INVITATION", "ada@example.com", "Invitation");
		verify(eventService, never()).save(any(NotificationEvent.class));
		verify(eventService, never()).markProcessed(anyLong());
	}

	@Test
	void organizationInvitationFailureStoresOnlyFailureTypeAndReturnsGenericException() {
		when(templateService.getSubject("ORGANIZATION_INVITATION")).thenReturn("Invitation");
		when(templateService.buildTemplate(eq("ORGANIZATION_INVITATION"), any())).thenReturn("Join Acme");
		doThrow(new IllegalStateException("SMTP unavailable"))
				.when(mailSender).send(any(SimpleMailMessage.class));

		assertThatThrownBy(() -> emailService.processOrganizationInvitation(
				new OrganizationInviteEvent("ada@example.com", "Acme", RAW_LINK)))
				.isInstanceOf(IllegalStateException.class)
				.hasMessage("Organization invitation email delivery failed")
				.hasMessageNotContaining("raw-secret-token");

		verify(auditService).logFailure(
				"ORGANIZATION_INVITATION",
				"ada@example.com",
				"Invitation",
				IllegalStateException.class.getName());
		verify(eventService, never()).save(any(NotificationEvent.class));
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

	private void stubWelcomeTemplate() {
		when(templateService.getSubject("WELCOME_EMAIL")).thenReturn("Welcome");
		when(templateService.buildTemplate(eq("WELCOME_EMAIL"), any())).thenReturn("Hello Ada");
	}
}
