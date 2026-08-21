package com.ums.org.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.util.ReflectionTestUtils;

import com.ums.events.event.AuditEvent;
import com.ums.events.publisher.AuditPublisher;
import com.ums.org.client.UserClient;
import com.ums.org.dto.AcceptOrganizationInvitationRequest;
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

@ExtendWith(MockitoExtension.class)
class OrganizationInvitationAcceptanceServiceTests {

	private static final UUID ORGANIZATION_ID = UUID.fromString("00000000-0000-0000-0000-000000000010");
	private static final UUID ACTOR_ID = UUID.fromString("00000000-0000-0000-0000-000000000011");
	private static final UUID OTHER_USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000012");
	private static final UUID INVITATION_ID = UUID.fromString("00000000-0000-0000-0000-000000000013");
	private static final UUID MEMBERSHIP_ID = UUID.fromString("00000000-0000-0000-0000-000000000014");
	private static final String EMAIL = "invitee@example.test";
	private static final String RAW_TOKEN = "raw-invitation-token-must-not-be-persisted";
	private static final String TOKEN_HASH = "a".repeat(64);

	@Mock
	private OrganizationInvitationRepository invitationRepository;
	@Mock
	private OrganizationRepository organizationRepository;
	@Mock
	private OrganizationMemberRepository memberRepository;
	@Mock
	private UserClient userClient;
	@Mock
	private OrganizationInvitationTokenService invitationTokenService;
	@Mock
	private AuditPublisher auditPublisher;

	private OrganizationInvitationAcceptanceService service;

	@BeforeEach
	void setUp() {
		service = new OrganizationInvitationAcceptanceService(
				invitationRepository, organizationRepository, memberRepository, userClient,
				invitationTokenService, auditPublisher);
	}

	@Test
	void matchingAuthenticatedUserAcceptsExactlyOnceAndCreatesMembership() {
		OrganizationInvitation invitation = pendingInvitation(TOKEN_HASH, LocalDateTime.now().plusHours(24));
		Organization organization = organization(OrganizationStatus.ACTIVE);
		when(userClient.getUser(ACTOR_ID)).thenReturn(user(ACTOR_ID, "Invitee@Example.Test"));
		when(invitationTokenService.hash(RAW_TOKEN)).thenReturn(TOKEN_HASH);
		when(invitationRepository.findByTokenHashForUpdate(TOKEN_HASH)).thenReturn(Optional.of(invitation));
		when(organizationRepository.findById(ORGANIZATION_ID)).thenReturn(Optional.of(organization));
		when(memberRepository.existsByOrganizationIdAndUserId(ORGANIZATION_ID, ACTOR_ID)).thenReturn(false);
		when(memberRepository.saveAndFlush(any(OrganizationMember.class))).thenAnswer(invocation -> {
			OrganizationMember member = invocation.getArgument(0);
			member.setId(MEMBERSHIP_ID);
			return member;
		});
		when(invitationRepository.saveAndFlush(any(OrganizationInvitation.class)))
				.thenAnswer(invocation -> invocation.getArgument(0));

		OrganizationInvitationAcceptanceResponse response = service.acceptInvitation(RAW_TOKEN, ACTOR_ID);

		assertThat(response.invitationId()).isEqualTo(INVITATION_ID);
		assertThat(response.organizationId()).isEqualTo(ORGANIZATION_ID);
		assertThat(response.membershipId()).isEqualTo(MEMBERSHIP_ID);
		assertThat(response.role()).isEqualTo(OrganizationRole.MEMBER);
		assertThat(response.status()).isEqualTo(OrganizationInvitationStatus.ACCEPTED);
		assertThat(response.acceptedAt()).isNotNull();
		assertThat(invitation.getStatus()).isEqualTo(OrganizationInvitationStatus.ACCEPTED);
		assertThat(invitation.getActiveEmailKey()).isNull();
		assertThat(invitation.getTokenHash()).isEqualTo(TOKEN_HASH).doesNotContain(RAW_TOKEN);
		verify(invitationRepository).findByTokenHashForUpdate(TOKEN_HASH);

		ArgumentCaptor<OrganizationMember> memberCaptor = ArgumentCaptor.forClass(OrganizationMember.class);
		verify(memberRepository).saveAndFlush(memberCaptor.capture());
		assertThat(memberCaptor.getValue().getOrganizationId()).isEqualTo(ORGANIZATION_ID);
		assertThat(memberCaptor.getValue().getUserId()).isEqualTo(ACTOR_ID);
		assertThat(memberCaptor.getValue().getRole()).isEqualTo(OrganizationRole.MEMBER);

		ArgumentCaptor<AuditEvent> auditCaptor = ArgumentCaptor.forClass(AuditEvent.class);
		verify(auditPublisher).publish(auditCaptor.capture());
		AuditEvent auditEvent = auditCaptor.getValue();
		assertThat(auditEvent.getAction()).isEqualTo("ORGANIZATION_INVITATION_ACCEPT");
		assertThat(auditEvent.getEntityId()).isEqualTo(INVITATION_ID.toString());
		assertThat(auditEvent.getUserEmail()).isNull();
		assertThat(auditEvent.getDetails()).doesNotContain(EMAIL, RAW_TOKEN, TOKEN_HASH);
	}

