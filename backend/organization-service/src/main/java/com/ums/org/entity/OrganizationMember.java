package com.ums.org.entity;

import java.time.LocalDateTime;
import java.util.UUID;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import com.ums.org.enums.OrganizationRole;

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
@Table(name = "organization_members")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrganizationMember {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	@Column(columnDefinition = "CHAR(36)")
	@JdbcTypeCode(SqlTypes.CHAR)
	private UUID id;

	@Column(name = "organization_id", columnDefinition = "CHAR(36)")
	@JdbcTypeCode(SqlTypes.CHAR)
	private UUID organizationId;

	@Column(name = "user_id", columnDefinition = "CHAR(36)")
	@JdbcTypeCode(SqlTypes.CHAR)
	private UUID userId;

	@Enumerated(EnumType.STRING)
	private OrganizationRole role;

	private LocalDateTime joinedAt;
}