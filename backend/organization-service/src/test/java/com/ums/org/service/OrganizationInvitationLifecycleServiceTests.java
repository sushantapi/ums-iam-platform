package com.ums.org.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import com.ums.events.event.AuditEvent;
import com.ums.events.publisher.AuditPublisher;
import com.ums.org.client.UserClient;
import com.ums.org.config.OrganizationInvitationProperties;
import com.ums.org.dto.OrganizationInvitationResponse;
import com.ums.org.dto.UserSummaryPageResponse;
import com.ums.org.dto.UserSummaryResponse;
import com.ums.org.entity.Organization;
import com.ums.org.entity.OrganizationInvitation;
import com.ums.org.enums.OrganizationInvitationStatus;
import com.ums.org.enums.OrganizationRole;
import com.ums.org.enums.OrganizationStatus;
import com.ums.org.exception.BadRequestException;
import com.ums.org.exception.ResourceNotFoundException;
import com.ums.org.publisher.OrganizationEventPublisher;
import com.ums.org.repositoty.OrganizationInvitationRepository;
import com.ums.org.repositoty.OrganizationMemberRepository;
import com.ums.org.repositoty.OrganizationRepository;
import com.ums.org.security.IssuedInvitationToken;
import com.ums.org.security.OrganizationInvitationTokenService;

@ExtendWith(MockitoExtension.class)
class OrganizationInvitationLifecycleServiceTests {

	private static final UUID ORGANIZATION_ID = UUID.fromString("00000000-0000-0000-0000-000000000010");
	private static final UUID ACTOR_ID = UUID.fromString("00000000-0000-0000-0000-000000000011");
	private static final UUID INVITATION_ID = UUID.fromString("00000000-0000-0000-0000-000000000012");
	private static final UUID NEW_INVITATION_ID = UUID.fromString("00000000-0000-0000-0000-000000000014");
	private static final UUID EXISTING_USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000013");
	private static final String EMAIL = "invitee@example.test";
	private static final String OLD_HASH = "a".repeat(64);
	private static final String NEW_HASH = "b".repeat(64);
	private static final String NEW_RAW_TOKEN = "new-raw-invitation-token";

	@Mock
	private OrganizationRepository organizationRepository;
	@Mock
	private OrganizationInvitationRepository invitationRepository;
	@Mock
	private OrganizationMemberRepository memberRepository;
	@Mock
	private OrganizationAccessService accessService;
	@Mock
	private UserClient userClient;
	@Mock
	private OrganizationInvitationTokenService invitationTokenService;
	@Mock
	private OrganizationInvitationProperties invitationProperties;
	@Mock
	private OrganizationEventPublisher eventPublisher;
	@Mock
	private AuditPublisher auditPublisher;

	private OrganizationInvitationLifecycleService service;
	private Organization organization;

	@BeforeEach
	void setUp() {
		service = new OrganizationInvitationLifecycleService(
				organizationRepository, invitationRepository, memberRepository, accessService, userClient,
				invitationTokenService, invitationProperties, eventPublisher, auditPublisher);
		organization = Organization.builder()
				.id(ORGANIZATION_ID)
				.name("Example Org")
				.slug("example-org")
				.ownerId(ACTOR_ID)
				.status(OrganizationStatus.ACTIVE)
				.build();
		when(organizationRepository.findById(ORGANIZATION_ID)).thenReturn(Optional.of(organization));
	}

	@Test
	void resendPendingRotatesTokenRefreshesExpiryAndSchedulesSecureDelivery() {
		OrganizationInvitation invitation = pendingInvitation(LocalDateTime.now().plusHours(24));
		when(invitationRepository.findScopedByIdForUpdate(ORGANIZATION_ID, INVITATION_ID))
				.thenReturn(Optional.of(invitation));
		when(userClient.getUsers(0, 200, EMAIL)).thenReturn(emptyUsers());
		when(invitationProperties.getExpiryHours()).thenReturn(72L);
		when(invitationTokenService.issue()).thenReturn(new IssuedInvitationToken(NEW_RAW_TOKEN, NEW_HASH));
		when(invitationRepository.saveAndFlush(invitation)).thenReturn(invitation);

		LocalDateTime before = LocalDateTime.now();
		OrganizationInvitationResponse response = service.resendInvitation(
				ORGANIZATION_ID, INVITATION_ID, ACTOR_ID, false);

		assertThat(invitation.getTokenHash()).isEqualTo(NEW_HASH).isNotEqualTo(OLD_HASH);
		assertThat(invitation.getExpiresAt()).isAfterOrEqualTo(before.plusHours(72));
		assertThat(response.id()).isEqualTo(INVITATION_ID);
		assertThat(response.status()).isEqualTo(OrganizationInvitationStatus.PENDING);
		verify(accessService).assertCanManageMembers(ACTOR_ID, organization, false);
		verify(invitationRepository).findScopedByIdForUpdate(ORGANIZATION_ID, INVITATION_ID);
		verify(eventPublisher).publishOrganizationInvitationAfterCommit(
				INVITATION_ID, EMAIL, "Example Org", NEW_RAW_TOKEN);

		ArgumentCaptor<AuditEvent> auditCaptor = ArgumentCaptor.forClass(AuditEvent.class);
		verify(auditPublisher).publish(auditCaptor.capture());
		AuditEvent audit = auditCaptor.getValue();
		assertThat(audit.getAction()).isEqualTo("ORGANIZATION_INVITATION_RESEND");
		assertThat(audit.getDetails()).doesNotContain(EMAIL, NEW_RAW_TOKEN, NEW_HASH, OLD_HASH);
	}

