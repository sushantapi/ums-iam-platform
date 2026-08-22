package com.ums.org.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.junit.jupiter.api.Test;

import com.ums.org.enums.OrganizationInvitationStatus;
import com.ums.org.enums.OrganizationRole;

import jakarta.persistence.Column;

class OrganizationInvitationTests {

	private static final String TOKEN_HASH = "a".repeat(64);
	private static final LocalDateTime ISSUED_AT = LocalDateTime.of(2026, 8, 21, 20, 0);

	@Test
	void createsPendingInvitationWithOnlyHashedTokenPersistence() {
		OrganizationInvitation invitation = OrganizationInvitation.createPending(
				UUID.randomUUID(), "invitee@example.test", OrganizationRole.MEMBER,
				UUID.randomUUID(), TOKEN_HASH, ISSUED_AT.plusDays(2), ISSUED_AT);

		assertThat(invitation.getStatus()).isEqualTo(OrganizationInvitationStatus.PENDING);
		assertThat(invitation.getNormalizedEmail()).isEqualTo("invitee@example.test");
		assertThat(invitation.getActiveEmailKey()).isEqualTo("invitee@example.test");
		assertThat(invitation.getTokenHash()).isEqualTo(TOKEN_HASH);
		assertThat(invitation.getLastSentAt()).isNull();

		Set<String> fields = Arrays.stream(OrganizationInvitation.class.getDeclaredFields())
				.map(Field::getName)
				.collect(Collectors.toSet());
		assertThat(fields).contains("tokenHash");
		assertThat(fields).doesNotContain("rawToken", "inviteLink", "token");
	}

	@Test
	void tokenHashMappingMatchesAppliedFlywaySchema() throws NoSuchFieldException {
		Field tokenHash = OrganizationInvitation.class.getDeclaredField("tokenHash");

		Column column = tokenHash.getAnnotation(Column.class);
		JdbcTypeCode jdbcType = tokenHash.getAnnotation(JdbcTypeCode.class);

		assertThat(column).isNotNull();
		assertThat(column.columnDefinition()).isEqualTo("CHAR(64)");
		assertThat(column.length()).isEqualTo(64);
		assertThat(jdbcType).isNotNull();
		assertThat(jdbcType.value()).isEqualTo(SqlTypes.CHAR);
	}

	@Test
	void rejectsOwnerRoleAndNonNormalizedEmail() {
		UUID organizationId = UUID.randomUUID();
		UUID inviterId = UUID.randomUUID();

		assertThatThrownBy(() -> OrganizationInvitation.createPending(
				organizationId, "invitee@example.test", OrganizationRole.OWNER,
				inviterId, TOKEN_HASH, ISSUED_AT.plusDays(2), ISSUED_AT))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("OWNER");

		assertThatThrownBy(() -> OrganizationInvitation.createPending(
				organizationId, " Invitee@Example.Test ", OrganizationRole.MEMBER,
				inviterId, TOKEN_HASH, ISSUED_AT.plusDays(2), ISSUED_AT))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("trimmed and lowercase");
	}

	@Test
	void terminalTransitionsClearActiveEmailKeyAndPreventReplayTransitions() {
		OrganizationInvitation invitation = OrganizationInvitation.createPending(
				UUID.randomUUID(), "invitee@example.test", OrganizationRole.ADMIN,
				UUID.randomUUID(), TOKEN_HASH, ISSUED_AT.plusDays(2), ISSUED_AT);

		invitation.markAccepted(ISSUED_AT.plusHours(1));

		assertThat(invitation.getStatus()).isEqualTo(OrganizationInvitationStatus.ACCEPTED);
		assertThat(invitation.getActiveEmailKey()).isNull();
		assertThat(invitation.getAcceptedAt()).isEqualTo(ISSUED_AT.plusHours(1));
		assertThatThrownBy(() -> invitation.markRevoked(ISSUED_AT.plusHours(2)))
				.isInstanceOf(IllegalStateException.class)
				.hasMessage("Invitation is not pending");
	}

	@Test
	void tokenRotationDoesNotClaimDeliveryUntilNotificationIsDispatched() {
		OrganizationInvitation invitation = OrganizationInvitation.createPending(
				UUID.randomUUID(), "invitee@example.test", OrganizationRole.MEMBER,
				UUID.randomUUID(), TOKEN_HASH, ISSUED_AT.plusDays(2), ISSUED_AT);
		String rotatedHash = "b".repeat(64);

		invitation.rotateToken(rotatedHash, ISSUED_AT.plusDays(3), ISSUED_AT.plusHours(1));

		assertThat(invitation.getTokenHash()).isEqualTo(rotatedHash);
		assertThat(invitation.getExpiresAt()).isEqualTo(ISSUED_AT.plusDays(3));
		assertThat(invitation.getLastSentAt()).isNull();

		invitation.markNotificationSent(ISSUED_AT.plusHours(2));
		assertThat(invitation.getLastSentAt()).isEqualTo(ISSUED_AT.plusHours(2));
	}
}
