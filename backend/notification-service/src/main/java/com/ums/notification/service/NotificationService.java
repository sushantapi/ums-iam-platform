package com.ums.notification.service;

import com.ums.events.event.EmailVerificationEvent;
import com.ums.events.event.MfaOtpEvent;
import com.ums.events.event.PasswordResetEvent;
import com.ums.events.event.organization.OrganizationCreatedEvent;
import com.ums.events.event.role.RoleAssignedEvent;
import com.ums.events.event.user.UserRegisteredEvent;

public interface NotificationService {

	void processUserRegistered(UserRegisteredEvent event);

	void processEmailVerification(EmailVerificationEvent event);

	void processPasswordReset(PasswordResetEvent event);

	void processMfaOtp(MfaOtpEvent event);

	void sendOrganizationCreatedEmail(OrganizationCreatedEvent event);

	void processRoleAssigned(RoleAssignedEvent event);

}
