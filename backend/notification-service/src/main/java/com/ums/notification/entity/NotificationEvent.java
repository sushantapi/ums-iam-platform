package com.ums.notification.entity;

import java.time.LocalDateTime;

import com.ums.notification.enums.NotificationChannel;
import com.ums.notification.enums.NotificationStatus;
import com.ums.notification.enums.NotificationType;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "notification_events")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationEvent {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	private String recipient;

	private String recipientEmail;

	private String templateCode;

	private String correlationId;

	@Enumerated(EnumType.STRING)
	private NotificationChannel channel;

	@Enumerated(EnumType.STRING)
	private NotificationType notificationType;

	@Enumerated(EnumType.STRING)
	private NotificationStatus status;

	@Lob
	private String payload;

	@Lob
	private String errorMessage;

	private Integer retryCount;

	private LocalDateTime createdAt;

	private LocalDateTime processedAt;
}