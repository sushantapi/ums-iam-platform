package com.ums.auth.service;

import java.time.Duration;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class TokenBlacklistService {

	private final StringRedisTemplate redisTemplate;

	public void blacklist(String jti, long ttlSeconds) {

		redisTemplate.opsForValue().set("blacklist:" + jti, "true", Duration.ofSeconds(ttlSeconds));
	}

	public boolean isBlacklisted(String jti) {

		return Boolean.TRUE.equals(redisTemplate.hasKey("blacklist:" + jti));
	}
}