	@Test
	void resendAuditWaitsForCommitWhenTransactionSynchronizationIsActive() {
		OrganizationInvitation invitation = pendingInvitation(LocalDateTime.now().plusHours(24));
		when(invitationRepository.findScopedByIdForUpdate(ORGANIZATION_ID, INVITATION_ID))
				.thenReturn(Optional.of(invitation));
		when(userClient.getUsers(0, 200, EMAIL)).thenReturn(emptyUsers());
		when(invitationProperties.getExpiryHours()).thenReturn(72L);
		when(invitationTokenService.issue()).thenReturn(new IssuedInvitationToken(NEW_RAW_TOKEN, NEW_HASH));
		when(invitationRepository.saveAndFlush(invitation)).thenReturn(invitation);

		TransactionSynchronizationManager.initSynchronization();
		TransactionSynchronizationManager.setActualTransactionActive(true);
		try {
			service.resendInvitation(ORGANIZATION_ID, INVITATION_ID, ACTOR_ID, false);

			verify(auditPublisher, never()).publish(org.mockito.ArgumentMatchers.any(AuditEvent.class));
			TransactionSynchronizationManager.getSynchronizations()
					.forEach(synchronization -> synchronization.afterCommit());

			ArgumentCaptor<AuditEvent> auditCaptor = ArgumentCaptor.forClass(AuditEvent.class);
			verify(auditPublisher).publish(auditCaptor.capture());
			AuditEvent audit = auditCaptor.getValue();
			assertThat(audit.getEventType()).isEqualTo("organization.invitation.resent");
			assertThat(audit.getAction()).isEqualTo("ORGANIZATION_INVITATION_RESEND");
			assertThat(audit.getEntityId()).isEqualTo(INVITATION_ID.toString());
			assertThat(audit.getUserEmail()).isNull();
			assertThat(audit.getDetails()).doesNotContain(EMAIL, NEW_RAW_TOKEN, NEW_HASH, OLD_HASH);
		} finally {
			TransactionSynchronizationManager.setActualTransactionActive(false);
			TransactionSynchronizationManager.clearSynchronization();
		}
	}

	@Test
	void resendExpiredCreatesFreshInvitationWithoutRevivingOldRow() {
		OrganizationInvitation expired = pendingInvitation(LocalDateTime.now().minusMinutes(1));
		when(invitationRepository.findScopedByIdForUpdate(ORGANIZATION_ID, INVITATION_ID))
				.thenReturn(Optional.of(expired));
		when(userClient.getUsers(0, 200, EMAIL)).thenReturn(emptyUsers());
		when(invitationRepository.findByOrganizationIdAndNormalizedEmailAndStatus(
				ORGANIZATION_ID, EMAIL, OrganizationInvitationStatus.PENDING)).thenReturn(Optional.empty());
		when(invitationProperties.getExpiryHours()).thenReturn(72L);
		when(invitationTokenService.issue()).thenReturn(new IssuedInvitationToken(NEW_RAW_TOKEN, NEW_HASH));
		when(invitationRepository.saveAndFlush(org.mockito.ArgumentMatchers.any(OrganizationInvitation.class)))
				.thenAnswer(invocation -> {
					OrganizationInvitation saved = invocation.getArgument(0);
					if (saved != expired && saved.getId() == null) {
						ReflectionTestUtils.setField(saved, "id", NEW_INVITATION_ID);
					}
					return saved;
				});

		OrganizationInvitationResponse response = service.resendInvitation(
				ORGANIZATION_ID, INVITATION_ID, ACTOR_ID, false);

		assertThat(expired.getStatus()).isEqualTo(OrganizationInvitationStatus.EXPIRED);
		assertThat(expired.getActiveEmailKey()).isNull();
		assertThat(response.id()).isEqualTo(NEW_INVITATION_ID);
		assertThat(response.status()).isEqualTo(OrganizationInvitationStatus.PENDING);
		assertThat(response.role()).isEqualTo(OrganizationRole.MEMBER);
		assertThat(response.inviterId()).isEqualTo(ACTOR_ID);
		verify(eventPublisher).publishOrganizationInvitationAfterCommit(
				NEW_INVITATION_ID, EMAIL, "Example Org", NEW_RAW_TOKEN);
	}

