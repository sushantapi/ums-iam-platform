package com.ums.notification.strategy;

import org.springframework.stereotype.Component;

import com.ums.notification.domain.NotificationContext;
import com.ums.notification.enums.NotificationChannel;
import com.ums.notification.service.EmailService;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class EmailNotificationStrategy implements NotificationStrategy {

	private final EmailService emailService;

	@Override
	public NotificationChannel getChannel() {
		return NotificationChannel.EMAIL;
	}

	@Override
	public void send(NotificationContext context) {

		switch (context.getTemplateCode()) {

		case "WELCOME_EMAIL" -> emailService.sendWelcomeEmail(context.getRecipientEmail(), context.getRecipientName());

		case "EMAIL_VERIFICATION" -> emailService.sendVerificationEmail(context.getRecipientEmail(),
				context.getRecipientName(), String.valueOf(context.getVariables().get("verificationLink")));

		case "PASSWORD_RESET" -> emailService.sendPasswordResetEmail(context.getRecipientEmail(),
				context.getRecipientName(), String.valueOf(context.getVariables().get("resetLink")));

		case "MFA_OTP" ->
			emailService.sendOtpEmail(context.getRecipientEmail(), String.valueOf(context.getVariables().get("otp")));

		default -> throw new IllegalArgumentException("Unsupported template: " + context.getTemplateCode());
		}
	}
}