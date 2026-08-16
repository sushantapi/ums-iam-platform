package com.ums.auth.entity;

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
@Table(name = "processed_security_events")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProcessedSecurityEvent {

	@Id
	@JdbcTypeCode(SqlTypes.CHAR)
	@Column(name = "event_id", length = 36, nullable = false)
	private UUID eventId;

	@Column(name = "event_type", length = 80, nullable = false)
	private String eventType;

	@JdbcTypeCode(SqlTypes.CHAR)
	@Column(name = "user_id", length = 36, nullable = false)
	private UUID userId;

	@CreationTimestamp
	@Column(name = "processed_at", nullable = false, updatable = false)
	private LocalDateTime processedAt;
}
