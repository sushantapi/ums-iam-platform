package com.ums.gateway.security;

import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class JwtTokenValidator {

    private final JwtDecoder jwtDecoder;

    public Jwt validateToken(String token) {
        return jwtDecoder.decode(token);
    }
}