package com.ums.admin.dto.response;

import java.util.UUID;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserDetailResponse {

	private UUID id;

	private String firstName;

	private String lastName;

	private String email;

	private String mobile;

	private Boolean active;
}