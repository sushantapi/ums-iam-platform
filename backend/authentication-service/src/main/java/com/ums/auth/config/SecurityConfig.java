package com.ums.auth.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.preauth.AbstractPreAuthenticatedProcessingFilter;

import com.ums.auth.security.InternalServiceAuthenticationFilter;
import com.ums.auth.security.TrustedGatewayAuthenticationFilter;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

	@Bean
	@Order(1)
	SecurityFilterChain internalSecurityFilterChain(
			HttpSecurity http,
			InternalServiceAuthenticationFilter internalServiceAuthenticationFilter) throws Exception {
		return http
				.securityMatcher("/api/v1/internal/**", "/internal/**")
				.csrf(csrf -> csrf.disable())
				.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
				.httpBasic(httpBasic -> httpBasic.disable())
				.formLogin(form -> form.disable())
				.authorizeHttpRequests(auth -> auth.anyRequest().hasRole("INTERNAL_SERVICE"))
				.addFilterBefore(internalServiceAuthenticationFilter, AbstractPreAuthenticatedProcessingFilter.class)
				.build();
	}

	@Bean
	@Order(2)
	SecurityFilterChain externalSecurityFilterChain(
			HttpSecurity http,
			TrustedGatewayAuthenticationFilter trustedGatewayAuthenticationFilter) throws Exception {
		return http
				.csrf(csrf -> csrf.disable())
				.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
				.httpBasic(httpBasic -> httpBasic.disable())
				.formLogin(form -> form.disable())
				.authorizeHttpRequests(auth -> auth
						.requestMatchers(HttpMethod.POST,
								"/api/v1/auth/register",
								"/api/v1/auth/login",
								"/api/v1/auth/refresh",
								"/api/v1/auth/forgot-password",
								"/api/v1/auth/reset-password",
								"/api/v1/auth/mfa/challenge/verify")
							.permitAll()
						.requestMatchers(HttpMethod.POST,
								"/api/v1/auth/mfa/totp/setup",
								"/api/v1/auth/mfa/totp/confirm",
								"/api/v1/auth/mfa/recovery-codes/rotate",
								"/api/v1/auth/mfa/disable")
							.authenticated()
						.requestMatchers(HttpMethod.GET, "/api/v1/auth/mfa/status").authenticated()
						.requestMatchers(HttpMethod.GET, "/actuator/health", "/actuator/info").permitAll()
						.requestMatchers(HttpMethod.POST, "/api/v1/auth/logout").authenticated()
						.requestMatchers("/api/v1/admin/sessions/**", "/api/v1/admin/users/*/sessions/**")
							.hasRole("SUPER_ADMIN")
						.anyRequest().denyAll())
				.addFilterBefore(trustedGatewayAuthenticationFilter, AbstractPreAuthenticatedProcessingFilter.class)
				.build();
	}

	@Bean
	UserDetailsService userDetailsService() {
		return new InMemoryUserDetailsManager();
	}
}
