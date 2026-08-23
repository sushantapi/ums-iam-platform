package com.ums.gateway.filter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.server.ServerWebExchange;

import com.ums.gateway.security.TokenRevocationService;

import reactor.core.publisher.Mono;

class GatewayJwtAuthenticationFilterTests {

	private static final String GATEWAY_SECRET = "test-gateway-secret";

	private final TokenRevocationService tokenRevocationService = mock(TokenRevocationService.class);
	private final GatewayJwtAuthenticationFilter filter =
			new GatewayJwtAuthenticationFilter(GATEWAY_SECRET, tokenRevocationService);

	@Test
	void stripsClientSuppliedIdentityHeadersFromPublicRequests() {
		ServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest
				.post("/api/v1/auth/login")
				.header("X-Authenticated-User", "spoofed")
				.header("X-Authenticated-Organization", "00000000-0000-0000-0000-000000000777")
				.header("X-MFA-Verified", "true")
				.header("X-User-Roles", "SUPER_ADMIN")
				.header("X-User-Permissions", "ROLE_WRITE")
				.header("X-Internal-Gateway-Secret", "spoofed-secret")
				.build());
		AtomicReference<ServerWebExchange> forwarded = new AtomicReference<>();

		filter.filter(exchange, capturedExchange -> {
			forwarded.set(capturedExchange);
			return Mono.empty();
		}).block();

		HttpHeaders headers = forwarded.get().getRequest().getHeaders();
		assertThat(headers.containsKey("X-Authenticated-User")).isFalse();
		assertThat(headers.containsKey("X-Authenticated-Organization")).isFalse();
		assertThat(headers.containsKey("X-MFA-Verified")).isFalse();
		assertThat(headers.containsKey("X-User-Roles")).isFalse();
		assertThat(headers.containsKey("X-User-Permissions")).isFalse();
		assertThat(headers.containsKey("X-Internal-Gateway-Secret")).isFalse();
	}

	@Test
	void recoveryAndMfaChallengeRoutesArePublicOnlyForExactPostRequests() {
		assertThat(filter.isPublicRequest(HttpMethod.POST, "/api/v1/auth/forgot-password")).isTrue();
		assertThat(filter.isPublicRequest(HttpMethod.POST, "/api/v1/auth/reset-password")).isTrue();
		assertThat(filter.isPublicRequest(HttpMethod.POST, "/api/v1/auth/mfa/challenge/verify")).isTrue();

		assertThat(filter.isPublicRequest(HttpMethod.GET, "/api/v1/auth/forgot-password")).isFalse();
		assertThat(filter.isPublicRequest(HttpMethod.GET, "/api/v1/auth/reset-password")).isFalse();
		assertThat(filter.isPublicRequest(HttpMethod.GET, "/api/v1/auth/mfa/challenge/verify")).isFalse();
		assertThat(filter.isPublicRequest(HttpMethod.POST, "/api/v1/auth/forgot-password/extra")).isFalse();
		assertThat(filter.isPublicRequest(HttpMethod.POST, "/api/v1/auth/reset-password/extra")).isFalse();
		assertThat(filter.isPublicRequest(HttpMethod.POST, "/api/v1/auth/mfa/challenge/verify/extra")).isFalse();
	}

	@Test
	void neighboringAuthRoutesRemainProtected() {
		assertThat(filter.isPublicRequest(HttpMethod.POST, "/api/v1/auth/logout")).isFalse();
		assertThat(filter.isPublicRequest(HttpMethod.POST, "/api/v1/auth/admin-reset-password")).isFalse();
		assertThat(filter.isPublicRequest(HttpMethod.GET, "/api/v1/auth/login")).isFalse();
	}

	@Test
	void revokedAccessTokenIsRejectedBeforeRouting() {
		Jwt jwt = Jwt.withTokenValue("revoked-access-token")
				.header("alg", "RS256")
				.claim("jti", "jti-revoked")
				.subject("00000000-0000-0000-0000-000000000001")
				.issuedAt(Instant.now())
				.expiresAt(Instant.now().plusSeconds(300))
				.claim("roles", List.of("SUPER_ADMIN"))
				.claim("sessionId", "00000000-0000-0000-0000-000000000099")
				.build();
		when(tokenRevocationService.isRevoked(any(Jwt.class))).thenReturn(Mono.just(true));

		JwtAuthenticationToken authentication = new JwtAuthenticationToken(
				jwt, List.of(new SimpleGrantedAuthority("ROLE_SUPER_ADMIN")));
		authentication.setAuthenticated(true);
		ServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/api/v1/users/me").build())
				.mutate()
				.principal(Mono.just(authentication))
				.build();
		AtomicBoolean routed = new AtomicBoolean(false);

		filter.filter(exchange, ignored -> Mono.fromRunnable(() -> routed.set(true)).then()).block();

		assertThat(exchange.getResponse().getStatusCode()).isEqualTo(org.springframework.http.HttpStatus.UNAUTHORIZED);
		assertThat(routed).isFalse();
	}

	@Test
	void injectsTrustedIdentityOnlyFromValidatedJwtAuthentication() {
		String organizationId = "00000000-0000-0000-0000-000000000777";
		Jwt jwt = Jwt.withTokenValue("access-token")
				.header("alg", "RS256")
				.claim("jti", "jti-1")
				.subject("00000000-0000-0000-0000-000000000001")
				.issuedAt(Instant.now())
				.expiresAt(Instant.now().plusSeconds(300))
				.claim("roles", List.of("SUPER_ADMIN"))
				.claim("permissions", List.of("ROLE_WRITE"))
				.claim("organizationId", organizationId)
				.claim("mfaVerified", true)
				.claim("sessionId", "00000000-0000-0000-0000-000000000099")
				.build();
		when(tokenRevocationService.isRevoked(jwt)).thenReturn(Mono.just(false));
		JwtAuthenticationToken authentication = new JwtAuthenticationToken(
				jwt, List.of(new SimpleGrantedAuthority("ROLE_SUPER_ADMIN")));
		ServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest
				.get("/api/v1/users/me")
				.header("X-Authenticated-User", "spoofed")
				.header("X-Authenticated-Organization", "00000000-0000-0000-0000-000000000888")
				.header("X-MFA-Verified", "false")
				.build())
				.mutate()
				.principal(Mono.just(authentication))
				.build();
		AtomicReference<ServerWebExchange> forwarded = new AtomicReference<>();

		filter.filter(exchange, capturedExchange -> {
			forwarded.set(capturedExchange);
			return Mono.empty();
		}).block();

		HttpHeaders headers = forwarded.get().getRequest().getHeaders();
		assertThat(headers.getFirst("X-Authenticated-User"))
				.isEqualTo("00000000-0000-0000-0000-000000000001");
		assertThat(headers.getFirst("X-Authenticated-Organization")).isEqualTo(organizationId);
		assertThat(headers.getFirst("X-MFA-Verified")).isEqualTo("true");
		assertThat(headers.getFirst("X-User-Roles")).contains("SUPER_ADMIN");
		assertThat(headers.getFirst("X-User-Permissions")).contains("ROLE_WRITE");
		assertThat(headers.getFirst("X-Internal-Gateway-Secret")).isEqualTo(GATEWAY_SECRET);
	}
}
