package com.ums.gateway.security;

import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import reactor.core.publisher.Mono;

@Service
public class TokenRevocationService {

	private static final String TOKEN_BLACKLIST_PREFIX = "blacklist:";
	private static final String SESSION_REVOKED_PREFIX = "session-revoked:";

	private final ReactiveStringRedisTemplate redisTemplate;

	public TokenRevocationService(ReactiveStringRedisTemplate redisTemplate) {
		this.redisTemplate = redisTemplate;
	}

	public Mono<Boolean> isRevoked(Jwt jwt) {
		String jti = jwt.getId();
		String sessionId = jwt.getClaimAsString("sessionId");

		if (!StringUtils.hasText(jti) || !StringUtils.hasText(sessionId)) {
			return Mono.just(true);
		}

		Mono<Boolean> tokenRevoked = redisTemplate.hasKey(TOKEN_BLACKLIST_PREFIX + jti);
		Mono<Boolean> sessionRevoked = redisTemplate.hasKey(SESSION_REVOKED_PREFIX + sessionId);

		return Mono.zip(tokenRevoked, sessionRevoked)
				.map(result -> Boolean.TRUE.equals(result.getT1()) || Boolean.TRUE.equals(result.getT2()));
	}
}
