package com.ums.org.service;

import java.time.LocalDateTime;
import java.util.Locale;
import java.util.UUID;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import com.ums.events.event.AuditEvent;
import com.ums.events.publisher.AuditPublisher;
import com.ums.org.client.UserClient;
import com.ums.org.config.OrganizationInvitationProperties;
import com.ums.org.dto.OrganizationInvitationResponse;
import com.ums.org.dto.UserSummaryPageResponse;
import com.ums.org.entity.Organization;
import com.ums.org.entity.OrganizationInvitation;
import com.ums.org.enums.OrganizationInvitationStatus;
import com.ums.org.exception.BadRequestException;
import com.ums.org.exception.ResourceNotFoundException;
import com.ums.org.publisher.OrganizationEventPublisher;
import com.ums.org.repositoty.OrganizationInvitationRepository;
import com.ums.org.repositoty.OrganizationMemberRepository;
import com.ums.org.repositoty.OrganizationRepository;
import com.ums.org.security.IssuedInvitationToken;
import com.ums.org.security.OrganizationInvitationTokenService;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class OrganizationInvitationLifecycleService {

	private final OrganizationRepository organizationRepository;
	private final OrganizationInvitationRepository invitationRepository;
	private final OrganizationMemberRepository memberRepository;
	private final OrganizationAccessService accessService;
	private final UserClient userClient;
	private final OrganizationInvitationTokenService invitationTokenService;
	private final OrganizationInvitationProperties invitationProperties;
	private final OrganizationEventPublisher eventPublisher;
	private final AuditPublisher auditPublisher;

	public OrganizationInvitationResponse resendInvitation(UUID organizationId, UUID invitationId,
			UUID actorUserId, boolean superAdmin) {
		Organization organization = loadOrganization(organizationId);
		accessService.assertCanManageMembers(actorUserId, organization, superAdmin);
		OrganizationInvitation invitation = loadScopedInvitation(organizationId, invitationId);

		if (invitation.getStatus() == OrganizationInvitationStatus.ACCEPTED
				|| invitation.getStatus() == OrganizationInvitationStatus.REVOKED) {
			throw new BadRequestException("Invitation is not resendable");
		}
		if (targetAlreadyMember(organizationId, invitation.getNormalizedEmail())) {
			throw new BadRequestException("User already belongs to organization");
		}

		LocalDateTime now = LocalDateTime.now();
		if (invitation.isExpiredAt(now)) {
			invitation.markExpired(now);
			invitationRepository.saveAndFlush(invitation);
		}

		if (invitation.getStatus() == OrganizationInvitationStatus.EXPIRED) {
			return reissueExpiredInvitation(organization, invitation, actorUserId, now);
		}
		if (!invitation.isPending()) {
			throw new BadRequestException("Invitation is not resendable");
		}

		IssuedInvitationToken issuedToken = invitationTokenService.issue();
		invitation.rotateToken(issuedToken.tokenHash(), now.plusHours(invitationProperties.getExpiryHours()), now);
		OrganizationInvitation saved = invitationRepository.saveAndFlush(invitation);

		eventPublisher.publishOrganizationInvitationAfterCommit(saved.getId(), saved.getNormalizedEmail(),
				organization.getName(), issuedToken.rawToken());
		publishAudit("organization.invitation.resent", "ORGANIZATION_INVITATION_RESEND",
				actorUserId, saved.getId(), "Organization invitation resent");
		return toResponse(saved);
	}

	public OrganizationInvitationResponse revokeInvitation(UUID organizationId, UUID invitationId,
			UUID actorUserId, boolean superAdmin) {
		Organization organization = loadOrganization(organizationId);
		accessService.assertCanManageMembers(actorUserId, organization, superAdmin);
		OrganizationInvitation invitation = loadScopedInvitation(organizationId, invitationId);

		if (invitation.getStatus() == OrganizationInvitationStatus.REVOKED
				|| invitation.getStatus() == OrganizationInvitationStatus.EXPIRED) {
			return toResponse(invitation);
		}
		if (invitation.getStatus() == OrganizationInvitationStatus.ACCEPTED) {
			throw new BadRequestException("Accepted invitation cannot be revoked");
		}

		LocalDateTime now = LocalDateTime.now();
		if (invitation.isExpiredAt(now)) {
			invitation.markExpired(now);
			return toResponse(invitationRepository.saveAndFlush(invitation));
		}

		invitation.markRevoked(now);
		OrganizationInvitation saved = invitationRepository.saveAndFlush(invitation);
		publishAudit("organization.invitation.revoked", "ORGANIZATION_INVITATION_REVOKE",
				actorUserId, saved.getId(), "Organization invitation revoked");
		return toResponse(saved);
	}

	private OrganizationInvitationResponse reissueExpiredInvitation(Organization organization,
			OrganizationInvitation expiredInvitation, UUID actorUserId, LocalDateTime now) {
		invitationRepository.findByOrganizationIdAndNormalizedEmailAndStatus(
				organization.getId(), expiredInvitation.getNormalizedEmail(), OrganizationInvitationStatus.PENDING)
				.ifPresent(existing -> {
					throw new BadRequestException("A pending invitation already exists for this email");
				});

		IssuedInvitationToken issuedToken = invitationTokenService.issue();
		OrganizationInvitation replacement = OrganizationInvitation.createPending(
				organization.getId(), expiredInvitation.getNormalizedEmail(), expiredInvitation.getRole(), actorUserId,
				issuedToken.tokenHash(), now.plusHours(invitationProperties.getExpiryHours()), now);

		try {
			OrganizationInvitation saved = invitationRepository.saveAndFlush(replacement);
			eventPublisher.publishOrganizationInvitationAfterCommit(saved.getId(), saved.getNormalizedEmail(),
					organization.getName(), issuedToken.rawToken());
			publishAudit("organization.invitation.resent", "ORGANIZATION_INVITATION_RESEND",
					actorUserId, saved.getId(), "Organization invitation reissued from expired invitation "
							+ expiredInvitation.getId());
			return toResponse(saved);
		} catch (DataIntegrityViolationException ex) {
			throw new BadRequestException("A pending invitation already exists for this email");
		}
	}

	private Organization loadOrganization(UUID organizationId) {
		return organizationRepository.findById(organizationId)
				.orElseThrow(() -> new ResourceNotFoundException("Organization not found"));
	}

	private OrganizationInvitation loadScopedInvitation(UUID organizationId, UUID invitationId) {
		OrganizationInvitation invitation = invitationRepository.findById(invitationId)
				.orElseThrow(() -> new ResourceNotFoundException("Organization invitation not found"));
		if (!organizationId.equals(invitation.getOrganizationId())) {
			throw new ResourceNotFoundException("Organization invitation not found");
		}
		return invitation;
	}

	private boolean targetAlreadyMember(UUID organizationId, String normalizedEmail) {
		UserSummaryPageResponse users = userClient.getUsers(0, 200, normalizedEmail);
		if (users == null || users.content() == null || users.content().isEmpty()) {
			return false;
		}
		return users.content().stream()
				.filter(user -> user != null && user.id() != null && user.email() != null)
				.filter(user -> normalizedEmail.equals(user.email().trim().toLowerCase(Locale.ROOT)))
				.anyMatch(user -> memberRepository.existsByOrganizationIdAndUserId(organizationId, user.id()));
	}

	private void publishAudit(String eventType, String action, UUID actorUserId, UUID invitationId, String details) {
		try {
			auditPublisher.publish(AuditEvent.builder()
					.eventType(eventType)
					.serviceName("organization-service")
					.userId(actorUserId.toString())
					.action(action)
					.entityType("ORGANIZATION_INVITATION")
					.entityId(invitationId.toString())
					.details(details)
					.timestamp(LocalDateTime.now())
					.build());
		} catch (RuntimeException ex) {
			log.warn("Organization invitation audit publication failed action={} invitationId={} failureType={}",
					action, invitationId, ex.getClass().getName());
		}
	}

	private OrganizationInvitationResponse toResponse(OrganizationInvitation invitation) {
		return new OrganizationInvitationResponse(
				invitation.getId(), invitation.getOrganizationId(), invitation.getNormalizedEmail(), invitation.getRole(),
				invitation.getStatus(), invitation.getInviterId(), invitation.getExpiresAt(), invitation.getLastSentAt(),
				invitation.getCreatedAt());
	}
}
