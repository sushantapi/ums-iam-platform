package com.ums.org.entity;

import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;
import java.util.regex.Pattern;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import com.ums.org.enums.OrganizationInvitationStatus;
import com.ums.org.enums.OrganizationRole;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "organization_invitations")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder(access = AccessLevel.PRIVATE)
public class OrganizationInvitation extends BaseEntity {

	private static final Pattern SHA256_HEX = Pattern.compile("[0-9a-f]{64}");

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	@Column(columnDefinition = "CHAR(36)")
	@JdbcTypeCode(SqlTypes.CHAR)
	private UUID id;

	@Column(name = "organization_id", nullable = false, columnDefinition = "CHAR(36)")
	@JdbcTypeCode(SqlTypes.CHAR)
	private UUID organizationId;

	@Column(name = "normalized_email", nullable = false, length = 320)
	private String normalizedEmail;

	@Column(name = "active_email_key", length = 320)
	private String activeEmailKey;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 30)
	private OrganizationRole role;

	@Column(name = "inviter_id", nullable = false, columnDefinition = "CHAR(36)")
	@JdbcTypeCode(SqlTypes.CHAR)
	private UUID inviterId;

	@Column(name = "token_hash", nullable = false, unique = true, length = 64)
	private String tokenHash;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 30)
	private OrganizationInvitationStatus status;

	@Column(name = "expires_at", nullable = false)
	private LocalDateTime expiresAt;

	@Column(name = "last_sent_at")
	private LocalDateTime lastSentAt;

	@Column(name = "accepted_at")
	private LocalDateTime acceptedAt;

	@Column(name = "revoked_at")
	private LocalDateTime revokedAt;

	@Column(name = "expired_at")
	private LocalDateTime expiredAt;

	public static OrganizationInvitation createPending(UUID organizationId, String normalizedEmail,
			OrganizationRole role, UUID inviterId, String tokenHash, LocalDateTime expiresAt,
			LocalDateTime sentAt) {
		Objects.requireNonNull(organizationId, "organizationId is required");
		Objects.requireNonNull(inviterId, "inviterId is required");
		Objects.requireNonNull(expiresAt, "expiresAt is required");
		Objects.requireNonNull(sentAt, "sentAt is required");
		validateEmail(normalizedEmail);
		validateRole(role);
		validateTokenHash(tokenHash);
		if (!expiresAt.isAfter(sentAt)) {
			throw new IllegalArgumentException("expiresAt must be after sentAt");
		}

		return OrganizationInvitation.builder()
				.organizationId(organizationId)
				.normalizedEmail(normalizedEmail)
				.activeEmailKey(normalizedEmail)
				.role(role)
				.inviterId(inviterId)
				.tokenHash(tokenHash)
				.status(OrganizationInvitationStatus.PENDING)
				.expiresAt(expiresAt)
				.lastSentAt(sentAt)
				.build();
	}

	public boolean isPending() {
		return status == OrganizationInvitationStatus.PENDING;
	}

	public boolean isExpiredAt(LocalDateTime now) {
		Objects.requireNonNull(now, "now is required");
		return isPending() && !expiresAt.isAfter(now);
	}

	public void rotateToken(String newTokenHash, LocalDateTime newExpiresAt, LocalDateTime sentAt) {
		assertPending();
		validateTokenHash(newTokenHash);
		Objects.requireNonNull(newExpiresAt, "newExpiresAt is required");
		Objects.requireNonNull(sentAt, "sentAt is required");
		if (!newExpiresAt.isAfter(sentAt)) {
			throw new IllegalArgumentException("newExpiresAt must be after sentAt");
		}
		tokenHash = newTokenHash;
		expiresAt = newExpiresAt;
		lastSentAt = sentAt;
	}

	public void markAccepted(LocalDateTime when) {
		transitionTo(OrganizationInvitationStatus.ACCEPTED, when);
		acceptedAt = when;
	}

	public void markRevoked(LocalDateTime when) {
		transitionTo(OrganizationInvitationStatus.REVOKED, when);
		revokedAt = when;
	}

	public void markExpired(LocalDateTime when) {
		transitionTo(OrganizationInvitationStatus.EXPIRED, when);
		expiredAt = when;
	}

	private void transitionTo(OrganizationInvitationStatus target, LocalDateTime when) {
		assertPending();
		Objects.requireNonNull(when, "transition time is required");
		status = target;
		activeEmailKey = null;
	}

	private void assertPending() {
		if (!isPending()) {
			throw new IllegalStateException("Invitation is not pending");
		}
	}

	private static void validateEmail(String normalizedEmail) {
		if (normalizedEmail == null || normalizedEmail.isBlank() || normalizedEmail.length() > 320) {
			throw new IllegalArgumentException("normalizedEmail is required and must not exceed 320 characters");
		}
		if (!normalizedEmail.equals(normalizedEmail.trim())
				|| !normalizedEmail.equals(normalizedEmail.toLowerCase(Locale.ROOT))) {
			throw new IllegalArgumentException("normalizedEmail must be trimmed and lowercase");
		}
	}

	private static void validateRole(OrganizationRole role) {
		Objects.requireNonNull(role, "role is required");
		if (role == OrganizationRole.OWNER) {
			throw new IllegalArgumentException("OWNER cannot be assigned through an invitation");
		}
	}

	private static void validateTokenHash(String tokenHash) {
		if (tokenHash == null || !SHA256_HEX.matcher(tokenHash).matches()) {
			throw new IllegalArgumentException("tokenHash must be a lowercase SHA-256 hex digest");
		}
	}
}
