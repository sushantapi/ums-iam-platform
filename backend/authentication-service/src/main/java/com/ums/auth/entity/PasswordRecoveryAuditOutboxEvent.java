package com.ums.auth.entity;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "password_recovery_audit_outbox")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PasswordRecoveryAuditOutboxEvent {

	public enum Status {
		PENDING,
		PUBLISHED
	}

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	@JdbcTypeCode(SqlTypes.CHAR)
	@Column(length = 36)
	private UUID id;

	@Column(name = "event_type", nullable = false, length = 150)
	private String eventType;

	@Column(name = "service_name", nullable = false, length = 100)
	private String serviceName;

	@Column(name = "user_id", length = 64)
	private String userId;

	@Column(name = "user_email", length = 150)
	private String userEmail;

	@Column(nullable = false, length = 100)
	private String action;

	@Column(name = "entity_type", length = 100)
	private String entityType;

	@Column(name = "entity_id", length = 100)
	private String entityId;

	@Column(length = 1000)
	private String details;

	@Column(name = "ip_address", length = 255)
	private String ipAddress;

	@Column(name = "event_timestamp", nullable = false)
	private LocalDateTime eventTimestamp;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private Status status;

	@Column(nullable = false)
	private int attempts;

	@Column(name = "next_attempt_at", nullable = false)
	private Instant nextAttemptAt;

	@CreationTimestamp
	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	@Column(name = "published_at")
	private Instant publishedAt;

	@Column(name = "last_error", length = 255)
	private String lastError;
}
