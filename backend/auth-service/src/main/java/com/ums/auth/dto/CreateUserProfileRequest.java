package com.ums.auth.dto;

import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateUserProfileRequest {

	private UUID userId;

	private String firstName;

	private String lastName;

	private String email;

	private String mobile;
}