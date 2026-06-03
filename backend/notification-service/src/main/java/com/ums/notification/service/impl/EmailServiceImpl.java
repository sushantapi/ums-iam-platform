/*
 * package com.ums.notification.service.impl;
 * 
 * import java.util.Map;
 * 
 * import org.springframework.mail.SimpleMailMessage; import
 * org.springframework.mail.javamail.JavaMailSender; import
 * org.springframework.stereotype.Service;
 * 
 * import com.ums.notification.service.EmailService; import
 * com.ums.notification.service.TemplateService;
 * 
 * import lombok.RequiredArgsConstructor;
 * 
 * @Service
 * 
 * @RequiredArgsConstructor public class EmailServiceImpl implements
 * EmailService {
 * 
 * private final JavaMailSender mailSender;
 * 
 * private final TemplateService templateService;
 * 
 * @Override public void sendWelcomeEmail(String email, String firstName) {
 * 
 * SimpleMailMessage message = new SimpleMailMessage();
 * 
 * message.setTo(email);
 * 
 * message.setSubject(templateService.getSubject("WELCOME_EMAIL"));
 * 
 * String body = templateService.buildTemplate("WELCOME_EMAIL", Map.of("name",
 * firstName));
 * 
 * message.setText(body);
 * 
 * mailSender.send(message); }
 * 
 * @Override public void sendVerificationEmail(String email, String firstName,
 * String verificationLink) {
 * 
 * SimpleMailMessage message = new SimpleMailMessage();
 * 
 * message.setTo(email);
 * 
 * message.setSubject(templateService.getSubject("EMAIL_VERIFICATION"));
 * 
 * String body = templateService.buildTemplate("EMAIL_VERIFICATION",
 * Map.of("name", firstName, "verificationLink", verificationLink));
 * 
 * message.setText(body);
 * 
 * mailSender.send(message); }
 * 
 * @Override public void sendPasswordResetEmail(String email, String firstName,
 * String resetLink) {
 * 
 * SimpleMailMessage message = new SimpleMailMessage();
 * 
 * message.setTo(email);
 * 
 * message.setSubject(templateService.getSubject("PASSWORD_RESET"));
 * 
 * String body = templateService.buildTemplate("PASSWORD_RESET", Map.of("name",
 * firstName, "resetLink", resetLink));
 * 
 * message.setText(body);
 * 
 * mailSender.send(message); }
 * 
 * @Override public void sendOtpEmail(String email, String otp) {
 * 
 * SimpleMailMessage message = new SimpleMailMessage();
 * 
 * message.setTo(email);
 * 
 * message.setSubject(templateService.getSubject("MFA_OTP"));
 * 
 * String body = templateService.buildTemplate("MFA_OTP", Map.of("name", email,
 * "otp", otp));
 * 
 * message.setText(body);
 * 
 * mailSender.send(message); } }
 */

package com.ums.notification.service.impl;

import java.util.Map;

import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import com.ums.events.event.organization.OrganizationCreatedEvent;
import com.ums.events.event.organization.OrganizationInviteEvent;
import com.ums.notification.service.EmailService;
import com.ums.notification.service.NotificationAuditService;
import com.ums.notification.service.TemplateService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailServiceImpl implements EmailService {

	private final JavaMailSender mailSender;

	private final TemplateService templateService;

	private final NotificationAuditService auditService;

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

		String subject = templateService.getSubject(templateCode);

		try {

			log.info("Sending {} email to {}", templateCode, recipientEmail);

			String body = templateService.buildTemplate(templateCode, variables);

			SimpleMailMessage message = new SimpleMailMessage();

			message.setTo(recipientEmail);
			message.setSubject(subject);
			message.setText(body);

			mailSender.send(message);

			auditService.logSuccess(templateCode, recipientEmail, subject);

			log.info("{} email sent successfully to {}", templateCode, recipientEmail);

		} catch (Exception ex) {

			log.error("Failed to send {} email to {}", templateCode, recipientEmail, ex);

			auditService.logFailure(templateCode, recipientEmail, subject, ex.getMessage());

			throw ex;
		}
	}

	@Override
	public void processOrganizationInvitation(OrganizationInviteEvent event) {
		// TODO Auto-generated method stub

	}

	@Override
	public void sendOrganizationCreatedEmail(String email, String organizationName) {

		sendEmail("ORGANIZATION_CREATED", email, Map.of("organizationName", organizationName));
	}
}