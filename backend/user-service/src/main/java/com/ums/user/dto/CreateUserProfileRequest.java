package com.ums.user.dto;

import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateUserProfileRequest {

	private String firstName;

	private String lastName;

	private String email;

	private String mobile;
}