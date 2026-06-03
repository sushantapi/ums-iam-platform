package com.ums.user.dto;

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
public class UpdateProfileRequest {

	private String firstName;

	private String lastName;

	private String mobile;

	private String address;

	private String city;

	private String state;

	private String country;

	private String zipCode;
}