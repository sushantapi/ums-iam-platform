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
public class UserPreferenceResponse {

	private UUID userId;

	private String language;

	private String theme;

	private Boolean emailNotification;

	private Boolean smsNotification;
}