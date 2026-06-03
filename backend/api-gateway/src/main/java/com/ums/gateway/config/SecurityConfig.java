package com.ums.gateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;

@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

	@Bean
	SecurityWebFilterChain springSecurityFilterChain(ServerHttpSecurity http) {

		return http

				.csrf(ServerHttpSecurity.CsrfSpec::disable)

				.authorizeExchange(ex -> ex

						.pathMatchers("/api/v1/auth/login", "/api/v1/auth/register", "/api/v1/auth/refresh",
								"/swagger-ui/**", "/v3/api-docs/**")
						.permitAll()

						.anyExchange().authenticated())

				.httpBasic(ServerHttpSecurity.HttpBasicSpec::disable)

				.formLogin(ServerHttpSecurity.FormLoginSpec::disable)

				.build();
	}
}