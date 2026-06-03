package com.ums.user.dto;

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
public class UserProfileResponse {

	private UUID userId;

	private String firstName;

	private String lastName;

	private String email;

	private String mobile;

	private String avatarUrl;

	private String address;

	private String city;

	private String state;

	private String country;

	private String zipCode;
}