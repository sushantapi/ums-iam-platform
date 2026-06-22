/*
 * package com.ums.auth.config;
 * 
 * import org.springframework.context.annotation.Bean; import
 * org.springframework.context.annotation.Configuration; import
 * org.springframework.security.config.Customizer; import
 * org.springframework.security.config.annotation.method.configuration.
 * EnableMethodSecurity; import
 * org.springframework.security.config.annotation.web.builders.HttpSecurity;
 * import org.springframework.security.config.http.SessionCreationPolicy; import
 * org.springframework.security.web.SecurityFilterChain; import
 * org.springframework.security.web.authentication.
 * UsernamePasswordAuthenticationFilter;
 * 
 * import com.ums.auth.security.JwtAuthenticationEntryPoint; import
 * com.ums.auth.security.JwtAuthenticationFilter;
 * 
 * import lombok.RequiredArgsConstructor;
 * 
 * @Configuration
 * 
 * @EnableMethodSecurity
 * 
 * @RequiredArgsConstructor public class SecurityConfig {
 * 
 * private final JwtAuthenticationFilter jwtFilter; private final
 * JwtAuthenticationEntryPoint entryPoint;
 * 
 * @Bean SecurityFilterChain securityFilterChain(HttpSecurity http) throws
 * Exception {
 * 
 * http.csrf(csrf -> csrf.disable())
 * 
 * .sessionManagement(session ->
 * session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
 * 
 * .exceptionHandling(ex -> ex.authenticationEntryPoint(entryPoint))
 * 
 * .authorizeHttpRequests(auth -> auth
 * 
 * .requestMatchers("/api/v1/auth/register", "/api/v1/auth/login",
 * "/api/v1/auth/refresh") .permitAll()
 * 
 * .anyRequest().authenticated())
 * 
 * .httpBasic(Customizer.withDefaults());
 * 
 * http.addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);
 * 
 * return http.build(); } }
 */

package com.ums.auth.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import com.ums.security.filter.JwtAuthenticationFilter;

import lombok.RequiredArgsConstructor;

@Configuration
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

	private final JwtAuthenticationFilter jwtAuthenticationFilter;

	@Bean
	SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

		http.csrf(csrf -> csrf.disable())

				.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

				.authorizeHttpRequests(auth -> auth

						.requestMatchers("/api/v1/auth/register", "/api/v1/auth/login", "/api/v1/auth/refresh")
						.permitAll()

						.requestMatchers("/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**", "/actuator/**")
						.permitAll()

						.anyRequest().authenticated());

		http.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

		return http.build();
	}
}
