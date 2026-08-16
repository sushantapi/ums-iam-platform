package com.ums.admin.security;

import java.io.IOException;
import java.util.List;

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
public class InternalServiceAuthenticationFilter extends OncePerRequestFilter {

	private static final String INTERNAL_SERVICE_SECRET_HEADER = "X-Internal-Service-Secret";
	private final String internalServiceSecret;

	public InternalServiceAuthenticationFilter(@Value("${internal.service.secret}") String internalServiceSecret) {
		this.internalServiceSecret = internalServiceSecret;
	}

	@Override
	protected boolean shouldNotFilter(HttpServletRequest request) {
		String path = request.getRequestURI();
		return !path.startsWith("/api/v1/internal/") && !path.startsWith("/internal/");
	}

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
			throws ServletException, IOException {
		String suppliedSecret = request.getHeader(INTERNAL_SERVICE_SECRET_HEADER);
		if (StringUtils.hasText(internalServiceSecret) && internalServiceSecret.equals(suppliedSecret)) {
			SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
					"internal-service", null, List.of(new SimpleGrantedAuthority("ROLE_INTERNAL_SERVICE"))));
		}
		try {
			filterChain.doFilter(request, response);
		} finally {
			SecurityContextHolder.clearContext();
		}
	}
}
