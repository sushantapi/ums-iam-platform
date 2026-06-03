package com.ums.user.dto;

import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdatePreferenceRequest {

	private String language;

	private String theme;

	private Boolean emailNotification;

	private Boolean smsNotification;
}