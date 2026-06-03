package com.ums.notification.dto;

import java.util.Map;

import com.ums.notification.enums.NotificationChannel;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class NotificationRequest {

	private String recipient;

	private String templateCode;

	private NotificationChannel channel;

	private Map<String, Object> variables;
}