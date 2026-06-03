package com.ums.security.config;

import java.security.interfaces.RSAPublicKey;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.ums.security.service.JwtTokenProvider;

@Configuration
public class RsaKeyConfig {

	@Bean
	public RSAPublicKey rsaPublicKey(JwtTokenProvider jwtTokenProvider) {

		return jwtTokenProvider.getPublicKey();
	}
}