	@Test
	void acceptanceRequestStringRepresentationRedactsBearerToken() {
		AcceptOrganizationInvitationRequest request = new AcceptOrganizationInvitationRequest(RAW_TOKEN);
		assertThat(request.toString()).doesNotContain(RAW_TOKEN).contains("<redacted>");
	}

	@Test
	void tamperedTokenIsRejectedWithoutMembershipMutation() {
		String tamperedHash = "b".repeat(64);
		when(userClient.getUser(ACTOR_ID)).thenReturn(user(ACTOR_ID, EMAIL));
		when(invitationTokenService.hash("tampered-token")).thenReturn(tamperedHash);
		when(invitationRepository.findByTokenHashForUpdate(tamperedHash)).thenReturn(Optional.empty());

		assertRejected(() -> service.acceptInvitation("tampered-token", ACTOR_ID));

		verify(memberRepository, never()).saveAndFlush(any(OrganizationMember.class));
		verify(auditPublisher, never()).publish(any());
	}

	@Test
	void wrongAuthenticatedEmailIsRejected() {
		OrganizationInvitation invitation = pendingInvitation(TOKEN_HASH, LocalDateTime.now().plusHours(24));
		stubInvitationLookup(invitation);
		when(userClient.getUser(ACTOR_ID)).thenReturn(user(ACTOR_ID, "other@example.test"));

		assertRejected(() -> service.acceptInvitation(RAW_TOKEN, ACTOR_ID));

		verify(organizationRepository, never()).findById(any());
		verify(memberRepository, never()).saveAndFlush(any(OrganizationMember.class));
	}

	@Test
	void expiredInvitationIsRejected() {
		OrganizationInvitation invitation = pendingInvitation(TOKEN_HASH, LocalDateTime.now().minusMinutes(1));
		stubMatchingUserAndInvitation(invitation);

		assertRejected(() -> service.acceptInvitation(RAW_TOKEN, ACTOR_ID));

		assertThat(invitation.getStatus()).isEqualTo(OrganizationInvitationStatus.PENDING);
		verify(memberRepository, never()).saveAndFlush(any(OrganizationMember.class));
	}

	@Test
	void revokedInvitationIsRejected() {
		OrganizationInvitation invitation = pendingInvitation(TOKEN_HASH, LocalDateTime.now().plusHours(24));
		invitation.markRevoked(LocalDateTime.now());
		stubMatchingUserAndInvitation(invitation);

		assertRejected(() -> service.acceptInvitation(RAW_TOKEN, ACTOR_ID));

		verify(memberRepository, never()).saveAndFlush(any(OrganizationMember.class));
	}

	@Test
	void acceptedInvitationReplayIsRejected() {
		OrganizationInvitation invitation = pendingInvitation(TOKEN_HASH, LocalDateTime.now().plusHours(24));
		invitation.markAccepted(LocalDateTime.now());
		stubMatchingUserAndInvitation(invitation);

		assertRejected(() -> service.acceptInvitation(RAW_TOKEN, ACTOR_ID));

		verify(memberRepository, never()).saveAndFlush(any(OrganizationMember.class));
	}

