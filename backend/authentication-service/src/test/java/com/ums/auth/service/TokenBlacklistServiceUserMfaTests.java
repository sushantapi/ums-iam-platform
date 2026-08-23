package com.ums.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

class TokenBlacklistServiceUserMfaTests {

	@Test
	void userFailureBudgetPersistsIndependentlyOfChallengeJti() {
		StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
		@SuppressWarnings("unchecked")
		ValueOperations<String, String> valueOperations = mock(ValueOperations.class);
		UUID userId = UUID.randomUUID();
		String attemptsKey = "mfa-user-attempts:" + userId;
		String blockedKey = "mfa-user-blocked:" + userId;

		when(redisTemplate.opsForValue()).thenReturn(valueOperations);
		when(valueOperations.setIfAbsent(eq(attemptsKey), eq("1"), any(Duration.class)))
				.thenReturn(true, false);
		when(valueOperations.increment(attemptsKey)).thenReturn(5L);

		TokenBlacklistService service = new TokenBlacklistService(redisTemplate);

		assertThat(service.recordMfaUserFailure(userId, 900, 5)).isFalse();
		assertThat(service.recordMfaUserFailure(userId, 900, 5)).isTrue();

		verify(valueOperations).set(eq(blockedKey), eq("true"), any(Duration.class));
	}

	@Test
	void blockedStateAndClearUseUserScopedKeys() {
		StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
		UUID userId = UUID.randomUUID();
		String attemptsKey = "mfa-user-attempts:" + userId;
		String blockedKey = "mfa-user-blocked:" + userId;
		when(redisTemplate.hasKey(blockedKey)).thenReturn(true);

		TokenBlacklistService service = new TokenBlacklistService(redisTemplate);

		assertThat(service.isMfaUserBlocked(userId)).isTrue();
		service.clearMfaUserFailures(userId);

		verify(redisTemplate).delete(attemptsKey);
		verify(redisTemplate).delete(blockedKey);
	}
}
