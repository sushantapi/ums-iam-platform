package com.ums.auth.entity;

import java.time.LocalDateTime;
import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;

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
@Table(name = "auth_sessions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuthSession {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@Column(name = "user_id", nullable = false)
	private UUID userId;

	@Column(name = "access_token", columnDefinition = "TEXT")
	private String accessToken;

	@Column(name = "refresh_token", columnDefinition = "TEXT")
	private String refreshToken;

	@Column(name = "device_name")
	private String deviceName;

	@Column(name = "device_type")
	private String deviceType;

	@Column(name = "ip_address")
	private String ipAddress;

	@Column(name = "user_agent", columnDefinition = "TEXT")
	private String userAgent;

	@Column(name = "expires_at")
	private LocalDateTime expiresAt;

	@Column(name = "is_revoked")
	private Boolean isRevoked = false;

	@CreationTimestamp
	private LocalDateTime createdAt;
}