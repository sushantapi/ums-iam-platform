package com.ums.auth.dto;

import java.util.List;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AuthResponse {

	private String accessToken;

	private String tokenType;

	private Long expiresIn;

	private List<String> roles;

	private List<String> permissions;
}