package com.ums.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Duration;

import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

class TokenBlacklistServiceMfaTests {

	@Test
	void challengeConsumptionUsesAtomicSetIfAbsent() {
		StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
		@SuppressWarnings("unchecked")
		ValueOperations<String, String> valueOperations = mock(ValueOperations.class);
		when(redisTemplate.opsForValue()).thenReturn(valueOperations);
		when(valueOperations.setIfAbsent(
				eq("mfa-challenge-consumed:jti-1"),
				eq("true"),
				any(Duration.class)))
				.thenReturn(true, false);
		TokenBlacklistService service = new TokenBlacklistService(redisTemplate);

		assertThat(service.consumeMfaChallenge("jti-1", 300)).isTrue();
		assertThat(service.consumeMfaChallenge("jti-1", 300)).isFalse();
	}

	@Test
	void failedAttemptCounterExpiresWithChallengeAndStopsAtLimit() {
		StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
		@SuppressWarnings("unchecked")
		ValueOperations<String, String> valueOperations = mock(ValueOperations.class);
		when(redisTemplate.opsForValue()).thenReturn(valueOperations);
		when(valueOperations.setIfAbsent(
				eq("mfa-challenge-attempts:jti-2"),
				eq("1"),
				any(Duration.class)))
				.thenReturn(true, false);
		when(valueOperations.increment("mfa-challenge-attempts:jti-2")).thenReturn(5L);
		TokenBlacklistService service = new TokenBlacklistService(redisTemplate);

		assertThat(service.recordMfaChallengeFailure("jti-2", 300, 5)).isFalse();
		assertThat(service.recordMfaChallengeFailure("jti-2", 300, 5)).isTrue();
	}
}
