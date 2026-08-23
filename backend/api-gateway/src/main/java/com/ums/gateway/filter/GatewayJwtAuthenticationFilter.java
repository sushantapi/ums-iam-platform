package com.ums.gateway.filter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;

import com.ums.gateway.security.TokenRevocationService;

import reactor.core.publisher.Mono;

@Component
public class GatewayJwtAuthenticationFilter implements GlobalFilter, Ordered {

	private static final Logger log = LoggerFactory.getLogger(GatewayJwtAuthenticationFilter.class);
	private static final String AUTHENTICATED_USER_HEADER = "X-Authenticated-User";
	private static final String USER_ROLES_HEADER = "X-User-Roles";
	private static final String USER_PERMISSIONS_HEADER = "X-User-Permissions";
	private static final String INTERNAL_GATEWAY_SECRET_HEADER = "X-Internal-Gateway-Secret";

	private final String internalGatewaySecret;
	private final TokenRevocationService tokenRevocationService;

	public GatewayJwtAuthenticationFilter(
			@Value("${internal.gateway.secret}") String internalGatewaySecret,
			TokenRevocationService tokenRevocationService) {
		this.internalGatewaySecret = internalGatewaySecret;
		this.tokenRevocationService = tokenRevocationService;
	}

	@Override
	public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
		String path = exchange.getRequest().getURI().getPath();
		HttpMethod method = exchange.getRequest().getMethod();

		log.debug("Incoming request: {} {}", method, path);

		ServerWebExchange sanitizedExchange = stripClientIdentityHeaders(exchange);

		if (HttpMethod.OPTIONS.equals(method) || isPublicRequest(method, path)) {
			return chain.filter(sanitizedExchange);
		}

		return sanitizedExchange.getPrincipal().filter(Authentication.class::isInstance).cast(Authentication.class)
				.filter(Authentication::isAuthenticated).filter(JwtAuthenticationToken.class::isInstance)
				.cast(JwtAuthenticationToken.class)
				.flatMap(authentication -> tokenRevocationService.isRevoked(authentication.getToken())
						.flatMap(revoked -> (revoked
								? reject(sanitizedExchange, HttpStatus.UNAUTHORIZED)
								: chain.filter(withTrustedIdentityHeaders(sanitizedExchange, authentication)))
								.thenReturn(true))
						.onErrorResume(ex -> {
							log.error("Token revocation check failed", ex);
							return reject(sanitizedExchange, HttpStatus.SERVICE_UNAVAILABLE).thenReturn(true);
						}))
				.defaultIfEmpty(false)
				.flatMap(handled -> handled ? Mono.empty() : chain.filter(sanitizedExchange));
	}

	private Mono<Void> reject(ServerWebExchange exchange, HttpStatus status) {
		exchange.getResponse().setStatusCode(status);
		return exchange.getResponse().setComplete();
	}

	@Override
	public int getOrder() {
		return -1;
	}

	boolean isPublicRequest(HttpMethod method, String path) {
		if (HttpMethod.POST.equals(method)) {
			return path.equals("/api/v1/auth/register")
					|| path.equals("/api/v1/auth/login")
					|| path.equals("/api/v1/auth/refresh")
					|| path.equals("/api/v1/auth/forgot-password")
					|| path.equals("/api/v1/auth/reset-password")
					|| path.equals("/api/v1/auth/mfa/challenge/verify");
		}

		return HttpMethod.GET.equals(method)
				&& (path.equals("/actuator/health") || path.equals("/actuator/info"));
	}

	private ServerWebExchange stripClientIdentityHeaders(ServerWebExchange exchange) {
		ServerHttpRequest sanitizedRequest = exchange.getRequest().mutate().headers(headers -> {
			headers.remove(AUTHENTICATED_USER_HEADER);
			headers.remove(USER_ROLES_HEADER);
			headers.remove(USER_PERMISSIONS_HEADER);
			headers.remove(INTERNAL_GATEWAY_SECRET_HEADER);
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
				.header(USER_PERMISSIONS_HEADER, permissions == null ? "" : permissions.toString())
				.header(INTERNAL_GATEWAY_SECRET_HEADER, internalGatewaySecret)
				.build();

		return exchange.mutate().request(trustedRequest).build();
	}
}
