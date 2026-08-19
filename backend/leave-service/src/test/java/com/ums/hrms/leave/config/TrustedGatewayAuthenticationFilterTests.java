package com.ums.hrms.leave.config;
import static org.assertj.core.api.Assertions.assertThat;
import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.*;
import org.springframework.mock.web.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import jakarta.servlet.*;
class TrustedGatewayAuthenticationFilterTests {
 private static final String SECRET="test-gateway-secret";
 @AfterEach void clear(){SecurityContextHolder.clearContext();}
 @Test void authenticatesTrustedRequestAndClearsContext() throws Exception{UUID id=UUID.randomUUID();var f=new TrustedGatewayAuthenticationFilter(SECRET);var r=new MockHttpServletRequest();r.addHeader(TrustedGatewayAuthenticationFilter.INTERNAL_GATEWAY_SECRET_HEADER,SECRET);r.addHeader(TrustedGatewayAuthenticationFilter.AUTHENTICATED_USER_HEADER,id.toString());r.addHeader(TrustedGatewayAuthenticationFilter.USER_ROLES_HEADER,"HR_MANAGER");r.addHeader(TrustedGatewayAuthenticationFilter.USER_PERMISSIONS_HEADER,"LEAVE_READ");var c=new AtomicReference<Authentication>();f.doFilter(r,new MockHttpServletResponse(),capture(c));assertThat(c.get()).isNotNull();assertThat(c.get().getName()).isEqualTo(id.toString());assertThat(c.get().getAuthorities()).extracting("authority").containsExactlyInAnyOrder("ROLE_HR_MANAGER","LEAVE_READ");assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();}
 @Test void rejectsSpoofedIdentityWithoutSecret() throws Exception{var f=new TrustedGatewayAuthenticationFilter(SECRET);var r=new MockHttpServletRequest();r.addHeader(TrustedGatewayAuthenticationFilter.AUTHENTICATED_USER_HEADER,UUID.randomUUID().toString());r.addHeader(TrustedGatewayAuthenticationFilter.USER_ROLES_HEADER,"SUPER_ADMIN");r.addHeader(TrustedGatewayAuthenticationFilter.USER_PERMISSIONS_HEADER,"*");var c=new AtomicReference<Authentication>();f.doFilter(r,new MockHttpServletResponse(),capture(c));assertThat(c.get()).isNull();}
 @Test void rejectsMalformedIdentity() throws Exception{var f=new TrustedGatewayAuthenticationFilter(SECRET);var r=new MockHttpServletRequest();r.addHeader(TrustedGatewayAuthenticationFilter.INTERNAL_GATEWAY_SECRET_HEADER,SECRET);r.addHeader(TrustedGatewayAuthenticationFilter.AUTHENTICATED_USER_HEADER,"not-a-uuid");var c=new AtomicReference<Authentication>();f.doFilter(r,new MockHttpServletResponse(),capture(c));assertThat(c.get()).isNull();}
 private FilterChain capture(AtomicReference<Authentication> c){return (req,res)->c.set(SecurityContextHolder.getContext().getAuthentication());}
}
