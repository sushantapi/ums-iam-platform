package com.ums.org.publisher;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.util.UriComponentsBuilder;

import com.ums.events.constants.RabbitMQConstants;
import com.ums.events.event.organization.OrganizationCreatedEvent;
import com.ums.events.event.organization.OrganizationInviteEvent;
import com.ums.org.config.OrganizationInvitationProperties;
import com.ums.org.service.OrganizationInvitationDeliveryStateService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
@RequiredArgsConstructor
public class OrganizationEventPublisher {

	private final RabbitTemplate rabbitTemplate;
	private final OrganizationInvitationProperties invitationProperties;
	private final OrganizationInvitationDeliveryStateService invitationDeliveryStateService;

	public void publishOrganizationCreated(OrganizationCreatedEvent event) {

		log.info("Publishing OrganizationCreatedEvent: {}", event);

		rabbitTemplate.convertAndSend(RabbitMQConstants.ORGANIZATION_EXCHANGE,
				RabbitMQConstants.ORGANIZATION_CREATED_ROUTING_KEY, event);
	}

	public void publishOrganizationInvitationAfterCommit(UUID invitationId, String email, String organizationName,
			String rawToken) {
		Objects.requireNonNull(invitationId, "invitationId is required");
		if (email == null || email.isBlank()) {
			throw new IllegalArgumentException("Invitation email is required");
		}
		if (rawToken == null || rawToken.isBlank()) {
			throw new IllegalArgumentException("Invitation token is required");
		}
		if (!TransactionSynchronizationManager.isSynchronizationActive()
				|| !TransactionSynchronizationManager.isActualTransactionActive()) {
			throw new IllegalStateException("Organization invitation publication requires an active transaction");
		}

		TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
			@Override
			public void afterCommit() {
				dispatchOrganizationInvitation(invitationId, email, organizationName, rawToken);
			}
		});
	}

	private void dispatchOrganizationInvitation(UUID invitationId, String email, String organizationName,
			String rawToken) {
		LocalDateTime dispatchedAt = LocalDateTime.now();
		try {
			String inviteLink = UriComponentsBuilder.fromUriString(invitationProperties.getAcceptPageUrl())
					.fragment("token=" + rawToken)
					.build()
					.encode()
					.toUriString();
			OrganizationInviteEvent event = new OrganizationInviteEvent(email, organizationName, inviteLink);

			rabbitTemplate.convertAndSend(RabbitMQConstants.ORGANIZATION_EXCHANGE,
					RabbitMQConstants.ORGANIZATION_INVITATION_ROUTING_KEY, event);
			log.info("Organization invitation notification dispatched invitationId={}", invitationId);
		} catch (RuntimeException ex) {
			log.warn("Organization invitation notification dispatch failed invitationId={} failureType={}",
					invitationId, ex.getClass().getName());
			return;
		}

		try {
			invitationDeliveryStateService.markNotificationSent(invitationId, dispatchedAt);
		} catch (RuntimeException ex) {
			log.warn("Organization invitation sent-state update failed invitationId={} failureType={}",
					invitationId, ex.getClass().getName());
		}
	}
}
