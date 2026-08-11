package com.ums.gateway.filter;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.server.ServerWebExchange;

import reactor.core.publisher.Mono;

class GatewayJwtAuthenticationFilterTests {

	private static final String GATEWAY_SECRET = "test-gateway-secret";

	private final GatewayJwtAuthenticationFilter filter = new GatewayJwtAuthenticationFilter(GATEWAY_SECRET);

	@Test
	void stripsClientSuppliedIdentityHeadersFromPublicRequests() {
		ServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest
				.post("/api/v1/auth/login")
				.header("X-Authenticated-User", "spoofed")
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
		assertThat(headers.containsKey("X-User-Roles")).isFalse();
		assertThat(headers.containsKey("X-User-Permissions")).isFalse();
		assertThat(headers.containsKey("X-Internal-Gateway-Secret")).isFalse();
	}

	@Test
	void injectsTrustedIdentityOnlyFromValidatedJwtAuthentication() {
		Jwt jwt = Jwt.withTokenValue("access-token")
				.header("alg", "RS256")
				.subject("00000000-0000-0000-0000-000000000001")
				.issuedAt(Instant.now())
				.expiresAt(Instant.now().plusSeconds(300))
				.claim("roles", List.of("SUPER_ADMIN"))
				.claim("permissions", List.of("ROLE_WRITE"))
				.build();
		JwtAuthenticationToken authentication = new JwtAuthenticationToken(
				jwt, List.of(new SimpleGrantedAuthority("ROLE_SUPER_ADMIN")));
		ServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest
				.get("/api/v1/users/me")
				.header("X-Authenticated-User", "spoofed")
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
		assertThat(headers.getFirst("X-User-Roles")).contains("SUPER_ADMIN");
		assertThat(headers.getFirst("X-User-Permissions")).contains("ROLE_WRITE");
		assertThat(headers.getFirst("X-Internal-Gateway-Secret")).isEqualTo(GATEWAY_SECRET);
	}
}
