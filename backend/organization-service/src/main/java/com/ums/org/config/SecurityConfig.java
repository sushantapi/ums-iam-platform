package com.ums.org.config;

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
public class SecurityConfig {

	@Bean
	@Order(1)
	SecurityFilterChain internalSecurityFilterChain(HttpSecurity http,
			InternalServiceAuthenticationFilter internalServiceAuthenticationFilter) throws Exception {
		return http
				.securityMatcher("/api/v1/internal/**", "/internal/**")
				.csrf(csrf -> csrf.disable())
				.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
				.httpBasic(basic -> basic.disable())
				.formLogin(form -> form.disable())
				.authorizeHttpRequests(auth -> auth.anyRequest().hasRole("INTERNAL_SERVICE"))
				.addFilterBefore(internalServiceAuthenticationFilter, AbstractPreAuthenticatedProcessingFilter.class)
				.build();
	}

	@Bean
	@Order(2)
	SecurityFilterChain gatewaySecurityFilterChain(HttpSecurity http, TrustedGatewayAuthenticationFilter gatewayAuthFilter)
			throws Exception {
		return http
				.csrf(csrf -> csrf.disable())
				.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
				.httpBasic(basic -> basic.disable())
				.formLogin(form -> form.disable())
				.addFilterBefore(gatewayAuthFilter, AbstractPreAuthenticatedProcessingFilter.class)
				.authorizeHttpRequests(auth -> auth
						.requestMatchers(HttpMethod.GET, "/actuator/health", "/actuator/info").permitAll()
						.requestMatchers("/api/v1/organizations/**").authenticated()
						.anyRequest().denyAll())
				.build();
	}

	@Bean
	UserDetailsService userDetailsService() {
		return new InMemoryUserDetailsManager();
	}
}
