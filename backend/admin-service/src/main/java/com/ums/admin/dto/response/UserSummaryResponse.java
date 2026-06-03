package com.ums.admin.dto.response;

import java.util.UUID;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserSummaryResponse {

	private UUID id;

	private String firstName;

	private String lastName;

	private String email;
}