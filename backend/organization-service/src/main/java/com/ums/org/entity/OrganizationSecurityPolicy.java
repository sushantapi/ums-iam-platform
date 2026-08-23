package com.ums.org.entity;

import java.util.UUID;

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
@Table(name = "organization_security_policies")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrganizationSecurityPolicy extends BaseEntity {

	@Id
	@JdbcTypeCode(SqlTypes.CHAR)
	@Column(name = "organization_id", nullable = false, length = 36, columnDefinition = "CHAR(36)")
	private UUID organizationId;

	@Column(name = "require_mfa", nullable = false)
	private boolean requireMfa;

	@JdbcTypeCode(SqlTypes.CHAR)
	@Column(name = "updated_by", length = 36, columnDefinition = "CHAR(36)")
	private UUID updatedBy;
}
