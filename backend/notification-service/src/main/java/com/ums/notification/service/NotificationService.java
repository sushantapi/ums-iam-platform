package com.ums.notification.service;

import com.ums.events.event.organization.OrganizationCreatedEvent;
import com.ums.events.event.user.UserRegisteredEvent;
import com.ums.notification.event.EmailVerificationEvent;
import com.ums.notification.event.MfaOtpEvent;
import com.ums.notification.event.PasswordResetEvent;

public interface NotificationService {

	void processUserRegistered(UserRegisteredEvent event);

	void processEmailVerification(EmailVerificationEvent event);

	void processPasswordReset(PasswordResetEvent event);

	void processMfaOtp(MfaOtpEvent event);

	void sendOrganizationCreatedEmail(OrganizationCreatedEvent event);

}