package com.ums.hrms.leave.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import jakarta.servlet.FilterChain;

class TrustedGatewayAuthenticationFilterTests {

    private static final String SECRET = "test-gateway-secret";

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void authenticatesTrustedRequestAndClearsContext() throws Exception {
        UUID id = UUID.randomUUID();
        var filter = new TrustedGatewayAuthenticationFilter(SECRET);
        var request = requestWithSecret(SECRET);
        request.addHeader(TrustedGatewayAuthenticationFilter.AUTHENTICATED_USER_HEADER, id.toString());
        request.addHeader(TrustedGatewayAuthenticationFilter.USER_ROLES_HEADER, "HR_MANAGER");
        request.addHeader(TrustedGatewayAuthenticationFilter.USER_PERMISSIONS_HEADER, "LEAVE_READ");

        var captured = new AtomicReference<Authentication>();
        filter.doFilter(request, new MockHttpServletResponse(), capture(captured));

        assertThat(captured.get()).isNotNull();
        assertThat(captured.get().getName()).isEqualTo(id.toString());
        assertThat(captured.get().getAuthorities())
                .extracting("authority")
                .containsExactlyInAnyOrder("ROLE_HR_MANAGER", "LEAVE_READ");
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void preservesExistingRolePrefixWithoutDoublePrefixing() throws Exception {
        var filter = new TrustedGatewayAuthenticationFilter(SECRET);
        var request = requestWithSecret(SECRET);
        request.addHeader(TrustedGatewayAuthenticationFilter.AUTHENTICATED_USER_HEADER, UUID.randomUUID().toString());
        request.addHeader(TrustedGatewayAuthenticationFilter.USER_ROLES_HEADER, "ROLE_HR_MANAGER");

        var captured = new AtomicReference<Authentication>();
        filter.doFilter(request, new MockHttpServletResponse(), capture(captured));

        assertThat(captured.get()).isNotNull();
        assertThat(captured.get().getAuthorities())
                .extracting("authority")
                .containsExactly("ROLE_HR_MANAGER");
    }

    @Test
    void mapsPermissionsWithoutRolePrefix() throws Exception {
        var filter = new TrustedGatewayAuthenticationFilter(SECRET);
        var request = requestWithSecret(SECRET);
        request.addHeader(TrustedGatewayAuthenticationFilter.AUTHENTICATED_USER_HEADER, UUID.randomUUID().toString());
        request.addHeader(TrustedGatewayAuthenticationFilter.USER_PERMISSIONS_HEADER, "LEAVE_READ,LEAVE_CREATE");

        var captured = new AtomicReference<Authentication>();
        filter.doFilter(request, new MockHttpServletResponse(), capture(captured));

        assertThat(captured.get()).isNotNull();
        assertThat(captured.get().getAuthorities())
                .extracting("authority")
                .containsExactlyInAnyOrder("LEAVE_READ", "LEAVE_CREATE");
    }

    @Test
    void rejectsWrongSecret() throws Exception {
        var filter = new TrustedGatewayAuthenticationFilter(SECRET);
        var request = requestWithSecret("wrong-secret");
        request.addHeader(TrustedGatewayAuthenticationFilter.AUTHENTICATED_USER_HEADER, UUID.randomUUID().toString());

        var captured = new AtomicReference<Authentication>();
        filter.doFilter(request, new MockHttpServletResponse(), capture(captured));

        assertThat(captured.get()).isNull();
    }

    @Test
    void rejectsValidSecretWithoutUserIdentity() throws Exception {
        var filter = new TrustedGatewayAuthenticationFilter(SECRET);
        var request = requestWithSecret(SECRET);

        var captured = new AtomicReference<Authentication>();
        filter.doFilter(request, new MockHttpServletResponse(), capture(captured));

        assertThat(captured.get()).isNull();
    }

    @Test
    void rejectsSpoofedIdentityWithoutSecret() throws Exception {
        var filter = new TrustedGatewayAuthenticationFilter(SECRET);
        var request = new MockHttpServletRequest();
        request.addHeader(TrustedGatewayAuthenticationFilter.AUTHENTICATED_USER_HEADER, UUID.randomUUID().toString());
        request.addHeader(TrustedGatewayAuthenticationFilter.USER_ROLES_HEADER, "SUPER_ADMIN");
        request.addHeader(TrustedGatewayAuthenticationFilter.USER_PERMISSIONS_HEADER, "*");

        var captured = new AtomicReference<Authentication>();
        filter.doFilter(request, new MockHttpServletResponse(), capture(captured));

        assertThat(captured.get()).isNull();
    }

    @Test
    void rejectsMalformedIdentity() throws Exception {
        var filter = new TrustedGatewayAuthenticationFilter(SECRET);
        var request = requestWithSecret(SECRET);
        request.addHeader(TrustedGatewayAuthenticationFilter.AUTHENTICATED_USER_HEADER, "not-a-uuid");

        var captured = new AtomicReference<Authentication>();
        filter.doFilter(request, new MockHttpServletResponse(), capture(captured));

        assertThat(captured.get()).isNull();
    }

    private MockHttpServletRequest requestWithSecret(String secret) {
        var request = new MockHttpServletRequest();
        request.addHeader(TrustedGatewayAuthenticationFilter.INTERNAL_GATEWAY_SECRET_HEADER, secret);
        return request;
    }

    private FilterChain capture(AtomicReference<Authentication> captured) {
        return (request, response) -> captured.set(SecurityContextHolder.getContext().getAuthentication());
    }
}
