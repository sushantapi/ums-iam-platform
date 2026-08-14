package com.ums.auth.service;

import java.time.Duration;
import java.util.UUID;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class TokenBlacklistService {

	private static final String TOKEN_BLACKLIST_PREFIX = "blacklist:";
	private static final String SESSION_REVOKED_PREFIX = "session-revoked:";

	private final StringRedisTemplate redisTemplate;

	public void blacklist(String jti, long ttlSeconds) {
		if (jti == null || jti.isBlank() || ttlSeconds <= 0) {
			return;
		}
		redisTemplate.opsForValue().set(TOKEN_BLACKLIST_PREFIX + jti, "true", Duration.ofSeconds(ttlSeconds));
	}

	public boolean isBlacklisted(String jti) {
		return jti != null && Boolean.TRUE.equals(redisTemplate.hasKey(TOKEN_BLACKLIST_PREFIX + jti));
	}

	public void revokeSession(UUID sessionId, long ttlSeconds) {
		if (sessionId == null || ttlSeconds <= 0) {
			return;
		}
		redisTemplate.opsForValue().set(SESSION_REVOKED_PREFIX + sessionId, "true", Duration.ofSeconds(ttlSeconds));
	}

	public boolean isSessionRevoked(UUID sessionId) {
		return sessionId != null && Boolean.TRUE.equals(redisTemplate.hasKey(SESSION_REVOKED_PREFIX + sessionId));
	}
}
