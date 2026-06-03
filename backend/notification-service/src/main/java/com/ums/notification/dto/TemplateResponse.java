package com.ums.notification.dto;

import java.time.LocalDateTime;
import java.util.UUID;

import com.ums.notification.enums.NotificationChannel;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TemplateResponse {

	private UUID id;

	private String templateCode;

	private String subject;

	private String body;

	private NotificationChannel channel;

	private Boolean active;

	private LocalDateTime createdAt;
}