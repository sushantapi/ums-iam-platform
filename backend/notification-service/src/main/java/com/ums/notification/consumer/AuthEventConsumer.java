package com.ums.notification.consumer;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import com.ums.events.constants.RabbitMQConstants;
import com.ums.events.event.EmailVerificationEvent;
import com.ums.events.event.MfaOtpEvent;
import com.ums.events.event.PasswordResetEvent;
import com.ums.notification.service.NotificationService;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class AuthEventConsumer {

	private final NotificationService notificationService;

	@RabbitListener(queues = RabbitMQConstants.NOTIFICATION_EMAIL_VERIFICATION_QUEUE)
	public void consumeEmailVerification(EmailVerificationEvent event) {
		notificationService.processEmailVerification(event);
	}

	@RabbitListener(queues = RabbitMQConstants.NOTIFICATION_PASSWORD_RESET_QUEUE)
	public void consumePasswordReset(PasswordResetEvent event) {
		notificationService.processPasswordReset(event);
	}

	@RabbitListener(queues = RabbitMQConstants.NOTIFICATION_MFA_OTP_QUEUE)
	public void consumeMfaOtp(MfaOtpEvent event) {
		notificationService.processMfaOtp(event);
	}
}
