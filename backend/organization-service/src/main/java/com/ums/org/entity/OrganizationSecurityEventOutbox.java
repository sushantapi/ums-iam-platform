package com.ums.org.entity;

import java.time.LocalDateTime;
import java.util.UUID;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "organization_security_event_outbox")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrganizationSecurityEventOutbox extends BaseEntity {

	public enum Status {
		PENDING,
		PUBLISHED
	}

	@Id
	@JdbcTypeCode(SqlTypes.CHAR)
	@Column(name = "event_id", nullable = false, length = 36, columnDefinition = "CHAR(36)")
	private UUID eventId;

	@JdbcTypeCode(SqlTypes.CHAR)
	@Column(name = "organization_id", nullable = false, length = 36, columnDefinition = "CHAR(36)")
	private UUID organizationId;

	@Column(name = "event_type", nullable = false, length = 80)
	private String eventType;

	@JdbcTypeCode(SqlTypes.CHAR)
	@Column(name = "updated_by", nullable = false, length = 36, columnDefinition = "CHAR(36)")
	private UUID updatedBy;

	@Column(name = "occurred_at", nullable = false)
	private LocalDateTime occurredAt;

	@Enumerated(EnumType.STRING)
	@Builder.Default
	@Column(name = "status", nullable = false, length = 20)
	private Status status = Status.PENDING;

	@Builder.Default
	@Column(name = "attempts", nullable = false)
	private int attempts = 0;

	@Column(name = "next_attempt_at")
	private LocalDateTime nextAttemptAt;

	@Column(name = "published_at")
	private LocalDateTime publishedAt;

	@Column(name = "last_error_type", length = 255)
	private String lastErrorType;
}
