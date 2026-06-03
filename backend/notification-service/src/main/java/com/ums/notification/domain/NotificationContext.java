package com.ums.notification.domain;

import java.util.Map;

import com.ums.notification.enums.NotificationChannel;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class NotificationContext {

	private NotificationChannel channel;

	private String recipientEmail;

	private String recipientName;

	private String templateCode;

	private Map<String, Object> variables;
}