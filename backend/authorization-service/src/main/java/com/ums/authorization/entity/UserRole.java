package com.ums.authorization.entity;

import java.time.LocalDateTime;
import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "user_roles", uniqueConstraints = {
		@UniqueConstraint(name = "uk_user_role_scope", columnNames = { "user_id", "role_id", "scope_type",
				"scope_id" }) })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserRole {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	@JdbcTypeCode(SqlTypes.CHAR)
	@Column(length = 36)
	private UUID id;

	@JdbcTypeCode(SqlTypes.CHAR)
	@Column(name = "user_id", nullable = false, length = 36)
	private UUID userId;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "role_id", nullable = false)
	private Role role;

	@JdbcTypeCode(SqlTypes.CHAR)
	@Column(name = "assigned_by", length = 36)
	private UUID assignedBy;

	@Builder.Default
	@Column(name = "scope_type", nullable = false, length = 30)
	private String scopeType = "PLATFORM";

	@Builder.Default
	@Column(name = "scope_id", nullable = false, length = 36)
	private String scopeId = "*";

	@Column(name = "expires_at")
	private LocalDateTime expiresAt;

	@Builder.Default
	@Column(nullable = false)
	private Boolean active = true;

	@CreationTimestamp
	@Column(nullable = false, updatable = false)
	private LocalDateTime assignedAt;
}
