package com.ums.auth.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import com.ums.auth.security.filter.JwtAuthenticationFilter;
import com.ums.auth.security.service.CustomUserDetailsService;

import lombok.RequiredArgsConstructor;

@Configuration
@RequiredArgsConstructor
public class SecurityConfig {

	private final JwtAuthenticationFilter jwtAuthenticationFilter;

	@Bean
	SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

		http.csrf(csrf -> csrf.disable())
				.authorizeHttpRequests(auth -> auth.requestMatchers("/api/v1/auth/register", "/api/v1/auth/login")
						.permitAll().anyRequest().authenticated())
				.httpBasic(Customizer.withDefaults());

		return http.build();
	}

//	@Bean
//	public AuthenticationProvider authenticationProvider(CustomUserDetailsService customUserDetailsService,
//			PasswordEncoder passwordEncoder) {
//
//		DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
//
//		provider.setUserDetailsService(customUserDetailsService);
//
//		provider.setPasswordEncoder(passwordEncoder);
//
//		return provider;
//	}

	@Bean
	public PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}
}