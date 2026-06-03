package com.ums.notification.dto;

import com.ums.notification.enums.NotificationChannel;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateTemplateRequest {

	@NotBlank
	private String templateCode;

	@NotBlank
	private String subject;

	@NotBlank
	private String body;

	private NotificationChannel channel;
}