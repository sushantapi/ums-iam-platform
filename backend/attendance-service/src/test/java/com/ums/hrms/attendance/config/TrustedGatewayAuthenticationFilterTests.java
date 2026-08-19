package com.ums.hrms.attendance.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;

class TrustedGatewayAuthenticationFilterTests {

    private static final String SECRET = "test-gateway-secret";

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void authenticatesOnlyWhenGatewaySecretAndUserIdentityAreValid() throws Exception {
        UUID userId = UUID.randomUUID();
        TrustedGatewayAuthenticationFilter filter = new TrustedGatewayAuthenticationFilter(SECRET);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(TrustedGatewayAuthenticationFilter.INTERNAL_GATEWAY_SECRET_HEADER, SECRET);
        request.addHeader(TrustedGatewayAuthenticationFilter.AUTHENTICATED_USER_HEADER, userId.toString());
        request.addHeader(TrustedGatewayAuthenticationFilter.USER_ROLES_HEADER, "HR_ADMIN,HR_MANAGER");
        request.addHeader(TrustedGatewayAuthenticationFilter.USER_PERMISSIONS_HEADER, "ATTENDANCE_CREATE,ATTENDANCE_READ");
        AtomicReference<Authentication> captured = new AtomicReference<>();

        filter.doFilter(request, new MockHttpServletResponse(), capturingChain(captured));

        assertThat(captured.get()).isNotNull();
        assertThat(captured.get().getName()).isEqualTo(userId.toString());
        assertThat(captured.get().getAuthorities())
                .extracting("authority")
                .containsExactlyInAnyOrder("ROLE_HR_ADMIN", "ROLE_HR_MANAGER", "ATTENDANCE_CREATE", "ATTENDANCE_READ");
    }

    @Test
    void rejectsCallerControlledIdentityWithoutTrustedGatewaySecret() throws Exception {
        TrustedGatewayAuthenticationFilter filter = new TrustedGatewayAuthenticationFilter(SECRET);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(TrustedGatewayAuthenticationFilter.AUTHENTICATED_USER_HEADER, UUID.randomUUID().toString());
        request.addHeader(TrustedGatewayAuthenticationFilter.USER_ROLES_HEADER, "SUPER_ADMIN");
        request.addHeader(TrustedGatewayAuthenticationFilter.USER_PERMISSIONS_HEADER, "ATTENDANCE_CREATE");
        AtomicReference<Authentication> captured = new AtomicReference<>();

        filter.doFilter(request, new MockHttpServletResponse(), capturingChain(captured));

        assertThat(captured.get()).isNull();
    }

    @Test
    void rejectsInvalidUserIdentityEvenWithTrustedGatewaySecret() throws Exception {
        TrustedGatewayAuthenticationFilter filter = new TrustedGatewayAuthenticationFilter(SECRET);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(TrustedGatewayAuthenticationFilter.INTERNAL_GATEWAY_SECRET_HEADER, SECRET);
        request.addHeader(TrustedGatewayAuthenticationFilter.AUTHENTICATED_USER_HEADER, "not-a-uuid");
        AtomicReference<Authentication> captured = new AtomicReference<>();

        filter.doFilter(request, new MockHttpServletResponse(), capturingChain(captured));

        assertThat(captured.get()).isNull();
    }

    private FilterChain capturingChain(AtomicReference<Authentication> captured) {
        return new FilterChain() {
            @Override
            public void doFilter(ServletRequest request, ServletResponse response)
                    throws IOException, ServletException {
                captured.set(SecurityContextHolder.getContext().getAuthentication());
            }
        };
    }
}
