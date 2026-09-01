package com.ums.notification.service;

import com.ums.events.event.organization.OrganizationInviteEvent;
import com.ums.notification.entity.NotificationEvent;

public interface EmailService {

	void sendWelcomeEmail(String email, String firstName);

	void sendVerificationEmail(String email, String firstName, String verificationLink);

	void sendPasswordResetEmail(String email, String firstName, String resetLink);

	void sendOtpEmail(String email, String otp);

	void processOrganizationInvitation(OrganizationInviteEvent event);

	void sendOrganizationCreatedEmail(String email, String organizationName);

	void sendRoleAssignedEmail(String email, String firstName, String roleName);

	void retry(NotificationEvent event);
}
