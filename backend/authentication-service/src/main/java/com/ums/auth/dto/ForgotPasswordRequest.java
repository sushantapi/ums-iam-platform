package com.ums.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ForgotPasswordRequest {

	@NotBlank(message = "Email is required")
	@Email(message = "Must be a valid email")
	@Size(max = 150, message = "Email must not exceed 150 characters")
	private String email;
}
