package com.ums.org.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.ums.events.publisher.AuditPublisher;
import com.ums.org.client.UserClient;
import com.ums.org.config.OrganizationInvitationProperties;
import com.ums.org.dto.CreateOrganizationInvitationRequest;
import com.ums.org.dto.OrganizationInvitationResponse;
import com.ums.org.dto.UserSummaryPageResponse;
import com.ums.org.dto.UserSummaryResponse;
import com.ums.org.entity.Organization;
import com.ums.org.entity.OrganizationInvitation;
import com.ums.org.enums.OrganizationInvitationStatus;
import com.ums.org.enums.OrganizationRole;
import com.ums.org.enums.OrganizationStatus;
import com.ums.org.exception.BadRequestException;
import com.ums.org.publisher.OrganizationEventPublisher;
import com.ums.org.repositoty.OrganizationInvitationRepository;
import com.ums.org.repositoty.OrganizationMemberRepository;
import com.ums.org.repositoty.OrganizationRepository;
import com.ums.org.security.IssuedInvitationToken;
import com.ums.org.security.OrganizationInvitationTokenService;
import com.ums.org.service.OrganizationAccessService;

@ExtendWith(MockitoExtension.class)
class OrganizationInvitationServiceTests {

	private static final UUID ORGANIZATION_ID = UUID.fromString("00000000-0000-0000-0000-000000000010");
	private static final UUID ACTOR_ID = UUID.fromString("00000000-0000-0000-0000-000000000011");
	private static final UUID INVITATION_ID = UUID.fromString("00000000-0000-0000-0000-000000000012");
	private static final UUID EXISTING_USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000013");
	private static final String EMAIL = "invitee@example.test";
	private static final String TOKEN_HASH = "a".repeat(64);
	private static final String RAW_TOKEN = "raw-invitation-token-must-not-be-persisted";

	@Mock
	private OrganizationRepository organizationRepository;

	@Mock
	private OrganizationMemberRepository memberRepository;

	@Mock
	private OrganizationInvitationRepository invitationRepository;

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

	@InjectMocks
	private OrganizationServiceImpl service;

	private Organization organization;

