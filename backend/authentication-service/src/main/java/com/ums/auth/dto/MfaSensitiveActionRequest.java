package com.ums.auth.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class MfaSensitiveActionRequest {

	@NotBlank(message = "Password is required")
	private String password;

	private String totpCode;

	private String recoveryCode;
}
