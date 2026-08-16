package com.ums.user.config;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.UUID;
import java.util.stream.Stream;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class TrustedGatewayAuthenticationFilter extends OncePerRequestFilter {

	public static final String AUTHENTICATED_USER_HEADER = "X-Authenticated-User";
	private static final String USER_ROLES_HEADER = "X-User-Roles";
	private static final String USER_PERMISSIONS_HEADER = "X-User-Permissions";
	private static final String INTERNAL_GATEWAY_SECRET_HEADER = "X-Internal-Gateway-Secret";

	private final String internalGatewaySecret;

	public TrustedGatewayAuthenticationFilter(@Value("${internal.gateway.secret}") String internalGatewaySecret) {
		this.internalGatewaySecret = internalGatewaySecret;
	}

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
			throws ServletException, IOException {
		String authenticatedUser = request.getHeader(AUTHENTICATED_USER_HEADER);
		String gatewaySecret = request.getHeader(INTERNAL_GATEWAY_SECRET_HEADER);

		if (isTrustedGatewayRequest(gatewaySecret) && StringUtils.hasText(authenticatedUser)
				&& isUuid(authenticatedUser)) {
			Collection<SimpleGrantedAuthority> authorities = parseAuthorities(
					request.getHeader(USER_ROLES_HEADER),
					request.getHeader(USER_PERMISSIONS_HEADER));
			UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
					authenticatedUser, null, authorities);
			SecurityContextHolder.getContext().setAuthentication(authentication);
		}

		try {
			filterChain.doFilter(request, response);
		} finally {
			SecurityContextHolder.clearContext();
		}
	}

	private boolean isTrustedGatewayRequest(String gatewaySecret) {
		return StringUtils.hasText(internalGatewaySecret) && internalGatewaySecret.equals(gatewaySecret);
	}

	private boolean isUuid(String value) {
		try {
			UUID.fromString(value);
			return true;
		} catch (IllegalArgumentException ex) {
			return false;
		}
	}

	private Collection<SimpleGrantedAuthority> parseAuthorities(String rolesHeader, String permissionsHeader) {
		return Stream.concat(
				splitHeader(rolesHeader).map(this::toRoleAuthority),
				splitHeader(permissionsHeader))
				.map(SimpleGrantedAuthority::new)
				.toList();
	}

	private Stream<String> splitHeader(String headerValue) {
		if (!StringUtils.hasText(headerValue)) {
			return Stream.empty();
		}

		String normalized = headerValue.replace("[", "").replace("]", "");
		return Arrays.stream(normalized.split(","))
				.map(String::trim)
				.filter(StringUtils::hasText);
	}

	private String toRoleAuthority(String role) {
		if (role.startsWith("ROLE_")) {
			return role;
		}

		return "ROLE_" + role;
	}
}
