package com.ums.auth.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class MfaTotpConfirmRequest {

	@NotBlank(message = "MFA code is required")
	private String code;
}
