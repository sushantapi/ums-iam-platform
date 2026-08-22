package com.ums.auth.dto;

import java.time.Instant;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class MfaTotpSetupResponse {
	private String secret;
	private String provisioningUri;
	private Instant expiresAt;
}
