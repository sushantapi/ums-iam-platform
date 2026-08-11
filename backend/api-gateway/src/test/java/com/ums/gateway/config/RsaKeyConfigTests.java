package com.ums.gateway.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;

import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;

class RsaKeyConfigTests {

	private final RsaKeyConfig config = new RsaKeyConfig();

	@Test
	void gatewayAcceptsAccessTokensAndRejectsRefreshTokens() {
		var validator = config.jwtValidator("ums-iam-platform", "");

		assertThat(validator.validate(jwt("ACCESS")).hasErrors()).isFalse();
		assertThat(validator.validate(jwt("REFRESH")).hasErrors()).isTrue();
	}

	private Jwt jwt(String type) {
		Instant now = Instant.now();
		return Jwt.withTokenValue("token")
				.header("alg", "RS256")
				.subject("00000000-0000-0000-0000-000000000001")
				.issuer("ums-iam-platform")
				.issuedAt(now)
				.expiresAt(now.plusSeconds(300))
				.claim("type", type)
				.build();
	}
}
