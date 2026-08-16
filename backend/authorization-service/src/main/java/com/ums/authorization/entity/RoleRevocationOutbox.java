package com.ums.authorization.entity;

import java.time.LocalDateTime;
import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "role_revocation_outbox")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RoleRevocationOutbox {

	public static final String STATUS_PENDING = "PENDING";
	public static final String STATUS_FAILED = "FAILED";
	public static final String STATUS_PUBLISHED = "PUBLISHED";

	@Id
	@JdbcTypeCode(SqlTypes.CHAR)
	@Column(name = "event_id", length = 36, nullable = false)
	private UUID eventId;

	@JdbcTypeCode(SqlTypes.CHAR)
	@Column(name = "assignment_id", length = 36, nullable = false)
	private UUID assignmentId;

	@JdbcTypeCode(SqlTypes.CHAR)
	@Column(name = "user_id", length = 36, nullable = false)
	private UUID userId;

	@JdbcTypeCode(SqlTypes.CHAR)
	@Column(name = "role_id", length = 36, nullable = false)
	private UUID roleId;

	@Column(name = "role_name", length = 50, nullable = false)
	private String roleName;

	@JdbcTypeCode(SqlTypes.CHAR)
	@Column(name = "revoked_by", length = 36)
	private UUID revokedBy;

	@Column(name = "revoked_at", nullable = false)
	private LocalDateTime revokedAt;

	@Builder.Default
	@Column(nullable = false, length = 20)
	private String status = STATUS_PENDING;

	@Builder.Default
	@Column(nullable = false)
	private int attempts = 0;

	@Column(name = "last_error", length = 1000)
	private String lastError;

	@CreationTimestamp
	@Column(name = "created_at", nullable = false, updatable = false)
	private LocalDateTime createdAt;

	@Column(name = "published_at")
	private LocalDateTime publishedAt;

	public void markPublished(LocalDateTime publishedAt) {
		this.status = STATUS_PUBLISHED;
		this.publishedAt = publishedAt;
		this.lastError = null;
		this.attempts++;
	}

	public void markFailed(String error) {
		this.status = STATUS_FAILED;
		this.lastError = error;
		this.attempts++;
	}
}
