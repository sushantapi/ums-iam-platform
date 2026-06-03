package com.ums.security.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

import com.ums.security.service.JwtTokenProvider;

import lombok.RequiredArgsConstructor;

@Configuration
@RequiredArgsConstructor
public class JwtDecoderConfig {

	private final JwtTokenProvider jwtTokenProvider;

	@Bean
	public JwtDecoder jwtDecoder() {

		return NimbusJwtDecoder.withPublicKey(jwtTokenProvider.getPublicKey()).build();
	}
}