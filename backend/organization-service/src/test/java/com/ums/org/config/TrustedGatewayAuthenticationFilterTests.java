package com.ums.org.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

class TrustedGatewayAuthenticationFilterTests {

	private static final String GATEWAY_SECRET = "test-gateway-secret";

	@AfterEach
	void clearSecurityContext() {
		SecurityContextHolder.clearContext();
	}

	@Test
	void trustedGatewayHeadersCreateOrganizationAndMfaAssuranceAuthorities() throws Exception {
		UUID userId = UUID.randomUUID();
		UUID organizationId = UUID.randomUUID();
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.addHeader(TrustedGatewayAuthenticationFilter.AUTHENTICATED_USER_HEADER, userId.toString());
		request.addHeader(TrustedGatewayAuthenticationFilter.AUTHENTICATED_ORGANIZATION_HEADER, organizationId.toString());
		request.addHeader(TrustedGatewayAuthenticationFilter.MFA_VERIFIED_HEADER, "true");
		request.addHeader("X-User-Roles", "[ORG_ADMIN]");
		request.addHeader("X-Internal-Gateway-Secret", GATEWAY_SECRET);
		MockHttpServletResponse response = new MockHttpServletResponse();
		AtomicReference<Authentication> captured = new AtomicReference<>();

		TrustedGatewayAuthenticationFilter filter = new TrustedGatewayAuthenticationFilter(GATEWAY_SECRET);
		filter.doFilter(request, response, (servletRequest, servletResponse) ->
				captured.set(SecurityContextHolder.getContext().getAuthentication()));

		Authentication authentication = captured.get();
		assertThat(authentication).isNotNull();
		assertThat(authentication.getName()).isEqualTo(userId.toString());
		assertThat(authentication.getAuthorities())
				.extracting("authority")
				.contains(
						"ROLE_ORG_ADMIN",
						TrustedGatewayAuthenticationFilter.ORGANIZATION_CONTEXT_AUTHORITY_PREFIX + organizationId,
						TrustedGatewayAuthenticationFilter.MFA_VERIFIED_AUTHORITY);
	}

	@Test
	void malformedOrUnverifiedContextDoesNotCreateAssuranceAuthorities() throws Exception {
		UUID userId = UUID.randomUUID();
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.addHeader(TrustedGatewayAuthenticationFilter.AUTHENTICATED_USER_HEADER, userId.toString());
		request.addHeader(TrustedGatewayAuthenticationFilter.AUTHENTICATED_ORGANIZATION_HEADER, "not-a-uuid");
		request.addHeader(TrustedGatewayAuthenticationFilter.MFA_VERIFIED_HEADER, "false");
		request.addHeader("X-Internal-Gateway-Secret", GATEWAY_SECRET);
		AtomicReference<Authentication> captured = new AtomicReference<>();

		TrustedGatewayAuthenticationFilter filter = new TrustedGatewayAuthenticationFilter(GATEWAY_SECRET);
		filter.doFilter(request, new MockHttpServletResponse(), (servletRequest, servletResponse) ->
				captured.set(SecurityContextHolder.getContext().getAuthentication()));

		assertThat(captured.get()).isNotNull();
		assertThat(captured.get().getAuthorities())
				.extracting("authority")
				.doesNotContain(TrustedGatewayAuthenticationFilter.MFA_VERIFIED_AUTHORITY)
				.noneMatch(authority -> authority.toString().startsWith(
						TrustedGatewayAuthenticationFilter.ORGANIZATION_CONTEXT_AUTHORITY_PREFIX));
	}
}
