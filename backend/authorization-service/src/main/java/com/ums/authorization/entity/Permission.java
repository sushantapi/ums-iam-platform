package com.ums.authorization.entity;

import java.time.LocalDateTime;
import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
@Table(name = "permissions", uniqueConstraints = {
		@UniqueConstraint(name = "uk_permission_resource_action", columnNames = { "resource_id", "action" }) })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Permission {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	@JdbcTypeCode(SqlTypes.CHAR)
	@Column(length = 36)
	private UUID id;

	@Column(nullable = false, unique = true, length = 120)
	private String code;

	@ManyToOne(fetch = jakarta.persistence.FetchType.LAZY)
	@JoinColumn(name = "resource_id", nullable = false)
	private Resource resource;

	@Column(nullable = false, length = 50)
	private String action;

	@Column(columnDefinition = "TEXT")
	private String description;

	@Builder.Default
	@Column(nullable = false)
	private Boolean active = true;

	@CreationTimestamp
	@Column(nullable = false, updatable = false)
	private LocalDateTime createdAt;

	@UpdateTimestamp
	@Column(nullable = false)
	private LocalDateTime updatedAt;
}
