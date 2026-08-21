package com.ums.notification.service;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

@ExtendWith(MockitoExtension.class)
class PasswordResetEmailServiceTests {

	private static final String EMAIL = "user@example.com";
	private static final String SUBJECT = "Reset your UMS password";
	private static final String RESET_LINK =
			"http://localhost:5174/reset-password?token=raw-sensitive-token";

	@Mock
	private JavaMailSender mailSender;

	@Mock
	private TemplateService templateService;

	@Mock
	private NotificationAuditService auditService;

	private PasswordResetEmailService service;

	@BeforeEach
	void setUp() {
		service = new PasswordResetEmailService(mailSender, templateService, auditService);
		when(templateService.getSubject("PASSWORD_RESET")).thenReturn(SUBJECT);
		when(templateService.buildTemplate(eq("PASSWORD_RESET"), anyMap()))
				.thenReturn("Use " + RESET_LINK);
	}

	@Test
	void deliveryFailurePersistsOnlyFailureClassification() {
		MailSendException mailFailure = new MailSendException(
				"SMTP failed while sending " + RESET_LINK);
		doThrow(mailFailure).when(mailSender).send(any(SimpleMailMessage.class));

		assertThatThrownBy(() -> service.send(EMAIL, RESET_LINK))
				.isInstanceOf(IllegalStateException.class)
				.hasCause(mailFailure);

		verify(auditService).logFailure(
				"PASSWORD_RESET", EMAIL, SUBJECT, MailSendException.class.getName());
	}

	@Test
	void successfulDeliveryIsNotRetriedWhenAuditPersistenceFails() {
		doThrow(new IllegalStateException("audit database unavailable"))
				.when(auditService).logSuccess("PASSWORD_RESET", EMAIL, SUBJECT);

		assertThatCode(() -> service.send(EMAIL, RESET_LINK)).doesNotThrowAnyException();

		verify(mailSender, times(1)).send(any(SimpleMailMessage.class));
		verify(auditService).logSuccess("PASSWORD_RESET", EMAIL, SUBJECT);
	}

	@Test
	void failureAuditPersistenceCannotReplaceOriginalDeliveryFailure() {
		MailSendException mailFailure = new MailSendException("SMTP unavailable");
		doThrow(mailFailure).when(mailSender).send(any(SimpleMailMessage.class));
		doThrow(new IllegalStateException("audit database unavailable"))
				.when(auditService).logFailure(
						"PASSWORD_RESET", EMAIL, SUBJECT, MailSendException.class.getName());

		assertThatThrownBy(() -> service.send(EMAIL, RESET_LINK))
				.isInstanceOf(IllegalStateException.class)
				.hasCause(mailFailure);
	}
}
