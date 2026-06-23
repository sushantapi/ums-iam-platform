package com.ums.gateway.filter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpMethod;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;

import reactor.core.publisher.Mono;

@Component
public class GatewayJwtAuthenticationFilter implements GlobalFilter, Ordered {

	private static final Logger log = LoggerFactory.getLogger(GatewayJwtAuthenticationFilter.class);
	private static final String AUTHENTICATED_USER_HEADER = "X-Authenticated-User";
	private static final String USER_ROLES_HEADER = "X-User-Roles";
	private static final String USER_PERMISSIONS_HEADER = "X-User-Permissions";

	@Override
	public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {

		String path = exchange.getRequest().getURI().getPath();

		log.debug("Incoming request: {}", path);

		ServerWebExchange sanitizedExchange = stripClientIdentityHeaders(exchange);

		if (HttpMethod.OPTIONS.equals(exchange.getRequest().getMethod()) || isPublicPath(path)) {
			return chain.filter(sanitizedExchange);
		}

		return sanitizedExchange.getPrincipal().filter(Authentication.class::isInstance).cast(Authentication.class)
				.filter(Authentication::isAuthenticated).filter(JwtAuthenticationToken.class::isInstance)
				.cast(JwtAuthenticationToken.class)
				.flatMap(authentication -> chain.filter(withTrustedIdentityHeaders(sanitizedExchange, authentication)))
				.switchIfEmpty(chain.filter(sanitizedExchange));
	}

	@Override
	public int getOrder() {
		return -1;
	}

	private boolean isPublicPath(String path) {
		return path.equals("/api/v1/auth/register") || path.equals("/api/v1/auth/login")
				|| path.equals("/api/v1/auth/refresh") || path.equals("/api/v1/auth/forgot-password")
				|| path.equals("/api/v1/auth/reset-password") || path.equals("/api/v1/auth/verify-email")
				|| path.equals("/api/v1/auth/email-verification");
	}

	private ServerWebExchange stripClientIdentityHeaders(ServerWebExchange exchange) {
		ServerHttpRequest sanitizedRequest = exchange.getRequest().mutate().headers(headers -> {
			headers.remove(AUTHENTICATED_USER_HEADER);
			headers.remove(USER_ROLES_HEADER);
			headers.remove(USER_PERMISSIONS_HEADER);
		}).build();

		return exchange.mutate().request(sanitizedRequest).build();
	}

	private ServerWebExchange withTrustedIdentityHeaders(ServerWebExchange exchange,
			JwtAuthenticationToken authentication) {
		String userId = authentication.getToken().getSubject();
		Object roles = authentication.getToken().getClaim("roles");
		Object permissions = authentication.getToken().getClaim("permissions");

		log.debug("Injecting trusted identity headers for subject {}", userId);

		ServerHttpRequest trustedRequest = exchange.getRequest().mutate().header(AUTHENTICATED_USER_HEADER, userId)
				.header(USER_ROLES_HEADER, roles == null ? "" : roles.toString())
				.header(USER_PERMISSIONS_HEADER, permissions == null ? "" : permissions.toString()).build();

		return exchange.mutate().request(trustedRequest).build();
	}
}
