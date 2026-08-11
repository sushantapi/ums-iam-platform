package com.ums.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.preauth.AbstractPreAuthenticatedProcessingFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class AuditSecurityConfig {

	@Bean
	@Order(1)
	SecurityFilterChain internalSecurityFilterChain(HttpSecurity http,
			InternalServiceAuthenticationFilter internalServiceAuthenticationFilter) throws Exception {
		return http.securityMatcher("/api/v1/internal/**", "/internal/**").csrf(csrf -> csrf.disable())
				.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
				.httpBasic(httpBasic -> httpBasic.disable()).formLogin(form -> form.disable())
				.authorizeHttpRequests(auth -> auth.anyRequest().hasRole("INTERNAL_SERVICE"))
				.addFilterBefore(internalServiceAuthenticationFilter, AbstractPreAuthenticatedProcessingFilter.class)
				.build();
	}

	@Bean
	@Order(2)
	SecurityFilterChain gatewaySecurityFilterChain(HttpSecurity http,
			TrustedGatewayAuthenticationFilter trustedGatewayAuthenticationFilter) throws Exception {
		return http.csrf(csrf -> csrf.disable())
				.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
				.httpBasic(httpBasic -> httpBasic.disable()).formLogin(form -> form.disable())
				.authorizeHttpRequests(
						auth -> auth.requestMatchers(HttpMethod.GET, "/actuator/health", "/actuator/info").permitAll()
								.requestMatchers(HttpMethod.GET, "/api/v1/audit/events", "/api/v1/audit/events/**")
								.hasAnyAuthority("ROLE_SUPER_ADMIN", "ROLE_AUDIT_ADMIN", "ROLE_SECURITY",
										"ROLE_COMPLIANCE", "AUDIT_READ")
								.anyRequest().denyAll())
				.addFilterBefore(trustedGatewayAuthenticationFilter, AbstractPreAuthenticatedProcessingFilter.class)
				.build();
	}

	@Bean
	UserDetailsService userDetailsService() {
		return new InMemoryUserDetailsManager();
	}
}
