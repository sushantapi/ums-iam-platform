package com.ums.notification.service.impl;

import org.springframework.stereotype.Service;

import com.ums.events.event.organization.OrganizationCreatedEvent;
import com.ums.events.event.user.UserRegisteredEvent;
import com.ums.notification.event.EmailVerificationEvent;
import com.ums.notification.event.MfaOtpEvent;
import com.ums.notification.event.PasswordResetEvent;
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

		log.info("Processing UserRegisteredEvent: {}", event.getEmail());

		emailService.sendWelcomeEmail(event.getEmail(), event.getFirstName());
	}

	@Override
	public void processEmailVerification(EmailVerificationEvent event) {

		log.info("Processing EmailVerificationEvent: {}", event.getEmail());

		emailService.sendVerificationEmail(event.getEmail(), event.getFirstName(), event.getVerificationLink());
	}

	@Override
	public void processPasswordReset(PasswordResetEvent event) {

		log.info("Processing PasswordResetEvent: {}", event.getEmail());

		emailService.sendPasswordResetEmail(event.getEmail(), event.getFirstName(), event.getResetLink());
	}

	@Override
	public void processMfaOtp(MfaOtpEvent event) {

		log.info("Processing MfaOtpEvent: {}", event.getEmail());

		emailService.sendOtpEmail(event.getEmail(), event.getOtp());
	}

	@Override
	public void sendOrganizationCreatedEmail(OrganizationCreatedEvent event) {

		log.info("Processing OrganizationCreatedEvent : {}", event.getOrganizationName());

		emailService.sendOrganizationCreatedEmail(event.getOwnerEmail(), event.getOrganizationName());
	}
}