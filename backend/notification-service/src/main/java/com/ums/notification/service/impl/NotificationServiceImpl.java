package com.ums.notification.service.impl;

import org.springframework.stereotype.Service;

import com.ums.events.event.EmailVerificationEvent;
import com.ums.events.event.MfaOtpEvent;
import com.ums.events.event.PasswordResetEvent;
import com.ums.events.event.organization.OrganizationCreatedEvent;
import com.ums.events.event.role.RoleAssignedEvent;
import com.ums.events.event.role.RoleRevokedEvent;
import com.ums.events.event.user.UserRegisteredEvent;
import com.ums.notification.service.EmailService;
import com.ums.notification.service.NotificationRecipientResolver;
import com.ums.notification.service.NotificationService;
import com.ums.notification.service.PasswordResetEmailService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

	private final EmailService emailService;
	private final PasswordResetEmailService passwordResetEmailService;
	private final NotificationRecipientResolver recipientResolver;

	@Override
	public void processUserRegistered(UserRegisteredEvent event) {
		log.info("Processing UserRegisteredEvent for userId={}", event.getUserId());
		emailService.sendWelcomeEmail(event.getEmail(), event.getFirstName());
	}

	@Override
	public void processEmailVerification(EmailVerificationEvent event) {
		log.info("Processing EmailVerificationEvent");
		emailService.sendVerificationEmail(event.getEmail(), event.getEmail(), event.getOtp());
	}

	@Override
	public void processPasswordReset(PasswordResetEvent event) {
		log.info("Processing PasswordResetEvent");
		passwordResetEmailService.send(event.getEmail(), event.getResetLink());
	}

	@Override
	public void processMfaOtp(MfaOtpEvent event) {
		log.info("Processing MfaOtpEvent");
		emailService.sendOtpEmail(event.getEmail(), event.getOtp());
	}

	@Override
	public void sendOrganizationCreatedEmail(OrganizationCreatedEvent event) {
		log.info("Processing OrganizationCreatedEvent for organizationId={}", event.getOrganizationId());
		emailService.sendOrganizationCreatedEmail(event.getOwnerEmail(), event.getOrganizationName());
	}

	@Override
	public void processRoleAssigned(RoleAssignedEvent event) {
		if (event == null || event.getUserId() == null || event.getRoleName() == null || event.getRoleName().isBlank()) {
			log.warn("Skipping invalid RoleAssignedEvent");
			return;
		}

		log.info("Processing RoleAssignedEvent eventId={} userId={} role={}",
				event.getEventId(), event.getUserId(), event.getRoleName());

		recipientResolver.resolve(event.getUserId(), event.getEmail(), event.getFirstName())
				.ifPresentOrElse(
					recipient -> emailService.sendRoleAssignedEmail(
							recipient.email(),
							recipient.firstName(),
							event.getRoleName(),
							normalizeScopeType(event.getScopeType()),
							normalizeScopeId(event.getScopeType(), event.getScopeId())),
					() -> log.warn("Role assigned email skipped because recipient could not be resolved userId={}",
							event.getUserId()));
	}

	@Override
	public void processRoleRevoked(RoleRevokedEvent event) {
		if (event == null || event.getUserId() == null || event.getRoleName() == null || event.getRoleName().isBlank()) {
			log.warn("Skipping invalid RoleRevokedEvent");
			return;
		}

		log.info("Processing RoleRevokedEvent eventId={} userId={} role={}",
				event.getEventId(), event.getUserId(), event.getRoleName());

		recipientResolver.resolve(event.getUserId(), null, null)
				.ifPresentOrElse(
					recipient -> emailService.sendRoleRevokedEmail(
							recipient.email(),
							recipient.firstName(),
							event.getRoleName(),
							normalizeScopeType(event.getScopeType()),
							normalizeScopeId(event.getScopeType(), event.getScopeId())),
					() -> log.warn("Role revoked email skipped because recipient could not be resolved userId={}",
							event.getUserId()));
	}

	private String normalizeScopeType(String scopeType) {
		return scopeType == null || scopeType.isBlank() ? "PLATFORM" : scopeType.trim().toUpperCase();
	}

	private String normalizeScopeId(String scopeType, String scopeId) {
		String normalizedScopeType = normalizeScopeType(scopeType);
		if ("PLATFORM".equals(normalizedScopeType)) {
			return "*";
		}
		return scopeId == null || scopeId.isBlank() ? "unknown" : scopeId.trim();
	}
}
