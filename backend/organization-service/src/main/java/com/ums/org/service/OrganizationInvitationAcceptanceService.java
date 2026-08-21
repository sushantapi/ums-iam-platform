package com.ums.org.service;

import java.time.LocalDateTime;
import java.util.Locale;
import java.util.UUID;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import com.ums.events.event.AuditEvent;
import com.ums.events.publisher.AuditPublisher;
import com.ums.org.client.UserClient;
import com.ums.org.dto.OrganizationInvitationAcceptanceResponse;
import com.ums.org.dto.UserResponse;
import com.ums.org.entity.Organization;
import com.ums.org.entity.OrganizationInvitation;
import com.ums.org.entity.OrganizationMember;
import com.ums.org.enums.OrganizationInvitationStatus;
import com.ums.org.enums.OrganizationRole;
import com.ums.org.enums.OrganizationStatus;
import com.ums.org.exception.BadRequestException;
import com.ums.org.repositoty.OrganizationInvitationRepository;
import com.ums.org.repositoty.OrganizationMemberRepository;
import com.ums.org.repositoty.OrganizationRepository;
import com.ums.org.security.OrganizationInvitationTokenService;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class OrganizationInvitationAcceptanceService {

	private static final String INVALID_INVITATION_MESSAGE = "Invitation cannot be accepted";

	private final OrganizationInvitationRepository invitationRepository;
	private final OrganizationRepository organizationRepository;
	private final OrganizationMemberRepository memberRepository;
	private final UserClient userClient;
	private final OrganizationInvitationTokenService invitationTokenService;
	private final AuditPublisher auditPublisher;

	public OrganizationInvitationAcceptanceResponse acceptInvitation(String rawToken, UUID actorUserId) {
		if (actorUserId == null) {
			throw invalidInvitation();
		}

		UserResponse authenticatedUser = userClient.getUser(actorUserId);
		String authenticatedEmail = canonicalEmail(authenticatedUser, actorUserId);
		String tokenHash = hashToken(rawToken);

		OrganizationInvitation invitation = invitationRepository.findByTokenHashForUpdate(tokenHash)
				.orElseThrow(this::invalidInvitation);
		LocalDateTime now = LocalDateTime.now();

		if (!invitation.isPending() || invitation.isExpiredAt(now)
				|| invitation.getRole() == OrganizationRole.OWNER) {
			throw invalidInvitation();
		}
		if (!invitation.getNormalizedEmail().equals(authenticatedEmail)) {
			throw invalidInvitation();
		}

		Organization organization = organizationRepository.findById(invitation.getOrganizationId())
				.orElseThrow(this::invalidInvitation);
		if (organization.getStatus() != OrganizationStatus.ACTIVE) {
			throw invalidInvitation();
		}
		if (memberRepository.existsByOrganizationIdAndUserId(organization.getId(), actorUserId)) {
			throw invalidInvitation();
		}

		OrganizationMember member = OrganizationMember.builder()
				.organizationId(organization.getId())
				.userId(actorUserId)
				.role(invitation.getRole())
				.joinedAt(now)
				.build();

		try {
			OrganizationMember savedMember = memberRepository.saveAndFlush(member);
			invitation.markAccepted(now);
			OrganizationInvitation savedInvitation = invitationRepository.saveAndFlush(invitation);

			registerAcceptanceAuditAfterCommit(actorUserId, savedInvitation, organization.getId());
			return new OrganizationInvitationAcceptanceResponse(
					savedInvitation.getId(), organization.getId(), savedMember.getId(), savedInvitation.getRole(),
					savedInvitation.getStatus(), savedInvitation.getAcceptedAt());
		} catch (DataIntegrityViolationException ex) {
			throw invalidInvitation();
		}
	}

	private String hashToken(String rawToken) {
		try {
			return invitationTokenService.hash(rawToken);
		} catch (IllegalArgumentException ex) {
			throw invalidInvitation();
		}
	}

	private String canonicalEmail(UserResponse user, UUID actorUserId) {
		if (user == null || user.id() == null || !actorUserId.equals(user.id())
				|| user.email() == null || user.email().isBlank()) {
			throw invalidInvitation();
		}
		return user.email().trim().toLowerCase(Locale.ROOT);
	}

	private void registerAcceptanceAuditAfterCommit(UUID actorUserId, OrganizationInvitation invitation,
			UUID organizationId) {
		AuditEvent auditEvent = AuditEvent.builder()
				.eventType("organization.invitation.accepted")
				.serviceName("organization-service")
				.userId(actorUserId.toString())
				.action("ORGANIZATION_INVITATION_ACCEPT")
				.entityType("ORGANIZATION_INVITATION")
				.entityId(invitation.getId().toString())
				.details("Organization invitation accepted for organization " + organizationId)
				.timestamp(LocalDateTime.now())
				.build();

		if (TransactionSynchronizationManager.isSynchronizationActive()
				&& TransactionSynchronizationManager.isActualTransactionActive()) {
			TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
				@Override
				public void afterCommit() {
					publishAuditSafely(auditEvent, invitation.getId());
				}
			});
			return;
		}

		publishAuditSafely(auditEvent, invitation.getId());
	}

	private void publishAuditSafely(AuditEvent event, UUID invitationId) {
		try {
			auditPublisher.publish(event);
		} catch (RuntimeException ex) {
			log.warn("Organization invitation acceptance audit publication failed invitationId={} failureType={}",
					invitationId, ex.getClass().getName());
		}
	}

	private BadRequestException invalidInvitation() {
		return new BadRequestException(INVALID_INVITATION_MESSAGE);
	}
}
