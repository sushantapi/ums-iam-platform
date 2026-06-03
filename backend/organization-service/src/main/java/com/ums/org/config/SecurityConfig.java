/*
 * package com.ums.org.config;
 * 
 * import org.springframework.context.annotation.Bean; import
 * org.springframework.context.annotation.Configuration;
 * 
 * @Configuration
 * 
 * @EnableWebSecurity public class SecurityConfig {
 * 
 * @Bean SecurityFilterChain securityFilterChain(HttpSecurity http) throws
 * Exception {
 * 
 * http.csrf(csrf -> csrf.disable()).authorizeHttpRequests(auth ->
 * auth.anyRequest().permitAll());
 * 
 * return http.build(); } }
 */