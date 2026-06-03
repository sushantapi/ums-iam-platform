package com.ums.auth.security.service;

import java.time.Instant;

import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class JwtService {

	private final JwtEncoder jwtEncoder;

	public String generateToken(com.ums.auth.dto.JwtUser jwtUser) {

		Instant now = Instant.now();

		JwtClaimsSet claims = JwtClaimsSet.builder().issuer("auth-service").issuedAt(now)
				.expiresAt(now.plusSeconds(3600)).subject(jwtUser.getUserId().toString()).build();

		return jwtEncoder.encode(JwtEncoderParameters.from(claims)).getTokenValue();
	}
} 