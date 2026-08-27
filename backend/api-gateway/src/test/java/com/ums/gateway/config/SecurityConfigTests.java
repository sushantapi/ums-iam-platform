package com.ums.gateway.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.cors.CorsConfiguration;

class SecurityConfigTests {

	@Test
	void corsAllowedOriginsAreLoadedFromConfiguration() {
		SecurityConfig securityConfig =
				new SecurityConfig("https://admin.example.com, https://portfolio.example.com ");

		CorsConfiguration corsConfiguration = securityConfig
				.corsConfigurationSource()
				.getCorsConfiguration(exchangeForPreflight());

		assertThat(corsConfiguration).isNotNull();
		assertThat(corsConfiguration.getAllowedOrigins())
				.containsExactly("https://admin.example.com", "https://portfolio.example.com");
		assertThat(corsConfiguration.getAllowCredentials()).isTrue();
	}

	private MockServerWebExchange exchangeForPreflight() {
		MockServerHttpRequest request = MockServerHttpRequest
				.options("/api/v1/auth/login")
				.header("Origin", "https://admin.example.com")
				.header("Access-Control-Request-Method", "POST")
				.build();

		return MockServerWebExchange.from(request);
	}
}
