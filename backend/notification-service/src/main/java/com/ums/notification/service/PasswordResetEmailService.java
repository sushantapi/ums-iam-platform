package com.ums.notification.service;

import java.util.Map;

import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class PasswordResetEmailService {

	private static final String TEMPLATE_CODE = "PASSWORD_RESET";

	private final JavaMailSender mailSender;
	private final TemplateService templateService;
	private final NotificationAuditService auditService;

	public void send(String email, String resetLink) {
		String subject = TEMPLATE_CODE;
		try {
			subject = templateService.getSubject(TEMPLATE_CODE);
			String body = templateService.buildTemplate(
					TEMPLATE_CODE,
					Map.of("name", email, "resetLink", resetLink));

			SimpleMailMessage message = new SimpleMailMessage();
			message.setTo(email);
			message.setSubject(subject);
			message.setText(body);
			mailSender.send(message);

			auditService.logSuccess(TEMPLATE_CODE, email, subject);
			log.info("Password reset email sent to {}", maskEmail(email));
		} catch (Exception ex) {
			auditService.logFailure(TEMPLATE_CODE, email, subject, ex.getMessage());
			log.error("Password reset email delivery failed for {}", maskEmail(email), ex);
			throw new IllegalStateException("Password reset email delivery failed", ex);
		}
	}

	private String maskEmail(String email) {
		if (email == null || email.isBlank()) {
			return "<blank>";
		}
		int atIndex = email.indexOf('@');
		if (atIndex <= 1) {
			return "***";
		}
		return email.charAt(0) + "***" + email.substring(atIndex);
	}
}
