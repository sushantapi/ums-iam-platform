/*
 * package com.ums.gateway.filter;
 * 
 * import org.springframework.cloud.gateway.filter.GatewayFilterChain; import
 * org.springframework.cloud.gateway.filter.GlobalFilter; import
 * org.springframework.core.Ordered; import
 * org.springframework.http.HttpHeaders; import
 * org.springframework.http.HttpStatus; import
 * org.springframework.security.oauth2.jwt.Jwt; import
 * org.springframework.stereotype.Component; import
 * org.springframework.web.server.ServerWebExchange;
 * 
 * import com.ums.gateway.security.JwtTokenValidator;
 * 
 * import lombok.RequiredArgsConstructor; import reactor.core.publisher.Mono;
 * 
 * @Component
 * 
 * @RequiredArgsConstructor public class GatewayJwtAuthenticationFilter
 * implements GlobalFilter, Ordered {
 * 
 * private final JwtTokenValidator jwtTokenValidator;
 * 
 * @Override public Mono<Void> filter(ServerWebExchange exchange,
 * GatewayFilterChain chain) {
 * 
 * String path = exchange.getRequest().getURI().getPath();
 * 
 * // Public endpoints if (path.startsWith("/api/v1/auth/login") ||
 * path.startsWith("/api/v1/auth/register") ||
 * path.startsWith("/api/v1/auth/refresh") || path.startsWith("/swagger-ui") ||
 * path.startsWith("/v3/api-docs") || path.startsWith("/actuator")) {
 * 
 * return chain.filter(exchange); }
 * 
 * String authHeader =
 * exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
 * 
 * if (authHeader == null || !authHeader.startsWith("Bearer ")) {
 * 
 * exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
 * 
 * return exchange.getResponse().setComplete(); }
 * 
 * try {
 * 
 * String token = authHeader.substring(7);
 * 
 * Jwt jwt = jwtTokenValidator.validateToken(token);
 * 
 * String userId = jwt.getSubject();
 * 
 * Object roles = jwt.getClaim("roles");
 * 
 * ServerWebExchange modifiedExchange = exchange.mutate()
 * .request(exchange.getRequest().mutate().header("X-Authenticated-User",
 * userId) .header("X-User-Roles", roles == null ? "" :
 * roles.toString()).build()) .build();
 * 
 * return chain.filter(modifiedExchange);
 * 
 * } catch (Exception ex) {
 * 
 * exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
 * 
 * return exchange.getResponse().setComplete(); } }
 * 
 * @Override public int getOrder() { return -1; } }
 */

package com.ums.gateway.filter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
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

	private static final Logger log = LoggerFactory.getLogger(GatewayJwtAuthenticationFilter.class);

	private final JwtTokenValidator jwtTokenValidator;

	@Override
	public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {

		String path = exchange.getRequest().getURI().getPath();

		log.info("Incoming request: {}", path);

		if (HttpMethod.OPTIONS.equals(exchange.getRequest().getMethod()) || isPublicPath(path)) {
			return chain.filter(exchange);
		}

		String authHeader = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);

		log.info("Authorization Header Present: {}", authHeader != null);

		if (authHeader == null || !authHeader.startsWith("Bearer ")) {

			log.warn("Missing or invalid Authorization header");

			exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
			return exchange.getResponse().setComplete();
		}

		try {

			String token = authHeader.substring(7);

			Jwt jwt = jwtTokenValidator.validateToken(token);

			String userId = jwt.getSubject();

			Object roles = jwt.getClaim("roles");

			log.info("JWT validated successfully");
			log.info("User ID: {}", userId);
			log.info("Roles: {}", roles);

			ServerWebExchange modifiedExchange = exchange.mutate()
					.request(exchange.getRequest().mutate().header("X-Authenticated-User", userId)
							.header("X-User-Roles", roles == null ? "" : roles.toString()).build())
					.build();

			return chain.filter(modifiedExchange);

		} catch (Exception ex) {

			log.error("JWT validation failed", ex);

			exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
			return exchange.getResponse().setComplete();
		}
	}

	@Override
	public int getOrder() {
		return -1;
	}

	private boolean isPublicPath(String path) {
		return path.startsWith("/api/v1/auth/login")
				|| path.startsWith("/api/v1/auth/register")
				|| path.startsWith("/api/v1/auth/refresh")
				|| path.startsWith("/actuator")
				|| path.startsWith("/swagger-ui")
				|| path.startsWith("/v3/api-docs");
	}
}
