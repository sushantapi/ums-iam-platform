package com.ums.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.security.KeyPairGenerator;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import io.jsonwebtoken.JwtException;

class JwtServiceTests {

	private JwtService jwtService;

	@BeforeEach
	void setUp() throws Exception {
		var keyPairGenerator = KeyPairGenerator.getInstance("RSA");
		keyPairGenerator.initialize(2048);
		var keyPair = keyPairGenerator.generateKeyPair();

		jwtService = new JwtService();
		ReflectionTestUtils.setField(jwtService, "privateKey", keyPair.getPrivate());
		ReflectionTestUtils.setField(jwtService, "publicKey", keyPair.getPublic());
		ReflectionTestUtils.setField(jwtService, "issuer", "ums-iam-platform");
		ReflectionTestUtils.setField(jwtService, "audience", "ums-api-gateway");
		ReflectionTestUtils.setField(jwtService, "keyId", "test-key-1");
		ReflectionTestUtils.setField(jwtService, "accessTokenExpiryMs", 900000L);
		ReflectionTestUtils.setField(jwtService, "refreshTokenExpiryMs", 604800000L);
		ReflectionTestUtils.setField(jwtService, "mfaChallengeExpiryMs", 300000L);
	}

	@Test
	void rejectsTokenWhenConfiguredAudienceDoesNotMatch() {
		UUID sessionId = UUID.randomUUID();
		String token = jwtService.generateRefreshToken(UUID.randomUUID().toString(), sessionId);

		ReflectionTestUtils.setField(jwtService, "audience", "different-audience");

		assertThatThrownBy(() -> jwtService.validateAndExtract(token))
				.isInstanceOf(JwtException.class);
	}

	@Test
	void accessAndRefreshTokensCarryStrictTypesAndSessionBinding() {
		UUID userId = UUID.randomUUID();
		UUID sessionId = UUID.randomUUID();

		String accessToken = jwtService.generateAccessToken(
				userId.toString(),
				"user@example.com",
				Set.of("SUPER_ADMIN"),
				Set.of("AUDIT_READ"),
				sessionId);
		String refreshToken = jwtService.generateRefreshToken(userId.toString(), sessionId);

		var accessClaims = jwtService.validateAndExtract(accessToken);
		var refreshClaims = jwtService.validateAndExtract(refreshToken);

		assertThat(accessClaims.getIssuer()).isEqualTo("ums-iam-platform");
		assertThat(accessClaims.getAudience()).contains("ums-api-gateway");
		assertThat(accessClaims.getId()).isNotBlank();
		assertThat(accessClaims.get("type", String.class)).isEqualTo("ACCESS");
		assertThat(accessClaims.get("sessionId", String.class)).isEqualTo(sessionId.toString());
		assertThat(accessClaims.get("roles").toString()).contains("SUPER_ADMIN");
		assertThat(accessClaims.get("permissions").toString()).contains("AUDIT_READ");

		assertThat(refreshClaims.getIssuer()).isEqualTo("ums-iam-platform");
		assertThat(refreshClaims.getId()).isNotBlank();
		assertThat(refreshClaims.get("type", String.class)).isEqualTo("REFRESH");
		assertThat(refreshClaims.get("sessionId", String.class)).isEqualTo(sessionId.toString());
		assertThat(refreshClaims).doesNotContainKeys("roles", "permissions", "email");
	}

	@Test
	void mfaChallengeIsShortLivedSinglePurposeAndCarriesOnlyLoginContext() {
		UUID userId = UUID.randomUUID();
		UUID organizationId = UUID.randomUUID();

		String token = jwtService.generateMfaChallengeToken(
				userId.toString(), organizationId, "ADMIN_PORTAL", "Chrome");
		var claims = jwtService.validateAndExtract(token);

		assertThat(claims.getId()).isNotBlank();
		assertThat(claims.getSubject()).isEqualTo(userId.toString());
		assertThat(claims.get("type", String.class)).isEqualTo("MFA_CHALLENGE");
		assertThat(claims.get("organizationId", String.class)).isEqualTo(organizationId.toString());
		assertThat(claims.get("client", String.class)).isEqualTo("ADMIN_PORTAL");
		assertThat(claims.get("deviceInfo", String.class)).isEqualTo("Chrome");
		assertThat(claims).doesNotContainKeys("sessionId", "roles", "permissions", "email");
		assertThat(claims.getExpiration().getTime() - claims.getIssuedAt().getTime())
				.isBetween(299000L, 301000L);
	}
}