	@Test
	void resendRejectsRevokedInvitationWithoutIssuingToken() {
		OrganizationInvitation invitation = pendingInvitation(LocalDateTime.now().plusHours(24));
		invitation.markRevoked(LocalDateTime.now());
		when(invitationRepository.findScopedByIdForUpdate(ORGANIZATION_ID, INVITATION_ID))
				.thenReturn(Optional.of(invitation));

		assertThatThrownBy(() -> service.resendInvitation(
				ORGANIZATION_ID, INVITATION_ID, ACTOR_ID, false))
				.isInstanceOf(BadRequestException.class)
				.hasMessageContaining("not resendable");

		verify(invitationTokenService, never()).issue();
		verify(eventPublisher, never()).publishOrganizationInvitationAfterCommit(
				org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.anyString(),
				org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.anyString());
	}

	@Test
	void resendRejectsTargetThatBecameOrganizationMember() {
		OrganizationInvitation invitation = pendingInvitation(LocalDateTime.now().plusHours(24));
		when(invitationRepository.findScopedByIdForUpdate(ORGANIZATION_ID, INVITATION_ID))
				.thenReturn(Optional.of(invitation));
		when(userClient.getUsers(0, 200, EMAIL)).thenReturn(new UserSummaryPageResponse(
				List.of(new UserSummaryResponse(EXISTING_USER_ID, "Invitee", "User", EMAIL)), 0, 200, 1, 1));
		when(memberRepository.existsByOrganizationIdAndUserId(ORGANIZATION_ID, EXISTING_USER_ID)).thenReturn(true);

		assertThatThrownBy(() -> service.resendInvitation(
				ORGANIZATION_ID, INVITATION_ID, ACTOR_ID, false))
				.isInstanceOf(BadRequestException.class)
				.hasMessageContaining("already belongs");

		verify(invitationTokenService, never()).issue();
	}

	@Test
	void revokePendingIsIdempotentAndAuditsOnlyActualTransition() {
		OrganizationInvitation invitation = pendingInvitation(LocalDateTime.now().plusHours(24));
		when(invitationRepository.findScopedByIdForUpdate(ORGANIZATION_ID, INVITATION_ID))
				.thenReturn(Optional.of(invitation));
		when(invitationRepository.saveAndFlush(invitation)).thenReturn(invitation);

		OrganizationInvitationResponse first = service.revokeInvitation(
				ORGANIZATION_ID, INVITATION_ID, ACTOR_ID, false);
		OrganizationInvitationResponse second = service.revokeInvitation(
				ORGANIZATION_ID, INVITATION_ID, ACTOR_ID, false);

		assertThat(first.status()).isEqualTo(OrganizationInvitationStatus.REVOKED);
		assertThat(second.status()).isEqualTo(OrganizationInvitationStatus.REVOKED);
		assertThat(invitation.getActiveEmailKey()).isNull();
		verify(invitationRepository, times(1)).saveAndFlush(invitation);
		verify(invitationRepository, times(2)).findScopedByIdForUpdate(ORGANIZATION_ID, INVITATION_ID);
		verify(auditPublisher, times(1)).publish(org.mockito.ArgumentMatchers.any(AuditEvent.class));
		verify(eventPublisher, never()).publishOrganizationInvitationAfterCommit(
				org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.anyString(),
				org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.anyString());
	}

	@Test
	void revokeTimeExpiredPendingMarksExpiredInsteadOfRevoked() {
		OrganizationInvitation invitation = pendingInvitation(LocalDateTime.now().minusMinutes(1));
		when(invitationRepository.findScopedByIdForUpdate(ORGANIZATION_ID, INVITATION_ID))
				.thenReturn(Optional.of(invitation));
		when(invitationRepository.saveAndFlush(invitation)).thenReturn(invitation);

		OrganizationInvitationResponse response = service.revokeInvitation(
				ORGANIZATION_ID, INVITATION_ID, ACTOR_ID, false);

		assertThat(response.status()).isEqualTo(OrganizationInvitationStatus.EXPIRED);
		assertThat(invitation.getActiveEmailKey()).isNull();
		verify(auditPublisher, never()).publish(org.mockito.ArgumentMatchers.any(AuditEvent.class));
	}

	@Test
	void invitationOutsideOrganizationIsHiddenAsNotFound() {
		when(invitationRepository.findScopedByIdForUpdate(ORGANIZATION_ID, INVITATION_ID))
				.thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.revokeInvitation(
				ORGANIZATION_ID, INVITATION_ID, ACTOR_ID, false))
				.isInstanceOf(ResourceNotFoundException.class)
				.hasMessageContaining("not found");
	}

	private OrganizationInvitation pendingInvitation(LocalDateTime expiresAt) {
		LocalDateTime issuedAt = expiresAt.isAfter(LocalDateTime.now())
				? LocalDateTime.now()
				: expiresAt.minusHours(1);
		OrganizationInvitation invitation = OrganizationInvitation.createPending(
				ORGANIZATION_ID, EMAIL, OrganizationRole.MEMBER, ACTOR_ID, OLD_HASH, expiresAt, issuedAt);
		ReflectionTestUtils.setField(invitation, "id", INVITATION_ID);
		return invitation;
	}

	private UserSummaryPageResponse emptyUsers() {
		return new UserSummaryPageResponse(List.of(), 0, 200, 0, 0);
	}
}