	@BeforeEach
	void setUp() {
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
	void createInvitationNormalizesEmailAndPersistsOnlyTokenHash() {
		when(invitationRepository.findByOrganizationIdAndNormalizedEmailAndStatus(
				ORGANIZATION_ID, EMAIL, OrganizationInvitationStatus.PENDING)).thenReturn(Optional.empty());
		when(userClient.getUsers(0, 200, EMAIL)).thenReturn(emptyUsers());
		when(invitationProperties.getExpiryHours()).thenReturn(72L);
		when(invitationTokenService.issue()).thenReturn(new IssuedInvitationToken(RAW_TOKEN, TOKEN_HASH));
		when(invitationRepository.saveAndFlush(any(OrganizationInvitation.class))).thenAnswer(invocation -> {
			OrganizationInvitation invitation = invocation.getArgument(0);
			ReflectionTestUtils.setField(invitation, "id", INVITATION_ID);
			return invitation;
		});

		LocalDateTime before = LocalDateTime.now();
		OrganizationInvitationResponse response = service.createInvitation(
				ORGANIZATION_ID,
				new CreateOrganizationInvitationRequest("Invitee@Example.Test", OrganizationRole.MEMBER),
				ACTOR_ID,
				false);

		ArgumentCaptor<OrganizationInvitation> invitationCaptor = ArgumentCaptor.forClass(OrganizationInvitation.class);
		verify(invitationRepository).saveAndFlush(invitationCaptor.capture());
		OrganizationInvitation saved = invitationCaptor.getValue();

		assertThat(saved.getNormalizedEmail()).isEqualTo(EMAIL);
		assertThat(saved.getTokenHash()).isEqualTo(TOKEN_HASH).doesNotContain(RAW_TOKEN);
		assertThat(saved.getStatus()).isEqualTo(OrganizationInvitationStatus.PENDING);
		assertThat(saved.getLastSentAt()).isNull();
		assertThat(saved.getExpiresAt()).isAfterOrEqualTo(before.plusHours(72));
		assertThat(response.id()).isEqualTo(INVITATION_ID);
		assertThat(response.email()).isEqualTo(EMAIL);
		assertThat(response.status()).isEqualTo(OrganizationInvitationStatus.PENDING);

		Set<String> responseFields = Stream.of(OrganizationInvitationResponse.class.getRecordComponents())
				.map(component -> component.getName())
				.collect(Collectors.toSet());
		assertThat(responseFields).doesNotContain("rawToken", "token", "tokenHash", "inviteLink", "activeEmailKey");
		verify(accessService).assertCanManageMembers(ACTOR_ID, organization, false);
	}

	@Test
	void createInvitationRejectsOwnerRoleBeforeIssuingToken() {
		assertThatThrownBy(() -> service.createInvitation(
				ORGANIZATION_ID,
				new CreateOrganizationInvitationRequest(EMAIL, OrganizationRole.OWNER),
				ACTOR_ID,
				false))
				.isInstanceOf(BadRequestException.class)
				.hasMessageContaining("ownership transfer");

		verify(invitationTokenService, never()).issue();
		verify(userClient, never()).getUsers(anyInt(), anyInt(), anyString());
	}

	@Test
	void createInvitationRejectsActivePendingInvitation() {
		LocalDateTime now = LocalDateTime.now();
		OrganizationInvitation existing = OrganizationInvitation.createPending(
				ORGANIZATION_ID, EMAIL, OrganizationRole.MEMBER, ACTOR_ID, TOKEN_HASH,
				now.plusHours(24), now.minusMinutes(1));
		when(invitationRepository.findByOrganizationIdAndNormalizedEmailAndStatus(
				ORGANIZATION_ID, EMAIL, OrganizationInvitationStatus.PENDING)).thenReturn(Optional.of(existing));

		assertThatThrownBy(() -> service.createInvitation(
				ORGANIZATION_ID,
				new CreateOrganizationInvitationRequest(EMAIL, OrganizationRole.MEMBER),
				ACTOR_ID,
				false))
				.isInstanceOf(BadRequestException.class)
				.hasMessageContaining("pending invitation");

		verify(invitationTokenService, never()).issue();
	}

	@Test
	void createInvitationExpiresStalePendingInvitationBeforeCreatingReplacement() {
		LocalDateTime now = LocalDateTime.now();
		OrganizationInvitation stale = OrganizationInvitation.createPending(
				ORGANIZATION_ID, EMAIL, OrganizationRole.MEMBER, ACTOR_ID, TOKEN_HASH,
				now.minusMinutes(1), now.minusHours(2));
		when(invitationRepository.findByOrganizationIdAndNormalizedEmailAndStatus(
				ORGANIZATION_ID, EMAIL, OrganizationInvitationStatus.PENDING)).thenReturn(Optional.of(stale));
		when(userClient.getUsers(0, 200, EMAIL)).thenReturn(emptyUsers());
		when(invitationProperties.getExpiryHours()).thenReturn(72L);
		when(invitationTokenService.issue()).thenReturn(new IssuedInvitationToken(RAW_TOKEN, "b".repeat(64)));
		when(invitationRepository.saveAndFlush(any(OrganizationInvitation.class))).thenAnswer(invocation -> {
			OrganizationInvitation invitation = invocation.getArgument(0);
			if (invitation.getId() == null && invitation.getStatus() == OrganizationInvitationStatus.PENDING) {
				ReflectionTestUtils.setField(invitation, "id", INVITATION_ID);
			}
			return invitation;
		});

		OrganizationInvitationResponse response = service.createInvitation(
				ORGANIZATION_ID,
				new CreateOrganizationInvitationRequest(EMAIL, OrganizationRole.ADMIN),
				ACTOR_ID,
				false);

		assertThat(stale.getStatus()).isEqualTo(OrganizationInvitationStatus.EXPIRED);
		assertThat(stale.getActiveEmailKey()).isNull();
		assertThat(response.status()).isEqualTo(OrganizationInvitationStatus.PENDING);
		assertThat(response.role()).isEqualTo(OrganizationRole.ADMIN);
	}

	@Test
	void createInvitationRejectsEmailThatAlreadyBelongsToOrganizationMember() {
		when(invitationRepository.findByOrganizationIdAndNormalizedEmailAndStatus(
				ORGANIZATION_ID, EMAIL, OrganizationInvitationStatus.PENDING)).thenReturn(Optional.empty());
		when(userClient.getUsers(0, 200, EMAIL)).thenReturn(new UserSummaryPageResponse(
				List.of(new UserSummaryResponse(EXISTING_USER_ID, "Invitee", "User", "Invitee@Example.Test")),
				0, 200, 1, 1));
		when(memberRepository.existsByOrganizationIdAndUserId(ORGANIZATION_ID, EXISTING_USER_ID)).thenReturn(true);

		assertThatThrownBy(() -> service.createInvitation(
				ORGANIZATION_ID,
				new CreateOrganizationInvitationRequest(EMAIL, OrganizationRole.MEMBER),
				ACTOR_ID,
				false))
				.isInstanceOf(BadRequestException.class)
				.hasMessageContaining("already belongs");

		verify(invitationTokenService, never()).issue();
	}

	@Test
	void listInvitationsReturnsSafeMetadataAndMarksExpiredPendingRows() {
		LocalDateTime now = LocalDateTime.now();
		OrganizationInvitation expired = OrganizationInvitation.createPending(
				ORGANIZATION_ID, "old@example.test", OrganizationRole.MEMBER, ACTOR_ID, TOKEN_HASH,
				now.minusMinutes(1), now.minusDays(1));
		OrganizationInvitation pending = OrganizationInvitation.createPending(
				ORGANIZATION_ID, EMAIL, OrganizationRole.ADMIN, ACTOR_ID, "b".repeat(64),
				now.plusDays(2), now);
		when(invitationRepository.findByOrganizationIdOrderByCreatedAtDesc(ORGANIZATION_ID))
				.thenReturn(List.of(expired, pending));

		List<OrganizationInvitationResponse> responses = service.getInvitations(ORGANIZATION_ID, ACTOR_ID, false);

		assertThat(expired.getStatus()).isEqualTo(OrganizationInvitationStatus.EXPIRED);
		assertThat(expired.getActiveEmailKey()).isNull();
		assertThat(responses).hasSize(2);
		assertThat(responses).extracting(OrganizationInvitationResponse::email)
				.containsExactly("old@example.test", EMAIL);
		assertThat(responses).extracting(OrganizationInvitationResponse::status)
				.containsExactly(OrganizationInvitationStatus.EXPIRED, OrganizationInvitationStatus.PENDING);
		verify(accessService).assertCanManageMembers(ACTOR_ID, organization, false);
	}

	private UserSummaryPageResponse emptyUsers() {
		return new UserSummaryPageResponse(List.of(), 0, 200, 0, 0);
	}
}
