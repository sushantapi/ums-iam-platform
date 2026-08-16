package com.ums.authorization.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateRoleRequest {

	@NotBlank
	@Pattern(regexp = "^[A-Z][A-Z0-9_]{1,63}$")
	private String name;

	@Size(max = 255)
	private String description;
}