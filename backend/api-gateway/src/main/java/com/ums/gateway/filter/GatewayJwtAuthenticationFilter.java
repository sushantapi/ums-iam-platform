package com.ums.gateway.filter;

import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;

import com.ums.gateway.security.JwtTokenValidator;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
public class GatewayJwtAuthenticationFilter implements GlobalFilter, Ordered {

	private final JwtTokenValidator jwtTokenValidator;

	@Override
	public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {

		String path = exchange.getRequest().getURI().getPath();

		// Public endpoints
		if (path.startsWith("/api/v1/auth/login") || path.startsWith("/api/v1/auth/register")
				|| path.startsWith("/api/v1/auth/refresh") || path.startsWith("/swagger-ui")
				|| path.startsWith("/v3/api-docs") || path.startsWith("/actuator")) {

			return chain.filter(exchange);
		}

		String authHeader = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);

		if (authHeader == null || !authHeader.startsWith("Bearer ")) {

			exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);

			return exchange.getResponse().setComplete();
		}

		try {

			String token = authHeader.substring(7);

			Jwt jwt = jwtTokenValidator.validateToken(token);

			String userId = jwt.getSubject();

			Object roles = jwt.getClaim("roles");

			ServerWebExchange modifiedExchange = exchange.mutate()
					.request(exchange.getRequest().mutate().header("X-Authenticated-User", userId)
							.header("X-User-Roles", roles == null ? "" : roles.toString()).build())
					.build();

			return chain.filter(modifiedExchange);

		} catch (Exception ex) {

			exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);

			return exchange.getResponse().setComplete();
		}
	}

	@Override
	public int getOrder() {
		return -1;
	}
}