	@Test
	void inactiveOrganizationIsRejected() {
		OrganizationInvitation invitation = pendingInvitation(TOKEN_HASH, LocalDateTime.now().plusHours(24));
		stubMatchingUserAndInvitation(invitation);
		when(organizationRepository.findById(ORGANIZATION_ID))
				.thenReturn(Optional.of(organization(OrganizationStatus.SUSPENDED)));

		assertRejected(() -> service.acceptInvitation(RAW_TOKEN, ACTOR_ID));

		verify(memberRepository, never()).saveAndFlush(any(OrganizationMember.class));
	}

	@Test
	void existingMembershipIsRejected() {
		OrganizationInvitation invitation = pendingInvitation(TOKEN_HASH, LocalDateTime.now().plusHours(24));
		stubMatchingUserAndInvitation(invitation);
		when(organizationRepository.findById(ORGANIZATION_ID))
				.thenReturn(Optional.of(organization(OrganizationStatus.ACTIVE)));
		when(memberRepository.existsByOrganizationIdAndUserId(ORGANIZATION_ID, ACTOR_ID)).thenReturn(true);

		assertRejected(() -> service.acceptInvitation(RAW_TOKEN, ACTOR_ID));

		verify(memberRepository, never()).saveAndFlush(any(OrganizationMember.class));
	}

	@Test
	void canonicalUserIdentityMismatchIsRejectedBeforeTokenLookup() {
		when(userClient.getUser(ACTOR_ID)).thenReturn(user(OTHER_USER_ID, EMAIL));

		assertRejected(() -> service.acceptInvitation(RAW_TOKEN, ACTOR_ID));

		verify(invitationTokenService, never()).hash(any());
		verify(invitationRepository, never()).findByTokenHashForUpdate(any());
	}

	@Test
	void membershipUniquenessRaceIsRejectedWithoutConsumingInvitation() {
		OrganizationInvitation invitation = pendingInvitation(TOKEN_HASH, LocalDateTime.now().plusHours(24));
		stubMatchingUserAndInvitation(invitation);
		when(organizationRepository.findById(ORGANIZATION_ID))
				.thenReturn(Optional.of(organization(OrganizationStatus.ACTIVE)));
		when(memberRepository.existsByOrganizationIdAndUserId(ORGANIZATION_ID, ACTOR_ID)).thenReturn(false);
		when(memberRepository.saveAndFlush(any(OrganizationMember.class)))
				.thenThrow(new DataIntegrityViolationException("duplicate membership"));

		assertRejected(() -> service.acceptInvitation(RAW_TOKEN, ACTOR_ID));

		assertThat(invitation.getStatus()).isEqualTo(OrganizationInvitationStatus.PENDING);
		verify(invitationRepository, never()).saveAndFlush(any(OrganizationInvitation.class));
		verify(auditPublisher, never()).publish(any());
	}

	private void stubInvitationLookup(OrganizationInvitation invitation) {
		when(invitationTokenService.hash(RAW_TOKEN)).thenReturn(TOKEN_HASH);
		when(invitationRepository.findByTokenHashForUpdate(TOKEN_HASH)).thenReturn(Optional.of(invitation));
	}

	private void stubMatchingUserAndInvitation(OrganizationInvitation invitation) {
		when(userClient.getUser(ACTOR_ID)).thenReturn(user(ACTOR_ID, "Invitee@Example.Test"));
		stubInvitationLookup(invitation);
	}

	private OrganizationInvitation pendingInvitation(String tokenHash, LocalDateTime expiresAt) {
		LocalDateTime issuedAt = expiresAt.minusHours(1);
		OrganizationInvitation invitation = OrganizationInvitation.createPending(
				ORGANIZATION_ID, EMAIL, OrganizationRole.MEMBER, OTHER_USER_ID, tokenHash, expiresAt, issuedAt);
		ReflectionTestUtils.setField(invitation, "id", INVITATION_ID);
		return invitation;
	}

	private Organization organization(OrganizationStatus status) {
		return Organization.builder()
				.id(ORGANIZATION_ID)
				.name("Example Org")
				.slug("example-org")
				.ownerId(OTHER_USER_ID)
				.status(status)
				.build();
	}

	private UserResponse user(UUID id, String email) {
		return new UserResponse(id, email, "Invitee", "User");
	}

	private void assertRejected(org.assertj.core.api.ThrowableAssert.ThrowingCallable callable) {
		assertThatThrownBy(callable)
				.isInstanceOf(BadRequestException.class)
				.hasMessage("Invitation cannot be accepted");
	}
}
