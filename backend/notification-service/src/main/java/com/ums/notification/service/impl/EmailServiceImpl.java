package com.ums.notification.service.impl;

import java.util.Map;
import java.util.UUID;

import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ums.events.event.organization.OrganizationInviteEvent;
import com.ums.notification.entity.NotificationEvent;
import com.ums.notification.enums.NotificationChannel;
import com.ums.notification.enums.NotificationStatus;
import com.ums.notification.enums.NotificationType;
import com.ums.notification.service.EmailService;
import com.ums.notification.service.NotificationAuditService;
import com.ums.notification.service.NotificationEventService;
import com.ums.notification.service.TemplateService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailServiceImpl implements EmailService {

	private static final String ORGANIZATION_INVITATION_TEMPLATE = "ORGANIZATION_INVITATION";

	private final JavaMailSender mailSender;
	private final TemplateService templateService;
	private final NotificationAuditService auditService;
	private final NotificationEventService eventService;
	private final ObjectMapper objectMapper;

	@Override
	public void sendWelcomeEmail(String email, String firstName) {
		sendEmail("WELCOME_EMAIL", email, Map.of("name", firstName));
	}

	@Override
	public void sendVerificationEmail(String email, String firstName, String verificationLink) {
		sendEmail("EMAIL_VERIFICATION", email, Map.of("name", firstName, "verificationLink", verificationLink));
	}

	@Override
	public void sendPasswordResetEmail(String email, String firstName, String resetLink) {
		sendEmail("PASSWORD_RESET", email, Map.of("name", firstName, "resetLink", resetLink));
	}

	@Override
	public void sendOtpEmail(String email, String otp) {
		sendEmail("MFA_OTP", email, Map.of("name", email, "otp", otp));
	}

	private void sendEmail(String templateCode, String recipientEmail, Map<String, Object> variables) {
		NotificationEvent event = createEvent(templateCode, recipientEmail, variables);
		deliver(event, variables);
	}

	private void deliver(NotificationEvent event, Map<String, Object> variables) {
		String templateCode = event.getTemplateCode();
		String recipientEmail = event.getRecipientEmail();
		String subject = templateCode;

		try {
			subject = templateService.getSubject(templateCode);
			log.info("Sending {} email to {}", templateCode, maskEmail(recipientEmail));
			String body = templateService.buildTemplate(templateCode, variables);

			SimpleMailMessage message = new SimpleMailMessage();
			message.setTo(recipientEmail);
			message.setSubject(subject);
			message.setText(body);
			mailSender.send(message);

			auditService.logSuccess(templateCode, recipientEmail, subject);
			eventService.markProcessed(event.getId());
			log.info("{} email sent successfully to {}", templateCode, maskEmail(recipientEmail));
		} catch (Exception ex) {
			log.error("Failed to send {} email to {}", templateCode, maskEmail(recipientEmail), ex);
			auditService.logFailure(templateCode, recipientEmail, subject, ex.getMessage());
			eventService.markFailed(event.getId(), ex.getMessage());
		}
	}

	private NotificationEvent createEvent(String templateCode, String recipientEmail, Map<String, Object> variables) {
		try {
			NotificationEvent event = NotificationEvent.builder()
					.recipient(recipientEmail)
					.recipientEmail(recipientEmail)
					.templateCode(templateCode)
					.correlationId(UUID.randomUUID().toString())
					.channel(NotificationChannel.EMAIL)
					.notificationType(NotificationType.valueOf(templateCode))
					.status(NotificationStatus.PENDING)
					.payload(objectMapper.writeValueAsString(variables))
					.build();
			return eventService.save(event);
		} catch (Exception ex) {
			throw new IllegalStateException("Could not persist notification event", ex);
		}
	}

	@Override
	public void retry(NotificationEvent event) {
		try {
			Map<String, Object> variables = objectMapper.readValue(
					event.getPayload(), new TypeReference<Map<String, Object>>() {
					});
			deliver(event, variables);
		} catch (Exception ex) {
			eventService.markFailed(event.getId(), ex.getMessage());
		}
	}

	@Override
	public void processOrganizationInvitation(OrganizationInviteEvent event) {
		if (event == null || event.getEmail() == null || event.getEmail().isBlank()) {
			throw new IllegalArgumentException("Organization invitation email is required");
		}
		if (event.getInviteLink() == null || event.getInviteLink().isBlank()) {
			throw new IllegalArgumentException("Organization invitation link is required");
		}

		String subject = ORGANIZATION_INVITATION_TEMPLATE;
		String organizationName = event.getOrganizationName() == null || event.getOrganizationName().isBlank()
				? "your organization"
				: event.getOrganizationName();

		try {
			subject = templateService.getSubject(ORGANIZATION_INVITATION_TEMPLATE);
			String body = templateService.buildTemplate(ORGANIZATION_INVITATION_TEMPLATE,
					Map.of("organizationName", organizationName, "inviteLink", event.getInviteLink()));

			SimpleMailMessage message = new SimpleMailMessage();
			message.setTo(event.getEmail());
			message.setSubject(subject);
			message.setText(body);
			mailSender.send(message);
		} catch (Exception ex) {
			String failureType = ex.getClass().getName();
			safeInvitationAuditFailure(event.getEmail(), subject, failureType);
			log.warn("Organization invitation email delivery failed for {} failureType={}",
					maskEmail(event.getEmail()), failureType);
			throw new IllegalStateException("Organization invitation email delivery failed");
		}

		safeInvitationAuditSuccess(event.getEmail(), subject);
		log.info("Organization invitation email sent to {}", maskEmail(event.getEmail()));
	}

	@Override
	public void sendOrganizationCreatedEmail(String email, String organizationName) {
		sendEmail("ORGANIZATION_CREATED", email, Map.of("organizationName", organizationName));
	}

	private void safeInvitationAuditSuccess(String email, String subject) {
		try {
			auditService.logSuccess(ORGANIZATION_INVITATION_TEMPLATE, email, subject);
		} catch (RuntimeException auditFailure) {
			log.warn("Organization invitation delivery audit write failed status=SENT failureType={}",
					auditFailure.getClass().getName());
		}
	}

	private void safeInvitationAuditFailure(String email, String subject, String failureType) {
		try {
			auditService.logFailure(ORGANIZATION_INVITATION_TEMPLATE, email, subject, failureType);
		} catch (RuntimeException auditFailure) {
			log.warn("Organization invitation delivery audit write failed status=FAILED failureType={}",
					auditFailure.getClass().getName());
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
