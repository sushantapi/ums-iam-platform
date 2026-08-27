package com.ums.gateway.config;

import java.util.Arrays;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsConfigurationSource;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

    private static final String DEFAULT_ALLOWED_ORIGINS =
            "http://127.0.0.1:5174,http://localhost:5174";

    private final List<String> allowedOrigins;

    public SecurityConfig(
            @Value("${gateway.cors.allowed-origins:" + DEFAULT_ALLOWED_ORIGINS + "}")
            String allowedOrigins) {
        this.allowedOrigins = parseAllowedOrigins(allowedOrigins);
    }

    @Bean
    CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(allowedOrigins);
        configuration.setAllowedMethods(List.of(
                "GET",
                "POST",
                "PUT",
                "PATCH",
                "DELETE",
                "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source =
                new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    private List<String> parseAllowedOrigins(String configuredOrigins) {
        List<String> origins = Arrays.stream(configuredOrigins.split(","))
                .map(String::trim)
                .filter(origin -> !origin.isBlank())
                .toList();

        if (origins.isEmpty()) {
            throw new IllegalStateException("gateway.cors.allowed-origins must contain at least one origin");
        }

        return origins;
    }

    @Bean
    SecurityWebFilterChain springSecurityFilterChain(
            ServerHttpSecurity http,
            CorsConfigurationSource corsConfigurationSource) {

        return http
                .cors(cors ->
                        cors.configurationSource(corsConfigurationSource))
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .httpBasic(ServerHttpSecurity.HttpBasicSpec::disable)
                .formLogin(ServerHttpSecurity.FormLoginSpec::disable)
                .authorizeExchange(ex -> ex
                        .pathMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .pathMatchers(HttpMethod.POST,
                                "/api/v1/auth/register",
                                "/api/v1/auth/login",
                                "/api/v1/auth/refresh",
                                "/api/v1/auth/forgot-password",
                                "/api/v1/auth/reset-password",
                                "/api/v1/auth/mfa/challenge/verify")
                        .permitAll()
                        .pathMatchers(HttpMethod.GET,
                                "/actuator/health",
                                "/actuator/info")
                        .permitAll()
                        .anyExchange().authenticated())
                .oauth2ResourceServer(oauth2 ->
                        oauth2.jwt(jwt -> {
                        }))
                .build();
    }
}
