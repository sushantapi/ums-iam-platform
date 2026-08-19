package com.ums.hrms.attendance.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

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

        filter.doFilter(request, new MockHttpServletResponse(), new MockFilterChain());

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void rejectsCallerControlledIdentityWithoutTrustedGatewaySecret() throws Exception {
        TrustedGatewayAuthenticationFilter filter = new TrustedGatewayAuthenticationFilter(SECRET);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(TrustedGatewayAuthenticationFilter.AUTHENTICATED_USER_HEADER, UUID.randomUUID().toString());
        request.addHeader(TrustedGatewayAuthenticationFilter.USER_ROLES_HEADER, "SUPER_ADMIN");
        request.addHeader(TrustedGatewayAuthenticationFilter.USER_PERMISSIONS_HEADER, "ATTENDANCE_CREATE");

        filter.doFilter(request, new MockHttpServletResponse(), new MockFilterChain());

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void rejectsInvalidUserIdentityEvenWithTrustedGatewaySecret() throws Exception {
        TrustedGatewayAuthenticationFilter filter = new TrustedGatewayAuthenticationFilter(SECRET);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(TrustedGatewayAuthenticationFilter.INTERNAL_GATEWAY_SECRET_HEADER, SECRET);
        request.addHeader(TrustedGatewayAuthenticationFilter.AUTHENTICATED_USER_HEADER, "not-a-uuid");

        filter.doFilter(request, new MockHttpServletResponse(), new MockFilterChain());

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }
}
