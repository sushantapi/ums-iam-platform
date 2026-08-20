package com.ums.hrms.payroll.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

class TrustedGatewayAuthenticationFilterTests {

    private static final String SECRET = "payroll-test-gateway-secret";
    private static final String USER_ID = "9466dcac-b809-401a-9e46-a58b7c0dda82";

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void authenticatesTrustedGatewayIdentityAndClearsContextAfterRequest() throws Exception {
        TrustedGatewayAuthenticationFilter filter = new TrustedGatewayAuthenticationFilter(SECRET);
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        request.addHeader(TrustedGatewayAuthenticationFilter.AUTHENTICATED_USER_HEADER, USER_ID);
        request.addHeader(TrustedGatewayAuthenticationFilter.INTERNAL_GATEWAY_SECRET_HEADER, SECRET);
        request.addHeader(TrustedGatewayAuthenticationFilter.USER_ROLES_HEADER, "HR_MANAGER");
        request.addHeader(
                TrustedGatewayAuthenticationFilter.USER_PERMISSIONS_HEADER,
                "PAYROLL_READ,PAYROLL_RUN_MANAGE");

        AtomicBoolean chainInvoked = new AtomicBoolean(false);

        filter.doFilter(request, response, (req, res) -> {
            chainInvoked.set(true);
            var authentication = SecurityContextHolder.getContext().getAuthentication();
            assertNotNull(authentication);
            assertEquals(USER_ID, authentication.getPrincipal());
            assertTrue(authentication.getAuthorities().stream()
                    .anyMatch(authority -> "ROLE_HR_MANAGER".equals(authority.getAuthority())));
            assertTrue(authentication.getAuthorities().stream()
                    .anyMatch(authority -> "PAYROLL_READ".equals(authority.getAuthority())));
            assertTrue(authentication.getAuthorities().stream()
                    .anyMatch(authority -> "PAYROLL_RUN_MANAGE".equals(authority.getAuthority())));
        });

        assertTrue(chainInvoked.get());
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void rejectsSpoofedHeadersWhenGatewaySecretDoesNotMatch() throws Exception {
        TrustedGatewayAuthenticationFilter filter = new TrustedGatewayAuthenticationFilter(SECRET);
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        request.addHeader(TrustedGatewayAuthenticationFilter.AUTHENTICATED_USER_HEADER, USER_ID);
        request.addHeader(TrustedGatewayAuthenticationFilter.INTERNAL_GATEWAY_SECRET_HEADER, "wrong-secret");
        request.addHeader(TrustedGatewayAuthenticationFilter.USER_ROLES_HEADER, "SUPER_ADMIN");

        filter.doFilter(request, response, (req, res) ->
                assertNull(SecurityContextHolder.getContext().getAuthentication()));

        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void failsClosedWhenConfiguredGatewaySecretIsBlank() throws Exception {
        TrustedGatewayAuthenticationFilter filter = new TrustedGatewayAuthenticationFilter(" ");
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        request.addHeader(TrustedGatewayAuthenticationFilter.AUTHENTICATED_USER_HEADER, USER_ID);
        request.addHeader(TrustedGatewayAuthenticationFilter.INTERNAL_GATEWAY_SECRET_HEADER, " ");

        filter.doFilter(request, response, (req, res) ->
                assertNull(SecurityContextHolder.getContext().getAuthentication()));

        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }
}
