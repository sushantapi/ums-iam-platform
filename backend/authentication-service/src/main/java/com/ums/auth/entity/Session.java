package com.ums.auth.entity;

import java.time.Instant;
import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "sessions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Session {

	@Id
	@Builder.Default
	@JdbcTypeCode(SqlTypes.CHAR)
	@Column(length = 36)
	private UUID id = UUID.randomUUID();

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "user_id", nullable = false)
	private User user;

	@Column(nullable = false, length = 500)
	private String refreshTokenHash;

	private String ipAddress;

	private String deviceInfo;

	private String client;

	@JdbcTypeCode(SqlTypes.CHAR)
	@Column(length = 36)
	private UUID organizationId;

	private Instant expiresAt;

	private Instant lastSeenAt;

	private Instant revokedAt;

	@Builder.Default
	private boolean revoked = false;

	@CreationTimestamp
	private Instant createdAt;
}
