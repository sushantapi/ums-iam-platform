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
	private static final String MFA_CHALLENGE_CONSUMED_PREFIX = "mfa-challenge-consumed:";
	private static final String MFA_CHALLENGE_ATTEMPTS_PREFIX = "mfa-challenge-attempts:";

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

	public boolean isMfaChallengeConsumed(String jti) {
		return jti != null && Boolean.TRUE.equals(redisTemplate.hasKey(MFA_CHALLENGE_CONSUMED_PREFIX + jti));
	}

	public boolean consumeMfaChallenge(String jti, long ttlSeconds) {
		if (jti == null || jti.isBlank() || ttlSeconds <= 0) {
			return false;
		}
		Boolean created = redisTemplate.opsForValue().setIfAbsent(
				MFA_CHALLENGE_CONSUMED_PREFIX + jti,
				"true",
				Duration.ofSeconds(ttlSeconds));
		return Boolean.TRUE.equals(created);
	}

	public boolean recordMfaChallengeFailure(String jti, long ttlSeconds, int maxAttempts) {
		if (jti == null || jti.isBlank() || ttlSeconds <= 0 || maxAttempts <= 0) {
			return true;
		}

		String key = MFA_CHALLENGE_ATTEMPTS_PREFIX + jti;
		Boolean created = redisTemplate.opsForValue().setIfAbsent(key, "1", Duration.ofSeconds(ttlSeconds));
		if (Boolean.TRUE.equals(created)) {
			return maxAttempts <= 1;
		}

		Long attempts = redisTemplate.opsForValue().increment(key);
		return attempts == null || attempts >= maxAttempts;
	}
}
