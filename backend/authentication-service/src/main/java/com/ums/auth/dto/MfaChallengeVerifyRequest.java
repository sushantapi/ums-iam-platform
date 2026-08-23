package com.ums.auth.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class MfaChallengeVerifyRequest {

	@NotBlank(message = "MFA challenge token is required")
	private String challengeToken;

	private String totpCode;

	private String recoveryCode;
}
