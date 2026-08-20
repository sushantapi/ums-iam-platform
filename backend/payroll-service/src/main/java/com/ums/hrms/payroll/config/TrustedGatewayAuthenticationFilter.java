package com.ums.hrms.payroll.config;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

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

    static final String AUTHENTICATED_USER_HEADER = "X-Authenticated-User";
    static final String USER_ROLES_HEADER = "X-User-Roles";
    static final String USER_PERMISSIONS_HEADER = "X-User-Permissions";
    static final String INTERNAL_GATEWAY_SECRET_HEADER = "X-Internal-Gateway-Secret";

    private final String secret;

    public TrustedGatewayAuthenticationFilter(@Value("${internal.gateway.secret}") String secret) {
        this.secret = secret;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        String user = request.getHeader(AUTHENTICATED_USER_HEADER);
        String suppliedSecret = request.getHeader(INTERNAL_GATEWAY_SECRET_HEADER);

        if (StringUtils.hasText(secret)
                && secret.equals(suppliedSecret)
                && StringUtils.hasText(user)
                && isUuid(user)) {
            List<SimpleGrantedAuthority> authorities = new ArrayList<>();
            values(request.getHeader(USER_ROLES_HEADER)).stream()
                    .map(role -> role.startsWith("ROLE_") ? role : "ROLE_" + role)
                    .map(SimpleGrantedAuthority::new)
                    .forEach(authorities::add);
            values(request.getHeader(USER_PERMISSIONS_HEADER)).stream()
                    .map(SimpleGrantedAuthority::new)
                    .forEach(authorities::add);

            SecurityContextHolder.getContext().setAuthentication(
                    new UsernamePasswordAuthenticationToken(user, null, authorities));
        }

        try {
            filterChain.doFilter(request, response);
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    private boolean isUuid(String value) {
        try {
            UUID.fromString(value);
            return true;
        } catch (IllegalArgumentException ex) {
            return false;
        }
    }

    private List<String> values(String header) {
        if (!StringUtils.hasText(header)) {
            return List.of();
        }
        return Arrays.stream(header.replace("[", "").replace("]", "").split(","))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .toList();
    }
}
