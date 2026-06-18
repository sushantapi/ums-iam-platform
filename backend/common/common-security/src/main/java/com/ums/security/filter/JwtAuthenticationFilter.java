package com.ums.security.filter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.ums.security.config.JwtTokenValidator;
import com.ums.security.dto.JwtUser;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

	private final JwtTokenValidator jwtTokenValidator;

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
			throws ServletException, IOException {

		String header = request.getHeader("Authorization");

		if (header != null && header.startsWith("Bearer ")) {

			try {

				String token = header.substring(7);

				Jwt jwt = jwtTokenValidator.validateToken(token);

				String userId = jwt.getSubject();
				String email = jwt.getClaimAsString("email");

				List<String> roles = jwt.getClaimAsStringList("roles");

				List<String> permissions = jwt.getClaimAsStringList("permissions");

				JwtUser jwtUser = JwtUser.builder().userId(UUID.fromString(userId)).email(email).roles(roles).build();

				List<SimpleGrantedAuthority> authorities = new ArrayList<>();

				if (roles != null) {
					roles.forEach(role -> authorities.add(new SimpleGrantedAuthority("ROLE_" + role)));
				}

				if (permissions != null) {
					permissions.forEach(permission -> authorities.add(new SimpleGrantedAuthority(permission)));
				}

				UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(jwtUser,
						null, authorities);

				SecurityContextHolder.getContext().setAuthentication(authentication);

			} catch (Exception ex) {

				log.error("JWT authentication failed: {}", ex.getMessage());

				SecurityContextHolder.clearContext();
			}
		}

		filterChain.doFilter(request, response);
	}
}