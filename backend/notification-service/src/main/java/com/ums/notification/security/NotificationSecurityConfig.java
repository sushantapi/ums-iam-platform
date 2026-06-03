package com.ums.notification.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class NotificationSecurityConfig {

	/*
	 * @Bean SecurityFilterChain securityFilterChain(HttpSecurity http) throws
	 * Exception {
	 * 
	 * http.csrf(csrf -> csrf.disable())
	 * 
	 * .authorizeHttpRequests(auth -> auth
	 * 
	 * .requestMatchers("/api/test/**").permitAll()
	 * 
	 * .requestMatchers("/actuator/**").permitAll()
	 * 
	 * .anyRequest().authenticated())
	 * 
	 * .httpBasic(Customizer.withDefaults());
	 * 
	 * return http.build(); }
	 */

	@Bean
	SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

		System.out.println("=== CUSTOM SECURITY CHAIN LOADED ===");

		http.csrf(csrf -> csrf.disable()).authorizeHttpRequests(auth -> auth

				.requestMatchers("/api/test/**").permitAll().requestMatchers("/test/**").permitAll()
				.requestMatchers("/actuator/**").permitAll().requestMatchers("/api/v1/notifications/**").permitAll()

				.anyRequest().authenticated());

		return http.build();
	}
}