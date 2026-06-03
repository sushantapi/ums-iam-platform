package com.ums.auth.entity;

import java.time.LocalDateTime;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
@Table(name = "mfa_configs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MfaConfig {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@Column(name = "user_id", nullable = false, unique = true)
	private UUID userId;

	@Column(name = "secret")
	private String secret;

	@Column(name = "is_enabled")
	private Boolean enabled = false;

	@Column(name = "verified_at")
	private LocalDateTime verifiedAt;
}