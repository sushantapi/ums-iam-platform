package com.ums.notification.service.impl;

import org.springframework.stereotype.Service;

import com.ums.events.event.EmailVerificationEvent;
import com.ums.events.event.MfaOtpEvent;
import com.ums.events.event.PasswordResetEvent;
import com.ums.events.event.organization.OrganizationCreatedEvent;
import com.ums.events.event.role.RoleAssignedEvent;
import com.ums.events.event.user.UserRegisteredEvent;
import com.ums.notification.service.EmailService;
import com.ums.notification.service.NotificationService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

	private final EmailService emailService;

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

		emailService.sendPasswordResetEmail(event.getEmail(), event.getEmail(), event.getOtp());
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

		if (event.getEmail() == null || event.getEmail().isBlank()) {
			log.warn("Skipping RoleAssignedEvent notification because recipient email is missing for userId={}",
					event.getUserId());
			return;
		}

		log.info("Processing RoleAssignedEvent for userId={} roleName={}", event.getUserId(), event.getRoleName());

		emailService.sendRoleAssignedEmail(event.getEmail(), event.getFirstName(), event.getRoleName());
	}
}
