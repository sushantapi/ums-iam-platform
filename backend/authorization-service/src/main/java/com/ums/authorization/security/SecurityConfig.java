package com.ums.authorization.security;

import org.springframework.core.annotation.Order;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import lombok.RequiredArgsConstructor;

@Configuration
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

	private final TrustedGatewayAuthenticationFilter trustedGatewayAuthenticationFilter;
	private final InternalServiceAuthenticationFilter internalServiceAuthenticationFilter;

	@Bean
	@Order(1)
	SecurityFilterChain internalSecurityFilterChain(HttpSecurity http) throws Exception {

		return http
				.securityMatcher("/api/v1/internal/**", "/internal/**")
				.csrf(csrf -> csrf.disable())
				.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
				.httpBasic(basic -> basic.disable())
				.formLogin(form -> form.disable())
				.authorizeHttpRequests(auth -> auth.anyRequest().hasRole("INTERNAL_SERVICE"))
				.addFilterBefore(internalServiceAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
				.build();
	}

	@Bean
	@Order(2)
	SecurityFilterChain apiSecurityFilterChain(HttpSecurity http) throws Exception {

		return http.csrf(csrf -> csrf.disable())
				.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
				.httpBasic(basic -> basic.disable())
				.formLogin(form -> form.disable())
				.authorizeHttpRequests(auth -> auth
						.requestMatchers(org.springframework.http.HttpMethod.GET,
								"/actuator/health", "/actuator/info").permitAll()
						.anyRequest().authenticated())
				.addFilterBefore(trustedGatewayAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
				.build();
	}
}
