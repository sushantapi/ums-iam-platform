package com.ums.auth.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class TokenResponse {
	private String accessToken;
	private String refreshToken;
	private String tokenType; // "Bearer"
	private long expiresIn; // seconds until access token expires
	private String userId;
	private String email;
	private boolean mfaRequired;
	private String mfaChallengeToken;
	private long mfaChallengeExpiresIn;
}
